package com.roche.sequencing.bioinformatics.common.mapping;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class CoverageCalculatorWithTracking<V> {

	private final LinkedList<StartWithTally> range;

	public CoverageCalculatorWithTracking() {
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

	public void addRange(int startPosition, int stopPosition, V tallyItem) {
		int index = findIndexOfFirstEqualOrLargerValue(startPosition);

		// insert an index for the start position
		if (startPosition != range.get(index).getPosition()) {
			List<V> oldTallyItems = range.get(index - 1).getTallyItems();
			range.add(index, new StartWithTally(startPosition, oldTallyItems));
		}

		List<V> oldTallyItems = null;
		// walk through and increment the tallies until a number that is larger than the stop position is found
		while ((index < range.size()) && range.get(index).getPosition() < stopPosition) {
			oldTallyItems = range.get(index).getTallyItems();
			range.get(index).incrementTally(tallyItem);
			index++;
		}

		if (range.get(index).getPosition() != stopPosition) {
			range.add(index, new StartWithTally(stopPosition, oldTallyItems));
		}
	}

	public List<RangeWithTallyAndTracking<V>> getTalliedRanges() {
		List<RangeWithTallyAndTracking<V>> talliedRanges = new ArrayList<RangeWithTallyAndTracking<V>>();
		Integer lastStart = null;
		Integer start = null;
		Integer lastStop = null;
		int lastTally = 0;
		List<V> lastTallyItems = null;
		int tally = 0;
		for (StartWithTally indexTally : range) {
			lastStart = start;
			start = indexTally.getPosition();
			lastStop = start;
			lastTally = tally;
			lastTallyItems = indexTally.getTallyItems();
			tally = indexTally.getTally();
			if (lastTally != 0) {
				talliedRanges.add(new RangeWithTallyAndTracking<V>(lastStart, lastStop, lastTallyItems));
			}
		}

		return talliedRanges;
	}

	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		Integer lastStart = null;
		Integer start = null;
		Integer lastStop = null;
		List<V> lastItems = null;
		int lastTally = 0;
		int tally = 0;
		for (StartWithTally indexTally : range) {
			lastStart = start;
			start = indexTally.getPosition();
			lastStop = start;
			lastTally = tally;
			lastItems = indexTally.getTallyItems();
			tally = indexTally.getTally();
			if (lastTally != 0) {
				stringBuilder.append("" + lastStart + " to " + lastStop + " : " + lastTally + " ");
				for (V item : lastItems) {
					stringBuilder.append(item + StringUtil.TAB);
				}
				stringBuilder.append(StringUtil.NEWLINE);
			}
		}
		return stringBuilder.toString();
	}

	public class StartWithTally {

		private final int position;
		private List<V> tallyItems;

		public StartWithTally(int index) {
			super();
			this.position = index;
			this.tallyItems = new ArrayList<V>();
		}

		public StartWithTally(int position, List<V> tallyItems) {
			super();
			this.position = position;
			this.tallyItems = new ArrayList<V>(tallyItems);
		}

		public void incrementTally(V tallyItem) {
			tallyItems.add(tallyItem);
		}

		public int getPosition() {
			return position;
		}

		public int getTally() {
			return tallyItems.size();
		}

		public List<V> getTallyItems() {
			return new ArrayList<V>(tallyItems);
		}
	}

	public static void main(String[] args) {
		CoverageCalculatorWithTracking<String> tallyRange = new CoverageCalculatorWithTracking<String>();
		tallyRange.addRange(1, 3, "one");
		tallyRange.addRange(7, 10, "two");
		tallyRange.addRange(2, 4, "three");
		tallyRange.addRange(6, 9, "four");
		tallyRange.addRange(1, 10, "five");
		tallyRange.addRange(2, 4, "six");
		System.out.println(tallyRange);
	}

}
