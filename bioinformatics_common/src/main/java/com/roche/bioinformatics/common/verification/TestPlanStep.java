package com.roche.bioinformatics.common.verification;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestPlanStep {

	private final String description;
	private final String[] args;
	private final List<TestPlanStepCheck> stepChecks;
	private final File resultsDirectory;

	public TestPlanStep(String description, String[] args, File resultsDirectory) {
		this.description = description;
		this.args = args;
		this.stepChecks = new ArrayList<TestPlanStepCheck>();
		this.resultsDirectory = resultsDirectory;
	}

	public void addVerification(TestPlanStepCheck stepCheck) {
		this.stepChecks.add(stepCheck);
	}

	public String[] getArgs() {
		return Arrays.copyOf(args, args.length);
	}

	public List<TestPlanStepCheck> getStepChecks() {
		return Collections.unmodifiableList(stepChecks);
	}

	public File getResultsDirectory() {
		return resultsDirectory;
	}

	public String getDescription() {
		return description;
	}

}
