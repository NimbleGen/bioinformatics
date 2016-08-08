package com.roche.sequencing.bioinformatics.common.genome;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.NucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.SimpleNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.BitSetUtil;

public class NMerGenomeTracker {

	private final BitSet moreThanOneBitset;
	private final int nMerLength;

	private NMerGenomeTracker(BitSet bitset, int nMerLength) {
		super();
		this.moreThanOneBitset = bitset;
		this.nMerLength = nMerLength;
	}

	public boolean isContainedMoreThanOnce(ISequence sequence) {
		if (sequence.size() != nMerLength) {
			throw new IllegalStateException("The provided length of the sequence must be [" + nMerLength + "] but was found to be [" + sequence.size() + "].");
		}

		int bitIndex = getNumber(sequence);
		return isContainedMoreThanOnce(bitIndex);
	}

	boolean isContainedMoreThanOnce(int bitIndexForSequence) {
		return moreThanOneBitset.get(bitIndexForSequence);
	}

	// TODO this is a work in progress
	// and of not high importance since technically only only an nmer of approximately 14 bases
	// can be stored in a nice file size. This is based on assuming that every permutation of 4 bases is given a unique but
	// calculatable number which represents a bit position to tell if there are more than one sequences of this
	// size represented. So the calculation for this size is:
	// 4^14 / 8 bits per byte / 1000 bytes per kb / 1000 kb per mb = 33.5544 mb
	// 4^15 / 8 bits per byte / 1000 bytes per kb / 1000 kb per mb = 134.2177 mb
	// 4^16 / 8 bits per byte / 1000 bytes per kb / 1000 kb per mb = 536.8709 mb
	// can't do 17 currently because of overflow on int datatype and limitations of bitset -- would need to create new datatype that
	// houses multiple bitsets

	public static NMerGenomeTracker createNMerGenomeTrackerFromFastaDirectory(File fastaDirectoryOrFile, File outputNMerGenomeTrackerFile, int nMerLength, boolean includeReverseCompliment)
			throws FileNotFoundException, IOException {
		NMerGenomeTracker returnTracker = null;
		int numberOfPermutations = (int) Math.pow(4, nMerLength);
		BitSet moreThanOneBitset = new BitSet(numberOfPermutations);

		FastaProcessor fastaProcessor = new FastaProcessor(nMerLength, includeReverseCompliment);

		FastaDirectoryParser.parseFastaFile(fastaDirectoryOrFile, fastaProcessor);

		BitSetUtil.writeBitSetToFile(fastaProcessor.getMoreThanOneBitSet(), outputNMerGenomeTrackerFile);
		returnTracker = new NMerGenomeTracker(moreThanOneBitset, nMerLength);

		return returnTracker;
	}

	public void writeToFile(File outputFile) throws IOException {
		BitSetUtil.writeBitSetToFile(moreThanOneBitset, outputFile);
	}

	public static NMerGenomeTracker createNMerGenomeTrackerFromInputStream(InputStream nMerGenomeTracker, int nMerLength) throws IOException {
		BitSet bitset = BitSetUtil.readBitSetFromInputStream(nMerGenomeTracker);
		return new NMerGenomeTracker(bitset, nMerLength);
	}

	public static NMerGenomeTracker createNMerGenomeTrackerFromFile(File nMerGenomeTrackerFile, int nMerLength) throws IOException {
		BitSet bitset = BitSetUtil.readBitSetFromFile(nMerGenomeTrackerFile);
		return new NMerGenomeTracker(bitset, nMerLength);
	}

	private final static String[] BASES = new String[] { "A", "C", "G", "T" };

	private static ISequence getSequence(long value, int sequenceLength) {
		StringBuilder sequenceAsStringBuilder = new StringBuilder();

		for (int i = sequenceLength - 1; i >= 0; i--) {
			long base = (long) Math.pow(4, i);
			int count = 0;
			while (value >= base) {
				value -= base;
				count++;
			}
			sequenceAsStringBuilder.append(BASES[count]);
		}
		return new NucleotideCodeSequence(sequenceAsStringBuilder.toString());
	}

