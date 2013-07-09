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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * 
 * A group of CommandLineOptions used to parse application arguments
 * 
 */
public class CommandLineOptionsGroup implements Iterable<CommandLineOption> {
	private final String usageBanner;
	private final Set<CommandLineOption> options;

	/**
	 * Default Constructor
	 * 
	 * @param usageBanner
	 *            a string that will be displayed along with usage
	 */
	public CommandLineOptionsGroup(String usageBanner) {
		this.usageBanner = usageBanner;
		options = new LinkedHashSet<CommandLineOption>();
	}

	/**
	 * @return the usage associated with this CommandLineOptionsGroup
	 */
	public String getUsage() {
		StringBuilder usageBuilder = new StringBuilder();

		if (usageBanner != null) {
			usageBuilder.append(usageBanner + StringUtil.NEWLINE);
		}

		for (CommandLineOption option : options) {
			usageBuilder.append(option.getUsage());
		}

		return usageBuilder.toString();
	}

	/**
	 * Add a CommanLineOption to this group
	 * 
	 * @param option
	 */
	public void addOption(CommandLineOption option) {
		if (option.getShortFormOption() != null) {
			CommandLineOption matchingArgument = getMatchingCommandLineOptionForShortFormOption(option.getShortFormOption());

			if (matchingArgument != null) {
				throw new IllegalStateException("Short form option[" + option.getShortFormOption() + "] is already being used by the existing member of CommandLineOptionGroup[" + matchingArgument
						+ "].");
			}
		}

		if (option.getLongFormOption() != null) {
			CommandLineOption matchingArgument = getMatchingCommandLineOptionForLongFormOption(option.getLongFormOption());

			if (matchingArgument != null) {
				throw new IllegalStateException("Long form option[" + option.getLongFormOption() + "] is already being used by the existing member of CommandLineOptionGroup[" + matchingArgument
						+ "].");
			}
		}

		options.add(option);
	}

	@Override
	public Iterator<CommandLineOption> iterator() {
		return options.iterator();
	}

	/**
	 * @param option
	 * @return the options that this matches with (note there would only be multiple options in the case of a short option indicator with multiple characters (for example -ac would be equivalent to
	 *         the two options -a and -c).
	 */
	OptionMatchingResults getMatchingCommandLineOptions(String option) {
		OptionMatchingResults optionMatchingResults = new OptionMatchingResults();

		if (option.startsWith(CommandLineParser.LONG_OPTION_INDICATOR)) {
			CommandLineOption matchingOption = getMatchingCommandLineOptionForLongFormOption(option);

			if (matchingOption != null) {
				optionMatchingResults.addMatchingOption(matchingOption);
			} else {
				optionMatchingResults.addUnrecognizedLongFormOption(option);
			}

		} else if (option.startsWith(CommandLineParser.SHORT_OPTION_INDICATOR)) {
			// this could actually more than one option combined
			char[] shortFormOptionsAsChars = ParsedCommandLine.parseShortFormOption(option);

			for (char shortFormOptionAsChar : shortFormOptionsAsChars) {
				CommandLineOption matchingOption = getMatchingCommandLineOptionForShortFormOption(shortFormOptionAsChar);

				if (matchingOption != null) {
					optionMatchingResults.addMatchingOption(matchingOption);
				} else {
					optionMatchingResults.addUnrecognizedShortFormOption("" + shortFormOptionAsChar);
				}
			}

		}

		return optionMatchingResults;

	}

	CommandLineOption getMatchingCommandLineOptionForShortFormOption(char shortFormOption) {
		CommandLineOption matchingArgument = null;

		argumentLoop: for (CommandLineOption argument : options) {
			Character argumentShortFormOption = argument.getShortFormOption();

			if ((argumentShortFormOption != null) && argumentShortFormOption.equals(shortFormOption)) {
				matchingArgument = argument;

				break argumentLoop;
			}
		}

		return matchingArgument;
	}

	CommandLineOption getMatchingCommandLineOptionForLongFormOption(String longFormOption) {
		CommandLineOption matchingArgument = null;

		longFormOption = longFormOption.replaceFirst(CommandLineParser.LONG_OPTION_INDICATOR, "");
		optiontLoop: for (CommandLineOption argument : options) {
			boolean caseInsensitiveArgumentMatchFound = argument.getLongFormOption().toLowerCase().equals(longFormOption.toLowerCase());

			if (caseInsensitiveArgumentMatchFound) {
				matchingArgument = argument;

				break optiontLoop;
			}
		}

		return matchingArgument;
	}

}
