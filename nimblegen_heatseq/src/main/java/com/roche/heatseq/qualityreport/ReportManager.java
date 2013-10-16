package com.roche.heatseq.qualityreport;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import net.sf.picard.fastq.FastqWriter;
import net.sf.picard.fastq.FastqWriterFactory;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;

import com.roche.heatseq.process.PrefuppCli;
import com.roche.heatseq.process.TabDelimitedFileWriter;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;

public class ReportManager {

	private final static String DUPLICATE_MAPPINGS_REPORT_NAME = "duplicate_mappings.txt";

	public final static String DETAILS_REPORT_NAME = "probe_details.txt";
	public final static String SUMMARY_REPORT_NAME = PrefuppCli.APPLICATION_NAME + "_summary.txt";
	public final static String PROBE_UID_QUALITY_REPORT_NAME = "probe_uid_quality.txt";
	public final static String UNABLE_TO_ALIGN_PRIMER_REPORT_NAME = "unable_to_align_primer_for_variable_length_uid.txt";
	public final static String UNABLE_TO_MAP_FASTQ_ONE_REPORT_NAME = "unable_to_map_one.fastq";
	public final static String UNABLE_TO_MAP_FASTQ_TWO_REPORT_NAME = "unable_to_map_two.fastq";
	public final static String PRIMER_ALIGNMENT_REPORT_NAME = "extension_primer_alignment.txt";
	public final static String UNIQUE_PROBE_TALLIES_REPORT_NAME = "unique_probe_tallies.txt";
	public final static String PROBE_COVERAGE_REPORT_NAME = "probe_coverage.bed";
	public final static String MAPPED_OFF_TARGET_READS_REPORT_NAME = "mapped_off_target_reads.bam";
	public final static String UNMAPPED_READS_REPORT_NAME = "unmapped_read_pairs.bam";

	private TabDelimitedFileWriter ambiguousMappingWriter;
	private TabDelimitedFileWriter probeUidQualityWriter;
	private TabDelimitedFileWriter unableToAlignPrimerWriter;
	private TabDelimitedFileWriter primerAlignmentWriter;
	private TabDelimitedFileWriter uniqueProbeTalliesWriter;
	private TabDelimitedFileWriter probeCoverageWriter;
	private SAMFileWriter mappedOffTargetReadsWriter;
	private SAMFileWriter unmappedReadsWriter;
	private DetailsReport detailsReport;
	private SummaryReport summaryReport;

	private FastqWriter fastqOneUnableToMapWriter;
	private FastqWriter fastqTwoUnableToMapWriter;

	private final boolean shouldOutputReports;

