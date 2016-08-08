package com.roche.sequencing.bioinformatics.common.genome;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.sequencing.bioinformatics.common.utils.AlphaNumericStringComparator;

public class GenomicRangedCoordinate implements Comparable<GenomicRangedCoordinate> {

	private final static Logger logger = LoggerFactory.getLogger(GenomicRangedCoordinate.class);

	public final static Comparator<String> CONTAINER_COMPARATOR = new AlphaNumericStringComparator(true);

	private final String container;
	private final long startLocation;
	private final long stopLocation;

	public GenomicRangedCoordinate(String container, long startLocation, long stopLocation) {
		super();
		this.container = container;
		this.startLocation = Math.min(startLocation, stopLocation);
		this.stopLocation = Math.max(startLocation, stopLocation);
	}

	public String getContainerName() {
		return container;
	}

	public long getStartLocation() {
		return startLocation;
	}

	public long getStopLocation() {
		return stopLocation;
	}

	public int size() {
		return (int) (stopLocation - startLocation + 1);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((container == null) ? 0 : container.hashCode());
		result = prime * result + (int) (startLocation ^ (startLocation >>> 32));
		result = prime * result + (int) (stopLocation ^ (stopLocation >>> 32));
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
		GenomicRangedCoordinate other = (GenomicRangedCoordinate) obj;
		if (container == null) {
			if (other.container != null)
				return false;
		} else if (!container.equals(other.container))
			return false;
		if (startLocation != other.startLocation)
			return false;
		if (stopLocation != other.stopLocation)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return container + ":" + startLocation + "-" + stopLocation;
	}

	public String toSummarizedString() {
		return container + ":" + startLocation + "-" + stopLocation;
	}

	public static GenomicRangedCoordinate fromString(String genomicRangedCoordinateAsString) {
		GenomicRangedCoordinate genomicRangedCoordinate = null;
		try {
			String[] split = genomicRangedCoordinateAsString.split(":");
			if (split.length == 2) {
				String container = split[0];
				String[] locationSplit = split[1].split("-");
				if (locationSplit.length == 2) {
					long startLocation = Long.parseLong(locationSplit[0]);
					long stopLocation = Long.parseLong(locationSplit[1]);
					genomicRangedCoordinate = new GenomicRangedCoordinate(container, startLocation, stopLocation);
				}
			}
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
		return genomicRangedCoordinate;
	}

	public GenomicCoordinate getStart() {
		return new GenomicCoordinate(container, startLocation);
	}

	public GenomicCoordinate getStop() {
		return new GenomicCoordinate(container, stopLocation);
	}

	@Override
	public int compareTo(GenomicRangedCoordinate rhs) {
		int result = CONTAINER_COMPARATOR.compare(container, rhs.getContainerName());
		if (result == 0) {
			result = Long.compare(startLocation, rhs.getStartLocation());
		}

		if (result == 0) {
			result = Long.compare(stopLocation, rhs.getStopLocation());
		}

		return result;
	}

}
