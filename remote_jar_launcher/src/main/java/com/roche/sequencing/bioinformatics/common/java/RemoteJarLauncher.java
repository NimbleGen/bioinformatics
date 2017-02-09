package com.roche.sequencing.bioinformatics.common.java;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

public class RemoteJarLauncher {

	private final static String LAUNCH_DESCRIPTION_EXTENSION = ".cfg";
	private final static String LAUNCH_DESCRIPTION_PREFIX = ".";

	private static final int STRING_BUILDER_INITIAL_SIZE = 1000;
	public static final int BYTES_PER_KB = 1024;

	public static final String NEWLINE = System.getProperty("line.separator");
	public static final String separator = System.getProperty("file.separator");

	private final static String PATH_TO_JAR_KEY = "path_to_jar";
	private final static String JAR_ARGUMENTS_KEY = "jar_arguments";
	private final static String JVM_ARGUMENTS_KEY = "jvm_arguments";
	private final static String REQUIRED_MIN_JVM_MAJOR_VERSION_KEY = "required_min_jvm_major_version";
	private final static String REQUIRED_MIN_JVM_BIT_DEPTH_KEY = "required_min_jvm_bit_depth";

	public static void main(String[] args) {
		System.out.println("Starting Remote JAR Launcher");
		try {
			copyRemoteAppAndRunLocally(args);
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			System.err.println(t.getMessage());
			if (!isHeadless()) {
				JOptionPane.showMessageDialog(null, t.getMessage(), "Error Launching Application", JOptionPane.WARNING_MESSAGE);
			}

		}
	}

