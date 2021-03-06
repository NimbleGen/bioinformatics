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

import java.text.DecimalFormat;

import com.roche.sequencing.bioinformatics.common.sequence.ICode;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCode;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * Compute a global alignment of two sequences using the Needleman Wunsch algorithm.
 * 
 */
public class NeedlemanWunschGlobalAlignment {

	private final DecimalFormat numberFormatter = new DecimalFormat("0.00");

	private final IAlignmentScorer alignmentScorer;

	private final ISequence referenceSequence;
	private final ISequence querySequence;
	private final TraceabilityMatrixCell[][] traceabilityMatrix;

	private AlignmentPair alignment;

	public NeedlemanWunschGlobalAlignment(ISequence referenceSequence, ISequence querySequence, IAlignmentScorer alignmentScorer) {
		this.referenceSequence = referenceSequence;
		this.querySequence = querySequence;
		traceabilityMatrix = new TraceabilityMatrixCell[querySequence.size() + 1][referenceSequence.size() + 1];
		this.alignmentScorer = alignmentScorer;
	}

	/**
	 * Constructor that uses a default alignment scoring table
	 * 
	 * @param referenceSequence
	 * @param querySequence
	 */
	public NeedlemanWunschGlobalAlignment(ISequence referenceSequence, ISequence querySequence) {
		this(referenceSequence, querySequence, new SimpleAlignmentScorer());
	}

	private void initializeTraceabilityMatrix() {
		for (int rowIndex = 0; rowIndex < traceabilityMatrix.length; rowIndex++) {
			for (int columnIndex = 0; columnIndex < traceabilityMatrix[rowIndex].length; columnIndex++) {
				TraceabilityMatrixCell cell = new TraceabilityMatrixCell(rowIndex, columnIndex);
				cell.setScore(getInitialScore(rowIndex, columnIndex));
				cell.setSourceCell(getInitialSourceCell(rowIndex, columnIndex));
				traceabilityMatrix[rowIndex][columnIndex] = cell;
			}
		}
	}

	private void completeTraceabilityMatrix() {
		for (int rowIndex = 1; rowIndex < traceabilityMatrix.length; rowIndex++) {
			for (int columnIndex = 1; columnIndex < traceabilityMatrix[rowIndex].length; columnIndex++) {
				completeCell(rowIndex, columnIndex);
			}
		}
	}

	/**
	 * return an alignment pair associated with the global alignment of the reference and query sequences provided.
	 * 
	 * @return
	 */
	public AlignmentPair getAlignmentPair() {
		if (alignment == null) {
			initializeTraceabilityMatrix();
			completeTraceabilityMatrix();

			IupacNucleotideCodeSequence alignment1 = new IupacNucleotideCodeSequence();
			IupacNucleotideCodeSequence alignment2 = new IupacNucleotideCodeSequence();
			TraceabilityMatrixCell currentCell = getStartingTracebackCell();

			while (!tracebackComplete(currentCell)) {
				if (currentCell.getRowIndex() - currentCell.getSourceCell().getRowIndex() == 1) {
					alignment2.append(IupacNucleotideCode.getCodeFromNucleotide(querySequence.getCodeAt(currentCell.getRowIndex() - 1)));
				} else {
					alignment2.append(IupacNucleotideCode.GAP);
				}

				if (currentCell.getColumnIndex() - currentCell.getSourceCell().getColumnIndex() == 1) {
					alignment1.append(IupacNucleotideCode.getCodeFromNucleotide(referenceSequence.getCodeAt(currentCell.getColumnIndex() - 1)));
				} else {
					alignment1.append(IupacNucleotideCode.GAP);
				}

				currentCell = currentCell.getSourceCell();
			}

			alignment = new AlignmentPair(alignment1.getReverse(), alignment2.getReverse());
		}

		return alignment;
	}

	/**
	 * @return the average score per location. This is guaranteed to be between the largest penalty and largest reward provided by the IAlignmentScorer
	 */
	public double getLengthNormalizedAlignmentScore() {
		ISequence referenceAlignment = getAlignmentPair().getReferenceAlignment();
		int alignmentLength = referenceAlignment.size();

		double lengthNormalizedAlignmentScore = ((double) getAlignmentScore()) / ((double) alignmentLength);

		return lengthNormalizedAlignmentScore;
	}

