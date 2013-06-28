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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Util for working with files
 * 
 */
public final class FileUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

	private static final int BYTES_PER_KB = 1024;
	private static final int THREAD_SLEEP_TIME = 100;
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
	private static String getFileExtension(String fileName) {
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
	 * recursively delete the contents of directory AND the directory
	 * 
	 * @param directory
	 * @return statistics of deletion
	 */
	public static FileDeleteStatistics deleteDirectoryAndContents(File directory) {
		FileDeleteStatistics returnStats = deleteDirectoryContents(directory);

		returnStats.add(deleteEmptyDirectory(directory));
		return returnStats;
	}

	/**
	 * recursively delete the contents of directory (But not the directory)
	 * 
	 * @param directory
	 * @return statistics of deletion
	 */
	private static FileDeleteStatistics deleteDirectoryContents(File directory) {
		FileDeleteStatistics stats = new FileDeleteStatistics();

		if ((directory != null) && directory.isDirectory()) {
			for (File file : directory.listFiles()) {
				if (file.isDirectory()) {
					FileDeleteStatistics returnStats = deleteDirectoryContents(file);

					stats.add(returnStats);
					stats.add(deleteEmptyDirectory(file));
				} else {
					stats.incrementFilesAttemptedToDelete();

					if (file.delete()) {
						stats.incrementFilesDeleted();
					} else {
						stats.addFileThatCouldNotBeDeleted(file);
					}
				}
			}
		}

		return stats;
	}

	/**
	 * @param directory
	 * @return
	 */
	private static FileDeleteStatistics deleteEmptyDirectory(File directory) {
		FileDeleteStatistics stats = new FileDeleteStatistics();
		if (directory != null) {
			stats.incrementDirectoriesAttemptedToDelete();

			if (directory.list().length > 0) {
				// Directories mounted on NFS volumes may have lingering
				// .nfsXXXX files
				// if no streams are open, it is likely from stale objects
				System.gc();

				try {
					Thread.sleep(THREAD_SLEEP_TIME);
				} catch (InterruptedException e) {
					LOGGER.warn("Thread was interrupted while trying to resolve stale objects");
				}
			}

			if (directory.delete()) {
				stats.incrementDirectoriesDeleted();
			} else {
				stats.addDirectoryThatCouldNotBeDeleted(directory);
			}
		}

		return stats;
	}

	/**
	 * Simple utility to read the entire contents of a file into a string This code was taken from http://snippets.dzone.com/posts/show/1335
	 * 
	 */
	static String readFileAsString(File file) throws java.io.IOException {
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
