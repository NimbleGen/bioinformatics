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

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.ApplicationSettings;
import com.roche.heatseq.objects.ParsedProbeFile;
import com.roche.heatseq.process.BamFileValidator;
import com.roche.heatseq.process.FastqValidator;
import com.roche.heatseq.process.InputFilesExistValidator;
import com.roche.heatseq.process.PrimerReadExtensionAndPcrDuplicateIdentification;
import com.roche.heatseq.process.PrimerReadExtensionAndPcrDuplicateIdentification.ReadNameDetails;
import com.roche.heatseq.utils.BamFileUtil;
import com.roche.heatseq.utils.ProbeFileUtil;
import com.roche.heatseq.utils.ProbeFileUtil.ProbeHeaderInformation;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.alignment.SimpleAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOption;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOptionsGroup;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.LoggingUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.genome.GenomeIdentifier;

public class DeduplicationCli {
	private final static Logger logger = LoggerFactory.getLogger(DeduplicationCli.class);

	private final static int MAX_NUMBER_OF_PROCESSORS = 20;

	private final static String SAM_FILE_EXTENSION = "sam";

	public final static int DEFAULT_EXTENSION_UID_LENGTH = 10;
	public final static int DEFAULT_LIGATION_UID_LENGTH = 0;
	public final static String BAM_EXTENSION = ".bam";

	private final static int BYTES_PER_MEGABYTE = 1000000;
	private final static DecimalFormat doubleFormatter = new DecimalFormat("#,###.##");

	public final static CommandLineOption USAGE_OPTION = new CommandLineOption("Print Usage", "usage", 'h', "Print Usage.", false, true);
	public final static CommandLineOption FASTQ_ONE_OPTION = new CommandLineOption("FastQ One File", "r1", null,
			"Path to first input fastq file (as an uncompressed file with a fastq extension or a compressed file with the gz extension).", true, false);
	public final static CommandLineOption FASTQ_TWO_OPTION = new CommandLineOption("FastQ Two File", "r2", null,
			"Path to second input fastq file (as an uncompressed file with a fastq extension or a compressed file with the gz extension).", true, false);
	public final static CommandLineOption INPUT_BAM_OPTION = new CommandLineOption("Input BAM or SAM File Path", "inputBam", null, "Path to input BAM or SAM file containing the aligned reads.", true,
			false);
	public final static CommandLineOption PROBE_OPTION = new CommandLineOption("Probe Information File", "probe", null, "NimbleGen probe file.", true, false);
	public final static CommandLineOption OUTPUT_DIR_OPTION = new CommandLineOption("Output Directory", "outputDir", null, "Location to store resultant files.", false, false);
	public final static CommandLineOption OUTPUT_FILE_PREFIX_OPTION = new CommandLineOption("Output File Prefix", "outputPrefix", null, "Text to put at beginning of output file names.", false, false);
	public final static CommandLineOption TMP_DIR_OPTION = new CommandLineOption("Temporary Directory", "tmpDir", null, "Location to store temporary files.", false, false);
	public final static CommandLineOption NUM_PROCESSORS_OPTION = new CommandLineOption("Number of Processors", "numProcessors", null,
			"The number of threads to run in parallel.  If not specified this will default to the number of cores available on the machine.  The designated value and default value will be capped at "
					+ MAX_NUMBER_OF_PROCESSORS + ".", false, false);
	public final static CommandLineOption OUTPUT_BAM_FILE_NAME_OPTION = new CommandLineOption("Output Bam File Name", "outputBamFileName", 'o', "Name for output bam file.", true, false);
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
	private final static CommandLineOption KEEP_DUPLICATES_OPTION = new CommandLineOption("Keep Duplicates", "disableDuplicateRemoval", null,
			"Keep duplicate reads in the bam file instead of removing them. ", false, true, true);
	public final static CommandLineOption MERGE_PAIRS_OPTION = new CommandLineOption("Merge Pairs", "mergePairs", null, "Merge pairs using the highest quality base reads from each read.", false, true);
	public final static CommandLineOption TRIMMING_SKIPPED_OPTION = new CommandLineOption("Reads Were Not Trimmed Prior to Mapping", "readsNotTrimmed", null,
			"The reads were not trimmed prior to mapping.", false, true);
	private final static CommandLineOption INTERNAL_REPORTS_OPTION = new CommandLineOption("Output interal reports", "internalReports", null, "Output internal reports.", false, true, true);
	private final static CommandLineOption EXCLUDE_NEW_PROGRAM_IN_BAM_HEADER_OPTION = new CommandLineOption("Exclude Program in Bam Header", "excludeProgramInBamHeader", null,
			"Don not include a program entry for this application in the bam header.", false, true, true);
	private final static CommandLineOption SAVE_TEMP_OPTION = new CommandLineOption("Save Temp Files", "saveTemp", null, "Save temporary files.", false, true, true);
	public final static CommandLineOption VERSION_OPTION = new CommandLineOption("Print Version", "version", null, "Print the version for this application.", false, true);

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

