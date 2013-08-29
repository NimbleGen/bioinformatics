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

package com.roche.heatseq.qualityreport;

import com.roche.heatseq.objects.Probe;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * Simple object to hold onto statistics about processing that are useful for diagnostics
 * 
 * 
 */
public class ProbeProcessingStats {
	private final Probe probe;
	private final int totalUids;
	private final double averageNumberOfReadPairsPerUid;
	private final double standardDeviationOfReadPairsPerUid;
	private final int totalDuplicateReadPairsRemoved;
	private final int totalReadPairsRemainingAfterReduction;
	private final int minNumberOfReadPairsPerUid;
	private final int maxNumberOfReadPairsPerUid;
	private final String uidOfEntryWithMaxReadPairs;
	private final int totalTimeToProcessInMs;

	/**
	 * Constructor
	 * 
	 * @param probe
	 * @param totalUids
	 * @param averageNumberOfReadPairsPerUid
	 * @param standardDeviationOfReadPairsPerUid
	 * @param totalDuplicateReadPairsRemoved
	 * @param totalReadPairsRemainingAfterReduction
	 * @param minNumberOfReadPairsPerUid
	 * @param maxNumberOfReadPairsPerUid
	 * @param uidOfEntryWithMaxReadPairs
	 * @param totalTimeToProcessInMs
	 */
	public ProbeProcessingStats(Probe probe, int totalUids, double averageNumberOfReadPairsPerUid, double standardDeviationOfReadPairsPerUid, int totalDuplicateReadPairsRemoved,
			int totalReadPairsRemainingAfterReduction, int minNumberOfReadPairsPerUid, int maxNumberOfReadPairsPerUid, String uidOfEntryWithMaxReadPairs, int totalTimeToProcessInMs) {
		super();
		this.probe = probe;
		this.totalUids = totalUids;
		this.averageNumberOfReadPairsPerUid = averageNumberOfReadPairsPerUid;
		this.standardDeviationOfReadPairsPerUid = standardDeviationOfReadPairsPerUid;
		this.totalDuplicateReadPairsRemoved = totalDuplicateReadPairsRemoved;
		this.totalReadPairsRemainingAfterReduction = totalReadPairsRemainingAfterReduction;
		this.minNumberOfReadPairsPerUid = minNumberOfReadPairsPerUid;
		this.maxNumberOfReadPairsPerUid = maxNumberOfReadPairsPerUid;
		this.uidOfEntryWithMaxReadPairs = uidOfEntryWithMaxReadPairs;
		this.totalTimeToProcessInMs = totalTimeToProcessInMs;
	}

	public int getTotalUids() {
		return totalUids;
	}

	public double getAverageNumberOfReadPairsPerUid() {
		return averageNumberOfReadPairsPerUid;
	}

	public double getStandardDeviationOfReadPairsPerUid() {
		return standardDeviationOfReadPairsPerUid;
	}

	public int getTotalDuplicateReadPairsRemoved() {
		return totalDuplicateReadPairsRemoved;
	}

	public int getTotalReadPairsRemainingAfterReduction() {
		return totalReadPairsRemainingAfterReduction;
	}

	public Probe getProbe() {
		return probe;
	}

	public String toReportString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(probe.getProbeId() + StringUtil.TAB + probe.getSequenceName() + StringUtil.TAB + probe.getCaptureTargetStart() + StringUtil.TAB + probe.getCaptureTargetStop()
				+ StringUtil.TAB + probe.getProbeStrand() + StringUtil.TAB);
		stringBuilder.append(totalUids + StringUtil.TAB + averageNumberOfReadPairsPerUid + StringUtil.TAB + standardDeviationOfReadPairsPerUid + StringUtil.TAB + minNumberOfReadPairsPerUid
				+ StringUtil.TAB + maxNumberOfReadPairsPerUid + StringUtil.TAB + uidOfEntryWithMaxReadPairs.toUpperCase() + StringUtil.TAB + totalDuplicateReadPairsRemoved + StringUtil.TAB
				+ totalReadPairsRemainingAfterReduction + StringUtil.TAB + DateUtil.convertMillisecondsToHHMMSS(totalTimeToProcessInMs) + StringUtil.TAB);

		stringBuilder.append(StringUtil.NEWLINE);

		return stringBuilder.toString();
	}

}
