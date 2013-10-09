/**
 *   Copyright 2013 Roche NimbleGen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.roche.heatseq.qualityreport;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.roche.heatseq.process.TabDelimitedFileWriter;
import com.roche.imageexporter.Graphics2DImageExporter;
import com.roche.imageexporter.Graphics2DImageExporter.ImageType;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.statistics.RunningStats;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.DelimitedFileParserUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class ProbePlotter {

	private static int randomSeed = 1;

	public static void main(String[] args) {
		try {
			generateReadSubSampleReport(new File("D:/liang/results/report_unique_probe_tallies.txt"), new File("D:/liang/results/report_prefupp_summary.txt"), new File(
					"D:/liang/results/report_read_subsampling.txt"), new File("D:/liang/results/report_probe_subsampling.txt"), new int[] { 1, 10, 20, 50, 100 },
					new double[] { 0.01, 0.1, 1, 10, 100 });
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void generateReadSubSampleReport(File probeTalliesReportFile, File summaryReportFile, File outputSummaryReportFile, File outputProbeReportFile, int[] coverageDepths,
			double[] percentSizes) throws IOException {

		int totalRawReads = 0;

		// grab the total raw reads from the summary report
		String totalInputReadsHeader = "total_input_reads";
		String[] headerNames = new String[] { totalInputReadsHeader };
		Map<String, List<String>> headersToData = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(summaryReportFile, headerNames, StringUtil.TAB);
		List<String> totalInputReadsAsList = headersToData.get(totalInputReadsHeader);
		if (totalInputReadsAsList.size() == 1) {
			totalRawReads = Integer.parseInt(totalInputReadsAsList.get(0));
		}

		if (totalRawReads <= 0) {
			throw new IllegalStateException("Unable to retrieve column[" + totalInputReadsHeader + "] from file[" + summaryReportFile.getAbsolutePath() + "].");
		}

		String[] beginningHeader = new String[] { "orig_raw_reads", "percent_of_orig", "sub_sampled_raw_reads", "lower_bounds_on_target_reads", "upper_bounds_on_target_reads",
				"lower_bounds_reads_on_target_ratio", "upper_bounds_reads_on_target_ratio", "lower_bounds_raw_duplicate_rate", "upper_bounds_raw_duplicate_rate",
				"lower_bounds_on_target_duplicate_rate", "upper_bounds_on_target_duplicate_rate" };
		int totalColumns = beginningHeader.length + (coverageDepths.length * 2);

		String[] header = new String[totalColumns];

		for (int i = 0; i < totalColumns; i++) {
			if (i < beginningHeader.length) {
				header[i] = beginningHeader[i];
			} else {
				int indexInCoverageArray = (i - beginningHeader.length) / 2;
				boolean isFirstEntryForCoverage = (i - beginningHeader.length) % 2 == 0;
				if (isFirstEntryForCoverage) {
					header[i] = "lower_bounds_" + coverageDepths[indexInCoverageArray] + "x_coverage";
				} else {
					header[i] = "upper_bounds_" + coverageDepths[indexInCoverageArray] + "x_coverage";
				}
			}
		}

		String[] probeReportHeader = new String[] { "orig_raw_reads", "percent_of_orig", "sub_sampled_raw_reads", "number_of_simulations", "mean_occurences", "margin_of_error", "st_dev_occurrences",
				"upper_bound_occurrences", "lower_bound_occurrences" };
		TabDelimitedFileWriter probeSubSampleReport = new TabDelimitedFileWriter(outputProbeReportFile, probeReportHeader);

		TabDelimitedFileWriter readSubSampleReport = new TabDelimitedFileWriter(outputSummaryReportFile, header);
		long start = System.currentTimeMillis();

		try {
			Map<String, int[]> probeTallies = loadProbeTallies(probeTalliesReportFile);

			for (double percentSizeOfOriginal : percentSizes) {
				CoverageAndDuplicateRate coverageAndDuplicateRate = getCoverageAndDuplicateRateForEachDesiredCoverageDepth(probeTallies, totalRawReads, percentSizeOfOriginal, coverageDepths,
						probeSubSampleReport);
				double lowerBoundsOnTargetReads = coverageAndDuplicateRate.getLowerBoundsOnTargetReads();
				double upperBoundsOnTargetReads = coverageAndDuplicateRate.getUpperBoundsOnTargetReads();

				DecimalFormat format = new DecimalFormat("###,###,###,###,###,###.##");

				int newRawSize = (int) ((double) totalRawReads * (double) percentSizeOfOriginal / (double) 100);

				double lowerBoundsReadsOnTargetRatio = (double) lowerBoundsOnTargetReads / (double) newRawSize;
				double upperBoundsReadsOnTargetRatio = (double) upperBoundsOnTargetReads / (double) newRawSize;
				String[] beginningLine = new String[] { "" + totalRawReads, "" + percentSizeOfOriginal, "" + newRawSize, "" + format.format(lowerBoundsOnTargetReads),
						"" + format.format(upperBoundsOnTargetReads), "" + format.format(lowerBoundsReadsOnTargetRatio), "" + format.format(upperBoundsReadsOnTargetRatio),
						"" + format.format(coverageAndDuplicateRate.getLowerBoundsRawDuplicateRate()), "" + format.format(coverageAndDuplicateRate.getUpperBoundsRawDuplicateRate()),
						"" + format.format(coverageAndDuplicateRate.getLowerBoundsOnTargetDuplicateRate()), "" + format.format(coverageAndDuplicateRate.getUpperBoundsOnTargetDuplicateRate()) };
				String[] line = new String[totalColumns];

				Map<Integer, ProbeCoverageBounds> boundsByCoverage = coverageAndDuplicateRate.getCoverageByCoverageDepth();

				for (int i = 0; i < totalColumns; i++) {
					if (i < beginningLine.length) {
						line[i] = beginningLine[i];
					} else {
						int indexInCoverageArray = (i - beginningLine.length) / 2;
						boolean isFirstEntryForCoverage = (i - beginningLine.length) % 2 == 0;

						if (isFirstEntryForCoverage) {
							line[i] = "" + format.format(boundsByCoverage.get(coverageDepths[indexInCoverageArray]).getLowerBoundsCoverage());
						} else {
							line[i] = "" + format.format(boundsByCoverage.get(coverageDepths[indexInCoverageArray]).getUpperBoundsCoverage());
						}
					}
				}

				readSubSampleReport.writeLine((Object[]) line);
			}

			readSubSampleReport.close();
			probeSubSampleReport.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		long end = System.currentTimeMillis();
		System.out.println(DateUtil.convertMillisecondsToHHMMSS(end - start));

	}

	private static Map<String, int[]> loadProbeTallies(File probeTalliesFile) throws IOException {
		Map<String, int[]> probeTallies = new HashMap<String, int[]>();

		try (BufferedReader reader = new BufferedReader(new FileReader(probeTalliesFile))) {

			String line;
			while ((line = reader.readLine()) != null) {
				String[] probeAndValues = line.split(StringUtil.TAB);
				String probe = "";
				if (probeAndValues.length > 0) {
					probe = probeAndValues[0];
				}
				int[] values = new int[probeAndValues.length - 1];
				for (int i = 1; i < probeAndValues.length; i++) {
					String uidNameAndCount = probeAndValues[i];
					String[] nameAndCount = uidNameAndCount.split(":");
					if (nameAndCount.length == 2) {
						values[i - 1] = Integer.parseInt(nameAndCount[1]);
					}
				}
				probeTallies.put(probe, values);
			}
		}

		return probeTallies;
	}

	private static class CoverageAndDuplicateRate {
		private final Map<Integer, ProbeCoverageBounds> coverageByCoverageDepth;
		private final double lowerBoundsRawDuplicateRate;
		private final double upperBoundsRawDuplicateRate;
		private final double lowerBoundsOnTargetReads;
		private final double upperBoundsOnTargetReads;
		private final double lowerBoundsOnTargetDuplicateRate;
		private final double upperBoundsOnTargetDuplicateRate;

		public CoverageAndDuplicateRate(Map<Integer, ProbeCoverageBounds> coverageByCoverageDepth, double lowerBoundsRawDuplicateRate, double upperBoundsRawDuplicateRate,
				double lowerBoundsOnTargetReads, double upperBoundsOnTargetReads, double lowerBoundsOnTargetDuplicateRate, double upperBoundsOnTargetDuplicateRate) {
			super();
			this.coverageByCoverageDepth = coverageByCoverageDepth;
			this.lowerBoundsRawDuplicateRate = lowerBoundsRawDuplicateRate;
			this.upperBoundsRawDuplicateRate = upperBoundsRawDuplicateRate;
			this.lowerBoundsOnTargetReads = lowerBoundsOnTargetReads;
			this.upperBoundsOnTargetReads = upperBoundsOnTargetReads;
			this.lowerBoundsOnTargetDuplicateRate = lowerBoundsOnTargetDuplicateRate;
			this.upperBoundsOnTargetDuplicateRate = upperBoundsOnTargetDuplicateRate;
		}

		public Map<Integer, ProbeCoverageBounds> getCoverageByCoverageDepth() {
			return coverageByCoverageDepth;
		}

		public double getLowerBoundsRawDuplicateRate() {
			return lowerBoundsRawDuplicateRate;
		}

		public double getUpperBoundsRawDuplicateRate() {
			return upperBoundsRawDuplicateRate;
		}

		public double getLowerBoundsOnTargetDuplicateRate() {
			return lowerBoundsOnTargetDuplicateRate;
		}

		public double getUpperBoundsOnTargetDuplicateRate() {
			return upperBoundsOnTargetDuplicateRate;
		}

		public double getLowerBoundsOnTargetReads() {
			return lowerBoundsOnTargetReads;
		}

		public double getUpperBoundsOnTargetReads() {
			return upperBoundsOnTargetReads;
		}

		@Override
		public String toString() {
			return "CoverageAndDuplicateRate [coverageByCoverageDepth=" + coverageByCoverageDepth + ", lowerBoundsRawDuplicateRate=" + lowerBoundsRawDuplicateRate + ", upperBoundsRawDuplicateRate="
					+ upperBoundsRawDuplicateRate + ", lowerBoundsOnTargetDuplicateRate=" + lowerBoundsOnTargetDuplicateRate + ", upperBoundsOnTargetDuplicateRate=" + upperBoundsOnTargetDuplicateRate
					+ ", lowerBoundsOnTargetReads=" + lowerBoundsOnTargetReads + ", upperBoundsOnTargetReads=" + upperBoundsOnTargetReads + "]";
		}
	}

	public static enum Confidence {
		_80(80, 1.28155), _90(90, 1.64485), _95(95, 1.95996), _98(98, 2.33), _99(99, 2.57583), _99_5(99.5, 2.80703), _99_9(99.9, 3.29053);

		private final double confidence;
		private final double standardDeviationsAwayFromTheMean;

		private Confidence(double confidence, double standardDeviationsAwayFromTheMean) {
			this.confidence = confidence;
			this.standardDeviationsAwayFromTheMean = standardDeviationsAwayFromTheMean;
		}

		public double getConfidence() {
			return confidence;
		}

		public double getStandardDeviationsAwayFromTheMean() {
			return standardDeviationsAwayFromTheMean;
		}

	}

	private static class ProbeCoverageBounds {
		private final double upperBoundsCoverage;
		private final double lowerBoundsCoverage;

		public ProbeCoverageBounds(double upperBoundsCoverage, double lowerBoundsCoverage) {
			super();
			this.upperBoundsCoverage = upperBoundsCoverage;
			this.lowerBoundsCoverage = lowerBoundsCoverage;
		}

		public double getUpperBoundsCoverage() {
			return upperBoundsCoverage;
		}

		public double getLowerBoundsCoverage() {
			return lowerBoundsCoverage;
		}

		@Override
		public String toString() {
			return "ProbeCoverageBounds [upperBoundsCoverage=" + upperBoundsCoverage + ", lowerBoundsCoverage=" + lowerBoundsCoverage + "]";
		}
	}

	public static CoverageAndDuplicateRate getCoverageAndDuplicateRateForEachDesiredCoverageDepth(Map<String, int[]> uniqueReadTalliesByProbe, int totalRawReads, double percentSizeOfOriginal,
			int[] desiredCoverageDepths, TabDelimitedFileWriter probeSubSampleReport) {
		return getCoverageAndDuplicateRateForEachDesiredCoverageDepth(uniqueReadTalliesByProbe, totalRawReads, percentSizeOfOriginal, desiredCoverageDepths, Confidence._99_9, randomSeed,
				probeSubSampleReport);
	}

	private static interface IRead {

	}

	private static class OffTargetRead implements IRead {
	}

	private static class UniqueRead implements IRead {
		private final String probeName;
		private final int uniqueReadIndexForProbe;

		public UniqueRead(String probeName, int uniqueReadIndexForProbe) {
			super();
			this.probeName = probeName;
			this.uniqueReadIndexForProbe = uniqueReadIndexForProbe;
		}

		public String getProbeName() {
			return probeName;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((probeName == null) ? 0 : probeName.hashCode());
			result = prime * result + uniqueReadIndexForProbe;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			UniqueRead other = (UniqueRead) obj;
			if (probeName == null) {
				if (other.probeName != null)
					return false;
			} else if (!probeName.equals(other.probeName))
				return false;
			if (uniqueReadIndexForProbe != other.uniqueReadIndexForProbe)
				return false;
			return true;
		}
	}

	public static CoverageAndDuplicateRate getCoverageAndDuplicateRateForEachDesiredCoverageDepth(Map<String, int[]> uniqueReadTalliesByProbe, int totalRawReads, double percentSizeOfOriginal,
			int[] desiredCoverageDepths, Confidence confidence, long randomSeed, TabDelimitedFileWriter probeSubSampleReport) {

		Random random = new Random(randomSeed);

		// create a list containing all of the reads
		List<IRead> allReads = new ArrayList<IRead>(totalRawReads);

		int totalProbes = uniqueReadTalliesByProbe.size();

		for (String probeName : uniqueReadTalliesByProbe.keySet()) {
			int uniqueReadIndexForProbe = 0;
			for (int readQuantitiesForUniqueRead : uniqueReadTalliesByProbe.get(probeName)) {
				IRead uniqueRead = new UniqueRead(probeName, uniqueReadIndexForProbe);
				for (int i = 0; i < readQuantitiesForUniqueRead; i++) {
					allReads.add(uniqueRead);
				}
				uniqueReadIndexForProbe++;
			}
		}
		IRead offTargetRead = new OffTargetRead();
		while (allReads.size() < totalRawReads) {
			allReads.add(offTargetRead);
		}

		// the precision comes from the number of simulations which are based on the subsample size
		int numberOfSimulations = (int) Math.ceil((double) 1000 / (double) percentSizeOfOriginal);

		RunningStats uniqueReadsRunningStats = new RunningStats();

		RunningStats onTargetReadsRunningStats = new RunningStats();

		Map<String, RunningStats> runningStatsByProbe = new HashMap<String, RunningStats>();

		for (int simulationNumber = 0; simulationNumber < numberOfSimulations; simulationNumber++) {
			TallyMap<IRead> uniqueReadTally = subSample(allReads, percentSizeOfOriginal, random);

			int occurenceOfUniqueReadInSimulation = uniqueReadTally.getObjects().size();
			int offTargetReads = uniqueReadTally.getCount(offTargetRead);
			int onTargetReads = uniqueReadTally.sumOfAllBins - offTargetReads;
			if (offTargetReads > 0) {
				occurenceOfUniqueReadInSimulation--;
			}

			uniqueReadsRunningStats.addValue((double) occurenceOfUniqueReadInSimulation);

			onTargetReadsRunningStats.addValue((double) onTargetReads);

			// split the numberOfUniqueReads up by probe
			TallyMap<String> occurencesByProbe = new TallyMap<String>();

			for (Entry<IRead, Integer> entry : uniqueReadTally.getTalliesAsMap().entrySet()) {
				IRead read = entry.getKey();
				if (read instanceof UniqueRead) {
					int occurrences = entry.getValue();
					occurencesByProbe.addMultiple(((UniqueRead) read).getProbeName(), occurrences);
				}
			}

			for (String probeName : uniqueReadTalliesByProbe.keySet()) {
				int occurrences = occurencesByProbe.getCount(probeName);
				RunningStats runningStats = runningStatsByProbe.get(probeName);
				if (runningStats == null) {
					runningStats = new RunningStats();
				}
				runningStats.addValue(occurrences);
				runningStatsByProbe.put(probeName, runningStats);
			}

		}

		double meanOfUniqueReads = uniqueReadsRunningStats.getCurrentMean();
		double standardDeviationOfUniqueReads = uniqueReadsRunningStats.getCurrentStandardDeviation();

		double lowerBoundsOfUniqueReads = meanOfUniqueReads - (standardDeviationOfUniqueReads * confidence.standardDeviationsAwayFromTheMean);
		double upperBoundsOfUniqueReads = meanOfUniqueReads + (standardDeviationOfUniqueReads * confidence.standardDeviationsAwayFromTheMean);

		double meanOfOnTargetReads = onTargetReadsRunningStats.getCurrentMean();
		double standardDeviationOfOnTargetReads = onTargetReadsRunningStats.getCurrentStandardDeviation();

		double lowerBoundsOnTargetReads = meanOfOnTargetReads - (standardDeviationOfOnTargetReads * confidence.standardDeviationsAwayFromTheMean);
		double upperBoundsOnTargetReads = meanOfOnTargetReads + (standardDeviationOfOnTargetReads * confidence.standardDeviationsAwayFromTheMean);

		double[] lowerBoundsOfUniqueReadsByProbe = new double[totalProbes];
		double[] upperBoundsOfUniqueReadsByProbe = new double[totalProbes];

		int totalSubSampledRawReads = (int) ((double) totalRawReads * (double) percentSizeOfOriginal / 100.0);

		int probeIndex = 0;
		for (String probeName : uniqueReadTalliesByProbe.keySet()) {
			RunningStats runningStats = runningStatsByProbe.get(probeName);
			if (runningStats != null) {
				double meanOfUniqueReadsByProbe = runningStats.getCurrentMean();
				double standardDeviationOfUniqueReadsByProbe = runningStats.getCurrentStandardDeviation();

				double standardErrorAboutTheMean = (standardDeviationOfUniqueReadsByProbe * confidence.standardDeviationsAwayFromTheMean);
				lowerBoundsOfUniqueReadsByProbe[probeIndex] = Math.max(meanOfUniqueReadsByProbe - standardErrorAboutTheMean, 0.0);
				upperBoundsOfUniqueReadsByProbe[probeIndex] = meanOfUniqueReadsByProbe + standardErrorAboutTheMean;
				if (probeSubSampleReport != null) {
					probeSubSampleReport.writeLine(totalRawReads, percentSizeOfOriginal, totalSubSampledRawReads, numberOfSimulations, meanOfUniqueReadsByProbe, standardErrorAboutTheMean,
							standardDeviationOfUniqueReadsByProbe, lowerBoundsOfUniqueReadsByProbe[probeIndex], upperBoundsOfUniqueReadsByProbe[probeIndex]);
				}
			} else {
				if (probeSubSampleReport != null) {
					probeSubSampleReport.writeLine(totalRawReads, percentSizeOfOriginal, totalSubSampledRawReads, numberOfSimulations, "NaN", "NaN", "NaN", "NaN", "NaN");
				}
			}

			probeIndex++;
		}

		Map<Integer, ProbeCoverageBounds> coverageByCoverageDepth = new LinkedHashMap<Integer, ProbeCoverageBounds>();
		for (int desiredCoverageDepth : desiredCoverageDepths) {
			int probesMeetingCoverageThresholdAtLowerBounds = 0;
			int probesMeetingCoverageThresholdAtUpperBounds = 0;
			for (int i = 0; i < totalProbes; i++) {
				double upperBoundsForProbe = upperBoundsOfUniqueReadsByProbe[i];
				double lowerBoundsForProbe = lowerBoundsOfUniqueReadsByProbe[i];

				if (upperBoundsForProbe > desiredCoverageDepth) {
					probesMeetingCoverageThresholdAtUpperBounds++;
				}

				if (lowerBoundsForProbe > desiredCoverageDepth) {
					probesMeetingCoverageThresholdAtLowerBounds++;
				}
			}
			double upperBoundsCoverage = (double) probesMeetingCoverageThresholdAtUpperBounds / (double) totalProbes;
			double lowerBoundsCoverage = (double) probesMeetingCoverageThresholdAtLowerBounds / (double) totalProbes;

			coverageByCoverageDepth.put(desiredCoverageDepth, new ProbeCoverageBounds(upperBoundsCoverage, lowerBoundsCoverage));
		}

		int totalNewReads = (int) Math.round((double) totalRawReads * (double) percentSizeOfOriginal / (double) 100);
		double lowerBoundsRawDuplicateRate = (totalNewReads - upperBoundsOfUniqueReads) / totalNewReads;
		double upperBoundsRawDuplicateRate = (totalNewReads - lowerBoundsOfUniqueReads) / totalNewReads;

		double lowerBoundsOnTargetDuplicateRate = (upperBoundsOnTargetReads - upperBoundsOfUniqueReads) / upperBoundsOnTargetReads;
		double upperBoundsOnTargetDuplicateRate = (lowerBoundsOnTargetReads - lowerBoundsOfUniqueReads) / lowerBoundsOnTargetReads;

		return new CoverageAndDuplicateRate(coverageByCoverageDepth, lowerBoundsRawDuplicateRate, upperBoundsRawDuplicateRate, lowerBoundsOnTargetReads, upperBoundsOnTargetReads,
				lowerBoundsOnTargetDuplicateRate, upperBoundsOnTargetDuplicateRate);
	}

	public static <T> TallyMap<T> subSample(List<T> population, double percentSizeOfOriginal, Random random) {
		if (percentSizeOfOriginal > 100 || percentSizeOfOriginal < 0) {
			throw new IllegalStateException("percentSizeOfOriginal[" + percentSizeOfOriginal + "] must be between 0 and 100.");
		}

		TallyMap<T> tallyMap = new TallyMap<T>();

		int newSize = (int) Math.floor((double) population.size() * (double) percentSizeOfOriginal / 100.0);

		if (newSize == population.size()) {
			tallyMap.addAll(population);
		} else {
			Set<Integer> indexes = new TreeSet<Integer>();

			while (indexes.size() < newSize) {
				indexes.add(random.nextInt(population.size()));
			}

			for (Integer index : indexes) {
				tallyMap.add(population.get(index));
			}
		}

		return tallyMap;
	}

	public static void generateProbeMap(int[] tallies, String title, File outputFile, ImageType imageType) throws Exception {

		double maxValue = 0;

		for (int tally : tallies) {
			maxValue = Math.max(maxValue, tally);
		}

		int width = tallies.length;
		int height = (int) Math.round(maxValue);

		Graphics2DImageExporter imageExporter = new Graphics2DImageExporter(imageType, width, height);
		Graphics2D graphics = imageExporter.getGraphics2D();

		graphics.setPaint(Color.BLUE);
		graphics.setBackground(Color.red);
		FontMetrics metrics = graphics.getFontMetrics(graphics.getFont());
		int textHeight = metrics.getHeight();

		graphics.drawString(title, 10, 10 + textHeight);

		for (int tallyIndex = 0; tallyIndex < tallies.length; tallyIndex++) {
			int tally = tallies[tallyIndex];
			graphics.drawLine(tallyIndex, height, tallyIndex, height - tally);
		}
		imageExporter.exportImage(outputFile.getAbsolutePath());

	}

}
