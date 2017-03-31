package com.roche.sequencing.bioinformatics.common.fastqtool.settings;

public class FastqToolOutputSettings {

	public static final String OUTPUT_FILES_IN_SAME_DIRECTORY_AS_INPUT_FASTQ_KEY = "Output_Files_In_Same_Directory_As_Input_Fastq";
	public static final String OUTPUT_FILES_IN_SUBDIRECTORY_OF_INPUT_FASTQ_KEY = "Output_Files_In_Subdirectory_Of_Input_Fastq";
	public static final String OUTPUT_FILES_IN_APPLICATION_DIRECTORY_KEY = "Output_Files_In_Application_Directory";
	public static final String OUTPUT_FILES_IN_SUBDIRECTORY_OF_APPLICATION_DIRECTORY_KEY = "Output_Files_In_Subdirectory_Of_Application_Directory";
	public static final String OUTPUT_FILES_IN_DESIGNATED_DIRECTORY_KEY = "Output_Files_In_Designated_Directory";

	public static final String OUTPUT_SEQUENCE_SEARCH_FIND_SUMMARY_KEY = "Output_Sequence_Search_Find_Summary";
	public static final String OUTPUT_FASTQ_FIND_SUMMARY_KEY = "Output_Fastq_Find_Summary";
	public static final String OUTPUT_FIND_ALIGNMENT_KEY = "Output_Find_Alignment";
	public static final String OUTPUT_FIND_LOG_KEY = "Output_Find_Log";

	private final OutputFileActionEnum outputFileAction;
	private final boolean outputSequenceSearchFindSummary;
	private final boolean outputFastQFindSummary;
	private final boolean outputFindAlignment;
	private final boolean outputFindLog;

	private final String outputFileAcitonValue;

	public FastqToolOutputSettings(OutputFileActionEnum outputFileAction, boolean outputSequenceSearchFindSummary, boolean outputFastQFindSummary, boolean outputFindAlignment, boolean outputFindLog,
			String outputFileAcitonValue) {
		super();
		this.outputFileAction = outputFileAction;
		this.outputSequenceSearchFindSummary = outputSequenceSearchFindSummary;
		this.outputFastQFindSummary = outputFastQFindSummary;
		this.outputFindAlignment = outputFindAlignment;
		this.outputFindLog = outputFindLog;
		this.outputFileAcitonValue = outputFileAcitonValue;
	}

	public OutputFileActionEnum getOutputFileAction() {
		return outputFileAction;
	}

	public boolean isOutputSequenceSearchFindSummary() {
		return outputSequenceSearchFindSummary;
	}

	public boolean isOutputFastQFindSummary() {
		return outputFastQFindSummary;
	}

	public boolean isOutputFindAlignment() {
		return outputFindAlignment;
	}

	public boolean isOutputFindLog() {
		return outputFindLog;
	}

	public String getOutputFileActionValue() {
		return outputFileAcitonValue;
	}

}
