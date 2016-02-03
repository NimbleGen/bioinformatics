package com.roche.heatseq.verification_tests;

import java.io.File;

public class HsqUtilsFileSet {

	private final File r1;
	private final File r2;
	private final File probe;
	private final File bam;

	public HsqUtilsFileSet(File r1, File r2, File probe, File bam) {
		super();
		this.r1 = r1;
		this.r2 = r2;
		this.probe = probe;
		this.bam = bam;
	}

	public File getR1() {
		return r1;
	}

	public File getR2() {
		return r2;
	}

	public File getProbe() {
		return probe;
	}

	public File getBam() {
		return bam;
	}
}
