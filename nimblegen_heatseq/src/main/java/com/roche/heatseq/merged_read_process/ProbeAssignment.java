package com.roche.heatseq.merged_read_process;

import com.roche.heatseq.objects.Probe;
import com.roche.sequencing.bioinformatics.common.sequence.StartAndStopIndex;

public class ProbeAssignment {

	private final Probe assignedProbe;
	private final StartAndStopIndex fivePrimePrimerLocation;
	private final StartAndStopIndex threePrimePrimerLocation;
	private final String uid;
	private String uidGroup;

	private final boolean readContainsN;

	private final static ProbeAssignment NoProbeAssignment = new ProbeAssignment(null, null, null, null, true);

	public static ProbeAssignment createProbeAssignmentNotCompletedBecauseReadContainsN() {
		return NoProbeAssignment;
	}

	public static ProbeAssignment create(Probe assignedProbe, StartAndStopIndex fivePrimePrimerLocation, StartAndStopIndex threePrimePrimerLocation, String uid) {
		return new ProbeAssignment(assignedProbe, fivePrimePrimerLocation, threePrimePrimerLocation, uid, false);
	}

	public static ProbeAssignment ProbeAssignmentNotCompletedBecauseReadContainsN() {
		return NoProbeAssignment;
	}

	public ProbeAssignment(Probe assignedProbe, StartAndStopIndex fivePrimePrimerLocation, StartAndStopIndex threePrimePrimerLocation, String uid, boolean readContainsN) {
		super();
		this.assignedProbe = assignedProbe;
		this.fivePrimePrimerLocation = fivePrimePrimerLocation;
		this.threePrimePrimerLocation = threePrimePrimerLocation;
		this.readContainsN = readContainsN;
		this.uid = uid;
	}

	public Probe getAssignedProbe() {
		return assignedProbe;
	}

	public StartAndStopIndex getFivePrimePrimerLocation() {
		return fivePrimePrimerLocation;
	}

	public StartAndStopIndex getThreePrimePrimerLocation() {
		return threePrimePrimerLocation;
	}

	public String getUid() {
		return uid;
	}

	public boolean isReadContainsN() {
		return readContainsN;
	}

	public boolean readContainsN() {
		return readContainsN;
	}

	public String getUidGroup() {
		return uidGroup;
	}

	public void setUidGroup(String uidGroup) {
		this.uidGroup = uidGroup;
	}

	public static ProbeAssignment NoProbeAssignment() {
		return NoProbeAssignment;
	}

	public boolean isProbeAssigned() {
		return assignedProbe != null;
	}

}
