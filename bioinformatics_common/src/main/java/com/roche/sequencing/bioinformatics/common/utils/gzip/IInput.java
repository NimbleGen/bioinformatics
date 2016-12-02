package com.roche.sequencing.bioinformatics.common.utils.gzip;

import java.io.IOException;

public interface IInput {
	int read(byte b[]) throws IOException;

	int read() throws IOException;

	String readLine() throws IOException;
}
