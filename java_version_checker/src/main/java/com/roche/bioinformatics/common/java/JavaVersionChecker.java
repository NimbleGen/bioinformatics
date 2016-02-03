/*
 *    Copyright 2016 Roche NimbleGen Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.roche.bioinformatics.common.java;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class JavaVersionChecker {

	private final static Preferences preferences = Preferences.userRoot().node(JavaVersionChecker.class.getName());
	private final static String LAST_JVM_PREFERENCE = "jvm_path";

	private final static String SECONDARY_MAIN_CLASS_MANIFEST_TAG = "Second-Main-Class";
	private final static String REQUIRED_JAVA_VERSION_MANIFEST_TAG = "Required-Java-Version";
	private final static String REQUIRED_JAVA_BIT_TAG = "Required-Bit_JVM";
	private final static String DISPLAY_JAVA_VERSION_WARNING_IN_DIALOG_MANIFEST_TAG = "Display-Java-Version-Warning-In-Dialog";

	private final static String DEFAULT_LINUX_APPLICATIONS_DIRECTORY = "/usr/lib/jvm/";
	private final static String DEFAULT_WINDOWS_APPLICATIONS_DIRECTORY = "C:/Program Files/";

	public static void main(String[] args) {

		String requiredMajorMinorVersionOfJavaAsString = getManifestValue(REQUIRED_JAVA_VERSION_MANIFEST_TAG);
		double requiredMajorMinorJavaVersion = 1.6;
		if (requiredMajorMinorVersionOfJavaAsString != null) {
			requiredMajorMinorJavaVersion = Double.valueOf(requiredMajorMinorVersionOfJavaAsString);
		}

		String requiredJavaBitAsString = getManifestValue(REQUIRED_JAVA_BIT_TAG);
		int requiredJavaBit = 32;
		if (requiredJavaBitAsString != null) {
			requiredJavaBit = Integer.valueOf(requiredJavaBitAsString);
		}

		String mainClass = getManifestValue(SECONDARY_MAIN_CLASS_MANIFEST_TAG);
		if (mainClass == null) {
			throw new IllegalStateException("If you are using Java Version Checker to instantiate your class you must provide a \"" + SECONDARY_MAIN_CLASS_MANIFEST_TAG
					+ "\" tag and value in the manifest file so the correct main class is called when the Java Version is correct.");
		}
		mainClass = mainClass.replace("/", ".");

		boolean displayWarningWithDialog = false;
		String displayWarningWithDialogAsString = getManifestValue(DISPLAY_JAVA_VERSION_WARNING_IN_DIALOG_MANIFEST_TAG);
		if (displayWarningWithDialogAsString != null) {
			displayWarningWithDialog = Boolean.parseBoolean(displayWarningWithDialogAsString);
		}

		String versionAsString = System.getProperty("java.specification.version");
		double actualJavaVersionAsDouble = 1.5;
		try {
			actualJavaVersionAsDouble = Double.valueOf(versionAsString);
		} catch (NumberFormatException e) {
			System.err.println("Could not detect the version of the JVM[" + versionAsString + "] so using 1.5.");
		}

		boolean javaVersionIsInadequate = (actualJavaVersionAsDouble < requiredMajorMinorJavaVersion);

		String JVMBitAsString = System.getProperty("sun.arch.data.model");
		int actualJavaBitAsInteger = 32;
		try {
			actualJavaBitAsInteger = Integer.valueOf(JVMBitAsString);
		} catch (NumberFormatException e) {
			System.err.println("Could not detect the bit depth of the JVM[" + JVMBitAsString + "] so using 32.");
		}
		boolean javaJvmBitsAreInadequate = (actualJavaBitAsInteger < requiredJavaBit);

		boolean shouldLoadMainClass = !javaVersionIsInadequate && !javaJvmBitsAreInadequate;
		int javaExitNumber = 0;

		boolean launchedsuccessfully = false;

		if (shouldLoadMainClass) {
			Class<?> clazz;
			try {
				clazz = Class.forName(mainClass);
				Class<?>[] argTypes = new Class[] { String[].class };
				Method method = clazz.getDeclaredMethod("main", argTypes);
				method.invoke(null, (Object) args);
				launchedsuccessfully = true;
			} catch (Exception e) {
				throw new IllegalStateException("Could not instantiate the provided main class[" + mainClass + "]." + e.getMessage());
			}
		} else {

			String lastJvm = preferences.get(LAST_JVM_PREFERENCE, null);

			if (lastJvm != null) {

				// load using this jvm
				try {
					launch(new File(lastJvm), args, mainClass);
					launchedsuccessfully = true;
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, e.getMessage(), "Invalid JVM", JOptionPane.WARNING_MESSAGE);
					launchedsuccessfully = false;
				}

			}

			if (!launchedsuccessfully) {

				javaExitNumber = 1;

				String baseMessage = "";

				if (javaVersionIsInadequate) {
					baseMessage += "This application requires Java Version " + requiredMajorMinorJavaVersion + " or newer and found Java version[" + actualJavaVersionAsDouble + "].  ";
				}

				if (javaJvmBitsAreInadequate) {
					baseMessage += "This application requires a " + requiredJavaBit + " bit JVM or higher.  You are currently running a " + actualJavaBitAsInteger + " bit JVM.  ";
				}

				if (displayWarningWithDialog && !isHeadless()) {

					Boolean is64Bit = is64Bit();
					if (is64Bit != null && !is64Bit && requiredJavaBit == 64) {
						// just error out since an appropriate JVM will not be available on this OS
						String warning = baseMessage + "It appears as though you are using a 32-bit OS, which means you will not be able to execute this application on this system.";
						JOptionPane.showMessageDialog(null, warning, "Invalid JVM", JOptionPane.WARNING_MESSAGE);
					} else {

						String message = baseMessage + "\nWould you like to locate an appropriate JVM (version:" + requiredMajorMinorVersionOfJavaAsString + "+ and " + requiredJavaBit + "-bit+)?";

						Object[] options = { "Yes", "Close" };
						final int selection = JOptionPane.showOptionDialog(null, message, "Invalid JVM", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);

						if (selection == 0) {
							File jvmDirectory = null;
							JFileChooser fileChooser = new JFileChooser();
							fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
							fileChooser.setApproveButtonText("Open");

							fileChooser.setDialogTitle("Select JVM Bin Directory");
							String instructions = "<html>Select a JVM bin\\ Directory where:<br> -JVM version is " + requiredMajorMinorVersionOfJavaAsString + "+<br> -JVM is " + requiredJavaBit
									+ "-bit+</html>";
							File applicationsDirectory = null;
							if (isWindows()) {
								applicationsDirectory = new File(DEFAULT_WINDOWS_APPLICATIONS_DIRECTORY);
							} else if (isLinux()) {
								applicationsDirectory = new File(DEFAULT_LINUX_APPLICATIONS_DIRECTORY);
							}

							File defaultDirectory = null;
							if (applicationsDirectory != null) {
								File javaDirectory = new File(applicationsDirectory, "Java/");
								if (javaDirectory.exists()) {
									defaultDirectory = javaDirectory;
								} else {
									javaDirectory = new File(applicationsDirectory, "java/");
									if (javaDirectory.exists()) {
										defaultDirectory = javaDirectory;
									}
								}

								if (defaultDirectory == null) {
									defaultDirectory = applicationsDirectory;
								}
							} else {
								defaultDirectory = new File(".");
							}
							fileChooser.setCurrentDirectory(defaultDirectory);

							fileChooser.setAccessory(new JLabel(instructions));

							int returnValue = fileChooser.showOpenDialog(null);

							if (returnValue == JFileChooser.APPROVE_OPTION) {
								jvmDirectory = fileChooser.getSelectedFile();

								boolean jvmIsAdequate = testJVM(jvmDirectory, requiredJavaBit, requiredMajorMinorJavaVersion);
								if (jvmIsAdequate) {
									preferences.put(LAST_JVM_PREFERENCE, jvmDirectory.getAbsolutePath());
									try {
										launch(jvmDirectory, args, mainClass);
										launchedsuccessfully = true;
									} catch (IOException e) {
										e.printStackTrace();
									}
								} else {
									JOptionPane.showMessageDialog(null, "The provided JVM is not valid.", "Invalid JVM", JOptionPane.WARNING_MESSAGE);
								}
							}
						}
					}

				} else {
					String warning = baseMessage + "Please execute the application with an appropriate version of Java.";
					System.err.println(warning);
				}
			}
		}

		if (!launchedsuccessfully) {
			System.exit(javaExitNumber);
		}

	}

	public static void launch(File pathToJvmBinDirectory, String[] arguments, String mainClass) throws IOException {
		String path = new File(pathToJvmBinDirectory, "java").getAbsolutePath();

		String jarPath = new java.io.File(JavaVersionChecker.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();

		String[] commands = new String[arguments.length + 4];
		int index = 0;
		commands[index] = path;
		index++;
		commands[index] = "-cp";
		index++;
		commands[index] = jarPath;
		index++;
		for (String argument : arguments) {
			commands[index] = argument;
			index++;
		}
		commands[index] = mainClass;
		index++;
		ProcessBuilder processBuilder = new ProcessBuilder(commands);
		processBuilder.start();
	}

	public static boolean testJVM(File pathToJvmBinDirectory, int jvmBitRequirement, double jvmVersionRequirement) {
		boolean success = false;
		String path = new File(pathToJvmBinDirectory, "java").getAbsolutePath();
		ProcessBuilder processBuilder = new ProcessBuilder(path, "-D" + jvmBitRequirement, "-version");
		try {
			Process process = processBuilder.start();
			InputStream errorStream = process.getErrorStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
			String line;
			boolean versionLineFound = false;
			while (!versionLineFound && (line = reader.readLine()) != null) {
				if (line.contains("java version")) {
					versionLineFound = true;
					success = line.contains("" + jvmVersionRequirement);
				}
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			success = false;
		}

		return success;
	}

	/**
	 * @param manifestKey
	 * @return the value within the manifest file associated with the provided key and null if no manifest file exists or the key is not found within the manifest file.
	 */
	public static String getManifestValue(String manifestKey) {
		String manifestValue = null;
		try {
			Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
			while (manifestValue == null && resources.hasMoreElements()) {
				try {
					Manifest manifest = new Manifest(resources.nextElement().openStream());
					Attributes manifestAttributes = manifest.getMainAttributes();
					manifestValue = manifestAttributes.getValue(manifestKey);
				} catch (IOException e) {
				}
			}
		} catch (IOException e1) {
		}

		return manifestValue;
	}

	/**
	 * @return null if cannot be determined
	 */
	public static Boolean is64Bit() {
		Boolean is64bit = null;
		if (isWindows()) {
			if (System.getProperty("os.name").contains("Windows")) {
				is64bit = (System.getenv("ProgramFiles(x86)") != null);
			} else {
				is64bit = (System.getProperty("os.arch").indexOf("64") != -1);
			}
		}
		return is64bit;
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

	public static boolean isHeadless() {
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		return env.isHeadlessInstance();
	}

}
