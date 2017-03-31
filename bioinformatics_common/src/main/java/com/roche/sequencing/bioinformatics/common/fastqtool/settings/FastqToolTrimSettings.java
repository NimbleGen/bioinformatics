package com.roche.sequencing.bioinformatics.common.fastqtool.settings;

public class FastqToolTrimSettings {

	public static final String ADD_PRE_TEXT_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY = "Add_Pre_Text_To_FastQ_Annotation_With_Following_Key";
	public static final String REMOVE_PRE_TEXT_IN_OUTPUT_KEY = "Remove_Pre_Text_In_Output";
	public static final String ADD_FOUND_TEXT_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY = "Add_Found_Text_To_FastQ_Annotation_With_Following_Key";
	public static final String ADD_FOUND_SEQUENCE_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY = "Add_Found_Sequence_To_FastQ_Annotation_With_Following_Key";
	public static final String ADD_FOUND_SEQUENCE_PRIMARY_ID_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY = "Add_Found_Sequence_Primary_ID_To_FastQ_Annotation_With_Following_Key";
	public static final String ADD_FOUND_SEQUENCE_SECONDARY_ID_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY = "Add_Found_Sequence_Secondary_ID_To_FastQ_Annotation_With_Following_Key";
	public static final String ADD_FOUND_SEQUENCE_ORIENTATION_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY = "Add_Found_Sequence_Orientation_ID_To_FastQ_Annotation_With_Following_Key";
	public static final String REMOVE_FOUND_TEXT_IN_OUTPUT_KEY = "Remove_Found_Text_In_Output";
	public static final String REPLACE_FOUND_TEXT_WITH_SEARCH_SEQUENCE_KEY = "Replace_Found_Text_With_Search_Sequence";
	public static final String KEEP_FOUND_TEXT_IN_OUTPUT_KEY = "Keep_Found_Text_In_Output";
	public static final String ADD_POST_TEXT_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY = "Add_Post_Text_To_FastQ_Annotation_With_Following_Key";
	public static final String REMOVE_POST_TEXT_IN_OUTPUT_KEY = "Remove_Post_Text_In_Output";
	public static final String REPEAT_ENTRIES_WITH_MULTIPLE_MATCHES_KEY = "Repeat_Entries_With_Multiple_Matches";
	public static final String PUT_ENTRIES_WITH_MULTIPLE_MATCHES_IN_OWN_FILE_KEY = "Put_Entries_With_Multiple_Matches_In_Own_File";
	public static final String PUT_ENTRIES_WITH_MUTLIPLE_MATCHES_IN_NOT_FOUND_FILE_KEY = "Put_Entries_With_Mutliple_Matches_In_Not_Found_File";
	public static final String PUT_ENTRIES_WITH_MUTLIPLE_MATCHES_IN_SINGLE_MATCHES_FILE_KEY = "Put_Entries_With_Mutliple_Matches_In_Single_Matches_File";
	public static final String EXCLUDE_ENTRIES_WITH_MUTLIPLE_MATCHES_KEY = "Exclude_Entries_With_Mutliple_Matches";
	public static final String PUT_ENTRIES_WITH_NO_MATCHES_IN_OWN_FILE_KEY = "Put_Entries_With_No_Matches_In_Own_File";
	public static final String PUT_ENTRIES_WITH_NO_MATCHES_IN_SINGLE_MATCHES_FILE_KEY = "Put_Entries_With_No_Matches_In_Single_Matches_File";
	public static final String EXCLUDE_ENTRIES_WITH_NO_MATCHES_KEY = "Exclude_Entries_With_No_Matches";
	public static final String ADD_ANNOTATION_TO_ENTRIES_WITH_NO_MATCHES_KEY = "Add_Annotation_To_Entries_With_No_Matches";
	public static final String LEADING_QUALITY_TRIM_THRESHOLD_KEY = "Leading_Quality_Trim_Threshold";
	public static final String TRAILING_QUALITY_TRIM_THRESHOLD_KEY = "Trailing_Quality_Trim_Threshold";

	// Pre Text Settings
	private final String addPreTextToFastQAnnotationWithFollowingKey;
	private final boolean removePreTextInOutput;

	// Found Text Settings
	private final String addFoundTextToFastQAnnotationWithFollowingKey;
	private final String addFoundSequenceToFastQAnnotationWithFollowingKey;
	private final String addFoundSequencePrimaryIdToFastQAnnotationWithFollowingKey;
	private final String addFoundSequenceSecondaryIdToFastQAnnotationWithFollowingKey;
	private final String addFoundSequenceOrientationToFastQAnnotationWithFollowingKey;
	private final FoundTextActionEnum foundTextAction;

