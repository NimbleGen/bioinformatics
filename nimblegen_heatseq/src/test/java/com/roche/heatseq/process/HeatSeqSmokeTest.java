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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecordIterator;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.io.Files;

public class HeatSeqSmokeTest {

	Logger logger = LoggerFactory.getLogger(HeatSeqSmokeTest.class);

	private String outputBamFileName;
	private String outputDirectoryPath;

	@BeforeClass(groups = { "smoke" })
	public void setup() {
		File outputDirectory = Files.createTempDir();
		outputDirectoryPath = outputDirectory.getAbsolutePath();
		outputBamFileName = "output.bam";
	}

	@AfterClass(groups = { "smoke" })
	public void teardown() {
		try {
			FileUtils.deleteDirectory(new File(outputDirectoryPath));
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	@Test(groups = { "smoke" })
	public void smallMapRunTest() {
		URL fastQOneFilePath = getClass().getResource("one.fastq");
		URL fastQTwoFilePath = getClass().getResource("two.fastq");
		URL probeFilePath = getClass().getResource("probes.txt");

		String[] args = new String[] { "--fastQOne", fastQOneFilePath.getPath(), "--fastQTwo", fastQTwoFilePath.getPath(), "--probe", probeFilePath.getPath(), "--outputDir", outputDirectoryPath,
				"--outputBamFileName", outputBamFileName, "--uidLength", "14", "--outputReports" };

		PrefuppCli.runCommandLineApp(args);
		File outputBam = new File(outputDirectoryPath, outputBamFileName);
		int count = 0;
		try (final SAMFileReader samReader = new SAMFileReader(outputBam)) {

			SAMRecordIterator samRecordIter = samReader.iterator();
			while (samRecordIter.hasNext()) {
				samRecordIter.next();
				count++;
			}
			samRecordIter.close();
		}
		Assert.assertNotEquals(count, 0);
	}

	@Test(groups = { "smoke" })
	public void smallRunTest() {
		String outputBamFileName = "output.bam";
		URL fastQOneFilePath = getClass().getResource("one.fastq");
		URL fastQTwoFilePath = getClass().getResource("two.fastq");
		URL probeFilePath = getClass().getResource("probes.txt");
		URL bamFilePath = getClass().getResource("mapping.bam");
		URL bamIndexFilePath = getClass().getResource("mapping.bam.bai");

		String[] args = new String[] { "--fastQOne", fastQOneFilePath.getPath(), "--fastQTwo", fastQTwoFilePath.getPath(), "--probe", probeFilePath.getPath(), "--bam", bamFilePath.getPath(),
				"--bamIndex", bamIndexFilePath.getPath(), "--outputDir", outputDirectoryPath, "--outputBamFileName", outputBamFileName, "--uidLength", "14", "--outputReports" };
		PrefuppCli.runCommandLineApp(args);

		File outputBam = new File(outputDirectoryPath, outputBamFileName);
		int count = 0;
		try (final SAMFileReader samReader = new SAMFileReader(outputBam)) {

			SAMRecordIterator samRecordIter = samReader.iterator();
			while (samRecordIter.hasNext()) {
				samRecordIter.next();
				count++;
			}
			samRecordIter.close();
		}
		Assert.assertNotEquals(count, 0);
	}

}
