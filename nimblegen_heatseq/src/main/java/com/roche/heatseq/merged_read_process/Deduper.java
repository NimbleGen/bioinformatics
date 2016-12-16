package com.roche.heatseq.merged_read_process;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.fastq.FastqRecord;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.roche.heatseq.objects.ParsedProbeFile;
import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.utils.BamFileUtil;
import com.roche.heatseq.utils.FastqReader;
import com.roche.heatseq.utils.FastqWriter;
import com.roche.heatseq.utils.IlluminaFastQReadNameUtil;
import com.roche.heatseq.utils.SAMRecordUtil;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
import com.roche.sequencing.bioinformatics.common.multithreading.IExceptionListener;
import com.roche.sequencing.bioinformatics.common.multithreading.PausableFixedThreadPoolExecutor;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.IProgressListener;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class Deduper {

	private final static DecimalFormat DF = new DecimalFormat("#,###");
	private final static int MAX_NUMBER_OF_MISMATCHES_FOR_MAPPING = 2;
	private final static int NUMBER_OF_THREADS = 10;
	private final static boolean INCLUDE_UNMAPPED_READS = false;

	public static void dedup(File mergedFastq, Map<String, ProbeAssignment> readNameToProbeAssignment, ParsedProbeFile parsedProbeFile, DedupApproachEnum dedupApproach, boolean trimPrimers,
			File outputUnassignableFastqFile, File outputUniqueFastqFile, File outputDuplicateFastqFile, File outputBam, String commandLineSignature, String programName, String programVersion,
			IProgressListener progressListener) {

		try {
			FileUtil.createNewFile(outputUnassignableFastqFile);
			FileUtil.createNewFile(outputUniqueFastqFile);
			FileUtil.createNewFile(outputDuplicateFastqFile);
			FileUtil.createNewFile(outputBam);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		Map<String, FastqRecord> readNameToRecord = new HashMap<String, FastqRecord>();
		try (FastqReader fastQReader = new FastqReader(mergedFastq)) {
			while (fastQReader.hasNext()) {
				FastqRecord record = fastQReader.next();
				String readName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(record.getReadHeader());
				readNameToRecord.put(readName, record);
			}
		}

		Map<String, List<String>> probeIdToReadNamesMap = new HashMap<String, List<String>>();
		for (Entry<String, ProbeAssignment> entry : readNameToProbeAssignment.entrySet()) {
			String readName = entry.getKey();
			ProbeAssignment probeAssignment = entry.getValue();
			Probe probe = probeAssignment.getAssignedProbe();
			if (probe != null) {
				List<String> readNames = probeIdToReadNamesMap.get(probe.getProbeId());
				if (readNames == null) {
					readNames = new ArrayList<String>();
					probeIdToReadNamesMap.put(probe.getProbeId(), readNames);
				}
				readNames.add(readName);
			}
		}

		SAMFileHeader samHeader = BamFileUtil.getHeader(parsedProbeFile, commandLineSignature, programName, programVersion, false);
		samHeader.setSortOrder(SortOrder.coordinate);
		SAMFileWriter samWriter = new SAMFileWriterFactory().makeBAMWriter(samHeader, false, outputBam, 9);

		try (FastqWriter uniqueFastqWriter = new FastqWriter(outputUniqueFastqFile)) {
			try (FastqWriter duplicateFastqWriter = new FastqWriter(outputDuplicateFastqFile)) {
				try (FastqWriter unassignableFastqWriter = new FastqWriter(outputUnassignableFastqFile)) {
					multithreadedDedup(probeIdToReadNamesMap, readNameToRecord, readNameToProbeAssignment, parsedProbeFile, dedupApproach, trimPrimers, unassignableFastqWriter, uniqueFastqWriter,
							duplicateFastqWriter, samWriter, progressListener);
				}
			}
		} finally {
			samWriter.close();
		}

	}

	private static void dedup(Map<String, List<String>> probeIdToReadNamesMap, Map<String, FastqRecord> readNameToRecord, Map<String, ProbeAssignment> readNameToProbeAssignment,
			ParsedProbeFile parsedProbeFile, DedupApproachEnum dedupApproach, boolean trimPrimers, FastqWriter unassignableFastqWriter, FastqWriter uniqueFastqWriter,
			FastqWriter duplicateFastqWriter, SAMFileWriter samWriter) {

		for (Probe probe : parsedProbeFile.getProbes()) {
			dedupOnProbe(probeIdToReadNamesMap, readNameToRecord, readNameToProbeAssignment, probe.getProbeId(), dedupApproach, trimPrimers, unassignableFastqWriter, uniqueFastqWriter,
					duplicateFastqWriter, samWriter);
		}

		String noProbeAssigned = null;
		dedupOnProbe(probeIdToReadNamesMap, readNameToRecord, readNameToProbeAssignment, noProbeAssigned, dedupApproach, trimPrimers, unassignableFastqWriter, uniqueFastqWriter, duplicateFastqWriter,
				samWriter);

	}

	private static SAMRecord createSamRecord(FastqRecord record, Probe probe, ProbeAssignment probeAssignment, SAMFileWriter samWriter, boolean trimPrimers, Boolean isUnique) {
		SAMRecord samRecord = new SAMRecord(samWriter.getFileHeader());
		samRecord.setReadName(record.getReadHeader());
		samRecord.setHeader(samWriter.getFileHeader());

		// the paired reads have been merged so it is not paired
		samRecord.setReadPairedFlag(false);

		boolean isReadMapped = (probe != null);
		if (isReadMapped) {

			String trimmedReadString = record.getReadString();
			String trimmedReadQuality = record.getBaseQualityString();
			if (trimPrimers) {
				int startInReadInclusive = probeAssignment.getFivePrimePrimerLocation().getStopIndex() + 1;
				int stopInReadExclusive = probeAssignment.getThreePrimePrimerLocation().getStartIndex();
				trimmedReadString = record.getReadString().substring(startInReadInclusive, stopInReadExclusive);
				trimmedReadQuality = record.getBaseQualityString().substring(startInReadInclusive, stopInReadExclusive);
			}

			ISequence sequenceToCompare;
			int alignmentStart;
			if (trimPrimers) {
				sequenceToCompare = probe.getCaptureTargetSequence();
				alignmentStart = probe.getCaptureTargetStart();
			} else {
				sequenceToCompare = probe.getProbeSequence();
				alignmentStart = probe.getExtensionPrimerStart();
			}

			if (probe.getProbeStrand() == Strand.REVERSE) {
				trimmedReadString = new IupacNucleotideCodeSequence(trimmedReadString).getReverseCompliment().toString();
				trimmedReadQuality = StringUtil.reverse(trimmedReadQuality);
				sequenceToCompare = sequenceToCompare.getReverseCompliment();
			}

			samRecord.setReadString(trimmedReadString);
			samRecord.setBaseQualityString(trimmedReadQuality);

			NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(sequenceToCompare, new IupacNucleotideCodeSequence(trimmedReadString));
			String cigarString = alignment.getCigarString().getStandardCigarString();

			int insertSize = 0;
			if (samRecord.getReadNegativeStrandFlag()) {
				insertSize = -alignment.getAlignmentPair().getFirstNonInsertQueryMatchInReference();
			} else {
				insertSize = alignment.getAlignmentPair().getFirstNonInsertQueryMatchInReference();
			}

			alignmentStart += insertSize;

			// trim beginning and ending inserts
			Pattern beginningInserts = Pattern.compile("^(\\d+)I");
			Matcher beginningInsertsMatcher = beginningInserts.matcher(cigarString);
			if (beginningInsertsMatcher.find()) {
				cigarString = cigarString.substring(beginningInsertsMatcher.group(0).length());
			}

			Pattern endingInserts = Pattern.compile("(\\d+)I$");
			Matcher endingInsertsMatcher = endingInserts.matcher(cigarString);
			if (endingInsertsMatcher.find()) {
				cigarString = cigarString.substring(0, cigarString.length() - endingInsertsMatcher.group(0).length());
			}
			isReadMapped = (cigarString.length() > 0) && (alignment.getNumberOfMismatches() <= MAX_NUMBER_OF_MISMATCHES_FOR_MAPPING);

			if (isReadMapped) {
				// TODO figure out a mapping quality score
				// per the sam file spec(https://samtools.github.io/hts-specs/SAMv1.pdf) a value of 255 means that the mapping quality is not available
				samRecord.setMappingQuality(255);

				samRecord.setReadUnmappedFlag(false);

				SAMRecordUtil.setSamRecordProbeIdAttribute(samRecord, probe.getProbeId());
				samRecord.setReadNegativeStrandFlag(probe.getProbeStrand() == Strand.REVERSE);

				String fivePrimeUid = "";
				String threePrimeUid = "";
				String uid = probeAssignment.getUid();
				String[] splitUid = uid.split(ProbeAssigner.UID_DELIMITER);
				if (splitUid.length == 2) {
					fivePrimeUid = splitUid[0];
					threePrimeUid = splitUid[1];
				} else if (uid.startsWith(ProbeAssigner.UID_DELIMITER) && splitUid.length == 1) {
					threePrimeUid = splitUid[0];
				} else if (uid.endsWith(ProbeAssigner.UID_DELIMITER) && splitUid.length == 1) {
					fivePrimeUid = splitUid[0];
				}

				SAMRecordUtil.setSamRecordLigationUidAttribute(samRecord, threePrimeUid);
				SAMRecordUtil.setSamRecordExtensionUidAttribute(samRecord, fivePrimeUid);

				String uidGroup = probeAssignment.getUidGroup();
				SAMRecordUtil.setSamRecordUidGroupAttribute(samRecord, uidGroup);

				samRecord.setDuplicateReadFlag(!isUnique);

				samRecord.setReferenceName(probe.getSequenceName());

				samRecord.setCigarString(cigarString);
				samRecord.setAlignmentStart(alignmentStart);
				if (samRecord.getReadNegativeStrandFlag()) {
					samRecord.setInferredInsertSize(-trimmedReadString.length());
				} else {
					samRecord.setInferredInsertSize(trimmedReadString.length());
				}
			}

		}

		// note: this is not in the else of the preceding if because if we can't align the read it is then classified as unmapped.
		if (!isReadMapped) {
			samRecord.setReadUnmappedFlag(true);
			samRecord.setReadString(record.getReadString());
			samRecord.setBaseQualityString(record.getBaseQualityString());

			// per the sam file spec(https://samtools.github.io/hts-specs/SAMv1.pdf) a value of 0 means that read in unmapped
			samRecord.setMappingQuality(0);
		}

		return samRecord;
	}

	public static FastqRecord createFastqRecord(FastqRecord record, ProbeAssignment probeAssignment, boolean trimPrimers) {
		String annotation = record.getBaseQualityHeader();
		annotation += " uid=" + probeAssignment.getUid() + " uidGroup=" + probeAssignment.getUidGroup() + " probeId=" + probeAssignment.getAssignedProbe().getProbeId();

		String trimmedReadString = record.getReadString();
		String trimmedReadQuality = record.getBaseQualityString();

		if (trimPrimers) {
			int startInReadInclusive = probeAssignment.getFivePrimePrimerLocation().getStopIndex() + 1;
			int stopInReadExclusive = probeAssignment.getThreePrimePrimerLocation().getStartIndex();
			trimmedReadString = record.getReadString().substring(startInReadInclusive, stopInReadExclusive);
			trimmedReadQuality = record.getBaseQualityString().substring(startInReadInclusive, stopInReadExclusive);
		}

		FastqRecord result = new FastqRecord(record.getReadHeader(), trimmedReadString, annotation, trimmedReadQuality);

		return result;
	}

	private static void dedupOnProbe(Map<String, List<String>> probeIdToReadNamesMap, Map<String, FastqRecord> readNameToRecord, Map<String, ProbeAssignment> readNameToProbeAssignment,
			String probeId, DedupApproachEnum dedupApproach, boolean trimPrimers, FastqWriter unassignableFastqWriter, FastqWriter uniqueFastqWriter, FastqWriter duplicateFastqWriter,
			SAMFileWriter samWriter) {

		if (probeId == null) {
			// this will be called once per run to handle the unmapped reads
			for (Entry<String, ProbeAssignment> entry : readNameToProbeAssignment.entrySet()) {
				String readName = entry.getKey();
				ProbeAssignment probeAssignment = entry.getValue();

				FastqRecord record = readNameToRecord.get(readName);

				if (!probeAssignment.isProbeAssigned()) {
					unassignableFastqWriter.write(record);
					if (INCLUDE_UNMAPPED_READS) {
						samWriter.addAlignment(createSamRecord(record, null, probeAssignment, samWriter, trimPrimers, null));
					}
				}
			}
		} else {
			// we have a probe id so process all reads associated with this probe id
			Map<String, List<FastqRecord>> probeRecordsByUidGroup = new HashMap<String, List<FastqRecord>>();
			List<String> readNames = probeIdToReadNamesMap.get(probeId);
			if (readNames != null) {
				for (String readName : readNames) {
					ProbeAssignment probeAssignment = readNameToProbeAssignment.get(readName);
					FastqRecord record = readNameToRecord.get(readName);

					String uidGroup = probeAssignment.getUidGroup();
					List<FastqRecord> uidGroupRecords = probeRecordsByUidGroup.get(uidGroup);
					if (uidGroupRecords == null) {
						uidGroupRecords = new ArrayList<FastqRecord>();
						probeRecordsByUidGroup.put(uidGroup, uidGroupRecords);
					}
					uidGroupRecords.add(record);
				}
			}

			for (List<FastqRecord> records : probeRecordsByUidGroup.values()) {
				if (dedupApproach == DedupApproachEnum.BEST_READ) {
					FastqRecord bestRecord = null;
					short bestQualityScore = -1;

					for (FastqRecord record : records) {
						// a higher quality score indicates a smaller probability of error (source: http://www.illumina.com/truseq/quality_101/quality_scores.ilmn)
						short qualityScore = BamFileUtil.getQualityScore(record.getBaseQualityString());

						if (qualityScore > bestQualityScore) {
							bestRecord = record;
							bestQualityScore = qualityScore;
						}
					}

					for (FastqRecord record : records) {
						String readName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(record.getReadHeader());
						ProbeAssignment probeAssignment = readNameToProbeAssignment.get(readName);
						Probe probe = probeAssignment.getAssignedProbe();

						boolean isUnique = record.equals(bestRecord);
						if (isUnique) {
							uniqueFastqWriter.write(createFastqRecord(record, probeAssignment, trimPrimers));
						} else {
							duplicateFastqWriter.write(createFastqRecord(record, probeAssignment, trimPrimers));
						}
						SAMRecord samRecord = createSamRecord(record, probe, probeAssignment, samWriter, trimPrimers, isUnique);
						synchronized (samWriter) {
							samWriter.addAlignment(samRecord);
						}
					}
				} else if (dedupApproach == DedupApproachEnum.CONSENSUS) {
					throw new IllegalStateException("The consensus dedup approach is not implemented yet.");
				} else {
					throw new AssertionError();
				}

			}
		}

	}

	private static void multithreadedDedup(Map<String, List<String>> probeIdToReadNamesMap, Map<String, FastqRecord> readNameToRecord, Map<String, ProbeAssignment> readNameToProbeAssignment,
			ParsedProbeFile parsedProbeFile, DedupApproachEnum dedupApproach, boolean trimPrimers, FastqWriter unassignableFastqWriter, FastqWriter uniqueFastqWriter,
			FastqWriter duplicateFastqWriter, SAMFileWriter samWriter, IProgressListener progressListener) {

		List<Throwable> exceptions = new ArrayList<Throwable>();

		PausableFixedThreadPoolExecutor executor = new PausableFixedThreadPoolExecutor(NUMBER_OF_THREADS, "DEDUP_");
		executor.addExceptionListener(new IExceptionListener() {
			@Override
			public void exceptionOccurred(Throwable throwable) {
				exceptions.add(throwable);
			}
		});

		AtomicInteger completedCount = new AtomicInteger(0);
		// +1 because we all look at the case where no probe is assigned
		int totalProbes = parsedProbeFile.getProbes().size() + 1;

		for (Probe probe : parsedProbeFile.getProbes()) {
			DedupOnProbeRunnable runnable = new DedupOnProbeRunnable(probeIdToReadNamesMap, readNameToRecord, readNameToProbeAssignment, probe.getProbeId(), dedupApproach, trimPrimers,
					unassignableFastqWriter, uniqueFastqWriter, duplicateFastqWriter, samWriter, progressListener, completedCount, totalProbes);
			executor.submit(runnable);
		}

		String noProbeAssigned = null;
		DedupOnProbeRunnable runnable = new DedupOnProbeRunnable(probeIdToReadNamesMap, readNameToRecord, readNameToProbeAssignment, noProbeAssigned, dedupApproach, trimPrimers,
				unassignableFastqWriter, uniqueFastqWriter, duplicateFastqWriter, samWriter, progressListener, completedCount, totalProbes);
		executor.submit(runnable);

		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (exceptions.size() > 0) {
			throw new RuntimeException(exceptions.get(0));
		}
	}

	private static class DedupOnProbeRunnable implements Runnable {

		private final Map<String, List<String>> probeIdToReadNamesMap;
		private final Map<String, FastqRecord> readNameToRecord;
		private final Map<String, ProbeAssignment> readNameToProbeAssignment;
		private final String probeId;
		private final DedupApproachEnum dedupApproach;
		private final boolean trimPrimers;
		private final FastqWriter unassignableFastqWriter;
		private final FastqWriter uniqueFastqWriter;
		private final FastqWriter duplicateFastqWriter;
		private final SAMFileWriter samWriter;
		private final IProgressListener progressListener;
		private final AtomicInteger completedCount;
		private final int totalProbes;

		public DedupOnProbeRunnable(Map<String, List<String>> probeIdToReadNamesMap, Map<String, FastqRecord> readNameToRecord, Map<String, ProbeAssignment> readNameToProbeAssignment, String probeId,
				DedupApproachEnum dedupApproach, boolean trimPrimers, FastqWriter unassignableFastqWriter, FastqWriter uniqueFastqWriter, FastqWriter duplicateFastqWriter, SAMFileWriter samWriter,
				IProgressListener progressListener, AtomicInteger completedCount, int totalProbes) {
			super();
			this.probeIdToReadNamesMap = probeIdToReadNamesMap;
			this.readNameToRecord = readNameToRecord;
			this.readNameToProbeAssignment = readNameToProbeAssignment;
			this.probeId = probeId;
			this.dedupApproach = dedupApproach;
			this.trimPrimers = trimPrimers;
			this.unassignableFastqWriter = unassignableFastqWriter;
			this.uniqueFastqWriter = uniqueFastqWriter;
			this.duplicateFastqWriter = duplicateFastqWriter;
			this.samWriter = samWriter;
			this.progressListener = progressListener;
			this.completedCount = completedCount;
			this.totalProbes = totalProbes;
		}

		@Override
		public void run() {
			dedupOnProbe(probeIdToReadNamesMap, readNameToRecord, readNameToProbeAssignment, probeId, dedupApproach, trimPrimers, unassignableFastqWriter, uniqueFastqWriter, duplicateFastqWriter,
					samWriter);
			int probesCompleted = completedCount.incrementAndGet();
			if (progressListener != null) {
				int percentComplete = (int) Math.floor(((double) probesCompleted / totalProbes) * 100);
				progressListener.updateProgress(percentComplete, "Done deduping the reads for " + DF.format(probesCompleted) + " of " + DF.format(totalProbes) + " probes.");
			}
		}

	}

}
