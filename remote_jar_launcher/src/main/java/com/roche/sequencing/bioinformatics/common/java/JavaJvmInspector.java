package com.roche.sequencing.bioinformatics.common.java;

public class JavaJvmInspector {

	private final static String JVM_BIT_DEPTH_PROPERTY = "sun.arch.data.model";
	private final static String JVM_SPEC_VERSION_PROPERTY = "java.specification.version";
	private final static String JVM_VERSION_PROPERTY = "java.version ";
	private final static String JVM_VENDOR_PROPERTY = "java.vendor";

	public final static String JVM_BIT_DEPTH_KEY = "JVM_BIT_DEPTH";
	public final static String JVM_VERSION_KEY = "JVM_VERSION";
	public final static String JVM_VENDOR_KEY = "JVM_VENDOR";

	private final static String UNIDENTIFIED_VERSION = "1.5";
	private final static String UNIDENTIFIED_BIT_DEPTH = "32";
	private final static String UNIDENTIFIED_VENDOR = "unknown";

	public final static String DELIMITER = ":";

	/**
	 * This class via a is called via a Java Process using the current JVM from within JvmUtil.getJvmInspectorOutput and produces output which is then parsed by said method to discover JVM details.
	 */

	public static void main(String[] args) {
		outputJvmDetails();
	}

	public static void outputJvmDetails() {
		String versionAsString = System.getProperty(JVM_VERSION_PROPERTY);
		if (versionAsString != null) {
			System.out.println(JVM_VERSION_KEY + DELIMITER + versionAsString);
		} else {
			versionAsString = System.getProperty(JVM_SPEC_VERSION_PROPERTY);
			if (versionAsString != null) {
				System.out.println(JVM_VERSION_KEY + DELIMITER + versionAsString);
			} else {
				System.out.println(JVM_VERSION_KEY + DELIMITER + UNIDENTIFIED_VERSION);
			}
		}

		String bitDepthAsString = System.getProperty(JVM_BIT_DEPTH_PROPERTY);
		if (bitDepthAsString != null) {
			System.out.println(JVM_BIT_DEPTH_KEY + DELIMITER + bitDepthAsString);
		} else {
			System.out.println(JVM_BIT_DEPTH_KEY + DELIMITER + UNIDENTIFIED_BIT_DEPTH);
		}

		String vendorAsString = System.getProperty(JVM_VENDOR_PROPERTY);
		if (vendorAsString != null) {
			System.out.println(JVM_VENDOR_KEY + DELIMITER + vendorAsString);
		} else {
			System.out.println(JVM_VENDOR_KEY + DELIMITER + UNIDENTIFIED_VENDOR);
		}

	}
}
