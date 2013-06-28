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

package com.roche.sequencing.bioinformatics.common.alignment;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCode;

/**
 * 
 * A pair of sequences representing their alignment relative to each other.
 * 
 */
public class AlignmentPair {
	private final ISequence referenceAlignment;
	private final ISequence queryAlignment;

	private int firstNonDeletionIndex;
	private int lastNonDeletionIndex;

	AlignmentPair(ISequence referenceAlignment, ISequence queryAlignment) {
		super();
		this.referenceAlignment = referenceAlignment;
		this.queryAlignment = queryAlignment;

		firstNonDeletionIndex = 0;
		while ((firstNonDeletionIndex < queryAlignment.size()) && (queryAlignment.getCodeAt(firstNonDeletionIndex) == IupacNucleotideCode.GAP)) {
			firstNonDeletionIndex++;
		}

		lastNonDeletionIndex = queryAlignment.size() - 1;
		while ((lastNonDeletionIndex >= 0) && (queryAlignment.getCodeAt(lastNonDeletionIndex) == IupacNucleotideCode.GAP)) {
			lastNonDeletionIndex--;
		}

	}

	/**
	 * @return the reference sequence alignment
	 */
	public ISequence getReferenceAlignment() {
		return referenceAlignment;
	}

	/**
	 * @return the query sequence alignment
	 */
	public ISequence getQueryAlignment() {
		return queryAlignment;
	}

	/**
	 * @return the reference sequence alignment with no beginning or ending gaps
	 */
	public ISequence getReferenceAlignmentWithoutTerminalInserts() {
		return referenceAlignment.subSequence(firstNonDeletionIndex, lastNonDeletionIndex);
	}

	/**
	 * @return the query sequence alignment with no beginning or ending gaps
	 */
	public ISequence getQueryAlignmentWithoutTerminalInserts() {
		return queryAlignment.subSequence(firstNonDeletionIndex, lastNonDeletionIndex);
	}

	/**
	 * @return the cigar string representative of this alignment
	 */
	public CigarString getCigarString() {
		return CigarStringUtil.getCigarString(this);
	}

	/**
	 * @return the cigar string representative of the reverse of this alignment
	 */
	public CigarString getReverseCigarString() {
		return getCigarString().reverse();
	}

	/**
	 * @return the edit distance (number of mutations or inserts when converting from the query sequence to the reference sequence).
	 */
	public int getEditDistance() {
		return getCigarString().getEditDistance();
	}

	/**
	 * @return mismatch details string
	 */
	public String getMismatchDetailsString() {
		return CigarStringUtil.getMismatchDetailsString(this);
	}

	/**
	 * @return the reverse of the mismatch details string
	 */
	public String getReverseMismatchDetailsString() {
		return CigarStringUtil.getMismatchDetailsString(getReferenceAlignmentWithoutTerminalInserts().getReverse(), getQueryAlignmentWithoutTerminalInserts().getReverse(), getReverseCigarString());
	}

	/**
	 * @return an alignment representing how the reverse of both the query sequence and reference sequence would align
	 */
	public AlignmentPair getReverse() {
		AlignmentPair newAlignmentPair = new AlignmentPair(referenceAlignment.getReverse(), queryAlignment.getReverse());
		return newAlignmentPair;
	}

	/**
	 * @return an alignment representing how the compliment of both the query sequence and reference sequence would align
	 */
	public AlignmentPair getCompliment() {
		AlignmentPair newAlignmentPair = new AlignmentPair(referenceAlignment.getCompliment(), queryAlignment.getCompliment());
		return newAlignmentPair;
	}

	/**
	 * @return an alignment representing how the reverse compliment of both the query sequence and reference sequence would align
	 */
	public AlignmentPair getReverseCompliment() {
		AlignmentPair newAlignmentPair = new AlignmentPair(referenceAlignment.getReverseCompliment(), queryAlignment.getReverseCompliment());
		return newAlignmentPair;
	}
}
