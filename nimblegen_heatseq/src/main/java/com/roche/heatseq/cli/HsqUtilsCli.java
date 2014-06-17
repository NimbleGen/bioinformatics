package com.roche.heatseq.cli;

import com.roche.sequencing.bioinformatics.common.commandline.Command;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineParser;
import com.roche.sequencing.bioinformatics.common.commandline.Commands;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.utils.ManifestUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class HsqUtilsCli {

	public final static String APPLICATION_NAME = "HSQUtils";
	private static String applicationVersionFromManifest = "unversioned";

	private final static String TRIM_COMMAND_NAME = "Trim";
	private final static String IDENTIFY_DUPLICATES_COMMAND_NAME = "IdentifyDuplicates";

	public static void main(String[] args) {
		runCommandLineApp(args);
	}

	public static void runCommandLineApp(String[] args) {
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
		boolean showUsage = parsedCommandLine.isOptionPresent(IdentifyDuplicatesCli.USAGE_OPTION) || noOptionsProvided || onlyCommandOptionProvided;

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
					TrimCli.trim(parsedCommandLine);
				} else if (activeCommand.getCommandName().equals(IDENTIFY_DUPLICATES_COMMAND_NAME)) {
					IdentifyDuplicatesCli.identifyDuplicates(parsedCommandLine, commandLineSignature);
				} else {
					throw new AssertionError();
				}
			}

		}

	}

	private static Commands getCommands() {
		Commands commands = new Commands("Available Commands:" + StringUtil.NEWLINE + "Command" + StringUtil.TAB + "Description");
		commands.addCommand(new Command(TRIM_COMMAND_NAME, "Trim reads within the fastq files to represent the capture target regions.", TrimCli.getCommandLineOptionsGroupForTrimming()));
		commands.addCommand(new Command(IDENTIFY_DUPLICATES_COMMAND_NAME,
				"Identify duplicate reads and include only portions of the read that overlap with the capture target sequence in the sequence alignment (BAM file).", IdentifyDuplicatesCli
						.getCommandLineOptionsGroup()));
		return commands;
	}

	private static void outputToConsole(String output) {
		System.out.println(output);
	}
}
