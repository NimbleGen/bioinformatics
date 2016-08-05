package com.roche.sequencing.bioinformatics.common.genome;

public class StartAndStopLocationsInFile {

	private final long startLocationInBytes;
	private final long stopLocationInBytes;

	public StartAndStopLocationsInFile(long startLocationInBytes, long stopLocationInBytes) {
		super();
		this.startLocationInBytes = startLocationInBytes;
		this.stopLocationInBytes = stopLocationInBytes;
	}

	public long getStartLocationInBytes() {
		return startLocationInBytes;
	}

	public long getStopLocationInBytes() {
		return stopLocationInBytes;
	}

}
