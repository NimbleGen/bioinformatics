package com.roche.sequencing.bioinformatics.common.utils;

public class NumberUtil {

	private static int MAX_UNSIGNED_SHORT_VALUE_AS_INT = 65535;

	private static int NUMBER_OF_BITS_IN_SHORT = 16;

	private NumberUtil() {
		throw new AssertionError();
	}

	public static short convertUnsignedShortValueAsIntToUnsignedShort(int unsignedShortAsInt) {
		if (unsignedShortAsInt < 0 || unsignedShortAsInt > MAX_UNSIGNED_SHORT_VALUE_AS_INT) {
			throw new IllegalArgumentException("The value of positiveSignedIntValue[" + unsignedShortAsInt + "] must be greater than or equal to zero and less than or equal to ["
					+ MAX_UNSIGNED_SHORT_VALUE_AS_INT + "].");
		}
		short unsignedShort = (short) (Math.pow(2, NUMBER_OF_BITS_IN_SHORT) + (short) unsignedShortAsInt);

		return unsignedShort;
	}

	private static int convertUnsignedShortToPositiveSignedInt(short unsignedShortValue) {
		int positiveSignedInt = (int) unsignedShortValue & 0xffff;
		return positiveSignedInt;
	}

	public static void main(String[] args) {
		int maxShort = (Short.MAX_VALUE + 1) * 2 - 1;
		short unsignedShort = convertUnsignedShortValueAsIntToUnsignedShort(maxShort);
		int value = convertUnsignedShortToPositiveSignedInt(unsignedShort);
		System.out.println(maxShort + "  " + unsignedShort + "  " + value);

		long maxInt = ((long) Integer.MAX_VALUE + 1) * 2 - 1;
		System.out.println(maxInt);
	}

	public static String getNumberSuffix(int number) {
		String suffix;
		int ones = number % 10;
		int tens = (number / 10) % 10;

		if (tens == 1) {
			suffix = "th";
		} else if (ones == 1) {
			suffix = "st";
		} else if (ones == 2) {
			suffix = "nd";
		} else if (ones == 3) {
			suffix = "rd";
		} else {
			suffix = "th";
		}
		return suffix;
	}

	public static String addSuffixToNumber(int number) {
		return number + getNumberSuffix(number);
	}

}
