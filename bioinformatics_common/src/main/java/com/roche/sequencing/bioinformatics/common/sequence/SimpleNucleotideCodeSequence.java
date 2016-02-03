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
import java.util.Iterator;
import java.util.Objects;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.roche.sequencing.bioinformatics.common.utils.BitSetUtil;

/**
 * 
 * Sequence composed of IUPAC codes
 * 
 */
public class SimpleNucleotideCodeSequence implements ISequence, Comparable<SimpleNucleotideCodeSequence> {
	public static final int BITS_PER_NUCLEOTIDE = 3;
	private static final int BITS_PER_BYTE = 8;

	private static final BiMap<BitSet, IupacNucleotideCode> BIT_TO_IUPAC_NUCLEOTIDE_CODE_MAP = HashBiMap.create();

	static {
		BIT_TO_IUPAC_NUCLEOTIDE_CODE_MAP.put(BitSetUtil.createBitSetFromBinaryString("000"), IupacNucleotideCode.A);
		BIT_TO_IUPAC_NUCLEOTIDE_CODE_MAP.put(BitSetUtil.createBitSetFromBinaryString("100"), IupacNucleotideCode.C);
		BIT_TO_IUPAC_NUCLEOTIDE_CODE_MAP.put(BitSetUtil.createBitSetFromBinaryString("010"), IupacNucleotideCode.G);
		BIT_TO_IUPAC_NUCLEOTIDE_CODE_MAP.put(BitSetUtil.createBitSetFromBinaryString("110"), IupacNucleotideCode.T);
		BIT_TO_IUPAC_NUCLEOTIDE_CODE_MAP.put(BitSetUtil.createBitSetFromBinaryString("001"), IupacNucleotideCode.U);
		BIT_TO_IUPAC_NUCLEOTIDE_CODE_MAP.put(BitSetUtil.createBitSetFromBinaryString("101"), IupacNucleotideCode.GAP);
		BIT_TO_IUPAC_NUCLEOTIDE_CODE_MAP.put(BitSetUtil.createBitSetFromBinaryString("011"), IupacNucleotideCode.N);
		// open bits "111"
	}

	private final BitSet sequenceAsBits;
	private int currentNumberOfBits;

	/**
	 * Constructor taking in a stringof IUPAC characters
	 * 
	 * @param iupacNucleotideCodesAsString
	 */
	public SimpleNucleotideCodeSequence(String iupacNucleotideCodesAsString) {
		this(IupacNucleotideCode.getCodesFromString(iupacNucleotideCodesAsString));
	}

	/**
	 * Empty Constructor
	 */
	public SimpleNucleotideCodeSequence() {
		this("");
	}

	public SimpleNucleotideCodeSequence(int numberOfBits, BitSet sequenceAsBits) {
		this.currentNumberOfBits = numberOfBits;
		this.sequenceAsBits = sequenceAsBits;
	}

	SimpleNucleotideCodeSequence(ICode[] codes) {
		Objects.requireNonNull(codes, "argument[codes] cannot be null");

		if (codes instanceof IupacNucleotideCode[]) {
			IupacNucleotideCode[] codesAsIupacNucleotideCode = (IupacNucleotideCode[]) codes;

			sequenceAsBits = new BitSet(codesAsIupacNucleotideCode.length * BITS_PER_NUCLEOTIDE);

			for (int i = 0; i < codes.length; i++) {
				setCodeAt(i, codesAsIupacNucleotideCode[i]);
			}

			currentNumberOfBits = codesAsIupacNucleotideCode.length * BITS_PER_NUCLEOTIDE;
		} else {
			throw new IllegalArgumentException("The passed in ICode[] codes must be of type IupacNucleotideCode[] to be utilized by the constructor.");
		}
	}

	@Override
	public ISequence subSequence(int start, int end) {
		SimpleNucleotideCodeSequence subSequence = new SimpleNucleotideCodeSequence();

		end = Math.min(end, size() - 1);

		for (int i = start; i <= end; i++) {
			int newIndex = i - start;
			IupacNucleotideCode code = (IupacNucleotideCode) getCodeAt(i);

			subSequence.setCodeAt(newIndex, code);
		}

		return subSequence;
	}

	@Override
	public synchronized void append(ISequence codeSequenceToAppend) {
		if (codeSequenceToAppend instanceof SimpleNucleotideCodeSequence) {
			SimpleNucleotideCodeSequence codeSequenceToAppendAsIupacSequence = (SimpleNucleotideCodeSequence) codeSequenceToAppend;

			for (int i = codeSequenceToAppendAsIupacSequence.currentNumberOfBits - 1; i >= 0; i--) {
				if (codeSequenceToAppendAsIupacSequence.sequenceAsBits.get(i)) {
					sequenceAsBits.set(currentNumberOfBits + i);
				}
			}

			currentNumberOfBits += codeSequenceToAppendAsIupacSequence.currentNumberOfBits;
		} else {
			throw new IllegalArgumentException("The passed in ISequence codeSequenceToAppend must be of type SimpleNucleotideCodeSequence to be utilized by the append method.");
		}
	}

