package com.roche.sequencing.bioinformatics.common.utils.gzip;

public interface IGZipParser {

	void datasetStarted(GZipMemberData datasetMemberData, long datasetCompressedStartInBytes);

	void blockParsed(GZipMemberData datasetMemberData, GZipBlock block, byte[] startingDictionaryBytes, long blockCompressedStartPositionInBits);

	void datasetCompleted(GZipMemberData datasetMemberData, long datasetCompressedEndInBytes, Long crc32, Long iSize);

}
