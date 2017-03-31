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
package com.roche.sequencing.bioinformatics.common.utils.fastq;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

import htsjdk.samtools.fastq.FastqConstants;
import htsjdk.samtools.fastq.FastqRecord;

public class FastqWriter implements AutoCloseable {

	private final BufferedOutputStream writer;

	public FastqWriter(File fileToWrite) {
		String extension = FileUtil.getFileExtension(fileToWrite.getName());
		boolean shouldZip = extension.equals("gz");
		if (shouldZip) {
			try {
				writer = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(fileToWrite)));
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		} else {
			try {
				writer = new BufferedOutputStream(new FileOutputStream(fileToWrite));
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
	}

	public void write(FastqRecord record) {
		try {
			String recordAsString = FastqConstants.SEQUENCE_HEADER + record.getReadHeader() + StringUtil.NEWLINE + record.getReadString() + StringUtil.NEWLINE + FastqConstants.QUALITY_HEADER
					+ (record.getBaseQualityHeader() == null ? "" : record.getBaseQualityHeader()) + StringUtil.NEWLINE + record.getBaseQualityString() + StringUtil.NEWLINE;
			writer.write(recordAsString.getBytes());
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	public void close() {
		try {
			writer.close();
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

}
