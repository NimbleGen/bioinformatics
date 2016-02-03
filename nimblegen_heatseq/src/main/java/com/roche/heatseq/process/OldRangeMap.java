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
package com.roche.heatseq.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class OldRangeMap<O> implements RangeMap<O> {

	public final Map<Range, List<O>> map;

	public OldRangeMap() {
		map = new HashMap<Range, List<O>>();
	}

	public int size() {
		return map.size();
	}

	public void put(int startInclusive, int stopInclusive, O object) {
		Range range = new Range(startInclusive, stopInclusive);
		List<O> objects = map.get(range);
		if (objects == null) {
			objects = new ArrayList<O>();
			map.put(range, objects);
		}
		objects.add(object);
	}

	public List<O> getObjectsThatContainRangeInclusiveOld(int startInclusive, int stopInclusive) {
		return getObjectsThatContainRangeInclusive(startInclusive, stopInclusive);
	}

	public List<O> getObjectsThatContainRangeInclusive(int startInclusive, int stopInclusive) {
		int start = Math.min(startInclusive, stopInclusive);
		int stop = Math.max(startInclusive, stopInclusive);

		List<O> objectsWithinRange = new ArrayList<O>();
		for (Entry<Range, List<O>> entry : map.entrySet()) {
			Range range = entry.getKey();
			if (start >= range.getStartInclusive() && stop <= range.getStopInclusive()) {
				objectsWithinRange.addAll(entry.getValue());
			}
		}

		return objectsWithinRange;
	}

	private static class Range {
		private final int startInclusive;
		private final int stopInclusive;

		public Range(int startInclusive, int stopInclusive) {
			super();
			this.startInclusive = Math.min(startInclusive, stopInclusive);
			this.stopInclusive = Math.max(startInclusive, stopInclusive);
		}

		public int getStartInclusive() {
			return startInclusive;
		}

		public int getStopInclusive() {
			return stopInclusive;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + startInclusive;
			result = prime * result + stopInclusive;
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
			Range other = (Range) obj;
			if (startInclusive != other.startInclusive)
				return false;
			if (stopInclusive != other.stopInclusive)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Range [startInclusive=" + startInclusive + ", stopInclusive=" + stopInclusive + "]";
		}

	}

}
