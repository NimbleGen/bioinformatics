package com.roche.sequencing.bioinformatics.common.genome;

import com.roche.sequencing.bioinformatics.common.sequence.Strand;

public class StrandedGenomicRangedCoordinate extends GenomicRangedCoordinate {

	private final Strand strand;

	public StrandedGenomicRangedCoordinate(String container, Strand strand, long startLocation, long stopLocation) {
		super(container, startLocation, stopLocation);
		this.strand = strand;
	}

	public Strand getStrand() {
		return strand;
	}

	public GenomicRangedCoordinate getGenomicRangedCoordinate() {
		return this;
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
		StrandedGenomicRangedCoordinate other = (StrandedGenomicRangedCoordinate) obj;
		if (strand != other.strand)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getContainerName() + ":" + getStartLocation() + "-" + getStopLocation() + ":" + strand;
	}

}
