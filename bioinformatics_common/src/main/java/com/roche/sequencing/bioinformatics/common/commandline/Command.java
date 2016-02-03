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
package com.roche.sequencing.bioinformatics.common.commandline;

import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class Command {

	private final String commandName;
	private final String commandDescription;
	private final CommandLineOptionsGroup commandOptions;

	public Command(String commandName, String commandDescription, CommandLineOptionsGroup commandOptions) {
		super();
		this.commandName = commandName;
		this.commandDescription = commandDescription;
		this.commandOptions = commandOptions;
	}

	public String getCommandName() {
		return commandName;
	}

	public String getCommandDescription() {
		return commandDescription;
	}

	public CommandLineOptionsGroup getCommandOptions() {
		return commandOptions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((commandDescription == null) ? 0 : commandDescription.hashCode());
		result = prime * result + ((commandName == null) ? 0 : commandName.hashCode());
		result = prime * result + ((commandOptions == null) ? 0 : commandOptions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Command other = (Command) obj;
		if (commandDescription == null) {
			if (other.commandDescription != null)
				return false;
		} else if (!commandDescription.equals(other.commandDescription))
			return false;
		if (commandName == null) {
			if (other.commandName != null)
				return false;
		} else if (!commandName.equals(other.commandName))
			return false;
		if (commandOptions == null) {
			if (other.commandOptions != null)
				return false;
		} else if (!commandOptions.equals(other.commandOptions))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Command [commandName=" + commandName + ", commandDescription=" + commandDescription + ", commandOptions=" + commandOptions + "]";
	}

	public String getUsage() {
		StringBuilder usageBuilder = new StringBuilder();
		usageBuilder.append(getCommandName() + StringUtil.NEWLINE);
		usageBuilder.append(StringUtil.TAB + getCommandDescription() + StringUtil.NEWLINE);
		usageBuilder.append(StringUtil.NEWLINE);
		usageBuilder.append(StringUtil.TAB + "OPTIONS:" + StringUtil.NEWLINE);
		usageBuilder.append(getCommandOptions().getUsage());
		return usageBuilder.toString();
	}

}
