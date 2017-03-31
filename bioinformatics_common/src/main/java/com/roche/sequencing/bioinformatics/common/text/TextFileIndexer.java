package com.roche.sequencing.bioinformatics.common.text;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.BitSetUtil;
import com.roche.sequencing.bioinformatics.common.utils.ByteUtil;
import com.roche.sequencing.bioinformatics.common.utils.InputStreamFactory;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.gzip.GZipUtil;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IBytes;

public class TextFileIndexer {

	private final static int BYTES_FOR_MAIN_ENTRIES = 4;
	private final static int BITS_PER_BYTE = 8;
	private final static ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
	private final static boolean IS_SIGNED = false;

	private final static int MAGIC_NUMBER = 901020304;
	public final static int VERSION = 2;
	private final static int MAP_TERMINATION_CODE = 909090909;

	private TextFileIndexer() {
		throw new AssertionError();
	}

	public static TextFileIndex indexText(File file, int recordedLineIncrement, ITextProgressListener optionalProgressListener) {
		TextFileIndex textFileIndex = null;
		textFileIndex = indexText(new InputStreamFactory(file), recordedLineIncrement, null, optionalProgressListener);
		return textFileIndex;
	}

	public static TextFileIndex indexText(InputStreamFactory inputStreamFactory, int recordedLineIncrement) {
		return indexText(inputStreamFactory, recordedLineIncrement, null, null);
	}

