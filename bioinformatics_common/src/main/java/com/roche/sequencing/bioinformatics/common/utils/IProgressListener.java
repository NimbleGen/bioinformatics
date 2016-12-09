package com.roche.sequencing.bioinformatics.common.utils;

public interface IProgressListener {

	void updateProgress(double percentComplete, String status);

}
