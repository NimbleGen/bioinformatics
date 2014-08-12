package com.roche.sequencing.bioinformatics.common.utils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

/**
 * This is a comparator for sorting strings based on alphanumeric sorting. All consecutive numeric digits are treated as one character which can then be compared against other numeric digits. Negative
 * numbers are not handled. Roman numerals present at the end of the string can be compared if true for the convertRomanNumeralsAtEndOfString flag in the constructor.
 * 
 * 
 * 
 * Note: this implementation may need to be changed to Comparator<? super String> to handle super types such as {@link java.lang.CharSequence}
 */
public class AlphaNumericStringComparator implements Comparator<String>, Serializable {

	private static final long serialVersionUID = 1L;

	private static int LHS_GreaterThan_RHS = 1;
	private static int LHS_LessThan_RHS = -1;
	private static int LHS_EQUALS_RHS = 0;

	/**
	 * Number of names ending in "non roman numerals", below which all names will be passed through a roman-numeral -> digital conversion attempt before sorting.
	 */
	private static int NON_ROMAN_NUMERAL_THRESHOLD = 5;

	private boolean convertRomanNumeralsAtEndOfString;

	public AlphaNumericStringComparator() {
		this(false);
	}

	public AlphaNumericStringComparator(List<String> listOfItemsToUseToDetermineComparisonUsingRomanNumeralConversion) {
		this(shouldConvertRomanNumerals(listOfItemsToUseToDetermineComparisonUsingRomanNumeralConversion));
	}

