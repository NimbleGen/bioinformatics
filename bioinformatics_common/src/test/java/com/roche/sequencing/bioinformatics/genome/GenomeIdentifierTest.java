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
package com.roche.sequencing.bioinformatics.genome;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

public class GenomeIdentifierTest {

	@Test(groups = { "unit" })
	public void testHg19() {
		Map<String, Integer> hg19Sizes = new HashMap<String, Integer>();
		hg19Sizes.put("chr1", 249250621);
		hg19Sizes.put("chr3", 198022430);
		String matchingName = GenomeIdentifier.getMatchingGenomeName(hg19Sizes);
		Assert.assertEquals(matchingName, "hg19");
	}

	@Test(groups = { "unit" })
	public void testHg19_2() {
		Map<String, Integer> hg19Sizes = new HashMap<String, Integer>();
		hg19Sizes.put("chr1", 249250621);
		hg19Sizes.put("nonsense", -11119);
		String matchingName = GenomeIdentifier.getMatchingGenomeName(hg19Sizes);
		Assert.assertNull(matchingName);
	}

}
