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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NumberFormatterUtil {

	private NumberFormatterUtil() {
		throw new AssertionError();
	}

	/**
	 * Formats a double as a string with a default number of significant digits depending on the argument's value.
	 * 
	 * @param someDouble
	 *            double to render
	 * @param includeThousandsSeparator
	 *            Specify if the comma should be included for numbers like 10,234.56
	 * @return such a string
	 */
	public static String formatDouble(Double someDouble, boolean includeThousandsSeparator) {

		int numberOfDigitsPastTheDecimal = 4;

		// Reduce the number of digits depending on the value of the double
		if (Math.abs(someDouble) >= 100.0) {
			numberOfDigitsPastTheDecimal = 3;
		}
		if (Math.abs(someDouble) >= 1000.0) {
			numberOfDigitsPastTheDecimal = 2;
		}

		return formatDouble(someDouble, numberOfDigitsPastTheDecimal, includeThousandsSeparator);
	}

	/**
	 * Formats a double as a string with the specified number of fractional digits.
	 * 
	 * @param someDouble
	 *            double to render
	 * @param fractionCount
	 *            Number of Fraction Digits
	 * @param includeThousandsSeparator
	 *            Specify if the comma should be included for numbers like 10,234.56
	 * 
	 * @return such a string
	 */
	public static String formatDouble(Double someDouble, int fractionCount, boolean includeThousandsSeparator) {
		NumberFormat numberFormat = NumberFormat.getInstance();
		numberFormat.setMinimumFractionDigits(fractionCount);
		numberFormat.setMaximumFractionDigits(fractionCount);
		numberFormat.setGroupingUsed(includeThousandsSeparator);
		String result = numberFormat.format(someDouble);
		return result;
	}

	/**
	 * Formats a double as a string with the specified number of fractional digits. Includes the thousands separator.
	 * 
	 * @param someDouble
	 *            double to render
	 * @param fractionCount
	 *            Number of Fraction Digits
	 * @return such a string
	 */
	public static String formatDouble(Double someDouble, int fractionCount) {
		return formatDouble(someDouble, fractionCount, true);
	}

	public static String addCommas(int value) {
		return NumberFormat.getNumberInstance(Locale.US).format(value);
	}

	/**
	 * Return a list of strings were adjacent values are clumped together. So 1,2,3,5,6,7,8,10 would be expressed as 1-3, 5-8, 10
	 * 
	 * @param numbers
	 * @return
	 */
	public static String summarizeNumbersAsString(int[] numbers) {
		// filter out duplicates
		Set<Integer> set = new HashSet<Integer>();
		for (int number : numbers) {
			set.add(number);
		}
		List<Integer> sortedNumbers = new ArrayList<Integer>(set);
		Collections.sort(sortedNumbers);

		StringBuilder summaryBuilder = new StringBuilder();

		Integer currentRunMin = null;
		Integer lastNumber = null;
		for (int number : sortedNumbers) {
			if (currentRunMin == null) {
				currentRunMin = number;
			} else if (number != lastNumber + 1) {
				if (currentRunMin == lastNumber) {
					summaryBuilder.append(wrapNegativeNumbers(currentRunMin) + ", ");
				} else {
					summaryBuilder.append(wrapNegativeNumbers(currentRunMin) + "-" + wrapNegativeNumbers(lastNumber) + ", ");
				}
				currentRunMin = null;
			}
			lastNumber = number;
		}

		if (currentRunMin == null) {
			summaryBuilder.append(wrapNegativeNumbers(lastNumber));
		} else {
			summaryBuilder.append(wrapNegativeNumbers(currentRunMin) + "-" + wrapNegativeNumbers(lastNumber) + ", ");
		}

		return summaryBuilder.toString();
	}

	private static String wrapNegativeNumbers(int number) {
		String valueAsString = "" + number;
		if (number < 0) {
			valueAsString = "(" + number + ")";
		}
		return valueAsString;
	}

	public static void main(String[] args) {
		System.out.println(summarizeNumbersAsString(new int[] { -5, -4, -3, 9, 0, 100, 1, 2, 3, 5, 6, 7, 8, 10 }));
	}
}
