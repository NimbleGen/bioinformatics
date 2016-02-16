package com.roche.bioinformatics.common.verification.testplan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefinitionsSection {

	private final List<Definition> definitions;

	public DefinitionsSection() {
		this.definitions = new ArrayList<Definition>();
	}

	public void addDefinition(Definition definition) {
		this.definitions.add(definition);
	}

	public List<Definition> getDefinitions() {
		return Collections.unmodifiableList(definitions);
	}

}
