package com.roche.sequencing.bioinformatics.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ListUtil {

	private ListUtil() {
		throw new AssertionError();
	}

	public static <T> String toString(List<T> list) {
		StringBuilder stringBuilder = new StringBuilder();

		if (list.size() == 1) {
			stringBuilder.append(list.get(0));
		} else if (list.size() == 2) {
			stringBuilder.append(list.get(0) + " and " + list.get(1));
		} else {
			stringBuilder.append(list.get(0));
			for (int i = 1; i < list.size() - 1; i++) {
				stringBuilder.append(", " + list.get(i));
			}
			stringBuilder.append(" and " + list.get(list.size() - 1));
		}
		return stringBuilder.toString();
	}

	public static <T> String toString(List<T> list, String delimiter) {
		String returnString = "";

		if (list != null && list.size() > 0) {
			StringBuilder returnStringBuilder = new StringBuilder();
			for (Object string : list) {
				returnStringBuilder.append(string + delimiter);
			}
			returnString = returnStringBuilder.substring(0, returnStringBuilder.length() - delimiter.length());
		}
		return returnString;
	}

	public static <T> List<T> intersection(List<T> listOne, List<T> listTwo) {
		ArrayList<T> onlyInOne = new ArrayList<T>(listOne);
		onlyInOne.removeAll(listTwo);

		ArrayList<T> inBoth = new ArrayList<T>(listOne);
		inBoth.removeAll(onlyInOne);

		return inBoth;
	}

	private static class NormalComparator<T extends Comparable<T>> implements Comparator<T> {
		public int compare(T o1, T o2) {
			return o1.compareTo(o2);
		}
	}

	public static <T extends Comparable<T>> List<Integer> getSortedIndex(final List<T> list) {
		return getSortedIndex(list, new NormalComparator<T>());
	}

	public static <T> List<Integer> getSortedIndex(final List<T> list, final Comparator<T> comparator) {
		List<Integer> sortedIndexes = new ArrayList<Integer>();
		for (int i = 0; i < list.size(); i++) {
			sortedIndexes.add(i);
		}

		Collections.sort(sortedIndexes, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				T objectOne = list.get(o1);
				T objectTwo = list.get(o2);
				return comparator.compare(objectOne, objectTwo);
			}
		});

		return sortedIndexes;
	}

	public static List<Integer> convertToList(int[] values) {
		List<Integer> list = new ArrayList<Integer>();
		for (int value : values) {
			list.add(value);
		}
		return list;
	}

	public static <T extends Number> double sum(List<T> listOfNumbers) {
		double sum = 0;
		for (T number : listOfNumbers) {
			sum += number.doubleValue();
		}
		return sum;
	}
}
