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

/**
 * Holds onto a range of values
 * 
 * @author Kurt Heilman
 * 
 */
public class Range implements Serializable {

	private static final long serialVersionUID = 1L;

	private final double start, end;

	public Range(double start, double end) {
		super();
		this.end = end;
		this.start = start;
	}

	/**
	 * @return the start
	 */
	public double getStart() {
		return start;
	}

	/**
	 * @return the end
	 */
	public double getEnd() {
		return end;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "start:" + start + " end:" + end;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(end);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(start);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Range other = (Range) obj;
		if (Double.doubleToLongBits(end) != Double.doubleToLongBits(other.end))
			return false;
		if (Double.doubleToLongBits(start) != Double.doubleToLongBits(other.start))
			return false;
		return true;
	}

	public double size() {
		return end - start;
	}

}
