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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 
 * Enum representing all Iupac codes
 * 
 */
public enum IupacNucleotideCode implements ICode, Comparable<IupacNucleotideCode> {
	A("A", NucleotideCode.ADENINE, "T"), C("C", NucleotideCode.CYTOSINE, "G"), G("G", NucleotideCode.GUANINE, "C"), T("T", NucleotideCode.THYMINE, "A"), U("U", NucleotideCode.THYMINE, "A"), R("R",
			new NucleotideCode[] { NucleotideCode.ADENINE, NucleotideCode.GUANINE }, "Y"), Y("Y", new NucleotideCode[] { NucleotideCode.CYTOSINE, NucleotideCode.THYMINE }, "R"), S("S",
			new NucleotideCode[] { NucleotideCode.GUANINE, NucleotideCode.CYTOSINE }, "S"), W("W", new NucleotideCode[] { NucleotideCode.ADENINE, NucleotideCode.THYMINE }, "W"), K("K",
			new NucleotideCode[] { NucleotideCode.GUANINE, NucleotideCode.THYMINE }, "M"), M("M", new NucleotideCode[] { NucleotideCode.ADENINE, NucleotideCode.CYTOSINE }, "K"), B("B",
			new NucleotideCode[] { NucleotideCode.CYTOSINE, NucleotideCode.GUANINE, NucleotideCode.THYMINE }, "V"), D("D", new NucleotideCode[] { NucleotideCode.ADENINE, NucleotideCode.GUANINE,
			NucleotideCode.THYMINE }, "H"), H("H", new NucleotideCode[] { NucleotideCode.ADENINE, NucleotideCode.CYTOSINE, NucleotideCode.THYMINE }, "D"), V("V", new NucleotideCode[] {
			NucleotideCode.ADENINE, NucleotideCode.CYTOSINE, NucleotideCode.GUANINE }, "B"), N("N", new NucleotideCode[] { NucleotideCode.ADENINE, NucleotideCode.CYTOSINE, NucleotideCode.THYMINE,
			NucleotideCode.GUANINE }, "N"), GAP(new String[] { "_", ".", "-" }, new NucleotideCode[0], "_");

	private final String[] abbreviations;
	private final NucleotideCode[] nucleotideCodes;
	private final String complimentAbbreviation;

	private static Map<Set<NucleotideCode>, IupacNucleotideCode> reverseCodeMap = new HashMap<Set<NucleotideCode>, IupacNucleotideCode>();
	static {
		for (IupacNucleotideCode code : IupacNucleotideCode.values()) {
			if (code != IupacNucleotideCode.U) {
				Set<NucleotideCode> key = new HashSet<NucleotideCode>();
				for (NucleotideCode nucleotideCode : code.nucleotideCodes) {
					key.add(nucleotideCode);
				}
				reverseCodeMap.put(key, code);
			}
		}
	}

	private IupacNucleotideCode(String abbreviation, NucleotideCode nucleotideCode, String complimentAbbreviation) {
		this(abbreviation, new NucleotideCode[] { nucleotideCode }, complimentAbbreviation);
	}

	private IupacNucleotideCode(String abbreviation, NucleotideCode[] nucleotides, String complimentAbbreviation) {
		this(new String[] { abbreviation }, nucleotides, complimentAbbreviation);
	}

	private IupacNucleotideCode(String[] abbreviations, NucleotideCode[] nucleotides, String complimentAbbreviation) {
		this.abbreviations = Arrays.copyOf(abbreviations, abbreviations.length);
		this.nucleotideCodes = Arrays.copyOf(nucleotides, nucleotides.length);
		this.complimentAbbreviation = complimentAbbreviation;
	}

	public static IupacNucleotideCode getCode(Set<NucleotideCode> nucleotideCodes) {
		return reverseCodeMap.get(nucleotideCodes);
	}

