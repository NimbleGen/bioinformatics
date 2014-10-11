package com.roche.heatseq.objects;

import java.util.List;

public class ExtendReadResults {

	private final List<IReadPair> extendedReads;
	private final List<IReadPair> unableToExtendReads;

	public ExtendReadResults(List<IReadPair> extendedReads, List<IReadPair> unableToExtendReads) {
		super();
		this.extendedReads = extendedReads;
		this.unableToExtendReads = unableToExtendReads;
	}

	public List<IReadPair> getExtendedReads() {
		return extendedReads;
	}

	public List<IReadPair> getUnableToExtendReads() {
		return unableToExtendReads;
	}

}
