/*
 *    Copyright 2013 Roche NimbleGen Inc.
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

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Simple Util to store codes for future lookup
 * 
 */
class NucleotideCodeLookup {
	private static NucleotideCodeLookup singleton;

	private final Map<String, NucleotideCode> abbreviationToNucleotideCodeMap;
	private final Map<String, IupacNucleotideCode> abbreviationToIupaceNucleotideCodeMap;

	private NucleotideCodeLookup() {
		abbreviationToNucleotideCodeMap = new HashMap<String, NucleotideCode>();

		for (NucleotideCode code : NucleotideCode.values()) {
			abbreviationToNucleotideCodeMap.put(code.getAbbreviation(), code);
		}

		abbreviationToIupaceNucleotideCodeMap = new HashMap<String, IupacNucleotideCode>();

		for (IupacNucleotideCode code : IupacNucleotideCode.values()) {
			for (String abbreviation : code.getAbbreviations()) {
				abbreviationToIupaceNucleotideCodeMap.put(abbreviation, code);
			}

		}
	}

	static NucleotideCodeLookup getInstance() {
		if (singleton == null) {
			singleton = new NucleotideCodeLookup();
		}

		return singleton;
	}

	NucleotideCode getNucleotideCode(String abbreviation) {
		return abbreviationToNucleotideCodeMap.get(abbreviation);
	}

	IupacNucleotideCode getIupacNucleotideCode(String abbreviation) {
		return abbreviationToIupaceNucleotideCodeMap.get(abbreviation);
	}

}
