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

import com.roche.sequencing.bioinformatics.common.commandline.Command;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineParser;
import com.roche.sequencing.bioinformatics.common.commandline.Commands;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FunGeneralErrors;
import com.roche.sequencing.bioinformatics.common.utils.LoggingUtil;
import com.roche.sequencing.bioinformatics.common.utils.ManifestUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class HsqUtilsCli {

	public final static String APPLICATION_NAME = "HSQUtils";
	public final static String JAR_FILE_NAME = "hsqutils.jar";
	private static String applicationVersionFromManifest = "unversioned--currently running in eclipse";

	public final static String TRIM_COMMAND_NAME = "trim";
	public final static String DEDUPLICATION_COMMAND_NAME = "dedup";

	public final static String FILE_LOGGER_NAME = "root";

	public static void main(String[] args) {
		String funError = FunGeneralErrors.getFunError();
		try {
			runCommandLineApp(args);
		} catch (Throwable t) {
			File logFile = LoggingUtil.getLogFile();

			String underlineFrame = StringUtil.repeatString("_", funError.length());
			CliStatusConsole.logError(underlineFrame);
			CliStatusConsole.logError("");
			CliStatusConsole.logError(funError);
			CliStatusConsole.logError("");
			CliStatusConsole.logError(t);
			if (logFile != null) {
				CliStatusConsole.logError("You may find additional details regarding your error in the log file [" + logFile.getAbsolutePath() + "].");
			} else {
				t.printStackTrace();
			}
			CliStatusConsole.logError("If you are unable to fix this issue and believe the application is in error please contact technical support at \"http://www.nimblegen.com/contact\".");
			CliStatusConsole.logError("");
			CliStatusConsole.logError(underlineFrame);
			CliStatusConsole.logError("");
			System.exit(1);
		}
	}

	public static void runCommandLineApp(String[] args) {
		String version = ManifestUtil.getManifestValue("version");
		if (version != null) {
			applicationVersionFromManifest = version;
		}

		Commands commands = getCommands();
		ParsedCommandLine parsedCommandLine = CommandLineParser.parseCommandLine(args, commands);

		boolean showVersion = parsedCommandLine.isOptionPresent(DeduplicationCli.VERSION_OPTION);
		if (showVersion) {
			CliStatusConsole.logStatus("Version: " + applicationVersionFromManifest);
		} else {
			String commandLineSignature = CommandLineParser.getCommandLineCallSignature(JAR_FILE_NAME, args, true);
			CliStatusConsole.logStatus("");
			CliStatusConsole.logStatus("---------------------------------");
			CliStatusConsole.logStatus("Roche NimbleGen HeatSeq Utilities (version:" + applicationVersionFromManifest + ")");
			CliStatusConsole.logStatus("---------------------------------");
			CliStatusConsole.logStatus("");
			CliStatusConsole.logStatus("The command line you typed was interpreted as follows:");
			CliStatusConsole.logStatus(commandLineSignature);
			CliStatusConsole.logStatus("");

			Command activeCommand = parsedCommandLine.getActiveCommand();
			boolean noOptionsProvided = (args.length == 0);
			boolean onlyCommandOptionProvided = (activeCommand != null) && (args.length == 1);
			boolean showUsage = parsedCommandLine.isOptionPresent(DeduplicationCli.USAGE_OPTION) || noOptionsProvided || onlyCommandOptionProvided;

			if (showUsage) {
				if (activeCommand != null) {
					CliStatusConsole.logStatus(activeCommand.getUsage());
				} else {
					if (noOptionsProvided) {
						CliStatusConsole.logStatus("");
						CliStatusConsole.logStatus("___________________________________________________________________");
						CliStatusConsole.logStatus("");
						CliStatusConsole.logStatus("A command was not provided.  Please select from the commands below.");
						CliStatusConsole.logStatus("As an example, trimming would be executed as follows:");
						CliStatusConsole.logStatus(APPLICATION_NAME + " " + TRIM_COMMAND_NAME + " <trim specific arguments>");
						CliStatusConsole.logStatus("");
					} else {
						CliStatusConsole.logStatus("");
						CliStatusConsole.logStatus("___________________________________________________________________");
						CliStatusConsole.logStatus("");
					}
					CliStatusConsole.logStatus(commands.getUsage());
					CliStatusConsole.logStatus("___________________________________________________________________");
					CliStatusConsole.logStatus("");
				}

			} else {
				CommandLineParser.throwCommandLineParsingExceptions(parsedCommandLine);

				if (activeCommand != null) {
					if (activeCommand.getCommandName().equals(TRIM_COMMAND_NAME)) {
						TrimCli.trim(parsedCommandLine, commandLineSignature, APPLICATION_NAME, applicationVersionFromManifest);
					} else if (activeCommand.getCommandName().equals(DEDUPLICATION_COMMAND_NAME)) {
						DeduplicationCli.identifyDuplicates(parsedCommandLine, commandLineSignature, APPLICATION_NAME, applicationVersionFromManifest);
					} else {
						throw new AssertionError();
					}
				}

			}
		}

	}

	private static Commands getCommands() {
		Commands commands = new Commands("Available Commands:" + StringUtil.NEWLINE + "Command" + StringUtil.TAB + StringUtil.TAB + "Description" + StringUtil.NEWLINE + "------" + StringUtil.TAB
				+ StringUtil.TAB + "-----------");
		commands.addCommand(new Command(TRIM_COMMAND_NAME, "Trim reads within the fastq files to represent the capture target regions.", TrimCli.getCommandLineOptionsGroupForTrimming()));
		commands.addCommand(new Command(DEDUPLICATION_COMMAND_NAME,
				"Eliminate or Identify duplicate reads and include only portions of the read that overlap with the capture target sequence in the sequence alignment (BAM file).", DeduplicationCli
						.getCommandLineOptionsGroup()));
		return commands;
	}

	public static File getLogFile(File outputDirectory, String outputFilePrefix, String applicationName, String command) {
		if (outputFilePrefix == null) {
			outputFilePrefix = "";
		}
		String logFileName = outputFilePrefix;
		logFileName = logFileName + applicationName + "_" + command + "_" + DateUtil.getCurrentDateINYYYY_MM_DD_HH_MM_SS() + ".log";
		logFileName = logFileName.replaceAll(" ", "_");
		logFileName = logFileName.replaceAll("/", "_");
		logFileName = logFileName.replaceAll(":", "-");
		File logFile = new File(outputDirectory, logFileName);
		return logFile;
	}

	public static String getTempPrefix(String applicationName, String outputFilePrefix) {
		String tempPrefix = outputFilePrefix;
		if (!tempPrefix.isEmpty()) {
			tempPrefix += applicationName + "_";
		} else {
			tempPrefix = applicationName + "_";
		}
		return tempPrefix;
	}

}
