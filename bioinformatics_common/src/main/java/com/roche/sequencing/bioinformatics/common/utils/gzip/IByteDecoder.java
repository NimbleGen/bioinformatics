package com.roche.sequencing.bioinformatics.common.utils.gzip;

import java.io.File;

public interface IByteDecoder {

	byte[] decodeBytes(long blockUncompressedDecodedStartPositionInBytes, byte[] bytes);

	void persistToFile(File file);

}