	public static TextFileIndex indexText(InputStreamFactory inputStreamFactory, int recordedLineIncrement, ITextFileIndexerLineListeners lineListener,
			ITextProgressListener optionalProgressListener) {
		List<Long> linePositionsInBytes = new ArrayList<Long>();
		Map<Integer, Integer> maxCharsByTabCount = new HashMap<Integer, Integer>();

		long startTimeInMs = System.currentTimeMillis();

		int linesRead = 1;
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

		boolean isCompressed = false;

		try {
			isCompressed = GZipUtil.isCompressed(inputStreamFactory);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		InputStream inputStream;
		try {
			inputStream = inputStreamFactory.createInputStream();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		if (isCompressed) {
			try {
				inputStream = new GZIPInputStream(inputStream);
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		long fileSizeInBytes = inputStreamFactory.getSizeInBytes();
		double lastPercentUpdated = -1;

		try (InputStream bufferedInputStream = new BufferedInputStream(inputStream)) {

			// this will actually put the current position at the header line but since the first line found
			// is never read it will work as expected
			long currentPositionInBytes = 0;
			linePositionsInBytes.add(currentPositionInBytes);

			byte[] bytes = new byte[4096];

			int numberOfBytesRead = 0;

			while (((numberOfBytesRead = bufferedInputStream.read(bytes)) != -1)) {

				ByteBuffer in = null;
				if (numberOfBytesRead < bytes.length) {
					in = ByteBuffer.wrap(Arrays.copyOf(bytes, numberOfBytesRead));
				} else {
					in = ByteBuffer.wrap(bytes);
				}

				CharBuffer out = CharBuffer.allocate(1);
				out.position(0);

				int lastInPosition = 0;
				long lastPositionInBytes = -1;
				int currentLineLength = 0;
				int tabsInLine = 0;
				StringBuilder currentLine = new StringBuilder();
				while (in.hasRemaining() && (lastPositionInBytes != currentPositionInBytes)) {

					if (optionalProgressListener != null) {
						double percentComplete = ((double) currentPositionInBytes / (double) fileSizeInBytes) * 100;
						int truncatedPercentCopmplete = (int) Math.floor(percentComplete);
						if (truncatedPercentCopmplete >= (lastPercentUpdated + 1)) {
							lastPercentUpdated = truncatedPercentCopmplete;
							long currentProcessTimeInMs = System.currentTimeMillis() - startTimeInMs;
							optionalProgressListener.progressOccurred(new ProgressUpdate((linesRead - 1), percentComplete, currentProcessTimeInMs));
						}
					}

					decoder.decode(in, out, true);
					char currentCharacter = out.array()[0];
					int characterLengthInBytes = (in.position() - lastInPosition);
					lastPositionInBytes = currentPositionInBytes;
					currentPositionInBytes += characterLengthInBytes;
					lastInPosition = in.position();
					out.position(0);
					if (currentCharacter == StringUtil.NEWLINE_SYMBOL) {
						if (lineListener != null) {
							lineListener.lineRead(linesRead, currentLine.toString());
						}
						currentLine = new StringBuilder();

						if (linesRead % recordedLineIncrement == 0) {
							linePositionsInBytes.add(currentPositionInBytes);
						}
						// we need to know which lines might be the longest, but the number of characters per
						// tab is not known here. So assuming that characters per tab is greater than or equal to 1
						// we keep a list of all possible longest lines.
						Integer maxChars = maxCharsByTabCount.get(tabsInLine);
						if (maxChars == null) {
							maxChars = currentLineLength;
							maxCharsByTabCount.put(tabsInLine, maxChars);
						} else {
							if (currentLineLength > maxChars) {
								maxCharsByTabCount.put(tabsInLine, currentLineLength);
							}
						}

						linesRead++;

						tabsInLine = 0;
						currentLineLength = 0;
					} else if (StringUtil.TAB.equals("" + currentCharacter)) {
						currentLine.append(currentCharacter);
						tabsInLine++;
						currentLineLength++;
					} else {
						currentLine.append(currentCharacter);
						currentLineLength++;
					}

					if (Thread.currentThread().isInterrupted()) {
						throw new RuntimeException("Indexing was stopped.");
					}

				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		if (optionalProgressListener != null) {
			double percentComplete = 100;
			long currentProcessTimeInMs = System.currentTimeMillis() - startTimeInMs;
			optionalProgressListener.progressOccurred(new ProgressUpdate((linesRead - 1), percentComplete, currentProcessTimeInMs));
		}

		return new TextFileIndex(fileSizeInBytes, recordedLineIncrement, ArraysUtil.convertToLongArray(linePositionsInBytes), linesRead, maxCharsByTabCount, VERSION);
	}

	public static TextFileIndex indexText(InputStreamFactory inputStreamFactory, int recordedLineIncrement, GZipIndex gZipIndex, IBytes gZipDictionaryBytes, ITextFileIndexerLineListeners lineListener,
			ITextProgressListener optionalProgressListener) {
		List<Long> linePositionsInBytes = new ArrayList<Long>();
		Map<Integer, Integer> maxCharsByTabCount = new HashMap<Integer, Integer>();

		long startTimeInMs = System.currentTimeMillis();

		int linesRead = 1;
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

		boolean isCompressed = false;

		try {
			isCompressed = GZipUtil.isCompressed(inputStreamFactory);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		InputStream inputStream;
		try {
			inputStream = inputStreamFactory.createInputStream();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		if (isCompressed) {
			try {
				inputStream = new GZIPInputStream(inputStream);
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		long fileSizeInBytes = inputStreamFactory.getSizeInBytes();
		double lastPercentUpdated = -1;

		try (InputStream bufferedInputStream = new BufferedInputStream(inputStream)) {

			// this will actually put the current position at the header line but since the first line found
			// is never read it will work as expected
			long currentPositionInBytes = 0;
			linePositionsInBytes.add(currentPositionInBytes);

			byte[] bytes = new byte[4096];

			int numberOfBytesRead = 0;

			StringBuilder currentLine = new StringBuilder();
			while (((numberOfBytesRead = bufferedInputStream.read(bytes)) != -1)) {

				ByteBuffer in = null;
				if (numberOfBytesRead < bytes.length) {
					in = ByteBuffer.wrap(Arrays.copyOf(bytes, numberOfBytesRead));
				} else {
					in = ByteBuffer.wrap(bytes);
				}

				CharBuffer out = CharBuffer.allocate(1);
				out.position(0);

				int lastInPosition = 0;
				long lastPositionInBytes = -1;
				int currentLineLength = 0;
				int tabsInLine = 0;
				while (in.hasRemaining() && (lastPositionInBytes != currentPositionInBytes)) {

					if (optionalProgressListener != null) {
						double percentComplete = ((double) currentPositionInBytes / (double) fileSizeInBytes) * 100;
						int truncatedPercentCopmplete = (int) Math.floor(percentComplete);
						if (truncatedPercentCopmplete >= (lastPercentUpdated + 1)) {
							lastPercentUpdated = truncatedPercentCopmplete;
							long currentProcessTimeInMs = System.currentTimeMillis() - startTimeInMs;
							optionalProgressListener.progressOccurred(new ProgressUpdate((linesRead - 1), percentComplete, currentProcessTimeInMs));
						}
					}

					decoder.decode(in, out, true);
					char currentCharacter = out.array()[0];
					int characterLengthInBytes = (in.position() - lastInPosition);
					lastPositionInBytes = currentPositionInBytes;
					currentPositionInBytes += characterLengthInBytes;
					lastInPosition = in.position();
					out.position(0);
					if (currentCharacter == StringUtil.NEWLINE_SYMBOL) {
						if (lineListener != null) {
							lineListener.lineRead(linesRead, currentLine.toString());
						}
						currentLine = new StringBuilder();

						if (linesRead % recordedLineIncrement == 0) {
							linePositionsInBytes.add(currentPositionInBytes);
						}
						// we need to know which lines might be the longest, but the number of characters per
						// tab is not known here. So assuming that characters per tab is greater than or equal to 1
						// we keep a list of all possible longest lines.
						Integer maxChars = maxCharsByTabCount.get(tabsInLine);
						if (maxChars == null) {
							maxChars = currentLineLength;
							maxCharsByTabCount.put(tabsInLine, maxChars);
						} else {
							if (currentLineLength > maxChars) {
								maxCharsByTabCount.put(tabsInLine, currentLineLength);
							}
						}

						linesRead++;

						tabsInLine = 0;
						currentLineLength = 0;
					} else if (StringUtil.TAB.equals("" + currentCharacter)) {
						currentLine.append(currentCharacter);
						tabsInLine++;
						currentLineLength++;
					} else {
						currentLine.append(currentCharacter);
						currentLineLength++;
					}

					if (Thread.currentThread().isInterrupted()) {
						throw new RuntimeException("Indexing was stopped.");
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		if (optionalProgressListener != null) {
			double percentComplete = 100;
			long currentProcessTimeInMs = System.currentTimeMillis() - startTimeInMs;
			optionalProgressListener.progressOccurred(new ProgressUpdate((linesRead - 1), percentComplete, currentProcessTimeInMs));
		}

		return new TextFileIndex(fileSizeInBytes, recordedLineIncrement, ArraysUtil.convertToLongArray(linePositionsInBytes), linesRead, maxCharsByTabCount, VERSION);
	}

	public static TextFileIndex loadIndexFile(File indexFile) throws IOException {
		BitSet bitSet = BitSetUtil.readBitSetFromFile(indexFile);

		List<Integer> integersFromFile = new ArrayList<Integer>();
		int currentBitIndex = 0;
		Integer lastValue = null;
		do {
			int nextBitIndex = currentBitIndex + BITS_PER_BYTE * BYTES_FOR_MAIN_ENTRIES;
			byte[] bytes = BitSetUtil.getByteArray(bitSet, currentBitIndex, nextBitIndex - 1);
			lastValue = (int) ByteUtil.convertBytesToLong(bytes, BYTE_ORDER, IS_SIGNED);
			integersFromFile.add(lastValue);
			currentBitIndex = nextBitIndex;
		} while (lastValue.intValue() != MAP_TERMINATION_CODE);

		int i = 0;
		int magicNumber = integersFromFile.get(i);
		if (magicNumber != MAGIC_NUMBER) {
			throw new IOException(
					"The provided file is not a Text File Index since it contains the incorrect magic number[" + magicNumber + "] instead of the required magic number[" + MAGIC_NUMBER + "].");
		}
		i++;
		int version = integersFromFile.get(i);
		i++;
		int fileSizeAsIntOne = integersFromFile.get(i);
		i++;
		int fileSizeAsIntTwo = integersFromFile.get(i);
		i++;
		long fileSizeInBytes = convertTwoIntsToLong(fileSizeAsIntOne, fileSizeAsIntTwo);
		int bitsPerEntry = integersFromFile.get(i);
		i++;
		int lineNumberModulus = integersFromFile.get(i);
		i++;
		int numberOfLines = integersFromFile.get(i);
		i++;
		Map<Integer, Integer> maxCharactersInALineByTabCount = new HashMap<Integer, Integer>();
		// -1 for the map termination code
		for (int j = i; j < integersFromFile.size() - 1; j += 2) {
			int tabCount = integersFromFile.get(j);
			int maxChars = integersFromFile.get(j + 1);
			maxCharactersInALineByTabCount.put(tabCount, maxChars);
		}

		List<Long> linePositions = new ArrayList<Long>();
		Long lastLinePosition = null;
		Long linePosition = null;
		do {
			byte[] linePositionAsBytes = BitSetUtil.getByteArray(bitSet, currentBitIndex, currentBitIndex + bitsPerEntry - 1);
			currentBitIndex += bitsPerEntry;
			lastLinePosition = linePosition;
			linePosition = ByteUtil.convertBytesToLong(linePositionAsBytes, BYTE_ORDER, IS_SIGNED);
			linePositions.add(linePosition);

		} while (lastLinePosition == null || linePosition > lastLinePosition);

		return new TextFileIndex(fileSizeInBytes, lineNumberModulus, ArraysUtil.convertToLongArray(linePositions.subList(0, linePositions.size() - 1)), numberOfLines, maxCharactersInALineByTabCount,
				version);
	}

	private static int[] convertLongToTwoInts(long longValue) {
		int intOne = (int) (longValue >> 32);
		int intTwo = (int) longValue;
		return new int[] { intOne, intTwo };
	}

	private static long convertTwoIntsToLong(int intOne, int intTwo) {
		long longValue = (((long) intOne) << 32) | (intTwo & 0xffffffffL);
		return longValue;
	}

	public static void saveIndexedTextToFile(TextFileIndex textFileIndex, File indexFile) throws IOException {
		BitSet bitSet = new BitSet();
		int currentBitIndex = 0;

		long[] linePositions = textFileIndex.getBytePositionOfLines();
		long maxIndexValue = linePositions[linePositions.length - 1];
		int bitsPerEntry = BitSetUtil.getBitsRequiredToStoreUnsignedLong(maxIndexValue);
		int lineNumberModulus = textFileIndex.getRecordedLineIncrements();
		int numberOfLines = textFileIndex.getNumberOfLines();
		long fileSizeInBytes = textFileIndex.getFileSizeInBytes();
		int[] fileSizeAsTwoInts = convertLongToTwoInts(fileSizeInBytes);
		int fileSizeAsIntOne = fileSizeAsTwoInts[0];
		int fileSizeAsIntTwo = fileSizeAsTwoInts[1];

		List<Integer> integersToWrite = new ArrayList<Integer>();
		integersToWrite.add(MAGIC_NUMBER);
		integersToWrite.add(VERSION);
		integersToWrite.add(fileSizeAsIntOne);
		integersToWrite.add(fileSizeAsIntTwo);
		integersToWrite.add(bitsPerEntry);
		integersToWrite.add(lineNumberModulus);
		integersToWrite.add(numberOfLines);
		for (Entry<Integer, Integer> entry : textFileIndex.getMaxCharactersInALineByTabCount().entrySet()) {
			integersToWrite.add(entry.getKey());
			integersToWrite.add(entry.getValue());
		}
		integersToWrite.add(MAP_TERMINATION_CODE);
		byte[] integersToWriteAsBytes = ByteUtil.convertIntegersToBytes(integersToWrite, BYTES_FOR_MAIN_ENTRIES, BYTE_ORDER, IS_SIGNED);
		currentBitIndex += BitSetUtil.copy(integersToWriteAsBytes, bitSet, currentBitIndex);

		for (int i = 0; i < linePositions.length; i++) {
			long linePosition = linePositions[i];
			byte[] locationBytes = ByteUtil.convertLongToBytes(linePosition, (int) Math.ceil(bitsPerEntry / (double) ByteUtil.BITS_PER_BYTE), BYTE_ORDER, IS_SIGNED);
			currentBitIndex += BitSetUtil.copy(locationBytes, bitsPerEntry, bitSet, currentBitIndex);
		}

		BitSetUtil.writeBitSetToFile(bitSet, indexFile);
	}
}
