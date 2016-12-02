package com.roche.sequencing.bioinformatics.common.utils.gzip;

import java.io.IOException;
import java.io.InputStream;

import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class InputStreamInput implements IInput {

	private final InputStream inputStream;

	public InputStreamInput(InputStream inputStream) {
		super();
		this.inputStream = inputStream;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return inputStream.read(b);
	}

	@Override
	public int read() throws IOException {
		return inputStream.read();
	}

	@Override
	public String readLine() throws IOException {
		StringBuilder line = new StringBuilder();
		int result = read();
		while ((((char) result) != (int) StringUtil.NEWLINE_SYMBOL) && (result >= 0)) {
			line.append((char) result);
			result = read();
		}

		String lineText = line.toString();
		if (result < 0 && lineText.isEmpty()) {
			lineText = null;
		}

		return lineText;
	}

}
