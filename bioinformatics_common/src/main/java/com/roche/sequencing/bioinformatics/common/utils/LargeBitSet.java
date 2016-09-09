package com.roche.sequencing.bioinformatics.common.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class LargeBitSet implements IBitSet {

	private final static int BYTES_IN_A_LONG = 8;

	private static final int EOF = -1;

	private final long MAX_SIZE_FOR_TO_BYTE_ARRAY = (long) Integer.MAX_VALUE * (long) ByteUtil.BITS_PER_BYTE;
	private final long MAX_SIZE_FOR_TO_LONG_ARRAY = (long) Integer.MAX_VALUE * (long) ByteUtil.BITS_PER_BYTE * BYTES_IN_A_LONG;

	// Make sure the max number is divisible by the number of bits in a byte
	// so when writing and reading multiple bitsets to file we do not get offset
	private final static int MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET = (Integer.MAX_VALUE / ByteUtil.BITS_PER_BYTE) * ByteUtil.BITS_PER_BYTE;

	private final List<BitSet> bitSets;
	private long currentLength;

	/**
	 * Creates a new bit set. All bits are initially {@code false}.
	 */
	public LargeBitSet() {
		this(0);
	}

	public int getNumberOfBitSetsUsed() {
		return bitSets.size();
	}

	/**
	 * Creates a bit set whose initial size is large enough to explicitly represent bits with indices in the range {@code 0} through {@code nbits-1}. All bits are initially {@code false}.
	 *
	 * @param nbits
	 *            the initial size of the bit set
	 * @throws NegativeArraySizeException
	 *             if the specified initial size is negative
	 */
	public LargeBitSet(long nBits) {
		this.bitSets = new ArrayList<BitSet>();
		currentLength = 0;
	}

	public byte[] toByteArray() {
		byte[] bytes = null;
		if (currentLength < MAX_SIZE_FOR_TO_BYTE_ARRAY) {
			int mainBytesIndex = 0;
			bytes = new byte[(int) (currentLength / ByteUtil.BITS_PER_BYTE)];

			int bitsetIndex = 0;
			while (((long) bitsetIndex * (long) MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET) < currentLength) {
				BitSet bitSet = bitSets.get(bitsetIndex);
				byte[] currentBytes = bitSet.toByteArray();
				for (int byteIndex = 0; byteIndex < currentBytes.length; byteIndex++) {
					bytes[mainBytesIndex] = currentBytes[byteIndex];
					mainBytesIndex++;
				}
				bitsetIndex++;
			}

		} else {
			throw new IllegalStateException("The current LargeBitSet is too large to be represented as byte[].");
		}
		return bytes;
	}

	public long[] toLongArray() {
		if (currentLength < MAX_SIZE_FOR_TO_LONG_ARRAY) {
			// TODO
			throw new IllegalStateException("Not Implemented Yet.");
		} else {
			throw new IllegalStateException("The current LargeBitSet is too large to be represented as long[].");
		}
	}

	private BitSet getBitSet(int bitSetIndex) {
		BitSet bitSet = null;
		if (bitSets.size() <= bitSetIndex) {
			for (int i = bitSets.size(); i <= bitSetIndex; i++) {
				bitSets.add(new BitSet());
			}
		}
		bitSet = bitSets.get(bitSetIndex);
		return bitSet;
	}

	/**
	 * Sets the bit at the specified index to the complement of its current value.
	 *
	 * @param bitIndex
	 *            the index of the bit to flip
	 * @throws IndexOutOfBoundsException
	 *             if the specified index is negative
	 */
	public void flip(long bitIndex) {
		int bitSetIndex = (int) (bitIndex / MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET);
		int indexInBitSet = (int) (bitIndex % MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET);
		getBitSet(bitSetIndex).flip(indexInBitSet);
		currentLength = Math.max(currentLength, bitIndex);
	}

	/**
	 * Sets each bit from the specified {@code fromIndex} (inclusive) to the specified {@code toIndex} (exclusive) to the complement of its current value.
	 *
	 * @param fromIndex
	 *            index of the first bit to flip
	 * @param toIndex
	 *            index after the last bit to flip
	 * @throws IndexOutOfBoundsException
	 *             if {@code fromIndex} is negative, or {@code toIndex} is negative, or {@code fromIndex} is larger than {@code toIndex}
	 */
	public void flip(long fromIndex, long toIndex) {
		for (long i = fromIndex; i <= toIndex; i++) {
			flip(i);
		}
	}

	/**
	 * Sets the bit at the specified index to {@code true}.
	 *
	 * @param bitIndex
	 *            a bit index
	 * @throws IndexOutOfBoundsException
	 *             if the specified index is negative
	 */
	public void set(long bitIndex) {
		int bitSetIndex = (int) (bitIndex / MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET);
		int indexInBitSet = (int) (bitIndex % MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET);
		getBitSet(bitSetIndex).set(indexInBitSet);
		currentLength = Math.max(currentLength, bitIndex);
	}

	/**
	 * Sets the bit at the specified index to the specified value.
	 *
	 * @param bitIndex
	 *            a bit index
	 * @param value
	 *            a boolean value to set
	 * @throws IndexOutOfBoundsException
	 *             if the specified index is negative
	 */
	public void set(long bitIndex, boolean value) {
		if (bitIndex < 0) {
			throw new IllegalArgumentException("The provided bitIndex[" + bitIndex + "] must be greater than or equal to zero.");
		}
		int bitSetIndex = (int) (bitIndex / MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET);
		int indexInBitSet = (int) (bitIndex % MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET);

		getBitSet(bitSetIndex).set(indexInBitSet, value);
		currentLength = Math.max(currentLength, bitIndex);
	}

	/**
	 * Sets the bits from the specified {@code fromIndex} (inclusive) to the specified {@code toIndex} (exclusive) to {@code true}.
	 *
	 * @param fromIndex
	 *            index of the first bit to be set
	 * @param toIndex
	 *            index after the last bit to be set
	 * @throws IndexOutOfBoundsException
	 *             if {@code fromIndex} is negative, or {@code toIndex} is negative, or {@code fromIndex} is larger than {@code toIndex}
	 */
	public void set(long fromIndex, long toIndex) {
		for (long i = fromIndex; i <= toIndex; i++) {
			set(i);
		}
	}

	/**
	 * Sets the bits from the specified {@code fromIndex} (inclusive) to the specified {@code toIndex} (exclusive) to the specified value.
	 *
	 * @param fromIndex
	 *            index of the first bit to be set
	 * @param toIndex
	 *            index after the last bit to be set
	 * @param value
	 *            value to set the selected bits to
	 * @throws IndexOutOfBoundsException
	 *             if {@code fromIndex} is negative, or {@code toIndex} is negative, or {@code fromIndex} is larger than {@code toIndex}
	 */
	public void set(long fromIndex, long toIndex, boolean value) {
		for (long i = fromIndex; i <= toIndex; i++) {
			set(i, value);
		}
	}

	/**
	 * Sets the bit specified by the index to {@code false}.
	 *
	 * @param bitIndex
	 *            the index of the bit to be cleared
	 * @throws IndexOutOfBoundsException
	 *             if the specified index is negative
	 */
	public void clear(long bitIndex) {
		int bitSetIndex = (int) (bitIndex / MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET);
		int indexInBitSet = (int) (bitIndex % MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET);
		getBitSet(bitSetIndex).clear(indexInBitSet);
	}

	/**
	 * Sets the bits from the specified {@code fromIndex} (inclusive) to the specified {@code toIndex} (exclusive) to {@code false}.
	 *
	 * @param fromIndex
	 *            index of the first bit to be cleared
	 * @param toIndex
	 *            index after the last bit to be cleared
	 * @throws IndexOutOfBoundsException
	 *             if {@code fromIndex} is negative, or {@code toIndex} is negative, or {@code fromIndex} is larger than {@code toIndex}
	 */
	public void clear(long fromIndex, long toIndex) {
		for (long i = fromIndex; i <= toIndex; i++) {
			clear(i);
		}
	}

	/**
	 * Sets all of the bits in this BitSet to {@code false}.
	 *
	 */
	public void clear() {
		for (BitSet bitSet : bitSets) {
			bitSet.clear();
		}
	}

	/**
	 * Returns the value of the bit with the specified index. The value is {@code true} if the bit with the index {@code bitIndex} is currently set in this {@code LargeBitSet}; otherwise, the result
	 * is {@code false}.
	 *
	 * @param bitIndex
	 *            the bit index
	 * @return the value of the bit with the specified index
	 * @throws IndexOutOfBoundsException
	 *             if the specified index is negative
	 */
	public boolean get(long bitIndex) {
		int bitSetIndex = (int) (bitIndex / MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET);
		int indexInBitSet = (int) (bitIndex % MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET);
		return getBitSet(bitSetIndex).get(indexInBitSet);
	}

	/**
	 * Returns a new {@code LargeBitSet} composed of bits from this {@code LargeBitSet} from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
	 *
	 * @param fromIndex
	 *            index of the first bit to include
	 * @param toIndex
	 *            index after the last bit to include
	 * @return a new {@code LargeBitSet} from a range of this {@code LargeBitSet}
	 * @throws IndexOutOfBoundsException
	 *             if {@code fromIndex} is negative, or {@code toIndex} is negative, or {@code fromIndex} is larger than {@code toIndex}
	 */
	@Override
	public BitSet getBitSet(long fromIndex, long toIndexExclusive) {
		BitSet newBitSet = new BitSet();
		for (long i = fromIndex; i < toIndexExclusive; i++) {
			boolean value = get(i);
			newBitSet.set((int) (i - fromIndex), value);
		}
		return newBitSet;
	}

	/**
	 * Returns the index of the first bit that is set to {@code true} that occurs on or after the specified starting index. If no such bit exists then {@code -1} is returned.
	 *
	 * <p>
	 * To iterate over the {@code true} bits in a {@code LargeBitSet}, use the following loop:
	 *
	 * <pre>
	 * {@code
	 * for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
	 *     // operate on index i here
	 * }}
	 * </pre>
	 *
	 * @param fromIndex
	 *            the index to start checking from (inclusive)
	 * @return the index of the next set bit, or {@code -1} if there is no such bit
	 * @throws IndexOutOfBoundsException
	 *             if the specified index is negative
	 */
	public int nextSetBit(long fromIndex) {
		// TODO
		throw new IllegalStateException("Not Implemented Yet.");
	}

	/**
	 * Returns the index of the first bit that is set to {@code false} that occurs on or after the specified starting index.
	 *
	 * @param fromIndex
	 *            the index to start checking from (inclusive)
	 * @return the index of the next clear bit
	 * @throws IndexOutOfBoundsException
	 *             if the specified index is negative
	 */
	public long nextClearBit(long fromIndex) {
		// TODO
		throw new IllegalStateException("Not Implemented Yet.");

	}

	/**
	 * Returns the index of the nearest bit that is set to {@code true} that occurs on or before the specified starting index. If no such bit exists, or if {@code -1} is given as the starting index,
	 * then {@code -1} is returned.
	 *
	 * <p>
	 * To iterate over the {@code true} bits in a {@code LargeBitSet}, use the following loop:
	 *
	 * <pre>
	 * {@code
	 * for (int i = bs.length(); (i = bs.previousSetBit(i-1)) >= 0; ) {
	 *     // operate on index i here
	 * }}
	 * </pre>
	 *
	 * @param fromIndex
	 *            the index to start checking from (inclusive)
	 * @return the index of the previous set bit, or {@code -1} if there is no such bit
	 * @throws IndexOutOfBoundsException
	 *             if the specified index is less than {@code -1}
	 */
	public long previousSetBit(long fromIndex) {
		// TODO
		throw new IllegalStateException("Not Implemented Yet.");
	}

	/**
	 * Returns the index of the nearest bit that is set to {@code false} that occurs on or before the specified starting index. If no such bit exists, or if {@code -1} is given as the starting index,
	 * then {@code -1} is returned.
	 *
	 * @param fromIndex
	 *            the index to start checking from (inclusive)
	 * @return the index of the previous clear bit, or {@code -1} if there is no such bit
	 * @throws IndexOutOfBoundsException
	 *             if the specified index is less than {@code -1}
	 */
	public int previousClearBit(long fromIndex) {
		// TODO
		throw new IllegalStateException("Not Implemented Yet.");

	}

	/**
	 * Returns the "logical size" of this {@code LargeBitSet}: the index of the highest set bit in the {@code LargeBitSet} plus one. Returns zero if the {@code LargeBitSet} contains no set bits.
	 *
	 * @return the logical size of this {@code LargeBitSet}
	 */
	public long length() {
		return currentLength;
	}

	/**
	 * Returns true if this {@code LargeBitSet} contains no bits that are set to {@code true}.
	 *
	 * @return boolean indicating whether this {@code LargeBitSet} is empty
	 */
	public boolean isEmpty() {
		return currentLength == 0;
	}

	/**
	 * Returns true if the specified {@code LargeBitSet} has any bits set to {@code true} that are also set to {@code true} in this {@code LargeBitSet}.
	 *
	 * @param set
	 *            {@code LargeBitSet} to intersect with
	 * @return boolean indicating whether this {@code LargeBitSet} intersects the specified {@code LargeBitSet}
	 */
	public boolean intersects(LargeBitSet set) {

		throw new IllegalStateException("Not Implemented Yet.");
		// TODO

	}

	/**
	 * Returns the number of bits set to {@code true} in this {@code LargeBitSet}.
	 *
	 * @return the number of bits set to {@code true} in this {@code LargeBitSet}
	 */
	public int cardinality() {
		// TODO
		throw new IllegalStateException("Not Implemented Yet.");

	}

	/**
	 * Performs a logical <b>AND</b> of this target bit set with the argument bit set. This bit set is modified so that each bit in it has the value {@code true} if and only if it both initially had
	 * the value {@code true} and the corresponding bit in the bit set argument also had the value {@code true}.
	 *
	 * @param set
	 *            a bit set
	 */
	public void and(LargeBitSet set) {
		// TODO
		throw new IllegalStateException("Not Implemented Yet.");
	}

	/**
	 * Performs a logical <b>OR</b> of this bit set with the bit set argument. This bit set is modified so that a bit in it has the value {@code true} if and only if it either already had the value
	 * {@code true} or the corresponding bit in the bit set argument has the value {@code true}.
	 *
	 * @param set
	 *            a bit set
	 */
	public void or(LargeBitSet set) {
		// TODO
		throw new IllegalStateException("Not Implemented Yet.");
	}

	/**
	 * Performs a logical <b>XOR</b> of this bit set with the bit set argument. This bit set is modified so that a bit in it has the value {@code true} if and only if one of the following statements
	 * holds:
	 * <ul>
	 * <li>The bit initially has the value {@code true}, and the corresponding bit in the argument has the value {@code false}.
	 * <li>The bit initially has the value {@code false}, and the corresponding bit in the argument has the value {@code true}.
	 * </ul>
	 *
	 * @param set
	 *            a bit set
	 */
	public void xor(LargeBitSet set) {
		// TODO
		throw new IllegalStateException("Not Implemented Yet.");
	}

	/**
	 * Clears all of the bits in this {@code LargeBitSet} whose corresponding bit is set in the specified {@code LargeBitSet}.
	 *
	 * @param set
	 *            the {@code LargeBitSet} with which to mask this {@code LargeBitSet}
	 */
	public void andNot(LargeBitSet set) {
		// TODO
		throw new IllegalStateException("Not Implemented Yet.");
	}

	/**
	 * Returns the number of bits of space actually in use by this {@code LargeBitSet} to represent bit values. The maximum element in the set is the size - 1st element.
	 *
	 * @return the number of bits currently in this bit set
	 */
	public long size() {
		long size = 0;
		for (BitSet bitSet : bitSets) {
			int bitSetSize = bitSet.size();
			// the bitset size function is using an int return type when
			// it should be using a long since the actual return value can overrun an int
			if (bitSetSize < 0) {
				// the int return type has been overrun
				long nonOverrunBitSetSize = (long) Integer.MAX_VALUE + ((long) bitSetSize - (long) Integer.MIN_VALUE) + 1;
				size += nonOverrunBitSetSize;
			} else {
				size += (long) bitSetSize;
			}

		}
		return size;
	}

	public void writeToFile(File outputFile) throws IOException {
		// erase the existing file since the content at the end of the file
		// would be preserved if it is not written over thus preventing the containerInformationStart location
		// from being stored at the very end of the file
		if (outputFile.exists()) {
			outputFile.delete();
		}
		FileUtil.createNewFile(outputFile);
		try (FileOutputStream writer = new FileOutputStream(outputFile)) {
			int count = 0;
			for (BitSet bitSet : bitSets) {
				writer.write(bitSet.toByteArray());

				System.out.println("writing bitset:" + count + ": " + BitSetUtil.getBinaryStringOfBits(bitSet, 0, 100));

				count++;
			}
		}
	}

	/**
	 * @param bits
	 * @return the bitset as a string
	 */
	public String getBinaryStringOfBits(int start, int endInclusive) {
		StringBuilder returnString = new StringBuilder();

		for (int i = start; i <= endInclusive; i++) {
			if (get(i)) {
				returnString.append("1");
			} else {
				returnString.append("0");
			}

			if (i > 0 && i % ByteUtil.BITS_PER_BYTE == 0) {
				returnString.append(" ");
			}

		}

		return returnString.toString();
	}

	public static LargeBitSet readLargeBitSetFromInputStream(InputStream genomeSequenceSearcherInputStream) throws IOException {
		LargeBitSet largeBitSet = new LargeBitSet();

		int bytesRead;
		byte[] buffer = new byte[4096];

		long totalBitsRead = 0;
		while (EOF != (bytesRead = genomeSequenceSearcherInputStream.read(buffer))) {
			BitSet bitSet = BitSet.valueOf(buffer);
			int bitsRead = bytesRead * ByteUtil.BITS_PER_BYTE;
			BitSetUtil.copy(bitSet, bitsRead, largeBitSet, totalBitsRead);
			totalBitsRead += bitsRead;
		}
		System.out.println("total data read in bits:" + totalBitsRead);

		int count = 0;
		for (BitSet bitSet : largeBitSet.bitSets) {
			System.out.println(" reading bitset:" + count + ": " + BitSetUtil.getBinaryStringOfBits(bitSet, 0, 100));
			count++;
		}

		return largeBitSet;
	}
}
