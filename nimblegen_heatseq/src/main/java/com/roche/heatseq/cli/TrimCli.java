package com.roche.heatseq.cli;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.process.FastqReadTrimmer;
import com.roche.heatseq.qualityreport.LoggingUtil;
import com.roche.heatseq.utils.ProbeFileUtil;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOptionsGroup;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class TrimCli {

	private static final Logger logger = LoggerFactory.getLogger(TrimCli.class);
	private static final String FASTQ_EXTENSION = ".fastq";

	public static void trim(ParsedCommandLine parsedCommandLine, String commandLineSignature, String applicationName, String applicationVersion) {
		long applicationStart = System.currentTimeMillis();
		CliStatusConsole.logStatus("Trimming has started at " + DateUtil.convertTimeInMillisecondsToDate(applicationStart) + "." + StringUtil.NEWLINE);

		String outputDirectoryString = parsedCommandLine.getOptionsValue(DeduplicationCli.OUTPUT_DIR_OPTION);

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

		String outputFilePrefix = parsedCommandLine.getOptionsValue(DeduplicationCli.OUTPUT_FILE_PREFIX_OPTION);
		if (outputFilePrefix == null) {
			outputFilePrefix = "";
		} else if (!outputFilePrefix.isEmpty() && outputFilePrefix.charAt(outputFilePrefix.length() - 1) != '_') {
			// Add an underscore as a separation character for the prefix if there is not already an underscore as the last prefix character
			outputFilePrefix = outputFilePrefix + "_";
		}

		File fastQ1File = new File(parsedCommandLine.getOptionsValue(DeduplicationCli.FASTQ_ONE_OPTION));

		if (!fastQ1File.exists()) {
			throw new IllegalStateException("Unable to find provided FASTQ1 file[" + fastQ1File.getAbsolutePath() + "].");
		}

		File fastQ2File = new File(parsedCommandLine.getOptionsValue(DeduplicationCli.FASTQ_TWO_OPTION));

		if (!fastQ2File.exists()) {
			throw new IllegalStateException("Unable to find provided FASTQ2 file[" + fastQ2File.getAbsolutePath() + "].");
		}

		if (fastQ1File.getAbsolutePath().equals(fastQ2File.getAbsolutePath())) {
			throw new IllegalStateException("The same file[" + fastQ2File.getAbsolutePath() + "] was provided for FASTQ1 and FASTQ2.");
		}

		File probeFile = new File(parsedCommandLine.getOptionsValue(DeduplicationCli.PROBE_OPTION));

		if (!probeFile.exists()) {
			throw new IllegalStateException("Unable to find provided PROBE file[" + probeFile.getAbsolutePath() + "].");
		}

		String outputFastQ1FileName = new File(outputDirectory, outputFilePrefix + "trimmed_" + FileUtil.getFileNameWithoutExtension(fastQ1File.getName())).getAbsolutePath();
		if (!outputFastQ1FileName.toLowerCase().endsWith(FASTQ_EXTENSION)) {
			outputFastQ1FileName += FASTQ_EXTENSION;
		}
		File outputFastQ1File = new File(outputFastQ1FileName);

		String outputFastQ2FileName = new File(outputDirectory, outputFilePrefix + "trimmed_" + FileUtil.getFileNameWithoutExtension(fastQ2File.getName())).getAbsolutePath();
		if (!outputFastQ2FileName.toLowerCase().endsWith(FASTQ_EXTENSION)) {
			outputFastQ2FileName += FASTQ_EXTENSION;
		}
		File outputFastQ2File = new File(outputFastQ2FileName);

		File logFile = HsqUtilsCli.getLogFile(outputDirectory, outputFilePrefix, applicationName, "trim");
		try {
			LoggingUtil.setLogFile(HsqUtilsCli.FILE_LOGGER_NAME, logFile);
		} catch (IOException e2) {
			throw new IllegalStateException("Unable to create log file at " + logFile.getAbsolutePath() + ".", e2);
		}

		logger.info(applicationName + " version:" + applicationVersion);
		logger.info("command line signature: " + commandLineSignature);

		try {
			FastqReadTrimmer.trimReads(fastQ1File, fastQ2File, probeFile, DeduplicationCli.DEFAULT_EXTENSION_UID_LENGTH, DeduplicationCli.DEFAULT_LIGATION_UID_LENGTH, outputFastQ1File,
					outputFastQ2File);

			long applicationStop = System.currentTimeMillis();
			CliStatusConsole.logStatus(StringUtil.NEWLINE + "Trimming has completed succesfully.");
			CliStatusConsole.logStatus("Start Time: " + DateUtil.convertTimeInMillisecondsToDate(applicationStart) + "  Stop Time: " + DateUtil.convertTimeInMillisecondsToDate(applicationStop)
					+ "  Total Time  (hh:mm:ss): " + DateUtil.convertMillisecondsToHHMMSS(applicationStop - applicationStart) + StringUtil.NEWLINE);

			String genomeNameFromProbeInfoFile = ProbeFileUtil.extractGenomeNameInLowerCase(probeFile);
			if (genomeNameFromProbeInfoFile != null) {
				CliStatusConsole.logStatus("Please make sure to use genome build [" + genomeNameFromProbeInfoFile
						+ "] when mapping these reads to ensure the mappings correspond with the correct locations defined in the probe information file.");
			}
		} catch (IOException e) {
			throw new IllegalStateException("Unable to trim reads from fastq1File[" + fastQ1File.getAbsolutePath() + "] and fastq2File[" + fastQ1File + "] using probe information file["
					+ probeFile.getAbsolutePath() + "].", e);
		}

	}

	public static CommandLineOptionsGroup getCommandLineOptionsGroupForTrimming() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup();
		group.addOption(DeduplicationCli.USAGE_OPTION);
		group.addOption(DeduplicationCli.FASTQ_ONE_OPTION);
		group.addOption(DeduplicationCli.FASTQ_TWO_OPTION);
		group.addOption(DeduplicationCli.PROBE_OPTION);
		group.addOption(DeduplicationCli.OUTPUT_DIR_OPTION);
		group.addOption(DeduplicationCli.OUTPUT_FILE_PREFIX_OPTION);
		return group;
	}

}
