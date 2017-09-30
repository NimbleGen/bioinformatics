package com.roche.sequencing.bioinformatics.common.java;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class JvmUtil {

	public static final String NEWLINE = System.getProperty("line.separator");

	private final static String MAIN_WINDOWS_OS_DRIVE = System.getenv("SystemDrive");

	private final static String[] COMMON_PLACES_FOR_LINUX_JVM = new String[] { "/usr/bin/", "/usr/java/latest/bin", "/etc/alternatives/java/", "/usr/java/", "/usr/lib64/jvm", "/usr/lib/jvm" };
	private final static String[] COMMON_PLACES_FOR_MAC_OSX_JVM = new String[] { "/Library/Java/JavaVirtualMachines/", "/System/Library/Java/JavaVirtualMachines/" };
	private final static String[] COMMON_PLACES_FOR_WINDOWS_JVM = new String[] { MAIN_WINDOWS_OS_DRIVE + "\\Program Files\\Java\\", MAIN_WINDOWS_OS_DRIVE + "\\Program Files (x86)\\Java\\" };

	private final static DirectoryFilter directoryFilter = new DirectoryFilter();;

	public static JvmDetails[] findJvms() {
		System.out.println(getOsName());
		List<JvmDetails> allJvmDetails = new ArrayList<JvmDetails>();

		List<File> directoriesToSearch = new ArrayList<File>();
		JavaExecutableFileFilter javaExecutableFileFilter = null;
		String environmentVariablesSplitter = null;
		if (isWindows()) {
			for (String commonPlace : COMMON_PLACES_FOR_WINDOWS_JVM) {
				File directory = new File(commonPlace);
				if (directory.exists() && directory.isDirectory()) {
					directoriesToSearch.add(directory);
				}
			}
			javaExecutableFileFilter = new JavaExecutableFileFilter("java.exe");
			environmentVariablesSplitter = ";";
		} else if (isLinux()) {
			for (String commonPlace : COMMON_PLACES_FOR_LINUX_JVM) {
				File directory = new File(commonPlace);
				if (directory.exists() && directory.isDirectory()) {
					directoriesToSearch.add(directory);
				}
			}
			javaExecutableFileFilter = new JavaExecutableFileFilter("java");
			environmentVariablesSplitter = ":";
		} else if (isMacOsX()) {
			for (String commonPlace : COMMON_PLACES_FOR_MAC_OSX_JVM) {
				File directory = new File(commonPlace);
				if (directory.exists() && directory.isDirectory()) {
					directoriesToSearch.add(directory);
				}
			}
			javaExecutableFileFilter = new JavaExecutableFileFilter("java");
			environmentVariablesSplitter = ":";
		} else {
			throw new AssertionError();
		}
		for (String environmentalVariable : new String[] { "PATH", "JAVA_HOME" }) {
			String paths = System.getenv(environmentalVariable);
			if (paths != null) {

				for (String path : paths.split(environmentVariablesSplitter)) {
					File pathFile = new File(path);
					String lowerCasePathFilePath = pathFile.getAbsolutePath().toLowerCase();
					boolean containsBin = lowerCasePathFilePath.contains("bin");
					boolean containsJreOrJdk = lowerCasePathFilePath.contains("jre") || lowerCasePathFilePath.contains("jdk");
					if (pathFile.exists() && containsBin && containsJreOrJdk) {
						directoriesToSearch.add(pathFile);
					}
				}
			}
		}

		Set<File> allCandidates = new HashSet<File>();
		for (File directory : directoriesToSearch) {
			allCandidates.addAll(getCandidates(directory, javaExecutableFileFilter));
		}

		for (File candidate : allCandidates) {
			JvmDetails jvmDetails = getJvmDetails(candidate);
			if (jvmDetails != null) {
				allJvmDetails.add(jvmDetails);
			}
		}

		return allJvmDetails.toArray(new JvmDetails[0]);
	}

	public static JvmDetails getBestSufficientJvm(File initialJavaToCheck, Integer requiredMinJvmBitDepth, Integer requiredMinJvmVersion) {
		JvmDetails jvmDetails;
		if (initialJavaToCheck != null && initialJavaToCheck.exists()) {
			jvmDetails = JvmUtil.getJvmDetails(initialJavaToCheck);
		} else {
			jvmDetails = null;
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
			// Note: This was too verbose
			// System.out.println();
			// System.out.println("Found the following " + requiredMinJvmBitDepth + "-bit JVMs versioned " + requiredMinJvmVersion + " or greater:");
			// for (JvmDetails sufficientJvm : sufficientJvms) {
			// System.out.println(sufficientJvm);
			// }

			jvmDetails = sufficientJvms.get(0);
		}
		return jvmDetails;
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

	private static class JavaExecutableFileFilter implements FileFilter {
		private final String javaExecutableName;

		public JavaExecutableFileFilter(String javaExecutableName) {
			super();
			this.javaExecutableName = javaExecutableName;
		}

		public boolean accept(File pathname) {
			boolean accept = false;
			if (pathname.isDirectory()) {
				String lowerCaseDirectoryName = pathname.getAbsolutePath().toLowerCase();
				boolean isJreOrJdk = lowerCaseDirectoryName.contains("jdk") || lowerCaseDirectoryName.contains("jre");
				if (isJreOrJdk) {
					File binDirectory = new File(pathname, "bin");
					if (binDirectory.exists()) {
						File javaExecutable = new File(binDirectory, javaExecutableName);
						accept = javaExecutable.exists();
					}
				}
			}

			return accept;
		}
	}

	private static class DirectoryFilter implements FileFilter {

		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
	}

	private static Set<File> getCandidates(File directory, JavaExecutableFileFilter javaExecutableFileFilter) {
		Set<File> candidates = new HashSet<File>();
		if (directory != null) {
			File[] files = directory.listFiles(javaExecutableFileFilter);
			if (files != null) {
				for (File javaFile : files) {
					candidates.add(javaFile);
				}
			}

			File[] subDirectories = directory.listFiles(directoryFilter);
			if (subDirectories != null) {
				for (File subDirectory : subDirectories) {
					if (!candidates.contains(subDirectory)) {
						candidates.addAll(getCandidates(subDirectory, javaExecutableFileFilter));
					}
				}
			}
		}
		return candidates;

	}

	public static JvmDetails getJvmDetails(File pathToJreOrJdkFolder) {
		JvmDetails jvmDetails = null;
		if (pathToJreOrJdkFolder.exists()) {
			File pathToJavaExecutable;
			if (isWindows()) {
				pathToJavaExecutable = new File(pathToJreOrJdkFolder, "bin\\java.exe");
			} else {
				pathToJavaExecutable = new File(pathToJreOrJdkFolder, "bin/java");
			}

			try {
				String jvmInspectorOutput = getJvmInspectorOutput(pathToJavaExecutable);
				String[] lines = jvmInspectorOutput.split(NEWLINE);
				if (lines.length == 3) {
					String versionLine = lines[0];
					JavaVersion version = null;
					if (versionLine.startsWith(JavaJvmInspector.JVM_VERSION_KEY)) {
						versionLine = versionLine.replace(JavaJvmInspector.JVM_VERSION_KEY + JavaJvmInspector.DELIMITER, "");
						String[] splitVersion = versionLine.split("\\.");
						if (splitVersion.length == 2) {
							try {
								int majorVersion = Integer.parseInt(splitVersion[1]);
								int minorVersion = 0;
								version = new JavaVersion(majorVersion, minorVersion);
							} catch (NumberFormatException e) {
								System.out.println(e.getMessage());
							}
						}
					}

					String bitDepthLine = lines[1];
					JvmBitDepthEnum bitDepth = null;
					if (bitDepthLine.startsWith(JavaJvmInspector.JVM_BIT_DEPTH_KEY)) {
						bitDepthLine = bitDepthLine.replace(JavaJvmInspector.JVM_BIT_DEPTH_KEY + JavaJvmInspector.DELIMITER, "");
						if (bitDepthLine.equals("64")) {
							bitDepth = JvmBitDepthEnum.BIT_DEPTH_64;
						} else if (bitDepthLine.equals("32")) {
							bitDepth = JvmBitDepthEnum.BIT_DEPTH_32;
						}
					}

					String vendorLine = lines[2];
					String vendor = null;
					if (vendorLine.startsWith(JavaJvmInspector.JVM_VENDOR_KEY)) {
						vendorLine = vendorLine.replace(JavaJvmInspector.JVM_VENDOR_KEY + JavaJvmInspector.DELIMITER, "");
						vendor = vendorLine;
					}

					JvmTypeEnum jvmType = JvmTypeEnum.JRE;
					if (pathToJreOrJdkFolder.getAbsolutePath().toLowerCase().contains("jdk")) {
						jvmType = JvmTypeEnum.JDK;
					}

					if (version != null && bitDepth != null && vendor != null) {

						jvmDetails = new JvmDetails(version, bitDepth, jvmType, vendor, pathToJreOrJdkFolder, pathToJavaExecutable);
					}
				}

			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}

		return jvmDetails;
	}

	// Take from StringUtil
	private static Random RANDOM = new Random();
	// Taken from StringUtil
	private static char[] CHARACTERS = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2',
			'3', '4', '5', '6', '7', '8', '9' };

	// Taken from StringUtil
	public synchronized static String generateRandomString(int length) {
		char[] text = new char[length];
		for (int i = 0; i < length; i++) {
			text[i] = CHARACTERS[RANDOM.nextInt(CHARACTERS.length)];
		}
		return new String(text);

	}

	public static String getJvmInspectorOutput(File pathToJavaExecutable) throws URISyntaxException {
		StringBuilder output = new StringBuilder();

		String path = pathToJavaExecutable.getAbsolutePath();

		ProcessBuilder jvmInspectorProcessBuilder = null;

		if (isInJar(JavaJvmInspector.class)) {
			InputStream inputStream = null;
			try {
				inputStream = JavaJvmInspector.class.getResourceAsStream("JavaJvmInspector.class");
				String classDirectory = JavaJvmInspector.class.getName();
				classDirectory = classDirectory.replace(".", File.separator);
				File tempDirectory = getTempDirectory();

				File outputLocation = new File(tempDirectory, classDirectory + "_" + System.currentTimeMillis() + "_" + generateRandomString(10) + ".class");
				if (!outputLocation.exists()) {
					outputLocation.getParentFile().mkdirs();
				}

				OutputStream outputStream = null;
				try {
					outputStream = new FileOutputStream(outputLocation);
					byte[] buffer = new byte[1024];
					int len;
					while ((len = inputStream.read(buffer)) != -1) {
						outputStream.write(buffer, 0, len);
					}
				} catch (Exception e) {
					throw new IllegalStateException(e.getMessage(), e);
				} finally {
					if (outputStream != null) {
						try {
							outputStream.close();
						} catch (IOException e) {
							throw new IllegalStateException(e.getMessage(), e);
						}
					}
				}
				jvmInspectorProcessBuilder = new ProcessBuilder(path, "-classpath", tempDirectory.getAbsolutePath(), JavaJvmInspector.class.getName());
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						throw new IllegalStateException(e.getMessage(), e);
					}
				}
			}

		} else {
			String classPath = new File(JavaJvmInspector.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
			jvmInspectorProcessBuilder = new ProcessBuilder(path, "-classpath", classPath, JavaJvmInspector.class.getName());
			jvmInspectorProcessBuilder.redirectErrorStream(true);
		}

		try {
			Process process = jvmInspectorProcessBuilder.start();
			InputStream errorStream = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line + NEWLINE);
			}
		} catch (IOException e) {
			output.append(e.getMessage());
		}

		return output.toString();
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

	public static boolean isInJar(Class<?> resourceReferenceClass) {
		boolean isInJar = false;
		try {
			File resourceAsFile = new File(resourceReferenceClass.getProtectionDomain().getCodeSource().getLocation().toURI());
			// if in jar will not be a directory
			isInJar = !resourceAsFile.isDirectory();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return isInJar;
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

}
