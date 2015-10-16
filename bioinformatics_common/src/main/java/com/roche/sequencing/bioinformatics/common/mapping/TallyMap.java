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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * 
 * Simple class that keeps track of counts/tallies of objects
 * 
 * @param <O>
 */
public class TallyMap<O> {

	private Map<O, AtomicInteger> objectCount;
	private int largestCount;
	public int sumOfAllBins = 0;
	private Set<O> objectsWithLargestCount;

	public TallyMap() {
		this.objectCount = new ConcurrentHashMap<O, AtomicInteger>();
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

	public boolean contains(O object) {
		return objectCount.containsKey(object);
	}

	public int getSumOfAllBins() {
		return sumOfAllBins;
	}

	public int getCount(O object) {
		int currentCount = 0;
		AtomicInteger atomicCurrentCount = objectCount.get(object);
		if (atomicCurrentCount != null) {
			currentCount = atomicCurrentCount.get();
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
		AtomicInteger atomicCurrentCount = objectCount.get(object);

		if (atomicCurrentCount != null) {
			atomicCurrentCount.addAndGet(numberOfAdditions);
		} else {
			atomicCurrentCount = new AtomicInteger(numberOfAdditions);
			objectCount.put(object, atomicCurrentCount);
		}

		int currentCount = atomicCurrentCount.get();
		if (currentCount > largestCount) {
			largestCount = currentCount;
			objectsWithLargestCount.clear();
			objectsWithLargestCount.add(object);
		} else if (currentCount == largestCount) {
			objectsWithLargestCount.add(object);
		}

	}

	public Map<O, Integer> getTalliesAsMap() {
		Map<O, Integer> talliesAsMap = new HashMap<O, Integer>(objectCount.size());
		for (Entry<O, AtomicInteger> entry : objectCount.entrySet()) {
			talliesAsMap.put(entry.getKey(), entry.getValue().get());
		}
		return talliesAsMap;
	}

	public Set<O> getObjects() {
		return objectCount.keySet();
	}

	public List<Entry<O, Integer>> getObjectsSortedFromMostTalliesToLeast() {
		List<Entry<O, Integer>> entries = new ArrayList<Entry<O, Integer>>(getTalliesAsMap().entrySet());
		Collections.sort(entries, new Comparator<Entry<O, Integer>>() {
			@Override
			public int compare(Entry<O, Integer> o1, Entry<O, Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		return entries;
	}

	public void addAll(TallyMap<O> tallyMap) {
		for (Entry<O, AtomicInteger> entry : tallyMap.objectCount.entrySet()) {
			addMultiple(entry.getKey(), entry.getValue().get());
		}
	}

	public String getHistogramAsString() {
		StringBuilder histogram = new StringBuilder();
		for (Entry<O, Integer> entry : getObjectsSortedFromMostTalliesToLeast()) {
			histogram.append(entry.getKey() + " : " + entry.getValue() + StringUtil.NEWLINE);
		}
		return histogram.toString();
	}
}
