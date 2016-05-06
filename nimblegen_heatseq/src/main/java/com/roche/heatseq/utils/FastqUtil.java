package com.roche.heatseq.utils;

import htsjdk.samtools.fastq.FastqRecord;

import java.io.File;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.NucleotideCodeSequence;


public class FastqUtil {

	private FastqUtil() {
		throw new AssertionError();
	}

	public static void reverseCompliment(File inputFastqFile, File outputFastqFile) {

		try (FastqWriter writer = new FastqWriter(outputFastqFile)) {
			try (FastqReader reader = new FastqReader(inputFastqFile)) {
				while (reader.hasNext()) {
					FastqRecord record = reader.next();

					ISequence sequence = new NucleotideCodeSequence(record.getReadString());
					ISequence newSequence = sequence.getCompliment();

					String qualityString = record.getBaseQualityString();
					String newQualityString = qualityString;// StringUtil.reverse(qualityString);

					FastqRecord newRecord = new FastqRecord(record.getReadHeader(), newSequence.toString(), record.getBaseQualityHeader(), newQualityString);
					writer.write(newRecord);

				}
			}
		}

	}

	public static void main(String[] args) {
		// reverseCompliment(new File("U:\\kurts_space\\carolina_818\\S2_2_R2_quality_filtered.fastq"), new File("U:\\kurts_space\\carolina_818\\rcS2_2_R2_quality_filtered.fastq"));
		// System.out.println("done");
		ISequence sequence = new NucleotideCodeSequence("TTGCACTGTACTCCTCTTGACCTGCTGTGGCACCTTTTACTTCAATTCAGTTAACACACTACCGTCGGAT");
		System.out.println(sequence.getReverseCompliment());
	}

}
