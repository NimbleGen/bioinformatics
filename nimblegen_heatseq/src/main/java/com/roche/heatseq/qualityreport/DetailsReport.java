package com.roche.heatseq.qualityreport;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import com.roche.heatseq.objects.Probe;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class DetailsReport {

	private final PrintWriter detailsReportWriter;

	public DetailsReport(File detailsReportFile) throws IOException {
		FileUtil.createNewFile(detailsReportFile);
		detailsReportWriter = new PrintWriter(detailsReportFile);
		detailsReportWriter.println("probe_id" + StringUtil.TAB + "chromosome" + StringUtil.TAB + "target_start" + StringUtil.TAB + "target_stop" + StringUtil.TAB + "probe_strand" + StringUtil.TAB
				+ "total_uids" + StringUtil.TAB + "average_number_of_read_pairs_per_uid" + StringUtil.TAB + "standard_deviation_of_read_pairs_per_uid" + StringUtil.TAB + "min_read_pairs_per_uid"
				+ StringUtil.TAB + "max_read_pairs_per_uid" + StringUtil.TAB + "uid_with_max_read_pairs" + StringUtil.TAB + "total_duplicate_read_pairs_removed" + StringUtil.TAB
				+ "total_read_pairs_after_duplicate_removal" + StringUtil.TAB + "total_time_to_process_in_Ms");
		detailsReportWriter.flush();
	}

	public void writeEntry(ProbeProcessingStats probeProcessingStats) {
		detailsReportWriter.print(probeProcessingStats.toReportString());
		detailsReportWriter.flush();
	}

	public void writeBlankEntry(Probe probe) {
		detailsReportWriter.print(probe.getProbeId() + StringUtil.TAB + probe.getSequenceName() + StringUtil.TAB + probe.getCaptureTargetStart() + StringUtil.TAB + probe.getCaptureTargetStop()
				+ StringUtil.TAB + probe.getProbeStrand().getSymbol() + StringUtil.TAB + 0 + StringUtil.TAB + 0 + StringUtil.TAB + "NaN" + StringUtil.TAB + "0" + StringUtil.TAB + "0" + StringUtil.TAB
				+ "" + StringUtil.TAB + "0" + StringUtil.TAB + "0" + StringUtil.TAB + "0:00:00");
		detailsReportWriter.flush();
	}

	public void close() {
		detailsReportWriter.close();
	}

}
