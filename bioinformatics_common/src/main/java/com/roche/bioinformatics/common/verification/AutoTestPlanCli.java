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
package com.roche.bioinformatics.common.verification;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.bioinformatics.common.verification.runs.TestPlan;
import com.roche.sequencing.bioinformatics.common.commandline.Command;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOption;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOptionsGroup;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineParser;
import com.roche.sequencing.bioinformatics.common.commandline.Commands;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.FunGeneralErrors;
import com.roche.sequencing.bioinformatics.common.utils.LoggingUtil;
import com.roche.sequencing.bioinformatics.common.utils.ManifestUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class AutoTestPlanCli {

	public final static String APPLICATION_NAME = "AutoTestPlan";
	public final static String JAR_FILE_NAME = "autotestplan.jar";
	private static String applicationVersionFromManifest = "unversioned--currently running in eclipse";

	public final static String TEST_PLAN_COMMAND_NAME = "testplan";
	public final static String TEST_PLAN_REPORT_COMMAND_NAME = "report";

	public final static CommandLineOption USAGE_OPTION = new CommandLineOption("Print Usage", "usage", 'h', "Print Usage.", false, true);
	public final static CommandLineOption TEST_PLAN_DIRECTORY_OPTION = new CommandLineOption("Test Plan Directory", "testPlanDir", null, "Path to the Test Plan Directory", true, false);
	public final static CommandLineOption TEST_PLAN_EXECUTION_RESULTS_DIRECTORY_OPTION = new CommandLineOption("Test Plan Execution Directory", "runDir", null,
			"Base directory for placing results file from running the test plan", true, false);
	public final static CommandLineOption APPLICATION_JAR_FILE_OPTION = new CommandLineOption("Application Jar File", "application", null, "The path to the application jar file to be tested.", true,
			false);
	public final static CommandLineOption JVM_BIN_PATH_OPTION = new CommandLineOption("JVM Bin Path", "jvm", null, "The path to the jvm bin to use for executing the tests.", false, false);
	public final static CommandLineOption OUTPUT_FILE_OPTION = new CommandLineOption("Output File", "output", null, "The output file to write the test plan or report.", true, false);

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

	public static String getApplicationVersion() {
		return applicationVersionFromManifest;
	}

	public static void runCommandLineApp(String[] args) {
		String version = ManifestUtil.getManifestValue("version");
		if (version != null) {
			applicationVersionFromManifest = version;
		}

		String commandLineSignature = CommandLineParser.getCommandLineCallSignature(JAR_FILE_NAME, args, true);
		CliStatusConsole.logStatus("");
		CliStatusConsole.logStatus("---------------------------------");
		CliStatusConsole.logStatus("Roche NimbleGen AutoTestPlan (version:" + applicationVersionFromManifest + ")");
		CliStatusConsole.logStatus("---------------------------------");
		CliStatusConsole.logStatus("");
		CliStatusConsole.logStatus("The command line you typed was interpreted as follows:");
		CliStatusConsole.logStatus(commandLineSignature);
		CliStatusConsole.logStatus("");
		Commands commands = getCommands();
		ParsedCommandLine parsedCommandLine = CommandLineParser.parseCommandLine(args, commands);
		Command activeCommand = parsedCommandLine.getActiveCommand();
		boolean noOptionsProvided = (args.length == 0);
		boolean onlyCommandOptionProvided = (activeCommand != null) && (args.length == 1);
		boolean showUsage = parsedCommandLine.isOptionPresent(USAGE_OPTION) || noOptionsProvided || onlyCommandOptionProvided;

		if (showUsage) {
			if (activeCommand != null) {
				CliStatusConsole.logStatus(activeCommand.getUsage());
			} else {
				if (noOptionsProvided) {
					CliStatusConsole.logStatus("");
					CliStatusConsole.logStatus("___________________________________________________________________");
					CliStatusConsole.logStatus("");
					CliStatusConsole.logStatus("A command was not provided.  Please select from the commands below.");
					CliStatusConsole.logStatus("As an example, a Test Plan would be generated by executing the following:");
					CliStatusConsole.logStatus(APPLICATION_NAME + " " + TEST_PLAN_COMMAND_NAME + " <test plan specific arguments>");
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
				if (activeCommand.getCommandName().equals(TEST_PLAN_COMMAND_NAME)) {
					runTestPlan(parsedCommandLine, commandLineSignature, APPLICATION_NAME, applicationVersionFromManifest, false);
				} else if (activeCommand.getCommandName().equals(TEST_PLAN_REPORT_COMMAND_NAME)) {
					runTestPlan(parsedCommandLine, commandLineSignature, APPLICATION_NAME, applicationVersionFromManifest, true);
				} else {
					throw new AssertionError();
				}
			}

		}

	}

	private static Commands getCommands() {
		Commands commands = new Commands("Available Commands:" + StringUtil.NEWLINE + "Command" + StringUtil.TAB + StringUtil.TAB + "Description" + StringUtil.NEWLINE + "------" + StringUtil.TAB
				+ StringUtil.TAB + "-----------");
		commands.addCommand(new Command(TEST_PLAN_COMMAND_NAME, "Create a Test Plan based on the provided Test Plan directory.", getCommandLineOptionsGroupForTestPlan()));
		commands.addCommand(new Command(TEST_PLAN_REPORT_COMMAND_NAME, "Create a Test Plan Report (which will actually run the test plan) based on the provided Test Plan directory.",
				getCommandLineOptionsGroupForTestPlanReport()));
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

	public static CommandLineOptionsGroup getCommandLineOptionsGroupForTestPlan() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup();
		group.addOption(USAGE_OPTION);
		group.addOption(TEST_PLAN_DIRECTORY_OPTION);
		group.addOption(OUTPUT_FILE_OPTION);
		return group;
	}

	public static CommandLineOptionsGroup getCommandLineOptionsGroupForTestPlanReport() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup();
		group.addOption(USAGE_OPTION);
		group.addOption(TEST_PLAN_DIRECTORY_OPTION);
		group.addOption(TEST_PLAN_EXECUTION_RESULTS_DIRECTORY_OPTION);
		group.addOption(APPLICATION_JAR_FILE_OPTION);
		group.addOption(OUTPUT_FILE_OPTION);
		group.addOption(JVM_BIN_PATH_OPTION);
		return group;
	}

	public static File getLogFile(File outputFile, String applicationName, String command) {
		String logFileName = applicationName + "_" + command + "_" + DateUtil.getCurrentDateINYYYY_MM_DD_HH_MM_SS() + ".log";
		logFileName = logFileName.replaceAll(" ", "_");
		logFileName = logFileName.replaceAll("/", "_");
		logFileName = logFileName.replaceAll(":", "-");
		File logFile = new File(outputFile.getParentFile(), logFileName);
		return logFile;
	}

	public static void runTestPlan(ParsedCommandLine parsedCommandLine, String commandLineSignature, String applicationName, String applicationVersion, boolean generateReport) {
		long applicationStart = System.currentTimeMillis();
		CliStatusConsole.logStatus("The generation of the Test Plan has started at " + DateUtil.convertTimeInMillisecondsToDate(applicationStart) + "(YYYY/MM/DD HH:MM:SS)." + StringUtil.NEWLINE);

		String outputFileString = parsedCommandLine.getOptionsValue(OUTPUT_FILE_OPTION);

		File outputFile = null;
		if (outputFileString != null) {
			outputFile = new File(outputFileString);
			if (!outputFile.exists()) {
				try {
					FileUtil.createNewFile(outputFile);
				} catch (IOException e) {
					throw new IllegalStateException("Could not create provided output file[" + outputFile.getAbsolutePath() + "].", e);
				}
			}
		}

		File logFile = getLogFile(outputFile, applicationName, parsedCommandLine.getActiveCommand().getCommandName());
		try {
			LoggingUtil.setLogFile(FILE_LOGGER_NAME, logFile);
		} catch (IOException e2) {
			throw new IllegalStateException("Unable to create log file at " + logFile.getAbsolutePath() + ".", e2);
		}

		File testPlanDirectory = new File(parsedCommandLine.getOptionsValue(TEST_PLAN_DIRECTORY_OPTION));

		if (!testPlanDirectory.isDirectory()) {
			throw new IllegalArgumentException("The provided file for " + TEST_PLAN_DIRECTORY_OPTION.getLongFormOption() + " is not a directory.");
		}

		if (!testPlanDirectory.exists()) {
			throw new IllegalArgumentException("The provided file for " + TEST_PLAN_DIRECTORY_OPTION.getLongFormOption() + " does not exist.");
		}

		Logger logger = LoggerFactory.getLogger(AutoTestPlanCli.class);
		logger.info(applicationName + " version:" + applicationVersion);
		logger.info("command line signature: " + commandLineSignature);

		TestPlan testPlan = TestPlan.readFromDirectory(testPlanDirectory);
		if (generateReport) {
			File jvmBinFile = null;
			if (parsedCommandLine.isOptionPresent(JVM_BIN_PATH_OPTION)) {
				String jvmBinPath = parsedCommandLine.getOptionsValue(JVM_BIN_PATH_OPTION);

				jvmBinFile = new File(jvmBinPath);
				if (!jvmBinFile.exists()) {
					throw new IllegalArgumentException("The provided argument for " + JVM_BIN_PATH_OPTION.getLongFormOption() + "[" + jvmBinPath + "] does not exist.");
				}
			}

			String appToTestString = parsedCommandLine.getOptionsValue(APPLICATION_JAR_FILE_OPTION);
			File appToTest = new File(appToTestString);

			if (!appToTest.exists()) {
				throw new IllegalArgumentException("The provided argument for " + APPLICATION_JAR_FILE_OPTION.getLongFormOption() + "[" + appToTestString + "] does not exist.");
			}

			File testPlanExecutionDirectory = new File(parsedCommandLine.getOptionsValue(TEST_PLAN_EXECUTION_RESULTS_DIRECTORY_OPTION));

			testPlan.createTestPlanReport(appToTest, testPlanExecutionDirectory, outputFile, jvmBinFile);
		} else {
			testPlan.createTestPlan(outputFile);
		}

	}

}
