package com.roche.sequencing.bioinformatics.common.genome;

import com.roche.sequencing.bioinformatics.common.sequence.Strand;

public class StrandedGenomicCoordinate extends GenomicCoordinate {

	private final Strand strand;

	public StrandedGenomicCoordinate(String container, Strand strand, long location) {
		super(container, location);
		this.strand = strand;
	}

	public Strand getStrand() {
		return strand;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((strand == null) ? 0 : strand.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		StrandedGenomicCoordinate other = (StrandedGenomicCoordinate) obj;
		if (strand != other.strand)
			return false;
		return true;
	}

}
