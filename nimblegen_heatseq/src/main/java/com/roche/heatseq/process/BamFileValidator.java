package com.roche.heatseq.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class BamFileValidator {

	private BamFileValidator() {
		throw new AssertionError();
	}

	public static void validate(File bamOrSamFile) {
		if (!bamOrSamFile.exists()) {
			throw new IllegalStateException("Unable to find provided BAM or SAM file[" + bamOrSamFile.getAbsolutePath() + "].");
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(bamOrSamFile))) {
			reader.read();
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("The provided BAM or SAM file[" + bamOrSamFile.getAbsolutePath() + "] does not have read permissions.", e);
		} catch (IOException e) {
			throw new IllegalStateException("The provided BAM or SAM file[" + bamOrSamFile.getAbsolutePath() + "] does not have read permissions.", e);
		}

		if (bamOrSamFile.length() == 0) {
			throw new IllegalStateException("The provided BAM or SAM file[" + bamOrSamFile.getAbsolutePath() + "] is empty.");
		}
	}
}
