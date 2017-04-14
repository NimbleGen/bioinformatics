/*
 *    Copyright 2016 Roche NimbleGen Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.roche.heatseq.qualityreport;

import com.roche.sequencing.bioinformatics.common.utils.NumberFormatterUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.Probe;

/**
 * Simple object to hold onto statistics about processing that are useful for diagnostics
 * 
 * 
 */
public class ProbeProcessingStats {
	private final Probe probe;
	private final int totalDuplicateReadPairsRemoved;
	private final int totalReadPairsRemainingAfterReduction;
	private final int maxNumberOfReadPairsPerUid;
	private final String uidComposition;
	private final String uidCompositionByPosition;
	private final String weightedUidComposition;
	private final String weightedUidCompositionByPosition;
	private int numberOfUniqueReadPairsUnableExtendPrimer;
	private int numberOfDuplicateReadPairsUnableToExtendPrimer;

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
	public ProbeProcessingStats(Probe probe, int totalDuplicateReadPairsRemoved, int totalReadPairsRemainingAfterReduction, int maxNumberOfReadPairsPerUid, String uidComposition,
			String uidCompositionByPosition, String weightedUidComposition, String weightedUidCompositionByPosition) {
		super();
		this.probe = probe;
		this.totalDuplicateReadPairsRemoved = totalDuplicateReadPairsRemoved;
		this.totalReadPairsRemainingAfterReduction = totalReadPairsRemainingAfterReduction;
		this.maxNumberOfReadPairsPerUid = maxNumberOfReadPairsPerUid;
		this.uidComposition = uidComposition;
		this.uidCompositionByPosition = uidCompositionByPosition;
		this.weightedUidComposition = weightedUidComposition;
		this.weightedUidCompositionByPosition = weightedUidCompositionByPosition;
		this.numberOfUniqueReadPairsUnableExtendPrimer = 0;
		this.numberOfDuplicateReadPairsUnableToExtendPrimer = 0;
	}

	public int getTotalDuplicateReadPairsRemoved() {
		return totalDuplicateReadPairsRemoved - numberOfDuplicateReadPairsUnableToExtendPrimer;
	}

	public int getTotalReadPairsRemainingAfterReduction() {
		return totalReadPairsRemainingAfterReduction - numberOfUniqueReadPairsUnableExtendPrimer;
	}

	public Probe getProbe() {
		return probe;
	}

	public String toUidCompositionByProbeString() {
		String uidCompositionByProbeRow = StringUtil.TAB + uidComposition + StringUtil.TAB + uidCompositionByPosition + StringUtil.TAB + weightedUidComposition + StringUtil.TAB
				+ weightedUidCompositionByPosition;
		return uidCompositionByProbeRow;
	}

	String toReportString() {
		StringBuilder stringBuilder = new StringBuilder();

		int readPairsAfterReduction = getTotalReadPairsRemainingAfterReduction();
		int duplicateReadPairs = getTotalDuplicateReadPairsRemoved();

		int totalReadPairs = readPairsAfterReduction + duplicateReadPairs;

		double averageNumberOfReadPairsPerUid = getAverageNumberOfReadPairsPerUid();

		stringBuilder.append(probe.getProbeId() + StringUtil.TAB + totalReadPairs + StringUtil.TAB + readPairsAfterReduction + StringUtil.TAB + duplicateReadPairs + StringUtil.TAB);
		stringBuilder.append(NumberFormatterUtil.formatDouble(averageNumberOfReadPairsPerUid, 2) + StringUtil.TAB + maxNumberOfReadPairsPerUid);

		return stringBuilder.toString();
	}

	public void setNumberOfUniqueReadPairsUnableToExtendPrimer(int numberOfUniqueReadPairsUnableExtendPrimer) {
		this.numberOfUniqueReadPairsUnableExtendPrimer = numberOfUniqueReadPairsUnableExtendPrimer;
	}

	public void setNumberOfDuplicateReadPairsUnableToExtendPrimer(int numberOfDuplicateReadPairsUnableExtendPrimer) {
		this.numberOfDuplicateReadPairsUnableToExtendPrimer = numberOfDuplicateReadPairsUnableExtendPrimer;
	}

	public double getAverageNumberOfReadPairsPerUid() {
		double averageNumberOfReadPairsPerUid = 0;

		double readPairsAfterReduction = getTotalReadPairsRemainingAfterReduction();
		double duplicateReadPairs = getTotalDuplicateReadPairsRemoved();

		double totalReadPairs = readPairsAfterReduction + duplicateReadPairs;

		if ((totalReadPairs != 0) && (readPairsAfterReduction != 0)) {
			averageNumberOfReadPairsPerUid = totalReadPairs / ((double) readPairsAfterReduction);
		}
		return averageNumberOfReadPairsPerUid;
	}
}
