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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * Simple class that keeps track of counts/tallies of objects
 * 
 * @param <O>
 */
public class TallyMap<O> {

	private Map<O, Integer> objectCount;
	private int largestCount;
	public int sumOfAllBins = 0;
	private Set<O> objectsWithLargestCount;

	public TallyMap() {
		this.objectCount = new ConcurrentHashMap<O, Integer>();
		objectsWithLargestCount = new HashSet<O>();
	}

	public void add(O object) {
		addMultiple(object, 1);
	}

	public void addAll(Collection<O> objects) {
		if (objects != null) {
			for (O object : objects) {
				add(object);
			}
		}
	}

	public int getSumOfAllBins() {
		return sumOfAllBins;
	}

	public int getCount(O object) {
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

	public void addMultiple(O object, int numberOfAdditions) {
		sumOfAllBins += numberOfAdditions;
		Integer currentCount = getCount(object);
		currentCount += numberOfAdditions;
		if (currentCount > largestCount) {
			largestCount = currentCount;
			objectsWithLargestCount.clear();
			objectsWithLargestCount.add(object);
		} else if (currentCount == largestCount) {
			objectsWithLargestCount.add(object);
		}
		objectCount.put(object, currentCount);
	}

	public Map<O, Integer> getTalliesAsMap() {
		return objectCount;
	}

	public Set<O> getObjects() {
		return objectCount.keySet();
	}

	public List<Entry<O, Integer>> getObjectsSortedFromMostTalliesToLeast() {
		List<Entry<O, Integer>> entries = new ArrayList<Entry<O, Integer>>(objectCount.entrySet());
		Collections.sort(entries, new Comparator<Entry<O, Integer>>() {
			@Override
			public int compare(Entry<O, Integer> o1, Entry<O, Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		return entries;
	}

	public void addAll(TallyMap<O> tallyMap) {
		for (Entry<O, Integer> entry : tallyMap.objectCount.entrySet()) {
			addMultiple(entry.getKey(), entry.getValue());
		}
	}
}
