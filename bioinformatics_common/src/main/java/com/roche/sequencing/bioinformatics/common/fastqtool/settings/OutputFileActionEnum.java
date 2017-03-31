package com.roche.sequencing.bioinformatics.common.fastqtool.settings;

public enum OutputFileActionEnum {
	OUTPUT_FILES_IN_SAME_DIRECTORY_AS_INPUT_FASTQ(false), OUTPUT_FILES_IN_SUBDIRECTORY_OF_INPUT_FASTQ(true), OUTPUT_FILES_IN_APPLICATION_DIRECTORY(
			false), OUTPUT_FILES_IN_SUBDIRECTORY_OF_APPLICATION_DIRECTORY(true), OUTPUT_FILES_IN_DESIGNATED_DIRECTORY(true);

	private final boolean requiresFilePath;

	private OutputFileActionEnum(boolean requiresFilePath) {
		this.requiresFilePath = requiresFilePath;
	}

	public boolean requiresFilePath() {
		return requiresFilePath;
	}

}
