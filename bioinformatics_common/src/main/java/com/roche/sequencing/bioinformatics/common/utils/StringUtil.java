/**
 *   Copyright 2013 Roche NimbleGen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.roche.sequencing.bioinformatics.common.utils;

/**
 * Utility class for working with Strings
 * 
 */
public final class StringUtil {
	public static final String NEWLINE = System.getProperty("line.separator");

	public static final String TAB = "\t";
	public static final String CARRIAGE_RETURN = "\r";
	public static final String NEWLINE_SYMBOL = "\n";
	// NOTE: WINDOWS_NEWLINE = CARRIAGE_RETURN + LINE_FEED;
	// NOTE: LINUX_NEWLINE = LINE_FEED;
	public static final String LINUX_NEWLINE = NEWLINE_SYMBOL;
	public static final String WINDOWS_NEWLINE = CARRIAGE_RETURN + LINUX_NEWLINE;

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
	public static int nthOccurrence(String string, char character, int indexOfDesiredOccurence) {
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
	 * insert a string into the base string at intervals of spaces For example: insertStringEveryNSpaces("abcdefghijk","z",2) would return "abzcdzefzghzijzk"
	 * 
	 * @param baseString
	 *            string that will be manipulated
	 * @param stringToInsert
	 * @param spaces
	 *            number of spaces to place in between inserts
	 * @return altered string
	 */
	public static String insertStringEveryNSpaces(String baseString, String stringToInsert, int spaces) {
		StringBuilder result = new StringBuilder();
		int i = 0;
		for (; i + spaces < baseString.length(); i += spaces) {
			result.append(baseString.substring(i, i + spaces) + stringToInsert);
		}
		result.append(baseString.substring(i, baseString.length()));
		return result.toString();
	}
}