	private static int getNumber(ISequence sequence) {
		int value = 0;
		for (int i = 0; i < sequence.size(); i++) {
			long base = (long) Math.pow(4, i);
			String ntBase = sequence.getCodeAt(sequence.size() - i - 1).toString();
			baseLoop: for (int baseIndex = 0; baseIndex < BASES.length; baseIndex++) {
				if (ntBase.equals(BASES[baseIndex])) {
					value += baseIndex * base;
					break baseLoop;
				}
			}
		}
		return value;
	}

	private static class FastaProcessor implements IParsedFastaProcessor {
		private final BitSet oneOrMoreBitset;
		private final BitSet moreThanOneBitset;
		private final int nMerLength;
		private final boolean includeReverseCompliments;

		public FastaProcessor(int nMerLength, boolean includeReverseCompliments) {
			super();
			int numberOfPermutations = (int) Math.pow(4, nMerLength);
			oneOrMoreBitset = new BitSet(numberOfPermutations);
			moreThanOneBitset = new BitSet(numberOfPermutations);
			this.nMerLength = nMerLength;
			this.includeReverseCompliments = includeReverseCompliments;
		}

		@Override
		public void sequenceProcessed(String containerName, SimpleNucleotideCodeSequence sequence) {
			ISequence subSequence = sequence.subSequence(0, nMerLength - 1);
			for (int nextBaseIndex = nMerLength; nextBaseIndex <= sequence.size(); nextBaseIndex++) {
				int bitIndex = getNumber(subSequence);
				if (oneOrMoreBitset.get(bitIndex)) {
					moreThanOneBitset.set(bitIndex);
				}
				oneOrMoreBitset.set(bitIndex);

				if (nextBaseIndex < sequence.size()) {
					subSequence = subSequence.subSequence(1, subSequence.size() - 1);
					subSequence.append(sequence.subSequence(nextBaseIndex, nextBaseIndex));
				}
			}
		}

		public BitSet getMoreThanOneBitSet() {
			return moreThanOneBitset;
		}

		@Override
		public void doneProcessing() {
			int numberOfPermutations = (int) Math.pow(4, nMerLength);

			// go through the reverse compliment of every single found sequence
			if (includeReverseCompliments) {
				BitSet rcOneOrMoreBitSet = new BitSet(numberOfPermutations);
				for (int i = 0; i < numberOfPermutations; i++) {
					if (oneOrMoreBitset.get(i)) {
						ISequence rc = getSequence(i, nMerLength).getReverseCompliment();
						int newNumber = getNumber(rc);
						rcOneOrMoreBitSet.set(newNumber);
						if (oneOrMoreBitset.get(newNumber)) {
							moreThanOneBitset.set(newNumber);
						}
					}
				}
				oneOrMoreBitset.or(rcOneOrMoreBitSet);
			}

			double moreThanOneNumbersFound = 0;
			double oneOrMoreNumbersFound = 0;
			for (int i = 0; i < numberOfPermutations; i++) {
				if (oneOrMoreBitset.get(i)) {
					oneOrMoreNumbersFound++;
				}
				if (moreThanOneBitset.get(i)) {
					moreThanOneNumbersFound++;
				}
			}
			double percentOfAllPossibleNMersFoundMultipleTimes = moreThanOneNumbersFound / (double) numberOfPermutations;
			double percentOfAllPossibleNMersFoundOneOrMoreTimes = oneOrMoreNumbersFound / (double) numberOfPermutations;
			double percentOfAllPossibleNMersFoundOnce = percentOfAllPossibleNMersFoundOneOrMoreTimes - percentOfAllPossibleNMersFoundMultipleTimes;
			double percentOfAllUniqueNMersOutOfAllFoundNmers = percentOfAllPossibleNMersFoundOnce / percentOfAllPossibleNMersFoundOneOrMoreTimes;
			System.out.println("percentOfAllPossibleNMersFoundMultipleTimes:" + percentOfAllPossibleNMersFoundMultipleTimes);
			System.out.println("percentOfAllPossibleNMersFoundOneOrMoreTimes:" + percentOfAllPossibleNMersFoundOneOrMoreTimes);
			System.out.println("percentOfAllPossibleNMersFoundOnce:" + percentOfAllPossibleNMersFoundOnce);
			System.out.println("percentOfAllUniqueNMersOutOfAllFoundNmers:" + percentOfAllUniqueNMersOutOfAllFoundNmers);

		}
	}

