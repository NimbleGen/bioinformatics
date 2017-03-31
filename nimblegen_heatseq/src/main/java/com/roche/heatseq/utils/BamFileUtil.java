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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.roche.heatseq.process.BamFileValidator;
import com.roche.sequencing.bioinformatics.common.alignment.CigarString;
import com.roche.sequencing.bioinformatics.common.alignment.CigarStringUtil;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
import com.roche.sequencing.bioinformatics.common.alignment.SimpleAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.genome.Genome;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.IlluminaFastQReadNameUtil;
import com.roche.sequencing.bioinformatics.common.utils.ListUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.fastq.PicardException;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ParsedProbeFile;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.Probe;

import htsjdk.samtools.BAMIndexer;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileReader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.IOUtil;

@SuppressWarnings("deprecation")
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

	/**
	 * Generates a BAM index file from an input BAM file. Uses the same filename as the input file and appends a .bai to the end of the file
	 * 
	 * @param inputBamFile
	 *            input BAM file
	 * @param outputBamIndex
	 *            File for output index file
	 */
	public static File createIndex(File inputBamFile) {
		String outputBamIndexFileName = inputBamFile + ".bai";
		File outputBamIndexFile = new File(outputBamIndexFileName);
		createIndex(inputBamFile, outputBamIndexFile);
		return outputBamIndexFile;
	}

	/**
	 * Generates a BAM index file from an input BAM file. Uses the same filename as the input file and appends a .bai to the end of the file
	 * 
	 * @param inputBamFile
	 *            input BAM file
	 * @param outputBamIndex
	 *            File for output index file
	 */
	public static void createIndex(File inputBamFile, File outputBamIndexFile) {
		deprecatedCreateIndex(inputBamFile, outputBamIndexFile);
	}

	// TODO This is still being used because I haven't had the time to figure out
	// how this is done in the new API
	private static void deprecatedCreateIndex(File inputBamFile, File outputBamIndexFile) {
		// make sure that the default validation strategy for the SamReaderFactory is used here too.
		SAMFileReader.setDefaultValidationStringency(SamReaderFactory.makeDefault().validationStringency());

		try (SAMFileReader reader = new SAMFileReader(inputBamFile)) {
			BAMIndexer indexer = new BAMIndexer(outputBamIndexFile, reader.getFileHeader());

			reader.enableFileSource(true);

			// create and write the content
			for (SAMRecord rec : reader) {
				indexer.processAlignment(rec);
			}
			indexer.finish();
		}
	}

	public static void convertSamToBam(File inputSamFile, File outputBamFile) {
		picardSortAndCompress(true, inputSamFile, outputBamFile, SortOrder.coordinate, null);
	}

	private static void convertBamToSam(File inputBamFile, File outputSamFile) {
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
		IOUtil.assertFileIsReadable(input);
		IOUtil.assertFileIsWritable(output);

		try (SamReader reader = SamReaderFactory.makeDefault().open(input)) {

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
		} catch (IOException e) {
			throw new PicardException(e.getMessage(), e);
		}

		return output;
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
		}
		newHeader.setProgramRecords(programRecords);

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
		IOUtil.assertFileIsReadable(bamFile);
		boolean isSorted = false;
		try (SamReader reader = SamReaderFactory.makeDefault().open(bamFile)) {
			SAMFileHeader header = reader.getFileHeader();
			isSorted = header.getSortOrder().equals(SortOrder.coordinate);
		} catch (IOException e) {
			throw new PicardException(e.getMessage(), e);
		}
		return isSorted;
	}

	public static void main(String[] args) throws IOException {
		// validateBamFiles();
		compareBamFiles();
	}

	public static void compareBamFiles() throws FileNotFoundException {
		File newBamFile = new File("C:\\Users\\heilmank\\Desktop\\ryan\\dedup.bam");
		// this is the old
		File oldBamFile = new File("C:\\Users\\heilmank\\Desktop\\ryan\\dedup3.bam");
		compareBamFiles(oldBamFile, newBamFile);
	}

	public static void validateBamFiles() throws IOException {
		Genome genome = new Genome(new File("R:\\SoftwareDevelopment\\GenomeViewer\\hg38.gnm"));
		// Genome genome = new Genome(new File("R:\\SoftwareDevelopment\\GenomeViewer\\hg19.gnm"));

		// File directory = new File("S:\\workspace\\Caroline\\CN_311556\\fixed_full_analysis\\");
		// for (File sampleDir : directory.listFiles())
		// if (sampleDir.isDirectory()) {
		// for (File inputBamFile : sampleDir.listFiles(new FileFilter() {
		//
		// @Override
		// public boolean accept(File pathname) {
		// return pathname.getName().endsWith("dedup.bam");
		// }
		// })) {
		// validateCigarStringsAndReadLengths(inputBamFile, genome);
		// }
		// }

		// File inputBamFile = new File("C:\\Users\\heilmank\\Desktop\\D1415017-nt-opgez_S8_L001_dedup.bam");
		// File inputBamFile = new File("S:\\public\\kurt\\new_bug\\filtered_full_analysis\\S8_D1615160-opgezuiverd_S6_L001\\S8_D1615160-opgezuiverd_S6_L001_dedup.bam");
		// File inputBamFile = new File("S:\\public\\kurt\\new_bug\\filtered_full_analysis\\debug\\S8_D1615160-opgezuiverd_S6_L001\\S8_D1615160-opgezuiverd_S6_L001_dedup.bam");
		// File inputBamFile = new File("C:\\Users\\heilmank\\Desktop\\ryan2\\input.bam");
		// File inputBamFile = new File("C:\\Users\\heilmank\\Desktop\\ryan2\\dedup.bam");

		File inputBamFile = new File("C:\\Users\\heilmank\\Desktop\\ryan\\dedup.bam");
		// File inputBamFile = new File("C:\\Users\\heilmank\\Desktop\\ryan\\results\\S07_superpool1capture1_S1_35380000reads_dedup.bam");
		BamFileValidator.validate(inputBamFile);
		// File inputBamFile = new File("C:\\Users\\heilmank\\Desktop\\ryan2\\S01_superpool1capture1_S1_all_reads_sorted.bam");
		// validateCigarStringsAndReadLengths(inputBamFile, genome);
		// File inputBamFile = new File("C:\\Users\\heilmank\\Desktop\\junk\\dsHybrid.srt_REDUCED.bam");

		// createIndex(new File("D:/trim/kurt_trimmed.bam"));
		// convertBamToSam(new File("D:/kurts_space/heatseq/carolina_818/S2_2_sorted.bam"), new File("D:/kurts_space/heatseq/carolina_818/S2_2_sorted.sam"));
		boolean verbose = true;
		checkBamCigarString(inputBamFile, genome, verbose);
	}

	private static void compareBamFiles(File oldSamFile, File newSamFile) throws FileNotFoundException {
		Map<String, RecordPair> oldBamMap = parseBamFile(oldSamFile);
		Map<String, RecordPair> newBamMap = parseBamFile(newSamFile);

		int inOldButNotNew = 0;
		int inNewButNotOld = 0;
		int inBoth = 0;

		Set<String> allReadNames = new HashSet<>(oldBamMap.keySet());
		allReadNames.addAll(newBamMap.keySet());

		ComparisonResult allRawResults = null;
		ComparisonResult allFixedResults = null;

		for (String readName : allReadNames) {
			RecordPair oldRecordPair = oldBamMap.get(readName);
			RecordPair newRecordPair = newBamMap.get(readName);
			if (oldRecordPair == null && newRecordPair != null) {
				inNewButNotOld++;
			} else if (oldRecordPair != null && newRecordPair == null) {
				inOldButNotNew++;
			} else if (oldRecordPair != null && newRecordPair != null) {
				inBoth++;

				String oldOneReadString = oldRecordPair.getRecordOne().getReadString();
				String oldTwoReadString = oldRecordPair.getRecordTwo().getReadString();
				String newOneReadString = newRecordPair.getRecordOne().getReadString();
				String newTwoReadString = newRecordPair.getRecordTwo().getReadString();

				String oldOneCigar = oldRecordPair.getRecordOne().getCigar().toString();
				String oldTwoCigar = oldRecordPair.getRecordTwo().getCigar().toString();
				String newOneCigar = newRecordPair.getRecordOne().getCigar().toString();
				String newTwoCigar = newRecordPair.getRecordTwo().getCigar().toString();

				String oldOneMd = (String) oldRecordPair.getRecordOne().getAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG);
				String oldTwoMd = (String) oldRecordPair.getRecordTwo().getAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG);
				String newOneMd = (String) newRecordPair.getRecordOne().getAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG);
				String newTwoMd = (String) newRecordPair.getRecordTwo().getAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG);

				int oldOneStart = oldRecordPair.getRecordOne().getAlignmentStart();
				int oldTwoStart = oldRecordPair.getRecordTwo().getAlignmentStart();
				int newOneStart = newRecordPair.getRecordOne().getAlignmentStart();
				int newTwoStart = newRecordPair.getRecordTwo().getAlignmentStart();

				int oldOneStop = oldRecordPair.getRecordOne().getAlignmentEnd();
				int oldTwoStop = oldRecordPair.getRecordTwo().getAlignmentEnd();
				int newOneStop = newRecordPair.getRecordOne().getAlignmentEnd();
				int newTwoStop = newRecordPair.getRecordTwo().getAlignmentEnd();

				ComparisonResult result = compare(oldOneReadString, oldTwoReadString, newOneReadString, newTwoReadString, oldOneCigar, oldTwoCigar, newOneCigar, newTwoCigar, oldOneMd, oldTwoMd,
						newOneMd, newTwoMd, oldOneStart, oldTwoStart, newOneStart, newTwoStart, oldOneStop, oldTwoStop, newOneStop, newTwoStop);
				if (allRawResults == null) {
					allRawResults = result;
				} else {
					allRawResults.add(result);
				}

				// try and fix and then compare
				String fixedNewOneCigar = convertNewCigarStringToOld(newOneCigar);
				String fixedNewTwoCigar = convertNewCigarStringToOld(newTwoCigar);

				String fixedNewOneMd = convertNewMismatchDetailsStringToOld(newOneMd);
				String fixedNewTwoMd = convertNewMismatchDetailsStringToOld(newTwoMd);

				int fixedNewOneStop = convertNewStopToOld(newOneStop, newOneCigar);
				int fixedNewTwoStop = convertNewStopToOld(newTwoStop, newTwoCigar);

				ComparisonResult fixedResult = compare(oldOneReadString, oldTwoReadString, newOneReadString, newTwoReadString, oldOneCigar, oldTwoCigar, fixedNewOneCigar, fixedNewTwoCigar, oldOneMd,
						oldTwoMd, fixedNewOneMd, fixedNewTwoMd, oldOneStart, oldTwoStart, newOneStart, newTwoStart, oldOneStop, oldTwoStop, fixedNewOneStop, fixedNewTwoStop);
				if (allFixedResults == null) {
					allFixedResults = fixedResult;
				} else {
					allFixedResults.add(fixedResult);
				}

				String recordOneStrand = "FORWARD";
				if (oldRecordPair.getRecordOne().getReadFailsVendorQualityCheckFlag()) {
					recordOneStrand = "REVERSE";
				}

				String recordTwoStrand = "FORWARD";
				if (oldRecordPair.getRecordTwo().getReadFailsVendorQualityCheckFlag()) {
					recordTwoStrand = "REVERSE";
				}

				if (!fixedResult.allMatch()) {
					System.out.println("=============================================================");
					System.out.println(readName);
					System.out.println("-----" + ":ONE:" + recordOneStrand + "-------------------------------------");
					System.out.println(oldRecordPair.getRecordOne().getReferenceName() + ":" + oldRecordPair.getRecordOne().getAlignmentStart() + "-" + oldRecordPair.getRecordOne().getAlignmentEnd());
					outputComparisonOfRecords(oldOneReadString, oldOneCigar, oldOneMd, oldOneStart, oldOneStop, newOneReadString, fixedNewOneCigar, fixedNewOneMd, newOneStart, fixedNewOneStop);

					System.out.println("-----" + ":TWO:" + recordTwoStrand + "-------------------------------------");
					System.out.println(oldRecordPair.getRecordOne().getReferenceName() + ":" + oldRecordPair.getRecordOne().getAlignmentStart() + "-" + oldRecordPair.getRecordOne().getAlignmentEnd());
					outputComparisonOfRecords(oldTwoReadString, oldTwoCigar, oldTwoMd, oldTwoStart, oldTwoStop, newTwoReadString, fixedNewTwoCigar, fixedNewTwoMd, newTwoStart, fixedNewTwoStop);
					System.out.println("=============================================================");
				}
			}
		}
		System.out.println("--------------------------------------");
		System.out.println("RAW RESULTS:");
		System.out.println(allRawResults.getReport());
		System.out.println("FIXED RESULTS:");
		System.out.println(allFixedResults.getReport());

		System.out.println("In_Old_But_Not_New:" + inOldButNotNew + " In_New_But_Not_Old:" + inNewButNotOld + " In_Both:" + inBoth);
	}

	public static ComparisonResult compare(String oldOneReadString, String oldTwoReadString, String newOneReadString, String newTwoReadString, String oldOneCigar, String oldTwoCigar,
			String newOneCigar, String newTwoCigar, String oldOneMd, String oldTwoMd, String newOneMd, String newTwoMd, int oldOneStart, int oldTwoStart, int newOneStart, int newTwoStart,
			int oldOneStop, int oldTwoStop, int newOneStop, int newTwoStop) {
		boolean oneMatch = oldOneReadString.equals(newOneReadString);
		boolean twoMatch = oldTwoReadString.equals(newTwoReadString);
		boolean sequencesDiffer = (!oneMatch || !twoMatch);

		boolean oneCigarMatch = oldOneCigar.equals(newOneCigar);
		boolean twoCigarMatch = oldTwoCigar.toString().equals(newTwoCigar);
		boolean cigarsDiffer = (!oneCigarMatch || !twoCigarMatch);

		boolean oneMdMatch = oldOneMd.equals(newOneMd);
		boolean twoMdMatch = oldTwoMd.equals(newTwoMd);
		boolean mismatchDetailsDiffer = (!oneMdMatch || !twoMdMatch);

		boolean oneStartMatch = oldOneStart == newOneStart;
		boolean twoStartMatch = oldTwoStart == newTwoStart;
		boolean startsDiffer = (!oneStartMatch || !twoStartMatch);

		boolean oneStopMatch = oldOneStop == newOneStop;
		boolean twoStopMatch = oldTwoStop == newTwoStop;
		boolean stopsDiffer = (!oneStopMatch || !twoStopMatch);

		return new ComparisonResult(sequencesDiffer, cigarsDiffer, mismatchDetailsDiffer, startsDiffer, stopsDiffer);
	}

	private static class ComparisonResult {
		private int sequencesDiffer;
		private int cigarsDiffer;
		private int mismatchDetailsDiffer;
		private int startsDiffer;
		private int stopsDiffer;
		private int totalReadPairs;
		private int allMatch;

		public ComparisonResult(boolean sequencesDiffer, boolean cigarsDiffer, boolean mismatchDetailsDiffer, boolean startsDiffer, boolean stopsDiffer) {
			super();
			if (sequencesDiffer) {
				this.sequencesDiffer = 1;
			}
			if (cigarsDiffer) {
				this.cigarsDiffer = 1;
			}
			if (mismatchDetailsDiffer) {
				this.mismatchDetailsDiffer = 1;
			}
			if (startsDiffer) {
				this.startsDiffer = 1;
			}
			if (stopsDiffer) {
				this.stopsDiffer = 1;
			}

			if (!sequencesDiffer && !cigarsDiffer && !mismatchDetailsDiffer && !startsDiffer && !stopsDiffer) {
				this.allMatch = 1;
			}

			this.totalReadPairs = 1;
		}

		public boolean allMatch() {
			return this.totalReadPairs == this.allMatch;
		}

		public void add(ComparisonResult result) {
			sequencesDiffer += result.sequencesDiffer;
			cigarsDiffer += result.cigarsDiffer;
			mismatchDetailsDiffer += result.mismatchDetailsDiffer;
			startsDiffer += result.startsDiffer;
			stopsDiffer += result.stopsDiffer;
			allMatch += result.allMatch;
			totalReadPairs += result.totalReadPairs;
		}

		public String getReport() {
			StringBuilder report = new StringBuilder();
			report.append("reads_cigar_do_not_match:" + cigarsDiffer + " reads_cigar_match:" + (totalReadPairs - cigarsDiffer) + StringUtil.NEWLINE);
			report.append("reads_md_do_not_match:" + mismatchDetailsDiffer + " reads_md_match:" + (totalReadPairs - mismatchDetailsDiffer) + StringUtil.NEWLINE);
			report.append("reads_start_do_not_match:" + startsDiffer + " reads_start_match:" + (totalReadPairs - startsDiffer) + StringUtil.NEWLINE);
			report.append("reads_stop_do_not_match:" + stopsDiffer + " reads_stop_match:" + (totalReadPairs - stopsDiffer) + StringUtil.NEWLINE);
			report.append("all_match:" + allMatch + " reads_not_all_match:" + (totalReadPairs - allMatch) + StringUtil.NEWLINE);
			return report.toString();
		}
	}

	private static String convertNewCigarStringToOld(String newCigarString) {
		String returnCigarString = newCigarString;
		if (newCigarString.endsWith("D")) {
			int endIndex = newCigarString.length() - 2;
			while (Character.isDigit(newCigarString.charAt(endIndex))) {
				endIndex--;
			}
			returnCigarString = newCigarString.substring(0, endIndex + 1);
		}

		return returnCigarString;
	}

	private static int convertNewStopToOld(int stop, String newCigarString) {
		int returnStop = stop;
		if (newCigarString.endsWith("D")) {
			int endIndex = newCigarString.length() - 2;
			StringBuilder numberBuilder = new StringBuilder();
			while (Character.isDigit(newCigarString.charAt(endIndex))) {
				numberBuilder.append(newCigarString.charAt(endIndex));
				endIndex--;
			}

			Integer value = Integer.parseInt(StringUtil.reverse(numberBuilder.toString()));
			returnStop = stop - value;
		}

		return returnStop;
	}

	private static boolean isNucleotide(char character) {
		char upperCase = Character.toUpperCase(character);
		boolean isNucleotide = upperCase == 'A' || upperCase == 'C' || upperCase == 'G' || upperCase == 'T';
		return isNucleotide;
	}

	private static String convertNewMismatchDetailsStringToOld(String newMismatchDetailsString) {
		String returnMismatchDetailsString = newMismatchDetailsString;
		int endIndex = newMismatchDetailsString.length() - 1;
		while (isNucleotide(newMismatchDetailsString.charAt(endIndex))) {
			endIndex--;
		}
		if (newMismatchDetailsString.charAt(endIndex) == '^') {
			returnMismatchDetailsString = newMismatchDetailsString.substring(0, endIndex);
		}

		returnMismatchDetailsString = newMismatchDetailsString.replace("A", "Z");
		returnMismatchDetailsString = newMismatchDetailsString.replace("T", "A");
		returnMismatchDetailsString = newMismatchDetailsString.replace("Z", "T");

		returnMismatchDetailsString = newMismatchDetailsString.replace("G", "Z");
		returnMismatchDetailsString = newMismatchDetailsString.replace("C", "G");
		returnMismatchDetailsString = newMismatchDetailsString.replace("Z", "C");

		return returnMismatchDetailsString;
	}

	public static void outputComparisonOfRecords(String recordOneReadString, String recordOneCigarString, String recordOneMismatchDetails, int recordOneStart, int recordOneStop,
			String recordTwoReadString, String recordTwoCigarString, String recordTwoMismatchDetails, int recordTwoStart, int recordTwoStop) {

		printDetail("SEQ:", recordOneReadString, recordTwoReadString);
		printDetail("CIG:", recordOneCigarString, recordTwoCigarString);
		printDetail(" MD:", (String) recordOneMismatchDetails, recordTwoMismatchDetails);
		printDetail("STT:", "" + recordOneStart, "" + recordTwoStart);
		printDetail("STP:", "" + recordOneStop, "" + recordTwoStop);
	}

	private static void printDetail(String description, String stringOne, String stringTwo) {
		if (!stringOne.equals(stringTwo)) {
			System.out.println("1_" + description + stringOne);
			System.out.println("2_" + description + stringTwo);
		}
	}

	private static Map<String, RecordPair> parseBamFile(File inputSamFile) {
		Map<String, RecordPair> recordPairsByName = new HashMap<>();

		try (SamReader currentReader = SamReaderFactory.makeDefault().open(inputSamFile)) {
			Iterator<SAMRecord> iter = currentReader.iterator();
			while (iter.hasNext()) {
				SAMRecord record = iter.next();
				String recordName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(record.getReadName());
				RecordPair recordPair = recordPairsByName.get(recordName);
				if (recordPair == null) {
					recordPair = new RecordPair();
					recordPairsByName.put(recordName, recordPair);
				}
				if (record.getFirstOfPairFlag()) {
					recordPair.setRecordOne(record);
				} else {
					recordPair.setRecordTwo(record);
				}
			}
		} catch (IOException e) {
			throw new PicardException(e.getMessage(), e);
		}

		return recordPairsByName;
	}

	private static class RecordPair {
		private SAMRecord recordOne;
		private SAMRecord recordTwo;

		public RecordPair() {
			super();
		}

		public SAMRecord getRecordOne() {
			return recordOne;
		}

		public void setRecordOne(SAMRecord recordOne) {
			this.recordOne = recordOne;
		}

		public SAMRecord getRecordTwo() {
			return recordTwo;
		}

		public void setRecordTwo(SAMRecord recordTwo) {
			this.recordTwo = recordTwo;
		}
	}

	private static void validateCigarStringsAndReadLengths(File inputSamFile, Genome genome) throws FileNotFoundException {
		double matchScore = SimpleAlignmentScorer.DEFAULT_MATCH_SCORE;
		double mismatchPenalty = SimpleAlignmentScorer.DEFAULT_MISMATCH_PENALTY;
		double gapOpenPenalty = SimpleAlignmentScorer.DEFAULT_GAP_OPEN_PENALTY;
		double gapExtendPenalty = SimpleAlignmentScorer.DEFAULT_GAP_EXTEND_PENALTY;
		IAlignmentScorer alignmentScorer = new SimpleAlignmentScorer(matchScore, mismatchPenalty, gapExtendPenalty, gapOpenPenalty, false);

		StringBuilder readNames = new StringBuilder();

		int validCount = 0;
		int invalidCount = 0;
		try (SamReader currentReader = SamReaderFactory.makeDefault().open(inputSamFile)) {
			Iterator<SAMRecord> iter = currentReader.iterator();
			while (iter.hasNext()) {
				SAMRecord record = iter.next();
				boolean isValid = checkSAMRecordCigarStringAndLength(record, true);
				if (isValid) {
					validCount++;
				} else {
					readNames.append("\"" + record.getReadName() + "\",");
					ISequence reference = genome.getSequence(record.getReferenceName(), record.getAlignmentStart(), record.getAlignmentEnd());
					ISequence query = new IupacNucleotideCodeSequence(record.getReadString());
					NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(reference, query, alignmentScorer);
					CigarString cigarStringFromAlignment = alignment.getCigarString();
					System.out.println(alignment.getAlignmentAsString());
					System.out.println("cigar string from alignment:" + cigarStringFromAlignment.getCigarString(true, false));
					System.out.println("cigar string from record   :" + record.getCigarString());

					invalidCount++;
				}
			}

		} catch (IOException e) {
			throw new PicardException(e.getMessage(), e);
		}
		// System.out.println(readNames.toString());
		System.out.println("Valid:" + validCount + " Invalid:" + invalidCount);
		System.out.println(ListUtil.toString(READ_NAMES, "\", \""));
	}

	private static List<String> READ_NAMES = new ArrayList<>();

	public static boolean checkSAMRecordCigarStringAndLength(SAMRecord record, boolean verbose) {
		boolean isValid = false;
		String summarizedCigarString = record.getCigarString();
		String expandedCigarString = CigarStringUtil.expandCigarString(summarizedCigarString).toUpperCase();
		if (expandedCigarString.startsWith("D")) {
			READ_NAMES.add(IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(record.getReadName()));
		}
		int deletions = StringUtil.countMatches(expandedCigarString, "D");
		int lengthOfSequence = record.getReadLength();
		int lengthFromCigarString = expandedCigarString.length() - deletions;

		isValid = lengthOfSequence == lengthFromCigarString;
		if (!isValid && verbose) {
			String readName = record.getReadName();
			System.err.println("Read Name [" + readName + "] has a read length of [" + lengthOfSequence + "] but its cigar string represents a length of [" + lengthFromCigarString
					+ "] with the following cigar string[" + summarizedCigarString + "] and read[" + record.getReadString() + "].");
		}
		return isValid;
	}

	private static void checkBamCigarString(File inputSamFile, Genome genome, boolean verbose) throws FileNotFoundException {
		TallyMap<String> failedProbeIds = new TallyMap<>();
		TallyMap<String> reasonsForFailure = new TallyMap<>();
		List<String> failedReadNames = new ArrayList<>();
		TallyMap<String> failureType = new TallyMap<>();

		int readCount = 0;
		int containingNCount = 0;

		try (SamReader currentReader = SamReaderFactory.makeDefault().open(inputSamFile)) {
			Iterator<SAMRecord> iter = currentReader.iterator();
			while (iter.hasNext()) {
				SAMRecord record = iter.next();
				boolean containsN = record.getReadString().contains("N");
				if (!containsN) {
					CheckResult checkResult = checkBamCigarStringAndMismatchDetails(genome, record, verbose);
					boolean cigarStringAndLengthIsOkay = checkSAMRecordCigarStringAndLength(record, verbose);

					if (checkResult.failed || !cigarStringAndLengthIsOkay) {
						String readName = record.getReadName();
						String probeId = SAMRecordUtil.getProbeId(record);
						if (verbose) {
							System.out.println("Reason for Failure:" + checkResult.reasonForFailure);
							System.out.println("Read Name:" + readName);
							if (record.getFirstOfPairFlag()) {
								System.out.println("Is read one.");
							} else {
								System.out.println("Is read two.");
							}
							if (record.getReadNegativeStrandFlag()) {
								System.out.println("Is Negative Strand.");
							} else {
								System.out.println("Is Forward Strand.");
							}
							System.out.println(probeId);
						}
						if (probeId != null) {
							failedProbeIds.add(probeId);
						}
						failedReadNames.add(readName);
						if (checkResult.failed) {
							reasonsForFailure.add(checkResult.reasonForFailure);
						} else {
							reasonsForFailure.add("Length check failed.");
						}
						String failureRecordType = "";
						if (record.getFirstOfPairFlag()) {
							failureRecordType += "1";
						} else {
							failureRecordType += "2";
						}

						if (record.getReadNegativeStrandFlag()) {
							failureRecordType += "-";
						} else {
							failureRecordType += "+";
						}

						if (probeId.endsWith("+")) {
							failureRecordType += "+";
						} else {
							failureRecordType += "-";
						}
						failureType.add(failureRecordType);
					}
				} else {
					containingNCount++;
				}
				readCount++;
			}
		} catch (IOException e) {
			throw new PicardException(e.getMessage(), e);
		}

		StringBuilder pids = new StringBuilder();
		pids.append("\"");
		for (String probeId : failedProbeIds.getTalliesAsMap().keySet()) {
			pids.append(probeId + "\",\"");
		}
		pids.append("\"");
		System.out.println(pids.toString());
		System.out.println("Failed_Reads:" + failedReadNames.size() + "  Reads_Containing_N:" + containingNCount + "  Total_Reads:" + readCount);
		System.out.println(failureType.getHistogramAsString());
	}

	private static class CheckResult {
		private final boolean failed;
		private String reasonForFailure;

		public CheckResult(boolean failed, String reasonForFailure) {
			super();
			this.failed = failed;
			this.reasonForFailure = reasonForFailure;
		}

	}

	public static CheckResult checkBamCigarStringAndMismatchDetails(Genome genome, SAMRecord record, boolean isVerbose) {
		StringBuilder logBuilder = new StringBuilder();
		boolean failed = false;
		String reasonForFailure = null;

		String referenceName = record.getReferenceName();
		long start = record.getAlignmentStart();
		long stop = record.getAlignmentEnd();
		if (stop > start) {

			try {
				ISequence referenceSequence = genome.getSequence(referenceName, start, stop);
				if (referenceSequence.toString().contains("N")) {
					failed = true;
					reasonForFailure = "sequence from reference contains one or more Ns.";
				} else {
					ISequence sequence = new IupacNucleotideCodeSequence(record.getReadString());

					String summarizedCigarString = record.getCigarString();
					String expandedCigarString = CigarStringUtil.expandCigarString(summarizedCigarString);

					String mismatchDetailsString = (String) record.getAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG);
					Command[] commands = null;
					if (mismatchDetailsString == null) {
						failed = true;
						reasonForFailure = "No mismatch details string.";
					} else {
						commands = parseMismatchDetails(mismatchDetailsString);
						if (isVerbose) {
							logBuilder.append(mismatchDetailsString + StringUtil.NEWLINE);
							logBuilder.append(ArraysUtil.toString(commands, "") + StringUtil.NEWLINE);
						}

						// TODO this probably should be removed so the check is accurate
						if ((StringUtil.countMatches(expandedCigarString, "I") >= 0) && (StringUtil.countMatches(expandedCigarString, "D") >= 0)) {

							StringBuilder newReference = new StringBuilder();
							StringBuilder referenceFromMd = new StringBuilder();
							StringBuilder newSequence = new StringBuilder();

							int referenceIndex = 0;
							int mdIndex = 0;
							int index = 0;

							StringBuilder md = new StringBuilder();
							StringBuilder cigar = new StringBuilder();

							sequenceLoop: for (int cigarIndex = 0; cigarIndex < expandedCigarString.length(); cigarIndex++) {
								int mdIndexAtStart = mdIndex;
								char cigarSymbol = expandedCigarString.charAt(cigarIndex);
								cigar.append(cigarSymbol);
								Command command = null;
								if (mdIndex < commands.length) {
									command = commands[mdIndex];
									md.append(command);
								}

								boolean isMatch = CigarStringUtil.isMatch(cigarSymbol);
								boolean isMismatch = false;
								if (isMatch) {
									if (command == null) {
										reasonForFailure = "Expected Match or MisMatch in the mismatch details (based on contents on the cigar string) string but a mismatch detail was not found.";
										failed = true;
										break sequenceLoop;
									} else {
										if (command.isMisMatch) {
											isMismatch = true;
										} else if (command.isMatch) {

										} else {
											reasonForFailure = "Expected Match or MisMatch in the mismatch details (based on contents on the cigar string) string but found " + command.getType() + ".";
											failed = true;
											break sequenceLoop;
										}
									}
								}
								boolean isDeletion = CigarStringUtil.isDeletionToReference(cigarSymbol);
								boolean isInsertion = CigarStringUtil.isInsertionToReference(cigarSymbol);

								boolean isClip = CigarStringUtil.isClip(cigarSymbol);
								if (isMatch) {
									newReference.append(referenceSequence.subSequence(referenceIndex, referenceIndex));
									newSequence.append(sequence.subSequence(index, index));
									if (isMismatch) {
										if (command.getCharacter() != null) {
											referenceFromMd.append(command.getCharacter());
										} else {
											reasonForFailure = "Expected a provided character for MisMatch but it was not provided in the mismatch details string.";
											failed = true;
											break sequenceLoop;
										}
									} else {
										referenceFromMd.append(sequence.subSequence(index, index));
									}
									referenceIndex++;
									index++;
									mdIndex++;
								} else if (isInsertion) {
									newReference.append("_");
									newSequence.append(sequence.subSequence(index, index));
									referenceFromMd.append("_");
									index++;
								} else if (isDeletion) {
									newSequence.append("_");
									newReference.append(referenceSequence.subSequence(referenceIndex, referenceIndex));
									if (command != null && command.getCharacter() != null) {
										referenceFromMd.append(command.getCharacter());
									} else {
										reasonForFailure = "Expected a sequence in the mismatch details to describe the deleted sequence.";
										failed = true;
										break sequenceLoop;
									}

									mdIndex++;
									referenceIndex++;
								} else if (isClip) {
									break sequenceLoop;
								} else {
									throw new AssertionError("Unrecognized type");
								}

							}

							if (isVerbose) {
								logBuilder.append("   summarized_md:" + mismatchDetailsString + StringUtil.NEWLINE);
								logBuilder.append("summarized_cigar:" + summarizedCigarString + StringUtil.NEWLINE);
								logBuilder.append("         mdindex:" + mdIndex + StringUtil.NEWLINE);
								logBuilder.append("             seq:" + newSequence.toString() + StringUtil.NEWLINE);
								logBuilder.append("           cigar:" + cigar + StringUtil.NEWLINE);
								logBuilder.append("              md:" + md.toString() + StringUtil.NEWLINE);
								logBuilder.append("          md_ref:" + referenceFromMd.toString() + StringUtil.NEWLINE);
								logBuilder.append("             ref:" + newReference.toString() + StringUtil.NEWLINE);

								logBuilder.append(StringUtil.NEWLINE);
							}

							if (!newReference.toString().equals(referenceFromMd.toString())) {
								reasonForFailure = "Failed because reference does not match the reference built from the mismatch details string.";
								failed = true;
							}
						}
					}
				}
			} catch (IllegalStateException e) {
				reasonForFailure = e.getMessage();
				failed = true;
			}
		}

		if (failed) {
			System.out.println(logBuilder.toString());
		}
		return new CheckResult(failed, reasonForFailure);
	}

	private static Command[] parseMismatchDetails(String mismatchDetails) {
		List<Command> commands = new ArrayList<>();
		Set<Character> nucleotides = new HashSet<>();
		nucleotides.add('A');
		nucleotides.add('C');
		nucleotides.add('G');
		nucleotides.add('T');

		boolean isInserting = false;
		boolean wasNumber = false;
		String currentNumber = "";
		for (int i = 0; i < mismatchDetails.length(); i++) {
			char character = mismatchDetails.charAt(i);
			Command commandToAdd = null;

			if (Character.isDigit(character)) {
				currentNumber += character;
				isInserting = false;
				wasNumber = true;
			} else if (character == '^') {
				isInserting = true;
				wasNumber = false;
			} else if (nucleotides.contains(Character.toUpperCase(character))) {
				wasNumber = false;
				if (isInserting) {
					commandToAdd = Command.insert(character);
				} else {
					commandToAdd = Command.mismatch(character);
				}
			}

			if (!wasNumber && currentNumber.length() > 0) {
				int lastNumber = Integer.parseInt(currentNumber);
				currentNumber = "";

				for (int j = 0; j < lastNumber; j++) {
					commands.add(Command.match());
				}
			}

			if (commandToAdd != null) {
				commands.add(commandToAdd);
			}
		}

		if (currentNumber.length() > 0) {
			int lastNumber = Integer.parseInt(currentNumber);
			for (int j = 0; j < lastNumber; j++) {
				commands.add(Command.match());
			}
		}
		return commands.toArray(new Command[0]);
	}

	private static class Command {
		private final boolean isInsertion;
		private final boolean isMatch;
		private final boolean isMisMatch;
		private final Character character;

		public static Command insert(Character character) {
			return new Command(true, false, false, character);
		}

		public String getType() {
			String type;
			if (isInsertion) {
				type = "insertion";
			} else if (isMisMatch) {
				type = "mismatch";
			} else if (isMatch) {
				type = "match";
			} else {
				throw new AssertionError();
			}
			return type;
		}

		public static Command match() {
			return new Command(false, true, false, null);
		}

		public static Command mismatch(Character character) {
			return new Command(false, false, true, character);
		}

		private Command(boolean isInsertion, boolean isMatch, boolean isMisMatch, Character character) {
			super();
			this.isInsertion = isInsertion;
			this.isMatch = isMatch;
			this.isMisMatch = isMisMatch;
			this.character = character;
		}

		public boolean isInsertion() {
			return isInsertion;
		}

		public boolean isMatch() {
			return isMatch;
		}

		public boolean isMisMatch() {
			return isMisMatch;
		}

		public Character getCharacter() {
			return character;
		}

		public String toString() {
			String value;
			if (isMatch) {
				value = "=";
			} else {
				value = "" + character;
				if (isMisMatch) {
					value = value.toLowerCase();
				} else if (isInsertion) {
					// insertion
					value = value.toUpperCase();
				} else {
					throw new AssertionError();
				}
			}
			return value;
		}

	}

}
