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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.roche.sequencing.bioinformatics.common.stringsequence.ILetter;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * Compute a global alignment of two sequences using the Needleman Wunsch algorithm.
 * 
 */
public class NeedlemanWunschGlobalStringAlignment {

	private final DecimalFormat numberFormatter = new DecimalFormat("0.00");

	private final IStringAlignmentScorer alignmentScorer;

	private final List<ILetter> referenceSequence;
	private final List<ILetter> querySequence;
	private final TraceabilityMatrixCell[][] traceabilityMatrix;

	private StringAlignmentPair alignment;

	public NeedlemanWunschGlobalStringAlignment(List<ILetter> referenceSequence, List<ILetter> querySequence, IStringAlignmentScorer alignmentScorer) {
		this.referenceSequence = referenceSequence;
		this.querySequence = querySequence;
		traceabilityMatrix = new TraceabilityMatrixCell[querySequence.size() + 1][referenceSequence.size() + 1];
		this.alignmentScorer = alignmentScorer;
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
	public StringAlignmentPair getAlignmentPair() {
		if (alignment == null) {
			initializeTraceabilityMatrix();
			completeTraceabilityMatrix();

			List<ILetter> alignment1 = new ArrayList<ILetter>();
			List<ILetter> alignment2 = new ArrayList<ILetter>();
			TraceabilityMatrixCell currentCell = getStartingTracebackCell();

			while (!tracebackComplete(currentCell)) {
				if (currentCell.getRowIndex() - currentCell.getSourceCell().getRowIndex() == 1) {
					alignment2.add(querySequence.get(currentCell.getRowIndex() - 1));
				} else {
					alignment2.add(Gap.GAP);
				}

				if (currentCell.getColumnIndex() - currentCell.getSourceCell().getColumnIndex() == 1) {
					alignment1.add(referenceSequence.get(currentCell.getColumnIndex() - 1));
				} else {
					alignment1.add(Gap.GAP);
				}

				currentCell = currentCell.getSourceCell();
			}

			alignment = new StringAlignmentPair(Lists.reverse(alignment1), Lists.reverse(alignment2));
		}

		return alignment;
	}

	/**
	 * @return the average score per location. This is guaranteed to be between the largest penalty and largest reward provided by the IAlignmentScorer
	 */
	public double getLengthNormalizedAlignmentScore() {
		List<ILetter> referenceAlignment = getAlignmentPair().getReferenceAlignment();
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
	private IStringAlignmentScorer getAlignmentScorer() {
		return alignmentScorer;
	}

	/**
	 * @return the reference sequence used to calculate this alignment
	 */
	private List<ILetter> getReferenceSequence() {
		return referenceSequence;
	}

	/**
	 * @return the query sequence used to calculate this alignment
	 */
	private List<ILetter> getQuerySequence() {
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

				for (ILetter code : referenceSequence) {
					stringBuilder.append(" " + StringUtil.padLeft(code.toString(), lengthOfLongestScore + 1) + entryDelimiter);
				}

				stringBuilder.append(StringUtil.NEWLINE);

				lengthOfRow = stringBuilder.length();
				stringBuilder.append(StringUtil.repeatString("-", lengthOfRow) + StringUtil.NEWLINE);
				stringBuilder.append(" _" + entryDelimiter);
			} else {
				stringBuilder.append(" " + querySequence.get(i - 1) + entryDelimiter);
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
				verticalScore += getAlignmentScorer().getGapScore(getQuerySequence().get(currentCell.getRowIndex() - 1));
			} else {
				verticalScore += getAlignmentScorer().getGapStartScore(getQuerySequence().get(currentCell.getRowIndex() - 1));
			}
		}

		TraceabilityMatrixCell sourceOfCellToLeft = cellToLeft.getSourceCell();
		boolean isHorizontalGapContinuation = ((sourceOfCellToLeft != null) && (sourceOfCellToLeft.getRowIndex() == cellToLeft.getRowIndex()));
		double horizontalScore = cellToLeft.getScore();

		if (!isBottomOfColumn || getAlignmentScorer().shouldPenalizeEndingTerminalGaps()) {
			if (isHorizontalGapContinuation) {
				horizontalScore += getAlignmentScorer().getGapScore(getReferenceSequence().get(currentCell.getColumnIndex() - 1));
			} else {
				horizontalScore += getAlignmentScorer().getGapStartScore(getReferenceSequence().get(currentCell.getColumnIndex() - 1));
			}
		}

		double matchOrMismatchScore = cellAboveLeft.getScore()
				+ getAlignmentScorer().getMatchScore(getQuerySequence().get(currentCell.getRowIndex() - 1), getReferenceSequence().get(currentCell.getColumnIndex() - 1));

		double maxScore = ArraysUtil.max(horizontalScore, verticalScore, matchOrMismatchScore);

		currentCell.setScore(maxScore);

		if (maxScore == horizontalScore) {
			currentCell.setSourceCell(cellToLeft);
		} else if (maxScore == verticalScore) {
			currentCell.setSourceCell(cellAbove);
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
				for (int i = 0; i < columnIndex; i++) {
					score += getAlignmentScorer().getGapScore(getReferenceSequence().get(i));
				}
			} else {
				score = 0;
			}
		} else if ((columnIndex == 0) && (rowIndex != 0)) {
			if (getAlignmentScorer().shouldPenalizeStartingTerminalGaps()) {
				for (int i = 0; i < rowIndex; i++) {
					score += getAlignmentScorer().getGapScore(getQuerySequence().get(i));
				}
			} else {
				score = 0;
			}

		}

		return score;
	}

}
