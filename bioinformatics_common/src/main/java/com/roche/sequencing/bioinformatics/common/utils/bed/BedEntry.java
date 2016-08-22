package com.roche.sequencing.bioinformatics.common.utils.bed;

import java.util.Collections;
import java.util.List;

class BedEntry implements IBedEntry {

	private final String chromosomeName;
	private final int chromosomeStart;
	private final int chromosomeEnd;
	private final String name;
	private final Integer score;
	private final Character strand;
	private final Integer thickStart;
	private final Integer thickEnd;
	private final RGB itemRgb;
	private final Integer blockCount;
	private final List<Integer> blockSizes;
	private final List<Integer> blockStarts;

	BedEntry(String chromosomeName, int chromosomeStart, int chromosomeEnd) {
		this(chromosomeName, chromosomeStart, chromosomeEnd, null, null, null, null, null, null, null, null, null);

	}

	BedEntry(String chromosomeName, int chromosomeStart, int chromosomeEnd, String name, Integer score, Character strand, Integer thickStart, Integer thickEnd, RGB itemRgb, Integer blockCount,
			List<Integer> blockSizes, List<Integer> blockStarts) {
		super();
		this.chromosomeName = chromosomeName;
		this.chromosomeStart = chromosomeStart;
		this.chromosomeEnd = chromosomeEnd;
		this.name = name;
		this.score = score;
		this.strand = strand;
		this.thickStart = thickStart;
		this.thickEnd = thickEnd;
		this.itemRgb = itemRgb;
		this.blockCount = blockCount;
		this.blockSizes = blockSizes;
		this.blockStarts = blockStarts;
	}

	public String getContainerName() {
		return chromosomeName;
	}

	public int getChromosomeStart() {
		return chromosomeStart;
	}

	public int getChromosomeEnd() {
		return chromosomeEnd;
	}

	public String getName() {
		return name;
	}

	public Integer getScore() {
		return score;
	}

	public Character getStrand() {
		return strand;
	}

	public Integer getThickStart() {
		return thickStart;
	}

	public Integer getThickEnd() {
		return thickEnd;
	}

	public RGB getItemRgb() {
		return itemRgb;
	}

	public int getBlockCount() {
		int blockCountAsInt = 0;
		if (blockCount != null) {
			blockCountAsInt = blockCount;
		}
		return blockCountAsInt;
	}

	public List<Integer> getBlockSizes() {
		List<Integer> nonNullBlockSizes = blockSizes;
		if (nonNullBlockSizes == null) {
			nonNullBlockSizes = Collections.emptyList();
		}
		return nonNullBlockSizes;
	}

	public List<Integer> getBlockStarts() {
		List<Integer> nonNullBlockStarts = blockStarts;
		if (nonNullBlockStarts == null) {
			nonNullBlockStarts = Collections.emptyList();
		}
		return nonNullBlockStarts;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((blockCount == null) ? 0 : blockCount.hashCode());
		result = prime * result + ((blockSizes == null) ? 0 : blockSizes.hashCode());
		result = prime * result + ((blockStarts == null) ? 0 : blockStarts.hashCode());
		result = prime * result + chromosomeEnd;
		result = prime * result + ((chromosomeName == null) ? 0 : chromosomeName.hashCode());
		result = prime * result + chromosomeStart;
		result = prime * result + ((itemRgb == null) ? 0 : itemRgb.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((score == null) ? 0 : score.hashCode());
		result = prime * result + ((strand == null) ? 0 : strand.hashCode());
		result = prime * result + ((thickEnd == null) ? 0 : thickEnd.hashCode());
		result = prime * result + ((thickStart == null) ? 0 : thickStart.hashCode());
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
		BedEntry other = (BedEntry) obj;
		if (blockCount == null) {
			if (other.blockCount != null)
				return false;
		} else if (!blockCount.equals(other.blockCount))
			return false;
		if (blockSizes == null) {
			if (other.blockSizes != null)
				return false;
		} else if (!blockSizes.equals(other.blockSizes))
			return false;
		if (blockStarts == null) {
			if (other.blockStarts != null)
				return false;
		} else if (!blockStarts.equals(other.blockStarts))
			return false;
		if (chromosomeEnd != other.chromosomeEnd)
			return false;
		if (chromosomeName == null) {
			if (other.chromosomeName != null)
				return false;
		} else if (!chromosomeName.equals(other.chromosomeName))
			return false;
		if (chromosomeStart != other.chromosomeStart)
			return false;
		if (itemRgb == null) {
			if (other.itemRgb != null)
				return false;
		} else if (!itemRgb.equals(other.itemRgb))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (score == null) {
			if (other.score != null)
				return false;
		} else if (!score.equals(other.score))
			return false;
		if (strand == null) {
			if (other.strand != null)
				return false;
		} else if (!strand.equals(other.strand))
			return false;
		if (thickEnd == null) {
			if (other.thickEnd != null)
				return false;
		} else if (!thickEnd.equals(other.thickEnd))
			return false;
		if (thickStart == null) {
			if (other.thickStart != null)
				return false;
		} else if (!thickStart.equals(other.thickStart))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BedEntry [chromosomeName=" + chromosomeName + ", chromosomeStart=" + chromosomeStart + ", chromosomeEnd=" + chromosomeEnd + ", name=" + name + ", score=" + score + ", strand="
				+ strand + ", thickStart=" + thickStart + ", thickEnd=" + thickEnd + ", itemRgb=" + itemRgb + ", blockCount=" + blockCount + ", blockSizes=" + blockSizes + ", blockStarts="
				+ blockStarts + "]";
	}

}
