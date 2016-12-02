package com.roche.sequencing.bioinformatics.common.genome;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.roche.sequencing.bioinformatics.common.sequence.Strand;

public class GenomicContext {
	private final String container;
	private final long start;
	private final long stop;
	private final Strand strand;
	private final String annotation;

	private final static Pattern pattern = Pattern.compile("^>(.*?):(\\d+)\\.\\.(\\d+):([+-])\\s(.*?)$");

	public GenomicContext(String container, long start, long stop, Strand strand, String annotation) {
		super();
		this.container = container;
		this.start = start;
		this.stop = stop;
		this.strand = strand;
		this.annotation = annotation;
	}

	public GenomicContext(String descriptionAsString) {
		Matcher matcher = pattern.matcher(descriptionAsString);
		if (matcher.find()) {
			container = matcher.group(1);
			start = Long.parseLong(matcher.group(2));
			stop = Long.parseLong(matcher.group(3));
			strand = Strand.fromString(matcher.group(4));
			annotation = matcher.group(5);
		} else {
			throw new IllegalStateException("descriptionAsString[" + descriptionAsString + "] is not of the format container:start..stop:strand annotation (example: chr1:1200..1210:+ not_brca1)");
		}
	}

	public String getContainer() {
		return container;
	}

	public long getStart() {
		return start;
	}

	public long getStop() {
		return stop;
	}

	public Strand getStrand() {
		return strand;
	}

	public Region getRegion() {
		return new Region(new GenomicRangedCoordinate(container, start, stop));
	}

	public String getAnnotation() {
		return annotation;
	}

	public static Pattern getPattern() {
		return pattern;
	}

	public String getDescriptionAsString() {
		String description = container + ":" + start + ".." + stop + ":" + strand + " ";
		if (annotation != null) {
			description += annotation;
		}
		return description;
	}

	@Override
	public String toString() {
		return "SequenceDescription [container=" + container + ", start=" + start + ", stop=" + stop + ", strand=" + strand + ", annotation=" + annotation + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
		result = prime * result + ((container == null) ? 0 : container.hashCode());
		result = prime * result + (int) (start ^ (start >>> 32));
		result = prime * result + (int) (stop ^ (stop >>> 32));
		result = prime * result + ((strand == null) ? 0 : strand.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenomicContext other = (GenomicContext) obj;
		if (annotation == null) {
			if (other.annotation != null)
				return false;
		} else if (!annotation.equals(other.annotation))
			return false;
		if (container == null) {
			if (other.container != null)
				return false;
		} else if (!container.equals(other.container))
			return false;
		if (start != other.start)
			return false;
		if (stop != other.stop)
			return false;
		if (strand != other.strand)
			return false;
		return true;
	}

}
