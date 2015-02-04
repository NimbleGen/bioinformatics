/**
 *   Copyright 2013 Roche NimbleGen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.roche.sequencing.bioinformatics.common.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

/**
 * 
 * Util for working with files
 * 
 */
public final class FileUtil {

	public static final int BYTES_PER_KB = 1024;
	private static final int STRING_BUILDER_INITIAL_SIZE = 1000;

	private FileUtil() {
		throw new AssertionError();
	}

	/**
	 * @return a temporary directory specific to the flavor of the Operating System
	 */
	public static File getSystemSpecificTempDirectory() {
		String tempDirectory = System.getProperty("java.io.tmpdir");

		return new File(tempDirectory);
	}

	/**
	 * Returns the extension of the file. Returns an empty string if no extension is found.
	 * 
	 * @param file
	 * @return extension
	 */
	public static String getFileExtension(File file) {
		return getFileExtension(file.getName());
	}

	/**
	 * Returns the extension of the file name. Returns an empty string if no extension is found.
	 * 
	 * @param fileName
	 * @return extension
	 */
	public static String getFileExtension(String fileName) {
		String extension = "";
		int index = fileName.lastIndexOf('.');

		if ((index >= 0) && (index < fileName.length())) {
			extension = fileName.substring(index + 1, fileName.length());
		}

		return extension;
	}

	/**
	 * Returns file name without extension
	 * 
	 * @return shortened filename (no extension after the dot)
	 */
	public static String getFileNameWithoutExtension(String fileName) {
		String fileNameWithoutExtension = fileName;
		int index = fileName.lastIndexOf('.');

		if ((index >= 0) && (index < fileName.length())) {
			fileNameWithoutExtension = fileName.substring(0, index);
		}

		return fileNameWithoutExtension;
	}

