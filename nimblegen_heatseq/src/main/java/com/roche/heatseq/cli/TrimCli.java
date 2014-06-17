package com.roche.heatseq.cli;

import java.io.File;
import java.io.IOException;

import com.roche.heatseq.process.FastqReadTrimmer;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOptionsGroup;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;

public class TrimCli {

	public static void trim(ParsedCommandLine parsedCommandLine) {
		String outputDirectoryString = parsedCommandLine.getOptionsValue(IdentifyDuplicatesCli.OUTPUT_DIR_OPTION);
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

		String outputFilePrefix = parsedCommandLine.getOptionsValue(IdentifyDuplicatesCli.OUTPUT_FILE_PREFIX_OPTION);
		if (outputFilePrefix == null) {
			outputFilePrefix = "";
		}

		File fastQ1File = new File(parsedCommandLine.getOptionsValue(IdentifyDuplicatesCli.FASTQ_ONE_OPTION));

		if (!fastQ1File.exists()) {
			throw new IllegalStateException("Unable to find provided FASTQ1 file[" + fastQ1File.getAbsolutePath() + "].");
		}

		File fastQ2File = new File(parsedCommandLine.getOptionsValue(IdentifyDuplicatesCli.FASTQ_TWO_OPTION));

		if (!fastQ2File.exists()) {
			throw new IllegalStateException("Unable to find provided FASTQ2 file[" + fastQ2File.getAbsolutePath() + "].");
		}

		if (fastQ1File.getAbsolutePath().equals(fastQ2File.getAbsolutePath())) {
			throw new IllegalStateException("The same file[" + fastQ2File.getAbsolutePath() + "] was provided for FASTQ1 and FASTQ2.");
		}

		File probeFile = new File(parsedCommandLine.getOptionsValue(IdentifyDuplicatesCli.PROBE_OPTION));

		if (!probeFile.exists()) {
			throw new IllegalStateException("Unable to find provided PROBE file[" + probeFile.getAbsolutePath() + "].");
		}

		File outputFastQ1File = new File(outputDirectory, outputFilePrefix + "trimmed_" + FileUtil.getFileNameWithoutExtension(fastQ1File.getName()) + ".fastq");
		File outputFastQ2File = new File(outputDirectory, outputFilePrefix + "trimmed_" + FileUtil.getFileNameWithoutExtension(fastQ2File.getName()) + ".fastq");

		try {
			FastqReadTrimmer.trimReads(fastQ1File, fastQ2File, probeFile, IdentifyDuplicatesCli.DEFAULT_EXTENSION_UID_LENGTH, IdentifyDuplicatesCli.DEFAULT_LIGATION_UID_LENGTH, 0, 0,
					outputFastQ1File, outputFastQ2File);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

	}

	public static CommandLineOptionsGroup getCommandLineOptionsGroupForTrimming() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup();
		group.addOption(IdentifyDuplicatesCli.USAGE_OPTION);
		group.addOption(IdentifyDuplicatesCli.FASTQ_ONE_OPTION);
		group.addOption(IdentifyDuplicatesCli.FASTQ_TWO_OPTION);
		group.addOption(IdentifyDuplicatesCli.PROBE_OPTION);
		group.addOption(IdentifyDuplicatesCli.OUTPUT_DIR_OPTION);
		group.addOption(IdentifyDuplicatesCli.OUTPUT_FILE_PREFIX_OPTION);
		return group;
	}

}
