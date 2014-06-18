package com.roche.heatseq.process;

import java.io.File;
import java.io.IOException;

import net.sf.picard.fastq.FastqReader;
import net.sf.picard.fastq.FastqRecord;
import net.sf.picard.fastq.FastqWriter;
import net.sf.picard.fastq.FastqWriterFactory;

import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.objects.ProbesBySequenceName;
import com.roche.heatseq.utils.ProbeFileUtil;

public class FastqReadTrimmer {

	public static void trimReads(File inputFastqOneFile, File inputFastqTwoFile, File probeInfoFile, int extensionUidLength, int ligationUidLength, File outputFastqOneFile, File outputFastqTwoFile)
			throws IOException {
		ProbesBySequenceName probes = ProbeFileUtil.parseProbeInfoFile(probeInfoFile);

		ProbeInfoStats probeInfoStats = collectStatsFromProbeInformation(probes);

		int readOneTrimFromStart = extensionUidLength + probeInfoStats.getMaxExtensionPrimerLength();
		int readTwoTrimFromStart = ligationUidLength + probeInfoStats.getMaxLigationPrimerLength();

		trimReads(inputFastqOneFile, outputFastqOneFile, readOneTrimFromStart, probeInfoStats.getMinCaptureTargetLength() + probeInfoStats.getMinLigationPrimerLength());
		trimReads(inputFastqTwoFile, outputFastqTwoFile, readTwoTrimFromStart, probeInfoStats.getMinCaptureTargetLength() + probeInfoStats.getMinExtensionPrimerLength());
	}

	static ProbeInfoStats collectStatsFromProbeInformation(ProbesBySequenceName probes) throws IOException {

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

	static class ProbeInfoStats {
		private final int maxExtensionPrimerLength;
		private final int maxLigationPrimerLength;
		private final int maxCaptureTargetLength;

		private final int minExtensionPrimerLength;
		private final int minLigationPrimerLength;
		private final int minCaptureTargetLength;

		public ProbeInfoStats(int maxExtensionPrimerLength, int maxLigationPrimerLength, int maxCaptureTargetLength, int minExtensionPrimerLength, int minLigationPrimerLength,
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
	}

	public static void trimReads(File inputFastqFile, File outputFastqFile, int firstBaseToKeep, int lastBaseToKeep) {
		if (firstBaseToKeep < 0) {
			throw new IllegalArgumentException("First base to keep[" + firstBaseToKeep + "] must be greater than zero.");
		}

		if (lastBaseToKeep <= firstBaseToKeep) {
			throw new IllegalArgumentException("Last base to keep[" + lastBaseToKeep + "] must be greater than the first base to keep[" + firstBaseToKeep + "].");
		}

		FastqWriterFactory factory = new FastqWriterFactory();
		FastqWriter fastQWriter = factory.newWriter(outputFastqFile);
		try (FastqReader fastQReader = new FastqReader(inputFastqFile)) {
			while (fastQReader.hasNext()) {
				FastqRecord record = fastQReader.next();
				FastqRecord newRecord = trim(record, firstBaseToKeep, lastBaseToKeep);
				fastQWriter.write(newRecord);
			}
		}
		fastQWriter.close();
	}

	static FastqRecord trim(FastqRecord record, int firstBaseToKeep, int lastBaseToKeep) {
		String readString = record.getReadString();
		String readQuality = record.getBaseQualityString();

		if (firstBaseToKeep >= readString.length()) {
			throw new IllegalArgumentException("Unable to trim " + firstBaseToKeep + " bases from the beginning of a sequence with length[" + readString.length() + "]");
		}

		String newReadString = readString.substring(firstBaseToKeep, lastBaseToKeep + 1);
		String newReadQuality = readQuality.substring(firstBaseToKeep, lastBaseToKeep + 1);

		FastqRecord newRecord = new FastqRecord(record.getReadHeader(), newReadString, record.getBaseQualityHeader(), newReadQuality);
		return newRecord;
	}
}
