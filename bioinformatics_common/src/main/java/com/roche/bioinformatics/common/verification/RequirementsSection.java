package com.roche.bioinformatics.common.verification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequirementsSection {

	private final List<Requirement> requirements;

	public RequirementsSection() {
		this.requirements = new ArrayList<Requirement>();
	}

	public void addRequirement(Requirement requirement) {
		this.requirements.add(requirement);
	}

	public List<Requirement> getRequirements() {
		return Collections.unmodifiableList(requirements);
	}
}
