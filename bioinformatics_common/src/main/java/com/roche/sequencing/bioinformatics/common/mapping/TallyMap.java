/**
 *   Copyright 2013 Roche NimbleGen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.roche.sequencing.bioinformatics.common.mapping;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * Simple class that keeps track of counts/tallies of objects
 * 
 * @param <O>
 */
class TallyMap<O> {

	private Map<O, Integer> objectCount;
	private int largestCount;
	private Set<O> objectsWithLargestCount;

	public TallyMap() {
		this.objectCount = new ConcurrentHashMap<O, Integer>();
		objectsWithLargestCount = new HashSet<O>();
	}

	void add(O object) {
		Integer currentCount = getCount(object);
		currentCount++;
		if (currentCount > largestCount) {
			largestCount = currentCount;
			objectsWithLargestCount.clear();
			objectsWithLargestCount.add(object);
		} else if (currentCount == largestCount) {
			objectsWithLargestCount.add(object);
		}
		objectCount.put(object, currentCount);
	}

	void addAll(Collection<O> objects) {
		if (objects != null) {
			for (O object : objects) {
				add(object);
			}
		}
	}

	private int getCount(O object) {
		Integer currentCount = objectCount.get(object);
		if (currentCount == null) {
			currentCount = 0;
		}
		return currentCount;
	}

	public int getLargestCount() {
		return largestCount;
	}

	public Set<O> getObjectsWithLargestCount() {
		return objectsWithLargestCount;
	}
}
