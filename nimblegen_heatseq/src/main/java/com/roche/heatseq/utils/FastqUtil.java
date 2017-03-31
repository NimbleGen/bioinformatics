package com.roche.heatseq.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.NucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.fastq.FastqReader;
import com.roche.sequencing.bioinformatics.common.utils.fastq.FastqWriter;

import htsjdk.samtools.fastq.FastqRecord;

public class FastqUtil {

	private final static int LINES_PER_ENTRY = 4;

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

	public static void subSample(File fastqFile, File outputFile, int sampleSize) throws IOException {

		int numberOfLines = FileUtil.countNumberOfLinesInFile(fastqFile);
		int numberOfEntries = numberOfLines / LINES_PER_ENTRY;

		sampleSize = Math.min(sampleSize, numberOfEntries);
		int[] sortedSampledIndexes = getSortedSampledIndexes(numberOfEntries, sampleSize, System.currentTimeMillis());

		int fastqIndex = 0;
		int sortedSampleIndex = 0;
		try (FastqWriter writer = new FastqWriter(outputFile)) {
			try (FastqReader reader = new FastqReader(fastqFile)) {
				while (reader.hasNext() && (sortedSampleIndex < sortedSampledIndexes.length)) {
					FastqRecord record = reader.next();

					if (fastqIndex == sortedSampledIndexes[sortedSampleIndex]) {
						writer.write(record);
						sortedSampleIndex++;
					}

					fastqIndex++;
				}
			}
		}

	}

	private static int[] getSortedSampledIndexes(int totalEntries, int entriesToSample, long randomSeed) {
		int[] result = new int[totalEntries];
		for (int i = 0; i < totalEntries; i++) {
			result[i] = i;
		}

		Random random = new Random(randomSeed);

		int currentSampleSize = 0;
		while (currentSampleSize < entriesToSample) {
			int index = currentSampleSize + random.nextInt(totalEntries - currentSampleSize);
			int temp = result[currentSampleSize];
			result[currentSampleSize] = result[index];
			result[index] = temp;
			currentSampleSize++;
		}

		List<Integer> sortedResult = new ArrayList<>();
		for (int i = 0; i < currentSampleSize; i++) {
			sortedResult.add(result[i]);
		}
		Collections.sort(sortedResult);
		return ArraysUtil.convertToIntArray(sortedResult);
	}

	public static void main(String[] args) {
		File in = new File("C:\\Users\\heilmank\\Desktop\\in.fastq");
		File out = new File("C:\\Users\\heilmank\\Desktop\\out.fastq");

		try {
			subSample(in, out, 1000);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
