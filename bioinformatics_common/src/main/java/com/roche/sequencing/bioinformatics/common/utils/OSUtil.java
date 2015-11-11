/*
 *    Copyright 2013 Roche NimbleGen Inc.
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
		Boolean is64 = is64Bit();
		if (is64 == null) {
			osBits = "unknown";
		} else {
			if (is64) {
				osBits = "64";
			}
		}
		return osBits;
	}

}
