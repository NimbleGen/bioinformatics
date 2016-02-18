package com.roche.sequencing.bioinformatics.common.utils;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CheckSumUtil {

	private static final long PRIME = 31;

	private CheckSumUtil() {
		throw new AssertionError();
	}

	public static long checkSum(List<String> stringList) {
		long result = 1;
		for (String string : stringList) {
			result = (result * PRIME) + checkSum(string);
		}
		return result;
	}

	public static long checkSum(Map<String, String> stringMap) {
		long result = 1;
		for (Entry<String, String> entry : stringMap.entrySet()) {
			result = (result * PRIME) + checkSum(entry.getKey());
			result = (result * PRIME) + checkSum(entry.getValue());
		}
		return result;
	}

	public static long checkSum(String[] stringArray) {
		long result = 1;
		for (String string : stringArray) {
			result = (result * PRIME) + checkSum(string);
		}
		return result;
	}

	public static long checkSum(String string) {
		return (long) string.hashCode();
	}

}
