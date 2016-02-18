package com.roche.bioinformatics.common.verification.runs;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.yaml.snakeyaml.Yaml;

import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.CheckSumUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;

public class TestPlanRun {

	private final File runDirectory;
	private final String description;
	private final String command;
	private final String[] extraArguments;
	private final Map<String, String> inputArgumentNamesToFileNameRegex;

	private final List<TestPlanRunCheck> checks;
	private final List<String> arguments;

	private final static String DESCRIPTION_KEY = "description";
	private final static String COMMAND_KEY = "command";

	private final static String EXTRA_ARGUMENTS_KEY = "extraArguments";
	private final static String INPUT_ARGUMENT_NAMES_TO_FILE_NAME_REGEX_KEY = "inputArgumentNamesToFileNameRegex";

	private final static String[] VALID_KEYS = new String[] { DESCRIPTION_KEY, COMMAND_KEY, EXTRA_ARGUMENTS_KEY, INPUT_ARGUMENT_NAMES_TO_FILE_NAME_REGEX_KEY };

	private TestPlanRun(File runDirectory, String description, String command, String[] extraArguments, Map<String, String> fileNameRegexToInputArgumentNames, List<TestPlanRunCheck> checks) {
		super();
		this.runDirectory = runDirectory;
		this.description = description;
		this.command = command;
		this.extraArguments = extraArguments;
		this.inputArgumentNamesToFileNameRegex = fileNameRegexToInputArgumentNames;
		this.checks = checks;
		this.arguments = createArguments();
	}

	public String getDescription() {
		return description;
	}

	public String getCommand() {
		return command;
	}

	public String[] getExtraArguments() {
		return extraArguments;
	}

	public Map<String, String> getInputArgumentNameToRegexMap() {
		return inputArgumentNamesToFileNameRegex;
	}

	public List<TestPlanRunCheck> getChecks() {
		return Collections.unmodifiableList(checks);
	}

	public File getRunDirectory() {
		return runDirectory;
	}

	private List<String> createArguments() {
		List<String> arguments = new ArrayList<String>();

		arguments.add(command);

		if (inputArgumentNamesToFileNameRegex != null) {
			for (Entry<String, String> entry : inputArgumentNamesToFileNameRegex.entrySet()) {
				String argument = entry.getKey();
				String regex = entry.getValue();

				File matchingFile = FileUtil.getMatchingFileInDirectory(runDirectory, regex);

				if (matchingFile == null) {
					throw new IllegalStateException("Unable to find a file in directory[" + runDirectory.getAbsolutePath() + "] matching the regex[" + regex + "].");
				} else {
					arguments.add(argument);
					arguments.add(matchingFile.getAbsolutePath());
				}
			}
		}

		if (extraArguments != null) {
			for (String extraArgument : extraArguments) {
				arguments.add(extraArgument);
			}
		}
		return arguments;
	}

	public List<String> getArguments() {
		return Collections.unmodifiableList(arguments);
	}

	@SuppressWarnings("unchecked")
	public static TestPlanRun readFromDirectory(File testPlanRunDirectory) {
		TestPlanRun run = null;
		Yaml yaml = new Yaml();

		String[] applicationRunYamlFiles = testPlanRunDirectory.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith("run");
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
				String command = (String) root.get(COMMAND_KEY);

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

				Map<String, String> inputArgumentNamesToFileNameRegex = (Map<String, String>) root.get(INPUT_ARGUMENT_NAMES_TO_FILE_NAME_REGEX_KEY);

				List<TestPlanRunCheck> checks = TestPlanRunCheck.readFromDirectory(testPlanRunDirectory);

				run = new TestPlanRun(testPlanRunDirectory, description, command, extraArguments, inputArgumentNamesToFileNameRegex, checks);
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
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
