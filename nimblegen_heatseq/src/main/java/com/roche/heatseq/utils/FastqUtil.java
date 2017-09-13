package com.roche.heatseq.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.NucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.IlluminaFastQReadNameUtil;
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

	public static Map<String, String> getReadNameToFastqIndexMap(File fastqFile) {
		Map<String, String> readNameToFastqIndexMap = new HashMap<>();

		int fastqIndex = 0;
		try (FastqReader reader = new FastqReader(fastqFile)) {
			while (reader.hasNext()) {
				FastqRecord record = reader.next();
				String readName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(record.getReadHeader());
				readNameToFastqIndexMap.put(readName, "" + fastqIndex);
				fastqIndex++;
			}
		}

		return readNameToFastqIndexMap;
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

	public static void getFirstNEntries(File fastqFile, File outputFile, int numberOfEntries) throws IOException {

		int numberOfLines = FileUtil.countNumberOfLinesInFile(fastqFile);
		int totalNumberOfEntries = numberOfLines / LINES_PER_ENTRY;

		numberOfEntries = Math.min(totalNumberOfEntries, numberOfEntries);
		int[] sortedSampledIndexes = new int[numberOfEntries];
		for (int i = 0; i < numberOfEntries; i++) {
			sortedSampledIndexes[i] = i;
		}

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
		File in = new File("D:\\kurts_space\\shared\\Todd_big\\r1.fastq");
		File out = new File("D:\\kurts_space\\shared\\Todd_big\\r1_1000.fastq");

		try {
			getFirstNEntries(in, out, 1000);
		} catch (IOException e) {
			e.printStackTrace();
		}

		in = new File("D:\\kurts_space\\shared\\Todd_big\\r2.fastq");
		out = new File("D:\\kurts_space\\shared\\Todd_big\\r2_1000.fastq");

		try {
			getFirstNEntries(in, out, 1000);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
