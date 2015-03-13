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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * The result of using a CommandLineParser to parse the arguments passed into the application.
 * 
 */
public class ParsedCommandLine {
	private final Map<CommandLineOption, String> argumentToValueMap;
	private final CommandLineOptionsGroup group;

	private final Set<NameValuePair> unrecognizedShortFormOptions;
	private final Set<NameValuePair> unrecognizedLongFormOptions;

	private final Set<CommandLineOption> duplicateOptions;
	private final Set<CommandLineOption> foundOptionNames;

	private final List<String> nonOptionArguments;

	private final Map<CommandLineOption, String> flagOptionWithArguments;
	private final Set<CommandLineOption> nonFlagOptionWithoutArguments;

	private String unrecognizedCommand;
	private Command activeCommand;
	private Commands commands;

	ParsedCommandLine(CommandLineOptionsGroup group) {
		this.group = group;
		argumentToValueMap = new LinkedHashMap<CommandLineOption, String>();
		unrecognizedShortFormOptions = new LinkedHashSet<NameValuePair>();
		unrecognizedLongFormOptions = new LinkedHashSet<NameValuePair>();
		duplicateOptions = new LinkedHashSet<CommandLineOption>();
		foundOptionNames = new LinkedHashSet<CommandLineOption>();
		nonOptionArguments = new ArrayList<String>();
		flagOptionWithArguments = new LinkedHashMap<CommandLineOption, String>();
		nonFlagOptionWithoutArguments = new LinkedHashSet<CommandLineOption>();
		unrecognizedCommand = null;
	}

	void setUnrecognizedCommand(String unrecognizedCommand) {
		this.unrecognizedCommand = unrecognizedCommand;
	}

	void setActiveCommand(Command command) {
		this.activeCommand = command;
	}

	String getUnrecognizedCommand() {
		return unrecognizedCommand;
	}

	public Command getActiveCommand() {
		return activeCommand;
	}

	void setCommands(Commands commands) {
		this.commands = commands;
	}

	public Commands getCommands() {
		return commands;
	}

	private void markOptionAsFound(CommandLineOption option) {
		if (foundOptionNames.contains(option)) {
			duplicateOptions.add(option);
		} else {
			foundOptionNames.add(option);
		}
	}

	public String[] getNonOptionArguments() {
		return nonOptionArguments.toArray(new String[0]);
	}

	void addNonOptionArgument(String nonOptionArgument) {
		nonOptionArguments.add(nonOptionArgument);
	}

	void addFlagOptionWithArguments(CommandLineOption flagOption, String passedInArgument) {
		flagOptionWithArguments.put(flagOption, passedInArgument);
	}

	public Map<CommandLineOption, String> getFlagOptionWithArguments() {
		return new LinkedHashMap<CommandLineOption, String>(flagOptionWithArguments);
	}

	void addNonFlagOptionWithoutArguments(CommandLineOption nonFlagOption) {
		nonFlagOptionWithoutArguments.add(nonFlagOption);
	}

	public Set<CommandLineOption> getNonFlagOptionWithoutArguments() {
		return nonFlagOptionWithoutArguments;
	}

	/**
	 * @return the CommandLineOptionsGroup used to parse this command line
	 */
	public CommandLineOptionsGroup getCommandLineOptionsGroup() {
		return group;
	}

	void setArgumentValue(String option, String value) {
		if (CommandLineParser.isLongFormIdentifierArgument(option)) {
			setLongFormArgumentValue(option, value);
		} else if (CommandLineParser.isShortFormIdentifierArgument(option)) {
			setShortFormArgumentValue(option, value);
		} else {
			throw new AssertionError("Only options starting with " + CommandLineParser.LONG_OPTION_INDICATOR + " or " + CommandLineParser.SHORT_OPTION_INDICATOR + " are accepted.");
		}
	}

	private void setShortFormArgumentValue(String shortFormOption, String value) {
		char[] shortFormOptionsAsChars = parseShortFormOption(shortFormOption);

		for (char shortFormOptionAsChar : shortFormOptionsAsChars) {
			setShortFormArgumentValue(shortFormOptionAsChar, value);
		}
	}

	static char[] parseShortFormOption(String shortFormOption) {
		int optionsStartIndex = CommandLineParser.SHORT_OPTION_INDICATOR.length();
		char[] shortFormOptionsAsChars = new char[shortFormOption.length() - optionsStartIndex];

		for (int i = optionsStartIndex; i < shortFormOption.length(); i++) {
			char shortFormOptionAsChar = shortFormOption.charAt(i);

			shortFormOptionsAsChars[i - optionsStartIndex] = shortFormOptionAsChar;

		}

		return shortFormOptionsAsChars;
	}

	/**
	 * @param option
	 * @return true if the provided option was found within the arguments passed into the application
	 */
	public boolean isOptionPresent(CommandLineOption option) {
		return argumentToValueMap.containsKey(option);
	}

