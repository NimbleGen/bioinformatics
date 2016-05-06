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
package com.roche.sequencing.bioinformatics.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JVMUtil {

	private JVMUtil() {
		throw new AssertionError();
	}

	
	public static int getJvmBitsAsInt() {
		String jvmBitsAsString = getJvmBits();
		int jvmBitAsInt = Integer.valueOf(jvmBitsAsString);
		return jvmBitAsInt;
	}

	private static String getJvmBits() {
		String jvmBitsAsString = System.getProperty("sun.arch.data.model");
		return jvmBitsAsString;
	}

	private static String getJavaVersion() {
		String versionAsString = System.getProperty("java.specification.version");
		return versionAsString;
	}

	public static Version getJavaVersion(File pathToJvmBinDirectory) {
		Version version = null;
		String path = new File(pathToJvmBinDirectory, "java").getAbsolutePath();
		ProcessBuilder processBuilder = new ProcessBuilder(path, "-version");
		try {
			Process process = processBuilder.start();
			InputStream errorStream = process.getErrorStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
			String line;
			lineLoop: while ((line = reader.readLine()) != null) {
				if (line.contains("java version")) {
					Pattern pattern = Pattern.compile("\"(.*)\"");
					Matcher matcher = pattern.matcher(line);
					matcher.find();
					version = Version.fromString(matcher.group(1));
					break lineLoop;
				}
			}
		} catch (IOException e) {
		}

		return version;

	}

	
	public static double getJavaVersionAsDouble() {
		String versionAsString = getJavaVersion();
		double versionAsDouble = Double.valueOf(versionAsString);
		return versionAsDouble;
	}

	public static void main(String[] args) {
		System.out.println(getJavaVersion(new File("C:\\Program Files\\Java\\jre8\\bin")));
	}

}
