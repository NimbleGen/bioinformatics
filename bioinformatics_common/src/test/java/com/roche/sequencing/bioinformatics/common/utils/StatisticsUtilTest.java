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

import org.testng.Assert;
import org.testng.annotations.Test;

public class StatisticsUtilTest {

	// example from
	// http://easycalculation.com/statistics/learn-geometric-mean.php
	private double[] values1 = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 };
	private int[] intValues1 = new int[] { 1, 2, 3, 4, 5 };
	double[] logValues1 = new double[] { Math.log(1.0), Math.log(2.0), Math.log(3.0), Math.log(4.0), Math.log(5.0) };
	private double geometricMean1 = 2.60517;
	private double arithmeticMean1 = 3.0;
	private double sum1 = 15.0;
	private double unbiasedVariance1 = 2.5;
	private double biasedVariance1 = 2.0;
	private double standardDeviation1 = 1.58;
	private double median1 = 3.0;

	private double[] values2 = new double[] { 1.0, 2.0, 2.0, 3.0, 4.0, 5.0 };
	private double median2 = 2.5;

	private double[] values3 = new double[] { 1.0, 2.0, 2.0, 4.0, 4.0 };
	private double median3 = 2.0;

	@Test(groups = { "unit" })
	public void testGeometricMean() {
		double geometricMean = StatisticsUtil.geometricMean(values1);
		Assert.assertEquals(geometricMean1, geometricMean, 0.00001);
	}

	@Test(groups = { "unit" })
	public void testGeometricMeanFromLog() {
		double geometricMean = StatisticsUtil.geometricMeanFromLog(logValues1);
		Assert.assertEquals(geometricMean1, geometricMean, 0.00001);
	}

	@Test(groups = { "unit" })
	public void testArithmeticMean() {
		double arithmeticMean = StatisticsUtil.arithmeticMean(values1);
		Assert.assertEquals(arithmeticMean1, arithmeticMean, 0.00001);
	}

	@Test(groups = { "unit" })
	public void testSummation() {
		double sum = StatisticsUtil.summation(values1);
		Assert.assertEquals(sum1, sum, 0.00001);
	}

	@Test(groups = { "unit" })
	public void testUnbiasedVariance() {
		double variance = StatisticsUtil.variance(values1);
		Assert.assertEquals(unbiasedVariance1, variance, 0.00001);
	}

	@Test(groups = { "unit" })
	public void testBiasedVariance() {
		double variance = StatisticsUtil.variance(values1, false);
		Assert.assertEquals(biasedVariance1, variance, 0.00001);
	}

	@Test(groups = { "unit" })
	public void testStandardDeviation() {
		double standardDeviation = StatisticsUtil.standardDeviation(values1);
		Assert.assertEquals(standardDeviation1, standardDeviation, 0.01);
	}

	@Test(groups = { "unit" })
	public void testStandardDeviation2() {
		double standardDeviation = StatisticsUtil.standardDeviation(intValues1);
		Assert.assertEquals(standardDeviation1, standardDeviation, 0.01);
	}

	@Test(groups = { "unit" })
	public void testMedian1() {
		double median = StatisticsUtil.median(values1);
		Assert.assertEquals(median1, median, 0.00001);
	}

	@Test(groups = { "unit" })
	public void testMedian2() {
		double median = StatisticsUtil.median(values2);
		Assert.assertEquals(median2, median, 0.00001);
	}

	@Test(groups = { "unit" })
	public void testMedian3() {
		double median = StatisticsUtil.median(values3);
		Assert.assertEquals(median3, median, 0.00001);
	}

	@Test(groups = { "unit" })
	public void testMedian4() {
		double median = StatisticsUtil.median(new double[] { 1 });
		Assert.assertEquals(1, median, 0.00001);
	}

	@Test(groups = { "unit" })
	public void testQuantile() {
		double quantile = StatisticsUtil.quantile(3, values3)[1];
		Assert.assertEquals(4.0, quantile, 0.00001);
	}

	@Test(groups = { "unit" }, expectedExceptions = RuntimeException.class)
	public void testQuantilesTooMany() {
		StatisticsUtil.quantile(7, values3);
	}

}
