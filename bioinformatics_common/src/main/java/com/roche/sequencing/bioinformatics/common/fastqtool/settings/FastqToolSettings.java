package com.roche.sequencing.bioinformatics.common.fastqtool.settings;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.InputStreamFactory;

public class FastqToolSettings {

	private final static String FIND_SETTINGS_KEY = "FIND";
	private final static String TRIM_SETTINGS_KEY = "TRIM";
	private final static String OUTPUT_SETTINGS_KEY = "OUTPUT";

	private final FastqToolFindSettings findSettings;
	private final FastqToolTrimSettings trimSettings;
	private final FastqToolOutputSettings outputSettings;

	public FastqToolSettings(FastqToolFindSettings findSettings, FastqToolTrimSettings trimSettings, FastqToolOutputSettings outputSettings) {
		super();
		this.findSettings = findSettings;
		this.trimSettings = trimSettings;
		this.outputSettings = outputSettings;
	}

	public FastqToolFindSettings getFindSettings() {
		return findSettings;
	}

	public FastqToolTrimSettings getTrimSettings() {
		return trimSettings;
	}

	public FastqToolOutputSettings getOutputSettings() {
		return outputSettings;
	}

	@SuppressWarnings("unchecked")
	public static FastqToolSettings parseSettings(InputStreamFactory settings) throws FileNotFoundException, IOException {
		Yaml yaml = new Yaml();

		Map<String, Object> root = (Map<String, Object>) yaml.load(FileUtil.readStreamAsString(settings.createInputStream()));

		List<Object> findSettingsYaml = (List<Object>) root.get(FIND_SETTINGS_KEY);

		boolean includeForwardVersionOfSearchSequence = false;
		boolean includeReverseVersionOfSearchSequence = false;
		boolean includeReverseComplimentVersionOfSearchSequence = false;
		boolean includeComplimentVersionOfSearchSequence = false;

		int allowedMismatchBases = Integer.MAX_VALUE;
		int allowedInsertionGaps = Integer.MAX_VALUE;
		int allowedInsertionBases = Integer.MAX_VALUE;
		int allowedDeletionGaps = Integer.MAX_VALUE;
		int allowedDeletionBases = Integer.MAX_VALUE;

		for (Object findSetting : findSettingsYaml) {
			if (findSetting instanceof String) {
				String string = (String) findSetting;
				if (string.equals(FastqToolFindSettings.INCLUDE_FORWARD_VERSION_OF_SEARCH_SEQUENCE_KEY)) {
					includeForwardVersionOfSearchSequence = true;
				} else if (string.equals(FastqToolFindSettings.INCLUDE_REVERSE_VERSION_OF_SEARCH_SEQUENCE_KEY)) {
					includeReverseVersionOfSearchSequence = true;
				} else if (string.equals(FastqToolFindSettings.INCLUDE_REVERSE_COMPLIMENT_VERSION_OF_SEARCH_SEQUENCE_KEY)) {
					includeReverseComplimentVersionOfSearchSequence = true;
				} else if (string.equals(FastqToolFindSettings.INCLUDE_COMPLIMENT_VERSION_OF_SEARCH_SEQUENCE_KEY)) {
					includeComplimentVersionOfSearchSequence = true;
				} else {
					throw new IllegalStateException("Unrecognized option[" + findSetting + "] in the FIND section of the settings file[" + settings.getName() + "].");
				}
			} else if (findSetting instanceof Map<?, ?>) {
				Map<String, Integer> map = (Map<String, Integer>) findSetting;

				if (map.containsKey(FastqToolFindSettings.ALLOWED_MISMATCH_BASES_KEY)) {
					allowedMismatchBases = map.get(FastqToolFindSettings.ALLOWED_MISMATCH_BASES_KEY);
				} else if (map.containsKey(FastqToolFindSettings.ALLOWED_INSERTION_GAPS_KEY)) {
					allowedInsertionGaps = map.get(FastqToolFindSettings.ALLOWED_INSERTION_GAPS_KEY);
				} else if (map.containsKey(FastqToolFindSettings.ALLOWED_INSERTION_BASES_KEY)) {
					allowedInsertionBases = map.get(FastqToolFindSettings.ALLOWED_INSERTION_BASES_KEY);
				} else if (map.containsKey(FastqToolFindSettings.ALLOWED_DELETION_GAPS_KEY)) {
					allowedDeletionGaps = map.get(FastqToolFindSettings.ALLOWED_DELETION_GAPS_KEY);
				} else if (map.containsKey(FastqToolFindSettings.ALLOWED_DELETION_BASES_KEY)) {
					allowedDeletionBases = map.get(FastqToolFindSettings.ALLOWED_DELETION_BASES_KEY);
				} else {
					throw new IllegalStateException("Unrecognized option[" + findSetting + "] in the FIND section of the settings file[" + settings.getName() + "].");
				}
			} else {
				throw new IllegalStateException("Unrecognized option[" + findSetting + "] in the FIND section of the settings file[" + settings.getName() + "].");
			}
		}

		FastqToolFindSettings findSettings = new FastqToolFindSettings(allowedMismatchBases, allowedInsertionGaps, allowedInsertionBases, allowedDeletionGaps, allowedDeletionBases,
				includeForwardVersionOfSearchSequence, includeReverseVersionOfSearchSequence, includeReverseComplimentVersionOfSearchSequence, includeComplimentVersionOfSearchSequence);

		List<Object> trimSettingsYaml = (List<Object>) root.get(TRIM_SETTINGS_KEY);

		String addPreTextToFastQAnnotationWithFollowingKey = null;
		boolean removePreTextInOutput = false;
		String addFoundTextToFastQAnnotationWithFollowingKey = null;
		String addFoundSequenceToFastQAnnotationWithFollowingKey = null;
		String addFoundSequencePrimaryIdToFastQAnnotationWithFollowingKey = null;
		String addFoundSequenceSecondaryIdToFastQAnnotationWithFollowingKey = null;
		String addFoundSequenceOrientationToFastQAnnotationWithFollowingKey = null;

		FoundTextActionEnum foundTextAction = null;

		String addPostTextToFastQAnnotationWithFollowingKey = null;
		boolean removePostTextInOutput = false;

		MultipleMatchesActionEnum multipleMatchesAction = null;
		NoMatchesActionEnum noMatchesAction = null;

		int leadingQualityTrimThreshold = 0;
		int trailingQualityTrimThreshold = 0;

		for (Object trimSetting : trimSettingsYaml) {
			if (trimSetting instanceof String) {
				String string = (String) trimSetting;
				if (string.equals(FastqToolTrimSettings.REMOVE_PRE_TEXT_IN_OUTPUT_KEY)) {
					removePreTextInOutput = true;
				} else if (foundTextAction == null && string.equals(FastqToolTrimSettings.REMOVE_FOUND_TEXT_IN_OUTPUT_KEY)) {
					foundTextAction = FoundTextActionEnum.REMOVE_FOUND_TEXT_IN_OUPUT;
				} else if (foundTextAction == null && string.equals(FastqToolTrimSettings.REPLACE_FOUND_TEXT_WITH_SEARCH_SEQUENCE_KEY)) {
					foundTextAction = FoundTextActionEnum.REPLACE_FOUND_TEXT_IN_OUTPUT;
				} else if (foundTextAction == null && string.equals(FastqToolTrimSettings.KEEP_FOUND_TEXT_IN_OUTPUT_KEY)) {
					foundTextAction = FoundTextActionEnum.KEEP_FOUND_TEXT_IN_OUTPUT;
				} else if (string.equals(FastqToolTrimSettings.REMOVE_POST_TEXT_IN_OUTPUT_KEY)) {
					removePostTextInOutput = true;
				} else if (multipleMatchesAction == null && string.equals(FastqToolTrimSettings.PUT_ENTRIES_WITH_MULTIPLE_MATCHES_IN_OWN_FILE_KEY)) {
					multipleMatchesAction = MultipleMatchesActionEnum.PUT_ENTRIES_WITH_MULTIPLE_MATCHES_IN_OWN_FILE;
				} else if (multipleMatchesAction == null && string.equals(FastqToolTrimSettings.PUT_ENTRIES_WITH_MUTLIPLE_MATCHES_IN_NOT_FOUND_FILE_KEY)) {
					multipleMatchesAction = MultipleMatchesActionEnum.PUT_ENTRIES_WITH_MULTIPLE_MATCHES_IN_NOT_FOUND_FILE;
				} else if (multipleMatchesAction == null && string.equals(FastqToolTrimSettings.PUT_ENTRIES_WITH_MUTLIPLE_MATCHES_IN_SINGLE_MATCHES_FILE_KEY)) {
					multipleMatchesAction = MultipleMatchesActionEnum.PUT_ENTIES_WITH_MULTIPLE_MATCHES_IN_SINGLE_MATCHES_FILE;
				} else if (multipleMatchesAction == null && string.equals(FastqToolTrimSettings.EXCLUDE_ENTRIES_WITH_MUTLIPLE_MATCHES_KEY)) {
					multipleMatchesAction = MultipleMatchesActionEnum.EXCLUDE_ENTRIES_WITH_MULTIPLE_MATCHES;
				} else if (noMatchesAction == null && string.equals(FastqToolTrimSettings.PUT_ENTRIES_WITH_NO_MATCHES_IN_OWN_FILE_KEY)) {
					noMatchesAction = NoMatchesActionEnum.PUT_ENTRIES_WITH_NO_MATCHES_IN_OWN_FILE;
				} else if (noMatchesAction == null && string.equals(FastqToolTrimSettings.EXCLUDE_ENTRIES_WITH_NO_MATCHES_KEY)) {
					noMatchesAction = NoMatchesActionEnum.EXCLUDE_ENTRIES_WITH_NO_MATCHES;
				} else if (noMatchesAction == null && string.equals(FastqToolTrimSettings.PUT_ENTRIES_WITH_NO_MATCHES_IN_SINGLE_MATCHES_FILE_KEY)) {
					noMatchesAction = NoMatchesActionEnum.PUT_ENTRIES_WITH_NO_MATCHES_IN_SINGLE_MATCH_FILE;
				} else {
					throw new IllegalStateException("Unrecognized option[" + trimSetting + "] in the TRIM section of the settings file[" + settings.getName() + "].");
				}
			} else if (trimSetting instanceof Map<?, ?>) {
				Map<String, Object> map = (Map<String, Object>) trimSetting;

				if (map.containsKey(FastqToolTrimSettings.ADD_PRE_TEXT_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY)) {
					addPreTextToFastQAnnotationWithFollowingKey = (String) map.get(FastqToolTrimSettings.ADD_PRE_TEXT_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY);
				} else if (map.containsKey(FastqToolTrimSettings.ADD_FOUND_TEXT_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY)) {
					addFoundTextToFastQAnnotationWithFollowingKey = (String) map.get(FastqToolTrimSettings.ADD_FOUND_TEXT_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY);
				} else if (map.containsKey(FastqToolTrimSettings.ADD_FOUND_SEQUENCE_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY)) {
					addFoundSequenceToFastQAnnotationWithFollowingKey = (String) map.get(FastqToolTrimSettings.ADD_FOUND_SEQUENCE_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY);
				} else if (map.containsKey(FastqToolTrimSettings.ADD_FOUND_SEQUENCE_PRIMARY_ID_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY)) {
					addFoundSequencePrimaryIdToFastQAnnotationWithFollowingKey = (String) map.get(FastqToolTrimSettings.ADD_FOUND_SEQUENCE_PRIMARY_ID_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY);
				} else if (map.containsKey(FastqToolTrimSettings.ADD_FOUND_SEQUENCE_SECONDARY_ID_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY)) {
					addFoundSequenceSecondaryIdToFastQAnnotationWithFollowingKey = (String) map.get(FastqToolTrimSettings.ADD_FOUND_SEQUENCE_SECONDARY_ID_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY);
				} else if (map.containsKey(FastqToolTrimSettings.ADD_FOUND_SEQUENCE_ORIENTATION_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY)) {
					addFoundSequenceOrientationToFastQAnnotationWithFollowingKey = (String) map.get(FastqToolTrimSettings.ADD_FOUND_SEQUENCE_ORIENTATION_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY);
				} else if (map.containsKey(FastqToolTrimSettings.ADD_POST_TEXT_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY)) {
					addPostTextToFastQAnnotationWithFollowingKey = (String) map.get(FastqToolTrimSettings.ADD_POST_TEXT_TO_FASTQ_ANNOTATION_WITH_FOLLOWING_KEY_KEY);
				} else if (map.containsKey(FastqToolTrimSettings.LEADING_QUALITY_TRIM_THRESHOLD_KEY)) {
					leadingQualityTrimThreshold = (int) map.get(FastqToolTrimSettings.LEADING_QUALITY_TRIM_THRESHOLD_KEY);
				} else if (map.containsKey(FastqToolTrimSettings.TRAILING_QUALITY_TRIM_THRESHOLD_KEY)) {
					trailingQualityTrimThreshold = (int) map.get(FastqToolTrimSettings.TRAILING_QUALITY_TRIM_THRESHOLD_KEY);
				} else {
					throw new IllegalStateException("Unrecognized option[" + trimSetting + "] in the TRIM section of the settings file[" + settings.getName() + "].");
				}

			} else {
				throw new IllegalStateException("Unrecognized option[" + trimSetting + "] in the TRIM section of the settings file[" + settings.getName() + "].");
			}

		}

		FastqToolTrimSettings trimSettings = new FastqToolTrimSettings(addPreTextToFastQAnnotationWithFollowingKey, removePreTextInOutput, addFoundTextToFastQAnnotationWithFollowingKey,
				addFoundSequenceToFastQAnnotationWithFollowingKey, addFoundSequencePrimaryIdToFastQAnnotationWithFollowingKey, addFoundSequenceSecondaryIdToFastQAnnotationWithFollowingKey,
				addFoundSequenceOrientationToFastQAnnotationWithFollowingKey, foundTextAction, addPostTextToFastQAnnotationWithFollowingKey, removePostTextInOutput, multipleMatchesAction,
				noMatchesAction, leadingQualityTrimThreshold, trailingQualityTrimThreshold);

		List<Object> outputSettingsYaml = (List<Object>) root.get(OUTPUT_SETTINGS_KEY);

		OutputFileActionEnum outputFileAction = null;
		String outputFileActionValue = null;

		boolean outputSequenceSearchFindSummary = false;
		boolean outputFastQFindSummary = false;
		boolean outputFindAlignment = false;
		boolean outputFindLog = false;

		for (Object outputSetting : outputSettingsYaml) {
			if (outputSetting instanceof String) {
				String string = (String) outputSetting;
				if (outputFileAction == null && string.equals(FastqToolOutputSettings.OUTPUT_FILES_IN_SAME_DIRECTORY_AS_INPUT_FASTQ_KEY)) {
					outputFileAction = OutputFileActionEnum.OUTPUT_FILES_IN_SAME_DIRECTORY_AS_INPUT_FASTQ;
				} else if (outputFileAction == null && string.equals(FastqToolOutputSettings.OUTPUT_FILES_IN_APPLICATION_DIRECTORY_KEY)) {
					outputFileAction = OutputFileActionEnum.OUTPUT_FILES_IN_APPLICATION_DIRECTORY;
				} else if (string.equals(FastqToolOutputSettings.OUTPUT_SEQUENCE_SEARCH_FIND_SUMMARY_KEY)) {
					outputSequenceSearchFindSummary = true;
				} else if (string.equals(FastqToolOutputSettings.OUTPUT_FASTQ_FIND_SUMMARY_KEY)) {
					outputFastQFindSummary = true;
				} else if (string.equals(FastqToolOutputSettings.OUTPUT_FIND_ALIGNMENT_KEY)) {
					outputFindAlignment = true;
				} else if (string.equals(FastqToolOutputSettings.OUTPUT_FIND_LOG_KEY)) {
					outputFindLog = true;
				} else {
					throw new IllegalStateException("Unrecognized option[" + outputSetting + "] in the OUTPUT section of the settings file[" + settings.getName() + "].");
				}
			} else if (outputSetting instanceof Map<?, ?>) {
				Map<String, String> map = (Map<String, String>) outputSetting;

				if (outputFileAction == null && map.containsKey(FastqToolOutputSettings.OUTPUT_FILES_IN_SUBDIRECTORY_OF_INPUT_FASTQ_KEY)) {
					outputFileAction = OutputFileActionEnum.OUTPUT_FILES_IN_SUBDIRECTORY_OF_INPUT_FASTQ;
					outputFileActionValue = map.get(FastqToolOutputSettings.OUTPUT_FILES_IN_SUBDIRECTORY_OF_INPUT_FASTQ_KEY);
				} else if (outputFileAction == null && map.containsKey(FastqToolOutputSettings.OUTPUT_FILES_IN_SUBDIRECTORY_OF_APPLICATION_DIRECTORY_KEY)) {
					outputFileAction = OutputFileActionEnum.OUTPUT_FILES_IN_SUBDIRECTORY_OF_APPLICATION_DIRECTORY;
					outputFileActionValue = map.get(FastqToolOutputSettings.OUTPUT_FILES_IN_SUBDIRECTORY_OF_APPLICATION_DIRECTORY_KEY);
				} else if (outputFileAction == null && map.containsKey(FastqToolOutputSettings.OUTPUT_FILES_IN_DESIGNATED_DIRECTORY_KEY)) {
					outputFileAction = OutputFileActionEnum.OUTPUT_FILES_IN_DESIGNATED_DIRECTORY;
					outputFileActionValue = map.get(FastqToolOutputSettings.OUTPUT_FILES_IN_DESIGNATED_DIRECTORY_KEY);
				} else {
					throw new IllegalStateException("Unrecognized option[" + outputSetting + "] in the OUTPUT section of the settings file[" + settings.getName() + "].");
				}

			} else {
				throw new IllegalStateException("Unrecognized option[" + outputSetting + "] in the OUTPUT section of the settings file[" + settings.getName() + "].");
			}
		}
		FastqToolOutputSettings outputSettings = new FastqToolOutputSettings(outputFileAction, outputSequenceSearchFindSummary, outputFastQFindSummary, outputFindAlignment, outputFindLog,
				outputFileActionValue);

		FastqToolSettings toolSettings = new FastqToolSettings(findSettings, trimSettings, outputSettings);
		return toolSettings;
	}

	// public static void main(String[] args) throws FileNotFoundException, IOException {
	// File inputFile = new File("C:\\Users\\heilmank\\Desktop\\find\\settings.cfg");
	// FastqToolSettings settings = parseSettings(new InputStreamFactory(inputFile));
	// System.out.println("done.");
	// }

}
