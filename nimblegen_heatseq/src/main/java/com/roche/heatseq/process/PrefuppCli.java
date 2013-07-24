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
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOption;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOptionsGroup;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineParser;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;

public class PrefuppCli {
	private final static Logger logger = LoggerFactory.getLogger(PrefuppCli.class);

	private final static String APPLICATION_NAME = "prefupp";
	private final static String APPLICATION_VERSION = "1.0.0";
	public final static int DEFAULT_UID_LENGTH = 7;
	private final static String BAM_EXTENSION = ".bam";

	private final static String DUPLICATE_MAPPINGS_REPORT_NAME = "duplicate_mappings.txt";
	private final static String DEFAULT_OUTPUT_MAPPED_BAM_FILE_NAME = "mapping.reduced.bam";

	private final static CommandLineOption USAGE_OPTION = new CommandLineOption("Print Usage", "usage", 'h', "Print Usage.", false, true);
	private final static CommandLineOption FASTQ_ONE_OPTION = new CommandLineOption("fastQ One File", "fastQOne", null, "The first fastq file", true, false);
	private final static CommandLineOption FASTQ_TWO_OPTION = new CommandLineOption("fastQ Two File", "fastQTwo", null, "The second fastq file", true, false);
	private final static CommandLineOption BAM_OPTION = new CommandLineOption("BAM File", "bam", null, "The BAM file", false, false);
	private final static CommandLineOption PROBE_OPTION = new CommandLineOption("PROBE File", "probe", null, "The probe file", true, false);
	private final static CommandLineOption BAM_INDEX_OPTION = new CommandLineOption("BAM Index File", "bamIndex", null, "location for BAM index File.", false, false);
	private final static CommandLineOption OUTPUT_DIR_OPTION = new CommandLineOption("Output Directory", "outputDir", null, "location to store resultant files.", false, false);
	private final static CommandLineOption OUTPUT_FILE_PREFIX_OPTION = new CommandLineOption("Output File Prefix", "outputPrefix", null, "text to put at beginning of output file names", false, false);
	private final static CommandLineOption TMP_DIR_OPTION = new CommandLineOption("Temporary Directory", "tmpDir", null, "location to store temporary files.", false, false);
	private final static CommandLineOption SAVE_TMP_DIR_OPTION = new CommandLineOption("Save Temporary Files", "saveTmpFiles", null, "save temporary files for later debugging.", false, true);
	private final static CommandLineOption SHOULD_OUTPUT_REPORTS_OPTION = new CommandLineOption("Should Output Quality Reports", "outputReports", 'r',
			"Should this utility generate quality reports?  (Default: No)", false, true);
	private final static CommandLineOption SHOULD_OUTPUT_FASTQ_OPTION = new CommandLineOption("Should Output FastQ Results", "outputFastq", 'f',
			"Should this utility generate fastq result files?  (Default: No)", false, true);
	private final static CommandLineOption SHOULD_NOT_EXTEND_READS_TO_PRIMERS = new CommandLineOption("Should Not Extend Reads To Primers", "doNotExtendReads", null,
			"Should this utility not extend reads to the primers?  (Default: No)", false, true);
	private final static CommandLineOption NUM_PROCESSORS_OPTION = new CommandLineOption("Number of Processors", "numProcessors", null,
			"The number of threads to run in parallel.  If not specified this will default to the number of cores available on the machine.", false, false);
	private final static CommandLineOption UID_LENGTH_OPTION = new CommandLineOption("Length of UID in bases", "uidLength", null,
			"Length of the Universal Identifier.  If not specified this will default to " + DEFAULT_UID_LENGTH + " bases.", false, false);
	private final static CommandLineOption OUTPUT_BAM_FILE_NAME_OPTION = new CommandLineOption("Output Bam File Name", "outputBamFileName", 'o',
			"Name for output bam file.  If not specified this will default to [" + DEFAULT_OUTPUT_MAPPED_BAM_FILE_NAME + "].", false, false);

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

		boolean showUsage = parsedCommandLine.isOptionPresent(USAGE_OPTION);

