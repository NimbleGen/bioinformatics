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

	public static void trimReads(File inputFastqOneFile, File inputFastqTwoFile, File probeInfoFile, int extensionUidLength, int ligationUidLength, int extraReadOneBasesToTrim,
			int extraReadTwoBasesToTrim, File outputFastqOneFile, File outputFastqTwoFile) throws IOException {
		ProbesBySequenceName probes = ProbeFileUtil.parseProbeInfoFile(probeInfoFile);
		int maxExtensionPrimerLength = 0;
		int maxLigationPrimerLength = 0;
		int maxCaptureTargetLength = 0;
		for (Probe probe : probes) {
			int extensionPrimerLength = probe.getExtensionPrimerSequence().size();
			int ligationPrimerLength = probe.getLigationPrimerSequence().size();
			maxExtensionPrimerLength = Math.max(extensionPrimerLength, maxExtensionPrimerLength);
			maxLigationPrimerLength = Math.max(ligationPrimerLength, maxLigationPrimerLength);
			maxCaptureTargetLength = Math.max(probe.getCaptureTargetSequence().size(), maxCaptureTargetLength);
		}
		int readOneTrimFromStart = extensionUidLength + maxExtensionPrimerLength + extraReadOneBasesToTrim;
		int readTwoTrimFromStart = ligationUidLength + maxLigationPrimerLength + extraReadTwoBasesToTrim;

		trimReads(inputFastqOneFile, outputFastqOneFile, readOneTrimFromStart, maxCaptureTargetLength - (2 * extraReadOneBasesToTrim));
		trimReads(inputFastqTwoFile, outputFastqTwoFile, readTwoTrimFromStart, maxCaptureTargetLength - (2 * extraReadTwoBasesToTrim));
	}

	public static void trimReads(File inputFastqFile, File outputFastqFile, int basesToTrimFromBeginning, int maxNumberOfBasesToKeep) {
		if (basesToTrimFromBeginning < 0) {
			throw new IllegalArgumentException("Number of bases to trim from the beginning of the sequence[" + basesToTrimFromBeginning + "] must be greater than zero.");
		}

		if (maxNumberOfBasesToKeep < 0) {
			throw new IllegalArgumentException("Max number of bases to keep[" + maxNumberOfBasesToKeep + "] must be greater than zero.");
		}

		FastqWriterFactory factory = new FastqWriterFactory();
		FastqWriter fastQWriter = factory.newWriter(outputFastqFile);
		try (FastqReader fastQReader = new FastqReader(inputFastqFile)) {
			while (fastQReader.hasNext()) {
				FastqRecord record = fastQReader.next();
				String readString = record.getReadString();
				String readQuality = record.getBaseQualityString();

				if (basesToTrimFromBeginning >= readString.length()) {
					throw new IllegalArgumentException("Unable to trim " + basesToTrimFromBeginning + " bases from the beginning of a sequence with length[" + readString.length() + "]");
				}

				int startIndex = basesToTrimFromBeginning;
				int endIndex = startIndex + maxNumberOfBasesToKeep - 1;

				String newReadString = readString.substring(startIndex, endIndex);
				String newReadQuality = readQuality.substring(startIndex, endIndex);

				FastqRecord newRecord = new FastqRecord(record.getReadHeader(), newReadString, record.getBaseQualityHeader(), newReadQuality);

				fastQWriter.write(newRecord);
			}
		}
		fastQWriter.close();
	}

}
