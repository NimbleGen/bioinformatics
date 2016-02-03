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
