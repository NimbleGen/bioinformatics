/*
 *    Copyright 2016 Roche NimbleGen Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.roche.sequencing.bioinformatics.common.sequence;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.roche.sequencing.bioinformatics.common.utils.BitSetUtil;

/**
 * 
 * A sequence composed of nucleotide codes
 * 
 */
public class NucleotideCodeSequence implements ISequence, Comparable<NucleotideCodeSequence> {
	private static final int BITS_PER_NUCLEOTIDE = 2;

	private static final Map<BitSet, NucleotideCode> BIT_TO_NUCLEOTIDE_MAP = new HashMap<BitSet, NucleotideCode>();
	private static final Map<NucleotideCode, BitSet> NUCLEOTIDE_TO_BITS_MAP = new HashMap<NucleotideCode, BitSet>();

	static {
		BIT_TO_NUCLEOTIDE_MAP.put(BitSetUtil.createBitSetFromBinaryString("00"), NucleotideCode.ADENINE);
		BIT_TO_NUCLEOTIDE_MAP.put(BitSetUtil.createBitSetFromBinaryString("10"), NucleotideCode.CYTOSINE);
		BIT_TO_NUCLEOTIDE_MAP.put(BitSetUtil.createBitSetFromBinaryString("01"), NucleotideCode.GUANINE);
		BIT_TO_NUCLEOTIDE_MAP.put(BitSetUtil.createBitSetFromBinaryString("11"), NucleotideCode.THYMINE);

		for (BitSet keyBits : BIT_TO_NUCLEOTIDE_MAP.keySet()) {
			NucleotideCode valueCode = BIT_TO_NUCLEOTIDE_MAP.get(keyBits);

			NUCLEOTIDE_TO_BITS_MAP.put(valueCode, keyBits);
		}
	}

	private final BitSet sequenceAsBits;
	private int currentNumberOfBits;

	/**
	 * Constructor
	 * 
	 * @param nucleotidesAsString
	 */
	public NucleotideCodeSequence(String nucleotidesAsString) {
		this(NucleotideCode.getNucleotidesFromString(nucleotidesAsString));
	}

	/**
	 * Construct an empty NucleotideCodeSequence
	 */
	public NucleotideCodeSequence() {
		this("");
	}

	private NucleotideCodeSequence(ICode[] codes) {
		if (codes instanceof NucleotideCode[]) {
			NucleotideCode[] nucleotides = (NucleotideCode[]) codes;

			currentNumberOfBits = nucleotides.length * BITS_PER_NUCLEOTIDE;
			sequenceAsBits = new BitSet(currentNumberOfBits);

			for (int i = 0; i < nucleotides.length; i++) {
				setCodeAt(i, nucleotides[i]);
			}
		} else {
			throw new IllegalArgumentException("The passed in ICode[] codes of [" + codes.getClass() + "] must be of type Nucleotide[] to be utilized by the constructor.");
		}
	}

	/**
	 * Copy Constructor
	 * 
	 * @param orig
	 */
	public NucleotideCodeSequence(ISequence orig) {
		if (orig instanceof NucleotideCodeSequence) {
			NucleotideCodeSequence origAsNucleotideSequence = (NucleotideCodeSequence) orig;

			sequenceAsBits = (BitSet) origAsNucleotideSequence.sequenceAsBits.clone();
			currentNumberOfBits = origAsNucleotideSequence.currentNumberOfBits;
		} else {
			String nucleotidesAsString = orig.toString();
			NucleotideCode[] nucleotides = NucleotideCode.getNucleotidesFromString(nucleotidesAsString);
			currentNumberOfBits = nucleotides.length * BITS_PER_NUCLEOTIDE;
			sequenceAsBits = new BitSet(currentNumberOfBits);

			for (int i = 0; i < nucleotides.length; i++) {
				setCodeAt(i, nucleotides[i]);
			}
		}
	}

	@Override
	public ISequence subSequence(int start, int end) {
		NucleotideCodeSequence subSequence = new NucleotideCodeSequence();

		for (int i = start; i <= end; i++) {
			int newIndex = i - start;

			subSequence.setCodeAt(newIndex, (NucleotideCode) getCodeAt(i));
		}

		return subSequence;
	}

	@Override
	public synchronized void append(ISequence sequenceToAppend) {
		if (sequenceToAppend instanceof NucleotideCodeSequence) {
			NucleotideCodeSequence nucleotideSequenceToAppend = (NucleotideCodeSequence) sequenceToAppend;

			for (int i = nucleotideSequenceToAppend.currentNumberOfBits - 1; i >= 0; i--) {
				if (nucleotideSequenceToAppend.sequenceAsBits.get(i)) {
					sequenceAsBits.set(currentNumberOfBits + i);
				}
			}

			currentNumberOfBits += nucleotideSequenceToAppend.currentNumberOfBits;
		} else {
			throw new IllegalArgumentException("Passed in value ISequence nucleotideSequenceToAppend must be of type NucleotideSequece for method append.");
		}
	}

