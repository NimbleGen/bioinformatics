/*
 *    Copyright 2016 Roche NimbleGen Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.roche.heatseq.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.picard.io.IoUtil;
import net.sf.picard.sam.ValidateSamFile;
import net.sf.samtools.BAMIndexer;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

import com.roche.heatseq.objects.ParsedProbeFile;
import com.roche.heatseq.objects.Probe;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;

public class BamFileUtil {

	public final static short PHRED_ASCII_OFFSET = 33;

	private BamFileUtil() {
		throw new AssertionError();
	}

	/**
	 * @param baseQualityString
	 * @return the cumulative quality score for the given baseQualityString
	 */
	public static short getQualityScore(String baseQualityString) {
		short sumOfQualityScores = 0;

		for (int i = 0; i < baseQualityString.length(); i++) {
			short nonAdjustedBaseScore = (short) baseQualityString.charAt(i);
			short baseScore = (short) (nonAdjustedBaseScore - PHRED_ASCII_OFFSET);

			sumOfQualityScores += baseScore;
		}

		return sumOfQualityScores;
	}

	public static void createIndexOnCoordinateSortedBamFile(SAMFileReader samReader, File outputIndexFile) {
		createIndex(samReader, outputIndexFile);
	}

	/**
	 * Generates a BAM index file from an input BAM file. Uses the same filename as the input file and appends a .bai to the end of the file
	 * 
	 * @param inputBamFile
	 *            input BAM file
	 * @param outputBamIndex
	 *            File for output index file
	 */
	public static void createIndex(File inputBamFile) {
		String outputBamIndexFileName = inputBamFile + ".bai";
		File outputBamIndexFile = new File(outputBamIndexFileName);
		final SAMFileReader reader = new SAMFileReader(IoUtil.openFileForReading(inputBamFile));
		createIndex(reader, outputBamIndexFile);
	}

	/**
	 * Generates a BAM index file from an input BAM file reader
	 * 
	 * @param reader
	 *            SAMFileReader for input BAM file
	 * @param output
	 *            File for output index file
	 */
	public static void createIndex(SAMFileReader reader, File output) {

		BAMIndexer indexer = new BAMIndexer(output, reader.getFileHeader());

		reader.enableFileSource(true);

		// create and write the content
		for (SAMRecord rec : reader) {
			indexer.processAlignment(rec);
		}
		indexer.finish();

	}

	public static void convertSamToBam(File inputSamFile, File outputBamFile) {
		picardSortAndCompress(true, inputSamFile, outputBamFile, SortOrder.coordinate, null);
	}

	public static void convertBamToSam(File inputBamFile, File outputSamFile) {
		picardSortAndCompress(false, inputBamFile, outputSamFile, SortOrder.coordinate, null);
	}

	/**
	 * Sort the provided input file by readname and place the result in output
	 * 
	 * @param input
	 * @param output
	 * @return output
	 */
	public static File sortOnReadName(File input, File output) {
		return picardSortAndCompress(true, input, output, SortOrder.queryname, null);
	}

	/**
	 * Sort the provided input file by coordinates and place the result in output
	 * 
	 * @param input
	 * @param output
	 * @return output
	 */
	public static File sortOnCoordinates(File input, File output) {
		return picardSortAndCompress(true, input, output, SortOrder.coordinate, null);
	}

	/**
	 * Sort the provided input file by coordinates and place the result in output
	 * 
	 * @param input
	 * @param output
	 * @return output
	 */
	public static File sortOnCoordinatesAndExcludeReads(File input, File output, Set<String> readNamesToExclude) {
		return picardSortAndCompress(true, input, output, SortOrder.coordinate, readNamesToExclude);
	}

	/**
	 * Use picard to sort and compress the provided input file
	 * 
	 * @param input
	 * @param output
	 * @param sortOrder
	 * @return
	 */
	private static File picardSortAndCompress(boolean outputAsBam, File input, File output, SortOrder sortOrder, Set<String> readNamesToExclude) {
		IoUtil.assertFileIsReadable(input);
		IoUtil.assertFileIsWritable(output);

		final SAMFileReader reader = new SAMFileReader(IoUtil.openFileForReading(input));

		SAMFileHeader header = reader.getFileHeader();
		header.setSortOrder(sortOrder);

		SAMFileWriter writer = null;

		if (outputAsBam) {
			writer = new SAMFileWriterFactory().makeBAMWriter(header, false, output, 9);
		} else {
			writer = new SAMFileWriterFactory().makeSAMWriter(header, false, output);
		}

		for (final SAMRecord record : reader) {
			boolean shouldInclude = true;
			if (readNamesToExclude != null) {
				String readName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(record.getReadName());
				shouldInclude = !readNamesToExclude.contains(readName);
			}

			if (shouldInclude) {
				writer.addAlignment(record);
			}
		}
		writer.close();
		reader.close();

		return output;
	}

	/**
	 * Use picards validateSamFile to validate the provided inputBamFile
	 * 
	 * @param inputBamFile
	 * @param outputErrorFile
	 */
	public static void validateSamFile(File inputBamFile, File outputErrorFile) {
		ValidateSamFile.main(new String[] { "INPUT=" + inputBamFile.getAbsolutePath(), "OUTPUT=" + outputErrorFile.getAbsolutePath() });
	}

	/**
	 * Creates a new file header for our output BAM file
	 * 
	 * @param originalHeader
	 * @param probeInfo
	 * @param commandLineSignature
	 * @param programName
	 * @param programVersion
	 * @return
	 */
	public static SAMFileHeader getHeader(ParsedProbeFile probeInfo, String commandLineSignature, String programName, String programVersion, boolean excludeProgramInBamHeader) {
		return getHeader(false, excludeProgramInBamHeader, null, probeInfo, commandLineSignature, programName, programVersion);
	}

	/**
	 * Creates a new file header for our output BAM file
	 * 
	 * @param originalHeader
	 * @param probeInfo
	 * @param commandLineSignature
	 * @param programName
	 * @param programVersion
	 * @return
	 */
	public static SAMFileHeader getHeader(boolean onlyIncludeSequencesFromProbeFile, boolean excludeProgramInBamHeader, SAMFileHeader originalHeader, ParsedProbeFile probeInfo,
			String commandLineSignature, String programName, String programVersion) {
		SAMFileHeader newHeader = new SAMFileHeader();

		List<SAMProgramRecord> programRecords = new ArrayList<SAMProgramRecord>();
		if (originalHeader != null) {
			newHeader.setReadGroups(originalHeader.getReadGroups());
			programRecords.addAll(originalHeader.getProgramRecords());
		}

		if (!excludeProgramInBamHeader) {
			String uniqueProgramGroupId = programName + "_" + DateUtil.getCurrentDateINYYYY_MM_DD_HH_MM_SS();
			SAMProgramRecord programRecord = new SAMProgramRecord(uniqueProgramGroupId);
			programRecord.setProgramName(programName);
			programRecord.setProgramVersion(programVersion);
			programRecord.setCommandLine(commandLineSignature);
			programRecords.add(programRecord);
			newHeader.setProgramRecords(programRecords);
		}

		SAMSequenceDictionary sequenceDictionary = new SAMSequenceDictionary();
		if (originalHeader != null) {
			for (SAMSequenceRecord oldSequenceRecord : originalHeader.getSequenceDictionary().getSequences()) {
				if (!onlyIncludeSequencesFromProbeFile || probeInfo.containsSequenceName(oldSequenceRecord.getSequenceName())) {
					SAMSequenceRecord newSequenceRecord = new SAMSequenceRecord(oldSequenceRecord.getSequenceName(), oldSequenceRecord.getSequenceLength());
					sequenceDictionary.addSequence(newSequenceRecord);
				}
			}
		} else {
			for (String sequenceName : probeInfo.getSequenceNames()) {
				List<Probe> probes = probeInfo.getProbesBySequenceName(sequenceName);
				int lastPosition = 0;
				for (Probe probe : probes) {
					lastPosition = Math.max(lastPosition, probe.getStop() + 1);
				}
				SAMSequenceRecord newSequenceRecord = new SAMSequenceRecord(sequenceName, lastPosition);
				sequenceDictionary.addSequence(newSequenceRecord);
			}
		}
		newHeader.setSequenceDictionary(sequenceDictionary);
		return newHeader;
	}

	public static Map<String, Integer> getContainerSizesFromHeader(SAMFileHeader header) {
		Map<String, Integer> containerSizesByContainerName = new LinkedHashMap<String, Integer>();
		for (SAMSequenceRecord sequence : header.getSequenceDictionary().getSequences()) {
			containerSizesByContainerName.put(sequence.getSequenceName(), sequence.getSequenceLength());
		}
		return containerSizesByContainerName;
	}

	public static boolean isSortedBasedOnHeader(File bamFile, SortOrder sortOrder) {
		IoUtil.assertFileIsReadable(bamFile);

		final SAMFileReader reader = new SAMFileReader(IoUtil.openFileForReading(bamFile));

		SAMFileHeader header = reader.getFileHeader();
		boolean isSorted = header.getSortOrder().equals(SortOrder.coordinate);

		reader.close();

		return isSorted;
	}

	public static void main(String[] args) {
		// File inputBamFile = new File("S:\\public\\candace\\dsHybrid.bam");
		// File inputBamFile = new File("C:\\Users\\heilmank\\Desktop\\junk\\dsHybrid.srt_REDUCED.bam");
		// File outputErrorFile = new File("C:\\Users\\heilmank\\Desktop\\junk\\validation_errors.txt");
		// validateSamFile(inputBamFile, outputErrorFile);
		// createIndex(new File("D:/trim/kurt_trimmed.bam"));
		convertBamToSam(new File("D:/kurts_space/heatseq/carolina_818/S2_2_sorted.bam"), new File("D:/kurts_space/heatseq/carolina_818/S2_2_sorted.sam"));
	}

}
