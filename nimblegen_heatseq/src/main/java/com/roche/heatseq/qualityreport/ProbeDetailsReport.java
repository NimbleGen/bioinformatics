package com.roche.heatseq.qualityreport;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class ProbeDetailsReport {

	private final PrintWriter detailsReportWriter;

	private int duplicateReadPairsRemoved;
	private int probesWithNoMappedReadPairs;
	private int totalReadPairsAfterReduction;

	private int maxNumberOfUidsPerProbe;
	private int sumOfProbeDistinctUids;
	private int totalNonZeroProbes;
	private int totalProbes;
	private double sumOfAverageNumberOfReadPairsPerProbeUid;

	ProbeDetailsReport(File detailsReportFile) throws IOException {
		duplicateReadPairsRemoved = 0;
		probesWithNoMappedReadPairs = 0;
		totalReadPairsAfterReduction = 0;

		maxNumberOfUidsPerProbe = 0;
		sumOfProbeDistinctUids = 0;
		totalNonZeroProbes = 0;
		totalProbes = 0;

		sumOfAverageNumberOfReadPairsPerProbeUid = 0;

		if (detailsReportFile != null) {
			FileUtil.createNewFile(detailsReportFile);
			detailsReportWriter = new PrintWriter(detailsReportFile);
			detailsReportWriter.println("probe_id" + StringUtil.TAB + "total_read_pairs" + StringUtil.TAB + "total_read_pairs_after_duplicate_removal" + StringUtil.TAB
					+ "total_duplicate_read_pairs_removed" + StringUtil.TAB + "pct_duplicate_rate" + StringUtil.TAB + "average_number_of_read_pairs_per_uid" + StringUtil.TAB
					+ "max_read_pairs_per_uid" + StringUtil.TAB + "uid_with_max_read_pairs" + StringUtil.TAB + "unable_to_extend_primers");
			detailsReportWriter.flush();
		} else {
			detailsReportWriter = null;
		}
	}

	public void writeEntry(ProbeProcessingStats probeProcessingStats) {
		duplicateReadPairsRemoved += probeProcessingStats.getTotalDuplicateReadPairsRemoved();
		if (probeProcessingStats.getTotalUids() == 0) {
			probesWithNoMappedReadPairs++;
		} else {
			totalNonZeroProbes++;
		}
		totalReadPairsAfterReduction += probeProcessingStats.getTotalReadPairsRemainingAfterReduction();
		maxNumberOfUidsPerProbe = Math.max(maxNumberOfUidsPerProbe, probeProcessingStats.getTotalUids());
		sumOfProbeDistinctUids += probeProcessingStats.getTotalUids();

		sumOfAverageNumberOfReadPairsPerProbeUid += probeProcessingStats.getAverageNumberOfReadPairsPerUid();

		if (detailsReportWriter != null) {
			detailsReportWriter.print(probeProcessingStats.toReportString() + StringUtil.NEWLINE);
			detailsReportWriter.flush();
		}

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
		if (detailsReportWriter != null) {
			detailsReportWriter.close();
		}
	}

}
