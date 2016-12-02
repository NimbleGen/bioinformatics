package com.roche.sequencing.bioinformatics.common.utils.gzip.nayuki;

/* 
 * Simple DEFLATE decompressor
 * Copyright (c) Project Nayuki
 * 
 * https://www.nayuki.io/page/simple-deflate-decompressor
 * https://github.com/nayuki/Simple-DEFLATE-decompressor
 */

import java.io.EOFException;
import java.io.IOException;

/**
 * A stream of bits that can be read. Bits are packed in little endian within a byte. For example, the byte 0x87 reads as the sequence of bits [1,1,1,0,0,0,0,1].
 */
public interface IBitInputStream {

	/**
	 * Returns the current bit position, which ascends from 0 to 7 as bits are read.
	 * 
	 * @return the current bit position, which is between 0 and 7
	 */
	public int getBitPosition();

	/**
	 * 
	 * @return the next byte from the stream
	 */
	public int readByte() throws IOException;

	/**
	 * Reads a bit from this stream. Returns 0 or 1 if a bit is available, or -1 if the end of stream is reached. The end of stream always occurs on a byte boundary.
	 * 
	 * @return the next bit of 0 or 1, or -1 for the end of stream
	 * @throws IOException
	 *             if an I/O exception occurred
	 */
	public int readBit() throws IOException;

	/**
	 * Reads a bit from this stream. Returns 0 or 1 if a bit is available, or throws an {@code EOFException} if the end of stream is reached. The end of stream always occurs on a byte boundary.
	 * 
	 * @return the next bit of 0 or 1
	 * @throws IOException
	 *             if an I/O exception occurred
	 * @throws EOFException
	 *             if the end of stream is reached
	 */
	public int readBitIfNotEof() throws IOException;

	public long getNumberOfBitsRead();

}
