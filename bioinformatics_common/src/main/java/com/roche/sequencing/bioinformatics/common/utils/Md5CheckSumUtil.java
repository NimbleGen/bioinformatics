package com.roche.sequencing.bioinformatics.common.utils;

import org.apache.commons.codec.digest.DigestUtils;

public class Md5CheckSumUtil {

	private Md5CheckSumUtil() {
		throw new AssertionError();
	}

	/**
	 * return the md5 checksum for the string (note: this would be equivalent to writing this string to a file and asking for the md5sum of the file)
	 * 
	 * @param string
	 * @return the md5sum of the string or null if there was an error calculating the md5sum
	 */
	public static String md5sum(String string) {
		return DigestUtils.md5Hex(string);
	}

}
