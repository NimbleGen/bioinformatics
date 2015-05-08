package com.roche.sequencing.bioinformatics.common.utils;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class StringCompressor {

	private final Map<Character, BitSet> characterToBitSetMap;
	private final Map<BitSet, Character> bitSetToCharacterMap;
	private final int bitsPerCharacter;
	private final BitSet terminatingBitSet;

	public StringCompressor(Set<Character> characterLibrary) {
		Map<Character, BitSet> writeableCharacterToBitSetMap = new HashMap<Character, BitSet>();
		Map<BitSet, Character> writeableBitSetToCharacterMap = new HashMap<BitSet, Character>();

		// add one for terminating symbol
		int librarySize = characterLibrary.size() + 1;

		bitsPerCharacter = (int) Math.ceil(Math.log(librarySize) / Math.log(2));
		int i = 0;
		for (Character character : characterLibrary) {
			BitSet bitset = createBitSetFromUnsignedNumber(i, bitsPerCharacter);
			writeableCharacterToBitSetMap.put(character, bitset);
			writeableBitSetToCharacterMap.put(bitset, character);
			i++;
		}

		terminatingBitSet = createBitSetFromUnsignedNumber((int) Math.pow(2, bitsPerCharacter) - 1, bitsPerCharacter);
		characterToBitSetMap = Collections.unmodifiableMap(writeableCharacterToBitSetMap);
		bitSetToCharacterMap = Collections.unmodifiableMap(writeableBitSetToCharacterMap);
	}

	public BitSet compressString(String stringToCompress) {
		BitSet[] bitsets = new BitSet[stringToCompress.length() + 1];
		for (int i = 0; i < stringToCompress.length(); i++) {
			char character = stringToCompress.charAt(i);
			BitSet bitset = characterToBitSetMap.get(character);
			if (bitset == null) {
				throw new IllegalStateException("The provided character[" + character + "] is not in the library provided to StringCompressor.");
			}
			bitsets[i] = bitset;
		}
		// the all 1 bitset is the string terminating symbol
		bitsets[stringToCompress.length()] = terminatingBitSet;
		return BitSetUtil.combine(bitsPerCharacter, bitsets);
	}

	public String uncompress(BitSet compressedStringAsBitSet) {
		StringBuilder stringBuilder = new StringBuilder();
		boolean stringTerminatorFound = false;
		int i = 0;
		while (!stringTerminatorFound) {
			int fromIndex = i * bitsPerCharacter;
			int toIndex = (i + 1) * bitsPerCharacter;
			BitSet characterBitSet = compressedStringAsBitSet.get(fromIndex, toIndex);
			if (characterBitSet.equals(terminatingBitSet)) {
				stringTerminatorFound = true;
			} else {
				stringBuilder.append(bitSetToCharacterMap.get(characterBitSet));
			}
			i++;
		}
		return stringBuilder.toString();
	}

	private static BitSet createBitSetFromUnsignedNumber(int number, int bits) {
		number = Math.abs(number);
		BitSet bitset = new BitSet(bits);
		for (int i = bits - 1; i >= 0; i--) {
			int bitValue = (int) Math.pow(2, i);
			if (number >= bitValue) {
				bitset.set(bits - i - 1);
				number -= bitValue;
			}
		}
		return bitset;
	}

	public static void main(String[] args) {
		Set<Character> set = new HashSet<Character>();
		set.add('0');
		set.add('1');
		set.add('2');
		set.add('3');
		set.add('4');
		set.add('5');
		set.add('6');
		set.add('7');
		set.add('8');
		set.add('9');
		set.add(':');

		// StringCompressor stringCompressor = new StringCompressor(set);
		// System.out.println(stringCompressor.uncompress(stringCompressor.compressString("111:0010101:010102293845930a")));

		Random r = new Random();

		int x_size = 100000;
		int y_size = 100;

		String[][] strings = new String[x_size][y_size];
		// BitSet[][] compressedStrings = new BitSet[x_size][y_size];
		for (int x = 0; x < x_size; x++) {
			for (int y = 0; y < y_size; y++) {
				int value1 = r.nextInt(Integer.MAX_VALUE);
				int value2 = r.nextInt(Integer.MAX_VALUE);
				int value3 = r.nextInt(Integer.MAX_VALUE);
				int value4 = r.nextInt(Integer.MAX_VALUE);
				int value5 = r.nextInt(Integer.MAX_VALUE);
				int value6 = r.nextInt(Integer.MAX_VALUE);
				int value7 = r.nextInt(Integer.MAX_VALUE);
				int value8 = r.nextInt(Integer.MAX_VALUE);
				strings[x][y] = "" + value1 + value2 + value3 + value4 + value5 + value6 + value7 + value8 + value1 + value2 + value3 + value4 + value5 + value6 + value7 + value8;
				// compressedStrings[x][y] = stringCompressor.compressString("" + value1 + value2 + value3 + value4 + value5 + value6 + value7 + value8 + value1 + value2 + value3 + value4 + value5
				// + value6 + value7 + value8);
			}

		}

		for (int i = 0; i < 4; i++) {
			System.gc();
		}
		Runtime runtime = Runtime.getRuntime();

		int mb = 1024 * 1024;

		System.out.println("##### Heap utilization statistics [MB] #####");

		// Print used memory
		System.out.println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb);

		// Print free memory
		System.out.println("Free Memory:" + runtime.freeMemory() / mb);

		// Print total available memory
		System.out.println("Total Memory:" + runtime.totalMemory() / mb);

		// Print Maximum available memory
		System.out.println("Max Memory:" + runtime.maxMemory() / mb);

		System.out.println("done allocating");

		// System.out.println(stringCompressor.uncompress(compressedStrings[100][12]));
		System.out.println(strings[100][12]);

	}

}