	@Override
	public ISequence getReverse() {
		ICode[] codes = new NucleotideCode[size()];

		for (int i = size() - 1; i >= 0; i--) {
			ICode code = getCodeAt(i);

			codes[size() - i - 1] = code;
		}

		return new NucleotideCodeSequence(codes);
	}

	@Override
	public ISequence getReverseCompliment() {
		ICode[] codes = new NucleotideCode[size()];

		for (int i = size() - 1; i >= 0; i--) {
			ICode code = getCodeAt(i);

			codes[size() - i - 1] = code.getComplimentCode();
		}

		return new NucleotideCodeSequence(codes);
	}

	@Override
	public ISequence getCompliment() {
		ICode[] codes = new NucleotideCode[size()];

		for (int i = 0; i < size(); i++) {
			ICode code = getCodeAt(i);

			codes[i] = code.getComplimentCode();
		}

		return new NucleotideCodeSequence(codes);
	}

	@Override
	public ICode getCodeAt(int index) {
		if ((index >= size()) || (index < 0)) {
			throw new IndexOutOfBoundsException("Provided index[" + index + "] is larger than or equal to the current size[" + size() + "].");
		} else if (index < 0) {
			throw new IndexOutOfBoundsException("Provided index[" + index + "] is smaller than zero.");
		}

		int startIndex = index * BITS_PER_NUCLEOTIDE;
		int endIndex = startIndex + BITS_PER_NUCLEOTIDE;
		BitSet bitsForNucleotide = sequenceAsBits.get(startIndex, endIndex);
		NucleotideCode nucleotideCode = BIT_TO_NUCLEOTIDE_MAP.get(bitsForNucleotide);

		return nucleotideCode;
	}

	private final void setCodeAt(int index, NucleotideCode nucleotideCode) {
		int startingIndex = index * BITS_PER_NUCLEOTIDE;

		BitSet bitsToSet = NUCLEOTIDE_TO_BITS_MAP.get(nucleotideCode);

		for (int i = 0; i < bitsToSet.length(); i++) {
			sequenceAsBits.set(startingIndex + i, bitsToSet.get(i));
		}

		int lastBitLocationSet = (index + 1) * BITS_PER_NUCLEOTIDE;

		currentNumberOfBits = Math.max(lastBitLocationSet, currentNumberOfBits);
	}

	@Override
	public int size() {
		return currentNumberOfBits / BITS_PER_NUCLEOTIDE;
	}

	@Override
	public String toString() {
		StringBuilder returnString = new StringBuilder();

		for (int i = 0; i < size(); i++) {
			returnString.append(getCodeAt(i).toString());
		}

		return returnString.toString();
	}

	String toStringAsBits() {
		return BitSetUtil.getBinaryStringOfBits(sequenceAsBits, currentNumberOfBits);
	}

	@Override
	public double getGCPercent() {
		return SequenceUtil.getGCPercent(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;

		result = prime * result + currentNumberOfBits;
		result = prime * result + ((sequenceAsBits == null) ? 0 : sequenceAsBits.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		NucleotideCodeSequence other = (NucleotideCodeSequence) obj;

		if (currentNumberOfBits != other.currentNumberOfBits) {
			return false;
		}

		if (sequenceAsBits == null) {
			if (other.sequenceAsBits != null) {
				return false;
			}
		} else if (!sequenceAsBits.equals(other.sequenceAsBits)) {
			return false;
		}

		return true;
	}

	@Override
	public Iterator<ICode> iterator() {
		return new NucleotideCodeIterator();
	}

	private class NucleotideCodeIterator implements Iterator<ICode> {
		private int currentIndex;

		private NucleotideCodeIterator() {
			currentIndex = -1;
		}

		@Override
		public boolean hasNext() {
			return currentIndex < (size() - 1);
		}

		@Override
		public ICode next() {
			currentIndex++;
			return getCodeAt(currentIndex);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("This method is unavailable.");
		}

	}

	@Override
	public int compareTo(NucleotideCodeSequence o) {
		int result = 0;
		compareLoop: for (int i = 0; i < Math.min(o.size(), size()); i++) {
			result = getCodeAt(i).getIupaceNucleotideCodeEquivalent().compareTo(o.getCodeAt(i).getIupaceNucleotideCodeEquivalent());
			if (result == 0) {
				break compareLoop;
			}
		}

		return result;
	}

	@Override
	public boolean contains(ICode nucleotide) {
		boolean nucleotideFound = false;
		int i = 0;
		while (i < size() && !nucleotideFound) {
			nucleotideFound = getCodeAt(i).matches(nucleotide);
			i++;
		}
		return nucleotideFound;
	}

	@Override
	public ISequence subSequence(int start) {
		return subSequence(start, size() - 1);
	}

}
