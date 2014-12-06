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

	/**
	 * @return null if cannot be determined
	 */
	public static Boolean is64Bit() {
		Boolean is64bit = null;
		if (isWindows()) {
			if (System.getProperty("os.name").contains("Windows")) {
				is64bit = (System.getenv("ProgramFiles(x86)") != null);
			} else {
				is64bit = (System.getProperty("os.arch").indexOf("64") != -1);
			}
		}
		return is64bit;
	}

	/**
	 * @return null if cannot be determined
	 */
	public static boolean is32Bit() {
		Boolean is32Bit = null;
		Boolean is64Bit = is64Bit();
		if (is64Bit != null) {
			is32Bit = !is64Bit;
		}
		return is32Bit;
	}

}
