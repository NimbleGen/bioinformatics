package com.roche.sequencing.bioinformatics.common.utils;

import java.text.NumberFormat;
import java.text.ParseException;

public class NumberFormatUtil {
	private static final String NEWLINE = System.getProperty("line.separator");
	private static final String SPACE_STRING = " ";

	/**
	 * Formats a Integer as a string .
	 * 
	 * @param someInteger
	 *            Integer to render
	 * @return such a string
	 */
	public static String formatInteger(Integer someInteger) {
		return formatInt(someInteger);
	}

	/**
	 * Formats a int as a string with separators.
	 * 
	 * @param someInt
	 *            int to render
	 * @return such a string
	 */
	public static String formatInt(Integer someInt) {
		NumberFormat numberFormat = NumberFormat.getInstance();
		String result = numberFormat.format(someInt);
		return result;
	}

	/**
	 * Formats a long as a string with separators.
	 * 
	 * @param someLong
	 *            long to render
	 * @return such a string
	 */
	public static String formatLong(Long someLong) {
		NumberFormat numberFormat = NumberFormat.getInstance();
		String result = numberFormat.format(someLong);
		return result;
	}

	/**
	 * Round time in Milliseconds to the nearest second, and return as time in milliseconds
	 * 
	 * @param timeInMilliseconds
	 * @return roundedTimeInMilliSeconds rounded to the nearest second.
	 */
	public static long roundTimeToTheNearestSecondInMilliseconds(long timeInMilliseconds) {
		double timeInSeconds = ((double) timeInMilliseconds) / 1000.0;
		timeInSeconds = Math.round(timeInSeconds);
		long roundedTimeInMilliseconds = (long) (timeInSeconds * 1000.0);
		return roundedTimeInMilliseconds;
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

	public static float parseFloat(String stringValue) throws ParseException {
		return parseNumber(stringValue).floatValue();
	}

	public static double parseDouble(String stringValue) throws ParseException {
		return parseNumber(stringValue).doubleValue();
	}

	public static int parseInt(String stringValue) throws ParseException {
		return parseNumber(stringValue).intValue();
	}

	public static long parseLong(String stringValue) throws ParseException {
		return parseNumber(stringValue).longValue();
	}

	public static Number parseNumber(String stringValue) throws ParseException {
		NumberFormat numberFormat = NumberFormat.getInstance();
		return numberFormat.parse(stringValue);
	}

	/**
	 * Wrap string to ensure that desired maximum number of chars on a line , up to the maximum number of lines. After maximum number of lines has been reached, then append on that last line, meaning
	 * that the last line could be longer than the desiredMaximumCharactersPerLine Also - If the stringToWrap has a string longer than the desiredMaximumCharactersPerLine, it will not be truncated.
	 * 
	 * @param stringToWrap
	 * @param desiredMaximumCharactersPerLine
	 * @param maximumNumberOfLines
	 * @param alwaysHaveDesiredMaximumNumberOfLines
	 *            is true if the desire is to always have maximum number of lines.
	 * @return word wrapped string with up to the desired maximum number of lines.
	 */
	public static String wordWrapString(String stringToWrap, final int desiredMaximumCharactersPerLine, final int maximumNumberOfLines, final boolean alwaysHaveDesiredMaximumNumberOfLines) {
		return wordWrapString(stringToWrap, desiredMaximumCharactersPerLine, maximumNumberOfLines, alwaysHaveDesiredMaximumNumberOfLines, NEWLINE);
	}

	/**
	 * Wrap string to ensure that desired maximum number of chars on a line , up to the maximum number of lines. After maximum number of lines has been reached, then append on that last line, meaning
	 * that the last line could be longer than the desiredMaximumCharactersPerLine Also - If the stringToWrap has a string longer than the desiredMaximumCharactersPerLine, it will not be truncated.
	 * 
	 * @param stringToWrap
	 * @param desiredMaximumCharactersPerLine
	 * @param maximumNumberOfLines
	 * @param alwaysHaveDesiredMaximumNumberOfLines
	 *            is true if the desire is to always have maximum number of lines.
	 * @param lineBreakString
	 *            the string to use to word wrap (Usually NEWLINE)
	 * @return word wrapped string with up to the desired maximum number of lines.
	 */
	public static String wordWrapString(String stringToWrap, final int desiredMaximumCharactersPerLine, final int maximumNumberOfLines, final boolean alwaysHaveDesiredMaximumNumberOfLines,
			String lineBreakString) {
		String[] stringsToWrap = stringToWrap.split(SPACE_STRING);
		StringBuilder stringToReturn = new StringBuilder();
		int charactersInCurrentLine = 0;
		int numberOfLines = 1;
		int stringCount = 0;
		for (String aString : stringsToWrap) {
			int stringLength = aString.length();
			if (charactersInCurrentLine != 0) {
				if (numberOfLines < maximumNumberOfLines) {
					if (charactersInCurrentLine + stringLength > desiredMaximumCharactersPerLine) {
						stringToReturn.append(lineBreakString);
						numberOfLines++;
						charactersInCurrentLine = 0;
					}
				}
			}
			stringToReturn.append(aString);
			charactersInCurrentLine += stringLength;
			stringCount++;

			if (stringCount != stringsToWrap.length) {
				// Do not add space after last string
				stringToReturn.append(SPACE_STRING);
				charactersInCurrentLine += SPACE_STRING.length();
			}
		}
		if (alwaysHaveDesiredMaximumNumberOfLines) {
			for (int i = numberOfLines; i < maximumNumberOfLines; i++) {
				stringToReturn.append(lineBreakString);
			}
		}
		return stringToReturn.toString();
	}

	/**
	 * Wrap string to ensure that desired maximum number of chars on a line , up to the maximum number of lines. After maximum number of lines has been reached, then append on that last line, meaning
	 * that the last line could be longer than the desiredMaximumCharactersPerLine Also - If the stringToWrap has a string longer than the desiredMaximumCharactersPerLine, it will not be truncated.
	 * The returned string will be wrapped with <HTML> and <font> tag, and will have lines broken with <br>
	 * 
	 * @param stringToWrap
	 * @param desiredMaximumCharactersPerLine
	 * @param maximumNumberOfLines
	 * @param alwaysHaveDesiredMaximumNumberOfLines
	 *            is true if the desire is to always have maximum number of lines.
	 * @return word wrapped string with up to the desired maximum number of lines.
	 */
	public static String wordWrapStringIntoHTML(String stringToWrap, final int desiredMaximumCharactersPerLine, final int maximumNumberOfLines, final boolean alwaysHaveDesiredMaximumNumberOfLines) {
		stringToWrap = "<html><font face='arial' size='4'>" + wordWrapString(stringToWrap, desiredMaximumCharactersPerLine, maximumNumberOfLines, alwaysHaveDesiredMaximumNumberOfLines, "<br>")
				+ "</font></html>";
		return stringToWrap;
	}
}
