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
package com.roche.heatseq.process;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.cli.CliStatusConsole;
import com.roche.heatseq.cli.DeduplicationCli;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.fastq.FastqReader;
import com.roche.sequencing.bioinformatics.common.utils.fastq.PicardException;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ParsedProbeFile;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.Probe;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ProbeFileUtil;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ProbeFileUtil.ProbeHeaderInformation;

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.fastq.FastqWriterFactory;

public class FastqReadTrimmer {

	private static Logger logger = LoggerFactory.getLogger(FastqReadTrimmer.class);

	public static void trimReads(File inputFastqOneFile, File inputFastqTwoFile, ParsedProbeFile probeInfo, File probeInfoFile, File outputFastqOneFile, File outputFastqTwoFile, boolean trimPrimers)
			throws IOException {
		ProbeTrimmingInformation probeTrimmingInformation = getProbeTrimmingInformation(probeInfo, probeInfoFile, trimPrimers);
		boolean performThreePrimeTrimming = probeTrimmingInformation.isPerformThreePrimeTrimming();

		int readOneTrimFromStart = probeTrimmingInformation.getReadOneTrimFromStart();
		int readOneTrimStop = probeTrimmingInformation.getReadOneTrimStop();
		int readTwoTrimFromStart = probeTrimmingInformation.getReadTwoTrimFromStart();
		int readTwoTrimStop = probeTrimmingInformation.getReadTwoTrimStop();

		logger.info("read one--first base to keep:" + readOneTrimFromStart + "  lastBaseToKeep:" + readOneTrimStop);
		logger.info("read two--first base to keep:" + readTwoTrimFromStart + "  lastBaseToKeep:" + readTwoTrimStop);

		PrimerReadExtensionAndPcrDuplicateIdentification.verifyReadNamesCanBeHandledByDedup(inputFastqOneFile, inputFastqTwoFile);

		if (!trimPrimers) {
			performThreePrimeTrimming = false;
		}

		try {
			trimReads(inputFastqOneFile, outputFastqOneFile, readOneTrimFromStart, readOneTrimStop, performThreePrimeTrimming);
		} catch (PicardException e) {
			throw new IllegalStateException("Unable to parse Fastq File One[" + inputFastqOneFile.getAbsolutePath() + "].  " + e.getMessage());
		}
		CliStatusConsole.logStatus(
				"Finished trimming (1 of 2): " + inputFastqOneFile.getAbsolutePath() + ".  The trimmed output has been placed at " + outputFastqOneFile.getAbsolutePath() + "." + StringUtil.NEWLINE);
		try {
			trimReads(inputFastqTwoFile, outputFastqTwoFile, readTwoTrimFromStart, readTwoTrimStop, performThreePrimeTrimming);
		} catch (PicardException e) {
			throw new IllegalStateException("Unable to parse input Fastq File Two[" + inputFastqTwoFile.getAbsolutePath() + "].  " + e.getMessage());
		}
		CliStatusConsole.logStatus(
				"Finished trimming (2 of 2):" + inputFastqTwoFile.getAbsolutePath() + ".  The trimmed output has been placed at " + outputFastqTwoFile.getAbsolutePath() + "." + StringUtil.NEWLINE);
	}

	public static ProbeTrimmingInformation getProbeTrimmingInformation(ParsedProbeFile probeInfo, File probeInfoFile, boolean trimPrimers) throws IOException {
		ProbeInfoStats probeInfoStats = collectStatsFromProbeInformation(probeInfo);

		logger.info(probeInfoStats.toString());

		ProbeHeaderInformation probeHeaderInformation = ProbeFileUtil.extractProbeHeaderInformation(probeInfoFile);

		int extensionUidLength = DeduplicationCli.DEFAULT_EXTENSION_UID_LENGTH;
		if (probeHeaderInformation.getExtensionUidLength() != null) {
			extensionUidLength = probeHeaderInformation.getExtensionUidLength();
		}
		int ligationUidLength = DeduplicationCli.DEFAULT_LIGATION_UID_LENGTH;
		if (probeHeaderInformation.getLigationUidLength() != null) {
			ligationUidLength = probeHeaderInformation.getLigationUidLength();
		}
		int additionalExtensionTrimLength = 0;
		if (probeHeaderInformation.getAdditionalExtensionTrimLength() != null) {
			additionalExtensionTrimLength = probeHeaderInformation.getAdditionalExtensionTrimLength();
		}
		int additionalLigationTrimLength = 0;
		if (probeHeaderInformation.getAdditionalLigationTrimLength() != null) {
			additionalLigationTrimLength = probeHeaderInformation.getAdditionalLigationTrimLength();
		}

		boolean performThreePrimeTrimming = probeHeaderInformation.getPerformThreePrimeTrimming();

		int readOneTrimFromStart = additionalExtensionTrimLength + extensionUidLength;
		if (trimPrimers) {
			readOneTrimFromStart += probeInfoStats.getMaxExtensionPrimerLength();
		}

		int readOneTrimStop = probeInfoStats.getMinCaptureTargetLength();
		if (trimPrimers) {
			readOneTrimStop += probeInfoStats.getMinLigationPrimerLength();
		}

		int readTwoTrimFromStart = additionalLigationTrimLength + ligationUidLength;
		if (trimPrimers) {
			readTwoTrimFromStart += probeInfoStats.getMaxLigationPrimerLength();
		}

		int readTwoTrimStop = probeInfoStats.getMinCaptureTargetLength();
		if (trimPrimers) {
			readTwoTrimStop += probeInfoStats.getMinExtensionPrimerLength();
		}

		return new ProbeTrimmingInformation(performThreePrimeTrimming, readOneTrimFromStart, readOneTrimStop, readTwoTrimFromStart, readTwoTrimStop);

	}

