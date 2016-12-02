package com.roche.sequencing.bioinformatics.common.text;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.roche.sequencing.bioinformatics.common.text.GZipIndex.GZipBlockIndex;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.gzip.GZipBlock;
import com.roche.sequencing.bioinformatics.common.utils.gzip.GZipUtil;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IByteDecoder;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IBytes;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IInput;
import com.roche.sequencing.bioinformatics.common.utils.gzip.InputStreamInput;
import com.roche.sequencing.bioinformatics.common.utils.gzip.RandomAccessFileBytes;
import com.roche.sequencing.bioinformatics.common.utils.gzip.RandomAccessFileInput;

public class Document implements IDocument {

	private final static int BITS_PER_BYTE = 8;

	private final static int COPY_TO_FILE_LINE_INCREMENTS = 1000;
	private final static int TEXT_SEARCH_LINE_BUFFER = 1000;

	private final TextFileIndex textFileIndex;
	private final RandomAccessFile randomAccessToFile;
	private final IBytes dictionaryBytes;
	private final GZipIndex gZipIndex;
	private final IByteDecoder byteConverter;

	private String[] lastRetreivedText;
	private int lastStartingLineNumber;
	private int lastEndingLineNumber;

	public Document(TextFileIndex textFileIndex, File file, IByteDecoder optionalByteConverter) throws FileNotFoundException {
		this(textFileIndex, null, null, file, optionalByteConverter);
	}

	public Document(File file, File indexFile, File gZipIndexFile, File gZipDictionaryFile, IByteDecoder optionalByteConverter) throws IOException {
		randomAccessToFile = new RandomAccessFile(file, "r");
		textFileIndex = TextFileIndexer.loadIndexFile(indexFile);
		gZipIndex = GZipIndexer.loadIndexFile(gZipIndexFile, gZipDictionaryFile);
		dictionaryBytes = new RandomAccessFileBytes(new RandomAccessFile(gZipDictionaryFile, "r"));
		this.byteConverter = optionalByteConverter;
	}

	public Document(TextFileIndex textFileIndex, GZipIndex gZipIndex, IBytes dictionaryBytes, File file, IByteDecoder optionalByteConverter) throws FileNotFoundException {
		super();
		this.textFileIndex = textFileIndex;
		this.gZipIndex = gZipIndex;
		this.dictionaryBytes = dictionaryBytes;
		this.randomAccessToFile = new RandomAccessFile(file, "r");
		this.byteConverter = optionalByteConverter;
	}

	@Override
	public int getNumberOfLines() {
		return textFileIndex.getNumberOfLines();
	}

	public int getRecordedLineIncrements() {
		return textFileIndex.getRecordedLineIncrements();
	}

	@Override
	public void copyTextToFile(File outputFile, int startingLineNumber, int endingLineNumberInclusive) throws IOException {
		copyTextToFile(outputFile, startingLineNumber, null, endingLineNumberInclusive, null);
	}

