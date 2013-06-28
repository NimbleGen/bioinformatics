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

package com.roche.heatseq.objects;

import java.util.List;

import com.roche.heatseq.qualityreport.ProbeProcessingStats;

/**
 * Processing statistics and read pairs that are the result of reducing reads for a probe by UID
 */
public class UidReductionResultsForAProbe {
	private final ProbeProcessingStats probeProcessingStats;
	private final List<IReadPair> readPairs;

	public UidReductionResultsForAProbe(ProbeProcessingStats probeProcessingStats, List<IReadPair> readPairs) {
		super();
		this.probeProcessingStats = probeProcessingStats;
		this.readPairs = readPairs;
	}

	public ProbeProcessingStats getProbeProcessingStats() {
		return probeProcessingStats;
	}

	public List<IReadPair> getReadPairs() {
		return readPairs;
	}
}
