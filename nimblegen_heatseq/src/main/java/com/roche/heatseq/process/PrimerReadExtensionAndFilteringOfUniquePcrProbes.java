/**
 *   Copyright 2013 Roche NimbleGen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.roche.heatseq.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import net.sf.picard.fastq.FastqRecord;
import net.sf.picard.fastq.FastqWriter;
import net.sf.picard.fastq.FastqWriterFactory;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.ApplicationSettings;
import com.roche.heatseq.objects.IReadPair;
import com.roche.heatseq.objects.IlluminaFastQHeader;
import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.objects.ProbesBySequenceName;
import com.roche.heatseq.objects.SAMRecordPair;
import com.roche.heatseq.objects.UidReductionResultsForAProbe;
import com.roche.heatseq.qualityreport.DetailsReport;
import com.roche.heatseq.qualityreport.ReportManager;
import com.roche.mapping.SAMRecordUtil;
import com.roche.mapping.SAMRecordUtil.SamReadCount;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;

/*
 * Class to get reads for each probe from a merged BAM file, determine which read to use for each UID, extend the reads to the target primers, and output the reduced and extended reads to a new BAM file
 * 
 * Assumptions:
 * Fastq1 always contains UID
 * Fastq1 always contains extension primer
 * Fastq1 always has same strandedness as probe
 * Fastq2 always contains ligation primer
 * Fastq2 always has opposite strandedness as probe
 * 
 */
class PrimerReadExtensionAndFilteringOfUniquePcrProbes {

	private static Logger logger = LoggerFactory.getLogger(PrimerReadExtensionAndFilteringOfUniquePcrProbes.class);

	private static volatile Semaphore primerReadExtensionAndFilteringOfUniquePcrProbesSemaphore = null;

	/**
	 * We never create instances of this class, we only expose static methods
	 */
	private PrimerReadExtensionAndFilteringOfUniquePcrProbes() {
		throw new AssertionError();
	}

