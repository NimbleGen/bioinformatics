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

import java.util.ArrayList;
import java.util.List;

import com.roche.sequencing.bioinformatics.common.sequence.ICode;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCode;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class CigarStringUtil {
	public final static char CIGAR_DELETION_FROM_REFERENCE = 'D';
	public final static char CIGAR_ALIGNMENT_MATCH = 'M';
	public final static char CIGAR_INSERTION_TO_REFERENCE = 'I';
	public final static char CIGAR_SEQUENCE_MATCH = '=';
	public final static char CIGAR_SEQUENCE_MISMATCH = 'X';
	public final static char CIGAR_SOFT_CLIPPING = 'S';
	public final static char CIGAR_HARD_CLIPPING = 'H';
	public final static String REFERENCE_DELETION_INDICATOR_IN_MD_TAG = "^";

	private CigarStringUtil() {
		throw new AssertionError();
	}

	public static boolean isMatch(char character) {
		boolean isMatch = CIGAR_SEQUENCE_MATCH == character || CIGAR_ALIGNMENT_MATCH == character;
		return isMatch;
	}

	public static boolean isInsertionToReference(char character) {
		boolean isInsertion = CIGAR_INSERTION_TO_REFERENCE == character;
		return isInsertion;
	}

	public static boolean isMismatch(char character) {
		boolean isMismatch = CIGAR_SEQUENCE_MISMATCH == character;
		return isMismatch;
	}

	public static boolean isDeletionToReference(char character) {
		boolean isDeletion = CIGAR_DELETION_FROM_REFERENCE == character;
		return isDeletion;
	}

	static CigarString getCigarString(AlignmentPair alignmentPair) {
		ISequence referenceSequenceAlignment = alignmentPair.getReferenceAlignment();
		ISequence querySequenceAlignment = alignmentPair.getQueryAlignment();

		StringBuilder cigarStringBuilder = new StringBuilder();

		for (int i = 0; i < referenceSequenceAlignment.size(); i++) {
			ICode referenceCode = referenceSequenceAlignment.getCodeAt(i);
			ICode queryCode = querySequenceAlignment.getCodeAt(i);

			if (referenceCode.matches(queryCode)) {
				cigarStringBuilder.append(CIGAR_SEQUENCE_MATCH);
			} else if (referenceCode.equals(IupacNucleotideCode.GAP)) {
				cigarStringBuilder.append(CIGAR_INSERTION_TO_REFERENCE);
			} else if (queryCode.equals(IupacNucleotideCode.GAP)) {
				cigarStringBuilder.append(CIGAR_DELETION_FROM_REFERENCE);
			} else {
				cigarStringBuilder.append(CIGAR_SEQUENCE_MISMATCH);
			}
		}

		return CigarString.FromNonSummarizedCigarString(cigarStringBuilder.toString());
	}

	static String replaceSequenceMatchesAndMismatchesFromCigarString(String nonSummarizedCigarString) {
		nonSummarizedCigarString = nonSummarizedCigarString.replaceAll("" + CIGAR_SEQUENCE_MISMATCH, "" + CIGAR_ALIGNMENT_MATCH);
		nonSummarizedCigarString = nonSummarizedCigarString.replaceAll("" + CIGAR_SEQUENCE_MATCH, "" + CIGAR_ALIGNMENT_MATCH);
		return nonSummarizedCigarString;
	}

	static String getMismatchDetailsString(AlignmentPair alignmentPair) {
		ISequence referenceSequenceAlignment = alignmentPair.getReferenceAlignment();
		ISequence querySequenceAlignment = alignmentPair.getQueryAlignment();
		CigarString cigarString = getCigarString(alignmentPair);
		return getMismatchDetailsString(referenceSequenceAlignment, querySequenceAlignment, cigarString);
	}

	static String getMismatchDetailsString(ISequence referenceAlignmentSequence, ISequence queryAlignmentSequence, CigarString cigarString) {
		String summarizedCigarStringWithMisMatches = cigarString.getCigarString(true, true, false, false);

		List<MismatchDetailsEntry> entries = new ArrayList<>();

		StringBuilder currentString = new StringBuilder();
		int alignmentIndex = 0;
		for (char currentChar : summarizedCigarStringWithMisMatches.toCharArray()) {
			if (Character.isDigit(currentChar)) {
				currentString.append(currentChar);
			} else {
				int number = Integer.valueOf(currentString.toString());

				MismatchDetailsEntry lastEntry = null;
				if (entries.size() > 0) {
					lastEntry = entries.get(entries.size() - 1);
				}

				if (currentChar == CIGAR_ALIGNMENT_MATCH || currentChar == CIGAR_SEQUENCE_MATCH) {
					if (lastEntry != null && lastEntry.isAMatch()) {
						lastEntry.count += number;
					} else {
						entries.add(new MismatchDetailsEntry(currentChar, number, null));
					}
				} else if (currentChar == CIGAR_INSERTION_TO_REFERENCE) {
				} else if (currentChar == CIGAR_DELETION_FROM_REFERENCE) {
					ISequence sequence = referenceAlignmentSequence.subSequence(alignmentIndex, alignmentIndex + number - 1);
					entries.add(new MismatchDetailsEntry(currentChar, number, sequence));
				} else if (currentChar == CIGAR_SEQUENCE_MISMATCH) {
					ISequence sequence = referenceAlignmentSequence.subSequence(alignmentIndex, alignmentIndex + number - 1);
					entries.add(new MismatchDetailsEntry(currentChar, number, sequence));
				} else {
					throw new AssertionError("Unrecognized character[" + currentChar + "] in cigar string[" + summarizedCigarStringWithMisMatches + "].");
				}
				alignmentIndex += number;
				currentString = new StringBuilder();
			}
		}

		StringBuilder mismatchDetailsStringBuilder = new StringBuilder();
		MismatchDetailsEntry previousEntry = null;
		for (MismatchDetailsEntry entry : entries) {
			boolean isFirstEntry = (previousEntry == null);
			if (isFirstEntry) {
				if (!entry.isAMatch()) {
					mismatchDetailsStringBuilder.append("0");
				}
			}
			if (previousEntry != null && !previousEntry.isAMatch() && !entry.isAMatch()) {
				mismatchDetailsStringBuilder.append("0");
			}
			mismatchDetailsStringBuilder.append(entry.toMismatchDetailsString());
			previousEntry = entry;
		}

		return mismatchDetailsStringBuilder.toString();
	}

	private static class MismatchDetailsEntry {
		private final char type;
		private int count;
		private final ISequence sequence;

		public MismatchDetailsEntry(char type, int count, ISequence sequence) {
			super();
			this.type = type;
			this.count = count;
			this.sequence = sequence;
		}

		public String toMismatchDetailsString() {
			String mismatchDetailsString;
			if (type == CIGAR_ALIGNMENT_MATCH || type == CIGAR_SEQUENCE_MATCH) {
				mismatchDetailsString = "" + count;
			} else if (type == CIGAR_INSERTION_TO_REFERENCE) {
				mismatchDetailsString = "";
			} else if (type == CIGAR_DELETION_FROM_REFERENCE) {
				mismatchDetailsString = REFERENCE_DELETION_INDICATOR_IN_MD_TAG + sequence;
			} else if (type == CIGAR_SEQUENCE_MISMATCH) {
				mismatchDetailsString = "" + sequence;
			} else {
				throw new AssertionError("Unrecognized character[" + type + "].");
			}
			return mismatchDetailsString;
		}

		public boolean isAMatch() {
			boolean isAMatch = (type == CIGAR_ALIGNMENT_MATCH || type == CIGAR_SEQUENCE_MATCH);
			return isAMatch;

		}

	}

	public static String summarizeCigarString(String cigarString) {
		StringBuilder summarizedCigarString = new StringBuilder();

		if (cigarString.length() > 0) {
			int numberOfRepeats = 1;
			char currentlyRepeatingChar = cigarString.charAt(0);

			for (int i = 1; i < cigarString.length(); i++) {
				char currentChar = cigarString.charAt(i);

				if (currentlyRepeatingChar == currentChar) {
					numberOfRepeats++;
				} else {
					summarizedCigarString.append("" + numberOfRepeats + currentlyRepeatingChar);
					numberOfRepeats = 1;
					currentlyRepeatingChar = currentChar;
				}
			}

			summarizedCigarString.append("" + numberOfRepeats + currentlyRepeatingChar);
		}

		return summarizedCigarString.toString();
	}

	static int getEditDistance(CigarString cigarString) {
		int editDistance = 0;
		String nonSummarizedCigarStringWithMisMatches = cigarString.getCigarString(false, true, false, false);
		for (char currentChar : nonSummarizedCigarStringWithMisMatches.toCharArray()) {

			if ((currentChar == CIGAR_INSERTION_TO_REFERENCE) || (currentChar == CIGAR_SEQUENCE_MISMATCH)) {
				editDistance++;
			} else if (currentChar == CIGAR_ALIGNMENT_MATCH) {
			} else if (currentChar == CIGAR_SEQUENCE_MATCH) {
			} else if (currentChar == CIGAR_DELETION_FROM_REFERENCE) {
			} else {
				throw new AssertionError("Unrecognized character[" + currentChar + "] in cigar string[" + nonSummarizedCigarStringWithMisMatches + "].");
			}
		}

		return editDistance;
	}

	public static String expandCigarString(String summarizedCigarString) {
		StringBuilder expandedCigarString = new StringBuilder();
		StringBuilder currentString = new StringBuilder();
		for (char currentChar : summarizedCigarString.toCharArray()) {
			if (Character.isDigit(currentChar)) {
				currentString.append(currentChar);
			} else {
				int number = Integer.valueOf(currentString.toString());
				if (currentChar == CIGAR_ALIGNMENT_MATCH || currentChar == CIGAR_SEQUENCE_MATCH) {
					for (int i = 0; i < number; i++) {
						expandedCigarString.append(currentChar);
					}
				} else if (currentChar == CIGAR_INSERTION_TO_REFERENCE) {
					for (int i = 0; i < number; i++) {
						expandedCigarString.append(currentChar);
					}
				} else if (currentChar == CIGAR_DELETION_FROM_REFERENCE) {
					for (int i = 0; i < number; i++) {
						expandedCigarString.append(currentChar);
					}
				} else if (currentChar == CIGAR_SEQUENCE_MISMATCH) {
					for (int i = 0; i < number; i++) {
						expandedCigarString.append(currentChar);
					}
				} else if (currentChar == CIGAR_SOFT_CLIPPING) {
					expandedCigarString.append(currentChar);
				} else if (currentChar == CIGAR_HARD_CLIPPING) {
					expandedCigarString.append(currentChar);
				} else {
					throw new AssertionError("Unrecognized character[" + currentChar + "] in cigar string[" + summarizedCigarString + "].");
				}
				currentString = new StringBuilder();
			}
		}
		return expandedCigarString.toString();
	}

	public static boolean isSoftClip(char cigarSymbol) {
		boolean isSoftClip = cigarSymbol == CIGAR_SOFT_CLIPPING;
		return isSoftClip;
	}

	public static boolean isHardClip(char cigarSymbol) {
		boolean isHardClip = cigarSymbol == CIGAR_HARD_CLIPPING;
		return isHardClip;
	}

	public static boolean isClip(char cigarSymbol) {
		boolean isClip = isSoftClip(cigarSymbol) || isHardClip(cigarSymbol);
		return isClip;
	}

	public static String reverseCigarString(String summarizedCigarString) {
		String expandedCigarString = expandCigarString(summarizedCigarString);
		String reversedCigarString = summarizeCigarString(StringUtil.reverse(expandedCigarString));
		return reversedCigarString;
	}

}
