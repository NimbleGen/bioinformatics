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

import org.testng.Assert;
import org.testng.annotations.Test;

import com.roche.sequencing.bioinformatics.common.alignment.AlignmentPair;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;

public class NucleotideSequenceTest {
	@Test(groups = { "unit" })
	public void testCreateReallyLargeSequence() {
		String repeatedString = "ACGTGGTTAACCTACGTCACCATGCAAAACCCCTTTGGGATCAGTCGTGCGCTCTCTGAGAGAGACTGCAGTCCCCCGGGTGTGTGCGCGCGAAGTCGTG";
		NucleotideCodeSequence nucleotideCodeSequence = new NucleotideCodeSequence(repeatedString);
		IupacNucleotideCodeSequence iupacSequence = new IupacNucleotideCodeSequence(repeatedString);
		int numberOfRepeats = 3000;

		for (int i = 0; i < numberOfRepeats; i++) {
			nucleotideCodeSequence.append(new NucleotideCodeSequence(repeatedString));
			iupacSequence.append(new IupacNucleotideCodeSequence(repeatedString));
		}

		Assert.assertEquals(nucleotideCodeSequence.size(), repeatedString.length() * (numberOfRepeats + 1));
	}

	@Test(groups = { "unit" })
	public void testAppend() {
		String one = "AACC";
		String two = "GGTT";
		String twoAppendedToOne = "AACCGGTT";
		NucleotideCodeSequence seqOne = new NucleotideCodeSequence(one);
		NucleotideCodeSequence seqTwo = new NucleotideCodeSequence(two);
		NucleotideCodeSequence seqTwoAppendedToOne = new NucleotideCodeSequence(twoAppendedToOne);

		seqOne.append(seqTwo);
		Assert.assertEquals(seqOne, seqTwoAppendedToOne);
		Assert.assertEquals(seqOne.toString(), seqTwoAppendedToOne.toString());
		Assert.assertEquals(seqOne.toStringAsBits(), seqTwoAppendedToOne.toStringAsBits());
	}

	@Test(groups = { "unit" })
	public void testAlignment() {
		String repeatedString = "AATTACCGATATAATTTTACTTTTGTCCCC";
		NucleotideCodeSequence nucleotideCodeSequence = new NucleotideCodeSequence(repeatedString);
		NucleotideCodeSequence nucleotideSequenceToFind = new NucleotideCodeSequence("GATAT");
		NeedlemanWunschGlobalAlignment needlemanWunsch = new NeedlemanWunschGlobalAlignment(nucleotideCodeSequence, nucleotideSequenceToFind);
		AlignmentPair alignment2 = needlemanWunsch.getAlignmentPair();

		Assert.assertNotNull(alignment2);
		Assert.assertFalse(needlemanWunsch.getTraceabilityMatrixAsString().isEmpty());
	}

}
