/*
 *    Copyright 2016 Roche NimbleGen Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.roche.heatseq.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.cli.DeduplicationCli;
import com.roche.heatseq.cli.HsqUtilsCli;
import com.roche.heatseq.objects.ApplicationSettings;
import com.roche.heatseq.objects.ExtendReadResults;
import com.roche.heatseq.objects.IReadPair;
import com.roche.heatseq.objects.ReadNameSet;
import com.roche.heatseq.objects.SAMRecordPair;
import com.roche.heatseq.objects.UidReductionResultsForAProbe;
import com.roche.heatseq.process.FastqReadTrimmer.ProbeTrimmingInformation;
import com.roche.heatseq.qualityreport.ProbeDetailsReport;
import com.roche.heatseq.qualityreport.ProbeProcessingStats;
import com.roche.heatseq.qualityreport.ReportManager;
import com.roche.heatseq.utils.BamFileUtil;
import com.roche.heatseq.utils.SAMRecordUtil;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.AlphaNumericStringComparator;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.IlluminaFastQReadNameUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.TabDelimitedFileWriter;
import com.roche.sequencing.bioinformatics.common.utils.fastq.FastqReader;
import com.roche.sequencing.bioinformatics.common.utils.fastq.PicardException;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ParsedProbeFile;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.Probe;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ProbeFileUtil;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.fastq.FastqRecord;

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
	final static int DEFAULT_MAX_RECORDS_IN_RAM = 500000;

	// final Set<String> uniqueReads;
	private final Set<String> unableToExtendReads;
	private final AtomicLong numberOfUniqueReadPairs;
	// final Set<String> duplicateReads;
	private final AtomicLong numberOfDuplicateReadPairs;
	private final String commonReadNameBeginning;

	private final static Map<String, ProbeProcessingStats> probeProcessingStatsByProbeId = new ConcurrentHashMap<String, ProbeProcessingStats>();

	/**
	 * We never create instances of this class, we only expose static methods
	 */
	public PrimerReadExtensionAndPcrDuplicateIdentification(String commonReadNameBeginning) {
		// uniqueReads = new ReadNameSet(commonReadNameBeginning);
		unableToExtendReads = new ReadNameSet(commonReadNameBeginning);
		// duplicateReads = new ReadNameSet(commonReadNameBeginning);
		this.numberOfUniqueReadPairs = new AtomicLong();
		this.numberOfDuplicateReadPairs = new AtomicLong();
		this.commonReadNameBeginning = commonReadNameBeginning;
	}

	/**
	 * Get reads for each probe from a merged BAM file, determine which read to use for each UID, extend the reads to the target primers, and output the reduced and extended reads to a new BAM file
	 * 
	 * @param applicationSettings
	 *            The context our application is running under.
	 */
	public void filterBamEntriesByUidAndExtendReadsToPrimers(ApplicationSettings applicationSettings) {

		long mergeStart = System.currentTimeMillis();

		// Parse the input probe file
		ParsedProbeFile probeInfo = ProbeFileUtil.parseProbeInfoFileWithValidation(applicationSettings.getProbeFile());

		Map<String, Probe> readsToProbeAssignments = PrimerReadExtensionAndPcrDuplicateIdentification.getReadToProbeAssignments(probeInfo, applicationSettings, commonReadNameBeginning);

		File mergedBamFileSortedByCoordinates;
		File indexFileForMergedBamFileSortedByCoordinates;
		try {
			mergedBamFileSortedByCoordinates = File.createTempFile("merged_bam_sorted_by_coordinates_", ".bam", applicationSettings.getTempDirectory());
		} catch (IOException e1) {
			throw new IllegalStateException("Unable to create temp files at [" + applicationSettings.getTempDirectory() + "].");
		}

		ProbeTrimmingInformation probeTrimmingInformation;
		try {
			probeTrimmingInformation = FastqReadTrimmer.getProbeTrimmingInformation(probeInfo, applicationSettings.getProbeFile(), !applicationSettings.isReadsNotTrimmed());
		} catch (IOException e1) {
			throw new IllegalStateException("Unable to read probe file[" + applicationSettings.getProbeFile() + "].");
		}

		try {
			FastqAndBamFileMerger.createMergedFastqAndBamFileFromUnsortedFiles(applicationSettings.getBamFile(), applicationSettings.getBamFileIndex(), applicationSettings.getFastQ1File(),
					applicationSettings.getFastQ2File(), mergedBamFileSortedByCoordinates, applicationSettings.isReadsNotTrimmed(), probeTrimmingInformation, applicationSettings.getTempDirectory(),
					readsToProbeAssignments, commonReadNameBeginning);
		} catch (UnableToMergeFastqAndBamFilesException e) {
			throw new IllegalStateException("The provided BAM file contains reads that were not trimmed using the " + HsqUtilsCli.APPLICATION_NAME + " " + HsqUtilsCli.TRIM_COMMAND_NAME
					+ " command or the supplied fastq files are not the files provided to the " + HsqUtilsCli.APPLICATION_NAME + " " + HsqUtilsCli.TRIM_COMMAND_NAME
					+ " command.  Please verify that fastq and bam files are correct.  If trimming was skipped please provide the --" + DeduplicationCli.TRIMMING_SKIPPED_OPTION.getLongFormOption()
					+ " option to the " + HsqUtilsCli.DEDUPLICATION_COMMAND_NAME + " command line arguments.  BAM file[" + applicationSettings.getBamFile().getAbsolutePath() + "] fastq1["
					+ applicationSettings.getFastQ1File().getAbsolutePath() + "] fastq2[" + applicationSettings.getFastQ2File().getAbsolutePath() + "].");
		}
		long timeAfterMergeUnsorted = System.currentTimeMillis();
		logger.debug("Done merging bam and fastq files ... result[" + mergedBamFileSortedByCoordinates.getAbsolutePath() + "] in "
				+ DateUtil.convertMillisecondsToHHMMSS(timeAfterMergeUnsorted - mergeStart) + ".");

		// Build bam index
		indexFileForMergedBamFileSortedByCoordinates = new File(mergedBamFileSortedByCoordinates.getParent(), mergedBamFileSortedByCoordinates.getName() + ".bai");

		BamFileUtil.createIndex(mergedBamFileSortedByCoordinates, indexFileForMergedBamFileSortedByCoordinates);
		long timeAfterBuildBamIndex = System.currentTimeMillis();
		logger.debug("Done creating index for merged and sorted bam file ... result[" + indexFileForMergedBamFileSortedByCoordinates.getAbsolutePath() + "] in "
				+ DateUtil.convertMillisecondsToHHMMSS(timeAfterBuildBamIndex - timeAfterMergeUnsorted));

		// Initialize the thread semaphore if it hasn't already been initialized

		if (primerReadExtensionAndFilteringOfUniquePcrProbesSemaphore == null) {
			primerReadExtensionAndFilteringOfUniquePcrProbesSemaphore = new Semaphore(applicationSettings.getNumProcessors());
		}

		long start = System.currentTimeMillis();

		// Set up the reports files
		ReportManager reportManager = new ReportManager(applicationSettings.getProgramName(), applicationSettings.getProgramVersion(), applicationSettings.getSampleName(),
				applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix(), applicationSettings.isShouldOutputReports());

		// Actually do the work
		filterBamEntriesByUidAndExtendReadsToPrimers(applicationSettings, mergedBamFileSortedByCoordinates, indexFileForMergedBamFileSortedByCoordinates, readsToProbeAssignments, probeInfo,
				reportManager);

		long stop = System.currentTimeMillis();

		// Report on performance
		logger.debug("Total time: " + DateUtil.convertMillisecondsToHHMMSS(stop - start));
	}

	static boolean readPairAlignsWithProbeCoordinates(Probe probe, SAMRecord record, boolean isFastq1, int extensionUidLength, int ligationUidLength) {
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

	private static Map<String, Probe> getReadToProbeAssignments(ParsedProbeFile probeInfo, ApplicationSettings applicationSettings, String commonReadNameBeginning) {
		long start = System.currentTimeMillis();
		Map<String, Probe> readToProbeAssignments = new ConcurrentHashMap<String, Probe>();
		Set<String> sequenceNames = probeInfo.getSequenceNames();

		Map<String, IRangeMap<Probe>> positveStrandProbesRangesBySequenceName = new ConcurrentHashMap<String, IRangeMap<Probe>>();
		Map<String, IRangeMap<Probe>> negativeStrandProbesRangesBySequenceName = new ConcurrentHashMap<String, IRangeMap<Probe>>();
		for (String sequenceName : sequenceNames) {
			List<Probe> probes = probeInfo.getProbesBySequenceName(sequenceName);
			IRangeMap<Probe> positiveStrandRangeMap = new RangeMap<Probe>();
			IRangeMap<Probe> negativeStrandRangeMap = new RangeMap<Probe>();
			for (Probe probe : probes) {
				int queryStart = probe.getStart();
				Integer basesInsideExtensionPrimerWindow = applicationSettings.getProbeHeaderInformation().getBasesInsideExtensionPrimerWindow();
				if (basesInsideExtensionPrimerWindow != null) {
					queryStart += basesInsideExtensionPrimerWindow;
				}

				int queryStop = probe.getStop();
				Integer basesInsideLigationPrimerWindow = applicationSettings.getProbeHeaderInformation().getBasesInsideLigationPrimerWindow();
				if (basesInsideLigationPrimerWindow != null) {
					queryStop -= basesInsideLigationPrimerWindow;
				}

				if (probe.getProbeStrand() == Strand.FORWARD) {
					positiveStrandRangeMap.put(queryStart, queryStop, probe);
				} else {
					negativeStrandRangeMap.put(queryStart, queryStop, probe);
				}
			}
			positveStrandProbesRangesBySequenceName.put(sequenceName, positiveStrandRangeMap);
			negativeStrandProbesRangesBySequenceName.put(sequenceName, negativeStrandRangeMap);
		}
		AtomicInteger assignedToMultProbesCount = new AtomicInteger(0);
		TallyMap<Probe> probesAssignedToMult = new TallyMap<Probe>();

		TallyMap<String> readNamesThatAreTheSameForMultiplePairs = new TallyMap<String>();

		ExecutorService executor = Executors.newFixedThreadPool(applicationSettings.getNumProcessors());

		for (String sequenceName : sequenceNames) {
			ReadToProbeAssigner readToProbeAssigner = new ReadToProbeAssigner(sequenceName, applicationSettings.getBamFile(), applicationSettings.getBamFileIndex(),
					positveStrandProbesRangesBySequenceName, negativeStrandProbesRangesBySequenceName, probesAssignedToMult, readNamesThatAreTheSameForMultiplePairs, readToProbeAssignments,
					assignedToMultProbesCount, commonReadNameBeginning);
			executor.execute(readToProbeAssigner);
		}

		// Wait until all our threads are done processing.
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		if (readNamesThatAreTheSameForMultiplePairs.getSumOfAllBins() > 0) {
			logger.info("The following reads names are not unique within the bam file for a single read pair and will not be utilized for deduplication:");
			for (Entry<String, Integer> repeatedReadNameEntry : readNamesThatAreTheSameForMultiplePairs.getTalliesAsMap().entrySet()) {
				// remove them here so that if it is repeated an odd number of times it doesn't end up using it
				readToProbeAssignments.remove(repeatedReadNameEntry.getKey());
				logger.info("read name:" + repeatedReadNameEntry.getKey() + "[found " + (repeatedReadNameEntry.getValue() + 1) + " time(s)].");
			}
		}

		if (assignedToMultProbesCount.intValue() > 0) {
			logger.info("Total number of reads assigned to multiple probes:" + assignedToMultProbesCount);
			logger.info("Probe_Id" + StringUtil.TAB + "Number_Of_Reads_Assigned_To_This_Probe_And_Another_Probe");
			for (Entry<Probe, Integer> entry : probesAssignedToMult.getTalliesAsMap().entrySet()) {
				logger.info(entry.getKey().getProbeId() + StringUtil.TAB + entry.getValue());
			}
		}

		long end = System.currentTimeMillis();
		logger.info("Total time for assigning reads to probes based on mapping: " + DateUtil.convertMillisecondsToHHMMSS(end - start));

		return readToProbeAssignments;
	}

	private static class ReadToProbeAssigner implements Runnable {

		private final String sequenceName;
		private final File samFile;
		private final File samIndexFile;
		private Map<String, IRangeMap<Probe>> positveStrandProbesRangesBySequenceName;
		private Map<String, IRangeMap<Probe>> negativeStrandProbesRangesBySequenceName;
		private final TallyMap<Probe> probesAssignedToMult;
		private final TallyMap<String> readNamesThatAreTheSameForMultiplePairs;
		private final Map<String, Probe> readToProbeAssignments;
		private final AtomicInteger assignedToMultProbesCount;
		private final String commonReadNameBeginning;

		public ReadToProbeAssigner(String sequenceName, File samFile, File samIndexFile, Map<String, IRangeMap<Probe>> positveStrandProbesRangesBySequenceName,
				Map<String, IRangeMap<Probe>> negativeStrandProbesRangesBySequenceName, TallyMap<Probe> probesAssignedToMult, TallyMap<String> readNamesThatAreTheSameForMultiplePairs,
				Map<String, Probe> readToProbeAssignments, AtomicInteger assignedToMultProbesCount, String commonReadNameBeginning) {
			super();
			this.sequenceName = sequenceName;
			this.samFile = samFile;
			this.samIndexFile = samIndexFile;
			this.positveStrandProbesRangesBySequenceName = positveStrandProbesRangesBySequenceName;
			this.negativeStrandProbesRangesBySequenceName = negativeStrandProbesRangesBySequenceName;
			this.probesAssignedToMult = probesAssignedToMult;
			this.readNamesThatAreTheSameForMultiplePairs = readNamesThatAreTheSameForMultiplePairs;
			this.readToProbeAssignments = readToProbeAssignments;
			this.assignedToMultProbesCount = assignedToMultProbesCount;
			this.commonReadNameBeginning = commonReadNameBeginning;
		}

		@Override
		public void run() {
			assignProbeToRead();
		}

		private void assignProbeToRead() {

			Map<String, List<Probe>> containedProbesForFirstFoundReadByReadName = new HashMap<String, List<Probe>>();
			Map<String, AlignmentStartAndStop> alignmentBoundsForFirstFoundReadByReadName = new HashMap<String, AlignmentStartAndStop>();
			try (SamReader samReader = SamReaderFactory.makeDefault().open(SamInputResource.of(samFile).index(samIndexFile))) {
				SAMRecordIterator allSamRecordsIter = samReader.query(sequenceName, 0, Integer.MAX_VALUE, false);
				while (allSamRecordsIter.hasNext()) {
					SAMRecord record = allSamRecordsIter.next();

					if (record.getReadName().equals("M01947:205:000000000-AB8AB:1:2112:6577:16586")) {
						System.out.println("here:");
					}
					
					// note: that sometimes a mate has incorrect details about whether or not its mate is mapped
					// so by checking the flag we will exclude these incorrectly labeled reads
					boolean isUnmapped = record.getMateUnmappedFlag() || record.getReadUnmappedFlag();

					if (record != null && !isUnmapped) {
						IRangeMap<Probe> probeRanges = null;

						// ASSUMPTIONS:
						// Fastq1/firstOfPair always has same strandedness as probe
						// Fastq2/secondOfPair always has opposite strandedness as probe
						boolean isNegativeStrand = record.getReadNegativeStrandFlag();
						if (record.getSecondOfPairFlag()) {
							isNegativeStrand = !isNegativeStrand;
						}

						if (isNegativeStrand) {
							probeRanges = negativeStrandProbesRangesBySequenceName.get(sequenceName);
						} else {
							probeRanges = positveStrandProbesRangesBySequenceName.get(sequenceName);
						}

						if (probeRanges != null) {
							String readName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(commonReadNameBeginning, record.getReadName());
							List<Probe> containedProbes = probeRanges.getObjectsThatContainRangeInclusive(record.getAlignmentStart(), record.getAlignmentEnd());

							List<Probe> containedProbesFromFirstFoundReadInPair = containedProbesForFirstFoundReadByReadName.get(readName);
							boolean isFirstFoundReadOfPair = containedProbesFromFirstFoundReadInPair == null;

							if (!isFirstFoundReadOfPair) {

								List<Probe> containedProbesFromSecondReadInPair = containedProbes;

								Set<Probe> containedProbesInBothReads = new HashSet<Probe>();
								for (Probe probe : containedProbesFromFirstFoundReadInPair) {
									if (containedProbesFromSecondReadInPair.contains(probe)) {
										containedProbesInBothReads.add(probe);
									}
								}

								Probe assignedProbe = null;
								if (containedProbesInBothReads.size() == 1) {
									assignedProbe = containedProbesInBothReads.iterator().next();
								} else if (containedProbesInBothReads.size() > 1) {
									List<ProbeAndOutOfBoundNucleotidesPair> containedProbesWithReadsWithinCaptureTarget = new ArrayList<ProbeAndOutOfBoundNucleotidesPair>();
									for (Probe probe : containedProbesInBothReads) {
										int probeStart = Math.min(probe.getCaptureTargetStart(), probe.getCaptureTargetStop());
										int probeStop = Math.max(probe.getCaptureTargetStart(), probe.getCaptureTargetStop());

										int readOneStart = 0;
										int readOneStop = 0;
										int readTwoStart = 0;
										int readTwoStop = 0;

										if (record.getFirstOfPairFlag()) {
											readOneStart = Math.min(record.getAlignmentStart(), record.getAlignmentEnd());
											readOneStop = Math.max(record.getAlignmentStart(), record.getAlignmentEnd());

											readTwoStart = alignmentBoundsForFirstFoundReadByReadName.get(readName).getStart();
											readTwoStop = alignmentBoundsForFirstFoundReadByReadName.get(readName).getStop();
										} else {
											readOneStart = alignmentBoundsForFirstFoundReadByReadName.get(readName).getStart();
											readOneStop = alignmentBoundsForFirstFoundReadByReadName.get(readName).getStop();

											readTwoStart = Math.min(record.getAlignmentStart(), record.getAlignmentEnd());
											readTwoStop = Math.max(record.getAlignmentStart(), record.getAlignmentEnd());
										}
										int totalNtsMissingFromStartOfCaptureTarget = 0;
										int totalNtsOutsideOfCaptureTarget = 0;
										if (readOneStart < probeStart) {
											totalNtsOutsideOfCaptureTarget += (probeStart - readOneStart);
										} else if (readOneStart > probeStart) {
											totalNtsMissingFromStartOfCaptureTarget += (readOneStart - probeStart);
										}

										if (readOneStop > probeStop) {
											totalNtsOutsideOfCaptureTarget += (readOneStop - probeStop);
										}

										if (readTwoStart < probeStart) {
											totalNtsOutsideOfCaptureTarget += (probeStart - readTwoStart);
										} else if (readTwoStart > probeStart) {
											totalNtsMissingFromStartOfCaptureTarget += (readTwoStart - probeStart);
										}
										if (readTwoStop > probeStop) {
											totalNtsOutsideOfCaptureTarget += (readTwoStop - probeStop);
										}

										containedProbesWithReadsWithinCaptureTarget
												.add(new ProbeAndOutOfBoundNucleotidesPair(totalNtsOutsideOfCaptureTarget, totalNtsMissingFromStartOfCaptureTarget, probe));
									}

									Collections.sort(containedProbesWithReadsWithinCaptureTarget, new Comparator<ProbeAndOutOfBoundNucleotidesPair>() {
										@Override
										public int compare(ProbeAndOutOfBoundNucleotidesPair o1, ProbeAndOutOfBoundNucleotidesPair o2) {
											return Integer.compare(o1.getNumberOfOutOfBoundNucleotides(), o2.getNumberOfOutOfBoundNucleotides());
										}
									});

									int bestOutOfBoundsNt = containedProbesWithReadsWithinCaptureTarget.get(0).getNumberOfOutOfBoundNucleotides();
									int secondBestOutOfBoundsNt = containedProbesWithReadsWithinCaptureTarget.get(1).getNumberOfOutOfBoundNucleotides();

									if (bestOutOfBoundsNt < secondBestOutOfBoundsNt) {
										assignedProbe = containedProbesWithReadsWithinCaptureTarget.get(0).getProbe();
									} else {

										Collections.sort(containedProbesWithReadsWithinCaptureTarget, new Comparator<ProbeAndOutOfBoundNucleotidesPair>() {
											@Override
											public int compare(ProbeAndOutOfBoundNucleotidesPair o1, ProbeAndOutOfBoundNucleotidesPair o2) {
												return Integer.compare(o1.getTotalNtsMissingFromStartOfCaptureTarget(), o2.getTotalNtsMissingFromStartOfCaptureTarget());
											}
										});

										int bestMissingNtFromStart = containedProbesWithReadsWithinCaptureTarget.get(0).getTotalNtsMissingFromStartOfCaptureTarget();
										int secondBestMissingNtFromStart = containedProbesWithReadsWithinCaptureTarget.get(1).getTotalNtsMissingFromStartOfCaptureTarget();

										if (bestMissingNtFromStart < secondBestMissingNtFromStart) {
											assignedProbe = containedProbesWithReadsWithinCaptureTarget.get(0).getProbe();
										} else {
											probesAssignedToMult.add(containedProbesWithReadsWithinCaptureTarget.get(0).getProbe());
											probesAssignedToMult.add(containedProbesWithReadsWithinCaptureTarget.get(1).getProbe());
											assignedToMultProbesCount.incrementAndGet();
											String probeId1 = containedProbesWithReadsWithinCaptureTarget.get(0).getProbe().getProbeId();
											String probeId2 = containedProbesWithReadsWithinCaptureTarget.get(1).getProbe().getProbeId();
											// note: at this point both statistics which were used from tie breakers are the same so
											// it does not matter if the exact statistics do not match the probe ids due to resorting (because they are the same values).
											logger.info("Read[" + readName + "] will not be processed because it aligns with multiple probes [probe1:" + probeId1 + "  (bases_outside_of_capture_score:"
													+ bestOutOfBoundsNt + " bases_missing_from_start:" + bestMissingNtFromStart + ")  ||  probe2:" + probeId2 + " (bases_outside_of_capture_score:"
													+ secondBestOutOfBoundsNt + " bases_missing_from_start:" + secondBestMissingNtFromStart + ")]");
										}
									}

								}

								if (assignedProbe != null) {
									// check to see if this readName has already been added, if so then there are more than two entries for this
									// read name and it should be excluded
									if (readToProbeAssignments.containsKey(readName)) {
										readNamesThatAreTheSameForMultiplePairs.add(readName);
									} else {
										readToProbeAssignments.put(readName, assignedProbe);
									}
								}

							} else {
								containedProbesForFirstFoundReadByReadName.put(readName, containedProbes);
								alignmentBoundsForFirstFoundReadByReadName.put(readName, new AlignmentStartAndStop(record.getAlignmentStart(), record.getAlignmentEnd()));
							}

						}
					}
				}

				allSamRecordsIter.close();
			} catch (IOException e) {
				throw new PicardException(e.getMessage(), e);
			}

		}
	}

	private static class AlignmentStartAndStop {
		private final int start;
		private final int stop;

		public AlignmentStartAndStop(int start, int stop) {
			super();
			this.start = Math.min(start, stop);
			this.stop = Math.max(start, stop);
		}

		public int getStart() {
			return start;
		}

		public int getStop() {
			return stop;
		}

	}

	private static class ProbeAndOutOfBoundNucleotidesPair {
		private final int numberOfOutOfBoundNucleotides;
		private final int totalNtsMissingFromStartOfCaptureTarget;
		private final Probe probe;

		public ProbeAndOutOfBoundNucleotidesPair(int numberOfOutOfBoundNucleotides, int totalNtsMissingFromStartOfCaptureTarget, Probe probe) {
			super();
			this.numberOfOutOfBoundNucleotides = numberOfOutOfBoundNucleotides;
			this.totalNtsMissingFromStartOfCaptureTarget = totalNtsMissingFromStartOfCaptureTarget;
			this.probe = probe;
		}

		public int getNumberOfOutOfBoundNucleotides() {
			return numberOfOutOfBoundNucleotides;
		}

		public Probe getProbe() {
			return probe;
		}

		public int getTotalNtsMissingFromStartOfCaptureTarget() {
			return totalNtsMissingFromStartOfCaptureTarget;
		}

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
	private void filterBamEntriesByUidAndExtendReadsToPrimers(ApplicationSettings applicationSettings, File mergedBamFile, File mergedBamFileIndex, Map<String, Probe> readsToProbeAssignments,
			ParsedProbeFile probeInfo, ReportManager reportManager) {
		long start = System.currentTimeMillis();

		Set<String> sequenceNames = probeInfo.getSequenceNames();

		Set<ISequence> distinctUids = Collections.newSetFromMap(new ConcurrentHashMap<ISequence, Boolean>());
		List<ISequence> uids = new ArrayList<ISequence>();

		SAMFileWriter samWriter = null;

		int totalProbes = 0;
		try (SamReader mergedSamReader = SamReaderFactory.makeDefault().open(SamInputResource.of(mergedBamFile).index(mergedBamFileIndex))) {

			String outputSortedBamFileName = applicationSettings.getOutputBamFileName();
			File outputSortedBamFile = new File(applicationSettings.getOutputDirectory(), outputSortedBamFileName);

			try {
				outputSortedBamFile.createNewFile();
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}

			SAMFileHeader header = BamFileUtil.getHeader(false, applicationSettings.isShouldExcludeProgramInBamHeader(), mergedSamReader.getFileHeader(), probeInfo,
					applicationSettings.getCommandLineSignature(), applicationSettings.getProgramName(), applicationSettings.getProgramVersion());
			header.setSortOrder(SortOrder.coordinate);
			samWriter = new SAMFileWriterFactory().setMaxRecordsInRam(DEFAULT_MAX_RECORDS_IN_RAM).setTempDirectory(applicationSettings.getTempDirectory()).makeBAMWriter(header, false,
					outputSortedBamFile, 0);

			List<SAMSequenceRecord> referenceSequencesInBam = mergedSamReader.getFileHeader().getSequenceDictionary().getSequences();
			List<String> referenceSequenceNamesInBam = new ArrayList<String>(referenceSequencesInBam.size());
			List<Integer> referenceSequenceLengthsInBam = new ArrayList<Integer>(referenceSequencesInBam.size());
			for (SAMSequenceRecord referenceSequence : referenceSequencesInBam) {
				referenceSequenceNamesInBam.add(referenceSequence.getSequenceName());
				referenceSequenceLengthsInBam.add(referenceSequence.getSequenceLength());
			}

			// Make an executor to handle processing the data for each probe in parallel
			ExecutorService executor = Executors.newFixedThreadPool(applicationSettings.getNumProcessors());
			for (String sequenceName : sequenceNames) {
				int totalReadPairsForSequence = 0;
				if (!referenceSequenceNamesInBam.contains(sequenceName)) {
					throw new IllegalStateException("Sequence [" + sequenceName
							+ "] from the probe file is not present as a reference sequence in the bam file.  Please make sure your probe sequence names match bam file reference sequence names.");
				}

				List<Probe> probes = probeInfo.getProbesBySequenceName(sequenceName);

				int totalProbesInSequence = probes.size();
				totalProbes += totalProbesInSequence;
				long sequenceProcessingStart = System.currentTimeMillis();
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

					int queryStart = probe.getStart();
					Integer basesInsideExtensionPrimerWindow = applicationSettings.getProbeHeaderInformation().getBasesInsideExtensionPrimerWindow();
					if (basesInsideExtensionPrimerWindow != null) {
						queryStart += basesInsideExtensionPrimerWindow;
					}

					int queryStop = probe.getStop();
					Integer basesInsideLigationPrimerWindow = applicationSettings.getProbeHeaderInformation().getBasesInsideLigationPrimerWindow();
					if (basesInsideLigationPrimerWindow != null) {
						queryStop -= basesInsideLigationPrimerWindow;
					}

					SAMRecordIterator samRecordIter = mergedSamReader.queryContained(sequenceName, queryStart, queryStop);

					while (samRecordIter.hasNext()) {
						SAMRecord record = samRecordIter.next();
						String readName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(commonReadNameBeginning, record.getReadName());
						Probe assignedProbe = readsToProbeAssignments.get(readName);

						if (assignedProbe != null && assignedProbe.equals(probe)) {

							SAMRecordPair pair = readNameToRecordsMap.get(readName);

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

								readNameToRecordsMap.put(readName, pair);

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
							// the read to probe assignment is no longer needed so remove it to save memory
							readsToProbeAssignments.remove(readName);
						}
					}

					totalReadPairsForSequence += readNameToCompleteRecordsMap.size();

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
				long sequenceProcessingStop = System.currentTimeMillis();
				logger.debug("Done processing " + sequenceName + " with " + totalProbesInSequence + " Probes and " + totalReadPairsForSequence + " Read Pairs in "
						+ DateUtil.convertMillisecondsToHHMMSS(sequenceProcessingStop - sequenceProcessingStart) + ".");
			}

			// Wait until all our threads are done processing.
			executor.shutdown();
			try {
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				throw new RuntimeException(e.getMessage(), e);
			}

			samWriter.close();

			// Make index for BAM file
			BamFileUtil.createIndex(outputSortedBamFile);

		} catch (IOException e) {
			throw new PicardException(e.getMessage(), e);
		}
		try (SamReader originalInputSamReader = SamReaderFactory.makeDefault().open(SamInputResource.of(applicationSettings.getBamFile()).index(applicationSettings.getBamFileIndex()))) {
			int totalReadPairs = 0;
			int totalFullyMappedOffTargetReadPairs = 0;
			int totalPartiallyMappedReadPairs = 0;
			int totalFullyMappedReadPairs = 0;
			int totalFullyUnmappedReadPairs = 0;

			int totalFullyMappedOnTargetReadPairs = 0;
			int uniqueOnTargetReadPairs = 0;
			int duplicateOnTargetReadPairs = 0;

			Map<String, SAMRecord> firstFoundRecordByReadName = new HashMap<String, SAMRecord>();

			SAMRecordIterator samRecordIter = originalInputSamReader.iterator();
			while (samRecordIter.hasNext()) {
				SAMRecord record = samRecordIter.next();

				String readName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(commonReadNameBeginning, record.getReadName());

				// a1 the following code is just to give more details on which reads are incorrect with respect to mate mapping details
				if (firstFoundRecordByReadName.containsKey(readName)) {
					totalReadPairs++;
					SAMRecord firstRecord = firstFoundRecordByReadName.get(readName);
					SAMRecord secondRecord = record;

					boolean firstRecordMapped = !firstRecord.getReadUnmappedFlag();
					boolean secondRecordMapped = !secondRecord.getReadUnmappedFlag();
					boolean readAndMateMapped = firstRecordMapped && secondRecordMapped;

					boolean firstFoundIsCorrect = firstRecord.getMateUnmappedFlag() == secondRecord.getReadUnmappedFlag();
					boolean secondFoundIsCorrect = secondRecord.getMateUnmappedFlag() == firstRecord.getReadUnmappedFlag();
					boolean isMateMappedFlagIncorrect = !firstFoundIsCorrect || !secondFoundIsCorrect;
					if (isMateMappedFlagIncorrect) {
						String message = "Reads associated with read name[" + readName + "] are not labeled correctly with regards to mate mappings.";
						if (readAndMateMapped) {
							message += "  Despite both reads in the pair being mapped, the mate unmmapped flag is set to true for at least one of the reads.  These reads will be marked as off-target.";
						} else {
							message += "  Since both of the reads in the pair are not actually mapped this will not affect the results.";
						}

						logger.info(message);
					}

					boolean partiallyMapped = (firstRecordMapped && !secondRecordMapped) || (!firstRecordMapped && secondRecordMapped);
					if (partiallyMapped) {
						totalPartiallyMappedReadPairs++;
					} else if (!readAndMateMapped) {
						totalFullyUnmappedReadPairs++;
					} else if (readAndMateMapped) {
						totalFullyMappedReadPairs++;
					} else {
						throw new AssertionError("Unable to classify read[" + readName + "].");
					}

					firstFoundRecordByReadName.remove(readName);
				} else {
					firstFoundRecordByReadName.put(readName, record);
				}
			}

			uniqueOnTargetReadPairs = numberOfUniqueReadPairs.intValue();
			duplicateOnTargetReadPairs = numberOfDuplicateReadPairs.intValue();

			totalFullyMappedOnTargetReadPairs = uniqueOnTargetReadPairs + duplicateOnTargetReadPairs;
			totalFullyMappedOffTargetReadPairs = totalFullyMappedReadPairs - totalFullyMappedOnTargetReadPairs;

			if (firstFoundRecordByReadName.size() > 0) {
				logger.info("There are [" + firstFoundRecordByReadName.size() + "] reads that do not contain a matching pair with the same read name.");
				if (firstFoundRecordByReadName.size() <= 10) {
					for (String unpairdReadName : firstFoundRecordByReadName.keySet()) {
						logger.info("Read name[" + unpairdReadName + "] does not contain a matching pair in the bam file.");
					}
				}
			}
			int unpairedReads = firstFoundRecordByReadName.size();

			samRecordIter.close();

			List<String> probeIds = new ArrayList<String>(probeProcessingStatsByProbeId.keySet());
			Collections.sort(probeIds, new ProbeIdComparator());

			ProbeDetailsReport detailsReport = reportManager.getDetailsReport();
			if (detailsReport != null) {
				for (String probeId : probeIds) {
					detailsReport.writeEntry(probeProcessingStatsByProbeId.get(probeId));
				}
			}

			long end = System.currentTimeMillis();
			long processingTimeInMs = end - start;
			reportManager.completeSummaryReport(distinctUids, uids, processingTimeInMs, totalProbes, totalReadPairs, totalFullyMappedOffTargetReadPairs, totalPartiallyMappedReadPairs,
					totalFullyUnmappedReadPairs, totalFullyMappedOnTargetReadPairs, uniqueOnTargetReadPairs, duplicateOnTargetReadPairs, unpairedReads);

			reportManager.close();
		} catch (IOException e) {
			throw new PicardException(e.getMessage(), e);
		}
	}

	private static class ProbeIdComparator implements Comparator<String> {

		private AlphaNumericStringComparator alphaNumericComparator = new AlphaNumericStringComparator();

		@Override
		public int compare(String probeId1, String probeId2) {
			int probe1ChrDivider = probeId1.indexOf(":");
			int probe2ChrDivider = probeId2.indexOf(":");

			String probe1Chromosome = probeId1.substring(0, probe1ChrDivider);
			String probe2Chromosome = probeId2.substring(0, probe2ChrDivider);

			int result = alphaNumericComparator.compare(probe1Chromosome, probe2Chromosome);

			if (result == 0) {
				String probe1StartAsString = probeId1.substring(probe1ChrDivider + 1, probeId1.indexOf(":", probe1ChrDivider + 1));
				String probe2StartAsString = probeId2.substring(probe2ChrDivider + 1, probeId2.indexOf(":", probe2ChrDivider + 1));

				try {
					int probe1Start = Integer.valueOf(probe1StartAsString);
					int probe2Start = Integer.valueOf(probe2StartAsString);
					result = Integer.compare(probe1Start, probe2Start);
				} catch (NumberFormatException e) {
					logger.warn("Unable to parse probe ids[" + probeId1 + "],[" + probeId2 + "].");
				}

				if (result == 0) {
					result = probeId1.compareTo(probeId2);
				}
			}

			return result;
		}
	}

	/**
	 * Does the work of filtering by UID and extending reads to primers.
	 */
	private class PrimerReadExtensionAndFilteringOfUniquePcrProbesTask implements Runnable {

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
						applicationSettings.isUseStrictReadToProbeMatching());

				List<IReadPair> uniqueReads = probeReductionResults.getUniqueReadPairs();
				List<IReadPair> duplicateReads = probeReductionResults.getDuplicateReadPairs();

				List<IReadPair> readsToWrite = new ArrayList<IReadPair>();

				int numberOfUniqueReadPairsUnableExtendPrimer = 0;
				int numberOfDuplicateReadPairsUnableExtendPrimer = 0;

				ExtendReadResults extendUniqueReadResults = ExtendReadsToPrimer.extendReadsToPrimers(probe, uniqueReads, alignmentScorer);
				readsToWrite.addAll(extendUniqueReadResults.getExtendedReads());
				numberOfUniqueReadPairsUnableExtendPrimer = extendUniqueReadResults.getUnableToExtendReads().size();

				// need to keep track of these for reporting
				for (IReadPair readPair : extendUniqueReadResults.getUnableToExtendReads()) {
					PrimerReadExtensionAndPcrDuplicateIdentification.this.unableToExtendReads.add(readPair.getReadName());
				}

				// need to keep track of these for reporting
				PrimerReadExtensionAndPcrDuplicateIdentification.this.numberOfUniqueReadPairs.addAndGet(extendUniqueReadResults.getExtendedReads().size());

				if (applicationSettings.isKeepDuplicates() || applicationSettings.isMarkDuplicates()) {
					ExtendReadResults extendDuplicateReadResults = ExtendReadsToPrimer.extendReadsToPrimers(probe, duplicateReads, alignmentScorer);
					readsToWrite.addAll(extendDuplicateReadResults.getExtendedReads());
					numberOfDuplicateReadPairsUnableExtendPrimer = extendDuplicateReadResults.getUnableToExtendReads().size();

					PrimerReadExtensionAndPcrDuplicateIdentification.this.numberOfDuplicateReadPairs.addAndGet(extendDuplicateReadResults.getExtendedReads().size());
				} else {
					PrimerReadExtensionAndPcrDuplicateIdentification.this.numberOfDuplicateReadPairs.addAndGet(duplicateReads.size());
				}

				ProbeProcessingStats probeProcessingStats = probeReductionResults.getProbeProcessingStats();
				probeProcessingStats.setNumberOfUniqueReadPairsUnableToExtendPrimer(numberOfUniqueReadPairsUnableExtendPrimer);
				probeProcessingStats.setNumberOfDuplicateReadPairsUnableToExtendPrimer(numberOfDuplicateReadPairsUnableExtendPrimer);
				probeProcessingStatsByProbeId.put(probe.getProbeId(), probeProcessingStats);

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
		private void writeReadsToSamFile(SAMFileWriter samWriter, List<IReadPair> readPairs, boolean mergePairs) {
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

		private SAMRecord mergePairs(IReadPair readPair) {
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

			if (!readPair.getExtensionUid().isEmpty()) {
				SAMRecordUtil.setSamRecordExtensionUidAttribute(mergedRecord, readPair.getExtensionUid());
			}
			if (!readPair.getLigationUid().isEmpty()) {
				SAMRecordUtil.setSamRecordLigationUidAttribute(mergedRecord, readPair.getLigationUid());
			}
			if (!readPair.getProbeId().isEmpty()) {
				SAMRecordUtil.setSamRecordProbeIdAttribute(mergedRecord, readPair.getProbeId());
			}
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

		private MergeInformation(ISequence overlappingSequence, String overlappingQuality) {
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

	public static class ReadNameDetails {
		private final String commonReadNameBeginning;
		private final Set<Character> charactersExcludingCommonBeginning;

		private ReadNameDetails(String commonReadNameBeginning, Set<Character> charactersExcludingCommonBeginning) {
			super();
			this.commonReadNameBeginning = commonReadNameBeginning;
			this.charactersExcludingCommonBeginning = charactersExcludingCommonBeginning;
		}

		public String getCommonReadNameBeginning() {
			return commonReadNameBeginning;
		}

		public Set<Character> getCharacterLibraryExcludingCommonBeginning() {
			return charactersExcludingCommonBeginning;
		}
	}

	public static ReadNameDetails verifyReadNamesCanBeHandledByDedupAndFindCommonReadNameBeginning(File inputFastqOne, File inputFastqTwo) {
		long start = System.currentTimeMillis();
		String currentCommonBeginning = null;
		Set<Character> uniqueCharacters = new HashSet<Character>();
		int fastqEntryIndex = 0;
		try (FastqReader fastQOneReader = new FastqReader(inputFastqOne)) {
			try (FastqReader fastQTwoReader = new FastqReader(inputFastqTwo)) {
				while (fastQOneReader.hasNext() && fastQTwoReader.hasNext()) {

					FastqRecord oneRecord = fastQOneReader.next();
					FastqRecord twoRecord = fastQTwoReader.next();
					String readNameOne = oneRecord.getReadHeader();
					String readNameTwo = twoRecord.getReadHeader();

					if (currentCommonBeginning == null) {
						currentCommonBeginning = readNameOne;
					} else {
						int i = 0;
						while (i < readNameOne.length() && i < currentCommonBeginning.length() && readNameOne.charAt(i) == currentCommonBeginning.charAt(i)) {
							i++;
						}
						currentCommonBeginning = currentCommonBeginning.substring(0, i);

						i = 0;
						while (i < readNameTwo.length() && i < currentCommonBeginning.length() && readNameTwo.charAt(i) == currentCommonBeginning.charAt(i)) {
							i++;
						}
						currentCommonBeginning = currentCommonBeginning.substring(0, i);
					}

					String uniqueReadNameOne = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(currentCommonBeginning, readNameOne);
					String uniqueReadNameTwo = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(currentCommonBeginning, readNameTwo);
					if (!uniqueReadNameOne.equals(uniqueReadNameTwo)) {
						int lineNumber = (fastqEntryIndex * 4) + 1;
						throw new IllegalStateException("The read names[" + readNameOne + "][" + readNameTwo + "] found at line[" + lineNumber + "] in fastqOne[" + inputFastqOne.getAbsolutePath()
								+ "] and fastqTwo[" + inputFastqTwo.getAbsolutePath() + "] respectively are not valid Illumina read names.");
					}

					for (int i = 0; i < uniqueReadNameOne.length(); i++) {
						uniqueCharacters.add(uniqueReadNameOne.charAt(i));
					}

					fastqEntryIndex++;
				}
			}
		}
		long end = System.currentTimeMillis();
		logger.info("Verified that the read names can be handled by dedup and found the common beginning for all read names[" + currentCommonBeginning + "] in "
				+ DateUtil.convertMillisecondsToHHMMSS(end - start) + ".");
		return new ReadNameDetails(currentCommonBeginning, uniqueCharacters);
	}
}
