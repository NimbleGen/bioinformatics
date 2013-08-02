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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.roche.heatseq.objects.ProbesByContainerName;
import com.roche.heatseq.objects.SAMRecordPair;
import com.roche.heatseq.objects.UidReductionResultsForAProbe;
import com.roche.heatseq.qualityreport.DetailsReport;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

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

	public final static String REPORT_DIRECTORY = "/reports/";
	public final static String DETAILS_REPORT_NAME = "processing_details.txt";
	private final static String EXTENSION_ERRORS_REPORT_NAME = "extension_errors.txt";
	public final static String PROBE_UID_QUALITY_REPORT_NAME = "probe_uid_quality.txt";
	public final static String UNABLE_TO_ALIGN_PRIMER_REPORT_NAME = "unable_to_align_primer_for_variable_length_uid.txt";
	public final static String UNABLE_TO_MAP_FASTQ_ONE_REPORT_NAME = "unable_to_map_one.fastq";
	public final static String UNABLE_TO_MAP_FASTQ_TWO_REPORT_NAME = "unable_to_map_two.fastq";
	public final static String PRIMER_ALIGNMENT_REPORT_NAME = "extension_primer_alignment.txt";

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
		File detailsReportFile = new File(applicationSettings.getOutputDirectory(), REPORT_DIRECTORY + applicationSettings.getOutputFilePrefix() + DETAILS_REPORT_NAME);
		File extensionErrorsReportFile = new File(applicationSettings.getOutputDirectory(), REPORT_DIRECTORY + applicationSettings.getOutputFilePrefix() + EXTENSION_ERRORS_REPORT_NAME);
		File probeUidQualityReportFile = new File(applicationSettings.getOutputDirectory(), REPORT_DIRECTORY + applicationSettings.getOutputFilePrefix() + PROBE_UID_QUALITY_REPORT_NAME);
		File unableToAlignPrimerReportFile = new File(applicationSettings.getOutputDirectory(), REPORT_DIRECTORY + applicationSettings.getOutputFilePrefix() + UNABLE_TO_ALIGN_PRIMER_REPORT_NAME);
		File primerAlignmentReportFile = new File(applicationSettings.getOutputDirectory(), REPORT_DIRECTORY + applicationSettings.getOutputFilePrefix() + PRIMER_ALIGNMENT_REPORT_NAME);

		logger.debug("Creating details report file at " + detailsReportFile.getAbsolutePath());

		DetailsReport detailsReport = null;
		PrintWriter extensionErrorsWriter = null;
		PrintWriter probeUidQualityWriter = null;
		PrintWriter unableToAlignPrimerWriter = null;
		PrintWriter primerAlignmentWriter = null;

		if (applicationSettings.isShouldOutputQualityReports()) {
			try {
				detailsReport = new DetailsReport(detailsReportFile);

				FileUtil.createNewFile(extensionErrorsReportFile);
				extensionErrorsWriter = new PrintWriter(extensionErrorsReportFile);

				FileUtil.createNewFile(probeUidQualityReportFile);
				probeUidQualityWriter = new PrintWriter(probeUidQualityReportFile);
				probeUidQualityWriter.println("probe_id" + StringUtil.TAB + "probe_container" + StringUtil.TAB + "probe_capture_start" + StringUtil.TAB + "probe_capture_stop" + StringUtil.TAB
						+ "strand" + StringUtil.TAB + "uid" + StringUtil.TAB + "read_one_quality" + StringUtil.TAB + "read_two_quality" + StringUtil.TAB + "total_quality" + StringUtil.TAB
						+ "read_name");

				FileUtil.createNewFile(unableToAlignPrimerReportFile);
				unableToAlignPrimerWriter = new PrintWriter(unableToAlignPrimerReportFile);
				unableToAlignPrimerWriter.println("container_name" + StringUtil.TAB + "probe_start" + StringUtil.TAB + "protbe_stop" + StringUtil.TAB + "extension_primer_sequence" + StringUtil.TAB
						+ "read_name" + StringUtil.TAB + "read_string");

				FileUtil.createNewFile(primerAlignmentReportFile);
				primerAlignmentWriter = new PrintWriter(primerAlignmentReportFile);
				primerAlignmentWriter.println("uid_length" + StringUtil.TAB + "substituions" + StringUtil.TAB + "insertions" + StringUtil.TAB + "deletions" + StringUtil.TAB + "edit_distance"
						+ StringUtil.TAB + "read" + StringUtil.TAB + "extension_primer" + StringUtil.TAB);
			} catch (IOException e) {
				throw new IllegalStateException("Could not create details report file[" + detailsReportFile.getAbsolutePath() + "].");
			}
		}

		// Parse the input probe file
		ProbesByContainerName probeInfo = null;
		try {
			probeInfo = ProbeFileUtil.parseProbeInfoFile(applicationSettings.getProbeFile());
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}

		if (probeInfo == null) {
			throw new IllegalStateException("Unable to parse probe info file[" + applicationSettings.getProbeFile() + "].");
		}

		// Actually do the work
		filterBamEntriesByUidAndExtendReadsToPrimers(applicationSettings, probeInfo, detailsReport, extensionErrorsWriter, probeUidQualityWriter, unableToAlignPrimerWriter, primerAlignmentWriter);

		// Clean up the reports
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

		long stop = System.currentTimeMillis();

		// Report on performance
		logger.debug("Total time:" + (stop - start) + "ms");
	}

	/**
	 * Get reads for each probe from a merged BAM file, determine which read to use for each UID, extend the reads to the target primers, and output the reduced and extended reads to a new BAM file.
	 * 
	 * @param applicationSettings
	 *            The context the application is running under
	 * @param probeInfo
	 *            All the probes in the input probe file, by container
	 * @param detailReportWriter
	 *            Writer for reporting detailed processing information
	 * @param extensionErrorsWriter
	 *            Writer for reporting errors detected when extending the reads to the primers
	 * @param probeUidQualityWriter
	 *            Writer for reporting quality per UID
	 */
	private static void filterBamEntriesByUidAndExtendReadsToPrimers(ApplicationSettings applicationSettings, ProbesByContainerName probeInfo, DetailsReport detailsReport,
			PrintWriter extensionErrorsWriter, PrintWriter probeUidQualityWriter, PrintWriter unableToAlignPrimerWriter, PrintWriter primerAlignmentWriter) {
		Set<String> containerNames = probeInfo.getContainerNames();

		SAMFileWriter samWriter = null;

		try (SAMFileReader samReader = new SAMFileReader(applicationSettings.getBamFile(), applicationSettings.getBamFileIndex())) {

			String outputUnsortedBamFileName = FileUtil.getFileNameWithoutExtension(applicationSettings.getOriginalBamFileName()) + "_UNSORTED_REDUCED."
					+ FileUtil.getFileExtension(applicationSettings.getBamFile());
			File outputUnsortedBamFile = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix() + outputUnsortedBamFileName);

			String outputSortedBamFileName = applicationSettings.getOutputBamFileName();
			File outputSortedBamFile = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix() + outputSortedBamFileName);

			String outputBamIndexFileName = outputSortedBamFileName + ".bai";
			File outputBamIndexFile = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix() + outputBamIndexFileName);

			try {
				outputUnsortedBamFile.createNewFile();
				outputSortedBamFile.createNewFile();
				outputBamIndexFile.createNewFile();
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}

			samWriter = new SAMFileWriterFactory().makeSAMOrBAMWriter(
					getHeader(samReader.getFileHeader(), probeInfo, applicationSettings.getCommandLineSignature(), applicationSettings.getProgramName(), applicationSettings.getProgramVersion()),
					false, outputUnsortedBamFile);

			FastqWriter fastqOneWriter = null;
			FastqWriter fastqTwoWriter = null;
			if (applicationSettings.isShouldOutputFastq()) {
				File fastqOne = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix() + applicationSettings.getOriginalBamFileName() + "_one.fastq");
				File fastqTwo = new File(applicationSettings.getOutputDirectory(), applicationSettings.getOutputFilePrefix() + applicationSettings.getOriginalBamFileName() + "_two.fastq");
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
			for (String containerName : containerNames) {

				if (!referenceSequenceNamesInBam.contains(containerName)) {
					throw new IllegalStateException(
							"Chromosome/Container["
									+ containerName
									+ "] from probe file is not present as a reference sequence in the bam file.  Please make sure your probe container/chromosome names match bam file reference sequence names.");
				}

				List<Probe> probes = probeInfo.getProbesByContainerName(containerName);

				int totalProbes = probes.size();
				logger.debug("Beginning processing " + containerName + " with " + totalProbes + " PROBES ");

				for (Probe probe : probes) {
					int referenceSequenceIndex = referenceSequenceNamesInBam.indexOf(containerName);
					int referenceSequenceLength = referenceSequenceLengthsInBam.get(referenceSequenceIndex);
					if (referenceSequenceLength < probe.getStop()) {
						throw new IllegalStateException("Probe Chromosome/Container[" + containerName + "] start[" + probe.getStart() + "] stop[" + probe.getStop() + "] found in the probe file["
								+ applicationSettings.getProbeFile().getAbsolutePath() + "] is outside of the length[" + referenceSequenceLength + "] of reference sequence[" + containerName
								+ "] found in the bam file.");
					}

					// Try getting the reads for this probe here before passing them to the worker
					Map<String, SAMRecordPair> readNameToRecordsMap = new HashMap<String, SAMRecordPair>();
					SAMRecordIterator samRecordIter = samReader.queryContained(containerName, probe.getStart(), probe.getStop());
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

					Runnable worker = new PrimerReadExtensionAndFilteringOfUniquePcrProbesTask(probe, applicationSettings, samWriter, extensionErrorsWriter, probeUidQualityWriter, detailsReport,
							unableToAlignPrimerWriter, primerAlignmentWriter, fastqOneWriter, fastqTwoWriter, readNameToRecordsMap);
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
			samReader.close();

			BamFileUtil.sortOnCoordinates(outputUnsortedBamFile, outputSortedBamFile);

			outputUnsortedBamFile.delete();
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
	private static SAMFileHeader getHeader(SAMFileHeader originalHeader, ProbesByContainerName probeInfo, String commandLineSignature, String programName, String programVersion) {
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
			if (probeInfo.containsContainerName(oldSequenceRecord.getSequenceName())) {
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
		private final PrintWriter probeUidQualityWriter;
		private final DetailsReport detailsReport;
		private final PrintWriter unableToAlignPrimerWriter;
		private final PrintWriter primerAlignmentWriter;
		private final FastqWriter fastqOneWriter;
		private final FastqWriter fastqTwoWriter;
		Map<String, SAMRecordPair> readNameToRecordsMap;

		/**
		 * All the information we need to filter by UID and extend reads to primers. We provide thread safety by synchronizing on the writers for all blocks that should write data for one probe.
		 * 
		 * @param probe
		 *            The probe we're processing information for
		 * @param containerName
		 *            The reference container the probe is part of
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
				PrintWriter probeUidQualityWriter, DetailsReport detailsReport, PrintWriter unableToAlignPrimerWriter, PrintWriter primerAlignmentWriter, FastqWriter fastqOneWriter,
				FastqWriter fastqTwoWriter, Map<String, SAMRecordPair> readNameToRecordsMap) {
			this.probe = probe;
			this.applicationSettings = applicationSettings;
			this.samWriter = samWriter;
			this.extensionErrorsWriter = extensionErrorsWriter;
			this.probeUidQualityWriter = probeUidQualityWriter;
			this.detailsReport = detailsReport;
			this.unableToAlignPrimerWriter = unableToAlignPrimerWriter;
			this.primerAlignmentWriter = primerAlignmentWriter;
			this.fastqOneWriter = fastqOneWriter;
			this.fastqTwoWriter = fastqTwoWriter;
			this.readNameToRecordsMap = readNameToRecordsMap;
		}

		/**
		 * Actually performs the reduction by UID and extension to the primers
		 */
		@Override
		public void run() {
			try {
				UidReductionResultsForAProbe probeReductionResults = FilterByUid.reduceProbesByUid(probe, readNameToRecordsMap, probeUidQualityWriter, unableToAlignPrimerWriter,
						primerAlignmentWriter, applicationSettings.isAllowVariableLengthUids());

				if (detailsReport != null) {
					synchronized (detailsReport) {
						detailsReport.writeEntry(probeReductionResults.getProbeProcessingStats());
					}
				}

				List<IReadPair> readsToWrite = null;
				if (applicationSettings.isShouldExtendReads()) {
					List<IReadPair> extendedReads = ExtendReadsToPrimer.extendReadsToPrimers(probe, probeReductionResults.getReadPairs(), extensionErrorsWriter);
					readsToWrite = extendedReads;
				} else {
					readsToWrite = probeReductionResults.getReadPairs();
				}

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
