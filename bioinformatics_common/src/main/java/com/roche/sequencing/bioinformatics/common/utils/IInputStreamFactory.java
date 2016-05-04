package com.roche.sequencing.bioinformatics.common.utils;

import java.io.FileNotFoundException;
import java.io.InputStream;

public interface IInputStreamFactory {
	InputStream createInputStream() throws FileNotFoundException;

	String getName();

	long getSizeInBytes();
}