	/**
	 * @return the alignment score associated with this alignment
	 */
	public double getAlignmentScore() {
		getAlignmentPair();
		return getStartingTracebackCell().getScore();
	}

	/**
	 * @return the alignment scorer used to produce this alignment
	 */
	private IAlignmentScorer getAlignmentScorer() {
		return alignmentScorer;
	}

	/**
	 * @return the reference sequence used to calculate this alignment
	 */
	public ISequence getReferenceSequence() {
		return referenceSequence;
	}

	/**
	 * @return the query sequence used to calculate this alignment
	 */
	public ISequence getQuerySequence() {
		return querySequence;
	}

	private int getLengthOfLongestScore() {
		int lengthOfLongestScore = 0;

		for (int i = 0; i < traceabilityMatrix.length; i++) {
			TraceabilityMatrixCell[] currentRow = traceabilityMatrix[i];

			for (int j = 0; j < currentRow.length; j++) {
				TraceabilityMatrixCell cell = currentRow[j];
				String scoreAsString = numberFormatter.format(cell.getScore());

				lengthOfLongestScore = Math.max(scoreAsString.length(), lengthOfLongestScore);
			}
		}

		return lengthOfLongestScore;
	}

	/**
	 * @return a string representation of this alignment
	 */
	public String getAlignmentAsString() {
		return getAlignmentPair().getAlignmentAsString();
	}

	/**
	 * @return a string represenation of the traceability matrix of this alignment
	 */
	public String getTraceabilityMatrixAsString() {
		// make sure alignment has been calculated
		getAlignmentPair();
		String entryDelimiter = " | ";
		int lengthOfLongestScore = getLengthOfLongestScore();

		StringBuilder stringBuilder = new StringBuilder();
		int lengthOfRow = 0;

		for (int i = 0; i < traceabilityMatrix.length; i++) {
			if (i == 0) {
				// print out sequence
				stringBuilder.append("  " + entryDelimiter);
				stringBuilder.append(StringUtil.padLeft("_", lengthOfLongestScore + 1) + entryDelimiter);

				for (ICode code : referenceSequence) {
					stringBuilder.append(" " + StringUtil.padLeft(code.toString(), lengthOfLongestScore + 1) + entryDelimiter);
				}

				stringBuilder.append(StringUtil.NEWLINE);

				lengthOfRow = stringBuilder.length();
				stringBuilder.append(StringUtil.repeatString("-", lengthOfRow) + StringUtil.NEWLINE);
				stringBuilder.append(" _" + entryDelimiter);
			} else {
				stringBuilder.append(" " + querySequence.getCodeAt(i - 1) + entryDelimiter);
			}

			TraceabilityMatrixCell[] currentRow = traceabilityMatrix[i];

			for (int j = 0; j < currentRow.length; j++) {
				TraceabilityMatrixCell cell = currentRow[j];

				if (cell != null) {
					String scoreAsString = numberFormatter.format(cell.getScore());

					stringBuilder.append(cell.getSourceIndicator() + " " + StringUtil.padLeft(scoreAsString, lengthOfLongestScore) + entryDelimiter);
				}
			}

			stringBuilder.append(StringUtil.NEWLINE);
			stringBuilder.append(StringUtil.repeatString("-", lengthOfRow));
			stringBuilder.append(StringUtil.NEWLINE);

		}

		return stringBuilder.toString();
	}

	/**
	 * @return the cigar string associated with this alignment
	 */
	public CigarString getCigarString() {
		return getAlignmentPair().getCigarString();
	}

	/**
	 * @return the cigar string associated with an alignment on the reverse of the reference and query sequences
	 */
	public CigarString getReverseCigarString() {
		return getAlignmentPair().getReverseCigarString();
	}

	/**
	 * @return mismatch details string
	 */
	public String getMismatchDetailsString() {
		return getAlignmentPair().getMismatchDetailsString();
	}

