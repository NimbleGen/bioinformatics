package com.roche.bioinformatics.common.verification;

public class Reference {

	private final String documentName;
	private final String documentReference;
	private final String documentVersion;

	public Reference(String documentName, String documentReference, String documentVersion) {
		super();
		this.documentName = documentName;
		this.documentReference = documentReference;
		this.documentVersion = documentVersion;
	}

	public String getDocumentName() {
		return documentName;
	}

	public String getDocumentReference() {
		return documentReference;
	}

	public String getDocumentVersion() {
		return documentVersion;
	}

}