	public ReportManager(File outputDirectory, String outputFilePrefix, int uidLength, SAMFileHeader samFileHeader, boolean shouldOutputReports) {

		this.shouldOutputReports = shouldOutputReports;

		if (shouldOutputReports) {
			File ambiguousMappingFile = new File(outputDirectory, outputFilePrefix + DUPLICATE_MAPPINGS_REPORT_NAME);
			try {
				FileUtil.createNewFile(ambiguousMappingFile);
				ambiguousMappingWriter = new TabDelimitedFileWriter(ambiguousMappingFile, new String[] { "read_name", "read_string", "sequence_name", "extension_primer_start",
						"extension_primer_stop", "capture_target_start", "capture_target_stop", "ligation_primer_start", "ligation_primer_stop", "probe_strand" });
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File probeUidQualityFile = new File(outputDirectory, outputFilePrefix + PROBE_UID_QUALITY_REPORT_NAME);
			try {
				FileUtil.createNewFile(probeUidQualityFile);
				probeUidQualityWriter = new TabDelimitedFileWriter(probeUidQualityFile, new String[] { "probe_id", "uid", "read_one_quality", "read_two_quality", "total_quality", "read_name",
						"read_sequence" });
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File unableToAlignPrimerFile = new File(outputDirectory, outputFilePrefix + UNABLE_TO_ALIGN_PRIMER_REPORT_NAME);
			try {
				FileUtil.createNewFile(unableToAlignPrimerFile);
				unableToAlignPrimerWriter = new TabDelimitedFileWriter(unableToAlignPrimerFile, new String[] { "sequence_name", "probe_start", "probe_stop", "extension_primer_sequence", "read_name",
						"read_string" });
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			final FastqWriterFactory factory = new FastqWriterFactory();

			File unableToMapFastqOneFile = new File(outputDirectory, outputFilePrefix + UNABLE_TO_MAP_FASTQ_ONE_REPORT_NAME);
			try {
				FileUtil.createNewFile(unableToMapFastqOneFile);
				fastqOneUnableToMapWriter = factory.newWriter(unableToMapFastqOneFile);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File unableToMapFastqTwoFile = new File(outputDirectory, outputFilePrefix + UNABLE_TO_MAP_FASTQ_TWO_REPORT_NAME);
			try {
				FileUtil.createNewFile(unableToMapFastqTwoFile);
				fastqTwoUnableToMapWriter = factory.newWriter(unableToMapFastqTwoFile);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File primerAlignmentFile = new File(outputDirectory, outputFilePrefix + PRIMER_ALIGNMENT_REPORT_NAME);
			try {
				FileUtil.createNewFile(primerAlignmentFile);
				primerAlignmentWriter = new TabDelimitedFileWriter(primerAlignmentFile, new String[] { "uid_length", "substituions", "insertions", "deletions", "edit_distance", "read",
						"extension_primer", "probe_sequence_name", "capture_target_start", "capture_target_stop", "probe_strand" });
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

			SAMFileWriterFactory samFactory = new SAMFileWriterFactory();

			File mappedOffTargetFile = new File(outputDirectory, outputFilePrefix + MAPPED_OFF_TARGET_READS_REPORT_NAME);
			try {
				FileUtil.createNewFile(mappedOffTargetFile);
				mappedOffTargetReadsWriter = samFactory.makeBAMWriter(samFileHeader, true, mappedOffTargetFile);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File unMappedFile = new File(outputDirectory, outputFilePrefix + UNMAPPED_READS_REPORT_NAME);
			try {
				FileUtil.createNewFile(unMappedFile);
				unmappedReadsWriter = samFactory.makeBAMWriter(samFileHeader, true, unMappedFile);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File detailsReportFile = new File(outputDirectory, outputFilePrefix + DETAILS_REPORT_NAME);
			try {
				FileUtil.createNewFile(detailsReportFile);
				detailsReport = new DetailsReport(detailsReportFile);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			File summaryReportFile = null;

			summaryReportFile = new File(outputDirectory, outputFilePrefix + SUMMARY_REPORT_NAME);
			try {
				FileUtil.createNewFile(summaryReportFile);
				summaryReport = new SummaryReport(summaryReportFile, uidLength);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	public boolean isReporting() {
		return shouldOutputReports;
	}

	public void close() {
		if (ambiguousMappingWriter != null) {
			ambiguousMappingWriter.close();
		}

		if (probeUidQualityWriter != null) {
			probeUidQualityWriter.close();
		}
		if (unableToAlignPrimerWriter != null) {
			unableToAlignPrimerWriter.close();
		}
		if (primerAlignmentWriter != null) {
			primerAlignmentWriter.close();
		}
		if (fastqOneUnableToMapWriter != null) {
			fastqOneUnableToMapWriter.close();
		}
		if (fastqTwoUnableToMapWriter != null) {
			fastqTwoUnableToMapWriter.close();
		}

		if (probeCoverageWriter != null) {
			probeCoverageWriter.close();
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

		if (mappedOffTargetReadsWriter != null) {
			mappedOffTargetReadsWriter.close();
		}

		if (unmappedReadsWriter != null) {
			unmappedReadsWriter.close();
		}

	}

	public DetailsReport getDetailsReport() {
		return detailsReport;
	}

	public TabDelimitedFileWriter getUniqueProbeTalliesWriter() {
		return uniqueProbeTalliesWriter;
	}

	public TabDelimitedFileWriter getProbeCoverageWriter() {
		return probeCoverageWriter;
	}

	public SummaryReport getSummaryReport() {
		return summaryReport;
	}

	public TabDelimitedFileWriter getProbeUidQualityWriter() {
		return probeUidQualityWriter;
	}

	public TabDelimitedFileWriter getUnableToAlignPrimerWriter() {
		return unableToAlignPrimerWriter;
	}

	public TabDelimitedFileWriter getAmbiguousMappingWriter() {
		return ambiguousMappingWriter;
	}

	public FastqWriter getFastqOneUnableToMapWriter() {
		return fastqOneUnableToMapWriter;
	}

	public FastqWriter getFastqTwoUnableToMapWriter() {
		return fastqTwoUnableToMapWriter;
	}

	public TabDelimitedFileWriter getPrimerAlignmentWriter() {
		return primerAlignmentWriter;
	}

	public SAMFileWriter getMappedOffTargetReadsWriter() {
		return mappedOffTargetReadsWriter;
	}

	public SAMFileWriter getUnMappedReadPairsWriter() {
		return unmappedReadsWriter;
	}

	public void completeSummaryReport(TallyMap<String> readNamesToDistinctProbeAssignmentCount, Set<ISequence> distinctUids, List<ISequence> nonDistinctUids, long processingTimeInMs, int totalProbes,
			int totalReads, int totalMappedReads) {
		summaryReport.setDistinctUidComposition(NucleotideCompositionUtil.getNucleotideComposition(distinctUids));
		summaryReport.setDistinctUidCompositionByBase(NucleotideCompositionUtil.getNucleotideCompositionByPosition(distinctUids));

		summaryReport.setNonDistinctUidComposition(NucleotideCompositionUtil.getNucleotideComposition(nonDistinctUids));
		summaryReport.setNonDistinctUidCompositionByBase(NucleotideCompositionUtil.getNucleotideCompositionByPosition(nonDistinctUids));

		summaryReport.setProcessingTimeInMs(processingTimeInMs);
		summaryReport.setDuplicateReadPairsRemoved(detailsReport.getDuplicateReadPairsRemoved());
		summaryReport.setProbesWithNoMappedReadPairs(detailsReport.getProbesWithNoMappedReadPairs());
		summaryReport.setTotalReadPairsAfterReduction(detailsReport.getTotalReadPairsAfterReduction());

		summaryReport.setAverageUidsPerProbe(detailsReport.getAverageNumberOfUidsPerProbe());
		summaryReport.setAverageUidsPerProbeWithReads(detailsReport.getAverageNumberOfUidsPerProbeWithAssignedReads());
		summaryReport.setMaxUidsPerProbe(detailsReport.getMaxNumberOfUidsPerProbe());
		summaryReport.setAverageNumberOfReadPairsPerProbeUid(detailsReport.getAverageNumberOfReadPairsPerProbeUid());

		int readPairsAssignedToMultipleProbes = 0;
		for (int counts : readNamesToDistinctProbeAssignmentCount.getTalliesAsMap().values()) {
			if (counts > 1) {
				readPairsAssignedToMultipleProbes++;
			}
		}
		summaryReport.setReadPairsAssignedToMultipleProbes(readPairsAssignedToMultipleProbes);
		summaryReport.setDistinctUidsFound(distinctUids.size());
		summaryReport.setTotalProbes(totalProbes);

		summaryReport.setUnmappedReads(totalReads - totalMappedReads);
		summaryReport.setMappedReads(totalMappedReads);
	}

}
