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
package com.roche.sequencing.bioinformatics.common.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ArraysUtilTest {

	@Test(groups = { "unit" })
	public void testMin() {
		Assert.assertEquals(ArraysUtil.min(-1, 5, 10, 10000), -1);
	}

	@Test(groups = { "unit" })
	public void testMax() {
		Assert.assertEquals(ArraysUtil.max(-1, 5, 10, 10000), 10000);
	}

	@Test(groups = { "unit" })
	public void testConvertToDoubleArray() {
		double[] doubleValues = new double[] { -1, 5, 10, 10000 };
		int[] intValues = new int[] { -1, 5, 10, 10000 };
		Assert.assertEquals(ArraysUtil.convertToDoubleArray(intValues), doubleValues);
	}

}
