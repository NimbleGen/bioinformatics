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
import com.roche.heatseq.objects.ProbesBySequenceName;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.DelimitedFileParserUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public final class ProbeFileUtil {
	private final static Logger logger = LoggerFactory.getLogger(ProbeFileUtil.class);

	private final static String[] PROBE_INFO_HEADER_NAMES = new String[] { "probe_id", "chromosome", "probe_strand", "ext_start", "ext_stop", "ext_sequence", "lig_start", "lig_stop", "lig_sequence",
			"target_start", "target_stop", "target_sequence", "annotation" };

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
	public static ProbesBySequenceName parseProbeInfoFile(File probeInfoFile) throws IOException {
		long probeParsingStartInMs = System.currentTimeMillis();
		Map<String, List<String>> headerNameToValues = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(probeInfoFile, PROBE_INFO_HEADER_NAMES, StringUtil.TAB);
		ProbesBySequenceName probeInfo = new ProbesBySequenceName();

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

			int extensionPrimerStart = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));
			int extensionPrimerStop = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

			ISequence extensionPrimerSequence = new IupacNucleotideCodeSequence(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

			int ligationPrimerStart = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));
			int ligationPrimerStop = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

			ISequence ligationPrimerSequence = new IupacNucleotideCodeSequence(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

			int captureTargetStart = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));
			int captureTargetStop = Integer.valueOf(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));
			ISequence captureTargetSequence = new IupacNucleotideCodeSequence(headerNameToValues.get(PROBE_INFO_HEADER_NAMES[headerIndex++]).get(i));

			// annotation
			headerIndex++;

			Probe probe = new Probe(probeId, sequenceName, extensionPrimerStart, extensionPrimerStop, extensionPrimerSequence, ligationPrimerStart, ligationPrimerStop, ligationPrimerSequence,
					captureTargetStart, captureTargetStop, captureTargetSequence, probeStrand);

			probeInfo.addProbe(sequenceName, probe);

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

}
