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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	 * @return a temporary directory specific to the flavor of the Operating
	 *         System
	 */
	public static File getSystemSpecificTempDirectory() {
		String tempDirectory = System.getProperty("java.io.tmpdir");

		return new File(tempDirectory);
	}

	/**
	 * Returns the extension of the file. Returns an empty string if no
	 * extension is found.
	 * 
	 * @param file
	 * @return extension
	 */
	public static String getFileExtension(File file) {
		return getFileExtension(file.getName());
	}

	/**
	 * Returns the extension of the file name. Returns an empty string if no
	 * extension is found.
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
	 * Simple utility to read the entire contents of a file into a string This
	 * code was taken from http://snippets.dzone.com/posts/show/1335
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
		}
		return fileData.toString();
	}

	/**
	 * 
	 * @param file
	 * @param textToFind
	 * @return the line number of the first occurence of this text, -1 if no
	 *         such line exists.
	 * @throws FileNotFoundException
	 */
	public static int findLineNumberOfFirstOccurrenceOfText(File file, String textToFind) throws FileNotFoundException {
		int lineNumber = -1;
		int[] lineNumbers = getLineNumbersContainingTextInFile(file, textToFind, 0, 1);
		if (lineNumbers.length > 0) {
			lineNumber = lineNumbers[0];
		}
		return lineNumber;
	}

	/**
	 * 
	 * @param file
	 * @param textToFind
	 * @param startingLineNumber
	 *            (0-based)
	 * @param numberOfMatchesToFind
	 * @return the line numbers of lines that contain the provided text
	 * @throws FileNotFoundException
	 */
	public static int[] getLineNumbersContainingTextInFile(File file, String textToFind, int startingLineNumber,
			int numberOfMatchesToFind) throws FileNotFoundException {
		List<Integer> lineNumbers = new ArrayList<Integer>();

		try (Scanner scanner = new Scanner(file)) {
			int lineNumber = 0;
			lineLoop: while (scanner.hasNextLine()) {
				if (lineNumber >= startingLineNumber) {
					String line = scanner.nextLine();
					lineNumber++;
					if (line.contains(textToFind)) {
						lineNumbers.add(lineNumber);
					}
					if (lineNumbers.size() >= numberOfMatchesToFind) {
						break lineLoop;
					}
				}
			}
		}

		return ArraysUtil.convertToIntArray(lineNumbers);
	}

	/**
	 * Simple utility to read the entire contents of a file into a string This
	 * code was taken from http://snippets.dzone.com/posts/show/1335
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
	 * @throws IOException
	 */
	public static String readFirstLineAsString(File file) throws IOException {
		String firstLine = null;
		StringBuilder firstLineBuilder = new StringBuilder();
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
			byte[] character = new byte[4096];
			int readChars = 0;
			if ((readChars = inputStream.read(character)) != -1) {
				characterLoop: for (int i = 0; i < readChars; ++i) {

					if (((char) character[i]) == StringUtil.NEWLINE_SYMBOL) {
						firstLine = firstLineBuilder.toString();
						break characterLoop;
					} else {
						if (((char) character[i]) != StringUtil.CARRIAGE_RETURN) {
							firstLineBuilder.append((char) character[i]);
						}
					}
				}
			}
		}
		return firstLine;
	}

	/**
	 * calls Files createNewFile but will also create new subdirectories if
	 * needed.
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
			int length = absoluteDirectories.length < relativeDirectories.length ? absoluteDirectories.length
					: relativeDirectories.length;

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
		int count = 1;
		boolean empty = true;
		InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
		try {
			byte[] character = new byte[1024];
			int readChars = 0;
			while ((readChars = inputStream.read(character)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					if (((char) character[i]) == StringUtil.NEWLINE_SYMBOL) {
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
	 * Recursively delete a directory. If the first attempt fails due to an
	 * IOException, garbage collect, sleep, and try one more time to get around
	 * an issue with NFS volumes.
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
			// Directories mounted on NFS volumes may have lingering .nfsXXXX
			// files
			// if no streams are open, it is likely from stale objects
			int totalAttempts = 5;
			for (int i = 0; i < totalAttempts; i++) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
				}
				System.gc();
				try {
					FileUtils.deleteDirectory(directory);
				} catch (IOException e1) {
					Logger logger = LoggerFactory.getLogger(FileUtil.class);
					if (i == totalAttempts - 1) {
						logger.warn("Unable to delete directory[" + directory.getAbsolutePath() + "] on attempt "
								+ (i + 1) + ".  Will attempt deletion on exit.");
						directory.deleteOnExit();
					} else {
						logger.warn("Unable to delete directory[" + directory.getAbsolutePath() + "] on attempt "
								+ (i + 1) + ".  Will attempt deletion " + (totalAttempts - i - 1) + " more times.");
						continue;
					}
				}
				break;
			}
		}
	}

	public static void writeStringToFile(File file, String stringToWrite) throws IOException {
		createNewFile(file);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(stringToWrite);
		}
	}

	/**
	 * taken from here
	 * http://codereview.stackexchange.com/questions/47923/simplifying-a-path
	 * -Kurt Heilman
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
		File fileB = new File("D://kurts_space/jigar_dirt/");
		File fileC = new File("C://kurts_space/jigar_dirt/");
		// String relativePath = FileUtil.convertToRelativePath(fileA, fileB);
		System.out.println(isDirectoryParentOfFile(fileB, fileA));
		System.out.println(isDirectoryParentOfFile(fileC, fileA));
	}

	public static boolean isDirectoryParentOfFile(File directory, File file) {
		boolean directoryIsParent = false;
		File ancestorOfFile = file.getParentFile();
		while (ancestorOfFile != null && !directoryIsParent) {
			directoryIsParent = ancestorOfFile.equals(directory);
			ancestorOfFile = ancestorOfFile.getParentFile();
		}
		return directoryIsParent;
	}

	/**
	 * 
	 * @param fileOne
	 * @param fileTwo
	 * @param ignoreEOL
	 * @return true if the files contents are the same.
	 * @throws IOException
	 */
	public static boolean filesContentsAreEqual(File fileOne, File fileTwo, boolean ignoreEOL) throws IOException {
		boolean contentsAreEqual = false;
		if (ignoreEOL) {
			contentsAreEqual = FileUtils.contentEqualsIgnoreEOL(fileOne, fileTwo, null);
		} else {
			contentsAreEqual = FileUtils.contentEquals(fileOne, fileTwo);
		}
		return contentsAreEqual;
	}

	public static List<File> getAllSubFiles(File directory) {
		List<File> subFiles = new ArrayList<File>();

		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				subFiles.addAll(getAllSubFiles(file));
			} else {
				subFiles.add(file);
			}
		}

		return subFiles;

	}

	/**
	 * 
	 * @param directory
	 * @return the subdirectories of the provided directory.
	 */
	public static File[] getSubDirectories(File directory) {
		if (!directory.isDirectory()) {
			throw new IllegalStateException(
					"The provided file[" + directory.getAbsolutePath() + "] is not a directory.");
		}

		File[] subdirectories = directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});

		return subdirectories;
	}

	/**
	 * return the files that match the provided regex in the provided directory
	 * (does not examine children directories of said directory).
	 * 
	 * @param directory
	 * @param regex
	 * @return matching files
	 */
	public static List<File> getMatchingFilesInDirectory(File directory, String regex) {
		return getMatchingFilesInDirectory(directory, regex, true);
	}

	/**
	 * return the files that match the provided regex in the provided directory
	 * (does not examine children directories of said directory).
	 * 
	 * @param directory
	 * @param regex
	 * @return matching files
	 */
	public static List<File> getMatchingFilesInDirectory(File directory, String regex, boolean isCaseInsensitive) {
		List<File> matchingFiles = new ArrayList<File>();

		final Pattern pattern = isCaseInsensitive ? Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
				: Pattern.compile(regex);

		String[] matchingFileNames = directory.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				boolean accept = pattern.matcher(name).matches();
				return accept;
			}
		});

		if (matchingFileNames != null) {
			for (String matchingFileName : matchingFileNames) {
				matchingFiles.add(new File(directory, matchingFileName));
			}
		}

		return matchingFiles;
	}

	/**
	 * returns the matching file if only one exists matching the regex in the
	 * provided directory, null if no such files exist and throws an
	 * IllegalArgumentException if more than on of such files exist.
	 * 
	 * @param directory
	 * @param regex
	 * @return matching file
	 */
	public static File getMatchingFileInDirectory(File directory, String regex) {
		return getMatchingFileInDirectory(directory, regex, true);
	}

	/**
	 * returns the matching file if only one exists matching the regex in the
	 * provided directory, null if no such files exist and throws an
	 * IllegalArgumentException if more than on of such files exist.
	 * 
	 * @param directory
	 * @param regex
	 * @return matching file
	 */
	public static File getMatchingFileInDirectory(File directory, String regex, boolean isCaseInsensitive) {
		List<File> matchingFiles = getMatchingFilesInDirectory(directory, regex, isCaseInsensitive);

		File matchingFile = null;
		if (matchingFiles.size() == 1) {
			matchingFile = matchingFiles.get(0);
		} else if (matchingFiles.size() > 1) {
			throw new IllegalArgumentException("The provided directory[" + directory
					+ "] contains more than one file matching the provided regex[" + regex + "].");
		}
		return matchingFile;
	}

	public static File getMatchingDirectoryRelativeToBaseDirectory(File baseDirectory,
			String[] regularExpressionsForRelativeFolders) {
		File currentDirectory = baseDirectory;
		for (String regularExpressionForNextFolder : regularExpressionsForRelativeFolders) {
			if (regularExpressionForNextFolder.equals(".")) {
				// stay in the current directory
			} else if (regularExpressionForNextFolder.equals("..")) {
				currentDirectory = currentDirectory.getParentFile();
			} else {
				List<File> matchingFiles = new ArrayList<File>();
				for (File childFile : currentDirectory.listFiles()) {
					if (childFile.isDirectory()) {
						if (Pattern.matches(regularExpressionForNextFolder, childFile.getName())) {
							matchingFiles.add(childFile);
						}
					}
				}
				if (matchingFiles.size() == 1) {
					currentDirectory = matchingFiles.get(0);
				} else if (matchingFiles.size() == 0) {
					throw new IllegalStateException("Unable to locate a sub folder matching the regular expression["
							+ regularExpressionForNextFolder + "] in the directory["
							+ currentDirectory.getAbsolutePath() + "].");
				} else {
					throw new IllegalStateException("The regular expression[" + regularExpressionForNextFolder
							+ "] matches more than one file (" + matchingFiles.size()
							+ " matches found) in the directory[" + currentDirectory.getAbsolutePath() + "].");
				}
			}
		}

		return currentDirectory;
	}
}
