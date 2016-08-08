package com.roche.sequencing.bioinformatics.common.directorymonitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirectoryMonitor implements IDirectoryMonitor {

	private WatchService watchService;
	private Map<File, Set<Pattern>> directoryToPatternMap;
	private Set<IFileEventListener> fileEventListeners;
	private Set<File> scannedDirectories;
	private boolean monitorDirectoryRunnableIsOnEventQueue;
	private boolean isMonitoring;

	public DirectoryMonitor() throws IOException {
		watchService = FileSystems.getDefault().newWatchService();
		directoryToPatternMap = new ConcurrentHashMap<File, Set<Pattern>>();
		scannedDirectories = Collections.newSetFromMap(new ConcurrentHashMap<File, Boolean>());
		fileEventListeners = new LinkedHashSet<IFileEventListener>();
		isMonitoring = false;
	}

	@Override
	public File[] getDirectories() {
		return directoryToPatternMap.keySet().toArray(new File[0]);
	}

	@Override
	public void addDirectoryToMonitor(File directory) {
		addDirectoryToMonitor(directory, new Pattern[0]);
	}

	@Override
	public void addDirectoryToMonitor(File directory, Pattern pattern) {
		addDirectoryToMonitor(directory, new Pattern[] { pattern });
	}

	@Override
	public void addDirectoryToMonitor(File directory, Pattern[] patterns) {
		addFilePatterns(directory, patterns);
		if (isMonitoring) {
			scanDirectory(directory);
		}
	}

	@Override
	public void removeDirectoryFromMonitor(File directory) {
		directoryToPatternMap.remove(directory);
		Path path = directory.toPath();
		try {
			// Since the entry cannot be removed reduce the kinds of events that we listen for to
			// just deletions. Since there is no longer a directory entry in the directoryToPatternMap
			// this directory monitor will ignore the event anyway but this will reduce how many events are
			// triggered fro the underlying WatchService.
			path.register(watchService, StandardWatchEventKinds.ENTRY_DELETE);
		} catch (IOException e) {
			throw new IllegalStateException("Could not monitor directory[" + directory.getAbsolutePath() + "]." + e.getMessage(), e);
		}
	}

	private void addFilePatterns(File directory, Pattern[] patterns) {
		if (!directory.isDirectory()) {
			throw new IllegalStateException("The provided directory[" + directory.getAbsolutePath() + "] is not a directory.");
		}
		if (directoryToPatternMap.containsKey(directory)) {
			for (Pattern pattern : patterns) {
				directoryToPatternMap.get(directory).add(pattern);
			}
		} else {
			Set<Pattern> patternSet = Collections.newSetFromMap(new ConcurrentHashMap<Pattern, Boolean>());
			for (Pattern pattern : patterns) {
				patternSet.add(pattern);
			}
			directoryToPatternMap.put(directory, patternSet);
			Path path = directory.toPath();
			try {
				path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
			} catch (IOException e) {
				throw new IllegalStateException("Could not monitor directory[" + directory.getAbsolutePath() + "]." + e.getMessage(), e);
			}

		}
	}

	@Override
	public Pattern[] getFilePatterns(File directory) {
		return directoryToPatternMap.get(directory).toArray(new Pattern[0]);
	}

	@Override
	public void start() {
		if (!monitorDirectoryRunnableIsOnEventQueue) {
			DirectoryMonitorThreadEventQueue.runInBackground(new MonitorDirectory());
		}
		if (DirectoryMonitorThreadEventQueue.isPaused()) {
			DirectoryMonitorThreadEventQueue.resume();
		}
		for (File directory : directoryToPatternMap.keySet()) {
			scanDirectory(directory);
		}
		isMonitoring = true;
	}

	private void scanDirectory(final File directoryBeingMonitored) {
		DirectoryMonitorThreadEventQueue.runInBackground(new Runnable() {
			@Override
			public void run() {
				internalScanDirectory(directoryBeingMonitored);
			}

		});

	}

	private void internalScanDirectory(File directoryBeingMonitored) {
		synchronized (scannedDirectories) {
			if (scannedDirectories.add(directoryBeingMonitored)) {
				System.out.println("SCANNING" + directoryBeingMonitored.getAbsolutePath());
				for (File file : directoryBeingMonitored.listFiles()) {
					if (!file.isDirectory()) {
						notifyListenersOfEventsIfNecessary(directoryBeingMonitored, file, FileEventEnum.INITIALLY_DETECTED);
					}
				}
			}
		}
	}

	private class MonitorDirectory implements Runnable {

		@Override
		public void run() {
			boolean valid = true;
			do {
				WatchKey watchKey;
				try {
					watchKey = watchService.take();
					for (WatchEvent<?> event : watchKey.pollEvents()) {
						WatchEvent.Kind<?> kind = event.kind();
						String fileName = event.context().toString();
						FileEventEnum fileEvent = FileEventEnum.CREATED;
						if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
							fileEvent = FileEventEnum.CREATED;
						} else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
							fileEvent = FileEventEnum.REMOVED;
						} else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
							fileEvent = FileEventEnum.MODIFIED;
						}

						Path directory = (Path) watchKey.watchable();
						File directoryAsFile = directory.toFile();

						final File file = new File(directoryAsFile, fileName);

						DirectoryMonitor.this.notifyListenersOfEventsIfNecessary(directoryAsFile, file, fileEvent);

					}
					valid = watchKey.reset();
				} catch (InterruptedException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			} while (valid);
			System.out.println("No longer valid so monitoring is down.");
		}
	}

	private void notifyListenersOfEventsIfNecessary(final File directoryBeingMonitored, final File file, FileEventEnum event) {
		if (directoryToPatternMap.containsKey(directoryBeingMonitored)) {
			Set<Pattern> patterns = directoryToPatternMap.get(directoryBeingMonitored);
			boolean matchesPattern = false;
			if (patterns.size() == 0) {
				// no patterns exist so all entries are accepted
				matchesPattern = true;
			} else {
				patternLoop: for (Pattern pattern : patterns) {
					Matcher matcher = pattern.matcher(file.getAbsolutePath());
					if (matcher.find()) {
						matchesPattern = true;
						break patternLoop;
					}
				}
			}

			if (matchesPattern) {
				if (event == FileEventEnum.CREATED) {
					DirectoryMonitorThreadEventQueue.runInBackground(new Runnable() {
						@Override
						public void run() {
							for (IFileEventListener fileEventListener : fileEventListeners) {
								fileEventListener.fileCreated(directoryBeingMonitored, file);
							}
						}

					});
				} else if (event == FileEventEnum.REMOVED) {
					DirectoryMonitorThreadEventQueue.runInBackground(new Runnable() {
						@Override
						public void run() {
							for (IFileEventListener fileEventListener : fileEventListeners) {
								fileEventListener.fileRemoved(directoryBeingMonitored, file);
							}
						}

					});
				} else if (event == FileEventEnum.MODIFIED) {
					DirectoryMonitorThreadEventQueue.runInBackground(new Runnable() {
						@Override
						public void run() {
							for (IFileEventListener fileEventListener : fileEventListeners) {
								fileEventListener.fileModified(directoryBeingMonitored, file);
							}
						}

					});
				} else if (event == FileEventEnum.INITIALLY_DETECTED) {
					DirectoryMonitorThreadEventQueue.runInBackground(new Runnable() {

						@Override
						public void run() {
							for (IFileEventListener fileEventListener : fileEventListeners) {
								fileEventListener.fileInitiallyDetected(directoryBeingMonitored, file);
							}
						}

					});
				} else {
					throw new AssertionError();
				}

			}
		}
	}

	@Override
	public void stop() {
		DirectoryMonitorThreadEventQueue.pause();
	}

	public void shutdown() {
		DirectoryMonitorThreadEventQueue.shutdownNow();
		isMonitoring = false;
	}

	@Override
	public IFileEventListener[] getFileEventListeners() {
		return fileEventListeners.toArray(new IFileEventListener[0]);
	}

	@Override
	public void clearFileEventListeners() {
		fileEventListeners.clear();
	}

	@Override
	public void addFileEventListener(IFileEventListener fileEventListener) {
		fileEventListeners.add(fileEventListener);
	}

	@Override
	public void removeFileEventListener(IFileEventListener fileEventListener) {
		fileEventListeners.remove(fileEventListener);
	}

	@Override
	public boolean isMonitoring() {
		return isMonitoring;
	}

}
