package com.roche.bioinformatics.common.verification;

public class Reviewer {

	private final Person person;
	private final String role;

	public Reviewer(Person person, String role) {
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
