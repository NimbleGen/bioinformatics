package com.roche.sequencing.bioinformatics.common.utils.gzip;

import java.util.Arrays;

import com.roche.sequencing.bioinformatics.common.utils.ByteUtil;

public class Bytes implements IBytes {

	private final byte[] bytes;

	public Bytes(byte[] bytes) {
		super();
		this.bytes = bytes;
	}

	@Override
	public byte[] getBytes(long offset, long length) {
		return ByteUtil.copyOf(bytes, (int) offset, (int) length);
	}

	@Override
	public void close() {
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytes);
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
		Bytes other = (Bytes) obj;
		if (!Arrays.equals(bytes, other.bytes))
			return false;
		return true;
	}

}
