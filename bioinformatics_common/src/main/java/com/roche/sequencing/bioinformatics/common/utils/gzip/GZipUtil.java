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
package com.roche.sequencing.bioinformatics.common.utils.gzip;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.text.GZipIndex;
import com.roche.sequencing.bioinformatics.common.text.GZipIndex.GZipBlockIndex;
import com.roche.sequencing.bioinformatics.common.utils.ByteUtil;
import com.roche.sequencing.bioinformatics.common.utils.CheckSumUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.IInputStreamFactory;
import com.roche.sequencing.bioinformatics.common.utils.InputStreamFactory;
import com.roche.sequencing.bioinformatics.common.utils.gzip.nayuki.CircularDictionary;
import com.roche.sequencing.bioinformatics.common.utils.gzip.nayuki.Deflate;

public final class GZipUtil {

	private final static Logger logger = LoggerFactory.getLogger(GZipUtil.class);

	private final static int BITS_PER_BYTE = 8;

	private final static int GZIP_MAGIC_NUMBER_ID_ONE = 31;
	private final static int GZIP_MAGIC_NUMBER_ID_TWO = 139;

	private final static int DEFLATE_COMPRESSION_METHOD_INDICATOR = 8;
	private final static int DEFLATE_EXTRA_FLAGS_MAX_COMPRESSION = 2;
	private final static int DEFLATE_EXTRA_FLAGS_FASTEST_COMPRESSION = 4;

	private final static ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

	private final static int BYTE_BUFFER_SIZE = 4096;

	private final static int BGZF_SUBFIELD_BLOCK_SIZE_IDENTIFIER_ONE = 66;
	private final static int BGZF_SUBFIELD_BLOCK_SIZE_IDENTIFIER_TWO = 67;

	private final static int MIN_GZIP_MEMBER_DATA_SIZE_IN_BYTES = 10;

	private GZipUtil() {
		throw new AssertionError();
	}

