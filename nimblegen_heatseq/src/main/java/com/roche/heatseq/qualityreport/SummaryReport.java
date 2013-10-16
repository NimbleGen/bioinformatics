package com.roche.heatseq.qualityreport;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class SummaryReport {

	private final PrintWriter detailsReportWriter;

	private final int uidLength;

	private long processingTimeInMs;

	private int mappedReads;
	private int unmappedReads;

	private int duplicateReadPairsRemoved;

	private int totalProbes;
	private int probesWithNoMappedReadPairs;
	private int totalReadPairsAfterReduction;

	private int readPairsAssignedToMultipleProbes;

	private int distinctUidsFound;

	private double averageUidsPerProbeWithReads;
	private double averageUidsPerProbe;
	private int maxUidsPerProbe;

	private String uidCompositionByBase;
	private String uidComposition;

	private String weightedUidCompositionByBase;
	private String weightedUidComposition;

	private double averageNumberOfReadPairsPerProbeUid;

	public SummaryReport(File summaryReportFile, int uidLength) throws IOException {
		this.uidLength = uidLength;
		FileUtil.createNewFile(summaryReportFile);
		detailsReportWriter = new PrintWriter(summaryReportFile);
		detailsReportWriter.flush();
	}

	public void setProcessingTimeInMs(long processingTimeInMs) {
		this.processingTimeInMs = processingTimeInMs;
	}

	public void setDuplicateReadPairsRemoved(int duplicateReadPairsRemoved) {
		this.duplicateReadPairsRemoved = duplicateReadPairsRemoved;
	}

	public void setReadPairsAssignedToMultipleProbes(int readPairsAssignedToMultipleProbes) {
		this.readPairsAssignedToMultipleProbes = readPairsAssignedToMultipleProbes;
	}

	public void setMappedReads(int mappedReads) {
		this.mappedReads = mappedReads;
	}

	public void setUnmappedReads(int unmappedReads) {
		this.unmappedReads = unmappedReads;
	}

	public void setProbesWithNoMappedReadPairs(int probesWithNoMappedReadPairs) {
		this.probesWithNoMappedReadPairs = probesWithNoMappedReadPairs;
	}

	public void setTotalReadPairsAfterReduction(int totalReadPairsAssignedToProbes) {
		this.totalReadPairsAfterReduction = totalReadPairsAssignedToProbes;
	}

	public void setDistinctUidsFound(int distinctUidsFound) {
		this.distinctUidsFound = distinctUidsFound;
	}

	public void setTotalProbes(int totalProbes) {
		this.totalProbes = totalProbes;
	}

	public void setAverageUidsPerProbe(double averageUidsPerProbe) {
		this.averageUidsPerProbe = averageUidsPerProbe;
	}

	public void setAverageUidsPerProbeWithReads(double averageUidsPerProbeWithReads) {
		this.averageUidsPerProbeWithReads = averageUidsPerProbeWithReads;
	}

	public void setMaxUidsPerProbe(int maxUidsPerProbe) {
		this.maxUidsPerProbe = maxUidsPerProbe;
	}

	public void setAverageNumberOfReadPairsPerProbeUid(double averageNumberOfReadPairsPerUid) {
		this.averageNumberOfReadPairsPerProbeUid = averageNumberOfReadPairsPerUid;
	}

	public void setUidCompositionByBase(String uidCompositionByBase) {
		this.uidCompositionByBase = uidCompositionByBase;
	}

	public void setUidComposition(String weightedUidComposition) {
		this.weightedUidComposition = weightedUidComposition;
	}

	public void setWeightedUidCompositionByBase(String weightedUidCompositionByBase) {
		this.weightedUidCompositionByBase = weightedUidCompositionByBase;
	}

	public void setWeightedUidComposition(String uidComposition) {
		this.uidComposition = uidComposition;
	}

	public void close() {
		long theoreticalUniqueUids = Math.round(Math.pow(4, uidLength));
		double uidRatio = (double) distinctUidsFound / (double) theoreticalUniqueUids;
		DecimalFormat formatter = new DecimalFormat("0.0000");
		double onTargetDuplicateRate = (double) duplicateReadPairsRemoved / (double) (totalReadPairsAfterReduction + duplicateReadPairsRemoved);
		int probeSpecificReads = (duplicateReadPairsRemoved + totalReadPairsAfterReduction) * 2;
		double probeSpecificToTotalReadsRatio = (double) probeSpecificReads / (double) (mappedReads + unmappedReads);

		detailsReportWriter.println("processing_time(HH:MM:SS)" + StringUtil.TAB + DateUtil.convertMillisecondsToHHMMSS(processingTimeInMs));
		detailsReportWriter.println("total_input_reads" + StringUtil.TAB + (mappedReads + unmappedReads));
		detailsReportWriter.println("input_unmapped_reads" + StringUtil.TAB + unmappedReads);
		detailsReportWriter.println("input_mapped_reads" + StringUtil.TAB + mappedReads);
		detailsReportWriter.println("duplicate_read_pairs_removed" + StringUtil.TAB + duplicateReadPairsRemoved);
		detailsReportWriter.println("read_pairs_assigned_to_multiple_probes" + StringUtil.TAB + readPairsAssignedToMultipleProbes);
		detailsReportWriter.println("probes_with_no_mapped_read_pairs" + StringUtil.TAB + probesWithNoMappedReadPairs);
		detailsReportWriter.println("total_probes" + StringUtil.TAB + totalProbes);
		detailsReportWriter.println("total_read_pairs_after_reduction" + StringUtil.TAB + totalReadPairsAfterReduction);
		detailsReportWriter.println("distinct_uids_found" + StringUtil.TAB + distinctUidsFound);
		detailsReportWriter.println("possible_unique_uids_of_length_" + uidLength + StringUtil.TAB + theoreticalUniqueUids);
		detailsReportWriter.println("uid_ratio" + StringUtil.TAB + formatter.format(uidRatio));
		detailsReportWriter.println("average_uids_per_probe" + StringUtil.TAB + formatter.format(averageUidsPerProbe));
		detailsReportWriter.println("average_uids_per_probes_with_reads" + StringUtil.TAB + formatter.format(averageUidsPerProbeWithReads));
		detailsReportWriter.println("max_uids_per_probe" + StringUtil.TAB + maxUidsPerProbe);
		detailsReportWriter.println("average_read_pairs_per_probe_uid" + StringUtil.TAB + formatter.format(averageNumberOfReadPairsPerProbeUid));
		detailsReportWriter.println("on-target_duplicate_rate" + StringUtil.TAB + formatter.format(onTargetDuplicateRate));
		detailsReportWriter.println("probe-specific_reads" + StringUtil.TAB + probeSpecificReads);
		detailsReportWriter.println("probe-specific_to_total_reads_ratio" + StringUtil.TAB + formatter.format(probeSpecificToTotalReadsRatio));
		detailsReportWriter.println("unique_uid_nucleotide_composition" + StringUtil.TAB + uidComposition);
		detailsReportWriter.println("unique_uid_nucleotide_composition_by_base" + StringUtil.TAB + uidCompositionByBase);
		detailsReportWriter.println("weighted_uid_nucleotide_composition" + StringUtil.TAB + weightedUidComposition);
		detailsReportWriter.println("weighted_uid_nucleotide_composition_by_base" + StringUtil.TAB + weightedUidCompositionByBase);

		detailsReportWriter.close();
	}
}
