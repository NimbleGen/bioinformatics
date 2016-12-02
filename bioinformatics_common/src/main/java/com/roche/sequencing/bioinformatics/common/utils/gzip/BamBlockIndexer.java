package com.roche.sequencing.bioinformatics.common.utils.gzip;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.roche.sequencing.bioinformatics.common.utils.FileUtil;

public class BamBlockIndexer {

	private final static int BYTES_PER_INT = 4;
	private final static int BYTES_PER_LONG = 8;
	private final static int LONGS_PER_ENTRY = 3;
	private final static int BYTES_PER_ENTRY = BYTES_PER_LONG * LONGS_PER_ENTRY;

	public static void saveToFile(File bamBlockIndexFile, Map<Long, byte[]> leftOverBytesByBlockUncompressedDecodedEndPosition) throws FileNotFoundException, IOException {
		FileUtil.createNewFile(bamBlockIndexFile);
		int bytesSectionStart = 0;
		try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(bamBlockIndexFile))) {
			int bytesForEntries = leftOverBytesByBlockUncompressedDecodedEndPosition.size() * BYTES_PER_ENTRY;

			output.write(new byte[BYTES_PER_INT + bytesForEntries]);

			bytesSectionStart = BYTES_PER_INT + bytesForEntries;

			// write out the bytes keep
			for (byte[] bytes : leftOverBytesByBlockUncompressedDecodedEndPosition.values()) {
				if (bytes != null) {
					output.write(bytes);
				}
			}
		}

		try (RandomAccessFile output = new RandomAccessFile(bamBlockIndexFile, "rw")) {
			output.seek(0);

			output.writeInt(leftOverBytesByBlockUncompressedDecodedEndPosition.size());

			int currentBytePointer = bytesSectionStart;
			for (Entry<Long, byte[]> entry : leftOverBytesByBlockUncompressedDecodedEndPosition.entrySet()) {
				long blockEndingUncompressedStart = entry.getKey();
				byte[] bytes = entry.getValue();
				long bytesLength;
				if (bytes != null) {
					bytesLength = bytes.length;
				} else {
					bytesLength = 0;
				}
				output.writeLong(blockEndingUncompressedStart);
				output.writeLong(currentBytePointer);
				output.writeLong(bytesLength);
				currentBytePointer += bytesLength;
			}
		}
	}

	public static Map<Long, byte[]> loadFromFile(File bamBlockIndexFile) throws FileNotFoundException, IOException {
		Map<Long, byte[]> leftOverBytesByBlockUncompressedDecodedEndPosition = new HashMap<Long, byte[]>();

		try (RandomAccessFile input = new RandomAccessFile(bamBlockIndexFile, "rw")) {
			input.seek(0);

			int size = input.readInt();

			long[] blockEndingUncompressedStart = new long[size];
			long[] bytesLocation = new long[size];
			long[] bytesLength = new long[size];

			for (int i = 0; i < size; i++) {
				blockEndingUncompressedStart[i] = input.readLong();
				bytesLocation[i] = input.readLong();
				bytesLength[i] = input.readLong();
			}

			for (int i = 0; i < size; i++) {
				input.seek(bytesLocation[i]);
				byte[] bytes = new byte[(int) bytesLength[i]];
				input.read(bytes);
				leftOverBytesByBlockUncompressedDecodedEndPosition.put(blockEndingUncompressedStart[i], bytes);
			}
		}

		return leftOverBytesByBlockUncompressedDecodedEndPosition;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Map<Long, byte[]> map = new HashMap<Long, byte[]>();

		map.put(1L, "one".getBytes());
		map.put(2L, "two".getBytes());
		map.put(3L, "three".getBytes());
		map.put(4L, "four".getBytes());
		map.put(5L, "five".getBytes());
		map.put(6L, "six".getBytes());

		File file = new File("C:\\Users\\heilmank\\Desktop\\map.bn");

		saveToFile(file, map);

		Map<Long, byte[]> map2 = loadFromFile(file);
		for (Entry<Long, byte[]> entry : map2.entrySet()) {
			System.out.println(entry.getKey() + " " + new String(entry.getValue()));
		}

	}

}
