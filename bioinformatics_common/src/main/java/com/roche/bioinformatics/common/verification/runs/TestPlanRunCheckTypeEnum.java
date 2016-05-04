package com.roche.bioinformatics.common.verification.runs;

public enum TestPlanRunCheckTypeEnum {

	OUTPUT_FILE_CONTAINS_TEXT(false), OUTPUT_FILE_DOES_NOT_CONTAIN_TEXT(false), OUTPUT_FILE_PRESENT_WITH_NONZERO_SIZE(false), OUTPUT_FILE_PRESENT(false), OUTPUT_FILE_NOT_PRESENT(false), OUTPUT_FILE_MATCHES_MD5SUM(
			false), OUTPUT_FILE_MATCHES_EXISTING_FILE(false), CONSOLE_ERRORS_CONTAINS_TEXT(false), CONSOLE_ERRORS_DOES_NOT_CONTAIN_TEXT(false), CONSOLE_OUTPUT_CONTAINS_TEXT(false), CONSOLE_OUTPUT_DOES_NOT_CONTAIN_TEXT(
			false), RECORD_CONSOLE_OUTPUT(true);

	private final boolean isInformationOnly;

	private TestPlanRunCheckTypeEnum(boolean isInformationOnly) {
		this.isInformationOnly = isInformationOnly;
	}

	public boolean isInformationOnly() {
		return isInformationOnly;
	}

}
