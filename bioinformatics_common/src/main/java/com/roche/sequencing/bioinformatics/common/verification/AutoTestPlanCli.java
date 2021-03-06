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
package com.roche.sequencing.bioinformatics.common.verification;

import java.io.File;
import java.io.IOException;

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
import com.roche.sequencing.bioinformatics.common.verification.runs.TestPlan;

public class AutoTestPlanCli {

	public final static String APPLICATION_NAME = "AutoTestPlan";
	private final static String JAR_FILE_NAME = "autotestplan.jar";
	private static String applicationVersionFromManifest = "unversioned--currently running in eclipse";

	private final static String TEST_PLAN_COMMAND_NAME = "testplan";
	private final static String TEST_PLAN_REPORT_COMMAND_NAME = "report";

	private final static CommandLineOption USAGE_OPTION = new CommandLineOption("Print Usage", "usage", 'h', "Print Usage.", false, true);
	private final static CommandLineOption TEST_PLAN_DIRECTORY_OPTION = new CommandLineOption("Test Plan Directory", "testPlanDir", null, "Path to the Test Plan Directory", true, false);
	private final static CommandLineOption TEST_PLAN_EXECUTION_RESULTS_DIRECTORY_OPTION = new CommandLineOption("Test Plan Execution Directory", "runDir", null,
			"Base directory for placing results file from running the test plan", true, false);
	private final static CommandLineOption APPLICATION_JAR_FILE_OPTION = new CommandLineOption("Application Jar File", "application", null, "The path to the application jar file to be tested.", true,
			false);
	private final static CommandLineOption APPLICATION_NAME_OPTION = new CommandLineOption("Application Name", "applicationName", null,
			"The name of the application jar file to be tested that will be displayed in the test plan.", true, false);
	private final static CommandLineOption JVM_BIN_PATH_OPTION = new CommandLineOption("JVM Bin Path", "jvm", null,
			"The path to the jvm bin to use for executing the tests.  If not provided, the application will use the default JVM.", false, false);
	private final static CommandLineOption OUTPUT_FILE_OPTION = new CommandLineOption("Output File", "output", null, "The output file to write the test plan or report.", true, false);
	private final static CommandLineOption ZIP_RESULTS_OPTION = new CommandLineOption("Zip Results", "zipResults", null,
			"Zip the results files and place them in the output directory then delete the results directory.", false, true);
	private final static CommandLineOption STARTING_FOLDER_OPTION = new CommandLineOption("Starting Folder", "startFolder", null,
			"Starts at the provided folder name if it exists and skips all previous tests.", false, false);
	private final static CommandLineOption STOPPING_FOLDER_OPTION = new CommandLineOption("Stopping Folder", "stopFolder", null,
			"Stops at the provided folder name if it exists and skips all tests found after this folder.", false, false);

	private final static String FILE_LOGGER_NAME = "root";

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
			CliStatusConsole.logError("If you are unable to fix this issue and believe the application is in error please contact technical support at \"sequencing.roche.com/support\".");
			CliStatusConsole.logError("");
			CliStatusConsole.logError(underlineFrame);
			CliStatusConsole.logError("");
			System.exit(1);
		}
	}

	public static String getApplicationVersion() {
		return applicationVersionFromManifest;
	}

	private static void runCommandLineApp(String[] args) {
		String version = ManifestUtil.getManifestValue("version");
		if (version != null) {
			applicationVersionFromManifest = version;
		}

		String jarFileName = JAR_FILE_NAME;
		boolean isRunningWithinJar = AutoTestPlanCli.class.getResource("AutoTestPlanCli.class").toString().startsWith("jar");
		if (isRunningWithinJar) {
			try {
				File jarFile = new java.io.File(AutoTestPlanCli.class.getProtectionDomain().getCodeSource().getLocation().getPath());
				jarFileName = jarFile.getAbsolutePath();
			} catch (Exception e) {
				jarFileName = JAR_FILE_NAME;
			}
		}

		String commandLineSignature = CommandLineParser.getCommandLineCallSignature(jarFileName, args, true);
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

	private static CommandLineOptionsGroup getCommandLineOptionsGroupForTestPlan() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup();
		group.addOption(USAGE_OPTION);
		group.addOption(TEST_PLAN_DIRECTORY_OPTION);
		group.addOption(APPLICATION_NAME_OPTION);
		group.addOption(OUTPUT_FILE_OPTION);
		return group;
	}

	private static CommandLineOptionsGroup getCommandLineOptionsGroupForTestPlanReport() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup();
		group.addOption(USAGE_OPTION);
		group.addOption(TEST_PLAN_DIRECTORY_OPTION);
		group.addOption(TEST_PLAN_EXECUTION_RESULTS_DIRECTORY_OPTION);
		group.addOption(APPLICATION_JAR_FILE_OPTION);
		group.addOption(OUTPUT_FILE_OPTION);
		group.addOption(ZIP_RESULTS_OPTION);
		group.addOption(STARTING_FOLDER_OPTION);
		group.addOption(STOPPING_FOLDER_OPTION);
		group.addOption(JVM_BIN_PATH_OPTION);
		return group;
	}

	private static File getLogFile(File outputFile, String applicationName, String command) {
		String logFileName = applicationName + "_" + command + "_" + DateUtil.getCurrentDateINYYYY_MM_DD_HH_MM_SS() + ".log";
		logFileName = logFileName.replaceAll(" ", "_");
		logFileName = logFileName.replaceAll("/", "_");
		logFileName = logFileName.replaceAll(":", "-");
		File logFile = new File(outputFile.getParentFile(), logFileName);
		return logFile;
	}

	private static void runTestPlan(ParsedCommandLine parsedCommandLine, String commandLineSignature, String applicationName, String applicationVersion, boolean generateReport) {
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

		CliStatusConsole.logStatus(applicationName + " version:" + applicationVersion);
		CliStatusConsole.logStatus("command line signature: " + commandLineSignature);

		TestPlan testPlan = TestPlan.readFromDirectory(testPlanDirectory);
		CliStatusConsole.logStatus("Test Plan CheckSum:" + testPlan.checkSum());
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

			boolean createZip = parsedCommandLine.isOptionPresent(ZIP_RESULTS_OPTION);
			String startingFolder = null;
			if (parsedCommandLine.isOptionPresent(STARTING_FOLDER_OPTION)) {
				startingFolder = parsedCommandLine.getOptionsValue(STARTING_FOLDER_OPTION);
			}

			String stoppingFolder = null;
			if (parsedCommandLine.isOptionPresent(STOPPING_FOLDER_OPTION)) {
				stoppingFolder = parsedCommandLine.getOptionsValue(STOPPING_FOLDER_OPTION);
			}

			testPlan.createTestPlanReport(appToTest, testPlanExecutionDirectory, outputFile, jvmBinFile, createZip, startingFolder, stoppingFolder);
		} else {
			applicationName = parsedCommandLine.getOptionsValue(APPLICATION_NAME_OPTION);
			testPlan.createTestPlan(outputFile, applicationName);
		}

	}

}
