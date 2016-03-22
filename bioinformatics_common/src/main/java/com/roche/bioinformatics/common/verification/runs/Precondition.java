package com.roche.bioinformatics.common.verification.runs;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.yaml.snakeyaml.Yaml;

import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.CheckSumUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.JVMUtil;
import com.roche.sequencing.bioinformatics.common.utils.OSUtil;
import com.roche.sequencing.bioinformatics.common.utils.Version;

public class Precondition {

	private final static int BYTES_PER_MEGABYTE = 1000000;
	private final static int MEGABYTES_PER_GIGABYTE = 1000;

	private final PreconditionEnum preconditionEnum;
	private final String[] values;

	public Precondition(PreconditionEnum preconditionEnum, String[] values) {
		if (preconditionEnum == null) {
			throw new IllegalArgumentException("The provided variable preconditionEnum is null.");
		}
		this.preconditionEnum = preconditionEnum;
		this.values = values;
	}

	public String getDescription() {
		String description = null;

		switch (preconditionEnum) {
		case OS:
			if (values.length > 1) {
				description = "The test plan will be run on one of the following Operating Systems: [" + ArraysUtil.toString(values, ", ") + "].";
			} else if (values.length == 1) {
				description = "The test plan will be run on the " + values[0] + " Operating System.";
			}
			break;
		case OS_BIT_DEPTH:
			if (values.length == 1) {
				description = "The test plan will be run on a " + values[0] + "-bit Operating System.";
			} else {
				description = "The test plan will be run on [" + ArraysUtil.toString(values, "-bit, ") + "] Operating Systems.";
			}
			break;
		case MIN_JAVA_VERSION:
			description = "The test plan will be run using a Java Virtual Machine of Version[" + values[0] + "] or greater.";
			break;
		case MAX_JAVA_VERSION:
			description = "The test plan will be run using a Java Virtual Machine of Version[" + values[0] + "] or less.";
			break;
		case REQUIRED_STORAGE:
			description = "The test plan will be run on a system with at least [" + values[0] + "] of storage in the output directory.";
			break;
		default:
			throw new AssertionError();
		}

		return description;
	}

	public void checkPrecondition(File outputDirectory, File pathToJvmBinDirectory) {
		switch (preconditionEnum) {
		case OS:
			boolean matchFoundForOs = false;
			valueLoop: for (String value : values) {
				if (value.toLowerCase().equals("windows")) {
					matchFoundForOs = OSUtil.isWindows();
				} else if (value.toLowerCase().equals("mac")) {
					matchFoundForOs = OSUtil.isMacOsX();
				} else if (value.toLowerCase().equals("linux")) {
					matchFoundForOs = OSUtil.isLinux();
				} else {
					throw new IllegalStateException(
							"An unrecognized value[" + value + "] was provided for the key[" + preconditionEnum + "].  Acceptable values for key[" + preconditionEnum + "] are [windows,mac,linux]");
				}
				if (matchFoundForOs) {
					break valueLoop;
				}
			}

			if (!matchFoundForOs) {
				throw new IllegalStateException("The precondition OS: [" + ArraysUtil.toString(values, ", ") + "] was not met.  The OS of the current system is [" + OSUtil.getOsName() + "].");
			}

			break;
		case OS_BIT_DEPTH:
			Boolean matchFoundForOsBitDepth = false;
			valueLoop: for (String value : values) {
				if (value.toLowerCase().equals("64")) {
					matchFoundForOsBitDepth = OSUtil.is64Bit();
				} else if (value.toLowerCase().equals("32")) {
					matchFoundForOsBitDepth = OSUtil.is32Bit();
				} else {
					throw new IllegalStateException(
							"An unrecognized value[" + value + "] was provided for the key[" + preconditionEnum + "].  Acceptable values for key[" + preconditionEnum + "] are [32,64]");
				}
				if (matchFoundForOsBitDepth != null && matchFoundForOsBitDepth) {
					break valueLoop;
				}
			}

			if (matchFoundForOsBitDepth == null || !matchFoundForOsBitDepth) {
				throw new IllegalStateException(
						"The precondition OS_BIT_DEPTH: [" + ArraysUtil.toString(values, ", ") + "] was not met.  The OS Bit Depth of the current system is [" + OSUtil.getOsBits() + "].");
			}

			break;
		case MIN_JAVA_VERSION:
			if (values.length > 1) {
				throw new IllegalStateException(
						"Too many values provided for key[" + preconditionEnum + "].  One value was expected whereas the following values were provided:[" + ArraysUtil.toString(values, ", ") + "].");
			}

			Version minVersion = Version.fromString(values[0]);
			Version actualVersionForMin = JVMUtil.getJavaVersion(pathToJvmBinDirectory);
			if (actualVersionForMin == null) {
				throw new IllegalStateException("Unable to determine Java version from the provided JVM bin directory[" + pathToJvmBinDirectory + "].");
			}

			if (minVersion.compareTo(actualVersionForMin) > 0) {
				throw new IllegalStateException("The actual JVM version[" + actualVersionForMin + "] is less than the provided value[" + minVersion + "] for the key[" + preconditionEnum + "].");
			}
			break;
		case MAX_JAVA_VERSION:
			if (values.length > 1) {
				throw new IllegalStateException(
						"Too many values provided for key[" + preconditionEnum + "].  One value was expected whereas the following values were provided:[" + ArraysUtil.toString(values, ", ") + "].");
			}

			Version maxVersion = Version.fromString(values[0]);
			Version actualVersionForMax = JVMUtil.getJavaVersion(pathToJvmBinDirectory);

			if (actualVersionForMax == null) {
				throw new IllegalStateException("Unable to determine Java version from the provided JVM bin directory[" + pathToJvmBinDirectory + "].");
			}

			if (maxVersion.compareTo(actualVersionForMax) < 0) {
				throw new IllegalStateException("The actual JVM version[" + actualVersionForMax + "] is greater than the provided value[" + maxVersion + "] for the key[" + preconditionEnum + "].");
			}

			break;
		case REQUIRED_STORAGE:
			if (values.length > 1) {
				throw new IllegalStateException(
						"Too many values provided for key[" + preconditionEnum + "].  One value was expected whereas the following values were provided:[" + ArraysUtil.toString(values, ", ") + "].");
			}

			String value = values[0];
			double sizeInMb = getSizeInMb(value);

			long availableSpaceInMb = outputDirectory.getUsableSpace() / BYTES_PER_MEGABYTE;

			if ((double) availableSpaceInMb < sizeInMb) {
				throw new IllegalStateException("The space available[" + sizeInMb + "MB] in the output directory[" + outputDirectory.getAbsolutePath() + "] is less than the provided value[" + value
						+ "] for the key[" + preconditionEnum + "].");
			}

			break;
		default:
			throw new AssertionError();
		}
	}

