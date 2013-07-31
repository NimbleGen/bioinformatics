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
import java.nio.file.Files;
import java.nio.file.Path;

import net.sf.samtools.SAMFileReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.ApplicationSettings;
import com.roche.mapping.MapperFiltererAndExtender;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.alignment.SimpleAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOption;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOptionsGroup;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineParser;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;

public class PrefuppCli {
	private final static Logger logger = LoggerFactory.getLogger(PrefuppCli.class);

	private final static String APPLICATION_NAME = "prefupp";
	private final static String APPLICATION_VERSION = "1.0.0";
	public final static int DEFAULT_UID_LENGTH = 7;
	private final static String BAM_EXTENSION = ".bam";

	private final static String DUPLICATE_MAPPINGS_REPORT_NAME = "duplicate_mappings.txt";

	private final static CommandLineOption USAGE_OPTION = new CommandLineOption("Print Usage", "usage", 'h', "Print Usage.", false, true);
	private final static CommandLineOption FASTQ_ONE_OPTION = new CommandLineOption("fastQ One File", "r1", null, "path to first input fastq file", true, false);
	private final static CommandLineOption FASTQ_TWO_OPTION = new CommandLineOption("fastQ Two File", "r2", null, "path to second second input fastq file", true, false);
	private final static CommandLineOption INPUT_BAM_OPTION = new CommandLineOption("Input BAM File Path", "inputBam", null, "path to input BAM file containing the aligned reads", false, false);
	private final static CommandLineOption PROBE_OPTION = new CommandLineOption("PROBE File", "probe", null, "The probe file", true, false);
	private final static CommandLineOption OUTPUT_DIR_OPTION = new CommandLineOption("Output Directory", "outputDir", null, "location to store resultant files.", false, false);
	private final static CommandLineOption OUTPUT_FILE_PREFIX_OPTION = new CommandLineOption("Output File Prefix", "outputPrefix", null, "text to put at beginning of output file names", false, false);
	private final static CommandLineOption TMP_DIR_OPTION = new CommandLineOption("Temporary Directory", "tmpDir", null, "location to store temporary files.", false, false);
	private final static CommandLineOption SAVE_TMP_DIR_OPTION = new CommandLineOption("Save Temporary Files", "saveTmpFiles", null, "save temporary files for later debugging.", false, true);
	private final static CommandLineOption SHOULD_OUTPUT_REPORTS_OPTION = new CommandLineOption("Should Output Quality Reports", "outputReports", 'r',
			"Should this utility generate quality reports?  (Default: No)", false, true);
	private final static CommandLineOption SHOULD_OUTPUT_FASTQ_OPTION = new CommandLineOption("Should Output FastQ Results", "outputFastq", 'f',
			"Should this utility generate fastq result files?  (Default: No)", false, true);
	private final static CommandLineOption NUM_PROCESSORS_OPTION = new CommandLineOption("Number of Processors", "numProcessors", null,
			"The number of threads to run in parallel.  If not specified this will default to the number of cores available on the machine.", false, false);
	private final static CommandLineOption UID_LENGTH_OPTION = new CommandLineOption("Length of UID in Bases", "uidLength", null,
			"Length of the Universal Identifier.  If not specified this will default to " + DEFAULT_UID_LENGTH + " bases.", false, false);
	private final static CommandLineOption ALLOW_VARIABLE_LENGTH_UIDS_OPTION = new CommandLineOption("Allow Variable Length Uids", "allow_variable_length_uids", null,
			"Allow Variable Length Uids (cannot be defined when uidLength is given)", false, true);
	private final static CommandLineOption OUTPUT_BAM_FILE_NAME_OPTION = new CommandLineOption("Output Bam File Name", "outputBamFileName", 'o', "Name for output bam file.", true, false);
	private final static CommandLineOption MATCH_SCORE_OPTION = new CommandLineOption("Match Score", "matchScore", null,
			"The score given to matching nucleotides when extending alignments to the primers (Default: 1)", false, false);
	private final static CommandLineOption MISMATCH_PENALTY_OPTION = new CommandLineOption("Mismatch Penalty", "mismatchPenalty", null,
			"The penalty subtracted for mismatched nucleotides when extending alignments to the primers (Default: 4)", false, false);
	private final static CommandLineOption GAP_OPEN_PENALTY_OPTION = new CommandLineOption("Gap Open Penalty", "gapOpenPenalty", null,
			"The penalty for opening a gap when extending alignments to the primers (Default: 6)", false, false);
	private final static CommandLineOption GAP_EXTEND_PENALTY_OPTION = new CommandLineOption("Gap Extend Penalty", "gapExtendPenalty", null,
			"The penalty for extending a gap when extending alignments to the primers (Default: 1)", false, false);

