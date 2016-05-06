package com.roche.sequencing.bioinformatics.common.mapping;

public class RangeWithTally {

	private final int start;
	private final int stop;
	private final int count;

	RangeWithTally(int start, int stop, int count) {
		super();
		this.start = start;
		this.stop = stop;
		this.count = count;
	}

	public int getStart() {
		return start;
	}

	public int getStop() {
		return stop;
	}

	public int getCount() {
		return count;
	}

	@Override
	public String toString() {
		return "RangeWithTally [start=" + start + ", stop=" + stop + ", count=" + count + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + count;
		result = prime * result + start;
		result = prime * result + stop;
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
		RangeWithTally other = (RangeWithTally) obj;
		if (count != other.count)
			return false;
		if (start != other.start)
			return false;
		if (stop != other.stop)
			return false;
		return true;
	}

}
