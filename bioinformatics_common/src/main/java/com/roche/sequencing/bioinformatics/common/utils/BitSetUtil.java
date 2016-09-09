/*
 *    Copyright 2016 Roche NimbleGen Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.roche.sequencing.bioinformatics.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

import org.apache.commons.io.IOUtils;

/**
 * 
 * Util for working with BitSets
 * 
 */
public final class BitSetUtil {
	private BitSetUtil() {
		throw new AssertionError();
	}

	/**
	 * converts a string to a bitset counting all 1's as true and all other characters as false
	 * 
	 * @param bitsAsBinaryString
	 * @return
	 */
	public static BitSet createBitSetFromBinaryString(String bitsAsBinaryString) {
		BitSet bitSet = new BitSet(bitsAsBinaryString.length());

		for (int i = 0; i < bitsAsBinaryString.length(); i++) {
			if (bitsAsBinaryString.substring(i, i + 1).equals("1")) {
				bitSet.set(i, true);
			} else {
				bitSet.set(i, false);
			}
		}

		return bitSet;
	}

	/**
	 * @param bits
	 * @return the bitset as a string
	 */
	public static String getBinaryStringOfBits(BitSet bits, int size) {
		StringBuilder returnString = new StringBuilder();

		for (int i = 0; i < size; i++) {
			if (bits.get(i)) {
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

	/**
	 * @param bits
	 * @return the bitset as a string
	 */
	public static String getBinaryStringOfBits(BitSet bitset, int start, int endInclusive) {
		StringBuilder returnString = new StringBuilder();

		for (int i = start; i <= endInclusive; i++) {
			if (bitset.get(i)) {
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

	/**
	 * combine a variable number of bitsets of the same length
	 * 
	 * @param length
	 * @param bitsets
	 * @return
	 */
	public static BitSet combine(int length, BitSet... bitsets) {
		BitSet combinedBitSet = new BitSet();
		int currentBitset = 0;
		for (BitSet bitset : bitsets) {
			for (int i = 0; i < length; i++) {
				if (bitset.get(i)) {
					combinedBitSet.set((currentBitset * length) + i);
				}
			}
			currentBitset++;
		}
		return combinedBitSet;
	}

	public static void writeBitSetToFile(BitSet bitset, File outputFile) throws IOException {
		// erase the existing file since the content at the end of the file
		// would be preserved if it is not written over thus preventing the containerInformationStart location
		// from being stored at the very end of the file
		if (outputFile.exists()) {
			outputFile.delete();
		}
		FileUtil.createNewFile(outputFile);
		try (FileOutputStream writer = new FileOutputStream(outputFile)) {
			writer.write(bitset.toByteArray());
		}
	}

	public static BitSet readBitSetFromFile(File inputFile) throws IOException {
		return readBitSetFromInputStream(new FileInputStream(inputFile));
	}

	public static BitSet readBitSetFromInputStream(InputStream inputStream) throws IOException {
		byte[] byteArray = IOUtils.toByteArray(inputStream);
		BitSet bitset = BitSet.valueOf(byteArray);
		return bitset;
	}

	public static int getBitsRequiredToStoreUnsignedInt(int value) {
		int requiredBits = 1;
		while (value > Math.pow(2, requiredBits) - 1) {
			requiredBits++;
		}
		return requiredBits;
	}

	public static int getBitsRequiredToStoreUnsignedLong(long value) {
		if (value < 0) {
			throw new IllegalStateException("The provided value[" + value + "] cannot be represented by an unsigned long because it is negative.");
		}
		int requiredBits = 1;
		while (value > Math.pow(2, requiredBits) - 1) {
			requiredBits++;
		}
		return requiredBits;
	}

	/**
	 * 
	 * @param fromBytes
	 * @param toBitSet
	 * @param bitsetStartInBits
	 * @return the number of bits written
	 */
	public static int copy(byte[] fromBytes, BitSet toBitSet, int bitsetStartInBits) {
		int totalBits = fromBytes.length * ByteUtil.BITS_PER_BYTE;
		for (int i = 0; i < totalBits; i++) {
			boolean isBitOn = false;
			int byteIndex = i / ByteUtil.BITS_PER_BYTE;
			if (byteIndex < fromBytes.length) {
				Byte theByte = fromBytes[byteIndex];
				int bitIndexInByte = i % ByteUtil.BITS_PER_BYTE;
				isBitOn = ByteUtil.isBitOn(theByte, bitIndexInByte);
			}
			int indexInBitSet = bitsetStartInBits + i;
			toBitSet.set(indexInBitSet, isBitOn);
		}
		return totalBits;
	}

	/**
	 * 
	 * @param fromBytes
	 * @param toBitSet
	 * @param bitsetStartInBits
	 * @return the number of bits written
	 */
	public static int copy(byte[] fromBytes, LargeBitSet toBitSet, long bitsetStartInBits) {
		int totalBits = fromBytes.length * ByteUtil.BITS_PER_BYTE;
		for (int i = 0; i < totalBits; i++) {
			boolean isBitOn = false;
			int byteIndex = i / ByteUtil.BITS_PER_BYTE;
			if (byteIndex < fromBytes.length) {
				Byte theByte = fromBytes[byteIndex];
				int bitIndexInByte = i % ByteUtil.BITS_PER_BYTE;
				isBitOn = ByteUtil.isBitOn(theByte, bitIndexInByte);
			}
			long indexInBitSet = bitsetStartInBits + i;
			toBitSet.set(indexInBitSet, isBitOn);
		}
		return totalBits;
	}

	/**
	 * 
	 * @param fromBytes
	 * @param toBitSet
	 * @param bitsetStartInBits
	 * @return the number of bits written
	 */
	public static int copy(byte[] fromBytes, int bitsToUse, BitSet toBitSet, int bitsetStartInBits) {
		for (int i = 0; i < bitsToUse; i++) {
			int byteIndex = i / ByteUtil.BITS_PER_BYTE;
			boolean isBitOn = false;
			if (fromBytes.length > byteIndex) {
				Byte theByte = fromBytes[byteIndex];
				int bitIndexInByte = i % ByteUtil.BITS_PER_BYTE;
				isBitOn = ByteUtil.isBitOn(theByte, bitIndexInByte);
			}
			int indexInBitSet = bitsetStartInBits + i;
			toBitSet.set(indexInBitSet, isBitOn);
		}
		return bitsToUse;
	}

	/**
	 * 
	 * @param fromBytes
	 * @param toBitSet
	 * @param bitsetStartInBits
	 * @return the number of bits written
	 */
	public static int copy(byte[] fromBytes, int bitsToUse, LargeBitSet toBitSet, long bitsetStartInBits) {
		for (int i = 0; i < bitsToUse; i++) {
			int byteIndex = i / ByteUtil.BITS_PER_BYTE;
			boolean isBitOn = false;
			if (fromBytes.length > byteIndex) {
				Byte theByte = fromBytes[byteIndex];
				int bitIndexInByte = i % ByteUtil.BITS_PER_BYTE;
				isBitOn = ByteUtil.isBitOn(theByte, bitIndexInByte);
			}
			long indexInBitSet = bitsetStartInBits + i;
			toBitSet.set(indexInBitSet, isBitOn);
		}
		return bitsToUse;
	}

	/**
	 * 
	 * @param fromBitSet
	 * @param fromBitSetLength
	 * @param toBitSet
	 * @param toBitsetStartInBits
	 * @return the number of bits written
	 */
	public static int copy(BitSet fromBitSet, int fromBitSetLength, BitSet toBitSet, int toBitsetStartInBits) {
		for (int fromBitIndex = 0; fromBitIndex < fromBitSetLength; fromBitIndex++) {
			boolean isBitOn = fromBitSet.get(fromBitIndex);
			int toBitIndex = toBitsetStartInBits + fromBitIndex;
			toBitSet.set(toBitIndex, isBitOn);
		}
		return fromBitSetLength;
	}

	/**
	 * 
	 * @param fromBitSet
	 * @param fromBitSetLengthInBits
	 * @param toBitSet
	 * @param toBitsetStartInBits
	 * @return the number of bits written
	 */
	public static int copy(BitSet fromBitSet, int fromBitSetLengthInBits, LargeBitSet toBitSet, long toBitsetStartInBits) {
		for (int fromBitIndex = 0; fromBitIndex < fromBitSetLengthInBits; fromBitIndex++) {
			boolean isBitOn = fromBitSet.get(fromBitIndex);
			long toBitIndex = toBitsetStartInBits + ((long) fromBitIndex);
			toBitSet.set(toBitIndex, isBitOn);
		}
		return fromBitSetLengthInBits;
	}

	public static byte[] getByteArray(BitSet bitset, int startBit, int stopBit) {
		return bitset.get(startBit, stopBit + 1).toByteArray();
	}

	public static byte[] getByteArray(IBitSet bitset, long startBit, long stopBit) {
		byte[] returnBytes = bitset.getBitSet(startBit, stopBit + 1).toByteArray();
		int length = (int) (stopBit - startBit + 1);
		if (returnBytes.length == 0) {
			returnBytes = new byte[length / ByteUtil.BITS_PER_BYTE];
		}
		return returnBytes;
	}

}
