package com.roche.heatseq.merged_read_process;

import htsjdk.samtools.fastq.FastqRecord;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.roche.heatseq.cli.CliStatusConsole;
import com.roche.heatseq.objects.ParsedProbeFile;
import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.utils.FastqReader;
import com.roche.heatseq.utils.IlluminaFastQReadNameUtil;
import com.roche.heatseq.utils.ProbeFileUtil;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
import com.roche.sequencing.bioinformatics.common.alignment.SimpleAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.mapping.SimpleMapper;
import com.roche.sequencing.bioinformatics.common.multithreading.IExceptionListener;
import com.roche.sequencing.bioinformatics.common.multithreading.PausableFixedThreadPoolExecutor;
import com.roche.sequencing.bioinformatics.common.sequence.ICode;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCode;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.StartAndStopIndex;
import com.roche.sequencing.bioinformatics.common.utils.IProgressListener;

public class ProbeAssigner {

	public final static String UID_DELIMITER = "%";
	private final static int numberOfThreads = 20;
	private final static DecimalFormat DF = new DecimalFormat("#,###");

	// this is used when selecting the best of the probe candidates
	// they must have an edit distance no greater than this number
	private final static int BEST_PROBE_ALIGNMENT_EDIT_DISTANCE_THRESHOLD = 15;

	private final static int SUGGESTED_NUMBER_OF_CANDIDATES_FOR_BEST_PROBE_ALIGNMENT = 5;

	private final static double DEFAULT_MIN_RATIO_OF_HITS_TO_AVAILABLE_HITS = 0.4;

	// these are used by the probe mapper
	private final static Integer COMPARISON_SEQUENCE_SIZE = 11;
	private final static int QUERY_SPACING = 1;
	private final static int REFERENCE_SPACING = 1;

	private static AtomicLong start = new AtomicLong();

	private ProbeAssigner() {
		throw new AssertionError();
	}

