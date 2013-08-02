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
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.google.common.io.Files;
import com.roche.bioinformatics.common.testing.NgTestListener;

@Listeners(NgTestListener.class)
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
		boolean hasFailedTests = NgTestListener.hasFailedTests(getClass());
		if (!hasFailedTests) {
			File outputDirectory = new File(outputDirectoryPath);
			try {
				FileUtils.deleteDirectory(outputDirectory);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		} else {
			System.out.println("This test has failed so preserving files at [" + outputDirectoryPath + "].");
		}
	}

	@Test(groups = { "smoke" })
	public void smallMapRunTest() {
		URL fastQOneFilePath = getClass().getResource("one.fastq");
		URL fastQTwoFilePath = getClass().getResource("two.fastq");
		URL probeFilePath = getClass().getResource("probes.txt");

		String[] args = new String[] { "--r1", fastQOneFilePath.getPath(), "--r2", fastQTwoFilePath.getPath(), "--probe", probeFilePath.getPath(), "--outputDir", outputDirectoryPath,
				"--outputBamFileName", outputBamFileName, "--uidLength", "14", "--outputReports", "--outputPrefix", "prefix" };

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

		String[] args = new String[] { "--r1", fastQOneFilePath.getPath(), "--r2", fastQTwoFilePath.getPath(), "--probe", probeFilePath.getPath(), "--inputBam", bamFilePath.getPath(), "--outputDir",
				outputDirectoryPath, "--outputBamFileName", outputBamFileName, "--uidLength", "14", "--outputReports", "--outputPrefix", "prefix" };
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
	public void smallReverseTest() {
		String outputBamFileName = "reverse_output.bam";
		URL fastQOneFilePath = getClass().getResource("reverse_test_one.fastq");
		URL fastQTwoFilePath = getClass().getResource("reverse_test_two.fastq");
		URL probeFilePath = getClass().getResource("reverse_test_probe.txt");
		URL bamFilePath = getClass().getResource("reverse_test.bam");
		// URL bamIndexFilePath = getClass().getResource("mapping.bam.bai");

		String[] args = new String[] { "--r1", fastQOneFilePath.getPath(), "--r2", fastQTwoFilePath.getPath(), "--probe", probeFilePath.getPath(), "--inputBam", bamFilePath.getPath(), "--outputDir",
				outputDirectoryPath, "--outputBamFileName", outputBamFileName, "--uidLength", "7", "--outputReports", "--outputPrefix", "prefix" };
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
	public void smallFowardTest() {
		String outputBamFileName = "reverse_output.bam";
		URL fastQOneFilePath = getClass().getResource("reverse_test_one.fastq");
		URL fastQTwoFilePath = getClass().getResource("reverse_test_two.fastq");
		URL probeFilePath = getClass().getResource("forward_test_probe.txt");
		URL bamFilePath = getClass().getResource("reverse_test.bam");
		// URL bamIndexFilePath = getClass().getResource("mapping.bam.bai");

		String[] args = new String[] { "--r1", fastQOneFilePath.getPath(), "--r2", fastQTwoFilePath.getPath(), "--probe", probeFilePath.getPath(), "--inputBam", bamFilePath.getPath(), "--outputDir",
				outputDirectoryPath, "--outputBamFileName", outputBamFileName, "--uidLength", "7", "--outputReports", "--outputPrefix", "prefix" };
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
