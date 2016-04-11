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

	public void addRange(int startPosition, int stopPosition) {
		if (startPosition > stopPosition) {
			int temp = stopPosition;
			stopPosition = startPosition;
			startPosition = temp;
		}
		int index = findIndexOfFirstEqualOrLargerValue(startPosition);

		// insert an index for the start position
		if (startPosition != range.get(index).getPosition()) {
			int oldTally = range.get(index - 1).getTally();
			range.add(index, new StartWithTally(startPosition, oldTally));
		}

		// walk through and increment the tallies until a number that is larger than the stop position is found
		while ((index < range.size()) && range.get(index).getPosition() < stopPosition) {
			range.get(index).incrementTally();
			index++;
		}
		if (range.get(index).getPosition() != stopPosition) {
			// the preceding tally has already been incremented so subtract one
			int oldTally = range.get(index - 1).getTally() - 1;
			range.add(index, new StartWithTally(stopPosition, oldTally));
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

	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
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
				stringBuilder.append("" + lastStart + " to " + lastStop + " : " + lastTally + StringUtil.NEWLINE);
			}
		}
		return stringBuilder.toString();
	}

	public class StartWithTally {

		private final int position;
		private int tally;

		public StartWithTally(int index) {
			super();
			this.position = index;
			this.tally = 0;
		}

		public StartWithTally(int position, int tally) {
			super();
			this.position = position;
			this.tally = tally;
		}

		public void incrementTally() {
			tally++;
		}

		public int getPosition() {
			return position;
		}

		public int getTally() {
			return tally;
		}

		public void incrementTally(int count) {
			tally += count;
		}
	}

	public static void main(String[] args) {
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
		System.out.println(tallyRange);
	}

}
