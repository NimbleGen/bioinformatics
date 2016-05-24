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

/**
 * 
 * Enum representation of the four basic nucleotide codes
 * 
 */
public enum NucleotideCode implements ICode {
	ADENINE("Adenine", "A", "T"), CYTOSINE("Cytosine", "C", "G"), GUANINE("Gaunine", "G", "C"), THYMINE("Thymine", "T", "A");

	private final String abbreviation;
	private final String name;
	private final String complimentAbbreviation;

	private NucleotideCode(String name, String abbreviation, String complimentAbbreviation) {
		this.name = name;
		this.abbreviation = abbreviation;
		this.complimentAbbreviation = complimentAbbreviation;
	}

	/**
	 * @return string abbreviation for this nucleotide code
	 */
	public String getAbbreviation() {
		return abbreviation;
	}

	/**
	 * @return the name of the nucleotide this code represents
	 */
	public String getName() {
		return name;
	}

	@Override
	public boolean matches(ICode nucleotide) {
		boolean matches = (nucleotide instanceof NucleotideCode) && this.abbreviation.equals(((NucleotideCode) nucleotide).abbreviation);

		return matches;
	}

	/**
	 * @param codeAsString
	 * @return returns null if no such string is found
	 */
	static NucleotideCode[] getNucleotidesFromString(String nucleotidesAsString) {
		nucleotidesAsString = nucleotidesAsString.toUpperCase();
		NucleotideCode[] nucleotides = new NucleotideCode[nucleotidesAsString.length()];

		for (int i = 0; i < nucleotidesAsString.length(); i++) {
			String characterToTranslate = nucleotidesAsString.substring(i, i + 1);
			NucleotideCode matchingNucleotide = getNucleotideFromString(characterToTranslate);

			if (matchingNucleotide != null) {
				nucleotides[i] = matchingNucleotide;
			} else {
				throw new IllegalArgumentException("Unable to translate character[" + characterToTranslate + "] from nucleotidesAsString[" + nucleotidesAsString + "].");
			}
		}

		return nucleotides;
	}

	/**
	 * 
	 * @param nucleotideAsString
	 * @return the NucleotideCode associate with the provided string
	 */
	public static NucleotideCode getNucleotideFromString(String nucleotideAsString) {
		NucleotideCode matchingNucleotide = NucleotideCodeLookup.getInstance().getNucleotideCode(nucleotideAsString);

		return matchingNucleotide;
	}

	@Override
	public IupacNucleotideCode getIupaceNucleotideCodeEquivalent() {
		IupacNucleotideCode iupaceNucleotideCodeEquivalent = null;

		if (this == CYTOSINE) {
			iupaceNucleotideCodeEquivalent = IupacNucleotideCode.C;
		} else if (this == GUANINE) {
			iupaceNucleotideCodeEquivalent = IupacNucleotideCode.G;
		} else if (this == ADENINE) {
			iupaceNucleotideCodeEquivalent = IupacNucleotideCode.A;
		} else if (this == THYMINE) {
			iupaceNucleotideCodeEquivalent = IupacNucleotideCode.T;
		}

		return iupaceNucleotideCodeEquivalent;
	}

	@Override
	public String toString() {
		return abbreviation;
	}

	@Override
	public NucleotideCode[] getNucleotides() {
		return new NucleotideCode[] { this };
	}

	@Override
	public ICode getComplimentCode() {
		return getNucleotideFromString(complimentAbbreviation);
	}

}
