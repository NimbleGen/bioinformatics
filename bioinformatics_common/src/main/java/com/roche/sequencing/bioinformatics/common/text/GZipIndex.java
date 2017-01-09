package com.roche.sequencing.bioinformatics.common.text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.roche.sequencing.bioinformatics.common.utils.gzip.Bytes;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IBytes;
import com.roche.sequencing.bioinformatics.common.utils.gzip.RandomAccessFileBytes;

public class GZipIndex {

	private final TreeMap<Long, GZipBlockIndex> blockIndexesByUncompressedStartInBytes;

	public GZipIndex(List<GZipBlockIndex> blockIndexes, File blockDictionariesFile) throws FileNotFoundException {
		this(blockIndexes, new RandomAccessFileBytes(new RandomAccessFile(blockDictionariesFile, "r")));
	}

	public GZipIndex(List<GZipBlockIndex> blockIndexes, byte[] blockDictionaries) {
		this(blockIndexes, new Bytes(blockDictionaries));
	}

	public GZipIndex(List<GZipBlockIndex> blockIndexes, IBytes blockDictionaries) {
		this.blockIndexesByUncompressedStartInBytes = new TreeMap<Long, GZipIndex.GZipBlockIndex>();
		for (GZipBlockIndex blockIndex : blockIndexes) {
			blockIndexesByUncompressedStartInBytes.put(blockIndex.getUncompressedDecodedStartInBytes(), blockIndex);
		}
	}

	public GZipBlockIndex getBlockIndex(long positionInUncompressedBytes) {
		GZipBlockIndex index = null;
		Entry<Long, GZipBlockIndex> entry = blockIndexesByUncompressedStartInBytes.floorEntry(positionInUncompressedBytes);
		if (entry != null) {
			index = entry.getValue();
		}
		return index;
	}

	public List<GZipBlockIndex> getAllBlockIndexes() {
		return new ArrayList<GZipIndex.GZipBlockIndex>(blockIndexesByUncompressedStartInBytes.values());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((blockIndexesByUncompressedStartInBytes == null) ? 0 : blockIndexesByUncompressedStartInBytes.hashCode());
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
		GZipIndex other = (GZipIndex) obj;
		if (blockIndexesByUncompressedStartInBytes == null) {
			if (other.blockIndexesByUncompressedStartInBytes != null)
				return false;
		} else if (!blockIndexesByUncompressedStartInBytes.equals(other.blockIndexesByUncompressedStartInBytes))
			return false;
		return true;
	}

	public static class GZipBlockIndex {
		private final long compressedStartInBits;
		private final long uncompressedDecodedStartInBytes;
		private final long startingLineNumber;
		private final int numberOfNewLinesInBlock;
		private final long compressedSizeInBits;
		private final long offsetIntoDictionariesBytes;
		private final long numberOfBytesInDictionary;

		public GZipBlockIndex(long compressedStartInBits, long uncompressedDecodedStartInBytes, long startingLineNumber, int numberOfNewLinesInBlock, long compressedSizeInBits,
				long offsetIntoDictionariesBytes, long bytesInDictionary) {
			super();
			this.compressedStartInBits = compressedStartInBits;
			this.uncompressedDecodedStartInBytes = uncompressedDecodedStartInBytes;
			this.startingLineNumber = startingLineNumber;
			this.numberOfNewLinesInBlock = numberOfNewLinesInBlock;
			this.compressedSizeInBits = compressedSizeInBits;
			this.offsetIntoDictionariesBytes = offsetIntoDictionariesBytes;
			this.numberOfBytesInDictionary = bytesInDictionary;
		}

		public long getCompressedStartInBits() {
			return compressedStartInBits;
		}

		public long getUncompressedDecodedStartInBytes() {
			return uncompressedDecodedStartInBytes;
		}

		public long getStartingLineNumber() {
			return startingLineNumber;
		}

		public int getNumberOfNewLinesInBlock() {
			return numberOfNewLinesInBlock;
		}

		public long getCompressedSizeInBits() {
			return compressedSizeInBits;
		}

		public long getOffsetIntoDictionariesBytes() {
			return offsetIntoDictionariesBytes;
		}

		public long getNumberOfBytesInDictionary() {
			return numberOfBytesInDictionary;
		}

		@Override
		public String toString() {
			return "GZipBlockIndex [compressedStartInBits=" + compressedStartInBits + ", uncompressedStartInBytes=" + uncompressedDecodedStartInBytes + ", startingLineNumber=" + startingLineNumber
					+ ", numberOfNewLinesInBlock=" + numberOfNewLinesInBlock + ", compressedSizeInBits=" + compressedSizeInBits + ", offsetIntoDictionariesBytes=" + offsetIntoDictionariesBytes
					+ ", numberOfBytesInDictionary=" + numberOfBytesInDictionary + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (compressedSizeInBits ^ (compressedSizeInBits >>> 32));
			result = prime * result + (int) (compressedStartInBits ^ (compressedStartInBits >>> 32));
			result = prime * result + (int) (numberOfBytesInDictionary ^ (numberOfBytesInDictionary >>> 32));
			result = prime * result + numberOfNewLinesInBlock;
			result = prime * result + (int) (offsetIntoDictionariesBytes ^ (offsetIntoDictionariesBytes >>> 32));
			result = prime * result + (int) (startingLineNumber ^ (startingLineNumber >>> 32));
			result = prime * result + (int) (uncompressedDecodedStartInBytes ^ (uncompressedDecodedStartInBytes >>> 32));
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
			GZipBlockIndex other = (GZipBlockIndex) obj;
			if (compressedSizeInBits != other.compressedSizeInBits)
				return false;
			if (compressedStartInBits != other.compressedStartInBits)
				return false;
			if (numberOfBytesInDictionary != other.numberOfBytesInDictionary)
				return false;
			if (numberOfNewLinesInBlock != other.numberOfNewLinesInBlock)
				return false;
			if (offsetIntoDictionariesBytes != other.offsetIntoDictionariesBytes)
				return false;
			if (startingLineNumber != other.startingLineNumber)
				return false;
			if (uncompressedDecodedStartInBytes != other.uncompressedDecodedStartInBytes)
				return false;
			return true;
		}

	}

}
