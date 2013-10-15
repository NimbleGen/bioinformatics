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
		detailsReportWriter.println("processing_time(HH:MM:SS)" + StringUtil.TAB + "total_input_reads" + StringUtil.TAB + "input_unmapped_reads" + StringUtil.TAB + "input_mapped_reads"
				+ StringUtil.TAB + "duplicate_read_pairs_removed" + StringUtil.TAB + "read_pairs_assigned_to_multiple_probes" + StringUtil.TAB + "probes_with_no_mapped_read_pairs" + StringUtil.TAB
				+ "total_probes" + StringUtil.TAB + "total_read_pairs_after_reduction" + StringUtil.TAB + "distinct_uids_found" + StringUtil.TAB + "possible_unique_uids_of_length_" + uidLength
				+ StringUtil.TAB + "uid_ratio" + StringUtil.TAB + "average_uids_per_probe" + StringUtil.TAB + "average_uids_per_probes_with_reads" + StringUtil.TAB + "max_uids_per_probe"
				+ StringUtil.TAB + "average_read_pairs_per_probe_uid" + StringUtil.TAB + "on-target_duplicate_rate" + StringUtil.TAB + "probe-specific_reads" + StringUtil.TAB
				+ "probe-specific_to_total_reads_ratio" + StringUtil.TAB + "unique_uid_nucleotide_composition" + StringUtil.TAB + "unique_uid_nucleotide_composition_by_base" + StringUtil.TAB
				+ "weighted_uid_nucleotide_composition" + StringUtil.TAB + "weighted_uid_nucleotide_composition_by_base");
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
		detailsReportWriter.println(DateUtil.convertMillisecondsToHHMMSS(processingTimeInMs) + StringUtil.TAB + (mappedReads + unmappedReads) + StringUtil.TAB + unmappedReads + StringUtil.TAB
				+ mappedReads + StringUtil.TAB + duplicateReadPairsRemoved + StringUtil.TAB + readPairsAssignedToMultipleProbes + StringUtil.TAB + probesWithNoMappedReadPairs + StringUtil.TAB
				+ totalProbes + StringUtil.TAB + totalReadPairsAfterReduction + StringUtil.TAB + distinctUidsFound + StringUtil.TAB + theoreticalUniqueUids + StringUtil.TAB
				+ formatter.format(uidRatio) + StringUtil.TAB + formatter.format(averageUidsPerProbe) + StringUtil.TAB + formatter.format(averageUidsPerProbeWithReads) + StringUtil.TAB
				+ maxUidsPerProbe + StringUtil.TAB + formatter.format(averageNumberOfReadPairsPerProbeUid) + StringUtil.TAB + formatter.format(onTargetDuplicateRate) + StringUtil.TAB
				+ probeSpecificReads + StringUtil.TAB + formatter.format(probeSpecificToTotalReadsRatio) + StringUtil.TAB + uidComposition + StringUtil.TAB + uidCompositionByBase + StringUtil.TAB
				+ weightedUidComposition + StringUtil.TAB + weightedUidCompositionByBase);
		detailsReportWriter.close();
	}

}
