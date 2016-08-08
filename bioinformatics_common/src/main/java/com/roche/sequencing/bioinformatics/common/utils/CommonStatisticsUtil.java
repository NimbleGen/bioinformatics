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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class containing utility methods for various statistical functions
 * 
 * @author Kurt Heilman
 * 
 */
public class CommonStatisticsUtil {

	/**
	 * constant for tuning the biweight algorithm: basically values that do not have a distance from the median that is within this number times the average distance from the median are not included
	 * in calculating the weighted mean
	 * 
	 * so larger values will mean that more outliers are used in calculating the biweight mean
	 */
	private static final double DEFAULT_BIWEIGHT_OUTLIER_TUNING_VARIABLE = 5.0;
	private static final Logger logger = LoggerFactory.getLogger(CommonStatisticsUtil.class);

	// private final static MathContext MATH_CONTEXT = new MathContext(1000);

	private CommonStatisticsUtil() {
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
		double arithmeticMean;

		int size = values.length;

		double sum = summation(values);

		arithmeticMean = sum / size;

		return arithmeticMean;
	}

	/**
	 * @param values
	 *            source of data for summation calculation
	 * @return the sum of all values within the array
	 */
	public static double summation(double[] values) {
		double sum = 0.0;
		int size = values.length;

		for (int i = 0; i < size; i++) {
			sum += values[i];
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
	public static double variance(double[] values, boolean isUnbiased) {
		double variance = Double.NaN;

		double mean = arithmeticMean(values);
		int size = values.length;

		if (size > 0) {
			double sum = 0.0;
			for (int i = 0; i < size; i++) {
				sum += Math.pow((values[i] - mean), 2.0);
			}

			if (isUnbiased) {
				variance = sum / (size - 1);
			} else {
				variance = sum / size;
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
	public static double standardDeviation(double[] values, boolean isUnbiased) {
		double standardDeviation = Math.sqrt(variance(values, isUnbiased));
		return standardDeviation;
	}

	/**
	 * @param values
	 *            set of data on which to compute
	 * @return the standard deviation of the values
	 */
	public static double standardDeviation(double[] values) {
		return standardDeviation(values, true);
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

		if (numberOfSubsets < 0 || numberOfSubsets > values.length) {
			IllegalStateException e = new IllegalStateException("numberOfSubsets[" + numberOfSubsets + "] must be less than or equal to the number of values[" + values.length + "].");
			logger.warn(e.getMessage(), e);
			throw e;
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

	/**
	 * return the Interquartile Range for these values
	 * 
	 * @param values
	 *            set of values to be split into quartiles
	 * @return interquartile range
	 */
	public static Range InterQuartileRange(double[] values) {
		double[] quantiles = quantile(4, values);
		double quantileOneValue = quantiles[0];
		double quantileThreeValue = quantiles[2];
		return new Range(quantileOneValue, quantileThreeValue);
	}

	/**
	 * Returns the median of the superset of data indicated by the start and length.
	 * 
	 * @param values
	 *            superset of the data to get the median of
	 * @param start
	 *            the start position in the data set
	 * @param length
	 *            The number of values to use in the data set
	 * @return The median of the specified subset of the data values
	 * 
	 */
	public static double median(double[] values, int start, int length) {
		if ((start < 0) || (start > values.length)) {
			ArrayIndexOutOfBoundsException e = new ArrayIndexOutOfBoundsException("Starting position outside of data array");
			logger.warn(e.getMessage(), e);
			throw e;
		}

		if ((length < 1) || (length > (values.length - start))) {
			ArrayIndexOutOfBoundsException e = new ArrayIndexOutOfBoundsException("Requested size is illegal based on start location in data array");
			logger.warn(e.getMessage(), e);
			throw e;
		}

		// not optimal way of doing this but don't see this function being used
		// a lot
		double[] newValues = new double[length];
		System.arraycopy(values, start, newValues, 0, length);

		return median(newValues);
	}

	/**
	 * Calculates a biweight mean using Tukey's biweight algorithm
	 * 
	 * The algorithm consists of 6 steps:
	 * 
	 * 1) calculate the median to define the center of the data 2) calculate the absolute distance each point is from the median 3) calculate the median of these distances, the Median Absolute
	 * Deviation (MAD) 4) calculate a uniform measure of distance from the center by dividing the distance each point is from the median by the MAD multiplied by a tuning Constant (usually 5). 5)
	 * calculate a weight by running the result from step 4 through a bisquare function. 6) the biweight mean is the sum of all weighted points divided by the sum of all weights.
	 * 
	 * @param values
	 *            source of data for calculation
	 * @return a biweight mean using Tukey's biweigth algorithm
	 */
	public static double biweightMean(double[] values) {
		return biweightMean(values, DEFAULT_BIWEIGHT_OUTLIER_TUNING_VARIABLE);
	}

	/**
	 * A tunable Tukey biweight mean algorithm
	 * 
	 * The algorithm consists of 6 steps:
	 * 
	 * 1) calculate the median to define the center of the data 2) calculate the absolute distance each point is from the median 3) calculate the median of these distances, the Median Absolute
	 * Deviation (MAD) 4) calculate a uniform measure of distance from the center by dividing the distance each point is from the median by the MAD multiplied by a tuning Constant (usually 5). 5)
	 * calculate a weight by running the result from step 4 through a bisquare function. 6) the biweight mean is the sum of all weighted points divided by the sum of all weights.
	 * 
	 * A description of the tukey's biweight mean algorithm can be found at: http ://www.itl.nist.gov/div898/software/dataplot.html/refman2/ch2/biweight .pdf
	 * 
	 * @param values
	 *            source of data for biweight mean calculation
	 * @param biweightOutlierTuningVariable
	 *            (default 5) tuning variable for tuning the biweight algorithm: basically values that do not have a distance from the median that is within this number times the average distance from
	 *            the median are not included in calculating the weighted mean so larger values will mean that more outliers are used in calculating the biweight mean
	 * @return biweight mean
	 */
	public static double biweightMean(double[] values, double biweightOutlierTuningVariable) {
		double biweightMean;
		if (biweightOutlierTuningVariable <= 0) {
			throw new IllegalStateException("biweightOutlierTuningVaraible[" + biweightOutlierTuningVariable + " must be greater than zero.");
		}

		if (values.length <= 0) {
			IllegalStateException e = new IllegalStateException("values size must be greater than 0.");
			logger.warn(e.getMessage(), e);
			throw e;
		}

		int size = values.length;

		// 1) calculate the median to define the center of the data
		double median = CommonStatisticsUtil.median(values);

		// 2) calculate the absolute distance each point is from the median
		double[] absDistFromMedian = new double[size];
		for (int i = 0; i < size; i++) {
			absDistFromMedian[i] = Math.abs(values[i] - median);
		}

		// 3) calculate the median of these distances, the Median Absolute
		// Deviation (MAD)
		double medianAbsoluteDeviation = CommonStatisticsUtil.median(absDistFromMedian);

		if (medianAbsoluteDeviation == 0.0) {
			// all values are the same so return one of these values
			biweightMean = values[0];
		} else {

			// tuning variable for tuning the biweight algorithm: basically
			// values that do not have a distance from the median that is within
			// this number times the average distance
			// from
			// the
			// median are not included in calculating the weighted mean
			// so larger values will mean that more outliers are used in
			// calculating the biweight mean
			double denominator = biweightOutlierTuningVariable * medianAbsoluteDeviation;

			// Typically a very small value such as 0.0001 is added
			// to the denominator to avoid values of 0, this would
			// lead to very large values for uniformDistanceFromCenter values
			// which would ultimately lead to weight values of 0 since the
			// uniformDistanceFromCenter values are greater than 1.
			// If all weight values are 0 then a mean can't be calculated
			// so return the default biweightMean of NaN.
			// we can avoid this circumstance because there is no way
			// denominator variable
			// will be 0 based on previous checks of biweightTuningVariable and
			// medianAbsoluteDeviation

			double weightsSum = 0.0;
			double weightedValuesSum = 0.0;

			for (int i = 0; i < size; i++) {
				double value = values[i];
				double numerator = value - median;

				// 4) calculate a uniform measure of distance from the center by
				// dividing the distance each point is from the median by
				double uniformDistanceFromCenterValue = numerator / denominator;

				// 5) calculate a weight by running the result from step 4
				// through a bisquare function.
				double bisquareWeight = bisquareWeight(uniformDistanceFromCenterValue);

				weightsSum += bisquareWeight;
				weightedValuesSum += value * bisquareWeight;
			}

			// 6) the biweight mean is the sum of all weighted points divided by
			// the sum of all weights.
			biweightMean = weightedValuesSum / weightsSum;

		}

		return biweightMean;
	}

	/**
	 * Takes a set of data and computes a dataset summary.
	 * 
	 * @param values
	 *            some set of data
	 * @return a summary of some basic statistics
	 */
	public static DatasetSummary getDatasetSummary(float[] values) {
		return getDatasetSummary(values);
	}

	/**
	 * Takes a set of data and computes a dataset summary.
	 * 
	 * @param values
	 *            some set of data
	 * @return a summary of some basic statistics
	 */
	public static DatasetSummary getDatasetSummary(double[] values) {

		DatasetSummary returnValue;
		switch (values.length) {
		case (0): {
			returnValue = DatasetSummary.valueOfUndefined();
			break;
		}
		case (1): {
			double num = values[0];
			returnValue = new DatasetSummary(num, num, Double.NaN, num, Double.NaN, num);
			break;
		}
		case (2): {
			double median = median(values);
			double arithmeticMean = arithmeticMean(values);
			double min = Math.min(values[0], values[1]);
			double max = Math.max(values[0], values[1]);
			returnValue = new DatasetSummary(min, max, Double.NaN, median, Double.NaN, arithmeticMean);
			break;
		}
		case (3): {
			double[] valuesCopy = new double[values.length];
			System.arraycopy(values, 0, valuesCopy, 0, values.length);
			Arrays.sort(valuesCopy);
			double arithmeticMean = arithmeticMean(valuesCopy);
			double min = valuesCopy[0];
			double median = valuesCopy[1];
			double max = valuesCopy[2];
			returnValue = new DatasetSummary(min, max, Double.NaN, median, Double.NaN, arithmeticMean);
			break;
		}
		default: {

			// do not want to mutate the original array so create a copy and
			// sort that
			double[] valuesCopy = new double[values.length];
			System.arraycopy(values, 0, valuesCopy, 0, values.length);
			Arrays.sort(valuesCopy);
			double[] quartiles = quantile(4, valuesCopy, true);
			double min = valuesCopy[0];
			double max = valuesCopy[valuesCopy.length - 1];
			returnValue = new DatasetSummary(min, max, quartiles[0], quartiles[1], quartiles[2], arithmeticMean(valuesCopy));
		}

		}

		return returnValue;
	}

	public static double rootMeanSquareDeviation(double[] values) {
		double mean = arithmeticMean(values);

		double sum = 0;
		for (int i = 0; i < values.length; i++) {
			double differenceFromMean = values[i] - mean;
			sum += (differenceFromMean * differenceFromMean);
		}
		logger.debug("root mean square deviation sum:" + sum);
		double ratio = sum / (double) values.length;
		logger.debug("root mean square deviation length:" + values.length);
		logger.debug("root mean square deviation ratio:" + ratio);
		double rootMeanSquareDeviation = Math.sqrt(ratio);
		return rootMeanSquareDeviation;
	}

	/**
	 * convenience function for squaring a number
	 * 
	 * @param value
	 *            value to square
	 * @return square of a number
	 */
	private static double square(double value) {
		return (value * value);
	}

	/**
	 * Providese the weight value based on the uniform distance from the center value.
	 * 
	 * @param uniformDistanceFromCenter
	 *            uniform distance from center
	 * @return weight value: 0, if absolute value of uniformDistanceFromCentera is greater than 1. otherwise, return (1-UDC^2) / 2
	 */
	private static double bisquareWeight(double uniformDistanceFromCenter) {
		double weight = 0.0;
		// Some descriptions of this is < instead of <=
		if (Math.abs(uniformDistanceFromCenter) <= 1) {
			weight = square((1 - square(uniformDistanceFromCenter)));
		}
		return weight;
	}

	public static class MeanAndVariance {
		private final double mean;
		private final double variance;

		public MeanAndVariance(double mean, double variance) {
			super();
			this.mean = mean;
			this.variance = variance;
		}

		public double getMean() {
			return mean;
		}

		public double getVariance() {
			return variance;
		}

		public double getStandardDeviation() {
			return Math.sqrt(variance);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(mean);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(variance);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MeanAndVariance other = (MeanAndVariance) obj;
			if (Double.doubleToLongBits(mean) != Double.doubleToLongBits(other.mean))
				return false;
			if (Double.doubleToLongBits(variance) != Double.doubleToLongBits(other.variance))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "MeanAndVariance [mean=" + mean + ", variance=" + variance + ", getStandardDeviation()=" + getStandardDeviation() + "]";
		}

	}

	// public static MeanAndVariance calculateMeanAndVariance(double[] values) {
	// return calculateMeanAndVariance(values, true);
	// }

	// public static MeanAndVariance calculateMeanAndVariance(double[] values, boolean isUnbiased) {
	// double sumOfValues = 0;
	// BigDecimal sumOfValuesSquared = BigDecimal.ZERO;
	//
	// for (double value : values) {
	// sumOfValues += value;
	// sumOfValuesSquared = sumOfValuesSquared.add(new BigDecimal(value).pow(2));
	// }
	//
	// double mean = sumOfValues / values.length;
	// double n = values.length;
	// if (isUnbiased) {
	// n--;
	// }
	// BigDecimal meanSquared = new BigDecimal(mean).pow(2);
	// BigDecimal variance = sumOfValuesSquared.divide(new BigDecimal(n), MATH_CONTEXT).subtract(meanSquared);
	// return new MeanAndVariance(mean, variance.doubleValue());
	// }
	//
	// public static double calculateCorrelationCoefficient(double[] xValues, double[] yValues) {
	// int n = xValues.length;
	// if (xValues.length != yValues.length) {
	// throw new IllegalStateException("The size of the provided xValues[" + xValues.length + "] is not equal to the size of the provided yValues[" + yValues.length + "].");
	// }
	//
	// MeanAndVariance xMeanAndVariance = calculateMeanAndVariance(xValues, true);
	// MeanAndVariance yMeanAndVariance = calculateMeanAndVariance(yValues, true);
	//
	// double sumOfDifferences = 0;
	// for (int i = 0; i < n; i++) {
	// double x = xValues[i];
	// double y = yValues[i];
	// double differences = (x - xMeanAndVariance.mean) * (y - yMeanAndVariance.mean);
	// sumOfDifferences += differences;
	// }
	//
	// double r = sumOfDifferences / (xMeanAndVariance.getStandardDeviation() * yMeanAndVariance.getStandardDeviation()) / (n - 1);
	//
	// return r;
	// }

	// public static void main(String[] args) {
	// double[] x = new double[] { 3, 3, 6 };
	// double[] y = new double[] { 2, 3, 4 };
	// System.out.println(calculateCorrelationCoefficient(x, y));
	// }

	/**
	 * Calculates the cumulative normal distribution function at x which is the probability of a value being less than x which is the area under the normal curve to the left of x.
	 * 
	 * @param x
	 * @return the probability of a value being less than x.
	 */
	public static double cumulativeNormalProbability(double x) {
		if (x > 6.0)
			return (1.0);
		if (x < -6.0)
			return (0.0);

		// 0.3989423 = 1 / Math.sqrt(2 * PI)
		double probabilityDensity = 0.3989423 * Math.exp((-x) * x * 0.5);

		// Numerical approximation for the normal cumulative distribution
		// function
		double p = 0.2316419;
		double b1 = 0.31938153;
		double b2 = -0.356563782;
		double b3 = 1.781477937;
		double b4 = -1.821255978;
		double b5 = 1.330274429;

		double positiveX = (x >= 0) ? x : -x;
		double t = 1.0 / (1.0 + positiveX * p);

		double phi = ((((b5 * t + b4) * t + b3) * t + b2) * t + b1) * t;
		phi = 1.0 - probabilityDensity * phi;
		if (x < 0.0)
			phi = 1.0 - phi;
		return phi;
	}
}
