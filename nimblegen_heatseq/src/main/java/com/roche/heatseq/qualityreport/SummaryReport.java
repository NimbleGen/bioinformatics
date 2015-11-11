/*
 *    Copyright 2013 Roche NimbleGen Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.roche.heatseq.qualityreport;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.TabDelimitedFileWriter;

public class SummaryReport {

	private final TabDelimitedFileWriter detailsReportWriter;

	private final String sampleName;

	private int totalReads = 0;
	private int totalReadPairs = 0;
	private int totalFullyMappedOffTargetReadPairs = 0;
	private int totalPartiallyMappedReadPairs = 0;
	private int totalFullyUnmappedReadPairs;
	private int totalFullyMappedOnTargetReadPairs = 0;

	private int duplicateReadPairsRemoved;

	private int totalProbes;
	private int probesWithNoMappedReadPairs;
	private int totalReadPairsAfterReduction;

	private int unpairedReads;

	private int distinctUidsFound;

	private double averageUidsPerProbeWithReads;
	private double averageUidsPerProbe;
	private int maxUidsPerProbe;

	private double averageNumberOfReadPairsPerProbeUid;

	SummaryReport(String softwareName, String softwareVersion, String sampleName, File summaryReportFile) throws IOException {
		this.sampleName = sampleName;

		String preHeader = "#software_name=" + softwareName + " software_version=" + softwareVersion;

		FileUtil.createNewFile(summaryReportFile);
		String[] header = new String[] { "sample_prefix", "input_read_pairs", "pairs_with_both_reads_mapped", "pairs_with_both_reads_unmapped", "pairs_with_only_one_read_mapped",
				"pairs_with_on-target_reads", "pct_pairs_with_on-target_reads", "pairs_with_off-target_reads", "pct_pairs_with_off-target_reads", "duplicate_read_pairs_removed", "probes",
				"probes_with_no_mapped_read_pairs", "unique_read_pairs", "distinct_uids_found", "average_uids_per_probe", "average_uids_per_probes_with_reads", "max_uids_per_probe",
				"average_read_pairs_per_uid", "unpaired_reads", "input_reads" };
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

	public void setTotalFullyMappedOffTargetReadPairs(int totalFullyMappedOffTargetReadPairs) {
		this.totalFullyMappedOffTargetReadPairs = totalFullyMappedOffTargetReadPairs;
	}

	public void setTotalPartiallyMappedReadPairs(int totalPartiallyMappedReadPairs) {
		this.totalPartiallyMappedReadPairs = totalPartiallyMappedReadPairs;
	}

	public void setTotalFullyUnmappedReads(int totalFullyUnmappedReadPairs) {
		this.totalFullyUnmappedReadPairs = totalFullyUnmappedReadPairs;
	}

	public void setTotalFullyMappedOnTargetReadPairs(int totalFullyMappedOnTargetReadPairs) {
		this.totalFullyMappedOnTargetReadPairs = totalFullyMappedOnTargetReadPairs;
	}

	public void setTotalReads(int totalReads) {
		this.totalReads = totalReads;
	}

	public void setTotalReadPairs(int totalReadPairs) {
		this.totalReadPairs = totalReadPairs;
	}

	public void setUnpairedReads(int unpairedReads) {
		this.unpairedReads = unpairedReads;
	}

	void close() {
		DecimalFormat formatter = new DecimalFormat("0.0000");

		String inputReadPairs = "" + (totalReadPairs);
		String inputReads = "" + totalReads;
		int numberOfPairsWithBothReadsMapped = (totalFullyMappedOffTargetReadPairs + totalFullyMappedOnTargetReadPairs);
		String pairsWithBothReadsMapped = "" + numberOfPairsWithBothReadsMapped;
		String pairsWithOnlyOneReadMapped = "" + totalPartiallyMappedReadPairs;
		String pairsWithBothReadsUnmapped = "" + totalFullyUnmappedReadPairs;
		int numberOfPairsWithOnTargetReads = totalFullyMappedOnTargetReadPairs;

		String pairsWithOnTargetReads = "" + numberOfPairsWithOnTargetReads;

		int numberOfPairsWithOffTargetReads = totalFullyMappedOffTargetReadPairs;

		String percentPairsWithOffTargetReads = formatter.format(((double) numberOfPairsWithOffTargetReads / (double) numberOfPairsWithBothReadsMapped) * 100);
		String percentPairsWithOnTargetReads = formatter.format(((double) numberOfPairsWithOnTargetReads / (double) numberOfPairsWithBothReadsMapped) * 100);
		String uniqueReadpairs = "" + totalReadPairsAfterReduction;
		String averageUidsPerProbesWithReads = "" + formatter.format(averageUidsPerProbeWithReads);
		String averageReadPairsPerUid = "" + formatter.format(averageNumberOfReadPairsPerProbeUid);
		detailsReportWriter.writeLine(sampleName, inputReadPairs, pairsWithBothReadsMapped, pairsWithBothReadsUnmapped, pairsWithOnlyOneReadMapped, pairsWithOnTargetReads,
				percentPairsWithOnTargetReads, numberOfPairsWithOffTargetReads, percentPairsWithOffTargetReads, duplicateReadPairsRemoved, totalProbes, probesWithNoMappedReadPairs, uniqueReadpairs,
				distinctUidsFound, formatter.format(averageUidsPerProbe), averageUidsPerProbesWithReads, maxUidsPerProbe, averageReadPairsPerUid, unpairedReads, inputReads);
		detailsReportWriter.close();
	}

}
