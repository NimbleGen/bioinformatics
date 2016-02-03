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
package com.roche.heatseq.utils;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Set;

import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.sequence.ICode;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCode;

public class NucleotideCompositionUtil {

	public static String getNucleotideCompositionByPosition(Collection<ISequence> sequences) {
		StringBuilder nucleotideComposition = new StringBuilder();

		int position = 0;
		boolean stillHasData = true;

		while (stillHasData) {
			TallyMap<ICode> codesAtPosition = new TallyMap<ICode>();
			for (ISequence sequence : sequences) {
				if (position < sequence.size()) {
					codesAtPosition.add(sequence.getCodeAt(position));
				}
			}
			stillHasData = codesAtPosition.getSumOfAllBins() > 0;

			if (stillHasData) {

				nucleotideComposition.append(position + "[");

				int total = codesAtPosition.sumOfAllBins;

				// need the display order to be consistent to use order from enum
				ICode[] codesToDisplay = new ICode[] { IupacNucleotideCode.A, IupacNucleotideCode.C, IupacNucleotideCode.G, IupacNucleotideCode.T };

				for (ICode code : codesToDisplay) {
					int count = codesAtPosition.getCount(code);
					double percent = (double) count / (double) total;
					DecimalFormat formatter = new DecimalFormat("0.00");
					nucleotideComposition.append(code + "(" + formatter.format(percent) + ")");
				}

				nucleotideComposition.append("] ");
				position++;
			}

		}

		return nucleotideComposition.toString();
	}

	public static String getNucleotideComposition(Collection<ISequence> sequences) {
		StringBuilder nucleotideComposition = new StringBuilder();

		TallyMap<ICode> codesTally = new TallyMap<ICode>();
		for (ISequence sequence : sequences) {
			for (ICode code : sequence) {
				codesTally.add(code);
			}
		}

		Set<ICode> foundCodes = codesTally.getObjects();

		int total = codesTally.getSumOfAllBins();

		// need the display order to be consistent to use order from enum
		for (ICode code : IupacNucleotideCode.values()) {
			if (foundCodes.contains(code)) {
				int count = codesTally.getCount(code);
				double percent = (double) count / (double) total;
				DecimalFormat formatter = new DecimalFormat(".000");
				nucleotideComposition.append(code + "(" + formatter.format(percent) + ")");
			}
		}

		return nucleotideComposition.toString();
	}

}