	public AlphaNumericStringComparator(boolean convertRomanNumeralsAtEndOfString) {
		super();
		this.convertRomanNumeralsAtEndOfString = convertRomanNumeralsAtEndOfString;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compare(String stringOne, String stringTwo) {
		return compare(stringOne, stringTwo, convertRomanNumeralsAtEndOfString);
	}

	private int compare(String stringOne, String stringTwo, boolean convertRomanNumerals) {
		stringOne = stringOne.toLowerCase();
		stringTwo = stringTwo.toLowerCase();

		// 1 is object1 is greater
		// -1 is object2 is greater
		// 0 is they are the same
		int returnValue = LHS_EQUALS_RHS;

		// keep track if stringOne and stringTwo are roman numerals so we can
		// choose if roman
		// numerals come before regular numbers
		boolean oneIsRomanNumeral = false;
		boolean twoIsRomanNumeral = false;
		if (convertRomanNumerals) {
			String newStringOne = RomanNumeralConverterUtil.convertRomanNumeralAtEndOfStringToNumber(stringOne);
			oneIsRomanNumeral = !stringOne.equals(newStringOne);
			stringOne = newStringOne;
			String newStringTwo = RomanNumeralConverterUtil.convertRomanNumeralAtEndOfStringToNumber(stringTwo);
			twoIsRomanNumeral = !stringTwo.equals(newStringTwo);
			stringTwo = newStringTwo;

		}

		// walk through both strings
		int indexStringOne = 0;
		int indexStringTwo = 0;

		// while there are still characters to still process in both strings and
		// the strings are still equal
		while (indexStringOne < stringOne.length() && indexStringTwo < stringTwo.length() && returnValue == LHS_EQUALS_RHS) {

			char charOne = stringOne.charAt(indexStringOne);
			char charTwo = stringTwo.charAt(indexStringTwo);

			// suck in the whole char one number
			// if (Character.isDigit(charOne)){
			String charOneDigits = "";// +charOne;
			while (Character.isDigit(charOne)) {
				charOneDigits += charOne;
				indexStringOne++;
				if (indexStringOne < stringOne.length()) {
					charOne = stringOne.charAt(indexStringOne);
				} else {
					charOne = ' ';
				}

			}
			int numberOne = -1;
			if (charOneDigits.length() > 0) {
				numberOne = Integer.parseInt(charOneDigits);
			}
			// }

			String charTwoDigits = "";
			// suck in the whole char two number
			while (Character.isDigit(charTwo)) {
				charTwoDigits += charTwo;
				indexStringTwo++;
				if (indexStringTwo < stringTwo.length()) {
					charTwo = stringTwo.charAt(indexStringTwo);
				} else {
					charTwo = ' ';
				}
			}

			int numberTwo = -1;
			if (charTwoDigits.length() > 0) {
				numberTwo = Integer.parseInt(charTwoDigits);
			}

			if (numberOne >= 0 && numberTwo == -1) {
				// only stringOne has a number
				returnValue = LHS_LessThan_RHS;
			} else if (numberOne == -1 && numberTwo >= 0) {
				// only stringTwo has a number
				returnValue = LHS_GreaterThan_RHS;
			} else if (numberOne >= 0 && numberTwo >= 0) {
				// both stringOne and stringTwo have numbers
				if (numberOne > numberTwo) {
					returnValue = LHS_GreaterThan_RHS;
				} else if (numberOne < numberTwo) {
					returnValue = LHS_LessThan_RHS;
				} else {
					// the numbers are the same so decrement the counters
					// which needed to look at the next letter to see if it was
					// a digit
					indexStringOne--;
					indexStringTwo--;
				}
			} else {
				// just looking at characters
				char lowerCaseCharOne = Character.toLowerCase(charOne);
				char lowerCaseCharTwo = Character.toLowerCase(charTwo);

				if (lowerCaseCharOne == lowerCaseCharTwo) {
					boolean charOneIsLower = lowerCaseCharOne == charOne;
					boolean charTwoIsLower = lowerCaseCharTwo == charTwo;
					if (charOneIsLower && !charTwoIsLower) {
						returnValue = LHS_GreaterThan_RHS;
					} else if (!charOneIsLower && charTwoIsLower) {
						returnValue = LHS_LessThan_RHS;
					}
				} else if (lowerCaseCharOne > lowerCaseCharTwo) {
					returnValue = LHS_GreaterThan_RHS;
				} else if (lowerCaseCharOne < lowerCaseCharTwo) {
					returnValue = LHS_LessThan_RHS;
				}
			}

			indexStringOne++;
			indexStringTwo++;
		}

		// if the strings are equals and there are no longer any strings to
		// process then the longer string is greater
		if (returnValue == LHS_EQUALS_RHS) {
			if (stringOne.length() > stringTwo.length()) {
				returnValue = LHS_GreaterThan_RHS;
			} else if (stringOne.length() < stringTwo.length()) {
				returnValue = LHS_LessThan_RHS;
			} else {
				// they are the same size so
				// check if they are roman numerals
				if (oneIsRomanNumeral && !twoIsRomanNumeral) {
					returnValue = LHS_GreaterThan_RHS;
				} else if (twoIsRomanNumeral && !oneIsRomanNumeral) {
					returnValue = LHS_LessThan_RHS;
				} else if (twoIsRomanNumeral && oneIsRomanNumeral && convertRomanNumerals) {
					// both are roman numerals so check the case
					// that one is in upper case and one is in lower case by
					// comparing them as non-roman numerals
					returnValue = compare(stringOne, stringTwo, false);
				}
			}
		}

		// otherwise they will be equal

		return returnValue;
	}

	/**
	 * Method to help determine if RomanNumeral comparison is needed on the given objects based on number of string that end in Roman numerals. This is a purely heuristic method.
	 * 
	 * @param names
	 *            names to be analyzed for appropriateness of roman numeral conversion attempts
	 * @return true if Roman numerals should be converted to numbers
	 */
	public static boolean shouldConvertRomanNumerals(List<String> names) {
		return !shouldNotConvertRomanNumerals(names);
	}

	/**
	 * Method to help determine if RomanNumeral comparison is not needed on the given objects based on number of string that end in Roman numerals. This is a purely heuristic method.
	 * 
	 * @param names
	 *            names to be analyzed for appropriateness of roman numeral conversion attempts
	 * @return true if Roman numerals should not be converted to numbers
	 */
	public static boolean shouldNotConvertRomanNumerals(List<String> names) {
		// the problem is that some chromosomes are labeled with
		// Roman numerals (ex. chrxi). ordering differs whether chrx
		// is treated as a roman numeral or the sex chromosome
		// to overcome this we can walk through all the labels,
		// and count the number of labels that contain Roman numeral
		// numbering
		int nonRomanNumeralCount = 0;
		for (String name : names) {
			if (RomanNumeralConverterUtil.containsDigit(name)) {
				nonRomanNumeralCount++;
			} else if (!RomanNumeralConverterUtil.containsRomanNumeralAtEndOfString(name)) {
				nonRomanNumeralCount++;
			}
		}

		// XXX (Jaz) logic is strange. I think we will need to reevaluate in
		// 1.1. Probably should be something like:
		// return nonRomanNumeralCount < (names.size() -
		// NON_ROMAN_NUMERAL_THRESHOLD); Will need to think about when names has
		// only got a few entries.
		return nonRomanNumeralCount > NON_ROMAN_NUMERAL_THRESHOLD;

	}

}
