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

package com.roche.sequencing.bioinformatics.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * Utility class to help parse delimited files
 * 
 */
public final class DelimitedFileParserUtil {
	private static final String CARRIAGE_RETURN = StringUtil.CARRIAGE_RETURN;

	// NOTE: WINDOWS_NEWLINE = CARRIAGE_RETURN + LINE_FEED;
	// NOTE: LINUX_NEWLINE = LINE_FEED;

	private DelimitedFileParserUtil() {
		throw new AssertionError();
	}

	/**
	 * @param delimitedFile
	 * @param headerNames
	 * @param columnDelimiter
	 * @return a list of row entries for each provided header name
	 * @throws IOException
	 */
	public static Map<String, List<String>> getHeaderNameToValuesMapFromDelimitedFile(File delimitedFile, String[] headerNames, String columnDelimiter) throws IOException {
		String fileName = delimitedFile.getAbsolutePath();
		Map<String, List<String>> headerNameToValuesMap = new HashMap<String, List<String>>();

		// walk down the rows until the header is found
		boolean headerFound = false;

		InputStream fileInputStream = new FileInputStream(delimitedFile);
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, Charset.forName("UTF-8")))) {
			String currentRow = null;
			while (!headerFound && ((currentRow = bufferedReader.readLine()) != null)) {
				// remove carriage return if this is a windows based file
				if (currentRow.endsWith(CARRIAGE_RETURN)) {
					currentRow = currentRow.substring(0, currentRow.length() - 1);
				}

				String[] parsedCurrentRow = null;

				if (currentRow != null) {
					parsedCurrentRow = currentRow.split(columnDelimiter);
				}

				if ((currentRow != null) && (parsedCurrentRow != null)) {
					int columnCount = parsedCurrentRow.length;

					Set<String> foundHeaderMatches = new HashSet<String>();

					for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
						String cellString = parsedCurrentRow[columnIndex];

						if (cellString != null) {
							headerNameLoop: for (String headerName : headerNames) {
								boolean matchFound = cellString.toLowerCase().startsWith(headerName.toLowerCase());

								if (matchFound) {
									foundHeaderMatches.add(headerName);

									break headerNameLoop;
								}
							}
						}
					}

					headerFound = foundHeaderMatches.size() >= headerNames.length;
				}
			}

			int linesOfData = 0;
			if (headerFound) {
				String headerRow = currentRow;
				String[] parsedHeaderRow = headerRow.split(columnDelimiter);

				Map<Integer, String> headerNameToColumnMap = getHeaderNameToColumnIndexMapping(headerNames, parsedHeaderRow);

				while ((currentRow = bufferedReader.readLine()) != null) {
					linesOfData++;
					// remove carriage return if this is a windows based file
					if (currentRow.endsWith(CARRIAGE_RETURN)) {
						currentRow = currentRow.substring(0, currentRow.length() - 1);
					}

					String[] parsedCurrentRow = currentRow.split(columnDelimiter);

					if (parsedCurrentRow != null) {
						Map<String, String> headerNameToValueMapFromRow = parseRow(headerNameToColumnMap, parsedCurrentRow);

						for (String headerName : headerNames) {
							String value = headerNameToValueMapFromRow.get(headerName);

							if (value == null) {
								value = "";
							}

							List<String> values = headerNameToValuesMap.get(headerName);

							if (values == null) {
								values = new ArrayList<String>();
							}

							values.add(value);

							headerNameToValuesMap.put(headerName, values);
						}

					}

				}
			}

			// make sure all the header names were found
			if (linesOfData > 0) {
				StringBuilder headerNamesNotFound = new StringBuilder();
				for (String headerName : headerNames) {
					if (!headerNameToValuesMap.containsKey(headerName)) {
						headerNamesNotFound.append(headerName + " ");
					}
				}
				if (headerNamesNotFound.length() > 0) {
					throw new IllegalStateException("Could not find the following header columns in file[" + fileName + "]: " + headerNamesNotFound.toString());
				}
			}
		}

		return headerNameToValuesMap;
	}

	/**
	 * 
	 * @param headerRow
	 * @return parsed header information or null if the information for all columns could not be attained.
	 */
	private static Map<Integer, String> getHeaderNameToColumnIndexMapping(String[] headerNames, String[] parsedHeaderRow) {
		Map<Integer, String> nameToColumnMapping = new HashMap<Integer, String>();

		int columnCount = parsedHeaderRow.length;

		for (int i = 0; i < columnCount; i++) {
			String value = parsedHeaderRow[i];

			if ((value != null) && !value.isEmpty()) {
				headerNameLoop: for (String headerName : headerNames) {
					if (value.toLowerCase().startsWith(headerName.toLowerCase())) {
						nameToColumnMapping.put(i, headerName);

						break headerNameLoop;
					}

				}
			}
		}

		return nameToColumnMapping;
	}

	private static Map<String, String> parseRow(Map<Integer, String> headerInfo, String[] parsedCurrentRow) {
		Map<String, String> headerNameToValueMap = new HashMap<String, String>();
		int columnCount = parsedCurrentRow.length;

		for (int i = 0; i < columnCount; i++) {
			String value = parsedCurrentRow[i];
			String headerName = headerInfo.get(i);

			if (headerName != null) {
				headerNameToValueMap.put(headerName, value);
			}
		}

		return headerNameToValueMap;
	}

}
