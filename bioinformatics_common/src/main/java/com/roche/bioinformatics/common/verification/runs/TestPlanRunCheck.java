package com.roche.bioinformatics.common.verification.runs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.yaml.snakeyaml.Yaml;

import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.CheckSumUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.Md5CheckSumUtil;

public class TestPlanRunCheck {

	private final static String CHECK_TYPE_KEY = "checkType";
	private final static String OUTPUT_FILE_REGEX_KEY = "outputFileRegex";
	private final static String RELATIVE_PATH_TO_OUTPUT_FILE_KEY = "relativePathToOutputFile";
	private final static String TEXT_TO_FIND_KEY = "textToFind";
	private final static String MD5SUM_KEY = "md5Sum";
	private final static String RELATIVE_PATH_TO_MATCHING_FILE_KEY = "relativePathToMatchingFile";
	private final static String RELATIVE_PATH_TO_MATCHING_FILE_DIRECTORY_KEY = "relativePathToMatchingFileDirectory";
	private final static String MATCHING_FILE_REGEX_KEY = "matchingFileRegex";
	private final static String ACCEPTANCE_CRITERIA_KEY = "acceptanceCriteria";
	private final static String DESCRIPTION_KEY = "description";
	private final static String REQUIREMENTS_KEY = "requirements";

	private final TestPlanRunCheckTypeEnum checkType;
	private final String[] textToFind;
	private final String outputFileRegex;
	private final String[] relativePathToOutputFile;
	private final String md5Sum;
	private final String relativePathToMatchingFile;
	private final String relativePathToMatchingFileDirectory;
	private final String matchingFileRegex;
	private final String description;
	private final String[] requirements;
	private final String acceptanceCriteria;

	private TestPlanRun parentRun;

	private TestPlanRunCheck(TestPlanRun parentRun, TestPlanRunCheckTypeEnum checkType, String[] textToFind, String outputFileRegex, String[] relativePathToOutputFile, String md5Sum,
			String relativePathToMatchingFile, String relativePathToMatchingFileDirectory, String matchingFileRegex, String description, String[] requirements, String acceptanceCriteria) {
		super();
		this.parentRun = parentRun;
		this.checkType = checkType;
		this.textToFind = textToFind;
		this.outputFileRegex = outputFileRegex;
		this.relativePathToOutputFile = relativePathToOutputFile;
		this.md5Sum = md5Sum;
		this.relativePathToMatchingFile = relativePathToMatchingFile;
		this.relativePathToMatchingFileDirectory = relativePathToMatchingFileDirectory;
		this.matchingFileRegex = matchingFileRegex;
		this.description = description;
		this.acceptanceCriteria = acceptanceCriteria;
		this.requirements = requirements;

		switch (checkType) {
		case CONSOLE_ERRORS_CONTAINS_TEXT:
		case CONSOLE_ERRORS_DOES_NOT_CONTAIN_TEXT:
		case CONSOLE_OUTPUT_DOES_NOT_CONTAIN_TEXT:
		case CONSOLE_OUTPUT_CONTAINS_TEXT:
			if (textToFind == null || textToFind.length == 0) {
				throw new IllegalStateException("The textToFind key/value pair is expected for the check type[" + checkType + "].");
			} else if (outputFileRegex != null || md5Sum != null || relativePathToMatchingFile != null || relativePathToMatchingFileDirectory != null || matchingFileRegex != null) {
				throw new IllegalStateException("Key/value pairs were provided which do not match what is expected for the check type[" + checkType + "].");
			}
			break;
		case OUTPUT_FILE_CONTAINS_TEXT:
			if (textToFind == null || textToFind.length == 0 || outputFileRegex == null || outputFileRegex.isEmpty()) {
				throw new IllegalStateException("The textToFind and outputFileRegex key/value pairs are expected for the check type[" + checkType + "].");
			} else if (md5Sum != null || relativePathToMatchingFile != null || relativePathToMatchingFileDirectory != null || matchingFileRegex != null) {
				throw new IllegalStateException("Key/value pairs were provided which do not match what is expected for the check type[" + checkType + "].");
			}
			break;
		case OUTPUT_FILE_PRESENT:
			if (outputFileRegex == null || outputFileRegex.isEmpty()) {
				throw new IllegalStateException("The outputFileRegex key/value pair is expected for the check type[" + checkType + "].");
			} else if (textToFind != null || md5Sum != null || relativePathToMatchingFile != null || relativePathToMatchingFileDirectory != null || matchingFileRegex != null) {
				throw new IllegalStateException("Key/value pairs were provided which do not match what is expected for the check type[" + checkType + "].");
			}
			break;
		case OUTPUT_FILE_NOT_PRESENT:
			if (outputFileRegex == null || outputFileRegex.isEmpty()) {
				throw new IllegalStateException("The outputFileRegex key/value pair is expected for the check type[" + checkType + "].");
			} else if (textToFind != null || md5Sum != null || relativePathToMatchingFile != null || relativePathToMatchingFileDirectory != null || matchingFileRegex != null) {
				throw new IllegalStateException("Key/value pairs were provided which do not match what is expected for the check type[" + checkType + "].");
			}
			break;
		case OUTPUT_FILE_MATCHES_EXISTING_FILE:
			boolean directoryAndRegexInputs = matchingFileRegex != null && !matchingFileRegex.isEmpty() && relativePathToMatchingFileDirectory != null && !relativePathToMatchingFileDirectory.isEmpty()
					&& outputFileRegex != null && !outputFileRegex.isEmpty();
			boolean fileInput = relativePathToMatchingFile != null && !relativePathToMatchingFile.isEmpty() && outputFileRegex != null && !outputFileRegex.isEmpty();

			if (!directoryAndRegexInputs && !fileInput) {
				throw new IllegalStateException(
						"The relativePathToMatchingFile or relativePathToMatchingFileDirectory and the matchingFileRegex key/value pairs are expected for the check type[" + checkType + "].");
			} else if (directoryAndRegexInputs && (md5Sum != null || textToFind != null || relativePathToMatchingFile != null)) {
				throw new IllegalStateException("Key/value pairs were provided which do not match what is expected for the check type[" + checkType + "].");
			} else if (fileInput && (md5Sum != null || textToFind != null || relativePathToMatchingFileDirectory != null || matchingFileRegex != null)) {
				throw new IllegalStateException("Key/value pairs were provided which do not match what is expected for the check type[" + checkType + "].");
			}
			break;
		case OUTPUT_FILE_MATCHES_MD5SUM:
			if (md5Sum == null || md5Sum.isEmpty() || outputFileRegex == null || outputFileRegex.isEmpty()) {
				throw new IllegalStateException("The md5Sum and outputFileRegex key/value pairs are expected for the check type[" + checkType + "].");
			} else if (textToFind != null || relativePathToMatchingFile != null || relativePathToMatchingFileDirectory != null || matchingFileRegex != null) {
				throw new IllegalStateException("Key/value pairs were provided which do not match what is expected for the check type[" + checkType + "].");
			}
			break;
		default:
			throw new IllegalStateException("The TestPlanRunCheckType[" + checkType + "] is not recognized.");
		}

	}

