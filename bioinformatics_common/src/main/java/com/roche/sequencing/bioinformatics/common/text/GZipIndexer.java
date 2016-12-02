package com.roche.sequencing.bioinformatics.common.text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.roche.sequencing.bioinformatics.common.text.GZipIndex.GZipBlockIndex;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.ByteUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.InputStreamFactory;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.gzip.GZipBlock;
import com.roche.sequencing.bioinformatics.common.utils.gzip.GZipMemberData;
import com.roche.sequencing.bioinformatics.common.utils.gzip.GZipUtil;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IByteDecoder;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IGZipParser;

public class GZipIndexer {

	private final static int BYTES_PER_INT = 4;
	private final static int BYTES_PER_LONG = 8;
	private final static int BITS_PER_BYTE = 8;
	private final static ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
	private final static boolean IS_SIGNED = false;

	private final static int MAGIC_NUMBER = 604010304;
	private final static int VERSION = 1;
	private final static int ENTRY_START_INT = 57;
	private final static byte[] ENTRY_START_CODE = ByteUtil.convertIntToBytes(ENTRY_START_INT, 1, BYTE_ORDER, IS_SIGNED);

	private GZipIndexer() {
		throw new AssertionError();
	}

	public static class GZipIndexPair {
		private final GZipIndex gzipIndex;
		private final TextFileIndex textFileIndex;

		public GZipIndexPair(GZipIndex gzipIndex, TextFileIndex textFileIndex) {
			super();
			this.gzipIndex = gzipIndex;
			this.textFileIndex = textFileIndex;
		}

		public GZipIndex getGzipIndex() {
			return gzipIndex;
		}

		public TextFileIndex getTextFileIndex() {
			return textFileIndex;
		}

	}

	private static class GZipParser implements IGZipParser {

		private final List<Long> linePositionsInBytes;
		private final Map<Integer, Integer> maxCharsByTabCount;
		private final int recordedLineIncrement;
		private final OutputStream dictionariesBytesOutputStream;
		private final List<GZipBlockIndex> blockIndexes;
		private final long totalBytesInFile;
		private final IByteDecoder byteDecoder;
		private final ITextProgressListener optionalProgressListener;

		private long startingLineNumber;
		private long offsetIntoDictionariesBytes;
		private long totalLinesReadInFile;
		private long uncompressedAndDecodedPositionInBytes;

		private long processStartTimeInMs;
		private double lastPercentComplete;

		public GZipParser(List<Long> linePositionsInBytes, Map<Integer, Integer> maxCharsByTabCount, int recordedLineIncrement, OutputStream dictionariesBytesOutputStream,
				List<GZipBlockIndex> blockIndexes, long totalBytesInFile, IByteDecoder optionalByteConverter, ITextProgressListener optionalProgressListener) {
			super();
			this.linePositionsInBytes = linePositionsInBytes;
			this.linePositionsInBytes.add(0L);
			this.maxCharsByTabCount = maxCharsByTabCount;
			this.recordedLineIncrement = recordedLineIncrement;
			this.dictionariesBytesOutputStream = dictionariesBytesOutputStream;
			this.blockIndexes = blockIndexes;
			this.byteDecoder = optionalByteConverter;
			this.uncompressedAndDecodedPositionInBytes = 0;
			this.startingLineNumber = 0;
			this.offsetIntoDictionariesBytes = 0;
			this.totalLinesReadInFile = 0;
			this.optionalProgressListener = optionalProgressListener;
			this.totalBytesInFile = totalBytesInFile;
			this.processStartTimeInMs = System.currentTimeMillis();
			this.lastPercentComplete = 0;

		}

