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
import org.testng.annotations.Test;

import com.roche.mapping.datasimulator.FastQDataSimulator;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;

public class StressTest {

	private String outputBamFileName = "output.bam";
	private String fastqOneFileName = "large_one.fastq";
	private String fastqTwoFileName = "large_two.fastq";
	private String probeInfoFileName = "probes.txt";
	private String outputDirectory;

	private File fastqOneFile;
	private File fastqTwoFile;
	private File probesFile;

	@BeforeClass(groups = { "stress" })
	public void setup() {
		File tempDir = FileUtil.getSystemSpecificTempDirectory();
		outputDirectory = tempDir.getAbsolutePath();
		FastQDataSimulator.createSimulatedIlluminaReads(tempDir, fastqOneFileName, fastqTwoFileName, probeInfoFileName, 10000, 50, 50, 160, "M40D10R5^CCCAAATTTGGGM110", true);
		fastqOneFile = new File(outputDirectory, fastqOneFileName);
		fastqTwoFile = new File(outputDirectory, fastqTwoFileName);
		probesFile = new File(outputDirectory, probeInfoFileName);

	}

	@AfterClass(groups = { "stress" })
	public void teardown() {
		File outputBamFile = new File(outputDirectory, outputBamFileName);
		outputBamFile.delete();
		fastqOneFile.delete();
		fastqTwoFile.delete();
		probesFile.delete();
	}

	@Test(groups = { "stress" })
	public void smallMapRunTest() {
		String[] args = new String[] { "--fastQOne", fastqOneFile.getAbsolutePath(), "--fastQTwo", fastqTwoFile.getAbsolutePath(), "--probe", probesFile.getAbsolutePath(), "--outputDir",
				outputDirectory, "--outputBamFileName", outputBamFileName, "--uidLength", "14", "--outputReports" };

		PrefuppCli.runCommandLineApp(args);
		File outputBam = new File(outputDirectory, outputBamFileName);
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
	public void smallRunTest() {
		String outputBamFileName = "output.bam";
		URL fastQOneFilePath = getClass().getResource("one.fastq");
		URL fastQTwoFilePath = getClass().getResource("two.fastq");
		URL probeFilePath = getClass().getResource("probes.txt");
		URL bamFilePath = getClass().getResource("mapping.bam");

		String[] args = new String[] { "--fastQOne", fastQOneFilePath.getPath(), "--fastQTwo", fastQTwoFilePath.getPath(), "--probe", probeFilePath.getPath(), "--bam", bamFilePath.getPath(),
				"--outputDir", outputDirectory, "--outputBamFileName", outputBamFileName, "--uidLength", "14", "--outputReports" };
		PrefuppCli.runCommandLineApp(args);

		File outputBam = new File(outputDirectory, outputBamFileName);
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
