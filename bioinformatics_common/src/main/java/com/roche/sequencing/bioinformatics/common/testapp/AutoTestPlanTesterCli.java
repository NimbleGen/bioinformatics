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
package com.roche.sequencing.bioinformatics.common.testapp;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Files;
import com.roche.sequencing.bioinformatics.common.commandline.Command;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOption;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOptionsGroup;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineParser;
import com.roche.sequencing.bioinformatics.common.commandline.Commands;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class AutoTestPlanTesterCli {

	public final static String TEST_COMMAND_NAME = "test";

	public final static CommandLineOption USAGE_OPTION = new CommandLineOption("Print Usage", "usage", 'h', "Print Usage.", false, true);
	public final static CommandLineOption CONSOLE_OUTPUT_FILE_OPTION = new CommandLineOption("Console Output File", "consoleOutput", null,
			"File whose contents should be read and printed to the console output.", false, false);
	public final static CommandLineOption CONSOLE_ERRORS_OUTPUT_FILE_OPTION = new CommandLineOption("Console Errors File", "consoleErrors", null,
			"File whose contents should be read and printed to the console errors output.", false, false);
	public final static CommandLineOption OUTPUT_FILE_OPTION = new CommandLineOption("Output File", "outputFile", null, "File which will be copied to the output directory.", false, false);
	public final static CommandLineOption OUTPUT_DIR_OPTION = new CommandLineOption("Output Directory", "outputDir", null, "Directory where output file should be copied.", false, false);

	public final static String FILE_LOGGER_NAME = "root";

	public static void main(String[] args) {
		try {
			runCommandLineApp(args);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}

	public static void runCommandLineApp(String[] args) throws IOException {
		Commands commands = getCommands();
		ParsedCommandLine parsedCommandLine = CommandLineParser.parseCommandLine(args, commands);
		Command activeCommand = parsedCommandLine.getActiveCommand();
		CommandLineParser.throwCommandLineParsingExceptions(parsedCommandLine);
		if (activeCommand == null) {
			throw new IllegalStateException("No command was provided.");
		} else {
			if (activeCommand.getCommandName().equals(TEST_COMMAND_NAME)) {
				runTest(parsedCommandLine);
			} else {
				throw new AssertionError();
			}
		}

	}

	private static Commands getCommands() {
		Commands commands = new Commands("Available Commands:" + StringUtil.NEWLINE + "Command" + StringUtil.TAB + StringUtil.TAB + "Description" + StringUtil.NEWLINE + "------" + StringUtil.TAB
				+ StringUtil.TAB + "-----------");
		commands.addCommand(new Command(TEST_COMMAND_NAME, "Create the designated console output, console errors output and output files..", getCommandLineOptionsGroupForTest()));
		return commands;
	}

	public static CommandLineOptionsGroup getCommandLineOptionsGroupForTest() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup();
		group.addOption(USAGE_OPTION);
		group.addOption(CONSOLE_OUTPUT_FILE_OPTION);
		group.addOption(CONSOLE_ERRORS_OUTPUT_FILE_OPTION);
		group.addOption(OUTPUT_FILE_OPTION);
		group.addOption(OUTPUT_DIR_OPTION);
		return group;
	}

	public static void runTest(ParsedCommandLine parsedCommandLine) throws IOException {
		String consoleOutputFileAsString = parsedCommandLine.getOptionsValue(CONSOLE_OUTPUT_FILE_OPTION);
		if (consoleOutputFileAsString != null && !consoleOutputFileAsString.isEmpty()) {
			File consoleOutputFile = new File(consoleOutputFileAsString);
			if (consoleOutputFile.exists()) {
				String consoleOutput = FileUtil.readFileAsString(consoleOutputFile);
				System.out.println(consoleOutput);
			}
		}

		String consoleErrorsOutputFileAsString = parsedCommandLine.getOptionsValue(CONSOLE_ERRORS_OUTPUT_FILE_OPTION);
		if (consoleErrorsOutputFileAsString != null && !consoleErrorsOutputFileAsString.isEmpty()) {
			File consoleErrorsOutputFile = new File(consoleErrorsOutputFileAsString);
			if (consoleErrorsOutputFile.exists()) {
				String consoleErrorsOutput = FileUtil.readFileAsString(consoleErrorsOutputFile);
				System.err.println(consoleErrorsOutput);
			}
		}

		String outputFileAsString = parsedCommandLine.getOptionsValue(OUTPUT_FILE_OPTION);
		String outputDirAsString = parsedCommandLine.getOptionsValue(OUTPUT_DIR_OPTION);
		if (outputFileAsString != null && !outputFileAsString.isEmpty() && outputDirAsString != null && !outputDirAsString.isEmpty()) {
			File outputFile = new File(outputFileAsString);
			File outputDir = new File(outputDirAsString);
			if (outputFile.exists()) {
				FileUtil.createDirectory(outputDir);
				File newFile = new File(outputDir, outputFile.getName());
				Files.copy(outputFile, newFile);
			}
		}

	}
}
