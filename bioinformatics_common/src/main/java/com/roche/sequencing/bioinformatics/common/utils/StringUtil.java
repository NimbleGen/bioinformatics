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

package com.roche.sequencing.bioinformatics.common.utils;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.plaf.FontUIResource;

/**
 * Utility class for working with Strings
 * 
 */
public final class StringUtil {
	public static final String NEWLINE = System.getProperty("line.separator");

	public static final String TAB = "\t";
	public static final char CARRIAGE_RETURN = '\r';
	public static final char NEWLINE_SYMBOL = '\n';
	// NOTE: WINDOWS_NEWLINE = CARRIAGE_RETURN + LINE_FEED;
	// NOTE: LINUX_NEWLINE = LINE_FEED;
	public static final String LINUX_NEWLINE = "" + NEWLINE_SYMBOL;
	public static final String WINDOWS_NEWLINE = CARRIAGE_RETURN + LINUX_NEWLINE;

	private final static int DEFAULT_MAX_LABEL_LENGTH_FOR_REDUCTION = 20;

	private static Random RANDOM = new Random(System.currentTimeMillis());
	private static char[] CHARACTERS = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2',
			'3', '4', '5', '6', '7', '8', '9' };

	private StringUtil() {
		throw new AssertionError();
	}

	/**
	 * @param numberOfSpaces
	 * @return String containing the provided number of spaces
	 */
	public static String getSpacesAsString(int numberOfSpaces) {
		return repeatString(" ", numberOfSpaces);
	}

	/**
	 * @param stringToRepeat
	 * @param numberOfRepeats
	 * @return a string that repeats the provided string the provided number of times
	 */
	public static String repeatString(String stringToRepeat, int numberOfRepeats) {
		StringBuilder stringBuilder = new StringBuilder();

		for (int i = 0; i < numberOfRepeats; i++) {
			stringBuilder.append(stringToRepeat);
		}

		return stringBuilder.toString();
	}

	/**
	 * @param string
	 * @param lengthOfDesiredStringWithPadding
	 * @return the provided string with padding to the left so the total length of the string equals the provided length
	 */
	public static String padLeft(String string, int lengthOfDesiredStringWithPadding) {
		return String.format("%1$" + lengthOfDesiredStringWithPadding + "s", string);
	}

	/**
	 * @param string
	 * @return a reversed representation of the provided string
	 */
	public static String reverse(String string) {
		StringBuilder reversedStringBuilder = new StringBuilder();

		for (int i = string.length() - 1; i >= 0; i--) {
			reversedStringBuilder.append(string.charAt(i));
		}

		return reversedStringBuilder.toString();
	}

	/**
	 * @param strings
	 * @return true if all the provided string equal each other
	 */
	public static boolean equals(String... strings) {
		boolean equals = true;

		if (strings.length > 1) {
			String firstString = strings[0];

			compareLoop: for (int i = 1; i < strings.length; i++) {
				equals = firstString.equals(strings[i]);

				if (!equals) {
					break compareLoop;
				}
			}
		}

		return equals;
	}

	/**
	 * Return the index of the nth occurrence of a character in a string
	 * 
	 * @param string
	 *            The source string
	 * @param character
	 *            The query character
	 * @param indexOfDesiredOccurence
	 *            Which occurrence of the character to return the index for
	 * @return The index of the nth occurrence of the character, or -1 if there isn't an nth occurrence of the character in the string.
	 */

	static int nthOccurrence(String string, char character, int indexOfDesiredOccurence) {
		int position = string.indexOf(character, 0);
		int timesCharacterFoundSoFar = 1;
		while ((timesCharacterFoundSoFar < indexOfDesiredOccurence) && (position != -1)) {
			timesCharacterFoundSoFar++;
			position = string.indexOf(character, position + 1);
		}
		if (timesCharacterFoundSoFar != indexOfDesiredOccurence) {
			position = -1;
		}
		return position;
	}

	/**
	 * @param string
	 * @return true if the provided string can be represented as a number
	 */
	public static boolean isNumeric(String string) {
		boolean isNumeric = true;
		try {
			Double.valueOf(string);
		} catch (NumberFormatException e) {
			isNumeric = false;
		}
		return isNumeric;
	}

	/**
	 * 
	 * @param reference
	 * @param query
	 * @return the number of query strings found in the reference
	 */
	public static int countMatches(String reference, String query) {
		int count = reference.length() - reference.replaceAll(query, "").length();
		return count;
	}

	public static String[] splitIntoLines(String string, FontUIResource font, double maxLineWidth) {
		List<String> lines = new ArrayList<String>();
		StringBuilder currentLineText = new StringBuilder();
		int currentIndex = 0;
		int indexOfLastSpace = 0;
		int startIndexOfCurrentLine = 0;

		while (currentIndex < string.length()) {
			char currentChar = string.charAt(currentIndex);
			if (Character.isSpaceChar(currentChar)) {
				indexOfLastSpace = currentIndex;
			}
			currentLineText.append(currentChar);
			Dimension stringSize = GraphicsUtil.getStringExtent(font, currentLineText.toString());
			if ((stringSize.getWidth() > maxLineWidth)) {
				if (indexOfLastSpace == startIndexOfCurrentLine) {
					throw new IllegalStateException("The text can not be split on spaces.");
				}

				lines.add(currentLineText.substring(0, (indexOfLastSpace - startIndexOfCurrentLine)));
				currentIndex = indexOfLastSpace + 1;
				startIndexOfCurrentLine = currentIndex;
				currentLineText = new StringBuilder();
			} else if (currentIndex == (string.length() - 1)) {
				lines.add(currentLineText.toString());
				currentIndex++;
			} else {
				currentIndex++;
			}
		}

		return lines.toArray(new String[0]);
	}

	public static String[] splitIntoLines(String string, int maxCharactersInALine) {
		List<String> lines = new ArrayList<String>();
		StringBuilder currentLineText = new StringBuilder();
		int currentIndex = 0;
		int indexOfLastSpace = 0;
		int startIndexOfCurrentLine = 0;

		while (currentIndex < string.length()) {
			char currentChar = string.charAt(currentIndex);
			if (Character.isSpaceChar(currentChar)) {
				indexOfLastSpace = currentIndex;
			}
			currentLineText.append(currentChar);
			if ((currentLineText.length() > maxCharactersInALine)) {
				if (indexOfLastSpace == startIndexOfCurrentLine) {
					throw new IllegalStateException("The text can not be split on spaces.");
				}

				lines.add(currentLineText.substring(0, (indexOfLastSpace - startIndexOfCurrentLine)));
				currentIndex = indexOfLastSpace + 1;
				startIndexOfCurrentLine = currentIndex;
				currentLineText = new StringBuilder();
			} else if (currentIndex == (string.length() - 1)) {
				lines.add(currentLineText.toString());
				currentIndex++;
			} else {
				currentIndex++;
			}
		}

		return lines.toArray(new String[0]);
	}

	public static void main(String[] args) {
		String s = "A boy ran very far to get home.  He made it very quickly.";
		System.out.println(ArraysUtil.toString(splitIntoLines(s, 10)));
	}

	/**
	 * insert a string into the base string at intervals of spaces For example: insertStringEveryNSpaces("abcdefghijk","z",2) would return "abzcdzefzghzijzk"
	 * 
	 * @param baseString
	 *            string that will be manipulated
	 * @param stringToInsert
	 * @param spaces
	 *            number of spaces to place in between inserts
	 * @return altered string
	 */

	static String insertStringEveryNSpaces(String baseString, String stringToInsert, int spaces) {
		StringBuilder result = new StringBuilder();
		int i = 0;
		for (; i + spaces < baseString.length(); i += spaces) {
			result.append(baseString.substring(i, i + spaces) + stringToInsert);
		}
		result.append(baseString.substring(i, baseString.length()));
		return result.toString();
	}

	public static boolean contains(String referenceString, String queryString, int allowedMismatches) {
		boolean match = false;

		int startingReferenceIndex = 0;

		while (!match && startingReferenceIndex + queryString.length() <= referenceString.length()) {
			int queryIndex = 0;
			int mismatches = 0;
			boolean tooManyMisMatches = false;
			while (queryIndex < queryString.length() && !tooManyMisMatches) {
				if (queryString.charAt(queryIndex) != referenceString.charAt(startingReferenceIndex + queryIndex)) {
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

	/**
	 * Taken from herehttp://stackoverflow.com/questions/220547/printable-char-in-java
	 * 
	 * @param character
	 * @return
	 */

	public static boolean isPrintableChar(char character) {
		Character.UnicodeBlock block = Character.UnicodeBlock.of(character);
		return (!Character.isISOControl(character)) && character != KeyEvent.CHAR_UNDEFINED && block != null && block != Character.UnicodeBlock.SPECIALS;
	}

	public synchronized static String generateRandomString(int length) {
		char[] text = new char[length];
		for (int i = 0; i < length; i++) {
			text[i] = CHARACTERS[RANDOM.nextInt(CHARACTERS.length)];
		}
		return new String(text);

	}

	public static String replace(String originalString, int index, Character charToReplaceWith) {
		String replacedString = originalString.substring(0, index) + charToReplaceWith + originalString.substring(index + 1);
		return replacedString;
	}

	public static String reduceString(String string) {
		return reduceString(string, DEFAULT_MAX_LABEL_LENGTH_FOR_REDUCTION);
	}

	public static String reduceString(String string, int length) {
		String reducedString = string;
		if (string.length() > length) {
			reducedString = string.substring(0, length / 2) + "..." + string.substring(string.length() - length / 2);
		}
		return reducedString;
	}
}