		if (showUsage) {
			outputToConsole(parsedCommandLine.getCommandLineOptionsGroup().getUsage());
		} else {
			try {
				CommandLineParser.throwCommandLineParsingExceptions(parsedCommandLine);
			} catch (Exception e) {
				throw new IllegalStateException(parsedCommandLine.getCommandLineOptionsGroup().getUsage());
			}

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
			if (parsedCommandLine.isOptionPresent(UID_LENGTH_OPTION)) {
				try {
					uidLength = Integer.parseInt(parsedCommandLine.getOptionsValue(UID_LENGTH_OPTION));
				} catch (NumberFormatException ex) {
					throw new IllegalStateException("UID length specified is not an integer[" + parsedCommandLine.getOptionsValue(UID_LENGTH_OPTION) + "].");
				}
			}

			String outputBamFileName = null;
			if (parsedCommandLine.isOptionPresent(OUTPUT_BAM_FILE_NAME_OPTION)) {
				outputBamFileName = parsedCommandLine.getOptionsValue(OUTPUT_BAM_FILE_NAME_OPTION);
				if (!outputBamFileName.endsWith(BAM_EXTENSION)) {
					outputBamFileName += BAM_EXTENSION;
				}
			}

			boolean shouldOutputQualityReports = parsedCommandLine.isOptionPresent(SHOULD_OUTPUT_REPORTS_OPTION);
			boolean shouldOutputFastq = parsedCommandLine.isOptionPresent(SHOULD_OUTPUT_FASTQ_OPTION);
			boolean shouldExtendReads = !parsedCommandLine.isOptionPresent(SHOULD_NOT_EXTEND_READS_TO_PRIMERS);

			if (parsedCommandLine.isOptionPresent(BAM_OPTION)) {
				String bamFileString = parsedCommandLine.getOptionsValue(BAM_OPTION);
				File bamFile = new File(bamFileString);

				if (outputBamFileName == null) {
					outputBamFileName = FileUtil.getFileNameWithoutExtension(bamFile.getName()) + ".reduced.bam";
				}

				if (!bamFile.exists()) {
					throw new IllegalStateException("Unable to find provided BAM file[" + bamFile.getAbsolutePath() + "].");
				}

				File bamIndexFile = null;
				String bamIndexFileString = parsedCommandLine.getOptionsValue(BAM_INDEX_OPTION);

				if (bamIndexFileString != null) {
					bamIndexFile = new File(bamIndexFileString);
				} else {
					// a bam index file was not provided so look for the index file in the same location as the bam file
					File tempBamIndexfile = new File(bamFileString + ".bai");

					if (tempBamIndexfile.exists()) {
						bamIndexFile = tempBamIndexfile;
						outputToConsole("Using the BAM Index File located at [" + bamIndexFile + "].");
					}
				}

				if ((bamIndexFile == null) || !bamIndexFile.exists()) {
					// a bam index file was not provided so create one in the default location
					bamIndexFile = new File(bamFileString + ".bai");
					outputToConsole("A BAM Index File was not passed in and not found in the default location so creating bam index file at:" + bamIndexFile);

					try {
						SAMFileReader samReader = new SAMFileReader(bamFile);
						BamFileUtil.createIndex(samReader, bamIndexFile);
					} catch (Exception e) {
						throw new IllegalStateException("Could not find or create bam index file at [" + bamIndexFile.getAbsolutePath() + "].  Please provide this file using the following option:"
								+ BAM_INDEX_OPTION.getUsage(), e);
					}

				}

				sortMergeFilterAndExtendReads(probeFile, bamFile, bamIndexFile, fastQ1WithUidsFile, fastQ2File, outputDirectory, outputBamFileName, outputFilePrefix, tmpDirectory, saveTmpFiles,
						shouldOutputQualityReports, shouldOutputFastq, shouldExtendReads, commandLineSignature, numProcessors, uidLength);
			} else {
				if (outputBamFileName == null) {
					outputBamFileName = DEFAULT_OUTPUT_MAPPED_BAM_FILE_NAME;
				}

				outputToConsole("A bam file was not provided so a mapping will be performed.");
				File outputBamFile = new File(outputDirectory, outputFilePrefix + outputBamFileName);
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
						APPLICATION_NAME, APPLICATION_VERSION, commandLineSignature);
				mapFilterAndExtend.mapFilterAndExtend();
			}
		}
		long end = System.currentTimeMillis();
		outputToConsole("Processing Completed (Total time:" + (end - start) + "ms).");
	}

	private static void sortMergeFilterAndExtendReads(File probeFile, File bamFile, File bamIndexFile, File fastQ1WithUidsFile, File fastQ2File, File outputDirectory, String outputBamFileName,
			String outputFilePrefix, File tmpDirectory, boolean saveTmpDirectory, boolean shouldOutputQualityReports, boolean shouldOutputFastq, boolean shouldExtendReads,
			String commandLineSignature, int numProcessors, int uidLength) {
		File tempOutputDirectory = null;
		File mergedBamFileSortedByCoordinates = null;
		File indexFileForMergedBamFileSortedByCoordinates = null;

		try {

			Path tempOutputDirectoryPath = Files.createTempDirectory(tmpDirectory.toPath(), "nimblegen_");
			tempOutputDirectory = tempOutputDirectoryPath.toFile();
			mergedBamFileSortedByCoordinates = File.createTempFile("merged_bam_sorted_by_coordinates_", ".bam", tempOutputDirectory);
			indexFileForMergedBamFileSortedByCoordinates = File.createTempFile("index_of_merged_bam_sorted_by_coordinates_", ".bamindex", tempOutputDirectory);

			// Make sure our tmp files are deleted when we exit
			if (!saveTmpDirectory) {
				tempOutputDirectory.deleteOnExit();
				mergedBamFileSortedByCoordinates.deleteOnExit();
				indexFileForMergedBamFileSortedByCoordinates.deleteOnExit();
			}

			long totalTimeStart = System.currentTimeMillis();

			FastqAndBamFileMerger.createMergedFastqAndBamFileFromUnsortedFiles(bamFile, fastQ1WithUidsFile, fastQ2File, mergedBamFileSortedByCoordinates, uidLength);
			long timeAfterMergeUnsorted = System.currentTimeMillis();
			logger.debug("done merging bam and fastqfiles ... result[" + mergedBamFileSortedByCoordinates.getAbsolutePath() + "] in " + (timeAfterMergeUnsorted - totalTimeStart) + "ms.");

			// Build bam index
			SAMFileReader samReader = new SAMFileReader(mergedBamFileSortedByCoordinates);

			indexFileForMergedBamFileSortedByCoordinates = BamFileUtil.createIndexOnCoordinateSortedBamFile(samReader, indexFileForMergedBamFileSortedByCoordinates);
			long timeAfterBuildBamIndex = System.currentTimeMillis();
			logger.debug("done creating index for merged and sorted bam file ... result[" + indexFileForMergedBamFileSortedByCoordinates.getAbsolutePath() + "] in "
					+ (timeAfterBuildBamIndex - timeAfterMergeUnsorted) + "ms.");
			samReader.close();

			ApplicationSettings applicationSettings = new ApplicationSettings(probeFile, mergedBamFileSortedByCoordinates, indexFileForMergedBamFileSortedByCoordinates, fastQ1WithUidsFile,
					fastQ2File, outputDirectory, outputBamFileName, outputFilePrefix, tmpDirectory, bamFile.getName(), shouldOutputQualityReports, shouldOutputFastq, shouldExtendReads,
					commandLineSignature, APPLICATION_NAME, APPLICATION_VERSION, numProcessors);

			PrimerReadExtensionAndFilteringOfUniquePcrProbes.filterBamEntriesByUidAndExtendReadsToPrimers(applicationSettings);

			long totalTimeStop = System.currentTimeMillis();

			logger.debug("done (" + (totalTimeStop - totalTimeStart) + " ms)");
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
		group.addOption(BAM_OPTION);
		group.addOption(BAM_INDEX_OPTION);
		group.addOption(OUTPUT_DIR_OPTION);
		group.addOption(OUTPUT_BAM_FILE_NAME_OPTION);
		group.addOption(OUTPUT_FILE_PREFIX_OPTION);
		group.addOption(TMP_DIR_OPTION);
		group.addOption(SAVE_TMP_DIR_OPTION);
		group.addOption(SHOULD_OUTPUT_REPORTS_OPTION);
		group.addOption(SHOULD_OUTPUT_FASTQ_OPTION);
		group.addOption(SHOULD_NOT_EXTEND_READS_TO_PRIMERS);
		group.addOption(NUM_PROCESSORS_OPTION);
		group.addOption(UID_LENGTH_OPTION);

		return group;
	}

}
