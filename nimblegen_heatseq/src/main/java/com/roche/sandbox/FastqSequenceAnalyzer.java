package com.roche.sandbox;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sf.picard.fastq.FastqReader;
import net.sf.picard.fastq.FastqRecord;

import com.roche.heatseq.objects.IlluminaFastQHeader;
import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.objects.ProbesBySequenceName;
import com.roche.heatseq.process.PrefuppCli;
import com.roche.heatseq.process.ProbeFileUtil;
import com.roche.heatseq.process.TabDelimitedFileWriter;
import com.roche.heatseq.qualityreport.ReportManager;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOption;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOptionsGroup;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineParser;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.mapping.SimpleMapper;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.DelimitedFileParserUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class FastqSequenceAnalyzer {

	// private final static String READ_ONE_TERMINAL_PRIMER = "AGATCGGAAGAG";
	// private final static String READ_TWO_TERMINAL_PRIMER = "ACACTACCGTCGG";

	private final static String APPLICATION_NAME = "Project Prefupp Tool";
	private static String applicationVersionFromManifest;

	// private static final String DIRECTORY_NAME = "hotspot_cancer_1";
	// private static final String RESULTS_PATH = "/results2/";

	private final static CommandLineOption INPUT_DIR_OPTION = new CommandLineOption("Input Directory", "inputDir", null, "location to find overview.txt file and related files", true, false);
	private final static CommandLineOption RESULTS_PATH_OPTION = new CommandLineOption("Results Location", "results", null, "The location to place the results", true, false);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		runApplicationViaCommandLine(args);
	}

	private static CommandLineOptionsGroup getCommandLineOptionsGroup() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup(APPLICATION_NAME + " (Version:" + applicationVersionFromManifest + ")");
		group.addOption(INPUT_DIR_OPTION);
		group.addOption(RESULTS_PATH_OPTION);
		return group;
	}

	private static void runApplicationViaCommandLine(String[] args) {

		ParsedCommandLine parsedCommandLine = CommandLineParser.parseCommandLine(args, getCommandLineOptionsGroup());
		CommandLineParser.throwCommandLineParsingExceptions(parsedCommandLine);

		File inputDirectory = new File(parsedCommandLine.getOptionsValue(INPUT_DIR_OPTION));
		if (!inputDirectory.isDirectory()) {
			throw new IllegalStateException("The provided input directory[" + inputDirectory.getAbsolutePath() + "] is not a directory.");
		}

		File resultsDirectory = new File(parsedCommandLine.getOptionsValue(RESULTS_PATH_OPTION));
		if (!resultsDirectory.exists()) {
			try {
				FileUtil.createDirectory(resultsDirectory);
			} catch (IOException e) {
				System.out.println("Unable to create results directory[" + resultsDirectory.getAbsolutePath() + "]." + e.getMessage());
			}
		}

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
			runProject(new File(inputDirectory, "overview.txt"), resultsDirectory);
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

	private static void runProject(File projectOverviewFile, File resultsDirectory) throws IOException {
		String[] headerNames = new String[] { "Directory", "Probe File", "Fastq 1", "Fastq 2", "Bam File" };
		Map<String, List<String>> entries = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(projectOverviewFile, headerNames, StringUtil.TAB);

		List<String> directories = entries.get(headerNames[0]);
		List<String> probeFiles = entries.get(headerNames[1]);
		List<String> fastq1Files = entries.get(headerNames[2]);
		List<String> fastq2Files = entries.get(headerNames[3]);
		List<String> bamFiles = entries.get(headerNames[4]);

		Map<String, SimpleMapper<Probe>> extensionMapperByProbeFileName = new HashMap<String, SimpleMapper<Probe>>();
		Map<String, SimpleMapper<Probe>> ligationMapperByProbeFileName = new HashMap<String, SimpleMapper<Probe>>();

		for (int i = 0; i < directories.size(); i++) {
			String probeFileName = probeFiles.get(i);
			File directoryFile = new File(projectOverviewFile.getParentFile(), directories.get(i));
			File probeFile = new File(directoryFile, probeFileName);
			File fastq1File = new File(directoryFile, fastq1Files.get(i));
			File fastq2File = new File(directoryFile, fastq2Files.get(i));
			File bamFile = new File(directoryFile, bamFiles.get(i));
			String prefixName = "_" + bamFile.getName().replaceAll("\\.srt\\.bam", "");

			if (extensionMapperByProbeFileName.get(probeFileName) == null && ligationMapperByProbeFileName.get(probeFile) == null) {
				SimpleMapper<Probe> extensionMapper = new SimpleMapper<Probe>(7, 2, 1, 5);
				SimpleMapper<Probe> ligationMapper = new SimpleMapper<Probe>(7, 2, 1, 5);
				// SimpleMapper<Probe> extensionMapper = new SimpleMapper<Probe>(3, 3, 1, 2);
				// SimpleMapper<Probe> ligationMapper = new SimpleMapper<Probe>(3, 3, 1, 2);

				ProbesBySequenceName probes = ProbeFileUtil.parseProbeInfoFile(probeFile);

				for (Probe probe : probes.getProbes()) {
					extensionMapper.addReferenceSequence(probe.getExtensionPrimerSequence(), probe);
					ligationMapper.addReferenceSequence(probe.getLigationPrimerSequence(), probe);
				}

				extensionMapperByProbeFileName.put(probeFileName, extensionMapper);
				ligationMapperByProbeFileName.put(probeFileName, ligationMapper);
			}

			String[] arguments = new String[] { "--r1", fastq1File.getAbsolutePath(), "--r2", fastq2File.getAbsolutePath(), "--probe", probeFile.getAbsolutePath(), "--inputBam",
					bamFile.getAbsolutePath(), "--outputDir", resultsDirectory.getAbsolutePath(), "--outputBamFileName", prefixName + "_results.bam", "--outputReports", "--uidLength", "10",
					"--lenientValidation", "--outputPrefix", prefixName, "--outputFastq" };

			PrefuppCli.main(arguments);

			File unmappedFastqOneFile = new File(resultsDirectory, prefixName + ReportManager.UNABLE_TO_MAP_FASTQ_ONE_REPORT_NAME);
			File unmappedFastqTwoFile = new File(resultsDirectory, prefixName + ReportManager.UNABLE_TO_MAP_FASTQ_TWO_REPORT_NAME);

			File probeDetailsFile = new File(resultsDirectory, prefixName + ReportManager.DETAILS_REPORT_NAME);
			File prefuppSummaryFile = new File(resultsDirectory, prefixName + ReportManager.SUMMARY_REPORT_NAME);

			SimpleMapper<Probe> extensionMapper = extensionMapperByProbeFileName.get(probeFiles.get(i));
			SimpleMapper<Probe> ligationMapper = ligationMapperByProbeFileName.get(probeFiles.get(i));

			analyzeReads(projectOverviewFile, probeDetailsFile, unmappedFastqOneFile, unmappedFastqTwoFile, prefuppSummaryFile, extensionMapper, ligationMapper, new File(resultsDirectory, prefixName
					+ "_unmapped_unassigned_read_ones.txt"), new File(resultsDirectory, prefixName + "_unmapped_unassigned_read_twos.txt"), new File(resultsDirectory, prefixName
					+ "_probe_breakdown.txt"), new File(resultsDirectory, prefixName + "_read_pairs_pie_charts.pdf"));

			tallyRepeatSequences(unmappedFastqOneFile, new File(unmappedFastqOneFile.getAbsolutePath() + "_counts.txt"));
			tallyRepeatSequences(unmappedFastqTwoFile, new File(unmappedFastqTwoFile.getAbsolutePath() + "_counts.txt"));

		}

	}

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

	static void analyzeReads(File outputDirectory, File probeDetailsFile, File fastqOneFile, File fastqTwoFile, File prefuppSummaryFile, SimpleMapper<Probe> extensionMapper,
			SimpleMapper<Probe> ligationMapper, File outputReadOneUnassignedToProbeFile, File outputReadTwoUnassignedToProbeFile, File probeBreakdownFile, File outputPieChartsFile) throws IOException {
		long start = System.currentTimeMillis();
		Map<String, ProbeBreakdown> probeIdToBreakdownMap = new HashMap<String, ProbeBreakdown>();

		TallyMap<Probe> readCountsByProbe = new TallyMap<Probe>();
		Set<String> unassignedReadNames = new LinkedHashSet<String>();
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

					TallyMap<Probe> probeTallyMap = extensionMapper.getReferenceTallyMap(readOneSequence);
					probeTallyMap.addAll(ligationMapper.getReferenceTallyMap(readOneSequence));

					probeTallyMap.addAll(extensionMapper.getReferenceTallyMap(readTwoSequence.getReverseCompliment()));
					probeTallyMap.addAll(ligationMapper.getReferenceTallyMap(readTwoSequence.getReverseCompliment()));

					Iterator<Probe> bestCandidates = probeTallyMap.getObjectsWithLargestCount().iterator();

					Probe fullyMappedProbe = null;
					while (fullyMappedProbe == null && bestCandidates.hasNext()) {
						Probe currentProbe = bestCandidates.next();
						ISequence extensionSequence = currentProbe.getExtensionPrimerSequence();
						ISequence ligationSequence = currentProbe.getLigationPrimerSequence();

						boolean readOneContainsExtension = matches(readOneSequence, extensionSequence);
						boolean readOneContainsLigation = matches(readOneSequence, ligationSequence);

						boolean readTwoContainsReverseComplimentExtension = matches(readTwoSequence, extensionSequence.getReverseCompliment());
						boolean readTwoContainsReverseComplimentLigation = matches(readTwoSequence, ligationSequence.getReverseCompliment());

						boolean fullyMappedProbeFound = (readOneContainsExtension && readOneContainsLigation && readTwoContainsReverseComplimentExtension && readTwoContainsReverseComplimentLigation);
						if (fullyMappedProbeFound) {
							fullyMappedProbe = currentProbe;
						}
					}

					if (fullyMappedProbe != null) {
						readCountsByProbe.add(fullyMappedProbe);
					} else {
						unassignedReadNames.add(readName);
					}
				}

			}
		}

		for (Probe probe : readCountsByProbe.getObjects()) {
			int fullyMappedCount = readCountsByProbe.getCount(probe);
			ProbeBreakdown probeBreakdown = new ProbeBreakdown();
			probeBreakdown.setUnmappedReadPairs(fullyMappedCount);
			probeIdToBreakdownMap.put(probe.getProbeId(), probeBreakdown);
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

		try (TabDelimitedFileWriter writer = new TabDelimitedFileWriter(outputReadOneUnassignedToProbeFile, new String[] { "sequence", "count" })) {
			for (Entry<ISequence, Integer> sequenceAndCount : unassignedReadOneTallies.getObjectsSortedFromMostTalliesToLeast()) {
				int count = sequenceAndCount.getValue();
				ISequence sequence = sequenceAndCount.getKey();
				writer.writeLine(sequence, count);
			}
		}

		try (TabDelimitedFileWriter writer = new TabDelimitedFileWriter(outputReadTwoUnassignedToProbeFile, new String[] { "sequence", "count" })) {
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
			if (probeBreakdown == null) {
				probeBreakdown = new ProbeBreakdown();
				probeBreakdown.setUnmappedReadPairs(0);
			}
			probeBreakdown.setUniqueMappedOnTargetReadPairs(totalUids);
			double totalReadPairs = totalUids * averageNumberOfReadPairsPerUid;
			probeBreakdown.setDuplicateMappedOnTargetReadPairs((int) (totalReadPairs - totalUids));
			probeIdToBreakdownMap.put(probeId, probeBreakdown);
		}
		String[] probeBreakdownHeader = new String[] { "probe_id", "unique_mapped_on_target_read_pairs", "duplicate_mapped_on_target_read_pairs", "mapped_on_target_read_pairs", "unmapped_read_pairs",
				"total_read_pairs" };

		try (TabDelimitedFileWriter writer = new TabDelimitedFileWriter(probeBreakdownFile, probeBreakdownHeader)) {
			for (Entry<String, ProbeBreakdown> entry : probeIdToBreakdownMap.entrySet()) {
				String probeId = entry.getKey();
				ProbeBreakdown probeBreakdown = entry.getValue();
				writer.writeLine(probeId, probeBreakdown.getUniqueMappedOnTargetReadPairs(), probeBreakdown.getDuplicateMappedOnTargetReadPairs(), probeBreakdown.getMappedOnTargetReadPairs(),
						probeBreakdown.getUnmappedReadPairs(), probeBreakdown.getTotalReadPairs());
			}
		}

		PieChart.generateVisualReport(outputDirectory + " PAIRED READS SUMMARY", prefuppSummaryFile, probeBreakdownFile, outputPieChartsFile);

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
