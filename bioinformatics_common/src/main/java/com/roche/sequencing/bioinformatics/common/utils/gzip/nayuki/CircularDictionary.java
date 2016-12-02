package com.roche.sequencing.bioinformatics.common.utils.gzip.nayuki;

/* 
 * Simple DEFLATE decompressor
 * Copyright (c) Project Nayuki
 * 
 * https://www.nayuki.io/page/simple-deflate-decompressor
 * https://github.com/nayuki/Simple-DEFLATE-decompressor
 */

import java.io.IOException;
import java.io.OutputStream;

import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.ByteUtil;

/**
 * A finite circular buffer of bytes, useful as an implicit dictionary for Lempel-Ziv schemes.
 */
public final class CircularDictionary {

	public final static int DICTIONARY_SIZE = 32768;

	/*---- Fields ----*/

	// Buffer of byte data.
	private byte[] data;

	// Index of next byte to write to, always in the range [0, data.length).
	private int index;

	private int loopsSinceInit;
	private int accessedDistanceInFirstLoopSinceInit;

	/*---- Constructor ----*/

	/**
	 * Constructs a circular dictionary of the specified size, initialized to zeros.
	 * 
	 * @param size
	 *            the size, which must be positive
	 */
	public CircularDictionary() {
		data = new byte[DICTIONARY_SIZE];
		index = 0;
		mark();
	}

	public CircularDictionary(byte[] startingDictionaryBytes) {
		data = new byte[DICTIONARY_SIZE];
		for (int i = 0; i < startingDictionaryBytes.length; i++) {
			data[DICTIONARY_SIZE - startingDictionaryBytes.length + i] = startingDictionaryBytes[i];
		}
		index = 0;
		mark();
	}

	public void mark() {
		loopsSinceInit = 0;
		accessedDistanceInFirstLoopSinceInit = 0;
	}

	public int getFurthestByteUsedInFirstLoop() {
		return accessedDistanceInFirstLoopSinceInit;
	}

	public byte[] getDictionaryBytes() {
		byte[] startBytes = ByteUtil.copyOf(data, index, data.length - index);
		byte[] endBytes = ByteUtil.copyOf(data, 0, index);
		byte[] allBytes = ArraysUtil.concatenate(startBytes, endBytes);
		return allBytes;
	}

	/*---- Methods ----*/

	/**
	 * Appends the specified byte to this circular dictionary. This overwrites the byte value at {@code size} positions ago.
	 * 
	 * @param b
	 *            the byte value to append
	 */
	public void append(int b) {
		if (index < 0 || index >= data.length) {
			throw new AssertionError();
		}
		data[index] = (byte) b;
		index = (index + 1) % data.length;

		if (index == 0) {
			loopsSinceInit++;
		}
	}

	/**
	 * Copies {@code len} bytes starting at {@code dist} bytes ago to the specified output stream and also back into this buffer itself.
	 * <p>
	 * Note that if the length exceeds the distance, then some of the output data will be a copy of data that was copied earlier in the process.
	 * </p>
	 * 
	 * @param dist
	 *            the distance to go back, which must be positive but no greater than the buffer's size
	 * @param len
	 *            the length to copy, which must be non-negative and is allowed to exceed the distance
	 * @param out
	 *            the output stream to write to
	 * @throws NullPointerException
	 *             if the output stream is {@code null}
	 * @throws IllegalArgumentException
	 *             if the length is negative, distance is not positive, or distance is greater than the buffer size
	 * @throws IOException
	 *             if an I/O exception occurs
	 */
	public void copy(int dist, int len, OutputStream out) throws IOException {
		if (out == null) {
			throw new NullPointerException();
		}
		if (len < 0 || dist < 1 || dist > data.length) {
			throw new IllegalArgumentException();
		}

		// This calculation is correct for all possible values and does not overflow
		int readIndex = (index - dist + data.length) % data.length;

		if ((loopsSinceInit == 0) && (index < dist)) {
			int newMarkDistance = Math.abs(index - dist);
			accessedDistanceInFirstLoopSinceInit = Math.max(accessedDistanceInFirstLoopSinceInit, newMarkDistance);
		}

		if (readIndex < 0 || readIndex >= data.length) {
			throw new AssertionError();
		}

		for (int i = 0; i < len; i++) {
			byte b = data[readIndex];
			readIndex = (readIndex + 1) % data.length;
			out.write(b);
			append(b);
		}
	}

}
