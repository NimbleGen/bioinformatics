package com.roche.sequencing.bioinformatics.common.utils;

public class OSUtil {

	private OSUtil() {
		throw new AssertionError();
	}

	public static String getOsName() {
		return System.getProperty("os.name");
	}

	public static boolean isWindows() {
		return getOsName().startsWith("Windows");
	}

	public static boolean isLinux() {
		return getOsName().startsWith("Linux");
	}

}
