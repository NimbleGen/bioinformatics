package com.roche.sequencing.bioinformatics.common.text;

public class TextPosition {
	private final int lineNumber;
	private final int columnIndex;

	public TextPosition(int lineNumber, int startingCharacterIndexInLine) {
		super();
		this.lineNumber = lineNumber;
		this.columnIndex = startingCharacterIndexInLine;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public int getColumnIndex() {
		return columnIndex;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + lineNumber;
		result = prime * result + columnIndex;
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
		TextPosition other = (TextPosition) obj;
		if (lineNumber != other.lineNumber)
			return false;
		if (columnIndex != other.columnIndex)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TextIndex [lineNumber=" + lineNumber + ", startingCharacterIndexInLine=" + columnIndex + "]";
	}

}
