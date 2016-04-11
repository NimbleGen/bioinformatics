package com.roche.sequencing.bioinformatics.common.utils;

import java.util.BitSet;

public class BooleanMatrix {

	private final int width;
	private final int height;
	private final BitSet[] booleanMatrixAsBitSetArray;
	private final static long MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET = 2000000000;

	public BooleanMatrix(int width, int height) {
		this.width = width;
		this.height = height;
		long size = (long) width * (long) height;
		int bitSetsNeeded = (int) ((long) size / (long) MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET) + 1;
		booleanMatrixAsBitSetArray = new BitSet[bitSetsNeeded];
		for (int i = 0; i < bitSetsNeeded; i++) {
			if (i == bitSetsNeeded - 1) {
				// this is the last one
				booleanMatrixAsBitSetArray[i] = new BitSet((int) (size % (long) MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET));
			} else {
				booleanMatrixAsBitSetArray[i] = new BitSet((int) (MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET));
			}
		}
	}

	private long getBitSetIndex(int x, int y) {
		long bitSetIndex = ((long) y * width) + (long) x;
		return bitSetIndex;
	}

	public void setPresent(int x, int y) {
		long bitSetStartIndex = getBitSetIndex(x, y);
		int bitSetToUse = (int) ((long) bitSetStartIndex / (long) MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET);
		int indexInBitSetToUse = (int) (bitSetStartIndex % (long) MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET);

		booleanMatrixAsBitSetArray[bitSetToUse].set(indexInBitSetToUse);
	}

	public boolean isTrue(int x, int y) {
		long bitSetStartIndex = getBitSetIndex(x, y);
		int bitSetToUse = (int) ((long) bitSetStartIndex / (long) MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET);
		int indexInBitSetToUse = (int) (bitSetStartIndex % (long) MAX_NUMBER_OF_BITS_TO_USE_PER_BITSET);

		return booleanMatrixAsBitSetArray[bitSetToUse].get(indexInBitSetToUse);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
