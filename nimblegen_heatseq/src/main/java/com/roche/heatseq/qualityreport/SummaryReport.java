package com.roche.heatseq.qualityreport;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.TabDelimitedFileWriter;

public class SummaryReport {

	private final TabDelimitedFileWriter detailsReportWriter;

	private final int extensionUidLength;
	private final int ligationUidLength;

	private final String sampleName;

	private int totalReads = 0;
	private int totalFullyMappedOffTargetReads = 0;
	private int totalPartiallyMappedReads = 0;
	private int totalFullyUnmappedReads;
	private int totalFullyMappedOnTargetReads = 0;

	private int duplicateReadPairsRemoved;

	private int totalProbes;
	private int probesWithNoMappedReadPairs;
	private int totalReadPairsAfterReduction;

	private int distinctUidsFound;

	private double averageUidsPerProbeWithReads;
	private double averageUidsPerProbe;
	private int maxUidsPerProbe;

	private double averageNumberOfReadPairsPerProbeUid;

	SummaryReport(String softwareName, String softwareVersion, String sampleName, File summaryReportFile, int extensionUidLength, int ligationUidLength) throws IOException {
		this.extensionUidLength = extensionUidLength;
		this.ligationUidLength = ligationUidLength;
		this.sampleName = sampleName;

		String preHeader = "#software_name=" + softwareName + " software_version=" + softwareVersion;

		FileUtil.createNewFile(summaryReportFile);
		String[] header = new String[] { "sample_prefix", "input_read_pairs", "pairs_with_both_reads_mapped", "pairs_with_both_reads_unmapped", "pairs_with_only_one_read_mapped",
				"pairs_with_on-target_reads", "pct_pairs_with_on-target_reads", "pairs_with_off-target_reads", "pct_pairs_with_off-target_reads", "duplicate_read_pairs_removed", "probes",
				"probes_with_no_mapped_read_pairs", "unique_read_pairs", "theoretical_unique_uids_of_length_" + (extensionUidLength + ligationUidLength), "distinct_uids_found",
				"pct_distinct_uids_of_theoretical", "average_uids_per_probe", "average_uids_per_probes_with_reads", "max_uids_per_probe", "average_read_pairs_per_uid" };
		detailsReportWriter = new TabDelimitedFileWriter(summaryReportFile, preHeader, header);
	}

	public void setDuplicateReadPairsRemoved(int duplicateReadPairsRemoved) {
		this.duplicateReadPairsRemoved = duplicateReadPairsRemoved;
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

		String inputReadPairs = "" + (totalReads / 2);
		int numberOfPairsWithBothReadsMapped = ((totalFullyMappedOffTargetReads + totalFullyMappedOnTargetReads) / 2);
		String pairsWithBothReadsMapped = "" + numberOfPairsWithBothReadsMapped;
		String pairsWithOnlyOneReadMapped = "" + totalPartiallyMappedReads / 2;
		String pairsWithBothReadsUnmapped = "" + totalFullyUnmappedReads / 2;
		int numberOfPairsWithOnTargetReads = totalFullyMappedOnTargetReads / 2;
		String pairsWithOnTargetReads = "" + numberOfPairsWithOnTargetReads;
		String percentPairsWithOnTargetReads = formatter.format(((double) numberOfPairsWithOnTargetReads / (double) numberOfPairsWithBothReadsMapped) * 100);
		int numberOfPairsWithOffTargetReads = totalFullyMappedOffTargetReads / 2;
		String pairsWithOffTargetReads = "" + numberOfPairsWithOffTargetReads;
		String percentPairsWithOffTargetReads = formatter.format(((double) numberOfPairsWithOffTargetReads / (double) numberOfPairsWithBothReadsMapped) * 100);
		String uniqueReadpairs = "" + totalReadPairsAfterReduction;
		String theoreticalUniqueUidsOfLength = "" + theoreticalUniqueUids;
		String percentDistinctUidsOfTheoretical = "" + formatter.format(uidRatio * 100);
		String averageUidsPerProbesWithReads = "" + formatter.format(averageUidsPerProbeWithReads);
		String averageReadPairsPerUid = "" + formatter.format(averageNumberOfReadPairsPerProbeUid);
		detailsReportWriter.writeLine(sampleName, inputReadPairs, pairsWithBothReadsMapped, pairsWithBothReadsUnmapped, pairsWithOnlyOneReadMapped, pairsWithOnTargetReads,
				percentPairsWithOnTargetReads, pairsWithOffTargetReads, percentPairsWithOffTargetReads, duplicateReadPairsRemoved, totalProbes, probesWithNoMappedReadPairs, uniqueReadpairs,
				theoreticalUniqueUidsOfLength, distinctUidsFound, percentDistinctUidsOfTheoretical, formatter.format(averageUidsPerProbe), averageUidsPerProbesWithReads, maxUidsPerProbe,
				averageReadPairsPerUid);
		detailsReportWriter.close();
	}
}