	/**
	 * Get reads for each probe from a merged BAM file, determine which read to use for each UID, extend the reads to the target primers, and output the reduced and extended reads to a new BAM file
	 * 
	 * @param applicationSettings
	 *            The context our application is running under.
	 */
	static void filterBamEntriesByUidAndExtendReadsToPrimers(ApplicationSettings applicationSettings) {

		// Initialize the thread semaphore if it hasn't already been initialized

		if (primerReadExtensionAndFilteringOfUniquePcrProbesSemaphore == null) {
			primerReadExtensionAndFilteringOfUniquePcrProbesSemaphore = new Semaphore(applicationSettings.getNumProcessors());
		}

		long start = System.currentTimeMillis();

		// Parse the input probe file
		ProbesBySequenceName probeInfo = null;
		try {
			probeInfo = ProbeFileUtil.parseProbeInfoFile(applicationSettings.getProbeFile());
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
		if (probeInfo == null) {
			throw new IllegalStateException("Unable to parse probe info file[" + applicationSettings.getProbeFile() + "].");
		}

		SAMFileHeader samHeader = null;
		try (SAMFileReader samReader = new SAMFileReader(applicationSettings.getBamFile(), applicationSettings.getBamFileIndex())) {
			samHeader = BamFileUtil.getHeader(false, samReader.getFileHeader(), probeInfo, applicationSettings.getCommandLineSignature(), applicationSettings.getProgramName(),
					applicationSettings.getProgramVersion());
		}

		// Set up the reports files
		ReportManager reportManager = new ReportManager(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix(), applicationSettings.getUidLength(), samHeader,
				applicationSettings.isShouldOutputQualityReports());

		// Actually do the work
		filterBamEntriesByUidAndExtendReadsToPrimers(applicationSettings, probeInfo, reportManager);

		long stop = System.currentTimeMillis();

		// Report on performance
		logger.debug("Total time: " + DateUtil.convertMillisecondsToHHMMSS(stop - start));
	}

	/**
	 * Get reads for each probe from a merged BAM file, determine which read to use for each UID, extend the reads to the target primers, and output the reduced and extended reads to a new BAM file.
	 * 
	 * @param applicationSettings
	 *            The context the application is running under
	 * @param probeInfo
	 *            All the probes in the input probe file, by sequence
	 * @param detailReportWriter
	 *            Writer for reporting detailed processing information
	 * @param extensionErrorsWriter
	 *            Writer for reporting errors detected when extending the reads to the primers
	 * @param probeUidQualityWriter
	 *            Writer for reporting quality per UID
	 */
	private static void filterBamEntriesByUidAndExtendReadsToPrimers(ApplicationSettings applicationSettings, ProbesBySequenceName probeInfo, ReportManager reportManager) {
		long start = System.currentTimeMillis();

		Set<String> sequenceNames = probeInfo.getSequenceNames();

		TallyMap<String> readNamesToDistinctProbeAssignmentCount = new TallyMap<String>();
		Set<ISequence> distinctUids = Collections.newSetFromMap(new ConcurrentHashMap<ISequence, Boolean>());
		List<ISequence> uids = new ArrayList<ISequence>();

		SAMFileWriter samWriter = null;

		try (SAMFileReader samReader = new SAMFileReader(applicationSettings.getBamFile(), applicationSettings.getBamFileIndex())) {

			if (reportManager.isReporting()) {
				SamReadCount readCount = SAMRecordUtil.countReads(samReader);
				reportManager.getSummaryReport().setUnmappedReads(readCount.getTotalUnmappedReads());
				reportManager.getSummaryReport().setMappedReads(readCount.getTotalMappedReads());
			}

			String outputUnsortedBamFileName = FileUtil.getFileNameWithoutExtension(applicationSettings.getOriginalBamFileName()) + "_UNSORTED_REDUCED."
					+ FileUtil.getFileExtension(applicationSettings.getBamFile());
			File outputUnsortedBamFile = new File(applicationSettings.getOutputDirectory(), outputUnsortedBamFileName);

			String outputSortedBamFileName = applicationSettings.getOutputBamFileName();
			File outputSortedBamFile = new File(applicationSettings.getOutputDirectory(), outputSortedBamFileName);

			try {
				outputUnsortedBamFile.createNewFile();
				outputSortedBamFile.createNewFile();
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}

			// Make an unsorted BAM file writer with the fastest level of compression
			samWriter = new SAMFileWriterFactory().makeBAMWriter(
					BamFileUtil.getHeader(true, samReader.getFileHeader(), probeInfo, applicationSettings.getCommandLineSignature(), applicationSettings.getProgramName(),
							applicationSettings.getProgramVersion()), false, outputUnsortedBamFile, 0);

			FastqWriter fastqOneWriter = null;
			FastqWriter fastqTwoWriter = null;
			if (applicationSettings.isShouldOutputFastq()) {
				File fastqOne = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOriginalBamFileName() + "_one.fastq");
				File fastqTwo = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOriginalBamFileName() + "_two.fastq");
				logger.debug("Output fastq files will be created at fastqone[" + fastqOne.getAbsolutePath() + "] and fastqtwo[" + fastqTwo.getAbsolutePath() + "].");
				final FastqWriterFactory fastqWriterFactory = new FastqWriterFactory();
				fastqOneWriter = fastqWriterFactory.newWriter(fastqOne);
				fastqTwoWriter = fastqWriterFactory.newWriter(fastqTwo);
			}

			List<SAMSequenceRecord> referenceSequencesInBam = samReader.getFileHeader().getSequenceDictionary().getSequences();
			List<String> referenceSequenceNamesInBam = new ArrayList<String>(referenceSequencesInBam.size());
			List<Integer> referenceSequenceLengthsInBam = new ArrayList<Integer>(referenceSequencesInBam.size());
			for (SAMSequenceRecord referenceSequence : referenceSequencesInBam) {
				referenceSequenceNamesInBam.add(referenceSequence.getSequenceName());
				referenceSequenceLengthsInBam.add(referenceSequence.getSequenceLength());
			}

			// Make an executor to handle processing the data for each probe in parallel
			ExecutorService executor = Executors.newFixedThreadPool(applicationSettings.getNumProcessors());
			int totalProbes = 0;

			for (String sequenceName : sequenceNames) {

				if (!referenceSequenceNamesInBam.contains(sequenceName)) {
					throw new IllegalStateException("Sequence[" + sequenceName
							+ "] from probe file is not present as a reference sequence in the bam file.  Please make sure your probe sequence names match bam file reference sequence names.");
				}

				List<Probe> probes = probeInfo.getProbesBySequenceName(sequenceName);

				int totalProbesInSequence = probes.size();
				totalProbes += totalProbesInSequence;
				logger.debug("Beginning processing " + sequenceName + " with " + totalProbesInSequence + " PROBES ");

				for (Probe probe : probes) {
					int referenceSequenceIndex = referenceSequenceNamesInBam.indexOf(sequenceName);
					int referenceSequenceLength = referenceSequenceLengthsInBam.get(referenceSequenceIndex);
					if (referenceSequenceLength < probe.getStop()) {
						throw new IllegalStateException("Probe Sequence[" + sequenceName + "] start[" + probe.getStart() + "] stop[" + probe.getStop() + "] found in the probe file["
								+ applicationSettings.getProbeFile().getAbsolutePath() + "] is outside of the length[" + referenceSequenceLength + "] of reference sequence[" + sequenceName
								+ "] found in the bam file.");
					}

					// Try getting the reads for this probe here before passing them to the worker
					Map<String, SAMRecordPair> readNameToRecordsMap = new HashMap<String, SAMRecordPair>();

					SAMRecordIterator samRecordIter = null;
					if (applicationSettings.isNotTrimmedWithinTheCaptureTargetSequence()) {
						samRecordIter = samReader.queryContained(sequenceName, probe.getStart(), probe.getStop());
					} else {
						samRecordIter = samReader.queryContained(sequenceName, probe.getCaptureTargetStart(), probe.getCaptureTargetStop());
					}

					while (samRecordIter.hasNext()) {
						SAMRecord record = samRecordIter.next();

						// ASSUMPTIONS:
						// Fastq1/firstOfPair always has same strandedness as probe
						// Fastq2/secondOfPair always has opposite strandedness as probe
						boolean recordStrandMatchesProbeStrand = (record.getReadNegativeStrandFlag() == (probe.getProbeStrand() == Strand.REVERSE));
						boolean isFastq1 = record.getFirstOfPairFlag();
						boolean isFastq2 = record.getSecondOfPairFlag();

						boolean readOrientationIsSuitableForProbe = (isFastq1 && recordStrandMatchesProbeStrand) || (isFastq2 && !recordStrandMatchesProbeStrand);
						if (readOrientationIsSuitableForProbe) {

							String readName = record.getReadName();
							readName = readName.toLowerCase();

							String uniqueReadName = IlluminaFastQHeader.getUniqueIdForReadHeader(readName);
							SAMRecordPair pair = readNameToRecordsMap.get(uniqueReadName);

							if (pair == null) {
								pair = new SAMRecordPair();
							}

							if (!record.getMateUnmappedFlag() && !record.getReadUnmappedFlag()) {
								if (record.getFirstOfPairFlag()) {
									pair.setFirstOfPairRecord(record);
								} else if (record.getSecondOfPairFlag()) {
									pair.setSecondOfPairRecord(record);
								} else {
									throw new AssertionError();
								}

								readNameToRecordsMap.put(uniqueReadName, pair);
							}
						}
					}
					samRecordIter.close();

					// remove any records that don't have both pairs
					Map<String, SAMRecordPair> readNameToCompleteRecordsMap = new HashMap<String, SAMRecordPair>();
					for (Entry<String, SAMRecordPair> entry : readNameToRecordsMap.entrySet()) {
						SAMRecordPair pair = entry.getValue();
						if (pair.getFirstOfPairRecord() != null && pair.getSecondOfPairRecord() != null) {
							readNameToCompleteRecordsMap.put(entry.getKey(), pair);
							readNamesToDistinctProbeAssignmentCount.add(entry.getKey());
						}
					}

					Runnable worker = new PrimerReadExtensionAndFilteringOfUniquePcrProbesTask(probe, applicationSettings, samWriter, reportManager, fastqOneWriter, fastqTwoWriter,
							readNameToCompleteRecordsMap, applicationSettings.getAlignmentScorer(), distinctUids, uids);

					try {
						// Don't execute more threads than we have processors
						primerReadExtensionAndFilteringOfUniquePcrProbesSemaphore.acquire();
					} catch (InterruptedException e) {
						logger.warn(e.getMessage(), e);
					}
					executor.execute(worker);
				}
			}

			// Wait until all our threads are done processing.
			executor.shutdown();
			try {
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				throw new RuntimeException(e.getMessage(), e);
			}

			if (fastqOneWriter != null) {
				fastqOneWriter.close();
			}
			if (fastqTwoWriter != null) {
				fastqTwoWriter.close();
			}
			samWriter.close();

			int totalReads = 0;
			int totalMappedReads = 0;

			SAMRecordIterator samRecordIter = samReader.iterator();
			while (samRecordIter.hasNext()) {
				SAMRecord record = samRecordIter.next();
				totalReads++;
				if (!record.getReadUnmappedFlag()) {
					totalMappedReads++;
				}
				Set<String> mappedOnTargetReadNames = readNamesToDistinctProbeAssignmentCount.getTalliesAsMap().keySet();
				String readName = record.getReadName();
				boolean readAndMateMapped = !record.getMateUnmappedFlag() && record.getReadUnmappedFlag();
				if (!readAndMateMapped) {
					reportManager.getUnMappedReadPairsWriter().addAlignment(record);
				} else if (readAndMateMapped && !mappedOnTargetReadNames.contains(readName)) {
					reportManager.getMappedOffTargetReadsWriter().addAlignment(record);
				}
			}
			samRecordIter.close();

			samReader.close();

			// Sort the output BAM file,
			BamFileUtil.sortOnCoordinates(outputUnsortedBamFile, outputSortedBamFile);
			outputUnsortedBamFile.delete();

			// Make index for BAM file
			BamFileUtil.createIndex(outputSortedBamFile);

			long end = System.currentTimeMillis();

			if (reportManager.isReporting()) {
				long processingTimeInMs = end - start;
				reportManager.completeSummaryReport(readNamesToDistinctProbeAssignmentCount, distinctUids, uids, processingTimeInMs, totalProbes, totalReads, totalMappedReads);
			}

			reportManager.close();
		}
	}

	/**
	 * Does the work of filtering by UID and extending reads to primers.
	 */
	private static class PrimerReadExtensionAndFilteringOfUniquePcrProbesTask implements Runnable {

		private final Probe probe;
		private final ApplicationSettings applicationSettings;
		private final SAMFileWriter samWriter;
		private final FastqWriter fastqOneWriter;
		private final FastqWriter fastqTwoWriter;
		private final IAlignmentScorer alignmentScorer;
		private final Set<ISequence> distinctUids;
		private final List<ISequence> uids;
		private final ReportManager reportManager;
		Map<String, SAMRecordPair> readNameToRecordsMap;

		/**
		 * All the information we need to filter by UID and extend reads to primers. We provide thread safety by synchronizing on the writers for all blocks that should write data for one probe.
		 * 
		 * @param probe
		 *            The probe we're processing information for
		 * @param applicationSettings
		 *            The context the application is running under
		 * @param samWriter
		 *            The writer we'll use to output the reduced BAM file
		 * @param extensionErrorsWriter
		 *            Writer for reporting errors detected when extending the reads to the primers
		 * @param probeUidQualityWriter
		 *            Writer for reporting quality per UID
		 * @param detailReportWriter
		 *            Writer for reporting detailed processing information
		 * @param fastqOneWriter
		 *            Writer to output reads into fastq files
		 * @param fastqTwoWriter
		 *            Writer to output reads into fastq files
		 * @param readNameToRecordsMap
		 */

		PrimerReadExtensionAndFilteringOfUniquePcrProbesTask(Probe probe, ApplicationSettings applicationSettings, SAMFileWriter samWriter, ReportManager reportManager, FastqWriter fastqOneWriter,
				FastqWriter fastqTwoWriter, Map<String, SAMRecordPair> readNameToRecordsMap, IAlignmentScorer alignmentScorer, Set<ISequence> distinctUids, List<ISequence> uids) {
			this.probe = probe;
			this.applicationSettings = applicationSettings;
			this.samWriter = samWriter;
			this.fastqOneWriter = fastqOneWriter;
			this.fastqTwoWriter = fastqTwoWriter;
			this.readNameToRecordsMap = readNameToRecordsMap;
			this.alignmentScorer = alignmentScorer;
			this.distinctUids = distinctUids;
			this.uids = uids;
			this.reportManager = reportManager;
		}

		/**
		 * Actually performs the reduction by UID and extension to the primers
		 */
		@Override
		public void run() {
			try {
				UidReductionResultsForAProbe probeReductionResults = FilterByUid.reduceProbesByUid(probe, readNameToRecordsMap, reportManager, applicationSettings.isAllowVariableLengthUids(),
						alignmentScorer, distinctUids, uids);
				DetailsReport detailsReport = reportManager.getDetailsReport();
				synchronized (detailsReport) {
					if (probeReductionResults != null) {
						detailsReport.writeEntry(probeReductionResults.getProbeProcessingStats());
					} else {
						detailsReport.writeBlankEntry(probe);
					}
				}

				List<IReadPair> readsToWrite = ExtendReadsToPrimer.extendReadsToPrimers(probe, probeReductionResults.getReadPairs(), alignmentScorer);

				writeReadsToSamFile(samWriter, readsToWrite);
				if (fastqOneWriter != null && fastqTwoWriter != null) {
					writeReadsToFastQFiles(readsToWrite, fastqOneWriter, fastqTwoWriter);
				}

			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			} finally {
				primerReadExtensionAndFilteringOfUniquePcrProbesSemaphore.release();
			}
		}

		/**
		 * Write the reduced reads to our output BAM file
		 * 
		 * @param samWriter
		 * @param readPairs
		 */
		private static void writeReadsToSamFile(SAMFileWriter samWriter, List<IReadPair> readPairs) {
			synchronized (samWriter) {
				SAMFileHeader header = samWriter.getFileHeader();
				for (IReadPair readPair : readPairs) {
					SAMRecord record = readPair.getRecord();
					SAMRecord mate = readPair.getMateRecord();

					record.setHeader(header);
					mate.setHeader(header);

					String recordReferenceName = record.getReferenceName();
					String mateReferenceName = mate.getReferenceName();

					record.setReferenceName(recordReferenceName);
					record.setMateReferenceName(mateReferenceName);

					mate.setReferenceName(mateReferenceName);
					mate.setMateReferenceName(recordReferenceName);

					samWriter.addAlignment(mate);
					samWriter.addAlignment(record);
				}
			}
		}

		/**
		 * If the user has requested it, write the reads to fastQ files
		 * 
		 * @param readPairs
		 * @param fastqOneWriter
		 * @param fastqTwoWriter
		 */
		private static void writeReadsToFastQFiles(List<IReadPair> readPairs, FastqWriter fastqOneWriter, FastqWriter fastqTwoWriter) {
			synchronized (fastqOneWriter) {
				for (IReadPair readPair : readPairs) {
					FastqRecord recordOne = getFastqRecord(readPair.getRecord());
					FastqRecord recordTwo = getFastqRecord(readPair.getMateRecord());
					fastqOneWriter.write(recordOne);
					fastqTwoWriter.write(recordTwo);
				}
			}
		}

		/**
		 * Utility to get a Fastq record from a SAMRecord
		 * 
		 * @param samRecord
		 * @return
		 */
		private static FastqRecord getFastqRecord(SAMRecord samRecord) {
			int pairNumber = 1;
			if (samRecord.getSecondOfPairFlag()) {
				pairNumber = 2;
			}
			String headerPrefix = samRecord.getReadName() + " " + pairNumber + ":N:0:1";
			String sequence = samRecord.getReadString();
			String qualityPrefix = "+";
			String quality = samRecord.getBaseQualityString();
			FastqRecord fastqRecord = new FastqRecord(headerPrefix, sequence, qualityPrefix, quality);
			return fastqRecord;
		}
	}
}