	/**
	 * Add the provided code to the end of this sequence
	 * 
	 * @param code
	 */
	public void append(ICode code) {
		if (code instanceof IupacNucleotideCode) {
			IupacNucleotideCode codeAsIupacCode = (IupacNucleotideCode) code;

			append(new SimpleNucleotideCodeSequence(new IupacNucleotideCode[] { codeAsIupacCode }));
		}
	}

	@Override
	public ICode getCodeAt(int index) {
		if ((index >= size()) || (index < 0)) {
			throw new IndexOutOfBoundsException("Provided index[" + index + "] is larger than the current size[" + size() + "] or smaller than zero.");
		}

		int startIndex = index * BITS_PER_NUCLEOTIDE;
		int endIndex = startIndex + BITS_PER_NUCLEOTIDE;
		BitSet bitsForNucleotide = sequenceAsBits.get(startIndex, endIndex);
		IupacNucleotideCode code = BIT_TO_IUPAC_NUCLEOTIDE_CODE_MAP.get(bitsForNucleotide);

		return code;
	}

	private void setCodeAt(int index, IupacNucleotideCode code) {
		int startingIndex = index * BITS_PER_NUCLEOTIDE;

		BitSet bitsToSet = BIT_TO_IUPAC_NUCLEOTIDE_CODE_MAP.inverse().get(code);
		if (bitsToSet == null) {
			throw new IllegalStateException("Could not save iupac code[" + code.name() + "].");
		}
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

	@Override
	public ISequence getReverse() {
		ICode[] codes = new IupacNucleotideCode[size()];

		for (int i = size() - 1; i >= 0; i--) {
			ICode code = getCodeAt(i);

			codes[size() - i - 1] = code;
		}

		return new SimpleNucleotideCodeSequence(codes);
	}

	@Override
	public ISequence getReverseCompliment() {
		ICode[] codes = new IupacNucleotideCode[size()];

		for (int i = size() - 1; i >= 0; i--) {
			ICode code = getCodeAt(i);

			codes[size() - i - 1] = code.getComplimentCode();
		}

		return new SimpleNucleotideCodeSequence(codes);
	}

	@Override
	public ISequence getCompliment() {
		ICode[] codes = new IupacNucleotideCode[size()];

		for (int i = 0; i < size(); i++) {
			ICode code = getCodeAt(i);

			codes[i] = code.getComplimentCode();
		}

		return new SimpleNucleotideCodeSequence(codes);
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

		SimpleNucleotideCodeSequence other = (SimpleNucleotideCodeSequence) obj;

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
		return new IupacNucleotideCodeIterator();
	}

	private class IupacNucleotideCodeIterator implements Iterator<ICode> {
		private int currentIndex;

		private IupacNucleotideCodeIterator() {
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
	public int compareTo(SimpleNucleotideCodeSequence o) {
		int result = 0;
		compareLoop: for (int i = 0; i < Math.min(o.size(), size()); i++) {
			result = getCodeAt(i).getIupaceNucleotideCodeEquivalent().compareTo(o.getCodeAt(i).getIupaceNucleotideCodeEquivalent());
			if (result == 0) {
				break compareLoop;
			}
		}

		return result;
	}

	public byte[] getSequenceAsBytes() {
		int numberOfBytesNeeded = (currentNumberOfBits / BITS_PER_BYTE);
		if (currentNumberOfBits % BITS_PER_BYTE > 0) {
			numberOfBytesNeeded++;
		}

		byte[] bytes = new byte[numberOfBytesNeeded];
		byte emptyByte = 0;
		for (int currentByteIndex = 0; currentByteIndex < numberOfBytesNeeded; currentByteIndex++) {
			int startBitIndex = currentByteIndex * BITS_PER_BYTE;
			int stopBitIndexExclusive = (currentByteIndex + 1) * BITS_PER_BYTE;
			BitSet bitsForByte = sequenceAsBits.get(startBitIndex, stopBitIndexExclusive);
			if (bitsForByte.length() == 0) {
				bytes[currentByteIndex] = emptyByte;
			} else {
				byte[] byteAsArray = bitsForByte.toByteArray();
				bytes[currentByteIndex] = byteAsArray[0];
			}
		}
		return bytes;
	}

}
