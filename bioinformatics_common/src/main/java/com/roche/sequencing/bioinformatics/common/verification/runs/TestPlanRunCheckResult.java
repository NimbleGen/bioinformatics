package com.roche.sequencing.bioinformatics.common.verification.runs;

public class TestPlanRunCheckResult {

	private final boolean isPassed;
	private final String resultDescription;

	public TestPlanRunCheckResult(boolean isPassed, String resultDescription) {
		super();
		this.isPassed = isPassed;
		this.resultDescription = resultDescription;
	}

	public boolean isPassed() {
		return isPassed;
	}

	public String getResultDescription() {
		return resultDescription;
	}

}