		InputFilesExistValidator.validate(fastQ1File, fastQ2File, probeInfoFile, samOrBamFile);

		FastqValidator.validate(fastQ1File, fastQ2File);

		ParsedProbeFile parsedProbeFile = ProbeFileUtil.parseProbeInfoFileWithValidation(probeInfoFile);

		long requiredTempSpaceInBytes = fastQ1File.length() * 4;
		long usableTempSpaceInBytes = tempDirectory.getUsableSpace();
		if (usableTempSpaceInBytes <= requiredTempSpaceInBytes) {
			throw new IllegalStateException("The amount of temporary storage space required by this application is "
					+ doubleFormatter.format((double) requiredTempSpaceInBytes / (double) BYTES_PER_MEGABYTE) + "MB which is greater than the amount of usable space in the temp directory ["
					+ doubleFormatter.format((double) usableTempSpaceInBytes / (double) BYTES_PER_MEGABYTE) + "MB].");
		}
		long requiredOutputSpaceInBytes = fastQ1File.length() * 2;
		long usableOutputSpaceInBytes = outputDirectory.getUsableSpace();
		if (usableOutputSpaceInBytes <= requiredOutputSpaceInBytes) {
			throw new IllegalStateException("The amount of storage space required by this application's output is "
					+ doubleFormatter.format((double) requiredOutputSpaceInBytes / (double) BYTES_PER_MEGABYTE) + "MB which is greater than the amount of usable space in the output directory ["
					+ doubleFormatter.format((double) usableOutputSpaceInBytes / (double) BYTES_PER_MEGABYTE) + "MB].");
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
						CliStatusConsole.logStatus("WARNING: This version of HSQUtils does not recognize the specified genome build [" + genomeNameFromProbeInfoFile
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
			SAMFileReader.setDefaultValidationStringency(ValidationStringency.LENIENT);
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

			// Try to locate or create an index file for the input bam file
			File bamIndexFile = null;
			File sortedBamFile = null;
			File validSamOrBamInputFile = null;
			boolean isSamFormat = false;

			boolean newSortedBamFileCreated = false;

			try (SAMFileReader samReader = new SAMFileReader(samOrBamFile)) {
				isSamFormat = !samReader.isBinary();

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
							CliStatusConsole
									.logStatus("It appears that the incorrect genome was used for mapping.  The names and sizes of the genome sequences used for mapping found in the provided BAM/SAM file ["
											+ samOrBamFile.getAbsolutePath()
											+ "] do not match the sequence sizes expected based on the indicated genome build ["
											+ genomeNameFromProbeInfoFile
											+ "] by the probe information file ["
											+ probeInfoFile.getAbsolutePath()
											+ "].  Deduplication will continue but the results should be classified as suspect.  Please review the mismatch genome report in the log file["
											+ logFile.getCanonicalPath() + "] for details on how the expected genome and provided genome differ." + StringUtil.NEWLINE);
						}
					}
				} catch (FileNotFoundException e1) {
					throw new IllegalStateException("Could not find the provided BAM file[" + samOrBamFile.getAbsolutePath() + "].");
				}

				boolean isSortIndicatedInHeader = header.getSortOrder().equals(SortOrder.coordinate);
				boolean isSamFile = FileUtil.getFileExtension(samOrBamFile).equals(SAM_FILE_EXTENSION);

