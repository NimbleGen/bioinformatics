package com.roche.sequencing.bioinformatics.common.verification.runs;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;

import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.CheckSumUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;

public class TestPlanRun {

	private final static String DESCRIPTION_KEY = "description";
	private final static String COMMAND_KEY = "command";
	private final static String RESULTS_SUBDIRECTORY_KEY = "resultsSubDirectory";
	private final static String VARIABLES_KEY = "variables";
	private final static String NOTES_KEY = "notes";

	private final static String EXTRA_ARGUMENTS_KEY = "extraArguments";
	private final static String INPUT_ARGUMENT_NAMES_TO_FILE_NAME_REGEX_KEY = "inputArgumentNamesToFileNameRegex";

	private final static String[] VALID_KEYS = new String[] { DESCRIPTION_KEY, COMMAND_KEY, EXTRA_ARGUMENTS_KEY, INPUT_ARGUMENT_NAMES_TO_FILE_NAME_REGEX_KEY, RESULTS_SUBDIRECTORY_KEY, VARIABLES_KEY };

	private final static String RUN_DIRECTORY_VARIABLE_KEY = "RUN_DIR";
	private final static String TEST_PLAN_DIRECTORY_VARIABLE_KEY = "TEST_PLAN_DIR";
	private final static String SYSTEM_TEMP_DIRECTORY_VARIABLE_KEY = "SYSTEM_TEMP_DIR";

	private final File testPlanBaseDirectory;
	private final File runDirectory;
	private final String resultsSubDirectory;
	private final String description;
	private final String command;
	private final String[] extraArguments;
	private final String[] notes;
	private final Map<String, String> variables;
	private final Map<String, List<String>> inputArgumentNamesToFileNameRegex;

	private List<TestPlanRunCheck> checks;
	private List<String> arguments;

	private TestPlanRun(File testPlanBaseDirectory, File runDirectory, String description, String command, String[] extraArguments, Map<String, List<String>> fileNameRegexToInputArgumentNames,
			String resultsSubDirectory, Map<String, String> variables, String[] notes) {
		super();
		this.testPlanBaseDirectory = testPlanBaseDirectory;
		this.runDirectory = runDirectory;
		this.description = description;
		this.command = command;
		this.extraArguments = extraArguments;
		this.inputArgumentNamesToFileNameRegex = fileNameRegexToInputArgumentNames;
		this.resultsSubDirectory = resultsSubDirectory;
		this.variables = new HashMap<String, String>();
		this.notes = notes;
		populateRunVariables(variables);
	}

	private void populateRunVariables(Map<String, String> variables) {
		for (Entry<String, String> entry : System.getenv().entrySet()) {
			this.variables.put(entry.getKey(), entry.getValue());
		}

		this.variables.putAll(variables);

		this.variables.put(RUN_DIRECTORY_VARIABLE_KEY, runDirectory.getAbsolutePath());
		this.variables.put(TEST_PLAN_DIRECTORY_VARIABLE_KEY, testPlanBaseDirectory.getAbsolutePath());
		this.variables.put(SYSTEM_TEMP_DIRECTORY_VARIABLE_KEY, FileUtil.getSystemSpecificTempDirectory().getAbsolutePath());
	}

	public String[] getNotes() {
		return notes;
	}

	private void setChecks(List<TestPlanRunCheck> checks) {
		this.checks = checks;
	}

	public String getDescription() {
		return replaceVariables(description);
	}

	public String getCommand() {
		return replaceVariables(command);
	}

	public String[] getExtraArguments() {
		return extraArguments;
	}

	public List<TestPlanRunCheck> getChecks() {
		return Collections.unmodifiableList(checks);
	}

	public File getRunDirectory() {
		return runDirectory;
	}

	public String getResultsSubDirectory() {
		return resultsSubDirectory;
	}

	public Map<String, String> getVariables() {
		return variables;
	}

	String replaceVariables(String text) {
		String replacedText = text;

		if (replacedText != null) {
			for (Entry<String, String> entry : variables.entrySet()) {
				// The backslash stuff is because replace all treats the backslash
				// as an escape character
				String key = entry.getKey();
				String value = entry.getValue();
				if (key != null && value != null) {
					replacedText = replacedText.replaceAll("(?i)" + Pattern.quote("$" + key + "$"), value.replaceAll("\\\\", "\\\\\\\\"));
				} else {
					System.out.println("key is null.");
				}
			}
		}

		return replacedText;
	}

