package com.roche.sandbox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import net.sf.picard.fastq.FastqReader;
import net.sf.picard.fastq.FastqRecord;
import net.sf.picard.fastq.FastqWriter;
import net.sf.picard.fastq.FastqWriterFactory;

import org.apache.commons.io.FileUtils;

import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.objects.ProbesBySequenceName;
import com.roche.heatseq.process.ProbeFileUtil;
import com.roche.heatseq.process.TabDelimitedFileWriter;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
import com.roche.sequencing.bioinformatics.common.alignment.SimpleAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOption;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOptionsGroup;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineParser;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.mapping.SimpleMapper;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.sequence.ICode;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCode;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.ManifestUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class KensAligner {

	private final static String APPLICATION_NAME = "KEN`S ALIGNMENT TOOL";
	private static String applicationVersionFromManifest;

	private final static int DEFAULT_NUMBER_OF_TOP_MAPPED_PROBES_TO_ALIGN = 20;

	private static double matchScore = 5;
	private static double mismatchScore = 0;
	private static double gapOpenScore = -0.1;
	private static double gapExtendScore = -0.02;

	private final static CommandLineOption USAGE_OPTION = new CommandLineOption("Print Usage", "usage", 'h', "Print Usage.", false, true);
	private final static CommandLineOption FASTQ_ONE_OPTION = new CommandLineOption("fastQ File", "r1", null, "path to input fastq file", true, false);
	private final static CommandLineOption PROBE_OPTION = new CommandLineOption("PROBE File", "probe", null, "The probe file", true, false);
	private final static CommandLineOption OUTPUT_DIR_OPTION = new CommandLineOption("Output Directory", "outputDir", null, "location to store resultant files.", false, false);
	private final static CommandLineOption OUTPUT_TRACEABILITY_MATRIX_OPTION = new CommandLineOption("Output Traceability Matrix", "trace", null,
			"flag for outputting traceability matrix for the best alignments", false, true);
	private final static CommandLineOption NUMBER_OF_TOP_PROBE_MATCHES_TO_ALIGN_OPTION = new CommandLineOption("Max Candidate Probes For Alignment", "maxCandidateProbes", null,
			"The max Number of top candidate probes to align in order to find best alignment (default:" + DEFAULT_NUMBER_OF_TOP_MAPPED_PROBES_TO_ALIGN
					+ ").  A smaller number relative to the number of total probes will run faster but may miss optimal alignments.", false, false);
	private final static CommandLineOption MAX_NUMBER_OF_RECORDS_TO_READ_OPTION = new CommandLineOption("Max Number of Records to Read", "maxReads", null,
			"Maximum number of records to read from the " + FASTQ_ONE_OPTION.getOptionName() + " option (default:" + Long.MAX_VALUE + ").", false, false);
	private final static CommandLineOption MIN_ALIGNMENT_SCORE_THRESHOLD_OPTION = new CommandLineOption("Min Alignment Threshold Score", "minAlignmentScore", null,
			"Minimum Value Needed From the Alignment Score to be considered for succesful alignment. (default: " + Integer.MIN_VALUE + ")", false, false);
	private final static CommandLineOption MATCH_SCORE_OPTION = new CommandLineOption("Match Score", "matchScore", null,
			"The score given to matching nucleotides when extending alignments to the primers (Default: " + matchScore + ")", false, false);
	private final static CommandLineOption MISMATCH_SCORE_OPTION = new CommandLineOption("Mismatch Score", "mismatchScore", null,
			"The score for mismatched nucleotides when extending alignments to the primers (Default: " + mismatchScore + ")", false, false);
	private final static CommandLineOption GAP_OPEN_SCORE_OPTION = new CommandLineOption("Gap Open Score", "gapOpenScore", null,
			"The score for opening a gap when extending alignments to the primers (Default: " + gapOpenScore + ")", false, false);
	private final static CommandLineOption GAP_EXTEND_SCORE_OPTION = new CommandLineOption("Gap Extend Score", "gapExtendScore", null,
			"The score for extending a gap when extending alignments to the primers (Default: " + gapExtendScore + ")", false, false);

	public static void main(String[] args) throws IOException {
		// runApplicationProgrammatically();
		runApplicationViaCommandLine(args);
	}

	private static void runApplicationViaCommandLine(String[] args) throws IOException {

		String version = ManifestUtil.getManifestValue("version");
		if (version != null) {
			applicationVersionFromManifest = version;
		}

		ParsedCommandLine parsedCommandLine = CommandLineParser.parseCommandLine(args, getCommandLineOptionsGroup());
		boolean noOptionsProvided = (args.length == 0);
		boolean showUsage = parsedCommandLine.isOptionPresent(USAGE_OPTION) || noOptionsProvided;

		if (showUsage) {
			System.out.println(parsedCommandLine.getCommandLineOptionsGroup().getUsage());
		} else {
			CommandLineParser.throwCommandLineParsingExceptions(parsedCommandLine);

			IAlignmentScorer alignmentScorer = getAlignmentScorer(parsedCommandLine);

			File fastQ1WithUidsFile = new File(parsedCommandLine.getOptionsValue(FASTQ_ONE_OPTION));

			if (!fastQ1WithUidsFile.exists()) {
				throw new IllegalStateException("Unable to find provided FASTQ1 file[" + fastQ1WithUidsFile.getAbsolutePath() + "].");
			}

			File probeFile = new File(parsedCommandLine.getOptionsValue(PROBE_OPTION));

			if (!probeFile.exists()) {
				throw new IllegalStateException("Unable to find provided PROBE file[" + probeFile.getAbsolutePath() + "].");
			}

			String outputDirectoryString = parsedCommandLine.getOptionsValue(OUTPUT_DIR_OPTION);
			File outputDirectory = null;
			if (outputDirectoryString != null) {
				outputDirectory = new File(outputDirectoryString);
				if (!outputDirectory.exists()) {
					try {
						FileUtils.forceMkdir(outputDirectory);
					} catch (IOException e) {
						throw new IllegalStateException("Could not create provided output directory[" + outputDirectory.getAbsolutePath() + "].", e);
					}
				}
				if (!outputDirectory.isDirectory()) {
					throw new IllegalStateException("Provided output directory[" + outputDirectory.getAbsolutePath() + "] is not valid.");
				}
			} else {
				// current working directory
				outputDirectory = new File(".");
			}

			int minAlignmentScoreThreshold = Integer.MIN_VALUE;
			if (parsedCommandLine.isOptionPresent(MIN_ALIGNMENT_SCORE_THRESHOLD_OPTION)) {
				try {
					minAlignmentScoreThreshold = Integer.parseInt(parsedCommandLine.getOptionsValue(MIN_ALIGNMENT_SCORE_THRESHOLD_OPTION));
				} catch (NumberFormatException ex) {
					throw new IllegalStateException("Value specified for min alignment threshold score is not an integer[" + parsedCommandLine.getOptionsValue(MIN_ALIGNMENT_SCORE_THRESHOLD_OPTION)
							+ "].");
				}
			}

			long maxNumberOfRecordsToRead = Long.MAX_VALUE;
			if (parsedCommandLine.isOptionPresent(MAX_NUMBER_OF_RECORDS_TO_READ_OPTION)) {
				try {
					maxNumberOfRecordsToRead = Long.parseLong(parsedCommandLine.getOptionsValue(MAX_NUMBER_OF_RECORDS_TO_READ_OPTION));
				} catch (NumberFormatException ex) {
					throw new IllegalStateException("Value specified for " + MAX_NUMBER_OF_RECORDS_TO_READ_OPTION.getOptionName() + " is not a long["
							+ parsedCommandLine.getOptionsValue(MAX_NUMBER_OF_RECORDS_TO_READ_OPTION) + "].");
				}
			}

			int numberOfTopMatchedProbesToAlign = DEFAULT_NUMBER_OF_TOP_MAPPED_PROBES_TO_ALIGN;
			if (parsedCommandLine.isOptionPresent(NUMBER_OF_TOP_PROBE_MATCHES_TO_ALIGN_OPTION)) {
				try {
					numberOfTopMatchedProbesToAlign = Integer.parseInt(parsedCommandLine.getOptionsValue(NUMBER_OF_TOP_PROBE_MATCHES_TO_ALIGN_OPTION));
				} catch (NumberFormatException ex) {
					throw new IllegalStateException("Value specified for " + NUMBER_OF_TOP_PROBE_MATCHES_TO_ALIGN_OPTION.getOptionName() + " is not an int["
							+ parsedCommandLine.getOptionsValue(NUMBER_OF_TOP_PROBE_MATCHES_TO_ALIGN_OPTION) + "].");
				}
			}

			boolean outputTraceabilityMatrix = parsedCommandLine.isOptionPresent(OUTPUT_TRACEABILITY_MATRIX_OPTION);

			alignReadsToProbes(probeFile, fastQ1WithUidsFile, outputDirectory, alignmentScorer, minAlignmentScoreThreshold, maxNumberOfRecordsToRead, numberOfTopMatchedProbesToAlign,
					outputTraceabilityMatrix);

		}

	}

	@SuppressWarnings("unused")
	private static void runApplicationProgrammatically() {
		File probeInfoFile = new File("D:/ken/180mer_cancer_tiling.uw.96_probe_ATM.RNGformat.txt");
		File fastqReadFile = new File("D:/ken/Des-RTR-250_S3_L001_R1_001.fastq");
		File outputDirectory = new File("D:/ken/results/");
		try {
			alignReadsToProbes(probeInfoFile, fastqReadFile, outputDirectory, null, Integer.MIN_VALUE, Long.MAX_VALUE, DEFAULT_NUMBER_OF_TOP_MAPPED_PROBES_TO_ALIGN, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class BestProbeMatch {
		private NeedlemanWunschGlobalAlignment bestExtensionAlignment;
		private NeedlemanWunschGlobalAlignment bestLigationAlignment;
		private Probe bestProbe;
		private final ISequence readSequence;
		private final String readName;

		public BestProbeMatch(String readName, ISequence readSequence) {
			super();
			this.readName = readName;
			this.readSequence = readSequence;
			this.bestExtensionAlignment = null;
			this.bestLigationAlignment = null;
			this.bestProbe = null;
		}

		public NeedlemanWunschGlobalAlignment getBestExtensionAlignment() {
			return bestExtensionAlignment;
		}

		public void setBestExtensionAlignment(NeedlemanWunschGlobalAlignment bestExtensionAlignment) {
			this.bestExtensionAlignment = bestExtensionAlignment;
		}

		public NeedlemanWunschGlobalAlignment getBestLigationAlignment() {
			return bestLigationAlignment;
		}

		public void setBestLigationAlignment(NeedlemanWunschGlobalAlignment bestLigationAlignment) {
			this.bestLigationAlignment = bestLigationAlignment;
		}

		public Probe getBestProbe() {
			return bestProbe;
		}

		public void setBestProbe(Probe bestProbe) {
			this.bestProbe = bestProbe;
		}

		public boolean bestProbeExists() {
			return this.bestProbe != null;
		}

		public ISequence getReadSequence() {
			return readSequence;
		}

		public String getReadName() {
			return readName;
		}
	}

	public static BestProbeMatch getBestProbeMatch(String readName, ISequence readSequence, SimpleMapper<Probe> extensionMapper, SimpleMapper<Probe> ligationMapper, int minAlignmentScoreThreshold,
			int numberOfTopMatchedProbesToAlign, IAlignmentScorer alignmentScorer) {

		BestProbeMatch bestProbeMatch = new BestProbeMatch(readName, readSequence);

		double highestAlignmentScoreSum = minAlignmentScoreThreshold;

		TallyMap<Probe> matchingExtensionProbeCounts = extensionMapper.getReferenceTallyMap(readSequence);
		TallyMap<Probe> matchingLigationProbeCounts = ligationMapper.getReferenceTallyMap(readSequence);

		matchingExtensionProbeCounts.addAll(matchingLigationProbeCounts);
		TallyMap<Probe> matchingCombinedProbeCounts = matchingExtensionProbeCounts;

		List<Entry<Probe, Integer>> bestProbes = matchingCombinedProbeCounts.getObjectsSortedFromMostTalliesToLeast();

		for (int bestProbesIndex = 0; bestProbesIndex < Math.min(numberOfTopMatchedProbesToAlign, bestProbes.size()); bestProbesIndex++) {
			Probe probe = bestProbes.get(bestProbesIndex).getKey();
			ISequence extensionPrimer = probe.getExtensionPrimerSequence();
			ISequence ligationPrimer = probe.getLigationPrimerSequence();

			NeedlemanWunschGlobalAlignment extensionAlignment = new NeedlemanWunschGlobalAlignment(readSequence, extensionPrimer, alignmentScorer);
			NeedlemanWunschGlobalAlignment ligationAlignment = new NeedlemanWunschGlobalAlignment(readSequence, ligationPrimer, alignmentScorer);

			double alignmentScoreSum = extensionAlignment.getAlignmentScore() + ligationAlignment.getAlignmentScore();
			if (alignmentScoreSum > highestAlignmentScoreSum) {
				bestProbeMatch.setBestExtensionAlignment(extensionAlignment);
				bestProbeMatch.setBestLigationAlignment(ligationAlignment);

				highestAlignmentScoreSum = alignmentScoreSum;
				bestProbeMatch.setBestProbe(probe);
			}
		}

		return bestProbeMatch;

	}

	public static void alignReadsToProbes(File probeInfoFile, File fastqReadFile, File outputDirectory, IAlignmentScorer alignmentScorer, int minAlignmentScoreThreshold,
			long maxNumberOfRecordsToRead, int numberOfTopMatchedProbesToAlign, boolean outputTraceabilityMatrix) throws IOException {

		long start = System.currentTimeMillis();

		if (alignmentScorer == null) {
			alignmentScorer = new KensScorer(matchScore, mismatchScore, gapExtendScore, gapOpenScore, false);
		}

		File assignedReadsFile = new File(outputDirectory, "assigned_reads.tsv");
		FileUtil.createNewFile(assignedReadsFile);

		File alignmentFile = new File(outputDirectory, "read_alignments.txt");
		FileUtil.createNewFile(alignmentFile);
		BufferedWriter alignmentFileWriter = new BufferedWriter(new FileWriter(alignmentFile));

		BufferedWriter traceabilityMatrixFileWriter = null;
		if (outputTraceabilityMatrix) {
			File traceabilityMatrixFile = new File(outputDirectory, "read_traceability_matrices.txt");
			FileUtil.createNewFile(alignmentFile);
			traceabilityMatrixFileWriter = new BufferedWriter(new FileWriter(traceabilityMatrixFile));
		}

		File unassignedReadsFastqFile = new File(outputDirectory, "unassigned_reads.fastq");
		FileUtil.createNewFile(unassignedReadsFastqFile);
		FastqWriterFactory fastqWriterFactory = new FastqWriterFactory();
		FastqWriter unassignedReadsFastqFileWriter = fastqWriterFactory.newWriter(unassignedReadsFastqFile);

		int currentReadIndex = 0;

		ProbesBySequenceName probes = ProbeFileUtil.parseProbeInfoFile(probeInfoFile);

		long mapIndexingStart = System.currentTimeMillis();
		SimpleMapper<Probe> extensionMapper = new SimpleMapper<Probe>(3, 3, 1, 2);
		SimpleMapper<Probe> ligationMapper = new SimpleMapper<Probe>(3, 3, 1, 2);
		for (Probe probe : probes.getProbes()) {
			extensionMapper.addReferenceSequence(probe.getExtensionPrimerSequence(), probe);
			ligationMapper.addReferenceSequence(probe.getLigationPrimerSequence(), probe);
		}
		long mapIndexingStop = System.currentTimeMillis();
		System.out.println("Probe File[" + probeInfoFile.getAbsolutePath() + "] Indexed in " + DateUtil.convertMillisecondsToHHMMSS(mapIndexingStop - mapIndexingStart) + "(HH:MM:SS).");

		String[] assignedReadsHeader = new String[] { "read_name", "probe_id", "total_alignment_score", "uid", "expected_extension_sequence", "actual_extension_sequence", "extension_cigar_string",
				"extension_alignment_score", "extension_edit_distance", "expected_ligation_sequence", "actual_ligation_sequence", "ligation_cigar_string", "ligation_alignment_score",
				"ligation_edit_distance" };
		try (TabDelimitedFileWriter assignedReadsWriter = new TabDelimitedFileWriter(assignedReadsFile, assignedReadsHeader)) {
			try (FastqReader reader = new FastqReader(fastqReadFile)) {
				while (reader.hasNext() && currentReadIndex < maxNumberOfRecordsToRead) {
					currentReadIndex++;

					FastqRecord record = reader.next();

					ISequence readSequence = new IupacNucleotideCodeSequence(record.getReadString());
					String readName = record.getReadHeader();

					ISequence reverseComplimentReadSequence = readSequence.getReverseCompliment();
					String reverseComplimentReadName = "reverse_compliment:" + readName;

					BestProbeMatch bestProbeMatch = getBestProbeMatch(readName, readSequence, extensionMapper, ligationMapper, minAlignmentScoreThreshold, numberOfTopMatchedProbesToAlign,
							alignmentScorer);
					BestProbeMatch reverseComplimentBestProbeMatch = getBestProbeMatch(reverseComplimentReadName, reverseComplimentReadSequence, extensionMapper, ligationMapper,
							minAlignmentScoreThreshold, numberOfTopMatchedProbesToAlign, alignmentScorer);

					// if (reverseComplimentBestProbeMatch != null
					// && reverseComplimentBestProbeMatch.getBestExtensionLigationAlignmentScoreSum() > bestProbeMatch.getBestExtensionLigationAlignmentScoreSum()) {
					// bestProbeMatch = reverseComplimentBestProbeMatch;
					// }

					// print out reports
					outputResults(record, reverseComplimentBestProbeMatch, alignmentFileWriter, traceabilityMatrixFileWriter, assignedReadsWriter, unassignedReadsFastqFileWriter);
					outputResults(record, bestProbeMatch, alignmentFileWriter, traceabilityMatrixFileWriter, assignedReadsWriter, unassignedReadsFastqFileWriter);
				}
			}
		}

		unassignedReadsFastqFileWriter.close();
		alignmentFileWriter.close();
		if (traceabilityMatrixFileWriter != null) {
			traceabilityMatrixFileWriter.close();
		}

		long end = System.currentTimeMillis();
		System.out.println(APPLICATION_NAME + " (version:" + applicationVersionFromManifest + ") completed running in " + DateUtil.convertMillisecondsToHHMMSS(end - start) + "(HH:MM:SS). "
				+ currentReadIndex + " records processed.");
	}

	private static void outputResults(FastqRecord record, BestProbeMatch bestProbeMatch, BufferedWriter alignmentFileWriter, BufferedWriter traceabilityMatrixFileWriter,
			TabDelimitedFileWriter assignedReadsWriter, FastqWriter unassignedReadsFastqFileWriter) throws IOException {
		if (bestProbeMatch.bestProbeExists()) {
			NeedlemanWunschGlobalAlignment bestExtensionAlignment = bestProbeMatch.getBestExtensionAlignment();
			NeedlemanWunschGlobalAlignment bestLigationAlignment = bestProbeMatch.getBestLigationAlignment();
			Probe bestProbe = bestProbeMatch.getBestProbe();
			String readName = bestProbeMatch.getReadName();
			ISequence readSequence = bestProbeMatch.getReadSequence();

			ISequence uid = readSequence.subSequence(0, bestExtensionAlignment.getIndexOfFirstMatchInReference() - 1);

			String actualExtensionPrimer = bestExtensionAlignment.getAlignmentPair().getReferenceAlignmentWithoutEndingAndBeginningInserts().toString();
			String expectedExtensionPrimer = bestProbe.getExtensionPrimerSequence().toString();
			String actualLigationPrimer = bestLigationAlignment.getAlignmentPair().getReferenceAlignmentWithoutEndingAndBeginningInserts().toString();
			String expectedLigationPrimer = bestProbe.getLigationPrimerSequence().toString();

			alignmentFileWriter.write("_________________________________________" + StringUtil.NEWLINE);
			alignmentFileWriter.write("Read Name:" + readName + StringUtil.NEWLINE);
			alignmentFileWriter.write("Probe:" + bestProbe.getProbeId() + StringUtil.NEWLINE);
			alignmentFileWriter.write("Extension Edit Distance:" + bestExtensionAlignment.getEditDistance() + StringUtil.NEWLINE);
			alignmentFileWriter.write("Extension Alignment:" + StringUtil.NEWLINE);
			alignmentFileWriter.write(bestExtensionAlignment.getAlignmentPair().getReferenceAlignment().toString() + StringUtil.NEWLINE);
			alignmentFileWriter.write(StringUtil.repeatString(" ", bestExtensionAlignment.getIndexOfFirstMatchInReference()) + bestExtensionAlignment.getCigarString().getCigarString(false, true)
					+ StringUtil.NEWLINE);
			alignmentFileWriter.write(bestExtensionAlignment.getAlignmentPair().getQueryAlignment().toString() + StringUtil.NEWLINE);
			alignmentFileWriter.write(StringUtil.NEWLINE);
			alignmentFileWriter.write("Ligation Edit Distance:" + bestLigationAlignment.getEditDistance() + StringUtil.NEWLINE);
			alignmentFileWriter.write("Ligation Alignment:" + StringUtil.NEWLINE);
			alignmentFileWriter.write(bestLigationAlignment.getAlignmentPair().getReferenceAlignment().toString() + StringUtil.NEWLINE);
			alignmentFileWriter.write(StringUtil.repeatString(" ", bestLigationAlignment.getIndexOfFirstMatchInReference()) + bestLigationAlignment.getCigarString().getCigarString(false, true)
					+ StringUtil.NEWLINE);
			alignmentFileWriter.write(bestLigationAlignment.getAlignmentPair().getQueryAlignment().toString() + StringUtil.NEWLINE);
			alignmentFileWriter.write("_________________________________________" + StringUtil.NEWLINE);

			if (traceabilityMatrixFileWriter != null) {
				traceabilityMatrixFileWriter.write("_________________________________________" + StringUtil.NEWLINE);
				traceabilityMatrixFileWriter.write("Read Name:" + readName + StringUtil.NEWLINE);
				traceabilityMatrixFileWriter.write("Probe:" + bestProbe.getProbeId() + StringUtil.NEWLINE);
				traceabilityMatrixFileWriter.write("Extension Traceability Matrix:" + StringUtil.NEWLINE);
				traceabilityMatrixFileWriter.write(bestExtensionAlignment.getTraceabilityMatrixAsString() + StringUtil.NEWLINE);
				traceabilityMatrixFileWriter.write("Ligation Traceability Matrix:" + StringUtil.NEWLINE);
				traceabilityMatrixFileWriter.write(bestLigationAlignment.getTraceabilityMatrixAsString() + StringUtil.NEWLINE);
			}

			assignedReadsWriter.writeLine(readName, bestProbe.getProbeId(), bestExtensionAlignment.getAlignmentScore() + bestLigationAlignment.getAlignmentScore(), uid, expectedExtensionPrimer,
					actualExtensionPrimer, bestExtensionAlignment.getCigarString().getCigarString(false, true), bestExtensionAlignment.getAlignmentScore(), bestExtensionAlignment.getEditDistance(),
					expectedLigationPrimer, actualLigationPrimer, bestLigationAlignment.getCigarString().getCigarString(false, true), bestLigationAlignment.getAlignmentScore(),
					bestLigationAlignment.getEditDistance());
		} else {
			// TODO If a threshold is added to edit distance this may output values
			unassignedReadsFastqFileWriter.write(record);
		}

	}

	private static IAlignmentScorer getAlignmentScorer(ParsedCommandLine parsedCommandLine) {
		// Set up our alignment scorer

		if (parsedCommandLine.isOptionPresent(MATCH_SCORE_OPTION)) {
			try {
				matchScore = Double.parseDouble(parsedCommandLine.getOptionsValue(MATCH_SCORE_OPTION));
			} catch (NumberFormatException ex) {
				throw new IllegalStateException("Value specified for match score is not an integer[" + parsedCommandLine.getOptionsValue(MATCH_SCORE_OPTION) + "].");
			}
		}
		if (parsedCommandLine.isOptionPresent(MISMATCH_SCORE_OPTION)) {
			try {
				mismatchScore = Double.parseDouble(parsedCommandLine.getOptionsValue(MISMATCH_SCORE_OPTION));
			} catch (NumberFormatException ex) {
				throw new IllegalStateException("Value specified for mismatch penalty is not a double[" + parsedCommandLine.getOptionsValue(MISMATCH_SCORE_OPTION) + "].");
			}
		}
		if (parsedCommandLine.isOptionPresent(GAP_OPEN_SCORE_OPTION)) {
			try {
				gapOpenScore = Double.parseDouble(parsedCommandLine.getOptionsValue(GAP_OPEN_SCORE_OPTION));
			} catch (NumberFormatException ex) {
				throw new IllegalStateException("Value specified for gap open penalty is not a double[" + parsedCommandLine.getOptionsValue(GAP_OPEN_SCORE_OPTION) + "].");
			}
		}
		if (parsedCommandLine.isOptionPresent(GAP_EXTEND_SCORE_OPTION)) {
			try {
				gapExtendScore = Double.parseDouble(parsedCommandLine.getOptionsValue(GAP_EXTEND_SCORE_OPTION));
			} catch (NumberFormatException ex) {
				throw new IllegalStateException("Value specified for gap extend penalty not a double[" + parsedCommandLine.getOptionsValue(GAP_EXTEND_SCORE_OPTION) + "].");
			}
		}

		IAlignmentScorer alignmentScorer = new KensScorer(matchScore, mismatchScore, gapExtendScore, gapOpenScore, false);

		return alignmentScorer;

	}

	private static CommandLineOptionsGroup getCommandLineOptionsGroup() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup(APPLICATION_NAME + " (Version:" + applicationVersionFromManifest + ")");
		group.addOption(USAGE_OPTION);
		group.addOption(FASTQ_ONE_OPTION);
		group.addOption(PROBE_OPTION);
		group.addOption(OUTPUT_DIR_OPTION);
		group.addOption(OUTPUT_TRACEABILITY_MATRIX_OPTION);
		group.addOption(MIN_ALIGNMENT_SCORE_THRESHOLD_OPTION);
		group.addOption(NUMBER_OF_TOP_PROBE_MATCHES_TO_ALIGN_OPTION);
		group.addOption(MAX_NUMBER_OF_RECORDS_TO_READ_OPTION);
		group.addOption(MATCH_SCORE_OPTION);
		group.addOption(MISMATCH_SCORE_OPTION);
		group.addOption(GAP_OPEN_SCORE_OPTION);
		group.addOption(GAP_EXTEND_SCORE_OPTION);
		return group;
	}

	private static class KensScorer extends SimpleAlignmentScorer {

		public KensScorer(double match, double mismatch, double gapExtension, double gapStart, boolean shouldPenalizeTerminalGaps) {
			super(match, mismatch, gapExtension, gapStart, shouldPenalizeTerminalGaps);
		}

		@Override
		public double getMatchScore(ICode codeOne, ICode codeTwo) {
			// An n will be counted as a mismatch
			double score = mismatchScore;
			if (!(codeOne.equals(IupacNucleotideCode.N) || (codeTwo.equals(IupacNucleotideCode.N)))) {
				score = super.getMatchScore(codeOne, codeTwo);
			}
			return score;
		}

	}

}
