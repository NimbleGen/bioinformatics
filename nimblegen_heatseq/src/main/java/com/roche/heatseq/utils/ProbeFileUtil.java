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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.ParsedProbeFile;
import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.process.ProbeInfoFileValidator;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.DelimitedFileParserUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.Md5CheckSumUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public final class ProbeFileUtil {
	private final static Logger logger = LoggerFactory.getLogger(ProbeFileUtil.class);

	private final static String[] PROBE_INFO_HEADER_NAMES = new String[] { "probe_id", "chromosome", "probe_strand", "ext_start", "ext_stop", "ext_sequence", "lig_start", "lig_stop", "lig_sequence",
			"target_start", "target_stop", "target_sequence", "annotation" };

	private final static String EXTENSION_UID_NAME_IN_PROBE_INFO_HEADER = "extension_uid";
	private final static String LIGATION_UID_NAME_IN_PROBE_INFO_HEADER = "ligation_uid";
	private final static String ADDITIONAL_EXTENSION_IN_PROBE_INFO_HEADER = "additional_extension";
	private final static String ADDITIONAL_LIGATION_IN_PROBE_INFO_HEADER = "additional_ligation";
	private final static String GENOME_NAME_IN_PROBE_INFO_HEADER = "genome";
	private final static String DO_NOT_PERFORM_THREE_PRIME_TRIMMING_IN_PROBE_INFO_HEADER = "do_not_perform_three_prime_trimming";
	private final static String BASES_INSIDE_EXTENSION_PRIMER_WINDOW = "bases_inside_extension_primer_window";
	private final static String BASES_INSIDE_LIGATION_PRIMER_WINDOW = "bases_inside_ligation_primer_window";
	private final static String HEADERLESS_MD5SUM = "md5sum_without_header";

	private ProbeFileUtil() {
		throw new AssertionError();
	}

	public static ParsedProbeFile parseProbeInfoFileWithValidation(File probeInfoFile) {
		return ProbeInfoFileValidator.validateAndParseProbeInfoFile(probeInfoFile);
	}

	/**
	 * Parse the probeInfoFile into an object
	 * 
	 * @param probeInfoFile
	 * @return an object representing all of the information found in a probeInfoFile
	 * @throws IOException
	 */
	public static ParsedProbeFile parseProbeInfoFile(File probeInfoFile) throws IOException {
		long probeParsingStartInMs = System.currentTimeMillis();

		Map<String, List<String>> headerNameToValues = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(probeInfoFile, PROBE_INFO_HEADER_NAMES, StringUtil.TAB);
		ParsedProbeFile probeInfo = new ParsedProbeFile();

		int numberOfEntries = 0;
		Iterator<List<String>> iter = headerNameToValues.values().iterator();

		if (iter.hasNext()) {
			numberOfEntries = iter.next().size();
		}

		for (int i = 0; i < numberOfEntries; i++) {
			int headerIndex = 0;

			String probeId = headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i);

			String sequenceName = headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i);

			String probeStrandAsString = headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i);
			Strand probeStrand = Strand.fromString(probeStrandAsString);

			try {
				int extensionPrimerStart = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));
				int extensionPrimerStop = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

				ISequence extensionPrimerSequence = new IupacNucleotideCodeSequence(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

				int ligationPrimerStart = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));
				int ligationPrimerStop = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

				ISequence ligationPrimerSequence = new IupacNucleotideCodeSequence(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

				int captureTargetStart = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));
				int captureTargetStop = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));
				ISequence captureTargetSequence = new IupacNucleotideCodeSequence(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

				String annotation = headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i);

				Probe probe = new Probe(probeId, sequenceName, extensionPrimerStart, extensionPrimerStop, extensionPrimerSequence, ligationPrimerStart, ligationPrimerStop, ligationPrimerSequence,
						captureTargetStart, captureTargetStop, captureTargetSequence, probeStrand, annotation);

				probeInfo.addProbe(sequenceName, probe);
			} catch (NumberFormatException e) {
				throw new IllegalStateException("Unable to parse line[" + (i + 3) + "] of the probe info file[" + probeInfoFile.getAbsolutePath() + "].");
			}

		}

		long probeParsingStopInMs = System.currentTimeMillis();

		logger.debug("Done parsing probeInfo[" + probeInfoFile.getAbsolutePath() + "]  Total time: " + DateUtil.convertMillisecondsToHHMMSS(probeParsingStopInMs - probeParsingStartInMs));

		return probeInfo;
	}

	public static enum FileOsFlavor {
		WINDOWS, LINUX, CURRENT_SYSTEM
	};

	public static String getHeaderlessMd5SumOfFile(File probeInfoFile) {
		String headerlessMd5Sum = null;
		try {
			String fileString = FileUtil.readFileAsString(probeInfoFile);

			String headerlessText = fileString;
			if (fileString.startsWith("#")) {
				int firstLineEndIndex = fileString.indexOf(StringUtil.LINUX_NEWLINE);
				headerlessText = fileString.substring(firstLineEndIndex + 1, fileString.length());
			}

			if (headerlessText.length() > 0) {
				headerlessMd5Sum = Md5CheckSumUtil.md5sum(headerlessText.toString());
			}
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
		return headerlessMd5Sum;
	}

	/**
	 * write all the provided probes to the provided outputProbeInfoFile
	 * 
	 * @param probes
	 * @param outputProbeInfoFile
	 * @param fileOsFlavor
	 * @throws IOException
	 */
	public static void writeProbesToFile(List<Probe> probes, File outputProbeInfoFile, FileOsFlavor fileOsFlavor) throws IOException {

		String newLine = null;
		if (fileOsFlavor == FileOsFlavor.CURRENT_SYSTEM) {
			newLine = StringUtil.NEWLINE;
		} else if (fileOsFlavor == FileOsFlavor.LINUX) {
			newLine = StringUtil.LINUX_NEWLINE;
		}
		if (fileOsFlavor == FileOsFlavor.WINDOWS) {
			newLine = StringUtil.WINDOWS_NEWLINE;
		}

		if (!outputProbeInfoFile.exists()) {
			outputProbeInfoFile.createNewFile();
		}

		StringBuilder headerBuilder = new StringBuilder();

		for (String headerName : PROBE_INFO_HEADER_NAMES) {
			headerBuilder.append(headerName + StringUtil.TAB);
		}
		// exclude the last tab
		String header = headerBuilder.substring(0, headerBuilder.length() - 1);

		FileWriter probeFileWriter = new FileWriter(outputProbeInfoFile.getAbsoluteFile());
		BufferedWriter probeWriter = new BufferedWriter(probeFileWriter);

		probeWriter.write(header + newLine);

		for (Probe probe : probes) {
			StringBuilder lineBuilder = new StringBuilder();

			lineBuilder.append(probe.getProbeId() + StringUtil.TAB);
			lineBuilder.append(probe.getSequenceName() + StringUtil.TAB);
			lineBuilder.append(probe.getProbeStrand().getSymbol() + StringUtil.TAB);
			lineBuilder.append(probe.getExtensionPrimerStart() + StringUtil.TAB);
			lineBuilder.append(probe.getExtensionPrimerStop() + StringUtil.TAB);
			lineBuilder.append(probe.getExtensionPrimerSequence() + StringUtil.TAB);
			lineBuilder.append(probe.getLigationPrimerStart() + StringUtil.TAB);
			lineBuilder.append(probe.getLigationPrimerStop() + StringUtil.TAB);
			lineBuilder.append(probe.getLigationPrimerSequence() + StringUtil.TAB);
			lineBuilder.append(probe.getCaptureTargetStart() + StringUtil.TAB);
			lineBuilder.append(probe.getCaptureTargetStop() + StringUtil.TAB);
			lineBuilder.append(probe.getCaptureTargetSequence() + StringUtil.TAB);
			// annotation
			lineBuilder.append("" + StringUtil.TAB);

			String line = lineBuilder.toString();
			probeWriter.write(line + newLine);
		}

		probeWriter.close();
	}

	public static class ProbeHeaderInformation {
		private final Integer ligationUidLength;
		private final Integer extensionUidLength;
		private final Integer additionalLigationTrimLength;
		private final Integer additionalExtensionTrimLength;
		private final Integer basesInsideExtensionPrimerWindow;
		private final Integer basesInsideLigationPrimerWindow;
		private final Boolean performThreePrimeTrimming;
		private final String genomeName;
		private final String headerlessMd5Sum;

		public ProbeHeaderInformation(Integer ligationUidLength, Integer extensionUidLength, Integer additionalLigationTrimLength, Integer additionalExtensionTrimLength,
				Integer basesInsideExtensionPrimerWindow, Integer basesInsideLigationPrimerWindow, Boolean performThreePrimeTrimming, String genomeName, String headerlessMd5Sum) {
			super();
			this.ligationUidLength = ligationUidLength;
			this.extensionUidLength = extensionUidLength;
			this.additionalLigationTrimLength = additionalLigationTrimLength;
			this.additionalExtensionTrimLength = additionalExtensionTrimLength;
			this.basesInsideExtensionPrimerWindow = basesInsideExtensionPrimerWindow;
			this.basesInsideLigationPrimerWindow = basesInsideLigationPrimerWindow;
			this.performThreePrimeTrimming = performThreePrimeTrimming;
			this.genomeName = genomeName;
			this.headerlessMd5Sum = headerlessMd5Sum;
		}

		public Integer getLigationUidLength() {
			return ligationUidLength;
		}

		public Integer getExtensionUidLength() {
			return extensionUidLength;
		}

		public Integer getAdditionalLigationTrimLength() {
			return additionalLigationTrimLength;
		}

		public Integer getAdditionalExtensionTrimLength() {
			return additionalExtensionTrimLength;
		}

		public Integer getBasesInsideExtensionPrimerWindow() {
			return basesInsideExtensionPrimerWindow;
		}

		public Integer getBasesInsideLigationPrimerWindow() {
			return basesInsideLigationPrimerWindow;
		}

		public Boolean getPerformThreePrimeTrimming() {
			return performThreePrimeTrimming;
		}

		public String getGenomeName() {
			return genomeName;
		}

		public String getHeaderlessMd5Sum() {
			return headerlessMd5Sum;
		}
	}

	public static ProbeHeaderInformation extractProbeHeaderInformation(File probeFile) throws FileNotFoundException {
		Integer ligationUidLength = null;
		Map<String, String> nameValuePairs = null;
		try {
			nameValuePairs = DelimitedFileParserUtil.parseCommentLinesNameValuePairs(probeFile);
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
			nameValuePairs = new HashMap<String, String>();
		}
		String ligationUidLengthAsString = nameValuePairs.get(LIGATION_UID_NAME_IN_PROBE_INFO_HEADER);
		if (ligationUidLengthAsString != null) {
			try {
				ligationUidLength = Integer.parseInt(ligationUidLengthAsString);
			} catch (NumberFormatException e) {
				logger.warn("Unable to parse extension uid length from probe information header[" + ligationUidLengthAsString + "] as integer.");
			}
		}

		Integer extensionUidLength = null;
		String extensionUidLengthAsString = nameValuePairs.get(EXTENSION_UID_NAME_IN_PROBE_INFO_HEADER);
		if (extensionUidLengthAsString != null) {
			try {
				extensionUidLength = Integer.parseInt(extensionUidLengthAsString);
			} catch (NumberFormatException e) {
				logger.warn("Unable to parse extension uid length from probe information header[" + extensionUidLengthAsString + "] as integer.");
			}
		}

		Integer additionalLigationLength = null;
		String additionalLigationLengthAsString = nameValuePairs.get(ADDITIONAL_LIGATION_IN_PROBE_INFO_HEADER);
		if (additionalLigationLengthAsString != null) {
			try {
				additionalLigationLength = Integer.parseInt(additionalLigationLengthAsString);
			} catch (NumberFormatException e) {
				logger.warn("Unable to parse extension uid length from probe information header[" + additionalLigationLengthAsString + "] as integer.");
			}
		}

		Integer additionalExtensionLength = null;
		String additionalExtensionLengthAsString = nameValuePairs.get(ADDITIONAL_EXTENSION_IN_PROBE_INFO_HEADER);
		if (additionalExtensionLengthAsString != null) {
			try {
				additionalExtensionLength = Integer.parseInt(additionalExtensionLengthAsString);
			} catch (NumberFormatException e) {
				logger.warn("Unable to parse extension uid length from probe information header[" + additionalExtensionLengthAsString + "] as integer.");
			}
		}

		Integer basesInsideExtensionPrimerWindow = null;
		String basesInsideExtensionPrimerWindowAsString = nameValuePairs.get(BASES_INSIDE_EXTENSION_PRIMER_WINDOW);
		if (basesInsideExtensionPrimerWindowAsString != null) {
			try {
				basesInsideExtensionPrimerWindow = Integer.parseInt(basesInsideExtensionPrimerWindowAsString);
			} catch (NumberFormatException e) {
				logger.warn("Unable to parse bases inside extension primer window from probe information header[" + basesInsideExtensionPrimerWindowAsString + "] as integer.");
			}
		}

		Integer basesInsideLigationPrimerWindow = null;
		String basesInsideLigationPrimerWindowAsString = nameValuePairs.get(BASES_INSIDE_LIGATION_PRIMER_WINDOW);
		if (basesInsideLigationPrimerWindowAsString != null) {
			try {
				basesInsideLigationPrimerWindow = Integer.parseInt(basesInsideLigationPrimerWindowAsString);
			} catch (NumberFormatException e) {
				logger.warn("Unable to parse bases inside ligation primer window from probe information header[" + basesInsideLigationPrimerWindowAsString + "] as integer.");
			}
		}

		Boolean performThreePrimeTrimming = !nameValuePairs.containsKey(DO_NOT_PERFORM_THREE_PRIME_TRIMMING_IN_PROBE_INFO_HEADER);

		String genomeName = nameValuePairs.get(GENOME_NAME_IN_PROBE_INFO_HEADER);
		if (genomeName != null) {
			genomeName = genomeName.toLowerCase();
		}

		String headerlessMd5Sum = nameValuePairs.get(HEADERLESS_MD5SUM);

		return new ProbeHeaderInformation(ligationUidLength, extensionUidLength, additionalLigationLength, additionalExtensionLength, basesInsideExtensionPrimerWindow,
				basesInsideLigationPrimerWindow, performThreePrimeTrimming, genomeName, headerlessMd5Sum);
	}

	public static void reverseComplimentProbeFile(File inputProbeInfoFile, File outputProbeInfoFile) {
		try {
			FileUtil.createNewFile(outputProbeInfoFile);

			ParsedProbeFile parsedProbeFile = parseProbeInfoFile(inputProbeInfoFile);

			String firstLine = FileUtil.readFirstLineAsString(inputProbeInfoFile);

			try (FileWriter writer = new FileWriter(outputProbeInfoFile)) {
				if (firstLine.startsWith("#")) {
					// write out the comment line
					writer.write(firstLine + StringUtil.NEWLINE);
				}

				// write out the header
				for (String columnHeader : PROBE_INFO_HEADER_NAMES) {
					writer.write(columnHeader);
					if (columnHeader.equals(PROBE_INFO_HEADER_NAMES[PROBE_INFO_HEADER_NAMES.length - 1])) {
						writer.write(StringUtil.NEWLINE);
					} else {
						writer.write(StringUtil.TAB);
					}
				}

				for (Probe probe : parsedProbeFile.getProbes()) {
					writer.write(probe.getProbeId() + StringUtil.TAB + probe.getSequenceName() + StringUtil.TAB + probe.getProbeStrand() + StringUtil.TAB + probe.getExtensionPrimerStart()
							+ StringUtil.TAB + probe.getExtensionPrimerStop() + StringUtil.TAB + probe.getExtensionPrimerSequence().getReverseCompliment() + StringUtil.TAB
							+ probe.getLigationPrimerStart() + StringUtil.TAB + probe.getLigationPrimerStop() + StringUtil.TAB + probe.getLigationPrimerSequence().getReverseCompliment()
							+ StringUtil.TAB + probe.getCaptureTargetStart() + StringUtil.TAB + probe.getCaptureTargetStop() + StringUtil.TAB + probe.getCaptureTargetSequence().getReverseCompliment()
							+ StringUtil.TAB + probe.getAnnotation());
				}

			}

		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
}
