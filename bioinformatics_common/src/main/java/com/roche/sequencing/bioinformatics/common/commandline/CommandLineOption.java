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

import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * 
 * A definition of an option used to extract one value from application arguments (arguments passed into the main method)
 * 
 */
public class CommandLineOption {
	private final String optionName;
	private final String longFormOption;
	private final Character shortFormOption;
	private final String description;
	private final boolean isRequired;
	private final boolean isFlag;

	public CommandLineOption(String optionName, String longFormOption, Character shortFormOption, String description, boolean isRequired, boolean isFlag) {
		super();
		this.optionName = optionName;
		this.longFormOption = longFormOption;
		this.shortFormOption = shortFormOption;
		this.description = description;
		this.isRequired = isRequired;
		this.isFlag = isFlag;
	}

	/**
	 * @return the option name
	 */
	public String getOptionName() {
		return optionName;
	}

	public String getLongFormOption() {
		return longFormOption;
	}

	public Character getShortFormOption() {
		return shortFormOption;
	}

	public String getDescription() {
		return description;
	}

	public boolean isRequired() {
		return isRequired;
	}

	public boolean isFlag() {
		return isFlag;
	}

	public String getUsage() {
		StringBuilder usageBuilder = new StringBuilder();

		usageBuilder.append(StringUtil.TAB + getOptionName() + StringUtil.TAB);

		if (getShortFormOption() != null) {
			usageBuilder.append(CommandLineParser.SHORT_OPTION_INDICATOR + getShortFormOption());

			if (getLongFormOption() != null) {
				usageBuilder.append(", ");
			}
		}

		if (getLongFormOption() != null) {
			usageBuilder.append(CommandLineParser.LONG_OPTION_INDICATOR + getLongFormOption());
		}

		usageBuilder.append(StringUtil.TAB);
		usageBuilder.append(getDescription());

		if (isRequired()) {
			usageBuilder.append("(required)");
		} else {
			usageBuilder.append("(optional)");
		}

		if (isFlag()) {
			usageBuilder.append("(flag)");
		}

		usageBuilder.append(StringUtil.NEWLINE);
		return usageBuilder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;

		result = prime * result + ((optionName == null) ? 0 : optionName.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + (isFlag ? 1231 : 1237);
		result = prime * result + (isRequired ? 1231 : 1237);
		result = prime * result + ((longFormOption == null) ? 0 : longFormOption.hashCode());
		result = prime * result + ((shortFormOption == null) ? 0 : shortFormOption.hashCode());
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

		CommandLineOption other = (CommandLineOption) obj;

		if (optionName == null) {
			if (other.optionName != null) {
				return false;
			}
		} else if (!optionName.equals(other.optionName)) {
			return false;
		}

		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
			return false;
		}

		if (isFlag != other.isFlag) {
			return false;
		}

		if (isRequired != other.isRequired) {
			return false;
		}

		if (longFormOption == null) {
			if (other.longFormOption != null) {
				return false;
			}
		} else if (!longFormOption.equals(other.longFormOption)) {
			return false;
		}

		if (shortFormOption == null) {
			if (other.shortFormOption != null) {
				return false;
			}
		} else if (!shortFormOption.equals(other.shortFormOption)) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return "CommandLineOption [optionName=" + optionName + ", longFormOption=" + longFormOption + ", shortFormOption=" + shortFormOption + ", description=" + description + ", isRequired="
				+ isRequired + ", isFlag=" + isFlag + "]";
	}

}
