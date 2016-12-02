package com.roche.sequencing.bioinformatics.common.genome;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.roche.sequencing.bioinformatics.common.sequence.Strand;

public class StrandedGenomicRangedCoordinate extends GenomicRangedCoordinate {

	private static Pattern pattern = Pattern.compile("([a-zA-Z0-9]+[:]\\d+[:]\\d+)[:]([+-])");

	private final Strand strand;

	public StrandedGenomicRangedCoordinate(String locationAsString) {
		super(getNonStrandedString(locationAsString));
		Matcher matcher = pattern.matcher(locationAsString);
		if (matcher.find()) {
			this.strand = Strand.fromString(matcher.group(2));
		} else {
			throw new IllegalArgumentException(
					"The provided locationAsString["
							+ locationAsString
							+ "] does not match the required format [container:startPosition-endPosition:strand] or [container:startPosition:endPosition:strand] for example: [chr1:1:1000:+] or [chr1:1-1000:+]");
		}
	}

	private static String getNonStrandedString(String locationAsString) {
		String nonStrandedString = "";
		Matcher matcher = pattern.matcher(locationAsString);
		if (matcher.find()) {
			nonStrandedString = matcher.group(1);
		} else {
			throw new IllegalArgumentException(
					"The provided locationAsString["
							+ locationAsString
							+ "] does not match the required format [container:startPosition-endPosition:strand] or [container:startPosition:endPosition:strand] for example: [chr1:1:1000:+] or [chr1:1-1000:+]");
		}
		return nonStrandedString;
	}

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