	public static void main(String[] args) {
		outputToConsole("Primer Read Extension and Filtering of Unique PCR Probes");

		try {
			runCommandLineApp(args);
		} catch (IllegalStateException e) {
			outputToConsole(e.getMessage());
			System.exit(-1);
		}
	}

	static void runCommandLineApp(String[] args) {
		long start = System.currentTimeMillis();
		String commandLineSignature = CommandLineParser.getCommandLineCallSignature(APPLICATION_NAME, args, true);
		outputToConsole(commandLineSignature);
		outputToConsole("");
		ParsedCommandLine parsedCommandLine = CommandLineParser.parseCommandLine(args, getCommandLineOptionsGroup());
		boolean noOptionsProvided = (args.length == 0);
		boolean showUsage = parsedCommandLine.isOptionPresent(USAGE_OPTION) || noOptionsProvided;

		if (showUsage) {
			outputToConsole(parsedCommandLine.getCommandLineOptionsGroup().getUsage());
		} else {
			CommandLineParser.throwCommandLineParsingExceptions(parsedCommandLine);

			String outputDirectoryString = parsedCommandLine.getOptionsValue(OUTPUT_DIR_OPTION);
			File outputDirectory = null;
			if (outputDirectoryString != null) {
				outputDirectory = new File(outputDirectoryString);
				if (!outputDirectory.exists() || !outputDirectory.isDirectory()) {
					throw new IllegalStateException("Unable to find provided output directory[" + outputDirectory.getAbsolutePath() + "].");
				}
			} else {
				// current working directory
				outputDirectory = new File(".");
			}

			String outputFilePrefix = parsedCommandLine.getOptionsValue(OUTPUT_FILE_PREFIX_OPTION);
			if (outputFilePrefix == null) {
				outputFilePrefix = "";
			}

			String tmpDirectoryString = parsedCommandLine.getOptionsValue(TMP_DIR_OPTION);
			File tmpDirectory = null;
			if (tmpDirectoryString != null) {
				tmpDirectory = new File(tmpDirectoryString);
				if (!tmpDirectory.exists() || !tmpDirectory.isDirectory()) {
					throw new IllegalStateException("Unable to find provided temporary directory[" + tmpDirectory.getAbsolutePath() + "].");
				}
			} else {
				// default temp directory
				tmpDirectory = FileUtil.getSystemSpecificTempDirectory();
			}

			boolean saveTmpFiles = parsedCommandLine.isOptionPresent(SAVE_TMP_DIR_OPTION);

			File fastQ1WithUidsFile = new File(parsedCommandLine.getOptionsValue(FASTQ_ONE_OPTION));

			if (!fastQ1WithUidsFile.exists()) {
				throw new IllegalStateException("Unable to find provided FASTQ1 file[" + fastQ1WithUidsFile.getAbsolutePath() + "].");
			}

			File fastQ2File = new File(parsedCommandLine.getOptionsValue(FASTQ_TWO_OPTION));

			if (!fastQ2File.exists()) {
				throw new IllegalStateException("Unable to find provided FASTQ2 file[" + fastQ2File.getAbsolutePath() + "].");
			}

			File probeFile = new File(parsedCommandLine.getOptionsValue(PROBE_OPTION));

			if (!probeFile.exists()) {
				throw new IllegalStateException("Unable to find provided PROBE file[" + probeFile.getAbsolutePath() + "].");
			}

			int numProcessors = Runtime.getRuntime().availableProcessors();
			if (parsedCommandLine.isOptionPresent(NUM_PROCESSORS_OPTION)) {
				try {
					numProcessors = Integer.parseInt(parsedCommandLine.getOptionsValue(NUM_PROCESSORS_OPTION));

				} catch (NumberFormatException ex) {
					throw new IllegalStateException("Value specified for number of processors is not an integer[" + parsedCommandLine.getOptionsValue(NUM_PROCESSORS_OPTION) + "].");
				}
			}

			int uidLength = DEFAULT_UID_LENGTH;
			boolean uidLengthOptionIsPresent = parsedCommandLine.isOptionPresent(UID_LENGTH_OPTION);
			if (uidLengthOptionIsPresent) {
				try {
					uidLength = Integer.parseInt(parsedCommandLine.getOptionsValue(UID_LENGTH_OPTION));
				} catch (NumberFormatException ex) {
					throw new IllegalStateException("UID length specified is not an integer[" + parsedCommandLine.getOptionsValue(UID_LENGTH_OPTION) + "].");
				}
			}

			boolean allowVariableLengthUids = parsedCommandLine.isOptionPresent(ALLOW_VARIABLE_LENGTH_UIDS_OPTION);
			if (uidLengthOptionIsPresent && allowVariableLengthUids) {
				throw new IllegalStateException("You must either specify a UID length using the option --" + UID_LENGTH_OPTION.getLongFormOption()
						+ " OR indicate that variable length uids are allowed via the option --" + ALLOW_VARIABLE_LENGTH_UIDS_OPTION.getLongFormOption() + ".  Providing both options is not allowed.");
			}

			String outputBamFileName = parsedCommandLine.getOptionsValue(OUTPUT_BAM_FILE_NAME_OPTION);
			if (!outputBamFileName.endsWith(BAM_EXTENSION)) {
				outputBamFileName += BAM_EXTENSION;
			}

			boolean shouldOutputQualityReports = parsedCommandLine.isOptionPresent(SHOULD_OUTPUT_REPORTS_OPTION);
			boolean shouldOutputFastq = parsedCommandLine.isOptionPresent(SHOULD_OUTPUT_FASTQ_OPTION);

			// Set up our alignment scorer
			int matchScore = SimpleAlignmentScorer.DEFAULT_MATCH_SCORE;
			int mismatchPenalty = SimpleAlignmentScorer.DEFAULT_MISMATCH_PENALTY;
			int gapOpenPenalty = SimpleAlignmentScorer.DEFAULT_GAP_OPEN_PENALTY;
			int gapExtendPenalty = SimpleAlignmentScorer.DEFAULT_GAP_EXTEND_PENALTY;

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

			IAlignmentScorer alignmentScorer = new SimpleAlignmentScorer(matchScore, mismatchPenalty, gapExtendPenalty, gapOpenPenalty, false);

			if (parsedCommandLine.isOptionPresent(INPUT_BAM_OPTION)) {
				try {
					String bamFileString = parsedCommandLine.getOptionsValue(INPUT_BAM_OPTION);
					File bamFile = new File(bamFileString);

					if (!bamFile.exists()) {
						throw new IllegalStateException("Unable to find provided BAM file[" + bamFile.getAbsolutePath() + "].");
					}

					Path tempOutputDirectoryPath = Files.createTempDirectory(tmpDirectory.toPath(), "nimblegen_");
					final File tempOutputDirectory = tempOutputDirectoryPath.toFile();
					// Delete our temporary directory when we shut down the JVM if the user hasn't asked us to keep it
					if (!saveTmpFiles) {
						Runtime.getRuntime().addShutdownHook(new Thread() {
							@Override
							public void run() {
								try {
									FileUtil.deleteDirectory(tempOutputDirectory);
								} catch (IOException e) {
									outputToConsole("Couldn't delete temp directory [" + tempOutputDirectory.getAbsolutePath() + "]:" + e.getMessage());
								}
							}
						});
					}

					// Try to locate or create an index file for the input bam file
					File bamIndexFile = null;

					// Look for the index in the same location as the file but with a .bai extension instead of a .bam extension
					File tempBamIndexfile = new File(FileUtil.getFileNameWithoutExtension(bamFileString) + ".bai");
					if (tempBamIndexfile.exists()) {
						bamIndexFile = tempBamIndexfile;
						outputToConsole("Using the BAM Index File located at [" + bamIndexFile + "].");
					}

					// Try looking for a .bai file in the same location as the bam file
					if (bamIndexFile == null) {
						// Try looking for a .bam.bai file in the same location as the bam file
						tempBamIndexfile = new File(bamFileString + ".bai");
						if (tempBamIndexfile.exists()) {
							bamIndexFile = tempBamIndexfile;
							outputToConsole("Using the BAM Index File located at [" + bamIndexFile + "].");
						}
					}

					// We couldn't find an index file, create one in our temp directory
					if ((bamIndexFile == null) || !bamIndexFile.exists()) {
						// a bam index file was not provided so create one in the default location
						bamIndexFile = File.createTempFile("bam_index_", ".bai", tempOutputDirectory);
						outputToConsole("A BAM Index File was not passed in and not found in the default location so creating bam index file at:" + bamIndexFile);
						try {
							SAMFileReader samReader = new SAMFileReader(bamFile);
							BamFileUtil.createIndex(samReader, bamIndexFile);
						} catch (Exception e) {
							throw new IllegalStateException("Could not find or create bam index file at [" + bamIndexFile.getAbsolutePath() + "].", e);
						}
					}

					sortMergeFilterAndExtendReads(probeFile, bamFile, bamIndexFile, fastQ1WithUidsFile, fastQ2File, outputDirectory, outputBamFileName, outputFilePrefix, tempOutputDirectory,
							shouldOutputQualityReports, shouldOutputFastq, commandLineSignature, numProcessors, uidLength, allowVariableLengthUids, alignmentScorer);

				} catch (Exception e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			} else {
				outputToConsole("A bam file was not provided so a mapping will be performed.");
				File outputBamFile = new File(outputDirectory, outputBamFileName);
				try {
					FileUtil.createNewFile(outputBamFile);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}

				File ambiguousMappingFile = null;
				if (shouldOutputQualityReports) {
					ambiguousMappingFile = new File(outputDirectory + PrimerReadExtensionAndFilteringOfUniquePcrProbes.REPORT_DIRECTORY, DUPLICATE_MAPPINGS_REPORT_NAME);
					try {
						FileUtil.createNewFile(ambiguousMappingFile);
					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}

				MapperFiltererAndExtender mapFilterAndExtend = new MapperFiltererAndExtender(fastQ1WithUidsFile, fastQ2File, probeFile, outputBamFile, ambiguousMappingFile, numProcessors, uidLength,
						APPLICATION_NAME, APPLICATION_VERSION, commandLineSignature, alignmentScorer);
				mapFilterAndExtend.mapFilterAndExtend();
			}
			long end = System.currentTimeMillis();
			outputToConsole("Processing Completed (Total time: " + DateUtil.convertMillisecondsToHHMMSS(end - start) + ").");
		}

	}

	private static void sortMergeFilterAndExtendReads(File probeFile, File bamFile, File bamIndexFile, File fastQ1WithUidsFile, File fastQ2File, File outputDirectory, String outputBamFileName,
			String outputFilePrefix, File tempOutputDirectory, boolean shouldOutputQualityReports, boolean shouldOutputFastq, String commandLineSignature, int numProcessors, int uidLength,
			boolean allowVariableLengthUids, IAlignmentScorer alignmentScorer) {
		try {

			final File mergedBamFileSortedByCoordinates = File.createTempFile("merged_bam_sorted_by_coordinates_", ".bam", tempOutputDirectory);
			final File indexFileForMergedBamFileSortedByCoordinates = File.createTempFile("index_of_merged_bam_sorted_by_coordinates_", ".bamindex", tempOutputDirectory);

			long totalTimeStart = System.currentTimeMillis();

			FastqAndBamFileMerger.createMergedFastqAndBamFileFromUnsortedFiles(bamFile, fastQ1WithUidsFile, fastQ2File, mergedBamFileSortedByCoordinates, uidLength);
			long timeAfterMergeUnsorted = System.currentTimeMillis();
			logger.debug("done merging bam and fastqfiles ... result[" + mergedBamFileSortedByCoordinates.getAbsolutePath() + "] in " + (timeAfterMergeUnsorted - totalTimeStart) + "ms.");

			// Build bam index
			SAMFileReader samReader = new SAMFileReader(mergedBamFileSortedByCoordinates);

			BamFileUtil.createIndexOnCoordinateSortedBamFile(samReader, indexFileForMergedBamFileSortedByCoordinates);
			long timeAfterBuildBamIndex = System.currentTimeMillis();
			logger.debug("done creating index for merged and sorted bam file ... result[" + indexFileForMergedBamFileSortedByCoordinates.getAbsolutePath() + "] in "
					+ DateUtil.convertMillisecondsToHHMMSS(timeAfterBuildBamIndex - timeAfterMergeUnsorted));
			samReader.close();

			ApplicationSettings applicationSettings = new ApplicationSettings(probeFile, mergedBamFileSortedByCoordinates, indexFileForMergedBamFileSortedByCoordinates, fastQ1WithUidsFile,
					fastQ2File, outputDirectory, outputBamFileName, outputFilePrefix, bamFile.getName(), shouldOutputQualityReports, shouldOutputFastq, commandLineSignature, APPLICATION_NAME,
					APPLICATION_VERSION, numProcessors, allowVariableLengthUids, alignmentScorer);

			PrimerReadExtensionAndFilteringOfUniquePcrProbes.filterBamEntriesByUidAndExtendReadsToPrimers(applicationSettings);

			long totalTimeStop = System.currentTimeMillis();
			logger.debug("done - Total time: (" + DateUtil.convertMillisecondsToHHMMSS(totalTimeStop - totalTimeStart) + ")");
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private static void outputToConsole(String output) {
		System.out.println(output);
	}

	private static CommandLineOptionsGroup getCommandLineOptionsGroup() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup("PREFUPP (Primer Read Extension and Filtering of Unique PCR Probes) Command Line Usage:");

		group.addOption(USAGE_OPTION);
		group.addOption(FASTQ_ONE_OPTION);
		group.addOption(FASTQ_TWO_OPTION);
		group.addOption(PROBE_OPTION);
		group.addOption(INPUT_BAM_OPTION);
		group.addOption(OUTPUT_DIR_OPTION);
		group.addOption(OUTPUT_BAM_FILE_NAME_OPTION);
		group.addOption(OUTPUT_FILE_PREFIX_OPTION);
		group.addOption(TMP_DIR_OPTION);
		group.addOption(SAVE_TMP_DIR_OPTION);
		group.addOption(SHOULD_OUTPUT_REPORTS_OPTION);
		group.addOption(SHOULD_OUTPUT_FASTQ_OPTION);
		group.addOption(NUM_PROCESSORS_OPTION);
		group.addOption(UID_LENGTH_OPTION);
		group.addOption(ALLOW_VARIABLE_LENGTH_UIDS_OPTION);
		group.addOption(MATCH_SCORE_OPTION);
		group.addOption(MISMATCH_PENALTY_OPTION);
		group.addOption(GAP_OPEN_PENALTY_OPTION);
		group.addOption(GAP_EXTEND_PENALTY_OPTION);
		return group;
	}
}