	static ProbeInfoStats collectStatsFromProbeInformation(ParsedProbeFile probes) throws IOException {

		int maxExtensionPrimerLength = 0;
		int maxLigationPrimerLength = 0;
		int maxCaptureTargetLength = 0;

		int minExtensionPrimerLength = Integer.MAX_VALUE;
		int minLigationPrimerLength = Integer.MAX_VALUE;
		int minCaptureTargetLength = Integer.MAX_VALUE;

		for (Probe probe : probes) {
			int extensionPrimerLength = probe.getExtensionPrimerSequence().size();
			int ligationPrimerLength = probe.getLigationPrimerSequence().size();
			int captureTargetLength = probe.getCaptureTargetSequence().size();
			maxExtensionPrimerLength = Math.max(extensionPrimerLength, maxExtensionPrimerLength);
			maxLigationPrimerLength = Math.max(ligationPrimerLength, maxLigationPrimerLength);
			maxCaptureTargetLength = Math.max(captureTargetLength, maxCaptureTargetLength);
			minExtensionPrimerLength = Math.min(extensionPrimerLength, minExtensionPrimerLength);
			minLigationPrimerLength = Math.min(ligationPrimerLength, minLigationPrimerLength);
			minCaptureTargetLength = Math.min(captureTargetLength, minCaptureTargetLength);
		}

		return new ProbeInfoStats(maxExtensionPrimerLength, maxLigationPrimerLength, maxCaptureTargetLength, minExtensionPrimerLength, minLigationPrimerLength, minCaptureTargetLength);
	}

	public static class ProbeTrimmingInformation {
		private final boolean performThreePrimeTrimming;
		private final int readOneTrimFromStart;
		private final int readOneTrimStop;
		private final int readTwoTrimFromStart;
		private final int readTwoTrimStop;

		private ProbeTrimmingInformation(boolean performThreePrimeTrimming, int readOneTrimFromStart, int readOneTrimStop, int readTwoTrimFromStart, int readTwoTrimStop) {
			super();
			this.performThreePrimeTrimming = performThreePrimeTrimming;
			this.readOneTrimFromStart = readOneTrimFromStart;
			this.readOneTrimStop = readOneTrimStop;
			this.readTwoTrimFromStart = readTwoTrimFromStart;
			this.readTwoTrimStop = readTwoTrimStop;
		}

		public boolean isPerformThreePrimeTrimming() {
			return performThreePrimeTrimming;
		}

		public int getReadOneTrimFromStart() {
			return readOneTrimFromStart;
		}

		public int getReadOneTrimStop() {
			return readOneTrimStop;
		}

		public int getReadTwoTrimFromStart() {
			return readTwoTrimFromStart;
		}

		public int getReadTwoTrimStop() {
			return readTwoTrimStop;
		}
	}

	static class ProbeInfoStats {
		private final int maxExtensionPrimerLength;
		private final int maxLigationPrimerLength;
		private final int maxCaptureTargetLength;

		private final int minExtensionPrimerLength;
		private final int minLigationPrimerLength;
		private final int minCaptureTargetLength;

		private ProbeInfoStats(int maxExtensionPrimerLength, int maxLigationPrimerLength, int maxCaptureTargetLength, int minExtensionPrimerLength, int minLigationPrimerLength,
				int minCaptureTargetLength) {
			super();
			this.maxExtensionPrimerLength = maxExtensionPrimerLength;
			this.maxLigationPrimerLength = maxLigationPrimerLength;
			this.maxCaptureTargetLength = maxCaptureTargetLength;
			this.minExtensionPrimerLength = minExtensionPrimerLength;
			this.minLigationPrimerLength = minLigationPrimerLength;
			this.minCaptureTargetLength = minCaptureTargetLength;
		}

