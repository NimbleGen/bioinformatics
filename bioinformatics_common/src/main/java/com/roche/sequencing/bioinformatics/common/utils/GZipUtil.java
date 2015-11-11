/*
 *    Copyright 2013 Roche NimbleGen Inc.
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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public final class GZipUtil {

	private GZipUtil() {
		throw new AssertionError();
	}

	public static boolean isCompressed(File file) throws FileNotFoundException, IOException {
		int numberOfBytes = (int) Math.min(file.length(), 10);
		byte[] bytes = new byte[numberOfBytes];
		try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
			dis.readFully(bytes);
		}
		return isCompressed(bytes);
	}

	/*
	 * Determines if a byte array is compressed. The java.util.zip GZip implementaiton does not expose the GZip header so it is difficult to determine if a string is compressed.
	 * 
	 * @param bytes an array of bytes
	 * 
	 * @return true if the array is compressed or false otherwise
	 * 
	 * @throws java.io.IOException if the byte array couldn't be read
	 */
	public static boolean isCompressed(byte[] bytes) throws IOException {
		boolean isCompressed = false;
		if ((bytes != null) && (bytes.length >= 2)) {
			isCompressed = ((bytes[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8)));
		}
		return isCompressed;
	}
}