	/**
	 * @param option
	 * @return true if the provided options were all found within the arguments passed into the application
	 */
	public CommandLineOption[] getMissingOptions(CommandLineOption[] options) {
		List<CommandLineOption> missingOptions = new ArrayList<CommandLineOption>();
		for (CommandLineOption option : options) {
			if (!argumentToValueMap.containsKey(option)) {
				missingOptions.add(option);
			}
		}
		return missingOptions.toArray(new CommandLineOption[0]);
	}

	/**
	 * @param option
	 * @return true if any of the provided options were found within the arguments passed into the application
	 */
	public boolean isAnyOptionPresent(CommandLineOption[] options) {
		boolean isAnyOptionPresent = false;
		optionLoop: for (CommandLineOption option : options) {
			isAnyOptionPresent = argumentToValueMap.containsKey(option);
			if (isAnyOptionPresent) {
				break optionLoop;
			}
		}
		return isAnyOptionPresent;
	}

	private void setShortFormArgumentValue(char shortFormOption, String value) {
		CommandLineOption option = group.getMatchingCommandLineOptionForShortFormOption(shortFormOption);
		if (option == null) {
			unrecognizedShortFormOptions.add(new NameValuePair("" + shortFormOption, value));
		} else {
			markOptionAsFound(option);
			if (option.isFlag() && (value != null) && !value.isEmpty()) {
				throw new IllegalStateException("Value[" + value + "] was passed in for a flag option[" + option.getOptionName() + "].");
			}

			argumentToValueMap.put(option, value);
		}
	}

	private void setLongFormArgumentValue(String longFormOption, String value) {
		longFormOption = longFormOption.replaceFirst(CommandLineParser.LONG_OPTION_INDICATOR, "");

		CommandLineOption option = group.getMatchingCommandLineOptionForLongFormOption(longFormOption);

		if (option == null) {
			unrecognizedLongFormOptions.add(new NameValuePair(longFormOption, value));
		} else {
			markOptionAsFound(option);
			if (option.isFlag() && (value != null) && !value.isEmpty()) {
				throw new IllegalStateException("Value[" + value + "] was passed in for a flag option[" + option.getOptionName() + "].");
			}

			argumentToValueMap.put(option, value);
		}
	}

	void setArgumentValue(CommandLineOption option, String value) {
		argumentToValueMap.put(option, value);
	}

	/**
	 * @param option
	 * @return the value associated with the given option
	 */
	public String getOptionsValue(CommandLineOption option) {
		return argumentToValueMap.get(option);
	}

	/**
	 * @return missing required options
	 */
	public CommandLineOption[] getMissingRequiredOptions() {
		Set<CommandLineOption> missingRequiredOptions = new LinkedHashSet<CommandLineOption>();
		if (group != null) {
			for (CommandLineOption argument : group) {
				if (argument.isRequired() && !(argumentToValueMap.containsKey(argument) || nonFlagOptionWithoutArguments.contains(argument))) {
					missingRequiredOptions.add(argument);
				}
			}
		}

		return missingRequiredOptions.toArray(new CommandLineOption[0]);
	}

	void addUnrecognizedShortFormOptions(String unrecognizedOption) {
		unrecognizedShortFormOptions.add(new NameValuePair(unrecognizedOption, ""));
	}

	void addUnrecognizedLongFormOptions(String unrecognizedOption) {
		unrecognizedLongFormOptions.add(new NameValuePair(unrecognizedOption, ""));
	}

	/**
	 * @return unrecognized short form options
	 */
	public Set<NameValuePair> getUnrecognizedShortFormOptions() {
		return unrecognizedShortFormOptions;
	}

	/**
	 * @return unrecognized long form options
	 */
	public Set<NameValuePair> getUnrecognizedLongFormOption() {
		return unrecognizedLongFormOptions;
	}

	/**
	 * @return duplicate arguments
	 */
	public Set<CommandLineOption> getDuplicateOptions() {
		return duplicateOptions;
	}

	@Override
	public String toString() {
		return "ParsedCommandLine [argumentToValueMap=" + argumentToValueMap + ", group=" + group + ", unrecognizedShortFormOptions=" + unrecognizedShortFormOptions + ", unrecognizedLongFormOption="
				+ unrecognizedLongFormOptions + ", nonOptionArguments=" + nonOptionArguments + " Missing Required Options=" + Arrays.toString(getMissingRequiredOptions()) + "]";
	}

	/**
	 * 
	 * Class used internally to store name value pairs
	 * 
	 */
	static class NameValuePair {
		private final String name;
		private final String value;

		private NameValuePair(String name, String value) {
			super();
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;

			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj == null) {
				return false;
			}

			if (getClass() != obj.getClass()) {
				return false;
			}

			NameValuePair other = (NameValuePair) obj;

			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}

			if (value == null) {
				if (other.value != null) {
					return false;
				}
			} else if (!value.equals(other.value)) {
				return false;
			}

			return true;
		}

	}

}