		public int getMaxExtensionPrimerLength() {
			return maxExtensionPrimerLength;
		}

		public int getMaxLigationPrimerLength() {
			return maxLigationPrimerLength;
		}

		public int getMaxCaptureTargetLength() {
			return maxCaptureTargetLength;
		}

		public int getMinExtensionPrimerLength() {
			return minExtensionPrimerLength;
		}

		public int getMinLigationPrimerLength() {
			return minLigationPrimerLength;
		}

		public int getMinCaptureTargetLength() {
			return minCaptureTargetLength;
		}

		@Override
		public String toString() {
			return "ProbeInfoStats [maxExtensionPrimerLength=" + maxExtensionPrimerLength + ", maxLigationPrimerLength=" + maxLigationPrimerLength + ", maxCaptureTargetLength="
					+ maxCaptureTargetLength + ", minExtensionPrimerLength=" + minExtensionPrimerLength + ", minLigationPrimerLength=" + minLigationPrimerLength + ", minCaptureTargetLength="
					+ minCaptureTargetLength + "]";
		}

	}

	static class TrimmedRead {
		private final String trimmedReadString;
		private final String trimmedReadQuality;

		private TrimmedRead(String trimmedReadString, String trimmedReadQuality) {
			super();
			this.trimmedReadString = trimmedReadString;
			this.trimmedReadQuality = trimmedReadQuality;
		}

		public String getTrimmedReadString() {
			return trimmedReadString;
		}

		public String getTrimmedReadQuality() {
			return trimmedReadQuality;
		}
	}

	private static void trimReads(File inputFastqFile, File outputFastqFile, int firstBaseToKeep, int lastBaseToKeep, boolean performThreePrimeTrimming) {
		if (firstBaseToKeep < 0) {
			throw new IllegalArgumentException("First base to keep[" + firstBaseToKeep + "] must be greater than zero.");
		}

		if (lastBaseToKeep <= firstBaseToKeep) {
			throw new IllegalArgumentException("Last base to keep[" + lastBaseToKeep + "] must be greater than the first base to keep[" + firstBaseToKeep + "].");
		}

		if (outputFastqFile.exists()) {
			outputFastqFile.delete();
		}
		try {
			FileUtil.createNewFile(outputFastqFile);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create an output file at [" + outputFastqFile.getAbsolutePath() + "].", e);
		}

		int recordIndex = 0;
		FastqWriterFactory factory = new FastqWriterFactory();
		FastqWriter fastQWriter = factory.newWriter(outputFastqFile);
		try {
			try (FastqReader fastQReader = new FastqReader(inputFastqFile)) {
				while (fastQReader.hasNext()) {
					FastqRecord record = fastQReader.next();
					FastqRecord newRecord = trim(record, firstBaseToKeep, lastBaseToKeep, performThreePrimeTrimming, recordIndex);
					fastQWriter.write(newRecord);
					recordIndex++;
				}
			}
		} finally {
			fastQWriter.close();
		}
	}

	static FastqRecord trim(FastqRecord record, int firstBaseToKeep, int lastBaseToKeep, boolean performThreePrimeTrimming, int recordIndex) {
		String readName = record.getReadHeader();
		String readString = record.getReadString();
		String readQuality = record.getBaseQualityString();

		TrimmedRead trimmedRead = trim(readString, readQuality, firstBaseToKeep, lastBaseToKeep, performThreePrimeTrimming);

		FastqRecord newRecord = new FastqRecord(readName, trimmedRead.getTrimmedReadString(), record.getBaseQualityHeader(), trimmedRead.getTrimmedReadQuality());
		return newRecord;

	}

	static TrimmedRead trim(String readString, String readQuality, int firstBaseToKeep, int lastBaseToKeep, boolean performThreePrimeTrimming) {
		if (firstBaseToKeep >= readString.length()) {
			throw new IllegalArgumentException("Unable to trim " + firstBaseToKeep + " bases from the beginning of a sequence with length[" + readString.length() + "]");
		}

		if (readString.length() != readQuality.length()) {
			throw new IllegalStateException("The Read String[" + readString + "] and Read Quality[" + readQuality + "] are not of the same length, their lengths are [" + readString.length()
					+ "] and [" + readQuality.length() + "] respectively.");
		}

		int lastBase = readString.length() - 1;
		if (performThreePrimeTrimming) {
			lastBase = Math.min(lastBaseToKeep, readString.length() - 1);
		}

		String newReadString = readString.substring(firstBaseToKeep, lastBase + 1);
		String newReadQuality = readQuality.substring(firstBaseToKeep, lastBase + 1);

		return new TrimmedRead(newReadString, newReadQuality);
	}
}
