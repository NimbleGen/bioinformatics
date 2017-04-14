package com.roche.heatseq.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.roche.heatseq.process.BamFileValidator;
import com.roche.heatseq.utils.BamFileUtil.CheckResult;
import com.roche.sequencing.bioinformatics.common.alignment.CigarStringUtil;
import com.roche.sequencing.bioinformatics.common.genome.Genome;
import com.roche.sequencing.bioinformatics.common.genome.StrandedGenomicRangedCoordinate;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.AlphaNumericStringComparator;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.IlluminaFastQReadNameUtil;
import com.roche.sequencing.bioinformatics.common.utils.ListUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.fastq.PicardException;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

public class BamFileValidationComparisonUtil {

	private BamFileValidationComparisonUtil() {
		throw new AssertionError();
	}

	public static void main(String[] args) throws IOException {
		compareSingleBamFiles();
		// compareBamFilesInTestPlanRuns();
		// validateBamFiles();
	}

	public static void compareBamFilesInTestPlanRuns() throws IOException {
		File oldTestDir = new File("C:\\Users\\heilmank\\Desktop\\hsqutils_testplan_results_20160516105046\\hsqutils_testplan_results");
		File newTestDir = new File("C:\\Users\\heilmank\\Desktop\\hsqutils_testplan_results_v1_1_20170411141655\\hsqutils_testplan_results_v1_1");
		// File newTestDir = new File("R:\\SoftwareDevelopment\\HeatSeqApplication\\Validation\\autotestplan_current\\hsqutils_testplan_results_v1_1");

		Genome hg38Genome = new Genome(new File("R:\\SoftwareDevelopment\\GenomeViewer\\hg38.gnm"));
		Genome hg19Genome = new Genome(new File("R:\\SoftwareDevelopment\\GenomeViewer\\hg19.gnm"));

		for (File directory : newTestDir.listFiles()) {
			String directoryName = directory.getName();
			if (directoryName.contains("run_58")) {
				File outputDir = new File(directory, "outputDir");
				if (outputDir.exists()) {
					Genome genome = hg19Genome;
					if (directoryName.contains("run_63")) {
						genome = hg38Genome;
					}
					File[] files = outputDir.listFiles(new FileFilter() {
						@Override
						public boolean accept(File pathname) {
							return pathname.getName().endsWith(".bam");
						}
					});

					if (files.length > 1) {
						throw new AssertionError();
					}

					if (files.length == 1) {
						File newBamFile = files[0];
						File oldBamFile = new File(oldTestDir, directoryName + "\\outputDir\\" + newBamFile.getName());
						if (newBamFile.exists() && oldBamFile.exists()) {
							System.out.println("comparing newBam[" + newBamFile.getAbsolutePath() + "] to oldBam[" + oldBamFile.getAbsolutePath() + "].");
							checkBamCigarString(newBamFile, genome, true);
							// checkBamCigarString(oldBamFile, genome, true);
							compareBamFiles(oldBamFile, newBamFile, genome);
						}

						File outputDirectory = new File("C:\\Users\\heilmank\\Desktop\\bam_comp\\" + directoryName + "\\");
						FileUtil.createDirectory(outputDirectory);
						if (newBamFile.exists()) {
							BamFileUtil.convertBamToSam(newBamFile, new File(outputDirectory, "new_" + newBamFile.getName() + ".sam"));
						}
						if (oldBamFile.exists()) {
							BamFileUtil.convertBamToSam(oldBamFile, new File(outputDirectory, "old_" + oldBamFile.getName() + ".sam"));
						}
					}

				}
			}
		}
	}

	public static void compareSingleBamFiles() throws IOException {
		Genome genome = new Genome(new File("R:\\SoftwareDevelopment\\GenomeViewer\\hg19.gnm"));

		File newBamFile = new File("C:\\Users\\heilmank\\Desktop\\a\\1_1\\output.bam");
		// this is the old
		File oldBamFile = new File("C:\\Users\\heilmank\\Desktop\\a\\1_0\\output.bam");
		compareBamFiles(oldBamFile, newBamFile, genome);
	}

