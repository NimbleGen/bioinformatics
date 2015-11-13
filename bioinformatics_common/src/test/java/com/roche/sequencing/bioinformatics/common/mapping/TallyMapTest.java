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

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TallyMapTest {

	@BeforeClass
	public void setUp() {
	}

	@Test(groups = { "unit" })
	public void mappingOneTest() {
		TallyMap<Integer> tallyMap = new TallyMap<Integer>();
		tallyMap.add(32);
		List<Integer> numbers = new ArrayList<Integer>();
		numbers.add(24);
		numbers.add(32);
		numbers.add(32);
		numbers.add(32);
		numbers.add(-32);
		numbers.add(0);
		tallyMap.addAll(numbers);
		Assert.assertEquals(tallyMap.getObjectsWithLargestCount().iterator().next(), new Integer(32));
		Assert.assertEquals(tallyMap.getLargestCount(), 4);
	}

}
