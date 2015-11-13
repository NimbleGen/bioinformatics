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

package com.roche.heatseq.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

public class IlluminaFastqQReadNameUtilTest {

	@Test(groups = { "unit" })
	public void oldIlluminaReadNameTest() {
		String readName = "@MS5_15454:1:1110:12527:26507#26/1";
		String expectedReadName = "@MS5_15454:1:1110:12527:26507#26";
		Assert.assertEquals(IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(readName), expectedReadName);
	}

	@Test(groups = { "unit" })
	public void newIlluminaReadNameTest() {
		String readName = "@M01077:35:000000000-A3J96:1:1102:13646:7860 1:N:0:1";
		String expectedReadName = "@M01077:35:000000000-A3J96:1:1102:13646:7860";
		Assert.assertEquals(IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(readName), expectedReadName);
	}

	@Test(groups = { "unit" })
	public void simpleTextTest() {
		String readName = "@test1";
		String expectedReadName = "@test1";
		Assert.assertEquals(IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(readName), expectedReadName);
	}

}
