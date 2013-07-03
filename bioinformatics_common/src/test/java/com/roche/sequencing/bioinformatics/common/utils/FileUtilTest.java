package com.roche.sequencing.bioinformatics.common.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.testng.Assert;
import org.testng.annotations.Test;

public class FileUtilTest {

	@Test(groups = { "unit" })
	public void testGetFileExtension() {
		String extension = "extension";
		File file = new File("/path/to/file/file." + extension);
		Assert.assertEquals(FileUtil.getFileExtension(file), extension);
	}

	@Test(groups = { "unit" })
	public void testGetFileNameWithoutExtension() {
		String fileName = "fileName";
		String extension = "extension";
		File file = new File("/path/to/file/" + fileName + "." + extension);
		Assert.assertEquals(FileUtil.getFileNameWithoutExtension(file.getName()), fileName);
	}

	@Test(groups = { "unit" })
	public void testReadFileAsString() throws IOException {
		String fileContents = "Test_contents";
		URL filePath = getClass().getResource("readFileAsStringTestFile.txt");
		File testFile = new File(filePath.getPath());
		String fileAsString = FileUtil.readFileAsString(testFile);
		Assert.assertEquals(fileAsString, fileContents);
	}

}