	public static void main(String[] args) throws IOException {
		// create();

		create(16, new File("D://kurts_space/sequence/hg38_all.fa"), new File("D://kurts_space/sequence/other/"), "all", true);
		create(16, new File("D://kurts_space/sequence/other/Homo_sapiens.GRCh38.cdna.all.fa"), new File("D://kurts_space/sequence/other/"), "cdna", false);
		create(16, new File("D://kurts_space/sequence/other/Homo_sapiens.GRCh38.cds.all.fa"), new File("D://kurts_space/sequence/other/"), "cds", false);
		create(16, new File("D://kurts_space/sequence/other/Homo_sapiens.GRCh38.ncrna.fa"), new File("D://kurts_space/sequence/other/"), "ncrna", false);
		create(16, new File("D://kurts_space/sequence/other/Homo_sapiens.GRCh38.pep.all.fa"), new File("D://kurts_space/sequence/other/"), "pep", false);
		// NMerGenomeTracker tracker1 = loadTracker(14, new File("D://kurts_space/sequence/other/"), "cdna");
		// NMerGenomeTracker tracker2 = loadTracker(14, new File("D://kurts_space/sequence/other/"), "cds");
		// NMerGenomeTracker tracker3 = loadTracker(14, new File("D://kurts_space/sequence/other/"), "ncrna");
		// NMerGenomeTracker tracker = NMerGenomeTracker.createNMerGenomeTrackerFromFile(new File("D://kurts_space//sequence//hg19_nmer_" + 14 + "_w_rc.nmer"), 14);
		//
		// NMerGenomeTracker combinedTracker = combineTrackers(tracker1, tracker2, tracker3, tracker);
		// printDetails(combinedTracker, 14);
		// combinedTracker.writeToFile(new File("D://kurts_space/sequence/other/hg19_nmer_14_genome_w_rc_and_transcripts.nmer"));

		// create2();
	}