	private static double getSizeInMb(String sizeText) {
		double sizeInMb = 0;

		String lowerCaseSizeText = sizeText.toLowerCase();

		try {
			if (lowerCaseSizeText.endsWith("mb")) {
				lowerCaseSizeText = lowerCaseSizeText.replace("mb", "");
				sizeInMb = Double.parseDouble(lowerCaseSizeText);
			} else if (lowerCaseSizeText.endsWith("gb")) {
				lowerCaseSizeText = lowerCaseSizeText.replace("mb", "");
				double sizeInGb = Double.parseDouble(lowerCaseSizeText);
				sizeInMb = sizeInGb * MEGABYTES_PER_GIGABYTE;
			} else {
				throw new IllegalStateException("Unrecognized format[" + sizeText + "] for key[" + PreconditionEnum.REQUIRED_STORAGE + "].");
			}
		} catch (NumberFormatException e) {
			throw new IllegalStateException("Unrecognized format[" + sizeText + "] for key[" + PreconditionEnum.REQUIRED_STORAGE + "].");
		}

		return sizeInMb;
	}

	public long checkSum() {
		final int prime = 31;
		long result = 1;
		result = prime * result + ((preconditionEnum == null) ? 0 : CheckSumUtil.checkSum(preconditionEnum.name()));
		result = prime * result + CheckSumUtil.checkSum(values);
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
		Precondition other = (Precondition) obj;
		if (preconditionEnum != other.preconditionEnum)
			return false;
		if (!Arrays.equals(values, other.values))
			return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	public static List<Precondition> readFromDirectory(File testPlanDirectory) {
		List<Precondition> preconditions = new ArrayList<Precondition>();
		Yaml yaml = new Yaml();

		String[] preconditionNames = testPlanDirectory.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith("preconditions");
			}
		});

		for (String preconditionFileName : preconditionNames) {
			File inputYaml = new File(testPlanDirectory, preconditionFileName);
			try {
				Map<String, Object> root = (Map<String, Object>) yaml.load(FileUtil.readFileAsString(inputYaml));

				for (Entry<String, Object> entry : root.entrySet()) {

					PreconditionEnum preconditionEnum = PreconditionEnum.valueOf(entry.getKey());
					if (preconditionEnum == null) {
						throw new IllegalStateException("The key value[" + entry.getKey() + "] found in the precondition file[" + inputYaml.getAbsolutePath() + "] is not a valid precondition key.");
					}

					String[] values = null;
					Object value = entry.getValue();
					if (value instanceof List) {
						List<Object> valueAsList = (List<Object>) value;
						values = new String[valueAsList.size()];
						for (int i = 0; i < valueAsList.size(); i++) {
							values[i] = valueAsList.get(i).toString();
						}
					} else {
						String valueAsString = value.toString();
						values = new String[] { valueAsString };
					}

					preconditions.add(new Precondition(preconditionEnum, values));
				}

			} catch (Exception e) {
				String message = "Problems parsing file[" + inputYaml.getAbsolutePath() + "].";
				if (e.getMessage() != null) {
					message += "  " + e.getMessage();
				}
				throw new IllegalStateException(message, e);
			}
		}
		return preconditions;
	}

}