	public static void validateBamFiles() throws IOException {
		// Genome genome = new Genome(new File("R:\\SoftwareDevelopment\\GenomeViewer\\hg38.gnm"));
		Genome genome = new Genome(new File("R:\\SoftwareDevelopment\\GenomeViewer\\hg19.gnm"));

		File inputBamFile = new File("R:\\SoftwareDevelopment\\HeatSeqApplication\\Validation\\autotestplan_current\\hsqutils_testplan\\run2\\expected_results\\output.bam");
		BamFileValidator.validate(inputBamFile);
		boolean verbose = true;
		checkBamCigarString(inputBamFile, genome, verbose);
	}

	private static void compareBamFiles(File oldSamFile, File newSamFile, Genome genome) throws FileNotFoundException {
		Map<String, RecordPair> oldBamMap = parseBamFile(oldSamFile);
		Map<String, RecordPair> newBamMap = parseBamFile(newSamFile);

		List<String> inOldButNotNewList = new ArrayList<>();
		List<String> inNewButNotOldList = new ArrayList<>();

		int inOldButNotNew = 0;
		int inNewButNotOld = 0;
		int inBoth = 0;
		int assignedToDifferentProbes = 0;

		int fixedCompareSkipped = 0;
		int mdMismatches = 0;

		Set<String> allReadNames = new HashSet<>(oldBamMap.keySet());
		allReadNames.addAll(newBamMap.keySet());

		// ComparisonResult allRawResults = null;
		ComparisonResult allFixedResults = null;

		Set<String> dupInOld_uniqueInNew = new HashSet<>();
		Set<String> uniqueInOld_dupInNew = new HashSet<>();

		TallyMap<String> oldProbeTally = new TallyMap<>();
		TallyMap<String> newProbeTally = new TallyMap<>();
		TallyMap<String> newDupProbeTally = new TallyMap<>();
		TallyMap<String> newUniqueProbeTally = new TallyMap<>();

		for (String readName : allReadNames) {
			RecordPair oldRecordPair = oldBamMap.get(readName);
			RecordPair newRecordPair = newBamMap.get(readName);
			if (oldRecordPair == null && newRecordPair != null) {
				inNewButNotOld++;
				String newProbeId = SAMRecordUtil.getProbeId(newRecordPair.getRecordOne());
				newProbeTally.add(newProbeId);
				if (newRecordPair.getRecordOne().getDuplicateReadFlag()) {
					newDupProbeTally.add(newProbeId);
				} else {
					newUniqueProbeTally.add(newProbeId);
				}
				inNewButNotOldList.add(readName + "(" + newProbeId + ")");
			} else if (oldRecordPair != null && newRecordPair == null) {
				inOldButNotNew++;
				String oldProbeId = SAMRecordUtil.getProbeId(oldRecordPair.getRecordOne());
				oldProbeTally.add(oldProbeId);
				inOldButNotNewList.add(readName + "(" + oldProbeId + ")");
			} else if (oldRecordPair != null && newRecordPair != null) {
				inBoth++;

				if (oldRecordPair.getRecordOne().getDuplicateReadFlag() && !newRecordPair.getRecordOne().getDuplicateReadFlag()) {
					dupInOld_uniqueInNew.add(readName);
				} else if (!oldRecordPair.getRecordOne().getDuplicateReadFlag() && newRecordPair.getRecordOne().getDuplicateReadFlag()) {
					uniqueInOld_dupInNew.add(readName);
				}

				String oldOneReadString = oldRecordPair.getRecordOne().getReadString();
				String oldTwoReadString = oldRecordPair.getRecordTwo().getReadString();
				String oldProbeId = SAMRecordUtil.getProbeId(oldRecordPair.getRecordOne());
				String newOneReadString = newRecordPair.getRecordOne().getReadString();
				String newTwoReadString = newRecordPair.getRecordTwo().getReadString();
				String newProbeId = SAMRecordUtil.getProbeId(newRecordPair.getRecordOne());

				if (!oldProbeId.equals(newProbeId)) {
					assignedToDifferentProbes++;
				}

				oldProbeTally.add(oldProbeId);
				newProbeTally.add(newProbeId);
				if (newRecordPair.getRecordOne().getDuplicateReadFlag()) {
					newDupProbeTally.add(newProbeId);
				} else {
					newUniqueProbeTally.add(newProbeId);
				}

				String oldOneCigar = oldRecordPair.getRecordOne().getCigar().toString();
				String oldTwoCigar = oldRecordPair.getRecordTwo().getCigar().toString();
				String newOneCigar = newRecordPair.getRecordOne().getCigar().toString();
				String newTwoCigar = newRecordPair.getRecordTwo().getCigar().toString();

				String oldOneMd = (String) oldRecordPair.getRecordOne().getAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG);
				String oldTwoMd = (String) oldRecordPair.getRecordTwo().getAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG);
				String newOneMd = (String) newRecordPair.getRecordOne().getAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG);
				String newTwoMd = (String) newRecordPair.getRecordTwo().getAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG);

				int oldOneStart = oldRecordPair.getRecordOne().getAlignmentStart();
				int oldTwoStart = oldRecordPair.getRecordTwo().getAlignmentStart();
				int newOneStart = newRecordPair.getRecordOne().getAlignmentStart();
				int newTwoStart = newRecordPair.getRecordTwo().getAlignmentStart();

				int oldOneStop = oldRecordPair.getRecordOne().getAlignmentEnd();
				int oldTwoStop = oldRecordPair.getRecordTwo().getAlignmentEnd();
				int newOneStop = newRecordPair.getRecordOne().getAlignmentEnd();
				int newTwoStop = newRecordPair.getRecordTwo().getAlignmentEnd();

				String recordOneStrand = "FORWARD";
				if (oldRecordPair.getRecordOne().getReadNegativeStrandFlag()) {
					recordOneStrand = "REVERSE";
				}

				String recordTwoStrand = "FORWARD";
				if (oldRecordPair.getRecordTwo().getReadNegativeStrandFlag()) {
					recordTwoStrand = "REVERSE";
				}

				String fixedNewOneCigar = newOneCigar;
				String fixedNewTwoCigar = newTwoCigar;

				String fixedNewOneMd = newOneMd;
				String fixedNewTwoMd = newTwoMd;

				int fixedNewOneStop = newOneStop;
				int fixedNewTwoStop = newTwoStop;

				boolean isProbeNegativeStrand = oldProbeId.endsWith("-");

				String expandedNewCigarOne = CigarStringUtil.expandCigarString(newOneCigar);
				String expandedNewCigarTwo = CigarStringUtil.expandCigarString(newTwoCigar);
				String expandedOldCigarOne = CigarStringUtil.expandCigarString(oldOneCigar);
				String expandedOldCigarTwo = CigarStringUtil.expandCigarString(oldTwoCigar);

				boolean shouldSkipNewOne = expandedNewCigarOne.startsWith("I") || expandedNewCigarOne.startsWith("D") || expandedNewCigarOne.endsWith("I") || expandedNewCigarOne.endsWith("D");
				boolean shouldSkipNewTwo = expandedNewCigarTwo.startsWith("I") || expandedNewCigarTwo.startsWith("D") || expandedNewCigarTwo.endsWith("I") || expandedNewCigarTwo.endsWith("D");
				boolean shouldSkipOldOne = expandedOldCigarOne.startsWith("I") || expandedOldCigarOne.startsWith("D") || expandedOldCigarOne.endsWith("I") || expandedOldCigarOne.endsWith("D");
				boolean shouldSkipOldTwo = expandedOldCigarTwo.startsWith("I") || expandedOldCigarTwo.startsWith("D") || expandedOldCigarTwo.endsWith("I") || expandedOldCigarTwo.endsWith("D");
				boolean shouldSkip = shouldSkipNewOne || shouldSkipNewTwo || shouldSkipOldOne || shouldSkipOldTwo;

				if (shouldSkip) {
					fixedCompareSkipped++;
				} else {
					if (isProbeNegativeStrand) {
						fixedNewOneCigar = convertNewCigarStringToOld(newOneCigar);
						fixedNewTwoCigar = convertNewCigarStringToOld(newTwoCigar);

						fixedNewOneMd = convertNewMismatchDetailsStringToOld(newOneMd);
						fixedNewTwoMd = convertNewMismatchDetailsStringToOld(newTwoMd);

						fixedNewOneStop = convertNewStopToOld(newOneStop, newOneCigar);
						fixedNewTwoStop = convertNewStopToOld(newTwoStop, newTwoCigar);
					}

					ComparisonResult fixedResult = compare(oldOneReadString, oldTwoReadString, newOneReadString, newTwoReadString, oldOneCigar, oldTwoCigar, fixedNewOneCigar, fixedNewTwoCigar,
							oldOneMd, oldTwoMd, fixedNewOneMd, fixedNewTwoMd, oldOneStart, oldTwoStart, newOneStart, newTwoStart, oldOneStop, oldTwoStop, fixedNewOneStop, fixedNewTwoStop);

					if (allFixedResults == null) {
						allFixedResults = fixedResult;
					} else {
						allFixedResults.add(fixedResult);
					}

					// note: this should not get hit once the comparisons match
					if (!fixedResult.allButMdMatch()) {
						System.out.println("=============================================================");
						System.out.println(readName);
						System.out.println(
								oldRecordPair.getRecordOne().getReferenceName() + ":" + oldRecordPair.getRecordOne().getAlignmentStart() + "-" + oldRecordPair.getRecordOne().getAlignmentEnd());
						System.out.println("-----" + ":ONE:" + recordOneStrand + "-------------------------------------" + oldProbeId);
						outputComparisonOfRecords(oldOneReadString, oldOneCigar, oldOneMd, oldOneStart, oldOneStop, newOneReadString, fixedNewOneCigar, fixedNewOneMd, newOneStart, fixedNewOneStop);

						System.out.println("-----" + ":TWO:" + recordTwoStrand + "-------------------------------------");
						outputComparisonOfRecords(oldTwoReadString, oldTwoCigar, oldTwoMd, oldTwoStart, oldTwoStop, newTwoReadString, fixedNewTwoCigar, fixedNewTwoMd, newTwoStart, fixedNewTwoStop);

						System.out.println("=============================================================");

					}
				}
			}
		}
		System.out.println("--------------------------------------");
		System.out.println("FIXED RESULTS:");
		System.out.println(allFixedResults.getReport() + " skipped:" + fixedCompareSkipped + " md_mismatches_only:" + mdMismatches + " assignedToDifferentProbes:" + assignedToDifferentProbes);
		System.out.println("In_Old_But_Not_New:" + inOldButNotNew + " In_New_But_Not_Old:" + inNewButNotOld + " In_Both:" + inBoth);
		if (inOldButNotNewList.size() > 0) {
			System.out.println("**In Old but Not New:" + ListUtil.toString(inOldButNotNewList));
		}
		if (inNewButNotOldList.size() > 0) {
			System.out.println("**In New but Not Old:" + ListUtil.toString(inNewButNotOldList));
		}
		System.out.println("Duplicate in old, Unique in New:" + ListUtil.toString(new ArrayList<String>(dupInOld_uniqueInNew)));
		System.out.println("Unique in old, Duplicate in New:" + ListUtil.toString(new ArrayList<String>(uniqueInOld_dupInNew)));

		Comparator<String> stringComparator = new AlphaNumericStringComparator(true);

		List<String> probeIds = new ArrayList<>(oldProbeTally.getTalliesAsMap().keySet());
		Collections.sort(probeIds, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				StrandedGenomicRangedCoordinate g1 = new StrandedGenomicRangedCoordinate(o1);
				StrandedGenomicRangedCoordinate g2 = new StrandedGenomicRangedCoordinate(o2);

				int result = stringComparator.compare(g1.getContainerName(), g2.getContainerName());
				if (result == 0) {
					result = Long.compare(g1.getStartLocation(), g2.getStartLocation());
					if (result == 0) {
						result = Long.compare(g1.getStopLocation(), g2.getStopLocation());
						if (result == 0) {
							result = Boolean.compare(g2.getStrand() == Strand.FORWARD, g1.getStrand() == Strand.FORWARD);
						}
					}
				}

				return result;
			}
		});

		for (String probeId : probeIds) {
			System.out
					.println(probeId + StringUtil.TAB + newProbeTally.getCount(probeId) + StringUtil.TAB + newUniqueProbeTally.getCount(probeId) + StringUtil.TAB + newDupProbeTally.getCount(probeId));
		}

		System.out.println();
		System.out.println();
	}

	public static ComparisonResult compare(String oldOneReadString, String oldTwoReadString, String newOneReadString, String newTwoReadString, String oldOneCigar, String oldTwoCigar,
			String newOneCigar, String newTwoCigar, String oldOneMd, String oldTwoMd, String newOneMd, String newTwoMd, int oldOneStart, int oldTwoStart, int newOneStart, int newTwoStart,
			int oldOneStop, int oldTwoStop, int newOneStop, int newTwoStop) {
		boolean oneMatch = oldOneReadString.equals(newOneReadString);
		boolean twoMatch = oldTwoReadString.equals(newTwoReadString);
		boolean sequencesDiffer = (!oneMatch || !twoMatch);

		boolean oneCigarMatch = oldOneCigar.equals(newOneCigar);
		boolean twoCigarMatch = oldTwoCigar.toString().equals(newTwoCigar);
		boolean cigarsDiffer = (!oneCigarMatch || !twoCigarMatch);

		boolean oneMdMatch = oldOneMd.equals(newOneMd);
		boolean twoMdMatch = oldTwoMd.equals(newTwoMd);
		boolean mismatchDetailsDiffer = (!oneMdMatch || !twoMdMatch);

		boolean oneStartMatch = oldOneStart == newOneStart;
		boolean twoStartMatch = oldTwoStart == newTwoStart;
		boolean startsDiffer = (!oneStartMatch || !twoStartMatch);

		boolean oneStopMatch = oldOneStop == newOneStop;
		boolean twoStopMatch = oldTwoStop == newTwoStop;
		boolean stopsDiffer = (!oneStopMatch || !twoStopMatch);

		return new ComparisonResult(sequencesDiffer, cigarsDiffer, mismatchDetailsDiffer, startsDiffer, stopsDiffer);
	}

	private static class ComparisonResult {
		private int sequencesDiffer;
		private int cigarsDiffer;
		private int mismatchDetailsDiffer;
		private int startsDiffer;
		private int stopsDiffer;
		private int totalReadPairs;
		private int allButMdMatch;

		public ComparisonResult(boolean sequencesDiffer, boolean cigarsDiffer, boolean mismatchDetailsDiffer, boolean startsDiffer, boolean stopsDiffer) {
			super();
			if (sequencesDiffer) {
				this.sequencesDiffer = 1;
			}
			if (cigarsDiffer) {
				this.cigarsDiffer = 1;
			}
			if (mismatchDetailsDiffer) {
				this.mismatchDetailsDiffer = 1;
			}
			if (startsDiffer) {
				this.startsDiffer = 1;
			}
			if (stopsDiffer) {
				this.stopsDiffer = 1;
			}

			if (!sequencesDiffer && !cigarsDiffer && !startsDiffer && !stopsDiffer) {
				this.allButMdMatch = 1;
			}

			this.totalReadPairs = 1;
		}

		public boolean allButMdMatch() {
			return this.totalReadPairs == this.allButMdMatch;
		}

		public void add(ComparisonResult result) {
			sequencesDiffer += result.sequencesDiffer;
			cigarsDiffer += result.cigarsDiffer;
			mismatchDetailsDiffer += result.mismatchDetailsDiffer;
			startsDiffer += result.startsDiffer;
			stopsDiffer += result.stopsDiffer;
			allButMdMatch += result.allButMdMatch;
			totalReadPairs += result.totalReadPairs;
		}

		public String getReport() {
			StringBuilder report = new StringBuilder();
			report.append("reads_cigar_do_not_match:" + cigarsDiffer + " reads_cigar_match:" + (totalReadPairs - cigarsDiffer) + StringUtil.NEWLINE);
			report.append("reads_md_do_not_match:" + mismatchDetailsDiffer + " reads_md_match:" + (totalReadPairs - mismatchDetailsDiffer) + StringUtil.NEWLINE);
			report.append("reads_start_do_not_match:" + startsDiffer + " reads_start_match:" + (totalReadPairs - startsDiffer) + StringUtil.NEWLINE);
			report.append("reads_stop_do_not_match:" + stopsDiffer + " reads_stop_match:" + (totalReadPairs - stopsDiffer) + StringUtil.NEWLINE);
			report.append("all_match_or_just_md_mismatch:" + allButMdMatch + " reads_not_all_match_except_md:" + (totalReadPairs - allButMdMatch));
			return report.toString();
		}
	}

	private static String convertNewCigarStringToOld(String newCigarString) {
		String returnCigarString = newCigarString;
		if (newCigarString.endsWith("D")) {
			int endIndex = newCigarString.length() - 2;
			while (Character.isDigit(newCigarString.charAt(endIndex))) {
				endIndex--;
			}
			returnCigarString = newCigarString.substring(0, endIndex + 1);
		}

		StringBuilder numberBuilder = new StringBuilder();
		int i = 0;
		while (Character.isDigit(newCigarString.charAt(i))) {
			i++;
			numberBuilder.append(newCigarString).charAt(i);

		}

		return returnCigarString;
	}

	private static int convertNewStopToOld(int stop, String newCigarString) {
		int returnStop = stop;
		if (newCigarString.endsWith("D")) {
			int endIndex = newCigarString.length() - 2;
			StringBuilder numberBuilder = new StringBuilder();
			while (Character.isDigit(newCigarString.charAt(endIndex))) {
				numberBuilder.append(newCigarString.charAt(endIndex));
				endIndex--;
			}

			Integer value = Integer.parseInt(StringUtil.reverse(numberBuilder.toString()));
			returnStop = stop - value;
		}

		return returnStop;
	}

	private static boolean isNucleotide(char character) {
		char upperCase = Character.toUpperCase(character);
		boolean isNucleotide = upperCase == 'A' || upperCase == 'C' || upperCase == 'G' || upperCase == 'T';
		return isNucleotide;
	}

	private static String convertNewMismatchDetailsStringToOld(String newMismatchDetailsString) {
		String returnMismatchDetailsString = newMismatchDetailsString;
		int endIndex = newMismatchDetailsString.length() - 1;
		while (isNucleotide(newMismatchDetailsString.charAt(endIndex))) {
			endIndex--;
		}
		if (newMismatchDetailsString.charAt(endIndex) == '^') {
			returnMismatchDetailsString = newMismatchDetailsString.substring(0, endIndex);
		}

		returnMismatchDetailsString = returnMismatchDetailsString.replace("A", "Z");
		returnMismatchDetailsString = returnMismatchDetailsString.replace("T", "A");
		returnMismatchDetailsString = returnMismatchDetailsString.replace("Z", "T");

		returnMismatchDetailsString = returnMismatchDetailsString.replace("G", "Z");
		returnMismatchDetailsString = returnMismatchDetailsString.replace("C", "G");
		returnMismatchDetailsString = returnMismatchDetailsString.replace("Z", "C");

		return returnMismatchDetailsString;
	}

	private static void checkBamCigarString(File inputSamFile, Genome genome, boolean verbose) throws FileNotFoundException {
		TallyMap<String> failedProbeIds = new TallyMap<>();
		TallyMap<String> reasonsForFailure = new TallyMap<>();
		List<String> failedReadNames = new ArrayList<>();
		TallyMap<String> failureType = new TallyMap<>();

		int readCount = 0;
		int containingNCount = 0;

		try (SamReader currentReader = SamReaderFactory.makeDefault().open(inputSamFile)) {
			Iterator<SAMRecord> iter = currentReader.iterator();
			while (iter.hasNext()) {
				SAMRecord record = iter.next();
				boolean containsN = record.getReadString().contains("N");
				if (!containsN) {
					CheckResult checkResult = BamFileUtil.checkBamCigarStringAndMismatchDetails(genome, record, verbose);
					boolean cigarStringAndLengthIsOkay = BamFileUtil.checkSAMRecordCigarStringAndLength(record, verbose);

					if (checkResult.isFailed() || !cigarStringAndLengthIsOkay) {
						String readName = record.getReadName();
						String probeId = SAMRecordUtil.getProbeId(record);
						if (verbose) {
							System.out.println("Reason for Failure:" + checkResult.getReasonForFailure());
							System.out.println("Read Name:" + readName);
							if (record.getFirstOfPairFlag()) {
								System.out.println("Is read one.");
							} else {
								System.out.println("Is read two.");
							}
							if (record.getReadNegativeStrandFlag()) {
								System.out.println("Is Negative Strand.");
							} else {
								System.out.println("Is Forward Strand.");
							}
							System.out.println(probeId);
						}
						if (probeId != null) {
							failedProbeIds.add(probeId);
						}
						failedReadNames.add(readName);
						if (checkResult.isFailed()) {
							reasonsForFailure.add(checkResult.getReasonForFailure());
						} else {
							reasonsForFailure.add("Length check failed.");
						}
						String failureRecordType = "";
						if (record.getFirstOfPairFlag()) {
							failureRecordType += "1";
						} else {
							failureRecordType += "2";
						}

						if (record.getReadNegativeStrandFlag()) {
							failureRecordType += "-";
						} else {
							failureRecordType += "+";
						}

						if (probeId.endsWith("+")) {
							failureRecordType += "+";
						} else {
							failureRecordType += "-";
						}
						failureType.add(failureRecordType);
					}
				} else {
					containingNCount++;
				}
				readCount++;
			}
		} catch (IOException e) {
			throw new PicardException(e.getMessage(), e);
		}

		StringBuilder pids = new StringBuilder();
		pids.append("\"");
		for (String probeId : failedProbeIds.getTalliesAsMap().keySet()) {
			pids.append(probeId + "\",\"");
		}
		pids.append("\"");
		System.out.println(pids.toString());
		System.out.println("Failed_Reads:" + failedReadNames.size() + "  Reads_Containing_N:" + containingNCount + "  Total_Reads:" + readCount);
		System.out.println(failureType.getHistogramAsString());
	}

	private static Map<String, RecordPair> parseBamFile(File inputSamFile) {
		Map<String, RecordPair> recordPairsByName = new HashMap<>();

		try (SamReader currentReader = SamReaderFactory.makeDefault().open(inputSamFile)) {
			Iterator<SAMRecord> iter = currentReader.iterator();
			while (iter.hasNext()) {
				SAMRecord record = iter.next();
				String recordName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(record.getReadName());
				RecordPair recordPair = recordPairsByName.get(recordName);
				if (recordPair == null) {
					recordPair = new RecordPair();
					recordPairsByName.put(recordName, recordPair);
				}
				if (record.getFirstOfPairFlag()) {
					recordPair.setRecordOne(record);
				} else {
					recordPair.setRecordTwo(record);
				}
			}
		} catch (IOException e) {
			throw new PicardException(e.getMessage(), e);
		}

		return recordPairsByName;
	}

	private static class RecordPair {
		private SAMRecord recordOne;
		private SAMRecord recordTwo;

		public RecordPair() {
			super();
		}

		public SAMRecord getRecordOne() {
			return recordOne;
		}

		public void setRecordOne(SAMRecord recordOne) {
			this.recordOne = recordOne;
		}

		public SAMRecord getRecordTwo() {
			return recordTwo;
		}

		public void setRecordTwo(SAMRecord recordTwo) {
			this.recordTwo = recordTwo;
		}
	}

	public static void outputComparisonOfRecords(String recordOneReadString, String recordOneCigarString, String recordOneMismatchDetails, int recordOneStart, int recordOneStop,
			String recordTwoReadString, String recordTwoCigarString, String recordTwoMismatchDetails, int recordTwoStart, int recordTwoStop) {

		printDetail("SEQ:", recordOneReadString, recordTwoReadString);
		printDetail("CIG:", recordOneCigarString, recordTwoCigarString);
		printDetail(" MD:", (String) recordOneMismatchDetails, recordTwoMismatchDetails);
		printDetail("STT:", "" + recordOneStart, "" + recordTwoStart);
		printDetail("STP:", "" + recordOneStop, "" + recordTwoStop);
	}

	private static void printDetail(String description, String stringOne, String stringTwo) {
		printDetail(description, stringOne, stringTwo, false);
	}

	private static void printDetail(String description, String stringOne, String stringTwo, boolean alwaysDisplay) {
		if (!stringOne.equals(stringTwo) || alwaysDisplay) {
			System.out.println("1_" + description + stringOne);
			System.out.println("2_" + description + stringTwo);
		}
	}

}
