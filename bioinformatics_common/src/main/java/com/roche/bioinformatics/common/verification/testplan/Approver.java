package com.roche.bioinformatics.common.verification.testplan;

public class Approver {

	private final String name;
	private final String role;

	public Approver(String name, String role) {
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
