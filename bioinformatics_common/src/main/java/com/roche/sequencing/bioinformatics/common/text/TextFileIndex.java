package com.roche.sequencing.bioinformatics.common.text;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

public class TextFileIndex {

	private final int recordedLineIncrement;
	private final long[] bytePositionOfLines;
	private final int numberOfLines;
	private final Map<Integer, Integer> maxCharsByTabCount;

	public TextFileIndex(int recordedLineIncrement, long[] bytePositionOfLines, int numberOfLines, Map<Integer, Integer> maxCharsByTabCount) {
		super();
		this.recordedLineIncrement = recordedLineIncrement;
		this.bytePositionOfLines = bytePositionOfLines;
		this.numberOfLines = numberOfLines;
		this.maxCharsByTabCount = maxCharsByTabCount;
	}

	public int getRecordedLineIncrements() {
		return recordedLineIncrement;
	}

	public long[] getBytePositionOfLines() {
		return bytePositionOfLines;
	}

	public int getNumberOfLines() {
		return numberOfLines;
	}

	public Map<Integer, Integer> getMaxCharactersInALineByTabCount() {
		return maxCharsByTabCount;
	}

	public int getNumberOfCharactersInLongestLine(int charactersPerTab) {
		int max = 0;

		for (Entry<Integer, Integer> entry : maxCharsByTabCount.entrySet()) {
			int tabCount = entry.getKey();
			int numberOfCharacters = entry.getValue();
			int lineLength = (numberOfCharacters - tabCount) + (tabCount * charactersPerTab);
			if (lineLength > max) {
				max = lineLength;
			}
		}
		return max;
	}

	public int getMostTabsFoundInALine() {
		return Collections.max(maxCharsByTabCount.keySet());
	}

	@Override
	public String toString() {
		return "TextFileIndex [recordedLineIncrement=" + recordedLineIncrement + ", numberOfLines=" + numberOfLines + ", maxCharsByTabCount=" + maxCharsByTabCount + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytePositionOfLines);
		result = prime * result + recordedLineIncrement;
		result = prime * result + ((maxCharsByTabCount == null) ? 0 : maxCharsByTabCount.hashCode());
		result = prime * result + numberOfLines;
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
		TextFileIndex other = (TextFileIndex) obj;
		if (!Arrays.equals(bytePositionOfLines, other.bytePositionOfLines))
			return false;
		if (recordedLineIncrement != other.recordedLineIncrement)
			return false;
		if (maxCharsByTabCount == null) {
			if (other.maxCharsByTabCount != null)
				return false;
		} else if (!maxCharsByTabCount.equals(other.maxCharsByTabCount))
			return false;
		if (numberOfLines != other.numberOfLines)
			return false;
		return true;
	}

}
