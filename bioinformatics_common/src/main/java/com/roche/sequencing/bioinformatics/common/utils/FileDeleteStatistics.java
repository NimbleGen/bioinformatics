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

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 
 * Simple object to keep track of what happens when a deletion is attempted on a file or directory
 * 
 */
public class FileDeleteStatistics {
	private int filesAttemptedToDelete, directoriesAttemptedToDelete, filesDeleted, directoriesDeleted;
	private final Set<String> directoriesThatCouldNotBeDeleted;
	private final Set<String> filesThatCouldNotBeDeleted;

	/**
	 * Constructor
	 */
	public FileDeleteStatistics() {
		super();
		filesAttemptedToDelete = 0;
		directoriesAttemptedToDelete = 0;
		filesDeleted = 0;
		directoriesDeleted = 0;
		directoriesThatCouldNotBeDeleted = new LinkedHashSet<String>();
		filesThatCouldNotBeDeleted = new LinkedHashSet<String>();
	}

	void add(FileDeleteStatistics statsToAdd) {
		filesAttemptedToDelete += statsToAdd.filesAttemptedToDelete;
		directoriesAttemptedToDelete += statsToAdd.directoriesAttemptedToDelete;
		filesDeleted += statsToAdd.filesDeleted;
		directoriesDeleted += statsToAdd.directoriesDeleted;
		directoriesThatCouldNotBeDeleted.addAll(statsToAdd.directoriesThatCouldNotBeDeleted);
		filesThatCouldNotBeDeleted.addAll(statsToAdd.filesThatCouldNotBeDeleted);
	}

	void addDirectoryThatCouldNotBeDeleted(File directoryThatCouldNotBeDeleted) {
		directoriesThatCouldNotBeDeleted.add(directoryThatCouldNotBeDeleted.getAbsolutePath());
	}

	void addFileThatCouldNotBeDeleted(File fileThatCouldNotBeDeleted) {
		filesThatCouldNotBeDeleted.add(fileThatCouldNotBeDeleted.getAbsolutePath());
	}

	void incrementFilesAttemptedToDelete() {
		filesAttemptedToDelete++;
	}

	void incrementDirectoriesAttemptedToDelete() {
		directoriesAttemptedToDelete++;
	}

	void incrementFilesDeleted() {
		filesDeleted++;
	}

	void incrementDirectoriesDeleted() {
		directoriesDeleted++;
	}

	/**
	 * @return the number of files that were attempted to delete
	 */
	public int getFilesAttemptedToDelete() {
		return filesAttemptedToDelete;
	}

	/**
	 * @return the number of directories that were attempted to delete
	 */
	public int getDirectoriesAttemptedToDelete() {
		return directoriesAttemptedToDelete;
	}

	/**
	 * @return the number of files deleted
	 */
	public int getFilesDeleted() {
		return filesDeleted;
	}

	/**
	 * @return the number of directories deleted
	 */
	public int getDirectoriesDeleted() {
		return directoriesDeleted;
	}

	/**
	 * @return true if all the files and directories that were attempted to be deleted were actually deleted
	 */
	public boolean isSuccess() {
		boolean success = filesDeleted == filesAttemptedToDelete;

		success = success && (directoriesDeleted == directoriesAttemptedToDelete);
		return success;
	}

	@Override
	public String toString() {
		return "FileDeleteStatistics [filesAttemptedToDelete=" + filesAttemptedToDelete + ", directoriesAttemptedToDelete=" + directoriesAttemptedToDelete + ", filesDeleted=" + filesDeleted
				+ ", directoriesDeleted=" + directoriesDeleted + ", directoriesThatCouldNotBeDeleted=" + directoriesThatCouldNotBeDeleted + ", filesThatCouldNotBeDeleted="
				+ filesThatCouldNotBeDeleted + "]";
	}

}
