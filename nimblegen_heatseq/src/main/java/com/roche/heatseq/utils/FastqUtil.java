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
}