	public static boolean isCompressed(File file) {
		boolean isCompressed = false;
		try {
			isCompressed = isCompressed(new InputStreamFactory(file));
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
		return isCompressed;
	}

	public static boolean isCompressed(IInputStreamFactory inputStreamFactory) throws FileNotFoundException, IOException {
		boolean isCompressed;

		try {
			byte[] bytes = new byte[10];

			try (DataInputStream dis = new DataInputStream(inputStreamFactory.createInputStream())) {
				dis.readFully(bytes);
			}
			isCompressed = isCompressed(bytes);
			;
		} catch (EOFException e) {
			// there is no way the file can be compressed since it doesn't contain enough bytes to hold the compression magic keys
			isCompressed = false;
		}

		return isCompressed;
	}

	/*
	 * Determines if a byte array is compressed. The java.util.zip GZip implementation does not expose the GZip header so it is difficult to determine if a string is compressed.
	 * 
	 * @param bytes an array of bytes
	 * 
	 * @return true if the array is compressed or false otherwise
	 * 
	 * @throws java.io.IOException if the byte array couldn't be read
	 */
	private static boolean isCompressed(byte[] bytes) throws IOException {
		boolean isCompressed = false;
		if ((bytes != null) && (bytes.length >= 2)) {
			isCompressed = ((bytes[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8)));
		}
		return isCompressed;
	}

	public static GZipMemberData uncompressMemberData(String inputStreamName, IInput input, boolean shouldPerformCrcCheck) throws IOException {
		int currentByteIndex = 0;

		GZipMemberData member = null;

		byte[] bytes = new byte[BYTE_BUFFER_SIZE];
		int bytesRead = input.read(bytes);

		if (bytesRead > MIN_GZIP_MEMBER_DATA_SIZE_IN_BYTES) {
			if (bytesRead < bytes.length) {
				bytes = ByteUtil.copyOf(bytes, 0, bytesRead);
			}

			// eat up the zero padded bytes
			int byteAsInt = 0;
			while (byteAsInt == 0) {
				byteAsInt = ByteUtil.convertByteToInt(bytes[currentByteIndex++], false);
			}

			int firstMagicNumber = byteAsInt;
			int secondMagicNumber = ByteUtil.convertByteToInt(bytes[currentByteIndex++], false);

			if (firstMagicNumber != GZIP_MAGIC_NUMBER_ID_ONE) {
				throw new IllegalStateException("The provided input[" + inputStreamName + "] is not a valid gzip block.  Expected the gzip magic number [" + GZIP_MAGIC_NUMBER_ID_ONE
						+ "] at the first byte but found [" + firstMagicNumber + "].");
			}

			if (secondMagicNumber != GZIP_MAGIC_NUMBER_ID_TWO) {
				throw new IllegalStateException("The provided input[" + inputStreamName + "] is not a valid gzip block.  Expected the gzip magic number [" + GZIP_MAGIC_NUMBER_ID_TWO
						+ "] at the second byte but found [" + secondMagicNumber + "].");
			}
			int compressionMethodIdentifier = ByteUtil.convertByteToInt(bytes[currentByteIndex++], false);
			if (compressionMethodIdentifier != DEFLATE_COMPRESSION_METHOD_INDICATOR) {
				throw new IllegalStateException(
						"The provided compression method[" + compressionMethodIdentifier + "] does not match the expected compression method indicator for 'deflate' compression ["
								+ DEFLATE_COMPRESSION_METHOD_INDICATOR + "] which is the only type of compression this implementation can handle.");
			}

			byte flagsByte = bytes[currentByteIndex++];
			boolean fileIsAsciiText = ByteUtil.isBitOn(flagsByte, 0);
			boolean cyclicRedundancyCheckIsInHeader = ByteUtil.isBitOn(flagsByte, 1);
			boolean extraFieldsInHeader = ByteUtil.isBitOn(flagsByte, 2);
			boolean originalFileNameInHeader = ByteUtil.isBitOn(flagsByte, 3);
			boolean commentInHeader = ByteUtil.isBitOn(flagsByte, 4);

			if (ByteUtil.isBitOn(flagsByte, 5)) {
				logger.warn("The first reserved bit in the FLG section(6th bit of 4th byte) was set to 1 and it requires a value of 0.  This suggests that the gzip block may be corrupt.");
			}
			if (ByteUtil.isBitOn(flagsByte, 6)) {
				logger.warn("The second reserved bit in the FLG section(7th bit of 4th byte) was set to 1 and it requires a value of 0.  This suggests that the gzip block may be corrupt.");
			}
			if (ByteUtil.isBitOn(flagsByte, 7)) {
				logger.warn("The third reserved bit in the FLG section(8th bit of 4th byte) was set to 1 and it requires a value of 0.  This suggests that the gzip block may be corrupt.");
			}

			byte[] modificationTimeBytes = ByteUtil.copyOf(bytes, currentByteIndex, 4);
			currentByteIndex += 4;
			int modificationInSecondsFrom1970 = ByteUtil.convertBytesToInt(modificationTimeBytes, BYTE_ORDER, false);
			LocalDateTime originalFileLastModifiedDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(modificationInSecondsFrom1970), TimeZone.getTimeZone("UTC").toZoneId());

			int extraFlags = ByteUtil.convertByteToInt(bytes[currentByteIndex++], false);
			DeflateCompressionEnum deflateCompression = null;
			if (extraFlags == DEFLATE_EXTRA_FLAGS_FASTEST_COMPRESSION) {
				deflateCompression = DeflateCompressionEnum.FASTEST_COMPRESSION;
			} else if (extraFlags == DEFLATE_EXTRA_FLAGS_MAX_COMPRESSION) {
				deflateCompression = DeflateCompressionEnum.MAX_COMPRESSION;
			}

			int osAsInt = ByteUtil.convertByteToInt(bytes[currentByteIndex++], false);
			GZipOperatingSystemEnum operatingSystem = GZipOperatingSystemEnum.getOperatingSystemEnum(osAsInt);

			byte[] extraBytes = null;
			if (extraFieldsInHeader) {
				byte[] extraFieldSizeBytes = ByteUtil.copyOf(bytes, currentByteIndex, 2);
				currentByteIndex += 2;
				int sizeInBytesOfExtraFields = ByteUtil.convertBytesToInt(extraFieldSizeBytes, BYTE_ORDER, false);
				extraBytes = ByteUtil.copyOf(bytes, currentByteIndex, sizeInBytesOfExtraFields);
				currentByteIndex += sizeInBytesOfExtraFields;
			}

			List<GzipHeaderExtraSubfield> subfields = new ArrayList<GzipHeaderExtraSubfield>();
			if (extraBytes != null) {
				int extraBytesIndex = 0;
				while (extraBytesIndex < extraBytes.length) {
					int subfieldIdentifierOne = ByteUtil.convertByteToInt(extraBytes[extraBytesIndex++], false);
					int subfieldIdentifierTwo = ByteUtil.convertByteToInt(extraBytes[extraBytesIndex++], false);
					byte[] subfieldLengthBytes = ByteUtil.copyOf(extraBytes, extraBytesIndex, 2);
					extraBytesIndex += 2;
					int subfieldLength = ByteUtil.convertBytesToInt(subfieldLengthBytes, BYTE_ORDER, false);
					byte[] subfieldBytes = ByteUtil.copyOf(extraBytes, extraBytesIndex, subfieldLength);
					extraBytesIndex += subfieldLength;
					subfields.add(new GzipHeaderExtraSubfield(subfieldIdentifierOne, subfieldIdentifierTwo, subfieldBytes));
				}
			}

			String originalFileName = null;
			if (originalFileNameInHeader) {
				StringBuilder stringBuilder = new StringBuilder();
				int currentByteValue = 0;
				while ((currentByteValue = ByteUtil.convertByteToInt(bytes[currentByteIndex++], false)) != 0) {
					stringBuilder.append((char) currentByteValue);
				}
				originalFileName = stringBuilder.toString();
			}

			String comment = null;
			if (commentInHeader) {
				StringBuilder stringBuilder = new StringBuilder();
				int currentByteValue = 0;
				while ((currentByteValue = ByteUtil.convertByteToInt(bytes[currentByteIndex++], false)) != 0) {
					stringBuilder.append((char) currentByteValue);
				}
				comment = stringBuilder.toString();
			}

			Integer cyclicRedundancyCheck16 = null;
			if (cyclicRedundancyCheckIsInHeader) {
				byte[] cyclicRedundancyCheckBytes = ByteUtil.copyOf(bytes, currentByteIndex, 2);
				currentByteIndex += 2;
				cyclicRedundancyCheck16 = ByteUtil.convertBytesToInt(cyclicRedundancyCheckBytes, BYTE_ORDER, false);
			}

			if (shouldPerformCrcCheck && cyclicRedundancyCheck16 != null) {
				byte[] memberBytes = ByteUtil.copyOf(bytes, 0, currentByteIndex);
				int calculatedCheckSum = CheckSumUtil.crc16(memberBytes);
				if (calculatedCheckSum != cyclicRedundancyCheck16) {
					throw new IllegalStateException("The cyclic redundancy check calculated for the member header[" + calculatedCheckSum + "]  does not match the crc16 value provided in the header["
							+ cyclicRedundancyCheck16 + "].");
				}
			}

			member = new GZipMemberData(fileIsAsciiText, originalFileLastModifiedDate, deflateCompression, operatingSystem, subfields, originalFileName, comment, cyclicRedundancyCheck16,
					currentByteIndex);
		}

		return member;
	}

