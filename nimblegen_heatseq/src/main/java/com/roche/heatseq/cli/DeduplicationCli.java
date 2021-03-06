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

package com.roche.heatseq.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.ApplicationSettings;
import com.roche.heatseq.process.BamFileValidator;
import com.roche.heatseq.process.FastqReadTrimmer;
import com.roche.heatseq.process.FastqReadTrimmer.ProbeTrimmingInformation;
import com.roche.heatseq.process.FastqValidator;
import com.roche.heatseq.process.InputFilesExistValidator;
import com.roche.heatseq.process.PrimerReadExtensionAndPcrDuplicateIdentification;
import com.roche.heatseq.process.ReadNameTracking;
import com.roche.heatseq.utils.BamFileUtil;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.alignment.SimpleAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOption;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOptionsGroup;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.genome.GenomeIdentifier;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.LoggingUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ParsedProbeFile;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ProbeFileUtil;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ProbeFileUtil.ProbeHeaderInformation;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;

public class DeduplicationCli {
	private final static Logger logger = LoggerFactory.getLogger(DeduplicationCli.class);

	private final static int MAX_NUMBER_OF_PROCESSORS = 24;

	public final static int DEFAULT_EXTENSION_UID_LENGTH = 10;
	public final static int DEFAULT_LIGATION_UID_LENGTH = 0;
	private final static String BAM_EXTENSION = ".bam";

	private final static int BYTES_PER_MEGABYTE = 1000000;
	private final static DecimalFormat doubleFormatter = new DecimalFormat("#,###.##");

