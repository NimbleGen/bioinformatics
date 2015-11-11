/*
 *    Copyright 2013 Roche NimbleGen Inc.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class for converting roman numerals found at the end of strings to numbers. Any recognized roman numeral characters are case insensitive so "V" and "v" are both recognized as 5. The following roman
 * numeral values are used: i-1, v-5, x-10, l-50, c-100, d-500, m-1000. This does not search for
 * 
 */
public class RomanNumeralConverter {

	private static Map<Character, Integer> getRomanNumerals() {
		// this class has not been checked for thread safety
		ConcurrentHashMap<Character, Integer> rnValues = new ConcurrentHashMap<Character, Integer>();
		rnValues.put('i', 1);
		rnValues.put('v', 5);
		rnValues.put('x', 10);
		rnValues.put('l', 50);
		rnValues.put('c', 100);
		rnValues.put('d', 500);
		rnValues.put('m', 1000);
		return rnValues;
	}

	/**
	 * determines if a string contains a numerical digit
	 * 
	 * @param myString
	 * @return true if string contains digit
	 */
	public static boolean containsDigit(String myString) {
		boolean containsDigit = false;

		int i = 0;
		while (i < myString.length() && !containsDigit) {
			containsDigit = Character.isDigit(myString.charAt(i));
			i++;
		}

		return containsDigit;
	}

	/**
	 * determines if a roman numeral is present at the end of the given string.
	 * 
	 * @param myString
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
		String wholeRomanNumeral = "";

		// do not perform the check at all if it contains a digit
		boolean isRomanNumeral = !containsDigit(myString);
		int i = myString.length() - 1;
		while (i >= 0 && isRomanNumeral) {
			char curChar = myString.charAt(i);
			isRomanNumeral = isRomanNumeral(curChar);
			if (isRomanNumeral) {
				wholeRomanNumeral = curChar + wholeRomanNumeral;
			}

			i--;
		}

		// replace the roman numeral with the number it represents
		if (wholeRomanNumeral.length() > 0) {
			int numberValue = convertRomanNumeralStringToInt(wholeRomanNumeral);
			returnString = myString.substring(0, i + 2) + numberValue;
		}

		return returnString;
	}

	private static boolean isRomanNumeral(char c) {
		Map<Character, Integer> rnValues = getRomanNumerals();

		// make sure the letter is lowercase
		c = Character.toLowerCase(c);

		return rnValues.containsKey(c);
	}

	/**
	 * method for retrieving the value of the given roman numeral string. Will skip any non-roman numeral characters. So "VIHH" will only interpret the V and I since "H" is not a roman numeral, thus
	 * giving the value 6.
	 * 
	 * @param romanNumeralString
	 * @return int representing the value of the given roman numeral string
	 */
	private static int convertRomanNumeralStringToInt(String romanNumeralString) {
		int totalValue = 0;

		// make sure the string is in lower case
		romanNumeralString = romanNumeralString.toLowerCase();

		// create a hashmap of all the letters values
		Map<Character, Integer> rnValues = getRomanNumerals();

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

			if (rnValues.containsKey(currentChar)) {
				int currentValue = rnValues.get(currentChar);
				int nextValue = 0;
				if (rnValues.containsKey(nextChar)) {
					nextValue = rnValues.get(nextChar);
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
