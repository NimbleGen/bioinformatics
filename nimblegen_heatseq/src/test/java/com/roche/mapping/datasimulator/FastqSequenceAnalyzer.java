package com.roche.mapping.datasimulator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.picard.fastq.FastqReader;
import net.sf.picard.fastq.FastqRecord;

import com.roche.heatseq.objects.IlluminaFastQHeader;
import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.objects.ProbesBySequenceName;
import com.roche.heatseq.process.PrefuppCli;
import com.roche.heatseq.process.ProbeFileUtil;
import com.roche.heatseq.process.TabDelimitedFileWriter;
import com.roche.heatseq.qualityreport.ReportManager;
import com.roche.sandbox.PieChart;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.DelimitedFileParserUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class FastqSequenceAnalyzer {

	// private final static String READ_ONE_TERMINAL_PRIMER = "AGATCGGAAGAG";
	// private final static String READ_TWO_TERMINAL_PRIMER = "ACACTACCGTCGG";

	private static final String DIRECTORY_NAME = "rebalance";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// try {
		// tallyRepeatSequences(new File("D:/ATM_140/results/50pm_report_unable_to_map_one.fastq"), new File("D:/fastq1_unmapped_tallies.txt"));
		// tallyRepeatSequences(new File("D:/ATM_140/results/50pm_report_unable_to_map_two.fastq"), new File("D:/fastq2_unmapped_tallies.txt"));
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		// try {
		// compareProbes(new File("D:/single_strand_probes/140mer_cancer_tiling.uw.96_probe_ATM.RNGformat.txt"), "GATTGTTAAACATTTACACTGACGTTGATAGCTGTGGTTTTATC");
		// compareProbes(new File("D:/single_strand_probes/140mer_cancer_tiling.uw.96_probe_ATM.RNGformat.txt"), "CATTTACACTGACGT");
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		try {
			runProject(new File("D:/" + DIRECTORY_NAME + "/overview.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		// System.out.println(match("AAACAAAT", "CBBAT", 1));

		// try {
		// // analyzeUnmappedReads(new File("D:/ATM_140/probe_info_96.txt"), new File("D:/ATM_140/results/50pm_report_unable_to_map_one.fastq"), new File(
		// // "D:/ATM_140/results/50pm_report_unable_to_map_two.fastq"), new File("D:/ATM_140/results/50pm_report_primer_alignment_counts.fastq"), new File(
		// // "D:/ATM_140/results/50pm_report_unmapped_read_two_with_no_primer_matches.txt"), new File("D:/ATM_140/results/50pm_report_unmapped_read_one_with_no_primer_matches.txt"), new File(
		// // "D:/ATM_140/results/50pm_report_unmapped_reads_with_multiple_primer_matches.txt"));
		//
		// analyzeUnmappedReads(new File("D:/ATM_140/probe_info_96.txt"), new File("D:/ATM_140/results/250pm_report_unable_to_map_one.fastq"), new File(
		// "D:/ATM_140/results/250pm_report_unable_to_map_two.fastq"), new File("D:/ATM_140/results/250pm_report_primer_alignment_counts.fastq"), new File(
		// "D:/ATM_140/results/250pm_report_unmapped_read_two_with_no_primer_matches.txt"), new File("D:/ATM_140/results/250pm_report_unmapped_read_one_with_no_primer_matches.txt"),
		// new File("D:/ATM_140/results/250pm_report_unmapped_reads_with_multiple_primer_matches.txt"));
		//
		// analyzeUnmappedReads(new File("D:/ATM_140/probe_info_96.txt"), new File("D:/ATM_140/results/500pm_report_unable_to_map_one.fastq"), new File(
		// "D:/ATM_140/results/500pm_report_unable_to_map_two.fastq"), new File("D:/ATM_140/results/500pm_report_primer_alignment_counts.fastq"), new File(
		// "D:/ATM_140/results/500pm_report_unmapped_read_two_with_no_primer_matches.txt"), new File("D:/ATM_140/results/500pm_report_unmapped_read_one_with_no_primer_matches.txt"),
		// new File("D:/ATM_140/results/500pm_report_unmapped_reads_with_multiple_primer_matches.txt"));
		//
		// analyzeUnmappedReads(new File("D:/ATM_140/probe_info_96.txt"), new File("D:/ATM_140/results/1000pm_report_unable_to_map_one.fastq"), new File(
		// "D:/ATM_140/results/1000pm_report_unable_to_map_two.fastq"), new File("D:/ATM_140/results/1000pm_report_primer_alignment_counts.fastq"), new File(
		// "D:/ATM_140/results/1000pm_report_unmapped_read_two_with_no_primer_matches.txt"), new File("D:/ATM_140/results/1000pm_report_unmapped_read_one_with_no_primer_matches.txt"),
		// new File("D:/ATM_140/results/1000pm_report_unmapped_reads_with_multiple_primer_matches.txt"));
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		// try {
		// analyzeLengths(new File("D:/ATM_140/results/50pm_report_unable_to_map_one.fastq"), new File("D:/ATM_140/results/50pm_report_unable_to_map_two.fastq"), READ_ONE_TERMINAL_PRIMER,
		// READ_TWO_TERMINAL_PRIMER);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

	}

	private static void runProject(File projectOverviewFile) throws IOException {
		String[] headerNames = new String[] { "Directory", "Probe File", "Fastq 1", "Fastq 2", "Bam File" };
		Map<String, List<String>> entries = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(projectOverviewFile, headerNames, StringUtil.TAB);

		List<String> directories = entries.get(headerNames[0]);
		List<String> probeFiles = entries.get(headerNames[1]);
		List<String> fastq1Files = entries.get(headerNames[2]);
		List<String> fastq2Files = entries.get(headerNames[3]);
		List<String> bamFiles = entries.get(headerNames[4]);

		// File terminalPrimerStatsFile = new File("D:/50ngCapture/results/readPrimerStats.txt");
		// FileUtil.createNewFile(terminalPrimerStatsFile);
		// String[] header = new String[] { "fastq1", "fastq2", "r1Mean", "r1StDev", "r1Found", "r1Missing", "r1PercetContainingTerminalPrimer", "r2Mean", "r2StDev", "r2Found", "r2Missing",
		// "r2PercetContainingTerminalPrimer", "totalReadPairs" };
		// V
		// GATTGTTAAACATTTACACTGACGTTGATAGCTGTGGTTTTATC
		// String sequence = "GATTGTTAAACATTTACA";// +"CTGACGTTGATAGCTGTGGTTTTATC";
		// String sequence = "ACTGACGTTGATAGCTGTGGTTTTATC";
		// File sequenceRatioFile = new File("D:/50ngCapture/results/unread_matching_" + sequence + "_stats.txt");
		// FileUtil.createNewFile(sequenceRatioFile);
		// String[] header2 = new String[] { "fastq1", "matching_reads", "total_reads", "matching_ratio" };
		// try (TabDelimitedFileWriter readPrimerStatsWriter = new TabDelimitedFileWriter(terminalPrimerStatsFile, header)) {
		// try (TabDelimitedFileWriter sequenceRatioStatsWriter = new TabDelimitedFileWriter(sequenceRatioFile, header2)) {

		TallyMap<String> tally = new TallyMap<String>();
		File tallyFile = new File("D:" + DIRECTORY_NAME + "/results/tally.txt");
		FileUtil.createNewFile(tallyFile);

		for (int i = 0; i < directories.size(); i++) {
			File directoryFile = new File(directories.get(i));
			File resultsDirectory = new File(directoryFile, "/results2/");
			File probeFile = new File(directoryFile, probeFiles.get(i));
			File fastq1File = new File(directoryFile, fastq1Files.get(i));
			File fastq2File = new File(directoryFile, fastq2Files.get(i));
			File bamFile = new File(directoryFile, bamFiles.get(i));
			String prefixName = "_" + bamFile.getName().replaceAll("\\.srt\\.bam", "");

			String[] arguments = new String[] { "--r1", fastq1File.getAbsolutePath(), "--r2", fastq2File.getAbsolutePath(), "--probe", probeFile.getAbsolutePath(), "--inputBam",
					bamFile.getAbsolutePath(), "--outputDir", resultsDirectory.getAbsolutePath(), "--outputBamFileName", prefixName + "_results.bam", "--outputReports", "--uidLength", "10",
					"--lenientValidation", "--outputPrefix", prefixName, "--outputFastq" };

			PrefuppCli.main(arguments);

			File unmappedFastqOneFile = new File(resultsDirectory, prefixName + ReportManager.UNABLE_TO_MAP_FASTQ_ONE_REPORT_NAME);
			File unmappedFastqTwoFile = new File(resultsDirectory, prefixName + ReportManager.UNABLE_TO_MAP_FASTQ_TWO_REPORT_NAME);

			File probeDetailsFile = new File(resultsDirectory, prefixName + ReportManager.DETAILS_REPORT_NAME);
			File prefuppSummaryFile = new File(resultsDirectory, prefixName + ReportManager.SUMMARY_REPORT_NAME);
			analyzeReads(probeFile, probeDetailsFile, unmappedFastqOneFile, unmappedFastqTwoFile, prefuppSummaryFile, new File(resultsDirectory, prefixName + "_unmapped_counts.txt"), new File(
					resultsDirectory, prefixName + "_unmapped_unassigned_read_ones.txt"), new File(resultsDirectory, prefixName + "_unmapped_unassigned_read_twos.txt"), new File(resultsDirectory,
					prefixName + "_unmapped_assigned_to_mult_probes.txt"), new File(resultsDirectory, prefixName + "_probe_breakdown.txt"), new File(resultsDirectory, prefixName
					+ "_read_pairs_pie_charts.pdf"));

			// File mappedFastqOneFile = new File(resultsDirectory, bamFile.getName() + "_one.fastq");
			// File mappedFastqTwoFile = new File(resultsDirectory, bamFile.getName() + "_two.fastq");

			// analyzeReads(probeFile, mappedFastqOneFile, mappedFastqTwoFile, new File(resultsDirectory, prefixName + "_mapped_counts.txt"), new File(resultsDirectory, prefixName
			// + "_mapped_unassigned_read_ones.txt"), new File(resultsDirectory, prefixName + "_mapped_unassigned_read_twos.txt"), new File(resultsDirectory, prefixName
			// + "_mapped_assigned_to_mult_probes.txt"));

			tallyRepeatSequences(unmappedFastqOneFile, new File(unmappedFastqOneFile.getAbsolutePath() + "_counts.txt"));
			tallyRepeatSequences(unmappedFastqTwoFile, new File(unmappedFastqTwoFile.getAbsolutePath() + "_counts.txt"));

			tally.addAll(getMatchingTargetSequences(unmappedFastqOneFile, unmappedFastqTwoFile, "GATTGTTAAACATTTAC", "CTGACGTTGATAGCTGTGGTTTTATC"));

			// LengthStats lengthStats = analyzeLengths(unmappedFastqOneFile, unmappedFastqTwoFile, READ_ONE_TERMINAL_PRIMER, READ_TWO_TERMINAL_PRIMER);
			// readPrimerStatsWriter.writeLine(fastq1File.getName(), fastq2File.getName(), lengthStats.r1Mean, lengthStats.r1StDev, lengthStats.r1Found, lengthStats.r1Missing,
			// lengthStats.r1PercetContainingTerminalPrimer, lengthStats.r2Mean, lengthStats.r2StDev, lengthStats.r2Found, lengthStats.r2Missing,
			// lengthStats.r2PercetContainingTerminalPrimer, lengthStats.totalReadPairs);
			// Ratio ratio = lookForSequence(unmappedFastqOneFile, sequence);
			// sequenceRatioStatsWriter.writeLine(unmappedFastqOneFile.getName(), ratio.totalReadPairsContainingSequence, ratio.totalReadPairs, ratio.getRatio());
		}
		// }
		// }

		try (TabDelimitedFileWriter writer = new TabDelimitedFileWriter(tallyFile, new String[] { "sequence", "count" })) {
			for (Entry<String, Integer> sequenceAndCount : tally.getObjectsSortedFromMostTalliesToLeast()) {
				int count = sequenceAndCount.getValue();
				String sequence = sequenceAndCount.getKey();
				writer.writeLine(sequence, count);
			}
		}

	}

	// private final static void compareProbes(File probeFile, String sequenceToMatch) throws IOException {
	//
	// ISequence sequence = new IupacNucleotideCodeSequence(sequenceToMatch);
	//
	// List<ProbeAlignment> probeAlignments = new ArrayList<ProbeAlignment>();
	//
	// ProbesBySequenceName probeInfo = ProbeFileUtil.parseProbeInfoFile(probeFile);
	// for (Probe probe : probeInfo.getProbes()) {
	// ISequence extension = probe.getExtensionPrimerSequence();
	// ISequence ligation = probe.getLigationPrimerSequence();
	//
	// IAlignmentScorer scorer = new SimpleAlignmentScorer(5, 2, 0, -5, false);
	//
	// NeedlemanWunschGlobalAlignment extensionAlignment = new NeedlemanWunschGlobalAlignment(extension, sequence, scorer);
	// NeedlemanWunschGlobalAlignment ligationAlignment = new NeedlemanWunschGlobalAlignment(ligation, sequence, scorer);
	//
	// ProbeAlignment probeExtensionAlignment = new ProbeAlignment(extensionAlignment, true, probe);
	// ProbeAlignment probeLigationAlignment = new ProbeAlignment(ligationAlignment, false, probe);
	//
	// probeAlignments.add(probeLigationAlignment);
	// probeAlignments.add(probeExtensionAlignment);
	// }
	//
	// Collections.sort(probeAlignments, new Comparator<ProbeAlignment>() {
	// @Override
	// public int compare(ProbeAlignment o1, ProbeAlignment o2) {
	// return Integer.compare(o2.getAlignment().getAlignmentScore(), o1.getAlignment().getAlignmentScore());
	// }
	// });
	//
	// for (ProbeAlignment pa : probeAlignments) {
	// System.out.println(pa);
	// }
	// }

	// private static class ProbeAlignment {
	// private NeedlemanWunschGlobalAlignment alignment;
	// private boolean isExtension;
	// private Probe probe;
	//
	// public ProbeAlignment(NeedlemanWunschGlobalAlignment alignment, boolean isExtension, Probe probe) {
	// super();
	// this.alignment = alignment;
	// this.isExtension = isExtension;
	// this.probe = probe;
	// }
	//
	// public NeedlemanWunschGlobalAlignment getAlignment() {
	// return alignment;
	// }
	//
	// public void setAlignment(NeedlemanWunschGlobalAlignment alignment) {
	// this.alignment = alignment;
	// }
	//
	// public boolean isExtension() {
	// return isExtension;
	// }
	//
	// public void setExtension(boolean isExtension) {
	// this.isExtension = isExtension;
	// }
	//
	// public Probe getProbe() {
	// return probe;
	// }
	//
	// public void setProbe(Probe probe) {
	// this.probe = probe;
	// }
	//
	// public String toString() {
	// String string = probe.getProbeId();
	// if (isExtension) {
	// string += ":ext" + StringUtil.NEWLINE;
	// } else {
	// string += ":lig" + StringUtil.NEWLINE;
	// }
	// string += alignment.getAlignmentAsString();
	// return string;
	// }
	//
	// }

	private static void tallyRepeatSequences(File fastqFile, File outputFile) throws IOException {
		TallyMap<ISequence> sequenceTallies = new TallyMap<ISequence>();
		try (FastqReader fastQReader = new FastqReader(fastqFile)) {

			while (fastQReader.hasNext()) {
				FastqRecord record = fastQReader.next();
				sequenceTallies.add(new IupacNucleotideCodeSequence(record.getReadString()));
			}
		}

		try (TabDelimitedFileWriter writer = new TabDelimitedFileWriter(outputFile, new String[] { "sequence", "count" })) {
			for (Entry<ISequence, Integer> sequenceAndCount : sequenceTallies.getObjectsSortedFromMostTalliesToLeast()) {
				int count = sequenceAndCount.getValue();
				ISequence sequence = sequenceAndCount.getKey();
				writer.writeLine(sequence, count);
			}
		}
	}

	// private static LengthStats analyzeLengths(File fastqOneFile, File fastqTwoFile, String readOneTerminalPrimer, String readTwoTerminalPrimer) throws IOException {
	//
	// RunningStats readOneLengthRunningStats = new RunningStats();
	// RunningStats readTwoLengthRunningStats = new RunningStats();
	//
	// int readOneMissingTerminalPrimer = 0;
	// int readTwoMissingTerminalPrimer = 0;
	//
	// int totalReadPairs = 0;
	//
	// try (FastqReader fastQOneReader = new FastqReader(fastqOneFile)) {
	// try (FastqReader fastQTwoReader = new FastqReader(fastqTwoFile)) {
	// while (fastQOneReader.hasNext() && fastQTwoReader.hasNext()) {
	// totalReadPairs++;
	// FastqRecord recordOne = fastQOneReader.next();
	// FastqRecord recordTwo = fastQTwoReader.next();
	// String readOne = recordOne.getReadString();
	// String readTwo = recordTwo.getReadString();
	//
	// int readOneEnd = readOne.indexOf(readOneTerminalPrimer);
	// int readTwoEnd = readTwo.indexOf(readTwoTerminalPrimer);
	//
	// if (readOneEnd >= 0) {
	// readOneLengthRunningStats.addValue((double) readOneEnd);
	// } else {
	// readOneMissingTerminalPrimer++;
	// }
	//
	// if (readTwoEnd >= 0) {
	// readTwoLengthRunningStats.addValue((double) readTwoEnd);
	// } else {
	// readTwoMissingTerminalPrimer++;
	// }
	// }
	// }
	// }
	// double r1Mean = readOneLengthRunningStats.getCurrentMean();
	// double r1StDev = readOneLengthRunningStats.getCurrentStandardDeviation();
	// double r1Found = readOneLengthRunningStats.getCurrentNumberOfValues();
	// double r1Missing = readOneMissingTerminalPrimer;
	// double r1PercetContainingTerminalPrimer = r1Found / (double) totalReadPairs;
	//
	// double r2Mean = readTwoLengthRunningStats.getCurrentMean();
	// double r2StDev = readTwoLengthRunningStats.getCurrentStandardDeviation();
	// double r2Found = readTwoLengthRunningStats.getCurrentNumberOfValues();
	// double r2Missing = readTwoMissingTerminalPrimer;
	// double r2PercetContainingTerminalPrimer = r2Found / (double) totalReadPairs;
	//
	// LengthStats lengthStats = new LengthStats(r1Mean, r1StDev, r1Found, r1Missing, r1PercetContainingTerminalPrimer, r2Mean, r2StDev, r2Found, r2Missing, r2PercetContainingTerminalPrimer,
	// totalReadPairs);
	// return lengthStats;
	// }

	// private static Ratio lookForSequence(File fastqFile, String sequence) throws IOException {
	//
	// int totalReadPairs = 0;
	// int totalReadPairsContainingSequence = 0;
	//
	// try (FastqReader fastQReader = new FastqReader(fastqFile)) {
	// while (fastQReader.hasNext()) {
	// totalReadPairs++;
	// FastqRecord record = fastQReader.next();
	// String read = record.getReadString();
	//
	// if (read.contains(sequence)) {
	// totalReadPairsContainingSequence++;
	// }
	// }
	// }
	//
	// return new Ratio(totalReadPairs, totalReadPairsContainingSequence);
	// }

	// private static class Ratio {
	// int totalReadPairs;
	// int totalReadPairsContainingSequence;
	//
	// public Ratio(int totalReadPairs, int totalReadPairsContainingSequence) {
	// super();
	// this.totalReadPairs = totalReadPairs;
	// this.totalReadPairsContainingSequence = totalReadPairsContainingSequence;
	// }
	//
	// double getRatio() {
	// return (double) totalReadPairsContainingSequence / (double) totalReadPairs;
	// }
	// }

	// private static class LengthStats {
	// double r1Mean;
	// double r1StDev;
	// double r1Found;
	// double r1Missing;
	// double r1PercetContainingTerminalPrimer;
	//
	// double r2Mean;
	// double r2StDev;
	// double r2Found;
	// double r2Missing;
	// double r2PercetContainingTerminalPrimer;
	// double totalReadPairs;
	//
	// public LengthStats(double r1Mean, double r1StDev, double r1Found, double r1Missing, double r1PercetContainingTerminalPrimer, double r2Mean, double r2StDev, double r2Found, double r2Missing,
	// double r2PercetContainingTerminalPrimer, double totalReadPairs) {
	// super();
	// this.r1Mean = r1Mean;
	// this.r1StDev = r1StDev;
	// this.r1Found = r1Found;
	// this.r1Missing = r1Missing;
	// this.r1PercetContainingTerminalPrimer = r1PercetContainingTerminalPrimer;
	// this.r2Mean = r2Mean;
	// this.r2StDev = r2StDev;
	// this.r2Found = r2Found;
	// this.r2Missing = r2Missing;
	// this.r2PercetContainingTerminalPrimer = r2PercetContainingTerminalPrimer;
	// this.totalReadPairs = totalReadPairs;
	// }
	// }

	private static List<String> getMatchingTargetSequences(File fastqOneFile, File fastqTwoFile, String extensionPrimer, String ligationPrimer) {
		List<String> matchingTargetSequences = new ArrayList<String>();

		String extensionPrimerReverseCompliment = new IupacNucleotideCodeSequence(extensionPrimer).getReverseCompliment().toString();
		String ligationPrimerReverseCompliment = new IupacNucleotideCodeSequence(ligationPrimer).getReverseCompliment().toString();

		Pattern readOnePattern = Pattern.compile(extensionPrimer + "(.+?)" + ligationPrimer);
		Pattern readTwoPattern = Pattern.compile(ligationPrimerReverseCompliment + "(.+?)" + extensionPrimerReverseCompliment);

		try (FastqReader fastQOneReader = new FastqReader(fastqOneFile)) {
			try (FastqReader fastQTwoReader = new FastqReader(fastqTwoFile)) {
				while (fastQOneReader.hasNext() && fastQTwoReader.hasNext()) {
					FastqRecord recordOne = fastQOneReader.next();
					FastqRecord recordTwo = fastQTwoReader.next();
					String readOne = recordOne.getReadString();
					String readTwo = recordTwo.getReadString();

					boolean extensionPresentInR1 = readOne.contains(extensionPrimer);
					boolean ligationPresentInR1 = readOne.contains(ligationPrimer);

					boolean reverseComplimentExtensionPresentInR2 = readTwo.contains(extensionPrimerReverseCompliment);
					boolean reverseComplimentLigationPresentInR2 = readTwo.contains(ligationPrimerReverseCompliment);

					if (extensionPresentInR1 && ligationPresentInR1 && reverseComplimentExtensionPresentInR2 && reverseComplimentLigationPresentInR2) {
						Matcher r1Matcher = readOnePattern.matcher(readOne);
						Matcher r2Matcher = readTwoPattern.matcher(readTwo);
						boolean foundOne = r1Matcher.find();
						boolean foundTwo = r2Matcher.find();
						if (foundOne && foundTwo) {
							String readOneTarget = r1Matcher.group(1);
							String readTwoTarget = r2Matcher.group(1);
							String readTwoReverseComplimentTarget = new IupacNucleotideCodeSequence(readTwoTarget).getReverseCompliment().toString();

							if (readOneTarget.equals(readTwoReverseComplimentTarget)) {
								matchingTargetSequences.add(readOneTarget);
							}
						}
					}
				}
			}
		}

		return matchingTargetSequences;

	}

	private static class ProbeBreakdown {
		private int uniqueMappedOnTargetReadPairs;
		private int duplicateMappedOnTargetReadPairs;
		private int unmappedReadPairs;

		public ProbeBreakdown() {
			super();
		}

		public int getUniqueMappedOnTargetReadPairs() {
			return uniqueMappedOnTargetReadPairs;
		}

		public int getDuplicateMappedOnTargetReadPairs() {
			return duplicateMappedOnTargetReadPairs;
		}

		public int getMappedOnTargetReadPairs() {
			return uniqueMappedOnTargetReadPairs + duplicateMappedOnTargetReadPairs;
		}

		public int getUnmappedReadPairs() {
			return unmappedReadPairs;
		}

		public int getTotalReadPairs() {
			return getMappedOnTargetReadPairs() + unmappedReadPairs;
		}

		public void setUniqueMappedOnTargetReadPairs(int uniqueMappedOnTargetReadPairs) {
			this.uniqueMappedOnTargetReadPairs = uniqueMappedOnTargetReadPairs;
		}

		public void setDuplicateMappedOnTargetReadPairs(int duplicateMappedOnTargetReadPairs) {
			this.duplicateMappedOnTargetReadPairs = duplicateMappedOnTargetReadPairs;
		}

		public void setUnmappedReadPairs(int unmappedReadPairs) {
			this.unmappedReadPairs = unmappedReadPairs;
		}

	}

	private static void analyzeReads(File probeInfoFile, File probeDetailsFile, File fastqOneFile, File fastqTwoFile, File prefuppSummaryFile, File outputCountFile, File readOneUnassignedToProbeFile,
			File readTwoUnassignedToProbeFile, File readsAssignedToMultipleProbesFile, File probeBreakdownFile, File outputPieChartsFile) throws IOException {
		long start = System.currentTimeMillis();
		ProbesBySequenceName probesBySequenceName = ProbeFileUtil.parseProbeInfoFile(probeInfoFile);

		Map<String, Set<Probe>> readToProbeMap = new LinkedHashMap<String, Set<Probe>>();

		Map<String, ProbeBreakdown> probeIdToBreakdownMap = new HashMap<String, ProbeBreakdown>();

		try (TabDelimitedFileWriter writer = new TabDelimitedFileWriter(outputCountFile, new String[] { "probe", "r1_and_r2_complete", "r1_and_r2_partial", "r1_complete", "r1_partial", "r2_complete",
				"r2_partial", "r1_extension", "r1_ligation", "r2_rc_extension", "r2_rc_ligation" })) {

			for (Probe probe : probesBySequenceName.getProbes()) {
				int fullyMappedCount = 0;
				int partiallyMappedCount = 0;

				int r1Ext = 0;
				int r1Lig = 0;
				int r2RCExt = 0;
				int r2RCLig = 0;
				int r1comp = 0;
				int r1part = 0;
				int r2comp = 0;
				int r2part = 0;

				ISequence extensionSequence = probe.getExtensionPrimerSequence();
				ISequence ligationSequence = probe.getLigationPrimerSequence();
				ISequence extensionSequenceReverseCompliment = extensionSequence.getReverseCompliment();
				ISequence ligationSequenceReverseCompliment = ligationSequence.getReverseCompliment();
				long startT = System.currentTimeMillis();
				try (FastqReader fastQOneReader = new FastqReader(fastqOneFile)) {
					try (FastqReader fastQTwoReader = new FastqReader(fastqTwoFile)) {
						while (fastQOneReader.hasNext() && fastQTwoReader.hasNext()) {
							FastqRecord recordOne = fastQOneReader.next();
							FastqRecord recordTwo = fastQTwoReader.next();
							String readName = IlluminaFastQHeader.getBaseHeader(recordOne.getReadHeader());
							String readOne = recordOne.getReadString();
							String readTwo = recordTwo.getReadString();

							ISequence readOneSequence = new IupacNucleotideCodeSequence(readOne);
							ISequence readTwoSequence = new IupacNucleotideCodeSequence(readTwo);

							boolean readOneContainsExtension = matches(readOneSequence, extensionSequence);

							boolean readOneContainsLigation = matches(readOneSequence, ligationSequence);

							boolean readTwoContainsReverseComplimentExtension = matches(readTwoSequence, extensionSequenceReverseCompliment);
							boolean readTwoContainsReverseComplimentLigation = matches(readTwoSequence, ligationSequenceReverseCompliment);

							boolean fullyMapped = (readOneContainsExtension && readOneContainsLigation && readTwoContainsReverseComplimentExtension && readTwoContainsReverseComplimentLigation);
							boolean partiallyMapped = (!fullyMapped && (readOneContainsExtension || readOneContainsLigation || readTwoContainsReverseComplimentExtension || readTwoContainsReverseComplimentLigation));

							if (fullyMapped) {
								fullyMappedCount++;
							}

							if (partiallyMapped) {
								partiallyMappedCount++;
							}

							if (readOneContainsExtension) {
								r1Ext++;
							}

							if (readOneContainsLigation) {
								r1Lig++;
							}

							if (readOneContainsLigation && readOneContainsExtension) {
								r1comp++;
							}

							if ((!readOneContainsExtension && readOneContainsLigation) || (readOneContainsExtension && !readOneContainsLigation)) {
								r1part++;
							}

							if (readTwoContainsReverseComplimentExtension && readTwoContainsReverseComplimentLigation) {
								r2comp++;
							}

							if ((!readTwoContainsReverseComplimentExtension && readTwoContainsReverseComplimentLigation)
									|| (readTwoContainsReverseComplimentExtension && !readTwoContainsReverseComplimentLigation)) {
								r2part++;
							}

							if (readTwoContainsReverseComplimentExtension) {
								r2RCExt++;
							}

							if (readTwoContainsReverseComplimentLigation) {
								r2RCLig++;
							}

							Set<Probe> mappedProbes = readToProbeMap.get(readName);
							if (mappedProbes == null) {
								mappedProbes = new HashSet<Probe>();
							}

							if (fullyMapped) {
								mappedProbes.add(probe);
							}

							readToProbeMap.put(readName, mappedProbes);
						}
					}
				}
				long endT = System.currentTimeMillis();
				System.out.println("time:" + (endT - startT));
				writer.writeLine(probe.getProbeId(), fullyMappedCount, partiallyMappedCount, r1comp, r1part, r2comp, r2part, r1Ext, r1Lig, r2RCExt, r2RCLig);
				ProbeBreakdown probeBreakdown = new ProbeBreakdown();
				probeBreakdown.setUnmappedReadPairs(fullyMappedCount);
				probeIdToBreakdownMap.put(probe.getProbeId(), probeBreakdown);
			}
		}

		Set<String> unassignedReadNames = new LinkedHashSet<String>();

		try (TabDelimitedFileWriter multipleAssignedWriter = new TabDelimitedFileWriter(readsAssignedToMultipleProbesFile, new String[] { "Read_Name", "Number_Of_Probes", "Probe_Names" })) {

			for (Entry<String, Set<Probe>> entry : readToProbeMap.entrySet()) {
				int size = entry.getValue().size();
				if (size == 0) {
					unassignedReadNames.add(entry.getKey());
				}

				if (size > 2) {
					StringBuilder probeNames = new StringBuilder();
					Set<Probe> probes = entry.getValue();
					for (Probe probe : probes) {
						probeNames.append(probe.getProbeId() + ", ");
					}
					multipleAssignedWriter.writeLine(entry.getKey(), size, probeNames.toString().substring(0, probeNames.length() - 2));
				}
			}

			TallyMap<ISequence> unassignedReadOneTallies = new TallyMap<ISequence>();
			TallyMap<ISequence> unassignedReadTwoTallies = new TallyMap<ISequence>();

			try (FastqReader fastQOneReader = new FastqReader(fastqOneFile)) {
				try (FastqReader fastQTwoReader = new FastqReader(fastqTwoFile)) {
					while (fastQOneReader.hasNext() && fastQTwoReader.hasNext()) {
						FastqRecord recordOne = fastQOneReader.next();
						FastqRecord recordTwo = fastQTwoReader.next();
						String readName = IlluminaFastQHeader.getBaseHeader(recordOne.getReadHeader());
						if (unassignedReadNames.contains(readName)) {
							unassignedReadOneTallies.add(new IupacNucleotideCodeSequence(recordOne.getReadString()));
							unassignedReadTwoTallies.add(new IupacNucleotideCodeSequence(recordTwo.getReadString()));
						}

					}
				}
			}

			try (TabDelimitedFileWriter writer = new TabDelimitedFileWriter(readOneUnassignedToProbeFile, new String[] { "sequence", "count" })) {
				for (Entry<ISequence, Integer> sequenceAndCount : unassignedReadOneTallies.getObjectsSortedFromMostTalliesToLeast()) {
					int count = sequenceAndCount.getValue();
					ISequence sequence = sequenceAndCount.getKey();
					writer.writeLine(sequence, count);
				}
			}

			try (TabDelimitedFileWriter writer = new TabDelimitedFileWriter(readTwoUnassignedToProbeFile, new String[] { "sequence", "count" })) {
				for (Entry<ISequence, Integer> sequenceAndCount : unassignedReadTwoTallies.getObjectsSortedFromMostTalliesToLeast()) {
					int count = sequenceAndCount.getValue();
					ISequence sequence = sequenceAndCount.getKey();
					writer.writeLine(sequence, count);
				}
			}

			String[] probeDetailsHeaderNames = new String[] { "probe_id", "total_uids", "average_number_of_read_pairs_per_uid" };
			Iterator<Map<String, String>> parsedProbeDetails = DelimitedFileParserUtil.getHeaderNameToValueMapRowIteratorFromDelimitedFile(probeDetailsFile, probeDetailsHeaderNames, StringUtil.TAB);
			while (parsedProbeDetails.hasNext()) {
				Map<String, String> line = parsedProbeDetails.next();
				String probeId = line.get(probeDetailsHeaderNames[0]);
				int totalUids = Integer.valueOf(line.get(probeDetailsHeaderNames[1]));
				double averageNumberOfReadPairsPerUid = Double.valueOf(line.get(probeDetailsHeaderNames[2]));
				ProbeBreakdown probeBreakdown = probeIdToBreakdownMap.get(probeId);
				probeBreakdown.setUniqueMappedOnTargetReadPairs(totalUids);
				double totalReadPairs = totalUids * averageNumberOfReadPairsPerUid;
				probeBreakdown.setDuplicateMappedOnTargetReadPairs((int) (totalReadPairs - totalUids));
				probeIdToBreakdownMap.put(probeId, probeBreakdown);
			}
			String[] probeBreakdownHeader = new String[] { "probe_id", "unique_mapped_on_target_read_pairs", "duplicate_mapped_on_target_read_pairs", "mapped_on_target_read_pairs",
					"unmapped_read_pairs", "total_read_pairs" };

			try (TabDelimitedFileWriter writer = new TabDelimitedFileWriter(probeBreakdownFile, probeBreakdownHeader)) {
				for (Entry<String, ProbeBreakdown> entry : probeIdToBreakdownMap.entrySet()) {
					String probeId = entry.getKey();
					ProbeBreakdown probeBreakdown = entry.getValue();
					writer.writeLine(probeId, probeBreakdown.getUniqueMappedOnTargetReadPairs(), probeBreakdown.getDuplicateMappedOnTargetReadPairs(), probeBreakdown.getMappedOnTargetReadPairs(),
							probeBreakdown.getUnmappedReadPairs(), probeBreakdown.getTotalReadPairs());
				}
			}
		}

		PieChart.generateVisualReport(DIRECTORY_NAME + " PAIRED READS SUMMARY", prefuppSummaryFile, probeBreakdownFile, outputPieChartsFile);

		long stop = System.currentTimeMillis();
		System.out.println("total time:" + (stop - start));

	}

	private static boolean matches(ISequence referenceSequence, ISequence querySequence) {
		return match(referenceSequence.toString(), querySequence.toString(), 4);
	}

	private static boolean match(String referenceString, String queryString, int allowedMismatches) {
		boolean match = false;

		int startingReferenceIndex = 0;

		while (!match && startingReferenceIndex + queryString.length() <= referenceString.length()) {
			int queryIndex = 0;
			int mismatches = 0;
			boolean tooManyMisMatches = false;
			while (queryIndex < queryString.length() && !tooManyMisMatches) {
				if (queryString.charAt(queryIndex) != referenceString.charAt(startingReferenceIndex + queryIndex)) {
					mismatches++;
				}
				queryIndex++;
				tooManyMisMatches = (mismatches > allowedMismatches);
			}
			match = !tooManyMisMatches;
			startingReferenceIndex++;
		}
		return match;
	}
}
