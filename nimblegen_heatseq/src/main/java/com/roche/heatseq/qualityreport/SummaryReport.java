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

	private final int extensionUidLength;
	private final int ligationUidLength;

	private long processingTimeInMs;

	private int totalReads = 0;
	private int totalFullyMappedOffTargetReads = 0;
	private int totalPartiallyMappedReads = 0;
	private int totalFullyUnmappedReads;
	private int totalFullyMappedOnTargetReads = 0;

	private int duplicateReadPairsRemoved;

	private int totalProbes;
	private int probesWithNoMappedReadPairs;
	private int totalReadPairsAfterReduction;

	private int readPairsAssignedToMultipleProbes;

	private int distinctUidsFound;

	private double averageUidsPerProbeWithReads;
	private double averageUidsPerProbe;
	private int maxUidsPerProbe;

	private double averageNumberOfReadPairsPerProbeUid;

	private final String softwareName;
	private final String softwareVersion;

	SummaryReport(String softwareName, String softwareVersion, File summaryReportFile, int extensionUidLength, int ligationUidLength) throws IOException {
		this.extensionUidLength = extensionUidLength;
		this.ligationUidLength = ligationUidLength;
		this.softwareName = softwareName;
		this.softwareVersion = softwareVersion;
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

	public void setTotalFullyMappedOffTargetReads(int totalFullyMappedOffTargetReads) {
		this.totalFullyMappedOffTargetReads = totalFullyMappedOffTargetReads;
	}

	public void setTotalPartiallyMappedReads(int totalPartiallyMappedReads) {
		this.totalPartiallyMappedReads = totalPartiallyMappedReads;
	}

	public void setTotalFullyUnmappedReads(int totalFullyUnmappedReads) {
		this.totalFullyUnmappedReads = totalFullyUnmappedReads;
	}

	public void setTotalFullyMappedOnTargetReads(int totalFullyMappedOnTargetReads) {
		this.totalFullyMappedOnTargetReads = totalFullyMappedOnTargetReads;
	}

	public void setTotalReads(int totalReads) {
		this.totalReads = totalReads;
	}

	void close() {
		long theoreticalUniqueUids = Math.round(Math.pow(4, extensionUidLength + ligationUidLength));
		double uidRatio = (double) distinctUidsFound / (double) theoreticalUniqueUids;
		DecimalFormat formatter = new DecimalFormat("0.0000");
		int onTargetReadPairs = (totalFullyMappedOnTargetReads / 2);
		double onTargetDuplicateRate = (double) duplicateReadPairsRemoved / (double) (onTargetReadPairs);
		int probeSpecificReads = (duplicateReadPairsRemoved + totalReadPairsAfterReduction) * 2;
		double probeSpecificToTotalReadsRatio = (double) probeSpecificReads / (double) (totalReads);

		double percentMappedReadsOnTarget = (double) totalFullyMappedOnTargetReads / (double) (totalFullyMappedOffTargetReads + totalFullyMappedOnTargetReads);

		detailsReportWriter.println(softwareName + "_version" + StringUtil.TAB + softwareVersion);
		detailsReportWriter.println("processing_time(HH:MM:SS)" + StringUtil.TAB + DateUtil.convertMillisecondsToHHMMSS(processingTimeInMs));
		detailsReportWriter.println("total_input_reads" + StringUtil.TAB + (totalReads));
		detailsReportWriter.println("input_fully_unmapped_reads" + StringUtil.TAB + totalFullyUnmappedReads);
		detailsReportWriter.println("input_fully_mapped_reads" + StringUtil.TAB + (totalFullyMappedOffTargetReads + totalFullyMappedOnTargetReads));
		detailsReportWriter.println("input_partially_mapped_reads" + StringUtil.TAB + totalPartiallyMappedReads);
		detailsReportWriter.println("on_target_reads" + StringUtil.TAB + totalFullyMappedOnTargetReads);
		detailsReportWriter.println("off_target_fully_mapped_reads" + StringUtil.TAB + totalFullyMappedOffTargetReads);
		detailsReportWriter.println("percent_fully_mapped_reads_on_target" + StringUtil.TAB + formatter.format(percentMappedReadsOnTarget));
		detailsReportWriter.println("duplicate_read_pairs_removed" + StringUtil.TAB + duplicateReadPairsRemoved);
		detailsReportWriter.println("read_pairs_assigned_to_multiple_probes" + StringUtil.TAB + readPairsAssignedToMultipleProbes);
		detailsReportWriter.println("probes_with_no_mapped_read_pairs" + StringUtil.TAB + probesWithNoMappedReadPairs);
		detailsReportWriter.println("total_probes" + StringUtil.TAB + totalProbes);
		detailsReportWriter.println("total_read_pairs_after_reduction" + StringUtil.TAB + totalReadPairsAfterReduction);
		detailsReportWriter.println("distinct_uids_found" + StringUtil.TAB + distinctUidsFound);
		detailsReportWriter.println("possible_unique_uids_of_length_" + (extensionUidLength + ligationUidLength) + StringUtil.TAB + theoreticalUniqueUids);
		detailsReportWriter.println("uid_ratio" + StringUtil.TAB + formatter.format(uidRatio));
		detailsReportWriter.println("average_uids_per_probe" + StringUtil.TAB + formatter.format(averageUidsPerProbe));
		detailsReportWriter.println("average_uids_per_probes_with_reads" + StringUtil.TAB + formatter.format(averageUidsPerProbeWithReads));
		detailsReportWriter.println("max_uids_per_probe" + StringUtil.TAB + maxUidsPerProbe);
		detailsReportWriter.println("average_read_pairs_per_probe_uid" + StringUtil.TAB + formatter.format(averageNumberOfReadPairsPerProbeUid));
		detailsReportWriter.println("on-target_duplicate_rate" + StringUtil.TAB + formatter.format(onTargetDuplicateRate));
		detailsReportWriter.println("probe-specific_reads" + StringUtil.TAB + probeSpecificReads);
		detailsReportWriter.println("probe-specific_to_total_reads_ratio" + StringUtil.TAB + formatter.format(probeSpecificToTotalReadsRatio));
		detailsReportWriter.close();
	}
}
