package com.roche.sequencing.bioinformatics.common.utils.bed;

import java.util.List;

class SimpleBedEntry implements IBedEntry {

	private final String chromosomeName;
	private final int start;
	private final int chromosomeEnd;

	public SimpleBedEntry(String chromosomeName, int chromosomeStart, int chromosomeEnd) {
		super();
		this.chromosomeName = chromosomeName;
		this.start = chromosomeStart;
		this.chromosomeEnd = chromosomeEnd;
	}

	public String getChromosomeName() {
		return chromosomeName;
	}

	public int getChromosomeStart() {
		return start;
	}

	public int getChromosomeEnd() {
		return chromosomeEnd;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Integer getScore() {
		return null;
	}

	@Override
	public Character getStrand() {
		return null;
	}

	@Override
	public Integer getThickStart() {
		return null;
	}

	@Override
	public Integer getThickEnd() {
		return null;
	}

	@Override
	public RGB getItemRgb() {
		return null;
	}

	@Override
	public Integer getBlockCount() {
		return null;
	}

	@Override
	public List<Integer> getBlockSizes() {
		return null;
	}

	@Override
	public List<Integer> getBlockStarts() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + chromosomeEnd;
		result = prime * result + ((chromosomeName == null) ? 0 : chromosomeName.hashCode());
		result = prime * result + start;
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
		SimpleBedEntry other = (SimpleBedEntry) obj;
		if (chromosomeEnd != other.chromosomeEnd)
			return false;
		if (chromosomeName == null) {
			if (other.chromosomeName != null)
				return false;
		} else if (!chromosomeName.equals(other.chromosomeName))
			return false;
		if (start != other.start)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SimpleBedEntry [chromosomeName=" + chromosomeName + ", chromosomeStart=" + start + ", chromosomeEnd=" + chromosomeEnd + "]";
	}

}
