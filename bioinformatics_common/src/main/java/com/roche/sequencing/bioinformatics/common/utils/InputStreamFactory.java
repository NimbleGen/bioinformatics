package com.roche.sequencing.bioinformatics.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class InputStreamFactory {

	private final File file;

	private final String resourceName;
	private final Class<?> relativeResourceClass;

	public InputStreamFactory(File file) {
		super();
		this.file = file;
		this.resourceName = null;
		this.relativeResourceClass = null;
	}

	public InputStreamFactory(Class<?> relativeResourceClass, String resourceName) {
		super();
		this.file = null;
		if (resourceName == null) {
			throw new IllegalStateException("A null value was passed in as a resource name.");
		}
		this.resourceName = resourceName;
		this.relativeResourceClass = relativeResourceClass;
	}

	public InputStream createInputStream() throws FileNotFoundException {
		InputStream inputStream;
		if (file != null) {
			inputStream = new FileInputStream(file);
		} else if (resourceName != null && relativeResourceClass != null) {
			inputStream = relativeResourceClass.getResourceAsStream(resourceName);
		} else {
			throw new AssertionError();
		}
		if (inputStream == null) {
			throw new IllegalStateException("Unable to create an input stream:" + this);
		}

		return inputStream;
	}

	@Override
	public String toString() {
		return "InputStreamFactory [file=" + file + ", resourceName=" + resourceName + ", relativeResourceClass=" + relativeResourceClass + "]";
	}

}
