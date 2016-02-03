package com.roche.bioinformatics.common.verification;

public class TestPlanStepCheck {

	private final String description;
	private final String expectedResultDescription;
	private final IStepChecker stepChecker;

	public TestPlanStepCheck(String description, String expectedResultDescription, IStepChecker stepChecker) {
		super();
		this.description = description;
		this.expectedResultDescription = expectedResultDescription;
		this.stepChecker = stepChecker;
	}

	public String getDescription() {
		return description;
	}

	public String getExpectedResultDescription() {
		return expectedResultDescription;
	}

	public IStepChecker getStepChecker() {
		return stepChecker;
	}

}
