package com.roche.sequencing.bioinformatics.common.utils;

public class JVMUtil {

	private JVMUtil() {
		throw new AssertionError();
	}

	public static int getJvmBitsAsInt() {
		String jvmBitsAsString = getJvmBits();
		int jvmBitAsInt = Integer.valueOf(jvmBitsAsString);
		return jvmBitAsInt;
	}

	public static String getJvmBits() {
		String jvmBitsAsString = System.getProperty("sun.arch.data.model");
		return jvmBitsAsString;
	}

	public static String getJavaVersion() {
		String versionAsString = System.getProperty("java.specification.version");
		return versionAsString;
	}

	public static double getJavaVersionAsDouble() {
		String versionAsString = getJavaVersion();
		double versionAsDouble = Double.valueOf(versionAsString);
		return versionAsDouble;
	}

}
