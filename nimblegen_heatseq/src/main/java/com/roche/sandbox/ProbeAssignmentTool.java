package com.roche.sandbox;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.objects.ProbesBySequenceName;
import com.roche.heatseq.process.ProbeFileUtil;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOption;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineOptionsGroup;
import com.roche.sequencing.bioinformatics.common.commandline.CommandLineParser;
import com.roche.sequencing.bioinformatics.common.commandline.ParsedCommandLine;
import com.roche.sequencing.bioinformatics.common.mapping.SimpleMapper;
import com.roche.sequencing.bioinformatics.common.utils.ManifestUtil;

public class ProbeAssignmentTool {

	private final static String APPLICATION_NAME = "Probe Assignment TOOL";
	private static String applicationVersionFromManifest;

	private final static CommandLineOption USAGE_OPTION = new CommandLineOption("Print Usage", "usage", 'h', "Print Usage.", false, true);
	private final static CommandLineOption FASTQ_ONE_OPTION = new CommandLineOption("fastQ One File", "r1", null, "path to first input fastq file", true, false);
	private final static CommandLineOption FASTQ_TWO_OPTION = new CommandLineOption("fastQ Two File", "r2", null, "path to second second input fastq file", true, false);
	private final static CommandLineOption PROBE_OPTION = new CommandLineOption("PROBE File", "probe", null, "The probe file", true, false);
	private final static CommandLineOption PREFUPP_SUMMARY_OPTION = new CommandLineOption("Prefupp Summary File", "prefuppSummary", null, "The prefupp summary file", true, false);
	private final static CommandLineOption OUTPUT_DIR_OPTION = new CommandLineOption("Output Directory", "outputDir", null, "location to store resultant files.", false, false);
	private final static CommandLineOption OUTPUT_FILE_PREFIX_OPTION = new CommandLineOption("Output File Prefix", "outputPrefix", null, "text to put at beginning of output file names", false, false);

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

			File fastQ1WithUidsFile = new File(parsedCommandLine.getOptionsValue(FASTQ_ONE_OPTION));

			if (!fastQ1WithUidsFile.exists()) {
				throw new IllegalStateException("Unable to find provided FASTQ1 file[" + fastQ1WithUidsFile.getAbsolutePath() + "].");
			}

			File fastQ2File = new File(parsedCommandLine.getOptionsValue(FASTQ_TWO_OPTION));

			if (!fastQ2File.exists()) {
				throw new IllegalStateException("Unable to find provided FASTQ2 file[" + fastQ2File.getAbsolutePath() + "].");
			}

			File probeFile = new File(parsedCommandLine.getOptionsValue(PROBE_OPTION));
			if (!probeFile.exists()) {
				throw new IllegalStateException("Unable to find provided PROBE file[" + probeFile.getAbsolutePath() + "].");
			}

			File prefuppSummaryFile = new File(parsedCommandLine.getOptionsValue(PREFUPP_SUMMARY_OPTION));
			if (!prefuppSummaryFile.exists()) {
				throw new IllegalStateException("Unable to find provided Prefupp Summary file[" + prefuppSummaryFile.getAbsolutePath() + "].");
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

			String outputFilePrefix = parsedCommandLine.getOptionsValue(OUTPUT_FILE_PREFIX_OPTION);
			if (outputFilePrefix == null) {
				outputFilePrefix = "";
			}

			assignProbesToReads(prefuppSummaryFile, probeFile, fastQ1WithUidsFile, fastQ2File, outputDirectory, outputFilePrefix);

		}

	}

	private static void assignProbesToReads(File prefuppSummaryFile, File probeFile, File fastqOneFile, File fastqTwoFile, File outputDirectory, String outputFilePrefix) throws IOException {

		SimpleMapper<Probe> extensionMapper = new SimpleMapper<Probe>(7, 2, 1, 5);
		SimpleMapper<Probe> ligationMapper = new SimpleMapper<Probe>(7, 2, 1, 5);
		ProbesBySequenceName probes = ProbeFileUtil.parseProbeInfoFile(probeFile);

		for (Probe probe : probes.getProbes()) {
			extensionMapper.addReferenceSequence(probe.getExtensionPrimerSequence(), probe);
			ligationMapper.addReferenceSequence(probe.getLigationPrimerSequence(), probe);
		}

		File outputReadOneUnassignedToProbeFile = new File(outputDirectory, outputFilePrefix + "unassigned_read_one.txt");
		File outputReadTwoUnassignedToProbeFile = new File(outputDirectory, outputFilePrefix + "unassigned_read_two.txt");
		File probeBreakdownFile = new File(outputDirectory, outputFilePrefix + "_probe_breakdown.txt");
		File outputPieChartsFile = new File(outputDirectory, outputFilePrefix + "_visual_probe_breakdown.pdf");
		FastqSequenceAnalyzer.analyzeReads(outputDirectory, probeFile, fastqOneFile, fastqTwoFile, prefuppSummaryFile, extensionMapper, ligationMapper, outputReadOneUnassignedToProbeFile,
				outputReadTwoUnassignedToProbeFile, probeBreakdownFile, outputPieChartsFile);

	}

	private static CommandLineOptionsGroup getCommandLineOptionsGroup() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup(APPLICATION_NAME + " (Version:" + applicationVersionFromManifest + ")");
		group.addOption(USAGE_OPTION);
		group.addOption(FASTQ_ONE_OPTION);
		group.addOption(FASTQ_TWO_OPTION);
		group.addOption(PROBE_OPTION);
		group.addOption(PREFUPP_SUMMARY_OPTION);
		group.addOption(OUTPUT_DIR_OPTION);
		group.addOption(OUTPUT_FILE_PREFIX_OPTION);
		return group;
	}

}
