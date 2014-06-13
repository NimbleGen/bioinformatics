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
			usageBuilder.append(command.getUsage());
			usageBuilder.append(StringUtil.NEWLINE);
			usageBuilder.append(StringUtil.NEWLINE);
		}

		return usageBuilder.toString();
	}

}
