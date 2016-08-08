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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import com.roche.sequencing.bioinformatics.common.utils.BitSetUtil;

/**
 * 
 * Sequence composed of IUPAC codes
 * 
 */
class ChanneledNucleotideCodeSequence implements ISequence, Comparable<ChanneledNucleotideCodeSequence> {
	private static final int BITS_PER_BYTE = 8;
	private static final int NUMBER_OF_CHANNELS = 4;

	private final BitSet aChannelAsBits;
	private final BitSet cChannelAsBits;
	private final BitSet gChannelAsBits;
	private final BitSet tChannelAsBits;
	private int currentNumberOfBits;

	/**
	 * Constructor taking in a stringof IUPAC characters
	 * 
	 * @param iupacNucleotideCodesAsString
	 */
	public ChanneledNucleotideCodeSequence(String iupacNucleotideCodesAsString) {
		this(IupacNucleotideCode.getCodesFromString(iupacNucleotideCodesAsString));
	}

	/**
	 * Empty Constructor
	 */
	public ChanneledNucleotideCodeSequence() {
		this("");
	}

	private ChanneledNucleotideCodeSequence(ICode[] codes) {
		Objects.requireNonNull(codes, "argument[codes] cannot be null");

		if (codes instanceof IupacNucleotideCode[]) {
			IupacNucleotideCode[] codesAsIupacNucleotideCode = (IupacNucleotideCode[]) codes;

			aChannelAsBits = new BitSet(codesAsIupacNucleotideCode.length);
			cChannelAsBits = new BitSet(codesAsIupacNucleotideCode.length);
			gChannelAsBits = new BitSet(codesAsIupacNucleotideCode.length);
			tChannelAsBits = new BitSet(codesAsIupacNucleotideCode.length);

			for (int i = 0; i < codes.length; i++) {
				setCodeAt(i, codesAsIupacNucleotideCode[i]);
			}

			currentNumberOfBits = codesAsIupacNucleotideCode.length;
		} else {
			throw new IllegalArgumentException("The passed in ICode[] codes must be of type IupacNucleotideCode[] to be utilized by the constructor.");
		}
	}

	@Override
	public ISequence subSequence(int start, int end) {
		ChanneledNucleotideCodeSequence subSequence = new ChanneledNucleotideCodeSequence();

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
		if (codeSequenceToAppend instanceof ChanneledNucleotideCodeSequence) {
			ChanneledNucleotideCodeSequence codeSequenceToAppendAsIupacSequence = (ChanneledNucleotideCodeSequence) codeSequenceToAppend;

			for (int i = codeSequenceToAppendAsIupacSequence.currentNumberOfBits - 1; i >= 0; i--) {
				if (codeSequenceToAppendAsIupacSequence.aChannelAsBits.get(i)) {
					aChannelAsBits.set(currentNumberOfBits + i);
				}
				if (codeSequenceToAppendAsIupacSequence.cChannelAsBits.get(i)) {
					cChannelAsBits.set(currentNumberOfBits + i);
				}
				if (codeSequenceToAppendAsIupacSequence.gChannelAsBits.get(i)) {
					gChannelAsBits.set(currentNumberOfBits + i);
				}
				if (codeSequenceToAppendAsIupacSequence.tChannelAsBits.get(i)) {
					tChannelAsBits.set(currentNumberOfBits + i);
				}
			}

			currentNumberOfBits += codeSequenceToAppendAsIupacSequence.currentNumberOfBits;
		} else {
			throw new IllegalArgumentException("The passed in ISequence codeSequenceToAppend must be of type ChanneledNucleotideCodeSequence to be utilized by the append method.");
		}
	}

	@Override
	public ICode getCodeAt(int index) {
		if ((index >= size()) || (index < 0)) {
			throw new IndexOutOfBoundsException("Provided index[" + index + "] is larger than the current size[" + size() + "] or smaller than zero.");
		}

		Set<NucleotideCode> codes = new HashSet<NucleotideCode>();
		if (aChannelAsBits.get(index)) {
			codes.add(NucleotideCode.ADENINE);
		}
		if (cChannelAsBits.get(index)) {
			codes.add(NucleotideCode.CYTOSINE);
		}
		if (gChannelAsBits.get(index)) {
			codes.add(NucleotideCode.GUANINE);
		}
		if (tChannelAsBits.get(index)) {
			codes.add(NucleotideCode.THYMINE);
		}
		IupacNucleotideCode code = IupacNucleotideCode.getCode(codes);
		return code;
	}

