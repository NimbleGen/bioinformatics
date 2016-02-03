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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IntersectionUtil {

	private IntersectionUtil() {
		throw new AssertionError();
	}

	public static <O> Set<O> getIntersection(Set<O> set1, Set<O> set2) {
		Set<O> returnSet = null;
		if (set1 != null && set2 != null) {
			boolean set1IsLarger = set1.size() > set2.size();
			Set<O> clonedSet = new HashSet<O>(set1IsLarger ? set2 : set1);
			clonedSet.retainAll(set1IsLarger ? set1 : set2);
			returnSet = clonedSet;
		} else {
			returnSet = new HashSet<O>(0);
		}
		return returnSet;
	}

	public static <O> List<O> getIntersection(List<O> list1, List<O> list2) {
		List<O> returnList = null;
		if (list1 != null && list2 != null) {
			boolean listIsLarger = list1.size() > list2.size();
			List<O> clonedList = new ArrayList<O>(listIsLarger ? list2 : list1);
			clonedList.retainAll(listIsLarger ? list1 : list2);
			returnList = clonedList;
		} else {
			returnList = new ArrayList<O>(0);
		}
		return returnList;
	}

}
