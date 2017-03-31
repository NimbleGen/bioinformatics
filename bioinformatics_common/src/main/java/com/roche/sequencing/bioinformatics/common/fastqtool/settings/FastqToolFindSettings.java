package com.roche.sequencing.bioinformatics.common.fastqtool.settings;

import com.roche.sequencing.bioinformatics.common.fastqtool.FastqFindAndTrimTool;
import com.roche.sequencing.bioinformatics.common.fastqtool.OrientationEnum;

public class FastqToolFindSettings {

	public static final String ALLOWED_MISMATCH_BASES_KEY = "Allowed_Mismatch_Bases";
	public static final String ALLOWED_INSERTION_GAPS_KEY = "Allowed_Insertion_Gaps";
	public static final String ALLOWED_INSERTION_BASES_KEY = "Allowed_Insertion_Bases";
	public static final String ALLOWED_DELETION_GAPS_KEY = "Allowed_Deletion_Gaps";
	public static final String ALLOWED_DELETION_BASES_KEY = "Allowed_Deletion_Bases";

	public static final String INCLUDE_FORWARD_VERSION_OF_SEARCH_SEQUENCE_KEY = "Include_Forward_Version_Of_Search_Sequence";
	public static final String INCLUDE_REVERSE_VERSION_OF_SEARCH_SEQUENCE_KEY = "Include_Reverse_Version_Of_Search_Sequence";
	public static final String INCLUDE_REVERSE_COMPLIMENT_VERSION_OF_SEARCH_SEQUENCE_KEY = "Include_Reverse_Compliment_Version_Of_Search_Sequence";
	public static final String INCLUDE_COMPLIMENT_VERSION_OF_SEARCH_SEQUENCE_KEY = "Include_Compliment_Version_Of_Search_Sequence";

	private final int allowedMismatchBases;
	private final int allowedInsertionGaps;
	private final int allowedInsertionBases;
	private final int allowedDeletionGaps;
	private final int allowedDeletionBases;

	private final boolean includeForwardVersionOfSearchSequence;
	private final boolean includeReverseVersionOfSearchSequence;
	private final boolean includeReverseComplimentVersionOfSearchSequence;
	private final boolean includeComplimentVersionOfSearchSequence;

	public FastqToolFindSettings(int allowedMismatchBases, int allowedInsertionGaps, int allowedInsertionBases, int allowedDeletionGaps, int allowedDeletionBases,
			boolean includeForwardVersionOfSearchSequence, boolean includeReverseVersionOfSearchSequence, boolean includeReverseComplimentVersionOfSearchSequence,
			boolean includeComplimentVersionOfSearchSequence) {
		super();
		this.allowedMismatchBases = allowedMismatchBases;
		this.allowedInsertionGaps = allowedInsertionGaps;
		this.allowedInsertionBases = allowedInsertionBases;
		this.allowedDeletionGaps = allowedDeletionGaps;
		this.allowedDeletionBases = allowedDeletionBases;
		this.includeForwardVersionOfSearchSequence = includeForwardVersionOfSearchSequence;
		this.includeReverseVersionOfSearchSequence = includeReverseVersionOfSearchSequence;
		this.includeReverseComplimentVersionOfSearchSequence = includeReverseComplimentVersionOfSearchSequence;
		this.includeComplimentVersionOfSearchSequence = includeComplimentVersionOfSearchSequence;
	}

	public int getAllowedMismatchBases() {
		return allowedMismatchBases;
	}

	public int getAllowedInsertionGaps() {
		return allowedInsertionGaps;
	}

	public int getAllowedInsertionBases() {
		return allowedInsertionBases;
	}

	public int getAllowedDeletionGaps() {
		return allowedDeletionGaps;
	}

	public int getAllowedDeletionBases() {
		return allowedDeletionBases;
	}

	public boolean isIncludeForwardVersionOfSearchSequence() {
		return includeForwardVersionOfSearchSequence;
	}

	public boolean isIncludeReverseVersionOfSearchSequence() {
		return includeReverseVersionOfSearchSequence;
	}

	public boolean isIncludeReverseComplimentVersionOfSearchSequence() {
		return includeReverseComplimentVersionOfSearchSequence;
	}

	public boolean isIncludeComplimentVersionOfSearchSequence() {
		return includeComplimentVersionOfSearchSequence;
	}

	public boolean shouldIncludeOrientation(OrientationEnum orientation) {
		boolean shouldInclude = false;

		switch (orientation) {
		case COMPLIMENT:
			shouldInclude = isIncludeComplimentVersionOfSearchSequence();
			break;
		case REVERSE_COMPLIMENT:
			shouldInclude = isIncludeReverseComplimentVersionOfSearchSequence();
			break;
		case REVERSE:
			shouldInclude = isIncludeReverseVersionOfSearchSequence();
			break;
		case FORWARD:
			shouldInclude = isIncludeForwardVersionOfSearchSequence();
			break;
		default:
			throw new AssertionError();
		}

		return shouldInclude;
	}

	public String getDefaultOrientationAbbreviations() {
		StringBuilder defaultAbbreviationsBuilder = new StringBuilder();

		if (isIncludeComplimentVersionOfSearchSequence()) {
			defaultAbbreviationsBuilder.append(OrientationEnum.COMPLIMENT.getAbbreviation() + FastqFindAndTrimTool.ORIENTATION_DELIMITER);
		}
		if (isIncludeForwardVersionOfSearchSequence()) {
			defaultAbbreviationsBuilder.append(OrientationEnum.FORWARD.getAbbreviation() + FastqFindAndTrimTool.ORIENTATION_DELIMITER);
		}
		if (isIncludeReverseComplimentVersionOfSearchSequence()) {
			defaultAbbreviationsBuilder.append(OrientationEnum.REVERSE_COMPLIMENT.getAbbreviation() + FastqFindAndTrimTool.ORIENTATION_DELIMITER);
		}
		if (isIncludeReverseVersionOfSearchSequence()) {
			defaultAbbreviationsBuilder.append(OrientationEnum.REVERSE.getAbbreviation() + FastqFindAndTrimTool.ORIENTATION_DELIMITER);
		}

		String defaultOrientationAbbreviations = "";
		if (defaultAbbreviationsBuilder.length() > 0) {
			// remove the last comma
			defaultOrientationAbbreviations = defaultAbbreviationsBuilder.substring(0, defaultAbbreviationsBuilder.length() - 1);
		}
		return defaultOrientationAbbreviations;
	}

}