		@Override
		public void blockParsed(GZipMemberData datasetMemberData, GZipBlock block, byte[] startingDictionaryBytes, long blockCompressedStartPositionInBits) {

			if (optionalProgressListener != null) {
				long compressedStartInBytes = blockCompressedStartPositionInBits / BITS_PER_BYTE;
				double percentComplete = ((double) compressedStartInBytes / (double) totalBytesInFile) * 100;
				boolean shouldNotifyOfProgress = Math.floor(percentComplete) > Math.floor(lastPercentComplete);
				if (shouldNotifyOfProgress) {
					long currentProcessTimeInMilliseconds = System.currentTimeMillis() - processStartTimeInMs;
					optionalProgressListener.progressOccurred(new ProgressUpdate((int) totalLinesReadInFile, percentComplete, currentProcessTimeInMilliseconds));
				}
				lastPercentComplete = percentComplete;
			}

			int numberOfNewLinesInBlock = 0;

			long lastInPosition = 0;
			long lastPositionInBytes = -1;
			int currentLineLength = 0;
			int tabsInLine = 0;

			long blockUncompressedDecodedStartPositionInBytes = uncompressedAndDecodedPositionInBytes;

			CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

			byte[] bytes = block.getUncompressedData();

			if (byteDecoder != null) {
				bytes = byteDecoder.decodeBytes(uncompressedAndDecodedPositionInBytes, bytes);
			}

			ByteBuffer in = ByteBuffer.wrap(bytes);

			CharBuffer out = CharBuffer.allocate(1);
			out.position(0);

			while (in.hasRemaining() && (lastPositionInBytes != uncompressedAndDecodedPositionInBytes)) {
				decoder.decode(in, out, true);
				char currentCharacter = out.array()[0];
				int characterLengthInBytes = (int) (in.position() - lastInPosition);
				lastPositionInBytes = uncompressedAndDecodedPositionInBytes;
				uncompressedAndDecodedPositionInBytes += characterLengthInBytes;
				lastInPosition = in.position();
				out.position(0);
				if (currentCharacter == StringUtil.NEWLINE_SYMBOL) {
					numberOfNewLinesInBlock++;
					totalLinesReadInFile++;
					if (totalLinesReadInFile % recordedLineIncrement == 0) {
						linePositionsInBytes.add(uncompressedAndDecodedPositionInBytes);
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

					tabsInLine = 0;
					currentLineLength = 0;
				} else if (StringUtil.TAB.equals("" + currentCharacter)) {
					tabsInLine++;
					currentLineLength++;
				} else {
					currentLineLength++;
				}
			}
			long numberOfBytesInDictionary = 0;
			if (startingDictionaryBytes != null) {

				startingDictionaryBytes = GZipUtil.compressBytes(startingDictionaryBytes);

				numberOfBytesInDictionary = startingDictionaryBytes.length;

				try {
					dictionariesBytesOutputStream.write(startingDictionaryBytes);
					dictionariesBytesOutputStream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			blockIndexes.add(new GZipBlockIndex(blockCompressedStartPositionInBits, blockUncompressedDecodedStartPositionInBytes, startingLineNumber, numberOfNewLinesInBlock, block
					.getCompressedSizeInBits(), offsetIntoDictionariesBytes, numberOfBytesInDictionary));

			startingLineNumber += numberOfNewLinesInBlock;

			offsetIntoDictionariesBytes += numberOfBytesInDictionary;
		}

		@Override
		public void datasetStarted(GZipMemberData datasetMemberData, long datasetStartInBytes) {
		}

		@Override
		public void datasetCompleted(GZipMemberData datasetMemberData, long datasetStartInBytes, Long crc32, Long iSize) {
		}
	}

	public static GZipIndexPair indexGZipBlocks(InputStreamFactory inputStreamFactory, File optionalBlockDictionariesFile, int recordedLineIncrement, ITextProgressListener optionalProgressListener,
			IByteDecoder optionalByteConverter) throws IOException {
		List<Long> linePositionsInBytes = new ArrayList<Long>();
		Map<Integer, Integer> maxCharsByTabCount = new HashMap<Integer, Integer>();

		OutputStream dictionariesBytesOutputStream;
		if (optionalBlockDictionariesFile != null) {
			FileUtil.createNewFile(optionalBlockDictionariesFile);
			dictionariesBytesOutputStream = new FileOutputStream(optionalBlockDictionariesFile);
		} else {
			dictionariesBytesOutputStream = new ByteArrayOutputStream();
		}

		List<GZipBlockIndex> blockIndexes = new ArrayList<GZipBlockIndex>();

		GZipParser parser = new GZipParser(linePositionsInBytes, maxCharsByTabCount, recordedLineIncrement, dictionariesBytesOutputStream, blockIndexes, inputStreamFactory.getSizeInBytes(),
				optionalByteConverter, optionalProgressListener);

		GZipUtil.decodeGZipFile(inputStreamFactory, parser, false, false, false);

		GZipIndex gZipIndex;
		if (optionalBlockDictionariesFile != null) {
			gZipIndex = new GZipIndex(blockIndexes, optionalBlockDictionariesFile);
		} else {
			gZipIndex = new GZipIndex(blockIndexes, ((ByteArrayOutputStream) dictionariesBytesOutputStream).toByteArray());
		}

		dictionariesBytesOutputStream.close();

		TextFileIndex fileIndex = new TextFileIndex(recordedLineIncrement, ArraysUtil.convertToLongArray(linePositionsInBytes), (int) parser.totalLinesReadInFile, maxCharsByTabCount);

		return new GZipIndexPair(gZipIndex, fileIndex);
	}

	public static GZipIndex loadIndexFile(File indexFile, File dictionaryBytes) throws IOException {
		byte[] bytes = FileUtil.readFileAsBytes(indexFile);

		int currentIndex = 0;
		int magicNumber = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, currentIndex, BYTES_PER_INT), BYTE_ORDER, IS_SIGNED);
		currentIndex += BYTES_PER_INT;
		if (magicNumber != MAGIC_NUMBER) {
			throw new IllegalStateException("The provided magic number[" + magicNumber + "] in the gzip index file[" + indexFile.getAbsolutePath() + "] does not match the required magic number["
					+ MAGIC_NUMBER + "].");
		}
		int version = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, currentIndex, BYTES_PER_INT), BYTE_ORDER, IS_SIGNED);
		currentIndex += BYTES_PER_INT;
		if (version != VERSION) {
			throw new IllegalStateException("The provided version[" + version + "] in the gzip index file[" + indexFile.getAbsolutePath() + "] does not match the required version[" + VERSION + "].");
		}

