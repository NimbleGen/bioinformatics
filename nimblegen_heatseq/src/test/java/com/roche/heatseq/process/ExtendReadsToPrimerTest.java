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

package com.roche.heatseq.process;

import org.testng.annotations.Test;

public class ExtendReadsToPrimerTest {

	private final static String[] readOne = { "GGAGGTGGAGGCGGCAGTGGCGGTGGTGGTGAGGGAGGGGGTGGCCCCTGAGCGTCATCTGCCCCC", "CGTCCCTCGCAAGTCAGGGGATCCGAGGTACCTGCAGCAGCAGCAGCAGCGCCGAGAGGCTGCGGC",
			"TCCATTTCTTAGAGGGAATGGTATATATACTTTATATAATATATAGTAATAATAGAATAAAAATAA" };
	private final static String[] readTwo = { "TCGCTGCCCAGCACCGCCGTCTGGTTGGCCGGCAGCCCCGCCTGCAGGATGGGCCGGTGCGGGGAGCGCTCTGTGG",
			"CCAGCCCGGCCCGACCCGACCGCACCCGGCGCCTGCCCTCGCTCGGCGTCCCCGGCCAGCCATGGGCCCTTGGAGC", "CAAGATGATGGGAGGCCTAGGATTTCATGAAGTCCTCAATAATGTAAGTAAACCTGAAAATCAAACCACAATAATT" };
	private final static String[] primerOne = { "GTGGGGGCGGCAGTGGCGG", "ATGCGTCCCTCGCAAGTCAGGG", "CTTAGAGGGAATGGTATA" };
	private final static String[] primerTwo = { "GCGGTGCTGGGCAGCGACGTG", "GGTCGGGTCGGGCCGGGC", "CTAGGCCTCCCATCATCTTGGTCCCCA" };

	@Test(groups = { "unit" })
	public void extendReadsTest_0() {
		testReads(0);
	}

	@Test(groups = { "unit" })
	public void extendReadsTest_1() {
		testReads(1);
	}

	@Test(groups = { "unit" })
	public void extendReadsTest_2() {
		testReads(2);
	}

	private void testReads(int testIndex) {
		testReads(primerOne[testIndex], primerTwo[testIndex], readOne[testIndex], readTwo[testIndex]);
	}

	private void testReads(String primerOne, String primerTwo, String readOne, String readTwo) {
		// Integer index = ExtendReadsToPrimer.getPrimerEndIndexInRead(new IupacNucleotideCodeSequence(primerOne), new IupacNucleotideCodeSequence(readOne), new SimpleAlignmentScorer());
		// Assert.assertNotNull(index);
		// index = ExtendReadsToPrimer.getPrimerEndIndexInRead(new IupacNucleotideCodeSequence(primerTwo), new IupacNucleotideCodeSequence(readTwo).getReverseCompliment(), new
		// SimpleAlignmentScorer());
		// Assert.assertNotNull(index);

	}

}
