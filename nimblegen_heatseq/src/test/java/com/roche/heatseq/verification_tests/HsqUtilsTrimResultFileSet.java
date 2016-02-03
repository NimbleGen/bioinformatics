package com.roche.heatseq.verification_tests;

import java.io.File;

public class HsqUtilsTrimResultFileSet {

	private final File logFile;
	private final File trimmedR1;
	private final File trimmedR2;

	public HsqUtilsTrimResultFileSet(File logFile, File trimmedR1, File trimmedR2) {
		super();
		this.logFile = logFile;
		this.trimmedR1 = trimmedR1;
		this.trimmedR2 = trimmedR2;
	}

	public File getLogFile() {
		return logFile;
	}

	public File getTrimmedR1() {
		return trimmedR1;
	}

	public File getTrimmedR2() {
		return trimmedR2;
	}

}
