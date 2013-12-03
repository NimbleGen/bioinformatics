package com.roche.sandbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.roche.sequencing.bioinformatics.common.mapping.SimpleMapper;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class MultipleSequenceAligner {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		File file = new File("D:\\hotspot_cancer_1\\results3\\_1-25pM50ng6hr_unmapped_unassigned_read_ones.txt");
		align(file, 10);
	}

	private static class Pair {
		private final int indexOne;
		private final int indexTwo;
		private final int matches;

		public Pair(int indexOne, int indexTwo, int matches) {
			super();
			this.indexOne = indexOne;
			this.indexTwo = indexTwo;
			this.matches = matches;
		}

		public int getIndexOne() {
			return indexOne;
		}

		public int getIndexTwo() {
			return indexTwo;
		}
	}

	private static void align(File file, int matchTreshold) throws FileNotFoundException, IOException {
		String[] sequences = readSequences(file);

		SimpleMapper<Integer> mapper = new SimpleMapper<Integer>(7, 2, 1, 5);

		List<Pair> pairs = new ArrayList<Pair>();

		int i = 0;
		for (String sequence : sequences) {
			mapper.addReferenceSequence(new IupacNucleotideCodeSequence(sequence), i);
			i++;
		}

		System.out.println("done creating initial sequence map");

		i = 0;
		for (String sequence : sequences) {
			TallyMap<Integer> matches = mapper.getReferenceTallyMap(new IupacNucleotideCodeSequence(sequence));

			for (Entry<Integer, Integer> entry : matches.getObjectsSortedFromMostTalliesToLeast()) {
				if (entry.getKey() != i && entry.getValue() > matchTreshold) {
					pairs.add(new Pair(entry.getKey(), i, entry.getValue()));
				}
			}
			i++;
		}

		System.out.println("done creating map pairs");

		Collections.sort(pairs, new Comparator<Pair>() {
			@Override
			public int compare(Pair o1, Pair o2) {
				return Integer.compare(o1.matches, o2.matches);
			}
		});

		List<Set<Integer>> bins = new ArrayList<Set<Integer>>();

		for (Pair pair : pairs) {

			Set<Integer> binsContainingOne = new HashSet<Integer>();
			Set<Integer> binsContainingTwo = new HashSet<Integer>();

			int binIndex = 0;
			// check if any of the bins already contain these indexes
			for (Set<Integer> bin : bins) {
				if (bin != null) {
					if (bin.contains(pair.getIndexOne())) {
						binsContainingOne.add(binIndex);
					}

					if (bin.contains(pair.getIndexTwo())) {
						binsContainingTwo.add(binIndex);
					}
				}
				binIndex++;
			}

			if (binsContainingOne.size() == 0 && binsContainingTwo.size() == 0) {
				Set<Integer> newBin = new HashSet<Integer>();
				newBin.add(pair.getIndexOne());
				newBin.add(pair.getIndexTwo());
				bins.add(newBin);
			}
			if (binsContainingOne.size() == 1 && binsContainingTwo.size() == 0) {
				Integer containingBinIndex = binsContainingOne.iterator().next();
				bins.get(containingBinIndex).add(pair.getIndexTwo());
			}
			if (binsContainingOne.size() == 0 && binsContainingTwo.size() == 1) {
				Integer containingBinIndex = binsContainingTwo.iterator().next();
				bins.get(containingBinIndex).add(pair.getIndexOne());
			}

			if (binsContainingOne.size() == 1 && binsContainingTwo.size() == 1) {
				Integer oneContainingBinIndex = binsContainingOne.iterator().next();
				Integer twoContainingBinIndex = binsContainingTwo.iterator().next();
				if (oneContainingBinIndex != twoContainingBinIndex) {
					// the two bins need to be merged
					bins.get(oneContainingBinIndex).addAll(bins.get(twoContainingBinIndex));
					bins.set(twoContainingBinIndex, null);
				}// note nothing needs to be done if they are already in the same bin
			}
			if (binsContainingOne.size() > 1 || binsContainingTwo.size() > 1) {
				throw new IllegalStateException("A number can only be represented in one bin at any given time.");
			}
		}

		System.out.println("here");

	}

	private static String[] readSequences(File file) throws FileNotFoundException, IOException {
		List<String> sequences = new ArrayList<>(1000000);
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			// skip the first line
			reader.readLine();

			while ((line = reader.readLine()) != null) {
				sequences.add(line.split(StringUtil.TAB)[0]);
			}
		}
		return sequences.toArray(new String[0]);
	}
}
