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
	private final static int EXTENSION_PRIMER_LENGTH = 22;
	private final static int LIGATION_PRIMER_LENGTH = 18;

	private final static int PROBE_LOCATION_GAP = 1000;
	private final static String SEQUENCE_NAME = "chr1";

	private final static int INDIVIDUAL_READ_LENGTH = 120;

	private final static int MIN_CAPTURE_TARGET_READ_LENGTH = 20;

	private final int numberOfProbes;

	private final File outputDirectory;

	private final int readsPerUidProbePair;
	private final int uidsPerProbe;

	private final String mismatchDetailsString;
	private final int captureTargetAndPrimersLength;

	private final boolean includeReverseProbes;
	private final boolean includeForwardProbes;

	private final String outputFastqOneFileName;
	private final String outputFastqTwoFileName;
	private final String outputProbesFileName;

	private final int uidLength;

	private FastQDataSimulator(File outputDirectory, String outputFastqOneFileName, String outputFastqTwoFileName, String outputProbesFileName, int uidLength, int numberOfProbes,
			int readsPerUidProbePair, int uidsPerProbe, int captureTargetAndPrimersLength, String mismatchDetailsString, boolean includeReverseProbes, boolean includeForwardProbes) {

		this.outputDirectory = outputDirectory;
		this.numberOfProbes = numberOfProbes;
		this.readsPerUidProbePair = readsPerUidProbePair;
		this.uidsPerProbe = uidsPerProbe;
		this.mismatchDetailsString = mismatchDetailsString;
		this.includeForwardProbes = includeForwardProbes;
		this.includeReverseProbes = includeReverseProbes;
		this.captureTargetAndPrimersLength = captureTargetAndPrimersLength;
		this.uidLength = uidLength;

		this.outputFastqOneFileName = outputFastqOneFileName;
		this.outputFastqTwoFileName = outputFastqTwoFileName;
		this.outputProbesFileName = outputProbesFileName;
	}

	private void write(int currentProbeLocation, int usedRecordIndex, String forwardReadHeader, String reverseReadHeader, String readString, String baseQualityHeader, BufferedWriter fastQOneWriter,
			BufferedWriter fastQTwoWriter, List<Probe> probes) {
		ISequence readSequence = new IupacNucleotideCodeSequence(readString);

		int probeIndex = 1;
		if (readSequence.size() > (EXTENSION_PRIMER_LENGTH + LIGATION_PRIMER_LENGTH + MIN_CAPTURE_TARGET_READ_LENGTH)) {
			String sequenceName = SEQUENCE_NAME;

			ISequence readStringOne = null;
			ISequence readStringTwo = null;

			ISequence currentReadSequence = readSequence;
			if (mismatchDetailsString != null && !mismatchDetailsString.isEmpty()) {
				currentReadSequence = mutate(readSequence, mismatchDetailsString);
			}

			// assumptions
			// primer sequence are always listed from 5' to 3' and not from start to stop
			// start is always at the lower coordinate regardless of strand
			if (includeForwardProbes) {
				int forwardExtensionPrimerStart = currentProbeLocation;
				int forwardExtensionPrimerStop = forwardExtensionPrimerStart + EXTENSION_PRIMER_LENGTH - 1;
				ISequence forwardExtensionPrimerSequence = readSequence.subSequence(0, EXTENSION_PRIMER_LENGTH - 1);
				int forwardLigationPrimerStop = currentProbeLocation + readSequence.size();
				int forwardLigationPrimerStart = forwardLigationPrimerStop - LIGATION_PRIMER_LENGTH + 1;
				ISequence forwardLigationPrimerSequence = readSequence.subSequence(readSequence.size() - LIGATION_PRIMER_LENGTH, readSequence.size() - 1);
				int forwardCaptureTargetStart = forwardExtensionPrimerStop + 1;
				int forwardCaptureTargetStop = forwardLigationPrimerStart - 1;
				ISequence forwardCaptureTargetSequence = readSequence.subSequence(EXTENSION_PRIMER_LENGTH, readSequence.size() - LIGATION_PRIMER_LENGTH - 1);
				Strand forwardProbeStrand = Strand.FORWARD;

				Probe forwardProbe = new Probe("" + probeIndex, sequenceName, forwardExtensionPrimerStart, forwardExtensionPrimerStop, forwardExtensionPrimerSequence, forwardLigationPrimerStart,
						forwardLigationPrimerStop, forwardLigationPrimerSequence, forwardCaptureTargetStart, forwardCaptureTargetStop, forwardCaptureTargetSequence, forwardProbeStrand);
				probes.add(forwardProbe);
				probeIndex++;

				readStringOne = currentReadSequence.subSequence(0, INDIVIDUAL_READ_LENGTH - 1);
				readStringTwo = currentReadSequence.subSequence(readSequence.size() - INDIVIDUAL_READ_LENGTH, readSequence.size() - 1).getReverseCompliment();

				for (int j = 0; j < uidsPerProbe; j++) {
					int variableUidLength = randomNumberGenerator.nextInt(uidLength) + 1;
					String uid = generateRandomSequence(variableUidLength);
					for (int i = 0; i < readsPerUidProbePair; i++) {
						String baseHeader = IlluminaFastQHeader.getBaseHeader(forwardReadHeader) + "0" + i + "0" + j;
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
			if (includeReverseProbes) {
				ISequence reverseComplimentReadSequence = readSequence.getReverseCompliment();

				int reverseLigationPrimerStart = currentProbeLocation;
				int reverseLigationPrimerStop = reverseLigationPrimerStart + LIGATION_PRIMER_LENGTH - 1;

				ISequence reverseLigationPrimerSequence = reverseComplimentReadSequence.subSequence(reverseComplimentReadSequence.size() - LIGATION_PRIMER_LENGTH,
						reverseComplimentReadSequence.size() - 1);

				int reverseExtensionPrimerStop = currentProbeLocation + reverseComplimentReadSequence.size();
				int reverseExtensionPrimerStart = reverseExtensionPrimerStop - EXTENSION_PRIMER_LENGTH + 1;

				ISequence reverseExtensionPrimerSequence = reverseComplimentReadSequence.subSequence(0, EXTENSION_PRIMER_LENGTH - 1);

				int reverseCaptureTargetStart = reverseExtensionPrimerStart - 1;
				int reverseCaptureTargetStop = reverseLigationPrimerStop + 1;

				ISequence reverseCaptureTargetSequence = reverseComplimentReadSequence.subSequence(EXTENSION_PRIMER_LENGTH, reverseComplimentReadSequence.size() - LIGATION_PRIMER_LENGTH - 1);
				Strand reverseProbeStrand = Strand.REVERSE;

				Probe reverseProbe = new Probe("" + probeIndex, sequenceName, reverseExtensionPrimerStart, reverseExtensionPrimerStop, reverseExtensionPrimerSequence, reverseLigationPrimerStart,
						reverseLigationPrimerStop, reverseLigationPrimerSequence, reverseCaptureTargetStart, reverseCaptureTargetStop, reverseCaptureTargetSequence, reverseProbeStrand);
				probes.add(reverseProbe);
				probeIndex++;

				ISequence currentReverseComplimentReadSequence = currentReadSequence.getReverseCompliment();

				readStringOne = currentReverseComplimentReadSequence.subSequence(0, INDIVIDUAL_READ_LENGTH - 1);
				readStringTwo = currentReverseComplimentReadSequence.subSequence(readSequence.size() - INDIVIDUAL_READ_LENGTH, readSequence.size() - 1).getReverseCompliment();

				for (int j = 0; j < uidsPerProbe; j++) {
					String uid = generateRandomSequence(uidLength);
					for (int i = 0; i < readsPerUidProbePair; i++) {
						String baseHeader = IlluminaFastQHeader.getBaseHeader(reverseReadHeader) + "0" + i + "0" + j;
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
					// need i+1 because a number like 00000 will be truncated to zero and not match when mapping whereas 10000 will not.
					String forwardReadHeader = "M01077:24:000000000-A20BUF:" + currentProbeLocation + ":" + i + ":" + i + ":" + (i + 1) + " 1:0:0:1";
					String reverseReadHeader = "M01077:24:000000000-A20BUR:" + currentProbeLocation + ":" + i + ":" + i + ":" + (i + 1) + " 1:0:0:1";
					String qualityHeader = "";
					write(currentProbeLocation, usedRecordIndex, forwardReadHeader, reverseReadHeader, readString, qualityHeader, fastQOneWriter, fastQTwoWriter, probes);
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
	public static void createSimulatedIlluminaReads(File outputDirectory, int uidLength, int numberOfProbes, int readsPerUidProbePair, int uidsPerProbe, int captureTargetAndPrimersLength,
			String mismatchDetailsString, boolean includeForwardProbes, boolean includeReverseProbes) {
		createSimulatedIlluminaReads(outputDirectory, DEFAULT_OUTPUT_FASTQ1_NAME, DEFAULT_OUTPUT_FASTQ2_NAME, DEFAULTOUTPUT_PROBE_INFO, uidLength, numberOfProbes, readsPerUidProbePair, uidsPerProbe,
				captureTargetAndPrimersLength, mismatchDetailsString, includeForwardProbes, includeReverseProbes);
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
	public static void createSimulatedIlluminaReads(File outputDirectory, String fastqOneFileName, String fastqTwoFileName, String probeFileName, int uidLength, int numberOfProbes,
			int readsPerUidProbePair, int uidsPerProbe, int captureTargetAndPrimersLength, String mismatchDetailsString, boolean includeForwardProbes, boolean includeReverseProbes) {
		FastQDataSimulator dataSimulator = new FastQDataSimulator(outputDirectory, fastqOneFileName, fastqTwoFileName, probeFileName, uidLength, numberOfProbes, readsPerUidProbePair, uidsPerProbe,
				captureTargetAndPrimersLength, mismatchDetailsString, includeForwardProbes, includeReverseProbes);
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
		createSimulatedIlluminaReads(outputDirectory, 14, 10000, 10, 10, 160, "M40D10R5^CCCAAATTTGGGM110", true, true);
	}

}
