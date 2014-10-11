/**
 *   Copyright 2013 Roche NimbleGen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.roche.sequencing.bioinformatics.common.utils;

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

	public static double[] convertToDoubleArray(int[] values) {
		double[] doubleArray = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			doubleArray[i] = values[i];
		}
		return doubleArray;
	}

	public static double[] convertToDoubleArray(Integer[] values) {
		double[] doubleArray = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			doubleArray[i] = values[i];
		}
		return doubleArray;
	}

}
