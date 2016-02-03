package com.roche.bioinformatics.common.verification;

public class Approver {

	private final Person person;
	private final String role;

	public Approver(Person person, String role) {
		super();
		this.person = person;
		this.role = role;
	}

	public Person getPerson() {
		return person;
	}

	public String getRole() {
		return role;
	}

}
