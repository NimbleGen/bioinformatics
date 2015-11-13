/*
 *    Copyright 2016 Roche NimbleGen Inc.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.roche.heatseq.cli.HsqUtilsCli;
import com.roche.sequencing.bioinformatics.common.alignment.CigarStringUtil;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.TabDelimitedFileWriter;

public class ReportManager {

	private final static String DUPLICATE_MAPPINGS_REPORT_NAME = "duplicate_mappings.txt";

	public final static String PROBE_DETAILS_REPORT_NAME = "probe_details.txt";
	public final static String SUMMARY_REPORT_NAME = HsqUtilsCli.APPLICATION_NAME + "_" + HsqUtilsCli.DEDUPLICATION_COMMAND_NAME + "_summary.txt";
	private final static String UID_COMPOSITION_REPORT_NAME = "uid_composition_by_probe.txt";
	private final static String UNIQUE_PROBE_TALLIES_REPORT_NAME = "unique_probe_tallies.txt";
	private final static String READS_MAPPED_TO_MULTIPLE_PROBES_REPORT_NAME = "reads_mapped_to_multiple_probes.txt";
	private final static String PROBE_COVERAGE_REPORT_NAME = "probe_coverage.bed";
	// private final static String MAPPED_OFF_TARGET_READS_REPORT_NAME = "mapped_off_target_reads.bam";
	// private final static String UNMAPPED_READS_REPORT_NAME = "unmapped_read_pairs.bam";
	// private final static String PARTIALLY_MAPPED_READS_REPORT_NAME = "partially_mapped_read_pairs.bam";

	private TabDelimitedFileWriter ambiguousMappingWriter;
	private TabDelimitedFileWriter uniqueProbeTalliesWriter;
	private TabDelimitedFileWriter probeCoverageWriter;
	private TabDelimitedFileWriter uidCompositionByProbeWriter;
	private TabDelimitedFileWriter readsMappedToMultipleProbesWriter;

	private ProbeDetailsReport detailsReport;
	private SummaryReport summaryReport;

	private final List<TallyMap<Character>> ligationMismatchDetailsByIndex;
	private final List<TallyMap<Character>> extensionMismatchDetailsByIndex;

	private final List<Integer> numberOfLigationErrors;
	private final List<Integer> numberOfExtensionErrors;
	private final List<Integer> numberOfLigationInsertions;
	private final List<Integer> numberOfExtensionInsertions;
	private final List<Integer> numberOfLigationDeletions;
	private final List<Integer> numberOfExtensionDeletions;
	private final List<Integer> numberOfLigationGains;
	private final List<Integer> numberOfExtensionGains;

	public ReportManager(String softwareName, String softwareVersion, String sampleName, File outputDirectory, String outputFilePrefix, boolean shouldOutputReports) {

		ligationMismatchDetailsByIndex = new ArrayList<TallyMap<Character>>();
		extensionMismatchDetailsByIndex = new ArrayList<TallyMap<Character>>();
		numberOfLigationErrors = new ArrayList<Integer>();
		numberOfExtensionErrors = new ArrayList<Integer>();
		numberOfLigationInsertions = new ArrayList<Integer>();
		numberOfExtensionInsertions = new ArrayList<Integer>();
		numberOfLigationDeletions = new ArrayList<Integer>();
		numberOfExtensionDeletions = new ArrayList<Integer>();
		numberOfLigationGains = new ArrayList<Integer>();
		numberOfExtensionGains = new ArrayList<Integer>();

		// the summary file is produced regardless of shouldOutputReports
		File detailsReportFile = new File(outputDirectory, outputFilePrefix + PROBE_DETAILS_REPORT_NAME);
		try {
			FileUtil.createNewFile(detailsReportFile);
			detailsReport = new ProbeDetailsReport(detailsReportFile);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		File summaryReportFile = null;
		summaryReportFile = new File(outputDirectory, outputFilePrefix + SUMMARY_REPORT_NAME);
		try {
			FileUtil.createNewFile(summaryReportFile);
			summaryReport = new SummaryReport(softwareName, softwareVersion, sampleName, summaryReportFile);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		if (shouldOutputReports) {
			File ambiguousMappingFile = new File(outputDirectory, outputFilePrefix + DUPLICATE_MAPPINGS_REPORT_NAME);
			try {
				FileUtil.createNewFile(ambiguousMappingFile);
				ambiguousMappingWriter = new TabDelimitedFileWriter(ambiguousMappingFile, new String[] { "read_name", "read_string", "sequence_name", "extension_primer_start",
						"extension_primer_stop", "capture_target_start", "capture_target_stop", "ligation_primer_start", "ligation_primer_stop", "probe_strand" });
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File uniqueProbeTalliesFile = new File(outputDirectory, outputFilePrefix + UNIQUE_PROBE_TALLIES_REPORT_NAME);
			try {
				FileUtil.createNewFile(uniqueProbeTalliesFile);
				uniqueProbeTalliesWriter = new TabDelimitedFileWriter(uniqueProbeTalliesFile, new String[0]);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File probeCoverageFile = new File(outputDirectory, outputFilePrefix + PROBE_COVERAGE_REPORT_NAME);
			try {
				FileUtil.createNewFile(probeCoverageFile);
				probeCoverageWriter = new TabDelimitedFileWriter(probeCoverageFile, new String[0]);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File readsMappedToMultipleProbesFile = new File(outputDirectory, outputFilePrefix + READS_MAPPED_TO_MULTIPLE_PROBES_REPORT_NAME);
			try {
				FileUtil.createNewFile(readsMappedToMultipleProbesFile);
				readsMappedToMultipleProbesWriter = new TabDelimitedFileWriter(readsMappedToMultipleProbesFile, new String[] { "read_name", "probe" });
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File uidCompositionByProbeFile = new File(outputDirectory, outputFilePrefix + UID_COMPOSITION_REPORT_NAME);
			try {
				FileUtil.createNewFile(uidCompositionByProbeFile);
				uidCompositionByProbeWriter = new TabDelimitedFileWriter(uidCompositionByProbeFile, new String[] {
						"probe_id",
						"unique_uid_nuclotide_composition" + StringUtil.TAB + "unique_uid_nuclotide_composition_by_position" + StringUtil.TAB + "weighted_uid_nuclotide_composition" + StringUtil.TAB
								+ "weighted_uid_nuclotide_composition_by_position" });
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

	}

	public void close() {
		if (ambiguousMappingWriter != null) {
			ambiguousMappingWriter.close();
		}

		if (probeCoverageWriter != null) {
			probeCoverageWriter.close();
		}

		if (readsMappedToMultipleProbesWriter != null) {
			readsMappedToMultipleProbesWriter.close();
		}

		if (uidCompositionByProbeWriter != null) {
			uidCompositionByProbeWriter.close();
		}

		if (detailsReport != null) {
			detailsReport.close();
		}

		if (summaryReport != null) {
			summaryReport.close();
		}

		if (uniqueProbeTalliesWriter != null) {
			uniqueProbeTalliesWriter.close();
		}
	}

	public ProbeDetailsReport getDetailsReport() {
		return detailsReport;
	}

	public TabDelimitedFileWriter getUniqueProbeTalliesWriter() {
		return uniqueProbeTalliesWriter;
	}

	public TabDelimitedFileWriter getProbeCoverageWriter() {
		return probeCoverageWriter;
	}

	public TabDelimitedFileWriter getReadsMappedToMultipleProbesWriter() {
		return readsMappedToMultipleProbesWriter;
	}

	public TabDelimitedFileWriter getUidCompisitionByProbeWriter() {
		return uidCompositionByProbeWriter;
	}

	public SummaryReport getSummaryReport() {
		return summaryReport;
	}

	public TabDelimitedFileWriter getAmbiguousMappingWriter() {
		return ambiguousMappingWriter;
	}

	public void completeSummaryReport(Set<ISequence> distinctUids, List<ISequence> nonDistinctUids, long processingTimeInMs, int totalProbes, int totalReadPairs,
			int totalFullyMappedOffTargetReadPairs, int totalPartiallyMappedReadPairs, int totalFullyUnmappedReadPairs, int totalFullyMappedOnTargetReadPairs, int uniqueOnTargetReadPairs,
			int duplicateOnTargetReadPairs, int unpairedReads) {

		summaryReport.setDuplicateReadPairsRemoved(duplicateOnTargetReadPairs);
		summaryReport.setTotalReadPairsAfterReduction(uniqueOnTargetReadPairs);

		summaryReport.setProbesWithNoMappedReadPairs(detailsReport.getProbesWithNoMappedReadPairs());

		summaryReport.setAverageUidsPerProbe(detailsReport.getAverageNumberOfUidsPerProbe());
		summaryReport.setAverageUidsPerProbeWithReads(detailsReport.getAverageNumberOfUidsPerProbeWithAssignedReads());
		summaryReport.setMaxUidsPerProbe(detailsReport.getMaxNumberOfUidsPerProbe());
		summaryReport.setAverageNumberOfReadPairsPerProbeUid(detailsReport.getAverageNumberOfReadPairsPerProbeUid());

		summaryReport.setDistinctUidsFound(distinctUids.size());
		summaryReport.setTotalProbes(totalProbes);

		summaryReport.setTotalFullyMappedOffTargetReadPairs(totalFullyMappedOffTargetReadPairs);

		summaryReport.setTotalPartiallyMappedReadPairs(totalPartiallyMappedReadPairs);

		summaryReport.setTotalFullyUnmappedReads(totalFullyUnmappedReadPairs);

		summaryReport.setTotalFullyMappedOnTargetReadPairs(totalFullyMappedOnTargetReadPairs);

		summaryReport.setUnpairedReads(unpairedReads);

		summaryReport.setTotalReads((totalReadPairs * 2) + unpairedReads);
		summaryReport.setTotalReadPairs(totalReadPairs);
	}

	public void addExtensionPrimerMismatchDetails(String extensionPrimerMismatchAlignment) {
		if (extensionPrimerMismatchAlignment != null) {
			String extensionPrimerMismatchDetails = extensionPrimerMismatchAlignment.split("\\?read\\?")[0];
			numberOfExtensionErrors.add(extensionPrimerMismatchDetails.length() - StringUtil.countMatches(extensionPrimerMismatchDetails, "" + CigarStringUtil.CIGAR_SEQUENCE_MATCH));
			int insertions = StringUtil.countMatches(extensionPrimerMismatchDetails, "" + CigarStringUtil.CIGAR_INSERTION_TO_REFERENCE);
			int deletions = StringUtil.countMatches(extensionPrimerMismatchDetails, "" + CigarStringUtil.CIGAR_DELETION_FROM_REFERENCE);
			numberOfExtensionInsertions.add(insertions);
			numberOfExtensionDeletions.add(deletions);
			numberOfExtensionGains.add(deletions - insertions);
			for (int i = 0; i < extensionPrimerMismatchDetails.length(); i++) {
				TallyMap<Character> tally = null;
				if (i < extensionMismatchDetailsByIndex.size()) {
					tally = extensionMismatchDetailsByIndex.get(i);
				} else {
					tally = new TallyMap<Character>();
					extensionMismatchDetailsByIndex.add(tally);
				}
				tally.add(extensionPrimerMismatchDetails.charAt(i));
			}
		}
	}

	public void addLigationPrimerMismatchDetails(String ligationPrimerMismatchAlignment) {
		if (ligationPrimerMismatchAlignment != null) {
			String ligationPrimerMismatchDetails = ligationPrimerMismatchAlignment.split("\\?read\\?")[0];
			numberOfLigationErrors.add(ligationPrimerMismatchDetails.length() - StringUtil.countMatches(ligationPrimerMismatchDetails, "" + CigarStringUtil.CIGAR_SEQUENCE_MATCH));
			int insertions = StringUtil.countMatches(ligationPrimerMismatchDetails, "" + CigarStringUtil.CIGAR_INSERTION_TO_REFERENCE);
			int deletions = StringUtil.countMatches(ligationPrimerMismatchDetails, "" + CigarStringUtil.CIGAR_DELETION_FROM_REFERENCE);
			numberOfLigationInsertions.add(insertions);
			numberOfLigationDeletions.add(deletions);
			numberOfLigationGains.add(deletions - insertions);
			for (int i = 0; i < ligationPrimerMismatchDetails.length(); i++) {
				TallyMap<Character> tally = null;
				if (i < ligationMismatchDetailsByIndex.size()) {
					tally = ligationMismatchDetailsByIndex.get(i);
				} else {
					tally = new TallyMap<Character>();
					ligationMismatchDetailsByIndex.add(tally);
				}
				tally.add(ligationPrimerMismatchDetails.charAt(i));
			}
		}
	}

}
