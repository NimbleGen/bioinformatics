package com.roche.sequencing.bioinformatics.common.directorymonitor;

import java.io.File;
import java.util.regex.Pattern;

public interface IDirectoryMonitor {

	/**
	 * get the directories being monitored
	 * 
	 * @return directories being monitored
	 */
	File[] getDirectories();

	/**
	 * Get all patterns that are currently being used to determine which files should be monitored within the directory.
	 * 
	 * @return Patterns
	 */
	Pattern[] getFilePatterns(File directory);

	/**
	 * Start monitoring the directory for file changes.
	 */
	void start();

	/**
	 * Stop monitoring the directory for file changes.
	 */
	void stop();

	/**
	 * Get all fileEventListeners assigned to be notified when a file is marked as unchanged.
	 * 
	 * @return
	 */
	IFileEventListener[] getFileEventListeners();

	/**
	 * Remove all fileEventListeners so they are not notified when a file is marked as unchanged.
	 */
	void clearFileEventListeners();

	/**
	 * Add a fileValidListener to be notified when a file in the monitored directory is marked as valid.
	 * 
	 * @param fileValidListener
	 */
	void addFileEventListener(IFileEventListener fileEventListener);

	/**
	 * Remove fileEventListener so it is not notified when a file event occurs.
	 * 
	 * @param fileEventListener
	 */
	void removeFileEventListener(IFileEventListener fileEventListener);

	/**
	 * @return true if the monitor is currently running
	 */
	boolean isMonitoring();

	/**
	 * Set the directory to monitor
	 * 
	 * @param directory
	 */
	void addDirectoryToMonitor(File directory, Pattern pattern);

	/**
	 * Set the directory to monitor
	 * 
	 * @param directory
	 */
	void addDirectoryToMonitor(File directory, Pattern[] patterns);

	/**
	 * Set the directory to monitor
	 * 
	 * @param directory
	 */
	void addDirectoryToMonitor(File directory);

	/**
	 * stop monitoring the provided directory
	 * 
	 * @param directory
	 */
	void removeDirectoryFromMonitor(File directory);
}
