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

package com.roche.sequencing.bioinformatics.common.commandline;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine.NameValuePair;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * 
 * Class to help parse command line arguments
 * 
 */
public class CommandLineParser {
	final static String LONG_OPTION_INDICATOR = "--";
	final static String SHORT_OPTION_INDICATOR = "-";

	private CommandLineParser() {
		throw new AssertionError();
	}

	public static ParsedCommandLine parseCommandLine(String[] arguments, Commands commands) {
		ParsedCommandLine parsedCommandLine = null;
		Command command = findMatchingCommand(arguments, commands);
		if (command != null) {
			parsedCommandLine = parseCommandLine(arguments, command.getCommandOptions());
			parsedCommandLine.setActiveCommand(command);
		} else {
			parsedCommandLine = new ParsedCommandLine(null);
			parsedCommandLine.setUnrecognizedCommand(arguments[0]);
		}
		parsedCommandLine.setCommands(commands);
		return parsedCommandLine;
	}

	static Command findMatchingCommand(String[] arguments, Commands commands) {
		Command matchingCommand = null;
		commandLoop: for (Command command : commands) {
			if (command.getCommandName().toLowerCase().equals(arguments[0].toLowerCase())) {
				matchingCommand = command;
				break commandLoop;
			}
		}
		return matchingCommand;
	}

	/**
	 * Parse the provided arguments given the commanLineOptionGroup(which defines what arguments are expected)
	 * 
	 * @param arguments
	 * @param commandLineOptionGroup
	 * @return ParsedCommandLine
	 */
	public static ParsedCommandLine parseCommandLine(String[] arguments, CommandLineOptionsGroup commandLineOptionGroup) {
		ParsedCommandLine parsedCommandLine = new ParsedCommandLine(commandLineOptionGroup);

		int i = 0;

		while (i < arguments.length) {
			String argument = arguments[i];
			boolean isOption = isOptionIdentifierArgument(argument);
			int nextArgumentIndex = i + 1;

			if (isOption) {
				OptionMatchingResults optionMatchingResults = commandLineOptionGroup.getMatchingCommandLineOptions(argument);

				for (CommandLineOption option : optionMatchingResults.getMatchingOptions()) {
					if ((option != null) && option.isFlag()) {
						if ((nextArgumentIndex < arguments.length) && !isOptionIdentifierArgument(arguments[nextArgumentIndex])) {
							String nextNonOptionArgument = arguments[nextArgumentIndex];
							nextArgumentIndex++;
							parsedCommandLine.addFlagOptionWithArguments(option, nextNonOptionArgument);
						} else {
							parsedCommandLine.setArgumentValue(option, "");
						}
					} else {
						// it is a non-flagged option argument
						String nextArgument = "";

						if ((nextArgumentIndex < arguments.length)) {
							nextArgument = arguments[nextArgumentIndex];
							parsedCommandLine.setArgumentValue(argument, nextArgument);
							nextArgumentIndex++;
						} else {
							parsedCommandLine.addNonFlagOptionWithoutArguments(option);
						}
					}
				}

				for (String unrecognizedOption : optionMatchingResults.getUnrecognizedShortFormOptions()) {
					parsedCommandLine.addUnrecognizedShortFormOptions(unrecognizedOption);
				}

				for (String unrecognizedOption : optionMatchingResults.getUnrecognizedLongFormOptions()) {
					parsedCommandLine.addUnrecognizedLongFormOptions(unrecognizedOption);
				}

			} else {

				parsedCommandLine.addNonOptionArgument(argument);
			}

			i = nextArgumentIndex;
		}

		return parsedCommandLine;
	}

	/**
	 * Parse the provided arguments given the commanLineOptionGroup(which defines what arguments are expected) and throw an exception if any errors occur
	 * 
	 * @param arguments
	 * @param commandLineOptionGroup
	 * @return ParsedCommandLine
	 */
	public static ParsedCommandLine parseCommandLineWithExceptions(String[] arguments, CommandLineOptionsGroup commandLineOptionGroup) {
		ParsedCommandLine parsedCommandLine = parseCommandLine(arguments, commandLineOptionGroup);

		throwCommandLineParsingExceptions(parsedCommandLine);
		return parsedCommandLine;
	}

