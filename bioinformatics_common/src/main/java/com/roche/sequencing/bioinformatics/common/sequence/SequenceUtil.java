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
package com.roche.sequencing.bioinformatics.common.sequence;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class SequenceUtil {

	private SequenceUtil() {
		throw new AssertionError();
	}

	public static boolean matches(ISequence referenceSequence, ISequence querySequence) {
		return match(referenceSequence, querySequence, 0);
	}

	private static boolean match(ISequence referenceString, ISequence queryString, int allowedMismatches) {
		boolean match = false;

		int startingReferenceIndex = 0;

		while (!match && startingReferenceIndex + queryString.size() <= referenceString.size()) {
			int queryIndex = 0;
			int mismatches = 0;
			boolean tooManyMisMatches = false;
			while (queryIndex < queryString.size() && !tooManyMisMatches) {
				ICode queryCode = queryString.getCodeAt(queryIndex);
				ICode referenceCode = referenceString.getCodeAt(startingReferenceIndex + queryIndex);
				if (!queryCode.matches(referenceCode)) {
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

	public static Set<StartAndStopIndex> findMatches(ISequence referenceString, ISequence queryString, int allowedMismatches, int maxNumberOfMatches) {
		Set<StartAndStopIndex> matches = new LinkedHashSet<StartAndStopIndex>();

		int startingReferenceIndex = 0;

		startingIndexLoop: while ((startingReferenceIndex + queryString.size()) <= referenceString.size()) {
			int queryIndex = 0;
			int mismatches = 0;
			boolean tooManyMisMatches = false;
			while (queryIndex < queryString.size() && !tooManyMisMatches) {
				if (!queryString.getCodeAt(queryIndex).matches(referenceString.getCodeAt(startingReferenceIndex + queryIndex))) {
					mismatches++;
				}
				queryIndex++;
				tooManyMisMatches = (mismatches > allowedMismatches);
			}
			if (!tooManyMisMatches) {
				matches.add(new StartAndStopIndex(startingReferenceIndex + 1, startingReferenceIndex + queryString.size()));
				if (matches.size() >= maxNumberOfMatches) {
					break startingIndexLoop;
				}
			}
			startingReferenceIndex++;
		}
		return matches;
	}

	public static Set<StartAndStopIndex> findAllMatches(ISequence referenceString, ISequence queryString, int allowedMismatches) {
		return findMatches(referenceString, queryString, allowedMismatches, Integer.MAX_VALUE);
	}

	public static StartAndStopIndex findFirstMatch(ISequence referenceString, ISequence queryString, int allowedMismatches) {
		Set<StartAndStopIndex> matches = findMatches(referenceString, queryString, allowedMismatches, 1);
		StartAndStopIndex match;
		if (matches.size() > 0) {
			match = matches.iterator().next();
		} else {
			match = null;
		}
		return match;
	}

	public static double getGCPercent(ISequence sequence) {
		double totalGCs = 0;
		for (ICode code : sequence) {
			if (code.matches(NucleotideCode.GUANINE) || code.matches(NucleotideCode.CYTOSINE)) {
				totalGCs++;
			}
		}
		double gcPercent = totalGCs / sequence.size() * 100;
		return gcPercent;
	}

	public static Map<ICode, Integer> getNucloetideCounts(ISequence sequence) {
		int As = 0;
		int Gs = 0;
		int Cs = 0;
		int Ts = 0;
		for (ICode code : sequence) {
			if (code.matches(NucleotideCode.ADENINE)) {
				As++;
			} else if (code.matches(NucleotideCode.CYTOSINE)) {
				Cs++;
			} else if (code.matches(NucleotideCode.GUANINE)) {
				Gs++;
			} else if (code.matches(NucleotideCode.THYMINE)) {
				Ts++;
			}
		}
		Map<ICode, Integer> ntCounts = new HashMap<ICode, Integer>();
		ntCounts.put(NucleotideCode.ADENINE, As);
		ntCounts.put(NucleotideCode.CYTOSINE, Cs);
		ntCounts.put(NucleotideCode.GUANINE, Gs);
		ntCounts.put(NucleotideCode.THYMINE, Ts);
		return ntCounts;
	}

	/**
	 * Marmur,J., and Doty,P. (1962) J Mol Biol 5:109-118 [PubMed]
	 * 
	 * @param sequence
	 * @return
	 */
	public static double getMeltingTemperatureInCelsiusUsingDotyMethod(ISequence sequence) {
		Map<ICode, Integer> ntCount = getNucloetideCounts(sequence);
		double meltingTemperature = 2 * (ntCount.get(NucleotideCode.ADENINE) + ntCount.get(NucleotideCode.THYMINE)) + 4 * (ntCount.get(NucleotideCode.GUANINE) + ntCount.get(NucleotideCode.CYTOSINE));
		return meltingTemperature;
	}

	/**
	 * Wallace,R.B., Shaffer,J., Murphy,R.F., Bonner,J., Hirose,T., and Itakura,K. (1979) Nucleic Acids Res 6:3543-3557 (Abstract) and Sambrook,J., and Russell,D.W. (2001) Molecular Cloning: A
	 * Laboratory Manual. Cold Spring Harbor Laboratory Press; Cold Spring Harbor, NY. (CHSL Press)
	 * 
	 * @param sequence
	 * @return
	 */
	public static double getMeltingTemperatureInCelsiusBasedOnGC(ISequence sequence) {
		Map<ICode, Integer> ntCount = getNucloetideCounts(sequence);
		int yG = ntCount.get(NucleotideCode.GUANINE);
		int zC = ntCount.get(NucleotideCode.CYTOSINE);
		int wA = ntCount.get(NucleotideCode.ADENINE);
		int xT = ntCount.get(NucleotideCode.THYMINE);
		// Tm= 64.9 +41*(yG+zC-16.4)/(wA+xT+yG+zC)
		double meltingTemperature = 64.9 + (41 * (yG + zC - 16.4) / (wA + xT + yG + zC));
		return meltingTemperature;
	}

	//
	public static double getMeltingTemperatureUsingNearestNeighborThermodynamics(ISequence sequence) {
		return NN_TM_Calculator.getTm(sequence);
	}

	public static double getMeltingTemperatureUsingNearestNeighborThermodynamics(ISequence sequence, NN_TM_ParametersSourceEnum paramterSource) {
		return NN_TM_Calculator.getTm(sequence, paramterSource);
	}

	public static void main(String[] args) {
		ISequence a = new IupacNucleotideCodeSequence("CGGAACATCCTGGGGGGGACTGTCTTCCGCCCACTGTCTGGCACTCCCTAGTGAGATGAACCCGGTACCTCAGATG");
		System.out.println(a);
		System.out.println(a.getReverseCompliment());
		// System.out.println("NA java:" + NucleicAcid.calcDefaultNearestNeighborTm(a.toString()));

	}

}
