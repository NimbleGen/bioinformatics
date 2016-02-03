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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DelimitedFileParserUtilTest {

	@Test(groups = { "unit" })
	public void testWindowsGetHeaderNameToValuesMapFromDelimitedFileleAsString() throws IOException {
		testGetHeaderNameToValuesMapFromDelimitedFileleAsString("delimitedWindowsTestFile.txt");
	}

	@Test(groups = { "unit" })
	public void testLinuxGetHeaderNameToValuesMapFromDelimitedFileleAsString() throws IOException {
		testGetHeaderNameToValuesMapFromDelimitedFileleAsString("delimitedLinuxTestFile.txt");
	}

	private void testGetHeaderNameToValuesMapFromDelimitedFileleAsString(String fileName) throws IOException {
		String[] headerNames = new String[] { "header3", "header1" };
		URL filePath = getClass().getResource(fileName);
		File testFile = new File(filePath.getPath());
		Map<String, List<String>> headerNameToColumns = DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(testFile, headerNames, StringUtil.TAB);
		List<String> header3data = headerNameToColumns.get(headerNames[0]);
		Assert.assertEquals(header3data.get(0), "r1c3");
		Assert.assertEquals(header3data.get(1), "r2c3");

		List<String> header1data = headerNameToColumns.get(headerNames[1]);
		Assert.assertEquals(header1data.get(0), "r1c1");
		Assert.assertEquals(header1data.get(1), "r2c1");
	}

}
