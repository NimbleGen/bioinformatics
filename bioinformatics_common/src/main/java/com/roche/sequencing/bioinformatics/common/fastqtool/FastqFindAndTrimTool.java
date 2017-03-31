package com.roche.sequencing.bioinformatics.common.fastqtool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;

import com.google.common.io.Files;
import com.roche.sequencing.bioinformatics.common.alignment.AlignmentPair;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
import com.roche.sequencing.bioinformatics.common.fastqtool.settings.FastqToolFindSettings;
import com.roche.sequencing.bioinformatics.common.fastqtool.settings.FastqToolOutputSettings;
import com.roche.sequencing.bioinformatics.common.fastqtool.settings.FastqToolSettings;
import com.roche.sequencing.bioinformatics.common.fastqtool.settings.FastqToolTrimSettings;
import com.roche.sequencing.bioinformatics.common.fastqtool.settings.FoundTextActionEnum;
import com.roche.sequencing.bioinformatics.common.fastqtool.settings.MultipleMatchesActionEnum;
import com.roche.sequencing.bioinformatics.common.fastqtool.settings.NoMatchesActionEnum;
import com.roche.sequencing.bioinformatics.common.mapping.SimpleMapper;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.DelimitedFileParserUtil;
import com.roche.sequencing.bioinformatics.common.utils.DelimitedLineParser;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.IlluminaFastQReadNameUtil;
import com.roche.sequencing.bioinformatics.common.utils.InputStreamFactory;
import com.roche.sequencing.bioinformatics.common.utils.LineParserException;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.fastq.FastqReader;
import com.roche.sequencing.bioinformatics.common.utils.fastq.FastqWriter;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ParsedProbeFile;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.Probe;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ProbeFileUtil;

import htsjdk.samtools.fastq.FastqRecord;

public class FastqFindAndTrimTool {

	private final static String PROBE_INFO_FILE_TEXT = ".txt";
	private final static String SETTINGS_FILE_TEXT = ".cfg";
	private final static String FASTQ_FILE_TEXT = ".fastq";
	private final static String SEQUENCE_FILE_TEXT = ".seq";

	private final static DecimalFormat DF = new DecimalFormat("###.##");

	private final static String OUTPUT_SEQUENCE_SEARCH_FIND_SUMMARY_FILE_NAME = "SEQUENCE_SEARCH_FIND_SUMMARY.TXT";
	private final static String OUTPUT_FASTQ_FIND_SUMMARY_FILE_NAME = "FASTQ_FIND_SUMMARY.TXT";
	private final static String OUTPUT_FIND_ALIGNMENT_FILE_NAME = "FIND_ALIGNMENT.TXT";
	private final static String OUTPUT_FIND_LOG_FILE_NAME = "FIND_LOG.TXT";

	private final static String NO_MATCH_FASTQ_FILE_PREFIX = "NO_MATCH_";
	private final static String MULTIPLE_MATCH_FASTQ_FILE_PREFIX = "MULTIPLE_MATCHES_";
	private final static String MATCH_FASTQ_FILE_PREFIX = "MATCH_";

	private final static String[] SEQUENCE_FILE_HEADER = new String[] { "ID", "SEQUENCE" };
	private final static String SEQUENCE_FILE_HEADER_SECONDARY_ID = "SECONDARY_ID";
	private final static String SEQUENCE_FILE_HEADER_ORIENTATIONS = "ORIENTATIONS";

	private final static int DEFAULT_COMPARISON_SEQUENCE_SIZE = 5;
	private final static int DEFAULT_REFERENCE_SPACING = 1;
	private final static int DEFAULT_QUERY_SPACING = 1;
	private final static int DEFAULT_MAX_REFERENCES_STORED_PER_SEQUENCE = 15000;

	private final static String DEFAULT_PRIMARY_ID_FOR_FASTQ_ENTRY_ANNOTATION_KEY = "probe_id";
	private final static String DEFAULT_SECONDARY_ID_FOR_FASTQ_ENTRY_ANNOTATION_KEY = "primer_type";
	private final static String DEFAULT_ORIENTATION_FOR_FASTQ_ENTRY_ANNOTATION_KEY = "orientation";

	private final static String DEFAULT_PRE_TEXT_FOR_FASTQ_ENTRY_ANNOTATION_KEY = "uid";

	private final static int PHRED_OFFSET = 33;

	public final static String ORIENTATION_DELIMITER = ",";

	private static class SequenceIdentifier {
		private final String primaryId;
		private final String secondaryId;
		private final OrientationEnum orientation;

		public SequenceIdentifier(String primaryId, String secondaryId, OrientationEnum orientation) {
			super();
			this.primaryId = primaryId;
			this.secondaryId = secondaryId;
			this.orientation = orientation;
		}

		public String getPrimaryId() {
			return primaryId;
		}

		public String getSecondaryId() {
			return secondaryId;
		}

		public OrientationEnum getOrientation() {
			return orientation;
		}

		@Override
		public String toString() {
			return primaryId + ":" + secondaryId + ":" + orientation.getAbbreviation();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((orientation == null) ? 0 : orientation.hashCode());
			result = prime * result + ((primaryId == null) ? 0 : primaryId.hashCode());
			result = prime * result + ((secondaryId == null) ? 0 : secondaryId.hashCode());
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
			SequenceIdentifier other = (SequenceIdentifier) obj;
			if (orientation != other.orientation)
				return false;
			if (primaryId == null) {
				if (other.primaryId != null)
					return false;
			} else if (!primaryId.equals(other.primaryId))
				return false;
			if (secondaryId == null) {
				if (other.secondaryId != null)
					return false;
			} else if (!secondaryId.equals(other.secondaryId))
				return false;
			return true;
		}

	}

