package com.roche.bioinformatics.common.verification.testplan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReferencesSection {

	private final List<Reference> references;

	public ReferencesSection() {
		this.references = new ArrayList<Reference>();
	}

	public void addReference(Reference reference) {
		this.references.add(reference);
	}

	public List<Reference> getReferences() {
		return Collections.unmodifiableList(references);
	}

}
