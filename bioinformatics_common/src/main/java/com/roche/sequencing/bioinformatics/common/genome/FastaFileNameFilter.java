package com.roche.sequencing.bioinformatics.common.genome;

import java.io.File;
import java.io.FilenameFilter;

import com.roche.sequencing.bioinformatics.common.utils.FileUtil;

public class FastaFileNameFilter implements FilenameFilter {

	private final static String FASTA_FILE_EXTENSION = "fa";

	@Override
	public boolean accept(File dir, String name) {
		return FileUtil.getFileExtension(name).equals(FASTA_FILE_EXTENSION);
	}

}
