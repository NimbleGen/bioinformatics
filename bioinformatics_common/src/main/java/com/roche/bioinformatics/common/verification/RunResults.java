package com.roche.bioinformatics.common.verification;

import java.io.File;

public class RunResults {
	private final String consoleOutput;
	private final String consoleErrors;
	private final File outputDirectory;

	public RunResults(String consoleOutput, String consoleErrors, File outputDirectory) {
		super();
		this.consoleOutput = consoleOutput;
		this.consoleErrors = consoleErrors;
		this.outputDirectory = outputDirectory;
	}

	public String getConsoleOutput() {
		return consoleOutput;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public String getConsoleErrors() {
		return consoleErrors;
	}
}