	public static Integer getBgzfBlockSize(GZipMemberData member) {
		Integer bgzfBlockSize = null;
		List<GzipHeaderExtraSubfield> subfields = member.getSubfields();

		subfieldLoop: for (GzipHeaderExtraSubfield subfield : subfields) {
			if ((subfield.getSubfieldIdentifierOne() == BGZF_SUBFIELD_BLOCK_SIZE_IDENTIFIER_ONE) && (subfield.getSubfieldIdentifierTwo() == BGZF_SUBFIELD_BLOCK_SIZE_IDENTIFIER_TWO)) {
				bgzfBlockSize = ByteUtil.convertBytesToInt(subfield.getSubfieldBytes(), BYTE_ORDER, false) - 1;
				break subfieldLoop;
			}
		}
		return bgzfBlockSize;
	}

	public static void decodeGZipFile(InputStreamFactory inputStreamFactory, IGZipParser gzipParser) {
		// TODO set to all true when crc and isize checks are working
		decodeGZipFile(inputStreamFactory, gzipParser, false, false, false);
	}

	public static void decodeGZipFile(InputStreamFactory inputStreamFactory, IGZipParser gzipParser, boolean performHeaderCRC, boolean performBodyCRC, boolean verifyISize) {
		long compressedPositionInBytes = 0;
		long uncompressedPositionInBytes = 0;
		String inputName = inputStreamFactory.getName();
		try (BufferedInputStream inputStream = new BufferedInputStream(inputStreamFactory.createInputStream())) {
			boolean lastDatasetWasFound = false;
			while (!lastDatasetWasFound) {
				DecodeDatasetResult result = decodeNextGZipDataSet(inputName, inputStream, gzipParser, compressedPositionInBytes, uncompressedPositionInBytes, performHeaderCRC, performBodyCRC,
						verifyISize);
				lastDatasetWasFound = result.isLastDatasetInFile;
				compressedPositionInBytes += result.datasetCompressedLengthInBytes;
				uncompressedPositionInBytes += result.datasetUncompressedLengthInBytes;
			}

		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private static class DecodeDatasetResult {
		private final boolean isLastDatasetInFile;
		private final long datasetCompressedLengthInBytes;
		private final long datasetUncompressedLengthInBytes;

		public DecodeDatasetResult(boolean isLastDatasetInFile, long datasetCompressedLengthInBytes, long datasetUncompressedLengthInBytes) {
			super();
			this.isLastDatasetInFile = isLastDatasetInFile;
			this.datasetCompressedLengthInBytes = datasetCompressedLengthInBytes;
			this.datasetUncompressedLengthInBytes = datasetUncompressedLengthInBytes;
		}

	}

	public static DecodeDatasetResult decodeNextGZipDataSet(String inputName, BufferedInputStream inputStream, IGZipParser gzipParser, long datasetCompressedStartInBytes,
			long datasetUncompressedStartInBytes, boolean performHeaderCRC, boolean performBodyCRC, boolean verifyISize) {
		long compressedBytePosition = datasetCompressedStartInBytes;
		long uncompressedBytePosition = datasetUncompressedStartInBytes;

		boolean isLastDatasetInFile = true;

		try {

			inputStream.mark(Integer.MAX_VALUE);
			GZipMemberData member = uncompressMemberData(inputName, new InputStreamInput(inputStream), performHeaderCRC);
			if (member != null) {
				gzipParser.datasetStarted(member, datasetCompressedStartInBytes);
				inputStream.reset();

				inputStream.skip(member.getCompressedSizeInBytes());
				compressedBytePosition += member.getCompressedSizeInBytes();

				GZipBlock block = null;

				int currentBitPositionInByte = 0;

				byte[] startingDictionaryBytes = null;

				boolean isFinalBlock = false;
				while (!isFinalBlock) {
					inputStream.mark(Integer.MAX_VALUE);

					block = decodeNextGZipBlock(inputStream, currentBitPositionInByte, startingDictionaryBytes);

					// not all of the bytes in the initial dictionary are used so there is no point in storing them
					// this basically gets rid of the first n unused bytes
					int bytesUsedInDictionaryBytes = block.getNumberOfBytesInInitialDictionaryUsed();
					startingDictionaryBytes = ByteUtil.copyOf(startingDictionaryBytes, CircularDictionary.DICTIONARY_SIZE - bytesUsedInDictionaryBytes, bytesUsedInDictionaryBytes);

					long blockCompressedStartPositionInBits = (compressedBytePosition * BITS_PER_BYTE) + currentBitPositionInByte;

					gzipParser.blockParsed(member, block, startingDictionaryBytes, blockCompressedStartPositionInBits);

					uncompressedBytePosition += block.getUncompressedSizeInBytes();

					startingDictionaryBytes = block.getEndingDictionaryBytes();

					isFinalBlock = block.isFinalBlock();
					inputStream.reset();

					if (block.getCompressedSizeInBits() <= 0) {
						throw new IllegalStateException("block size[" + block.getCompressedSizeInBits() + "].");
					}

					long compressedByteIndex = (block.getCompressedSizeInBits() + currentBitPositionInByte) / BITS_PER_BYTE;

					inputStream.skip(compressedByteIndex);
					compressedBytePosition += compressedByteIndex;

					currentBitPositionInByte = (int) ((block.getCompressedSizeInBits() + currentBitPositionInByte) % BITS_PER_BYTE);

					if (Thread.currentThread().isInterrupted()) {
						throw new RuntimeException("Decoding was stopped.");
					}
				}

				// need to read crc and and ISize
				byte[] nextFourBytes = new byte[4];
				inputStream.read(nextFourBytes);
				compressedBytePosition += 4;
				long crc32 = ByteUtil.convertBytesToLong(nextFourBytes, BYTE_ORDER, false);

				inputStream.read(nextFourBytes);
				compressedBytePosition += 4;
				long iSize = ByteUtil.convertBytesToLong(nextFourBytes, BYTE_ORDER, false);

				if (performBodyCRC) {
					// TODO not working
					long calculatedCheckSum = -1;
					if (calculatedCheckSum != crc32) {
						throw new IllegalStateException("The cyclic redundancy check calculated for the gzip dataset[" + calculatedCheckSum + "]  does not match the crc32 value[" + crc32
								+ "] provided after the compressed blocks in the gzip dataset.");
					} else {
						System.out.println("looking good:" + calculatedCheckSum);
					}
				}

				if (verifyISize) {
					// TODO not working
					long uncompressedSize = -1;
					if (iSize != uncompressedSize) {
						throw new IllegalStateException("The size calculated for the gzip dataset[" + uncompressedSize + "]  does not match the provided uncompressed size value[" + iSize
								+ "] provided after the compressed blocks in the gzip dataset.");
					}
				}

				gzipParser.datasetCompleted(member, compressedBytePosition, crc32, iSize);

				isLastDatasetInFile = ((iSize == 0) && (crc32 == 0));
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return new DecodeDatasetResult(isLastDatasetInFile, compressedBytePosition - datasetCompressedStartInBytes, (uncompressedBytePosition - datasetUncompressedStartInBytes));
	}

	public static GZipBlock decodeNextGZipBlock(RandomAccessFile randomAccessFile, int byteStartPosition, int bitStartPositionInCurrentByte, byte[] startingDictionaryBytes) throws IOException {
		randomAccessFile.seek(byteStartPosition);
		GZipBlock block = Deflate.deflateBlock(new RandomAccessFileInput(randomAccessFile), bitStartPositionInCurrentByte, startingDictionaryBytes);
		return block;
	}

	public static GZipBlock decodeNextGZipBlock(RandomAccessFile randomAccessFile, int bitStartPositionInCurrentByte, byte[] startingDictionaryBytes) throws IOException {
		GZipBlock block = Deflate.deflateBlock(new RandomAccessFileInput(randomAccessFile), bitStartPositionInCurrentByte, startingDictionaryBytes);
		return block;
	}

	public static GZipBlock decodeNextGZipBlock(InputStream inputStream, int bitStartPositionInCurrentByte, byte[] startingDictionaryBytes) throws IOException {
		GZipBlock block = Deflate.deflateBlock(new InputStreamInput(inputStream), bitStartPositionInCurrentByte, startingDictionaryBytes);
		return block;
	}

	public static byte[] compressBytes(byte[] bytesToCompress) {
		byte[] compressedData = null;
		try (ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream(bytesToCompress.length)) {
			try (GZIPOutputStream zipInputStream = new GZIPOutputStream(byteOutputStream)) {
				zipInputStream.write(bytesToCompress);
			}
			compressedData = byteOutputStream.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return compressedData;
	}

	public static byte[] uncompressBytes(byte[] bytesToUncompress) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] data = new byte[16384];
		int numberOfBytesRead;

		try (GZIPInputStream zippedInputStream = new GZIPInputStream(new ByteArrayInputStream(bytesToUncompress))) {
			while ((numberOfBytesRead = zippedInputStream.read(data, 0, data.length)) != -1) {
				outputStream.write(data, 0, numberOfBytesRead);
			}
			outputStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		byte[] uncompressedBytes = outputStream.toByteArray();
		return uncompressedBytes;
	}

	public static void compressFile(File inputFile, File outputFile) throws FileNotFoundException, IOException {
		byte[] buffer = new byte[4096];
		FileUtil.createNewFile(outputFile);
		try (GZIPOutputStream output = new GZIPOutputStream(new FileOutputStream(outputFile))) {
			try (FileInputStream input = new FileInputStream(inputFile)) {
				int len;
				while ((len = input.read(buffer)) > 0) {
					output.write(buffer, 0, len);
				}
			}
		}
	}

	public static GZipBlock getFirstBlock(GZipIndex zipIndex, File file) throws FileNotFoundException, IOException {
		GZipBlock block = null;
		try (RandomAccessFile randomAccessToFile = new RandomAccessFile(file, "r")) {
			GZipBlockIndex blockIndex = zipIndex.getBlockIndex(0);
			long startInBits = blockIndex.getCompressedStartInBits();
			long startInBytes = startInBits / BITS_PER_BYTE;
			int bitStartInFirstByte = (int) (startInBits % BITS_PER_BYTE);

			randomAccessToFile.seek(startInBytes);

			try {
				block = GZipUtil.decodeNextGZipBlock(randomAccessToFile, bitStartInFirstByte, new byte[0]);
			} catch (IllegalStateException e) {
			}
		}
		return block;
	}

	public static void main(String[] args) {
		// File gzipFile = new File("D:\\kurts_space\\hsq_with_pete\\1\\Batch2_Capture1_NA12878_L001_R1_001.fastq.gz");
		File gzipFile = new File("D:\\kurts_space\\hsq_with_pete\\1\\Batch2_Capture1_NA12878_L001_R2_001.fastq.gz");
		// File gzipFile = new File("D:\\kurts_space\\hsq_with_pete\\1\\Batch1_Capture1_NA12878_L001_2000000reads_dedup.bam");
		// File gzipFile = new File("C:\\Users\\heilmank\\Desktop\\sample.txt.gz");
		// File gzipFile = new File("C:\\Users\\heilmank\\Desktop\\empty.txt.gz");
		// File gzipFile = new File("C:\\Users\\heilmank\\Desktop\\e.gz");
		// File gzipFile = new File("C:\\Users\\heilmank\\Desktop\\e2.gz");
		// File gzipFile = new File("C:\\Users\\heilmank\\Desktop\\gunzip.c.gz");
		System.out.println("file size:" + gzipFile.length());
		InputStreamFactory inputStreamFactory = new InputStreamFactory(gzipFile);
		TallyMap<GZipMemberData> tally = new TallyMap<GZipMemberData>();
		decodeGZipFile(inputStreamFactory, new IGZipParser() {

			@Override
			public void datasetStarted(GZipMemberData datasetMemberData, long datasetStartInBytes) {
			}

			@Override
			public void datasetCompleted(GZipMemberData datasetMemberData, long datasetEndInBytes, Long crc32, Long iSize) {
			}

			@Override
			public void blockParsed(GZipMemberData datasetMemberData, GZipBlock block, byte[] startingDictionaryBytes, long blockStartPositionInBits) {
				tally.add(datasetMemberData);
			}
		});// , false, false, false, false);
		System.out.println("Number of datasets:" + tally.getObjects().size());
		System.out.println(tally.getHistogramAsString());

	}
}
