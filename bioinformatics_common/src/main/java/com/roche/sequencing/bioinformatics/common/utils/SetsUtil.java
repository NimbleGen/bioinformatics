package com.roche.sequencing.bioinformatics.common.utils;

import java.util.HashSet;
import java.util.Set;

public class SetsUtil {

	private SetsUtil() {
		throw new AssertionError();
	}

	public static <O> Set<O> getIntersection(Set<O> set1, Set<O> set2) {
		boolean set1IsLarger = set1.size() > set2.size();
		Set<O> clonedSet = new HashSet<O>(set1IsLarger ? set2 : set1);
		clonedSet.retainAll(set1IsLarger ? set1 : set2);
		return clonedSet;
	}

}
