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

public class RunningStats2 {

	private long n;
	private double M1;
	private double M2;
	private double M3;
	private double M4;

	public RunningStats2() {
		super();
	}

	public void addAll(double[] values) {
		for (double value : values) {
			add(value);
		}
	}

	public void add(double value) {
		long n1 = n;
		n++;
		double delta = value - M1;
		double delta_n = delta / n;
		double delta_n2 = delta_n * delta_n;
		double term1 = delta * delta_n * n1;
		M1 += delta_n;
		M4 += term1 * delta_n2 * (n * n - 3 * n + 3) + 6 * delta_n2 * M2 - 4 * delta_n * M3;
		M3 += term1 * delta_n * (n - 2) - 3 * delta_n * M2;
		M2 += term1;
	}

	public double getMean() {
		return M1;
	}

	public double getVariance() {
		return M2 / (n - 1.0);
	}

	public double getStandardDeviation() {
		return Math.sqrt(getVariance());
	}

	public double getSkewness() {
		return Math.sqrt(n) * M3 / Math.pow(M2, 1.5);
	}

	public double getKurtosis() {
		return n * M4 / (M2 * M2) - 3.0;
	}
}
