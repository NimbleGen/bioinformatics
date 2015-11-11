/*
 *    Copyright 2013 Roche NimbleGen Inc.
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
import java.io.IOException;
import java.text.DecimalFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.ParsedProbeFile;
import com.roche.heatseq.process.FastqReadTrimmer;
import com.roche.heatseq.process.FastqValidator;
import com.roche.heatseq.process.InputFilesExistValidator;
import com.roche.heatseq.process.ProbeInfoFileValidator;
import com.roche.heatseq.utils.ProbeFileUtil;
import com.roche.heatseq.utils.ProbeFileUtil.ProbeHeaderInformation;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOptionsGroup;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.LoggingUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class TrimCli {

	private static final Logger logger = LoggerFactory.getLogger(TrimCli.class);
	private static final String FASTQ_EXTENSION = ".fastq";
	private static final int BYTES_PER_GIGABYTE = 1000000000;
	private final static DecimalFormat doubleFormatter = new DecimalFormat("#,###.##");

	public static void trim(ParsedCommandLine parsedCommandLine, String commandLineSignature, String applicationName, String applicationVersion) {
		long applicationStart = System.currentTimeMillis();
		CliStatusConsole.logStatus("Trimming has started at " + DateUtil.convertTimeInMillisecondsToDate(applicationStart) + "(YYYY/MM/DD HH:MM:SS)." + StringUtil.NEWLINE);

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

		File logFile = HsqUtilsCli.getLogFile(outputDirectory, outputFilePrefix, applicationName, "trim");
		try {
			LoggingUtil.setLogFile(HsqUtilsCli.FILE_LOGGER_NAME, logFile);
		} catch (IOException e2) {
			throw new IllegalStateException("Unable to create log file at " + logFile.getAbsolutePath() + ".", e2);
		}

		File fastQ1File = new File(parsedCommandLine.getOptionsValue(DeduplicationCli.FASTQ_ONE_OPTION));
		File fastQ2File = new File(parsedCommandLine.getOptionsValue(DeduplicationCli.FASTQ_TWO_OPTION));
		File probeFile = new File(parsedCommandLine.getOptionsValue(DeduplicationCli.PROBE_OPTION));

		InputFilesExistValidator.validate(fastQ1File, fastQ2File, probeFile);

		FastqValidator.validate(fastQ1File, fastQ2File);

		long requiredSpaceInBytes = fastQ1File.length() * 2;
		long usableSpaceInBytes = outputDirectory.getUsableSpace();
		if (usableSpaceInBytes <= requiredSpaceInBytes) {
			throw new IllegalStateException("The amount of storage space required by this applications output is " + doubleFormatter.format(requiredSpaceInBytes / BYTES_PER_GIGABYTE)
					+ "GB which is greater than the amount of usable space in the output directory[" + doubleFormatter.format(usableSpaceInBytes / BYTES_PER_GIGABYTE) + "GB].");
		}

		ParsedProbeFile probeInfo = ProbeInfoFileValidator.validateAndParseProbeInfoFile(probeFile);

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

		logger.info(applicationName + " version:" + applicationVersion);
		logger.info("command line signature: " + commandLineSignature);

		try {
			FastqReadTrimmer.trimReads(fastQ1File, fastQ2File, probeInfo, probeFile, outputFastQ1File, outputFastQ2File);

			long applicationStop = System.currentTimeMillis();
			CliStatusConsole.logStatus(StringUtil.NEWLINE + "Trimming has completed successfully.");
			CliStatusConsole.logStatus("Start Time: " + DateUtil.convertTimeInMillisecondsToDate(applicationStart) + "(YYYY/MM/DD HH:MM:SS)  Stop Time: "
					+ DateUtil.convertTimeInMillisecondsToDate(applicationStop) + "(YYYY/MM/DD HH:MM:SS)  Total Time: " + DateUtil.convertMillisecondsToHHMMSS(applicationStop - applicationStart)
					+ "(HH:MM:SS)" + StringUtil.NEWLINE);

			ProbeHeaderInformation probeHeader = ProbeFileUtil.extractProbeHeaderInformation(probeFile);
			String genomeNameFromProbeInfoFile = probeHeader.getGenomeName();
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
