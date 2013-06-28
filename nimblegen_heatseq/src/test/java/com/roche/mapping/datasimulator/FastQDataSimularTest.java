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

package com.roche.mapping.datasimulator;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;

public class FastQDataSimularTest {

	@Test(groups = { "unit" })
	public void mutateTest() {
		ISequence referenceSequence = new IupacNucleotideCodeSequence("AAAAA");
		String mismatchDetailsString = "^CTM3TT";
		ISequence mutatedSequence = FastQDataSimulator.mutate(referenceSequence, mismatchDetailsString);
		assertEquals(mutatedSequence, new IupacNucleotideCodeSequence("CTAAATT"));
	}

	@Test(groups = { "unit" })
	public void mutateTest2() {
		ISequence referenceSequence = new IupacNucleotideCodeSequence("AAAAA");
		String mismatchDetailsString = "^CTM3TT^AAA";
		ISequence mutatedSequence = FastQDataSimulator.mutate(referenceSequence, mismatchDetailsString);
		assertEquals(mutatedSequence, new IupacNucleotideCodeSequence("CTAAATTAAA"));
	}

	@Test(groups = { "unit" })
	public void mutateTest3() {
		ISequence referenceSequence = new IupacNucleotideCodeSequence("AAAAA");
		String mismatchDetailsString = "^CTM3^GGGM0TT";
		ISequence mutatedSequence = FastQDataSimulator.mutate(referenceSequence, mismatchDetailsString);
		assertEquals(mutatedSequence, new IupacNucleotideCodeSequence("CTAAAGGGTT"));
	}

	@Test(groups = { "unit" })
	public void mutateTest4() {
		ISequence referenceSequence = new IupacNucleotideCodeSequence("AAAAA");
		String mismatchDetailsString = "M5";
		ISequence mutatedSequence = FastQDataSimulator.mutate(referenceSequence, mismatchDetailsString);
		assertEquals(mutatedSequence, new IupacNucleotideCodeSequence("AAAAA"));
	}

	@Test(groups = { "unit" })
	public void mutateTest5() {
		ISequence referenceSequence = new IupacNucleotideCodeSequence("AAAAA");
		String mismatchDetailsString = "^CTD1M2^GGGM0TT";
		ISequence mutatedSequence = FastQDataSimulator.mutate(referenceSequence, mismatchDetailsString);
		assertEquals(mutatedSequence, new IupacNucleotideCodeSequence("CTAAGGGTT"));
	}

}
