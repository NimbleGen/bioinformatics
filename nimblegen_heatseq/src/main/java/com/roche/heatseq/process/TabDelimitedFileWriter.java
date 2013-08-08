package com.roche.heatseq.process;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class TabDelimitedFileWriter implements AutoCloseable {

	private static final DecimalFormat decimalFormat = new DecimalFormat("##.##");
	private PrintWriter printWriter = null;
	private int columnCount = -1;

	/**
	 * Make a tab delimited file writer with the provided output file and header line
	 * 
	 * @param outputFile
	 * @param headers
	 * @throws IOException
	 */
	public TabDelimitedFileWriter(File outputFile, String[] headers) throws IOException {

		// Keep track of how many columns we should expect to see
		this.columnCount = headers.length;

		this.printWriter = new PrintWriter(new FileWriter(outputFile));
		boolean firstHeader = true;
		synchronized (this) {
			for (String header : headers) {
				if (!firstHeader) {
					printWriter.write(StringUtil.TAB);
				}
				firstHeader = false;
				printWriter.write(header);
			}
			printWriter.println();
		}
	}

	/**
	 * Write the provided values to the file. Ensures that the number of values matches the number of header columns. Formats numeric values consistently.
	 * 
	 * @param values
	 */
	public void writeLine(Object... values) {
		if (printWriter == null) {
			throw new IllegalArgumentException("Trying to write a line to a writer thas has been closed");
		}

		if (values.length != columnCount) {
			throw new IllegalArgumentException("Passed in " + values.length + " values to a writer with " + columnCount + " columns.");
		}

		boolean firstValue = true;
		synchronized (this) {
			for (Object value : values) {
				if (!firstValue) {
					printWriter.write(StringUtil.TAB);
				}
				firstValue = false;

				if (value instanceof Number) {
					// We want all numeric values to be formatted
					printWriter.write(decimalFormat.format(value));
				} else {
					// Convert the value to a string
					printWriter.write(value.toString());
				}
			}
			printWriter.println();
		}
	}

	/**
	 * Close the underlying print writer. Once a TabDelimitedFileWriter has been closed it can no longer be written to.
	 */
	public void close() {
		if (printWriter != null) {
			printWriter.flush();
			printWriter.close();
		}
		printWriter = null;
	}
}
