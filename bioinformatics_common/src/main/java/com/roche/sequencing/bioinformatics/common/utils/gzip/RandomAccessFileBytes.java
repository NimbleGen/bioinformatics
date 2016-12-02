package com.roche.sequencing.bioinformatics.common.utils.gzip;

import java.io.IOException;
import java.io.RandomAccessFile;

public class RandomAccessFileBytes implements IBytes {
	private final RandomAccessFile randomAccessFile;

	public RandomAccessFileBytes(RandomAccessFile randomAccessFile) {
		this.randomAccessFile = randomAccessFile;
	}

	@Override
	public byte[] getBytes(long offset, long length) {
		byte[] bytes = null;
		try {
			randomAccessFile.seek(offset);
			bytes = new byte[(int) length];
			randomAccessFile.read(bytes);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		return bytes;
	}

	@Override
	public void close() {
		if (randomAccessFile != null) {
			try {
				randomAccessFile.close();
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((randomAccessFile == null) ? 0 : randomAccessFile.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RandomAccessFileBytes other = (RandomAccessFileBytes) obj;
		if (randomAccessFile == null) {
			if (other.randomAccessFile != null)
				return false;
		} else if (!randomAccessFile.equals(other.randomAccessFile))
			return false;
		return true;
	}

}