	/**
	 * Parse the provided arguments given the commans(which defines what arguments are expected) and throw an exception if any errors occur
	 * 
	 * @param arguments
	 * @param Commands
	 * @return ParsedCommandLine
	 */
	public static ParsedCommandLine parseCommandLineWithExceptions(String[] arguments, Commands commands) {
		ParsedCommandLine parsedCommandLine = parseCommandLine(arguments, commands);

		throwCommandLineParsingExceptions(parsedCommandLine);
		return parsedCommandLine;
	}

	/**
	 * Take a ParsedCommandLine and throw any exceptions if the ParsedCommandLine did not parse without errors.
	 * 
	 * @param parsedCommandLine
	 */
	public static void throwCommandLineParsingExceptions(ParsedCommandLine parsedCommandLine) {
		CommandLineOption[] missingOptions = parsedCommandLine.getMissingRequiredOptions();

		String unrecognizedCommandError = "";
		if (parsedCommandLine.getUnrecognizedCommand() != null) {
			StringBuilder validCommandsBuilder = new StringBuilder();
			for (Command command : parsedCommandLine.getCommands()) {
				validCommandsBuilder.append(StringUtil.TAB + command.getCommandName() + StringUtil.NEWLINE);
			}
			unrecognizedCommandError = "The provided command[" + parsedCommandLine.getUnrecognizedCommand() + "] is not recognized.  Valid commands are the following:" + StringUtil.NEWLINE
					+ validCommandsBuilder.toString();
		}

		String missingOptionsError = "";

		if (missingOptions.length > 0) {
			StringBuilder missingOptionsBuilder = new StringBuilder();

			missingOptionsBuilder.append("Missing the following required option(s): " + StringUtil.NEWLINE);

			for (CommandLineOption option : missingOptions) {
				missingOptionsBuilder.append(StringUtil.TAB + option.getUsage());
			}

			missingOptionsError = missingOptionsBuilder.toString();
		}

		String duplicateArgumentsError = "";
		Set<String> duplicateArguments = parsedCommandLine.getDuplicateArguments();

		if (duplicateArguments.size() > 0) {
			StringBuilder duplicateArgumentsBuilder = new StringBuilder();

			duplicateArgumentsBuilder.append("The following argument(s) were found multiple times:" + StringUtil.NEWLINE);

			for (String argument : duplicateArguments) {
				duplicateArgumentsBuilder.append(StringUtil.TAB + argument + StringUtil.NEWLINE);
			}

			duplicateArgumentsError = duplicateArgumentsBuilder.toString();
		}

		String unrecognizedLongOptionsError = "";
		Set<NameValuePair> unrecognizedLongFormOptions = parsedCommandLine.getUnrecognizedLongFormOption();

		if (unrecognizedLongFormOptions.size() > 0) {
			StringBuilder unrecognizedLongOptionsBuilder = new StringBuilder();

			unrecognizedLongOptionsBuilder.append("The following long form option(s) were not recognized:" + StringUtil.NEWLINE);

			for (NameValuePair unrecognizedLongOption : unrecognizedLongFormOptions) {
				unrecognizedLongOptionsBuilder.append(StringUtil.TAB + unrecognizedLongOption.getName() + " " + unrecognizedLongOption.getValue() + StringUtil.NEWLINE);
			}

			unrecognizedLongOptionsError = unrecognizedLongOptionsBuilder.toString();
		}

		String unrecognizedShortOptionsError = "";
		Set<NameValuePair> unrecognizedShortFormOptions = parsedCommandLine.getUnrecognizedShortFormOptions();

		if (unrecognizedShortFormOptions.size() > 0) {
			StringBuilder unrecognizedShortOptionsBuilder = new StringBuilder();

			unrecognizedShortOptionsBuilder.append("The following short form option(s) were not recognized:" + StringUtil.NEWLINE);

			for (NameValuePair unrecognizedShortOption : unrecognizedShortFormOptions) {
				unrecognizedShortOptionsBuilder.append(StringUtil.TAB + CommandLineParser.SHORT_OPTION_INDICATOR + unrecognizedShortOption.getName() + " " + unrecognizedShortOption.getValue()
						+ StringUtil.NEWLINE);
			}

			unrecognizedShortOptionsError = unrecognizedShortOptionsBuilder.toString();
		}

		String flagOptionWithArgumentsError = "";
		Map<CommandLineOption, String> flagOptionsWithArguments = parsedCommandLine.getFlagOptionWithArguments();

		if (flagOptionsWithArguments.size() > 0) {
			StringBuilder flagOptionWithArgumentsErrorBuilder = new StringBuilder();

			flagOptionWithArgumentsErrorBuilder.append("The following flag option(s) were given arguments when none were expected:" + StringUtil.NEWLINE);

			for (Entry<CommandLineOption, String> entry : flagOptionsWithArguments.entrySet()) {
				flagOptionWithArgumentsErrorBuilder.append(StringUtil.TAB + entry.getKey().getUsage() + " " + entry.getValue() + StringUtil.NEWLINE);
			}

			flagOptionWithArgumentsError = flagOptionWithArgumentsErrorBuilder.toString();
		}

		String nonFlagOptionWithoutArgumentsError = "";
		Set<CommandLineOption> nonFlagOptionsWithoutArguments = parsedCommandLine.getNonFlagOptionWithoutArguments();

		if (nonFlagOptionsWithoutArguments.size() > 0) {
			StringBuilder nonFlagOptionWithoutArgumentsErrorBuilder = new StringBuilder();

			nonFlagOptionWithoutArgumentsErrorBuilder.append("The following option(s) require arguments that were not provided:" + StringUtil.NEWLINE);

			for (CommandLineOption option : nonFlagOptionsWithoutArguments) {
				nonFlagOptionWithoutArgumentsErrorBuilder.append(StringUtil.TAB + option.getUsage() + StringUtil.NEWLINE);
			}

			nonFlagOptionWithoutArgumentsError = nonFlagOptionWithoutArgumentsErrorBuilder.toString();
		}

		if (!unrecognizedCommandError.isEmpty() || !missingOptionsError.isEmpty() || !unrecognizedLongOptionsError.isEmpty() || !unrecognizedShortOptionsError.isEmpty()
				|| !duplicateArguments.isEmpty() || !flagOptionWithArgumentsError.isEmpty() || !nonFlagOptionWithoutArgumentsError.isEmpty()) {
			throw new IllegalStateException(missingOptionsError + unrecognizedLongOptionsError + unrecognizedShortOptionsError + duplicateArgumentsError + flagOptionWithArgumentsError
					+ nonFlagOptionWithoutArgumentsError);
		}
	}

	private static boolean isOptionIdentifierArgument(String argument) {
		return isShortFormIdentifierArgument(argument) || isLongFormIdentifierArgument(argument);
	}

	static boolean isShortFormIdentifierArgument(String argument) {
		return argument.startsWith(SHORT_OPTION_INDICATOR);
	}

	static boolean isLongFormIdentifierArgument(String argument) {
		return argument.startsWith(LONG_OPTION_INDICATOR);
	}

	/**
	 * @param args
	 * @param includeJvmParams
	 * @return the signature used to call this application
	 */
	public static String getCommandLineCallSignature(String applicationName, String[] args, boolean includeJvmParams) {
		StringBuilder signature = new StringBuilder();

		signature.append(applicationName);

		for (String commandLineArgument : args) {
			signature.append(" " + commandLineArgument);
		}

		if (includeJvmParams) {
			RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
			List<String> jvmArguments = RuntimemxBean.getInputArguments();
			for (String jvmArgument : jvmArguments) {
				signature.append(" " + jvmArgument);
			}
		}
		return signature.toString();
	}

}
