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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

	public static Map<String, String> parseNameDelimiterValueNewLineFile(File nameDelimiterValueNewLineFile, String nameValueDelimiter) throws IOException {
		Map<String, String> parsedNameValues = new HashMap<String, String>();

		InputStream fileInputStream = new FileInputStream(nameDelimiterValueNewLineFile);
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, Charset.forName("UTF-8")))) {
			String currentRow = null;
			while ((currentRow = bufferedReader.readLine()) != null) {
				String[] rowValues = currentRow.split(nameValueDelimiter);
				if (rowValues.length > 1) {
					parsedNameValues.put(rowValues[0], rowValues[1]);
				}
			}
		}

		return parsedNameValues;
	}

	/**
	 * Advances the buffered reader to the header line and returns the header line as a string
	 * 
	 * @param delimitedFile
	 * @param headerNames
	 * @param columnDelimiter
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	private static String findHeaderLine(String[] headerNames, String columnDelimiter, BufferedReader reader) throws IOException {
		// walk down the rows until the header is found
		boolean headerFound = false;

		String headerLine = null;

		String currentRow = null;
		while (!headerFound && ((currentRow = reader.readLine()) != null)) {
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
		if (headerFound) {
			headerLine = currentRow;
		}
		return headerLine;
	}

	private static class DelimitedFileLineIterator implements Iterator<Map<String, String>> {

		private final BufferedReader reader;
		private final String columnDelimiter;
		private final Map<Integer, String> headerNameToColumnMap;
		private String nextLine;

		public DelimitedFileLineIterator(String columnDelimiter, BufferedReader reader, Map<Integer, String> headerNameToColumnMap) throws IOException {
			this.columnDelimiter = columnDelimiter;
			this.reader = reader;
			this.headerNameToColumnMap = headerNameToColumnMap;
			this.nextLine = reader.readLine();
		}

		@Override
		public boolean hasNext() {
			boolean hasNext = nextLine != null;
			if (!hasNext) {
				try {
					reader.close();
				} catch (IOException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			}
			return hasNext;
		}

		@Override
		public Map<String, String> next() {
			Map<String, String> headerNameToValueMapFromRow = null;
			String currentRow = nextLine;

			try {
				nextLine = reader.readLine();
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}

			if (currentRow.endsWith(CARRIAGE_RETURN)) {
				currentRow = currentRow.substring(0, currentRow.length() - 1);
			}

			String[] parsedCurrentRow = currentRow.split(columnDelimiter);

			if (parsedCurrentRow != null) {
				headerNameToValueMapFromRow = parseRow(headerNameToColumnMap, parsedCurrentRow);
			}

			return headerNameToValueMapFromRow;
		}

		@Override
		public void remove() {
			throw new IllegalStateException("This method is not implemented.");
		}

	}

	public static Map<Integer, String> getHeaderIndexToHeaderNameMap(File delimitedFile, String[] headerNames, String columnDelimiter, boolean extractAdditionalHeaderNames) throws IOException {
		Map<Integer, String> headerColumnToNameMap = null;
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(delimitedFile), Charset.forName("UTF-8")))) {

			String headerLine = findHeaderLine(headerNames, columnDelimiter, bufferedReader);
			boolean headerFound = headerLine != null;
			if (headerFound) {
				String[] parsedHeaderRow = headerLine.split(columnDelimiter);
				headerColumnToNameMap = getColumnIndexToHeaderNameMapping(headerNames, parsedHeaderRow, extractAdditionalHeaderNames);
			}
		}

		return headerColumnToNameMap;
	}

	public static Iterator<Map<String, String>> getHeaderNameToValueMapRowIteratorFromDelimitedFile(File delimitedFile, String[] headerNames, String columnDelimiter)
			throws UnableToFindHeaderException, IOException {
		return getHeaderNameToValueMapRowIteratorFromDelimitedFile(delimitedFile, headerNames, columnDelimiter, false);
	}

	public static Iterator<Map<String, String>> getHeaderNameToValueMapRowIteratorFromDelimitedFile(File delimitedFile, String[] headerNames, String columnDelimiter,
			boolean extractAdditionalHeaderNames) throws UnableToFindHeaderException, IOException {

		DelimitedFileLineIterator delimitedFileLineIterator = null;
		// note this buffered reader is close when hasNext returns false (in DelimitedFileLineIterator)
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(delimitedFile), Charset.forName("UTF-8")));

		String headerLine = findHeaderLine(headerNames, columnDelimiter, bufferedReader);
		boolean headerFound = headerLine != null;
		if (headerFound) {
			String[] parsedHeaderRow = headerLine.split(columnDelimiter);

			Map<Integer, String> headerNameToColumnMap = getColumnIndexToHeaderNameMapping(headerNames, parsedHeaderRow, extractAdditionalHeaderNames);
			delimitedFileLineIterator = new DelimitedFileLineIterator(columnDelimiter, bufferedReader, headerNameToColumnMap);
		}

		return delimitedFileLineIterator;
	}

	/**
	 * @param delimitedFile
	 * @param headerNames
	 * @param columnDelimiter
	 * @return a list of row entries for each provided header name
	 * @throws IOException
	 */
	public static Map<String, List<String>> getHeaderNameToValuesMapFromDelimitedFile(File delimitedFile, String[] headerNames, String columnDelimiter) throws UnableToFindHeaderException, IOException {
		return getHeaderNameToValuesMapFromDelimitedFile(delimitedFile, headerNames, columnDelimiter, false);
	}

	/**
	 * @param delimitedFile
	 * @param headerNames
	 * @param columnDelimiter
	 * @return a list of row entries for each provided header name
	 * @throws IOException
	 */
	public static Map<String, List<String>> getHeaderNameToValuesMapFromDelimitedFile(File delimitedFile, String[] headerNames, String columnDelimiter, boolean extractAdditionalHeaderNames)
			throws UnableToFindHeaderException, IOException {
		return getHeaderNameToValuesMapFromDelimitedFile(new FileInputStream(delimitedFile), headerNames, columnDelimiter, extractAdditionalHeaderNames);
	}

	/**
	 * @param delimitedFile
	 * @param headerNames
	 * @param columnDelimiter
	 * @return a list of row entries for each provided header name
	 * @throws IOException
	 */
	public static Map<String, List<String>> getHeaderNameToValuesMapFromDelimitedFile(InputStream delimitedInputStream, String[] headerNames, String columnDelimiter) throws IOException {
		return getHeaderNameToValuesMapFromDelimitedFile(delimitedInputStream, headerNames, columnDelimiter, false);
	}

	/**
	 * @param delimitedFile
	 * @param headerNames
	 * @param columnDelimiter
	 * @return a list of row entries for each provided header name
	 * @throws IOException
	 */
	public static Map<String, List<String>> getHeaderNameToValuesMapFromDelimitedFile(InputStream delimitedInputStream, String[] headerNames, String columnDelimiter,
			boolean extractAdditionalHeaderNames) throws IOException {

		Map<String, List<String>> headerNameToValuesMap = new HashMap<String, List<String>>();

		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(delimitedInputStream, Charset.forName("UTF-8")))) {

			String headerLine = findHeaderLine(headerNames, columnDelimiter, bufferedReader);
			boolean headerFound = headerLine != null;
			int linesOfData = 0;
			if (headerFound) {
				String[] parsedHeaderRow = headerLine.split(columnDelimiter);

				Map<Integer, String> columnToHeaderNameMap = getColumnIndexToHeaderNameMapping(headerNames, parsedHeaderRow, extractAdditionalHeaderNames);
				String currentRow = null;
				while ((currentRow = bufferedReader.readLine()) != null) {
					linesOfData++;
					// remove carriage return if this is a windows based file
					if (currentRow.endsWith(CARRIAGE_RETURN)) {
						currentRow = currentRow.substring(0, currentRow.length() - 1);
					}

					String[] parsedCurrentRow = currentRow.split(columnDelimiter);

					if (parsedCurrentRow != null) {
						Map<String, String> headerNameToValueMapFromRow = parseRow(columnToHeaderNameMap, parsedCurrentRow);

						for (String headerName : headerNameToValueMapFromRow.keySet()) {
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
			} else {
				StringBuilder headerNamesAsString = new StringBuilder();
				for (String headerName : headerNames) {
					headerNamesAsString.append(headerName + " ");
				}
				throw new UnableToFindHeaderException("Could not find header containing header names[" + headerNamesAsString.toString() + "].");
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
					throw new UnableToFindHeaderException("Could not find the following header columns: " + headerNamesNotFound.toString());
				}
			}
		}

		return headerNameToValuesMap;
	}

	/**
	 * 
	 * @param headerRow
	 * @return parsed header information for provided header names and any additional headers if extractAdditionalHeaderNames is true.
	 */
	private static Map<Integer, String> getColumnIndexToHeaderNameMapping(String[] headerNames, String[] parsedHeaderRow, boolean extractAdditionalHeaderNames) {
		Map<Integer, String> columnToNameMapping = new HashMap<Integer, String>();

		int columnCount = parsedHeaderRow.length;

		for (int i = 0; i < columnCount; i++) {
			String value = parsedHeaderRow[i];

			if ((value != null) && !value.isEmpty()) {
				headerNameLoop: for (String headerName : headerNames) {
					if (value.toLowerCase().startsWith(headerName.toLowerCase())) {
						columnToNameMapping.put(i, headerName);
						break headerNameLoop;
					}

				}

				boolean requiredHeaderWasFoundAtThisIndex = columnToNameMapping.containsKey(i);
				if (!requiredHeaderWasFoundAtThisIndex && extractAdditionalHeaderNames) {
					columnToNameMapping.put(i, value);
				}

			}

		}

		return columnToNameMapping;
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

	public static boolean isHeaderNameFoundInHeader(String headerName, File delimitedFile, String columnDelimiter) throws IOException {
		boolean headerWithNameFound = false;
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(delimitedFile), Charset.forName("UTF-8")))) {
			String headerLine = findHeaderLine(new String[] { headerName }, columnDelimiter, bufferedReader);
			headerWithNameFound = headerLine != null;
		}

		return headerWithNameFound;
	}

	public static void filterFileBasedOnColumnValues(File delimitedFile, File reducedFilteredFile, String columnDelimiter, Map<String, String[]> headerNameToAcceptableValuesMapping)
			throws IOException {
		// create a header file out of all the headerNames in the map
		String[] headerNames = headerNameToAcceptableValuesMapping.keySet().toArray(new String[0]);

		String headerLine = "";
		Map<Integer, String> columnToHeaderNameMap = null;
		boolean headerFound = false;

		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(delimitedFile), Charset.forName("UTF-8")))) {
			headerLine = findHeaderLine(headerNames, columnDelimiter, bufferedReader);
			headerFound = headerLine != null;
			if (headerFound) {
				String[] parsedHeaderRow = headerLine.split(columnDelimiter);
				columnToHeaderNameMap = getColumnIndexToHeaderNameMapping(headerNames, parsedHeaderRow, false);
			}
		}

		if (headerFound) {
			try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(reducedFilteredFile))) {

				try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(delimitedFile), Charset.forName("UTF-8")))) {
					boolean pastHeaderLine = false;
					String currentRow = null;
					while ((currentRow = bufferedReader.readLine()) != null) {

						if (pastHeaderLine) {
							// remove carriage return if this is a windows based file
							if (currentRow.endsWith(CARRIAGE_RETURN)) {
								currentRow = currentRow.substring(0, currentRow.length() - 1);
							}

							String[] parsedCurrentRow = currentRow.split(columnDelimiter);

							if (parsedCurrentRow != null) {
								Map<String, String> headerNameToValueMapFromRow = parseRow(columnToHeaderNameMap, parsedCurrentRow);

								boolean isValueAcceptable = true;
								headerLoop: for (String headerName : headerNameToValueMapFromRow.keySet()) {

									String[] acceptableValues = headerNameToAcceptableValuesMapping.get(headerName);

									String value = headerNameToValueMapFromRow.get(headerName);

									if (value == null) {
										value = "";
									}

									boolean matchesAnAcceptableValue = false;
									if (acceptableValues != null) {
										acceptableLoop: for (String acceptableValue : acceptableValues) {
											matchesAnAcceptableValue = value.equals(acceptableValue);
											if (matchesAnAcceptableValue) {
												break acceptableLoop;
											}
										}
									}

									isValueAcceptable = acceptableValues == null || acceptableValues.length == 0 || matchesAnAcceptableValue;
									if (!isValueAcceptable) {
										break headerLoop;
									}

								}
								if (isValueAcceptable) {
									bufferedWriter.write(currentRow + StringUtil.NEWLINE);
								}

							}
						} else {
							pastHeaderLine = currentRow.equals(headerLine);
							bufferedWriter.write(currentRow + StringUtil.NEWLINE);
						}
					}
				}
			}

		} else {
			StringBuilder headerNamesAsString = new StringBuilder();
			for (String headerName : headerNames) {
				headerNamesAsString.append(headerName + " ");
			}
			throw new UnableToFindHeaderException("Could not find header containing header names[" + headerNamesAsString.toString() + "] in file[" + delimitedFile.getAbsolutePath() + "].");
		}
	}
}
