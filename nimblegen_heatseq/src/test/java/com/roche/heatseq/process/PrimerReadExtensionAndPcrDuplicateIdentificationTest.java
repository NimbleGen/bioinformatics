/**
 *   Copyright 2013 Roche NimbleGen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.roche.heatseq.process;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.roche.heatseq.process.PrimerReadExtensionAndPcrDuplicateIdentification.MergeInformation;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;

public class PrimerReadExtensionAndPcrDuplicateIdentificationTest {

	@Test(groups = { "unit" })
	public void standardOneTest() {
		ISequence upstreamSequence = new IupacNucleotideCodeSequence("AACCGGTT");
		String upstreamQuality = "HHHHHHHH";
		ISequence downstreamSequence = new IupacNucleotideCodeSequence("TTGGAACC");
		String downstreamQuality = "HHHHHHHH";
		MergeInformation mergeInformation = PrimerReadExtensionAndPcrDuplicateIdentification.mergeSequences(false, upstreamSequence, downstreamSequence, upstreamQuality, downstreamQuality, 10, 16);
		assertEquals(mergeInformation.getMergedSequence(), new IupacNucleotideCodeSequence("AACCGGTTGGAACC"));
	}

	@Test(groups = { "unit" })
	public void gapOneTest() {
		ISequence upstreamSequence = new IupacNucleotideCodeSequence("AACCGGTT");
		String upstreamQuality = "HHHHHHHH";
		ISequence downstreamSequence = new IupacNucleotideCodeSequence("TTGGAACC");
		String downstreamQuality = "HHHHHHHH";
		MergeInformation mergeInformation = PrimerReadExtensionAndPcrDuplicateIdentification.mergeSequences(false, upstreamSequence, downstreamSequence, upstreamQuality, downstreamQuality, 10, 20);
		assertEquals(mergeInformation.getMergedSequence(), new IupacNucleotideCodeSequence("AACCGGTTNNTTGGAACC"));
	}

	@Test(groups = { "unit" })
	public void disagreementOneTest() {
		ISequence upstreamSequence = new IupacNucleotideCodeSequence("AACCGGTC");
		String upstreamQuality = "HHHHHHHI";
		ISequence downstreamSequence = new IupacNucleotideCodeSequence("TTGGAACC");
		String downstreamQuality = "HHHHHHHH";
		MergeInformation mergeInformation = PrimerReadExtensionAndPcrDuplicateIdentification.mergeSequences(false, upstreamSequence, downstreamSequence, upstreamQuality, downstreamQuality, 10, 16);
		assertEquals(mergeInformation.getMergedSequence(), new IupacNucleotideCodeSequence("AACCGGTCGGAACC"));
	}

}
