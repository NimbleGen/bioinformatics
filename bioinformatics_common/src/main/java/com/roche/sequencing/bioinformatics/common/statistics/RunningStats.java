package com.roche.sequencing.bioinformatics.common.statistics;

import java.math.BigDecimal;
import java.math.MathContext;

public class RunningStats {

	private BigDecimal sumOfValues;
	private BigDecimal sumOfSquares;
	private double minValue;
	private double maxValue;
	private int numberOfValues;

	private final static MathContext MATH_CONTEXT = new MathContext(1000);

	public RunningStats() {
		numberOfValues = 0;
		sumOfValues = new BigDecimal(0);
		sumOfSquares = new BigDecimal(0);
		minValue = Double.MAX_VALUE;
		maxValue = -Double.MAX_VALUE;
	}

	public synchronized void addValue(double value) {
		BigDecimal valueAsBigDecimal = new BigDecimal(value);
		minValue = Math.min(minValue, value);
		maxValue = Math.max(maxValue, value);
		sumOfValues = sumOfValues.add(valueAsBigDecimal);
		sumOfSquares = sumOfSquares.add(valueAsBigDecimal.multiply(valueAsBigDecimal));
		numberOfValues++;
	}

	public void addAllValues(Iterable<Double> values) {
		for (double value : values) {
			addValue(value);
		}
	}

	public void addAllValues(double[] values) {
		for (double value : values) {
			addValue(value);
		}
	}

	public Double getCurrentMean() {
		Double currentMean = null;
		if (numberOfValues > 0) {
			currentMean = sumOfValues.divide(new BigDecimal(numberOfValues), MATH_CONTEXT).doubleValue();
		}
		return currentMean;
	}

	public double getCurrentPopulationVariance() {
		return getCurrentVariance(true);
	}

	public double getCurrentSampleVariance() {
		return getCurrentVariance(false);
	}

	public double getCurrentVariance() {
		return getCurrentSampleVariance();
	}

	private double getCurrentVariance(boolean isPopulationVariance) {
		BigDecimal sumOfValuesSquared = sumOfValues.multiply(sumOfValues);
		double variance = 0.0;
		BigDecimal finalDenominator = null;
		if (isPopulationVariance) {
			finalDenominator = new BigDecimal(numberOfValues);
		} else {
			// this results in a sample variance calculation
			finalDenominator = new BigDecimal(numberOfValues - 1);
		}
		variance = sumOfSquares.subtract(sumOfValuesSquared.divide(new BigDecimal(numberOfValues), MATH_CONTEXT)).divide(finalDenominator, MATH_CONTEXT).doubleValue();
		return variance;
	}

	public double getCurrentPopulationStandardDeviation() {
		return getCurrentStandardDeviation(true);
	}

	public double getCurrentSampleStandardDeviation() {
		return getCurrentStandardDeviation(false);
	}

	public double getCurrentStandardDeviation() {
		return getCurrentSampleStandardDeviation();
	}

	private double getCurrentStandardDeviation(boolean isPopulationStandardDeviation) {
		double variance = getCurrentVariance(isPopulationStandardDeviation);
		double standardDeviation = Math.sqrt(variance);
		return standardDeviation;
	}

	public int getCurrentNumberOfValues() {
		return numberOfValues;
	}

	public double getSumOfValues() {
		return sumOfValues.doubleValue();
	}

	@Override
	public String toString() {
		return "RunningStats [sumOfValues=" + sumOfValues + ", sumOfSquares=" + sumOfSquares + ", minValue=" + minValue + ", maxValue=" + maxValue + ", numberOfValues=" + numberOfValues + "]";
	}

}