	public String getDescription() {
		String description = "";

		if (this.description != null) {
			description = parentRun.replaceVariables(this.description);
		} else {
			switch (checkType) {
			case CONSOLE_ERRORS_CONTAINS_TEXT:
				description = "Search for the text['" + ArraysUtil.toString(textToFind, "', '") + "'] in the console errors.";
				break;
			case CONSOLE_ERRORS_DOES_NOT_CONTAIN_TEXT:
				if (isRequirementsPresent()) {
					description = "Verify ";
				} else {
					description = "Confirm ";
				}
				description += "that the console errors does not contain the text['" + ArraysUtil.toString(textToFind, "', '") + "'].";
				break;
			case OUTPUT_FILE_CONTAINS_TEXT:
				if (isRequirementsPresent()) {
					description = "Verify ";
				} else {
					description = "Confirm ";
				}
				description += "that the output file matching the regex[" + parentRun.replaceVariables(outputFileRegex) + "] contains the text['" + ArraysUtil.toString(textToFind, "', '") + "'].";
				break;
			case OUTPUT_FILE_PRESENT:
				if (isRequirementsPresent()) {
					description = "Verify ";
				} else {
					description = "Confirm ";
				}
				description += "that the output file matching the regex[" + parentRun.replaceVariables(outputFileRegex) + "].";

				break;
			case OUTPUT_FILE_NOT_PRESENT:
				if (isRequirementsPresent()) {
					description = "Verify ";
				} else {
					description = "Confirm ";
				}
				description += "that the output file matching the regex[" + parentRun.replaceVariables(outputFileRegex) + "] is NOT present.";
				break;
			case OUTPUT_FILE_DOES_NOT_CONTAIN_TEXT:
				if (isRequirementsPresent()) {
					description = "Verify ";
				} else {
					description = "Confirm ";
				}
				description += "that the output file matching the regex[" + parentRun.replaceVariables(outputFileRegex) + "] does not contain the text['" + ArraysUtil.toString(textToFind, "', '")
						+ "'].";
				break;
			case OUTPUT_FILE_MATCHES_EXISTING_FILE:
				if (isRequirementsPresent()) {
					description = "Verify ";
				} else {
					description = "Confirm ";
				}

				if (relativePathToMatchingFileDirectory != null && !relativePathToMatchingFileDirectory.isEmpty()) {
					description += "that the output file matching the regular expression[" + parentRun.replaceVariables(outputFileRegex)
							+ "] matches the expected result file which matches the regular expression[" + parentRun.replaceVariables(matchingFileRegex) + "] and is in the relative directory["
							+ relativePathToMatchingFileDirectory + "].";
				} else if (relativePathToMatchingFile != null && !relativePathToMatchingFile.isEmpty()) {
					description = "that the output file matching the regular expression[" + parentRun.replaceVariables(outputFileRegex) + "] matches the expected result file ["
							+ relativePathToMatchingFile + "].";
				} else {
					throw new AssertionError();
				}
				break;
			case OUTPUT_FILE_MATCHES_MD5SUM:
				if (isRequirementsPresent()) {
					description = "Verify ";
				} else {
					description = "Confirm ";
				}
				description += "that the output file matching the regex[" + parentRun.replaceVariables(outputFileRegex) + "] has a md5Sum of [" + md5Sum + "].";
				break;
			case CONSOLE_OUTPUT_CONTAINS_TEXT:
				description = "Search for the text['" + ArraysUtil.toString(textToFind, "', '") + "'] in the console output.";
				break;
			case CONSOLE_OUTPUT_DOES_NOT_CONTAIN_TEXT:
				if (isRequirementsPresent()) {
					description = "Verify ";
				} else {
					description = "Confirm ";
				}
				description += "that the console output does not contain the text['" + ArraysUtil.toString(textToFind, "', '") + "'].";
				break;
			default:
				throw new IllegalStateException("The TestPlanRunCheckType[" + checkType + "] is not recognized.");
			}
		}

		return description;
	}

