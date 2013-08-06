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

import java.util.Set;

import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.objects.ProbesBySequenceName;
import com.roche.sequencing.bioinformatics.common.mapping.SimpleMapper;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;

class SubReadProbeMapper {

	private final static int DEFAULT_COMPARISON_SEQUENCE_SIZE = 8;
	private final static int DEFAULT_MAX_REFERENCE_DEPTH = 1;
	private final static int DEFAULT_MAX_QUERY_DEPTH = 8;
	private final static int DEFAULT_MIN_HIT_THRESHOLD = 3;

	private final SimpleMapper<ProbeReference> simpleMapper;

	/**
	 * Constructor
	 */
	public SubReadProbeMapper() {
		simpleMapper = new SimpleMapper<ProbeReference>(DEFAULT_COMPARISON_SEQUENCE_SIZE, DEFAULT_MAX_REFERENCE_DEPTH, DEFAULT_MAX_QUERY_DEPTH, DEFAULT_MIN_HIT_THRESHOLD);
	}

	/**
	 * Add the probes that the query sequence will be mapped against.
	 * 
	 * @param probesBySequenceName
	 */
	void addProbes(ProbesBySequenceName probesBySequenceName) {
		for (String sequenceName : probesBySequenceName.getSequenceNames()) {
			for (Probe probe : probesBySequenceName.getProbesBySequenceName(sequenceName)) {
				ISequence sequence = probe.getCaptureTargetSequence();
				simpleMapper.addReferenceSequence(sequence, new ProbeReference(probe, false));
				ISequence oppositeStrandSequence = sequence.getReverseCompliment();
				simpleMapper.addReferenceSequence(oppositeStrandSequence, new ProbeReference(probe, true));
			}
		}
	}

	/**
	 * @param querySequence
	 * @return the best candidate probes that match this querySequence
	 */
	Set<ProbeReference> getBestCandidates(ISequence querySequence) {
		Set<ProbeReference> bestCandidates = simpleMapper.getBestCandidateReferences(querySequence);
		return bestCandidates;
	}

}