	private List<String> createArguments() {
		List<String> arguments = new ArrayList<String>();

		arguments.add(command);

		if (inputArgumentNamesToFileNameRegex != null) {
			for (Entry<String, List<String>> entry : inputArgumentNamesToFileNameRegex.entrySet()) {
				String argument = entry.getKey();
				argument = replaceVariables(argument);
				List<String> regexes = entry.getValue();

				File searchDirectory = runDirectory;

				for (String regex : regexes) {
					regex = replaceVariables(regex);
					File matchingFile = FileUtil.getMatchingFileInDirectory(searchDirectory, regex);

					List<String> directoriesSearched = new ArrayList<String>();
					while (matchingFile == null && !searchDirectory.equals(testPlanBaseDirectory)) {
						directoriesSearched.add(searchDirectory.getAbsolutePath());
						searchDirectory = searchDirectory.getParentFile();
						matchingFile = FileUtil.getMatchingFileInDirectory(searchDirectory, regex);
					}

					if (matchingFile == null) {
						throw new IllegalStateException("Unable to find a file in directory(ies)[" + ArraysUtil.toString(directoriesSearched.toArray(new String[0]), ",") + "] matching the regex["
								+ regex + "].");
					} else {
						arguments.add(argument);
						arguments.add(matchingFile.getAbsolutePath());
					}
				}
			}
		}

		if (extraArguments != null) {
			for (String extraArgument : extraArguments) {
				extraArgument = replaceVariables(extraArgument);
				arguments.add(extraArgument);
			}
		}
		return arguments;
	}

	public List<String> getArguments() {
		if (this.arguments == null) {
			this.arguments = createArguments();
		}
		return Collections.unmodifiableList(arguments);
	}

	public static TestPlanRun readFromDirectory(File testPlanBaseDirectory, File testPlanRunDirectory) {
		return readFromDirectory(testPlanBaseDirectory, testPlanRunDirectory, null);
	}

	@SuppressWarnings("unchecked")
	public static TestPlanRun readFromDirectory(File testPlanBaseDirectory, File testPlanRunDirectory, TestPlanRun runSettingsToInherit) {
		TestPlanRun run = null;
		Yaml yaml = new Yaml();

		String[] applicationRunYamlFiles = testPlanRunDirectory.list(new FilenameFilter() {

			@Override
			public boolean accept(File file, String name) {
				return name.toLowerCase().endsWith("run") && new File(file, name).isFile();
			}
		});

		if (applicationRunYamlFiles.length > 1) {
			throw new IllegalStateException("There were multiple[" + applicationRunYamlFiles.length + "] .run files found in the provided test plan run directory[" + testPlanRunDirectory
					+ "] whereas only 1 was expected.");
		}

		if (applicationRunYamlFiles.length == 1) {
			File inputYaml = new File(testPlanRunDirectory, applicationRunYamlFiles[0]);
			try {
				Map<String, Object> root = (Map<String, Object>) yaml.load(FileUtil.readFileAsString(inputYaml));
				List<String> unrecognizedKeys = new ArrayList<String>();
				for (String key : root.keySet()) {
					if (!ArraysUtil.contains(VALID_KEYS, key)) {
						unrecognizedKeys.add(key);
					}
				}

				if (unrecognizedKeys.size() > 0) {
					throw new IllegalArgumentException("The following YAML tags were not recognized: [" + ArraysUtil.toString(unrecognizedKeys.toArray(new String[0]), " ") + "] in the run file["
							+ testPlanRunDirectory.getAbsolutePath() + "].");
				}

				String description = (String) root.get(DESCRIPTION_KEY);
				if (description == null && runSettingsToInherit != null) {
					description = runSettingsToInherit.getDescription();
				}

				String command = (String) root.get(COMMAND_KEY);
				if (command == null && runSettingsToInherit != null) {
					command = runSettingsToInherit.getCommand();
				}

				String resultsSubDirectory = (String) root.get(RESULTS_SUBDIRECTORY_KEY);
				if (resultsSubDirectory == null && runSettingsToInherit != null) {
					resultsSubDirectory = runSettingsToInherit.getResultsSubDirectory();
				}

				String[] extraArguments = null;

				Object extraArgumentsAsObject = root.get(EXTRA_ARGUMENTS_KEY);
				if (extraArgumentsAsObject != null) {
					if (extraArgumentsAsObject instanceof List) {
						List<String> extraArgumentsAsList = (List<String>) root.get(EXTRA_ARGUMENTS_KEY);
						extraArguments = new String[0];
						if (extraArgumentsAsList != null) {
							extraArguments = extraArgumentsAsList.toArray(new String[0]);
						}
					} else {
						extraArguments = new String[] { (String) extraArgumentsAsObject };
					}
				}

				if (extraArguments == null && runSettingsToInherit != null) {
					extraArguments = runSettingsToInherit.getExtraArguments();
				}

				Map<String, List<String>> inputArgumentNamesToFileNameRegex = new HashMap<String, List<String>>();
				if (runSettingsToInherit != null) {
					inputArgumentNamesToFileNameRegex.putAll(runSettingsToInherit.inputArgumentNamesToFileNameRegex);
				}

				Map<?, ?> inputArgumentsMapAsGenericMap = (Map<?, ?>) root.get(INPUT_ARGUMENT_NAMES_TO_FILE_NAME_REGEX_KEY);

				if (inputArgumentsMapAsGenericMap != null) {
					Object firstValue = inputArgumentsMapAsGenericMap.values().iterator().next();

					if (firstValue != null) {
						if (firstValue instanceof String) {
							Map<String, String> currentInputArgumentNamesToFileNameRegex = (Map<String, String>) root.get(INPUT_ARGUMENT_NAMES_TO_FILE_NAME_REGEX_KEY);
							if (currentInputArgumentNamesToFileNameRegex != null) {
								for (Entry<String, String> entry : currentInputArgumentNamesToFileNameRegex.entrySet()) {
									List<String> regexes = new ArrayList<String>();
									regexes.add(entry.getValue());
									inputArgumentNamesToFileNameRegex.put(entry.getKey(), regexes);
								}
							}
						} else if (firstValue instanceof List<?>) {
							Map<String, String> currentInputArgumentNamesToFileNameRegex = (Map<String, String>) root.get(INPUT_ARGUMENT_NAMES_TO_FILE_NAME_REGEX_KEY);
							if (currentInputArgumentNamesToFileNameRegex != null) {
								inputArgumentNamesToFileNameRegex.putAll((Map<String, List<String>>) inputArgumentsMapAsGenericMap);
							}
						} else {
							throw new AssertionError();
						}
					}
				}

				Map<String, String> variablesMap = (Map<String, String>) root.get(VARIABLES_KEY);
				if (variablesMap == null) {
					variablesMap = new HashMap<String, String>();
				}

				if (runSettingsToInherit != null) {
					Map<String, String> inheritedVariables = runSettingsToInherit.getVariables();
					if (inheritedVariables != null) {
						variablesMap.putAll(inheritedVariables);
					}
				}

				String[] notes = TestPlanRunCheck.parseStringOrListYamlNode(root.get(NOTES_KEY));

				run = new TestPlanRun(testPlanBaseDirectory, testPlanRunDirectory, description, command, extraArguments, inputArgumentNamesToFileNameRegex, resultsSubDirectory, variablesMap, notes);
				List<TestPlanRunCheck> checks = TestPlanRunCheck.readFromDirectory(run, testPlanRunDirectory);
				run.setChecks(checks);
			} catch (Exception e) {
				throw new IllegalStateException("Unable to parse yaml file[" + inputYaml.getAbsolutePath() + "].  " + e.getMessage(), e);
			}
		}
		return run;
	}

