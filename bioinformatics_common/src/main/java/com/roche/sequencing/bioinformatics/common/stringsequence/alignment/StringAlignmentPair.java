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

package com.roche.sequencing.bioinformatics.common.stringsequence.alignment;

import java.util.ArrayList;
import java.util.List;

import com.roche.sequencing.bioinformatics.common.stringsequence.ILetter;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * 
 * A pair of sequences representing their alignment relative to each other.
 * 
 */
public class StringAlignmentPair {
	private final List<ILetter> referenceAlignment;
	private final List<ILetter> queryAlignment;

	private int firstNonDeletionIndexInQuery;
	private int lastNonDeletionIndexInQuery;

	private int firstNonDeletionIndexInReference;
	private int lastNonDeletionIndexInReference;

	StringAlignmentPair(List<ILetter> referenceAlignment, List<ILetter> queryAlignment) {
		super();
		this.referenceAlignment = referenceAlignment;
		this.queryAlignment = queryAlignment;

		firstNonDeletionIndexInQuery = 0;
		while ((firstNonDeletionIndexInQuery < queryAlignment.size()) && (queryAlignment.get(firstNonDeletionIndexInQuery) == Gap.GAP)) {
			firstNonDeletionIndexInQuery++;
		}

		lastNonDeletionIndexInQuery = queryAlignment.size() - 1;
		while ((lastNonDeletionIndexInQuery >= 0) && (queryAlignment.get(lastNonDeletionIndexInQuery) == Gap.GAP)) {
			lastNonDeletionIndexInQuery--;
		}

		firstNonDeletionIndexInReference = 0;
		while ((firstNonDeletionIndexInReference < referenceAlignment.size()) && (referenceAlignment.get(firstNonDeletionIndexInReference) == Gap.GAP)) {
			firstNonDeletionIndexInReference++;
		}

		lastNonDeletionIndexInReference = queryAlignment.size() - 1;
		while ((lastNonDeletionIndexInReference >= 0) && (referenceAlignment.get(lastNonDeletionIndexInReference) == Gap.GAP)) {
			lastNonDeletionIndexInReference--;
		}

	}

	public List<ILetter> getMergedAlignment() {
		List<ILetter> mergedAlignment = new ArrayList<ILetter>();

		for (int i = 0; i < referenceAlignment.size(); i++) {
			ILetter reference = referenceAlignment.get(i);
			ILetter query = queryAlignment.get(i);
			if (reference.equals(query)) {
				mergedAlignment.add(query);
			} else if (reference.equals(Gap.GAP)) {
				mergedAlignment.add(query);
			} else if (query.equals(Gap.GAP)) {
				mergedAlignment.add(reference);
			} else {
				throw new AssertionError();
			}
		}

		return mergedAlignment;
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
	public List<ILetter> getReferenceAlignment() {
		return referenceAlignment;
	}

	/**
	 * @return the query sequence alignment
	 */
	public List<ILetter> getQueryAlignment() {
		return queryAlignment;
	}

	/**
	 * @return the reference sequence alignment with no beginning or ending gaps
	 */
	public StringAlignmentPair getAlignmentWithoutEndingAndBeginningQueryInserts() {
		return new StringAlignmentPair(referenceAlignment.subList(firstNonDeletionIndexInQuery, lastNonDeletionIndexInQuery), queryAlignment.subList(firstNonDeletionIndexInQuery,
				lastNonDeletionIndexInQuery));
	}

	/**
	 * @return the reference sequence alignment with no beginning or ending gaps
	 */
	public StringAlignmentPair getAlignmentWithoutEndingReferenceInserts() {
		return new StringAlignmentPair(referenceAlignment.subList(0, lastNonDeletionIndexInReference), queryAlignment.subList(0, lastNonDeletionIndexInReference));
	}

	/**
	 * @return the reference sequence alignment with no beginning or ending gaps
	 */
	public StringAlignmentPair getAlignmentWithoutEndingAndBeginningReferenceInserts() {
		return new StringAlignmentPair(referenceAlignment.subList(firstNonDeletionIndexInQuery, lastNonDeletionIndexInReference), queryAlignment.subList(firstNonDeletionIndexInReference,
				lastNonDeletionIndexInReference));
	}

	/**
	 * @return the reference sequence alignment with no beginning or ending gaps
	 */
	public StringAlignmentPair getAlignmentWithoutEndingQueryInserts() {
		return new StringAlignmentPair(referenceAlignment.subList(0, lastNonDeletionIndexInQuery), queryAlignment.subList(0, lastNonDeletionIndexInQuery));
	}

	public int getFirstNonInsertQueryMatchInReference() {
		return firstNonDeletionIndexInQuery;
	}

}
