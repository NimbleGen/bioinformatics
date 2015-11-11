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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class containing utility methods for various statistical functions
 * 
 * 
 */
public class StatisticsUtil {

	private static final Logger logger = LoggerFactory.getLogger(StatisticsUtil.class);

	private StatisticsUtil() {
		throw new AssertionError();
	}

	/**
	 * Calculates the geometric mean
	 * 
	 * The geometric mean is the product of all values in the array to the Nth root, where N is the total number of values in the array.
	 * 
	 * reference--http://en.wikipedia.org/wiki/Geometric_mean
	 * 
	 * @param values
	 *            source of data for calculation
	 * @return geometric mean
	 */
	public static double geometricMean(double[] values) {
		double geometricMean;

		// convert (all the values to log form and
		// send them to geometricMeanFromLog which
		// is a more efficient way of calculating
		// the geometric mean since it uses addition of small log values opposed
		// to multiplication of large non-log values
		int size = values.length;

		double[] logValues = new double[size];
		for (int i = 0; i < size; i++) {
			logValues[i] = Math.log(values[i]);
		}

		geometricMean = geometricMeanFromLog(logValues);

		return geometricMean;
	}

	/**
	 * Calculates the geometric mean of log values.
	 * 
	 * The geometric mean of logarithmic values is simply the arithmethic mean converted to non-logarithmic values (exponentiated)
	 * 
	 * 
	 * @param logValues
	 *            array of values in logarithmic form
	 * @return geometric mean
	 */
	public static double geometricMeanFromLog(double[] logValues) {
		double logArithmeticMean = arithmeticMean(logValues);
		double geometricMean = Math.exp(logArithmeticMean);
		return geometricMean;
	}

	/**
	 * calculate the arithmetic mean
	 * 
	 * The arithmetic mean is the sum of all values in the array divided by the total number of values in the array.
	 * 
	 * @param values
	 *            source of data for mean calculation
	 * @return arithmetic mean
	 */
	public static double arithmeticMean(double[] values) {
		double arithmeticMean = 0;

		if (values != null) {
			int size = values.length;

			double sum = summation(values);

			arithmeticMean = sum / size;
		}
		return arithmeticMean;
	}

	/**
	 * @param values
	 *            source of data for summation calculation
	 * @return the sum of all values within the array
	 */
	public static double summation(double[] values) {
		double sum = 0.0;
		if (values != null) {
			int size = values.length;

			for (int i = 0; i < size; i++) {
				sum += values[i];
			}
		}
		return sum;
	}

	/**
	 * @param values
	 *            source of data for calculation
	 * @param unbiased
	 *            is an unbiased variance result desired
	 * @return the biased or unbiased variance
	 */
	public static double variance(double[] values, boolean unbiased) {
		double variance = Double.NaN;

		if (values != null) {
			double mean = arithmeticMean(values);
			int size = values.length;

			if (size > 0) {
				double sum = 0.0;
				for (int i = 0; i < size; i++) {
					sum += Math.pow((values[i] - mean), 2.0);
				}

				if (unbiased) {
					variance = sum / (size - 1);
				} else {
					variance = sum / size;
				}
			}
		}

		return variance;
	}

	/**
	 * @param values
	 *            source of data for variance calculation
	 * @return the unbiased variance of the values within the array
	 */
	public static double variance(double[] values) {
		return variance(values, true);
	}

	/**
	 * @param values
	 *            set of data on which to compute
	 * @return the standard deviation of the values
	 */
	public static double standardDeviation(double[] values) {
		double standardDeviation = Math.sqrt(variance(values));
		return standardDeviation;
	}

	/**
	 * @param values
	 *            set of data on which to compute
	 * @return the standard deviation of the values
	 */
	public static double standardDeviation(int[] values) {
		double[] valuesAsDoubleArray = ArraysUtil.convertToDoubleArray(values);
		return standardDeviation(valuesAsDoubleArray);
	}

	/**
	 * Calculated the value of the quantiles defined by the quantileIndex with numberOfSubsets on the given values. This method uses emperical distribution function with averaging to estimate the
	 * quantiles. Source: http://en.wikipedia.org/wiki/Quantile.
	 * 
	 * @param numberOfSubsets
	 *            desired number of subsets that the returned quantiles should define
	 * @param values
	 *            a set of values
	 * @return returns an array of given values for each quantile index where position 0 represents the first quantile value(returns NaN in the array if value cannot be calculated)
	 */
	public static double[] quantile(int numberOfSubsets, double[] values) {
		return quantile(numberOfSubsets, values, false);
	}

