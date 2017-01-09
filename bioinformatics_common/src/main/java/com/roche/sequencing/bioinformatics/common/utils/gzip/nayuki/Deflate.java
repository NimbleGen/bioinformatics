package com.roche.sequencing.bioinformatics.common.utils.gzip.nayuki;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import com.roche.sequencing.bioinformatics.common.utils.gzip.DeflateBlockTypeEnum;
import com.roche.sequencing.bioinformatics.common.utils.gzip.GZipBlock;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IInput;

public class Deflate {

	/* Code trees for static Huffman codes (btype = 1) */
	private static final CodeTree FIXED_LITERAL_LENGTH_CODE;
	private static final CodeTree FIXED_DISTANCE_CODE;

	static { // Make temporary tables of canonical code lengths
		int[] llcodelens = new int[288];
		Arrays.fill(llcodelens, 0, 144, 8);
		Arrays.fill(llcodelens, 144, 256, 9);
		Arrays.fill(llcodelens, 256, 280, 7);
		Arrays.fill(llcodelens, 280, 288, 8);
		FIXED_LITERAL_LENGTH_CODE = new CodeTree(llcodelens);

		int[] distcodelens = new int[32];
		Arrays.fill(distcodelens, 5);
		FIXED_DISTANCE_CODE = new CodeTree(distcodelens);
	}

	private Deflate() {
		throw new AssertionError();
	}

	public static GZipBlock deflateBlock(IInput input, int startingBit, byte[] startingDictionaryBytes) throws IOException {
		GZipBlock block = null;

		CircularDictionary dictionary;
		if (startingDictionaryBytes != null) {
			dictionary = new CircularDictionary(startingDictionaryBytes);
		} else {
			dictionary = new CircularDictionary();
		}

		IBitInputStream bitInputStream = new BitInputStream(input, startingBit);
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			// Process the stream of blocks
			boolean isFinalBlock = false;

			// Read the block header
			isFinalBlock = bitInputStream.readBitIfNotEof() == 1; // bfinal
			DeflateBlockTypeEnum blockType = DeflateBlockTypeEnum.getBlockType(readInt(bitInputStream, 2)); // btype
			// Decompress rest of block based on the type
			if (blockType == DeflateBlockTypeEnum.NO_COMPRESSION) {
				decompressUncompressedBlock(bitInputStream, dictionary, output);
			} else if (blockType == DeflateBlockTypeEnum.COMPRESSED_WITH_FIXED_HUFFMAN_CODES) {
				decompressHuffmanBlock(bitInputStream, dictionary, output, FIXED_LITERAL_LENGTH_CODE, FIXED_DISTANCE_CODE);
			} else if (blockType == DeflateBlockTypeEnum.COMPRESSED_WITH_DYNAMIC_HUFFMAN_CODES) {
				CodeTree[] litLenAndDist = decodeHuffmanCodes(bitInputStream);
				decompressHuffmanBlock(bitInputStream, dictionary, output, litLenAndDist[0], litLenAndDist[1]);
			} else if (blockType == DeflateBlockTypeEnum.RESERVED) {
				throw new IllegalStateException("Reserved block type");
			} else {
				throw new AssertionError();
			}

			output.flush();

			byte[] uncompressedData = null;
			uncompressedData = output.toByteArray();

			long numberOfCompressedBitsInBlock = bitInputStream.getNumberOfBitsRead();

			block = new GZipBlock(isFinalBlock, blockType, uncompressedData, numberOfCompressedBitsInBlock, dictionary.getDictionaryBytes(), dictionary.getFurthestByteUsedInFirstLoop());
		}

