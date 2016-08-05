package com.roche.sequencing.bioinformatics.common.mapping;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class CoverageCalculator {

	private final LinkedList<StartWithTally> range;

	public CoverageCalculator() {
		range = new LinkedList<StartWithTally>();
		range.add(new StartWithTally(-Integer.MAX_VALUE));
		range.add(new StartWithTally(Integer.MAX_VALUE));
	}

	// private int findIndexOfFirstEqualOrLargerValueByBruteForce(int value) {
	// int index = 0;
	// while ((index < range.size()) && range.get(index).getPosition() < value) {
	// index++;
	// }
	// return index;
	// }

	private int findIndexOfFirstEqualOrLargerValue(int position) {
		return binarySearch(0, range.size() - 1, position);
	}

	private int binarySearch(int minIndex, int maxIndex, int position) {
		// System.out.println("min:" + minIndex + "(" + range.get(minIndex).getPosition() + ") max:" + maxIndex + " (" + range.get(maxIndex).getPosition() + ")");
		int foundIndex = 0;
		int midIndex = minIndex + ((maxIndex - minIndex) / 2);
		int indexAboveMidIndex = midIndex + 1;
		// System.out.println(range.get(midIndex).getPosition() + " < " + position + " <= " + range.get(indexAboveMidIndex).getPosition());
		if (range.get(midIndex).getPosition() < position && range.get(indexAboveMidIndex).getPosition() >= position) {
			foundIndex = indexAboveMidIndex;
		} else if (range.get(indexAboveMidIndex).getPosition() > position) {
			foundIndex = binarySearch(minIndex, midIndex, position);
		} else if (range.get(midIndex).getPosition() <= position) {
			foundIndex = binarySearch(midIndex, maxIndex, position);
		} else {
			throw new AssertionError();
		}

		return foundIndex;
	}

	public void addRange(int startPositionInclusive, int stopPositionInclusive) {
		int stopPositionExclusive = stopPositionInclusive + 1;
		if (startPositionInclusive > stopPositionExclusive) {
			int temp = stopPositionExclusive;
			stopPositionExclusive = startPositionInclusive;
			startPositionInclusive = temp;
		}
		int index = findIndexOfFirstEqualOrLargerValue(startPositionInclusive);

		// insert an index for the start position
		if (startPositionInclusive != range.get(index).getPosition()) {
			int oldTally = range.get(index - 1).getTally();
			range.add(index, new StartWithTally(startPositionInclusive, oldTally));
		}

		// walk through and increment the tallies until a number that is larger than the stop position is found
		while ((index < range.size()) && range.get(index).getPosition() < stopPositionExclusive) {
			range.get(index).incrementTally();
			index++;
		}
		if (range.get(index).getPosition() != stopPositionExclusive) {
			// the preceding tally has already been incremented so subtract one
			int oldTally = range.get(index - 1).getTally() - 1;
			range.add(index, new StartWithTally(stopPositionExclusive, oldTally));
		}
	}

	public List<RangeWithTally> getTalliedRanges() {
		List<RangeWithTally> talliedRanges = new ArrayList<RangeWithTally>();
		Integer lastStart = null;
		Integer start = null;
		Integer lastStop = null;
		int lastTally = 0;
		int tally = 0;
		for (StartWithTally indexTally : range) {
			lastStart = start;
			start = indexTally.getPosition();
			lastStop = start;
			lastTally = tally;
			tally = indexTally.getTally();
			if (lastTally != 0) {
				talliedRanges.add(new RangeWithTally(lastStart, lastStop, lastTally));
			}
		}

		return talliedRanges;
	}

	public CoverageStats getCoverageStatsForRegionOfInterest(int regionOfInterestStartInclusive, int regionOfInterestStopInclusive) {
		int totalUniqueBasedCoveredInRegionOfInterest = 0;
		int totalRedundantBasesCoveredInRegionOfInterest = 0;

		int totalUniqueBasesOutsideRegionOfInterest = 0;
		int totalRedundantBasesOutsideRegionOfInterest = 0;

		int totalBasesInTally = 0;
		int totalUniqueBasesInTally = 0;

		List<RangeWithTally> talliedRanges = getTalliedRanges();
		for (RangeWithTally rangeWithTally : talliedRanges) {
			int rangeStartInclusive = rangeWithTally.getStart();
			int rangeStopExclusive = rangeWithTally.getStop();
			int count = rangeWithTally.getCount();

			int possibleOverlapStartInclusive = Math.max(regionOfInterestStartInclusive, rangeStartInclusive);
			Integer possibleOverlapStopInclusive = null;

			totalBasesInTally += ((rangeStopExclusive - rangeStartInclusive) * count);
			totalUniqueBasesInTally += ((rangeStopExclusive - rangeStartInclusive));
			possibleOverlapStopInclusive = Math.min(regionOfInterestStopInclusive, rangeStopExclusive - 1);

			boolean isOverlap = possibleOverlapStartInclusive <= possibleOverlapStopInclusive;

			if (isOverlap) {
				int coveredBasesInRegion = (possibleOverlapStopInclusive - possibleOverlapStartInclusive + 1);

				totalUniqueBasedCoveredInRegionOfInterest += coveredBasesInRegion;
				if (count > 1) {
					totalRedundantBasesCoveredInRegionOfInterest += (count - 1) * coveredBasesInRegion;
				}
			}
		}

		totalUniqueBasesOutsideRegionOfInterest = totalUniqueBasesInTally - totalUniqueBasedCoveredInRegionOfInterest;
		totalRedundantBasesOutsideRegionOfInterest = totalBasesInTally - totalUniqueBasedCoveredInRegionOfInterest - totalUniqueBasesOutsideRegionOfInterest
				- totalRedundantBasesCoveredInRegionOfInterest;

		return new CoverageStats(totalUniqueBasedCoveredInRegionOfInterest, totalRedundantBasesCoveredInRegionOfInterest, totalUniqueBasesOutsideRegionOfInterest,
				totalRedundantBasesOutsideRegionOfInterest, totalBasesInTally);
	}

	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		Integer lastStart = null;
		Integer start = null;
		Integer lastStop = null;
		int lastTally = 0;
		int tally = 0;
		int size = range.size();
		int i = 1;
		for (StartWithTally indexTally : range) {
			i++;
			boolean isStopInclusive = (i == size);
			lastStart = start;
			start = indexTally.getPosition();
			lastStop = start;
			lastTally = tally;
			tally = indexTally.getTally();
			if (lastTally != 0) {
				if (isStopInclusive) {
					stringBuilder.append("" + lastStart + "(inclusive) to " + (lastStop - 1) + "(inclusive) : " + lastTally + StringUtil.NEWLINE);
				} else {
					stringBuilder.append("" + lastStart + "(inclusive) to " + lastStop + "(exclusive) : " + lastTally + StringUtil.NEWLINE);
				}

			}
		}
		return stringBuilder.toString();
	}

	private class StartWithTally {

		private final int position;
		private int tally;

		private StartWithTally(int index) {
			super();
			this.position = index;
			this.tally = 0;
		}

		private StartWithTally(int position, int tally) {
			super();
			this.position = position;
			this.tally = tally;
		}

		private void incrementTally() {
			tally++;
		}

		public int getPosition() {
			return position;
		}

		public int getTally() {
			return tally;
		}
	}

	public static void main(String[] args) {
		test5();
	}

	public static void test1() {
		CoverageCalculator tallyRange = new CoverageCalculator();
		tallyRange.addRange(1, 3);
		tallyRange.addRange(7, 10);
		tallyRange.addRange(2, 4);
		tallyRange.addRange(6, 9);
		tallyRange.addRange(1, 10);
		tallyRange.addRange(2, 4);
		tallyRange.addRange(1, 3);
		tallyRange.addRange(7, 10);
		tallyRange.addRange(2, 4);
		tallyRange.addRange(6, 9);
		tallyRange.addRange(1, 10);
		tallyRange.addRange(2, 4);

		System.out.println(tallyRange.getCoverageStatsForRegionOfInterest(4, 6));

		System.out.println(tallyRange);
	}

	public static void test2() {
		CoverageCalculator tallyRange = new CoverageCalculator();
		tallyRange.addRange(1355, 1501);
		tallyRange.addRange(1501, 1667);
		tallyRange.addRange(1668, 1820);
		tallyRange.addRange(1820, 1974);
		tallyRange.addRange(1971, 2122);
		tallyRange.addRange(2123, 2278);
		tallyRange.addRange(2273, 2413);
		tallyRange.addRange(2408, 2564);
		tallyRange.addRange(2539, 2590);
		tallyRange.addRange(2592, 2698);

		System.out.println(tallyRange.getCoverageStatsForRegionOfInterest(1358, 2574));

		System.out.println(tallyRange);
	}

	public static void test3() {
		CoverageCalculator tallyRange = new CoverageCalculator();
		tallyRange.addRange(32906355, 32906501);
		tallyRange.addRange(32906501, 32906667);
		tallyRange.addRange(32906668, 32906820);
		System.out.println(tallyRange.getCoverageStatsForRegionOfInterest(32906355, 32906820));
		System.out.println(tallyRange);
	}

	public static void test4() {
		CoverageCalculator tallyRange = new CoverageCalculator();
		tallyRange.addRange(1, 5);
		tallyRange.addRange(6, 10);
		System.out.println(tallyRange.getCoverageStatsForRegionOfInterest(1, 10));
		System.out.println(tallyRange);
	}

	public static void test5() {
		CoverageCalculator tallyRange = new CoverageCalculator();
		tallyRange.addRange(32906357, 32906484);
		tallyRange.addRange(32906484, 32906640);
		tallyRange.addRange(32906637, 32906786);
		tallyRange.addRange(32906784, 32906923);
		tallyRange.addRange(32906924, 32907075);
		tallyRange.addRange(32907074, 32907234);
		tallyRange.addRange(32907234, 32907367);
		tallyRange.addRange(32907368, 32907523);
		tallyRange.addRange(32907483, 32907590);

		System.out.println(tallyRange.getCoverageStatsForRegionOfInterest(32906408, 32907524));
		System.out.println(tallyRange);
	}

}
