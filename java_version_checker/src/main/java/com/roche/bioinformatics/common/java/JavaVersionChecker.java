package com.roche.bioinformatics.common.java;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.swing.JOptionPane;

public class JavaVersionChecker {

	private final static String SECONDARY_MAIN_CLASS_MANIFEST_TAG = "Second-Main-Class";
	private final static String REQUIRED_JAVA_VERSION_MANIFEST_TAG = "Required-Java-Version";
	private final static String DISPLAY_JAVA_VERSION_WARNING_IN_DIALOG_MANIFEST_TAG = "Display-Java-Version-Warning-In-Dialog";

	public static void main(String[] args) {

		String requiredMajorMinorVersionOfJavaAsString = getManifestValue(REQUIRED_JAVA_VERSION_MANIFEST_TAG);
		double requiredMajorMinorJavaVersion = 1.6;
		if (requiredMajorMinorVersionOfJavaAsString != null) {
			requiredMajorMinorJavaVersion = Double.valueOf(requiredMajorMinorVersionOfJavaAsString);
		}

		String mainClass = getManifestValue(SECONDARY_MAIN_CLASS_MANIFEST_TAG);
		if (mainClass == null) {
			throw new IllegalStateException("If you are using Java Version Checker to instantiate your class you must provide a \"" + SECONDARY_MAIN_CLASS_MANIFEST_TAG
					+ "\" tag and value in the manifest file.  So the correct main class is called when the Java Version is correct.");
		}
		mainClass = mainClass.replace("/", ".");

		boolean displayWarningWithDialog = false;
		String displayWarningWithDialogAsString = getManifestValue(DISPLAY_JAVA_VERSION_WARNING_IN_DIALOG_MANIFEST_TAG);
		if (displayWarningWithDialogAsString != null) {
			displayWarningWithDialog = Boolean.parseBoolean(displayWarningWithDialogAsString);
		}

		String versionAsString = System.getProperty("java.specification.version");
		double actualJavaVersionAsDouble = Double.valueOf(versionAsString);

		if (actualJavaVersionAsDouble < requiredMajorMinorJavaVersion) {
			String warning = "This application requires Java Version " + requiredMajorMinorJavaVersion + " or newer.  Please execute the application with an appropriate version of Java.";
			if (displayWarningWithDialog) {
				JOptionPane.showMessageDialog(null, warning, "Incorrect Version of Java", JOptionPane.WARNING_MESSAGE);
			}
			System.err.println(warning);
			System.exit(1);
		}

		Class<?> clazz;
		try {
			clazz = Class.forName(mainClass);
			Class[] argTypes = new Class[] { String[].class };
			Method method = clazz.getDeclaredMethod("main", argTypes);
			method.invoke(null, (Object) args);
		} catch (Exception e) {
			throw new IllegalStateException("Could not instantiate the provided main class[" + mainClass + "]." + e.getMessage());
		}

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

}
