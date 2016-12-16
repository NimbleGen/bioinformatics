package com.roche.heatseq.merged_read_process;

import java.util.Map;

public class UidCorrectorResults {
	private final Map<String, ProbeAssignment> probeIdToReadNamesMap;
	private final int numberOfUidsCorrected;

	public UidCorrectorResults(Map<String, ProbeAssignment> probeIdToReadNamesMap, int numberOfUidsCorrected) {
		super();
		this.probeIdToReadNamesMap = probeIdToReadNamesMap;
		this.numberOfUidsCorrected = numberOfUidsCorrected;
	}

	public Map<String, ProbeAssignment> getProbeIdToReadNamesMap() {
		return probeIdToReadNamesMap;
	}

	public int getNumberOfUidsCorrected() {
		return numberOfUidsCorrected;
	}

}
