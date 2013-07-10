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
import java.net.URL;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecordIterator;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.google.common.io.Files;
import com.roche.mapping.datasimulator.FastQDataSimulator;

@Listeners(NgTestListener.class)
public class StressTest {

	private String outputBamFileName = "output.bam";
	private String fastqOneFileName = "large_one.fastq";
	private String fastqTwoFileName = "large_two.fastq";
	private String probeInfoFileName = "probes.txt";
	private String outputDirectoryPath;

	private File fastqOneFile;
	private File fastqTwoFile;
	private File probesFile;

	@BeforeClass(groups = { "stress" })
	public void setup() {
		File outputDirectory = Files.createTempDir();
		outputDirectoryPath = outputDirectory.getAbsolutePath();
		System.out.println("outputDirectory is [" + outputDirectoryPath + "].");
		FastQDataSimulator.createSimulatedIlluminaReads(outputDirectory, fastqOneFileName, fastqTwoFileName, probeInfoFileName, 10, 50, 50, 160, "M40D10R5^CCCAAATTTGGGM110", true);
		fastqOneFile = new File(outputDirectory, fastqOneFileName);
		fastqTwoFile = new File(outputDirectory, fastqTwoFileName);
		probesFile = new File(outputDirectory, probeInfoFileName);
	}

	@AfterClass(groups = { "stress" })
	public void teardown() {
		boolean hasFailedTests = NgTestListener.hasFailedTests(getClass());
		if (!hasFailedTests) {
			File outputDirectory = new File(outputDirectoryPath);
			outputDirectory.delete();
		}
	}

	@Test(groups = { "stress" })
	public void largeMapRunTest() {
		String[] args = new String[] { "--fastQOne", fastqOneFile.getAbsolutePath(), "--fastQTwo", fastqTwoFile.getAbsolutePath(), "--probe", probesFile.getAbsolutePath(), "--outputDir",
				outputDirectoryPath, "--outputBamFileName", outputBamFileName, "--uidLength", "14", "--outputReports" };

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

	@Test(groups = { "stress" })
	public void largeRunTest() {
		String outputBamFileName = "output.bam";
		URL fastQOneFilePath = getClass().getResource("one.fastq");
		URL fastQTwoFilePath = getClass().getResource("two.fastq");
		URL probeFilePath = getClass().getResource("probes.txt");
		URL bamFilePath = getClass().getResource("mapping.bam");

		String[] args = new String[] { "--fastQOne", fastQOneFilePath.getPath(), "--fastQTwo", fastQTwoFilePath.getPath(), "--probe", probeFilePath.getPath(), "--bam", bamFilePath.getPath(),
				"--outputDir", outputDirectoryPath, "--outputBamFileName", outputBamFileName, "--uidLength", "14", "--outputReports" };
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