	/**
	 * @return the reverse of the mismatch details string
	 */
	public String getReverseMismatchDetailsString() {
		return getAlignmentPair().getReverseMismatchDetailsString();
	}

	private void completeCell(int rowIndex, int columnIndex) {
		TraceabilityMatrixCell currentCell = traceabilityMatrix[rowIndex][columnIndex];
		TraceabilityMatrixCell cellAboveLeft = traceabilityMatrix[rowIndex - 1][columnIndex - 1];
		TraceabilityMatrixCell cellAbove = traceabilityMatrix[rowIndex - 1][columnIndex];
		TraceabilityMatrixCell cellToLeft = traceabilityMatrix[rowIndex][columnIndex - 1];

		boolean isBottomOfColumn = (rowIndex == (getQuerySequence().size()));
		boolean isEndOfRow = (columnIndex == (getReferenceSequence().size()));

		TraceabilityMatrixCell sourceOfCellAbove = cellAbove.getSourceCell();
		boolean isVerticalGapContinuation = ((sourceOfCellAbove != null) && (sourceOfCellAbove.getColumnIndex() == cellAbove.getColumnIndex()));
		double verticalScore = cellAbove.getScore();

		if (!isEndOfRow || getAlignmentScorer().shouldPenalizeEndingTerminalGaps()) {
			if (isVerticalGapContinuation) {
				if (cellAbove.getIndexInVerticalGap() != null) {
					verticalScore += getAlignmentScorer().getGapScore(cellAbove.getIndexInVerticalGap() + 1);
				} else {
					throw new AssertionError();
				}
			} else {
				verticalScore += getAlignmentScorer().getGapStartScore() + getAlignmentScorer().getGapScore(0);
			}
		}

		TraceabilityMatrixCell sourceOfCellToLeft = cellToLeft.getSourceCell();
		boolean isHorizontalGapContinuation = ((sourceOfCellToLeft != null) && (sourceOfCellToLeft.getRowIndex() == cellToLeft.getRowIndex()));
		double horizontalScore = cellToLeft.getScore();

		if (!isBottomOfColumn || getAlignmentScorer().shouldPenalizeEndingTerminalGaps()) {
			if (isHorizontalGapContinuation) {
				if (cellToLeft.getIndexInHorizontalGap() != null) {
					horizontalScore += getAlignmentScorer().getGapScore(cellToLeft.getIndexInHorizontalGap() + 1);
				} else {
					throw new AssertionError();
				}
			} else {
				horizontalScore += getAlignmentScorer().getGapStartScore() + getAlignmentScorer().getGapScore(0);
			}
		}

		double matchOrMismatchScore = cellAboveLeft.getScore()
				+ getAlignmentScorer().getMatchScore(getQuerySequence().getCodeAt(currentCell.getRowIndex() - 1), getReferenceSequence().getCodeAt(currentCell.getColumnIndex() - 1));

		double maxScore = ArraysUtil.max(horizontalScore, verticalScore, matchOrMismatchScore);

		currentCell.setScore(maxScore);

		if (maxScore == horizontalScore) {
			currentCell.setSourceCell(cellToLeft);
			if (cellToLeft.getIndexInHorizontalGap() != null) {
				currentCell.setIndexInHorizontalGap(cellToLeft.getIndexInHorizontalGap() + 1);
			} else {
				currentCell.setIndexInHorizontalGap(0);
			}
		} else if (maxScore == verticalScore) {
			currentCell.setSourceCell(cellAbove);
			if (cellAbove.getIndexInVerticalGap() != null) {
				currentCell.setIndexInVerticalGap(cellAbove.getIndexInVerticalGap() + 1);
			} else {
				currentCell.setIndexInVerticalGap(0);
			}
		} else if (maxScore == matchOrMismatchScore) {
			currentCell.setSourceCell(cellAboveLeft);
		} else {
			throw new AssertionError();
		}
	}

	private boolean tracebackComplete(TraceabilityMatrixCell currentCell) {
		return currentCell.getSourceCell() == null;
	}

	private TraceabilityMatrixCell getStartingTracebackCell() {
		TraceabilityMatrixCell startingTracebackCell = null;

		startingTracebackCell = traceabilityMatrix[traceabilityMatrix.length - 1][traceabilityMatrix[0].length - 1];

		return startingTracebackCell;
	}

