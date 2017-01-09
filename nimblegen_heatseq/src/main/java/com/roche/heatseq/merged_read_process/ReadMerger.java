package com.roche.heatseq.merged_read_process;

import htsjdk.samtools.fastq.FastqRecord;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.roche.heatseq.utils.FastqReader;
import com.roche.heatseq.utils.FastqWriter;
import com.roche.sequencing.bioinformatics.common.alignment.AlignmentPair;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
import com.roche.sequencing.bioinformatics.common.alignment.SimpleAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.multithreading.IExceptionListener;
import com.roche.sequencing.bioinformatics.common.multithreading.PausableFixedThreadPoolExecutor;
import com.roche.sequencing.bioinformatics.common.sequence.ICode;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCode;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.IProgressListener;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class ReadMerger {

	private final static DecimalFormat DF = new DecimalFormat("#,###");
	private final static DecimalFormat DF2 = new DecimalFormat("###.00");
	private final static int FASTQ_LINES_PER_READ = 4;
	private final static IAlignmentScorer MERGE_ALIGNMENT_SCORER = new SimpleAlignmentScorer(1, -1000, -100, -100, false, false);

	private ReadMerger() {
		throw new AssertionError();
	}

	private static class MergedReadsResults {
		private final FastqRecord mergedRecord;
		private final int numberOfBasesCorrected;
		private final boolean mismatchingBaseWithSameQualityScoreOccurred;

		public MergedReadsResults(FastqRecord mergedRecord, int numberOfBasesCorrected, int amountOfOverlap, boolean mismatchingBaseWithSameQualityScoreOccurred) {
			super();
			this.mergedRecord = mergedRecord;
			this.numberOfBasesCorrected = numberOfBasesCorrected;
			this.mismatchingBaseWithSameQualityScoreOccurred = mismatchingBaseWithSameQualityScoreOccurred;
		}

	}

	private static int countGaps(ISequence sequence) {
		int gapCount = 0;
		for (int i = 0; i < sequence.size(); i++) {
			if (sequence.getCodeAt(i).matches(IupacNucleotideCode.GAP)) {
				gapCount++;
			}
		}
		return gapCount;
	}

	private static MergedReadsResults mergeReads(FastqRecord recordOne, FastqRecord recordTwo) {
		ISequence readOne = new IupacNucleotideCodeSequence(recordOne.getReadString());
		ISequence readTwo = new IupacNucleotideCodeSequence(recordTwo.getReadString()).getReverseCompliment();

		NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(readTwo, readOne, MERGE_ALIGNMENT_SCORER);
		AlignmentPair alignmentPair = alignment.getAlignmentPair();
		ISequence readTwoAlignment = alignmentPair.getReferenceAlignment();
		ISequence readOneAlignment = alignmentPair.getQueryAlignment();

		int estimatedOverlap = readOne.size() - countGaps(readTwoAlignment);
		boolean readOneStartsWithGap = (readOneAlignment.getCodeAt(0).matches(IupacNucleotideCode.GAP));
		if ((estimatedOverlap <= 4) && (readOneStartsWithGap)) {
			int newOverlap = 0;
			if (readOne.getCodeAt(readOne.size() - 1).matches(readTwo.getCodeAt(0))) {
				newOverlap = 1;
			} else if ((readOne.getCodeAt(readOne.size() - 2).matches(readTwo.getCodeAt(0))) && (readOne.getCodeAt(readOne.size() - 1).matches(readTwo.getCodeAt(1)))) {
				newOverlap = 2;
			} else if ((readOne.getCodeAt(readOne.size() - 3).matches(readTwo.getCodeAt(0))) && (readOne.getCodeAt(readOne.size() - 2).matches(readTwo.getCodeAt(1)))
					&& (readOne.getCodeAt(readOne.size() - 1).matches(readTwo.getCodeAt(2)))) {
				newOverlap = 3;
			} else if ((readOne.getCodeAt(readOne.size() - 4).matches(readTwo.getCodeAt(0))) && (readOne.getCodeAt(readOne.size() - 3).matches(readTwo.getCodeAt(1)))
					&& (readOne.getCodeAt(readOne.size() - 2).matches(readTwo.getCodeAt(2))) && (readOne.getCodeAt(readOne.size() - 1).matches(readTwo.getCodeAt(3)))) {
				newOverlap = 4;
			}

			// just create an alignment where the two reads are pasted together
			readOneAlignment = new IupacNucleotideCodeSequence(readOne.toString());
			readOneAlignment.append(new IupacNucleotideCodeSequence(StringUtil.repeatString(IupacNucleotideCode.GAP.toString(), readTwo.size() - newOverlap)));

			readTwoAlignment = new IupacNucleotideCodeSequence(StringUtil.repeatString(IupacNucleotideCode.GAP.toString(), readOne.size() - newOverlap));
			readTwoAlignment.append(new IupacNucleotideCodeSequence(readTwo.toString()));
		}

		StringBuilder mergedSequence = new StringBuilder();
		StringBuilder mergedQuality = new StringBuilder();

		String readOneQuality = recordOne.getBaseQualityString();
		String readTwoQuality = StringUtil.reverse(recordTwo.getBaseQualityString());

		int readOneQualityIndex = 0;
		int readTwoQualityIndex = 0;

		int numberOfBasesCorrected = 0;
		int amountOfOverlap = 0;

		boolean mismatchingBaseWithSameQualityScoreOccurred = false;

		for (int i = 0; i < readOneAlignment.size(); i++) {
			ICode readOneCode = readOneAlignment.getCodeAt(i);
			ICode readTwoCode = readTwoAlignment.getCodeAt(i);

			boolean isReadOneAGap = readOneCode.matches(IupacNucleotideCode.GAP);
			boolean isReadTwoAGap = readTwoCode.matches(IupacNucleotideCode.GAP);
			if (isReadOneAGap && !isReadTwoAGap) {
				mergedSequence.append(readTwoCode);
				mergedQuality.append(readTwoQuality.charAt(readTwoQualityIndex));
				readTwoQualityIndex++;
			} else if (!isReadOneAGap && isReadTwoAGap) {
				mergedSequence.append(readOneCode);
				mergedQuality.append(readOneQuality.charAt(readOneQualityIndex));
				readOneQualityIndex++;
			} else if (!isReadOneAGap && !isReadTwoAGap) {
				amountOfOverlap++;
				int readOneSequencePhredScore = (int) readOneQuality.charAt(readOneQualityIndex);
				int readTwoSequencePhredScore = (int) readTwoQuality.charAt(readTwoQualityIndex);

				if (readOneSequencePhredScore > readTwoSequencePhredScore) {
					mergedSequence.append(readOneCode);
					mergedQuality.append(readOneQuality.charAt(readOneQualityIndex));
					if (!readOneCode.matches(readTwoCode)) {
						numberOfBasesCorrected++;
					}
				} else if (readOneSequencePhredScore < readTwoSequencePhredScore) {
					mergedSequence.append(readTwoCode);
					mergedQuality.append(readTwoQuality.charAt(readOneQualityIndex));
					if (!readOneCode.matches(readTwoCode)) {
						numberOfBasesCorrected++;
					}
				} else if (readOneSequencePhredScore == readTwoSequencePhredScore) {
					mergedSequence.append(readOneCode);
					mergedQuality.append(readOneQuality.charAt(readOneQualityIndex));
					if (!readOneCode.matches(readTwoCode)) {
						mismatchingBaseWithSameQualityScoreOccurred = true;
					}
				}

				readOneQualityIndex++;
				readTwoQualityIndex++;
			} else if (isReadOneAGap && isReadTwoAGap) {
				throw new AssertionError();
			}
		}

		FastqRecord mergedRecord = new FastqRecord(recordOne.getReadHeader(), mergedSequence.toString(), recordOne.getBaseQualityHeader(), mergedQuality.toString());
		return new MergedReadsResults(mergedRecord, numberOfBasesCorrected, amountOfOverlap, mismatchingBaseWithSameQualityScoreOccurred);
	}

	public static ReadMergerDetails mergeReads(PausableFixedThreadPoolExecutor executor, File fastqOneFile, File fastqTwoFile, File outputFastqFile, int maxNumberOfReplacementsPerReadPair) {
		return mergeReads(executor, fastqOneFile, fastqTwoFile, outputFastqFile, 0, null, maxNumberOfReplacementsPerReadPair, null);
	}

	public static String getUnmergedFastqName(File fastqFile) {
		String fastqName = "UNMERGED_" + fastqFile.getName();
		if (fastqName.endsWith(".gz")) {
			fastqName = fastqName.substring(0, fastqName.length() - 3);
		}
		return fastqName;
	}

	public static ReadMergerDetails mergeReads(PausableFixedThreadPoolExecutor executor, File fastqOneFile, File fastqTwoFile, File outputFastqFile, int startingRead, Integer numberOfReadsToProcess,
			int maxNumberOfConflictsPerReadPair, IProgressListener progressListener) {
		int numberOfLinesInEachFile = 0;
		try {
			numberOfLinesInEachFile = FileUtil.countNumberOfLinesInFile(fastqOneFile);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		return mergeReads(executor, fastqOneFile, fastqTwoFile, numberOfLinesInEachFile, outputFastqFile, startingRead, numberOfReadsToProcess, maxNumberOfConflictsPerReadPair, progressListener);
	}

	public static ReadMergerDetails mergeReads(PausableFixedThreadPoolExecutor executor, File fastqOneFile, File fastqTwoFile, int numberOfLinesInEachFile, File outputFastqFile, int startingRead,
			Integer numberOfReadsToProcess, int maxNumberOfConflictsPerReadPair, IProgressListener progressListener) {

		if (progressListener != null) {
			progressListener.updateProgress(0, "Starting to merge reads.");
		}

		int numberOfReadsInFile = numberOfLinesInEachFile / FASTQ_LINES_PER_READ;

		if (progressListener != null) {
			progressListener.updateProgress(0, "Found " + DF.format(numberOfReadsInFile) + " Fastq reads in " + DF.format(numberOfLinesInEachFile) + " text lines.");
		}

		if (numberOfReadsToProcess == null) {
			numberOfReadsToProcess = numberOfReadsInFile;
		} else {
			numberOfReadsToProcess = Math.min(numberOfReadsInFile, numberOfReadsToProcess);
		}

		Map<Integer, FastqRecord> mergedFastqWriterQueue = new HashMap<Integer, FastqRecord>();
		Map<Integer, FastqRecord> fastqOneWriterQueue = new HashMap<Integer, FastqRecord>();
		Map<Integer, FastqRecord> fastqTwoWriterQueue = new HashMap<Integer, FastqRecord>();

		File unmergedFastqOneFile = new File(outputFastqFile.getParentFile(), getUnmergedFastqName(fastqOneFile));
		File unmergedFastqTwoFile = new File(outputFastqFile.getParentFile(), getUnmergedFastqName(fastqTwoFile));

		AtomicInteger processedReadsCount = new AtomicInteger(0);
		AtomicInteger mergedReadCount = new AtomicInteger(0);

		List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();

		List<Throwable> exceptions = new ArrayList<Throwable>();

		try (FastqWriter mergedFastqWriter = new FastqWriter(outputFastqFile)) {
			try (FastqWriter unmergedFastqOneWriter = new FastqWriter(unmergedFastqOneFile)) {
				try (FastqWriter unmergedFastqTwoWriter = new FastqWriter(unmergedFastqTwoFile)) {

					AtomicInteger readNumber = new AtomicInteger(-1);
					AtomicInteger currentWriteReadNumber = new AtomicInteger(startingRead);
					try (FastqReader fastQOneReader = new FastqReader(fastqOneFile)) {
						try (FastqReader fastQTwoReader = new FastqReader(fastqTwoFile)) {
							int poolSize = executor.getMaximumPoolSize();
							for (int i = 0; i < poolSize; i++) {
								tasks.add(new MergeRecordsHelper(fastQOneReader, fastQTwoReader, startingRead, numberOfReadsToProcess, processedReadsCount, readNumber, mergedReadCount,
										maxNumberOfConflictsPerReadPair, mergedFastqWriter, unmergedFastqOneWriter, unmergedFastqTwoWriter, progressListener, mergedFastqWriterQueue,
										fastqOneWriterQueue, fastqTwoWriterQueue, currentWriteReadNumber));
							}

							IExceptionListener exceptionListener = new IExceptionListener() {
								@Override
								public void exceptionOccurred(Runnable runnable, Throwable throwable) {
									exceptions.add(throwable);
								}
							};
							executor.addExceptionListener(exceptionListener);

							try {
								List<Future<Object>> futures = executor.invokeAll(tasks);
								for (Future<Object> future : futures) {
									try {
										future.get();
									} catch (Exception e) {
										exceptions.add(e);
									}
								}
							} catch (InterruptedException e) {
								exceptions.add(e);
							}

							executor.removeExceptionListener(exceptionListener);

						}
					}

				}
			}
		}

		if (progressListener != null) {
			progressListener.updateProgress(100, "Merged " + DF.format(numberOfReadsInFile) + " Fastq reads.");
		}
		System.out.println("exceptions size:" + exceptions.size());
		for (Throwable throwable : exceptions) {
			System.out.println(throwable.getMessage());
		}

		return new ReadMergerDetails(mergedReadCount.get(), processedReadsCount.get(), exceptions.size() == 0, exceptions);
	}

	private static class MergeRecordsHelper implements Callable<Object> {

		private final FastqReader fastQOneReader;
		private final FastqReader fastQTwoReader;
		private final int startingRead;
		private final Integer numberOfReadsToProcess;

		private final AtomicInteger processedReadsCount;
		private final AtomicInteger readNumber;
		private final AtomicInteger mergedReadCount;
		private final AtomicInteger lastPercentComplete;

		private final int maxNumberOfConflictsPerReadPair;
		private final FastqWriter mergedFastqWriter;
		private final FastqWriter unmergedFastqOneWriter;
		private final FastqWriter unmergedFastqTwoWriter;
		private final IProgressListener progressListener;

		private final Map<Integer, FastqRecord> mergedFastqWriterQueue;

		private final Map<Integer, FastqRecord> fastqOneWriterQueue;
		private final Map<Integer, FastqRecord> fastqTwoWriterQueue;

		private final AtomicInteger currentWriteReadNumber;

		private static AtomicInteger activeHelpers = new AtomicInteger(0);

		public MergeRecordsHelper(FastqReader fastQOneReader, FastqReader fastQTwoReader, int startingRead, Integer numberOfReadsToProcess, AtomicInteger processedReadsCount,
				AtomicInteger readNumber, AtomicInteger mergedReadCount, int maxNumberOfConflictsPerReadPair, FastqWriter mergedFastqWriter, FastqWriter unmergedFastqOneWriter,
				FastqWriter unmergedFastqTwoWriter, IProgressListener progressListener, Map<Integer, FastqRecord> mergedFastqWriterQueue, Map<Integer, FastqRecord> fastqOneWriterQueue,
				Map<Integer, FastqRecord> fastqTwoWriterQueue, AtomicInteger currentWriteReadNumber) {
			super();
			this.fastQOneReader = fastQOneReader;
			this.fastQTwoReader = fastQTwoReader;
			this.startingRead = startingRead;
			this.numberOfReadsToProcess = numberOfReadsToProcess;
			this.processedReadsCount = processedReadsCount;
			this.readNumber = readNumber;
			this.mergedReadCount = mergedReadCount;
			this.maxNumberOfConflictsPerReadPair = maxNumberOfConflictsPerReadPair;
			this.mergedFastqWriter = mergedFastqWriter;
			this.unmergedFastqOneWriter = unmergedFastqOneWriter;
			this.unmergedFastqTwoWriter = unmergedFastqTwoWriter;
			this.progressListener = progressListener;
			this.lastPercentComplete = new AtomicInteger(0);
			activeHelpers.incrementAndGet();
			this.mergedFastqWriterQueue = mergedFastqWriterQueue;
			this.fastqOneWriterQueue = fastqOneWriterQueue;
			this.fastqTwoWriterQueue = fastqTwoWriterQueue;
			this.currentWriteReadNumber = currentWriteReadNumber;
		}

		@Override
		public Object call() {
			boolean allRecordsRead = false;
			while (!allRecordsRead && (numberOfReadsToProcess == null || (processedReadsCount.get() < numberOfReadsToProcess))) {
				FastqRecord recordOne = null;
				FastqRecord recordTwo = null;
				Integer currentReadNumber = null;
				synchronized (fastQOneReader) {
					if (fastQOneReader.hasNext() && fastQTwoReader.hasNext()) {
						recordOne = fastQOneReader.next();
						recordTwo = fastQTwoReader.next();
						currentReadNumber = readNumber.incrementAndGet();
					}
				}

				if (recordOne != null && recordTwo != null) {
					if (currentReadNumber >= startingRead) {

						if (recordOne.getBaseQualityString() == null) {
							throw new IllegalStateException("Incomplete Fastq Record found in record: " + currentReadNumber + " at line:" + (currentReadNumber * FASTQ_LINES_PER_READ)
									+ " in the fastq read one file.");
						}
						if (recordTwo.getBaseQualityString() == null) {
							throw new IllegalStateException("Incomplete Fastq Record found in record: " + currentReadNumber + " at line:" + (currentReadNumber * FASTQ_LINES_PER_READ)
									+ " in the fastq read two file.");
						}
						// this is where most of the time is spent
						MergedReadsResults results = mergeReads(recordOne, recordTwo);
						boolean mergedReadWritten = false;

						FastqRecord mergedRead = results.mergedRecord;
						if (mergedRead != null) {
							if ((results.numberOfBasesCorrected <= maxNumberOfConflictsPerReadPair) && (!results.mismatchingBaseWithSameQualityScoreOccurred)) {
								processedReadsCount.incrementAndGet();
								mergedReadCount.incrementAndGet();
								writeMergedRead(currentReadNumber, mergedRead);
								mergedReadWritten = true;
							}
						}

						if (!mergedReadWritten) {
							synchronized (unmergedFastqOneWriter) {
								processedReadsCount.incrementAndGet();
								writeUnmergedRead(currentReadNumber, recordOne, recordTwo);
							}
						}

						if (progressListener != null) {
							double numberOfProcessedReads = processedReadsCount.get();
							int percentComplete = (int) Math.floor((numberOfProcessedReads / (double) numberOfReadsToProcess) * 100);
							if (percentComplete > lastPercentComplete.getAndSet(percentComplete)) {
								String message = "" + DF.format(processedReadsCount) + " of " + DF.format(numberOfReadsToProcess) + " Fastq Read Pairs Processed.";
								double ratio = ((double) mergedReadCount.get() / (double) processedReadsCount.get()) * 100.0;
								message += StringUtil.NEWLINE + DF2.format(ratio) + "% of processed reads succesfully merged.";
								progressListener.updateProgress(percentComplete, message);
							}
						}

					}
				} else {
					allRecordsRead = true;
				}

				if (Thread.currentThread().isInterrupted()) {
					throw new RuntimeException("Read Merging was stopped.");
				}
			}

			if (activeHelpers.decrementAndGet() == 0) {
				if (mergedFastqWriterQueue.size() > 0 || fastqOneWriterQueue.size() > 0 || fastqTwoWriterQueue.size() > 0) {
					throw new IllegalStateException("Queue were not emptied: mergedQueue_size[" + mergedFastqWriterQueue.size() + "] fastq1Queue_size[" + fastqOneWriterQueue.size()
							+ "] fastq2Queue_size[" + fastqTwoWriterQueue.size() + "].  Waiting for read[" + currentWriteReadNumber.get() + "].");
				}
			}
			return null;
		}

		private void writeMergedRead(int readNumber, FastqRecord mergedRead) {
			synchronized (currentWriteReadNumber) {
				mergedFastqWriterQueue.put(readNumber, mergedRead);
				processMergedQueue();
				processUnmegedQueue();
			}
		}

		private void processMergedQueue() {
			synchronized (currentWriteReadNumber) {
				while (mergedFastqWriterQueue.containsKey(currentWriteReadNumber.get())) {
					int readNumberToWrite = currentWriteReadNumber.getAndIncrement();
					FastqRecord record = mergedFastqWriterQueue.remove(readNumberToWrite);
					if (record != null) {
						mergedFastqWriter.write(record);
					} else {
						System.out.println("Could not find record for " + readNumberToWrite + ".");
					}
				}
			}
		}

		private void writeUnmergedRead(int readNumber, FastqRecord recordOne, FastqRecord recordTwo) {
			synchronized (currentWriteReadNumber) {
				fastqOneWriterQueue.put(readNumber, recordOne);
				fastqTwoWriterQueue.put(readNumber, recordTwo);
				processUnmegedQueue();
				processMergedQueue();
			}
		}

		private void processUnmegedQueue() {
			synchronized (currentWriteReadNumber) {
				while (fastqOneWriterQueue.containsKey(currentWriteReadNumber.get()) && fastqTwoWriterQueue.containsKey(currentWriteReadNumber.get())) {
					int readNumberToWrite = currentWriteReadNumber.getAndIncrement();
					FastqRecord recordToWriteOne = fastqOneWriterQueue.remove(readNumberToWrite);
					FastqRecord recordToWriteTwo = fastqTwoWriterQueue.remove(readNumberToWrite);
					if ((recordToWriteOne != null) && (recordToWriteTwo != null)) {
						unmergedFastqOneWriter.write(recordToWriteOne);
						unmergedFastqTwoWriter.write(recordToWriteTwo);
					} else {
						System.out.println("Could not find record for " + readNumberToWrite + ".");
					}
				}
			}
		}
	}

}
