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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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

	public static String md5sum(File file) throws FileNotFoundException, IOException {
		String md5Sum = null;
		try (FileInputStream fis = new FileInputStream(file)) {
			md5Sum = DigestUtils.md5Hex(fis);
		}
		return md5Sum;
	}

	public static String md5sum(Class<?> resourceReferenceClass, String resourceName) throws FileNotFoundException, IOException {
		String md5Sum = null;
		try (InputStream is = resourceReferenceClass.getResourceAsStream(resourceName)) {
			md5Sum = DigestUtils.md5Hex(is);
		}
		return md5Sum;
	}
}
