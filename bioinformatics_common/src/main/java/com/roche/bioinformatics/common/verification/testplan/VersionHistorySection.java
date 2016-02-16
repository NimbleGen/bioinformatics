package com.roche.bioinformatics.common.verification.testplan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VersionHistorySection {

	private final List<Version> versions;

	public VersionHistorySection() {
		versions = new ArrayList<Version>();
	}

	public void addVersion(Version version) {
		this.versions.add(version);
	}

	public List<Version> getVersions() {
		return Collections.unmodifiableList(versions);
	}
}
