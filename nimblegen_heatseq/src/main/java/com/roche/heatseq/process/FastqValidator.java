package com.roche.heatseq.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import net.sf.picard.PicardException;
import net.sf.picard.fastq.FastqRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.utils.FastqReader;

public class FastqValidator {

	private static Logger logger = LoggerFactory.getLogger(FastqValidator.class);

	private FastqValidator() {
		throw new AssertionError();
	}

	public static void validate(File fastqOne, File fastqTwo) {
		int fastqOneSize = validateAndGetSize(fastqOne, "FASTQ1");
		int fastqTwoSize = validateAndGetSize(fastqTwo, "FASTQ2");

		if (fastqOne.getAbsolutePath().equals(fastqTwo.getAbsolutePath())) {
			throw new IllegalStateException("The same file[" + fastqTwo.getAbsolutePath() + "] was provided for FASTQ1 and FASTQ2.");
		}

		if (fastqOneSize == 0 && fastqTwoSize == 0) {
			throw new IllegalStateException("The provided FASTQ1[" + fastqOne.getAbsolutePath() + "] and FASTQ2[" + fastqTwo.getAbsolutePath() + "] contain no entries.");
		} else if (fastqOneSize == 0) {
			throw new IllegalStateException("The provided FASTQ1[" + fastqOne.getAbsolutePath() + "] contains no entries.");
		} else if (fastqTwoSize == 0) {
			throw new IllegalStateException("The provided FASTQ2[" + fastqTwo.getAbsolutePath() + "] contains no entries.");
		} else if (fastqOneSize != fastqTwoSize) {
			throw new IllegalStateException("The provided FASTQ1[" + fastqOne.getAbsolutePath() + "] and FASTQ2[" + fastqTwo.getAbsolutePath()
					+ "] contained a different number of entries, the number of FASTQ1 entries[" + fastqOneSize + "] and the number of FASTQ2 entries[" + fastqTwoSize + "].");
		}
	}

	private static int validateAndGetSize(File fastq, String fileName) {

		if (!fastq.exists()) {
			throw new IllegalStateException("Unable to find provided " + fileName + " file[" + fastq.getAbsolutePath() + "].");
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(fastq))) {
			reader.read();
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("The provided " + fileName + " file[" + fastq.getAbsolutePath() + "] does not have read permissions.", e);
		} catch (IOException e) {
			throw new IllegalStateException("The provided " + fileName + " file[" + fastq.getAbsolutePath() + "] does not have read permissions.", e);
		}

		if (fastq.length() == 0) {
			throw new IllegalStateException("The provided " + fileName + " file[" + fastq.getAbsolutePath() + "] is empty.");
		}

		int size = 0;
		try (FastqReader reader = new FastqReader(fastq)) {
			Iterator<FastqRecord> iter = reader.iterator();
			while (iter.hasNext()) {
				iter.next();
				size++;
			}
		} catch (PicardException e) {
			logger.warn(e.getMessage(), e);
		}

		return size;

	}

}
