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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonMathUtil {

	private static final Logger logger = LoggerFactory.getLogger(CommonMathUtil.class);

	private static final int MAX_FRACTIONAL_DIGITS = 10;
	private static final int MAX_FRACTIONAL_FLOAT_DIGITS = 6;

	private CommonMathUtil() {
		throw new AssertionError();
	}

	/**
	 * Returns a double with rounded to the number of fractional digits given. round(1.234,1) would return 1.2 round(1.234,2) would return 1.23
	 * 
	 * @param valueToRound
	 * @param fractionalDigits
	 *            (must be less than or equal to 10)
	 * @return rounded number
	 */
	public static double round(double valueToRound, int fractionalDigits) {
		if (fractionalDigits > MAX_FRACTIONAL_DIGITS) {
			IllegalStateException ise = new IllegalStateException("fractionalDigits must be less than or equal to " + MAX_FRACTIONAL_DIGITS + ".  Value was " + fractionalDigits + ".");
			logger.warn(ise.getMessage(), ise);
			throw ise;
		}
		double adjuster = Math.pow(10, fractionalDigits);
		double adjustedValueToRound = valueToRound * adjuster;
		double adjustedRoundedValue = Math.round(adjustedValueToRound);
		double roundedValue = adjustedRoundedValue / adjuster;
		return roundedValue;
	}

	/**
	 * Returns the total number of meaningful digits are in this value. getDigits(123.456) will return 6. getDigits(123.456000) will also return 6.
	 * 
	 * @param value
	 * @return
	 */
	public static int getDigits(double value) {
		String stringValue = "" + value;
		// subtract one for '.'
		int digits = 0;
		if (stringValue.endsWith(".0")) {
			digits = stringValue.length() - 2;
		} else {
			digits = stringValue.length() - 1;
		}
		return digits;
	}

	/**
	 * Returns the total number of meaningful digits are to the right of the decimal point (fractional digits). getFractionalDigits(123.456) will return 3. getFractionalDigits(123.4560) will also
	 * return 3.
	 * 
	 * @param value
	 * @return
	 */
	public static int getFractionalDigits(float value) {
		double newValue = round(value, MAX_FRACTIONAL_FLOAT_DIGITS);
		int fractionalDigits = getFractionalDigits(newValue);
		return fractionalDigits;
	}

	/**
	 * Returns the total number of meaningful digits are to the right of the decimal point (fractional digits). getFractionalDigits(123.456) will return 3. getFractionalDigits(123.456000) will also
	 * return 3.
	 * 
	 * @param value
	 * @return
	 */
	public static int getFractionalDigits(double value) {
		boolean fractionalDigitsFound = false;
		int maxFractionalDigits = MAX_FRACTIONAL_DIGITS;// getDigits(Double.
														// MIN_NORMAL);
		int fractionalDigits = -1;
		while (!fractionalDigitsFound && fractionalDigits < maxFractionalDigits) {
			fractionalDigits++;
			fractionalDigitsFound = (value == round(value, fractionalDigits));
		}
		return fractionalDigits;
	}

	/**
	 * computes the least common multiplier recursively using the Euclidean Algorithm
	 * 
	 * @param first
	 * @param second
	 * @return
	 */
	public static long getGreatestCommonDivisor(long first, long second) {
		long result = 0;
		long smaller = Math.min(first, second);
		long larger = Math.max(first, second);

		long newSmaller = larger % smaller;

		if (newSmaller == 0) {
			result = smaller;
		} else {
			result = getGreatestCommonDivisor(newSmaller, smaller);
		}

		return result;
	}

	/**
	 * returns the least common denominator of the two given numbers
	 * 
	 * @param first
	 * @param second
	 * @return least common denominator
	 */
	public static long getLeastCommonDenominator(long first, long second) {
		long smaller = Math.min(first, second);
		long larger = Math.max(first, second);

		long result = larger;
		while (!(result % smaller == 0) && result <= (larger * smaller)) {
			result += larger;
		}
		return result;
	}

	public static boolean equals(double first, double second, int fractionalDigitsToCompare) {
		double buffer = Math.pow(0.1, fractionalDigitsToCompare);
		boolean equals = (second < (first + buffer)) && second > (first - buffer);
		return equals;
	}
}
