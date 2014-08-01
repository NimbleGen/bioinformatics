package com.roche.sequencing.bioinformatics.common.mapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.NucleotideCode;
import com.roche.sequencing.bioinformatics.common.sequence.NucleotideCodeSequence;

public class SimpleNMerProfiler<O> {

	private final int merSize;
	private final Map<ISequence, Integer> permutationToIndexMap;
	private final List<ProfileAndObjectPair> references;

	public SimpleNMerProfiler(int merSize) {
		super();
		this.merSize = merSize;
		permutationToIndexMap = getNMerSequencePermutationToIndexMap(merSize);
		references = new ArrayList<ProfileAndObjectPair>();
	}

	private class ProfileAndObjectPair {
		private final O object;
		private final double[] profile;

		public ProfileAndObjectPair(O object, double[] profile) {
			super();
			this.object = object;
			this.profile = profile;
		}

		public O getObject() {
			return object;
		}

		public double[] getProfile() {
			return profile;
		}
	}

	private class DistanceAndObjectPair {
		private final O object;
		private final double distance;

		public DistanceAndObjectPair(O object, double distance) {
			super();
			this.object = object;
			this.distance = distance;
		}

		public O getObject() {
			return object;
		}

		public double getDistance() {
			return distance;
		}
	}

	/**
	 * Add a reference sequence with its associated unique identifier/key/sequence address
	 * 
	 * @param referenceSequence
	 * @param sequenceAddress
	 */
	public void addReferenceSequence(ISequence referenceSequence, O sequenceAddress) {
		double[] profile = getNMerProfile(merSize, permutationToIndexMap, referenceSequence);
		references.add(new ProfileAndObjectPair(sequenceAddress, profile));
	}

	/**
	 * @param querySequence
	 * @return the set of unique identifiers/keys/sequence addresses that best map to the provided query sequence
	 */
	public Set<O> getBestCandidateReferences(ISequence querySequence, int limit) {
		SortedSet<DistanceAndObjectPair> sortedCandidates = new TreeSet<DistanceAndObjectPair>(new Comparator<DistanceAndObjectPair>() {

			@Override
			public int compare(DistanceAndObjectPair o1, DistanceAndObjectPair o2) {
				return Double.compare(o1.getDistance(), o2.getDistance());
			}
		});

		double[] profile = getNMerProfile(merSize, permutationToIndexMap, querySequence);
		for (ProfileAndObjectPair pair : references) {
			double[] referenceProfile = pair.getProfile();
			double distance = distance(referenceProfile, profile);
			sortedCandidates.add(new DistanceAndObjectPair(pair.getObject(), distance));
		}

		Set<O> bestCandidates = new LinkedHashSet<O>();

		Iterator<DistanceAndObjectPair> iter = sortedCandidates.iterator();
		int i = 0;
		while (iter.hasNext() && i < limit) {
			bestCandidates.add(iter.next().getObject());
			i++;
		}

		return bestCandidates;
	}

	private static double distance(double[] a, double[] b) {
		double sumOfSquares = 0;
		for (int i = 0; i < a.length; i++) {
			double difference = a[i] - b[i];
			sumOfSquares += difference * difference;
		}
		return Math.sqrt(sumOfSquares);
	}

	private static LinkedHashSet<ISequence> generateAllNMerPermutations(int n) {
		LinkedHashSet<ISequence> permutations = new LinkedHashSet<ISequence>();

		for (int i = 0; i < n; i++) {
			LinkedHashSet<ISequence> permutationsAfterPreviousRound = permutations;
			permutations = new LinkedHashSet<ISequence>();

			for (NucleotideCode code : NucleotideCode.values()) {
				if (permutationsAfterPreviousRound.size() == 0) {
					permutations.add(new NucleotideCodeSequence(code.toString()));
				} else {
					for (ISequence sequence : permutationsAfterPreviousRound) {
						permutations.add(new NucleotideCodeSequence(code.toString() + sequence.toString()));
					}
				}
			}
		}
		return permutations;
	}

	private static Map<ISequence, Integer> getNMerSequencePermutationToIndexMap(int n) {
		LinkedHashSet<ISequence> nMerPermutations = generateAllNMerPermutations(n);
		Map<ISequence, Integer> permutationToIndexMap = new HashMap<ISequence, Integer>();
		int i = 0;
		for (ISequence sequence : nMerPermutations) {
			permutationToIndexMap.put(sequence, i);
			i++;
		}
		return permutationToIndexMap;
	}

	@SuppressWarnings("unused")
	private static double[] getNMerProfile(int n, ISequence sequence) {
		Map<ISequence, Integer> permutationToIndexMap = getNMerSequencePermutationToIndexMap(n);
		return getNMerProfile(n, permutationToIndexMap, sequence);
	}

	private static double[] getNMerProfile(int n, Map<ISequence, Integer> permutationToIndexMap, ISequence sequence) {
		double[] profile = new double[permutationToIndexMap.size()];

		for (int startIndex = 0; startIndex <= sequence.size() - n; startIndex++) {
			ISequence mer = new NucleotideCodeSequence(sequence.subSequence(startIndex, startIndex + n - 1).toString());
			Integer index = permutationToIndexMap.get(mer);
			if (index != null) {
				profile[index]++;
			}
		}

		return profile;
	}

	public static void main(String[] args) {

	}
}
