package com.roche.bioinformatics.common.verification.testplan;

public class Definition {

	private final String term;
	private final String definition;

	public Definition(String term, String definition) {
		super();
		this.term = term;
		this.definition = definition;
	}

	public String getTerm() {
		return term;
	}

	public String getDefinition() {
		return definition;
	}

}
