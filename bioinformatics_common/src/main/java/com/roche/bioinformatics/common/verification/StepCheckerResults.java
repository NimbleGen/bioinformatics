package com.roche.bioinformatics.common.verification;

public class StepCheckerResults {

	private final String resultsDescription;
	private final boolean isSuccess;

	public StepCheckerResults(String resultsDescription, boolean wasSuccess) {
		super();
		this.resultsDescription = resultsDescription;
		this.isSuccess = wasSuccess;
	}

	public String getResultsDescription() {
		return resultsDescription;
	}

	public boolean isSuccess() {
		return isSuccess;
	}

}
