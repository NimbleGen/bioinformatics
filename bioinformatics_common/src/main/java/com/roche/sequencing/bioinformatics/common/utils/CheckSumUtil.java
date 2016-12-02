package com.roche.sequencing.bioinformatics.common.utils;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class CheckSumUtil {

	private static final long PRIME = 31;

	private CheckSumUtil() {
		throw new AssertionError();
	}

	private static long checkSum(List<String> stringList) {
		long result = 1;
		for (String string : stringList) {
			result = (result * PRIME) + checkSum(string);
		}
		return result;
	}

	public static long checkSum(Map<String, List<String>> stringMap) {
		long result = 1;
		for (Entry<String, List<String>> entry : stringMap.entrySet()) {
			result = (result * PRIME) + checkSum(entry.getKey());
			result = (result * PRIME) + checkSum(entry.getValue());
		}
		return result;
	}

	public static long checkSum(String[] stringArray) {
		long result = 1;
		for (String string : stringArray) {
			result = (result * PRIME) + checkSum(string);
		}
		return result;
	}

	public static long checkSum(String string) {
		return (long) string.hashCode();
	}

	public static int crc16(final byte[] bytes) {
		int crc = 0xFFFF;

		for (int j = 0; j < bytes.length; j++) {
			crc = ((crc >>> 8) | (crc << 8)) & 0xffff;
			crc ^= (bytes[j] & 0xff);// byte to int, trunc sign
			crc ^= ((crc & 0xff) >> 4);
			crc ^= (crc << 12) & 0xffff;
			crc ^= ((crc & 0xFF) << 5) & 0xffff;
		}
		crc &= 0xffff;
		return crc;

	}

	public static long crc32(final byte[] bytes) {
		Checksum checksum = new CRC32();
		checksum.update(bytes, 0, bytes.length);
		long checksumValue = checksum.getValue();
		return checksumValue;
	}

}
