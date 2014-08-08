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
import java.util.HashSet;
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
import com.roche.heatseq.qualityreport.ProbeDetailsReport;
import com.roche.heatseq.qualityreport.ReportManager;
import com.roche.heatseq.utils.BamFileUtil;
import com.roche.heatseq.utils.FastqReader;
import com.roche.heatseq.utils.ProbeFileUtil;
import com.roche.heatseq.utils.SAMRecordUtil;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.TabDelimitedFileWriter;

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
public class PrimerReadExtensionAndPcrDuplicateIdentification {

	private static Logger logger = LoggerFactory.getLogger(PrimerReadExtensionAndPcrDuplicateIdentification.class);

	private static volatile Semaphore primerReadExtensionAndFilteringOfUniquePcrProbesSemaphore = null;
	private final static int READ_TO_PROBE_ALIGNMENT_BUFFER = 0;

	/**
	 * We never create instances of this class, we only expose static methods
	 */
	private PrimerReadExtensionAndPcrDuplicateIdentification() {
		throw new AssertionError();
	}

	/**
	 * Get reads for each probe from a merged BAM file, determine which read to use for each UID, extend the reads to the target primers, and output the reduced and extended reads to a new BAM file
	 * 
	 * @param applicationSettings
	 *            The context our application is running under.
	 */
	public static void filterBamEntriesByUidAndExtendReadsToPrimers(ApplicationSettings applicationSettings) {

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
		ReportManager reportManager = new ReportManager(applicationSettings.getProgramName(), applicationSettings.getProgramVersion(), applicationSettings.getOutputFilePrefix(),
				applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix(), applicationSettings.getExtensionUidLength(), applicationSettings.getLigationUidLength(),
				samHeader, applicationSettings.isShouldOutputReports());

		// Actually do the work
		filterBamEntriesByUidAndExtendReadsToPrimers(applicationSettings, probeInfo, reportManager);

		long stop = System.currentTimeMillis();

		// Report on performance
		logger.debug("Total time: " + DateUtil.convertMillisecondsToHHMMSS(stop - start));
	}

