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

package com.roche.sequencing.bioinformatics.common.alignment;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;

public class CigarStringUtilTest {

	@BeforeClass
	public void setUp() {
	}

	@Test(groups = { "integration" })
	public void mismatchDetailsOneTest() {
		ISequence sequenceOne = new IupacNucleotideCodeSequence("AAAAAAAAAAATTTTT--GTTTTT");
		ISequence sequenceTwo = new IupacNucleotideCodeSequence("AAAAAAAAAAGTTTTTACATTTTT");
		NeedlemanWunschGlobalAlignment globalAlignment = new NeedlemanWunschGlobalAlignment(sequenceOne, sequenceTwo);
		AlignmentPair alignmentPair = globalAlignment.getAlignmentPair();
		String mismatchDetailsString = CigarStringUtil.getMismatchDetailsString(alignmentPair);
		assertEquals(mismatchDetailsString, "10A5^ACG5");
	}

	@Test(groups = { "integration" })
	public void mismatchDetailsOneATest() {
		ISequence sequenceOne = new IupacNucleotideCodeSequence("AAAAAAAAAAGTTTTTACATTTTT");
		ISequence sequenceTwo = new IupacNucleotideCodeSequence("AAAAAAAAAAATTTTT--GTTTTT");
		NeedlemanWunschGlobalAlignment globalAlignment = new NeedlemanWunschGlobalAlignment(sequenceOne, sequenceTwo);
		AlignmentPair alignmentPair = globalAlignment.getAlignmentPair();
		String mismatchDetailsString = CigarStringUtil.getMismatchDetailsString(alignmentPair);
		assertEquals(mismatchDetailsString, "10G5A5");
	}

	@Test(groups = { "integration" })
	public void mismatchDetailsTwoTest() {
		ISequence sequenceOne = new IupacNucleotideCodeSequence(
				"CATTCCAGGCCTAAGATCCCGTCCATCGCCACTGGGATGGTGGGGGCCCTCCTCTTGCTGCTGGTGGTGGCCCTGGGGATCGGCCTCTTCATGCGAAGGCGCCACATCGTTCGGAAGCGCACGCTGCGGAGGCTGCTGCAGGAGAGGGAG");
		ISequence sequenceTwo = new IupacNucleotideCodeSequence("GTCCATCGCCAGTGGGATGGTGGGGGCCCTCCTCTTGCTGCTGGTGGTGGCCCTGGGGATCGGCCTCTTCATGCGAAGGCGCCACATCGTTCGGAAGCGCACGCTGCGGAGGCTGCTGCAGGAGAGGGAG");
		NeedlemanWunschGlobalAlignment globalAlignment = new NeedlemanWunschGlobalAlignment(sequenceOne, sequenceTwo);
		AlignmentPair alignmentPair = globalAlignment.getAlignmentPair();
		String mismatchDetailsString = CigarStringUtil.getMismatchDetailsString(alignmentPair);
		assertEquals(mismatchDetailsString, "11C118");
	}

	@Test(groups = { "integration" })
	public void mismatchDetailsThreeTest() {
		ISequence sequenceOne = new IupacNucleotideCodeSequence(
				"GACCCTTGTCTCTGTGTTCTTGTCCCCCCCAGCTTGTGGAGCCTCTTACACCCAGTGGAGAAGCTCCCAACCAAGCTCTCTTGAGGATCTTGAAGGAAACTGAATTCAAAAAGATCAAAGTGCTGGGCTCCGGTGCGTTCGGCACGGTGT");
		ISequence sequenceTwo = new IupacNucleotideCodeSequence("GCCCCCCCAGCTTGTGGAGCCTCTTACACCCAGTGGAGAAGCTCCCAACCAAGCTCTCTTGAGGATCTTGAAGGAAACTGAATTCAAAAAGATCAAAGTGCTGGGCTCCGGTGCGTTCGGCACGGTGT");
		NeedlemanWunschGlobalAlignment globalAlignment = new NeedlemanWunschGlobalAlignment(sequenceOne, sequenceTwo);
		AlignmentPair alignmentPair = globalAlignment.getAlignmentPair();
		String mismatchDetailsString = CigarStringUtil.getMismatchDetailsString(alignmentPair.getAlignmentWithoutEndingAndBeginningQueryInserts().getReferenceAlignment(), alignmentPair
				.getAlignmentWithoutEndingAndBeginningQueryInserts().getQueryAlignment(), globalAlignment.getCigarString());
		assertEquals(mismatchDetailsString, "0T127");
	}

