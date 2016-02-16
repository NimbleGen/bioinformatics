package com.roche.bioinformatics.common.verification.testplan;

public class Reviewer {

	private final String name;
	private final String role;

	public Reviewer(String name, String role) {
		super();
		this.name = name;
		this.role = role;
	}

	public String getName() {
		return name;
	}

	public String getRole() {
		return role;
	}

}