	private boolean isRequirementsPresent() {
		return requirements.length > 0;
	}

	public String[] getRequirements() {
		return requirements;
	}

	public String getAcceptanceCriteria() {
		String acceptanceCriteria = this.acceptanceCriteria;

		if (acceptanceCriteria != null && !acceptanceCriteria.isEmpty()) {
			parentRun.replaceVariables(acceptanceCriteria);
		} else {
			switch (checkType) {
			case CONSOLE_ERRORS_CONTAINS_TEXT:
				acceptanceCriteria = "The console errors contains the text \"" + ArraysUtil.toString(textToFind, "\", \"") + "\".";
				break;
			case CONSOLE_ERRORS_DOES_NOT_CONTAIN_TEXT:
				acceptanceCriteria = "The console errors does not contain the text \"" + ArraysUtil.toString(textToFind, "\", \"") + "\".";
				break;
			case OUTPUT_FILE_CONTAINS_TEXT:
				acceptanceCriteria = "The output file matching the regular expression \"" + parentRun.replaceVariables(outputFileRegex) + "\" contains the text '"
						+ ArraysUtil.toString(textToFind, "\", \"") + "'.";
				break;
			case OUTPUT_FILE_PRESENT:
				acceptanceCriteria = "The output file matching the regular expression \"" + parentRun.replaceVariables(outputFileRegex) + "\" is present.";
				break;
			case OUTPUT_FILE_NOT_PRESENT:
				acceptanceCriteria = "The output file matching the regular expression \"" + parentRun.replaceVariables(outputFileRegex) + "\" is not present.";
				break;
			case OUTPUT_FILE_DOES_NOT_CONTAIN_TEXT:
				acceptanceCriteria = "The output file matching the regular expression \"" + parentRun.replaceVariables(outputFileRegex) + "\" does not contain the text '"
						+ ArraysUtil.toString(textToFind, "\", \"") + "'.";
				break;
			case OUTPUT_FILE_MATCHES_EXISTING_FILE:
				if (relativePathToMatchingFileDirectory != null && !relativePathToMatchingFileDirectory.isEmpty()) {
					acceptanceCriteria = "The output file matching the regular expression \"" + parentRun.replaceVariables(outputFileRegex)
							+ "\" matches the expected result file which matches the regular expression \"" + parentRun.replaceVariables(matchingFileRegex) + "\" and is in the relative directory["
							+ relativePathToMatchingFileDirectory + "].";
				} else if (relativePathToMatchingFile != null && !relativePathToMatchingFile.isEmpty()) {
					acceptanceCriteria = "The output file matching the regular expression \"" + parentRun.replaceVariables(outputFileRegex) + "\" matches the expected result file ["
							+ relativePathToMatchingFile + "].";
				} else {
					throw new AssertionError();
				}
				break;
			case OUTPUT_FILE_MATCHES_MD5SUM:
				acceptanceCriteria = "The md5Sum of the output file matching the regular expression[" + parentRun.replaceVariables(outputFileRegex) + "] is [" + md5Sum + "].";
				break;
			case CONSOLE_OUTPUT_CONTAINS_TEXT:
				acceptanceCriteria = "The console output contains the text \"" + ArraysUtil.toString(textToFind, "\", \"") + "\".";
				break;
			case CONSOLE_OUTPUT_DOES_NOT_CONTAIN_TEXT:
				acceptanceCriteria = "The console output does not contain the text \"" + ArraysUtil.toString(textToFind, "\", \"") + "\".";
				break;
			default:
				throw new IllegalStateException("The TestPlanRunCheckType[" + checkType + "] is not recognized.");
			}
		}

		return acceptanceCriteria;
	}

