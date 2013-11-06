package com.roche.mapping.datasimulator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

import net.sf.picard.fastq.FastqReader;
import net.sf.picard.fastq.FastqRecord;

import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class FastqToFastaFormatter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			createFastaFilesForAllFastqFiles(new File("D:/rebalance/results/"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void createFastaFilesForAllFastqFiles(File directory) throws IOException {
		File[] fastqFiles = directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".fastq");
			}
		});

		for (File fastqFile : fastqFiles) {
			createFastaFileFromFastqFile(fastqFile);
		}
	}

	private static void createFastaFileFromFastqFile(File fastqFile) throws IOException {
		File outputFastaFile = new File(fastqFile.getAbsolutePath().replaceAll("fastq", "fasta"));
		FileUtil.createNewFile(outputFastaFile);

		FileWriter fw = new FileWriter(outputFastaFile);
		BufferedWriter bw = new BufferedWriter(fw);

		try (FastqReader fastQReader = new FastqReader(fastqFile)) {
			while (fastQReader.hasNext()) {
				FastqRecord record = fastQReader.next();
				String readOne = record.getReadString();

				bw.write(">" + record.getReadHeader() + StringUtil.NEWLINE);
				bw.write(readOne + StringUtil.NEWLINE);
			}
		}

		bw.close();

	}

}
