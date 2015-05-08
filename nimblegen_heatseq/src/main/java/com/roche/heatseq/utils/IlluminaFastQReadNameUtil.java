/**
 *   Copyright 2013 Roche NimbleGen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.roche.heatseq.utils;

/**
 * 
 * Class for Holding onto the details included in an Illumina Fastq header
 * 
 */
public class IlluminaFastQReadNameUtil {

	private IlluminaFastQReadNameUtil() {
		throw new AssertionError();
	}

	// Sample Illumina format prior to Casava 1.8
	// @MS5_15454:1:1110:12527:26507#26/1
	// Sample Illumina format as of Casava 1.8
	// @M01077:35:000000000-A3J96:1:1102:13646:7860 1:N:0:1
	/**
	 * Parse and return the unique identifier from the fastQ read header
	 * 
	 * @param readHeader
	 * @return
	 */
	public static String getUniqueIdForReadHeader(String readHeader) {
		return getUniqueIdForReadHeader(null, readHeader);
	}

	// Sample Illumina format prior to Casava 1.8
	// @MS5_15454:1:1110:12527:26507#26/1
	// Sample Illumina format as of Casava 1.8
	// @M01077:35:000000000-A3J96:1:1102:13646:7860 1:N:0:1
	/**
	 * Parse and return the unique identifier from the fastQ read header
	 * 
	 * @param readHeader
	 * @return
	 */
	public static String getUniqueIdForReadHeader(String commonReadNameBeginning, String readHeader) {
		if (commonReadNameBeginning != null && !commonReadNameBeginning.isEmpty()) {
			readHeader = readHeader.substring(commonReadNameBeginning.length());
		}

		int firstSpace = readHeader.indexOf(" ");
		int firstForwardSlash = readHeader.indexOf("/");

		if (firstSpace == -1) {
			firstSpace = Integer.MAX_VALUE;
		}

		if (firstForwardSlash < 0) {
			firstForwardSlash = Integer.MAX_VALUE;
		}

		int endIndex = Math.min(firstSpace, firstForwardSlash);
		endIndex = Math.min(endIndex, readHeader.length());

		String uniqueId = readHeader.substring(0, endIndex);

		return uniqueId;
	}
}
