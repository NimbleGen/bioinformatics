package com.roche.sequencing.bioinformatics.common.alignment;

import com.roche.sequencing.bioinformatics.common.sequence.ICode;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCode;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class MultipleSequenceAlignmentUtil {

	private static IAlignmentScorer alignmentScorer = new SimpleAlignmentScorer(1, -4, -6, -8, true, true);

	public static String[] align(IupacNucleotideCodeSequence referenceSequence, IupacNucleotideCodeSequence[] querySequences) {

		String[] results = null;

		if (querySequences.length > 0) {
			IupacNucleotideCodeSequence[] alignedReferenceSequences = new IupacNucleotideCodeSequence[querySequences.length];
			IupacNucleotideCodeSequence[] alignedQuerySequences = new IupacNucleotideCodeSequence[querySequences.length];
			for (int i = 0; i < querySequences.length; i++) {
				NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(referenceSequence, querySequences[i], alignmentScorer);
				alignedReferenceSequences[i] = (IupacNucleotideCodeSequence) alignment.getAlignmentPair().getReferenceAlignment();
				alignedQuerySequences[i] = (IupacNucleotideCodeSequence) alignment.getAlignmentPair().getQueryAlignment();
			}

			IupacNucleotideCodeSequence combinedAlignedReferenceSequence = alignedReferenceSequences[0];

			IupacNucleotideCodeSequence[] currentQueries = alignedQuerySequences;
			IupacNucleotideCodeSequence[] currentReferences = alignedReferenceSequences;

			for (int referenceIndexToCombine = 1; referenceIndexToCombine < querySequences.length; referenceIndexToCombine++) {
				// at this point any queries and references before referenceIndexToCombine should have the same
				// length as combinedAlignedReferenceSequence. The index that walks through these values
				// is combinedIndexInSequence
				for (int i = 0; i < referenceIndexToCombine; i++) {
					int expectedSize = combinedAlignedReferenceSequence.size();
					if (currentQueries[i].size() != expectedSize) {
						throw new IllegalStateException("query size actual[" + currentQueries[i].size() + "] expected[" + expectedSize + "]");
					}

					if (currentReferences[i].size() != expectedSize) {
						throw new IllegalStateException("reference size");
					}
				}

				IupacNucleotideCodeSequence newCombinedReference = new IupacNucleotideCodeSequence();
				IupacNucleotideCodeSequence currentReferenceBeingCombined = currentReferences[referenceIndexToCombine];

				IupacNucleotideCodeSequence[] newQueries = new IupacNucleotideCodeSequence[querySequences.length];
				IupacNucleotideCodeSequence[] newReferences = new IupacNucleotideCodeSequence[querySequences.length];
				for (int queryIndex = 0; queryIndex < querySequences.length; queryIndex++) {
					if (queryIndex <= referenceIndexToCombine) {
						newQueries[queryIndex] = new IupacNucleotideCodeSequence();
						newReferences[queryIndex] = new IupacNucleotideCodeSequence();
					} else {
						newQueries[queryIndex] = currentQueries[queryIndex];
						newReferences[queryIndex] = currentReferences[queryIndex];
					}

				}

				int sequenceIndexForCombinedSequences = 0;
				int currentReferenceIndexInSequence = 0;
				while (sequenceIndexForCombinedSequences < combinedAlignedReferenceSequence.size() || currentReferenceIndexInSequence < currentReferenceBeingCombined.size()) {
					ICode combinedCode = null;
					if (sequenceIndexForCombinedSequences < combinedAlignedReferenceSequence.size()) {
						combinedCode = combinedAlignedReferenceSequence.getCodeAt(sequenceIndexForCombinedSequences);
					}

					ICode referenceCode = null;
					if (currentReferenceIndexInSequence < currentReferenceBeingCombined.size()) {
						referenceCode = currentReferenceBeingCombined.getCodeAt(currentReferenceIndexInSequence);
					}
					if ((combinedCode != null && combinedCode.equals(IupacNucleotideCode.GAP)) && (referenceCode == null || !referenceCode.equals(IupacNucleotideCode.GAP))) {
						// the new reference and the associated query needs to have a gap added
						for (int queryIndex = 0; queryIndex < referenceIndexToCombine; queryIndex++) {
							newQueries[queryIndex].append(currentQueries[queryIndex].getCodeAt(sequenceIndexForCombinedSequences));
							newReferences[queryIndex].append(currentReferences[queryIndex].getCodeAt(sequenceIndexForCombinedSequences));
						}
						newQueries[referenceIndexToCombine].append(IupacNucleotideCode.GAP);
						newReferences[referenceIndexToCombine].append(IupacNucleotideCode.GAP);
						newCombinedReference.append(IupacNucleotideCode.GAP);
						sequenceIndexForCombinedSequences++;
					} else if ((referenceCode != null && referenceCode.equals(IupacNucleotideCode.GAP)) && (combinedCode == null || !combinedCode.equals(IupacNucleotideCode.GAP))) {
						// the combined reference and all queries associated with the combined reference needs to have a gap added
						newCombinedReference.append(IupacNucleotideCode.GAP);
						for (int queryIndex = 0; queryIndex < referenceIndexToCombine; queryIndex++) {
							newQueries[queryIndex].append(IupacNucleotideCode.GAP);
							newReferences[queryIndex].append(IupacNucleotideCode.GAP);
						}
						newQueries[referenceIndexToCombine].append(currentQueries[referenceIndexToCombine].getCodeAt(currentReferenceIndexInSequence));
						newReferences[referenceIndexToCombine].append(currentReferences[referenceIndexToCombine].getCodeAt(currentReferenceIndexInSequence));
						currentReferenceIndexInSequence++;
					} else {
						// match or mismatch -- no change to reference or queries
						newCombinedReference.append(combinedCode);
						for (int queryIndex = 0; queryIndex < referenceIndexToCombine; queryIndex++) {
							newQueries[queryIndex].append(currentQueries[queryIndex].getCodeAt(sequenceIndexForCombinedSequences));
							newReferences[queryIndex].append(currentReferences[queryIndex].getCodeAt(sequenceIndexForCombinedSequences));
						}
						newQueries[referenceIndexToCombine].append(currentQueries[referenceIndexToCombine].getCodeAt(currentReferenceIndexInSequence));
						newReferences[referenceIndexToCombine].append(currentReferences[referenceIndexToCombine].getCodeAt(currentReferenceIndexInSequence));

						sequenceIndexForCombinedSequences++;
						currentReferenceIndexInSequence++;
					}
				}

				combinedAlignedReferenceSequence = newCombinedReference;

				currentQueries = newQueries;
				currentReferences = newReferences;
			}

			results = new String[currentQueries.length + 1];
			results[0] = combinedAlignedReferenceSequence.toString();
			for (int i = 0; i < currentQueries.length; i++) {
				StringBuilder alignment = new StringBuilder();
				IupacNucleotideCodeSequence currentQuery = currentQueries[i];
				for (int queryIndex = 0; queryIndex < currentQuery.size(); queryIndex++) {
					ICode code = currentQuery.getCodeAt(queryIndex);
					if (code.equals(combinedAlignedReferenceSequence.getCodeAt(queryIndex))) {
						alignment.append(code.toString());
					} else {
						alignment.append(code.toString().toLowerCase());
					}
				}
				int padding = combinedAlignedReferenceSequence.size() - currentQuery.size();
				alignment.append(StringUtil.repeatString("_", padding));
				results[i + 1] = alignment.toString();
			}
		} else {
			results = new String[] { referenceSequence.toString() };
		}
		return results;
	}

	public static void main(String[] args) {
		IupacNucleotideCodeSequence refSeq = new IupacNucleotideCodeSequence(
				"ACAAGTGAGAGACAGGATCAGGTCAGCGGGCTACCACTGGGCCTCACCTCTATGGTGGGATCATATTCATCTACAAAGTGGTTCTGGATTAGCTGGATTGTCAGTGCGCTTTTCCCAACACC");
		IupacNucleotideCodeSequence[] querySeqs = new IupacNucleotideCodeSequence[4];
		querySeqs[0] = new IupacNucleotideCodeSequence("acaagtgagagacaggatcaggtgagcgggctaagaatggggctcacagggaaggggggagaatagtaatctacaaagtggatgaggattagatggattgtaagtgagaggagaagaaaagg");
		querySeqs[1] = new IupacNucleotideCodeSequence("ACAAGTGAGAGACAGGATCAGGTCAGCGGGCTACCACTGGGCCTCACCTCTATGGTGGGATCATATTCATCTACAAAGTGGTTCTGGATTAGCTGGATTGTCAGTGCGCTTTTCCCAACACt");
		querySeqs[2] = new IupacNucleotideCodeSequence("ACAAGTGAGAGACAGGATCAGGTCAGCGGGCTACCACTGGGCCTCACCTCTATGcTGGGATCATATTCATCTACAAAGTGGTTCTGGATTAGCTGGATTGTCAGTGCGCTTTTCCCAACACC");
		querySeqs[3] = new IupacNucleotideCodeSequence("ACAAGTGAGAGACAGGATCAGGTCAGCGGGCTACCAtTGGGCCTCACCTCTATGGTGGGATCATATTCATCTACAAAGTGGTTCTGGATTAGCTGGATTGTCAGTGCGCTTTTCCCAACACC");

		String[] results = align(refSeq, querySeqs);
		for (String result : results) {
			System.out.println(result);
		}
	}
}
