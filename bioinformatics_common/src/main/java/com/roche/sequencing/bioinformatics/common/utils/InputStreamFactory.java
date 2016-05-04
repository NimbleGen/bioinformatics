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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

public class InputStreamFactory implements IInputStreamFactory {

	private final File file;

	private final String resourceName;
	private final Class<?> relativeResourceClass;

	private final FileChannel fileChannel;
	private final String fileName;

	public InputStreamFactory(File file) {
		super();
		if (file == null) {
			throw new IllegalStateException("file cannot be null.");
		}
		this.file = file;
		this.resourceName = null;
		this.relativeResourceClass = null;

		fileChannel = null;
		fileName = null;
	}

	public InputStreamFactory(Class<?> relativeResourceClass, String resourceName) {
		super();
		this.file = null;
		if (resourceName == null) {
			throw new IllegalStateException("A null value was passed in as a resource name.");
		}
		this.resourceName = resourceName;
		this.relativeResourceClass = relativeResourceClass;

		fileChannel = null;
		fileName = null;
	}

	public InputStreamFactory(FileChannel fileChannel, String fileName) {
		super();
		this.fileChannel = fileChannel;
		this.fileName = fileName;

		this.file = null;

		this.resourceName = null;
		this.relativeResourceClass = null;

	}

	@Override
	public InputStream createInputStream() throws FileNotFoundException {
		InputStream inputStream;
		if (file != null) {
			inputStream = new FileInputStream(file);
		} else if (resourceName != null && relativeResourceClass != null) {
			inputStream = relativeResourceClass.getResourceAsStream(resourceName);
		} else if (this.fileChannel != null) {
			inputStream = new UncloseableInputStream(fileChannel);
		} else {
			throw new AssertionError();
		}
		if (inputStream == null) {
			throw new IllegalStateException("Unable to create an input stream:" + this);
		}

		return inputStream;
	}

	@Override
	public long getSizeInBytes() {
		long sizeInBytes = 0;
		try {
			if (file != null) {
				sizeInBytes = file.length();
			} else if (resourceName != null && relativeResourceClass != null) {
				sizeInBytes = relativeResourceClass.getResourceAsStream(resourceName).available();
			} else if (this.fileChannel != null) {
				sizeInBytes = fileChannel.size();
			} else {
				throw new AssertionError();
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return sizeInBytes;
	}

	@Override
	public String toString() {
		return "InputStreamFactory [file=" + file + ", resourceName=" + resourceName + ", relativeResourceClass=" + relativeResourceClass + "]";
	}

	@Override
	public String getName() {
		String name = resourceName;
		if (name == null || name.isEmpty()) {
			if (file != null) {
				name = file.getAbsolutePath();
			} else {
				name = fileName;
			}
		}
		return name;
	}

	private static class UncloseableInputStream extends InputStream {

		private final InputStream inputStream;
		private final FileChannel fileChannel;

		public UncloseableInputStream(FileChannel fileChannel) {
			super();
			this.inputStream = Channels.newInputStream(fileChannel);
			this.fileChannel = fileChannel;
		}

		@Override
		public int read() throws IOException {
			return inputStream.read();
		}

		@Override
		public int read(byte[] b) throws IOException {
			return inputStream.read(b);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return inputStream.read(b, off, len);
		}

		@Override
		public long skip(long n) throws IOException {
			return inputStream.skip(n);
		}

		@Override
		public int available() throws IOException {
			return inputStream.available();
		}

		@Override
		public synchronized void mark(int readlimit) {
			inputStream.mark(readlimit);
		}

		@Override
		public synchronized void reset() throws IOException {
			inputStream.reset();
		}

		@Override
		public boolean markSupported() {
			return inputStream.markSupported();
		}

		@Override
		public void close() throws IOException {
			fileChannel.position(0);
		}

	}

}
