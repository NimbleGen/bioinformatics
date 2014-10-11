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
	private final List<IReadPair> uniqueReadPairs;
	private final List<IReadPair> duplicateReadPairs;

	public UidReductionResultsForAProbe(ProbeProcessingStats probeProcessingStats, List<IReadPair> uniqueReadPairs, List<IReadPair> duplicateReadPairs) {
		super();
		this.probeProcessingStats = probeProcessingStats;
		this.uniqueReadPairs = uniqueReadPairs;
		this.duplicateReadPairs = duplicateReadPairs;
	}

	public ProbeProcessingStats getProbeProcessingStats() {
		return probeProcessingStats;
	}

	public List<IReadPair> getUniqueReadPairs() {
		return uniqueReadPairs;
	}

	public List<IReadPair> getDuplicateReadPairs() {
		return duplicateReadPairs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((duplicateReadPairs == null) ? 0 : duplicateReadPairs.hashCode());
		result = prime * result + ((probeProcessingStats == null) ? 0 : probeProcessingStats.hashCode());
		result = prime * result + ((uniqueReadPairs == null) ? 0 : uniqueReadPairs.hashCode());
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
		UidReductionResultsForAProbe other = (UidReductionResultsForAProbe) obj;
		if (duplicateReadPairs == null) {
			if (other.duplicateReadPairs != null)
				return false;
		} else if (!duplicateReadPairs.equals(other.duplicateReadPairs))
			return false;
		if (probeProcessingStats == null) {
			if (other.probeProcessingStats != null)
				return false;
		} else if (!probeProcessingStats.equals(other.probeProcessingStats))
			return false;
		if (uniqueReadPairs == null) {
			if (other.uniqueReadPairs != null)
				return false;
		} else if (!uniqueReadPairs.equals(other.uniqueReadPairs))
			return false;
		return true;
	}

}
