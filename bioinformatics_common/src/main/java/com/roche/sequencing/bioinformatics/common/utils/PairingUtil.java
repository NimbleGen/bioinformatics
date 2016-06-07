package com.roche.sequencing.bioinformatics.common.utils;

import java.util.HashMap;
import java.util.Map;

public class PairingUtil {

	private PairingUtil() {
		throw new AssertionError();
	}

	public static int cantorize(final int largerNumber, final int smallerNumber) {
		return (((largerNumber + smallerNumber) * (largerNumber + smallerNumber + 1)) / 2) + smallerNumber;
	}

	public static void main(String[] args) {
		int max = 1000000;
		Map<Integer, String> set = new HashMap<Integer, String>();
		for (int i = 0; i < max; i++) {
			for (int j = 0; j < max; j++) {
				if (j < i) {
					int key = cantorize(i, j);
					if (set.containsKey(key)) {
						System.out.println("conflict i:" + i + " j:" + j + " key:" + key + " with " + set.get(key));
					} else {
						set.put(key, "i:" + i + " j:" + j);
					}
				}
			}
		}
	}

}
