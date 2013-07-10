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

package com.roche.mapping.datasimulator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.IlluminaFastQHeader;
import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.process.BamFileUtil;
import com.roche.heatseq.process.ProbeFileUtil;
import com.roche.heatseq.process.ProbeFileUtil.FileOsFlavor;
import com.roche.sequencing.bioinformatics.common.alignment.CigarStringUtil;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class FastQDataSimulator {

	private final Logger logger = LoggerFactory.getLogger(FastQDataSimulator.class);

	private final static Random randomNumberGenerator = new Random(System.currentTimeMillis());

	private final static FileOsFlavor FILE_OS_FLAVOR = FileOsFlavor.LINUX;
	private final static String DEFAULT_OUTPUT_FASTQ1_NAME = "one.fastq";
	private final static String DEFAULT_OUTPUT_FASTQ2_NAME = "two.fastq";
	private final static String DEFAULTOUTPUT_PROBE_INFO = "probes.txt";
	private final static int UID_LENGTH = 14;
	private final static int EXTENSION_PRIMER_LENGTH = 22;
	private final static int LIGATION_PRIMER_LENGTH = 18;

	private final static int PROBE_LOCATION_GAP = 1000;
	private final static String CONTAINER_NAME = "chr1";

	private final static int INDIVIDUAL_READ_LENGTH = 120;

	private final static int MIN_CAPTURE_TARGET_READ_LENGTH = 20;

	private final int numberOfProbes;

	private final File outputDirectory;

	private final int readsPerUidProbePair;
	private final int uidsPerProbe;

	private final String mismatchDetailsString;
	private final int captureTargetAndPrimersLength;

	private final boolean alternateProbeStrands;

	private final String outputFastqOneFileName;
	private final String outputFastqTwoFileName;
	private final String outputProbesFileName;

	private FastQDataSimulator(File outputDirectory, String outputFastqOneFileName, String outputFastqTwoFileName, String outputProbesFileName, int numberOfProbes, int readsPerUidProbePair,
			int uidsPerProbe, int captureTargetAndPrimersLength, String mismatchDetailsString, boolean alternateProbeStrands) {

		this.outputDirectory = outputDirectory;
		this.numberOfProbes = numberOfProbes;
		this.readsPerUidProbePair = readsPerUidProbePair;
		this.uidsPerProbe = uidsPerProbe;
		this.mismatchDetailsString = mismatchDetailsString;
		this.alternateProbeStrands = alternateProbeStrands;
		this.captureTargetAndPrimersLength = captureTargetAndPrimersLength;

		this.outputFastqOneFileName = outputFastqOneFileName;
		this.outputFastqTwoFileName = outputFastqTwoFileName;
		this.outputProbesFileName = outputProbesFileName;
	}

	private void write(int currentProbeLocation, int usedRecordIndex, String readHeader, String readString, String baseQualityHeader, BufferedWriter fastQOneWriter, BufferedWriter fastQTwoWriter,
			List<Probe> probes) {
		ISequence readSequence = new IupacNucleotideCodeSequence(readString);

		if (readSequence.size() > (EXTENSION_PRIMER_LENGTH + LIGATION_PRIMER_LENGTH + MIN_CAPTURE_TARGET_READ_LENGTH)) {
			String containerName = CONTAINER_NAME;

			int extensionPrimerStart = 0;
			int extensionPrimerStop = 0;
			ISequence extensionPrimerSequence = null;
			int ligationPrimerStop = 0;
			int ligationPrimerStart = 0;
			ISequence ligationPrimerSequence = null;
			int captureTargetStart = 0;
			int captureTargetStop = 0;
			ISequence captureTargetSequence = null;
			int featureStart = 0;
			int featureStop = 0;
			Strand probeStrand = null;

			ISequence readStringOne = null;

			ISequence readStringTwo = null;

			boolean probeIsReversed = alternateProbeStrands && usedRecordIndex % 2 == 0;
			if (probeIsReversed) {
				extensionPrimerStop = currentProbeLocation + readSequence.size();
				extensionPrimerStart = extensionPrimerStop - EXTENSION_PRIMER_LENGTH + 1;
				extensionPrimerSequence = readSequence.getCompliment().subSequence(readSequence.size() - EXTENSION_PRIMER_LENGTH, readSequence.size() - 1);
				ligationPrimerStart = currentProbeLocation;
				ligationPrimerStop = currentProbeLocation + LIGATION_PRIMER_LENGTH - 1;
				ligationPrimerSequence = readSequence.getCompliment().subSequence(0, LIGATION_PRIMER_LENGTH - 1);
				captureTargetStart = ligationPrimerStop + 1;
				captureTargetStop = extensionPrimerStop - 1;
				captureTargetSequence = readSequence.getCompliment().subSequence(LIGATION_PRIMER_LENGTH, readSequence.size() - EXTENSION_PRIMER_LENGTH - 1);
				featureStart = captureTargetStart;
				featureStop = captureTargetStop;
				probeStrand = Strand.REVERSE;

				readStringOne = readSequence.getReverseCompliment().subSequence(0, INDIVIDUAL_READ_LENGTH - 1);

				readStringTwo = readSequence.subSequence(readSequence.size() - INDIVIDUAL_READ_LENGTH, readSequence.size() - 1);

			} else {
				extensionPrimerStart = currentProbeLocation;
				extensionPrimerStop = extensionPrimerStart + EXTENSION_PRIMER_LENGTH - 1;
				extensionPrimerSequence = readSequence.subSequence(0, EXTENSION_PRIMER_LENGTH - 1);
				ligationPrimerStop = currentProbeLocation + readSequence.size();
				ligationPrimerStart = ligationPrimerStop - LIGATION_PRIMER_LENGTH + 1;
				ligationPrimerSequence = readSequence.subSequence(readSequence.size() - LIGATION_PRIMER_LENGTH, readSequence.size() - 1);
				captureTargetStart = extensionPrimerStop + 1;
				captureTargetStop = ligationPrimerStart - 1;
				captureTargetSequence = readSequence.subSequence(EXTENSION_PRIMER_LENGTH, readSequence.size() - LIGATION_PRIMER_LENGTH - 1);
				featureStart = captureTargetStart;
				featureStop = captureTargetStop;
				probeStrand = Strand.FORWARD;
			}

			Probe probe = new Probe(1, containerName, extensionPrimerStart, extensionPrimerStop, extensionPrimerSequence, ligationPrimerStart, ligationPrimerStop, ligationPrimerSequence,
					captureTargetStart, captureTargetStop, captureTargetSequence, featureStart, featureStop, probeStrand);
			probes.add(probe);

			for (int j = 0; j < uidsPerProbe; j++) {
				ISequence currentReadSequence = readSequence;
				if (mismatchDetailsString != null && !mismatchDetailsString.isEmpty()) {
					currentReadSequence = mutate(readSequence, mismatchDetailsString);
				}
				readStringOne = currentReadSequence.subSequence(0, INDIVIDUAL_READ_LENGTH - 1);
				readStringTwo = currentReadSequence.subSequence(currentReadSequence.size() - INDIVIDUAL_READ_LENGTH, currentReadSequence.size() - 1).getReverseCompliment();
				String uid = generateRandomSequence(UID_LENGTH);
				for (int i = 0; i < readsPerUidProbePair; i++) {
					String baseHeader = IlluminaFastQHeader.getBaseHeader(readHeader) + "0" + i + "0" + j;
					String readOneString = readStringOne.toString();
					String readTwoString = readStringTwo.toString();

					String readOneQuality = generateRandomQualityScore(INDIVIDUAL_READ_LENGTH);
					String readTwoQuality = generateRandomQualityScore(INDIVIDUAL_READ_LENGTH);

					readOneString = uid + readOneString;

					readOneString = readOneString.substring(0, INDIVIDUAL_READ_LENGTH);

					if (baseQualityHeader == null || baseQualityHeader.isEmpty()) {
						baseQualityHeader = "+";
					}
					try {
						fastQOneWriter.write("@" + baseHeader + " 1:0:0:1" + StringUtil.NEWLINE + readOneString + StringUtil.NEWLINE + baseQualityHeader + StringUtil.NEWLINE + readOneQuality
								+ StringUtil.NEWLINE);
						fastQTwoWriter.write("@" + baseHeader + " 1:0:0:2" + StringUtil.NEWLINE + readTwoString + StringUtil.NEWLINE + baseQualityHeader + StringUtil.NEWLINE + readTwoQuality
								+ StringUtil.NEWLINE);
					} catch (IOException e) {
						throw new IllegalStateException(e.getMessage(), e);
					}
				}
			}

		}

	}

	private void createSimulatedReads() {
		List<Probe> probes = new ArrayList<Probe>();
		File outputFastQ1 = new File(outputDirectory, outputFastqOneFileName);
		File outputFastQ2 = new File(outputDirectory, outputFastqTwoFileName);

		try {
			FileUtil.createNewFile(outputFastQ1);
			FileUtil.createNewFile(outputFastQ2);
		} catch (IOException e1) {
			throw new IllegalStateException(e1.getMessage(), e1);
		}

		FileWriter fw1;
		FileWriter fw2;
		try {
			fw1 = new FileWriter(outputFastQ1.getAbsoluteFile());
			fw2 = new FileWriter(outputFastQ2.getAbsoluteFile());
		} catch (IOException e2) {
			throw new IllegalStateException(e2.getMessage(), e2);
		}
		try (BufferedWriter fastQOneWriter = new BufferedWriter(fw1)) {

			try (BufferedWriter fastQTwoWriter = new BufferedWriter(fw2)) {

				int currentProbeLocation = 1;
				int usedRecordIndex = 0;

				for (int i = 0; i < numberOfProbes; i++) {
					String readString = generateRandomSequence(captureTargetAndPrimersLength);
					String readHeader = "M01077:24:000000000-A20BU:1:" + i + ":" + i + ":" + i + " 1:0:0:1";
					String qualityHeader = "";
					write(currentProbeLocation, usedRecordIndex, readHeader, readString, qualityHeader, fastQOneWriter, fastQTwoWriter, probes);
					currentProbeLocation += PROBE_LOCATION_GAP;
					usedRecordIndex++;
				}
			}
		} catch (IOException e1) {
			logger.warn(e1.getMessage(), e1);
		}

		try {
			File outputProbeFile = new File(outputDirectory, outputProbesFileName);
			ProbeFileUtil.writeProbesToFile(probes, outputProbeFile, FILE_OS_FLAVOR);
			logger.debug("Probes written to " + outputProbeFile.getAbsolutePath());
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

	}

	private static String generateRandomSequence(int length) {
		StringBuilder sequence = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			int rand = randomNumberGenerator.nextInt(5);
			if (rand == 0) {
				sequence.append("A");
			} else if (rand == 1) {
				sequence.append("C");
			} else if (rand == 2) {
				sequence.append("G");
			} else if (rand == 3) {
				sequence.append("T");
			} else if (rand == 4) {
				sequence.append("N");
			}
		}
		return sequence.toString();
	}

	private static String generateRandomQualityScore(int length) {
		StringBuilder qualityString = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			int rand = randomNumberGenerator.nextInt(60);
			qualityString.append((char) (rand + BamFileUtil.PHRED_ASCII_OFFSET));
		}
		return qualityString.toString();
	}

	/**
	 * 
	 * @param outputDirectory
	 * @param numberOfProbes
	 * @param readsPerUidProbePair
	 * @param uidsPerProbe
	 * @param captureTargetAndPrimersLength
	 * @param mismatchDetailsString
	 *            for example "M40D10R5^CCCAAATTTGGGM110" represents match 40, delete 10, insert 5 randoms, insert CCCAAATTTGGG, and match 110
	 * @param alternateProbeStrands
	 */
	public static void createSimulatedIlluminaReads(File outputDirectory, int numberOfProbes, int readsPerUidProbePair, int uidsPerProbe, int captureTargetAndPrimersLength,
			String mismatchDetailsString, boolean alternateProbeStrands) {
		createSimulatedIlluminaReads(outputDirectory, DEFAULT_OUTPUT_FASTQ1_NAME, DEFAULT_OUTPUT_FASTQ2_NAME, DEFAULTOUTPUT_PROBE_INFO, numberOfProbes, readsPerUidProbePair, uidsPerProbe,
				captureTargetAndPrimersLength, mismatchDetailsString, alternateProbeStrands);
	}

	/**
	 * 
	 * @param outputDirectory
	 * @param fastqOneFileName
	 * @param fastqTwoFileName
	 * @param probeFileName
	 * @param numberOfProbes
	 * @param readsPerUidProbePair
	 * @param uidsPerProbe
	 * @param captureTargetAndPrimersLength
	 * @param mismatchDetailsString
	 *            for example "M40D10R5^CCCAAATTTGGGM110" represents match 40, delete 10, insert 5 randoms, insert CCCAAATTTGGG, and match 110
	 * @param alternateProbeStrands
	 */
	public static void createSimulatedIlluminaReads(File outputDirectory, String fastqOneFileName, String fastqTwoFileName, String probeFileName, int numberOfProbes, int readsPerUidProbePair,
			int uidsPerProbe, int captureTargetAndPrimersLength, String mismatchDetailsString, boolean alternateProbeStrands) {
		FastQDataSimulator dataSimulator = new FastQDataSimulator(outputDirectory, fastqOneFileName, fastqTwoFileName, probeFileName, numberOfProbes, readsPerUidProbePair, uidsPerProbe,
				captureTargetAndPrimersLength, mismatchDetailsString, alternateProbeStrands);
		dataSimulator.createSimulatedReads();
	}

	private static boolean isValidCode(String code) {
		boolean isValid = code.equals("A") || code.equals("C") || code.equals("G") || code.equals("T") || code.equals("N");
		return isValid;
	}

	static ISequence mutate(ISequence readSequence, String mismatchDetailsString) {
		StringBuilder mutatedSequence = new StringBuilder();
		int readSequenceIndex = 0;
		int i = 0;
		String currentChar = "" + mismatchDetailsString.charAt(i);
		while (i < mismatchDetailsString.length()) {
			if (currentChar.equals(CigarStringUtil.REFERENCE_DELETION_INDICATOR_IN_MD_TAG)) {

				// keep reading until you no longer find a character
				StringBuilder insertSequence = new StringBuilder();
				i++;
				currentChar = "" + mismatchDetailsString.charAt(i);
				while ((i < mismatchDetailsString.length()) && (isValidCode(currentChar))) {
					insertSequence.append(currentChar);
					i++;
					if (i < mismatchDetailsString.length()) {
						currentChar = "" + mismatchDetailsString.charAt(i);
					}
				}
				mutatedSequence.append(insertSequence);
			} else if (currentChar.equals("D") || currentChar.equals("M") || currentChar.equals("R")) {
				boolean isMatch = currentChar.equals("M");
				boolean isRandom = currentChar.equals("R");
				boolean isDeletion = currentChar.equals("D");
				i++;
				currentChar = "" + mismatchDetailsString.charAt(i);

				StringBuilder sizeAsString = new StringBuilder();
				// keep reading until you no longer find a number
				while ((i < mismatchDetailsString.length()) && (StringUtil.isNumeric(currentChar))) {
					sizeAsString.append(currentChar);
					i++;
					if (i < mismatchDetailsString.length()) {
						currentChar = "" + mismatchDetailsString.charAt(i);
					}
				}
				int size = Integer.valueOf(sizeAsString.toString());
				if (isMatch) {
					mutatedSequence.append(readSequence.subSequence(readSequenceIndex, readSequenceIndex + size - 1).toString());
					readSequenceIndex += size;
				} else if (isRandom) {
					String randomSequence = generateRandomSequence(size);
					mutatedSequence.append(randomSequence);
				} else if (isDeletion) {
					readSequenceIndex += size;
				} else {
					throw new IllegalStateException("Unrecognized option[" + currentChar + "].");
				}

			} else if (isValidCode(currentChar)) {
				StringBuilder mismatchString = new StringBuilder();
				// keep reading until you no long find a character
				while ((i < mismatchDetailsString.length()) && (isValidCode(currentChar))) {
					mismatchString.append(currentChar);
					i++;
					if (i < mismatchDetailsString.length()) {
						currentChar = "" + mismatchDetailsString.charAt(i);
					}
				}
				mutatedSequence.append(mismatchString);
				readSequenceIndex += mismatchString.length();
			} else if (currentChar.equals("" + CigarStringUtil.CIGAR_DELETION_FROM_REFERENCE)) {
				StringBuilder deletionSizeAsString = new StringBuilder();
				// keep reading until you no longer find a number
				while ((i < mismatchDetailsString.length()) && (StringUtil.isNumeric(currentChar))) {
					deletionSizeAsString.append(currentChar);
					i++;
					if (i < mismatchDetailsString.length()) {
						currentChar = "" + mismatchDetailsString.charAt(i);
					}
				}
				if (currentChar.equals("" + CigarStringUtil.CIGAR_DELETION_FROM_REFERENCE)) {
					i++;
					currentChar = "" + mismatchDetailsString.charAt(i);
				} else {
					throw new IllegalStateException("Deletion must be terminated by a " + CigarStringUtil.CIGAR_DELETION_FROM_REFERENCE);
				}
				int deletionSize = Integer.valueOf(deletionSizeAsString.toString());
				readSequenceIndex += deletionSize;
			} else {
				throw new IllegalStateException("unrecognized syntax in mismatch details string[" + mismatchDetailsString + "] at index[" + i + "].");
			}
		}

		if (readSequenceIndex != readSequence.size()) {
			throw new IllegalStateException("size[" + readSequenceIndex + "] indicated by mismatchDetailsString[" + mismatchDetailsString + "] does not match readSequence size[" + readSequence.size()
					+ "].");
		}

		return new IupacNucleotideCodeSequence(mutatedSequence.toString());
	}

	public static void main(String[] args) {
		File outputDirectory = new File("C:\\Users\\heilmank\\Desktop\\simulated_data2\\");
		createSimulatedIlluminaReads(outputDirectory, 10000, 10, 10, 160, "M40D10R5^CCCAAATTTGGGM110", true);
	}

}
