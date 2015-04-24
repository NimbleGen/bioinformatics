package com.roche.heatseq.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.sf.picard.fastq.FastqConstants;
import net.sf.picard.fastq.FastqRecord;

import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class FastqWriter implements AutoCloseable {

	private final BufferedWriter writer;

	public FastqWriter(File fileToWrite) {
		try {
			writer = new BufferedWriter(new FileWriter(fileToWrite));
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public void write(FastqRecord record) {
		try {
			String recordAsString = FastqConstants.SEQUENCE_HEADER + record.getReadHeader() + StringUtil.NEWLINE + record.getReadString() + StringUtil.NEWLINE + FastqConstants.QUALITY_HEADER
					+ (record.getBaseQualityHeader() == null ? "" : record.getBaseQualityHeader()) + StringUtil.NEWLINE + record.getBaseQualityString() + StringUtil.NEWLINE;
			writer.write(recordAsString);
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
