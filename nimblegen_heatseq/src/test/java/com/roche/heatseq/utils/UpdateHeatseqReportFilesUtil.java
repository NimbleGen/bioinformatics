package com.roche.heatseq.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.roche.sequencing.bioinformatics.common.utils.AlphaNumericStringComparator;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.DelimitedFileParserUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.IDelimitedLineParser;
import com.roche.sequencing.bioinformatics.common.utils.InputStreamFactory;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class UpdateHeatseqReportFilesUtil {

	private UpdateHeatseqReportFilesUtil() {
		throw new AssertionError();
	}

	private static class ProbeIdComparator implements Comparator<String> {

		private AlphaNumericStringComparator alphaNumericComparator = new AlphaNumericStringComparator();

		@Override
		public int compare(String probeId1, String probeId2) {
			int probe1ChrDivider = probeId1.indexOf(":");
			int probe2ChrDivider = probeId2.indexOf(":");

			String probe1Chromosome = probeId1.substring(0, probe1ChrDivider);
			String probe2Chromosome = probeId2.substring(0, probe2ChrDivider);

			int result = alphaNumericComparator.compare(probe1Chromosome, probe2Chromosome);

			if (result == 0) {
				String probe1StartAsString = probeId1.substring(probe1ChrDivider + 1, probeId1.indexOf(":", probe1ChrDivider + 1));
				String probe2StartAsString = probeId2.substring(probe2ChrDivider + 1, probeId2.indexOf(":", probe2ChrDivider + 1));

				try {
					int probe1Start = Integer.valueOf(probe1StartAsString);
					int probe2Start = Integer.valueOf(probe2StartAsString);
					result = Integer.compare(probe1Start, probe2Start);
				} catch (NumberFormatException e) {

				}

				if (result == 0) {
					result = probeId1.compareTo(probeId2);
				}
			}

			return result;
		}
	}

	public static void main(String[] args) throws IOException {
		// updateProbeDetailsFiles();
		updateSummaryFiles();
	}

	public static void updateProbeDetailsFiles() throws IOException {
		File[] files = new File[] {
				new File("R:\\SoftwareDevelopment\\HeatSeqApplication\\Validation\\autotestplan_current\\hsqutils_testplan\\run57\\expected_results\\EXPECTED_OUTPUT_probe_details.txt"),
				new File("R:\\SoftwareDevelopment\\HeatSeqApplication\\Validation\\autotestplan_current\\hsqutils_testplan\\run58\\expected_results\\EXPECTED_OUTPUT_probe_details.txt"),
				new File("R:\\SoftwareDevelopment\\HeatSeqApplication\\Validation\\autotestplan_current\\hsqutils_testplan\\run59\\expected_results\\EXPECTED_OUTPUT_probe_details.txt"),
				new File("R:\\SoftwareDevelopment\\HeatSeqApplication\\Validation\\autotestplan_current\\hsqutils_testplan\\run63\\expected_results\\EXPECTED_OUTPUT_probe_details.txt"),
				new File("R:\\SoftwareDevelopment\\HeatSeqApplication\\Validation\\autotestplan_current\\hsqutils_testplan\\run2\\expected_results\\Results_probe_details.txt"),
				new File(
						"R:\\SoftwareDevelopment\\HeatSeqApplication\\Validation\\autotestplan_current\\hsqutils_testplan\\run6_and_7\\run6\\expected_results\\Results_DuplicateCheck_probe_details.txt"),
				new File("R:\\SoftwareDevelopment\\HeatSeqApplication\\Validation\\autotestplan_current\\hsqutils_testplan\\run11\\expected_results\\Results_ext_UID_probe_details.txt") };

		for (File file : files) {
			FileInputStream fis = new FileInputStream(file);

			// Construct BufferedReader from InputStreamReader
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));

			List<String> lines = new ArrayList<String>();

			String firstLine = null;

			String line = null;
			while ((line = br.readLine()) != null) {
				if (firstLine == null) {
					firstLine = line;
				} else {
					lines.add(line);
				}
			}

			Collections.sort(lines, new Comparator<String>() {

				private final ProbeIdComparator comp = new ProbeIdComparator();

				@Override
				public int compare(String o1, String o2) {
					String[] split1 = o1.split(StringUtil.TAB);
					String id1 = split1[0];

					String[] split2 = o2.split(StringUtil.TAB);
					String id2 = split2[0];

					return comp.compare(id1, id2);
				}
			});

			br.close();

			StringBuilder output = new StringBuilder();

			output.append(removeLastColumn(firstLine) + StringUtil.LINUX_NEWLINE);
			for (String liney : lines) {
				output.append(removeLastColumn(liney) + StringUtil.LINUX_NEWLINE);
			}

			File outputFile = new File(file.getParent() + "//updated_" + file.getName());
			FileUtil.writeStringToFile(outputFile, output.toString());
		}
	}

	public static void updateSummaryFiles() throws IOException {
		final DecimalFormat formatter = new DecimalFormat("0.0000");
		final String[] SUMMARY_HEADER = new String[] { "duplicate_read_pairs_removed", "unique_read_pairs" };

		File[] files = new File[] { new File(
				"R:\\SoftwareDevelopment\\HeatSeqApplication\\Validation\\autotestplan_current\\hsqutils_testplan\\run2\\expected_results\\Results_HSQUtils_dedup_summary.txt") };

		for (File file : files) {

			final List<String> duplicateRates = new ArrayList<String>();
			DelimitedFileParserUtil.parseFile(new InputStreamFactory(file), SUMMARY_HEADER, new IDelimitedLineParser() {

				@Override
				public void parseDelimitedLine(Map<String, String> headerNameToValue) {
					double duplicateReadPairsRemoved = Double.parseDouble(headerNameToValue.get(SUMMARY_HEADER[0]));
					double uniqueReadPairs = Double.parseDouble(headerNameToValue.get(SUMMARY_HEADER[1]));
					String duplicateRateAsString = formatter.format(duplicateReadPairsRemoved / (uniqueReadPairs + duplicateReadPairsRemoved) * 100);
					duplicateRates.add(duplicateRateAsString);

				}

				@Override
				public void doneParsing(int linesOfData, String[] headerNames) {
				}

				@Override
				public void threadInterrupted() {
				}
			}, StringUtil.TAB, false);

			FileInputStream fis = new FileInputStream(file);

			// Construct BufferedReader from InputStreamReader
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));

			List<String> lines = new ArrayList<String>();

			String firstLine = null;
			String secondLine = null;

			String line = null;
			while ((line = br.readLine()) != null) {
				if (firstLine == null) {
					firstLine = line;
				} else if (secondLine == null) {
					secondLine = line;
				} else {
					lines.add(line);
				}
			}

			br.close();

			StringBuilder output = new StringBuilder();
			output.append(firstLine + StringUtil.NEWLINE);
			output.append(insertNthColumn(10, secondLine, "duplicate_rate") + StringUtil.LINUX_NEWLINE);
			int i = 0;
			for (String liney : lines) {
				if (!liney.isEmpty()) {
					output.append(insertNthColumn(10, liney, duplicateRates.get(i)) + StringUtil.LINUX_NEWLINE);
				} else {
					output.append(liney + StringUtil.LINUX_NEWLINE);
				}

				i++;
			}

			File outputFile = new File(file.getParent() + "//updated_" + file.getName());
			FileUtil.writeStringToFile(outputFile, output.toString());
		}
	}

	private static String insertNthColumn(int columnIndex, String line, String valueToInsert) {
		// add a space so a terminating tab is accounted for
		String[] split = line.split(StringUtil.TAB);
		String[] newSplit = new String[split.length + 1];
		for (int i = 0; i < split.length; i++) {
			if (i == columnIndex) {
				newSplit[i] = valueToInsert;
				newSplit[i + 1] = split[i];
			} else if (i > columnIndex) {
				newSplit[i + 1] = split[i];
			} else {
				newSplit[i] = split[i];
			}
		}
		String newString = ArraysUtil.toString(newSplit, StringUtil.TAB);
		return newString;
	}

	private static String removeLastColumn(String string) {
		// add a space so a terminating tab is accounted for
		string += " ";
		String[] split = string.split(StringUtil.TAB);
		split = Arrays.copyOf(split, split.length - 1);
		String newString = ArraysUtil.toString(split, StringUtil.TAB);
		return newString;
	}

}