	final static CommandLineOption USAGE_OPTION = new CommandLineOption("Print Usage", "usage", 'h', "Print Usage.", false, true);
	public final static CommandLineOption FASTQ_ONE_OPTION = new CommandLineOption("FastQ One File", "r1", null,
			"Path to first input fastq file (as an uncompressed file with a fastq extension or a compressed file with the gz extension).", true, false);
	public final static CommandLineOption FASTQ_TWO_OPTION = new CommandLineOption("FastQ Two File", "r2", null,
			"Path to second input fastq file (as an uncompressed file with a fastq extension or a compressed file with the gz extension).", true, false);
	public final static CommandLineOption INPUT_BAM_OPTION = new CommandLineOption("Input BAM or SAM File Path", "inputBam", null, "Path to input BAM or SAM file containing the aligned reads.", true,
			false);
	public final static CommandLineOption PROBE_OPTION = new CommandLineOption("Probe Information File", "probe", null, "NimbleGen probe file.", true, false);
	final static CommandLineOption OUTPUT_DIR_OPTION = new CommandLineOption("Output Directory", "outputDir", null, "Location to store resultant files.", false, false);
	final static CommandLineOption OUTPUT_FILE_PREFIX_OPTION = new CommandLineOption("Output File Prefix", "outputPrefix", null, "Text to put at beginning of output file names.", false, false);
	private final static CommandLineOption TMP_DIR_OPTION = new CommandLineOption("Temporary Directory", "tmpDir", null, "Location to store temporary files.", false, false);
	private final static CommandLineOption NUM_PROCESSORS_OPTION = new CommandLineOption("Number of Processors", "numProcessors", null,
			"The number of threads to run in parallel.  If not specified this will default to the number of cores available on the machine.  The designated value and default value will be capped at "
					+ MAX_NUMBER_OF_PROCESSORS + ".",
			false, false);
	private final static CommandLineOption OUTPUT_BAM_FILE_NAME_OPTION = new CommandLineOption("Output Bam File Name", "outputBamFileName", 'o', "Name for output bam file.", true, false);
	private final static CommandLineOption MATCH_SCORE_OPTION = new CommandLineOption("Match Score", "matchScore", null,
			"The score given to matching nucleotides when extending alignments to the primers. (Default: " + SimpleAlignmentScorer.DEFAULT_MATCH_SCORE + ")", false, false);
	private final static CommandLineOption MISMATCH_PENALTY_OPTION = new CommandLineOption("Mismatch Penalty", "mismatchPenalty", null,
			"The penalty subtracted for mismatched nucleotides when extending alignments to the primers. (Default: " + SimpleAlignmentScorer.DEFAULT_MISMATCH_PENALTY + ")", false, false);
	private final static CommandLineOption GAP_OPEN_PENALTY_OPTION = new CommandLineOption("Gap Open Penalty", "gapOpenPenalty", null,
			"The penalty for opening a gap when extending alignments to the primers. (Default: " + SimpleAlignmentScorer.DEFAULT_GAP_OPEN_PENALTY + ")", false, false);
	private final static CommandLineOption GAP_EXTEND_PENALTY_OPTION = new CommandLineOption("Gap Extend Penalty", "gapExtendPenalty", null,
			"The penalty for extending a gap when extending alignments to the primers. (Default: " + SimpleAlignmentScorer.DEFAULT_GAP_EXTEND_PENALTY + ")", false, false);
	// public final static CommandLineOption LENIENT_VALIDATION_STRINGENCY_OPTION = new CommandLineOption("Lenient Validation Stringency", "lenientValidation", null,
	// "Use a lenient validation stringency for all SAM files read by this program.", false, true);
	private final static CommandLineOption MARK_DUPLICATES_OPTION = new CommandLineOption("Mark Duplicates", "markDuplicates", null, "Mark duplicate reads in the bam file instead of removing them.",
			false, true);
	private final static CommandLineOption KEEP_DUPLICATES_OPTION = new CommandLineOption("Disable Duplicate Removal", "disableDuplicateRemoval", null,
			"Keep duplicate reads in the bam file instead of removing them. ", false, true, true);
	private final static CommandLineOption MERGE_PAIRS_OPTION = new CommandLineOption("Merge Pairs", "mergePairs", null, "Merge pairs using the highest quality base reads from each read.", false,
			true);
	public final static CommandLineOption TRIMMING_SKIPPED_OPTION = new CommandLineOption("Reads Were Not Trimmed Prior to Mapping", "readsNotTrimmed", null,
			"The reads were not trimmed prior to mapping.", false, true, true);
	private final static CommandLineOption INTERNAL_REPORTS_OPTION = new CommandLineOption("Output interal reports", "internalReports", null, "Output internal reports.", false, true, true);
	private final static CommandLineOption EXCLUDE_NEW_PROGRAM_IN_BAM_HEADER_OPTION = new CommandLineOption("Exclude Program in Bam Header", "excludeProgramInBamHeader", null,
			"Don not include a program entry for this application in the bam header.", false, true, true);
	private final static CommandLineOption SAVE_TEMP_OPTION = new CommandLineOption("Save Temp Files", "saveTemp", null, "Save temporary files.", false, true, true);
	private final static CommandLineOption READ_NAMES_TO_TRACK_FILE_OPTION = new CommandLineOption("Read Names To Track File", "trackReadNamesFile", null,
			"File with line delimited read names (or indexes) to track in the log.", false, false, true);
	final static CommandLineOption VERSION_OPTION = new CommandLineOption("Print Version", "version", null, "Print the version for this application.", false, true);

	// Note: these variables are for debugging purposes
	// saveTemporaryFiles default is false
	private static boolean saveTemporaryFiles = false;
	// allowVaraibleLengthUids default is false
	private final static boolean allowVariableLengthUids = false;
	// useStrictReadToProbeMatching default is false
	private final static boolean useStrictReadToProbeMatching = false;
	private final static boolean useLenientValidation = true;

