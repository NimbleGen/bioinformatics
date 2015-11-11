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
		assertEquals(mergeInformation.getMergedQuality(), "HHHHHHHHHHHHHH");
	}

	@Test(groups = { "unit" })
	public void gapOneTest() {
		ISequence upstreamSequence = new IupacNucleotideCodeSequence("AACCGGTT");
		String upstreamQuality = "HHHHHHHH";
		ISequence downstreamSequence = new IupacNucleotideCodeSequence("TTGGAACC");
		String downstreamQuality = "HHHHHHHH";
		MergeInformation mergeInformation = PrimerReadExtensionAndPcrDuplicateIdentification.mergeSequences(false, upstreamSequence, downstreamSequence, upstreamQuality, downstreamQuality, 10, 20);
		assertEquals(mergeInformation.getMergedSequence(), new IupacNucleotideCodeSequence("AACCGGTTNNTTGGAACC"));
		assertEquals(mergeInformation.getMergedQuality(), "HHHHHHHH!!HHHHHHHH");
	}

	@Test(groups = { "unit" })
	public void disagreementOneTest() {
		ISequence upstreamSequence = new IupacNucleotideCodeSequence("AACCGGTC");
		String upstreamQuality = "HHHHHHHI";
		ISequence downstreamSequence = new IupacNucleotideCodeSequence("TTGGAACC");
		String downstreamQuality = "HHHHHHHH";
		MergeInformation mergeInformation = PrimerReadExtensionAndPcrDuplicateIdentification.mergeSequences(false, upstreamSequence, downstreamSequence, upstreamQuality, downstreamQuality, 10, 16);
		assertEquals(mergeInformation.getMergedSequence(), new IupacNucleotideCodeSequence("AACCGGTCGGAACC"));
		assertEquals(mergeInformation.getMergedQuality(), "HHHHHHHIHHHHHH");
	}

	@Test(groups = { "unit" })
	public void noOverlapOneTest() {
		ISequence upstreamSequence = new IupacNucleotideCodeSequence("AACCGGTC");
		String upstreamQuality = "HHHHHHHH";
		ISequence downstreamSequence = new IupacNucleotideCodeSequence("TTGGAACC");
		String downstreamQuality = "HHHHHHHH";
		MergeInformation mergeInformation = PrimerReadExtensionAndPcrDuplicateIdentification.mergeSequences(false, upstreamSequence, downstreamSequence, upstreamQuality, downstreamQuality, 1, 9);
		assertEquals(mergeInformation.getMergedSequence(), new IupacNucleotideCodeSequence("AACCGGTCTTGGAACC"));
		assertEquals(mergeInformation.getMergedQuality(), "HHHHHHHHHHHHHHHH");
	}

	@Test(groups = { "unit" })
	public void onlyOverlapOneTest() {
		ISequence upstreamSequence = new IupacNucleotideCodeSequence("AACCGGTC");
		String upstreamQuality = "HHHHHHHH";
		ISequence downstreamSequence = new IupacNucleotideCodeSequence("AACCGGTC");
		String downstreamQuality = "HHHHHHHH";
		MergeInformation mergeInformation = PrimerReadExtensionAndPcrDuplicateIdentification.mergeSequences(false, upstreamSequence, downstreamSequence, upstreamQuality, downstreamQuality, 1, 1);
		assertEquals(mergeInformation.getMergedSequence(), new IupacNucleotideCodeSequence("AACCGGTC"));
		assertEquals(mergeInformation.getMergedQuality(), "HHHHHHHH");
	}

	@Test(groups = { "unit" })
	public void longerUpstreamOneTest() {
		ISequence upstreamSequence = new IupacNucleotideCodeSequence("TAACCGGTCC");
		String upstreamQuality = "HHHHHHHHHH";
		ISequence downstreamSequence = new IupacNucleotideCodeSequence("AACCGGTC");
		String downstreamQuality = "HHHHHHHH";
		MergeInformation mergeInformation = PrimerReadExtensionAndPcrDuplicateIdentification.mergeSequences(false, upstreamSequence, downstreamSequence, upstreamQuality, downstreamQuality, 25, 26);
		assertEquals(mergeInformation.getMergedSequence(), new IupacNucleotideCodeSequence("TAACCGGTCC"));
		assertEquals(mergeInformation.getMergedQuality(), "HHHHHHHHHH");
	}

}
