package com.roche.sequencing.bioinformatics.common.sequence;

import java.util.LinkedHashSet;
import java.util.Set;

public class SequenceUtil {

	private SequenceUtil() {
		throw new AssertionError();
	}

	public static boolean matches(ISequence referenceSequence, ISequence querySequence) {
		return match(referenceSequence, querySequence, 4);
	}

	public static boolean match(ISequence referenceString, ISequence queryString, int allowedMismatches) {
		boolean match = false;

		int startingReferenceIndex = 0;

		while (!match && startingReferenceIndex + queryString.size() <= referenceString.size()) {
			int queryIndex = 0;
			int mismatches = 0;
			boolean tooManyMisMatches = false;
			while (queryIndex < queryString.size() && !tooManyMisMatches) {
				if (queryString.getCodeAt(queryIndex) != referenceString.getCodeAt(startingReferenceIndex + queryIndex)) {
					mismatches++;
				}
				queryIndex++;
				tooManyMisMatches = (mismatches > allowedMismatches);
			}
			match = !tooManyMisMatches;
			startingReferenceIndex++;
		}
		return match;
	}

	public static Set<StartAndStopIndex> findAllMatches(ISequence referenceString, ISequence queryString, int allowedMismatches) {

		Set<StartAndStopIndex> matches = new LinkedHashSet<StartAndStopIndex>();

		int startingReferenceIndex = 0;

		while ((startingReferenceIndex + queryString.size()) <= referenceString.size()) {
			int queryIndex = 0;
			int mismatches = 0;
			boolean tooManyMisMatches = false;
			while (queryIndex < queryString.size() && !tooManyMisMatches) {
				if (queryString.getCodeAt(queryIndex) != referenceString.getCodeAt(startingReferenceIndex + queryIndex)) {
					mismatches++;
				}
				queryIndex++;
				tooManyMisMatches = (mismatches > allowedMismatches);
			}
			if (!tooManyMisMatches) {
				matches.add(new StartAndStopIndex(startingReferenceIndex + 1, startingReferenceIndex + queryString.size()));
			}
			startingReferenceIndex++;
		}
		return matches;
	}

	public static void main(String[] args) {
		ISequence a = new IupacNucleotideCodeSequence(
				"TCTTTCTGGTTGACCATCAAATATTCCTTCTCTGTTGTCATCAGAAGATAACGCTGATGATGAGGTGGACACACGACCAGCCTCCTTCTGGGAGACATCATAGTGCTAGTACTATGTCAAAGCAACAGTCCACACTTTGTCCAATGGTTTT");
		System.out.println(a.getReverseCompliment());
		ISequence b = new IupacNucleotideCodeSequence("AC");
		Set<StartAndStopIndex> matches = findAllMatches(a, b, 2);
		for (StartAndStopIndex ss : matches) {
			System.out.println(ss);
		}

	}

}