	static void identifyDuplicates(ParsedCommandLine parsedCommandLine, String commandLineSignature, String applicationName, String applicationVersion) {

		long applicationStart = System.currentTimeMillis();
		CliStatusConsole.logStatus("Deduplication has started at " + DateUtil.convertTimeInMillisecondsToDate(applicationStart) + "(YYYY/MM/DD HH:MM:SS)." + StringUtil.NEWLINE);

		String outputDirectoryString = parsedCommandLine.getOptionsValue(OUTPUT_DIR_OPTION);
		File outputDirectory = null;
		if (outputDirectoryString != null) {
			outputDirectory = new File(outputDirectoryString);
			if (!outputDirectory.exists()) {
				try {
					FileUtil.createDirectory(outputDirectory);
				} catch (IOException e) {
					throw new IllegalStateException("Could not create provided output directory[" + outputDirectory.getAbsolutePath() + "].", e);
				}
			}
			if (!outputDirectory.isDirectory()) {
				throw new IllegalStateException("Provided output directory[" + outputDirectory.getAbsolutePath() + "] is not valid.");
			}
		} else {
			// current working directory
			outputDirectory = new File(".");
		}

		String outputFilePrefix = parsedCommandLine.getOptionsValue(OUTPUT_FILE_PREFIX_OPTION);
		String sampleName = "";
		if (outputFilePrefix == null) {
			outputFilePrefix = "";
		} else if (!outputFilePrefix.isEmpty() && outputFilePrefix.charAt(outputFilePrefix.length() - 1) != '_') {
			// Add an underscore as a separation character for the prefix if there is not already an underscore as the last prefix character
			sampleName = outputFilePrefix;
			outputFilePrefix = outputFilePrefix + "_";
		}

		File logFile = HsqUtilsCli.getLogFile(outputDirectory, outputFilePrefix, applicationName, "dedup");

		try {
			LoggingUtil.setLogFile(HsqUtilsCli.FILE_LOGGER_NAME, logFile);
		} catch (IOException e2) {
			throw new IllegalStateException("Unable to create log file at " + logFile.getAbsolutePath() + ".", e2);
		}
		logger.info(applicationName + " version:" + applicationVersion);
		logger.info("command line signature: " + commandLineSignature);
		String tempDirectoryString = parsedCommandLine.getOptionsValue(TMP_DIR_OPTION);
		File tempDirectory = null;
		if (tempDirectoryString != null) {
			tempDirectory = new File(tempDirectoryString);
			try {
				FileUtil.createDirectory(tempDirectory);
			} catch (IOException e) {
				throw new IllegalStateException("Unable to create provided temporary directory[" + tempDirectory.getAbsolutePath() + "].");
			}
			if (!tempDirectory.exists() || !tempDirectory.isDirectory()) {
				throw new IllegalStateException("Unable to find provided temporary directory[" + tempDirectory.getAbsolutePath() + "].");
			}
		} else {
			// default temp directory
			tempDirectory = FileUtil.getSystemSpecificTempDirectory();
		}

		File fastQ1File = new File(parsedCommandLine.getOptionsValue(FASTQ_ONE_OPTION));
		File fastQ2File = new File(parsedCommandLine.getOptionsValue(FASTQ_TWO_OPTION));
		File probeInfoFile = new File(parsedCommandLine.getOptionsValue(PROBE_OPTION));
		String bamFileString = parsedCommandLine.getOptionsValue(INPUT_BAM_OPTION);
		File samOrBamFile = new File(bamFileString);

		if (parsedCommandLine.isOptionPresent(READ_NAMES_TO_TRACK_FILE_OPTION)) {
			File readTrackingFile = new File(parsedCommandLine.getOptionsValue(READ_NAMES_TO_TRACK_FILE_OPTION));
			try {
				String readNames = FileUtil.readFileAsString(readTrackingFile);
				for (String readName : readNames.split(StringUtil.NEWLINE)) {
					ReadNameTracking.addReadNameToTrack(readName);
				}
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}

		}

		InputFilesExistValidator.validate(fastQ1File, fastQ2File, probeInfoFile, samOrBamFile);

		int numberOfRecordsInFastq = FastqValidator.validateAndGetNumberOfRecords(fastQ1File, fastQ2File);

		ParsedProbeFile parsedProbeFile = ProbeFileUtil.parseProbeInfoFileWithValidation(probeInfoFile);

		long requiredTempSpaceInBytes = fastQ1File.length() * 4;
		long usableTempSpaceInBytes = tempDirectory.getUsableSpace();
		if (usableTempSpaceInBytes <= requiredTempSpaceInBytes) {
			throw new IllegalStateException("The amount of temporary storage space required by this application is "
					+ doubleFormatter.format((double) requiredTempSpaceInBytes / (double) BYTES_PER_MEGABYTE) + "MB which is greater than the amount of usable space["
					+ doubleFormatter.format((double) usableTempSpaceInBytes / (double) BYTES_PER_MEGABYTE) + "MB] in the temp directory[" + tempDirectory.getAbsolutePath() + "].");
		}
		long requiredOutputSpaceInBytes = fastQ1File.length() * 2;
		long usableOutputSpaceInBytes = outputDirectory.getUsableSpace();
		if (usableOutputSpaceInBytes <= requiredOutputSpaceInBytes) {
			throw new IllegalStateException("The amount of storage space required by this application's output is "
					+ doubleFormatter.format((double) requiredOutputSpaceInBytes / (double) BYTES_PER_MEGABYTE) + "MB which is greater than the amount of usable space ["
					+ doubleFormatter.format((double) usableOutputSpaceInBytes / (double) BYTES_PER_MEGABYTE) + "MB]  in the output directory[" + outputDirectory.getAbsolutePath() + "].");
		}

		int numProcessors = Runtime.getRuntime().availableProcessors();
		if (parsedCommandLine.isOptionPresent(NUM_PROCESSORS_OPTION)) {
			try {
				numProcessors = Integer.parseInt(parsedCommandLine.getOptionsValue(NUM_PROCESSORS_OPTION));

			} catch (NumberFormatException ex) {
				throw new IllegalStateException("Value specified for number of processors is not an integer[" + parsedCommandLine.getOptionsValue(NUM_PROCESSORS_OPTION) + "].");
			}
		}
		if (numProcessors > MAX_NUMBER_OF_PROCESSORS) {
			logger.info("The requested number of processors[" + numProcessors + "] is greater than the max number of processors allowed [" + MAX_NUMBER_OF_PROCESSORS
					+ "] so the max number of processors allowed will be used.");
			numProcessors = MAX_NUMBER_OF_PROCESSORS;
		}

		int extensionUidLength = DEFAULT_EXTENSION_UID_LENGTH;
		int ligationUidLength = DEFAULT_LIGATION_UID_LENGTH;
		String genomeNameFromProbeInfoFile = null;
		ProbeHeaderInformation probeHeaderInformation = null;
		boolean isGenomeNameFromProbeFileIsRecognized = false;
		try {
			probeHeaderInformation = ProbeFileUtil.extractProbeHeaderInformation(probeInfoFile);
			if (probeHeaderInformation != null) {
				genomeNameFromProbeInfoFile = probeHeaderInformation.getGenomeName();
				if (genomeNameFromProbeInfoFile != null) {
					isGenomeNameFromProbeFileIsRecognized = GenomeIdentifier.isGenomeNameRecognized(genomeNameFromProbeInfoFile);
					if (!isGenomeNameFromProbeFileIsRecognized) {
						CliStatusConsole.logStatus("WARNING: This version of HSQutils does not recognize the specified genome build [" + genomeNameFromProbeInfoFile
								+ "] and cannot verify that the BAM file header matches the indicated reference sequence.");
					}
				} else {
					CliStatusConsole.logStatus("Genome information was not found in the header of the probe information file.");
				}

				if (probeHeaderInformation.getExtensionUidLength() != null) {
					extensionUidLength = probeHeaderInformation.getExtensionUidLength();
				}
				if (probeHeaderInformation.getLigationUidLength() != null) {
					ligationUidLength = probeHeaderInformation.getLigationUidLength();
				}
			} else {
				CliStatusConsole.logStatus("There was no header found in the probe information file[" + probeInfoFile.getAbsolutePath() + "].");
			}
		} catch (FileNotFoundException e1) {
			// this should be picked up when the probe info file is originally validated
			throw new AssertionError();
		}

		String outputBamFileName = parsedCommandLine.getOptionsValue(OUTPUT_BAM_FILE_NAME_OPTION);
		if (outputBamFileName.contains("/") || outputBamFileName.contains("\\")) {
			throw new IllegalStateException("The value specified for " + OUTPUT_BAM_FILE_NAME_OPTION.getOptionName() + "[" + outputBamFileName
					+ "] contains a full file path but only a file name should be specified.  Use parameter --" + OUTPUT_DIR_OPTION.getLongFormOption() + " to specify the output directory.");
		}
		if (!outputBamFileName.endsWith(BAM_EXTENSION)) {
			outputBamFileName += BAM_EXTENSION;
		}

		// Set up our alignment scorer
		double matchScore = SimpleAlignmentScorer.DEFAULT_MATCH_SCORE;
		double mismatchPenalty = SimpleAlignmentScorer.DEFAULT_MISMATCH_PENALTY;
		double gapOpenPenalty = SimpleAlignmentScorer.DEFAULT_GAP_OPEN_PENALTY;
		double gapExtendPenalty = SimpleAlignmentScorer.DEFAULT_GAP_EXTEND_PENALTY;

		if (parsedCommandLine.isOptionPresent(MATCH_SCORE_OPTION)) {
			try {
				matchScore = Integer.parseInt(parsedCommandLine.getOptionsValue(MATCH_SCORE_OPTION));
				if (matchScore < 0) {
					throw new IllegalStateException("Value specified for match score must be >= 0 [" + parsedCommandLine.getOptionsValue(MATCH_SCORE_OPTION) + "].");
				}
			} catch (NumberFormatException ex) {
				throw new IllegalStateException("Value specified for match score is not an integer[" + parsedCommandLine.getOptionsValue(MATCH_SCORE_OPTION) + "].");
			}
		}
		if (parsedCommandLine.isOptionPresent(MISMATCH_PENALTY_OPTION)) {
			try {
				mismatchPenalty = -Integer.parseInt(parsedCommandLine.getOptionsValue(MISMATCH_PENALTY_OPTION));
				if (mismatchPenalty > 0) {
					throw new IllegalStateException("Value specified for mismatch penalty must be >= 0 [" + parsedCommandLine.getOptionsValue(MISMATCH_PENALTY_OPTION) + "].");
				}
			} catch (NumberFormatException ex) {
				throw new IllegalStateException("Value specified for mismatch penalty is not an integer[" + parsedCommandLine.getOptionsValue(MISMATCH_PENALTY_OPTION) + "].");
			}
		}
		if (parsedCommandLine.isOptionPresent(GAP_OPEN_PENALTY_OPTION)) {
			try {
				gapOpenPenalty = -Integer.parseInt(parsedCommandLine.getOptionsValue(GAP_OPEN_PENALTY_OPTION));
				if (gapOpenPenalty > 0) {
					throw new IllegalStateException("Value specified for gap open penalty must be >= 0 [" + parsedCommandLine.getOptionsValue(GAP_OPEN_PENALTY_OPTION) + "].");
				}
			} catch (NumberFormatException ex) {
				throw new IllegalStateException("Value specified for gap open penalty is not an integer[" + parsedCommandLine.getOptionsValue(GAP_OPEN_PENALTY_OPTION) + "].");
			}
		}
		if (parsedCommandLine.isOptionPresent(GAP_EXTEND_PENALTY_OPTION)) {
			try {
				gapExtendPenalty = -Integer.parseInt(parsedCommandLine.getOptionsValue(GAP_EXTEND_PENALTY_OPTION));
				if (gapExtendPenalty > 0) {
					throw new IllegalStateException("Value specified for gap extend penalty must be >= 0 [" + parsedCommandLine.getOptionsValue(GAP_EXTEND_PENALTY_OPTION) + "].");
				}
			} catch (NumberFormatException ex) {
				throw new IllegalStateException("Value specified for gap extend penalty not an integer[" + parsedCommandLine.getOptionsValue(GAP_EXTEND_PENALTY_OPTION) + "].");
			}
		}

		// boolean useLenientValidation = parsedCommandLine.isOptionPresent(LENIENT_VALIDATION_STRINGENCY_OPTION);
		if (useLenientValidation) {
			SamReaderFactory.setDefaultValidationStringency(ValidationStringency.LENIENT);
		}

		boolean markDuplicates = parsedCommandLine.isOptionPresent(MARK_DUPLICATES_OPTION);
		boolean keepDuplicates = parsedCommandLine.isOptionPresent(KEEP_DUPLICATES_OPTION);
		boolean shouldOutputInternalReports = parsedCommandLine.isOptionPresent(INTERNAL_REPORTS_OPTION);
		boolean shouldExcludeProgramInBamHeader = parsedCommandLine.isOptionPresent(EXCLUDE_NEW_PROGRAM_IN_BAM_HEADER_OPTION);
		saveTemporaryFiles = parsedCommandLine.isOptionPresent(SAVE_TEMP_OPTION);

		if (markDuplicates && keepDuplicates) {
			throw new IllegalStateException(MARK_DUPLICATES_OPTION.getLongFormOption() + " and " + KEEP_DUPLICATES_OPTION.getLongFormOption() + " cannot be used in the same run.");
		}

		boolean mergePairs = parsedCommandLine.isOptionPresent(MERGE_PAIRS_OPTION);

		IAlignmentScorer alignmentScorer = new SimpleAlignmentScorer(matchScore, mismatchPenalty, gapExtendPenalty, gapOpenPenalty, false);

		try {
			BamFileValidator.validate(samOrBamFile);

			String tempPrefix = HsqUtilsCli.getTempPrefix(applicationName, outputFilePrefix);
			Path tempOutputDirectoryPath = null;
			try {
				tempOutputDirectoryPath = Files.createTempDirectory(tempDirectory.toPath(), tempPrefix);
			} catch (Exception e) {
				throw new IllegalStateException("Unable to create temp directory at [" + tempDirectory.toString() + "].", e);
			}
			final File tempOutputDirectory = tempOutputDirectoryPath.toFile();
			// Delete our temporary directory when we shut down the JVM if the user hasn't asked us to keep it
			if (!saveTemporaryFiles) {
				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						try {
							FileUtil.deleteDirectory(tempOutputDirectory);
						} catch (IOException e) {
							CliStatusConsole.logStatus("Couldn't delete temp directory [" + tempOutputDirectory.getAbsolutePath() + "]:" + e.getMessage());
						}
					}
				});
			}

			try (SamReader samReader = SamReaderFactory.makeDefault().open(samOrBamFile);) {
				SAMFileHeader header = samReader.getFileHeader();

				try {
					if (genomeNameFromProbeInfoFile != null && isGenomeNameFromProbeFileIsRecognized) {
						header = samReader.getFileHeader();
						Map<String, Integer> containerSizesByNameFromBam = BamFileUtil.getContainerSizesFromHeader(header);

						List<String> containerNamesToRemove = new ArrayList<String>();

						// remove any containers that are not in the probe info file
						for (String containerNameFromBam : containerSizesByNameFromBam.keySet()) {
							if (!parsedProbeFile.containsSequenceName(containerNameFromBam)) {
								containerNamesToRemove.add(containerNameFromBam);
							}
						}

						for (String containerNameToRemove : containerNamesToRemove) {
							containerSizesByNameFromBam.remove(containerNameToRemove);
						}

						String matchingGenomeBasedOnContainerSizes = GenomeIdentifier.getMatchingGenomeName(containerSizesByNameFromBam);
						if (matchingGenomeBasedOnContainerSizes == null) {
							String message = "The genome used for mapping is not a recognized genome so the system cannot verify that it matches the provided genome[" + genomeNameFromProbeInfoFile
									+ "] from the probe information file.";
							logger.info(message);
							CliStatusConsole.logStatus(message);
						} else if (!genomeNameFromProbeInfoFile.equals(matchingGenomeBasedOnContainerSizes)) {
							logger.info("Mismatch Genome Report" + StringUtil.NEWLINE + GenomeIdentifier.createMismatchGenomeReportText(genomeNameFromProbeInfoFile, containerSizesByNameFromBam));
							CliStatusConsole.logStatus(StringUtil.NEWLINE + "WARNING:");
							CliStatusConsole.logStatus(
									"It appears that the incorrect genome was used for mapping.  The names and sizes of the genome sequences used for mapping found in the provided BAM/SAM file ["
											+ samOrBamFile.getAbsolutePath() + "] do not match the sequence sizes expected based on the indicated genome build [" + genomeNameFromProbeInfoFile
											+ "] by the probe information file [" + probeInfoFile.getAbsolutePath()
											+ "].  Deduplication will continue but the results should be classified as suspect.  Please review the mismatch genome report in the log file["
											+ logFile.getCanonicalPath() + "] for details on how the expected genome and provided genome differ." + StringUtil.NEWLINE);
						}
					}
				} catch (FileNotFoundException e1) {
					throw new IllegalStateException("Could not find the provided BAM file[" + samOrBamFile.getAbsolutePath() + "].");
				}

			}

			try (SamReader samReader = SamReaderFactory.makeDefault().open(samOrBamFile)) {
				SAMRecordIterator samIter = samReader.iterator();

				boolean noEntriesInBam = false;
				boolean aPairedReadOneFound = false;
				boolean aPairedReadTwoFound = false;

				if (!samIter.hasNext()) {
					noEntriesInBam = true;
				} else {
					samLoop: while (samIter.hasNext()) {
						SAMRecord record = samIter.next();

						if (record.getReadPairedFlag()) {
							aPairedReadOneFound = aPairedReadOneFound || record.getFirstOfPairFlag();
							aPairedReadTwoFound = aPairedReadTwoFound || record.getSecondOfPairFlag();
							if (aPairedReadOneFound && aPairedReadTwoFound) {
								break samLoop;
							}
						}
					}
				}
				samIter.close();

				if (noEntriesInBam) {
					throw new IllegalStateException("The provided BAM file[" + samOrBamFile.getAbsolutePath() + "] contains no entries.");
				} else if (!aPairedReadOneFound && !aPairedReadTwoFound) {
					throw new IllegalStateException("The provided BAM file[" + samOrBamFile.getAbsolutePath() + "] contains no paired read entries.  Paired reads are required for deduplication.");
				} else if (!aPairedReadOneFound) {
					throw new IllegalStateException("The provided BAM file[" + samOrBamFile.getAbsolutePath() + "] contains no read one entries.");
				} else if (!aPairedReadTwoFound) {
					throw new IllegalStateException("The provided BAM file[" + samOrBamFile.getAbsolutePath() + "] contains no read two entries.");
				}

			}

			boolean readsNotTrimmed = parsedCommandLine.isOptionPresent(TRIMMING_SKIPPED_OPTION);

			ProbeTrimmingInformation probeTrimmingInformation;
			try {
				probeTrimmingInformation = FastqReadTrimmer.getProbeTrimmingInformation(parsedProbeFile, probeInfoFile, !readsNotTrimmed);
			} catch (IOException e1) {
				throw new IllegalStateException("Unable to read probe file[" + probeInfoFile + "].");
			}

			sortMergeFilterAndExtendReads(applicationName, applicationVersion, probeInfoFile, parsedProbeFile, samOrBamFile, fastQ1File, fastQ2File, outputDirectory, outputBamFileName,
					outputFilePrefix, tempOutputDirectory, shouldOutputInternalReports, shouldExcludeProgramInBamHeader, commandLineSignature, numProcessors, extensionUidLength, ligationUidLength,
					allowVariableLengthUids, alignmentScorer, markDuplicates, keepDuplicates, mergePairs, useStrictReadToProbeMatching, readsNotTrimmed, probeHeaderInformation, sampleName,
					numberOfRecordsInFastq, probeTrimmingInformation);

			// TODO do not make the original creation of prelim output bam be sorted
			// TODO sort the output bam based on readnames and readnumbers
			// TODO create new bam that contains read names from fastq
			// TODO sort new bam by coords

			long applicationStop = System.currentTimeMillis();
			CliStatusConsole.logStatus("Deduplication has completed successfully.");
			CliStatusConsole.logStatus(
					"Start Time: " + DateUtil.convertTimeInMillisecondsToDate(applicationStart) + "(YYYY/MM/DD HH:MM:SS)  Stop Time: " + DateUtil.convertTimeInMillisecondsToDate(applicationStop)
							+ "(YYYY/MM/DD HH:MM:SS)  Total Time: " + DateUtil.convertMillisecondsToHHMMSS(applicationStop - applicationStart) + "(HH:MM:SS)" + StringUtil.NEWLINE);

		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private static void sortMergeFilterAndExtendReads(String applicationName, String applicationVersion, File probeFile, ParsedProbeFile parsedProbeFile, File bamFile, File fastQ1File,
			File fastQ2File, File outputDirectory, String outputBamFileName, String outputFilePrefix, File tempOutputDirectory, boolean shouldOutputReports, boolean shouldExcludeProgramInBamHeader,
			String commandLineSignature, int numProcessors, int extensionUidLength, int ligationUidLength, boolean allowVariableLengthUids, IAlignmentScorer alignmentScorer, boolean markDuplicates,
			boolean keepDuplicates, boolean mergePairs, boolean useStrictReadToProbeMatching, boolean readsNotTrimmed, ProbeHeaderInformation probeHeaderInformation, String sampleName,
			int numberOfRecordsInFastq, ProbeTrimmingInformation probeTrimmingInformation) {
		try {

			long totalTimeStart = System.currentTimeMillis();
			ApplicationSettings applicationSettings = new ApplicationSettings(probeFile, parsedProbeFile, bamFile, fastQ1File, fastQ2File, outputDirectory, tempOutputDirectory, outputBamFileName,
					outputFilePrefix, bamFile.getName(), shouldOutputReports, shouldExcludeProgramInBamHeader, commandLineSignature, applicationName, applicationVersion, numProcessors,
					allowVariableLengthUids, alignmentScorer, extensionUidLength, ligationUidLength, markDuplicates, keepDuplicates, mergePairs, useStrictReadToProbeMatching, probeHeaderInformation,
					readsNotTrimmed, sampleName, numberOfRecordsInFastq, probeTrimmingInformation);

			PrimerReadExtensionAndPcrDuplicateIdentification.verifyReadNamesCanBeHandledByDedup(applicationSettings.getFastQ1File(), applicationSettings.getFastQ2File());

			PrimerReadExtensionAndPcrDuplicateIdentification extendReadsAndIdentifyDuplicates = new PrimerReadExtensionAndPcrDuplicateIdentification();
			extendReadsAndIdentifyDuplicates.filterBamEntriesByUidAndExtendReadsToPrimers(applicationSettings);

			long totalTimeStop = System.currentTimeMillis();
			logger.debug("done - Total time: (" + DateUtil.convertMillisecondsToHHMMSS(totalTimeStop - totalTimeStart) + ")");
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	static CommandLineOptionsGroup getCommandLineOptionsGroup() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup();

		group.addOption(USAGE_OPTION);
		group.addOption(VERSION_OPTION);
		group.addOption(FASTQ_ONE_OPTION);
		group.addOption(FASTQ_TWO_OPTION);
		group.addOption(PROBE_OPTION);
		group.addOption(INPUT_BAM_OPTION);
		group.addOption(OUTPUT_DIR_OPTION);
		group.addOption(OUTPUT_BAM_FILE_NAME_OPTION);
		group.addOption(OUTPUT_FILE_PREFIX_OPTION);
		group.addOption(TMP_DIR_OPTION);
		group.addOption(NUM_PROCESSORS_OPTION);
		group.addOption(SAVE_TEMP_OPTION);
		group.addOption(TRIMMING_SKIPPED_OPTION);

		// group.addOption(MATCH_SCORE_OPTION);
		// group.addOption(MISMATCH_PENALTY_OPTION);
		// group.addOption(GAP_OPEN_PENALTY_OPTION);
		// group.addOption(GAP_EXTEND_PENALTY_OPTION);
		// group.addOption(LENIENT_VALIDATION_STRINGENCY_OPTION);
		group.addOption(MARK_DUPLICATES_OPTION);
		group.addOption(KEEP_DUPLICATES_OPTION);
		// group.addOption(MERGE_PAIRS_OPTION);
		// group.addOption(NOT_TRIMMED_TO_WITHIN_CAPTURE_TARGET_OPTION);
		group.addOption(INTERNAL_REPORTS_OPTION);
		group.addOption(READ_NAMES_TO_TRACK_FILE_OPTION);
		group.addOption(EXCLUDE_NEW_PROGRAM_IN_BAM_HEADER_OPTION);
		return group;
	}
}
