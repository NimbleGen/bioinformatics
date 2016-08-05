package com.roche.sequencing.bioinformatics.common.directorymonitor;

import java.io.File;

public interface IFileEventListener {

	void fileModified(File directoryBeingMonitored, File file);

	void fileCreated(File directoryBeingMonitored, File file);

	void fileRemoved(File directoryBeingMonitored, File file);

	void fileInitiallyDetected(File directoryBeingMonitored, File file);

}