		return block;
	}

	/* Method for reading and decoding dynamic Huffman codes (btype = 2) */
	// Reads from the bit input stream, decodes the Huffman code
	// specifications into code trees, and returns the trees.
	private static CodeTree[] decodeHuffmanCodes(IBitInputStream input) throws IOException {
		int numLitLenCodes = readInt(input, 5) + 257; // hlit + 257
		int numDistCodes = readInt(input, 5) + 1; // hdist + 1

		// Read the code length code lengths
		int numCodeLenCodes = readInt(input, 4) + 4; // hclen + 4
		int[] codeLenCodeLen = new int[19]; // This array is filled in a strange order
		codeLenCodeLen[16] = readInt(input, 3);
		codeLenCodeLen[17] = readInt(input, 3);
		codeLenCodeLen[18] = readInt(input, 3);
		codeLenCodeLen[0] = readInt(input, 3);
		for (int i = 0; i < numCodeLenCodes - 4; i++) {
			if (i % 2 == 0)
				codeLenCodeLen[8 + i / 2] = readInt(input, 3);
			else
				codeLenCodeLen[7 - i / 2] = readInt(input, 3);
		}

		// Create the code length code
		CodeTree codeLenCode;
		try {
			codeLenCode = new CodeTree(codeLenCodeLen);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e.getMessage());
		}

		// Read the main code lengths and handle runs
		int[] codeLens = new int[numLitLenCodes + numDistCodes];
		int runVal = -1;
		int runLen = 0;
		for (int i = 0; i < codeLens.length;) {
			if (runLen > 0) {
				if (runVal == -1)
					throw new AssertionError("Impossible state");
				codeLens[i] = runVal;
				runLen--;
				i++;
			} else {
				int sym = decodeSymbol(input, codeLenCode);
				if (0 <= sym && sym <= 15) {
					codeLens[i] = sym;
					runVal = sym;
					i++;
				} else if (sym == 16) {
					if (runVal == -1)
						throw new IllegalStateException("No code length value to copy");
					runLen = readInt(input, 2) + 3;
				} else if (sym == 17) {
					runVal = 0;
					runLen = readInt(input, 3) + 3;
				} else if (sym == 18) {
					runVal = 0;
					runLen = readInt(input, 7) + 11;
				} else
					throw new AssertionError("Symbol out of range");
			}
		}
		if (runLen > 0)
			throw new IllegalStateException("Run exceeds number of codes");

		// Create literal-length code tree
		int[] litLenCodeLen = Arrays.copyOf(codeLens, numLitLenCodes);
		CodeTree litLenCode;
		try {
			litLenCode = new CodeTree(litLenCodeLen);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e.getMessage());
		}

		// Create distance code tree with some extra processing
		int[] distCodeLen = Arrays.copyOfRange(codeLens, numLitLenCodes, codeLens.length);
		CodeTree distCode;
		if (distCodeLen.length == 1 && distCodeLen[0] == 0)
			distCode = null; // Empty distance code; the block shall be all literal symbols
		else {
			// Get statistics for upcoming logic
			int oneCount = 0;
			int otherPositiveCount = 0;
			for (int x : distCodeLen) {
				if (x == 1)
					oneCount++;
				else if (x > 1)
					otherPositiveCount++;
			}

			// Handle the case where only one distance code is defined
			if (oneCount == 1 && otherPositiveCount == 0) {
				// Add a dummy invalid code to make the Huffman tree complete
				distCodeLen = Arrays.copyOf(distCodeLen, 32);
				distCodeLen[31] = 1;
			}
			try {
				distCode = new CodeTree(distCodeLen);
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException(e.getMessage());
			}
		}

		return new CodeTree[] { litLenCode, distCode };
	}

	/* Block decompression methods */

	// Handles and copies an uncompressed block from the input bit stream.
	private static void decompressUncompressedBlock(IBitInputStream input, CircularDictionary dictionary, OutputStream output) throws IOException {
		// Discard bits to align to byte boundary
		while (input.getBitPosition() != 0) {
			input.readBitIfNotEof();
		}

		// Read length
		int len = readInt(input, 16);
		int nlen = readInt(input, 16);
		if ((len ^ 0xFFFF) != nlen) {
			throw new IllegalStateException("Invalid length in uncompressed block");
		}

		// Copy bytes
		for (int i = 0; i < len; i++) {
			int temp = input.readByte();
			if (temp == -1) {
				throw new EOFException();
			}
			output.write(temp);
			dictionary.append(temp);
		}
	}

	// Decompresses a Huffman-coded block from the input bit stream based on the given Huffman codes.
	private static void decompressHuffmanBlock(IBitInputStream input, CircularDictionary dictionary, OutputStream output, CodeTree litLenCode, CodeTree distCode) throws IOException {
		// distCode is allowed to be null

		while (true) {
			int sym = decodeSymbol(input, litLenCode);

			if (sym == 256) { // End of block
				break;
			}
			if (sym < 256) { // Literal byte
				output.write(sym);
				dictionary.append(sym);
			} else { // Length and distance for copying
				int run = decodeRunLength(input, sym);
				if (run < 3 || run > 258) {
					throw new AssertionError("Invalid run length");
				}
				if (distCode == null) {
					throw new IllegalStateException("Length symbol encountered with empty distance code");
				}
				int distSym = decodeSymbol(input, distCode);
				int dist = decodeDistance(input, distSym);
				if (dist < 1 || dist > 32768) {
					throw new AssertionError();
				}
				dictionary.copy(dist, run, output);
			}
		}
	}

	/* Symbol decoding methods */

	// Decodes the next symbol from the bit input stream based on
	// the given code tree. The returned symbol value is at least 0.
	private static int decodeSymbol(IBitInputStream input, CodeTree code) throws IOException {
		InternalNode currentNode = code.root;
		while (true) {
			int temp = input.readBitIfNotEof();
			Node nextNode;
			if (temp == 0) {
				nextNode = currentNode.leftChild;
			} else if (temp == 1) {
				nextNode = currentNode.rightChild;
			} else {
				throw new AssertionError();
			}

			if (nextNode instanceof Leaf) {
				return ((Leaf) nextNode).symbol;
			} else if (nextNode instanceof InternalNode) {
				currentNode = (InternalNode) nextNode;
			} else {
				throw new AssertionError();
			}
		}
	}

	// Returns the run length based on the given symbol and possibly reading more bits.
	private static int decodeRunLength(IBitInputStream input, int sym) throws IOException {
		int runLength = 0;
		if (sym < 257 || sym > 287) { // Cannot occur in the bit stream; indicates the decompressor is buggy
			throw new AssertionError();
		} else if (sym <= 264) {
			runLength = sym - 254;
		} else if (sym <= 284) {
			int numExtraBits = (sym - 261) / 4;
			runLength = (((sym - 265) % 4 + 4) << numExtraBits) + 3 + readInt(input, numExtraBits);
		} else if (sym == 285) {
			runLength = 258;
		} else {
			// sym is 286 or 287
			throw new IllegalStateException("Reserved length symbol: " + sym);
		}
		return runLength;
	}

	// Returns the distance based on the given symbol and possibly reading more bits.
	private static int decodeDistance(IBitInputStream input, int sym) throws IOException {
		int distance = 0;
		if (sym < 0 || sym > 31) { // Cannot occur in the bit stream; indicates the decompressor is buggy
			throw new AssertionError();
		}
		if (sym <= 3) {
			distance = sym + 1;
		} else if (sym <= 29) {
			int numExtraBits = sym / 2 - 1;
			distance = ((sym % 2 + 2) << numExtraBits) + 1 + readInt(input, numExtraBits);
		} else {
			// sym is 30 or 31
			throw new IllegalStateException("Reserved distance symbol: " + sym);
		}
		return distance;
	}

	/* Utility method */

	// Reads the given number of bits from the bit input stream as a single integer, packed in little endian.
	private static int readInt(IBitInputStream input, int numBits) throws IOException {
		int result = 0;
		for (int i = 0; i < numBits; i++) {
			result |= input.readBitIfNotEof() << i;
		}
		return result;
	}

}
