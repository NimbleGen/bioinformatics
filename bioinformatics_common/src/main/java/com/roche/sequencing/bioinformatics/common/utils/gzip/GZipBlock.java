package com.roche.sequencing.bioinformatics.common.utils.gzip;

import java.util.Arrays;

public class GZipBlock {

	private final boolean isFinalBlock;
	private final DeflateBlockTypeEnum blockType;
	private byte[] uncompressedData;
	private byte[] endingDictionaryBytes;
	private final long compressedSizeInBits;
	private final int numberOfBytesInInitialDictionaryUsed;

	public GZipBlock(boolean isFinalBlock, DeflateBlockTypeEnum blockType, byte[] uncompressedData, long compressedSizeInBits, byte[] endingDictionaryBytes, int numberOfBytesInInitialDictionaryUsed) {
		super();
		this.isFinalBlock = isFinalBlock;
		this.blockType = blockType;
		this.uncompressedData = uncompressedData;
		this.compressedSizeInBits = compressedSizeInBits;
		this.endingDictionaryBytes = endingDictionaryBytes;
		this.numberOfBytesInInitialDictionaryUsed = numberOfBytesInInitialDictionaryUsed;
	}

	public boolean isFinalBlock() {
		return isFinalBlock;
	}

	public DeflateBlockTypeEnum getBlockType() {
		return blockType;
	}

	public byte[] getUncompressedData() {
		return uncompressedData;
	}

	public long getCompressedSizeInBits() {
		return compressedSizeInBits;
	}

	void clearPayload() {
		uncompressedData = null;
		endingDictionaryBytes = null;
	}

	public byte[] getEndingDictionaryBytes() {
		return endingDictionaryBytes;
	}

	public long getUncompressedSizeInBytes() {
		return uncompressedData.length;
	}

	public int getNumberOfBytesInInitialDictionaryUsed() {
		return numberOfBytesInInitialDictionaryUsed;
	}

	@Override
	public String toString() {
		return "GZipBlock [isFinalBlock=" + isFinalBlock + ", blockType=" + blockType + ", endingDictionaryBytes=" + Arrays.toString(endingDictionaryBytes) + ", compressedSizeInBits="
				+ compressedSizeInBits + ", uncompressedSizeInBytes=" + getUncompressedSizeInBytes() + ", numberOfBytesInInitialDictionaryUsed=" + numberOfBytesInInitialDictionaryUsed + "]";
	}

}