	/**
	 * Simple utility to read the entire contents of a file into a string This code was taken from http://snippets.dzone.com/posts/show/1335
	 * 
	 */
	public static String readFileAsString(File file) throws java.io.IOException {
		StringBuilder fileData = new StringBuilder(STRING_BUILDER_INITIAL_SIZE);

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			char[] buf = new char[BYTES_PER_KB];
			int numRead = 0;

			while ((numRead = reader.read(buf)) != -1) {
				fileData.append(buf, 0, numRead);
			}

			return fileData.toString();
		}

	}

	/**
	 * Simple utility to read the entire contents of a file into a string This code was taken from http://snippets.dzone.com/posts/show/1335
	 * 
	 */
	public static String readStreamAsString(InputStream stream) throws java.io.IOException {
		StringBuilder fileData = new StringBuilder(STRING_BUILDER_INITIAL_SIZE);

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
			char[] buf = new char[BYTES_PER_KB];
			int numRead = 0;

			while ((numRead = reader.read(buf)) != -1) {
				fileData.append(buf, 0, numRead);
			}

			return fileData.toString();
		}

	}

	/**
	 * read the first line of a file as a string
	 * 
	 * @throws FileNotFoundException
	 */
	public static String readFirstLineAsString(File file) throws FileNotFoundException {
		String firstLine = null;
		try (Scanner scanner = new Scanner(file)) {
			firstLine = scanner.nextLine();
		}
		return firstLine;
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

	public static String convertToRelativePath(File fromFile, File toFile) {
		StringBuilder relativePathStringBuilder = null;

		String absolutePath = fromFile.getAbsolutePath();
		String relativeTo = toFile.getAbsolutePath();

		if (fromFile.isFile()) {
			absolutePath = fromFile.getParent();
		}

		absolutePath = absolutePath.replaceAll("\\\\", "/");
		relativeTo = relativeTo.replaceAll("\\\\", "/");

		if (!absolutePath.equals(relativeTo)) {

			String[] absoluteDirectories = absolutePath.split("/");
			String[] relativeDirectories = relativeTo.split("/");

			// Get the shortest of the two paths
			int length = absoluteDirectories.length < relativeDirectories.length ? absoluteDirectories.length : relativeDirectories.length;

			// Use to determine where in the loop we exited
			int lastCommonRoot = -1;
			int index;

			// Find common root
			indexLoop: for (index = 0; index < length; index++) {
				if (absoluteDirectories[index].equals(relativeDirectories[index])) {
					lastCommonRoot = index;
				} else {
					break indexLoop;

				}
			}
			if (lastCommonRoot != -1) {
				// Build up the relative path
				relativePathStringBuilder = new StringBuilder();
				// Add on the ..
				for (index = lastCommonRoot + 1; index < absoluteDirectories.length; index++) {
					if (absoluteDirectories[index].length() > 0) {
						relativePathStringBuilder.append("../");
					}
				}
				for (index = lastCommonRoot + 1; index < relativeDirectories.length - 1; index++) {
					relativePathStringBuilder.append(relativeDirectories[index] + "/");
				}
				relativePathStringBuilder.append(relativeDirectories[relativeDirectories.length - 1]);
			}
		}
		String returnRelativePath = null;
		if (relativePathStringBuilder != null) {
			returnRelativePath = relativePathStringBuilder.toString();
		} else {
			returnRelativePath = "./" + toFile.getAbsolutePath();
		}
		return returnRelativePath;
	}

	/**
	 * Create a directory and any parent directories that do not exist.
	 * 
	 * @param newDirectory
	 * @throws IOException
	 */
	public static void createDirectory(File newDirectory) throws IOException {
		FileUtils.forceMkdir(newDirectory);
	}

	public static int countNumberOfLinesInFile(File file) throws IOException {
		int count = 0;
		boolean empty = true;
		InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
		try {
			byte[] character = new byte[1024];
			int readChars = 0;
			while ((readChars = inputStream.read(character)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					if (character[i] == StringUtil.NEWLINE_SYMBOL) {
						++count;
					}
				}
			}

		} finally {
			inputStream.close();
		}
		return (count == 0 && !empty) ? 1 : count;
	}

	/**
	 * Recursively delete a directory. If the first attempt fails due to an IOException, garbage collect, sleep, and try one more time to get around an issue with NFS volumes.
	 * 
	 * @param directory
	 *            directory to delete
	 * @throws IOException
	 *             in case deletion is unsuccessful
	 */
	public static void deleteDirectory(File directory) throws IOException {
		try {
			// Attempt to recursively delete directory
			FileUtils.deleteDirectory(directory);
		} catch (IOException e) {
			// Directories mounted on NFS volumes may have lingering .nfsXXXX files
			// if no streams are open, it is likely from stale objects
			System.gc();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
			}
			FileUtils.deleteDirectory(directory);
		}
	}

	public static void writeStringToFile(File file, String stringToWrite) throws IOException {
		createNewFile(file);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(stringToWrite);
		}
	}

	/**
	 * taken from here http://codereview.stackexchange.com/questions/47923/simplifying-a-path -Kurt Heilman
	 * 
	 * @param path
	 * @return
	 */
	public static String simplifyPath(String path) {
		String simplifiedPath = null;
		Deque<String> pathDeterminer = new ArrayDeque<String>();
		path = path.replaceAll(Pattern.quote("\\"), "/");
		path = path.replaceAll(Pattern.quote("\\\\"), "/");
		String[] pathSplitter = path.split("/");
		StringBuilder absolutePath = new StringBuilder();
		for (String term : pathSplitter) {
			if (term == null || term.length() == 0 || term.equals(".")) {
				/* ignore these guys */
			} else if (term.equals("..")) {
				if (pathDeterminer.size() > 0) {
					pathDeterminer.removeLast();
				}
			} else {
				pathDeterminer.addLast(term);
			}
		}
		if (pathDeterminer.isEmpty()) {
			simplifiedPath = "/";
		} else {
			while (!pathDeterminer.isEmpty()) {
				absolutePath.insert(0, pathDeterminer.removeLast());
				absolutePath.insert(0, "/");
			}
			simplifiedPath = absolutePath.toString();
		}
		return simplifiedPath;
	}

	public static void main(String[] args) {
		File fileA = new File("D://kurts_space/jigar_dirt/2uM_Images/results/210097_bound_635.aln");
		File fileB = new File("D://kurts_space/jigar_dirt/2uM_Images/210097_bound_635.tif");
		String relativePath = FileUtil.convertToRelativePath(fileA, fileB);
		System.out.println(relativePath);
	}
}
