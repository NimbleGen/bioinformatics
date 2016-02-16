package com.roche.bioinformatics.common.verification.testplan;

public class Requirement {
	private final String requirementType;
	private final String requirementNumber;
	private final String requirementText;

	public Requirement(String requirementType, String requirementNumber, String requirementText) {
		super();
		this.requirementType = requirementType;
		this.requirementNumber = requirementNumber;
		this.requirementText = requirementText;
	}

	public String getRequirementType() {
		return requirementType;
	}

	public String getRequirementNumber() {
		return requirementNumber;
	}

	public String getRequirementText() {
		return requirementText;
	}

}
