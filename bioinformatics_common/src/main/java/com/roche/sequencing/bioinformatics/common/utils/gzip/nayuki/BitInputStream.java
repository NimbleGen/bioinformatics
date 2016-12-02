package com.roche.sequencing.bioinformatics.common.utils.gzip.nayuki;

/* 

 * Simple DEFLATE decompressor
 * Copyright (c) Project Nayuki
 * 
 * https://www.nayuki.io/page/simple-deflate-decompressor
 * https://github.com/nayuki/Simple-DEFLATE-decompressor
 */

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;

import com.roche.sequencing.bioinformatics.common.utils.gzip.IInput;
import com.roche.sequencing.bioinformatics.common.utils.gzip.InputStreamInput;

/**
 * A stream of bits that can be read. Because they come from an underlying byte stream, the total number of bits is always a multiple of 8. The bits are read in little endian. Mutable and not
 * thread-safe.
 * 
 * @see BitOutputStream
 */
public final class BitInputStream implements IBitInputStream {

	private final static int BITS_PER_BYTE = 8;

	/*---- Fields ----*/

	// The underlying byte stream to read from (not null).
	private IInput input;

	// Either in the range [0x00, 0xFF] if bits are available, or -1 if end of stream is reached.
	private int currentByteValue;

	// Number of remaining bits in the current byte, always between 0 and 7 (inclusive).
	private int numBitsRemainingInCurrentByte;

	private int numberOfBytesRead;

	private final int startingBit;

	/*---- Constructor ----*/

	/**
	 * Constructs a bit input stream based on the specified byte input stream.
	 * 
	 * @param in
	 *            the byte input stream
	 * @throws IOException
	 * @throws NullPointerException
	 *             if the input stream is {@code null}
	 */
	public BitInputStream(IInput in) throws IOException {
		this(in, 0);
	}

	public BitInputStream(IInput in, int startingBit) throws IOException {
		input = in;
		numberOfBytesRead = 0;
		this.startingBit = startingBit;
		numBitsRemainingInCurrentByte = 0;
		currentByteValue = 0;

		for (int i = 0; i < startingBit; i++) {
			readBit();
		}
	}

	/*---- Methods ----*/

	/**
	 * Returns the current bit position, which ascends from 0 to 7 as bits are read.
	 * 
	 * @return the current bit position, which is between 0 and 7
	 */
	public int getBitPosition() {
		if (numBitsRemainingInCurrentByte < 0 || numBitsRemainingInCurrentByte > 7) {
			throw new AssertionError();
		}
		int bitPosition = (BITS_PER_BYTE - numBitsRemainingInCurrentByte) % BITS_PER_BYTE;
		return bitPosition;
	}

	/**
	 * Reads a bit from this stream. Returns 0 or 1 if a bit is available, or -1 if the end of stream is reached. The end of stream always occurs on a byte boundary.
	 * 
	 * @return the next bit of 0 or 1, or -1 for the end of stream
	 * @throws IOException
	 *             if an I/O exception occurred
	 */
	public int readBit() throws IOException {
		int returnBit = -1;
		if (currentByteValue >= 0) {
			if (numBitsRemainingInCurrentByte == 0) {
				currentByteValue = input.read();
				numberOfBytesRead++;
				if (currentByteValue >= 0) {
					numBitsRemainingInCurrentByte = 7;
					returnBit = (currentByteValue >>> (7 - numBitsRemainingInCurrentByte)) & 1;
				}
			} else {
				numBitsRemainingInCurrentByte--;
				returnBit = (currentByteValue >>> (7 - numBitsRemainingInCurrentByte)) & 1;
			}
		}
		return returnBit;
	}

	/**
	 * Reads a bit from this stream. Returns 0 or 1 if a bit is available, or throws an {@code EOFException} if the end of stream is reached. The end of stream always occurs on a byte boundary.
	 * 
	 * @return the next bit of 0 or 1
	 * @throws IOException
	 *             if an I/O exception occurred
	 * @throws EOFException
	 *             if the end of stream is reached
	 */
	public int readBitIfNotEof() throws IOException {
		int result = readBit();
		if (result == -1) {
			throw new EOFException();
		}
		return result;
	}

	@Override
	public long getNumberOfBitsRead() {
		long bitsRead = (numberOfBytesRead * BITS_PER_BYTE) - startingBit - numBitsRemainingInCurrentByte;
		return bitsRead;
	}

	@Override
	public int readByte() throws IOException {
		int byteValue = 0;
		for (int i = 0; i < BITS_PER_BYTE; i++) {
			int nextBit = readBitIfNotEof();
			byteValue = byteValue | (nextBit << i);
		}
		return byteValue;
	}

	public static void main(String[] args) throws IOException {
		int bytesInTwoGigs = 2000000000;
		byte[] bytes = new byte[bytesInTwoGigs];
		bytes[0] = (byte) 125;
		bytes[25] = 123;
		bytes[125] = 23;
		IInput input = new InputStreamInput(new ByteArrayInputStream(bytes));
		BitInputStream bitInputStream = new BitInputStream(input);

		for (int i = 1; i <= bytesInTwoGigs / 2; i++) {
			bitInputStream.readByte();
			for (int j = 0; j < 8; j++) {
				bitInputStream.readBit();
			}
			boolean isAsExpected = bitInputStream.getNumberOfBitsRead() == (long) (i * 16);
			if (!isAsExpected) {
				throw new IllegalStateException("not as expected at " + i);
			}
		}

	}
}
