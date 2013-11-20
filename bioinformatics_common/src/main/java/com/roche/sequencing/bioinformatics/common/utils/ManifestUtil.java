package com.roche.sequencing.bioinformatics.common.utils;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManifestUtil {

	private static Logger logger = LoggerFactory.getLogger(ManifestUtil.class);

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
					logger.warn(e.getMessage());
				}
			}
		} catch (IOException e1) {
			logger.warn(e1.getMessage());
		}

		return manifestValue;
	}
}