	// private static void create() {
	// int nMerSize = 14;
	// boolean includeReverseCompliments = true;
	// // File sequenceDir = new File("Y://genomes/homo_sapiens/hg38_GRCh38/sequence/hg38_all.fa");
	// // File sequenceDir = new File("Y://genomes/homo_sapiens/hg19_GRCh37/sequence/original/");
	// File sequenceDir = new File("D://kurts_space/sequence/hg38_all.fa");
	// try {
	// String fileName = "hg19_nmer_" + nMerSize + ".nmer";
	// if (includeReverseCompliments) {
	// fileName = "hg19_nmer_" + nMerSize + "_w_rc.nmer";
	// }
	// NMerGenomeTracker.createNMerGenomeTrackerFromFastaDirectory(sequenceDir, new File("D://kurts_space/sequence/" + fileName), nMerSize, includeReverseCompliments);
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }

	private static void create(int nMerSize, File inputFile, File outputDir, String fileIdentifier, boolean includeReverseCompliments) {

		try {
			String fileName = fileIdentifier + "_hg19_nmer_" + nMerSize + ".nmer";
			if (includeReverseCompliments) {
				fileName = fileIdentifier + "_hg19_nmer_" + nMerSize + "_w_rc.nmer";
			}
			NMerGenomeTracker.createNMerGenomeTrackerFromFastaDirectory(inputFile, new File(outputDir, fileName), nMerSize, includeReverseCompliments);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static NMerGenomeTracker combineTrackers(NMerGenomeTracker... trackers) {
		BitSet bitSet = null;
		Integer nMerLength = null;
		for (NMerGenomeTracker tracker : trackers) {
			if (bitSet == null) {
				bitSet = (BitSet) tracker.moreThanOneBitset.clone();
				nMerLength = tracker.nMerLength;
			} else {
				if (tracker.nMerLength != nMerLength) {
					throw new IllegalStateException("Attempting to merge NMerGenomeTrackers of different lengths.");
				}
				bitSet.or(tracker.moreThanOneBitset);
			}
		}
		return new NMerGenomeTracker(bitSet, nMerLength);
	}

	// private static void load(int nMerSize, File inputDir, String fileIdentifier) {
	//
	// try {
	// String fileName = fileIdentifier + "_hg19_nmer_" + nMerSize + ".nmer";
	// NMerGenomeTracker specialTracker = NMerGenomeTracker.createNMerGenomeTrackerFromFile(new File(inputDir, fileName), nMerSize);
	// NMerGenomeTracker tracker = NMerGenomeTracker.createNMerGenomeTrackerFromFile(new File("D://kurts_space//sequence//hg19_nmer_" + nMerSize + ".nmer"), nMerSize);
	// int numberOfPermutations = (int) Math.pow(4, nMerSize);
	//
	// double numbersFound = 0;
	// for (int i = 0; i < numberOfPermutations; i++) {
	// if (specialTracker.isContainedMoreThanOnce(i) && !tracker.isContainedMoreThanOnce(i)) {
	// numbersFound++;
	// }
	// }
	// double percentFound = numbersFound / (double) numberOfPermutations;
	// System.out.println(fileIdentifier + " " + nMerSize + " %contained more than once: " + (percentFound * 100));
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	//
	// private static void printDetails(NMerGenomeTracker tracker, int nMerSize) {
	//
	// int numberOfPermutations = (int) Math.pow(4, nMerSize);
	//
	// double numbersFound = 0;
	// for (int i = 0; i < numberOfPermutations; i++) {
	// if (tracker.isContainedMoreThanOnce(i)) {
	// numbersFound++;
	// }
	// }
	// double percentFound = numbersFound / (double) numberOfPermutations;
	// System.out.println(nMerSize + " %contained more than once: " + (percentFound * 100));
	//
	// }
	//
	// private static NMerGenomeTracker loadTracker(int nMerSize, File inputDir, String fileIdentifier) throws IOException {
	// String fileName = fileIdentifier + "_hg19_nmer_" + nMerSize + ".nmer";
	// NMerGenomeTracker specialTracker = NMerGenomeTracker.createNMerGenomeTrackerFromFile(new File(inputDir, fileName), nMerSize);
	// return specialTracker;
	// }
	//
	// public static void load() {
	// for (int nMerLength = 5; nMerLength < 16; nMerLength++) {
	// File file = new File("D://kurts_space//sequence//hg19_nmer_" + nMerLength + ".nmer");
	// NMerGenomeTracker tracker;
	// try {
	// tracker = createNMerGenomeTrackerFromFile(file, nMerLength);
	//
	// int numberOfPermutations = (int) Math.pow(4, nMerLength);
	//
	// double numbersFound = 0;
	// for (int i = 0; i < numberOfPermutations; i++) {
	// if (tracker.isContainedMoreThanOnce(getSequence(i, nMerLength))) {
	// numbersFound++;
	// }
	// }
	// double percentFound = numbersFound / (double) numberOfPermutations;
	// System.out.println(nMerLength + " %contained more than once: " + (percentFound * 100));
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	//
	// }

	public static void create2() throws IOException {
		int nMerSize = 2;

		// File sequenceDir = new File("Y://genomes/homo_sapiens/hg38_GRCh38/sequence/hg38_all.fa");
		// File sequenceDir = new File("Y://genomes/homo_sapiens/hg19_GRCh37/sequence/original/");
		File sequenceDir = new File("D://kurts_space/sequence/short.fa");
		File nmerFile = new File("D://kurts_space/sequence/short_2.nmer");
		try {
			NMerGenomeTracker.createNMerGenomeTrackerFromFastaDirectory(sequenceDir, nmerFile, nMerSize, false);
		} catch (IOException e) {
			e.printStackTrace();
		}

		NMerGenomeTracker tracker = NMerGenomeTracker.createNMerGenomeTrackerFromFile(nmerFile, nMerSize);

		String[] contained = new String[] { "AA", "AC", "CC", "CT", "TT" };
		String[] notContained = new String[] { "AT", "AG", "CA", "CG", "TA", "TC", "TG", "GA", "GC", "GG", "GT" };

		for (String c : contained) {
			ISequence sequence = new NucleotideCodeSequence(c);
			if (!tracker.isContainedMoreThanOnce(sequence)) {
				throw new IllegalStateException("Sequence was not contained more than once [" + c + "].");
			}
		}

		for (String c : notContained) {
			ISequence sequence = new NucleotideCodeSequence(c);
			if (tracker.isContainedMoreThanOnce(sequence)) {
				throw new IllegalStateException("Sequence was contained more than once [" + c + "].");
			}
		}
	}

	public int getNMerSize() {
		return nMerLength;
	}

}
