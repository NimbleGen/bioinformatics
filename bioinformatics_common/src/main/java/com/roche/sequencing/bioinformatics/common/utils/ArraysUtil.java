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
}
