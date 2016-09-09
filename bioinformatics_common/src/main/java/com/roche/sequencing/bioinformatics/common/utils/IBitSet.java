package com.roche.sequencing.bioinformatics.common.utils;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;

public interface IBitSet {

	BitSet getBitSet(long fromIndex, long toIndexExclusive);

	void writeToFile(File outputFile) throws IOException;

	long size();

}