	public static void copyRemoteAppAndRunLocally(String[] args) throws URISyntaxException, IOException, InterruptedException {
		File jarFile = new File(RemoteJarLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		boolean isRunFromJarFile = !jarFile.isDirectory();
		String extension = ".jar";
		if (!isRunFromJarFile) {
			// throw new IllegalStateException("The Remote App Launcher only works as a Jar File.");
			jarFile = new File("C:\\Users\\heilmank\\Desktop\\runSlideViewer.jar");

		}
		File jarDirectory = jarFile.getParentFile();
		String jarFileName = jarFile.getName();
		String jarFileNameWithoutExtension = jarFileName.substring(0, jarFileName.length() - extension.length());
		File launchFile = new File(jarDirectory, LAUNCH_DESCRIPTION_PREFIX + jarFileNameWithoutExtension + LAUNCH_DESCRIPTION_EXTENSION);

		if (!launchFile.exists()) {
			throw new IllegalStateException("Unable to locate the configuration file[" + launchFile.getAbsolutePath() + "].");
		}

		String fileContents = readFileAsString(launchFile);
		String[] lines = fileContents.split("\n");

		Map<String, String> configurationMap = new HashMap<String, String>();
		for (String line : lines) {
			if (!line.startsWith("#")) {
				line = line.replaceAll("\r", "");
				int firstColon = line.indexOf(':');
				if (firstColon > 0) {
					String key = line.substring(0, firstColon);
					String value = line.substring(firstColon + 1, line.length());

					configurationMap.put(key.toLowerCase(), value);
				}
			}
		}

		// Note: This is set to whichever jvm was used to launch this jar
		String pathToJava = System.getProperty("java.home");
		File pathToJavaFile = new File(pathToJava);
		if (pathToJavaFile.getName().equals("jre") && pathToJavaFile.getParentFile().getName().toLowerCase().contains("jdk")) {
			pathToJavaFile = pathToJavaFile.getParentFile();
		}
		JvmDetails jvmDetails = JvmUtil.getJvmDetails(pathToJavaFile);
		System.out.println("jvm details:" + jvmDetails);
		String osName = getOsName();
		if (isMacOsX()) {
			osName = "mac";
		}

		String pathToJar = getConfigurationValue(configurationMap, PATH_TO_JAR_KEY, osName, null);
		String jarArguments = getConfigurationValue(configurationMap, JAR_ARGUMENTS_KEY, osName, "");
		String jvmArguments = getConfigurationValue(configurationMap, JVM_ARGUMENTS_KEY, osName, "");
		String requiredJvmVersionAsString = getConfigurationValue(configurationMap, REQUIRED_MIN_JVM_MAJOR_VERSION_KEY, osName, "");
		int requiredMinJvmVersion = 0;
		if (requiredJvmVersionAsString != "") {
			requiredMinJvmVersion = Integer.parseInt(requiredJvmVersionAsString);
		}
		String requiredMinJvmBitDepthAsString = getConfigurationValue(configurationMap, REQUIRED_MIN_JVM_BIT_DEPTH_KEY, osName, "");
		Integer requiredMinJvmBitDepth = null;
		if (requiredMinJvmBitDepthAsString != "") {
			requiredMinJvmBitDepth = Integer.parseInt(requiredMinJvmBitDepthAsString);
		}

		boolean jvmIsSufficient = (jvmDetails != null && (jvmDetails.getJvmBitDepth().isSufficientToHandleProvidedBitDepth(requiredMinJvmBitDepth))
				&& (requiredMinJvmVersion <= jvmDetails.getVersion().getMajorVersion()));

		if (!jvmIsSufficient) {
			System.out.println("The provided JVM does not meet the applications minimum requirements.  Searching for a " + requiredMinJvmBitDepth + "-bit JVM versioned " + requiredMinJvmVersion
					+ " or greater.");
			List<JvmDetails> sufficientJvms = new ArrayList<JvmDetails>();
			for (JvmDetails currentJvmDetails : JvmUtil.findJvms()) {
				boolean isSufficient = (currentJvmDetails != null && (currentJvmDetails.getJvmBitDepth().isSufficientToHandleProvidedBitDepth(requiredMinJvmBitDepth))
						&& (requiredMinJvmVersion <= currentJvmDetails.getVersion().getMajorVersion()));
				if (isSufficient) {
					sufficientJvms.add(currentJvmDetails);
				}
			}

			if (sufficientJvms.size() == 0) {
				throw new IllegalStateException("Unable to find a " + requiredMinJvmBitDepth + "-bit JVM versioned " + requiredMinJvmVersion + " or greater.");
			}

			Collections.sort(sufficientJvms, new JvmDetailsComparator());
			System.out.println();
			System.out.println("Found the following " + requiredMinJvmBitDepth + "-bit JVMs versioned " + requiredMinJvmVersion + " or greater:");
			for (JvmDetails sufficientJvm : sufficientJvms) {
				System.out.println(sufficientJvm);
			}

			jvmDetails = sufficientJvms.get(0);

			System.out.println();
			System.out.println("Using the following JVM:");
			System.out.println(jvmDetails);
			System.out.println();
		} else {
			System.out.println("The provided JVM meets the applications minimum requirements (a " + requiredMinJvmBitDepth + "-bit JVM versioned " + requiredMinJvmVersion + " or greater).");
		}

		if (pathToJar != null) {
			File jarToExecute = new File(jarDirectory, pathToJar);
			if (!jarToExecute.exists()) {
				throw new IllegalStateException("Unable to locate the jar file[" + jarToExecute.getAbsolutePath() + "] provided in configuration file[" + launchFile.getAbsolutePath() + "].");
			}
			File tempDirectory = getTempDirectory();
			File localJar = new File(tempDirectory, jarToExecute.getName());

			System.out.println("Copying JAR file[" + jarToExecute.getAbsolutePath() + "] to local directory[" + localJar.getAbsolutePath() + "].");
			try {
				copyFileUsingStream(jarToExecute, localJar);
			} catch (IOException e) {
				// cannot write to file because the file is in use
				if (localJar.exists()) {
					if (jarToExecute.length() != localJar.length()) {
						int i = 2;
						while (localJar.exists()) {
							localJar = new File(tempDirectory, jarFileName + "_" + i + ".jar");
							i++;
						}
						copyFileUsingStream(jarToExecute, localJar);
					}
				}
			}
			System.out.println("Done Copying JAR file.");

			// String classpath = System.getProperty("java.class.path");

			List<String> params = new ArrayList<String>();
			params.add(jvmDetails.getJavaExecutablePath().getAbsolutePath());
			for (String param : jvmArguments.split(" ")) {
				if (!param.equals("")) {
					params.add(param);
				}
			}
			params.add("-jar");
			params.add(localJar.getAbsolutePath());
			for (String param : jarArguments.split(" ")) {
				params.add(param);
			}

			System.out.println("Starting new JVM with following command:");
			StringBuilder command = new StringBuilder();
			for (String argument : params) {
				command.append(argument + " ");
			}
			System.out.println(command.toString());

			ProcessBuilder processBuilder = new ProcessBuilder(params);
			processBuilder.directory(jarDirectory);
			// merge the error stream with the input stream
			processBuilder.redirectErrorStream(true);

			try {
				final Process process = processBuilder.start();

				final StringBuilder outputString = new StringBuilder();
				InputStream stdout = process.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
				String line;
				while ((line = reader.readLine()) != null) {
					outputString.append(line);
					System.out.println(line);
				}

				int exitValue = process.waitFor();

				// in some cases the executables need a little nudging to terminate
				// such is the case when a 'press any key...' command is present
				process.getOutputStream().close();

				System.out.println("exit value:" + exitValue);
				String consoleOutputString = outputString.toString();

				if (exitValue != 0) {
					System.err.println("There was a problem launching the application.");
					System.err.println("arguments: " + NEWLINE + toString(params, NEWLINE));
					System.err.println(consoleOutputString);
					if (!isHeadless()) {
						String message = "There was a problem launching the application." + NEWLINE + "Creating a new process with the following arguments: " + NEWLINE + toString(params, NEWLINE)
								+ NEWLINE + toString(splitIntoLines(consoleOutputString, 50), NEWLINE);
						JOptionPane.showMessageDialog(null, message, "Error Launching Application", JOptionPane.WARNING_MESSAGE);
					}

				}
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
	}

	private static class JvmDetailsComparator implements Comparator<JvmDetails> {
		public int compare(JvmDetails o1, JvmDetails o2) {
			int result = o2.getVersion().getMajorVersion() - o1.getVersion().getMajorVersion();

			if (result == 0) {
				if (o1.getJvmType() != o2.getJvmType()) {
					if (o1.getJvmType() == JvmTypeEnum.JDK) {
						result = -1;
					} else {
						result = 1;
					}
				}
			}

			if (result == 0) {
				if (!o1.getVendor().equals(o2.getVendor())) {
					if (o1.getVendor().equals("Oracle Corporation")) {
						result = -1;
					} else {
						result = 1;
					}
				}
			}

			if (result == 0) {
				if (o1.getJvmBitDepth() != o2.getJvmBitDepth()) {
					if (o1.getJvmBitDepth() == JvmBitDepthEnum.BIT_DEPTH_64) {
						result = -1;
					} else {
						result = 1;
					}
				}
			}

			return result;
		}
	}

	public static String[] splitIntoLines(String string, int maxCharactersInALine) {
		List<String> lines = new ArrayList<String>();
		StringBuilder currentLineText = new StringBuilder();
		int currentIndex = 0;
		int indexOfLastSpace = 0;
		int startIndexOfCurrentLine = 0;

		while (currentIndex < string.length()) {
			char currentChar = string.charAt(currentIndex);
			if (Character.isSpaceChar(currentChar)) {
				indexOfLastSpace = currentIndex;
			}
			currentLineText.append(currentChar);
			if ((currentLineText.length() > maxCharactersInALine)) {
				if (indexOfLastSpace == startIndexOfCurrentLine) {
					throw new IllegalStateException("The text can not be split on spaces.");
				}

				lines.add(currentLineText.substring(0, (indexOfLastSpace - startIndexOfCurrentLine)));
				currentIndex = indexOfLastSpace + 1;
				startIndexOfCurrentLine = currentIndex;
				currentLineText = new StringBuilder();
			} else if (currentIndex == (string.length() - 1)) {
				lines.add(currentLineText.toString());
				currentIndex++;
			} else {
				currentIndex++;
			}
		}

		return lines.toArray(new String[0]);
	}

	private static void copyFileUsingStream(File source, File dest) throws IOException {
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(source);
			os = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
		} finally {
			is.close();
			os.close();
		}
	}

	private static String getConfigurationValue(Map<String, String> configurationMap, String key, String osName, String defaultValue) {
		String fullKey = (key + "_" + osName).toLowerCase();
		String value = configurationMap.get(fullKey);
		if (value == null) {
			value = configurationMap.get(key.toLowerCase());
		}
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

	public static String getOsName() {
		return System.getProperty("os.name");
	}

	public static boolean isWindows() {
		return getOsName().startsWith("Windows");
	}

	public static boolean isLinux() {
		return getOsName().startsWith("Linux");
	}

	public static boolean isMacOsX() {
		return getOsName().startsWith("Mac OS X");
	}

	public static <T> String toString(List<T> list, String delimiter) {
		String returnString = "";

		if (list != null && list.size() > 0) {
			StringBuilder returnStringBuilder = new StringBuilder();
			for (Object string : list) {
				returnStringBuilder.append(string + delimiter);
			}
			returnString = returnStringBuilder.substring(0, returnStringBuilder.length() - delimiter.length());
		}
		return returnString;
	}

	public static <T> String toString(T[] strings, String delimiter) {
		String returnString = "";

		if (strings != null && strings.length > 0) {
			StringBuilder returnStringBuilder = new StringBuilder();
			for (Object string : strings) {
				returnStringBuilder.append(string + delimiter);
			}
			returnString = returnStringBuilder.substring(0, returnStringBuilder.length() - delimiter.length());
		}
		return returnString;
	}

	private static String readFileAsString(File file) throws FileNotFoundException, IOException {
		StringBuilder fileData = new StringBuilder(STRING_BUILDER_INITIAL_SIZE);

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			char[] buf = new char[BYTES_PER_KB];
			int numRead = 0;

			while ((numRead = reader.read(buf)) != -1) {
				fileData.append(buf, 0, numRead);
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return fileData.toString();
	}

	public static File getTempDirectory() {
		File tempDirectory = null;
		String property = "java.io.tmpdir";
		String tempDirectoryAsString = System.getProperty(property);
		if (tempDirectoryAsString != null) {
			tempDirectory = new File(tempDirectoryAsString);
		}
		return tempDirectory;
	}

	public static boolean isHeadless() {
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		return env.isHeadlessInstance();
	}

}
