package com.roche.sequencing.bioinformatics.common.utils;

import java.util.ArrayList;
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

	public static <T> List<T> intersection(List<T> listOne, List<T> listTwo) {
		ArrayList<T> onlyInOne = new ArrayList<T>(listOne);
		onlyInOne.removeAll(listTwo);

		ArrayList<T> inBoth = new ArrayList<T>(listOne);
		inBoth.removeAll(onlyInOne);

		return inBoth;
	}
}
