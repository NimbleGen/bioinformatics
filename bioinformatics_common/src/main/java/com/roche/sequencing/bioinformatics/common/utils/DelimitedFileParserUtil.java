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

	// private static final Logger logger = LoggerFactory.getLogger(DelimitedFileParserUtil.class);

	private static final String CARRIAGE_RETURN = "" + StringUtil.CARRIAGE_RETURN;

	// NOTE: WINDOWS_NEWLINE = CARRIAGE_RETURN + LINE_FEED;
	// NOTE: LINUX_NEWLINE = LINE_FEED;

	private final static int BUFFER_SIZE = 131072;

	private static final Charset[] CHARSETS_TO_TRY = new Charset[] { Charset.forName("UTF-16"), Charset.forName("UTF-8") };

	private DelimitedFileParserUtil() {
		throw new AssertionError();
	}

	public static Map<String, String> parseNameDelimiterValueNewLineFile(File nameDelimiterValueNewLineFile, String nameValueDelimiter, Charset charset) throws IOException {
		Map<String, String> parsedNameValues = new HashMap<String, String>();

		InputStream fileInputStream = new FileInputStream(nameDelimiterValueNewLineFile);
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, charset))) {
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

	private static class Header {
		private final Charset charsetUsed;
		private final String headLine;
		private final int linesPriorToHeader;

		public Header(Charset charsetUsed, String headLine, int linesPriorToHeader) {
			super();
			this.charsetUsed = charsetUsed;
			this.headLine = headLine;
			this.linesPriorToHeader = linesPriorToHeader;
		}

		public Charset getCharsetUsed() {
			return charsetUsed;
		}

		public String getHeadLine() {
			return headLine;
		}

		public int getLinesPriorToHeader() {
			return linesPriorToHeader;
		}

	}

	private static Header findHeaderLine(String[] headerNames, String columnDelimiter, InputStreamFactory delimitedContentInputStreamFactory) throws IOException {
		Header header = null;
		charsetLoop: for (Charset charset : CHARSETS_TO_TRY) {
			header = findHeaderLine(headerNames, columnDelimiter, delimitedContentInputStreamFactory, charset);
			if (header != null) {
				break charsetLoop;
			}
		}
		return header;
	}

	/**
	 * 
	 * @param headerNames
	 * @param columnDelimiter
	 * @param delimitedFile
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	private static Header findHeaderLine(String[] headerNames, String columnDelimiter, InputStreamFactory delimitedContentInputStreamFactory, Charset charset) throws IOException {
		// walk down the rows until the header is found
		boolean headerFound = false;
		Header header = null;

		String currentRow = null;
		int linesRead = 0;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(delimitedContentInputStreamFactory.createInputStream(), charset), BUFFER_SIZE)) {

			while (!headerFound && ((currentRow = reader.readLine()) != null)) {
				// add one for the newline
				linesRead++;
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
						String cellString = parsedCurrentRow[columnIndex].trim();

						if (cellString != null) {
							headerNameLoop: for (String headerName : headerNames) {
								boolean matchFound = cellString.trim().toLowerCase().equals(headerName.trim().toLowerCase());

								if (matchFound) {
									foundHeaderMatches.add(headerName.trim().toLowerCase());

									break headerNameLoop;
								}
							}
						}
					}

					headerFound = foundHeaderMatches.size() >= headerNames.length;
				}
			}

			if (headerFound) {
				header = new Header(charset, currentRow, linesRead - 1);
			}
		}
		return header;
	}

	public static Map<String, String> parseCommentLinesNameValuePairs(File delimitedFile) throws IOException {
		Map<String, String> nameValuePairsMap = new HashMap<String, String>();
		String firstLine = FileUtil.readFirstLineAsString(delimitedFile);
		if (firstLine != null && firstLine.startsWith("#")) {
			String firstLineWithoutPound = firstLine.substring(1, firstLine.length());
			String[] nameValuePairs = firstLineWithoutPound.split(" ");
			for (String nameValuePair : nameValuePairs) {
				String[] splitNameValuePair = nameValuePair.split("=");
				if (splitNameValuePair.length == 1) {
					// flag variable
					nameValuePairsMap.put(splitNameValuePair[0], null);
				} else if (splitNameValuePair.length == 2) {
					nameValuePairsMap.put(splitNameValuePair[0], splitNameValuePair[1]);
				}
			}
		}
		return nameValuePairsMap;

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
				headerNameToValueMapFromRow = parseRow(headerNameToColumnMap, parsedCurrentRow, null);
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

		Header header = findHeaderLine(headerNames, columnDelimiter, new InputStreamFactory(delimitedFile));
		boolean headerFound = header != null;
		if (headerFound) {
			String[] parsedHeaderRow = header.getHeadLine().split(columnDelimiter);
			headerColumnToNameMap = getColumnIndexToHeaderNameMapping(headerNames, parsedHeaderRow, extractAdditionalHeaderNames);
		}

		return headerColumnToNameMap;
	}

	public static Iterator<Map<String, String>> getHeaderNameToValueMapRowIteratorFromDelimitedFile(File delimitedFile, String[] headerNames, String columnDelimiter)
			throws UnableToFindHeaderException, IOException {
		return getHeaderNameToValueMapRowIteratorFromDelimitedFile(delimitedFile, headerNames, columnDelimiter, false);
	}

	private static void skipLines(BufferedReader reader, long numberOfLinesToSkip) throws IOException {
		for (int i = 0; i < numberOfLinesToSkip; i++) {
			reader.readLine();
		}
	}

	public static Iterator<Map<String, String>> getHeaderNameToValueMapRowIteratorFromDelimitedFile(File delimitedFile, String[] headerNames, String columnDelimiter,
			boolean extractAdditionalHeaderNames) throws UnableToFindHeaderException, IOException {

		DelimitedFileLineIterator delimitedFileLineIterator = null;

		Header header = findHeaderLine(headerNames, columnDelimiter, new InputStreamFactory(delimitedFile));
		boolean headerFound = header != null;
		if (headerFound) {
			// note this buffered reader is close when hasNext returns false (in DelimitedFileLineIterator)
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(delimitedFile), header.getCharsetUsed()));
			skipLines(bufferedReader, header.getLinesPriorToHeader() + 1);
			String[] parsedHeaderRow = header.getHeadLine().split(columnDelimiter);

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
		return getHeaderNameToValuesMapFromDelimitedFile(new InputStreamFactory(delimitedFile), headerNames, columnDelimiter, extractAdditionalHeaderNames);
	}

	/**
	 * @param delimitedFile
	 * @param headerNames
	 * @param columnDelimiter
	 * @return a list of row entries for each provided header name
	 * @throws IOException
	 */
	public static Map<String, List<String>> getHeaderNameToValuesMapFromDelimitedFile(InputStreamFactory delimitedInputStream, String[] headerNames, String columnDelimiter) throws IOException {
		return getHeaderNameToValuesMapFromDelimitedFile(delimitedInputStream, headerNames, columnDelimiter, false);
	}

	/**
	 * @param delimitedFile
	 * @param headerNames
	 * @param columnDelimiter
	 * @return a list of row entries for each provided header name
	 * @throws IOException
	 */
	public static Map<String, List<String>> getHeaderNameToValuesMapFromDelimitedFile(InputStreamFactory delimitedInputStreamFactory, String[] headerNames, String columnDelimiter,
			boolean extractAdditionalHeaderNames) throws IOException {
		DefaultLineParser lineParser = new DefaultLineParser();
		parseFile(delimitedInputStreamFactory, headerNames, lineParser, columnDelimiter, extractAdditionalHeaderNames);
		return lineParser.getHeaderNameToValuesMap();
	}

	private static class DefaultLineParser implements IDelimitedLineParser {

		private final Map<String, List<String>> headerNameToValuesMap;

		public DefaultLineParser() {
			headerNameToValuesMap = new HashMap<String, List<String>>();
		}

		@Override
		public void parseDelimitedLine(Map<String, String> headerNameToValueMapFromRow) {
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

		public void doneParsing(int linesOfData, String[] headerNames) {
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

		public Map<String, List<String>> getHeaderNameToValuesMap() {
			return headerNameToValuesMap;
		}

		@Override
		public void threadInterrupted() {
			headerNameToValuesMap.clear();
		}
	}

	/**
	 * @param delimitedFile
	 * @param headerNames
	 * @param columnDelimiter
	 * @return a list of row entries for each provided header name
	 * @throws IOException
	 */
	public static void parseFile(InputStreamFactory delimitedInputStreamFactory, String[] headerNames, IDelimitedLineParser lineParser, String columnDelimiter, boolean extractAdditionalHeaderNames)
			throws IOException {

		Header header = findHeaderLine(headerNames, columnDelimiter, delimitedInputStreamFactory);
		boolean headerFound = header != null;
		boolean wasInterrupted = false;
		int linesOfData = 0;
		if (headerFound) {
			String[] parsedHeaderRow = header.getHeadLine().split(columnDelimiter);

			Map<Integer, String> columnToHeaderNameMap = getColumnIndexToHeaderNameMapping(headerNames, parsedHeaderRow, extractAdditionalHeaderNames);

			Map<String, String> headerNameToValueMapFromRow = new HashMap<String, String>();

			InputStream inputStream = delimitedInputStreamFactory.createInputStream();
			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, header.getCharsetUsed()), BUFFER_SIZE)) {
				skipLines(bufferedReader, header.getLinesPriorToHeader() + 1);
				String currentRow = null;
				rowLoop: while ((currentRow = bufferedReader.readLine()) != null) {
					linesOfData++;
					// remove carriage return if this is a windows based file
					currentRow = currentRow.replace(CARRIAGE_RETURN, "");

					String[] parsedCurrentRow = currentRow.split(columnDelimiter);

					if (parsedCurrentRow != null) {
						headerNameToValueMapFromRow = parseRow(columnToHeaderNameMap, parsedCurrentRow, headerNameToValueMapFromRow);

						lineParser.parseDelimitedLine(headerNameToValueMapFromRow);
					}

					if (linesOfData % 1000 == 0 && Thread.currentThread().isInterrupted()) {
						wasInterrupted = true;
						break rowLoop;
					}
				}
			}
		} else {
			StringBuilder headerNamesAsString = new StringBuilder();
			for (String headerName : headerNames) {
				headerNamesAsString.append(headerName + " ");
			}
			throw new UnableToFindHeaderException("Could not find header containing header names[" + headerNamesAsString.toString() + "] in file[" + delimitedInputStreamFactory.getName() + "].");
		}

		if (wasInterrupted) {
			lineParser.threadInterrupted();
		} else {
			lineParser.doneParsing(linesOfData, headerNames);
		}
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
					if (value.trim().toLowerCase().equals(headerName.trim().toLowerCase())) {
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

	private static Map<String, String> parseRow(Map<Integer, String> headerInfo, String[] parsedCurrentRow, Map<String, String> returnMapToPopulate) {
		Map<String, String> headerNameToValueMap = null;
		if (returnMapToPopulate != null) {
			headerNameToValueMap = returnMapToPopulate;
			headerNameToValueMap.clear();
		} else {
			headerNameToValueMap = new HashMap<String, String>();
		}

		int columnCount = parsedCurrentRow.length;
		Set<Integer> columnsUsed = new HashSet<Integer>();
		for (int i = 0; i < columnCount; i++) {
			String value = parsedCurrentRow[i];
			String headerName = headerInfo.get(i);
			columnsUsed.add(i);

			if (headerName != null) {
				headerNameToValueMap.put(headerName, value);
			}
		}

		for (int i = 0; i < headerInfo.size(); i++) {
			if (!columnsUsed.contains(i)) {
				String headerName = headerInfo.get(i);
				headerNameToValueMap.put(headerName, null);
			}
		}

		return headerNameToValueMap;
	}

	public static boolean isHeaderNameFoundInHeader(String headerName, File delimitedFile, String columnDelimiter) throws IOException {
		boolean headerWithNameFound = false;

		Header header = findHeaderLine(new String[] { headerName }, columnDelimiter, new InputStreamFactory(delimitedFile));
		headerWithNameFound = header != null;

		return headerWithNameFound;
	}

	public static void filterFileBasedOnColumnValues(File delimitedFile, File reducedFilteredFile, String columnDelimiter, Map<String, String[]> headerNameToAcceptableValuesMapping,
			Map<String, String[]> headerNameToExceptionalValuesToIncludeMapping) throws IOException {
		// create a header file out of all the headerNames in the map
		Set<String> headerNamesAsSet = new HashSet<String>();
		headerNamesAsSet.addAll(headerNameToAcceptableValuesMapping.keySet());
		headerNamesAsSet.addAll(headerNameToExceptionalValuesToIncludeMapping.keySet());
		String[] headerNames = headerNamesAsSet.toArray(new String[0]);

		Map<Integer, String> columnToHeaderNameMap = null;
		boolean headerFound = false;

		Header header = findHeaderLine(headerNames, columnDelimiter, new InputStreamFactory(delimitedFile));
		headerFound = header != null;
		if (headerFound) {
			String[] parsedHeaderRow = header.getHeadLine().split(columnDelimiter);
			columnToHeaderNameMap = getColumnIndexToHeaderNameMapping(headerNames, parsedHeaderRow, false);
		}

		if (headerFound) {
			try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(reducedFilteredFile))) {

				Map<String, String> headerNameToValueMapFromRow = new HashMap<String, String>();

				try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(delimitedFile), header.getCharsetUsed()))) {
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
								headerNameToValueMapFromRow = parseRow(columnToHeaderNameMap, parsedCurrentRow, headerNameToValueMapFromRow);

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

								boolean isValueExceptional = false;
								headerLoop: for (String headerName : headerNameToValueMapFromRow.keySet()) {

									String[] exceptionalValues = headerNameToExceptionalValuesToIncludeMapping.get(headerName);
									if (exceptionalValues != null) {
										String value = headerNameToValueMapFromRow.get(headerName);

										if (value == null) {
											value = "";
										}

										if (exceptionalValues != null) {
											exceptionalLoop: for (String exceptionalValue : exceptionalValues) {
												isValueExceptional = value.equals(exceptionalValue);
												if (isValueExceptional) {
													break exceptionalLoop;
												}
											}
										}

										if (isValueExceptional) {
											break headerLoop;
										}
									}
								}

								if (isValueAcceptable || isValueExceptional) {
									bufferedWriter.write(currentRow + StringUtil.NEWLINE);
								}

							}
						} else {
							pastHeaderLine = currentRow.equals(header.getHeadLine());
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
