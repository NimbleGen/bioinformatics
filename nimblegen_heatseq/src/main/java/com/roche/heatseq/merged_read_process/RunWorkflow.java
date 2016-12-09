package com.roche.heatseq.merged_read_process;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.roche.heatseq.objects.ParsedProbeFile;
import com.roche.heatseq.utils.ProbeFileUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.Timer;

public class RunWorkflow {

	public static void main(String[] args) {
		run1();
		// run4();

	}

	public static void run1() {
		int startingRead = 0;
		Integer numberOfReadsToProcess = 1004000;

		// inputs
		File fastqOneFile = new File("D:\\kurts_space\\hsq_with_pete\\1\\Batch1_Capture1_NA12878_L001_R1_001.fastq.gz");
		File fastqTwoFile = new File("D:\\kurts_space\\hsq_with_pete\\1\\Batch1_Capture1_NA12878_L001_R2_001.fastq.gz");
		File probeFile = new File("D:\\kurts_space\\hsq_with_pete\\1\\151020_HG19_Onco_Genes_HSQ_probe_info.txt");

		File outputDir = new File("D:\\kurts_space\\hsq_with_pete\\1\\new_results\\start" + startingRead + "_numreads" + numberOfReadsToProcess + "\\");
		runWorkflow(startingRead, numberOfReadsToProcess, fastqOneFile, fastqTwoFile, probeFile, outputDir);
	}

	public static void run4() {
		int startingRead = 0;
		Integer numberOfReadsToProcess = null;

		// inputs
		File fastqOneFile = new File("D:\\kurts_space\\hsq_with_pete\\4\\607387-750ng-6hr_merged_TGATAT_L001_R1_001.fastq");
		File fastqTwoFile = new File("D:\\kurts_space\\hsq_with_pete\\4\\607387-750ng-6hr_merged_TGATAT_L001_R2_001.fastq");
		File probeFile = new File("D:\\kurts_space\\hsq_with_pete\\4\\150109_HG38_Filtered_Exome_HSQ_HX1_probe_info.txt");

		File outputDir = new File("D:\\kurts_space\\hsq_with_pete\\4\\start" + startingRead + "_numreads" + numberOfReadsToProcess + "\\");
		runWorkflow(startingRead, numberOfReadsToProcess, fastqOneFile, fastqTwoFile, probeFile, outputDir);
	}

	public static void runWorkflow(int startingRead, Integer numberOfReadsToProcess, File fastqOneFile, File fastqTwoFile, File probeFile, File outputDir) {

		String programName = "program_name";
		String programVersion = "program_version";
		String commandLineSignature = "command_line";
		DedupApproachEnum dedupApproach = DedupApproachEnum.BEST_READ;
		boolean trimPrimers = true;

		// output
		File mergedFile = new File(outputDir, "merged.fastq");
		File outputUnassignedFastq = new File(outputDir, "unnasigned.fastq");
		File outputUniqueFastq = new File(outputDir, "unique.fastq");
		File outputDuplicateFastq = new File(outputDir, "duplicate.fastq");
		File outputBam = new File(outputDir, "result.bam");

		try {
			FileUtil.createNewFile(mergedFile);
			FileUtil.createNewFile(outputUniqueFastq);
			FileUtil.createNewFile(outputDuplicateFastq);
			FileUtil.createNewFile(outputBam);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		Timer timer = new Timer();
		timer.start("whole workflow");
		timer.start("parse probe info");
		ParsedProbeFile parsedProbeFile;
		try {
			parsedProbeFile = ProbeFileUtil.parseProbeInfoFile(probeFile);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		timer.stop("parse probe info");

		timer.start("merging reads");
		System.out.println("Merging Reads.");
		ReadMerger.mergeReads(fastqOneFile, fastqTwoFile, mergedFile, startingRead, numberOfReadsToProcess, 0, null);
		timer.stop("merging reads");

		timer.start("assigning probes to reads");
		System.out.println("Assigning Reads to Probe.");
		Map<String, ProbeAssignment> readNameToProbeAssignmentMap = ProbeAssigner.assignReadsToProbes(mergedFile, parsedProbeFile, null);
		timer.stop("assigning probes to reads");

		timer.start("correcting uids");
		System.out.println("Correcting Uids");
		// this will populate the uid group when possible
		readNameToProbeAssignmentMap = UidCorrector.correctUids(readNameToProbeAssignmentMap, null);
		timer.stop("correcting uids");

		timer.start("dedup");
		System.out.println("deduping.");
		Deduper.dedup(mergedFile, readNameToProbeAssignmentMap, parsedProbeFile, dedupApproach, trimPrimers, outputUnassignedFastq, outputUniqueFastq, outputDuplicateFastq, outputBam,
				commandLineSignature, programName, programVersion, null);
		timer.stop("dedup");
		timer.stop("whole workflow");
		System.out.println(timer.getSummary());
	}

	public static void check() {

	}

}
