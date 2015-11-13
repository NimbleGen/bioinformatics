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

import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Class for converting roman numerals found at the end of strings to numbers. Any recognized roman numeral characters are case insensitive so "V" and "v" are both recognized as 5. The following roman
 * numeral values are used: i-1, v-5, x-10, l-50, c-100, d-500, m-1000. This does not search for
 *
 */
public class RomanNumeralConverterUtil {
	private static final RomanNumeralValues ROMAN_NUMERAL_VALUES = new RomanNumeralValues();

	private static class RomanNumeralValues extends HashMap<Character, Integer> {
		private static final long serialVersionUID = 1L;

		private RomanNumeralValues() {
			super();
			// This map is intended to be instantiated only once as a singleton
			// static variable.
			// By adding lower and upper case to the hash map, we eliminate the
			// need to lower case the character.
			put('i', 1);
			put('I', 1);
			put('v', 5);
			put('V', 5);
			put('x', 10);
			put('X', 10);
			put('l', 50);
			put('L', 50);
			put('c', 100);
			put('C', 100);
			put('d', 500);
			put('D', 500);
			put('m', 1000);
			put('M', 1000);
		}
	}

	private static final Pattern ANY_DIGITS = Pattern.compile(".*[0-9]{1,}.*");

	private RomanNumeralConverterUtil() {
		throw new AssertionError();
	}

	/**
	 * determines if a string contains a numerical digit
	 *
	 * @param myString
	 *            string to check for digits
	 * @return true if string contains digit
	 */
	public static boolean containsDigit(String myString) {
		return ANY_DIGITS.matcher(myString).matches();
	}

	/**
	 * determines if a roman numeral is present at the end of the given string.
	 *
	 * @param myString
	 *            string to check for roman numerals
	 * @return true if string is terminated by a roman numeral
	 */
	public static boolean containsRomanNumeralAtEndOfString(String myString) {
		return (!myString.equals(convertRomanNumeralAtEndOfStringToNumber(myString)));
	}

	/**
	 * converts a roman numeral found at the end of a string to a number
	 *
	 * @param myString
	 *            whole string including roman numeral at the end e.g. "chrIV"
	 * @return new string which has the roman numeral at the end of a string converted to a number e.g. "chr4"
	 */
	public static String convertRomanNumeralAtEndOfStringToNumber(String myString) {
		String returnString = myString;
		// Roman Numerals are only counted if they come at the end of the string
		// and there are no integers in the string

		// walk backwards through the string looking for roman numerals
		StringBuilder wholeRomanNumeral = new StringBuilder();

		// do not perform the check at all if it contains a digit
		boolean isRomanNumeral = !containsDigit(myString);
		if (isRomanNumeral) {
			int i = myString.length() - 1;
			while (i >= 0 && isRomanNumeral) {
				char curChar = myString.charAt(i);
				isRomanNumeral = isRomanNumeral(curChar);
				if (isRomanNumeral) {
					wholeRomanNumeral.insert(0, curChar);
				}

				i--;
			}

			// replace the roman numeral with the number it represents
			if (wholeRomanNumeral.length() > 0) {
				int numberValue = convertRomanNumeralStringToInt(wholeRomanNumeral.toString());
				returnString = myString.substring(0, i + 2) + numberValue;
			}
		}
		return returnString;
	}

	private static boolean isRomanNumeral(char c) {
		return ROMAN_NUMERAL_VALUES.containsKey(c);
	}

	/**
	 * method for retrieving the value of the given roman numeral string. Will skip any non-roman numeral characters. So "VIHH" will only interpret the V and I since "H" is not a roman numeral, thus
	 * giving the value 6.
	 *
	 * @param romanNumeralString
	 *            roman numeral-containing string
	 * @return int representing the value of the given roman numeral string
	 */
	private static int convertRomanNumeralStringToInt(String romanNumeralString) {
		int totalValue = 0;

		// walk through the string character at a time
		// if a letters numeric value is smaller than the next numbers
		// numeric value, subtract the first number from the total, otherwise
		// add the first number to the total
		for (int i = 0; i < romanNumeralString.length(); i++) {
			char currentChar = romanNumeralString.charAt(i);

			char nextChar = ' ';
			if ((i + 1) < romanNumeralString.length()) {
				nextChar = romanNumeralString.charAt(i + 1);
			}

			if (ROMAN_NUMERAL_VALUES.containsKey(currentChar)) {
				int currentValue = ROMAN_NUMERAL_VALUES.get(currentChar);
				int nextValue = 0;
				if (ROMAN_NUMERAL_VALUES.containsKey(nextChar)) {
					nextValue = ROMAN_NUMERAL_VALUES.get(nextChar);
				}

				if (currentValue < nextValue) {
					// the letters value is smaller so subtract it from the
					// total
					totalValue -= currentValue;
				} else {
					// the letters value is larger or the same so add it to the
					// total
					totalValue += currentValue;
				}

			}

		}

		return totalValue;
	}

}
