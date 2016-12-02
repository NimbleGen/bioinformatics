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

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Util for working with primitive arrays and the likeness
 * 
 */
public final class ArraysUtil {
	private ArraysUtil() {
		throw new AssertionError();
	}

	/**
	 * @param values
	 * @return the max of all provided values
	 */
	public static double max(double... values) {
		double max = -Double.MAX_VALUE;

		for (double value : values) {
			if (value > max) {
				max = value;
			}
		}

		return max;
	}

	/**
	 * @param values
	 * @return the max of all provided values
	 */
	public static long max(long... values) {
		long max = -Long.MAX_VALUE;

		for (long value : values) {
			if (value > max) {
				max = value;
			}
		}

		return max;
	}

	/**
	 * @param values
	 * @return the max of all provided values
	 */
	public static int max(int... values) {
		int max = Integer.MIN_VALUE;

		for (int value : values) {
			if (value > max) {
				max = value;
			}
		}

		return max;
	}

	/**
	 * @param values
	 * @return the max of all provided values
	 */
	public static int max(int[]... values) {
		int max = Integer.MIN_VALUE;

		for (int[] rows : values) {
			for (int value : rows) {
				if (value > max) {
					max = value;
				}
			}
		}

		return max;
	}

	/**
	 * @param values
	 * @return the min of all provided values
	 */
	public static int min(int... values) {
		int min = Integer.MAX_VALUE;

		for (int value : values) {
			if (value < min) {
				min = value;
			}
		}

		return min;
	}

	/**
	 * @param values
	 * @return the min of all provided values
	 */
	public static double min(double... values) {
		double min = Integer.MAX_VALUE;

		for (double value : values) {
			if (value < min) {
				min = value;
			}
		}

		return min;
	}

	public static double min(List<? extends Number> values) {
		double min = Integer.MAX_VALUE;

		for (Number value : values) {
			if (value.doubleValue() < min) {
				min = value.doubleValue();
			}
		}

		return min;
	}

	public static double max(List<? extends Number> values) {
		double max = Integer.MIN_VALUE;

		for (Number value : values) {
			if (value.doubleValue() > max) {
				max = value.doubleValue();
			}
		}

		return max;
	}

	public static double[][] convertToDoubleArray(int[][] values) {
		double[][] doubleArray = new double[values.length][values[0].length];
		for (int i = 0; i < values.length; i++) {
			for (int j = 0; j < values[0].length; j++) {
				doubleArray[i][j] = values[i][j];
			}
		}
		return doubleArray;
	}

