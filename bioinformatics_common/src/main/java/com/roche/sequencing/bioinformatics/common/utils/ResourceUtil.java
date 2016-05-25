package com.roche.sequencing.bioinformatics.common.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;

public class ResourceUtil {

	public static File getResourceAsFile(Class<?> resourceReferenceClass, String resourceName) throws IOException {
		File file = null;
		boolean isInJar = isInJar(resourceReferenceClass);

		if (isInJar) {
			InputStream inputStream = resourceReferenceClass.getResourceAsStream(resourceName);
			if (inputStream != null) {
				File tempDir = Files.createTempDirectory("run", new FileAttribute<?>[0]).toFile();
				file = new File(tempDir, resourceName);
				Files.copy(inputStream, file.getAbsoluteFile().toPath());
				file.setExecutable(true);
				file.deleteOnExit();
				tempDir.deleteOnExit();
			} else {
				throw new IllegalStateException("Unable to find the resource[" + resourceName + "] relative to the reference class[" + resourceReferenceClass + "].");
			}
		} else {
			file = new File(resourceReferenceClass.getResource(resourceName).getFile());
		}
		return file;
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
}