	public static void runTool(String[] args) throws FileNotFoundException, IOException {
		File settingsFile = null;
		List<File> fastqFiles = new ArrayList<File>();
		List<File> sequenceFiles = new ArrayList<File>();
		List<File> probeInfoFiles = new ArrayList<File>();

		for (String arg : args) {
			if (arg.toLowerCase().contains(SETTINGS_FILE_TEXT)) {
				settingsFile = new File(arg);
			} else if (arg.toLowerCase().contains(FASTQ_FILE_TEXT)) {
				File fastqFile = new File(arg);
				fastqFiles.add(fastqFile);
			} else if (arg.toLowerCase().contains(SEQUENCE_FILE_TEXT)) {
				File sequenceFile = new File(arg);
				sequenceFiles.add(sequenceFile);
			} else if (arg.toLowerCase().contains(PROBE_INFO_FILE_TEXT)) {
				File probeInfoFile = new File(arg);
				probeInfoFiles.add(probeInfoFile);
			} else {
				throw new IllegalStateException("Unrecognized argument[" + arg + "].");
			}
		}

		if (fastqFiles.size() == 0) {
			throw new IllegalStateException("Unable to locate the Fastq File (file ending with .fastq extension) in the provided arguments[" + ArraysUtil.toString(args, ", ") + "].");
		}

		if (sequenceFiles.size() == 0 && probeInfoFiles.size() == 0) {
			throw new IllegalStateException(
					"Unable to locate an acceptable Sequences File (file ending with .seq extension and containing two tab delimited columns with the following header[ID	SEQUENCE]) or a Probe Info File in the provided arguments["
							+ ArraysUtil.toString(args, ", ") + "].");
		}

		if (settingsFile == null) {
			throw new IllegalStateException(
					"Unable to locate an acceptable settings file (file containing the text [settings.cfg] in the provided arguments[" + ArraysUtil.toString(args, ", ") + "].");
		}

		FastqToolSettings settings = FastqToolSettings.parseSettings(new InputStreamFactory(settingsFile));

		// extracting additional header names is necessary to pull out the optional SECONDARY_ID and ORIENTATIONS columns
		boolean extractAdditionalHeaderNames = true;

		FastqToolFindSettings findSettings = settings.getFindSettings();

		Map<SequenceIdentifier, ISequence> sequencesToFindById = new LinkedHashMap<>();

		for (File sequenceFile : sequenceFiles) {
			SequenceFileLineParser lineParser = new SequenceFileLineParser(sequenceFile.getAbsolutePath(), findSettings);
			DelimitedFileParserUtil.parseFile(new InputStreamFactory(sequenceFile), SEQUENCE_FILE_HEADER, lineParser, StringUtil.TAB, extractAdditionalHeaderNames, null);
			sequencesToFindById.putAll(lineParser.getSequencesById());
		}

		for (File probeInfoFile : probeInfoFiles) {
			ParsedProbeFile parsedProbeInfoFile = ProbeFileUtil.parseProbeInfoFile(probeInfoFile);
			for (Probe probe : parsedProbeInfoFile) {
				for (String secondaryId : new String[] { "extension", "ligation" }) {
					ISequence sequence;
					if (secondaryId.equals("extension")) {
						sequence = probe.getExtensionPrimerSequence();
					} else if (secondaryId.equals("ligation")) {
						sequence = probe.getLigationPrimerSequence();
					} else {
						throw new AssertionError();
					}
					for (OrientationEnum orientation : OrientationEnum.values()) {
						if (findSettings.shouldIncludeOrientation(orientation)) {
							sequencesToFindById.put(new SequenceIdentifier(probe.getProbeId(), secondaryId, orientation), orientation.orientSequence(sequence));
						}
					}
				}
			}
		}

		SimpleMapper<SequenceIdentifier> sequenceMapper = new SimpleMapper<>(DEFAULT_COMPARISON_SEQUENCE_SIZE, DEFAULT_REFERENCE_SPACING, DEFAULT_QUERY_SPACING,
				DEFAULT_MAX_REFERENCES_STORED_PER_SEQUENCE);
		for (Entry<SequenceIdentifier, ISequence> entry : sequencesToFindById.entrySet()) {
			SequenceIdentifier id = entry.getKey();
			ISequence sequence = entry.getValue();
			sequenceMapper.addReferenceSequence(sequence, id);
		}

		FastqToolOutputSettings outputSettings = settings.getOutputSettings();
		File outputDirectory = null;

		Map<String, List<File>> filesByReadNamesMd5Sum = new HashMap<>();
		// need a read name signature to determine if the file can be paired and ultimately deduped
		for (File fastqFile : fastqFiles) {
			MessageDigest md5SumForReadNames = null;
			try {
				md5SumForReadNames = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				throw new AssertionError();
			}

			try (FastqReader reader = new FastqReader(fastqFile)) {
				while (reader.hasNext()) {
					FastqRecord record = reader.next();

					String readName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(record.getReadHeader());
					md5SumForReadNames.update(readName.getBytes());
				}
			}

			String readNamesMd5Sum = Hex.encodeHexString(md5SumForReadNames.digest());
			List<File> files = filesByReadNamesMd5Sum.get(readNamesMd5Sum);
			if (files == null) {
				files = new ArrayList<>();
				filesByReadNamesMd5Sum.put(readNamesMd5Sum, files);
			}
			files.add(fastqFile);
		}

		Set<File> fastqFilesThatCanBePaired = new HashSet<>();
		// indentify if any files can be paired together and deduped based on an md5 sum of each fastq files read names
		for (Entry<String, List<File>> entry : filesByReadNamesMd5Sum.entrySet()) {
			List<File> inputFilesWithSameReadNames = entry.getValue();
			if (inputFilesWithSameReadNames.size() == 2) {
				fastqFilesThatCanBePaired.add(inputFilesWithSameReadNames.get(0));
				fastqFilesThatCanBePaired.add(inputFilesWithSameReadNames.get(1));
			}
		}

		Map<File, OutputFiles> intputFileToOutputFilesMap = new HashMap<>();
		for (File fastqFile : fastqFiles) {
			switch (outputSettings.getOutputFileAction()) {
			case OUTPUT_FILES_IN_APPLICATION_DIRECTORY:
				outputDirectory = new File("");
				break;
			case OUTPUT_FILES_IN_DESIGNATED_DIRECTORY:
				outputDirectory = new File(outputSettings.getOutputFileActionValue());

				break;
			case OUTPUT_FILES_IN_SAME_DIRECTORY_AS_INPUT_FASTQ:
				outputDirectory = fastqFile.getParentFile();
				break;
			case OUTPUT_FILES_IN_SUBDIRECTORY_OF_APPLICATION_DIRECTORY:
				outputDirectory = new File(outputSettings.getOutputFileActionValue());
				break;
			case OUTPUT_FILES_IN_SUBDIRECTORY_OF_INPUT_FASTQ:
				outputDirectory = new File(fastqFile.getParentFile(), outputSettings.getOutputFileActionValue());
				break;
			default:
				throw new IllegalStateException("An output directory designation was not provided in the 'OUTPUT' section of the settings file[" + settingsFile.getAbsolutePath() + "].");
			}

			String directoryName = FileUtil.getFileNameWithoutExtension(fastqFile.getName());
			File fastqSpecificOutputDirectory = new File(outputDirectory, directoryName);

			FileUtil.createDirectory(fastqSpecificOutputDirectory);

			// move the settings file here for a record of how the data was generated
			Files.copy(settingsFile, new File(fastqSpecificOutputDirectory, settingsFile.getName()));

			for (File sequenceFile : sequenceFiles) {
				Files.copy(sequenceFile, new File(fastqSpecificOutputDirectory, sequenceFile.getName()));
			}

			FastqToolTrimSettings trimSettings = settings.getTrimSettings();
			int leadingQualityThreshold = trimSettings.getLeadingQualityTrimThreshold();
			int trailingQualityThreshold = trimSettings.getTrailingQualityTrimThreshold();

			boolean canFileBePaired = fastqFilesThatCanBePaired.contains(fastqFile);
			// TODO pull this from dedup settings
			boolean shouldDedup = true;

			findAndTrim(settings, fastqFile, sequenceMapper, sequencesToFindById, fastqSpecificOutputDirectory, leadingQualityThreshold, trailingQualityThreshold, intputFileToOutputFilesMap,
					canFileBePaired, shouldDedup);
		}

		FastqToolTrimSettings trimSettings = settings.getTrimSettings();
		String foundSequencePrimaryIdAnnotationKey = trimSettings.getAddFoundSequencePrimaryIdToFastQAnnotationWithFollowingKey();
		if (foundSequencePrimaryIdAnnotationKey == null) {
			foundSequencePrimaryIdAnnotationKey = DEFAULT_PRIMARY_ID_FOR_FASTQ_ENTRY_ANNOTATION_KEY;
		}
		String foundSequenceSecondaryIdAnnotationKey = trimSettings.getAddFoundSequenceSecondaryIdToFastQAnnotationWithFollowingKey();
		if (foundSequenceSecondaryIdAnnotationKey == null) {
			foundSequenceSecondaryIdAnnotationKey = DEFAULT_SECONDARY_ID_FOR_FASTQ_ENTRY_ANNOTATION_KEY;
		}
		String foundSequenceOrientationAnnotationKey = trimSettings.getAddFoundSequenceOrientationToFastQAnnotationWithFollowingKey();
		if (foundSequenceOrientationAnnotationKey == null) {
			foundSequenceOrientationAnnotationKey = DEFAULT_ORIENTATION_FOR_FASTQ_ENTRY_ANNOTATION_KEY;
		}

		// identify if any files can be paired together and deduped based on an md5 sum of each fastq files read names
		for (Entry<String, List<File>> entry : filesByReadNamesMd5Sum.entrySet()) {
			List<File> inputFilesWithSameReadNames = entry.getValue();
			if (inputFilesWithSameReadNames.size() == 2) {
				// This should have first be R1 and second be R2 just to possibly avoid any confusion downstream in reporting
				Collections.sort(inputFilesWithSameReadNames);

				File firstOriginalFastqFile = inputFilesWithSameReadNames.get(0);
				OutputFiles firstOutputFiles = intputFileToOutputFilesMap.get(firstOriginalFastqFile);
				File secondOriginalFastqFile = inputFilesWithSameReadNames.get(1);
				OutputFiles secondOutputFiles = intputFileToOutputFilesMap.get(secondOriginalFastqFile);

				String firstName = FileUtil.getFileNameWithoutExtension(firstOriginalFastqFile.getName());
				String secondName = FileUtil.getFileNameWithoutExtension(secondOriginalFastqFile.getName());

				String pairIdentifier = firstName;

				if (firstName.length() == secondName.length()) {
					StringBuilder pairIdentifierBuilder = new StringBuilder();
					int numberOfMatches = 0;
					for (int i = 0; i < firstName.length(); i++) {
						if (firstName.charAt(i) == secondName.charAt(i)) {
							pairIdentifierBuilder.append(firstName.charAt(i));
							numberOfMatches++;
						}
					}

					// only one mismatch in entire name (should be R1 vs R2)
					if (numberOfMatches == firstName.length() - 1) {
						pairIdentifier = pairIdentifierBuilder.toString();
					}
				}

				File pairAssignedOutputDirectory = new File(outputDirectory, "PAIR_ASSIGNED_" + pairIdentifier);
				FileUtil.createDirectory(pairAssignedOutputDirectory);

				pairAssignmentsForIlluminaPairedFastqFiles(firstOriginalFastqFile, firstOutputFiles.getOutputSingleMatchFastqFile(), firstOutputFiles.getOutputMultipleMatchesFastQFile(),
						secondOriginalFastqFile, secondOutputFiles.getOutputSingleMatchFastqFile(), secondOutputFiles.getOutputMultipleMatchesFastQFile(), foundSequencePrimaryIdAnnotationKey,
						foundSequenceSecondaryIdAnnotationKey, foundSequenceOrientationAnnotationKey, pairAssignedOutputDirectory);

				dedupPairedFastqFiles();
			}
		}
		if (outputDirectory != null) {
			System.out.println("Results have been generated at [" + outputDirectory.getAbsolutePath() + "].");
		}

	}