		int nextInt = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, currentIndex, 1), BYTE_ORDER, IS_SIGNED);
		currentIndex += 1;

		List<GZipBlockIndex> blockIndexes = new ArrayList<GZipIndex.GZipBlockIndex>();
		while (nextInt == ENTRY_START_INT) {

			long compressedStartInBits = ByteUtil.convertBytesToLong(ByteUtil.copyOf(bytes, currentIndex, BYTES_PER_LONG), BYTE_ORDER, IS_SIGNED);
			currentIndex += BYTES_PER_LONG;

			long uncompressedStartInBytes = ByteUtil.convertBytesToLong(ByteUtil.copyOf(bytes, currentIndex, BYTES_PER_LONG), BYTE_ORDER, IS_SIGNED);
			currentIndex += BYTES_PER_LONG;

			long startingLineNumber = ByteUtil.convertBytesToLong(ByteUtil.copyOf(bytes, currentIndex, BYTES_PER_LONG), BYTE_ORDER, IS_SIGNED);
			currentIndex += BYTES_PER_LONG;

			long numberOfNewLinesInBlock = ByteUtil.convertBytesToLong(ByteUtil.copyOf(bytes, currentIndex, BYTES_PER_LONG), BYTE_ORDER, IS_SIGNED);
			currentIndex += BYTES_PER_LONG;

			long compressedSizeInBits = ByteUtil.convertBytesToLong(ByteUtil.copyOf(bytes, currentIndex, BYTES_PER_LONG), BYTE_ORDER, IS_SIGNED);
			currentIndex += BYTES_PER_LONG;

			long offsetIntoDictionariesBytes = ByteUtil.convertBytesToLong(ByteUtil.copyOf(bytes, currentIndex, BYTES_PER_LONG), BYTE_ORDER, IS_SIGNED);
			currentIndex += BYTES_PER_LONG;

			long numberOfBytesInDictionary = ByteUtil.convertBytesToLong(ByteUtil.copyOf(bytes, currentIndex, BYTES_PER_LONG), BYTE_ORDER, IS_SIGNED);
			currentIndex += BYTES_PER_LONG;

			GZipBlockIndex index = new GZipBlockIndex(compressedStartInBits, uncompressedStartInBytes, startingLineNumber, (int) numberOfNewLinesInBlock, compressedSizeInBits,
					offsetIntoDictionariesBytes, numberOfBytesInDictionary);

			blockIndexes.add(index);

			if (currentIndex < bytes.length) {
				nextInt = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, currentIndex, 1), BYTE_ORDER, IS_SIGNED);
				currentIndex += 1;
			} else {
				nextInt = 0;
			}
		}

		return new GZipIndex(blockIndexes, dictionaryBytes);
	}

	public static void saveGZipIndexToFile(GZipIndex gzipIndex, File gzipIndexFile) throws IOException {
		List<byte[]> bytesToWrite = new ArrayList<byte[]>();

		bytesToWrite.add(ByteUtil.convertIntToBytes(MAGIC_NUMBER, BYTES_PER_INT, BYTE_ORDER, IS_SIGNED));
		bytesToWrite.add(ByteUtil.convertIntToBytes(VERSION, BYTES_PER_INT, BYTE_ORDER, IS_SIGNED));

		for (GZipBlockIndex blockIndex : gzipIndex.getAllBlockIndexes()) {
			bytesToWrite.add(ENTRY_START_CODE);
			List<Long> longsToWrite = new ArrayList<Long>();
			longsToWrite.add(blockIndex.getCompressedStartInBits());
			longsToWrite.add(blockIndex.getUncompressedDecodedStartInBytes());
			longsToWrite.add(blockIndex.getStartingLineNumber());
			longsToWrite.add((long) blockIndex.getNumberOfNewLinesInBlock());
			longsToWrite.add(blockIndex.getCompressedSizeInBits());
			longsToWrite.add(blockIndex.getOffsetIntoDictionariesBytes());
			longsToWrite.add(blockIndex.getNumberOfBytesInDictionary());
			byte[] longsToWriteAsBytes = ByteUtil.convertLongsToBytes(longsToWrite, BYTES_PER_LONG, BYTE_ORDER, IS_SIGNED);
			bytesToWrite.add(longsToWriteAsBytes);

		}

		try (FileOutputStream output = new FileOutputStream(gzipIndexFile)) {
			for (byte[] currentBytes : bytesToWrite) {
				output.write(currentBytes);
			}
		}
	}

	public static void main(String[] args) {
		// File file = new File("D:\\kurts_space\\hsq_with_pete\\4\\607387-750ng-6hr_merged_TGATAT_L001_R1_001.fastq");
		File file = new File("D:\\kurts_space\\hsq_with_pete\\1\\Batch1_Capture1_NA12878_L001_R2_001.fastq.gz");
		File indexFile = new File(file.getParent(), "." + file.getName() + ".gzx");
		File dictionaryFile = new File(file.getParent(), "." + file.getName() + ".gzdict");
		File textIndexFile = new File(file.getParent(), "." + file.getName() + ".idx");

		// try {
		// GZipIndexPair pair = indexText(file, dictionaryFile, 10);
		// GZipIndex index = pair.getGzipIndex();
		//
		// TextFileIndexer.saveIndexedTextToFile(pair.getTextFileIndex(), textIndexFile);
		//
		// saveGZipIndexToFile(index, indexFile);
		// GZipIndex index2 = loadIndexFile(indexFile, dictionaryFile);
		// if (!index.equals(index2)) {
		// throw new IllegalStateException("indexes are not equal");
		// } else {
		// System.out.println("looks good.");
		// }
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

	}

}
