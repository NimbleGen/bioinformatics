package com.roche.sequencing.bioinformatics.common.genome;

public class GenomicCoordinate {

	private final String container;
	private final long location;

	public GenomicCoordinate(String container, long location) {
		super();
		this.container = container;
		this.location = location;
	}

	public String getContainer() {
		return container;
	}

	public long getLocation() {
		return location;
	}

	public static GenomicCoordinate fromString(String string) {
		String[] splitString = string.split(":");
		String chromosomeName = splitString[0];
		long location = Long.parseLong(splitString[1]);
		return new GenomicCoordinate(chromosomeName, location);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((container == null) ? 0 : container.hashCode());
		result = prime * result + (int) (location ^ (location >>> 32));
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
		GenomicCoordinate other = (GenomicCoordinate) obj;
		if (container == null) {
			if (other.container != null)
				return false;
		} else if (!container.equals(other.container))
			return false;
		if (location != other.location)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return container + ":" + location;
	}

}
