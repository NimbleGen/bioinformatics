package com.roche.sequencing.bioinformatics.common.text;

import java.io.File;
import java.io.IOException;

public interface IDocument {

	int getNumberOfLines();

	void copyTextToFile(File outputFile, int startingLineNumber, int endingLineNumberInclusive) throws IOException;

	void copyTextToFile(File outputFile, int startingLineNumber, Integer startingCharacterIndexInLine, int endingLineNumberInclusive, Integer endingCharacterIndexInLine) throws IOException;

	String[] getText(int startingLineNumber, int endingLineNumberInclusive);

	String[] getText(int startingLineNumber, Integer startingCharacterIndexInLine, int endingLineNumberInclusive, Integer endingCharacterIndexInLine);

	int getMostTabsFoundInALine();

	void close() throws IOException;

	int getNumberOfCharactersInLongestLine(int charactersPerTab);

	TextPosition search(int startingLine, int startingCharacterIndexInLine, Integer stopLine, boolean isSearchCaseSensitive, String searchString);

	TextPosition search(int startingLine, int startingCharacterIndexInLine, Integer stopLine, boolean isSearchCaseSensitive, String searchString, ITextProgressListener optionalTextProgressListener);

}
