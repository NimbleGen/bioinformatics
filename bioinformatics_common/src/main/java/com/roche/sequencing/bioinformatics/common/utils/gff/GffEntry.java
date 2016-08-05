package com.roche.sequencing.bioinformatics.common.utils.gff;

import com.roche.sequencing.bioinformatics.common.sequence.Strand;

public class GffEntry {

	private final ParsedGffFile parsedGffFile;

	private final int indexOfSource;
	private final int indexOfType;
	private final long start;
	private final long stop;
	private final double score;
	private final Strand strand;
	private final String attributes;

	public GffEntry(ParsedGffFile parsedGffFile, int indexOfSource, int indexOfType, long start, long stop, double score, Strand strand, String attributes) {
		super();
		this.parsedGffFile = parsedGffFile;
		this.indexOfSource = indexOfSource;
		this.indexOfType = indexOfType;
		this.start = start;
		this.stop = stop;
		this.score = score;
		this.strand = strand;
		this.attributes = attributes;
	}

	public String getSource() {
		return parsedGffFile.getSource(indexOfSource);
	}

	public String getType() {
		return parsedGffFile.getType(indexOfType);
	}

	public long getStart() {
		return start;
	}

	public long getStop() {
		return stop;
	}

	public double getScore() {
		return score;
	}

	public Strand getStrand() {
		return strand;
	}

	public String getAttributes() {
		return attributes;
	}

}
