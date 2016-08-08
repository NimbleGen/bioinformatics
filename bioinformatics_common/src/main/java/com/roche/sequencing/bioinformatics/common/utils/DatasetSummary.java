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

import java.io.Serializable;

public class DatasetSummary implements Serializable {

	private static final long serialVersionUID = 1L;

	private final static DatasetSummary UNDEFINED = new DatasetSummary(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

	private final double minimumValue;
	private final double maximumValue;
	private final double firstQuartile;
	private final double median;
	private final double thirdQuartile;
	private final double arithmeticMean;

	/**
	 * @param minimumValue
	 *            some double or Double.NAN
	 * @param maximumValue
	 *            some double or Double.NAN
	 * @param firstQuartile
	 *            some double or Double.NAN
	 * @param median
	 *            some double or Double.NAN
	 * @param thirdQuartile
	 *            some double or Double.NAN
	 * @param arithmeticMean
	 *            some double or Double.NAN
	 */
	public DatasetSummary(Double minimumValue, Double maximumValue, Double firstQuartile, Double median, Double thirdQuartile, Double arithmeticMean) {
		this.minimumValue = minimumValue;
		this.maximumValue = maximumValue;
		this.firstQuartile = firstQuartile;
		this.median = median;
		this.thirdQuartile = thirdQuartile;
		this.arithmeticMean = arithmeticMean;
	}

	public Double getMinimumValue() {
		return minimumValue;
	}

	public Double getMaximumValue() {
		return maximumValue;
	}

	public Double getFirstQuartile() {
		return firstQuartile;
	}

	public Double getMedian() {
		return median;
	}

	public Double getThirdQuartile() {
		return thirdQuartile;
	}

	public Double getArithmeticMean() {
		return arithmeticMean;
	}

	/**
	 * Static factory method for cases when no summarization is possible or appropriate.
	 * 
	 * @return such an instance
	 */
	public static DatasetSummary valueOfUndefined() {
		return UNDEFINED;
	}

}
