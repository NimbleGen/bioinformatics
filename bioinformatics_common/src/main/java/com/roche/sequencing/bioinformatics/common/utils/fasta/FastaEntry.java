package com.roche.sequencing.bioinformatics.common.utils.fasta;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;

public class FastaEntry {
	private final String description;
	private final ISequence sequence;

	public FastaEntry(String description, ISequence sequence) {
		super();
		this.description = description;
		this.sequence = sequence;
	}

	public String getDescription() {
		return description;
	}

	public ISequence getSequence() {
		return sequence;
	}

}
