package com.roche.heatseq.verification_tests;

import java.io.File;

import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;

public class HsqTestPlanUtil {

	private final static String PROBE_EXTENSION = "txt";
	private final static String PROBE_CONTAINS = "probe";

	private final static String R1_EXTENSION = "fastq";
	private final static String R1_COMPRESSED_EXTENSION = "fastq.gz";
	private final static String R1_CONTAINS = "r1";

	private final static String TRIMMED_R1_CONTAINS = "trimmed";
	private final static String TRIMMED_R2_CONTAINS = "trimmed";

	private final static String R2_EXTENSION = "fastq";
	private final static String R2_COMPRESSED_EXTENSION = "fastq.gz";
	private final static String R2_CONTAINS = "r2";

	private final static String BAM_EXTENSION = "bam";
	private final static String SAM_EXTENSION = "sam";

	private final static String LOG_EXTENSION = "log";

	public enum HsqUtilsCommandEnum {
		TRIM, DEDUP
	};

	private HsqTestPlanUtil() {
		throw new AssertionError();
	}

	public static String[] createArgs(HsqUtilsCommandEnum trimOrDedup, File stepDirectory, String[] extraArgs) {
		HsqUtilsFileSet fileSet = getFileSetFromDirectory(stepDirectory);

		File probe = fileSet.getProbe();
		File r1 = fileSet.getR1();
		File r2 = fileSet.getR2();
		File bam = fileSet.getBam();

		String[] args = null;
		if (trimOrDedup.equals(HsqUtilsCommandEnum.TRIM)) {
			if (r1 != null && r2 != null && probe != null) {
				args = new String[] { "trim", "--r1", r1.getAbsolutePath(), "--r2", r2.getAbsolutePath(), "--probe", probe.getAbsolutePath() };
				if (extraArgs != null && extraArgs.length > 0) {
					args = ArraysUtil.concatenate(args, extraArgs);
				}
			} else {
				throw new IllegalStateException("Unable to locate either the probe infor, r1 or r2 file for trim.");
			}

		} else if (trimOrDedup.equals(HsqUtilsCommandEnum.DEDUP)) {
			if (r1 != null && r2 != null && probe != null && bam != null) {
				args = new String[] { "dedup", "--r1", r1.getAbsolutePath(), "--r2", r2.getAbsolutePath(), "--probe", probe.getAbsolutePath(), "--bam", bam.getAbsolutePath() };
				if (extraArgs.length > 0) {
					args = ArraysUtil.concatenate(args, extraArgs);
				}
			} else {
				throw new IllegalStateException("Unable to locate either the probe infor, bam file, r1 or r2 file for dedup.");
			}
		} else {
			throw new IllegalStateException("Unrecognized " + HsqUtilsCommandEnum.class + " option[" + trimOrDedup + "].");
		}

		return args;
	}

	public static HsqUtilsFileSet getFileSetFromDirectory(File directory) {
		File r1 = null;
		File r2 = null;
		File probe = null;
		File bam = null;
		for (File file : directory.listFiles()) {
			String extension = FileUtil.getFileExtension(file).toLowerCase();
			String fileName = file.getName().toLowerCase();

			if ((extension.equals(R1_EXTENSION) || extension.equals(R1_COMPRESSED_EXTENSION)) && fileName.contains(R1_CONTAINS)) {
				if (r1 != null) {
					throw new IllegalStateException("Directory[" + directory.getAbsolutePath() + "] contains more than one R1 file([" + r1.getAbsolutePath() + "] and [" + file.getAbsolutePath()
							+ "].");
				} else {
					r1 = file;
				}
			}

			if ((extension.equals(R2_EXTENSION) || extension.equals(R2_COMPRESSED_EXTENSION)) && fileName.contains(R2_CONTAINS)) {
				if (r2 != null) {
					throw new IllegalStateException("Directory[" + directory.getAbsolutePath() + "] contains more than one R2 info file([" + r2.getAbsolutePath() + "] and [" + file.getAbsolutePath()
							+ "].");
				} else {
					r2 = file;
				}
			}

			if (extension.equals(PROBE_EXTENSION) && fileName.contains(PROBE_CONTAINS)) {
				if (probe != null) {
					throw new IllegalStateException("Directory[" + directory.getAbsolutePath() + "] contains more than one probe info file([" + probe.getAbsolutePath() + "] and ["
							+ file.getAbsolutePath() + "].");
				} else {
					probe = file;
				}
			}

			if (extension.equals(BAM_EXTENSION) || extension.equals(SAM_EXTENSION)) {
				if (bam != null) {
					throw new IllegalStateException("Directory[" + directory.getAbsolutePath() + "] contains more than one bam/sam file([" + bam.getAbsolutePath() + "] and [" + file.getAbsolutePath()
							+ "].");
				} else {
					bam = file;
				}
			}
		}

		return new HsqUtilsFileSet(r1, r2, probe, bam);
	}

	public static HsqUtilsTrimResultFileSet getTrimResultFileSetFromDirectory(File directory) {
		File trimmedR1 = null;
		File trimmedR2 = null;
		File log = null;
		for (File file : directory.listFiles()) {
			String extension = FileUtil.getFileExtension(file).toLowerCase();
			String fileName = file.getName().toLowerCase();

			if ((extension.equals(R1_EXTENSION)) && fileName.contains(R1_CONTAINS) && fileName.contains(TRIMMED_R1_CONTAINS)) {
				if (trimmedR1 != null) {
					throw new IllegalStateException("Directory[" + directory.getAbsolutePath() + "] contains more than one Trimmed R1 file([" + trimmedR1.getAbsolutePath() + "] and ["
							+ file.getAbsolutePath() + "].");
				} else {
					trimmedR1 = file;
				}
			}

			if ((extension.equals(R2_EXTENSION)) && fileName.contains(R2_CONTAINS) && fileName.contains(TRIMMED_R2_CONTAINS)) {
				if (trimmedR2 != null) {
					throw new IllegalStateException("Directory[" + directory.getAbsolutePath() + "] contains more than one Trimmed R2 file([" + trimmedR2.getAbsolutePath() + "] and ["
							+ file.getAbsolutePath() + "].");
				} else {
					trimmedR2 = file;
				}
			}

			if (extension.equals(LOG_EXTENSION)) {
				if (log != null) {
					throw new IllegalStateException("Directory[" + directory.getAbsolutePath() + "] contains more than one log file([" + log.getAbsolutePath() + "] and [" + file.getAbsolutePath()
							+ "].");
				} else {
					log = file;
				}
			}
		}

		return new HsqUtilsTrimResultFileSet(log, trimmedR1, trimmedR2);
	}

}