	/**
	 * @param codeAsString
	 * @return returns null if no such string is found
	 */
	static IupacNucleotideCode[] getCodesFromString(String codesAsString) {
		Objects.requireNonNull(codesAsString, "argument[codesAsString] cannot be null");
		codesAsString = codesAsString.toUpperCase();
		IupacNucleotideCode[] codes = new IupacNucleotideCode[codesAsString.length()];

		for (int i = 0; i < codesAsString.length(); i++) {
			String characterToTranslate = codesAsString.substring(i, i + 1);
			IupacNucleotideCode matchingCode = getCodeFromString(characterToTranslate);

			if (matchingCode != null) {
				codes[i] = matchingCode;
			} else {
				throw new IllegalArgumentException("Unable to translate character[" + characterToTranslate + "] from codesAsString[" + codesAsString + "].");
			}
		}

		return codes;
	}

	@Override
	public String toString() {
		if (abbreviations.length == 0) {
			throw new IllegalArgumentException("There is not at least one code for the given IupacNucleotideCode[" + this.name() + "].");
		}

		return abbreviations[0];
	}

	/**
	 * @param codeAsString
	 * @return return IUPAC code that matches the provided code or null if no such string is found
	 */
	public static IupacNucleotideCode getCodeFromNucleotide(ICode code) {
		IupacNucleotideCode matchingCode = null;

		if (code instanceof NucleotideCode) {
			IupacCodeLoop: for (IupacNucleotideCode iupacCode : IupacNucleotideCode.values()) {
				NucleotideCode[] nucleotides = iupacCode.getNucleotides();

				if ((nucleotides.length == 1) && nucleotides[0].equals(code)) {
					matchingCode = iupacCode;

					break IupacCodeLoop;
				}
			}
		} else if (code instanceof IupacNucleotideCode) {
			matchingCode = (IupacNucleotideCode) code;
		}

		return matchingCode;
	}

	/**
	 * @return the nucleotide equivalent to the provided code
	 */
	public NucleotideCode getNucleotideCodeEquivalent() {
		NucleotideCode nucleotideCodeEquivalent = null;

		if (this == A) {
			nucleotideCodeEquivalent = NucleotideCode.ADENINE;
		} else if (this == C) {
			nucleotideCodeEquivalent = NucleotideCode.CYTOSINE;
		} else if (this == G) {
			nucleotideCodeEquivalent = NucleotideCode.GUANINE;
		} else if (this == T) {
			nucleotideCodeEquivalent = NucleotideCode.THYMINE;
		} else {
			throw new IllegalStateException("Cannot convert iupac nucleotide code[" + this + "] to a nucleotide code [a,c,g,t].");
		}

		return nucleotideCodeEquivalent;

	}

	/**
	 * @param codeAsString
	 * @return return the IUPAC code equivalent of the provided string or null if no such string is found
	 */
	public static IupacNucleotideCode getCodeFromString(String codeAsString) {
		if (codeAsString.length() > 1) {
			throw new IllegalArgumentException("getCodeFromString only accepts strings of length 1 for argument, codeAsString[" + codeAsString + "] does not match this criteria.");
		}

		IupacNucleotideCode matchingCode = NucleotideCodeLookup.getInstance().getIupacNucleotideCode(codeAsString);

		return matchingCode;
	}

	/**
	 * @return all abbreviations for this code
	 */
	public String[] getAbbreviations() {
		return Arrays.copyOf(abbreviations, abbreviations.length);
	}

	@Override
	public NucleotideCode[] getNucleotides() {
		return Arrays.copyOf(nucleotideCodes, nucleotideCodes.length);
	}

	/**
	 * @param baseCall
	 * @return true if their is any overlap in the nucleotides sets that these two symbols represent
	 */
	@Override
	public boolean matches(ICode baseCall) {
		boolean matches = equals(baseCall);

		if (!matches) {
			nucleotideLoop: for (NucleotideCode nucleotideCode : nucleotideCodes) {
				for (NucleotideCode baseNucleotide : baseCall.getNucleotides()) {
					if (nucleotideCode == baseNucleotide) {
						matches = true;

						break nucleotideLoop;
					}
				}
			}
		}

		return matches;
	}

	@Override
	public IupacNucleotideCode getIupaceNucleotideCodeEquivalent() {
		return this;
	}

	@Override
	public ICode getComplimentCode() {
		return getCodeFromString(complimentAbbreviation);
	}

}
