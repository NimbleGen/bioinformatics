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
package com.roche.sequencing.bioinformatics.common.sequence;

public class StartAndStopIndex {

	private final int startIndex;
	private final int stopIndex;

	StartAndStopIndex(int startIndex, int stopIndex) {
		super();
		this.startIndex = startIndex;
		this.stopIndex = stopIndex;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getStopIndex() {
		return stopIndex;
	}

	@Override
	public String toString() {
		return "StartAndStopIndex [startIndex=" + startIndex + ", stopIndex=" + stopIndex + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + startIndex;
		result = prime * result + stopIndex;
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
		StartAndStopIndex other = (StartAndStopIndex) obj;
		if (startIndex != other.startIndex)
			return false;
		if (stopIndex != other.stopIndex)
			return false;
		return true;
	}

}
