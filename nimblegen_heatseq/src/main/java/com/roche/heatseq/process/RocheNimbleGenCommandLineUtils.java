package com.roche.heatseq.process;

import java.io.File;
import java.io.IOException;

import com.roche.sequencing.bioinformatics.common.commandline.Command;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOptionsGroup;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineParser;
import com.roche.sequencing.bioinformatics.common.commandline.Commands;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.ManifestUtil;

public class RocheNimbleGenCommandLineUtils {

	public final static String APPLICATION_NAME = "HSQUtils";
	private static String applicationVersionFromManifest = "unversioned";

	private final static String TRIM_COMMAND_NAME = "Trim";
	private final static String IDENTIFY_DUPLICATES_COMMAND_NAME = "IdentifyDuplicates";

	public static void main(String[] args) {
		runCommandLineApp(args);
	}

	static void runCommandLineApp(String[] args) {
		String version = ManifestUtil.getManifestValue("version");
		if (version != null) {
			applicationVersionFromManifest = version;
		}
		outputToConsole("Roche NimbleGen Command Line Utilities (version:" + applicationVersionFromManifest + ")");

		String commandLineSignature = CommandLineParser.getCommandLineCallSignature(APPLICATION_NAME, args, true);
		outputToConsole(commandLineSignature);
		outputToConsole("");

		Commands commands = getCommands();
		ParsedCommandLine parsedCommandLine = CommandLineParser.parseCommandLine(args, commands);
		Command activeCommand = parsedCommandLine.getActiveCommand();
		boolean noOptionsProvided = (args.length == 0);
		boolean onlyCommandOptionProvided = (activeCommand != null) && (args.length == 1);
		boolean showUsage = parsedCommandLine.isOptionPresent(PrefuppCli.USAGE_OPTION) || noOptionsProvided || onlyCommandOptionProvided;

		if (showUsage) {
			if (activeCommand != null) {
				outputToConsole(activeCommand.getUsage());
			} else {
				outputToConsole(commands.getUsage());
			}

		} else {
			CommandLineParser.throwCommandLineParsingExceptions(parsedCommandLine);

			if (activeCommand != null) {
				if (activeCommand.getCommandName().equals(TRIM_COMMAND_NAME)) {
					trim(parsedCommandLine);
				} else if (activeCommand.getCommandName().equals(IDENTIFY_DUPLICATES_COMMAND_NAME)) {
					PrefuppCli.identifyDuplicates(parsedCommandLine, commandLineSignature);
				} else {
					throw new AssertionError();
				}
			}

		}

	}

	private static void trim(ParsedCommandLine parsedCommandLine) {
		String outputDirectoryString = parsedCommandLine.getOptionsValue(PrefuppCli.OUTPUT_DIR_OPTION);
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

		String outputFilePrefix = parsedCommandLine.getOptionsValue(PrefuppCli.OUTPUT_FILE_PREFIX_OPTION);
		if (outputFilePrefix == null) {
			outputFilePrefix = "";
		}

		File fastQ1File = new File(parsedCommandLine.getOptionsValue(PrefuppCli.FASTQ_ONE_OPTION));

		if (!fastQ1File.exists()) {
			throw new IllegalStateException("Unable to find provided FASTQ1 file[" + fastQ1File.getAbsolutePath() + "].");
		}

		File fastQ2File = new File(parsedCommandLine.getOptionsValue(PrefuppCli.FASTQ_TWO_OPTION));

		if (!fastQ2File.exists()) {
			throw new IllegalStateException("Unable to find provided FASTQ2 file[" + fastQ2File.getAbsolutePath() + "].");
		}

		if (fastQ1File.getAbsolutePath().equals(fastQ2File.getAbsolutePath())) {
			throw new IllegalStateException("The same file[" + fastQ2File.getAbsolutePath() + "] was provided for FASTQ1 and FASTQ2.");
		}

		File probeFile = new File(parsedCommandLine.getOptionsValue(PrefuppCli.PROBE_OPTION));

		if (!probeFile.exists()) {
			throw new IllegalStateException("Unable to find provided PROBE file[" + probeFile.getAbsolutePath() + "].");
		}

		File outputFastQ1File = new File(outputDirectory, outputFilePrefix + "trimmed_" + FileUtil.getFileNameWithoutExtension(fastQ1File.getName()) + ".fastq");
		File outputFastQ2File = new File(outputDirectory, outputFilePrefix + "trimmed_" + FileUtil.getFileNameWithoutExtension(fastQ2File.getName()) + ".fastq");

		try {
			FastqReadTrimmer.trimReads(fastQ1File, fastQ2File, probeFile, PrefuppCli.DEFAULT_EXTENSION_UID_LENGTH, PrefuppCli.DEFAULT_LIGATION_UID_LENGTH, 0, 0, outputFastQ1File, outputFastQ2File);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

	}

	private static Commands getCommands() {
		Commands commands = new Commands("Command Line Usage:");
		commands.addCommand(new Command(TRIM_COMMAND_NAME, "Trim reads within the fastq files to represent the capture target regions.", getCommandLineOptionsGroupForTrimming()));
		commands.addCommand(new Command(IDENTIFY_DUPLICATES_COMMAND_NAME,
				"Identify duplicate reads and include only portions of the read that overlap with the capture target sequence in the sequence alignment (BAM file).", PrefuppCli
						.getCommandLineOptionsGroup()));
		return commands;
	}

	private static CommandLineOptionsGroup getCommandLineOptionsGroupForTrimming() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup();
		group.addOption(PrefuppCli.USAGE_OPTION);
		group.addOption(PrefuppCli.FASTQ_ONE_OPTION);
		group.addOption(PrefuppCli.FASTQ_TWO_OPTION);
		group.addOption(PrefuppCli.PROBE_OPTION);
		group.addOption(PrefuppCli.OUTPUT_DIR_OPTION);
		group.addOption(PrefuppCli.OUTPUT_FILE_PREFIX_OPTION);
		return group;
	}

	private static void outputToConsole(String output) {
		System.out.println(output);
	}
}
