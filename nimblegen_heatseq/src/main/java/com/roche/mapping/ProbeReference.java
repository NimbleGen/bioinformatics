/**
 *   Copyright 2013 Roche NimbleGen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.roche.mapping;

import com.roche.heatseq.objects.Probe;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;

/**
 * 
 * Convenience object for keeping track of probes and its counterpart reversed probe
 * 
 */
class ProbeReference {

	private final Probe probe;
	private final boolean isReversed;

	ProbeReference(Probe probe, boolean isReversed) {
		super();
		this.probe = probe;
		this.isReversed = isReversed;
	}

	public Probe getProbe() {
		return probe;
	}

	public boolean isReverseStrand() {
		return isReversed;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isReversed ? 1231 : 1237);
		result = prime * result + ((probe == null) ? 0 : probe.hashCode());
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
		ProbeReference other = (ProbeReference) obj;
		if (isReversed != other.isReversed)
			return false;
		if (probe == null) {
			if (other.probe != null)
				return false;
		} else if (!probe.equals(other.probe))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ProbeReference [probe=" + probe + ", isReverseStrand=" + isReversed + "]";
	}

	public Strand getProbeStrand() {
		Strand strand = probe.getProbeStrand();
		if (isReversed) {
			strand = strand.getOpposite();
		}
		return strand;
	}

	public ISequence getCaptureTargetSequence() {
		ISequence captureTargetSequence = probe.getCaptureTargetSequence();
		if (isReversed) {
			captureTargetSequence = captureTargetSequence.getReverseCompliment();
		}
		return captureTargetSequence;
	}

	public int getCaptureTargetStart() {
		return probe.getCaptureTargetStart();
	}

}
