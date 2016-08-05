package com.roche.sequencing.bioinformatics.common.directorymonitor;

import com.roche.sequencing.bioinformatics.common.multithreading.PausableFixedThreadPoolExecutor;

/**
 * 
 * Class for handling background UI tasks in DEVA (this is a lightweight alternative to SwingWorkers) The main intent is to have a thread pool handling these events instead of creating new threads to
 * run each runnable.
 * 
 */
public class DirectoryMonitorThreadEventQueue {

	private static final int NUMBER_OF_THREADS = 3;
	private final static String THREAD_NAME_PREFIX = "DIRECTORY_MONITOR_";
	private static PausableFixedThreadPoolExecutor executorService;

	private DirectoryMonitorThreadEventQueue() {
	}

	/**
	 * Synchronized function to call before any use of executorService.
	 */
	private static synchronized void initializeExecutorServiceSingleton() {
		if (executorService == null) {
			PausableFixedThreadPoolExecutor newExecutorService = new PausableFixedThreadPoolExecutor(NUMBER_OF_THREADS, THREAD_NAME_PREFIX);
			executorService = newExecutorService;
		}
	}

	/**
	 * run a job using a thread pool specifically dedicated for DEVA background UI tasks (this is a lightweight alternative to SwingWorkers
	 * 
	 * @param runnable
	 */
	public static void runInBackground(Runnable runnable) {
		initializeExecutorServiceSingleton();
		executorService.execute(runnable);
	}

	public static void pause() {
		if (executorService != null) {
			executorService.pause();
		}
	}

	public static void resume() {
		initializeExecutorServiceSingleton();
		executorService.resume();
	}

	public static boolean isPaused() {
		return executorService.isPaused();
	}

	public static void shutdownNow() {
		executorService.shutdownNow();
	}
}
