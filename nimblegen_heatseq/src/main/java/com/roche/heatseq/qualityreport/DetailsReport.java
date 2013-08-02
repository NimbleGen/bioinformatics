package com.roche.heatseq.qualityreport;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class DetailsReport {

	private final PrintWriter detailsReportWriter;

	public DetailsReport(File detailsReportFile) throws IOException {
		FileUtil.createNewFile(detailsReportFile);
		detailsReportWriter = new PrintWriter(detailsReportFile);
		detailsReportWriter.println("PREFUPP (Primer Read Extension and Filtering of Unique PCR Probes) PROCESSING DETAILS REPORT");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();

		detailsReportWriter.println(dateFormat.format(date));
		detailsReportWriter.println();
		detailsReportWriter.println("probe_index" + StringUtil.TAB + "probe_container_name" + StringUtil.TAB + "probes_capture_start" + StringUtil.TAB + "probe_capture_stop" + StringUtil.TAB
				+ "probe_strand" + StringUtil.TAB + "total_uids" + StringUtil.TAB + "average_number_of_read_pairs_per_uid" + StringUtil.TAB + "standard_deviation_of_read_pairs_per_uid"
				+ StringUtil.TAB + "min_number_of_read_pairs_per_uid" + StringUtil.TAB + "max_number_of_read_pairs_per_uid" + StringUtil.TAB + "uid_Of_entry_with_max_read_pairs" + StringUtil.TAB
				+ " total_duplicate_read_pairs_removed" + StringUtil.TAB + " totalReadPairsRemainingAfterReduction" + StringUtil.TAB + " totalTimeToProcessInMs");
		detailsReportWriter.flush();
	}

	public void writeEntry(ProbeProcessingStats probeProcessingStats) {
		detailsReportWriter.print(probeProcessingStats.toReportString());
		detailsReportWriter.flush();
	}

	public void close() {
		detailsReportWriter.close();
	}

}
