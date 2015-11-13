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

package com.roche.sequencing.bioinformatics.common.mapping;

import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.roche.sequencing.bioinformatics.common.sequence.NucleotideCodeSequence;

public class SimpleMapperTest {

	@BeforeClass
	public void setUp() {
	}

	@Test(groups = { "integration" })
	public void mappingOneTest() {
		String refOne = "chr1";
		String refTwo = "chr2";
		String refThree = "chr3";
		SimpleMapper<String> simpleMapper = new SimpleMapper<String>();
		simpleMapper.addReferenceSequence(new NucleotideCodeSequence("AACCGGTT"), refOne);
		simpleMapper.addReferenceSequence(new NucleotideCodeSequence("ATATATAAT"), refTwo);
		simpleMapper.addReferenceSequence(new NucleotideCodeSequence("CGATCGATT"), refThree);

		String querySequence = "ATACGA";
		List<String> candidates = simpleMapper.getBestCandidateReferences(new NucleotideCodeSequence(querySequence));
		assertEquals(candidates.size(), 0);
	}

	@Test(groups = { "integration" })
	public void mappingTwoTest() {
		String refOne = "chr1";
		String refTwo = "chr2";
		String refThree = "chr3";
		SimpleMapper<String> simpleMapper = new SimpleMapper<String>();
		simpleMapper.addReferenceSequence(new NucleotideCodeSequence("TGAAGGGAGGATGGGC"), refOne);
		simpleMapper.addReferenceSequence(new NucleotideCodeSequence("ATATATAAT"), refTwo);
		simpleMapper.addReferenceSequence(new NucleotideCodeSequence("CGATCGATT"), refThree);

		String querySequence = "TGAAGGGAGGATGGGC";
		List<String> candidates = simpleMapper.getBestCandidateReferences(new NucleotideCodeSequence(querySequence));
		assertEquals(candidates.size(), 1);
	}

}
