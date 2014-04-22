package com.roche.sequencing.bioinformatics.common.sequence;

public class StartAndStopIndex {

	private final int startIndex;
	private final int stopIndex;

	public StartAndStopIndex(int startIndex, int stopIndex) {
		super();
		this.startIndex = startIndex;
		this.stopIndex = stopIndex;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getStopIndex() {
		return stopIndex;
	}

	@Override
	public String toString() {
		return "StartAndStopIndex [startIndex=" + startIndex + ", stopIndex=" + stopIndex + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + startIndex;
		result = prime * result + stopIndex;
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
		StartAndStopIndex other = (StartAndStopIndex) obj;
		if (startIndex != other.startIndex)
			return false;
		if (stopIndex != other.stopIndex)
			return false;
		return true;
	}

}