	public TestPlanRunCheckResult check(RunResults runResults) {

		boolean success = false;
		String resultsDescription = "";

		switch (checkType) {
		case CONSOLE_ERRORS_CONTAINS_TEXT:
			List<String> foundText = new ArrayList<String>();
			List<String> notFoundText = new ArrayList<String>();
			for (String text : textToFind) {
				if (runResults.getConsoleErrors().contains(text)) {
					foundText.add(text);
				} else {
					notFoundText.add(text);
				}
			}

			success = foundText.size() == textToFind.length;
			if (success) {
				resultsDescription = "The text, \"" + ArraysUtil.toString(foundText.toArray(new String[0]), "\", \"") + "\", was found in the console errors.  The console errors have been saved at ["
						+ new File(runResults.getOutputDirectory(), TestPlan.CONSOLE_ERRORS_FILE_NAME).getAbsolutePath() + "] for your convenience.";
			} else {
				resultsDescription = "The text, \"" + ArraysUtil.toString(notFoundText.toArray(new String[0]), "\", \"")
						+ "\", was not found in the console errors.  The console errors have been saved at ["
						+ new File(runResults.getOutputDirectory(), TestPlan.CONSOLE_ERRORS_FILE_NAME).getAbsolutePath() + "] for your convenience.";
			}
			break;
		case CONSOLE_ERRORS_DOES_NOT_CONTAIN_TEXT:
			List<String> foundText2 = new ArrayList<String>();
			List<String> notFoundText2 = new ArrayList<String>();
			for (String text : textToFind) {
				if (runResults.getConsoleErrors().contains(text)) {
					foundText2.add(text);
				} else {
					notFoundText2.add(text);
				}
			}

			success = foundText2.isEmpty();
			if (success) {
				resultsDescription = "The text, \"" + ArraysUtil.toString(notFoundText2.toArray(new String[0]), "\", \"")
						+ "\", was not found in the console errors.  The console errors have been saved at ["
						+ new File(runResults.getOutputDirectory(), TestPlan.CONSOLE_ERRORS_FILE_NAME).getAbsolutePath() + "] for your convenience.";
			} else {
				resultsDescription = "The text, \"" + ArraysUtil.toString(foundText2.toArray(new String[0]), "\", \"") + "\", was found in the console errors.  The console errors have been saved at ["
						+ new File(runResults.getOutputDirectory(), TestPlan.CONSOLE_ERRORS_FILE_NAME).getAbsolutePath() + "] for your convenience.";
			}
			break;
		case OUTPUT_FILE_CONTAINS_TEXT:
			File outputFileForTextSearch = FileUtil.getMatchingFileInDirectory(runResults.getOutputDirectory(), parentRun.replaceVariables(outputFileRegex));

			if (outputFileForTextSearch != null) {
				try {
					Map<String, Integer> foundTextLineNumbers = new HashMap<String, Integer>();
					List<String> notFoundText5 = new ArrayList<String>();
					for (String text : textToFind) {
						int lineNumber = FileUtil.findLineNumberOfFirstOccurrenceOfText(outputFileForTextSearch, text);
						if (lineNumber > 0) {
							foundTextLineNumbers.put(text, lineNumber);
						} else {
							notFoundText5.add(text);
						}
					}

					success = notFoundText5.size() == 0;
					if (success) {
						StringBuilder textAndLineNumbers = new StringBuilder();
						for (Entry<String, Integer> entry : foundTextLineNumbers.entrySet()) {
							textAndLineNumbers.append("\"" + entry.getKey() + "\"[Line: " + entry.getValue() + "], ");
						}
						resultsDescription = "The text, " + textAndLineNumbers.substring(0, textAndLineNumbers.length() - 1) + ", was found in the output file["
								+ outputFileForTextSearch.getAbsolutePath() + "].";
					} else {
						resultsDescription = "The text, \"" + ArraysUtil.toString(notFoundText5.toArray(new String[0]), "\", \"") + "\", was NOT found in the output file["
								+ outputFileForTextSearch.getAbsolutePath() + "].";
					}
				} catch (FileNotFoundException e) {
					resultsDescription = "Unable to open the output file[" + outputFileForTextSearch.getAbsolutePath() + "] for finding the following text: \"" + textToFind + "\".  " + e.getMessage();
				}
			} else {
				resultsDescription = "Unable to locate a single file in output directory[" + runResults.getOutputDirectory() + "] matching the regex, \"" + parentRun.replaceVariables(outputFileRegex)
						+ "\".";
			}
			break;
		case OUTPUT_FILE_PRESENT:
			File directory = runResults.getOutputDirectory();
			if (relativePathToOutputFile != null && relativePathToOutputFile.length > 0) {
				String[] relativePathToOutputFileWithReplacedVariables = new String[relativePathToOutputFile.length];
				for (int i = 0; i < relativePathToOutputFile.length; i++) {
					relativePathToOutputFileWithReplacedVariables[i] = parentRun.replaceVariables(relativePathToOutputFile[i]);
				}

				directory = FileUtil.getMatchingDirectoryRelativeToBaseDirectory(directory, relativePathToOutputFileWithReplacedVariables);
			}
			List<File> presentOutputFiles = FileUtil.getMatchingFilesInDirectory(directory, parentRun.replaceVariables(outputFileRegex));
			success = presentOutputFiles != null;
			if (success) {
				String[] filePaths = new String[presentOutputFiles.size()];
				for (int i = 0; i < presentOutputFiles.size(); i++) {
					filePaths[i] = presentOutputFiles.get(i).getAbsolutePath();
				}
				resultsDescription = "Located the file(s)[" + ArraysUtil.toString(filePaths, ", ") + "] in the output directory[" + runResults.getOutputDirectory() + "] matching the regex, \""
						+ parentRun.replaceVariables(outputFileRegex) + "\".";
			} else {
				resultsDescription = "Unable to locate a single file in output directory[" + runResults.getOutputDirectory() + "] matching the regex, \"" + outputFileRegex + "\".";
			}
			break;
		case OUTPUT_FILE_NOT_PRESENT:
			File directoryForNotPresent = runResults.getOutputDirectory();
			if (relativePathToOutputFile != null && relativePathToOutputFile.length > 0) {
				String[] relativePathToOutputFileWithReplacedVariables = new String[relativePathToOutputFile.length];
				for (int i = 0; i < relativePathToOutputFile.length; i++) {
					relativePathToOutputFileWithReplacedVariables[i] = parentRun.replaceVariables(relativePathToOutputFile[i]);
				}

				directoryForNotPresent = FileUtil.getMatchingDirectoryRelativeToBaseDirectory(directoryForNotPresent, relativePathToOutputFileWithReplacedVariables);
			}
			List<File> notPresentOutputFiles = FileUtil.getMatchingFilesInDirectory(directoryForNotPresent, parentRun.replaceVariables(outputFileRegex));
			success = notPresentOutputFiles.size() == 0;
			if (success) {
				resultsDescription = "Succesfully could not locate a file in the output directory[" + runResults.getOutputDirectory() + "] matching the regex, \""
						+ parentRun.replaceVariables(outputFileRegex) + "\".";
			} else {
				String[] filePaths = new String[notPresentOutputFiles.size()];
				for (int i = 0; i < notPresentOutputFiles.size(); i++) {
					filePaths[i] = notPresentOutputFiles.get(i).getAbsolutePath();
				}
				resultsDescription = "Was able to locate a single file[" + ArraysUtil.toString(filePaths, ", ") + "] in the output directory[" + runResults.getOutputDirectory()
						+ "] matching the regex, \"" + outputFileRegex + "\".";
			}
			break;
		case OUTPUT_FILE_DOES_NOT_CONTAIN_TEXT:
			File outputFileForNotContainedTextSearch = FileUtil.getMatchingFileInDirectory(runResults.getOutputDirectory(), parentRun.replaceVariables(outputFileRegex));

			if (outputFileForNotContainedTextSearch != null) {
				try {
					Map<String, Integer> foundTextLineNumbers = new HashMap<String, Integer>();
					List<String> notFoundText7 = new ArrayList<String>();
					for (String text : textToFind) {
						int lineNumber = FileUtil.findLineNumberOfFirstOccurrenceOfText(outputFileForNotContainedTextSearch, text);
						if (lineNumber > 0) {
							foundTextLineNumbers.put(text, lineNumber);
						} else {
							notFoundText7.add(text);
						}
					}

					success = foundTextLineNumbers.size() == 0;
					if (success) {
						resultsDescription = "The text, \"" + ArraysUtil.toString(notFoundText7.toArray(new String[0]), "\", \"") + "\", was not found in the output file["
								+ outputFileForNotContainedTextSearch.getAbsolutePath() + "].";
					} else {
						StringBuilder textAndLineNumbers = new StringBuilder();
						for (Entry<String, Integer> entry : foundTextLineNumbers.entrySet()) {
							textAndLineNumbers.append("\"" + entry.getKey() + "\"[Line: " + entry.getValue() + "], ");
						}
						resultsDescription = "The text, " + textAndLineNumbers.substring(0, textAndLineNumbers.length() - 1) + ", was found in the output file["
								+ outputFileForNotContainedTextSearch.getAbsolutePath() + "].";
					}
				} catch (FileNotFoundException e) {
					resultsDescription = "Unable to open the output file[" + outputFileForNotContainedTextSearch.getAbsolutePath() + "] for finding the following text: \"" + textToFind + "\".  "
							+ e.getMessage();
				}
			} else {
				resultsDescription = "Unable to locate a single file in output directory[" + runResults.getOutputDirectory() + "] matching the regex, \"" + parentRun.replaceVariables(outputFileRegex)
						+ "\".";
			}
			break;
		case OUTPUT_FILE_MATCHES_EXISTING_FILE:
			File outputFile = FileUtil.getMatchingFileInDirectory(runResults.getOutputDirectory(), parentRun.replaceVariables(outputFileRegex));
			String existingFileRegex = null;
			File existingFile = null;
			File existingFileDirectory = null;
			if (relativePathToMatchingFileDirectory != null && !relativePathToMatchingFileDirectory.isEmpty()) {
				existingFileDirectory = new File(runResults.getTestDirectory(), relativePathToMatchingFileDirectory);
				existingFileRegex = parentRun.replaceVariables(matchingFileRegex);
				existingFile = FileUtil.getMatchingFileInDirectory(existingFileDirectory, existingFileRegex);
			} else if (relativePathToMatchingFile != null && !relativePathToMatchingFile.isEmpty()) {
				existingFile = new File(runResults.getTestDirectory(), relativePathToMatchingFile);
			} else {
				throw new AssertionError();
			}

			if (outputFile == null) {
				resultsDescription = "Unable to locate a single file in output directory[" + runResults.getOutputDirectory() + "] matching the regex, \"" + parentRun.replaceVariables(outputFileRegex)
						+ "\".";
			} else if (existingFile == null) {
				resultsDescription = "Unable to locate an existing file in the directory[" + existingFileDirectory + "] matching the regex, \"" + existingFileRegex + "\".";
			} else if (!existingFile.exists()) {
				resultsDescription = "Unable to locate the existing file at [" + existingFile.getAbsolutePath() + "].";
			} else {

				try {
					success = FileUtil.filesContentsAreEqual(outputFile, existingFile, true);
					if (success) {
						resultsDescription = "The contents of the output file[" + outputFile + "] match the existing file[" + existingFile.getAbsolutePath() + "].";
					} else {
						resultsDescription = "The contents of the output file[" + outputFile + "] DO NOT match the existing file[" + existingFile.getAbsolutePath() + "].";
					}
				} catch (IOException e) {
					resultsDescription = "Unable to compare output file[" + outputFile.getAbsolutePath() + "] with matching file[" + existingFile.getAbsolutePath() + "].  " + e.getMessage();
				}
			}

			break;
		case OUTPUT_FILE_MATCHES_MD5SUM:
			File matchingOutputFile = FileUtil.getMatchingFileInDirectory(runResults.getOutputDirectory(), parentRun.replaceVariables(outputFileRegex));

			if (matchingOutputFile != null) {
				try {
					String calculatedMd5Sum = Md5CheckSumUtil.md5sum(matchingOutputFile);
					success = md5Sum.equals(calculatedMd5Sum);
					if (success) {
						resultsDescription = "The calculatedmd5Sum[" + calculatedMd5Sum + "] on file[" + matchingOutputFile.getAbsolutePath() + "] matches the expected md5sum[" + md5Sum + "].";
					} else {
						resultsDescription = "The calculatedmd5Sum[" + calculatedMd5Sum + "] on file[" + matchingOutputFile.getAbsolutePath() + "] DOES NOTmatch the expected md5sum[" + md5Sum + "].";
					}
				} catch (IOException e) {
					resultsDescription = "Unable to calculate the md5Sum on file[" + matchingOutputFile.getAbsolutePath() + "].  " + e.getMessage();
				}
			} else {
				resultsDescription = "Unable to locate a single file in output directory[" + runResults.getOutputDirectory() + "] matching the regex[" + parentRun.replaceVariables(outputFileRegex)
						+ "].";
			}

			break;
		case CONSOLE_OUTPUT_CONTAINS_TEXT:
			List<String> foundText3 = new ArrayList<String>();
			List<String> notFoundText3 = new ArrayList<String>();
			for (String text : textToFind) {
				if (runResults.getConsoleOutput().contains(text)) {
					foundText3.add(text);
				} else {
					notFoundText3.add(text);
				}
			}

			success = notFoundText3.isEmpty();
			if (success) {
				resultsDescription = "The text, \"" + ArraysUtil.toString(foundText3.toArray(new String[0]), "\", \"") + "\", was found in the console output.  The console output has been saved at ["
						+ new File(runResults.getOutputDirectory(), TestPlan.CONSOLE_ERRORS_FILE_NAME).getAbsolutePath() + "] for your convenience.";
			} else {
				resultsDescription = "The text, \"" + ArraysUtil.toString(notFoundText3.toArray(new String[0]), "\", \"")
						+ "\", was NOT found in the console output.  The console output has been saved at ["
						+ new File(runResults.getOutputDirectory(), TestPlan.CONSOLE_ERRORS_FILE_NAME).getAbsolutePath() + "] for your convenience.";
			}
			break;
		case CONSOLE_OUTPUT_DOES_NOT_CONTAIN_TEXT:
			List<String> foundText4 = new ArrayList<String>();
			List<String> notFoundText4 = new ArrayList<String>();
			for (String text : textToFind) {
				if (runResults.getConsoleOutput().contains(text)) {
					foundText4.add(text);
				} else {
					notFoundText4.add(text);
				}
			}

			success = foundText4.isEmpty();
			if (success) {
				resultsDescription = "The text, \"" + ArraysUtil.toString(notFoundText4.toArray(new String[0]), "\", \"")
						+ "\", was not found in the console output.  The console output has been saved at ["
						+ new File(runResults.getOutputDirectory(), TestPlan.CONSOLE_ERRORS_FILE_NAME).getAbsolutePath() + "] for your convenience.";
			} else {
				resultsDescription = "The text, \"" + ArraysUtil.toString(foundText4.toArray(new String[0]), "\", \"") + "\", was found in the console output.  The console output has been saved at ["
						+ new File(runResults.getOutputDirectory(), TestPlan.CONSOLE_ERRORS_FILE_NAME).getAbsolutePath() + "] for your convenience.";
			}
			break;
		default:
			throw new IllegalStateException("The TestPlanRunCheckType[" + checkType + "] is not recognized.");
		}
		return new TestPlanRunCheckResult(success, resultsDescription);
	}