	private static void dedupPairedFastqFiles() {
		// TODO
	}

	private final static String SEQUENCE_SEARCH_PAIR_SUMMARY_FILE = "SEQUENCE_SEARCH_PAIR_SUMMARY.TXT";
	private final static String FASTQ_PAIR_SUMMARY_FILE = "FASTQ_PAIR_SUMMARY.TXT";

	private static void pairAssignmentsForIlluminaPairedFastqFiles(File firstOriginalFastqFile, File firstMatchFastqFile, File firstMultipleMatchFastqFile, File secondOriginalFastqFile,
			File secondMatchFastqFile, File secondMultipleMatchFastqFile, String foundSequencePrimaryIdAnnotationKey, String foundSequenceSecondaryIdAnnotationKey,
			String foundSequenceOrientationAnnotationKey, File pairAssignedOutputDirectory) {

		File fastqPairSummaryFile = new File(pairAssignedOutputDirectory, FASTQ_PAIR_SUMMARY_FILE);
		File sequencePairSummaryFile = new File(pairAssignedOutputDirectory, SEQUENCE_SEARCH_PAIR_SUMMARY_FILE);

		try (Writer fastqPairSummary = new BufferedWriter(new FileWriter(fastqPairSummaryFile))) {
			try (Writer sequencePairSummary = new BufferedWriter(new FileWriter(sequencePairSummaryFile))) {

				int numberOfPairedReadsThatDidNotPair = 0;
				int numberOfPairedReadsThatPairedToOnlyOneSequence = 0;
				int numberOfPairedReadsThatPairedToMultipleSequences = 0;

				TallyMap<PairedSequenceIdentifiers> pairedSequenceIdentifiersTally = new TallyMap<>();
				try (FastqReader readerForNames = new FastqReader(firstOriginalFastqFile)) {
					try (FastqReader firstReader = new FastqReader(firstMatchFastqFile)) {
						try (FastqReader secondReader = new FastqReader(secondMatchFastqFile)) {
							File outputFastqOneFile = new File(pairAssignedOutputDirectory, firstOriginalFastqFile.getName());
							FileUtil.createNewFile(outputFastqOneFile);
							try (FastqWriter firstWriter = new FastqWriter(outputFastqOneFile)) {
								File outputFastqTwoFile = new File(pairAssignedOutputDirectory, secondOriginalFastqFile.getName());
								FileUtil.createNewFile(outputFastqTwoFile);
								try (FastqWriter secondWriter = new FastqWriter(outputFastqTwoFile)) {

									FastqRecord firstRead = null;
									FastqRecord secondRead = null;

									if (firstReader.hasNext()) {
										firstRead = firstReader.next();
									}

									if (secondReader.hasNext()) {
										secondRead = secondReader.next();
									}

									while (readerForNames.hasNext() && firstReader.hasNext() && secondReader.hasNext()) {

										FastqRecord record = readerForNames.next();
										String readName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(record.getReadHeader());

										String firstReadName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(firstRead.getReadHeader());
										String secondReadName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(secondRead.getReadHeader());

										List<FastqRecord> readOneRecords = new ArrayList<>();
										// handle repeat entries
										while (firstReadName.equals(readName)) {
											readOneRecords.add(firstRead);
											firstRead = firstReader.next();
											firstReadName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(firstRead.getReadHeader());
										}

										List<FastqRecord> readTwoRecords = new ArrayList<>();
										while (secondReadName.equals(readName)) {
											readTwoRecords.add(secondRead);
											secondRead = secondReader.next();
											secondReadName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(secondRead.getReadHeader());
										}

										int readPairsAdded = 0;
										if (readOneRecords.size() > 0 && readTwoRecords.size() > 0) {
											for (int i = 0; i < readOneRecords.size(); i++) {
												FastqRecord readOneRecord = readOneRecords.get(i);
												String readOneAnnotation = readOneRecord.getBaseQualityHeader();
												Map<String, String> readOneNameValuePairs = splitFastqAnnotation(readOneAnnotation);

												String readOnePrimaryId = readOneNameValuePairs.get(foundSequencePrimaryIdAnnotationKey);
												String readOneSecondaryId = readOneNameValuePairs.get(foundSequenceSecondaryIdAnnotationKey);
												String readOneOrientationAbbreviation = readOneNameValuePairs.get(foundSequenceOrientationAnnotationKey);
												OrientationEnum readOneOrientation = OrientationEnum.getOrientationByAbbreviation(readOneOrientationAbbreviation);
												SequenceIdentifier readOneSequenceIdentifier = new SequenceIdentifier(readOnePrimaryId, readOneSecondaryId, readOneOrientation);

												if (readOnePrimaryId == null) {
													throw new AssertionError("Unable to find the read one primary id in the quality header of the fastq file[" + firstMatchFastqFile.getAbsolutePath()
															+ "] using the key[" + foundSequencePrimaryIdAnnotationKey + "].");
												}

												if (readOneSecondaryId == null) {
													throw new AssertionError("Unable to find the read one secondary id in the quality header of the fastq file[" + firstMatchFastqFile.getAbsolutePath()
															+ "] using the key[" + foundSequenceSecondaryIdAnnotationKey + "].");
												}

												for (int j = 0; j < readTwoRecords.size(); j++) {
													FastqRecord readTwoRecord = readTwoRecords.get(j);
													String readTwoAnnotation = readTwoRecord.getBaseQualityHeader();
													Map<String, String> readTwoNameValuePairs = splitFastqAnnotation(readTwoAnnotation);

													String readTwoPrimaryId = readTwoNameValuePairs.get(foundSequencePrimaryIdAnnotationKey);
													String readTwoSecondaryId = readTwoNameValuePairs.get(foundSequenceSecondaryIdAnnotationKey);
													String readTwoOrientationAbbreviation = readOneNameValuePairs.get(foundSequenceOrientationAnnotationKey);
													OrientationEnum readTwoOrientation = OrientationEnum.getOrientationByAbbreviation(readTwoOrientationAbbreviation);
													SequenceIdentifier readTwoSequenceIdentifier = new SequenceIdentifier(readTwoPrimaryId, readTwoSecondaryId, readTwoOrientation);
													if (readTwoPrimaryId == null) {
														throw new AssertionError("Unable to find the read two primary id in the quality header of the fastq file["
																+ secondMatchFastqFile.getAbsolutePath() + "] using the key[" + foundSequencePrimaryIdAnnotationKey + "].");
													}

													if (readTwoSecondaryId == null) {
														throw new AssertionError("Unable to find the read two secondary id in the quality header of the fastq file["
																+ secondMatchFastqFile.getAbsolutePath() + "] using the key[" + foundSequenceSecondaryIdAnnotationKey + "].");
													}

													if (readOnePrimaryId.equals(readTwoPrimaryId) && !readOneSecondaryId.equals(readTwoSecondaryId)) {
														firstWriter.write(readOneRecord);
														secondWriter.write(readTwoRecord);
														pairedSequenceIdentifiersTally.add(new PairedSequenceIdentifiers(readOneSequenceIdentifier, readTwoSequenceIdentifier));
														readPairsAdded++;
													} else {

													}
												}
											}
										}

										if (readPairsAdded == 0) {
											numberOfPairedReadsThatDidNotPair++;
										} else if (readPairsAdded == 1) {
											numberOfPairedReadsThatPairedToOnlyOneSequence++;
										} else if (readPairsAdded > 1) {
											numberOfPairedReadsThatPairedToMultipleSequences++;
										}
									}

								}

							}
						}
					}

					fastqPairSummary.write("NUMBER_OF_PAIRED_FASTQ_READS_THAT_DID_NOT_PAIR" + StringUtil.TAB + "NUMBER_OF_PAIRED_FASTQ_READS_THAT_PAIRED_TO_ONLY_ONE_SEQUENCE" + StringUtil.TAB
							+ "NUMBER_OF_PAIRED_FASTQ_READS_THAT_PAIRED_TO_MULTIPLE_SEQUENCE" + StringUtil.NEWLINE);
					fastqPairSummary.write("" + numberOfPairedReadsThatDidNotPair + StringUtil.TAB + numberOfPairedReadsThatPairedToOnlyOneSequence + StringUtil.TAB
							+ numberOfPairedReadsThatPairedToMultipleSequences + StringUtil.NEWLINE);

					sequencePairSummary.write("PRIMARY_ID" + StringUtil.TAB + "FIRST_SECONDARY_ID" + StringUtil.TAB + "FIRST_ORIENTATION" + StringUtil.TAB + "SECOND_SECONDARY_ID" + StringUtil.TAB
							+ "SECOND_ORIENTATION" + StringUtil.TAB + "READ_PAIR_COUNT" + StringUtil.NEWLINE);
					for (Entry<PairedSequenceIdentifiers, Integer> entry : pairedSequenceIdentifiersTally.getObjectsSortedFromMostTalliesToLeast()) {
						SequenceIdentifier firstSequenceIdentifier = entry.getKey().getFirstSequenceIdentifier();
						SequenceIdentifier secondSequenceIdentifier = entry.getKey().getSecondSequenceIdentifier();
						int count = entry.getValue();
						sequencePairSummary.write(firstSequenceIdentifier.getPrimaryId() + StringUtil.TAB + firstSequenceIdentifier.getSecondaryId() + StringUtil.TAB
								+ firstSequenceIdentifier.getOrientation().getAbbreviation() + StringUtil.TAB + secondSequenceIdentifier.getSecondaryId() + StringUtil.TAB
								+ secondSequenceIdentifier.getOrientation().getAbbreviation() + StringUtil.TAB + count + StringUtil.NEWLINE);
					}
				} catch (IOException e1) {
					throw new IllegalStateException(e1.getMessage(), e1);
				}
			} catch (IOException e1) {
				throw new IllegalStateException(e1.getMessage(), e1);
			}

		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private static class PairedSequenceIdentifiers {
		private final SequenceIdentifier firstSequenceIdentifier;
		private final SequenceIdentifier secondSequenceIdentifier;

		public PairedSequenceIdentifiers(SequenceIdentifier firstSequenceIdentifier, SequenceIdentifier secondSequenceIdentifier) {
			super();
			this.firstSequenceIdentifier = firstSequenceIdentifier;
			this.secondSequenceIdentifier = secondSequenceIdentifier;
		}

		public SequenceIdentifier getFirstSequenceIdentifier() {
			return firstSequenceIdentifier;
		}

		public SequenceIdentifier getSecondSequenceIdentifier() {
			return secondSequenceIdentifier;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((firstSequenceIdentifier == null) ? 0 : firstSequenceIdentifier.hashCode());
			result = prime * result + ((secondSequenceIdentifier == null) ? 0 : secondSequenceIdentifier.hashCode());
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
			PairedSequenceIdentifiers other = (PairedSequenceIdentifiers) obj;
			if (firstSequenceIdentifier == null) {
				if (other.firstSequenceIdentifier != null)
					return false;
			} else if (!firstSequenceIdentifier.equals(other.firstSequenceIdentifier))
				return false;
			if (secondSequenceIdentifier == null) {
				if (other.secondSequenceIdentifier != null)
					return false;
			} else if (!secondSequenceIdentifier.equals(other.secondSequenceIdentifier))
				return false;
			return true;
		}

	}

	private static Map<String, String> splitFastqAnnotation(String annotation) {
		Map<String, String> valuesByNameMap = new HashMap<>();
		String[] splitAnnotation = annotation.split(" ");
		for (String nameValuePairs : splitAnnotation) {
			String[] splitNameValuePairs = nameValuePairs.split("=");
			if (splitNameValuePairs.length == 2) {
				String name = splitNameValuePairs[0];
				String value = splitNameValuePairs[1];
				valuesByNameMap.put(name, value);
			}
		}
		return valuesByNameMap;
	}

	private static class OutputFiles {
		private final File outputSequenceSearchFindSummaryFile;
		private final File outputFastQFindSummaryFile;
		private final File outputFindAlignmentFile;
		private final File outputFindLogFile;

		private final File outputNoMatchFastqFile;
		private final File outputMultipleMatchesFastQFile;
		private final File outputSingleMatchFastqFile;

		public OutputFiles(File outputSequenceSearchFindSummaryFile, File outputFastQFindSummaryFile, File outputFindAlignmentFile, File outputFindLogFile, File outputNoMatchFastqFile,
				File outputMultipleMatchesFastQFile, File outputSingleMatchFastqFile) {
			super();
			this.outputSequenceSearchFindSummaryFile = outputSequenceSearchFindSummaryFile;
			this.outputFastQFindSummaryFile = outputFastQFindSummaryFile;
			this.outputFindAlignmentFile = outputFindAlignmentFile;
			this.outputFindLogFile = outputFindLogFile;
			this.outputNoMatchFastqFile = outputNoMatchFastqFile;
			this.outputMultipleMatchesFastQFile = outputMultipleMatchesFastQFile;
			this.outputSingleMatchFastqFile = outputSingleMatchFastqFile;
		}

		public File getOutputSequenceSearchFindSummaryFile() {
			return outputSequenceSearchFindSummaryFile;
		}

		public File getOutputFastQFindSummaryFile() {
			return outputFastQFindSummaryFile;
		}

		public File getOutputFindAlignmentFile() {
			return outputFindAlignmentFile;
		}

		public File getOutputFindLogFile() {
			return outputFindLogFile;
		}

		public File getOutputNoMatchFastqFile() {
			return outputNoMatchFastqFile;
		}

		public File getOutputMultipleMatchesFastQFile() {
			return outputMultipleMatchesFastQFile;
		}

		public File getOutputSingleMatchFastqFile() {
			return outputSingleMatchFastqFile;
		}

		public List<File> getAllFiles() {
			List<File> allFiles = new ArrayList<>();
			if (outputSequenceSearchFindSummaryFile != null) {
				allFiles.add(outputSequenceSearchFindSummaryFile);
			}
			if (outputFindAlignmentFile != null) {
				allFiles.add(outputFindAlignmentFile);
			}
			if (outputFindLogFile != null) {
				allFiles.add(outputFindLogFile);
			}
			if (outputNoMatchFastqFile != null) {
				allFiles.add(outputNoMatchFastqFile);
			}
			if (outputMultipleMatchesFastQFile != null) {
				allFiles.add(outputMultipleMatchesFastQFile);
			}
			if (outputSingleMatchFastqFile != null) {
				allFiles.add(outputSingleMatchFastqFile);
			}

			return allFiles;
		}

	}

	private static class SequenceFileLineParser extends DelimitedLineParser {

		private final Map<SequenceIdentifier, ISequence> sequencesById;
		private final String fileName;
		private final FastqToolFindSettings findSettings;

		public SequenceFileLineParser(String fileName, FastqToolFindSettings findSettings) {
			super();
			this.sequencesById = new LinkedHashMap<>();
			this.fileName = fileName;
			this.findSettings = findSettings;
		}

		@Override
		public void parseDelimitedLine(Map<String, String> headerNameToValue) throws LineParserException {
			String id = headerNameToValue.get(SEQUENCE_FILE_HEADER[0]);
			ISequence sequence = new IupacNucleotideCodeSequence(headerNameToValue.get(SEQUENCE_FILE_HEADER[1]));
			String secondaryId = headerNameToValue.get(SEQUENCE_FILE_HEADER_SECONDARY_ID);
			String orientationAbbreviations = headerNameToValue.get(SEQUENCE_FILE_HEADER_ORIENTATIONS);
			if (orientationAbbreviations == null) {
				orientationAbbreviations = findSettings.getDefaultOrientationAbbreviations();
			}

			String[] splitOrientations = orientationAbbreviations.split(ORIENTATION_DELIMITER);
			List<OrientationEnum> orientations = new ArrayList<>();
			for (String orientationAbbreviation : splitOrientations) {
				OrientationEnum orientation = OrientationEnum.getOrientationByAbbreviation(orientationAbbreviation);
				if (orientation != null) {
					orientations.add(orientation);
				} else {
					throw new IllegalStateException(
							"Unable to identify the provided orientation abbreviation [" + orientationAbbreviation + "] provided in the 'ORIENTATIONS' column of the provided file[" + fileName + "].");
				}
			}

			for (OrientationEnum orientation : orientations) {
				sequencesById.put(new SequenceIdentifier(id, secondaryId, orientation), orientation.orientSequence(sequence));

			}

		}

		public Map<SequenceIdentifier, ISequence> getSequencesById() {
			return sequencesById;
		}

	}

	private static void findAndTrim(FastqToolSettings settings, File fastqFile, SimpleMapper<SequenceIdentifier> sequenceMapper, Map<SequenceIdentifier, ISequence> sequencesById, File outputDirectory,
			int leadingQualityThreshold, int trailingQualityThreshold, Map<File, OutputFiles> inputFileToOutputFilesMap, boolean canFileBePaired, boolean shouldDedup) throws IOException {

		FastqToolOutputSettings outputSettings = settings.getOutputSettings();

		File outputSequenceSearchFindSummaryFile = null;
		File outputFastQFindSummaryFile = null;
		File outputFindAlignmentFile = null;
		File outputFindLogFile = null;

		File outputNoMatchFastqFile = null;
		File outputMultipleMatchesFastQFile = null;
		File outputSingleMatchFastqFile = null;

		Writer outputSequenceSearchFindSummary = null;
		Writer outputFastQFindSummary = null;
		Writer outputFindAlignment = null;
		Writer outputFindLog = null;

		FastqWriter outputNoMatchFastq = null;
		FastqWriter outputMultipleMatchesFastQ = null;
		FastqWriter outputSingleMatchFastq = null;

		try {
			if (outputSettings.isOutputSequenceSearchFindSummary()) {
				outputSequenceSearchFindSummaryFile = new File(outputDirectory, OUTPUT_SEQUENCE_SEARCH_FIND_SUMMARY_FILE_NAME);
				FileUtil.createNewFile(outputSequenceSearchFindSummaryFile);
				outputSequenceSearchFindSummary = new BufferedWriter(new FileWriter(outputSequenceSearchFindSummaryFile));
			}

			if (outputSettings.isOutputFastQFindSummary()) {
				outputFastQFindSummaryFile = new File(outputDirectory, OUTPUT_FASTQ_FIND_SUMMARY_FILE_NAME);
				FileUtil.createNewFile(outputFastQFindSummaryFile);
				outputFastQFindSummary = new BufferedWriter(new FileWriter(outputFastQFindSummaryFile));
				outputFastQFindSummary.write("#DATE:" + DateUtil.getCurrentDateINYYYYMMDDHHMMSSwithColons() + "  SEARCHED_FASTQ_FILE:" + fastqFile.getAbsolutePath() + StringUtil.NEWLINE);
			}

			if (outputSettings.isOutputFindAlignment()) {
				outputFindAlignmentFile = new File(outputDirectory, OUTPUT_FIND_ALIGNMENT_FILE_NAME);
				FileUtil.createNewFile(outputFindAlignmentFile);
				outputFindAlignment = new BufferedWriter(new FileWriter(outputFindAlignmentFile));
			}

			if (outputSettings.isOutputFindLog()) {
				outputFindLogFile = new File(outputDirectory, OUTPUT_FIND_LOG_FILE_NAME);
				FileUtil.createNewFile(outputFindLogFile);
				outputFindLog = new BufferedWriter(new FileWriter(outputFindLogFile));
			}

			FastqToolTrimSettings trimSettings = settings.getTrimSettings();

			String matchFastqFileName = MATCH_FASTQ_FILE_PREFIX + fastqFile.getName();
			outputSingleMatchFastqFile = new File(outputDirectory, matchFastqFileName);
			FileUtil.createNewFile(outputSingleMatchFastqFile);
			outputSingleMatchFastq = new FastqWriter(outputSingleMatchFastqFile);

			if (trimSettings.getNoMatchesAction() == NoMatchesActionEnum.EXCLUDE_ENTRIES_WITH_NO_MATCHES) {
				// keep it null
				outputNoMatchFastq = null;
			} else if (trimSettings.getNoMatchesAction() == NoMatchesActionEnum.PUT_ENTRIES_WITH_NO_MATCHES_IN_OWN_FILE) {
				String fileName = NO_MATCH_FASTQ_FILE_PREFIX + fastqFile.getName();
				outputNoMatchFastqFile = new File(outputDirectory, fileName);
				FileUtil.createNewFile(outputNoMatchFastqFile);
				outputNoMatchFastq = new FastqWriter(outputNoMatchFastqFile);
			} else if (trimSettings.getNoMatchesAction() == NoMatchesActionEnum.PUT_ENTRIES_WITH_NO_MATCHES_IN_SINGLE_MATCH_FILE) {
				outputNoMatchFastqFile = outputSingleMatchFastqFile;
				outputNoMatchFastq = outputSingleMatchFastq;
			}

			if (trimSettings.getMultipleMatchesAction() == MultipleMatchesActionEnum.EXCLUDE_ENTRIES_WITH_MULTIPLE_MATCHES) {
				// keep it null
				outputMultipleMatchesFastQ = null;
			} else if (trimSettings.getMultipleMatchesAction() == MultipleMatchesActionEnum.PUT_ENTRIES_WITH_MULTIPLE_MATCHES_IN_OWN_FILE) {
				String fileName = MULTIPLE_MATCH_FASTQ_FILE_PREFIX + fastqFile.getName();
				outputMultipleMatchesFastQFile = new File(outputDirectory, fileName);
				FileUtil.createNewFile(outputMultipleMatchesFastQFile);
				outputMultipleMatchesFastQ = new FastqWriter(outputMultipleMatchesFastQFile);
			} else if (trimSettings.getMultipleMatchesAction() == MultipleMatchesActionEnum.PUT_ENTRIES_WITH_MULTIPLE_MATCHES_IN_NOT_FOUND_FILE) {
				outputMultipleMatchesFastQFile = outputNoMatchFastqFile;
				outputMultipleMatchesFastQ = outputNoMatchFastq;
			} else if (trimSettings.getMultipleMatchesAction() == MultipleMatchesActionEnum.PUT_ENTIES_WITH_MULTIPLE_MATCHES_IN_SINGLE_MATCHES_FILE) {
				outputMultipleMatchesFastQFile = outputSingleMatchFastqFile;
				outputMultipleMatchesFastQ = outputSingleMatchFastq;
			}

			boolean keepPreText = !trimSettings.isRemovePreTextInOutput();
			boolean keepFoundText = trimSettings.getFoundTextAction() == FoundTextActionEnum.KEEP_FOUND_TEXT_IN_OUTPUT;
			boolean replaceFoundTextWithSearchSequence = trimSettings.getFoundTextAction() == FoundTextActionEnum.REPLACE_FOUND_TEXT_IN_OUTPUT;
			boolean keepPostText = !trimSettings.isRemovePostTextInOutput();

			String preTextAnnotationKey = trimSettings.getAddPreTextToFastQAnnotationWithFollowingKey();
			if (preTextAnnotationKey == null && shouldDedup) {
				// since this file can be paired it must contain a primary id which is used for deduplication
				preTextAnnotationKey = DEFAULT_PRE_TEXT_FOR_FASTQ_ENTRY_ANNOTATION_KEY;
				if (outputFindLog != null) {
					outputFindLog.write("Fastq File[" + fastqFile.getAbsolutePath()
							+ "] needs to be deduped in downstream analysis so the pre text needs to be present in the  annotation output; the pre text will be outputted with the default pre text key["
							+ DEFAULT_PRE_TEXT_FOR_FASTQ_ENTRY_ANNOTATION_KEY + "]." + StringUtil.NEWLINE);
				}
			}
			String foundTextAnnotationKey = trimSettings.getAddFoundTextToFastQAnnotationWithFollowingKey();
			String foundSequenceAnnotationKey = trimSettings.getAddFoundSequenceToFastQAnnotationWithFollowingKey();
			String foundSequencePrimaryIdAnnotationKey = trimSettings.getAddFoundSequencePrimaryIdToFastQAnnotationWithFollowingKey();
			if (foundSequencePrimaryIdAnnotationKey == null && canFileBePaired) {
				// since this file can be paired it must contain a primary id which is used for deduplication
				foundSequencePrimaryIdAnnotationKey = DEFAULT_PRIMARY_ID_FOR_FASTQ_ENTRY_ANNOTATION_KEY;
				if (outputFindLog != null) {
					outputFindLog.write("Fastq File[" + fastqFile.getAbsolutePath()
							+ "] is paired with another file and does not have the primary id defined as an annotation output (which is needed for deduplication) so the primary id will be outputted with the default primary key["
							+ DEFAULT_PRIMARY_ID_FOR_FASTQ_ENTRY_ANNOTATION_KEY + "]." + StringUtil.NEWLINE);
				}
			} else {
				if (outputFindLog != null) {
					outputFindLog.write(
							"Fastq File[" + fastqFile.getAbsolutePath() + "] is using the primary id key [" + foundSequencePrimaryIdAnnotationKey + "] for annotation output." + StringUtil.NEWLINE);
				}
			}
			String foundSequenceSecondaryIdAnnotationKey = trimSettings.getAddFoundSequenceSecondaryIdToFastQAnnotationWithFollowingKey();
			if (foundSequenceSecondaryIdAnnotationKey == null && canFileBePaired) {
				// since this file can be paired it must contain a secondary id which is used for deduplication
				foundSequenceSecondaryIdAnnotationKey = DEFAULT_SECONDARY_ID_FOR_FASTQ_ENTRY_ANNOTATION_KEY;
				if (outputFindLog != null) {
					outputFindLog.write("Fastq File[" + fastqFile.getAbsolutePath()
							+ "] is paired with another file and does not have the secondary id defined as an annotation output (which is needed for deduplication) so the secondary id will be outputted with the default secondary key["
							+ DEFAULT_SECONDARY_ID_FOR_FASTQ_ENTRY_ANNOTATION_KEY + "]." + StringUtil.NEWLINE);
				}
			} else {
				if (outputFindLog != null) {
					outputFindLog.write("Fastq File[" + fastqFile.getAbsolutePath() + "] is using the secondary id key [" + foundSequenceSecondaryIdAnnotationKey + "] for annotation output."
							+ StringUtil.NEWLINE);
				}
			}
			String foundSequenceOrientationAnnotationKey = trimSettings.getAddFoundSequenceOrientationToFastQAnnotationWithFollowingKey();
			if (foundSequenceOrientationAnnotationKey == null && canFileBePaired) {
				// since this file can be paired it must contain an orientation which is used for pair assignment reports
				foundSequenceOrientationAnnotationKey = DEFAULT_ORIENTATION_FOR_FASTQ_ENTRY_ANNOTATION_KEY;
				if (outputFindLog != null) {
					outputFindLog.write("Fastq File[" + fastqFile.getAbsolutePath()
							+ "] is paired with another file and does not have the orientation defined as an annotation output so the orientation will be outputted with the default orientation key["
							+ DEFAULT_ORIENTATION_FOR_FASTQ_ENTRY_ANNOTATION_KEY + "]." + StringUtil.NEWLINE);
				}
			} else {
				if (outputFindLog != null) {
					outputFindLog.write(
							"Fastq File[" + fastqFile.getAbsolutePath() + "] is using the orientation key [" + foundSequenceSecondaryIdAnnotationKey + "] for annotation output." + StringUtil.NEWLINE);
				}
			}

			String postTextAnnotationKey = trimSettings.getAddPostTextToFastQAnnotationWithFollowingKey();

			FastqToolFindSettings findSettings = settings.getFindSettings();

			TallyMap<SequenceIdentifier> sharedMatchesBySequenceToFindId = new TallyMap<>();
			TallyMap<SequenceIdentifier> nonSharedMatchesBySequenceToFindId = new TallyMap<>();
			TallyMap<SequenceIdentifier> matchesBySequenceToFindId = new TallyMap<>();

			int fastqEntryNumber = 0;
			int assignedToSingleSequenceCount = 0;
			int assignedToMultipleSequenceCount = 0;
			int entriesSkippedBecauseContainedN = 0;

			try (FastqReader reader = new FastqReader(fastqFile)) {
				while (reader.hasNext()) {
					FastqRecord record = reader.next();

					ISequence recordSequence = new IupacNucleotideCodeSequence(record.getReadString());
					String recordQuality = record.getBaseQualityString();

					QualityTrimmedRead qualityTrimmedRead = trimReadBasedOnQuality(recordSequence, recordQuality, leadingQualityThreshold, trailingQualityThreshold);
					recordSequence = qualityTrimmedRead.getTrimmedSequence();
					recordQuality = qualityTrimmedRead.getTrimmedQuality();
					boolean trimOccurred = qualityTrimmedRead.trimOccurred();
					if (trimOccurred && outputFindLog != null) {
						outputFindLog.write(record.getReadHeader() + StringUtil.NEWLINE);
						outputFindLog.write("Leading_Bases_Trimmed:" + qualityTrimmedRead.getNumberOfLeadingBasesTrimmed() + " Trailing_Bases_Trimmed:"
								+ qualityTrimmedRead.getNumberOfTrailingBasesTrimmed() + StringUtil.NEWLINE);
					}

					if (recordSequence.toString().toLowerCase().contains("n")) {
						entriesSkippedBecauseContainedN++;
					} else {

						List<MatchDetails> matches = new ArrayList<>();

						List<SequenceIdentifier> candidateSequenceIds = sequenceMapper.getBestCandidateReferences(recordSequence);
						for (SequenceIdentifier candidateSequenceId : candidateSequenceIds) {
							ISequence sequenceToFind = sequencesById.get(candidateSequenceId);

							NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(recordSequence, sequenceToFind);

							AlignmentPair trimmedAlignment = alignment.getAlignmentPair().getAlignmentWithoutEndingAndBeginningQueryInserts();

							int numberOfMismatches = trimmedAlignment.getNumberOfMismatches();
							int numberOfInsertionGaps = trimmedAlignment.getNumberOfInsertionGapsRelativeToReference();
							int numberOfInsertionBases = trimmedAlignment.getNumberOfInsertionsRelativeToReference();
							int numberOfDeletionGaps = trimmedAlignment.getNumberOfDeletionGapsRelativeToReference();
							int numberOfDeletionBases = trimmedAlignment.getNumberOfDeletionsRelativeToReference();

							boolean hasAcceptableMismatchCount = numberOfMismatches <= findSettings.getAllowedMismatchBases();
							boolean hasAcceptableInsertionGapsCount = numberOfInsertionGaps <= findSettings.getAllowedInsertionGaps();
							boolean hasAcceptableInsertionBasesCount = numberOfInsertionBases <= findSettings.getAllowedInsertionBases();
							boolean hasAcceptableDeletionGapsCount = numberOfDeletionGaps <= findSettings.getAllowedDeletionGaps();
							boolean hasAcceptableDeletionBasesCount = numberOfDeletionBases <= findSettings.getAllowedDeletionBases();

							boolean isAcceptable = hasAcceptableMismatchCount && hasAcceptableInsertionGapsCount && hasAcceptableInsertionBasesCount && hasAcceptableDeletionGapsCount
									&& hasAcceptableDeletionBasesCount;
							if (isAcceptable) {
								matches.add(new MatchDetails(candidateSequenceId, sequenceToFind, alignment.getAlignmentPair()));
							}
						}

						if (matches.size() > 0) {
							if (matches.size() == 1) {
								assignedToSingleSequenceCount++;
								if (outputSingleMatchFastq != null) {
									MatchDetails matchDetails = matches.get(0);
									FastqRecord newRecord = getNewRecord(record, recordQuality, matchDetails, keepPreText, keepFoundText, replaceFoundTextWithSearchSequence, keepPostText,
											preTextAnnotationKey, foundTextAnnotationKey, foundSequenceAnnotationKey, foundSequencePrimaryIdAnnotationKey, foundSequenceSecondaryIdAnnotationKey,
											foundSequenceOrientationAnnotationKey, postTextAnnotationKey, sharedMatchesBySequenceToFindId, nonSharedMatchesBySequenceToFindId,
											matchesBySequenceToFindId);
									outputSingleMatchFastq.write(newRecord);
								}
							} else {
								for (MatchDetails matchDetails : matches) {
									if (outputMultipleMatchesFastQ != null) {
										FastqRecord newRecord = getNewRecord(record, recordQuality, matchDetails, keepPreText, keepFoundText, replaceFoundTextWithSearchSequence, keepPostText,
												preTextAnnotationKey, foundTextAnnotationKey, foundSequenceAnnotationKey, foundSequencePrimaryIdAnnotationKey, foundSequenceSecondaryIdAnnotationKey,
												foundSequenceOrientationAnnotationKey, postTextAnnotationKey, sharedMatchesBySequenceToFindId, nonSharedMatchesBySequenceToFindId,
												matchesBySequenceToFindId);
										outputSingleMatchFastq.write(newRecord);
										outputMultipleMatchesFastQ.write(newRecord);
									}
								}
								assignedToMultipleSequenceCount++;
							}
						} else {
							outputNoMatchFastq.write(record);
						}

						if (outputFindAlignment != null) {
							if (matches.size() > 0) {
								outputFindAlignment
										.write(">Entry:" + fastqEntryNumber + "(line#" + (fastqEntryNumber * 4) + ") " + record.getReadHeader() + " Matches:" + matches.size() + StringUtil.NEWLINE);
								for (MatchDetails matchDetails : matches) {
									AlignmentPair alignment = matchDetails.getAlignment();
									AlignmentPair trimmedAlignment = alignment.getAlignmentWithoutEndingAndBeginningQueryInserts();

									int numberOfMismatches = trimmedAlignment.getNumberOfMismatches();
									int numberOfInsertionGaps = trimmedAlignment.getNumberOfInsertionGapsRelativeToReference();
									int numberOfInsertionBases = trimmedAlignment.getNumberOfInsertionsRelativeToReference();
									int numberOfDeletionGaps = trimmedAlignment.getNumberOfDeletionGapsRelativeToReference();
									int numberOfDeletionBases = trimmedAlignment.getNumberOfDeletionsRelativeToReference();

									outputFindAlignment.write("**SEQ_TO_FIND_ID:" + matchDetails.getSequenceToFindId() + " mismatches:" + numberOfMismatches + " insertionGaps:" + numberOfInsertionGaps
											+ " insertionBases:" + numberOfInsertionBases + " deletionGaps:" + numberOfDeletionGaps + " deletionBases:" + numberOfDeletionBases + StringUtil.NEWLINE);
									outputFindAlignment.write(alignment.getAlignmentAsString());
								}
								outputFindAlignment.write(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + StringUtil.NEWLINE);
							}
						}

						if (trimOccurred && outputFindLog != null) {
							outputFindLog.write(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + StringUtil.NEWLINE);
						}
					}
					fastqEntryNumber++;

				}
			}

			if (outputSequenceSearchFindSummary != null) {
				outputSequenceSearchFindSummary.write("SEQUENCE_TO_FIND_PRIMARY_ID" + StringUtil.TAB + "SEQUENCE_TO_FIND_SECONDARY_ID" + StringUtil.TAB + "SEQUENCE_TO_FIND_ORIENTATION"
						+ StringUtil.TAB + "MATCHES" + StringUtil.TAB + "NON_SHARED_MATCHES" + StringUtil.TAB + "SHARED_MATCHES" + StringUtil.NEWLINE);
				for (Entry<SequenceIdentifier, Integer> entry : matchesBySequenceToFindId.getObjectsSortedFromMostTalliesToLeast()) {
					SequenceIdentifier sequenceToFindId = entry.getKey();
					int sharedMatches = sharedMatchesBySequenceToFindId.getCount(sequenceToFindId);
					int nonSharedMatches = nonSharedMatchesBySequenceToFindId.getCount(sequenceToFindId);
					outputSequenceSearchFindSummary
							.write(sequenceToFindId.getPrimaryId() + StringUtil.TAB + sequenceToFindId.getSecondaryId() + StringUtil.TAB + sequenceToFindId.getOrientation().getAbbreviation()
									+ StringUtil.TAB + entry.getValue() + StringUtil.TAB + nonSharedMatches + StringUtil.TAB + sharedMatches + StringUtil.NEWLINE);
				}
			}

			if (outputFastQFindSummary != null) {
				int notAssignedCount = fastqEntryNumber - assignedToSingleSequenceCount - assignedToMultipleSequenceCount - entriesSkippedBecauseContainedN;
				outputFastQFindSummary.write("TOTAL_READS:" + StringUtil.TAB + fastqEntryNumber + StringUtil.NEWLINE);
				outputFastQFindSummary.write("READS_ASSIGNED_TO_SINGLE_SEQUENCE_TO_FIND:" + StringUtil.TAB + assignedToSingleSequenceCount + " ("
						+ DF.format(100 * (double) assignedToSingleSequenceCount / (double) fastqEntryNumber) + "%)" + StringUtil.NEWLINE);
				outputFastQFindSummary.write("READS_ASSIGNED_TO_MULTIPLE_SEQUENCE_TO_FIND:" + StringUtil.TAB + assignedToMultipleSequenceCount + " ("
						+ DF.format(100 * (double) assignedToMultipleSequenceCount / (double) fastqEntryNumber) + "%)" + StringUtil.NEWLINE);
				outputFastQFindSummary.write("READS_SKIPPED_BECAUSE_THEY_CONTAIN_N:" + StringUtil.TAB + entriesSkippedBecauseContainedN + " ("
						+ DF.format(100 * (double) entriesSkippedBecauseContainedN / (double) fastqEntryNumber) + "%)" + StringUtil.NEWLINE);
				outputFastQFindSummary
						.write("READS_NOT_ASSIGNED:" + StringUtil.TAB + notAssignedCount + " (" + DF.format(100 * (double) notAssignedCount / (double) fastqEntryNumber) + "%)" + StringUtil.NEWLINE);
			}

			inputFileToOutputFilesMap.put(fastqFile, new OutputFiles(outputSequenceSearchFindSummaryFile, outputFastQFindSummaryFile, outputFindAlignmentFile, outputFindLogFile,
					outputNoMatchFastqFile, outputMultipleMatchesFastQFile, outputSingleMatchFastqFile));

		} finally {
			if (outputSequenceSearchFindSummary != null) {
				outputSequenceSearchFindSummary.close();
			}
			if (outputFastQFindSummary != null) {
				outputFastQFindSummary.close();
			}
			if (outputFindAlignment != null) {
				outputFindAlignment.close();
			}
			if (outputFindLog != null) {
				outputFindLog.close();
			}
			if (outputNoMatchFastq != null) {
				outputNoMatchFastq.close();
			}
			if (outputMultipleMatchesFastQ != null) {
				outputMultipleMatchesFastQ.close();
			}
			if (outputSingleMatchFastq != null) {
				outputSingleMatchFastq.close();
			}
		}
	}

	private static FastqRecord getNewRecord(FastqRecord record, String quality, MatchDetails matchDetails, boolean keepPreText, boolean keepFoundText, boolean replaceFoundTextWithSearchSequence,
			boolean keepPostText, String preTextAnnotationKey, String foundTextAnnotationKey, String foundSequenceAnnotationKey, String foundSequencePrimaryIdAnnotationKey,
			String foundSequenceSecondaryIdAnnotationKey, String foundSequenceOrientationAnnotationKey, String postTextAnnotationKey, TallyMap<SequenceIdentifier> sharedMatchesBySequenceToFindId,
			TallyMap<SequenceIdentifier> nonSharedMatchesBySequenceToFindId, TallyMap<SequenceIdentifier> matchesBySequenceToFindId) {
		String sequenceToFind = matchDetails.getSequenceToFind().toString();
		SequenceIdentifier sequenceToFindId = matchDetails.getSequenceToFindId();
		nonSharedMatchesBySequenceToFindId.add(sequenceToFindId);
		matchesBySequenceToFindId.add(sequenceToFindId);

		AlignmentPair alignment = matchDetails.getAlignment();

		int preTextIndex = alignment.getFirstNonInsertQueryMatchInReference();
		ISequence preText = alignment.getReferenceAlignment().subSequence(0, preTextIndex);
		String preTextAsString = preText.toString().replace("_", "");
		String preQuality = quality.substring(0, preTextAsString.length());

		if (preTextAsString.length() != preQuality.length()) {
			System.out.println(quality);
			System.out.println(alignment.getAlignmentAsString());
			System.out.println(preTextAsString.length() + " " + preQuality.length());
			throw new AssertionError();
		}
		int postTextIndex = alignment.getLastNonInsertQueryMatchInReference();
		ISequence referenceAlignment = alignment.getReferenceAlignment();
		ISequence postText = referenceAlignment.subSequence(postTextIndex, referenceAlignment.size());
		String postTextAsString = postText.toString().replace("_", "");

		String postQuality = quality.substring(quality.length() - postTextAsString.length(), quality.length());
		if (postTextAsString.length() != postQuality.length()) {
			System.out.println(quality);
			System.out.println(alignment.getAlignmentAsString());
			System.out.println(postTextAsString.length() + " " + postQuality.length());
			throw new AssertionError();
		}

		ISequence foundText = referenceAlignment.subSequence(preTextAsString.length(), postTextIndex - 1);
		String foundTextAsString = foundText.toString().toUpperCase().replace("_", "");
		String foundTextQuality = quality.substring(preTextAsString.length(), quality.length() - postQuality.length());

		if (foundTextAsString.length() != foundTextQuality.length()) {
			System.out.println(quality);
			System.out.println(alignment.getAlignmentAsString());
			System.out.println(foundTextAsString.length() + " " + foundTextQuality.length());
			throw new AssertionError();
		}

		StringBuilder trimmedSequenceBuilder = new StringBuilder();
		StringBuilder trimmedQualityBuilder = new StringBuilder();

		if (keepPreText) {
			trimmedSequenceBuilder.append(preTextAsString.toString());
			trimmedQualityBuilder.append(preQuality);
		}
		if (keepFoundText) {
			trimmedSequenceBuilder.append(foundTextAsString);
			trimmedQualityBuilder.append(foundTextQuality);
		} else if (replaceFoundTextWithSearchSequence) {
			trimmedSequenceBuilder.append(sequenceToFind);
			// ~ is the highest quality
			trimmedQualityBuilder.append(StringUtil.repeatString("~", sequenceToFind.length()));
		}

		if (keepPostText) {
			trimmedQualityBuilder.append(postTextAsString);
			trimmedSequenceBuilder.append(postTextAsString);
		}

		String additionalAnnotation = "";

		if (preTextAnnotationKey != null) {
			additionalAnnotation += preTextAnnotationKey + "=" + preText + " ";
		}

		if (postTextAnnotationKey != null) {
			additionalAnnotation += postTextAnnotationKey + "=" + postText + " ";
		}

		if (foundTextAnnotationKey != null) {
			additionalAnnotation += foundTextAnnotationKey + "=" + foundTextAsString + " ";
		}

		if (foundSequenceAnnotationKey != null) {
			additionalAnnotation += foundSequenceAnnotationKey + "=" + sequenceToFind + " ";
		}

		if (foundSequencePrimaryIdAnnotationKey != null) {
			additionalAnnotation += foundSequencePrimaryIdAnnotationKey + "=" + sequenceToFindId.getPrimaryId() + " ";
		}

		if (foundSequenceSecondaryIdAnnotationKey != null) {
			additionalAnnotation += foundSequenceSecondaryIdAnnotationKey + "=" + sequenceToFindId.getSecondaryId() + " ";
		}

		if (foundSequenceOrientationAnnotationKey != null) {
			additionalAnnotation += foundSequenceOrientationAnnotationKey + "=" + sequenceToFindId.getOrientation().getAbbreviation() + " ";
		}

		FastqRecord newRecord = createNewFastqRecordWithAdditionalAnnotation(record, additionalAnnotation, trimmedSequenceBuilder.toString(), trimmedQualityBuilder.toString());

		return newRecord;
	}

	private static FastqRecord createNewFastqRecordWithAdditionalAnnotation(FastqRecord record, String additionalAnnotation, String sequence, String quality) {
		String seqHeaderPrefix = record.getReadHeader();
		String seqLine = record.getReadString();
		if (sequence != null) {
			seqLine = sequence;
		}
		String qualHeaderPrefix = record.getBaseQualityHeader();
		if (qualHeaderPrefix != null) {
			qualHeaderPrefix += additionalAnnotation;
		} else {
			qualHeaderPrefix = additionalAnnotation;
		}
		String qualLine = record.getBaseQualityString();
		if (quality != null) {
			qualLine = quality;
		}
		FastqRecord newRecord = new FastqRecord(seqHeaderPrefix, seqLine, qualHeaderPrefix, qualLine);
		return newRecord;
	}

	private static QualityTrimmedRead trimReadBasedOnQuality(ISequence readSequence, String readQuality, int leadingQualityThreshold, int trailingQualityThreshold) {
		if (readSequence.size() != readQuality.length()) {
			throw new IllegalStateException("The length[" + readSequence.size() + "] for the provided readSequence[" + readSequence + "] does not match the length[" + readQuality.length()
					+ "] of the provided readQuality[" + readQuality + "].");
		}

		int leadingBasesToTrim = 0;
		boolean continueLeadTrimming = true;
		while (leadingBasesToTrim < readQuality.length() && continueLeadTrimming) {
			// lower numbers are worse quality
			int quality = (int) readQuality.charAt(leadingBasesToTrim) - PHRED_OFFSET;
			continueLeadTrimming = quality < leadingQualityThreshold;
			if (continueLeadTrimming) {
				leadingBasesToTrim++;
			}
		}

		int trailingBasesToTrim = 0;
		boolean continueTailTrimming = true;
		while (trailingBasesToTrim < (readQuality.length() - leadingBasesToTrim) && continueTailTrimming) {
			// lower numbers are worse quality
			int quality = (int) readQuality.charAt(readQuality.length() - trailingBasesToTrim - 1) - PHRED_OFFSET;
			continueTailTrimming = quality < trailingQualityThreshold;
			if (continueTailTrimming) {
				trailingBasesToTrim++;
			}
		}

		ISequence trimmedSequence = readSequence.subSequence(leadingBasesToTrim, readSequence.size() - trailingBasesToTrim - 1);
		String trimmedQuality = readQuality.substring(leadingBasesToTrim, readSequence.size() - trailingBasesToTrim);
		return new QualityTrimmedRead(trimmedSequence, trimmedQuality, leadingBasesToTrim, trailingBasesToTrim);
	}

	private static class QualityTrimmedRead {
		private final ISequence trimmedSequence;
		private final String trimmedQuality;
		private final int numberOfLeadingBasesTrimmed;
		private final int numberOfTrailingBasesTrimmed;

		public QualityTrimmedRead(ISequence trimmedSequence, String trimmedQuality, int numberOfLeadingBasesTrimmed, int numberOfTrailingBasesTrimmed) {
			super();
			this.trimmedSequence = trimmedSequence;
			this.trimmedQuality = trimmedQuality;
			this.numberOfLeadingBasesTrimmed = numberOfLeadingBasesTrimmed;
			this.numberOfTrailingBasesTrimmed = numberOfTrailingBasesTrimmed;
		}

		public boolean trimOccurred() {
			boolean trimOccurred = numberOfLeadingBasesTrimmed > 0 || numberOfTrailingBasesTrimmed > 0;
			return trimOccurred;
		}

		public ISequence getTrimmedSequence() {
			return trimmedSequence;
		}

		public String getTrimmedQuality() {
			return trimmedQuality;
		}

		public int getNumberOfLeadingBasesTrimmed() {
			return numberOfLeadingBasesTrimmed;
		}

		public int getNumberOfTrailingBasesTrimmed() {
			return numberOfTrailingBasesTrimmed;
		}

		@Override
		public String toString() {
			return "TrimmedRead [trimmedSequence=" + trimmedSequence + ", trimmedQuality=" + trimmedQuality + ", numberOfLeadingBasesTrimmed=" + numberOfLeadingBasesTrimmed
					+ ", numberOfTrailingBasesTrimmed=" + numberOfTrailingBasesTrimmed + "]";
		}

	}

	private static class MatchDetails {
		private final SequenceIdentifier sequenceToFindId;
		private final ISequence sequenceToFind;
		private final AlignmentPair alignment;

		public MatchDetails(SequenceIdentifier sequenceToFindId, ISequence sequenceToFind, AlignmentPair alignment) {
			super();
			this.sequenceToFindId = sequenceToFindId;
			this.sequenceToFind = sequenceToFind;
			this.alignment = alignment;
		}

		public SequenceIdentifier getSequenceToFindId() {
			return sequenceToFindId;
		}

		public ISequence getSequenceToFind() {
			return sequenceToFind;
		}

		public AlignmentPair getAlignment() {
			return alignment;
		}
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		runTool(args);
	}
}
