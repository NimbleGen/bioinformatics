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

/**
 * Class to keep track of each entry in the dynamic programming table
 * 
 * 
 */
class TraceabilityMatrixCell {
	private TraceabilityMatrixCell sourceCell;
	private double score;
	private final int row;
	private final int col;

	TraceabilityMatrixCell(int rowIndex, int columnIndex) {
		this.row = rowIndex;
		this.col = columnIndex;
	}

	/**
	 * Set the score of this cell
	 * 
	 * @param score
	 */
	public void setScore(double score) {
		this.score = score;
	}

	/**
	 * @return the score of this cell
	 */
	public double getScore() {
		return score;
	}

	/**
	 * Set the source of the cell
	 * 
	 * @param sourceCell
	 */
	public void setSourceCell(TraceabilityMatrixCell sourceCell) {
		this.sourceCell = sourceCell;
	}

	/**
	 * @return the row index of this cell
	 */
	public int getRowIndex() {
		return row;
	}

	/**
	 * @return the column index of this cell
	 */
	public int getColumnIndex() {
		return col;
	}

	/**
	 * @return the source of this cell
	 */
	public TraceabilityMatrixCell getSourceCell() {
		return sourceCell;
	}

	/**
	 * @return a string represenation of the source of this cell
	 */
	String getSourceIndicator() {
		String sourceIndicator = "?";

		if (sourceCell != null) {
			if (sourceCell.col == col) {
				sourceIndicator = "^";
			} else if (sourceCell.row == row) {
				sourceIndicator = "<";
			} else if ((sourceCell.row == (row - 1)) && (sourceCell.col == (col - 1))) {
				sourceIndicator = "\\";
			} else {
				throw new AssertionError();
			}
		}

		return sourceIndicator;
	}

	@Override
	public String toString() {
		return getSourceIndicator() + " " + score;
	}

}
