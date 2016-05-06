package com.roche.bioinformatics.common.verification.runs;

import java.io.File;

class RunResults {
	private final String consoleOutput;
	private final String consoleErrors;
	private final File outputDirectory;
	private final File testDirectory;

	public RunResults(String consoleOutput, String consoleErrors, File outputDirectory, File testDirectory) {
		super();
		this.consoleOutput = consoleOutput;
		this.consoleErrors = consoleErrors;
		this.outputDirectory = outputDirectory;
		this.testDirectory = testDirectory;
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

	public File getTestDirectory() {
		return testDirectory;
	}
}
