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
