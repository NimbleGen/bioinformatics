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

package com.roche.sequencing.bioinformatics.common.alignment;

import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * 
 * Represents the cigar string (details on how two sequences align with each other)
 * 
 */
public class CigarString {

	private final String nonSummarizedCigarString;

	private CigarString(String nonSummarizedCigarString) {
		super();
		this.nonSummarizedCigarString = nonSummarizedCigarString;
	}

	static CigarString FromNonSummarizedCigarString(String nonSummarizedCigarStringWithSequenceMismatches) {
		return new CigarString(nonSummarizedCigarStringWithSequenceMismatches);
	}

	public String getCigarString(boolean summarize, boolean includeSequenceMismatches) {
		return getCigarString(summarize, includeSequenceMismatches, false, false);
	}

	public String getCigarString(boolean summarize, boolean includeSequenceMismatches, boolean ignoreSequenceDeletions, boolean ignoreSequenceInsertions) {
		String cigarString = nonSummarizedCigarString;
		if (ignoreSequenceDeletions) {
			cigarString = cigarString.replaceAll("D", "");
		}
		if (ignoreSequenceInsertions) {
			cigarString = cigarString.replaceAll("I", "");
		}
		if (!includeSequenceMismatches) {
			cigarString = CigarStringUtil.replaceSequenceMatchesAndMismatchesFromCigarString(cigarString);
		}
		if (summarize) {
			cigarString = CigarStringUtil.summarizeCigarString(cigarString);
		}

		return cigarString;
	}

	public int getLengthOfReference() {
		int length = 0;
		for (int i = 0; i < nonSummarizedCigarString.length(); i++) {
			char character = nonSummarizedCigarString.charAt(i);
			if (character != 'I') {
				length++;
			}
		}
		return length;
	}

	/**
	 * @return a standard cigar string as found in BAM files (for example, 45M1I37M)
	 */
	public String getStandardCigarString() {
		return getCigarString(true, false, false, false);
	}

	/**
	 * @return a cigar string that represents the alignment of both sequences reversed
	 */
	public CigarString reverse() {
		return new CigarString(StringUtil.reverse(nonSummarizedCigarString));
	}

	/**
	 * @return the edit distance (number of mutations or inserts when converting from the query sequence to the reference sequence).
	 */
	public int getEditDistance() {
		return CigarStringUtil.getEditDistance(this);
	}

	@Override
	public String toString() {
		return "CigarString [nonSummarizedCigarString=" + nonSummarizedCigarString + "]";
	}

}
