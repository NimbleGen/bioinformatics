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
package com.roche.sequencing.bioinformatics.common.statistics;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;

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

	public BigDecimal getSumOfSquares() {
		return sumOfSquares;
	}

	public void setSumOfSquares(BigDecimal sumOfSquares) {
		this.sumOfSquares = sumOfSquares;
	}

	public double getMinValue() {
		return minValue;
	}

	public void setMinValue(double minValue) {
		this.minValue = minValue;
	}

	public double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	public int getNumberOfValues() {
		return numberOfValues;
	}

	public void setNumberOfValues(int numberOfValues) {
		this.numberOfValues = numberOfValues;
	}

	public void setSumOfValues(BigDecimal sumOfValues) {
		this.sumOfValues = sumOfValues;
	}

	@Override
	public String toString() {
		DecimalFormat formatter = new DecimalFormat("#.###");
		formatter.setRoundingMode(RoundingMode.HALF_UP);
		return "RunningStats [sumOfValues=" + formatter.format(sumOfValues) + ", sumOfSquares=" + formatter.format(sumOfSquares) + ", minValue=" + minValue + ", maxValue=" + maxValue
				+ ", numberOfValues=" + numberOfValues + "]";
	}

}
