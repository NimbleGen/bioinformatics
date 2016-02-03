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

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCode;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * 
 * A pair of sequences representing their alignment relative to each other.
 * 
 */
public class AlignmentPair {
	private final ISequence referenceAlignment;
	private final ISequence queryAlignment;

	private int firstNonDeletionIndexInQuery;
	private int lastNonDeletionIndexInQuery;

	private int firstNonDeletionIndexInReference;
	private int lastNonDeletionIndexInReference;

	AlignmentPair(ISequence referenceAlignment, ISequence queryAlignment) {
		super();
		this.referenceAlignment = referenceAlignment;
		this.queryAlignment = queryAlignment;

		firstNonDeletionIndexInQuery = 0;
		while ((firstNonDeletionIndexInQuery < queryAlignment.size()) && (queryAlignment.getCodeAt(firstNonDeletionIndexInQuery) == IupacNucleotideCode.GAP)) {
			firstNonDeletionIndexInQuery++;
		}

		lastNonDeletionIndexInQuery = queryAlignment.size() - 1;
		while ((lastNonDeletionIndexInQuery >= 0) && (queryAlignment.getCodeAt(lastNonDeletionIndexInQuery) == IupacNucleotideCode.GAP)) {
			lastNonDeletionIndexInQuery--;
		}

		firstNonDeletionIndexInReference = 0;
		while ((firstNonDeletionIndexInReference < referenceAlignment.size()) && (referenceAlignment.getCodeAt(firstNonDeletionIndexInReference) == IupacNucleotideCode.GAP)) {
			firstNonDeletionIndexInReference++;
		}

		lastNonDeletionIndexInReference = queryAlignment.size() - 1;
		while ((lastNonDeletionIndexInReference >= 0) && (referenceAlignment.getCodeAt(lastNonDeletionIndexInReference) == IupacNucleotideCode.GAP)) {
			lastNonDeletionIndexInReference--;
		}

	}

	/**
	 * @return a string representation of this alignment
	 */
	public String getAlignmentAsString() {
		StringBuilder alignmentAsStringBuilder = new StringBuilder();

		alignmentAsStringBuilder.append(getReferenceAlignment().toString() + StringUtil.NEWLINE);
		alignmentAsStringBuilder.append(getQueryAlignment().toString() + StringUtil.NEWLINE);

		return alignmentAsStringBuilder.toString();
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
	public AlignmentPair getAlignmentWithoutEndingAndBeginningQueryInserts() {
		return new AlignmentPair(referenceAlignment.subSequence(firstNonDeletionIndexInQuery, lastNonDeletionIndexInQuery), queryAlignment.subSequence(firstNonDeletionIndexInQuery,
				lastNonDeletionIndexInQuery));
	}

	/**
	 * @return the reference sequence alignment with no beginning or ending gaps
	 */
	public AlignmentPair getAlignmentWithoutEndingReferenceInserts() {
		return new AlignmentPair(referenceAlignment.subSequence(0, lastNonDeletionIndexInReference), queryAlignment.subSequence(0, lastNonDeletionIndexInReference));
	}

	/**
	 * @return the reference sequence alignment with no beginning or ending gaps
	 */
	public AlignmentPair getAlignmentWithoutEndingAndBeginningReferenceInserts() {
		return new AlignmentPair(referenceAlignment.subSequence(firstNonDeletionIndexInQuery, lastNonDeletionIndexInReference), queryAlignment.subSequence(firstNonDeletionIndexInReference,
				lastNonDeletionIndexInReference));
	}

	/**
	 * @return the reference sequence alignment with no beginning or ending gaps
	 */
	public AlignmentPair getAlignmentWithoutEndingQueryInserts() {
		return new AlignmentPair(referenceAlignment.subSequence(0, lastNonDeletionIndexInQuery), queryAlignment.subSequence(0, lastNonDeletionIndexInQuery));
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
	 * @return mismatch details string
	 */
	public String getMismatchDetailsString() {
		return CigarStringUtil.getMismatchDetailsString(this);
	}

	/**
	 * @return the reverse of the mismatch details string
	 */
	public String getReverseMismatchDetailsString() {
		AlignmentPair alignmentWithoutEndingAndBeginningQueryInserts = getAlignmentWithoutEndingAndBeginningQueryInserts();
		ISequence referenceSequenceAlignment = alignmentWithoutEndingAndBeginningQueryInserts.getReferenceAlignment();
		ISequence querySequenceAlignment = alignmentWithoutEndingAndBeginningQueryInserts.getQueryAlignment();

		return CigarStringUtil.getMismatchDetailsString(referenceSequenceAlignment.getReverse(), querySequenceAlignment.getReverse(), getReverseCigarString());
	}

	public int getFirstNonInsertQueryMatchInReference() {
		return firstNonDeletionIndexInQuery;
	}

}
