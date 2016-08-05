package com.roche.sequencing.bioinformatics.common.mapping;

public class CoverageStats {
	private final int totalUniqueBasesCoveredInRegionOfInterest;
	private final int totalRedundantBasesCoveredInRegionOfInterest;
	private final int totalUniqueBasesOutsideRegionOfInterest;
	private final int totalRedundantBasesOutsideRegionOfInterest;
	private final int totalBases;

	public CoverageStats(int totalUniqueBasesCoveredInRegionOfInterest, int totalRedundantBasesCoveredInRegionOfInterest, int totalUniqueBasesOutsideRegionOfInterest,
			int totalRedundantBasesOutsideRegionOfInterest, int totalBases) {
		super();
		this.totalUniqueBasesCoveredInRegionOfInterest = totalUniqueBasesCoveredInRegionOfInterest;
		this.totalRedundantBasesCoveredInRegionOfInterest = totalRedundantBasesCoveredInRegionOfInterest;
		this.totalUniqueBasesOutsideRegionOfInterest = totalUniqueBasesOutsideRegionOfInterest;
		this.totalRedundantBasesOutsideRegionOfInterest = totalRedundantBasesOutsideRegionOfInterest;
		this.totalBases = totalBases;
	}

	public int getTotalUniqueBasesCoveredInRegionOfInterest() {
		return totalUniqueBasesCoveredInRegionOfInterest;
	}

	public int getTotalRedundantBasesCoveredInRegionOfInterest() {
		return totalRedundantBasesCoveredInRegionOfInterest;
	}

	public int getTotalUniqueBasesOutsideRegionOfInterest() {
		return totalUniqueBasesOutsideRegionOfInterest;
	}

	public int getTotalRedundantBasesOutsideRegionOfInterest() {
		return totalRedundantBasesOutsideRegionOfInterest;
	}

	public int getTotalBases() {
		return totalBases;
	}

	@Override
	public String toString() {
		return "CoverageStats [totalUniqueBasesCoveredInRegionOfInterest=" + totalUniqueBasesCoveredInRegionOfInterest + ", totalRedundantBasesCoveredInRegionOfInterest="
				+ totalRedundantBasesCoveredInRegionOfInterest + ", totalUniqueBasesOutsideRegionOfInterest=" + totalUniqueBasesOutsideRegionOfInterest
				+ ", totalRedundantBasesOutsideRegionOfInterest=" + totalRedundantBasesOutsideRegionOfInterest + ", totalBases=" + totalBases + "]";
	}

}
