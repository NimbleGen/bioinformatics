package com.roche.heatseq.merged_read_process;

import htsjdk.samtools.fastq.FastqRecord;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.roche.heatseq.utils.FastqReader;
import com.roche.heatseq.utils.FastqWriter;
import com.roche.sequencing.bioinformatics.common.alignment.AlignmentPair;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
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
	private final static int NUMBER_OF_THREADS = 20;
	private final static int FASTQ_LINES_PER_READ = 4;

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

	private static MergedReadsResults mergeReads(FastqRecord recordOne, FastqRecord recordTwo) {
		ISequence readOne = new IupacNucleotideCodeSequence(recordOne.getReadString());
		ISequence readTwo = new IupacNucleotideCodeSequence(recordTwo.getReadString());
		NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(readOne, readTwo.getReverseCompliment());

		AlignmentPair alignmentPair = alignment.getAlignmentPair();
		ISequence reference = alignmentPair.getReferenceAlignment();
		ISequence query = alignmentPair.getQueryAlignment();

		StringBuilder mergedSequence = new StringBuilder();
		StringBuilder mergedQuality = new StringBuilder();

		String readOneQuality = recordOne.getBaseQualityString();
		String readTwoQuality = StringUtil.reverse(recordTwo.getBaseQualityString());

		int readOneQualityIndex = 0;
		int readTwoQualityIndex = 0;

		int numberOfBasesCorrected = 0;
		int amountOfOverlap = 0;

		boolean mismatchingBaseWithSameQualityScoreOccurred = false;

		for (int i = 0; i < reference.size(); i++) {
			ICode readOneCode = reference.getCodeAt(i);
			ICode readTwoCode = query.getCodeAt(i);

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

	public static ReadMergerDetails mergeReads(File fastqOneFile, File fastqTwoFile, File outputFastqFile, int maxNumberOfReplacementsPerReadPair) {
		return mergeReads(fastqOneFile, fastqTwoFile, outputFastqFile, 0, null, maxNumberOfReplacementsPerReadPair, null);
	}

	public static ReadMergerDetails mergeReads(File fastqOneFile, File fastqTwoFile, File outputFastqFile, int startingRead, Integer numberOfReadsToProcess, int maxNumberOfReplacementsPerReadPair,
			IProgressListener progressListener) {

		if (progressListener != null) {
			progressListener.updateProgress(0, "Starting to merged reads.");
		}

		int numberOfLinesInFile = 0;
		try {
			numberOfLinesInFile = FileUtil.countNumberOfLinesInFile(fastqOneFile);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		int numberOfReadsInFile = numberOfLinesInFile / FASTQ_LINES_PER_READ;

		if (progressListener != null) {
			progressListener.updateProgress(0, "Found " + DF.format(numberOfReadsInFile) + " Fastq reads in " + DF.format(numberOfLinesInFile) + " text lines.");
		}

		if (numberOfReadsToProcess == null) {
			numberOfReadsToProcess = numberOfReadsInFile;
		} else {
			numberOfReadsToProcess = Math.min(numberOfReadsInFile, numberOfReadsToProcess);
		}

		PausableFixedThreadPoolExecutor executor = new PausableFixedThreadPoolExecutor(NUMBER_OF_THREADS, "DEDUP_");
		executor.addExceptionListener(new IExceptionListener() {
			@Override
			public void exceptionOccurred(Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		});

		File unmergedFastqOneFile = new File(outputFastqFile.getParentFile(), "UNMERGED_" + fastqOneFile.getName());
		File unmergedFastqTwoFile = new File(outputFastqFile.getParentFile(), "UNMERGED_" + fastqTwoFile.getName());

		AtomicInteger processedReadsCount = new AtomicInteger(0);

		try (FastqWriter mergedFastqWriter = new FastqWriter(outputFastqFile)) {
			try (FastqWriter unmergedFastqOneWriter = new FastqWriter(unmergedFastqOneFile)) {
				try (FastqWriter unmergedFastqTwoWriter = new FastqWriter(unmergedFastqTwoFile)) {

					AtomicInteger readNumber = new AtomicInteger(0);
					try (FastqReader fastQOneReader = new FastqReader(fastqOneFile)) {
						try (FastqReader fastQTwoReader = new FastqReader(fastqTwoFile)) {
							for (int i = 0; i < NUMBER_OF_THREADS; i++) {
								executor.submit(new MergeRecordsHelper(fastQOneReader, fastQTwoReader, startingRead, numberOfReadsToProcess, processedReadsCount, readNumber, executor,
										maxNumberOfReplacementsPerReadPair, mergedFastqWriter, unmergedFastqOneWriter, unmergedFastqTwoWriter, progressListener));
							}

							try {
								executor.awaitTermination(1, TimeUnit.DAYS);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

						}
					}

				}
			}
		}

		if (progressListener != null) {
			progressListener.updateProgress(100, "Merged " + DF.format(numberOfReadsInFile) + " Fastq reads.");
		}

		return new ReadMergerDetails(processedReadsCount.get(), numberOfReadsToProcess);
	}

	private static class MergeRecordsHelper implements Runnable {

		private final FastqReader fastQOneReader;
		private final FastqReader fastQTwoReader;
		private final int startingRead;
		private final Integer numberOfReadsToProcess;

		private final AtomicInteger processedReadsCount;
		private final AtomicInteger readNumber;
		private final AtomicInteger lastPercentComplete;
		private final PausableFixedThreadPoolExecutor executor;

		private final int maxNumberOfReplacementsPerReadPair;
		private final FastqWriter mergedFastqWriter;
		private final FastqWriter unmergedFastqOneWriter;
		private final FastqWriter unmergedFastqTwoWriter;
		private final IProgressListener progressListener;

		private static AtomicInteger activeHelpers = new AtomicInteger(0);

		public MergeRecordsHelper(FastqReader fastQOneReader, FastqReader fastQTwoReader, int startingRead, Integer numberOfReadsToProcess, AtomicInteger processedReadsCount,
				AtomicInteger readNumber, PausableFixedThreadPoolExecutor executor, int maxNumberOfReplacementsPerReadPair, FastqWriter mergedFastqWriter, FastqWriter unmergedFastqOneWriter,
				FastqWriter unmergedFastqTwoWriter, IProgressListener progressListener) {
			super();
			this.fastQOneReader = fastQOneReader;
			this.fastQTwoReader = fastQTwoReader;
			this.startingRead = startingRead;
			this.numberOfReadsToProcess = numberOfReadsToProcess;
			this.processedReadsCount = processedReadsCount;
			this.readNumber = readNumber;
			this.executor = executor;
			this.maxNumberOfReplacementsPerReadPair = maxNumberOfReplacementsPerReadPair;
			this.mergedFastqWriter = mergedFastqWriter;
			this.unmergedFastqOneWriter = unmergedFastqOneWriter;
			this.unmergedFastqTwoWriter = unmergedFastqTwoWriter;
			this.progressListener = progressListener;
			this.lastPercentComplete = new AtomicInteger(0);
			activeHelpers.incrementAndGet();
		}

		@Override
		public void run() {
			boolean allRecordsRead = false;
			while (!allRecordsRead && (numberOfReadsToProcess == null || processedReadsCount.get() < numberOfReadsToProcess)) {
				FastqRecord recordOne = null;
				FastqRecord recordTwo = null;
				synchronized (fastQOneReader) {
					if (fastQOneReader.hasNext() && fastQTwoReader.hasNext()) {
						recordOne = fastQOneReader.next();
						recordTwo = fastQTwoReader.next();
					}
				}

				if (recordOne != null && recordTwo != null) {
					if (readNumber.incrementAndGet() >= startingRead) {
						// this is where most of the time is spent
						MergedReadsResults results = mergeReads(recordOne, recordTwo);
						boolean mergedReadWritten = false;

						FastqRecord mergedRead = results.mergedRecord;
						if (mergedRead != null) {
							if (results.numberOfBasesCorrected <= maxNumberOfReplacementsPerReadPair && !results.mismatchingBaseWithSameQualityScoreOccurred) {
								mergedFastqWriter.write(mergedRead);
								mergedReadWritten = true;
							}
						}

						if (!mergedReadWritten) {
							synchronized (unmergedFastqOneWriter) {
								unmergedFastqOneWriter.write(recordOne);
								unmergedFastqTwoWriter.write(recordTwo);
							}
						}

						double numberOfProcessedReads = processedReadsCount.incrementAndGet();
						if (progressListener != null) {
							int percentComplete = (int) Math.floor((numberOfProcessedReads / (double) numberOfReadsToProcess) * 100);
							if (percentComplete > lastPercentComplete.getAndSet(percentComplete)) {
								progressListener.updateProgress(percentComplete, "" + DF.format(readNumber.get()) + " of " + DF.format(numberOfReadsToProcess) + " Fastq Read Pairs merged.");
							}
						}

					}
				} else {
					allRecordsRead = true;
				}
			}

			if (activeHelpers.decrementAndGet() == 0) {
				System.out.println("shutdown.");
				executor.shutdown();
			}

		}
	}

}
