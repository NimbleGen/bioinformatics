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

import com.roche.mapping.SAMRecordUtil;
import com.roche.sequencing.bioinformatics.common.sequence.NucleotideCodeSequence;

public class SAMRecordReadExtenderTest {
	String[] probeSequences = new String[] { "CTTCCTTGTTCCTCCACCT", "AAAACTTCCGGGTTGTTCCTCCACCT" };
	String[] readSequences = new String[] { "CTTCCTTGTTCCTCCACCTCATTCCAGGCCTAAGATCCCGTCCATCGCCACTGGGATGGTGGGGGCCCTCCTCTTGCTGCTGGTGGTGGCCCTGGGGATCG", "GGGGGGGGCTTCCTTGTTCCTCCACCTCA" };
	Integer[] expectedStartIndexesInRead = new Integer[] { 18, null };
	private final int uidLength = 14;

	@Test(groups = { "unit" })
	public void readExtenderTest() {
		for (int i = 0; i < probeSequences.length; i++) {
			String probeSequence = probeSequences[i];
			String readSequence = readSequences[i];
			Integer expectedStartIndexInRead = expectedStartIndexesInRead[i];

			Integer actualStartIndexInRead = ExtendReadsToPrimer.getPrimerEndIndexInRead(new NucleotideCodeSequence(probeSequence), new NucleotideCodeSequence(readSequence));

			assertEquals(actualStartIndexInRead, expectedStartIndexInRead);
		}

	}

	@Test(groups = { "unit" })
	public void parseUidFromReadTest() {
		String expectedUid = "AAAAAAAAAAAAAC";
		String sequence = expectedUid + "CCCCCCCCCC";
		String uid = SAMRecordUtil.parseUidFromRead(sequence, uidLength);

		assertEquals(uid, expectedUid);
	}

	@Test(groups = { "unit" })
	public void removeUidFromReadTest() {
		String uid = "AAAAAAAAAAAAAC";
		String expectedSequenceWithoutUid = "CCCCCCCCCC";
		String sequence = uid + expectedSequenceWithoutUid;
		String sequenceWithoutUid = SAMRecordUtil.removeUidFromRead(sequence, uidLength);

		assertEquals(sequenceWithoutUid, expectedSequenceWithoutUid);
	}

}