	public long checkSum() {
		final int prime = 31;
		long result = 1;
		if (checks != null) {
			for (TestPlanRunCheck check : checks) {
				result = prime * result + check.checkSum();
			}
		}
		result = prime * result + ((command == null) ? 0 : CheckSumUtil.checkSum(command));
		result = prime * result + ((description == null) ? 0 : CheckSumUtil.checkSum(description));
		result = prime * result + ((extraArguments == null) ? 0 : CheckSumUtil.checkSum(extraArguments));
		result = prime * result + ((inputArgumentNamesToFileNameRegex == null) ? 0 : CheckSumUtil.checkSum(inputArgumentNamesToFileNameRegex));
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((arguments == null) ? 0 : arguments.hashCode());
		result = prime * result + ((checks == null) ? 0 : checks.hashCode());
		result = prime * result + ((command == null) ? 0 : command.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + Arrays.hashCode(extraArguments);
		result = prime * result + ((inputArgumentNamesToFileNameRegex == null) ? 0 : inputArgumentNamesToFileNameRegex.hashCode());
		result = prime * result + ((runDirectory == null) ? 0 : runDirectory.hashCode());
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
		TestPlanRun other = (TestPlanRun) obj;
		if (arguments == null) {
			if (other.arguments != null)
				return false;
		} else if (!arguments.equals(other.arguments))
			return false;
		if (checks == null) {
			if (other.checks != null)
				return false;
		} else if (!checks.equals(other.checks))
			return false;
		if (command == null) {
			if (other.command != null)
				return false;
		} else if (!command.equals(other.command))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (!Arrays.equals(extraArguments, other.extraArguments))
			return false;
		if (inputArgumentNamesToFileNameRegex == null) {
			if (other.inputArgumentNamesToFileNameRegex != null)
				return false;
		} else if (!inputArgumentNamesToFileNameRegex.equals(other.inputArgumentNamesToFileNameRegex))
			return false;
		if (runDirectory == null) {
			if (other.runDirectory != null)
				return false;
		} else if (!runDirectory.equals(other.runDirectory))
			return false;
		return true;
	}

}