	public static boolean readPairAlignsWithProbeCoordinates(Probe probe, SAMRecord record, boolean isFastq1, int extensionUidLength, int ligationUidLength) {
		boolean readLocationIsSuitableForProbe = false;
		int mappedReadLength = SAMRecordUtil.getMappedReadLengthFromAttribute(record);
		int rawReadLength = record.getReadLength();

		if (isFastq1) {
			int offset = mappedReadLength - rawReadLength + extensionUidLength;

			int differenceInLocation = Integer.MAX_VALUE;
			if (probe.getProbeStrand() == Strand.FORWARD) {
				int readStart = record.getAlignmentStart() + offset;
				int primerStop = probe.getExtensionPrimerStart();
				differenceInLocation = readStart - primerStop;
			} else {
				// there is an issue with some bam file records that getAlignmentEnd() is not set
				// so calculate it instead of using record.getAlignmentEnd()
				int recordAlignmentEnd = record.getAlignmentStart() + mappedReadLength - 1;
				int readStart = recordAlignmentEnd - offset;
				int primerStop = probe.getExtensionPrimerStop();
				differenceInLocation = primerStop - readStart;
			}
			readLocationIsSuitableForProbe = Math.abs(differenceInLocation) <= READ_TO_PROBE_ALIGNMENT_BUFFER;
		} else if (!isFastq1) {
			int offset = mappedReadLength - rawReadLength + ligationUidLength;
			int differenceInLocation = Integer.MAX_VALUE;
			if (probe.getProbeStrand() == Strand.FORWARD) {
				// there is an issue with some bam file records that getAlignmentEnd() is not set
				// so calculate it instead of using record.getAlignmentEnd()
				int recordAlignmentEnd = record.getAlignmentStart() + mappedReadLength - 1;
				int readStart = recordAlignmentEnd - offset;
				int primerStart = probe.getLigationPrimerStop();
				differenceInLocation = readStart - primerStart;
			} else {
				int readStart = record.getAlignmentStart() + offset;
				int primerStart = probe.getLigationPrimerStart();
				differenceInLocation = primerStart - readStart;
			}
			readLocationIsSuitableForProbe = Math.abs(differenceInLocation) <= READ_TO_PROBE_ALIGNMENT_BUFFER;
		} else {
			throw new AssertionError();
		}
		return readLocationIsSuitableForProbe;
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

		Map<String, Set<Probe>> readNamesToDistinctProbeAssignment = new HashMap<String, Set<Probe>>();
		Set<ISequence> distinctUids = Collections.newSetFromMap(new ConcurrentHashMap<ISequence, Boolean>());
		List<ISequence> uids = new ArrayList<ISequence>();

		SAMFileWriter samWriter = null;

		try (SAMFileReader samReader = new SAMFileReader(applicationSettings.getBamFile(), applicationSettings.getBamFileIndex())) {

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
					BamFileUtil.getHeader(false, samReader.getFileHeader(), probeInfo, applicationSettings.getCommandLineSignature(), applicationSettings.getProgramName(),
							applicationSettings.getProgramVersion()), false, outputUnsortedBamFile, 0);

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
					throw new IllegalStateException("Sequence [" + sequenceName
							+ "] from the probe file is not present as a reference sequence in the bam file.  Please make sure your probe sequence names match bam file reference sequence names.");
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
					if (applicationSettings.isNotTrimmedToWithinTheCaptureTargetSequence()) {
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

						boolean readLocationIsSuitableForProbe = true;
						// can not make assumptions about the location of the mapping if uids are variable length
						if (!applicationSettings.isAllowVariableLengthUids() && applicationSettings.isUseStrictReadToProbeMatching()) {
							readLocationIsSuitableForProbe = readPairAlignsWithProbeCoordinates(probe, record, isFastq1, applicationSettings.getExtensionUidLength(),
									applicationSettings.getLigationUidLength());
						}

						boolean readOrientationIsSuitableForProbe = (isFastq1 && recordStrandMatchesProbeStrand) || (isFastq2 && !recordStrandMatchesProbeStrand);
						if (readOrientationIsSuitableForProbe && readLocationIsSuitableForProbe) {

							String uniqueReadName = IlluminaFastQHeader.getUniqueIdForReadHeader(record.getReadName());
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
						String readName = entry.getKey();
						SAMRecordPair pair = entry.getValue();
						if (pair.getFirstOfPairRecord() != null && pair.getSecondOfPairRecord() != null) {
							readNameToCompleteRecordsMap.put(readName, pair);
							Set<Probe> probesAssignedToRead = readNamesToDistinctProbeAssignment.get(readName);
							if (probesAssignedToRead == null) {
								probesAssignedToRead = new HashSet<Probe>();
							}
							probesAssignedToRead.add(probe);
							readNamesToDistinctProbeAssignment.put(readName, probesAssignedToRead);
						}
					}

					Runnable worker = new PrimerReadExtensionAndFilteringOfUniquePcrProbesTask(probe, applicationSettings, samWriter, reportManager, readNameToCompleteRecordsMap,
							applicationSettings.getAlignmentScorer(), distinctUids, uids);

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

			samWriter.close();

			Set<String> readNamesOfReadsAssignedToMultipleProbesToExclude = new HashSet<String>();
			for (Entry<String, Set<Probe>> entry : readNamesToDistinctProbeAssignment.entrySet()) {
				String readName = entry.getKey();
				Set<Probe> assignedProbeIds = entry.getValue();
				if (assignedProbeIds.size() > 1) {
					for (Probe probe : assignedProbeIds) {
						TabDelimitedFileWriter readsMappedToMultipleProbesWriter = reportManager.getReadsMappedToMultipleProbesWriter();
						if (readsMappedToMultipleProbesWriter != null) {
							readsMappedToMultipleProbesWriter.writeLine(readName, probe.getProbeId());
							readNamesOfReadsAssignedToMultipleProbesToExclude.add(readName);
						}
					}
				}
			}

			// Sort the output BAM file,
			BamFileUtil.sortOnCoordinatesAndExcludeReads(outputUnsortedBamFile, outputSortedBamFile, readNamesOfReadsAssignedToMultipleProbesToExclude);
			outputUnsortedBamFile.delete();

			// Make index for BAM file
			BamFileUtil.createIndex(outputSortedBamFile);

			long end = System.currentTimeMillis();

			int totalReads = 0;
			int totalFullyMappedOffTargetReads = 0;
			int totalPartiallyMappedReads = 0;
			int totalFullyUnmappedReads = 0;
			int totalFullyMappedOnTargetReads = 0;

			Set<String> unmappedReadPairReadNames = new HashSet<String>();

			SAMRecordIterator samRecordIter = samReader.iterator();
			while (samRecordIter.hasNext()) {
				SAMRecord record = samRecordIter.next();
				totalReads++;

				String readName = IlluminaFastQHeader.getUniqueIdForReadHeader(record.getReadName());

				Set<Probe> assignedProbeIds = readNamesToDistinctProbeAssignment.get(readName);
				int numberOfAssignedProbes = 0;
				if (assignedProbeIds != null) {
					numberOfAssignedProbes = assignedProbeIds.size();
				}

				boolean readAndMateMapped = !record.getMateUnmappedFlag() && !record.getReadUnmappedFlag();
				boolean partiallyMapped = (!record.getMateUnmappedFlag() && record.getReadUnmappedFlag()) || (record.getMateUnmappedFlag() && !record.getReadUnmappedFlag());
				if (partiallyMapped) {
					if (reportManager.getPartiallyMappedReadPairsWriter() != null) {
						reportManager.getPartiallyMappedReadPairsWriter().addAlignment(record);
					}
					unmappedReadPairReadNames.add(record.getReadName());
					totalPartiallyMappedReads++;
				} else if (!readAndMateMapped) {
					if (reportManager.getUnMappedReadPairsWriter() != null) {
						reportManager.getUnMappedReadPairsWriter().addAlignment(record);
					}
					unmappedReadPairReadNames.add(record.getReadName());
					totalFullyUnmappedReads++;
				} else if (readAndMateMapped && (numberOfAssignedProbes == 0 || numberOfAssignedProbes > 1)) {
					if (reportManager.getMappedOffTargetReadsWriter() != null) {
						reportManager.getMappedOffTargetReadsWriter().addAlignment(record);
					}
					totalFullyMappedOffTargetReads++;
				} else {
					// the only remaining possible option is that it is mapped and on target so verify this
					boolean mappedAndOnTarget = readAndMateMapped && numberOfAssignedProbes == 1;
					assert mappedAndOnTarget;
					totalFullyMappedOnTargetReads++;
				}
			}

			if (reportManager.getFastqOneUnableToMapWriter() != null && reportManager.getFastqTwoUnableToMapWriter() != null) {

				try (FastqReader fastQOneReader = new FastqReader(applicationSettings.getFastQ1WithUidsFile())) {
					try (FastqReader fastQTwoReader = new FastqReader(applicationSettings.getFastQ2File())) {

						while (fastQOneReader.hasNext() && fastQTwoReader.hasNext()) {
							FastqRecord fastQOneRecord = fastQOneReader.next();
							FastqRecord fastQTwoRecord = fastQTwoReader.next();

							String readName = IlluminaFastQHeader.getBaseHeader(fastQOneRecord.getReadHeader());
							if (unmappedReadPairReadNames.contains(readName)) {
								// sum the quality and place in qualHeaderPrefix
								int fastQOneQualityScore = BamFileUtil.getQualityScore(fastQOneRecord.getBaseQualityString());
								int fastQTwoQualityScore = BamFileUtil.getQualityScore(fastQTwoRecord.getBaseQualityString());

								fastQOneRecord = new FastqRecord(fastQOneRecord.getReadHeader(), fastQOneRecord.getReadString(), "" + fastQOneQualityScore, fastQOneRecord.getBaseQualityString());
								fastQTwoRecord = new FastqRecord(fastQTwoRecord.getReadHeader(), fastQTwoRecord.getReadString(), "" + fastQTwoQualityScore, fastQTwoRecord.getBaseQualityString());

								reportManager.getFastqOneUnableToMapWriter().write(fastQOneRecord);
								reportManager.getFastqTwoUnableToMapWriter().write(fastQTwoRecord);
							}
						}
					}
				}
			}

			samRecordIter.close();

			long processingTimeInMs = end - start;
			reportManager.completeSummaryReport(readNamesToDistinctProbeAssignment, distinctUids, uids, processingTimeInMs, totalProbes, totalReads, totalFullyMappedOffTargetReads,
					totalPartiallyMappedReads, totalFullyUnmappedReads, totalFullyMappedOnTargetReads);

			samReader.close();
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
		private final IAlignmentScorer alignmentScorer;
		private final Set<ISequence> distinctUids;
		private final List<ISequence> uids;
		private final ReportManager reportManager;
		private final Map<String, SAMRecordPair> readNameToRecordsMap;

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

		PrimerReadExtensionAndFilteringOfUniquePcrProbesTask(Probe probe, ApplicationSettings applicationSettings, SAMFileWriter samWriter, ReportManager reportManager,
				Map<String, SAMRecordPair> readNameToRecordsMap, IAlignmentScorer alignmentScorer, Set<ISequence> distinctUids, List<ISequence> uids) {
			this.probe = probe;
			this.applicationSettings = applicationSettings;
			this.samWriter = samWriter;
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
				UidReductionResultsForAProbe probeReductionResults = FilterByUid.reduceReadsByProbeAndUid(probe, readNameToRecordsMap, reportManager, applicationSettings.isAllowVariableLengthUids(),
						applicationSettings.getExtensionUidLength(), applicationSettings.getLigationUidLength(), alignmentScorer, distinctUids, uids, applicationSettings.isMarkDuplicates(),
						applicationSettings.isKeepDuplicates(), applicationSettings.isUseStrictReadToProbeMatching());

				List<IReadPair> reducedReads = probeReductionResults.getReadPairs();
				List<IReadPair> readsToWrite = ExtendReadsToPrimer.extendReadsToPrimers(probe, reducedReads, alignmentScorer);

				int countOfUniqueReadsUnableToExtend = 0;
				for (IReadPair readPair : readsToWrite) {
					if (!readPair.isReadOneExtended() || !readPair.isReadTwoExtended()) {
						countOfUniqueReadsUnableToExtend++;
					}
					reportManager.addExtensionPrimerMismatchDetails(readPair.getReadOnePrimerMismatchDetails());
					reportManager.addLigationPrimerMismatchDetails(readPair.getReadTwoPrimerMismatchDetails());
				}
				probeReductionResults.getProbeProcessingStats().setExtensionErrors(countOfUniqueReadsUnableToExtend);

				ProbeDetailsReport detailsReport = reportManager.getDetailsReport();
				if (detailsReport != null) {
					synchronized (detailsReport) {
						detailsReport.writeEntry(probeReductionResults.getProbeProcessingStats());
					}
				}

				TabDelimitedFileWriter uidCompositionByProbeReport = reportManager.getUidCompisitionByProbeWriter();
				if (uidCompositionByProbeReport != null) {
					synchronized (uidCompositionByProbeReport) {
						if (uidCompositionByProbeReport != null) {
							uidCompositionByProbeReport.writeLine(probe.getProbeId() + StringUtil.TAB + probeReductionResults.getProbeProcessingStats().toUidCompositionByProbeString());
						}
					}
				}

				writeReadsToSamFile(samWriter, readsToWrite, applicationSettings.isMergePairs());
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
		private static void writeReadsToSamFile(SAMFileWriter samWriter, List<IReadPair> readPairs, boolean mergePairs) {
			synchronized (samWriter) {
				SAMFileHeader header = samWriter.getFileHeader();
				for (IReadPair readPair : readPairs) {
					if (mergePairs) {
						SAMRecord mergedRecord = mergePairs(readPair);
						if (mergedRecord != null) {
							samWriter.addAlignment(mergedRecord);
						}
					} else {
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
		}

		private static SAMRecord mergePairs(IReadPair readPair) {
			SAMRecord mergedRecord = new SAMRecord(readPair.getSamHeader());

			String readGroup = readPair.getReadGroup();
			mergedRecord.setAttribute(SAMRecordUtil.READ_GROUP_ATTRIBUTE_TAG, readGroup);

			mergedRecord.setReadPairedFlag(false);

			SAMRecord record = readPair.getRecord();
			SAMRecord mate = readPair.getMateRecord();

			SAMRecord upstreamRecord = null;
			SAMRecord downstreamRecord = null;

			if (record.getAlignmentStart() < mate.getAlignmentStart()) {
				upstreamRecord = record;
				downstreamRecord = mate;
			} else {
				upstreamRecord = mate;
				downstreamRecord = record;
			}

			boolean isNegativeStrand = record.getReadNegativeStrandFlag();
			ISequence captureTargetSequence = readPair.getCaptureTargetSequence();

			MergeInformation mergeInformation = mergeSequences(isNegativeStrand, new IupacNucleotideCodeSequence(upstreamRecord.getReadString()),
					new IupacNucleotideCodeSequence(downstreamRecord.getReadString()), upstreamRecord.getBaseQualityString(), downstreamRecord.getBaseQualityString(),
					upstreamRecord.getAlignmentStart(), downstreamRecord.getAlignmentStart());

			ISequence mergedSequence = mergeInformation.getMergedSequence();
			String mergedQuality = mergeInformation.getMergedQuality();

			NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(captureTargetSequence, mergedSequence);
			String cigarString = alignment.getCigarString().getStandardCigarString();

			mergedRecord.setCigarString(cigarString);
			String mismatchDetails = alignment.getMismatchDetails();
			if (mismatchDetails != null && !mismatchDetails.isEmpty()) {
				mergedRecord.setAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG, mismatchDetails);
			}

			mergedRecord.setReadString(mergedSequence.toString());
			mergedRecord.setBaseQualityString(mergedQuality);

			mergedRecord.setReadName(record.getReadName());
			mergedRecord.setAlignmentStart(record.getAlignmentStart());

			SAMRecordUtil.setSamRecordExtensionUidAttribute(mergedRecord, readPair.getExtensionUid());
			SAMRecordUtil.setSamRecordLigationUidAttribute(mergedRecord, readPair.getLigationUid());
			SAMRecordUtil.setSamRecordProbeIdAttribute(mergedRecord, readPair.getProbeId());
			mergedRecord.setReadNegativeStrandFlag(isNegativeStrand);
			mergedRecord.setReferenceName(record.getReferenceName());
			if (isNegativeStrand) {
				mergedRecord.setInferredInsertSize(-captureTargetSequence.size());
			} else {
				mergedRecord.setInferredInsertSize(captureTargetSequence.size());
			}

			return mergedRecord;
		}
	}

	static class MergeInformation {
		private final ISequence overlappingSequence;
		private final String overlappingQuality;

		public MergeInformation(ISequence overlappingSequence, String overlappingQuality) {
			super();
			this.overlappingSequence = overlappingSequence;
			this.overlappingQuality = overlappingQuality;
		}

		public ISequence getMergedSequence() {
			return overlappingSequence;
		}

		public String getMergedQuality() {
			return overlappingQuality;
		}

	}

	static MergeInformation mergeSequences(boolean isNegativeStrand, ISequence upstreamSequence, ISequence downstreamSequence, String upstreamQuality, String downstreamQuality,
			int upstreamAlignmentStart, int downstreamAlignmentStart) {

		// the following assertions are because I assume the genomic positions are 1-based and that zeros will not be encountered
		assert upstreamAlignmentStart >= 1;
		assert downstreamAlignmentStart >= 1;

		// establish end genomic positions for the upstream and downstream sequences using their lengths
		int upstreamLength = upstreamSequence.size();
		int downstreamLength = downstreamSequence.size();
		int upstreamAlignmentEnd = upstreamAlignmentStart + upstreamLength - 1;
		int downstreamAlignmentEnd = downstreamAlignmentStart + downstreamLength - 1;

		// initialize variables for three potential sections of the merged sequence, the lead, the overlap, and the trail
		int leadStartPosition = 0;
		int leadEndPosition = 0;
		int leadLength = 0;
		int overlapStartPosition = 0;
		int overlapEndPosition = 0;
		int overlapLength = 0;
		int trailStartPosition = 0;
		int trailEndPosition = 0;
		int trailLength = 0;

		// determine min and max alignment ends to assist in calculation overlap end and trail start and end
		int minAlignmentEnd = Math.min(upstreamAlignmentEnd, downstreamAlignmentEnd);
		int maxAlignmentEnd = Math.max(upstreamAlignmentEnd, downstreamAlignmentEnd);
		// determine which source (upstream or downstream) is the correct one to use to extract trailing sequence/quality from
		ISequence trailSourceSequence = null;
		String trailSourceQuality = null;
		int trailSourceAlignmentStart = 0;
		if (downstreamAlignmentEnd == maxAlignmentEnd) {
			trailSourceSequence = downstreamSequence;
			trailSourceQuality = downstreamQuality;
			trailSourceAlignmentStart = downstreamAlignmentStart;
		} else {
			trailSourceSequence = upstreamSequence;
			trailSourceQuality = upstreamQuality;
			trailSourceAlignmentStart = upstreamAlignmentStart;
		}

		// determine if an overlap has occurred and delineate lead and trail regions if present
		if (downstreamAlignmentStart <= upstreamAlignmentEnd) {
			// classification as downstream has already established a start position at or to the right of the upstream sequence
			overlapStartPosition = downstreamAlignmentStart;
			overlapEndPosition = minAlignmentEnd;
			overlapLength = overlapEndPosition - overlapStartPosition + 1;
			if (upstreamAlignmentStart < overlapStartPosition) {
				// there is a leading sequence upstream of the overlap
				leadStartPosition = upstreamAlignmentStart;
				leadEndPosition = overlapStartPosition - 1;
				leadLength = leadEndPosition - leadStartPosition + 1;
			}
			if (maxAlignmentEnd > overlapEndPosition) {
				// there is a trailing sequence downstream of the overlap
				trailStartPosition = overlapEndPosition + 1;
				trailEndPosition = maxAlignmentEnd;
				trailLength = trailEndPosition - trailStartPosition + 1;
			}
		} else {
			// without an overlap the upstream sequence becomes the lead and the downstream becomes the trail
			leadStartPosition = upstreamAlignmentStart;
			leadEndPosition = leadStartPosition + upstreamLength - 1;
			leadLength = leadEndPosition - leadStartPosition + 1;
			trailStartPosition = downstreamAlignmentStart;
			trailEndPosition = trailStartPosition + downstreamLength - 1;
			trailLength = trailEndPosition - trailStartPosition + 1;
		}

		// individually sections for the merge may be validly missing but when present should have positive starts with ends at or greater than the starts
		assert ((leadLength == 0) || (leadLength >= 1 && leadEndPosition >= leadStartPosition));
		assert ((overlapLength == 0) || (overlapLength >= 1 && overlapEndPosition >= overlapStartPosition));
		assert ((trailLength == 0) || (trailLength >= 1 && trailEndPosition >= trailStartPosition));

		// overlaps optionally have leads and or trails but in the absence of overlap there should be both a lead and a trail
		assert (overlapLength >= 1 || (leadLength >= 1 && trailLength >= 1));

		// initialize variables to record the merged sequence/quality results
		ISequence overlappingSequence = null;
		String overlappingQuality = null;
		ISequence leadSequence = null;
		String leadQuality = null;
		ISequence trailSequence = null;
		String trailQuality = null;

		if (overlapLength == 0) {
			// there is no overlap so fill the gap with N's and give these bases a poor quality score

			int gapStartPosition = leadEndPosition + 1;
			int gapEndPosition = trailStartPosition - 1;

			int fillSize = (gapEndPosition >= gapStartPosition ? (gapEndPosition - gapStartPosition + 1) : 0);

			if (fillSize > 0) {
				overlappingSequence = new IupacNucleotideCodeSequence(StringUtil.repeatString("N", fillSize));
				// ! represents the worst quality score
				overlappingQuality = StringUtil.repeatString("!", fillSize);
				// resetting overlapLength due to the artificial gap overlap creation will be used as a signal to inclusion in the final sequence and quality strings
				overlapLength = fillSize;
			}
			// sequences without overlaps but also without gaps (so that they abut each other) will not update overlapLength forcing an empty string to be used for the overlap sequence and quality
			// strings

			// in the absence of a true overlap, the lead and trail sequence and quality content can be used without modification
			leadSequence = upstreamSequence;
			leadQuality = upstreamQuality;

			trailSequence = downstreamSequence;
			trailQuality = downstreamQuality;
		} else if (overlapLength >= 1) {
			// extract the lead, overlap, and trail sequence/quality where available

			if (leadLength >= 1) {
				// extract the lead sequence/quality
				int leadStartIndexInUpstreamRecord = leadStartPosition - upstreamAlignmentStart;
				int leadEndIndexInUpstreamRecord = leadEndPosition - upstreamAlignmentStart;

				leadSequence = upstreamSequence.subSequence(leadStartIndexInUpstreamRecord, leadEndIndexInUpstreamRecord);
				leadQuality = upstreamQuality.substring(leadStartIndexInUpstreamRecord, leadEndIndexInUpstreamRecord + 1);
				assert leadSequence.size() == leadQuality.length();
			}

			// extract the overlap sequence/quality from both the upstream and downstream sources
			int overlapStartIndexInUpstreamRecord = overlapStartPosition - upstreamAlignmentStart;
			int overlapEndIndexInUpstreamRecord = overlapEndPosition - upstreamAlignmentStart;

			int overlapStartIndexInDownstreamRecord = overlapStartPosition - downstreamAlignmentStart;
			int overlapEndIndexInDownstreamRecord = overlapEndPosition - downstreamAlignmentStart;

			ISequence upstreamOverlapSequence = upstreamSequence.subSequence(overlapStartIndexInUpstreamRecord, overlapEndIndexInUpstreamRecord);
			String upstreamOverlapQuality = upstreamQuality.substring(overlapStartIndexInUpstreamRecord, overlapEndIndexInUpstreamRecord + 1);
			assert upstreamOverlapSequence.size() == upstreamOverlapQuality.length();

			ISequence downstreamOverlapSequenceOnPositiveStrand = downstreamSequence.subSequence(overlapStartIndexInDownstreamRecord, overlapEndIndexInDownstreamRecord);
			String downstreamOverlapQualityOnPositiveStrand = downstreamQuality.substring(overlapStartIndexInDownstreamRecord, overlapEndIndexInDownstreamRecord + 1);
			assert downstreamOverlapSequenceOnPositiveStrand.size() == downstreamOverlapQualityOnPositiveStrand.length();
			assert upstreamOverlapSequence.size() == downstreamOverlapSequenceOnPositiveStrand.size();

			StringBuilder overlappingSequenceBuilder = new StringBuilder();
			StringBuilder overlappingQualityBuilder = new StringBuilder();

			// use quality scores to resolve conflicts when merging content in the overlap region
			for (int i = 0; i < overlapLength; i++) {
				short upstreamQualityScoreAtPosition = BamFileUtil.getQualityScore(upstreamOverlapQuality.substring(i, i + 1));
				short downstreamQualityScoreAtPosition = BamFileUtil.getQualityScore(downstreamQuality.substring(i, i + 1));
				// a higher quality score indicates a smaller probability of error (source: http://www.illumina.com/truseq/quality_101/quality_scores.ilmn)
				if (upstreamQualityScoreAtPosition >= downstreamQualityScoreAtPosition) {
					overlappingSequenceBuilder.append(upstreamOverlapSequence.getCodeAt(i).toString());
					overlappingQualityBuilder.append(upstreamOverlapQuality.charAt(i));
				} else {
					overlappingSequenceBuilder.append(downstreamOverlapSequenceOnPositiveStrand.getCodeAt(i).toString());
					overlappingQualityBuilder.append(downstreamOverlapQualityOnPositiveStrand.charAt(i));
				}
			}

			if (trailLength >= 1) {
				// extract the trail sequence/quality
				int trailStartIndexInSourceRecord = trailStartPosition - trailSourceAlignmentStart;
				int trailEndIndexInSourceRecord = trailEndPosition - trailSourceAlignmentStart;
				trailSequence = trailSourceSequence.subSequence(trailStartIndexInSourceRecord, trailEndIndexInSourceRecord);
				trailQuality = trailSourceQuality.substring(trailStartIndexInSourceRecord, trailEndIndexInSourceRecord + 1);
				assert trailSequence.size() == trailQuality.length();
			}

			overlappingSequence = new IupacNucleotideCodeSequence(overlappingSequenceBuilder.toString());
			overlappingQuality = overlappingQualityBuilder.toString();

		}

		// merge the sections of the sequence and quality strings where sections may be validly empty
		ISequence mergedSequence = new IupacNucleotideCodeSequence();
		if (leadLength > 0) {
			mergedSequence.append(leadSequence);
		}
		if (overlapLength > 0) {
			mergedSequence.append(overlappingSequence);
		}
		if (trailLength > 0) {
			mergedSequence.append(trailSequence);
		}

		String mergedQuality = (leadLength > 0 ? leadQuality : "") + (overlapLength > 0 ? overlappingQuality : "") + (trailLength > 0 ? trailQuality : "");

		if (isNegativeStrand) {
			// transform the sequences as necessary when they are from the negative strand
			mergedSequence = mergedSequence.getReverseCompliment();
			mergedQuality = StringUtil.reverse(mergedQuality);
		}

		return new MergeInformation(mergedSequence, mergedQuality);
	}
}
