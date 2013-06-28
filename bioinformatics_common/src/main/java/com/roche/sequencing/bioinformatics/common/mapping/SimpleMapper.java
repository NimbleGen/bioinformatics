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

package com.roche.sequencing.bioinformatics.common.mapping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;

/**
 * 
 * Allows reference objects to be tied to a given sequence. Query sequence can then be used to query the mapping to find the most similar sequence.
 * 
 * @param <O>
 *            reference objects
 */
public class SimpleMapper<O> {

	private final static int DEFAULT_COMPARISON_SEQUENCE_SIZE = 10;
	private final static int DEFAULT_MAX_REFERENCE_DEPTH = 10;
	private final static int DEFAULT_MAX_QUERY_DEPTH = 1;
	private final static int DEFAULT_MIN_HIT_THRESHOLD = 3;
	private final static int MAX_NUMBER_OF_REFERENCES_ALLOWED_PER_SEQUENCE = 1000;

	private final int comparisonSequenceSize;
	private final int maxReferenceDepth;
	private final int maxQueryDepth;
	private final int minHitThreshold;

	private final Map<ISequence, Set<O>> sequenceSliceToReferenceAddressMap;
	private final Set<ISequence> overSaturatedSequences;

	/**
	 * Default Constructor
	 */
	public SimpleMapper() {
		this(DEFAULT_COMPARISON_SEQUENCE_SIZE, DEFAULT_MAX_REFERENCE_DEPTH, DEFAULT_MAX_QUERY_DEPTH, DEFAULT_MIN_HIT_THRESHOLD);
	}

	/**
	 * Constructor
	 * 
	 * @param comparisonSequenceSize
	 * @param maxReferenceDepth
	 * @param maxQueryDepth
	 * @param minHitThreshold
	 */
	public SimpleMapper(int comparisonSequenceSize, int maxReferenceDepth, int maxQueryDepth, int minHitThreshold) {
		if (maxQueryDepth != 1 && maxReferenceDepth != 1) {
			throw new IllegalStateException("Either the max reference depth[" + maxReferenceDepth + "] or the max query depth[" + maxQueryDepth
					+ "] must equal 1 to ensure that reference sequences match with query sequence.");
		}
		this.comparisonSequenceSize = comparisonSequenceSize;
		this.maxReferenceDepth = maxReferenceDepth;
		this.maxQueryDepth = maxQueryDepth;
		this.minHitThreshold = minHitThreshold;
		sequenceSliceToReferenceAddressMap = new ConcurrentHashMap<ISequence, Set<O>>();
		overSaturatedSequences = new ConcurrentSkipListSet<ISequence>();
	}

	/**
	 * Add a reference sequence with its associated unique identifier/key/sequence address
	 * 
	 * @param referenceSequence
	 * @param sequenceAddress
	 */
	public void addReferenceSequence(ISequence referenceSequence, O sequenceAddress) {
		if (referenceSequence.size() >= comparisonSequenceSize) {
			int sliceSpacing = (int) Math.ceil((double) comparisonSequenceSize / (double) maxReferenceDepth);
			for (int subsequenceStartIndex = 0; subsequenceStartIndex < referenceSequence.size() - comparisonSequenceSize; subsequenceStartIndex += sliceSpacing) {
				addSliceToReferenceMap(referenceSequence.subSequence(subsequenceStartIndex, subsequenceStartIndex + comparisonSequenceSize), sequenceAddress);
			}
		} else {
			throw new IllegalStateException("comparison sequence size[" + comparisonSequenceSize + "] must be less than the size of all sequences -- the current sequence size is "
					+ referenceSequence.size() + ".");
		}
	}

	private void addSliceToReferenceMap(ISequence sequence, O sequenceAddress) {
		if (!overSaturatedSequences.contains(sequence)) {
			Set<O> sequenceAddresses = sequenceSliceToReferenceAddressMap.get(sequence);
			if (sequenceAddresses == null) {
				sequenceAddresses = new HashSet<O>();
			}
			sequenceAddresses.add(sequenceAddress);
			if (sequenceAddresses.size() > MAX_NUMBER_OF_REFERENCES_ALLOWED_PER_SEQUENCE) {
				overSaturatedSequences.add(sequence);
				sequenceAddresses.clear();
			}
			sequenceSliceToReferenceAddressMap.put(sequence, sequenceAddresses);
		}
	}

	/**
	 * @param querySequence
	 * @return the set of unique identifiers/keys/sequence addresses that best map to the provided query sequence
	 */
	@SuppressWarnings("unchecked")
	public Set<O> getBestCandidateReferences(ISequence querySequence) {
		TallyMap<O> matchTallies = new TallyMap<O>();
		int sliceSpacing = (int) Math.ceil(((double) comparisonSequenceSize / (double) maxQueryDepth));
		for (int i = 0; i < querySequence.size() - comparisonSequenceSize; i += sliceSpacing) {
			ISequence querySequenceSlice = querySequence.subSequence(i, i + comparisonSequenceSize);
			matchTallies.addAll(sequenceSliceToReferenceAddressMap.get(querySequenceSlice));
		}
		Set<O> bestCandidates = null;
		if (matchTallies.getLargestCount() >= minHitThreshold) {
			bestCandidates = matchTallies.getObjectsWithLargestCount();
		} else {
			bestCandidates = (Set<O>) Collections.EMPTY_SET;
		}
		return bestCandidates;

	}

}
