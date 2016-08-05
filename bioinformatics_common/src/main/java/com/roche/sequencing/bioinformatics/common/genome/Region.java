package com.roche.sequencing.bioinformatics.common.genome;

public class Region {

	private final GenomicRangedCoordinate location;
	private final String string;

	public Region(GenomicRangedCoordinate location) {
		super();
		this.location = location;
		if (location.getStartLocation() == location.getStopLocation()) {
			this.string = location.getContainerName() + ":" + location.getStartLocation();
		} else {
			this.string = location.getContainerName() + ":" + location.getStartLocation() + "-" + location.getStopLocation();
		}
	}

	public GenomicRangedCoordinate getLocation() {
		return location;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((location == null) ? 0 : location.hashCode());
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
		Region other = (Region) obj;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return this.string;
	}

	public static Region fromString(String string) {
		GenomicRangedCoordinate coordinate = null;
		String[] splitString = string.split(":");
		String chromosomeName = splitString[0];
		String locations = splitString[1];
		if (locations.contains("-")) {
			splitString = locations.split("-");
			long start = Long.parseLong(splitString[0]);
			long stop = Long.parseLong(splitString[1]);
			coordinate = new GenomicRangedCoordinate(chromosomeName, start, stop);
		} else {
			long start = Long.parseLong(locations);
			coordinate = new GenomicRangedCoordinate(chromosomeName, start, start);
		}
		return new Region(coordinate);
	}

}
