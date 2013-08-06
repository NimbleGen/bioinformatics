/**
 *   Copyright 2013 Roche NimbleGen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.roche.heatseq.process;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sf.picard.fastq.FastqReader;
import net.sf.picard.fastq.FastqRecord;
import net.sf.picard.fastq.FastqWriter;
import net.sf.picard.fastq.FastqWriterFactory;
import net.sf.picard.io.IoUtil;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

import org.apache.commons.io.FileUtils;

import com.roche.imageexporter.Graphics2DImageExporter;
import com.roche.imageexporter.Graphics2DImageExporter.ImageType;
import com.roche.mapping.SAMRecordUtil;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.DelimitedFileParserUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class BamFileInternalUtil {

	public static final DecimalFormat decimalFormat = new DecimalFormat(",###.00");

	public static void splitBamFilesByMatches(File bamFileOne, File bamFileTwo, File outputDirectory) {
		IoUtil.assertFileIsReadable(bamFileOne);
		IoUtil.assertFileIsReadable(bamFileOne);

		File matchFile = new File(outputDirectory, "match.bam");
		File misMatchOneFile = new File(outputDirectory, "mismatch_one.bam");
		File misMatchTwoFile = new File(outputDirectory, "mismatch_two.bam");
		File onlyInOneFile = new File(outputDirectory, "only_in_one.bam");
		File onlyInTwoFile = new File(outputDirectory, "only_in_two.bam");

		try {
			FileUtil.createNewFile(matchFile);
			FileUtil.createNewFile(misMatchOneFile);
			FileUtil.createNewFile(misMatchTwoFile);
			FileUtil.createNewFile(onlyInOneFile);
			FileUtil.createNewFile(onlyInTwoFile);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		List<SAMRecord> bamFileOneRecords = new ArrayList<SAMRecord>();

		final SAMFileReader reader = new SAMFileReader(IoUtil.openFileForReading(bamFileOne));
		for (final SAMRecord record : reader) {
			bamFileOneRecords.add(record);
		}

		SAMRecordComparator comparator = new SAMRecordComparator();

		SAMFileHeader header1 = reader.getFileHeader();
		reader.close();
		Collections.sort(bamFileOneRecords, comparator);

		List<SAMRecord> bamFileTwoRecords = new ArrayList<SAMRecord>();
		final SAMFileReader reader2 = new SAMFileReader(IoUtil.openFileForReading(bamFileTwo));
		for (final SAMRecord record : reader2) {
			bamFileTwoRecords.add(record);
		}
		SAMFileHeader header2 = reader2.getFileHeader();
		reader2.close();
		Collections.sort(bamFileTwoRecords, comparator);

		header1.setSortOrder(SortOrder.queryname);
		header2.setSortOrder(SortOrder.queryname);

		final SAMFileWriter matchWriter = new SAMFileWriterFactory().makeSAMOrBAMWriter(header1, true, matchFile);
		final SAMFileWriter mismatchOneWriter = new SAMFileWriterFactory().makeSAMOrBAMWriter(header1, true, misMatchOneFile);
		final SAMFileWriter mismatchTwoWriter = new SAMFileWriterFactory().makeSAMOrBAMWriter(header2, true, misMatchTwoFile);
		final SAMFileWriter onlyInOneWriter = new SAMFileWriterFactory().makeSAMOrBAMWriter(header1, true, onlyInOneFile);
		final SAMFileWriter onlyInTwoWriter = new SAMFileWriterFactory().makeSAMOrBAMWriter(header2, true, onlyInTwoFile);

		int oneIndex = 0;
		int twoIndex = 0;

		while (oneIndex < bamFileOneRecords.size() && twoIndex < bamFileTwoRecords.size()) {
			SAMRecord one = bamFileOneRecords.get(oneIndex);
			SAMRecord two = bamFileTwoRecords.get(twoIndex);

			int compareResult = comparator.compare(one, two);

			if (compareResult == 0) {
				String errorDetails = getDoNotMatchDetails(one, two);
				boolean doDetailsMatch = errorDetails == null || errorDetails.isEmpty();
				if (doDetailsMatch) {
					matchWriter.addAlignment(one);
				} else {

					one.setAttribute("ER", errorDetails);
					mismatchOneWriter.addAlignment(one);
					two.setAttribute("ER", errorDetails);
					mismatchTwoWriter.addAlignment(two);
				}
				oneIndex++;
				twoIndex++;
			} else if (compareResult < 0) {
				onlyInOneWriter.addAlignment(one);
				oneIndex++;
			} else if (compareResult > 0) {
				onlyInTwoWriter.addAlignment(two);
				twoIndex++;
			}
		}

		matchWriter.close();
		mismatchOneWriter.close();
		mismatchTwoWriter.close();
		onlyInOneWriter.close();
		onlyInTwoWriter.close();
	}

	private static String getDoNotMatchDetails(SAMRecord one, SAMRecord two) {
		StringBuilder details = new StringBuilder();
		String mm1 = (String) one.getAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG);
		String mm2 = (String) two.getAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG);
		if (mm1 != null && mm1.charAt(0) == 0) {
			mm1 = mm1.substring(1);
		}
		if (mm1 != null && mm1.charAt(mm1.length() - 1) == 0) {
			mm1 = mm1.substring(0, mm1.length() - 1);
		}
		if (mm2 != null && mm2.charAt(0) == 0) {
			mm2 = mm2.substring(1);
		}
		if (mm2 != null && mm2.charAt(mm2.length() - 1) == 0) {
			mm2 = mm2.substring(0, mm2.length() - 1);
		}

		if (!one.getReadString().equals(two.getReadString())) {
			details.append(":Read Sequence:");
		}

		if (!one.getBaseQualityString().equals(two.getBaseQualityString())) {
			details.append(":Read Quality:");
		}

		if (!one.getCigarString().equals(two.getCigarString())) {
			details.append(":Cigar:");
		}

		if (!(one.getAlignmentStart() == two.getAlignmentStart())) {
			details.append(":Position:");
		}

		if (mm1 != null && mm2 != null && !mm1.equals(mm2)) {
			details.append(":Tag--MD:");
		}

		Integer oneEditDistance = (Integer) one.getAttribute(SAMRecordUtil.EDIT_DISTANCE_ATTRIBUTE_TAG);
		Integer twoEditDistance = (Integer) two.getAttribute(SAMRecordUtil.EDIT_DISTANCE_ATTRIBUTE_TAG);
		if (oneEditDistance != null && twoEditDistance != null && !(oneEditDistance.equals(twoEditDistance))) {
			details.append(":Tag--NM:");
		}

		if (!(one.getInferredInsertSize() == two.getInferredInsertSize())) {
			details.append(":Template Length:");
		}

		return details.toString();
	}

	private static class SAMRecordComparator implements Comparator<SAMRecord> {
		@Override
		public int compare(SAMRecord o1, SAMRecord o2) {
			int result = o1.getReadName().compareTo(o2.getReadName());
			if (result == 0) {
				result = Boolean.compare(o1.getMateNegativeStrandFlag(), o2.getMateNegativeStrandFlag());
			}
			return result;
		}
	}

	public static void createSubsetBamFile(final File inputSamOrBamFile, final File outputSamOrBamFile) {
		final SAMFileReader inputSam = new SAMFileReader(inputSamOrBamFile);
		final SAMFileWriter outputSam = new SAMFileWriterFactory().makeSAMOrBAMWriter(inputSam.getFileHeader(), true, outputSamOrBamFile);

		SAMRecordIterator iter = inputSam.queryOverlapping("chr1", 866376, 866568);

		while (iter.hasNext()) {
			final SAMRecord samRecord = iter.next();
			outputSam.addAlignment(samRecord);
		}
		outputSam.close();
		inputSam.close();
	}

	public static String[] getReadNames(File bamFile) {
		List<String> readNames = new ArrayList<String>();
		final SAMFileReader inputSam = new SAMFileReader(bamFile);
		// BamFileUtil.createIndex(inputSam, new File(inputSamOrBamFile.getAbsolutePath() + ".bai"));

		SAMRecordIterator iter = inputSam.iterator();

		while (iter.hasNext()) {
			final SAMRecord samRecord = iter.next();
			readNames.add(samRecord.getReadName());
		}
		inputSam.close();
		return readNames.toArray(new String[0]);
	}

	public static void filterFastqByReadNames(String[] readNames, File inputFastqOneFile, File inputFastqTwoFile, File outputFastqOneFile, File outputFastqTwoFile) {
		final FastqWriterFactory factory = new FastqWriterFactory();
		FastqWriter writerOne = factory.newWriter(outputFastqOneFile);
		FastqWriter writerTwo = factory.newWriter(outputFastqTwoFile);
		try (FastqReader fastQOneReader = new FastqReader(inputFastqOneFile)) {
			try (FastqReader fastQTwoReader = new FastqReader(inputFastqTwoFile)) {

				while (fastQOneReader.hasNext() && fastQTwoReader.hasNext()) {
					FastqRecord fastQOneRecord = fastQOneReader.next();
					FastqRecord fastQTwoRecord = fastQTwoReader.next();

					boolean match = false;
					readNameLoop: for (String readName : readNames) {
						String readHeaderOne = fastQOneRecord.getReadHeader();
						if (readHeaderOne.contains(readName)) {
							match = true;
							break readNameLoop;
						}
					}

					if (match) {
						writerOne.write(fastQOneRecord);
						writerTwo.write(fastQTwoRecord);
					}
				}
			}
		}
		writerOne.close();
		writerTwo.close();
	}

	public static void extractFirstNEntriesInFastq(int numberOfEntries, File inputFastqOneFile, File inputFastqTwoFile, File outputFastqOneFile, File outputFastqTwoFile) {
		final FastqWriterFactory factory = new FastqWriterFactory();
		FastqWriter writerOne = factory.newWriter(outputFastqOneFile);
		FastqWriter writerTwo = factory.newWriter(outputFastqTwoFile);
		int count = 0;
		try (FastqReader fastQOneReader = new FastqReader(inputFastqOneFile)) {
			try (FastqReader fastQTwoReader = new FastqReader(inputFastqTwoFile)) {

				while (fastQOneReader.hasNext() && fastQTwoReader.hasNext() && count < numberOfEntries) {
					FastqRecord fastQOneRecord = fastQOneReader.next();
					FastqRecord fastQTwoRecord = fastQTwoReader.next();
					count++;
					writerOne.write(fastQOneRecord);
					writerTwo.write(fastQTwoRecord);
				}
			}
		}
		writerOne.close();
		writerTwo.close();
	}

	public static void extractRandomNEntriesInFastq(int numberOfEntries, File inputFastqOneFile, File inputFastqTwoFile, File outputFastqOneFile, File outputFastqTwoFile) throws IOException {
		int totalEntries = FileUtil.countNumberOfLinesInFile(inputFastqOneFile) / 4;
		if (numberOfEntries > totalEntries) {
			throw new IllegalStateException("The fastqfile[" + inputFastqOneFile.getAbsolutePath() + "] only contains " + totalEntries + " so " + numberOfEntries
					+ " random samples can be selected from it.");
		} else {
			final FastqWriterFactory factory = new FastqWriterFactory();
			FastqWriter writerOne = factory.newWriter(outputFastqOneFile);
			FastqWriter writerTwo = factory.newWriter(outputFastqTwoFile);

			Random randomNumberGenerator = new Random(System.currentTimeMillis());
			SortedSet<Integer> sampleIndexes = Collections.synchronizedSortedSet(new TreeSet<Integer>());
			while (sampleIndexes.size() < numberOfEntries) {
				sampleIndexes.add(randomNumberGenerator.nextInt(totalEntries));
			}

			int currentIndex = 0;
			try (FastqReader fastQOneReader = new FastqReader(inputFastqOneFile)) {
				try (FastqReader fastQTwoReader = new FastqReader(inputFastqTwoFile)) {

					while (fastQOneReader.hasNext() && fastQTwoReader.hasNext() && currentIndex < totalEntries) {
						FastqRecord fastQOneRecord = fastQOneReader.next();
						FastqRecord fastQTwoRecord = fastQTwoReader.next();
						currentIndex++;
						if (sampleIndexes.contains(currentIndex)) {
							writerOne.write(fastQOneRecord);
							writerTwo.write(fastQTwoRecord);
						}
					}
				}
			}
			writerOne.close();
			writerTwo.close();
		}
	}

	private static class Probe {
		private final String probeName;
		private final int start;
		private final int stop;
		private final String strand;

		public Probe(String probeName, int start, int stop, String strand) {
			super();
			this.probeName = probeName;
			this.start = start;
			this.stop = stop;
			this.strand = strand;
		}

		@Override
		public String toString() {
			return "Probe [probeName=" + probeName + ", start=" + start + ", stop=" + stop + ", strand=" + strand + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((probeName == null) ? 0 : probeName.hashCode());
			result = prime * result + start;
			result = prime * result + stop;
			result = prime * result + ((strand == null) ? 0 : strand.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Probe other = (Probe) obj;
			if (probeName == null) {
				if (other.probeName != null)
					return false;
			} else if (!probeName.equals(other.probeName))
				return false;
			if (start != other.start)
				return false;
			if (stop != other.stop)
				return false;
			if (strand == null) {
				if (other.strand != null)
					return false;
			} else if (!strand.equals(other.strand))
				return false;
			return true;
		}

	}

	private static class ProbeAndUid {
		private final Probe probe;
		private final String uid;

		public ProbeAndUid(String uid, Probe probe) {
			super();
			this.uid = uid;
			this.probe = probe;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((probe == null) ? 0 : probe.hashCode());
			result = prime * result + ((uid == null) ? 0 : uid.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ProbeAndUid other = (ProbeAndUid) obj;
			if (probe == null) {
				if (other.probe != null)
					return false;
			} else if (!probe.equals(other.probe))
				return false;
			if (uid == null) {
				if (other.uid != null)
					return false;
			} else if (!uid.equals(other.uid))
				return false;
			return true;
		}

	}

	private static Map<Probe, String> getProbeToBlockMap(File blockFile) throws IOException {
		Map<Probe, String> probeToBlockMap = new HashMap<Probe, String>();
		String[] uidBlockHeaders = new String[] { "sequence_name", "capture_start", "capture_stop", "strand", "block" };
		Map<String, List<String>> blockHeadersToData = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(blockFile, uidBlockHeaders, StringUtil.TAB);
		List<String> blockSequenceNames = blockHeadersToData.get(uidBlockHeaders[0]);
		List<String> blockStart = blockHeadersToData.get(uidBlockHeaders[1]);
		List<String> blockStop = blockHeadersToData.get(uidBlockHeaders[2]);
		List<String> blockStrand = blockHeadersToData.get(uidBlockHeaders[3]);
		List<String> blockNames = blockHeadersToData.get(uidBlockHeaders[4]);
		for (int blockIndex = 0; blockIndex < blockSequenceNames.size(); blockIndex++) {
			probeToBlockMap.put(new Probe(blockSequenceNames.get(blockIndex), Integer.valueOf(blockStart.get(blockIndex)), Integer.valueOf(blockStop.get(blockIndex)), blockStrand.get(blockIndex)),
					blockNames.get(blockIndex));
		}
		return probeToBlockMap;
	}

	public static void createUidBlockReport(int maxUidLength, File probeUidQualityFile, File uidBlockReportByRead, File uidBlockReportByBlock, Map<Probe, String> probeToBlockMap) throws IOException {

		PrintWriter uidBlockReportByReadWriter = new PrintWriter(new FileWriter(uidBlockReportByRead));
		uidBlockReportByReadWriter.println("uid" + StringUtil.TAB + "uid_length" + StringUtil.TAB + "block_name" + StringUtil.TAB + "sequence_name" + StringUtil.TAB + "start" + StringUtil.TAB
				+ "stop");

		PrintWriter uidBlockReportByBlockWriter = new PrintWriter(new FileWriter(uidBlockReportByBlock));

		try {
			String[] probeUidQualityHeaders = new String[] { "uid", "probe_sequence_name", "probe_capture_start", "probe_capture_stop", "strand", "read_sequence" };

			Map<String, List<String>> probeHeadersToData = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(probeUidQualityFile, probeUidQualityHeaders, StringUtil.TAB);

			List<String> probeUidQualitySequenceNames = probeHeadersToData.get(probeUidQualityHeaders[1]);
			List<String> probeUidQualityStart = probeHeadersToData.get(probeUidQualityHeaders[2]);
			List<String> probeUidQualityStop = probeHeadersToData.get(probeUidQualityHeaders[3]);
			List<String> probeUidQualityStrand = probeHeadersToData.get(probeUidQualityHeaders[4]);

			Map<String, TallyMap<Integer>> uidLengthsByBlock = new HashMap<String, TallyMap<Integer>>();
			Map<String, TallyMap<Integer>> uniqueReadPairUidLengthsByBlock = new HashMap<String, TallyMap<Integer>>();

			Set<ProbeAndUid> foundReads = new HashSet<ProbeAndUid>();

			for (int probeUidIndex = 0; probeUidIndex < probeUidQualitySequenceNames.size(); probeUidIndex++) {
				String probeName = probeUidQualitySequenceNames.get(probeUidIndex);
				String probeStart = probeUidQualityStart.get(probeUidIndex);
				String probeStop = probeUidQualityStop.get(probeUidIndex);
				String probeStrand = probeUidQualityStrand.get(probeUidIndex);
				String uid = probeHeadersToData.get(probeUidQualityHeaders[0]).get(probeUidIndex);
				Probe probe = new Probe(probeName, Integer.valueOf(probeStart), Integer.valueOf(probeStop), probeStrand);
				String blockName = probeToBlockMap.get(probe);

				if (blockName != null) {
					ProbeAndUid probeAndUid = new ProbeAndUid(uid, probe);
					if (!foundReads.contains(probeAndUid)) {
						foundReads.add(probeAndUid);

						TallyMap<Integer> uniqueBlockTallyMap = uniqueReadPairUidLengthsByBlock.get(blockName);
						if (uniqueBlockTallyMap == null) {
							uniqueBlockTallyMap = new TallyMap<Integer>();
						}
						uniqueBlockTallyMap.add(uid.length());
						uniqueReadPairUidLengthsByBlock.put(blockName, uniqueBlockTallyMap);
					}

					uidBlockReportByReadWriter.println(uid + StringUtil.TAB + uid.length() + StringUtil.TAB + blockName + StringUtil.TAB + probeUidQualitySequenceNames.get(probeUidIndex)
							+ StringUtil.TAB + probeStart + StringUtil.TAB + probeStop);
					TallyMap<Integer> blockTallyMap = uidLengthsByBlock.get(blockName);
					if (blockTallyMap == null) {
						blockTallyMap = new TallyMap<Integer>();
					}
					blockTallyMap.add(uid.length());
					uidLengthsByBlock.put(blockName, blockTallyMap);
				} else {
					System.out.println("Could not find matching block for " + probe + ".");
				}
				if ((probeUidIndex % 10000) == 0) {
					System.out.println("probe uid index:" + probeUidIndex);
					uidBlockReportByReadWriter.flush();
				}
			}

			StringBuilder byBlockHeader = new StringBuilder();
			byBlockHeader.append("block" + StringUtil.TAB + "total_reads" + StringUtil.TAB);
			for (int i = 0; i < maxUidLength; i++) {
				byBlockHeader.append(i + StringUtil.TAB);
			}
			byBlockHeader.append("total_unique_reads" + StringUtil.TAB);
			for (int i = 0; i < maxUidLength; i++) {
				byBlockHeader.append("unique_" + i + StringUtil.TAB);
			}
			uidBlockReportByBlockWriter.println(byBlockHeader.substring(0, byBlockHeader.length() - 1));

			List<String> sortedBlockNames = new ArrayList<String>(uidLengthsByBlock.keySet());
			Collections.sort(sortedBlockNames);
			for (String blockName : sortedBlockNames) {
				StringBuilder currentLine = new StringBuilder();
				TallyMap<Integer> tallyMap = uidLengthsByBlock.get(blockName);
				int totalReads = tallyMap.getSumOfAllBins();
				currentLine.append(blockName + StringUtil.TAB + totalReads + StringUtil.TAB);
				for (int i = 0; i < maxUidLength; i++) {
					currentLine.append(tallyMap.getCount(i) + StringUtil.TAB);
				}

				TallyMap<Integer> uniqueTallyMap = uniqueReadPairUidLengthsByBlock.get(blockName);
				int totalUniqueReads = uniqueTallyMap.getSumOfAllBins();
				currentLine.append(totalUniqueReads + StringUtil.TAB);
				for (int i = 0; i < maxUidLength; i++) {
					currentLine.append(uniqueTallyMap.getCount(i) + StringUtil.TAB);
				}
				uidBlockReportByBlockWriter.println(currentLine.substring(0, currentLine.length() - 1));
			}
		} finally {
			uidBlockReportByReadWriter.close();

			uidBlockReportByBlockWriter.close();
		}
	}

	public static void createUidBlockBaseNucleotideCompositionReport(File probeUidQualityFile, Map<Probe, String> probeToBlockMap, File uidBlockBaseCompositionOutput) throws IOException {

		PrintWriter uidBlockBaseCompositionWriter = new PrintWriter(new FileWriter(uidBlockBaseCompositionOutput));
		try {
			uidBlockBaseCompositionWriter.println("block" + StringUtil.TAB + "a_count" + StringUtil.TAB + "a_percent" + StringUtil.TAB + "c_count" + StringUtil.TAB + "c_percent" + StringUtil.TAB
					+ "g_count" + StringUtil.TAB + "g_percent" + StringUtil.TAB + "t_count" + StringUtil.TAB + "t_percent" + StringUtil.TAB + "n_count" + StringUtil.TAB + "n_percent" + StringUtil.TAB
					+ "all_a_count" + StringUtil.TAB + "all_a_percent" + StringUtil.TAB + "all_c_count" + StringUtil.TAB + "all_c_percent" + StringUtil.TAB + "all_g_count" + StringUtil.TAB
					+ "all_g_percent" + StringUtil.TAB + "all_t_count" + StringUtil.TAB + "all_t_percent" + StringUtil.TAB + "all_n_count" + StringUtil.TAB + "all_n_percent");

			String[] probeUidQualityHeaders = new String[] { "uid", "probe_sequence_name", "probe_capture_start", "probe_capture_stop", "strand" };

			Map<String, List<String>> probeHeadersToData = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(probeUidQualityFile, probeUidQualityHeaders, StringUtil.TAB);

			List<String> probeUidQualitySequenceNames = probeHeadersToData.get(probeUidQualityHeaders[1]);
			List<String> probeUidQualityStart = probeHeadersToData.get(probeUidQualityHeaders[2]);
			List<String> probeUidQualityStop = probeHeadersToData.get(probeUidQualityHeaders[3]);
			List<String> probeUidQualityStrand = probeHeadersToData.get(probeUidQualityHeaders[4]);

			Map<String, TallyMap<Character>> nucleotideCompositionByBlock = new HashMap<String, TallyMap<Character>>();
			Map<String, TallyMap<Character>> allNucleotideCompositionByBlock = new HashMap<String, TallyMap<Character>>();

			Set<ProbeAndUid> uniqueProbeAndUidPairs = new HashSet<ProbeAndUid>();
			for (int probeUidIndex = 0; probeUidIndex < probeUidQualitySequenceNames.size(); probeUidIndex++) {
				String uid = probeHeadersToData.get(probeUidQualityHeaders[0]).get(probeUidIndex);
				Probe probe = new Probe(probeUidQualitySequenceNames.get(probeUidIndex), Integer.valueOf(probeUidQualityStart.get(probeUidIndex)), Integer.valueOf(probeUidQualityStop
						.get(probeUidIndex)), probeUidQualityStrand.get(probeUidIndex));
				String blockName = probeToBlockMap.get(probe);
				// only count unique uid nucleotides
				if (blockName != null) {
					ProbeAndUid probeAndUid = new ProbeAndUid(uid, probe);

					// all
					TallyMap<Character> allBlockTallyMap = allNucleotideCompositionByBlock.get(blockName);
					if (allBlockTallyMap == null) {
						allBlockTallyMap = new TallyMap<Character>();
					}
					for (Character c : uid.toCharArray()) {
						allBlockTallyMap.add(Character.toUpperCase(c));
					}
					allNucleotideCompositionByBlock.put(blockName, allBlockTallyMap);

					// just unique
					if (!uniqueProbeAndUidPairs.contains(probeAndUid)) {
						uniqueProbeAndUidPairs.add(probeAndUid);
						TallyMap<Character> blockTallyMap = nucleotideCompositionByBlock.get(blockName);
						if (blockTallyMap == null) {
							blockTallyMap = new TallyMap<Character>();
						}
						for (Character c : uid.toCharArray()) {
							blockTallyMap.add(Character.toUpperCase(c));
						}
						nucleotideCompositionByBlock.put(blockName, blockTallyMap);
					}
				} else {
					System.out.println("Could not find matching block for " + probe);
				}

				if ((probeUidIndex % 10000) == 0) {
					System.out.println("probe uid index:" + probeUidIndex);
				}
			}

			List<String> sortedBlockNames = new ArrayList<String>(nucleotideCompositionByBlock.keySet());
			Collections.sort(sortedBlockNames);
			for (String blockName : sortedBlockNames) {
				TallyMap<Character> tallyMap = nucleotideCompositionByBlock.get(blockName);
				int totalNucleotides = tallyMap.getSumOfAllBins();

				int aCount = tallyMap.getCount('A');
				int cCount = tallyMap.getCount('C');
				int gCount = tallyMap.getCount('G');
				int tCount = tallyMap.getCount('T');
				int nCount = tallyMap.getCount('N');

				double aPercent = ((double) aCount) / ((double) totalNucleotides);
				double cPercent = ((double) cCount) / ((double) totalNucleotides);
				double gPercent = ((double) gCount) / ((double) totalNucleotides);
				double tPercent = ((double) tCount) / ((double) totalNucleotides);
				double nPercent = ((double) nCount) / ((double) totalNucleotides);

				TallyMap<Character> allTallyMap = allNucleotideCompositionByBlock.get(blockName);
				int allTotalNucleotides = allTallyMap.getSumOfAllBins();

				int all_aCount = allTallyMap.getCount('A');
				int all_cCount = allTallyMap.getCount('C');
				int all_gCount = allTallyMap.getCount('G');
				int all_tCount = allTallyMap.getCount('T');
				int all_nCount = allTallyMap.getCount('N');

				double all_aPercent = ((double) all_aCount) / ((double) allTotalNucleotides);
				double all_cPercent = ((double) all_cCount) / ((double) allTotalNucleotides);
				double all_gPercent = ((double) all_gCount) / ((double) allTotalNucleotides);
				double all_tPercent = ((double) all_tCount) / ((double) allTotalNucleotides);
				double all_nPercent = ((double) all_nCount) / ((double) allTotalNucleotides);

				DecimalFormat format = new DecimalFormat("##.##");
				uidBlockBaseCompositionWriter.println(blockName + StringUtil.TAB + aCount + StringUtil.TAB + format.format(aPercent) + StringUtil.TAB + cCount + StringUtil.TAB
						+ format.format(cPercent) + StringUtil.TAB + gCount + StringUtil.TAB + format.format(gPercent) + StringUtil.TAB + tCount + StringUtil.TAB + format.format(tPercent)
						+ StringUtil.TAB + nCount + StringUtil.TAB + format.format(nPercent) + StringUtil.TAB + all_aCount + StringUtil.TAB + format.format(all_aPercent) + StringUtil.TAB + all_cCount
						+ StringUtil.TAB + format.format(all_cPercent) + StringUtil.TAB + all_gCount + StringUtil.TAB + format.format(all_gPercent) + StringUtil.TAB + all_tCount + StringUtil.TAB
						+ format.format(all_tPercent) + StringUtil.TAB + all_nCount + StringUtil.TAB + format.format(all_nPercent));
			}
		} finally {
			uidBlockBaseCompositionWriter.close();
		}
	}

	public static void processRandomNEntriesAndGenerateReports(File fastq1, File fastq2, File probeFile, File blockFile, Integer numberOfEntries, File baseOutputDirectory) throws Exception {
		File outputDirectory = new File(baseOutputDirectory, "/" + numberOfEntries + "/");
		FileUtils.forceMkdir(outputDirectory);

		File abbreviatedFastq1 = new File(outputDirectory, FileUtil.getFileNameWithoutExtension(fastq1.getName()) + "_" + numberOfEntries + ".fastq");
		File abbreviatedFastq2 = new File(outputDirectory, FileUtil.getFileNameWithoutExtension(fastq2.getName()) + "_" + numberOfEntries + ".fastq");

		if (numberOfEntries == null) {
			abbreviatedFastq1 = fastq1;
			abbreviatedFastq2 = fastq2;
		} else {
			extractRandomNEntriesInFastq(numberOfEntries, fastq1, fastq2, abbreviatedFastq1, abbreviatedFastq2);
			System.out.println("done creating abbreviated fastq files.");
		}

		// PrefuppCli.main(new String[] { "--fastQOne", abbreviatedFastq1.getAbsolutePath(), "--fastQTwo", abbreviatedFastq2.getAbsolutePath(), "--probe", probeFile.getAbsolutePath(), "--outputDir",
		// outputDirectory.getAbsolutePath(), "--outputReports", "--allow_variable_length_uids" });
		System.out.println("done mapping.");
		generateBlockReports(outputDirectory, blockFile);
		System.out.println("done with block reports.");

		Map<String, Double> totalReadMapByBlock = getTotalReadsMapByBlock(new File(outputDirectory, "/block_report_by_block.txt"), false);
		generateHeatMapFrom(totalReadMapByBlock, "All Reads per Block", new File(outputDirectory, "all_reads_heatmap.pdf"), ImageType.PDF);

		Map<String, Double> valuesByName = getTotalReadsMapByBlock(new File(outputDirectory, "/block_report_by_block.txt"), true);
		generateHeatMapFrom(valuesByName, "Unique (by Probe/UID) Reads per Block", new File(outputDirectory, "unique_reads_heatmap.pdf"), ImageType.PDF);

		Map<String, Double> editDistanceByBlock = getAverageEditDistanceByBlock(new File(outputDirectory, "/reports/extension_primer_alignment_with_blocks.txt"), new File(outputDirectory,
				"/block_report_by_block.txt"));
		generateHeatMapFrom(editDistanceByBlock, "Average Edit Distance for Mapped Reads per Block", new File(outputDirectory, "average_edit_distance_heatmap.pdf"), ImageType.PDF);

		Map<String, Double> averageUniqueUidByBlock = getAverageUniqueUidLengthBlock(new File(outputDirectory, "block_report_by_block.txt"));
		generateHeatMapFrom(averageUniqueUidByBlock, "Average UID Length per Read by Block", new File(outputDirectory, "average_uid_length_by_block.pdf"), ImageType.PDF);

	}

	private static class ColorScale {

		private final Color color;
		private final double minValue;
		private double range;

		public ColorScale(Color color, double minValue, double maxValue) {
			super();
			this.color = color;
			this.minValue = minValue;
			range = maxValue - minValue;
		}

		public Color getColor(double value) {
			double normalizedValue = ((double) value - minValue) / range;
			normalizedValue = Math.max(0, normalizedValue);
			int alpha = (int) (normalizedValue * 255);
			Color returnColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
			return returnColor;
		}
	}

	private static Map<String, Double> getTotalReadsMapByBlock(File blockFile, boolean justUniqueReads) throws IOException {
		Map<String, Double> totalReadsByBlock = new HashMap<String, Double>();

		String[] header = new String[] { "block", "total_reads", "total_unique_reads" };
		Map<String, List<String>> parsedFile = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(blockFile, header, StringUtil.TAB);

		List<String> blockNames = parsedFile.get(header[0]);
		List<String> reads = null;

		if (justUniqueReads) {
			reads = parsedFile.get(header[2]);
		} else {
			reads = parsedFile.get(header[1]);
		}

		for (int i = 0; i < blockNames.size(); i++) {
			String blockName = blockNames.get(i);
			String totalReadAsString = reads.get(i);
			double totalRead = Double.valueOf(totalReadAsString);
			totalReadsByBlock.put(blockName, totalRead);
		}

		return totalReadsByBlock;
	}

	private static void generateHeatMapFrom(Map<String, Double> blockToValueMap, String title, File outputFile, ImageType imageType) throws Exception {
		String[][] blockLayout = new String[][] { { "BLOCK49", "BLOCK50", "BLOCK51", "BLOCK52" }, { "BLOCK53", "BLOCK54", "BLOCK55", "BLOCK56" }, { "BLOCK57", "BLOCK58", "BLOCK59", "BLOCK60" },
				{ "BLOCK61", "BLOCK62", "BLOCK63", "BLOCK64" }, { "BLOCK65", "BLOCK66", "BLOCK67", "BLOCK68" }, { "BLOCK69", "BLOCK70", "BLOCK71", "BLOCK72" },
				{ "BLOCK25", "BLOCK26", "BLOCK27", "BLOCK28" }, { "BLOCK29", "BLOCK30", "BLOCK31", "BLOCK32" }, { "BLOCK33", "BLOCK34", "BLOCK35", "BLOCK36" },
				{ "BLOCK37", "BLOCK38", "BLOCK39", "BLOCK40" }, { "BLOCK41", "BLOCK42", "BLOCK43", "BLOCK44" }, { "BLOCK45", "BLOCK46", "BLOCK47", "BLOCK48" },
				{ "BLOCK01", "BLOCK02", "BLOCK03", "BLOCK04" }, { "BLOCK05", "BLOCK06", "BLOCK07", "BLOCK08" }, { "BLOCK09", "BLOCK10", "BLOCK11", "BLOCK12" },
				{ "BLOCK13", "BLOCK14", "BLOCK15", "BLOCK16" }, { "BLOCK17", "BLOCK18", "BLOCK19", "BLOCK20" }, { "BLOCK21", "BLOCK22", "BLOCK23", "BLOCK24" } };

		double minValue = Double.MAX_VALUE;
		double maxValue = Double.MIN_VALUE;

		for (Double value : blockToValueMap.values()) {
			minValue = Math.min(minValue, value);
			maxValue = Math.max(maxValue, value);
		}

		Color color = Color.blue;

		ColorScale colorScale = new ColorScale(color, minValue, maxValue);

		Graphics2DImageExporter imageExporter = new Graphics2DImageExporter(imageType, 400, 900);
		Graphics2D graphics = imageExporter.getGraphics2D();

		graphics.setPaint(Color.BLACK);

		FontMetrics metrics = graphics.getFontMetrics(graphics.getFont());
		int textHeight = metrics.getHeight();

		graphics.drawString(title, 10, 10 + textHeight);

		int rowHeight = 45;
		int columnWidth = 85;

		int currentY = 40;

		for (String[] row : blockLayout) {
			int currentX = 10;
			for (String block : row) {
				Double value = blockToValueMap.get(block);
				if (value == null) {
					value = 0.0;
				}
				Color blockColor = colorScale.getColor(value);
				graphics.setPaint(blockColor);
				graphics.fillRect(currentX, currentY, columnWidth, rowHeight);

				graphics.setPaint(Color.BLACK);
				graphics.drawRect(currentX, currentY, columnWidth, rowHeight);
				graphics.drawString(block, currentX + 4, currentY + textHeight);
				graphics.drawString("" + decimalFormat.format(value), currentX + 4, currentY + ((2 * textHeight) + 5));

				currentX += columnWidth;
			}
			currentY += rowHeight;
		}

		imageExporter.exportImage(outputFile.getAbsolutePath());

	}

	private static void generateBlockReports(File resultsDirectory, File blockFile) throws IOException {
		Map<Probe, String> probeToBlockMap = getProbeToBlockMap(blockFile);

		try {
			createUidBlockReport(10, new File(resultsDirectory, "/reports/probe_uid_quality.txt"), new File(resultsDirectory, "/block_report_by_read.txt"), new File(resultsDirectory,
					"/block_report_by_block.txt"), probeToBlockMap);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			createUidBlockBaseNucleotideCompositionReport(new File(resultsDirectory, "/reports/probe_uid_quality.txt"), probeToBlockMap, new File(resultsDirectory,
					"/unique_nucleotide_composition_by_block.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		addBlockToExtensionPrimerAlignmentReport(new File(resultsDirectory, "/reports/extension_primer_alignment.txt"), probeToBlockMap, new File(resultsDirectory,
				"/reports/extension_primer_alignment_with_blocks.txt"));

	}

	private static void addBlockToExtensionPrimerAlignmentReport(File extensionPrimerAlignmentReportFile, Map<Probe, String> probeToBlockMap, File outputFile) throws IOException {
		String[] extensionPrimerHeaders = new String[] { "probe_sequence_name", "capture_target_start", "capture_target_stop", "probe_strand" };

		Map<String, List<String>> primerHeadersToData = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(extensionPrimerAlignmentReportFile, extensionPrimerHeaders, StringUtil.TAB);

		List<String> probeUidQualitySequenceNames = primerHeadersToData.get(extensionPrimerHeaders[0]);
		List<String> probeUidQualityStart = primerHeadersToData.get(extensionPrimerHeaders[1]);
		List<String> probeUidQualityStop = primerHeadersToData.get(extensionPrimerHeaders[2]);
		List<String> probeUidQualityStrand = primerHeadersToData.get(extensionPrimerHeaders[3]);

		List<String> blockNameByLine = new ArrayList<String>();

		for (int probeUidIndex = 0; probeUidIndex < probeUidQualitySequenceNames.size(); probeUidIndex++) {
			String probeName = probeUidQualitySequenceNames.get(probeUidIndex);
			String probeStart = probeUidQualityStart.get(probeUidIndex);
			String probeStop = probeUidQualityStop.get(probeUidIndex);
			String probeStrand = probeUidQualityStrand.get(probeUidIndex);
			String block = probeToBlockMap.get(new Probe(probeName, Integer.valueOf(probeStart), Integer.valueOf(probeStop), probeStrand));
			if (block != null) {
				blockNameByLine.add(block);
			} else {
				blockNameByLine.add("NOT_FOUND");
			}
		}
		// if file doesn't exists, then create it
		if (!outputFile.exists()) {
			outputFile.createNewFile();
		}

		FileWriter fw = new FileWriter(outputFile);
		try (BufferedWriter bw = new BufferedWriter(fw)) {
			try (BufferedReader reader = new BufferedReader(new FileReader(extensionPrimerAlignmentReportFile))) {
				int lineCount = 0;
				String line;
				while ((line = reader.readLine()) != null) {
					if (lineCount == 0) {
						bw.write(line + StringUtil.TAB + "block" + StringUtil.NEWLINE);
					} else {
						bw.write(line + StringUtil.TAB + blockNameByLine.get(lineCount - 1) + StringUtil.NEWLINE);
					}
					lineCount++;
				}
			}

		}

	}

	private static Map<String, Double> getAverageEditDistanceByBlock(File extensionPrimerAlignmentWithBlocks, File blockReportByBlock) throws IOException {
		Map<String, Integer> editDistanceSumByBlock = getEditDistanceSumByBlock(extensionPrimerAlignmentWithBlocks);
		Map<String, Integer> totalReadsByBlock = getTotalReadsByBlock(blockReportByBlock);
		Map<String, Double> averageEditDistanceByBlock = getAverageEditDistanceByBlock(editDistanceSumByBlock, totalReadsByBlock);
		return averageEditDistanceByBlock;
	}

	private static Map<String, Integer> getEditDistanceSumByBlock(File extensionPerimerAlignmentWithBlocks) throws IOException {
		String[] extensionPrimerHeaders = new String[] { "edit_distance", "block" };
		Map<String, List<String>> primerHeadersToData = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(extensionPerimerAlignmentWithBlocks, extensionPrimerHeaders, StringUtil.TAB);
		List<String> editDistances = primerHeadersToData.get(extensionPrimerHeaders[0]);
		List<String> blockNames = primerHeadersToData.get(extensionPrimerHeaders[1]);

		TallyMap<String> tallyMap = new TallyMap<String>();
		for (int i = 0; i < blockNames.size(); i++) {
			String editDistanceAsString = editDistances.get(i);
			int editDistance = Integer.valueOf(editDistanceAsString);
			String blockName = blockNames.get(i);
			tallyMap.addMultiple(blockName, editDistance);
		}

		return tallyMap.getTalliesAsMap();
	}

	private static Map<String, Double> getAverageUniqueUidLengthBlock(File blockReportByBlockFile) throws IOException {
		String[] blockFileHeaders = new String[] { "block", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "total_reads" };
		Map<String, List<String>> headersToData = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(blockReportByBlockFile, blockFileHeaders, StringUtil.TAB);
		List<String> blockNames = headersToData.get(blockFileHeaders[0]);
		List<String> ones = headersToData.get(blockFileHeaders[2]);
		List<String> twos = headersToData.get(blockFileHeaders[3]);
		List<String> threes = headersToData.get(blockFileHeaders[4]);
		List<String> fours = headersToData.get(blockFileHeaders[5]);
		List<String> fives = headersToData.get(blockFileHeaders[6]);
		List<String> sixes = headersToData.get(blockFileHeaders[7]);
		List<String> sevens = headersToData.get(blockFileHeaders[8]);
		List<String> eights = headersToData.get(blockFileHeaders[9]);
		List<String> nines = headersToData.get(blockFileHeaders[10]);
		List<String> totalReadItems = headersToData.get(blockFileHeaders[11]);

		Map<String, Double> averageUidLengthByBlock = new HashMap<String, Double>();
		for (int i = 0; i < blockNames.size(); i++) {
			double sumOfLengths = 0;
			sumOfLengths += (1 * Integer.valueOf(ones.get(i)));
			sumOfLengths += (2 * Integer.valueOf(twos.get(i)));
			sumOfLengths += (3 * Integer.valueOf(threes.get(i)));
			sumOfLengths += (4 * Integer.valueOf(fours.get(i)));
			sumOfLengths += (5 * Integer.valueOf(fives.get(i)));
			sumOfLengths += (6 * Integer.valueOf(sixes.get(i)));
			sumOfLengths += (7 * Integer.valueOf(sevens.get(i)));
			sumOfLengths += (8 * Integer.valueOf(eights.get(i)));
			sumOfLengths += (9 * Integer.valueOf(nines.get(i)));
			String blockName = blockNames.get(i);
			double totalReads = Double.valueOf(totalReadItems.get(i));
			double average = sumOfLengths / totalReads;
			averageUidLengthByBlock.put(blockName, average);
		}
		return averageUidLengthByBlock;
	}

	private static Map<String, Integer> getTotalReadsByBlock(File blockReportByBlock) throws IOException {
		String[] blockFileHeaders = new String[] { "total_reads", "block" };
		Map<String, List<String>> primerHeadersToData = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(blockReportByBlock, blockFileHeaders, StringUtil.TAB);
		List<String> totalReadEntries = primerHeadersToData.get(blockFileHeaders[0]);
		List<String> blockNames = primerHeadersToData.get(blockFileHeaders[1]);

		Map<String, Integer> totalReadsByBlocks = new HashMap<String, Integer>();
		for (int i = 0; i < blockNames.size(); i++) {
			String totalReadsAsString = totalReadEntries.get(i);
			Integer totalReads = Integer.valueOf(totalReadsAsString);
			String blockName = blockNames.get(i);
			totalReadsByBlocks.put(blockName, totalReads);
		}

		return totalReadsByBlocks;
	}

	private static Map<String, Double> getAverageEditDistanceByBlock(Map<String, Integer> editDistanceSumByBlock, Map<String, Integer> totalReadsByBlock) {
		Map<String, Double> averageEditDistanceByBlock = new HashMap<String, Double>();

		for (String blockName : editDistanceSumByBlock.keySet()) {
			double editDistance = editDistanceSumByBlock.get(blockName);
			double totalReads = totalReadsByBlock.get(blockName);
			double averageEditDistance = editDistance / totalReads;
			averageEditDistanceByBlock.put(blockName, averageEditDistance);
		}

		return averageEditDistanceByBlock;

	}

	private static void runEntries(Integer numberOfEntries) throws Exception {
		File probeFile = new File("D:/manufacturing_test/HS_EXOME_picked_mip_probe_arms_80k.txt");
		File blockFile = new File("D:/manufacturing_test/80k_block_assignment.txt");

		File fastq1_56 = new File("D:/manufacturing_test/UID-57_R1.fastq");
		File fastq2_56 = new File("D:/manufacturing_test/UID-57_R2.fastq");

		File baseOutputDirectory_56 = new File("D:/manufacturing_test/56/");
		processRandomNEntriesAndGenerateReports(fastq1_56, fastq2_56, probeFile, blockFile, numberOfEntries, baseOutputDirectory_56);

		File fastq1_57 = new File("D:/manufacturing_test/UID-56_R1.fastq");
		File fastq2_57 = new File("D:/manufacturing_test/UID-56_R2.fastq");
		File baseOutputDirectory_57 = new File("D:/manufacturing_test/57/");
		processRandomNEntriesAndGenerateReports(fastq1_57, fastq2_57, probeFile, blockFile, numberOfEntries, baseOutputDirectory_57);

	}

	private static void runUnMappableEntries(Integer numberOfEntries) throws Exception {
		File probeFile = new File("D:/manufacturing_test/HS_EXOME_picked_mip_probe_arms_80k.txt");
		File blockFile = new File("D:/manufacturing_test/80k_block_assignment.txt");

		File fastq1_56 = new File("D:/manufacturing_test/56/10000000/reports/unable_to_map_one.fastq");
		File fastq2_56 = new File("D:/manufacturing_test/56/10000000/reports/unable_to_map_two.fastq");

		File baseOutputDirectory_56 = new File("D:/manufacturing_test/56/");
		processRandomNEntriesAndGenerateReports(fastq1_56, fastq2_56, probeFile, blockFile, numberOfEntries, baseOutputDirectory_56);
	}

	public static void convertFastqToFasta(File fastqFile, File outputFastaFile) throws IOException {
		StringBuilder fastaString = new StringBuilder();
		try (FastqReader fastQReader = new FastqReader(fastqFile)) {
			while (fastQReader.hasNext()) {
				FastqRecord record = fastQReader.next();
				fastaString.append(">" + record.getReadHeader() + StringUtil.NEWLINE + record.getReadString() + StringUtil.NEWLINE);
			}
		}
		FileUtils.writeStringToFile(outputFastaFile, fastaString.toString());
	}

	public static void combinePairIntoFasta(File fastqOneFile, File fastqTwoFile, File outputFastaFile) throws IOException {
		StringBuilder fastaString = new StringBuilder();
		try (FastqReader fastQOneReader = new FastqReader(fastqOneFile)) {
			try (FastqReader fastQTwoReader = new FastqReader(fastqTwoFile)) {
				while (fastQOneReader.hasNext()) {
					while (fastQTwoReader.hasNext()) {

						FastqRecord recordOne = fastQOneReader.next();
						FastqRecord recordTwo = fastQTwoReader.next();
						ISequence recordTwoSequence = new IupacNucleotideCodeSequence(recordTwo.getReadString());
						fastaString.append(">" + recordOne.getReadHeader() + StringUtil.NEWLINE + recordOne.getReadString() + "N" + recordTwoSequence.getReverseCompliment().toString()
								+ StringUtil.NEWLINE);
					}
				}
			}
		}
		FileUtils.writeStringToFile(outputFastaFile, fastaString.toString());
	}

	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();

		runEntries(null);

		long stop = System.currentTimeMillis();
		System.out.println("total time:" + (stop - start) + "ms");
	}

}