	/**
	 * Calculated the value of the quantiles defined by the quantileIndex with numberOfSubsets on the given values. This method uses emperical distribution function with averaging to estimate the
	 * quantiles. Source: http://en.wikipedia.org/wiki/Quantile.
	 * 
	 * @param numberOfSubsets
	 *            desired number of subsets that the returned quantiles should define
	 * @param values
	 *            a set of values
	 * @param isSorted
	 *            whether the value set is pre-sorted
	 * @return returns an array of given values for each quantile index where position 0 represents the first quantile value(returns NaN in the array if value cannot be calculated)
	 */
	private static double[] quantile(int numberOfSubsets, double[] values, boolean isSorted) {

		double[] quantileResults = new double[numberOfSubsets - 1];

		if (numberOfSubsets > values.length) {
			RuntimeException p = new RuntimeException("numberOfSubsets[" + numberOfSubsets + "] must be less than or equal to the number of values[" + values.length + "].");
			logger.warn(p.getMessage(), p);
			throw p;
		}

		int size = values.length;

		// sort the array
		if (!isSorted) {
			// do not want to mutate the original array so create a copy and
			// sort that
			double[] valuesCopy = new double[size];
			System.arraycopy(values, 0, valuesCopy, 0, values.length);
			Arrays.sort(valuesCopy);
			values = valuesCopy;
		}

		for (int quantileIndex = 1; quantileIndex < numberOfSubsets; quantileIndex++) {
			// make sure to convert from 1 based indexing to 0 based indexing
			double quantileLocation = (size * (((double) quantileIndex) / ((double) numberOfSubsets))) - 1.0;

			double quantileValue = Double.NaN;

			if (quantileLocation < ((double) size) && size > 0) {

				// check if the quantileLocation has a decimal value
				// this is the part that uses
				// emperical distribution function with averaging for estimating
				if (quantileLocation % 1 == 0) {
					// there is no decimal values so the quantile value
					// is the average of values found at quantileLocation
					// and quantileLocation + 1
					quantileValue = (values[(int) quantileLocation] + values[(int) quantileLocation + 1]) / 2;
				} else {
					// there is a decimal value so use the next value
					quantileValue = values[(int) quantileLocation + 1];
				}
			}

			quantileResults[quantileIndex - 1] = quantileValue;

		}

		return quantileResults;
	}

	/**
	 * @param values
	 *            dataset upon which to perform computation
	 * @return median the median of the value set
	 */
	public static double median(double[] values) {
		double median;

		if (values.length == 1) {
			median = values[0];
		} else {
			median = quantile(2, values)[0];
		}

		return median;
	}

	public static double getInterQuartileRange(double[] values) {
		double range = Double.NaN;
		if (values.length >= 4) {
			double[] quantiles = quantile(4, values);
			range = quantiles[2] = quantiles[0];
		}
		return range;
	}

	private static double[] convertToPdf(double[] values) {
		double minValue = Double.MAX_VALUE;

		for (int i = 0; i < values.length; i++) {
			double value = values[i];
			minValue = Math.min(minValue, value);
		}

		double sumOfValues = 0;
		for (int i = 0; i < values.length; i++) {
			double value = values[i] - minValue;
			sumOfValues += value;
		}

		double[] normalizedValues = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			double value = values[i];
			normalizedValues[i] = (value - minValue) / sumOfValues;
		}
		return normalizedValues;
	}

	private static int numberOfPeaks(double[] values) {
		int numberOfPeaks = 0;
		if (values.length > 3) {
			for (int i = 2; i < values.length; i++) {
				double firstValue = values[i - 2];
				double secondValue = values[i - 1];
				double thirdValue = values[i];

				if (secondValue > firstValue && secondValue > thirdValue) {
					numberOfPeaks++;
				}
			}
		}
		return numberOfPeaks;
	}

	public static DiscreteRandomVariableParameters getDiscreteRandomVariableParameters(double[] values) {
		values = convertToPdf(values);

		double lastSumOfProbabilities = 0;
		double sumOfProbabilities = 0;
		double median = -1;
		double expectedValue = 0;
		double secondMoment = 0;
		double thirdMoment = 0;

		for (int i = 0; i < values.length; i++) {
			double value = values[i];
			sumOfProbabilities += value;
			if (sumOfProbabilities >= 0.50 && lastSumOfProbabilities < 0.50) {
				median = i;
			}
			expectedValue += (i * value);
			secondMoment += (Math.pow(i, 2) * value);
			thirdMoment += (Math.pow(i, 3) * value);
			lastSumOfProbabilities = sumOfProbabilities;
		}

		double variance = secondMoment - (expectedValue * expectedValue);
		double standardDeviation = Math.sqrt(variance);
		double skewness = (thirdMoment - 3 * expectedValue * variance - (Math.pow(expectedValue, 3))) / Math.pow(standardDeviation, 3);

		int numberOfPeaks = numberOfPeaks(values);

		return new DiscreteRandomVariableParameters(expectedValue, standardDeviation, skewness, numberOfPeaks, median, values.length);
	}

	public static class DiscreteRandomVariableParameters {
		private final double mean;
		private final double standardDeviation;
		private final double skewness;
		private final double numberOfPeaks;
		private final double median;
		private final double length;
		private final double meanRatio;
		private final double medianRatio;

		public DiscreteRandomVariableParameters(double mean, double standardDeviation, double skewness, double numberOfPeaks, double median, double length) {
			super();
			this.mean = mean;
			this.standardDeviation = standardDeviation;
			this.skewness = skewness;
			this.numberOfPeaks = numberOfPeaks;
			this.median = median;
			this.length = length;
			this.meanRatio = mean / length;
			this.medianRatio = median / length;
		}

		public double getMean() {
			return mean;
		}

		public double getStandardDeviation() {
			return standardDeviation;
		}

		public double getSkewness() {
			return skewness;
		}

		public double getNumberOfPeaks() {
			return numberOfPeaks;
		}

		public double getMedian() {
			return median;
		}

		public double getLength() {
			return length;
		}

		public double getMeanRatio() {
			return meanRatio;
		}

		public double getMedianRatio() {
			return medianRatio;
		}

		@Override
		public String toString() {
			return "DiscreteRandomVariableParameters [mean=" + mean + ", standardDeviation=" + standardDeviation + ", skewness=" + skewness + ", numberOfPeaks=" + numberOfPeaks + ", median=" + median
					+ ", length=" + length + ", meanRatio=" + meanRatio + ", medianRatio=" + medianRatio + "]";
		}
	}
}
