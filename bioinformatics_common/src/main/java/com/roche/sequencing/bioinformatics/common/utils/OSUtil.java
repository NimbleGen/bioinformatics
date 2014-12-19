package com.roche.sequencing.bioinformatics.common.utils;

import java.awt.Window;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSUtil {

	private static Logger logger = LoggerFactory.getLogger(OSUtil.class);

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

	public static boolean isMacOsX() {
		return getOsName().startsWith("Mac OS X");
	}

	/**
	 * @param window
	 */
	public static boolean setOSXFullscreen(Window window, boolean isFullScreen) {
		boolean success = false;
		if (window != null && isMacOsX()) {
			try {
				Class<?> util = Class.forName("com.apple.eawt.FullScreenUtilities");
				Class<?> params[] = new Class[] { Window.class, Boolean.TYPE };
				Method method = util.getMethod("setWindowCanFullScreen", params);
				method.invoke(util, window, isFullScreen);
				success = true;
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		return success;
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

	public static String getOsBits() {
		String osBits = "32";
		if (is64Bit()) {
			osBits = "64";
		}
		return osBits;
	}

}
