package com.roche.sequencing.bioinformatics.common.utils.gzip;

import java.io.IOException;
import java.io.RandomAccessFile;

public class RandomAccessFileInput implements IInput {

	private final RandomAccessFile randomAccessFile;

	public RandomAccessFileInput(RandomAccessFile randomAccessFile) {
		super();
		this.randomAccessFile = randomAccessFile;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return randomAccessFile.read(b);
	}

	@Override
	public int read() throws IOException {
		return randomAccessFile.read();
	}

	@Override
	public String readLine() throws IOException {
		return randomAccessFile.readLine();
	}

}