	@SuppressWarnings("unchecked")
	public static List<TestPlanRunCheck> readFromDirectory(TestPlanRun parentRun, File testPlanRunDirectory) {
		List<TestPlanRunCheck> checks = new ArrayList<TestPlanRunCheck>();
		Yaml yaml = new Yaml();

		String[] checkFileNames = testPlanRunDirectory.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith("check");
			}
		});

		for (String checkFileName : checkFileNames) {
			File inputYaml = new File(testPlanRunDirectory, checkFileName);
			try {
				Map<String, Object> root = (Map<String, Object>) yaml.load(FileUtil.readFileAsString(inputYaml));

				TestPlanRunCheckTypeEnum checkType = TestPlanRunCheckTypeEnum.valueOf((String) root.get(CHECK_TYPE_KEY));
				Object textToFindAsObject = root.get(TEXT_TO_FIND_KEY);
				String[] textToFind = null;
				if (textToFindAsObject != null) {
					if (textToFindAsObject instanceof String) {
						textToFind = new String[1];
						textToFind[0] = (String) textToFindAsObject;
					} else if (textToFindAsObject instanceof List) {
						List<String> textToFindAsList = (List<String>) textToFindAsObject;
						textToFind = textToFindAsList.toArray(new String[0]);
					} else {
						throw new IllegalStateException("Unrecognized format for key[" + TEXT_TO_FIND_KEY + "] in check file[" + inputYaml.getAbsolutePath() + "].");
					}
				}

				String outputFileRegex = (String) root.get(OUTPUT_FILE_REGEX_KEY);
				String[] relativePathToOutputFile = null;
				Object relativePathToOutputFileAsObject = root.get(RELATIVE_PATH_TO_OUTPUT_FILE_KEY);
				if (relativePathToOutputFileAsObject != null) {
					if (relativePathToOutputFileAsObject instanceof String) {
						String relativePathToOutputFileAsString = (String) relativePathToOutputFileAsObject;
						if (relativePathToOutputFileAsString.contains("\\")) {
							relativePathToOutputFile = relativePathToOutputFileAsString.split("\\\\");
						} else if (relativePathToOutputFileAsString.contains("/")) {
							relativePathToOutputFile = relativePathToOutputFileAsString.split("/");
						} else {
							relativePathToOutputFile = new String[] { relativePathToOutputFileAsString };
						}
					} else if (relativePathToOutputFileAsObject instanceof String[]) {
						relativePathToOutputFile = (String[]) relativePathToOutputFileAsObject;
					} else {
						throw new IllegalStateException("Unrecognized format for key[" + RELATIVE_PATH_TO_OUTPUT_FILE_KEY + "] in check file[" + inputYaml.getAbsolutePath() + "].");
					}
				}
				String md5Sum = (String) root.get(MD5SUM_KEY);
				String relativePathToMatchingFile = (String) root.get(RELATIVE_PATH_TO_MATCHING_FILE_KEY);
				String relativePathToMatchingFileDirectory = (String) root.get(RELATIVE_PATH_TO_MATCHING_FILE_DIRECTORY_KEY);
				String matchingFileRegex = (String) root.get(MATCHING_FILE_REGEX_KEY);
				String acceptanceCriteria = (String) root.get(ACCEPTANCE_CRITERIA_KEY);
				String description = (String) root.get(DESCRIPTION_KEY);

				List<String> requirementsAsList = (List<String>) root.get(REQUIREMENTS_KEY);
				String[] requirements = new String[0];
				if (requirementsAsList != null) {
					requirements = requirementsAsList.toArray(new String[0]);
				}

				checks.add(new TestPlanRunCheck(parentRun, checkType, textToFind, outputFileRegex, relativePathToOutputFile, md5Sum, relativePathToMatchingFile, relativePathToMatchingFileDirectory,
						matchingFileRegex, description, requirements, acceptanceCriteria));
			} catch (Exception e) {
				throw new IllegalStateException(e.getMessage() + " File[" + inputYaml.getAbsolutePath() + "].", e);
			}
		}
		return checks;
	}

	public long checkSum() {
		final long prime = 31;
		long result = 1;
		result = prime * result + ((acceptanceCriteria == null) ? 0 : CheckSumUtil.checkSum(acceptanceCriteria));
		result = prime * result + ((checkType == null) ? 0 : CheckSumUtil.checkSum(checkType.name()));
		result = prime * result + ((description == null) ? 0 : CheckSumUtil.checkSum(description));
		result = prime * result + ((matchingFileRegex == null) ? 0 : CheckSumUtil.checkSum(matchingFileRegex));
		result = prime * result + ((md5Sum == null) ? 0 : CheckSumUtil.checkSum(md5Sum));
		result = prime * result + ((outputFileRegex == null) ? 0 : CheckSumUtil.checkSum(outputFileRegex));
		result = prime * result + ((relativePathToMatchingFile == null) ? 0 : CheckSumUtil.checkSum(relativePathToMatchingFile));
		result = prime * result + ((relativePathToMatchingFileDirectory == null) ? 0 : CheckSumUtil.checkSum(relativePathToMatchingFileDirectory));
		result = prime * result + ((requirements == null) ? 0 : CheckSumUtil.checkSum(requirements));
		result = prime * result + ((textToFind == null) ? 0 : CheckSumUtil.checkSum(textToFind));
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((acceptanceCriteria == null) ? 0 : acceptanceCriteria.hashCode());
		result = prime * result + ((checkType == null) ? 0 : checkType.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((matchingFileRegex == null) ? 0 : matchingFileRegex.hashCode());
		result = prime * result + ((md5Sum == null) ? 0 : md5Sum.hashCode());
		result = prime * result + ((outputFileRegex == null) ? 0 : outputFileRegex.hashCode());
		result = prime * result + ((relativePathToMatchingFile == null) ? 0 : relativePathToMatchingFile.hashCode());
		result = prime * result + ((relativePathToMatchingFileDirectory == null) ? 0 : relativePathToMatchingFileDirectory.hashCode());
		result = prime * result + Arrays.hashCode(requirements);
		result = prime * result + Arrays.hashCode(textToFind);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TestPlanRunCheck other = (TestPlanRunCheck) obj;
		if (acceptanceCriteria == null) {
			if (other.acceptanceCriteria != null)
				return false;
		} else if (!acceptanceCriteria.equals(other.acceptanceCriteria))
			return false;
		if (checkType != other.checkType)
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (matchingFileRegex == null) {
			if (other.matchingFileRegex != null)
				return false;
		} else if (!matchingFileRegex.equals(other.matchingFileRegex))
			return false;
		if (md5Sum == null) {
			if (other.md5Sum != null)
				return false;
		} else if (!md5Sum.equals(other.md5Sum))
			return false;
		if (outputFileRegex == null) {
			if (other.outputFileRegex != null)
				return false;
		} else if (!outputFileRegex.equals(other.outputFileRegex))
			return false;
		if (relativePathToMatchingFile == null) {
			if (other.relativePathToMatchingFile != null)
				return false;
		} else if (!relativePathToMatchingFile.equals(other.relativePathToMatchingFile))
			return false;
		if (relativePathToMatchingFileDirectory == null) {
			if (other.relativePathToMatchingFileDirectory != null)
				return false;
		} else if (!relativePathToMatchingFileDirectory.equals(other.relativePathToMatchingFileDirectory))
			return false;
		if (!Arrays.equals(requirements, other.requirements))
			return false;
		if (!Arrays.equals(textToFind, other.textToFind))
			return false;
		return true;
	}
}
