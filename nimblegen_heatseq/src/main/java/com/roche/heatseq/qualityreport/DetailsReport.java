package com.roche.heatseq.qualityreport;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import com.roche.heatseq.objects.Probe;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class DetailsReport {

	private final PrintWriter detailsReportWriter;

	private int duplicateReadPairsRemoved;
	private int probesWithNoMappedReadPairs;
	private int totalReadPairsAfterReduction;

	private int maxNumberOfUidsPerProbe;
	private int sumOfProbeDistinctUids;
	private int totalNonZeroProbes;
	private int totalProbes;
	private double sumOfAverageNumberOfReadPairsPerProbeUid;

	DetailsReport(File detailsReportFile) throws IOException {
		duplicateReadPairsRemoved = 0;
		probesWithNoMappedReadPairs = 0;
		totalReadPairsAfterReduction = 0;

		maxNumberOfUidsPerProbe = 0;
		sumOfProbeDistinctUids = 0;
		totalNonZeroProbes = 0;
		totalProbes = 0;

		sumOfAverageNumberOfReadPairsPerProbeUid = 0;

		FileUtil.createNewFile(detailsReportFile);
		detailsReportWriter = new PrintWriter(detailsReportFile);
		detailsReportWriter.println("probe_id" + StringUtil.TAB + "total_uids" + StringUtil.TAB + "average_number_of_read_pairs_per_uid" + StringUtil.TAB + "standard_deviation_of_read_pairs_per_uid"
				+ StringUtil.TAB + "min_read_pairs_per_uid" + StringUtil.TAB + "max_read_pairs_per_uid" + StringUtil.TAB + "uid_with_max_read_pairs" + StringUtil.TAB
				+ "total_duplicate_read_pairs_removed" + StringUtil.TAB + "total_read_pairs_after_duplicate_removal" + StringUtil.TAB + "on_target_duplicate_rate" + StringUtil.TAB
				+ "total_time_to_process_in_ms" + StringUtil.TAB + "unique_uid_nuclotide_composition" + StringUtil.TAB + "unique_uid_nuclotide_composition_by_position" + StringUtil.TAB
				+ "weighted_uid_nuclotide_composition" + StringUtil.TAB + "weighted_uid_nuclotide_composition_by_position");
		detailsReportWriter.flush();
	}

	public void writeEntry(ProbeProcessingStats probeProcessingStats) {
		duplicateReadPairsRemoved += probeProcessingStats.getTotalDuplicateReadPairsRemoved();
		if (probeProcessingStats.getTotalUids() == 0) {
			probesWithNoMappedReadPairs++;
		}
		totalReadPairsAfterReduction += probeProcessingStats.getTotalReadPairsRemainingAfterReduction();
		maxNumberOfUidsPerProbe = Math.max(maxNumberOfUidsPerProbe, probeProcessingStats.getTotalUids());
		sumOfProbeDistinctUids += probeProcessingStats.getTotalUids();

		sumOfAverageNumberOfReadPairsPerProbeUid += probeProcessingStats.getAverageNumberOfReadPairsPerUid();

		detailsReportWriter.print(probeProcessingStats.toReportString());
		detailsReportWriter.flush();
		totalProbes++;
		totalNonZeroProbes++;
	}

	public void writeBlankEntry(Probe probe) {
		probesWithNoMappedReadPairs++;
		detailsReportWriter.print(probe.getProbeId() + StringUtil.TAB + 0 + StringUtil.TAB + 0 + StringUtil.TAB + "NaN" + StringUtil.TAB + "0" + StringUtil.TAB + "0" + StringUtil.TAB + "0"
				+ StringUtil.TAB + "" + StringUtil.TAB + "0" + StringUtil.TAB + "0" + StringUtil.TAB + "0:00:00" + StringUtil.TAB + "" + StringUtil.TAB + "");
		detailsReportWriter.flush();
		totalProbes++;
	}

	public int getDuplicateReadPairsRemoved() {
		return duplicateReadPairsRemoved;
	}

	public int getProbesWithNoMappedReadPairs() {
		return probesWithNoMappedReadPairs;
	}

	public int getTotalReadPairsAfterReduction() {
		return totalReadPairsAfterReduction;
	}

	public int getMaxNumberOfUidsPerProbe() {
		return maxNumberOfUidsPerProbe;
	}

	public double getAverageNumberOfReadPairsPerProbeUid() {
		double averageNumberOfReadPairsPerUid = (double) sumOfAverageNumberOfReadPairsPerProbeUid / (double) totalNonZeroProbes;
		return averageNumberOfReadPairsPerUid;
	}

	public double getAverageNumberOfUidsPerProbeWithAssignedReads() {
		double averageNumberOfUids = (double) sumOfProbeDistinctUids / (double) totalNonZeroProbes;
		return averageNumberOfUids;
	}

	public double getAverageNumberOfUidsPerProbe() {
		double averageNumberOfUids = (double) sumOfProbeDistinctUids / (double) totalProbes;
		return averageNumberOfUids;
	}

	void close() {
		detailsReportWriter.close();
	}

}
