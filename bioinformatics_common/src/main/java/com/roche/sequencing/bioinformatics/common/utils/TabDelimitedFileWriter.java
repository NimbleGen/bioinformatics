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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;

public class TabDelimitedFileWriter implements AutoCloseable {
	// 1 mb buffer
	private final static int DEFAULT_BUFFER_SIZE = (int) (Math.pow(1024, 2));
	private static final DecimalFormat decimalFormat = new DecimalFormat("##.##");
	private Writer writer = null;
	private int columnCount = -1;
	private final boolean shouldValidateColumnCount;
	private final File outputFile;

	/**
	 * Make a tab delimited file writer with the provided output file
	 * 
	 * @param outputFile
	 * @throws IOException
	 */
	public TabDelimitedFileWriter(File outputFile) throws IOException {
		this(outputFile, null, new String[0], false);
	}

	/**
	 * Make a tab delimited file writer with the provided output file and header line
	 * 
	 * @param outputFile
	 * @param headers
	 * @throws IOException
	 */
	public TabDelimitedFileWriter(File outputFile, String[] headers) throws IOException {
		this(outputFile, null, headers, true);
	}

	/**
	 * Make a tab delimited file writer with the provided output file and header line
	 * 
	 * @param outputFile
	 * @param headers
	 * @throws IOException
	 */
	public TabDelimitedFileWriter(File outputFile, String preHeader, String[] headers) throws IOException {
		this(outputFile, preHeader, headers, true);
	}

	private TabDelimitedFileWriter(File outputFile, String preHeader, String[] headers, boolean shouldValidateColumnCount) throws IOException {
		// Keep track of how many columns we should expect to see
		this.outputFile = outputFile;
		this.columnCount = headers.length;
		this.writer = new BufferedWriter(new FileWriter(outputFile), DEFAULT_BUFFER_SIZE);
		boolean firstHeader = true;
		synchronized (this) {
			if (preHeader != null && !preHeader.isEmpty()) {
				writer.write(preHeader + StringUtil.NEWLINE);
			}

			for (String header : headers) {
				if (!firstHeader) {
					writer.write(StringUtil.TAB);
				}
				firstHeader = false;
				writer.write(header);
			}
			writer.write(StringUtil.NEWLINE);
			writer.flush();
		}
		this.shouldValidateColumnCount = false;
	}

	/**
	 * Write the provided values to the file. Ensures that the number of values matches the number of header columns. Formats numeric values consistently.
	 * 
	 * @param values
	 */
	public void writeLineFromArray(Object[] values) {
		synchronized (this) {
			try {
				if (writer == null) {
					throw new IllegalArgumentException("Trying to write a line to a writer thas has been closed");
				}

				if (shouldValidateColumnCount && values.length != columnCount) {
					throw new IllegalArgumentException("Passed in " + values.length + " values to a writer with " + columnCount + " columns.");
				}

				boolean firstValue = true;
				for (Object value : values) {
					if (!firstValue) {
						writer.write(StringUtil.TAB);
					}
					firstValue = false;

					if (value != null) {
						if (value instanceof Number) {
							// We want all numeric values to be formatted
							writer.write(decimalFormat.format(value));
						} else {
							// Convert the value to a string
							writer.write(value.toString());
						}
					}
				}
				writer.write(StringUtil.NEWLINE);
				writer.flush();
			} catch (IOException e) {
				throw new IllegalStateException("Unable to write to file[" + outputFile.getAbsolutePath() + "].");
			}
		}
	}

	/**
	 * Write the provided values to the file. Ensures that the number of values matches the number of header columns. Formats numeric values consistently.
	 * 
	 * @param values
	 */
	public void writeLine(Object... values) {
		synchronized (this) {
			try {
				if (writer == null) {
					throw new IllegalArgumentException("Trying to write a line to a writer thas has been closed");
				}

				if (shouldValidateColumnCount && values.length != columnCount) {
					throw new IllegalArgumentException("Passed in " + values.length + " values to a writer with " + columnCount + " columns.");
				}

				boolean firstValue = true;
				for (Object value : values) {
					if (!firstValue) {
						writer.write(StringUtil.TAB);
					}
					firstValue = false;

					if (value != null) {
						if (value instanceof Number) {
							// We want all numeric values to be formatted
							writer.write(decimalFormat.format(value));
						} else {
							// Convert the value to a string
							writer.write(value.toString());
						}
					}
				}
				writer.write(StringUtil.NEWLINE);
				writer.flush();
			} catch (IOException e) {
				throw new IllegalStateException("Unable to write to file[" + outputFile.getAbsolutePath() + "].");
			}
		}
	}

	/**
	 * Close the underlying print writer. Once a TabDelimitedFileWriter has been closed it can no longer be written to.
	 */
	public void close() {
		synchronized (this) {
			if (writer != null) {
				try {
					writer.flush();
					writer.close();
				} catch (IOException e) {
					throw new IllegalStateException("Unable to close file[" + outputFile.getAbsolutePath() + "].");
				}
			}
			writer = null;
		}
	}
}