	public static double[] convertToDoubleArray(int[] values) {
		double[] doubleArray = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			doubleArray[i] = values[i];
		}
		return doubleArray;
	}

	public static double[] convertToDoubleArray(short[] values) {
		double[] doubleArray = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			doubleArray[i] = values[i];
		}
		return doubleArray;
	}

	public static int[] convertToIntArray(List<? extends Number> values) {
		int[] intArray = new int[values.size()];
		for (int i = 0; i < values.size(); i++) {
			intArray[i] = values.get(i).intValue();
		}
		return intArray;
	}

	public static double[] convertToDoubleArray(List<? extends Number> values) {
		double[] doubleArray = new double[values.size()];
		for (int i = 0; i < values.size(); i++) {
			doubleArray[i] = values.get(i).doubleValue();
		}
		return doubleArray;
	}

	public static long[] convertToLongArray(List<? extends Number> values) {
		long[] longArray = new long[values.size()];
		for (int i = 0; i < values.size(); i++) {
			longArray[i] = values.get(i).longValue();
		}
		return longArray;
	}

	public static double[] convertToDoubleArray(Integer[] values) {
		double[] doubleArray = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			doubleArray[i] = values[i];
		}
		return doubleArray;
	}

	public static double[] createDoubleArray(int length, double defaultValue) {
		double[] newArray = new double[length];
		for (int i = 0; i < length; i++) {
			newArray[i] = defaultValue;
		}
		return newArray;
	}

	public static int[] createIntArray(int length, int defaultValue) {
		int[] newArray = new int[length];
		for (int i = 0; i < length; i++) {
			newArray[i] = defaultValue;
		}
		return newArray;
	}

	public static int[] createIncrementingArray(int start, int end, int increment) {
		int size = (end - start + 1) / increment;
		int[] array = new int[size];
		for (int i = 0; i < size; i++) {
			array[i] = start + (i * increment);
		}
		return array;
	}

	public static Integer[] convertFromPrimitiveToObject(int[] intArray) {
		Integer[] integerArray = new Integer[intArray.length];
		for (int i = 0; i < intArray.length; i++) {
			integerArray[i] = intArray[i];
		}
		return integerArray;
	}

	public static <T> T[] concatenate(T[] a, T[] b) {
		int aLen = a.length;
		int bLen = b.length;

		@SuppressWarnings("unchecked")
		T[] combinedArray = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
		System.arraycopy(a, 0, combinedArray, 0, aLen);
		System.arraycopy(b, 0, combinedArray, aLen, bLen);

		return combinedArray;
	}

	public static byte[] concatenate(byte[] a, byte[] b) {
		int aLen = a.length;
		int bLen = b.length;

		byte[] combinedArray = (byte[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
		System.arraycopy(a, 0, combinedArray, 0, aLen);
		System.arraycopy(b, 0, combinedArray, aLen, bLen);

		return combinedArray;
	}

	public static int[] concatenate(int[] a, int[] b) {
		int aLen = a.length;
		int bLen = b.length;

		int[] combinedArray = (int[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
		System.arraycopy(a, 0, combinedArray, 0, aLen);
		System.arraycopy(b, 0, combinedArray, aLen, bLen);

		return combinedArray;
	}

	public static <T> String toString(T[] strings, String delimiter) {
		String returnString = "";

		if (strings != null && strings.length > 0) {
			StringBuilder returnStringBuilder = new StringBuilder();
			for (Object string : strings) {
				returnStringBuilder.append(string + delimiter);
			}
			returnString = returnStringBuilder.substring(0, returnStringBuilder.length() - delimiter.length());
		}
		return returnString;
	}

	public static <T> String toString(T[] strings) {
		return toString(strings, ", ");
	}

	public static String toString(int[] numbers) {
		return toString(numbers, ", ");
	}

	public static String toString(short[] numbers) {
		return toString(numbers, ", ");
	}

	public static String toString(long[] numbers) {
		return toString(numbers, ", ");
	}

	public static String toString(long[] numbers, DecimalFormat formatter) {
		return toString(numbers, ", ", formatter);
	}

	public static String toString(double[] numbers) {
		return toString(numbers, ", ");
	}

	public static String toString(double[] numbers, DecimalFormat formatter) {
		return toString(numbers, ", ", formatter);
	}

	public static String toString(int[] numbers, String delimiter) {
		String returnString = "";

		if (numbers != null && numbers.length > 0) {
			StringBuilder returnStringBuilder = new StringBuilder();
			for (int i : numbers) {
				returnStringBuilder.append(i + delimiter);
			}
			returnString = returnStringBuilder.substring(0, returnStringBuilder.length() - delimiter.length());
		}
		return returnString;
	}

	public static String toString(double[] numbers, String delimiter) {
		return toString(numbers, delimiter, null);
	}

	public static String toString(short[] numbers, String delimiter) {
		return toString(numbers, delimiter, null);
	}

	public static String toString(long[] numbers, String delimiter) {
		return toString(numbers, delimiter, null);
	}

	public static String toString(char[] characters) {
		return toString(characters, ", ");
	}

	public static String toString(char[] characters, String delimiter) {
		String returnString = "";

		if (characters != null && characters.length > 0) {
			StringBuilder returnStringBuilder = new StringBuilder();
			for (char i : characters) {
				returnStringBuilder.append(i + delimiter);
			}
			returnString = returnStringBuilder.substring(0, returnStringBuilder.length() - delimiter.length());
		}
		return returnString;
	}

	public static String toString(double[] numbers, String delimiter, DecimalFormat formatter) {
		String returnString = "";

		if (numbers != null && numbers.length > 0) {
			StringBuilder returnStringBuilder = new StringBuilder();
			for (double i : numbers) {
				if (formatter != null) {
					returnStringBuilder.append(formatter.format(i) + delimiter);
				} else {
					returnStringBuilder.append(i + delimiter);

				}
			}
			returnString = returnStringBuilder.substring(0, returnStringBuilder.length() - delimiter.length());
		}
		return returnString;
	}

	public static String toString(short[] numbers, String delimiter, DecimalFormat formatter) {
		String returnString = "";

		if (numbers != null && numbers.length > 0) {
			StringBuilder returnStringBuilder = new StringBuilder();
			for (double i : numbers) {
				if (formatter != null) {
					returnStringBuilder.append(formatter.format(i) + delimiter);
				} else {
					returnStringBuilder.append(i + delimiter);

				}
			}
			returnString = returnStringBuilder.substring(0, returnStringBuilder.length() - delimiter.length());
		}
		return returnString;
	}

	public static String toString(long[] numbers, String delimiter, DecimalFormat formatter) {
		String returnString = "";

		if (numbers != null && numbers.length > 0) {
			StringBuilder returnStringBuilder = new StringBuilder();
			for (double i : numbers) {
				if (formatter != null) {
					returnStringBuilder.append(formatter.format(i) + delimiter);
				} else {
					returnStringBuilder.append(i + delimiter);

				}
			}
			returnString = returnStringBuilder.substring(0, returnStringBuilder.length() - delimiter.length());
		}
		return returnString;
	}

	public static <T> boolean contains(T[] values, T containedValue) {
		boolean isContained = false;
		int i = 0;
		while (!isContained && i < values.length) {
			isContained = containedValue.equals(values[i]);
			i++;
		}

		return isContained;
	}

	public static boolean contains(short[] values, short containedValue) {
		boolean isContained = false;
		int i = 0;
		while (!isContained && i < values.length) {
			isContained = containedValue == values[i];
			i++;
		}

		return isContained;
	}

	public static boolean contains(int[] values, int containedValue) {
		boolean isContained = false;
		int i = 0;
		while (!isContained && i < values.length) {
			isContained = containedValue == values[i];
			i++;
		}

		return isContained;
	}

	public static <T> String printMatrix(T[][] a) {
		StringBuilder stringBuilder = new StringBuilder();

		int maxEntryLength = 0;
		for (int row = 0; row < a.length; row++) {
			for (int column = 0; column < a[0].length; column++) {
				String entryAsString = "" + a[row][column];
				maxEntryLength = Math.max(entryAsString.length(), maxEntryLength);
			}
		}

		for (int row = 0; row < a.length; row++) {
			for (int column = 0; column < a[0].length; column++) {
				String entryAsString = "" + a[row][column];
				stringBuilder.append(a[row][column] + StringUtil.repeatString(" ", maxEntryLength - entryAsString.length()) + " ");
			}
			stringBuilder.append(StringUtil.NEWLINE);
		}
		return stringBuilder.toString();
	}

	public static String printMatrix(double[][] a) {
		StringBuilder stringBuilder = new StringBuilder();

		int maxEntryLength = 0;
		for (int row = 0; row < a.length; row++) {
			for (int column = 0; column < a[0].length; column++) {
				String entryAsString = "" + a[row][column];
				maxEntryLength = Math.max(entryAsString.length(), maxEntryLength);
			}
		}

		for (int row = 0; row < a.length; row++) {
			for (int column = 0; column < a[0].length; column++) {
				String entryAsString = "" + a[row][column];
				stringBuilder.append(a[row][column] + StringUtil.repeatString(" ", maxEntryLength - entryAsString.length()) + " ");
			}
			stringBuilder.append(StringUtil.NEWLINE);
		}
		return stringBuilder.toString();
	}

	public static int sum(int[] values) {
		int sum = 0;
		for (int value : values) {
			sum += value;
		}
		return sum;
	}

	public static int sum(int[] values, int startIndex, int stopIndex) {
		int sum = 0;
		for (int i = startIndex; i <= stopIndex; i++) {
			int value = values[i];
			sum += value;
		}
		return sum;
	}

	public static double sum(double[] values) {
		double sum = 0;
		for (double value : values) {
			sum += value;
		}
		return sum;
	}

	public static double sum(long[] values) {
		long sum = 0;
		for (long value : values) {
			sum += value;
		}
		return sum;
	}

	public static int indexOf(String[] array, String string) {
		int indexOf = -1;
		int currentIndex = 0;
		if (string != null) {
			arrayLoop: for (String currentString : array) {
				if (string.equals(currentString)) {
					indexOf = currentIndex;
					break arrayLoop;
				}
				currentIndex++;
			}
		}
		return indexOf;
	}

	/**
	 * returns all permutations of the given numbers
	 * 
	 * @param numbers
	 * @return
	 */
	public static int[][] permutations(int[] numbers) {
		return permutations(numbers.length, numbers);
	}

	/**
	 * returns all possible ways to choose permutations of the given size with the given numbers
	 * 
	 * @param numbers
	 * @return
	 */
	public static int[][] permutations(int desiredPermutationSize, int[] numbers) {
		int[][] permutations = new int[0][0];

		if (desiredPermutationSize > 0) {
			// n choose r
			int totalPermutations = StatisticsUtil.factorial(numbers.length) / StatisticsUtil.factorial(numbers.length - desiredPermutationSize);
			permutations = new int[totalPermutations][desiredPermutationSize];

			int currentPermutationIndex = 0;
			for (int i = 0; i < numbers.length; i++) {
				// remove this number
				// and get the permutation for all remaining numbers
				if (desiredPermutationSize > 1) {
					int[][] nextPermutations = permutations(desiredPermutationSize - 1, removeValueAtIndex(numbers, i));
					for (int j = 0; j < nextPermutations.length; j++) {
						permutations[currentPermutationIndex][0] = numbers[i];
						for (int k = 0; k < nextPermutations[j].length; k++) {
							permutations[currentPermutationIndex][k + 1] = nextPermutations[j][k];
						}
						currentPermutationIndex++;
					}
				} else {
					permutations[i] = new int[] { numbers[i] };
				}
			}
		}
		return permutations;
	}

	/**
	 * returns an array that has removed that value at the given index and shifted the remaining values to the left.
	 * 
	 * @param values
	 * @param index
	 * @return
	 */
	public static int[] removeValueAtIndex(int[] values, int indexToRemove) {
		int[] newNumbers = null;
		if (indexToRemove < values.length) {
			newNumbers = new int[values.length - 1];
			int newIndex = 0;
			for (int i = 0; i < values.length; i++) {
				if (i != indexToRemove) {
					newNumbers[newIndex] = values[i];
					newIndex++;
				}
			}
		} else {
			newNumbers = values;
		}
		return newNumbers;
	}

	/**
	 * removes all values in valuesToRemove array and shifts the remaining values to the left.
	 * 
	 * @param values
	 * @param valuesToRemove
	 * @return
	 */
	public static int[] removeValues(int[] values, int[] valuesToRemove) {
		List<Integer> newValues = new ArrayList<Integer>();

		for (int value : values) {
			if (!contains(valuesToRemove, value)) {
				newValues.add(value);
			}
		}

		int[] newValuesArray = new int[newValues.size()];
		for (int i = 0; i < newValues.size(); i++) {
			newValuesArray[i] = newValues.get(i);
		}
		return newValuesArray;
	}

	public static boolean equals(byte[] bytes, int start, byte[] comparisonBytes, int comparisonStart, int comparisonLength) {
		boolean isEqual = true;
		compLoop: for (int i = 0; i < comparisonLength; i++) {
			int index = start + i;
			int comparisonIndex = comparisonStart + i;
			isEqual = bytes[index] == comparisonBytes[comparisonIndex];
			if (!isEqual) {
				break compLoop;
			}
		}

		return isEqual;
	}

}
