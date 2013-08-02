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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.objects.ProbesByContainerName;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.DelimitedFileParserUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public final class ProbeFileUtil {
	private final static Logger logger = LoggerFactory.getLogger(ProbeFileUtil.class);

	private final static String[] PROBE_INFO_HEADER_NAMES = new String[] { "chr", "ext_probe_start", "ext_probe_stop", "ext_probe_sequence", "ext_copy_count", "lig_probe_start", "lig_probe_stop",
			"lig_probe_sequence", "lig_copy_count", "mip_target_start_position", "mip_target_stop_position", "mip_target_sequence", "feature_start_position", "feature_stop_position",
			"feature_mip_count", "probe_strand" };

	private ProbeFileUtil() {
		throw new AssertionError();
	}

	/**
	 * Parse the probeInfoFile into an object
	 * 
	 * @param probeInfoFile
	 * @return an object representing all of the information found in a probeInfoFile
	 * @throws IOException
	 */
	public static ProbesByContainerName parseProbeInfoFile(File probeInfoFile) throws IOException {
		long probeParsingStartInMs = System.currentTimeMillis();
		Map<String, List<String>> headerNameToValues = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(probeInfoFile, PROBE_INFO_HEADER_NAMES, StringUtil.TAB);
		ProbesByContainerName probeInfo = new ProbesByContainerName();

		int numberOfEntries = 0;
		Iterator<List<String>> iter = headerNameToValues.values().iterator();

		if (iter.hasNext()) {
			numberOfEntries = iter.next().size();
		}

		for (int i = 0; i < numberOfEntries; i++) {
			int headerIndex = 0;

			String containerName = headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i);
			int extensionPrimerStart = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));
			int extensionPrimerStop = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

			ISequence extensionPrimerSequence = new IupacNucleotideCodeSequence(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

			// ext copy count
			headerIndex++;

			int ligationPrimerStart = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));
			int ligationPrimerStop = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

			ISequence ligationPrimerSequence = new IupacNucleotideCodeSequence(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

			// lig copy count
			headerIndex++;

			int captureTargetStart = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));
			int captureTargetStop = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));
			ISequence captureTargetSequence = new IupacNucleotideCodeSequence(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

			int featureStart = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));
			int featureStop = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

			// mip count
			headerIndex++;

			String probeStrandAsString = headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i);
			Strand probeStrand = Strand.fromString(probeStrandAsString);
			Probe probe = new Probe(i, containerName, extensionPrimerStart, extensionPrimerStop, extensionPrimerSequence, ligationPrimerStart, ligationPrimerStop, ligationPrimerSequence,
					captureTargetStart, captureTargetStop, captureTargetSequence, featureStart, featureStop, probeStrand);

			probeInfo.addProbe(containerName, probe);

		}

		long probeParsingStopInMs = System.currentTimeMillis();

		logger.debug("Done parsing probeInfo[" + probeInfoFile.getAbsolutePath() + "]  Total time: " + DateUtil.convertMillisecondsToHHMMSS(probeParsingStopInMs - probeParsingStartInMs));

		return probeInfo;

	}

	public static enum FileOsFlavor {
		WINDOWS, LINUX, CURRENT_SYSTEM
	};

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

			lineBuilder.append(probe.getContainerName() + StringUtil.TAB);
			lineBuilder.append(probe.getExtensionPrimerStart() + StringUtil.TAB);
			lineBuilder.append(probe.getExtensionPrimerStop() + StringUtil.TAB);
			lineBuilder.append(probe.getExtensionPrimerSequence() + StringUtil.TAB);
			// ext_copy_count
			lineBuilder.append(0 + StringUtil.TAB);
			lineBuilder.append(probe.getLigationPrimerStart() + StringUtil.TAB);
			lineBuilder.append(probe.getLigationPrimerStop() + StringUtil.TAB);
			lineBuilder.append(probe.getLigationPrimerSequence() + StringUtil.TAB);
			// lig_copy_count
			lineBuilder.append(0 + StringUtil.TAB);
			lineBuilder.append(probe.getCaptureTargetStart() + StringUtil.TAB);
			lineBuilder.append(probe.getCaptureTargetStop() + StringUtil.TAB);
			lineBuilder.append(probe.getCaptureTargetSequence() + StringUtil.TAB);
			lineBuilder.append(probe.getFeatureStart() + StringUtil.TAB);
			lineBuilder.append(probe.getFeatureStop() + StringUtil.TAB);
			// feature_mip_count
			lineBuilder.append(0 + StringUtil.TAB);
			lineBuilder.append(probe.getProbeStrand().getSymbol());

			String line = lineBuilder.toString();
			probeWriter.write(line + newLine);
		}

		probeWriter.close();
	}

}
