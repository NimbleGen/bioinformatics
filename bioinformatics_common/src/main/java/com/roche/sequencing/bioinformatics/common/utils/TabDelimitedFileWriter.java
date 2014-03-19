package com.roche.sequencing.bioinformatics.common.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

public class TabDelimitedFileWriter implements AutoCloseable {

	private static final DecimalFormat decimalFormat = new DecimalFormat("##.##");
	private PrintWriter printWriter = null;
	private int columnCount = -1;
	private final boolean shouldValidateColumnCount;

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
		this.columnCount = headers.length;
		this.printWriter = new PrintWriter(new FileWriter(outputFile));
		boolean firstHeader = true;
		synchronized (this) {
			if (preHeader != null && !preHeader.isEmpty()) {
				printWriter.println(preHeader);
			}

			for (String header : headers) {
				if (!firstHeader) {
					printWriter.write(StringUtil.TAB);
				}
				firstHeader = false;
				printWriter.write(header);
			}
			printWriter.println();
			printWriter.flush();
		}
		this.shouldValidateColumnCount = false;
	}

	/**
	 * Write the provided values to the file. Ensures that the number of values matches the number of header columns. Formats numeric values consistently.
	 * 
	 * @param values
	 */
	public void writeLine(Object... values) {
		synchronized (this) {
			if (printWriter == null) {
				throw new IllegalArgumentException("Trying to write a line to a writer thas has been closed");
			}

			if (shouldValidateColumnCount && values.length != columnCount) {
				throw new IllegalArgumentException("Passed in " + values.length + " values to a writer with " + columnCount + " columns.");
			}

			boolean firstValue = true;
			for (Object value : values) {
				if (!firstValue) {
					printWriter.write(StringUtil.TAB);
				}
				firstValue = false;

				if (value != null) {
					if (value instanceof Number) {
						// We want all numeric values to be formatted
						printWriter.write(decimalFormat.format(value));
					} else {
						// Convert the value to a string
						printWriter.write(value.toString());
					}
				}
			}
			printWriter.println();
			printWriter.flush();
		}
	}

	/**
	 * Close the underlying print writer. Once a TabDelimitedFileWriter has been closed it can no longer be written to.
	 */
	public void close() {
		synchronized (this) {
			if (printWriter != null) {
				printWriter.flush();
				printWriter.close();
			}
			printWriter = null;
		}
	}
}
