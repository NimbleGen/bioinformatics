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

package com.roche.sequencing.bioinformatics.common.stringsequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;

/**
 * 
 * Allows reference objects to be tied to a given sequence. Query sequence can then be used to query the mapping to find the most similar sequence.
 * 
 * @param <O>
 *            reference objects
 */
public class SimpleWordMapper<O> {

	private final static int DEFAULT_COMPARISON_SEQUENCE_SIZE = 3;
	private final static int DEFAULT_REFERENCE_SPACING = 1;
	private final static int DEFAULT_QUERY_SPACING = 1;
	private final static int DEFAULT_BEST_CANDIDATE_LIMIT = 10;
	private final int comparisonSequenceSize;
	private final int referenceSpacing;
	private final int querySpacing;
	private Set<O> addresses;

	private final Map<ArrayWrapper<ILetter>, Set<O>> sequenceSliceToReferenceAddressMap;

	/**
	 * Default Constructor
	 */
	public SimpleWordMapper() {
		this(DEFAULT_COMPARISON_SEQUENCE_SIZE, DEFAULT_REFERENCE_SPACING, DEFAULT_QUERY_SPACING);
	}

	/**
	 * Constructor
	 * 
	 * @param comparisonSequenceSize
	 *            the size of the chunks that should be used for comparing
	 * @param maxReferenceDepth
	 *            the number of spaces to skip when building a library of chunks to compare against
	 * @param querySpacing
	 *            the number of spaces to skip when comparing chunks from a query sequence
	 * @param minHitThreshold
	 *            min number of hits required to return as a best candidate reference
	 */
	public SimpleWordMapper(int comparisonSequenceSize, int referenceSpacing, int querySpacing) {
		this.comparisonSequenceSize = comparisonSequenceSize;
		this.referenceSpacing = referenceSpacing;
		this.querySpacing = querySpacing;
		sequenceSliceToReferenceAddressMap = new ConcurrentHashMap<ArrayWrapper<ILetter>, Set<O>>();
		addresses = new HashSet<O>();
	}

	/**
	 * Add a reference sequence with its associated unique identifier/key/sequence address
	 * 
	 * @param referenceSequence
	 * @param sequenceAddress
	 */
	public void addReferenceSequence(ILetter[] referenceSequence, O sequenceAddress) {
		if (referenceSequence.length >= comparisonSequenceSize) {
			for (int subsequenceStartIndex = 0; subsequenceStartIndex < referenceSequence.length - comparisonSequenceSize; subsequenceStartIndex += referenceSpacing) {
				addSliceToReferenceMap(Arrays.copyOfRange(referenceSequence, subsequenceStartIndex, subsequenceStartIndex + comparisonSequenceSize - 1), sequenceAddress);
			}
			addresses.add(sequenceAddress);
		} else {
			throw new IllegalStateException("comparison sequence size[" + comparisonSequenceSize + "] must be less than the size of all sequences -- the current sequence size is "
					+ referenceSequence.length + ".");
		}
	}

	/**
	 * Add a reference sequence with its associated unique identifier/key/sequence address
	 * 
	 * @param referenceSequence
	 * @param sequenceAddress
	 */
	public void removeReferenceSequenceByAddress(O sequenceAddress) {
		for (Entry<ArrayWrapper<ILetter>, Set<O>> entry : sequenceSliceToReferenceAddressMap.entrySet()) {
			Set<O> set = entry.getValue();
			if (set.contains(sequenceAddress)) {
				if (set.size() == 1) {
					sequenceSliceToReferenceAddressMap.remove(entry.getKey());
				} else {
					set.remove(sequenceAddress);
					sequenceSliceToReferenceAddressMap.put(entry.getKey(), set);
				}
			}
		}
		addresses.remove(sequenceAddress);
	}

	private void addSliceToReferenceMap(ILetter[] sequence, O sequenceAddress) {
		Set<O> sequenceAddresses = sequenceSliceToReferenceAddressMap.get(sequence);
		if (sequenceAddresses == null) {
			sequenceAddresses = new HashSet<O>();
		}
		sequenceAddresses.add(sequenceAddress);
		sequenceSliceToReferenceAddressMap.put(new ArrayWrapper<ILetter>(sequence), sequenceAddresses);
	}

	/**
	 * @param querySequence
	 * @return the set of unique identifiers/keys/sequence addresses that best map to the provided query sequence
	 */
	@SuppressWarnings("unchecked")
	public List<O> getBestCandidateReferences(ILetter[] querySequence, int limit) {
		TallyMap<O> matchTallies = getReferenceTallyMap(querySequence);

		int lastAddedSize = 0;

		List<O> bestCandidates = new LinkedList<O>();
		if (matchTallies.getLargestCount() > 0) {
			entryLoop: for (Entry<O, Integer> entry : matchTallies.getObjectsSortedFromMostTalliesToLeast()) {

				if (bestCandidates.size() >= limit && entry.getValue() < lastAddedSize) {
					break entryLoop;
				} else {
					bestCandidates.add(entry.getKey());
					lastAddedSize = entry.getValue();
				}

			}
		} else {
			bestCandidates = (List<O>) Collections.EMPTY_LIST;
		}

		return bestCandidates;
	}

	/**
	 * @param querySequence
	 * @return the set of unique identifiers/keys/sequence addresses that best map to the provided query sequence
	 */
	public List<O> getBestCandidateReferences(ILetter[] querySequence) {
		return getBestCandidateReferences(querySequence, DEFAULT_BEST_CANDIDATE_LIMIT);
	}

	public int getOptimalScore(ILetter[] querySequence) {
		return querySequence.length - comparisonSequenceSize;
	}

	/**
	 * @param querySequence
	 * @return the tallyMap associated with hits for this query sequence
	 */
	public TallyMap<O> getReferenceTallyMap(ILetter[] querySequence) {
		TallyMap<O> matchTallies = new TallyMap<O>();
		for (int i = 0; i < querySequence.length - comparisonSequenceSize; i += querySpacing) {
			ILetter[] querySequenceSlice = Arrays.copyOfRange(querySequence, i, i + comparisonSequenceSize - 1);
			Set<O> addresses = sequenceSliceToReferenceAddressMap.get(new ArrayWrapper<ILetter>(querySequenceSlice));
			matchTallies.addAll(addresses);
		}
		return matchTallies;
	}

	public int getNumberOfReferenceSequences() {
		return addresses.size();
	}

	public List<O> getReferenceSequences() {
		return new ArrayList<O>(addresses);
	}

	private static class ArrayWrapper<O> {
		private final O[] wrappedArray;

		public ArrayWrapper(O[] wrappedArray) {
			super();
			this.wrappedArray = wrappedArray;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(wrappedArray);
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
			ArrayWrapper<?> other = (ArrayWrapper<?>) obj;
			if (!Arrays.equals(wrappedArray, other.wrappedArray))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return ArraysUtil.toString(wrappedArray, ", ");
		}

	}
}