	private void setCodeAt(int index, IupacNucleotideCode code) {
		aChannelAsBits.set(index, false);
		cChannelAsBits.set(index, false);
		gChannelAsBits.set(index, false);
		tChannelAsBits.set(index, false);
		for (NucleotideCode nucleotideCode : code.getNucleotides()) {
			if (nucleotideCode == NucleotideCode.ADENINE) {
				aChannelAsBits.set(index, true);
			} else if (nucleotideCode == NucleotideCode.CYTOSINE) {
				cChannelAsBits.set(index, true);
			} else if (nucleotideCode == NucleotideCode.GUANINE) {
				gChannelAsBits.set(index, true);
			} else if (nucleotideCode == NucleotideCode.THYMINE) {
				tChannelAsBits.set(index, true);
			}
		}
		currentNumberOfBits = Math.max(index, currentNumberOfBits);
	}

	@Override
	public int size() {
		return currentNumberOfBits;
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

		return new ChanneledNucleotideCodeSequence(codes);
	}

	@Override
	public ISequence getReverseCompliment() {
		ICode[] codes = new IupacNucleotideCode[size()];

		for (int i = size() - 1; i >= 0; i--) {
			ICode code = getCodeAt(i);

			codes[size() - i - 1] = code.getComplimentCode();
		}

		return new ChanneledNucleotideCodeSequence(codes);
	}

	@Override
	public ISequence getCompliment() {
		ICode[] codes = new IupacNucleotideCode[size()];

		for (int i = 0; i < size(); i++) {
			ICode code = getCodeAt(i);

			codes[i] = code.getComplimentCode();
		}

		return new ChanneledNucleotideCodeSequence(codes);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aChannelAsBits == null) ? 0 : aChannelAsBits.hashCode());
		result = prime * result + ((cChannelAsBits == null) ? 0 : cChannelAsBits.hashCode());
		result = prime * result + currentNumberOfBits;
		result = prime * result + ((gChannelAsBits == null) ? 0 : gChannelAsBits.hashCode());
		result = prime * result + ((tChannelAsBits == null) ? 0 : tChannelAsBits.hashCode());
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
		ChanneledNucleotideCodeSequence other = (ChanneledNucleotideCodeSequence) obj;
		if (aChannelAsBits == null) {
			if (other.aChannelAsBits != null)
				return false;
		} else if (!aChannelAsBits.equals(other.aChannelAsBits))
			return false;
		if (cChannelAsBits == null) {
			if (other.cChannelAsBits != null)
				return false;
		} else if (!cChannelAsBits.equals(other.cChannelAsBits))
			return false;
		if (currentNumberOfBits != other.currentNumberOfBits)
			return false;
		if (gChannelAsBits == null) {
			if (other.gChannelAsBits != null)
				return false;
		} else if (!gChannelAsBits.equals(other.gChannelAsBits))
			return false;
		if (tChannelAsBits == null) {
			if (other.tChannelAsBits != null)
				return false;
		} else if (!tChannelAsBits.equals(other.tChannelAsBits))
			return false;
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
	public int compareTo(ChanneledNucleotideCodeSequence o) {
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
		int numberOfBytesNeeded = ((currentNumberOfBits * NUMBER_OF_CHANNELS) / BITS_PER_BYTE);
		if (currentNumberOfBits % BITS_PER_BYTE > 0) {
			numberOfBytesNeeded++;
		}

		BitSet combinedChannels = BitSetUtil.combine(currentNumberOfBits, aChannelAsBits, cChannelAsBits, gChannelAsBits, tChannelAsBits);

		byte[] bytes = new byte[numberOfBytesNeeded];
		byte emptyByte = 0;
		for (int currentByteIndex = 0; currentByteIndex < numberOfBytesNeeded; currentByteIndex++) {
			int startBitIndex = currentByteIndex * BITS_PER_BYTE;
			int stopBitIndexExclusive = (currentByteIndex + 1) * BITS_PER_BYTE;

			BitSet bitsForByte = combinedChannels.get(startBitIndex, stopBitIndexExclusive);
			if (bitsForByte.length() == 0) {
				bytes[currentByteIndex] = emptyByte;
			} else {
				byte[] byteAsArray = bitsForByte.toByteArray();
				bytes[currentByteIndex] = byteAsArray[0];
			}
		}
		return bytes;
	}

	public static int getNumberOfMismatches(ChanneledNucleotideCodeSequence referenceSequence, ChanneledNucleotideCodeSequence querySequence) {
		int numberOfMismatches = 0;
		BitSet newA = (BitSet) querySequence.aChannelAsBits.clone();
		BitSet newC = (BitSet) querySequence.cChannelAsBits.clone();
		BitSet newG = (BitSet) querySequence.gChannelAsBits.clone();
		BitSet newT = (BitSet) querySequence.tChannelAsBits.clone();
		newA.andNot(referenceSequence.aChannelAsBits);
		newC.andNot(referenceSequence.cChannelAsBits);
		newG.andNot(referenceSequence.gChannelAsBits);
		newT.andNot(referenceSequence.tChannelAsBits);
		newA.or(newC);
		newA.or(newG);
		newA.or(newT);
		numberOfMismatches = newA.cardinality();
		return numberOfMismatches;
	}

	@Override
	public double getGCPercent() {
		return SequenceUtil.getGCPercent(this);
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

}
