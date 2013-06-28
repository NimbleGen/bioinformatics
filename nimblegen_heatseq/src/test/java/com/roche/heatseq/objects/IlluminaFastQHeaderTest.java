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

package com.roche.heatseq.objects;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class IlluminaFastQHeaderTest {
	private final String longHeader1text = "M01077:30:000000000-A38DE:1:1101:14490:1877 1:N:0:1";
	private final String shortHeader1text = "M01077:30:000000000-A38DE:1:1101:14490:1877";
	private final IlluminaFastQHeader expectedLongHeader1 = new IlluminaFastQHeader("M01077", 30, "000000000-A38DE", 1, 1101, 14490, 1877, (short) 1, false, 0, "1");
	private final IlluminaFastQHeader expectedShortHeader1 = new IlluminaFastQHeader("M01077", 30, "000000000-A38DE", 1, 1101, 14490, 1877);

	@Test(groups = { "unit" })
	public void longHeader1Test() {
		IlluminaFastQHeader header1 = IlluminaFastQHeader.parseIlluminaFastQHeader(longHeader1text);

		assertEquals(header1, expectedLongHeader1);

	}

	@Test(groups = { "unit" })
	public void shortHeader1Test() {
		IlluminaFastQHeader header1 = IlluminaFastQHeader.parseIlluminaFastQHeader(shortHeader1text);

		assertEquals(header1, expectedShortHeader1);

	}

}
