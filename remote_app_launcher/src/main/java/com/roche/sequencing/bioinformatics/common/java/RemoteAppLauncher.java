package com.roche.sequencing.bioinformatics.common.java;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

public class RemoteAppLauncher {

	private final static String LAUNCH_DESCRIPTION_EXTENSION = ".cfg";
	private final static String LAUNCH_DESCRIPTION_PREFIX = ".";

	private static final int STRING_BUILDER_INITIAL_SIZE = 1000;
	public static final int BYTES_PER_KB = 1024;

	public static final String NEWLINE = System.getProperty("line.separator");
	public static final String separator = System.getProperty("file.separator");

	private final static String PATH_TO_JAR_KEY = "path_to_jar";
	private final static String PATH_TO_JAVA_KEY = "path_to_java";
	private final static String JAR_ARGUMENTS_KEY = "jar_arguments";
	private final static String JVM_ARGUMENTS_KEY = "jvm_arguments";

	public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
		coptyRemoteAppAndRunLocally(args);
	}

	public static void coptyRemoteAppAndRunLocally(String[] args) throws URISyntaxException, IOException, InterruptedException {
		File jarFile = new File(RemoteAppLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		boolean isRunFromJarFile = !jarFile.isDirectory();
		if (!isRunFromJarFile) {
			jarFile = new File("C://Users/heilmank/Desktop/runSlideViewer.jar");
		}
		File jarDirectory = jarFile.getParentFile();
		String jarFileName = jarFile.getName();
		String jarFileNameWithoutExtension = jarFileName.substring(0, jarFileName.length() - 4);
		File launchFile = new File(jarDirectory, LAUNCH_DESCRIPTION_PREFIX + jarFileNameWithoutExtension + LAUNCH_DESCRIPTION_EXTENSION);
		String fileContents = readFileAsString(launchFile);
		String[] lines = fileContents.split("\n");

		Map<String, String> configurationMap = new HashMap<>();
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

		String defaultPathToJava = System.getProperty("java.home") + separator + "bin" + separator + "java";

		String osName = getOsName();
		if (isMacOsX()) {
			osName = "mac";
		}

		String pathToJar = getConfigurationValue(configurationMap, PATH_TO_JAR_KEY, osName, null);
		String pathToJava = getConfigurationValue(configurationMap, PATH_TO_JAVA_KEY, osName, defaultPathToJava);
		String jarArguments = getConfigurationValue(configurationMap, JAR_ARGUMENTS_KEY, osName, "");
		String jvmArguments = getConfigurationValue(configurationMap, JVM_ARGUMENTS_KEY, osName, "");

		if (pathToJar != null) {
			File jarToExecutre = new File(jarDirectory, pathToJar);
			if (!jarToExecutre.exists()) {
				throw new IllegalStateException("Unable to locate the jar file[" + jarToExecutre.getAbsolutePath() + "] provided in configuration file[" + launchFile.getAbsolutePath() + "].");
			}
			File tempDirectory = getTempDirectory();
			File localJar = new File(tempDirectory, jarToExecutre.getName());

			try {
				Files.copy(jarToExecutre.toPath(), localJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				// cannot write to file because the file is in use
				if (localJar.exists()) {
					if (!FileUtils.contentEquals(jarToExecutre, localJar)) {
						int i = 2;
						while (localJar.exists()) {
							localJar = new File(tempDirectory, jarFileName + "_" + i + ".jar");
							i++;
						}
						Files.copy(jarToExecutre.toPath(), localJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
				}

			}

			// String classpath = System.getProperty("java.class.path");

			List<String> params = new ArrayList<>();
			params.add(pathToJava);
			for (String param : jvmArguments.split(" ")) {
				if (!param.isEmpty()) {
					params.add(param);
				}
			}
			params.add("-jar");
			params.add(localJar.getAbsolutePath());
			for (String param : jarArguments.split(" ")) {
				params.add(param);
			}
			ProcessBuilder processBuilder = new ProcessBuilder(params);
			processBuilder.directory(jarDirectory);
			processBuilder.redirectErrorStream(true);

			try {
				Process process = processBuilder.start();

				StreamListener outputStreamListener = new StreamListener(process.getInputStream());
				StreamListener errorStreamListener = new StreamListener(process.getErrorStream());

				// in some cases the executables need a little nudging to terminate
				// such is the case when a 'press any key...' command is present
				process.getOutputStream().close();

				int exitValue = process.waitFor();

				String consoleOutputString = outputStreamListener.getString();
				String consoleErrorsString = errorStreamListener.getString();

				if (exitValue != 0) {
					System.out.println("Creating a new process with the following arguments: " + toString(params, ", "));
					System.out.println("Output: " + consoleOutputString);
					System.out.println("Errors: " + consoleErrorsString);
					System.out.println("Exit Status: " + exitValue);
				}
			} catch (IOException | InterruptedException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
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

	private static String readFileAsString(File file) throws FileNotFoundException, IOException {
		StringBuilder fileData = new StringBuilder(STRING_BUILDER_INITIAL_SIZE);

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			char[] buf = new char[BYTES_PER_KB];
			int numRead = 0;

			while ((numRead = reader.read(buf)) != -1) {
				fileData.append(buf, 0, numRead);
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

	private static class StreamListener {
		private final StringBuilder string;

		public StreamListener(final InputStream inputStream) {
			string = new StringBuilder();
			new Thread(new Runnable() {
				public void run() {
					try (BufferedReader sc = new BufferedReader(new InputStreamReader(inputStream))) {
						String line = null;
						while ((line = sc.readLine()) != null) {
							string.append(line + NEWLINE);
						}
					} catch (IOException e) {
						throw new IllegalStateException(e.getMessage(), e);
					}
				}
			}).start();
		}

		public String getString() {
			return string.toString();
		}
	}

}
