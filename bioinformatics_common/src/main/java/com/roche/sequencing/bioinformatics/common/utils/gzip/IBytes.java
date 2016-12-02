package com.roche.sequencing.bioinformatics.common.utils.gzip;

public interface IBytes {

	byte[] getBytes(long offset, long length);

	void close();

}