	public static Map<String, ProbeAssignment> getReadNameToProbeAssignmentMap(File mergedFastqFile, File probeFile, IProgressListener progressListener) {
		ParsedProbeFile parsedProbeFile;
		try {
			parsedProbeFile = ProbeFileUtil.parseProbeInfoFile(probeFile);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		return assignReadsToProbes(mergedFastqFile, parsedProbeFile, progressListener);
	}

	public static Map<String, ProbeAssignment> assignReadsToProbes(File mergedFastqFile, ParsedProbeFile parsedProbeFile, IProgressListener progressListener) {
		List<Probe> probes = parsedProbeFile.getProbes();

		Integer maxReferencesPerSequence = null;

		SimpleMapper<Probe> probeMapper = new SimpleMapper<Probe>(COMPARISON_SEQUENCE_SIZE, REFERENCE_SPACING, QUERY_SPACING, maxReferencesPerSequence);

		for (Probe probe : probes) {
			probeMapper.addReferenceSequence(probe.getProbeSequence(), probe);
		}

		int numberOfReadPairs = 0;
		try (FastqReader fastQReader = new FastqReader(mergedFastqFile)) {
			while (fastQReader.hasNext()) {
				fastQReader.next();
				numberOfReadPairs++;
			}
		}
		CliStatusConsole.logStatus("Total Reads:" + numberOfReadPairs);

		PausableFixedThreadPoolExecutor executor = new PausableFixedThreadPoolExecutor(numberOfThreads, "READ_PROCESSING_");
		executor.addExceptionListener(new IExceptionListener() {
			@Override
			public void exceptionOccurred(Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		});

		if (progressListener != null) {
			progressListener.updateProgress(0, "Starting to assign reads to probes");
		}

		AtomicInteger readsProcessedCount = new AtomicInteger(0);
		AtomicInteger lastPercentComplete = new AtomicInteger(0);
		start.set(System.currentTimeMillis());
		ReadToProbeAssigner[] readToProbeAssigners = new ReadToProbeAssigner[numberOfThreads];
		int readsPerThread = (int) Math.ceil((double) numberOfReadPairs / (double) numberOfThreads);
		for (int i = 0; i < numberOfThreads; i++) {
			int startingRead = i * readsPerThread;
			int endingRead = Math.min(numberOfReadPairs - 1, ((i + 1) * readsPerThread) - 1);
			ReadToProbeAssigner readToProbeAssigner = new ReadToProbeAssigner(mergedFastqFile, probeMapper, startingRead, endingRead, BEST_PROBE_ALIGNMENT_EDIT_DISTANCE_THRESHOLD,
					SUGGESTED_NUMBER_OF_CANDIDATES_FOR_BEST_PROBE_ALIGNMENT, readsProcessedCount, numberOfReadPairs, lastPercentComplete, progressListener);
			readToProbeAssigners[i] = readToProbeAssigner;
			executor.submit(readToProbeAssigner);
		}

		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Map<String, ProbeAssignment> classificationsFromReadClassifierByReadName = new LinkedHashMap<String, ProbeAssignment>();
		for (int i = 0; i < readToProbeAssigners.length; i++) {
			Map<String, ProbeAssignment> readAssignments = readToProbeAssigners[i].getProbeAssignmentsByReadName();
			if (readAssignments != null) {
				classificationsFromReadClassifierByReadName.putAll(readAssignments);
			} else {
				System.out.println("Null Read Assignment Error Avoided.");
			}
		}

		if (progressListener != null) {
			progressListener.updateProgress(100, "Done assigning reads to probes(" + DF.format(classificationsFromReadClassifierByReadName.size()) + " reads assigned).");
		}

		return classificationsFromReadClassifierByReadName;

	}

	private static class ReadToProbeAssigner implements Runnable {

		private final File fastqFile;

		private final SimpleMapper<Probe> probeMapper;

		private final int startingRead;
		private final int endingRead;

		private final int editDistanceThreshold;

		private final int suggestedNumberOfCandidatesForAlignment;

		private Map<String, ProbeAssignment> probeAssignmentsByReadName;

		private final AtomicInteger readsProcessedCount;

		private final int numberOfReadPairs;
		private final AtomicInteger lastPercentComplete;
		private final IProgressListener progressListener;

		public ReadToProbeAssigner(File fastqFile, SimpleMapper<Probe> probeMapper, int startingRead, int endingRead, int editDistanceThreshold, int suggestedNumberOfCandidatesForAlignment,
				AtomicInteger readsProcessedCount, int numberOfReadPairs, AtomicInteger lastPercentComplete, IProgressListener progressListener) {
			super();
			this.fastqFile = fastqFile;
			this.probeMapper = probeMapper;
			this.startingRead = startingRead;
			this.endingRead = endingRead;
			this.editDistanceThreshold = editDistanceThreshold;
			this.suggestedNumberOfCandidatesForAlignment = suggestedNumberOfCandidatesForAlignment;
			this.readsProcessedCount = readsProcessedCount;
			this.numberOfReadPairs = numberOfReadPairs;
			this.lastPercentComplete = lastPercentComplete;
			this.progressListener = progressListener;
		}

		public Map<String, ProbeAssignment> getProbeAssignmentsByReadName() {
			return probeAssignmentsByReadName;
		}

		@Override
		public void run() {
			try {
				probeAssignmentsByReadName = assignReadsToProbes(fastqFile, probeMapper, startingRead, endingRead, editDistanceThreshold, suggestedNumberOfCandidatesForAlignment, readsProcessedCount,
						numberOfReadPairs, lastPercentComplete, progressListener);
			} catch (Exception e) {
				e.printStackTrace();
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
	}

	private static Map<String, ProbeAssignment> assignReadsToProbes(File fastqFile, SimpleMapper<Probe> mapper, int startingRead, int endingRead, int editDistanceThreshold,
			int suggestedNumberOfCandidatesForAlignment, AtomicInteger readsProcessedCount, int numberOfReadPairs, AtomicInteger lastPercentComplete, IProgressListener progressListener)
			throws IOException {

		Map<String, ProbeAssignment> probeAssignmentsByReadName = new LinkedHashMap<String, ProbeAssignment>();

		int readNumber = 0;
		try (FastqReader fastQReader = new FastqReader(fastqFile)) {
			readLoop: while (fastQReader.hasNext()) {
				FastqRecord record = fastQReader.next();

				if (readNumber > endingRead) {
					break readLoop;
				}

				if (readNumber >= startingRead) {
					String readName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(record.getReadHeader());
					ProbeAssignment probeAssignment = classifyReadPair(record, mapper, startingRead, endingRead, editDistanceThreshold, suggestedNumberOfCandidatesForAlignment);
					probeAssignmentsByReadName.put(readName, probeAssignment);
					int readsProcessed = readsProcessedCount.getAndIncrement();

					if (progressListener != null) {
						int percentComplete = (int) Math.floor(((double) readsProcessed / (double) numberOfReadPairs) * 100);
						if (percentComplete > lastPercentComplete.getAndSet(percentComplete)) {
							progressListener.updateProgress(percentComplete, "" + DF.format(readsProcessed) + " of " + DF.format(numberOfReadPairs) + " reads examined.");
						}
					}

				}
				readNumber++;
			}

		}

		return probeAssignmentsByReadName;
	}

	private static ProbeAssignment classifyReadPair(FastqRecord record, SimpleMapper<Probe> probeMapper, int startingRead, int endingRead, int editDistanceThreshold,
			int suggestedNumberOfCandidatesForAlignment) {

		ProbeAssignment probeAssignment = null;

		String readAsString = record.getReadString();
		ISequence readSequence = new IupacNucleotideCodeSequence(readAsString);

		boolean readContainsN = readAsString.toLowerCase().contains("n");

		if (readContainsN) {
			probeAssignment = ProbeAssignment.createProbeAssignmentNotCompletedBecauseReadContainsN();
		} else {

			List<Probe> probeCandidates = probeMapper.getBestCandidateReferences(readSequence, suggestedNumberOfCandidatesForAlignment, DEFAULT_MIN_RATIO_OF_HITS_TO_AVAILABLE_HITS);

			Probe assignedProbe = null;
			StartAndStopIndex fivePrimePrimerStartAndStopIndex = null;

			StartAndStopIndex threePrimePrimerStartAndStopIndex = null;

			int lowestEditDistance = editDistanceThreshold;
			NeedlemanWunschGlobalAlignment bestAlignment = null;
			Probe bestProbe = null;

			for (Probe probeCandidate : probeCandidates) {
				NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(probeCandidate.getProbeSequence(), readSequence);
				if (alignment.getEditDistance() < lowestEditDistance) {
					bestAlignment = alignment;
					bestProbe = probeCandidate;
				}
			}

			String uid = null;

			if (bestProbe != null) {
				PrimerPositions primerPositions = findPrimerPositions(bestAlignment, bestProbe);
				fivePrimePrimerStartAndStopIndex = primerPositions.fivePrimerPrimerPosition;
				threePrimePrimerStartAndStopIndex = primerPositions.threePrimePrimerPosition;
				assignedProbe = bestProbe;

				String fivePrimeUid = readAsString.substring(0, fivePrimePrimerStartAndStopIndex.getStartIndex() + 1);
				String threePrimeUid = readAsString.substring(threePrimePrimerStartAndStopIndex.getStopIndex() + 1, readAsString.length());
				uid = fivePrimeUid + UID_DELIMITER + threePrimeUid;
			}

			probeAssignment = ProbeAssignment.create(assignedProbe, fivePrimePrimerStartAndStopIndex, threePrimePrimerStartAndStopIndex, uid);
		}

		return probeAssignment;
	}

	private static class PrimerPositions {
		private final StartAndStopIndex fivePrimerPrimerPosition;
		private final StartAndStopIndex threePrimePrimerPosition;

		public PrimerPositions(StartAndStopIndex fivePrimerPrimerPosition, StartAndStopIndex threePrimePrimerPosition) {
			super();
			this.fivePrimerPrimerPosition = fivePrimerPrimerPosition;
			this.threePrimePrimerPosition = threePrimePrimerPosition;
		}
	}

	private static PrimerPositions findPrimerPositions(NeedlemanWunschGlobalAlignment alignment, Probe probe) {
		ISequence probeAlignment = alignment.getAlignmentPair().getReferenceAlignment();
		ISequence readAlignment = alignment.getAlignmentPair().getQueryAlignment();

		int alignmentIndex = 0;
		int readIndex = 0;

		// walk through the readAlignment until no gaps are found
		while (probeAlignment.getCodeAt(alignmentIndex).matches(IupacNucleotideCode.GAP)) {
			alignmentIndex++;
			readIndex++;
		}

		int fivePrimePrimerStart = readIndex;
		int fivePrimePrimerSize = probe.getExtensionPrimerSequence().size();
		// read primer size number of non-gaps from the probe alignment
		int nonGapsFound = 0;
		while (nonGapsFound < fivePrimePrimerSize) {
			if (!probeAlignment.getCodeAt(alignmentIndex).equals(IupacNucleotideCode.GAP)) {
				nonGapsFound++;
			}
			if (!readAlignment.getCodeAt(alignmentIndex).equals(IupacNucleotideCode.GAP)) {
				readIndex++;
			}
			alignmentIndex++;
		}

		StartAndStopIndex fivePrimePrimerStartAndStopIndex = new StartAndStopIndex(fivePrimePrimerStart, readIndex - 1);

		int captureTargetSize = probe.getCaptureTargetSequence().size();
		nonGapsFound = 0;
		while (nonGapsFound < captureTargetSize) {
			if (!probeAlignment.getCodeAt(alignmentIndex).equals(IupacNucleotideCode.GAP)) {
				nonGapsFound++;
			}
			if (!readAlignment.getCodeAt(alignmentIndex).equals(IupacNucleotideCode.GAP)) {
				readIndex++;
			}
			alignmentIndex++;
		}

		int threePrimePrimerStart = readIndex;

		int threePrimePrimerSize = probe.getLigationPrimerSequence().size();
		nonGapsFound = 0;
		while (nonGapsFound < threePrimePrimerSize) {
			if (!probeAlignment.getCodeAt(alignmentIndex).equals(IupacNucleotideCode.GAP)) {
				nonGapsFound++;
			}
			if (!readAlignment.getCodeAt(alignmentIndex).equals(IupacNucleotideCode.GAP)) {
				readIndex++;
			}
			alignmentIndex++;
		}
		StartAndStopIndex threePrimePrimerStartAndStopIndex = new StartAndStopIndex(threePrimePrimerStart, readIndex - 1);

		return new PrimerPositions(fivePrimePrimerStartAndStopIndex, threePrimePrimerStartAndStopIndex);
	}

	private static final IAlignmentScorer scorer = new SimpleAlignmentScorer(SimpleAlignmentScorer.DEFAULT_MATCH_SCORE, SimpleAlignmentScorer.DEFAULT_MISMATCH_PENALTY, -2, -15, false, false);
	private static final double WEIGHTED_ALIGNMENT_SCORE_THRESHOLD = 9.0;

	public static StartAndStopIndex getLocationOfProvidedSequenceInRead(ISequence readSequence, ISequence primerSequence) {
		StartAndStopIndex primerStartAndStopLocation = null;

		NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(readSequence, primerSequence, scorer);
		ISequence readAlignment = alignment.getAlignmentPair().getReferenceAlignment();
		ISequence primerAlignment = alignment.getAlignmentPair().getQueryAlignment();

		ISequence primer = alignment.getAlignmentPair().getQueryAlignment();
		int primerIndex = 0;
		while (primer.getCodeAt(primerIndex).equals(IupacNucleotideCode.GAP)) {
			primerIndex++;
		}

		Integer primerEndIndexInRead = null;

		// walk backwards until we stop seeing gaps in the reference (aka read)
		ISequence reverseReadAlignment = readAlignment.getReverse();
		ISequence reversePrimerAlignment = primerAlignment.getReverse();
		boolean passedTrailingPrimerGaps = false;
		int i = 0;
		primerEndIndexInRead = 0;

		double weightedAlignmentScore = 0;
		int firstNonGapIndex = -1;

		while (i < reverseReadAlignment.size()) {
			ICode currentReadCode = reverseReadAlignment.getCodeAt(i);
			ICode currentPrimerCode = reversePrimerAlignment.getCodeAt(i);
			if (!passedTrailingPrimerGaps && !currentPrimerCode.matches(IupacNucleotideCode.GAP)) {
				passedTrailingPrimerGaps = true;
				firstNonGapIndex = i;
			}

			// start counting all bases in read when the initial primer gaps have been passed
			if (passedTrailingPrimerGaps && !currentReadCode.matches(IupacNucleotideCode.GAP)) {
				primerEndIndexInRead++;
				if (currentReadCode.matches(currentPrimerCode)) {
					double distanceFromFirstNonGap = i - firstNonGapIndex;
					double weightedScore = Math.pow(0.95, distanceFromFirstNonGap);
					weightedAlignmentScore += weightedScore;
				}
			}

			i++;
		}
		if (weightedAlignmentScore < WEIGHTED_ALIGNMENT_SCORE_THRESHOLD) {
			primerEndIndexInRead = null;
		}

		if (primerEndIndexInRead != null) {
			// indexes are zero based and counts are one based so subtract one
			primerEndIndexInRead -= 1;
			if (primerEndIndexInRead >= 0) {
				primerStartAndStopLocation = new StartAndStopIndex(primerIndex, primerEndIndexInRead);
			}
		}

		return primerStartAndStopLocation;
	}

	public static void main(String[] args) {
		File mergedFile = new File("D:\\kurts_space\\hsq_with_pete\\4\\merged.fastq");
		File probeFile = new File("D:\\kurts_space\\hsq_with_pete\\4\\150109_HG38_Filtered_Exome_HSQ_HX1_probe_info.txt");
		Map<String, ProbeAssignment> result = getReadNameToProbeAssignmentMap(mergedFile, probeFile, null);

		double total = result.size();

		int none = 0;
		int both = 0;
		int containsN = 0;

		for (Entry<String, ProbeAssignment> entry : result.entrySet()) {
			ProbeAssignment pa = entry.getValue();
			if (pa.isReadContainsN()) {
				containsN++;
			} else if (pa.getAssignedProbe() != null) {
				both++;
			} else {
				none++;
			}
		}

		DecimalFormat df = new DecimalFormat("###.##");
		System.out.println("none:" + none + " " + df.format((double) none / total));
		System.out.println("both:" + both + " " + df.format((double) both / total));
		System.out.println("containsN:" + containsN + " " + df.format((double) containsN / total));

	}

}