				if (isSortIndicatedInHeader) {
					logger.debug("The input BAM file[" + samOrBamFile.getAbsolutePath() + "] was deemed sorted based on header information.");
				}
				if (!isSortIndicatedInHeader || isSamFile) {
					// sam files need to be sorted and converted to bam files regardless
					boolean isSorted = !isSamFormat;
					long sortedCheckStart = System.currentTimeMillis();
					// verify the file is sorted by comparing lines
					SAMRecordIterator iter = samReader.iterator();
					SAMRecord lastRecord = null;
					int entryNumber = 0;
					recordLoop: while (iter.hasNext() && isSorted) {
						SAMRecord currentRecord = iter.next();
						if (lastRecord != null) {
							boolean isReferenceSame = lastRecord.getReferenceIndex() == currentRecord.getReferenceIndex();
							if (isReferenceSame) {
								boolean isAlignmentPositionSorted = lastRecord.getAlignmentStart() <= currentRecord.getAlignmentStart();
								isSorted = isAlignmentPositionSorted;
							} else {
								boolean bothReadsAreMapped = !currentRecord.getReadUnmappedFlag() && !lastRecord.getReadUnmappedFlag();
								if (bothReadsAreMapped) {
									boolean isReferenceIndexSorted = lastRecord.getReferenceIndex() < currentRecord.getReferenceIndex();
									isSorted = isReferenceIndexSorted;
								} else {
									// no need to keep checking since all the reads should be
									// unmapped after this point
									break recordLoop;
								}
							}
						}
						lastRecord = currentRecord;
						entryNumber++;
					}
					iter.close();
					long sortedCheckStop = System.currentTimeMillis();
					logger.debug("Time to check if sorted:" + DateUtil.convertMillisecondsToHHMMSS(sortedCheckStop - sortedCheckStart));

					if (isSorted) {
						logger.debug("The input BAM file[" + samOrBamFile.getAbsolutePath() + "] is sorted.");
						sortedBamFile = samOrBamFile;
					} else {
						CliStatusConsole.logStatus("The input SAM/BAM file is not sorted.");
						logger.info("SAM/BAM file was unsorted starting at entry[" + entryNumber + "].");
						sortedBamFile = new File(tempOutputDirectory, "sorted_" + FileUtil.getFileNameWithoutExtension(samOrBamFile.getName()) + ".bam");
						CliStatusConsole.logStatus("Creating a sorted input BAM file at [" + sortedBamFile.getAbsolutePath() + "].");

						long sortStart = System.currentTimeMillis();
						BamFileUtil.sortOnCoordinates(samOrBamFile, sortedBamFile);
						long sortStop = System.currentTimeMillis();
						CliStatusConsole.logStatus("Done creating a sorted BAM file in " + DateUtil.convertMillisecondsToHHMMSS(sortStop - sortStart) + "(HH:MM:SS).");

						newSortedBamFileCreated = true;
					}

				} else {
					CliStatusConsole.logStatus("The BAM header indicates that the BAM file is sorted.");
					sortedBamFile = samOrBamFile;
				}
			}

			try (SAMFileReader samReader = new SAMFileReader(sortedBamFile)) {
				if (!newSortedBamFileCreated) {
					// Look for the index in the same location as the file but with a .bai extension instead of a .bam extension
					File tempBamIndexfile = new File(FileUtil.getFileNameWithoutExtension(bamFileString) + ".bai");
					if (tempBamIndexfile.exists()) {
						bamIndexFile = tempBamIndexfile;
						CliStatusConsole.logStatus("Using the BAM Index File located at [" + bamIndexFile + "].");
					}

					// Try looking for a .bai file in the same location as the bam file
					if (bamIndexFile == null) {
						// Try looking for a .bam.bai file in the same location as the bam file
						tempBamIndexfile = new File(bamFileString + ".bai");
						if (tempBamIndexfile.exists()) {
							bamIndexFile = tempBamIndexfile;
							CliStatusConsole.logStatus("Using the BAM Index File located at [" + bamIndexFile + "].");
						}
					}
				}

				// We couldn't find an index file, create one in our temp directory
				if ((bamIndexFile == null) || !bamIndexFile.exists()) {
					// a bam index file was not provided so create one in the default location
					bamIndexFile = new File(tempOutputDirectory, sortedBamFile.getName() + ".bai");
					FileUtil.createNewFile(bamIndexFile);
					CliStatusConsole.logStatus("A BAM Index File was not found in the default location so creating bam index file at [" + bamIndexFile.getAbsolutePath() + "].");
					long indexStart = System.currentTimeMillis();
					try {
						BamFileUtil.createIndex(samReader, bamIndexFile);
						long indexStop = System.currentTimeMillis();
						CliStatusConsole.logStatus("Done creating the BAM Index File in " + DateUtil.convertMillisecondsToHHMMSS(indexStop - indexStart) + "(HH:MM:SS).");
					} catch (Exception e) {
						throw new IllegalStateException("Could not create bam index file at [" + bamIndexFile.getAbsolutePath() + "].", e);
					}
				}
			}

			validSamOrBamInputFile = sortedBamFile;

			try (SAMFileReader samReader = new SAMFileReader(validSamOrBamInputFile)) {
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

			sortMergeFilterAndExtendReads(applicationName, applicationVersion, probeInfoFile, validSamOrBamInputFile, bamIndexFile, fastQ1File, fastQ2File, outputDirectory, outputBamFileName,
					outputFilePrefix, tempOutputDirectory, shouldOutputInternalReports, shouldExcludeProgramInBamHeader, commandLineSignature, numProcessors, extensionUidLength, ligationUidLength,
					allowVariableLengthUids, alignmentScorer, markDuplicates, keepDuplicates, mergePairs, useStrictReadToProbeMatching, readsNotTrimmed, probeHeaderInformation, sampleName);

			long applicationStop = System.currentTimeMillis();
			CliStatusConsole.logStatus("Deduplication has completed successfully.");
			CliStatusConsole.logStatus("Start Time: " + DateUtil.convertTimeInMillisecondsToDate(applicationStart) + "(YYYY/MM/DD HH:MM:SS)  Stop Time: "
					+ DateUtil.convertTimeInMillisecondsToDate(applicationStop) + "(YYYY/MM/DD HH:MM:SS)  Total Time: " + DateUtil.convertMillisecondsToHHMMSS(applicationStop - applicationStart)
					+ "(HH:MM:SS)" + StringUtil.NEWLINE);

		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public static void sortMergeFilterAndExtendReads(String applicationName, String applicationVersion, File probeFile, File bamFile, File bamIndexFile, File fastQ1File, File fastQ2File,
			File outputDirectory, String outputBamFileName, String outputFilePrefix, File tempOutputDirectory, boolean shouldOutputReports, boolean shouldExcludeProgramInBamHeader,
			String commandLineSignature, int numProcessors, int extensionUidLength, int ligationUidLength, boolean allowVariableLengthUids, IAlignmentScorer alignmentScorer, boolean markDuplicates,
			boolean keepDuplicates, boolean mergePairs, boolean useStrictReadToProbeMatching, boolean readsNotTrimmed, ProbeHeaderInformation probeHeaderInformation, String sampleName) {
		try {

			long totalTimeStart = System.currentTimeMillis();
			ApplicationSettings applicationSettings = new ApplicationSettings(probeFile, bamFile, bamIndexFile, fastQ1File, fastQ2File, outputDirectory, tempOutputDirectory, outputBamFileName,
					outputFilePrefix, bamFile.getName(), shouldOutputReports, shouldExcludeProgramInBamHeader, commandLineSignature, applicationName, applicationVersion, numProcessors,
					allowVariableLengthUids, alignmentScorer, extensionUidLength, ligationUidLength, markDuplicates, keepDuplicates, mergePairs, useStrictReadToProbeMatching, probeHeaderInformation,
					readsNotTrimmed, sampleName);

			ReadNameDetails readNameDetails = PrimerReadExtensionAndPcrDuplicateIdentification.verifyReadNamesCanBeHandledByDedupAndFindCommonReadNameBeginning(applicationSettings.getFastQ1File(),
					applicationSettings.getFastQ2File());

			String commonReadNameBeginning = readNameDetails.getCommonReadNameBeginning();

			PrimerReadExtensionAndPcrDuplicateIdentification extendReadsAndIdentifyDuplicates = new PrimerReadExtensionAndPcrDuplicateIdentification(commonReadNameBeginning);
			extendReadsAndIdentifyDuplicates.filterBamEntriesByUidAndExtendReadsToPrimers(applicationSettings);

			long totalTimeStop = System.currentTimeMillis();
			logger.debug("done - Total time: (" + DateUtil.convertMillisecondsToHHMMSS(totalTimeStop - totalTimeStart) + ")");
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public static CommandLineOptionsGroup getCommandLineOptionsGroup() {
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
		group.addOption(EXCLUDE_NEW_PROGRAM_IN_BAM_HEADER_OPTION);
		return group;
	}
}