	// Post Text Settings
	private final String addPostTextToFastQAnnotationWithFollowingKey;
	private final boolean removePostTextInOutput;

	private final MultipleMatchesActionEnum multipleMatchesAction;
	private final NoMatchesActionEnum noMatchesAction;

	private final int leadingQualityTrimThreshold;
	private final int trailingQualityTrimThreshold;

	public FastqToolTrimSettings(String addPreTextToFastQAnnotationWithFollowingKey, boolean removePreTextInOutput, String addFoundTextToFastQAnnotationWithFollowingKey,
			String addFoundSequenceToFastQAnnotationWithFollowingKey, String addFoundSequencePrimaryIdToFastQAnnotationWithFollowingKey,
			String addFoundSequenceSecondaryIdToFastQAnnotationWithFollowingKey, String addFoundSequenceOrientationToFastQAnnotationWithFollowingKey, FoundTextActionEnum foundTextAction,
			String addPostTextToFastQAnnotationWithFollowingKey, boolean removePostTextInOutput, MultipleMatchesActionEnum multipleMatchesAction, NoMatchesActionEnum noMatchesAction,
			int leadingQualityTrimThreshold, int trailingQualityTrimThreshold) {
		super();
		this.addPreTextToFastQAnnotationWithFollowingKey = addPreTextToFastQAnnotationWithFollowingKey;
		this.removePreTextInOutput = removePreTextInOutput;
		this.addFoundTextToFastQAnnotationWithFollowingKey = addFoundTextToFastQAnnotationWithFollowingKey;
		this.addFoundSequenceToFastQAnnotationWithFollowingKey = addFoundSequenceToFastQAnnotationWithFollowingKey;
		this.addFoundSequencePrimaryIdToFastQAnnotationWithFollowingKey = addFoundSequencePrimaryIdToFastQAnnotationWithFollowingKey;
		this.addFoundSequenceSecondaryIdToFastQAnnotationWithFollowingKey = addFoundSequenceSecondaryIdToFastQAnnotationWithFollowingKey;
		this.addFoundSequenceOrientationToFastQAnnotationWithFollowingKey = addFoundSequenceOrientationToFastQAnnotationWithFollowingKey;
		this.foundTextAction = foundTextAction;
		this.addPostTextToFastQAnnotationWithFollowingKey = addPostTextToFastQAnnotationWithFollowingKey;
		this.removePostTextInOutput = removePostTextInOutput;
		this.multipleMatchesAction = multipleMatchesAction;
		this.noMatchesAction = noMatchesAction;
		this.leadingQualityTrimThreshold = leadingQualityTrimThreshold;
		this.trailingQualityTrimThreshold = trailingQualityTrimThreshold;
	}

	public String getAddPreTextToFastQAnnotationWithFollowingKey() {
		return addPreTextToFastQAnnotationWithFollowingKey;
	}

	public boolean isRemovePreTextInOutput() {
		return removePreTextInOutput;
	}

	public String getAddFoundTextToFastQAnnotationWithFollowingKey() {
		return addFoundTextToFastQAnnotationWithFollowingKey;
	}

	public String getAddFoundSequenceToFastQAnnotationWithFollowingKey() {
		return addFoundSequenceToFastQAnnotationWithFollowingKey;
	}

	public String getAddFoundSequencePrimaryIdToFastQAnnotationWithFollowingKey() {
		return addFoundSequencePrimaryIdToFastQAnnotationWithFollowingKey;
	}

	public String getAddFoundSequenceSecondaryIdToFastQAnnotationWithFollowingKey() {
		return addFoundSequenceSecondaryIdToFastQAnnotationWithFollowingKey;
	}

	public String getAddFoundSequenceOrientationToFastQAnnotationWithFollowingKey() {
		return addFoundSequenceOrientationToFastQAnnotationWithFollowingKey;
	}

	public FoundTextActionEnum getFoundTextAction() {
		return foundTextAction;
	}

	public String getAddPostTextToFastQAnnotationWithFollowingKey() {
		return addPostTextToFastQAnnotationWithFollowingKey;
	}

	public boolean isRemovePostTextInOutput() {
		return removePostTextInOutput;
	}

	public MultipleMatchesActionEnum getMultipleMatchesAction() {
		return multipleMatchesAction;
	}

	public NoMatchesActionEnum getNoMatchesAction() {
		return noMatchesAction;
	}

	public int getLeadingQualityTrimThreshold() {
		return leadingQualityTrimThreshold;
	}

	public int getTrailingQualityTrimThreshold() {
		return trailingQualityTrimThreshold;
	}

}
