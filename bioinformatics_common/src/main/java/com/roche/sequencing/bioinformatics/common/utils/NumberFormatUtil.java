package com.roche.sequencing.bioinformatics.common.utils;

import java.text.NumberFormat;

public class NumberFormatUtil {

	private NumberFormatUtil() {
		throw new AssertionError();
	}

	/**
	 * Formats a double as a string with a default number of significant digits depending on the argument's value. Does not include the thousands separator.
	 * 
	 * @param someDouble
	 *            double to render
	 * @return such a string
	 */
	public static String formatDouble(Double someDouble) {
		return formatDouble(someDouble, false);
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
	 * Formats a float as a string with a default number of significant digits depending on the argument's value. Does not include the thousands separator.
	 * 
	 * @param someFloat
	 *            float to render
	 * @return such a string
	 */
	public static String formatFloat(Float someFloat) {
		return formatDouble(someFloat.doubleValue());
	}

	/**
	 * Formats a float as a string with a default number of significant digits depending on the argument's value.
	 * 
	 * @param someFloat
	 *            float to render
	 * @param includeThousandsSeparator
	 *            Specify if the comma should be included for numbers like 10,234.56
	 * @return such a string
	 */
	public static String formatFloat(Float someFloat, boolean includeThousandsSeparator) {
		return formatDouble(someFloat.doubleValue(), includeThousandsSeparator);
	}

	/**
	 * Formats a float as a string with the specified number of fractional digits. Includes the thousands separator.
	 * 
	 * @param someFloat
	 *            float to render
	 * @param fractionCount
	 *            Number of Fraction Digits
	 * @return such a string
	 */
	public static String formatFloat(Float someFloat, int fractionCount) {
		return formatDouble(someFloat.doubleValue(), fractionCount);
	}

}
