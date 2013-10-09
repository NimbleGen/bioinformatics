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
import java.io.PrintWriter;
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
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceDictionary;
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
import com.roche.heatseq.qualityreport.NucleotideCompositionUtil;
import com.roche.heatseq.qualityreport.SummaryReport;
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

	public final static String DETAILS_REPORT_NAME = "processing_details.txt";
	public final static String SUMMARY_REPORT_NAME = PrefuppCli.APPLICATION_NAME + "_summary.txt";
	private final static String EXTENSION_ERRORS_REPORT_NAME = "extension_errors.txt";
	public final static String PROBE_UID_QUALITY_REPORT_NAME = "probe_uid_quality.txt";
	public final static String UNABLE_TO_ALIGN_PRIMER_REPORT_NAME = "unable_to_align_primer_for_variable_length_uid.txt";
	public final static String UNABLE_TO_MAP_FASTQ_ONE_REPORT_NAME = "unable_to_map_one.fastq";
	public final static String UNABLE_TO_MAP_FASTQ_TWO_REPORT_NAME = "unable_to_map_two.fastq";
	public final static String PRIMER_ALIGNMENT_REPORT_NAME = "extension_primer_alignment.txt";
	public final static String UNIQUE_PROBE_TALLIES_REPORT_NAME = "unique_probe_tallies.txt";
	public final static String PROBE_COVERAGE_REPORT_NAME = "probe_coverage.bed";
	public final static String MAPPED_OFF_TARGET_READS_REPORT_NAME = "mapped_off_target_reads.bed";

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

		// Set up the reports files
		File detailsReportFile = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix() + DETAILS_REPORT_NAME);
		File summaryReportFile = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix() + SUMMARY_REPORT_NAME);
		File extensionErrorsReportFile = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix() + EXTENSION_ERRORS_REPORT_NAME);
		File probeUidQualityReportFile = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix() + PROBE_UID_QUALITY_REPORT_NAME);
		File unableToAlignPrimerReportFile = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix() + UNABLE_TO_ALIGN_PRIMER_REPORT_NAME);
		File primerAlignmentReportFile = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix() + PRIMER_ALIGNMENT_REPORT_NAME);
		File uniqueProbeTalliesReportFile = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix() + UNIQUE_PROBE_TALLIES_REPORT_NAME);
		File probeCoverageReportFile = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix() + PROBE_COVERAGE_REPORT_NAME);
		File mappedOffTargetReadsReportFile = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix() + MAPPED_OFF_TARGET_READS_REPORT_NAME);

		logger.debug("Creating details report file at " + detailsReportFile.getAbsolutePath());

		DetailsReport detailsReport = null;
		SummaryReport summaryReport = null;
		PrintWriter extensionErrorsWriter = null;
		TabDelimitedFileWriter probeUidQualityWriter = null;
		TabDelimitedFileWriter unableToAlignPrimerWriter = null;
		TabDelimitedFileWriter primerAlignmentWriter = null;
		TabDelimitedFileWriter uniqueProbeTalliesWriter = null;
		TabDelimitedFileWriter probeCoverageWriter = null;
		TabDelimitedFileWriter mappedOffTargetReadsWriter = null;

		if (applicationSettings.isShouldOutputQualityReports()) {
			try {
				detailsReport = new DetailsReport(detailsReportFile);
				summaryReport = new SummaryReport(summaryReportFile, applicationSettings.getUidLength());
				FileUtil.createNewFile(extensionErrorsReportFile);
				extensionErrorsWriter = new PrintWriter(extensionErrorsReportFile);

				FileUtil.createNewFile(probeUidQualityReportFile);
				probeUidQualityWriter = new TabDelimitedFileWriter(probeUidQualityReportFile, new String[] { "probe_id", "probe_sequence_name", "probe_capture_start", "probe_capture_stop", "strand",
						"uid", "read_one_quality", "read_two_quality", "total_quality", "read_name" });

				unableToAlignPrimerWriter = new TabDelimitedFileWriter(unableToAlignPrimerReportFile, new String[] { "probe_id", "sequence_name", "probe_start", "probe_stop",
						"extension_primer_sequence", "read_name", "read_string" });

				FileUtil.createNewFile(primerAlignmentReportFile);
				primerAlignmentWriter = new TabDelimitedFileWriter(primerAlignmentReportFile, new String[] { "uid_length", "substituions", "insertions", "deletions", "edit_distance", "read",
						"extension_primer", "probe_name", "probe_capture_start", "probe_capture_stop", "probe_strand" });

				FileUtil.createNewFile(uniqueProbeTalliesReportFile);
				uniqueProbeTalliesWriter = new TabDelimitedFileWriter(uniqueProbeTalliesReportFile);

				FileUtil.createNewFile(probeCoverageReportFile);
				probeCoverageWriter = new TabDelimitedFileWriter(probeCoverageReportFile);

				FileUtil.createNewFile(mappedOffTargetReadsReportFile);
				mappedOffTargetReadsWriter = new TabDelimitedFileWriter(mappedOffTargetReadsReportFile);

			} catch (IOException e) {
				throw new IllegalStateException("Could not create report file.", e);
			}
		}

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

		// Actually do the work
		filterBamEntriesByUidAndExtendReadsToPrimers(applicationSettings, probeInfo, detailsReport, summaryReport, extensionErrorsWriter, probeUidQualityWriter, unableToAlignPrimerWriter,
				primerAlignmentWriter, uniqueProbeTalliesWriter, probeCoverageWriter, mappedOffTargetReadsWriter);

		// Clean up the reports
		if (summaryReport != null) {
			summaryReport.close();
		}
		if (detailsReport != null) {
			detailsReport.close();
		}
		if (extensionErrorsWriter != null) {
			extensionErrorsWriter.close();
		}
		if (probeUidQualityWriter != null) {
			probeUidQualityWriter.close();
		}
		if (unableToAlignPrimerWriter != null) {
			unableToAlignPrimerWriter.close();
		}
		if (primerAlignmentWriter != null) {
			primerAlignmentWriter.close();
		}
		if (uniqueProbeTalliesWriter != null) {
			uniqueProbeTalliesWriter.close();
		}
		if (probeCoverageReportFile != null) {
			probeCoverageWriter.close();
		}

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
	private static void filterBamEntriesByUidAndExtendReadsToPrimers(ApplicationSettings applicationSettings, ProbesBySequenceName probeInfo, DetailsReport detailsReport, SummaryReport summaryReport,
			PrintWriter extensionErrorsWriter, TabDelimitedFileWriter probeUidQualityWriter, TabDelimitedFileWriter unableToAlignPrimerWriter, TabDelimitedFileWriter primerAlignmentWriter,
			TabDelimitedFileWriter uniqueProbeTalliesWriter, TabDelimitedFileWriter probeCoverageWriter, TabDelimitedFileWriter mappedOffTargetReadsWriter) {
		long start = System.currentTimeMillis();

		Set<String> sequenceNames = probeInfo.getSequenceNames();

		TallyMap<String> readNamesToDistinctProbeAssignmentCount = new TallyMap<String>();
		Set<ISequence> distinctUids = Collections.newSetFromMap(new ConcurrentHashMap<ISequence, Boolean>());

		SAMFileWriter samWriter = null;

		try (SAMFileReader samReader = new SAMFileReader(applicationSettings.getBamFile(), applicationSettings.getBamFileIndex())) {

			if (summaryReport != null) {
				SamReadCount readCount = SAMRecordUtil.countReads(samReader);
				summaryReport.setUnmappedReads(readCount.getTotalUnmappedReads());
				summaryReport.setMappedReads(readCount.getTotalMappedReads());
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
					getHeader(samReader.getFileHeader(), probeInfo, applicationSettings.getCommandLineSignature(), applicationSettings.getProgramName(), applicationSettings.getProgramVersion()),
					false, outputUnsortedBamFile, 0);

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
						samRecordIter = samReader.queryContained(sequenceName, probe.getCaptureTargetStart(), probe.getCaptureTargetStop());
					} else {
						samRecordIter = samReader.queryContained(sequenceName, probe.getStart(), probe.getStop());
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

					Runnable worker = new PrimerReadExtensionAndFilteringOfUniquePcrProbesTask(probe, applicationSettings, samWriter, extensionErrorsWriter, probeUidQualityWriter, detailsReport,
							unableToAlignPrimerWriter, primerAlignmentWriter, uniqueProbeTalliesWriter, probeCoverageWriter, fastqOneWriter, fastqTwoWriter, readNameToCompleteRecordsMap,
							applicationSettings.getAlignmentScorer(), distinctUids);

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

			SAMRecordIterator samRecordIter = samReader.iterator();
			while (samRecordIter.hasNext()) {
				SAMRecord record = samRecordIter.next();
				Set<String> mappedReadNames = readNamesToDistinctProbeAssignmentCount.getTalliesAsMap().keySet();
				String readName = record.getReadName();
				if (!record.getReadUnmappedFlag() && !mappedReadNames.contains(readName)) {
					String strandString = "+";
					if (record.getReadNegativeStrandFlag()) {
						strandString = "-";
					}
					mappedOffTargetReadsWriter.writeLine(record.getReferenceName(), record.getAlignmentStart(), record.getAlignmentEnd(), readName, "", strandString);
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

			if (summaryReport != null) {
				summaryReport.setUidComposition(NucleotideCompositionUtil.getNucleotideComposition(distinctUids));
				summaryReport.setUidCompositionByBase(NucleotideCompositionUtil.getNucleotideCompositionByPosition(distinctUids));
				summaryReport.setProcessingTimeInMs(end - start);
				summaryReport.setDuplicateReadPairsRemoved(detailsReport.getDuplicateReadPairsRemoved());
				summaryReport.setProbesWithNoMappedReadPairs(detailsReport.getProbesWithNoMappedReadPairs());
				summaryReport.setTotalReadPairsAfterReduction(detailsReport.getTotalReadPairsAfterReduction());

				summaryReport.setAverageUidsPerProbe(detailsReport.getAverageNumberOfUidsPerProbe());
				summaryReport.setAverageUidsPerProbeWithReads(detailsReport.getAverageNumberOfUidsPerProbeWithAssignedReads());
				summaryReport.setMaxUidsPerProbe(detailsReport.getMaxNumberOfUidsPerProbe());
				summaryReport.setAverageNumberOfReadPairsPerProbeUid(detailsReport.getAverageNumberOfReadPairsPerProbeUid());

				int readPairsAssignedToMultipleProbes = 0;
				for (int counts : readNamesToDistinctProbeAssignmentCount.getTalliesAsMap().values()) {
					if (counts > 1) {
						readPairsAssignedToMultipleProbes++;
					}
				}
				summaryReport.setReadPairsAssignedToMultipleProbes(readPairsAssignedToMultipleProbes);
				summaryReport.setDistinctUidsFound(distinctUids.size());
				summaryReport.setTotalProbes(totalProbes);
			}
		}
	}

	/**
	 * Creates a new file header for our output BAM file
	 * 
	 * @param originalHeader
	 * @param probeInfo
	 * @param commandLineSignature
	 * @param programName
	 * @param programVersion
	 * @return
	 */
	private static SAMFileHeader getHeader(SAMFileHeader originalHeader, ProbesBySequenceName probeInfo, String commandLineSignature, String programName, String programVersion) {
		SAMFileHeader newHeader = new SAMFileHeader();

		newHeader.setReadGroups(originalHeader.getReadGroups());
		List<SAMProgramRecord> programRecords = new ArrayList<SAMProgramRecord>(originalHeader.getProgramRecords());

		String uniqueProgramGroupId = programName + "_" + DateUtil.getCurrentDateINYYYY_MM_DD_HH_MM_SS();
		SAMProgramRecord programRecord = new SAMProgramRecord(uniqueProgramGroupId);
		programRecord.setProgramName(programName);
		programRecord.setProgramVersion(programVersion);
		programRecord.setCommandLine(commandLineSignature);
		programRecords.add(programRecord);
		newHeader.setProgramRecords(programRecords);

		SAMSequenceDictionary sequenceDictionary = new SAMSequenceDictionary();
		for (SAMSequenceRecord oldSequenceRecord : originalHeader.getSequenceDictionary().getSequences()) {
			if (probeInfo.containsSequenceName(oldSequenceRecord.getSequenceName())) {
				SAMSequenceRecord newSequenceRecord = new SAMSequenceRecord(oldSequenceRecord.getSequenceName(), oldSequenceRecord.getSequenceLength());
				sequenceDictionary.addSequence(newSequenceRecord);
			}
		}
		newHeader.setSequenceDictionary(sequenceDictionary);
		return newHeader;
	}

	/**
	 * Does the work of filtering by UID and extending reads to primers.
	 */
	private static class PrimerReadExtensionAndFilteringOfUniquePcrProbesTask implements Runnable {

		private final Probe probe;
		private final ApplicationSettings applicationSettings;
		private final SAMFileWriter samWriter;
		private final PrintWriter extensionErrorsWriter;
		private final TabDelimitedFileWriter probeUidQualityWriter;
		private final DetailsReport detailsReport;
		private final TabDelimitedFileWriter unableToAlignPrimerWriter;
		private final TabDelimitedFileWriter primerAlignmentWriter;
		private final TabDelimitedFileWriter uniqueProbeTalliesWriter;
		private final TabDelimitedFileWriter probeCoverageWriter;
		private final FastqWriter fastqOneWriter;
		private final FastqWriter fastqTwoWriter;
		private final IAlignmentScorer alignmentScorer;
		private final Set<ISequence> distinctUids;
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

		PrimerReadExtensionAndFilteringOfUniquePcrProbesTask(Probe probe, ApplicationSettings applicationSettings, SAMFileWriter samWriter, PrintWriter extensionErrorsWriter,
				TabDelimitedFileWriter probeUidQualityWriter, DetailsReport detailsReport, TabDelimitedFileWriter unableToAlignPrimerWriter, TabDelimitedFileWriter primerAlignmentWriter,
				TabDelimitedFileWriter uniqueProbeTalliesWriter, TabDelimitedFileWriter probeCoverageWriter, FastqWriter fastqOneWriter, FastqWriter fastqTwoWriter,
				Map<String, SAMRecordPair> readNameToRecordsMap, IAlignmentScorer alignmentScorer, Set<ISequence> distinctUids) {
			this.probe = probe;
			this.applicationSettings = applicationSettings;
			this.samWriter = samWriter;
			this.extensionErrorsWriter = extensionErrorsWriter;
			this.probeUidQualityWriter = probeUidQualityWriter;
			this.detailsReport = detailsReport;
			this.unableToAlignPrimerWriter = unableToAlignPrimerWriter;
			this.primerAlignmentWriter = primerAlignmentWriter;
			this.uniqueProbeTalliesWriter = uniqueProbeTalliesWriter;
			this.probeCoverageWriter = probeCoverageWriter;
			this.fastqOneWriter = fastqOneWriter;
			this.fastqTwoWriter = fastqTwoWriter;
			this.readNameToRecordsMap = readNameToRecordsMap;
			this.alignmentScorer = alignmentScorer;
			this.distinctUids = distinctUids;
		}

		/**
		 * Actually performs the reduction by UID and extension to the primers
		 */
		@Override
		public void run() {
			try {
				UidReductionResultsForAProbe probeReductionResults = FilterByUid.reduceProbesByUid(probe, readNameToRecordsMap, probeUidQualityWriter, unableToAlignPrimerWriter,
						primerAlignmentWriter, uniqueProbeTalliesWriter, probeCoverageWriter, applicationSettings.isAllowVariableLengthUids(), alignmentScorer, distinctUids);
				synchronized (detailsReport) {
					if (detailsReport != null) {
						detailsReport.writeEntry(probeReductionResults.getProbeProcessingStats());
					} else {
						detailsReport.writeBlankEntry(probe);
					}
				}

				List<IReadPair> readsToWrite = ExtendReadsToPrimer.extendReadsToPrimers(probe, probeReductionResults.getReadPairs(), extensionErrorsWriter, alignmentScorer);

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