	private TraceabilityMatrixCell getInitialSourceCell(int rowIndex, int columnIndex) {
		TraceabilityMatrixCell initialPointer = null;
		if ((rowIndex == 0) && (columnIndex != 0)) {
			initialPointer = traceabilityMatrix[rowIndex][columnIndex - 1];
		} else if ((columnIndex == 0) && (rowIndex != 0)) {
			initialPointer = traceabilityMatrix[rowIndex - 1][columnIndex];
		}

		return initialPointer;
	}

	private double getInitialScore(int rowIndex, int columnIndex) {
		double score = 0;

		if ((rowIndex == 0) && (columnIndex != 0)) {
			if (getAlignmentScorer().shouldPenalizeStartingTerminalGaps()) {
				// this minus one in getAlignmentScorer().getGapScore(0) - 1 is so this behaves exactly as a previous version
				// when using the SimpleAlignmentScorer
				score = getAlignmentScorer().getGapStartScore() + ((columnIndex - 1) * (getAlignmentScorer().getGapScore(0) - 1));
			} else {
				score = 0;
			}
		} else if ((columnIndex == 0) && (rowIndex != 0)) {
			if (getAlignmentScorer().shouldPenalizeStartingTerminalGaps()) {
				// this minus one in getAlignmentScorer().getGapScore(0) - 1 is so this behaves exactly as a previous version
				// when using the SimpleAlignmentScorer
				score = getAlignmentScorer().getGapStartScore() + ((rowIndex - 1) * (getAlignmentScorer().getGapScore(0) - 1));
			} else {
				score = 0;
			}

		}

		return score;
	}

	/**
	 * @return the edit distance (number of mutations or inserts when converting from the query sequence to the reference sequence).
	 */
	public int getEditDistance() {
		return getCigarString().getEditDistance();
	}

	/**
	 * @return the mismatch details associated with this alignment
	 */
	public String getMismatchDetails() {
		return CigarStringUtil.getMismatchDetailsString(getAlignmentPair());
	}

	public int getNumberOfNucleotidesOutsideOfReference() {
		int numberOfNucleotidesOutsideOfReference = 0;
		int i = 0;
		ISequence referenceAlignment = getAlignmentPair().getReferenceAlignment();
		while (i < referenceAlignment.size() && referenceAlignment.getCodeAt(i).equals(IupacNucleotideCode.GAP)) {
			i++;
		}
		numberOfNucleotidesOutsideOfReference += i;

		i = referenceAlignment.size() - 1;
		while (i >= 0 && referenceAlignment.getCodeAt(i).equals(IupacNucleotideCode.GAP)) {
			i--;
		}
		numberOfNucleotidesOutsideOfReference += (referenceAlignment.size() - i - 1);
		return numberOfNucleotidesOutsideOfReference;
	}

	/**
	 * @return the reference index of the first sequence match in this alignment, -1 if no sequence matches exist
	 */
	public int getIndexOfFirstMatchInReference() {
		ISequence referenceAlignment = getAlignmentPair().getQueryAlignment();
		boolean matchFound = false;
		int index = 0;
		while (index < referenceAlignment.size() && !matchFound) {
			ICode currentCode = referenceAlignment.getCodeAt(index);
			if (!currentCode.matches(IupacNucleotideCode.GAP)) {
				matchFound = true;
			} else {
				index++;
			}
		}
		if (!matchFound) {
			index = -1;
		}
		return index;
	}

	public double getRatioOfQueryMatchesToQueryLength() {
		String unsummarizedCigarString = getCigarString().getCigarString(false, true);
		double matches = StringUtil.countMatches(unsummarizedCigarString, "" + CigarStringUtil.CIGAR_SEQUENCE_MATCH);
		double queryLength = querySequence.size();
		double queryMatchesToQueryLength = matches / queryLength;
		return queryMatchesToQueryLength;
	}

	public int getNumberOfMismatches() {
		return alignment.getNumberOfMismatches();
	}

	public int getNumberOfMatches() {
		return alignment.getNumberOfMatches();
	}

}
