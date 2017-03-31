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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import com.roche.sequencing.bioinformatics.common.utils.gzip.GZipUtil;

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

	public static String md5Sum(File file) throws FileNotFoundException, IOException {
		String md5Sum = null;
		try (FileInputStream fis = new FileInputStream(file)) {
			md5Sum = DigestUtils.md5Hex(fis);
		}
		return md5Sum;
	}

	public static String md5Sum(Class<?> resourceReferenceClass, String resourceName) throws FileNotFoundException, IOException {
		String md5Sum = null;
		try (InputStream is = resourceReferenceClass.getResourceAsStream(resourceName)) {
			md5Sum = DigestUtils.md5Hex(is);
		}
		return md5Sum;
	}

	public static String md5SumWithSkippingCommentLines(File file) throws FileNotFoundException, IOException {
		return md5SumWithSkippingCommentLines(new InputStreamFactory(file));
	}

	public static String md5SumWithSkippingCommentLines(IInputStreamFactory inputStreamFactory) throws FileNotFoundException, IOException {
		InputStream inputStream;
		if (GZipUtil.isCompressed(inputStreamFactory)) {
			inputStream = new GZIPInputStream(inputStreamFactory.createInputStream());
		} else {
			inputStream = new BufferedInputStream(inputStreamFactory.createInputStream());
		}

		MessageDigest md5SumDigest = null;
		try {
			md5SumDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError();
		}

		try {
			byte[] character = new byte[1024];
			StringBuilder currentLine = new StringBuilder();
			int readChars = 0;
			while ((readChars = inputStream.read(character)) != -1) {
				for (int i = 0; i < readChars; ++i) {
					char currentCharacter = (char) character[i];
					if (currentCharacter == StringUtil.NEWLINE_SYMBOL) {
						currentLine.append(currentCharacter);
						String line = currentLine.toString();
						if (!line.startsWith("#")) {
							md5SumDigest.update(line.getBytes());
						}

						currentLine = new StringBuilder();

					} else {
						currentLine.append(currentCharacter);
					}
				}
			}
		} finally {
			inputStream.close();
		}
		String md5Sum = Hex.encodeHexString(md5SumDigest.digest());
		return md5Sum;
	}
}
