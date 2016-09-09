package com.roche.sequencing.bioinformatics.common.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class FileBasedBitSet implements IBitSet {

	private final Logger logger = LoggerFactory.getLogger(FileBasedBitSet.class);
	private final File file;

	private final RandomAccessFile fileReader;

	private final static int BYTES_TO_CACHE_BUFFER = 64;
	private final static int CACHE_STACK_SIZE = 10;

	private final long maxByte;

	private Queue<BitSetCache> cachedQueue;

	public FileBasedBitSet(File file) throws IOException {
		super();
		this.file = file;
		this.maxByte = file.length() - 1;
		this.fileReader = new RandomAccessFile(file, "r");
		cachedQueue = new LinkedList<BitSetCache>();
	}

	private BitSet retrieveFromCache(long fromIndexInBits, long toIndexExclusiveInBits) {
		BitSet bitSet = null;
		long toIndexInBits = toIndexExclusiveInBits - 1;

		Iterator<BitSetCache> cacheIter = cachedQueue.iterator();
		cacheLoop: while (cacheIter.hasNext()) {
			BitSetCache bitSetCache = cacheIter.next();
			long cachedBitStart = bitSetCache.getCachedBitStart();
			long cachedBitStop = bitSetCache.getCachedBitStop();
			BitSet cachedBitSet = bitSetCache.getCachedBitSet();
			if (fromIndexInBits >= cachedBitStart && toIndexInBits <= cachedBitStop) {
				int startInCachedBits = (int) (fromIndexInBits - cachedBitStart);
				int stopInCachedBits = (int) (toIndexInBits - cachedBitStart);
				bitSet = cachedBitSet.get(startInCachedBits, stopInCachedBits + 1);
				break cacheLoop;
			}
		}
		return bitSet;
	}

	private void setCache(long requiredStartingByte, long requiredEndingByte) {
		try {
			int numberOfBytesRequired = (int) (requiredEndingByte - requiredStartingByte + 1);

			int numberOfBytesToRetrieve = numberOfBytesRequired + BYTES_TO_CACHE_BUFFER;
			long startingByte = Math.max(0, requiredStartingByte - (BYTES_TO_CACHE_BUFFER / 2));
			long endingByte = Math.min(startingByte + numberOfBytesToRetrieve - 1, maxByte);

			byte[] bytes = new byte[numberOfBytesToRetrieve];

			fileReader.seek(startingByte);
			fileReader.read(bytes, 0, numberOfBytesToRetrieve);

			BitSet cachedBitSet = new BitSet();
			long cachedBitStart = startingByte * ByteUtil.BITS_PER_BYTE;
			long cachedBitStop = ((endingByte + 1) * ByteUtil.BITS_PER_BYTE) - 1;

			for (long i = cachedBitStart; i <= cachedBitStop; i++) {
				int indexInBitSet = (int) (i - cachedBitStart);
				int byteIndex = (int) ((i / ByteUtil.BITS_PER_BYTE) - startingByte);
				int indexInByte = (int) (i % ByteUtil.BITS_PER_BYTE);

				boolean isBitOn = ByteUtil.isBitOn(bytes[byteIndex], indexInByte);
				if (isBitOn) {
					cachedBitSet.set(indexInBitSet);
				}
			}

			cachedQueue.offer(new BitSetCache(cachedBitSet, cachedBitStart, cachedBitStop));
			if (cachedQueue.size() > CACHE_STACK_SIZE) {
				cachedQueue.poll();
			}
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	@Override
	public BitSet getBitSet(long fromIndexInBits, long toIndexExclusiveInBits) {
		long startingByte = (long) (fromIndexInBits / ByteUtil.BITS_PER_BYTE);
		long endingByte = (long) ((toIndexExclusiveInBits - 1) / ByteUtil.BITS_PER_BYTE);

		BitSet bitSet = retrieveFromCache(fromIndexInBits, toIndexExclusiveInBits);

		if (bitSet == null) {
			setCache(startingByte, endingByte);
			bitSet = retrieveFromCache(fromIndexInBits, toIndexExclusiveInBits);
		}

		return bitSet;
	}

	@Override
	public void writeToFile(File outputFile) throws IOException {
		Files.copy(file, outputFile);
	}

	@Override
	public long size() {
		return file.length() * ByteUtil.BITS_PER_BYTE;
	}

	private static class BitSetCache {
		private final BitSet cachedBitSet;
		private final Long cachedBitStart;
		private final Long cachedBitStop;

		public BitSetCache(BitSet cachedBitSet, Long cachedBitStart, Long cachedBitStop) {
			super();
			this.cachedBitSet = cachedBitSet;
			this.cachedBitStart = cachedBitStart;
			this.cachedBitStop = cachedBitStop;
		}

		public BitSet getCachedBitSet() {
			return cachedBitSet;
		}

		public Long getCachedBitStart() {
			return cachedBitStart;
		}

		public Long getCachedBitStop() {
			return cachedBitStop;
		}

	}

}