	@Test(groups = { "integration" })
	public void mismatchDetailsFourTest() {
		ISequence sequenceOne = new IupacNucleotideCodeSequence(
				"GACCCTTGTCTCTGTGTTCTTGTCCCCCCCAGCTTGTGGAGCCTCTTACACCCAGTGGAGAAGCTCCCAACCAAGCTCTCTTGAGGAGGGTCTTGAAGGAAACTGAATTCAAAAAGATCAAAGTGCTGGGCTCCGGTGCGTTCGGCACGGTGT");
		ISequence sequenceTwo = new IupacNucleotideCodeSequence("GCCCCCCCAGCTTGTGGAGCCTCTTACACCCAGTGGAGAAGCTCCCAACCAAGCTCTCTTGAGGATCTTGAAGGAAACTGAATTCAAAAAGATCAAAGTGCTGGGCTCCGGTGCGTTCGGCACGGTGT");
		NeedlemanWunschGlobalAlignment globalAlignment = new NeedlemanWunschGlobalAlignment(sequenceOne, sequenceTwo);
		AlignmentPair alignmentPair = globalAlignment.getAlignmentPair();
		String mismatchDetailsString = CigarStringUtil.getMismatchDetailsString(alignmentPair.getAlignmentWithoutEndingAndBeginningQueryInserts().getReferenceAlignment(), alignmentPair
				.getAlignmentWithoutEndingAndBeginningQueryInserts().getQueryAlignment(), globalAlignment.getCigarString());
		assertEquals(mismatchDetailsString, "0T127");
	}

	@Test(groups = { "integration" })
	public void mismatchReverseDetailsTest() {
		ISequence sequenceOne = new IupacNucleotideCodeSequence(
				"CATTCCAGGCCTAAGATCCCGTCCATCGCCACTGGGATGGTGGGGGCCCTCCTCTTGCTGCTGGTGGTGGCCCTGGGGATCGGCCTCTTCATGCGAAGGCGCCACATCGTTCGGAAGCGCACGCTGCGGAGGCTGCTGCAGGAGAGGGAG").getReverse();
		ISequence sequenceTwo = new IupacNucleotideCodeSequence("GTCCATCGCCAGTGGGATGGTGGGGGCCCTCCTCTTGCTGCTGGTGGTGGCCCTGGGGATCGGCCTCTTCATGCGAAGGCGCCACATCGTTCGGAAGCGCACGCTGCGGAGGCTGCTGCAGGAGAGGGAG")
				.getReverse();
		NeedlemanWunschGlobalAlignment globalAlignment = new NeedlemanWunschGlobalAlignment(sequenceOne, sequenceTwo);
		AlignmentPair alignmentPair = globalAlignment.getAlignmentPair();
		String mismatchDetailsString = CigarStringUtil.getMismatchDetailsString(alignmentPair.getAlignmentWithoutEndingAndBeginningQueryInserts().getReferenceAlignment().getReverse(), alignmentPair
				.getAlignmentWithoutEndingAndBeginningQueryInserts().getQueryAlignment().getReverse(), globalAlignment.getReverseCigarString());
		assertEquals(mismatchDetailsString, "11C118");
	}

	@Test(groups = { "integration" })
	public void editDistanceTest() {
		ISequence sequenceOne = new IupacNucleotideCodeSequence("CATTCCG");
		ISequence sequenceTwo = new IupacNucleotideCodeSequence("CACTCCG");
		NeedlemanWunschGlobalAlignment globalAlignment = new NeedlemanWunschGlobalAlignment(sequenceOne, sequenceTwo);
		AlignmentPair alignmentPair = globalAlignment.getAlignmentPair();
		CigarString cigarString = CigarStringUtil.getCigarString(alignmentPair);
		int editDistance = CigarStringUtil.getEditDistance(cigarString);
		assertEquals(editDistance, 1);
	}

}
