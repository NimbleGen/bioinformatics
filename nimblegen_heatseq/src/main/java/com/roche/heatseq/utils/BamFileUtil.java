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

import com.roche.heatseq.utils.SAMRecordUtil.AlternativeHit;
import com.roche.sequencing.bioinformatics.common.alignment.CigarString;
import com.roche.sequencing.bioinformatics.common.alignment.CigarStringUtil;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
import com.roche.sequencing.bioinformatics.common.alignment.SimpleAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.genome.Genome;
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

	public static void validateCigarStringsAndReadLengths(File inputSamFile, Genome genome) throws FileNotFoundException {
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

	public static void main(String[] args) throws FileNotFoundException {
		File inputSamFile = new File("D:\\kurts_space\\heatseq\\2017\\probe_overlap\\S1_fulldbPlus_2x150_sorted.bam");
		Map<String, SAMRecord> firstRecord = new HashMap<String, SAMRecord>();
		Map<String, SAMRecord> secondRecord = new HashMap<String, SAMRecord>();
		try (SamReader currentReader = SamReaderFactory.makeDefault().open(inputSamFile)) {
			Iterator<SAMRecord> iter = currentReader.iterator();
			while (iter.hasNext()) {
				SAMRecord record = iter.next();
				if (record.getFirstOfPairFlag()) {
					firstRecord.put(record.getReadName(), record);
				} else {
					secondRecord.put(record.getReadName(), record);
				}
			}
		} catch (IOException e) {
			throw new PicardException(e.getMessage(), e);
		}

		for (String readName : firstRecord.keySet()) {
			SAMRecord first = firstRecord.get(readName);
			SAMRecord second = secondRecord.get(readName);

			List<AlternativeHit> firstAltHits = SAMRecordUtil.getAlternativeHitsFromAttribute(first);
			List<AlternativeHit> secondAltHits = SAMRecordUtil.getAlternativeHitsFromAttribute(second);

			if ((firstAltHits == null || firstAltHits.size() == 0) && (secondAltHits != null && secondAltHits.size() > 0 && second.getMappingQuality() > 0)) {
				System.out.println(readName);
			}
		}
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

	public static class CheckResult {
		private final boolean failed;
		private String reasonForFailure;

		public CheckResult(boolean failed, String reasonForFailure) {
			super();
			this.failed = failed;
			this.reasonForFailure = reasonForFailure;
		}

		public String getReasonForFailure() {
			return reasonForFailure;
		}

		public void setReasonForFailure(String reasonForFailure) {
			this.reasonForFailure = reasonForFailure;
		}

		public boolean isFailed() {
			return failed;
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
					Integer editDistance = (Integer) record.getAttribute(SAMRecordUtil.EDIT_DISTANCE_ATTRIBUTE_TAG);

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

						StringBuilder newReference = new StringBuilder();
						StringBuilder referenceFromMd = new StringBuilder();
						StringBuilder newSequence = new StringBuilder();

						int referenceIndex = 0;
						int mdIndex = 0;
						int index = 0;

						StringBuilder md = new StringBuilder();
						StringBuilder cigar = new StringBuilder();

						int newEditDistance = 0;

						sequenceLoop: for (int cigarIndex = 0; cigarIndex < expandedCigarString.length(); cigarIndex++) {
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
										newEditDistance++;
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
								newEditDistance++;
								newReference.append("_");
								newSequence.append(sequence.subSequence(index, index));
								referenceFromMd.append("_");
								index++;
							} else if (isDeletion) {
								newSequence.append("_");
								newReference.append(referenceSequence.subSequence(referenceIndex, referenceIndex));
								if (command != null && command.getCharacter() != null) {
									referenceFromMd.append(command.getCharacter());
									newEditDistance++;
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

						if (editDistance != newEditDistance) {
							reasonForFailure = "Failed because calculated edit distance[" + newEditDistance + "] does not match the provided edit distance[" + editDistance + "].";
							failed = true;
						}

						if (!newReference.toString().equals(referenceFromMd.toString())) {
							reasonForFailure = "Failed because reference does not match the reference built from the mismatch details string.";
							failed = true;
						}
					}
					// }
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
