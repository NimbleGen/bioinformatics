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

	public Double getCurrentPopulationVariance() {
		return getCurrentVariance(true);
	}

	public Double getCurrentSampleVariance() {
		return getCurrentVariance(false);
	}

	public Double getCurrentVariance() {
		return getCurrentSampleVariance();
	}

	private Double getCurrentVariance(boolean isPopulationVariance) {
		Double variance = Double.NaN;
		if (numberOfValues > 3) {
			BigDecimal n = null;
			if (isPopulationVariance) {
				n = new BigDecimal(numberOfValues);
			} else {
				// this results in a sample variance calculation
				n = new BigDecimal(numberOfValues - 1);
			}
			// Var(x) = E(x^2) - [E(X)]^2
			BigDecimal lhs = sumOfSquares.divide(n, MATH_CONTEXT);
			BigDecimal rhs = sumOfValues.divide(new BigDecimal(numberOfValues), MATH_CONTEXT).pow(2);
			variance = lhs.subtract(rhs).doubleValue();
		}
		return variance;
	}

	public Double getCurrentPopulationStandardDeviation() {
		return getCurrentStandardDeviation(true);
	}

	public Double getCurrentSampleStandardDeviation() {
		return getCurrentStandardDeviation(false);
	}

	public Double getCurrentStandardDeviation() {
		return getCurrentSampleStandardDeviation();
	}

	private Double getCurrentStandardDeviation(boolean isPopulationStandardDeviation) {
		Double standardDeviation = Double.NaN;
		Double variance = getCurrentVariance(isPopulationStandardDeviation);
		if (!variance.equals(Double.NaN)) {
			standardDeviation = Math.sqrt(variance);
		}
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
		return "RunningStats [sumOfValues=" + sumOfValues + ", sumOfSquares=" + sumOfSquares + ", minValue=" + minValue + ", maxValue=" + maxValue + ", numberOfValues=" + numberOfValues
				+ ", getCurrentMean()=" + getCurrentMean() + ", getCurrentVariance()=" + getCurrentVariance() + ", getCurrentStandardDeviation()=" + getCurrentStandardDeviation() + "]";
	}

	public void addAll(double[] values) {
		addAllValues(values);
	}

	public void add(double value) {
		addValue(value);
	}

}
