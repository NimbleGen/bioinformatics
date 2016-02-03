package com.roche.bioinformatics.common.verification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestPlanStepsSection {

	private final List<TestPlanStep> steps;

	public TestPlanStepsSection() {
		this.steps = new ArrayList<TestPlanStep>();
	}

	public void addStep(TestPlanStep step) {
		this.steps.add(step);
	}

	public List<TestPlanStep> getSteps() {
		return Collections.unmodifiableList(steps);
	}

}
