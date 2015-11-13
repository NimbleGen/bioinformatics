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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class Commands implements Iterable<Command> {

	private final String usageBanner;
	private final List<Command> commands;

	public Commands(String usageBanner) {
		super();
		this.commands = new LinkedList<Command>();
		this.usageBanner = usageBanner;
	}

	public Commands() {
		this(null);
	}

	public void addCommand(Command command) {
		commands.add(command);
	}

	@Override
	public Iterator<Command> iterator() {
		return commands.iterator();
	}

	public String getUsage() {
		StringBuilder usageBuilder = new StringBuilder();

		if (usageBanner != null) {
			usageBuilder.append(usageBanner + StringUtil.NEWLINE);
		}

		for (Command command : commands) {
			usageBuilder.append(command.getCommandName() + StringUtil.TAB + StringUtil.TAB + command.getCommandDescription());
			usageBuilder.append(StringUtil.NEWLINE);
		}

		return usageBuilder.toString();
	}

}
