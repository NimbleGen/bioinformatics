package com.roche.sequencing.bioinformatics.common.utils;

import java.math.BigInteger;
import java.nio.ByteOrder;

public class ByteUtil {

	private final static int BITS_PER_BYTE = 8;

	private final static int BYTES_IN_A_LONG = 8;
	private final static int BYTES_IN_AN_INT = 4;
	private final static int BYTES_IN_A_SHORT = 2;

	private final static long[] UNSIGNED_MAX_VALUES_BY_NUMBER_OF_BYTES_AS_INDEX = new long[] { 0, 255, 65535, 16777215, 4294967295L, 1099511627775L, 281474976710655L, 72057594037927935L };
	private final static long[] SIGNED_MAX_VALUES_BY_NUMBER_OF_BYTES_AS_INDEX = new long[] { 0, 127, 32767, 8388607, 2147483647, 549755813887L, 140737488355327L, 36028797018963967L,
			9223372036854775807L };

	private ByteUtil() {
		throw new AssertionError();
	}

	/**
	 * Convert the provided longValue to a byte array of size 8.
	 * 
	 * @param longValue
	 * @param byteOrder
	 * @return
	 */
	private static byte[] longToBytes(long longValue, ByteOrder byteOrder, boolean isSigned) {
		byte[] result = new byte[BYTES_IN_A_LONG];

		if (longValue < 0 && !isSigned) {
			throw new IllegalStateException("Cannot represent the negative value[" + longValue + "] in an unsigned byte array.");
		}

		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			for (int i = BYTES_IN_A_LONG - 1; i >= 0; i--) {
				result[i] = (byte) (longValue & 0xFF);
				longValue >>= BITS_PER_BYTE;
			}
		} else if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
			for (int i = 0; i < BYTES_IN_A_LONG; i++) {
				result[i] = (byte) (longValue & 0xFF);
				longValue >>= BITS_PER_BYTE;
			}
		} else {
			throw new IllegalStateException("Unrecognized ByteOrder value[" + byteOrder + "].");
		}
		return result;
	}

	private static short bytesToShort(byte[] bytes, ByteOrder byteOrder, boolean isSigned) {
		short value = bytesToNumber(bytes, byteOrder, isSigned, BYTES_IN_A_SHORT, "short").shortValue();
		return value;
	}

	private static int bytesToInt(byte[] bytes, ByteOrder byteOrder, boolean isSigned) {
		int value = bytesToNumber(bytes, byteOrder, isSigned, BYTES_IN_AN_INT, "int").intValue();
		return value;
	}

	private static long bytesToLong(byte[] bytes, ByteOrder byteOrder, boolean isSigned) {
		long value = bytesToNumber(bytes, byteOrder, isSigned, BYTES_IN_A_LONG, "long").longValue();
		return value;
	}

	private static Number bytesToNumber(byte[] bytes, ByteOrder byteOrder, boolean isSigned, int bytesInDataType, String dataTypeName) {
		long result = 0;

		if (bytes.length != bytesInDataType) {
			throw new IllegalArgumentException("The provided byte array has a length[" + bytes.length + "] other than the required length of [" + bytesInDataType + "].");
		}

		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			for (int i = 0; i < bytesInDataType; i++) {
				result <<= BITS_PER_BYTE;
				result |= (bytes[i] & 0xFF);
			}
		} else if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
			for (int i = bytesInDataType - 1; i >= 0; i--) {
				result <<= BITS_PER_BYTE;
				result |= (bytes[i] & 0xFF);
			}
		} else {
			throw new IllegalStateException("Unrecognized ByteOrder value[" + byteOrder + "].");
		}

		BigInteger resultAsBigInteger = BigInteger.valueOf(result);
		BigInteger maxSignedValueForDataType = getMaxPossibleValue(bytesInDataType, true);
		if (isSigned) {
			boolean resultIsBiggerThanMaxPossibleSignedValue = resultAsBigInteger.compareTo(maxSignedValueForDataType) > 0;
			if (resultIsBiggerThanMaxPossibleSignedValue) {
				throw new IllegalStateException("Unable to return the decoded value of [" + result + "] as a signed " + dataTypeName + " which has a max value of [" + maxSignedValueForDataType + "].");
			}
		} else {
			if (result < 0) {
				// Note: UNSIGNED_VALUE - SIGNED_VALUE = 2^NUMBER_OF_BITS
				// So to find which signed BigInteger value is equivalent to
				// the unsigned BigInteger value we want we use the following:
				// SIGNED_VALUE = UNSIGNED_VALUE - 2^NUMBER_OF_BITS
				BigInteger signedEquivalent = resultAsBigInteger.subtract(BigInteger.valueOf(2).pow(BITS_PER_BYTE * bytesInDataType));
				throw new IllegalStateException("Unable to return the decoded value of [" + signedEquivalent + "] as a signed " + dataTypeName + " which has a max value of ["
						+ maxSignedValueForDataType + "].");
			}
		}

		return result;
	}

	/**
	 * Return a byte[] representation of the short based on the passed in parameters.
	 * 
	 * @param value
	 * @param numberOfBytes
	 * @param byteOrder
	 * @param isSigned
	 * @return a byte[] representation of the short based on the passed in parameters.
	 */
	public static byte[] convertShortToBytes(short value, int numberOfBytes, ByteOrder byteOrder, boolean isSigned) {
		return convertLongToBytes((long) value, numberOfBytes, byteOrder, isSigned);
	}

	/**
	 * Return a byte[] representation of the int based on the passed in parameters.
	 * 
	 * @param value
	 * @param numberOfBytes
	 * @param byteOrder
	 * @param isSigned
	 * @return a byte[] representation of the int based on the passed in parameters.
	 */
	public static byte[] convertIntToBytes(int value, int numberOfBytes, ByteOrder byteOrder, boolean isSigned) {
		return convertLongToBytes((long) value, numberOfBytes, byteOrder, isSigned);
	}

	/**
	 * Return a byte[] representation of the long based on the passed in parameters.
	 * 
	 * @param value
	 * @param numberOfBytes
	 * @param byteOrder
	 * @param isSigned
	 * @return a byte[] representation of the long based on the passed in parameters.
	 */
	public static byte[] convertLongToBytes(long value, int numberOfBytes, ByteOrder byteOrder, boolean isSigned) {
		byte[] results = null;

		results = longToBytes(value, byteOrder, isSigned);

		if (results.length > numberOfBytes) {
			results = reduceToSmallestByteArray(results, byteOrder, isSigned);
		}

		if (results.length < numberOfBytes) {
			results = increaseNumberOfBytes(results, numberOfBytes, byteOrder, isSigned);
		}

		if (results.length != numberOfBytes) {
			String signedText = "an unsigned";
			if (isSigned) {
				signedText = "a signed";
			}

			BigInteger maxValue = getMaxPossibleValue(numberOfBytes, isSigned);

			throw new NumberOverflowException("The value[" + value + "] cannot be represented as " + signedText + " value which utilizes [" + numberOfBytes + "] bytes and has a max value of ["
					+ maxValue + "].  At least [" + results.length + "] bytes are required to store this value.");
		}

		return results;
	}

	/**
	 * Return a byte[] representation of the BigInteger based on the passed in parameters.
	 * 
	 * @param value
	 * @param numberOfBytes
	 * @param byteOrder
	 * @param isSigned
	 * @return a byte[] representation of the BigInteger based on the passed in parameters.
	 */
	public static byte[] convertBigIntegerToBytes(BigInteger value, int numberOfBytes, ByteOrder byteOrder, boolean isSigned) {
		boolean valueIsLessThanZero = value.compareTo(BigInteger.ZERO) < 0;
		if (valueIsLessThanZero && !isSigned) {
			throw new IllegalStateException("Cannot represent the negative value[" + value + "] in an unsigned byte array.");
		}

		byte[] convertedBytes = null;

		if (!isSigned) {
			BigInteger maxValueOfSigned = getMaxPossibleValue(numberOfBytes, true);
			boolean valueIsGreaterThanMaxSignedValue = value.compareTo(maxValueOfSigned) > 0;
			if (valueIsGreaterThanMaxSignedValue) {
				// Note: UNSIGNED_VALUE - SIGNED_VALUE = 2^NUMBER_OF_BITS
				// So to find which signed BigInteger value is equivalent to
				// the unsigned BigInteger value we want we use the following:
				// SIGNED_VALUE = UNSIGNED_VALUE - 2^NUMBER_OF_BITS
				BigInteger signedEquivalent = value.subtract(BigInteger.valueOf(2).pow(BITS_PER_BYTE * numberOfBytes));
				convertedBytes = signedEquivalent.toByteArray();
			}
		}

		if (convertedBytes == null) {
			convertedBytes = value.toByteArray();
		}

		if (convertedBytes.length < numberOfBytes) {
			convertedBytes = reduceToSmallestByteArray(convertedBytes, byteOrder, isSigned);
		}

		if (convertedBytes.length < numberOfBytes) {
			convertedBytes = increaseNumberOfBytes(convertedBytes, numberOfBytes, byteOrder, isSigned);
		} else {
			String signedText = "unsigned";
			if (isSigned) {
				signedText = "signed";
			}
			throw new IllegalStateException("The value[" + value + "] cannot be represented as a " + signedText + " value which utilizes [" + numberOfBytes + "].  At least [" + convertedBytes.length
					+ "] bytes are required to store this value.");
		}

		return convertedBytes;
	}

	/**
	 * Reduces the number of bytes to represent the provided number without changing its value.
	 * 
	 * @param originalBytes
	 * @param byteOrder
	 * @param isSigned
	 * @return a byte array which uses the least number of bytes to store the value.
	 */
	public static byte[] reduceToSmallestByteArray(byte[] originalBytes, ByteOrder byteOrder, boolean isSigned) {

		byte[] smallestBytes = null;

		byte emptyByte = (byte) 0;// 00000000
		if (isNegative(originalBytes, byteOrder, isSigned)) {
			emptyByte = (byte) -1;// 11111111
		}

		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			int byteIndexOfFirstValuedByte = 0;
			byteLoop: for (int i = 0; i < originalBytes.length; i++) {
				if (originalBytes[i] != emptyByte) {
					byteIndexOfFirstValuedByte = i;
					break byteLoop;
				}
			}

			if (byteIndexOfFirstValuedByte > 0) {
				int newByteSize = originalBytes.length - byteIndexOfFirstValuedByte;
				smallestBytes = new byte[newByteSize];
				for (int i = 0; i < newByteSize; i++) {
					int originalIndex = i + byteIndexOfFirstValuedByte;
					smallestBytes[i] = originalBytes[originalIndex];
				}
			} else {
				smallestBytes = originalBytes;
			}
		} else if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
			int byteIndexOfFirstValuedByte = 0;
			byteLoop: for (int i = originalBytes.length - 1; i >= 0; i--) {
				if (originalBytes[i] != emptyByte) {
					byteIndexOfFirstValuedByte = i;
					break byteLoop;
				}
			}

			if (byteIndexOfFirstValuedByte < originalBytes.length) {
				int newByteSize = byteIndexOfFirstValuedByte + 1;
				smallestBytes = new byte[newByteSize];
				for (int i = 0; i < newByteSize; i++) {
					smallestBytes[i] = originalBytes[i];
				}
			} else {
				smallestBytes = originalBytes;
			}
		} else {
			throw new IllegalStateException("Unrecognized ByteOrder value[" + byteOrder + "].");
		}

		return smallestBytes;
	}

	/**
	 * Increase the number of bytes used to store this number without affecting its value.
	 * 
	 * @param originalBytes
	 * @param desiredNumberOfBytes
	 * @param byteOrder
	 * @param isSigned
	 * @return
	 */
	public static byte[] increaseNumberOfBytes(byte[] originalBytes, int desiredNumberOfBytes, ByteOrder byteOrder, boolean isSigned) {
		if (originalBytes.length >= desiredNumberOfBytes) {
			throw new IllegalArgumentException("Cannot increase the number of bytes when the size of the original byte array[" + originalBytes.length
					+ "] is greater than or equal to the desired number of bytes[" + desiredNumberOfBytes + "].");
		}

		int numberOfBytesToInsert = desiredNumberOfBytes - originalBytes.length;

		byte[] newBytes = new byte[desiredNumberOfBytes];

		byte byteToAdd = (byte) 0;// 00000000
		if (isNegative(originalBytes, byteOrder, isSigned)) {
			byteToAdd = (byte) -1;// 11111111
		}

		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			// add the new bytes at the beginning
			for (int i = 0; i < numberOfBytesToInsert; i++) {
				newBytes[i] = byteToAdd;
			}
			for (int j = 0; j < originalBytes.length; j++) {
				newBytes[numberOfBytesToInsert + j] = originalBytes[j];
			}
		} else if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
			for (int j = 0; j < originalBytes.length; j++) {
				newBytes[j] = originalBytes[j];
			}
			for (int i = 0; i < numberOfBytesToInsert; i++) {
				newBytes[i + originalBytes.length] = byteToAdd;
			}
		} else {
			throw new IllegalStateException("Unrecognized ByteOrder value[" + byteOrder + "].");
		}

		return newBytes;
	}

	/**
	 * Detect whether this a negative value.
	 * 
	 * @param bytes
	 * @param byteOrder
	 * @param isSigned
	 * @return true if the provided details indicate a negative valued number
	 */
	public static boolean isNegative(byte[] bytes, ByteOrder byteOrder, boolean isSigned) {
		boolean isNegative = false;

		if (isSigned) {
			byte mostSignificantByte = getMostSignificantByte(bytes, byteOrder);
			isNegative = isBitOn(mostSignificantByte, BITS_PER_BYTE - 1);
		}

		return isNegative;
	}

	/**
	 * Return the most significant byte in the provided byte array based on endianess.
	 * 
	 * @param bytes
	 * @param byteOrder
	 * @return the most significant byte based on endianess.
	 */
	public static byte getMostSignificantByte(byte[] bytes, ByteOrder byteOrder) {
		byte mostSignificantByte = bytes[0];
		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			// do nothing
			// mostSignificantByte = bytes[0];
		} else if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
			mostSignificantByte = bytes[bytes.length - 1];
		} else {
			throw new IllegalStateException("Unrecognized ByteOrder value[" + byteOrder + "].");
		}
		return mostSignificantByte;
	}

	/**
	 * Return true is the bit found at the bitIndex position is on (meaning it has a value of 1).
	 * 
	 * @param aByte
	 * @param bitIndex
	 * @return true if the bitIndex position is occupied by a 1.
	 */
	public static boolean isBitOn(byte aByte, int bitIndex) {
		boolean isBitOn = getBitValue(aByte, bitIndex) == 1;
		return isBitOn;
	}

	/**
	 * Return the bit value(0 or 1) for the bit found at the bitIndex position in the provided byte.
	 * 
	 * @param aByte
	 * @param bitIndex
	 * @return 0 or 1 based on the bit value at the bitIndex position in the provided byte.
	 */
	public static int getBitValue(byte aByte, int bitIndex) {
		if (bitIndex >= BITS_PER_BYTE) {
			throw new ArrayIndexOutOfBoundsException("The provided bitIndex[" + bitIndex + "] is larger than the size of a byte[" + BITS_PER_BYTE + "].");
		}

		if (bitIndex < 0) {
			throw new ArrayIndexOutOfBoundsException("The provided bitIndex[" + bitIndex + "] must be greater than or equal to zero.");
		}

		int value = (aByte >> bitIndex) & 1;
		return value;
	}

	/**
	 * Sets the bit value of the provided byte at the provided bitIndex (0 <= bitIndex <=7) to the provided bitValue(0 or 1).
	 * 
	 * @param aByte
	 *            the byte in which the bit should be set
	 * @param bitIndex
	 *            an index between 0 and 7 (inclusive)
	 * @param bitValue
	 *            is a value of either 0 or 1.
	 * @return the original byte with the bit at the bitIndex position set to bitValue.
	 */
	public static int setBitValue(byte aByte, int bitIndex, int bitValue) {
		if (bitIndex >= BITS_PER_BYTE) {
			throw new ArrayIndexOutOfBoundsException("The provided bitIndex[" + bitIndex + "] is larger than the size of a byte[" + BITS_PER_BYTE + "].");
		}

		if (bitIndex < 0) {
			throw new ArrayIndexOutOfBoundsException("The provided bitIndex[" + bitIndex + "] must be greater than or equal to zero.");
		}

		byte newByte = (byte) 0;
		if (bitValue == 0) {
			newByte = (byte) (aByte & ~(1 << bitIndex));
		} else if (bitValue == 1) {
			newByte = (byte) (aByte | (1 << bitIndex));
		} else {
			throw new IllegalArgumentException("The provided bitValue[" + bitValue + "] must be either 0 or 1.");
		}

		return newByte;
	}

	/**
	 * Converts the provided bytes into a short based on the passed in parameters. A NumberOverflowException will be thrown if the isSigned parameter is false and the provided bytes cannot fit into a
	 * short. In cases where this is expected the exception can be caught and the convertBytesToInt method can be called subsequently.
	 * 
	 * @param bytes
	 * @param byteOrder
	 * @param isSigned
	 * @return int value of the bytes
	 * 
	 */
	public static short convertBytesToShort(byte[] bytes, ByteOrder byteOrder, boolean isSigned) {
		if (bytes.length > BYTES_IN_A_SHORT) {
			bytes = reduceToSmallestByteArray(bytes, byteOrder, isSigned);
		}

		if (bytes.length < BYTES_IN_A_SHORT) {
			bytes = increaseNumberOfBytes(bytes, BYTES_IN_A_SHORT, byteOrder, isSigned);
		}

		if (bytes.length != BYTES_IN_A_SHORT) {
			throw new NumberOverflowException("The number of bytes required [" + bytes.length
					+ "] to sufficiently store the value provided by the passed in arguments is too large to fit in a short datatype.");
		}

		short value = bytesToShort(bytes, byteOrder, isSigned);
		return value;
	}

	/**
	 * Converts the provided bytes into an int based on the passed in parameters. A NumberOverflowException will be thrown if the isSigned parameter is false and the provided bytes cannot fit into an
	 * int. In cases where this is expected the exception can be caught and the convertBytesToLong method can be called subsequently.
	 * 
	 * @param bytes
	 * @param byteOrder
	 * @param isSigned
	 * @return int value of the bytes
	 * 
	 */
	public static int convertBytesToInt(byte[] bytes, ByteOrder byteOrder, boolean isSigned) {
		if (bytes.length > BYTES_IN_AN_INT) {
			bytes = reduceToSmallestByteArray(bytes, byteOrder, isSigned);
		}

		if (bytes.length < BYTES_IN_AN_INT) {
			bytes = increaseNumberOfBytes(bytes, BYTES_IN_AN_INT, byteOrder, isSigned);
		}

		if (bytes.length != BYTES_IN_AN_INT) {
			throw new NumberOverflowException("The number of bytes required [" + bytes.length
					+ "] to sufficiently store the value provided by the passed in arguments is too large to fit in an int datatype.");
		}

		int value = bytesToInt(bytes, byteOrder, isSigned);
		return value;
	}

	/**
	 * Converts the provided bytes into a long based on the passed in parameters. A NumberOverflowException will be thrown if the isSigned parameter is false and the provided bytes cannot fit into a
	 * long. In cases where this is expected the exception can be caught and the convertBytesToBigInteger method can be called subsequently.
	 * 
	 * @param bytes
	 * @param byteOrder
	 * @param isSigned
	 * @return long value of the bytes
	 * 
	 */
	public static long convertBytesToLong(byte[] bytes, ByteOrder byteOrder, boolean isSigned) {
		if (bytes.length > BYTES_IN_A_LONG) {
			bytes = reduceToSmallestByteArray(bytes, byteOrder, isSigned);
		}

		if (bytes.length < BYTES_IN_A_LONG) {
			bytes = increaseNumberOfBytes(bytes, BYTES_IN_A_LONG, byteOrder, isSigned);
		}

		if (bytes.length != BYTES_IN_A_LONG) {
			throw new NumberOverflowException("The number of bytes required [" + bytes.length
					+ "] to sufficiently store the value provided by the passed in arguments is too large to fit in a long datatype.");
		}

		long value = bytesToLong(bytes, byteOrder, isSigned);
		return value;
	}

	public static BigInteger convertBytesToBigInteger(byte[] bytes, ByteOrder byteOrder, boolean isSigned) {

		if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
			bytes = toggleEndianess(bytes);
		}

		BigInteger value = new BigInteger(bytes);

		int numberOfBytes = bytes.length;

		boolean valueIsLessThanZero = value.compareTo(BigInteger.ZERO) < 0;
		if (!isSigned && valueIsLessThanZero) {
			// Note: UNSIGNED_VALUE - SIGNED_VALUE = 2^NUMBER_OF_BITS
			// So to find which signed BigInteger value is equivalent to
			// the unsigned BigInteger value we want we use the following:
			// UNSIGNED_VALUE = SIGNED_VALUE + 2^NUMBER_OF_BITS
			BigInteger unsignedEquivalent = value.add(BigInteger.valueOf(2).pow(BITS_PER_BYTE * numberOfBytes));
			value = unsignedEquivalent;
		}
		return value;
	}

	public static byte[] toggleEndianess(byte[] bytes) {
		byte[] toggledBytes = new byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			toggledBytes[i] = bytes[bytes.length - i - 1];
		}
		return toggledBytes;
	}

	/**
	 * Return the max possible value for a byte array containing the indicated number of bytes
	 * 
	 * @param numberOfBytes
	 * @param isSigned
	 *            (Are these bytes two's compiment?)
	 * @return the largest possible value that can be represented with a byte array corresponding to the provided details.
	 */
	public static BigInteger getMaxPossibleValue(int numberOfBytes, boolean isSigned) {

		BigInteger maxPossibleValue = null;
		BigInteger two = BigInteger.valueOf(2);
		if (isSigned) {
			if (numberOfBytes > SIGNED_MAX_VALUES_BY_NUMBER_OF_BYTES_AS_INDEX.length) {
				maxPossibleValue = two.pow(numberOfBytes - 1).subtract(BigInteger.ONE);
			} else {
				maxPossibleValue = BigInteger.valueOf(SIGNED_MAX_VALUES_BY_NUMBER_OF_BYTES_AS_INDEX[numberOfBytes]);
			}
		} else {
			if (numberOfBytes > UNSIGNED_MAX_VALUES_BY_NUMBER_OF_BYTES_AS_INDEX.length) {
				maxPossibleValue = two.pow(numberOfBytes * BITS_PER_BYTE).subtract(BigInteger.ONE);
			} else {
				maxPossibleValue = BigInteger.valueOf(UNSIGNED_MAX_VALUES_BY_NUMBER_OF_BYTES_AS_INDEX[numberOfBytes]);
			}
		}

		return maxPossibleValue;
	}

	/**
	 * Return a binary string (ex. 11100010 00011111) of the provided bytes. Note: the output will contain a space to separate consecutive bytes.
	 * 
	 * @param bytes
	 * @return A string containing 1s and 0s with a space for byte delimitation.
	 */
	public static String convertBytesToBinaryString(byte[] bytes) {
		StringBuilder stringBuilder = new StringBuilder();
		for (byte aByte : bytes) {
			String string = String.format("%8s", Integer.toBinaryString(aByte & 0xFF)).replace(' ', '0');
			stringBuilder.append(string + " ");
		}
		return stringBuilder.substring(0, stringBuilder.length() - 1);
	}

	/**
	 * Return a byte[] representation of the provided binary String. Note: Spaces will be removed prior to conversion which means strings using spaces to delimitate bytes will be processed correctly.
	 * 
	 * @param binaryString
	 * @return A byte[] representation of the provided binary string.
	 */
	public static byte[] convertBinaryStringToBytes(String binaryString) {
		binaryString = binaryString.replaceAll(" ", "");
		int length = binaryString.length();

		if (length == 0) {
			throw new IllegalArgumentException("The provided binary string[" + binaryString + "] is empty.");
		}

		int oneCount = StringUtil.countMatches(binaryString, "1");
		int zeroCount = StringUtil.countMatches(binaryString, "0");
		int oneAndZerocount = oneCount + zeroCount;
		if (oneAndZerocount != length) {
			throw new IllegalArgumentException("The provided binary string[" + binaryString + "] contains values others than 1 and 0.");
		}

		if (length % BITS_PER_BYTE != 0) {
			throw new IllegalArgumentException("The length[" + length + "] of the provided binary string[" + binaryString + "] is not divisible by the number of bits in a byte[" + BITS_PER_BYTE
					+ "].");
		}

		int numberOfBytes = (int) Math.ceil((double) binaryString.length() / (double) BITS_PER_BYTE);
		byte[] convertedBytes = new byte[numberOfBytes];

		for (int byteIndex = 0; byteIndex < numberOfBytes; byteIndex++) {
			int start = byteIndex * BITS_PER_BYTE;
			String byteAsString = binaryString.substring(start, start + BITS_PER_BYTE);

			int byteAsInt = Integer.parseInt(byteAsString, 2);
			convertedBytes[byteIndex] = (byte) byteAsInt;
		}
		return convertedBytes;
	}

	/**
	 * Invert the provided byte array.
	 * 
	 * @param bytes
	 * @return a byte array with all 0s flipped to 1s and 1s flipped to zeroes
	 */
	public static byte[] invert(byte[] bytes) {
		byte[] invertedBytes = new byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			invertedBytes[i] = (byte) (~bytes[i]);
		}
		return invertedBytes;
	}

	public static void main(String[] args) {
		String shortString = "00001000 11111111 11111110";
		byte[] bytes = convertBinaryStringToBytes(shortString);
		int intValue = convertBytesToInt(bytes, ByteOrder.BIG_ENDIAN, false);
		long longValue = convertBytesToLong(bytes, ByteOrder.BIG_ENDIAN, false);
		System.out.println("int value:" + intValue);
		System.out.println("long value:" + longValue);
		System.out.println(shortString);
		System.out.println(convertBytesToBinaryString(convertLongToBytes(intValue, 3, ByteOrder.BIG_ENDIAN, false)));

		// byte[] bytes = convertBinaryStringToBytes("00000101 10000101");
		// System.out.println(convertBytesToBinaryString(bytes));
		// System.out.println(isNegative(bytes, ByteOrder.BIG_ENDIAN, true));
		// System.out.println(isNegative(bytes, ByteOrder.LITTLE_ENDIAN, true));

		// bytes = increaseNumberOfBytes(bytes, 3, ByteOrder.LITTLE_ENDIAN, true);
		// System.out.println(convertBytesToBinaryString(bytes));

	}

}
