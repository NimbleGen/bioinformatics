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
