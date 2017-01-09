package com.roche.sequencing.bioinformatics.common.utils.gzip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.BitSetUtil;
import com.roche.sequencing.bioinformatics.common.utils.ByteUtil;
import com.roche.sequencing.bioinformatics.common.utils.ListUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class BamByteDecoder implements IByteDecoder {
	private final static String UNMAPPED_REFERENCE_NAME = "*";
	private final static char[] SEQUENCE_MAPPING = new char[] { '=', 'A', 'C', 'M', 'G', 'R', 'S', 'V', 'T', 'W', 'Y', 'H', 'K', 'D', 'B', 'N' };
	private final static char[] CIGAR_OPERATION_MAPPING = new char[] { 'M', 'I', 'D', 'N', 'S', 'H', 'P', '=', 'X' };
	private final static byte[] BAM_MAGIC_NUMBER_AS_BYTES = "BAM\1".getBytes();
	private final static ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
	private final static boolean IS_SIGNED = true;
	private final static int PHRED_OFFSET = 33;

	private final static String BAM_HEADER = "Query Name" + StringUtil.TAB + "Flag" + StringUtil.TAB + "Reference Name" + StringUtil.TAB + "Position" + StringUtil.TAB + "Map Quality" + StringUtil.TAB
			+ "Cigar" + StringUtil.TAB + "Mate Reference" + StringUtil.TAB + "Mate Position" + StringUtil.TAB + "Template Length" + StringUtil.TAB + "Read Sequence" + StringUtil.TAB + "Read Quality"
			+ StringUtil.TAB + "Tags" + StringUtil.NEWLINE;

	private String[] referenceNames;
	private Map<Long, byte[]> leftOverBytesByBlockEndingUncompressedStartPosition;

	private boolean shouldSetMap;
	private int headerLineNumber;

	public BamByteDecoder() {
		leftOverBytesByBlockEndingUncompressedStartPosition = new HashMap<Long, byte[]>();
		shouldSetMap = true;
	}

	public BamByteDecoder(byte[] firstBlockBytes, File bamBlockIndexFile) throws FileNotFoundException, IOException {
		this();
		leftOverBytesByBlockEndingUncompressedStartPosition = BamBlockIndexer.loadFromFile(bamBlockIndexFile);
		shouldSetMap = false;
		decodeBytes(0, firstBlockBytes);
	}

	@Override
	public byte[] decodeBytes(long blockUncompressedDecodedStartPositionInBytes, byte[] bytesFromBlock) {

		StringBuilder bamStringBuilder = new StringBuilder();

		byte[] bytes = bytesFromBlock;

		byte[] leftOverBytes = null;

		if (bytes.length == 0) {
			byte[] precedingLeftOverBytes = leftOverBytesByBlockEndingUncompressedStartPosition.get(blockUncompressedDecodedStartPositionInBytes);
			if (precedingLeftOverBytes != null) {
				leftOverBytes = precedingLeftOverBytes;
			}

		} else {
			byte[] precedingLeftOverBytes = leftOverBytesByBlockEndingUncompressedStartPosition.get(blockUncompressedDecodedStartPositionInBytes);
			if (precedingLeftOverBytes != null) {
				bytes = ArraysUtil.concatenate(precedingLeftOverBytes, bytesFromBlock);
			}

			boolean isStartOfBamBlock = ArraysUtil.equals(bytes, 0, BAM_MAGIC_NUMBER_AS_BYTES, 0, 3);
			int byteIndex;
			if (isStartOfBamBlock) {
				byteIndex = 4;
				int headerLength = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, IS_SIGNED);
				byteIndex += 4;

				String plainHeaderText = new String(ByteUtil.copyOf(bytes, byteIndex, headerLength));
				byteIndex += headerLength;

				int numberOfLinesInPlainHeaderText = plainHeaderText.split(StringUtil.LINUX_NEWLINE).length;

				bamStringBuilder.append(plainHeaderText);

				int numberOfReferenceSequences = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, IS_SIGNED);
				byteIndex += 4;

				referenceNames = new String[numberOfReferenceSequences];
				headerLineNumber = numberOfLinesInPlainHeaderText + numberOfReferenceSequences + 1;

				for (int i = 0; i < numberOfReferenceSequences; i++) {
					int referenceSequenceNameLengthPlusOne = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, IS_SIGNED);
					byteIndex += 4;

					int referenceSequenceNameLength = referenceSequenceNameLengthPlusOne - 1;

					String referenceSequenceName = new String(ByteUtil.copyOf(bytes, byteIndex, referenceSequenceNameLength));
					byteIndex += referenceSequenceNameLengthPlusOne;

					int referenceSequenceLength = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, IS_SIGNED);
					byteIndex += 4;

					bamStringBuilder.append(referenceSequenceName + StringUtil.TAB + referenceSequenceLength + StringUtil.NEWLINE);
					referenceNames[i] = referenceSequenceName;
				}

				bamStringBuilder.append(BAM_HEADER);
			} else {
				byteIndex = 0;
			}

			bamBlockLoop: while (byteIndex < bytes.length) {

				int bamBlockSize = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, IS_SIGNED);
				byteIndex += 4;
				int byteIndexStart = byteIndex;

				if ((byteIndexStart + bamBlockSize) > bytes.length) {
					int bamBlockStart = byteIndex - 4;
					leftOverBytes = ByteUtil.copyOf(bytes, bamBlockStart, (bytes.length - bamBlockStart));
					break bamBlockLoop;
				}

				int referenceIndex = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, IS_SIGNED);
				byteIndex += 4;

				String referenceName;
				if (referenceIndex == -1) {
					referenceName = UNMAPPED_REFERENCE_NAME;
				} else {
					referenceName = referenceNames[referenceIndex];
				}

				int position = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, IS_SIGNED) + 1;
				byteIndex += 4;

				int readNameLength = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 1), BYTE_ORDER, false);
				byteIndex += 1;

				int mapQuality = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 1), BYTE_ORDER, false);
				byteIndex += 1;

				// int bin =
				ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 2), BYTE_ORDER, false);
				byteIndex += 2;

				int numberOfCigarOperations = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 2), BYTE_ORDER, false);
				byteIndex += 2;

				int flag = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 2), BYTE_ORDER, false);
				byteIndex += 2;

				int sequenceLength = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, IS_SIGNED);
				byteIndex += 4;

				int mateReferenceIndex = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, IS_SIGNED);
				byteIndex += 4;

				String mateReferenceName;
				if (mateReferenceIndex == -1) {
					mateReferenceName = UNMAPPED_REFERENCE_NAME;
				} else {
					mateReferenceName = referenceNames[mateReferenceIndex];
				}

				int matePosition = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, IS_SIGNED) + 1;
				byteIndex += 4;

				int templateLength = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, IS_SIGNED);
				byteIndex += 4;

				// last character is NUL(\0)
				String readName = new String(ByteUtil.copyOf(bytes, byteIndex, readNameLength - 1));
				byteIndex += readNameLength;

				bamStringBuilder.append(readName + StringUtil.TAB + flag + StringUtil.TAB + referenceName + StringUtil.TAB + position + StringUtil.TAB + mapQuality);

				StringBuilder cigarStringBuilder = new StringBuilder();
				for (int i = 0; i < numberOfCigarOperations; i++) {
					byte[] cigarBytes = ByteUtil.copyOf(bytes, byteIndex, 4);
					byteIndex += 4;

					int opIndex = cigarBytes[0] & 0x000F;
					char opCode = CIGAR_OPERATION_MAPPING[opIndex];

					BitSet bitset = new BitSet();
					BitSetUtil.copy(cigarBytes, bitset, 0);
					byte[] shiftedBytes = BitSetUtil.getByteArray(bitset, 4, 59);
					int numberOfRepeats = ByteUtil.convertBytesToInt(shiftedBytes, BYTE_ORDER, false);
					cigarStringBuilder.append(numberOfRepeats + "" + opCode);
				}
				bamStringBuilder.append(StringUtil.TAB + cigarStringBuilder.toString() + StringUtil.TAB + mateReferenceName + StringUtil.TAB + matePosition + StringUtil.TAB + templateLength);

				char[] sequence = new char[sequenceLength];
				int i = 0;
				while (i < sequenceLength) {

					byte currentByte = bytes[byteIndex];

					int firstSeq = (currentByte & 0x0F);
					int secondSeq = (currentByte & 0xF0) >> 4;

					sequence[i] = SEQUENCE_MAPPING[secondSeq];

					if ((i + 1) < sequenceLength) {
						sequence[i + 1] = SEQUENCE_MAPPING[firstSeq];
					}
					i += 2;

					byteIndex++;
				}

				char[] quality = new char[sequenceLength];
				for (int j = 0; j < sequenceLength; j++) {
					quality[j] = (char) (bytes[byteIndex] + PHRED_OFFSET);
					byteIndex += 1;
				}

				bamStringBuilder.append(StringUtil.TAB + ArraysUtil.toString(sequence, "") + StringUtil.TAB + ArraysUtil.toString(quality, ""));

				int bytesLeft = (bamBlockSize - (byteIndex - byteIndexStart));
				while (bytesLeft > 0) {
					String tag = new String(ByteUtil.copyOf(bytes, byteIndex, 2));
					byteIndex += 2;
					char valueType = (char) bytes[byteIndex];
					byteIndex += 1;

					boolean isArrayType = valueType == 'B';
					int entries = 1;
					if (isArrayType) {
						valueType = (char) bytes[byteIndex];
						entries = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, false);
						byteIndex += 4;
					}

					List<String> values = new ArrayList<String>();
					for (int valueIndex = 0; valueIndex < entries; valueIndex++) {
						long longValue = 0;
						switch (valueType) {
						case 'c':
							longValue = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 1), BYTE_ORDER, true);
							byteIndex += 1;
							values.add("" + longValue);
							break;
						case 'C':
							longValue = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 1), BYTE_ORDER, false);
							byteIndex += 1;
							values.add("" + longValue);
							break;
						case 'S':
							longValue = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 2), BYTE_ORDER, true);
							byteIndex += 2;
							values.add("" + longValue);
							break;
						case 's':
							longValue = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 2), BYTE_ORDER, false);
							byteIndex += 2;
							values.add("" + longValue);
							break;
						case 'I':
							longValue = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, true);
							byteIndex += 4;
							values.add("" + longValue);
							break;
						case 'i':
							longValue = ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, false);
							byteIndex += 4;
							values.add("" + longValue);
							break;
						case 'f':
							float floatValue = Float.intBitsToFloat(ByteUtil.convertBytesToInt(ByteUtil.copyOf(bytes, byteIndex, 4), BYTE_ORDER, false));
							byteIndex += 4;
							values.add("" + floatValue);
							break;
						case 'A':
							values.add("" + (char) bytes[byteIndex]);
							byteIndex += 1;
							break;
						case 'H':
							values.add("" + ByteUtil.convertToHexadecimal(bytes[byteIndex]));
							byteIndex += 1;
							break;
						case 'Z':
							StringBuilder stringBuilder = new StringBuilder();
							int value = bytes[byteIndex];
							while ((value >= 33 && value <= 126) || ((char) value == ' ')) {
								stringBuilder.append((char) value);
								byteIndex++;
								value = bytes[byteIndex];
							}
							byteIndex++;
							values.add("" + stringBuilder.toString());
							break;
						default:
							break;
						}
					}
					bamStringBuilder.append(StringUtil.TAB + tag + ":" + valueType + ":" + ListUtil.toString(values, ", "));
					bytesLeft = (bamBlockSize - (byteIndex - byteIndexStart));
				}

				bamStringBuilder.append(StringUtil.NEWLINE);

				if ((bytes.length - byteIndex) < 4) {
					int bamBlockStart = byteIndex;
					leftOverBytes = ByteUtil.copyOf(bytes, bamBlockStart, (bytes.length - bamBlockStart));
					break bamBlockLoop;
				}

			}
		}

		byte[] bamBytes = bamStringBuilder.toString().getBytes();
		long blockEndingInUncompressedBytes = blockUncompressedDecodedStartPositionInBytes + bamBytes.length;
		if (shouldSetMap) {
			leftOverBytesByBlockEndingUncompressedStartPosition.put(blockEndingInUncompressedBytes, leftOverBytes);
		}

		return bamBytes;
	}

	public int getHeaderLineNumber() {
		return headerLineNumber;
	}

	@Override
	public void persistToFile(File file) {
		try {
			BamBlockIndexer.saveToFile(file, leftOverBytesByBlockEndingUncompressedStartPosition);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