	@Override
	public void copyTextToFile(File outputFile, int startingLineNumber, Integer startingCharacterIndexInLine, int endingLineNumberInclusive, Integer endingCharacterIndexInLine) throws IOException {
		FileUtil.createNewFile(outputFile);
		if (startingCharacterIndexInLine == null) {
			startingCharacterIndexInLine = 0;
		}

		if (startingCharacterIndexInLine < 0) {
			throw new IllegalArgumentException("The provided startingCharacterIndexInLine[" + startingCharacterIndexInLine + "] must be greater than or equal to zero.");
		}

		if (endingCharacterIndexInLine != null && endingCharacterIndexInLine < 1) {
			throw new IllegalArgumentException("The provided startingCharacterIndexInLine[" + startingCharacterIndexInLine + "] must be greater than or equal to one.");
		}

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
			if (startingLineNumber == endingLineNumberInclusive) {
				String lineText = getText(startingLineNumber, startingLineNumber)[0];

				if (endingCharacterIndexInLine == null) {
					endingCharacterIndexInLine = lineText.length() - 1;
				}

				if (startingCharacterIndexInLine >= lineText.length()) {
					throw new IllegalArgumentException("The provided startingCharacterIndexInLine[" + startingCharacterIndexInLine + "] must be less than the provided lines length["
							+ lineText.length() + "].");
				}

				if (startingCharacterIndexInLine > endingCharacterIndexInLine) {
					throw new IllegalArgumentException("The starting line number[" + startingLineNumber + "] and ending line number[" + endingLineNumberInclusive
							+ "]  are the same, but the startingCharacterIndexInLine[" + startingCharacterIndexInLine + "] is greater than the endingCharacterIndexInLine["
							+ endingCharacterIndexInLine + "] which means that no text is available for copying.");
				}

				int characterEndExclusive = Math.min(lineText.length(), endingCharacterIndexInLine + 1);

				lineText = lineText.substring(startingCharacterIndexInLine, characterEndExclusive);
			} else {
				String startLineText = getText(startingLineNumber, startingLineNumber)[0];
				if (startingCharacterIndexInLine >= startLineText.length()) {
					throw new IllegalArgumentException("The provided startingCharacterIndexInLine[" + startingCharacterIndexInLine + "] must be less than the provided lines length["
							+ startLineText.length() + "].");
				}

				if (startingCharacterIndexInLine > 0) {
					writer.write(startLineText.substring(startingCharacterIndexInLine, startLineText.length()) + StringUtil.NEWLINE);
				} else {
					writer.write(startLineText + StringUtil.NEWLINE);
				}

				int totalLines = endingLineNumberInclusive - startingLineNumber + 1;
				int totalLinesWithoutFirstAndLastLine = totalLines - 2;
				int loopsNeeded = (int) Math.ceil((double) totalLinesWithoutFirstAndLastLine / (double) COPY_TO_FILE_LINE_INCREMENTS);
				for (int i = 0; i < loopsNeeded; i++) {
					int startLine = (startingLineNumber + 1) + (COPY_TO_FILE_LINE_INCREMENTS * i);
					int endingLineInclusive = Math.min(startLine + COPY_TO_FILE_LINE_INCREMENTS - 1, (endingLineNumberInclusive - 1));
					String[] text = getText(startLine, endingLineInclusive);
					for (String line : text) {
						writer.write(line + StringUtil.NEWLINE);
					}
				}

				String endingLineText = getText(endingLineNumberInclusive, endingLineNumberInclusive)[0];
				if (endingCharacterIndexInLine == null) {
					endingCharacterIndexInLine = endingLineText.length() - 1;
				}
				if (endingCharacterIndexInLine < (endingLineText.length() - 1)) {
					int characterEndExclusive = Math.min(endingLineText.length(), endingCharacterIndexInLine + 1);
					writer.write(endingLineText.substring(0, characterEndExclusive) + StringUtil.NEWLINE);
				} else {
					writer.write(endingLineText + StringUtil.NEWLINE);
				}

			}
		}
	}

	private long cachedBlockStartingLineNumber;
	private long cachedBlockEndingLineNumber;
	private byte[] cachedBlockBytes;

	@Override
	public synchronized String[] getText(int startingLineNumber, int endingLineNumberInclusive) {
		String[] text;
		if (lastStartingLineNumber == startingLineNumber && lastEndingLineNumber == endingLineNumberInclusive && lastRetreivedText != null) {
			text = lastRetreivedText;
		} else {
			int lineIncrements = textFileIndex.getRecordedLineIncrements();
			int startingIndex = (int) Math.floor((double) startingLineNumber / (double) lineIncrements);
			int retrievedLineNumber = lineIncrements * startingIndex;

			text = new String[endingLineNumberInclusive - startingLineNumber + 1];

			long linesUncompressedDecodedPositionInBytes = textFileIndex.getBytePositionOfLines()[startingIndex];
			int lineNumbersInFile = textFileIndex.getNumberOfLines();
			try {
				IInput input = null;
				if (gZipIndex == null) {
					// this is not a compressed file so uncompressed = compressed
					randomAccessToFile.seek(linesUncompressedDecodedPositionInBytes);
					input = new RandomAccessFileInput(randomAccessToFile);
				} else {
					byte[] bytes;
					long blockStartingLineNumber;
					if (cachedBlockBytes != null && startingLineNumber >= cachedBlockStartingLineNumber && endingLineNumberInclusive <= cachedBlockEndingLineNumber) {
						bytes = cachedBlockBytes;
						blockStartingLineNumber = cachedBlockStartingLineNumber;
					} else {
						GZipBlockIndex blockIndex = gZipIndex.getBlockIndex(linesUncompressedDecodedPositionInBytes);
						long uncompressedDecodedPositionInBytes = blockIndex.getUncompressedDecodedStartInBytes();

						long startInBits = blockIndex.getCompressedStartInBits();
						long compressedStartInBytes = startInBits / BITS_PER_BYTE;
						int bitStartInFirstByte = (int) (startInBits % BITS_PER_BYTE);

						blockStartingLineNumber = blockIndex.getStartingLineNumber();
						long blockEndingLineNumber = blockStartingLineNumber + blockIndex.getNumberOfNewLinesInBlock();

						byte[] startingDictionaryBytes = dictionaryBytes.getBytes(blockIndex.getOffsetIntoDictionariesBytes(), blockIndex.getNumberOfBytesInDictionary());
						startingDictionaryBytes = GZipUtil.uncompressBytes(startingDictionaryBytes);

						randomAccessToFile.seek(compressedStartInBytes);

						GZipBlock block = GZipUtil.decodeNextGZipBlock(randomAccessToFile, bitStartInFirstByte, startingDictionaryBytes);
						bytes = block.getUncompressedData();
						if (byteConverter != null) {
							bytes = byteConverter.decodeBytes(uncompressedDecodedPositionInBytes, bytes);
						}
						uncompressedDecodedPositionInBytes += bytes.length;

						// note: in most cases this will not be run
						while (blockEndingLineNumber < endingLineNumberInclusive) {
							GZipBlockIndex nextBlockIndex = gZipIndex.getBlockIndex(uncompressedDecodedPositionInBytes);

							if (nextBlockIndex == blockIndex) {
								// throw new IllegalStateException("Did not get next block.");
							}

							long nextStartInBits = nextBlockIndex.getCompressedStartInBits();
							long nextCompressedStartInBytes = (nextStartInBits / BITS_PER_BYTE);
							int nextBitStartInFirstByte = (int) (nextStartInBits % BITS_PER_BYTE);

							byte[] nextStartingDictionaryBytes = dictionaryBytes.getBytes(nextBlockIndex.getOffsetIntoDictionariesBytes(), nextBlockIndex.getNumberOfBytesInDictionary());
							nextStartingDictionaryBytes = GZipUtil.uncompressBytes(nextStartingDictionaryBytes);

							randomAccessToFile.seek(nextCompressedStartInBytes);

							block = GZipUtil.decodeNextGZipBlock(randomAccessToFile, nextBitStartInFirstByte, nextStartingDictionaryBytes);
							byte[] nextBytes = block.getUncompressedData();
							if (byteConverter != null) {
								nextBytes = byteConverter.decodeBytes(uncompressedDecodedPositionInBytes, nextBytes);
							}
							uncompressedDecodedPositionInBytes += nextBytes.length;
							bytes = ArraysUtil.concatenate(bytes, nextBytes);

							long nextBlockStartingLineNumber = blockIndex.getStartingLineNumber();
							int newLinesInBlock = blockIndex.getNumberOfNewLinesInBlock();
							blockEndingLineNumber = nextBlockStartingLineNumber + newLinesInBlock;
							startInBits = nextStartInBits;
							blockIndex = nextBlockIndex;
						}

						cachedBlockBytes = bytes;
						cachedBlockStartingLineNumber = blockStartingLineNumber;
						cachedBlockEndingLineNumber = blockEndingLineNumber;
					}

					input = new InputStreamInput(new ByteArrayInputStream(bytes));

					// read until get to the retrievedLineNumber
					long currentLineNumber = blockStartingLineNumber;
					while (currentLineNumber < retrievedLineNumber) {
						input.readLine();
						currentLineNumber++;
					}

				}

				// at this point the input should be at the retrievedLineNumber
				for (int currentLine = retrievedLineNumber; currentLine <= endingLineNumberInclusive; currentLine++) {
					int index = currentLine - startingLineNumber;
					if (currentLine < lineNumbersInFile) {
						String textForLine = input.readLine();
						if (textForLine == null) {
							textForLine = "";
						}
						if (currentLine >= startingLineNumber) {
							text[index] = textForLine;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			lastRetreivedText = text;
			lastStartingLineNumber = startingLineNumber;
			lastEndingLineNumber = endingLineNumberInclusive;
		}
		return text;
	}

	@Override
	public int getNumberOfCharactersInLongestLine(int charactersPerTab) {
		return textFileIndex.getNumberOfCharactersInLongestLine(charactersPerTab);
	}

	@Override
	public TextPosition search(int startingLine, int startingCharacterIndexInLine, boolean isSearchCaseSensitive, String searchString) {
		return search(startingLine, startingCharacterIndexInLine, isSearchCaseSensitive, searchString, null);
	}

	@Override
	public TextPosition search(int startingLine, int startingCharacterIndexInLine, boolean isSearchCaseSensitive, String searchString, ITextProgressListener optionalTextProgressListener) {
		long timeStartInMs = System.currentTimeMillis();
		int linesInDocument = getNumberOfLines();

		searchString = searchString.trim();
		if (!isSearchCaseSensitive) {
			searchString = searchString.toLowerCase();
		}

		int currentStartingLine = startingLine;
		int currentEndingLine = (int) Math.min(currentStartingLine + TEXT_SEARCH_LINE_BUFFER - 1, linesInDocument - 1);
		String[] text = getText(currentStartingLine, currentEndingLine);

		if (startingCharacterIndexInLine > 0) {
			String firstLine = text[0];
			if (startingCharacterIndexInLine < firstLine.length()) {
				text[0] = firstLine.substring(startingCharacterIndexInLine, firstLine.length());
			} else {
				text[0] = "";
			}
		}

		TextPosition searchStringTextPosition = null;
		int linesSearched = 0;

		int lastPercentComplete = 0;

		// getNumberOfLine()+1 is so the first part of the first line is searched after the whole document has been searched
		while (searchStringTextPosition == null && linesSearched < linesInDocument + 1) {
			int currentLine = currentStartingLine;
			while (currentLine <= currentEndingLine && searchStringTextPosition == null) {
				int index = currentLine - currentStartingLine;

				String lineText = text[index];
				if (!isSearchCaseSensitive) {
					lineText = lineText.toLowerCase();
				}

				if (searchString.length() <= lineText.length()) {
					boolean searchStringFound = lineText.contains(searchString);
					if (searchStringFound) {
						int columnIndex = lineText.indexOf(searchString);
						searchStringTextPosition = new TextPosition(currentLine, columnIndex);
					}
				}

				currentLine++;
				linesSearched++;
			}

			if (optionalTextProgressListener != null) {
				double percentComplete = ((double) linesSearched / (double) (linesInDocument + 1)) * 100;
				if (Math.floor(percentComplete) > lastPercentComplete) {
					long timeInMs = System.currentTimeMillis();
					optionalTextProgressListener.progressOccurred(new ProgressUpdate(linesSearched, percentComplete, timeInMs - timeStartInMs));
					lastPercentComplete = (int) Math.floor(percentComplete);
				}
			}

			if (searchStringTextPosition == null) {
				if (currentEndingLine == linesInDocument - 1) {
					currentStartingLine = 0;
				} else {
					currentStartingLine = currentEndingLine + 1;
				}
				currentEndingLine = (int) Math.min(currentStartingLine + TEXT_SEARCH_LINE_BUFFER - 1, linesInDocument - 1);

				text = getText(currentStartingLine, currentEndingLine);
			}
		}

		if (optionalTextProgressListener != null) {
			long timeInMs = System.currentTimeMillis();
			optionalTextProgressListener.progressOccurred(new ProgressUpdate(linesSearched, 100, timeInMs - timeStartInMs));
		}

		return searchStringTextPosition;
	}

	@Override
	public void close() throws IOException {
		randomAccessToFile.close();
		if (dictionaryBytes != null) {
			dictionaryBytes.close();
		}
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		File file = new File("D:\\kurts_space\\hsq_with_pete\\4\\607387-750ng-6hr_merged_TGATAT_L001_R1_001.fastq");
		File indexFile = new File("D:\\kurts_space\\hsq_with_pete\\4\\.607387-750ng-6hr_merged_TGATAT_L001_R1_001.fastq.idx");
		File outputFile = new File("D:\\kurts_space\\hsq_with_pete\\4\\clipboard.txt");
		// TextCache tc = new TextCache(TextFileIndexer.loadIndexFile(indexFile), file);
		// tc.copyTextToFile(outputFile, 83534952, 93535052);

		File gzipFile = new File("D:\\kurts_space\\hsq_with_pete\\4\\clipboard.txt.gz");
		GZipUtil.compressFile(outputFile, gzipFile);
	}

	@Override
	public int getMostTabsFoundInALine() {
		return textFileIndex.getMostTabsFoundInALine();
	}
}
