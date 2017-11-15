package com.roche.heatseq.process;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.ProgressLoggerInterface;

public class ConcurrentSamFileWriter implements SAMFileWriter {

	private final SAMFileWriter samFileWriter;

	public ConcurrentSamFileWriter(SAMFileWriter samFileWriter) {
		this.samFileWriter = samFileWriter;
	}

	@Override
	public synchronized void addAlignment(SAMRecord alignment) {
		samFileWriter.addAlignment(alignment);
	}

	@Override
	public SAMFileHeader getFileHeader() {
		return samFileWriter.getFileHeader();
	}

	@Override
	public void setProgressLogger(ProgressLoggerInterface progress) {
		samFileWriter.setProgressLogger(progress);
	}

	@Override
	public void close() {
		samFileWriter.close();
	}

}
