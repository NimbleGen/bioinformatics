package com.roche.heatseq.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TextReplacer {

	public static final int BYTES_PER_KB = 1024;
	private static final int STRING_BUILDER_INITIAL_SIZE = 1000;

	public static void main(String[] args) throws IOException {
		File path = new File(args[1]);
		replaceNames(path);
	}

	private static void replaceNames(File file) throws IOException {
		for (File child : file.listFiles()) {
			if (child.isDirectory()) {
				replaceNames(child);
			} else if (child.getName().endsWith(".check")) {
				String fileText = readFileAsString(file);
				fileText = fileText.replaceAll("HSQUtils", "HSQutils");
				writeStringToFile(file, fileText);
			}
		}
	}

	public static String readFileAsString(File file) throws java.io.IOException {
		StringBuilder fileData = new StringBuilder(STRING_BUILDER_INITIAL_SIZE);

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			char[] buf = new char[BYTES_PER_KB];
			int numRead = 0;

			while ((numRead = reader.read(buf)) != -1) {
				fileData.append(buf, 0, numRead);
			}
		}
		return fileData.toString();
	}

	public static void writeStringToFile(File file, String stringToWrite) throws IOException {
		createNewFile(file);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(stringToWrite);
		}
	}

	/**
	 * calls Files createNewFile but will also create new subdirectories if needed.
	 * 
	 * @param fileForStoring
	 * @throws IOException
	 */
	public static boolean createNewFile(File fileForStoring) throws IOException {
		boolean success = false;

		if (!fileForStoring.exists()) {
			File parentFile = fileForStoring.getParentFile();

			if (parentFile != null) {
				parentFile.mkdirs();
			}

			success = fileForStoring.createNewFile();
		}

		return success;
	}

}
