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
import java.util.HashSet;
import java.util.Set;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
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
import com.roche.mapping.datasimulator.FastQDataSimulator;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;

@Listeners(NgTestListener.class)
public class StressTest {

	Logger logger = LoggerFactory.getLogger(StressTest.class);

	private String mappingOutputBamFileName = "mapping_output.bam";
	private String fastqOneFileName = "large_one.fastq";
	private String fastqTwoFileName = "large_two.fastq";
	private String probeInfoFileName = "probes.txt";
	private String outputDirectoryPath;

	private File fastqOneFile;
	private File fastqTwoFile;
	private File probesFile;

	private final static int NUMBER_OF_PROBES = 5;
	private final static int UIDS_PER_PROBE = 5;
	private final static int READS_PER_UID_PROBE_PAIR = 1;
	private final static int UID_LENGTH = 10;
	// private final static String MUTATE_STRING = "M40D10R5^CCCAAATTTGGGM110";
	private final static String MUTATE_STRING = "";

	private final static int NUMBER_OF_SAM_ENTRIES_PER_UID_READ_PAIR = 4;

	@BeforeClass(groups = { "stress" })
	public void setup() {
		System.out.println("trying to create a temp directory at " + System.getProperty("java.io.tmpdir"));
		File outputDirectory = Files.createTempDir();
		outputDirectoryPath = outputDirectory.getAbsolutePath();
		System.out.println("outputDirectory is [" + outputDirectoryPath + "].");
		FastQDataSimulator.createSimulatedIlluminaReads(outputDirectory, fastqOneFileName, fastqTwoFileName, probeInfoFileName, UID_LENGTH, NUMBER_OF_PROBES, READS_PER_UID_PROBE_PAIR, UIDS_PER_PROBE,
				160, MUTATE_STRING, true, true);
		fastqOneFile = new File(outputDirectory, fastqOneFileName);
		fastqTwoFile = new File(outputDirectory, fastqTwoFileName);
		probesFile = new File(outputDirectory, probeInfoFileName);
	}

	@AfterClass(groups = { "stress" })
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

	@Test(groups = { "stress" })
	public void largeMapRunTest() {
		String[] args = new String[] { "--fastQOne", fastqOneFile.getAbsolutePath(), "--fastQTwo", fastqTwoFile.getAbsolutePath(), "--probe", probesFile.getAbsolutePath(), "--outputDir",
				outputDirectoryPath, "--outputBamFileName", mappingOutputBamFileName, "--uidLength", "" + UID_LENGTH, "--outputReports" };

		PrefuppCli.runCommandLineApp(args);
		File outputBam = new File(outputDirectoryPath, mappingOutputBamFileName);
		int count = 0;
		Set<Integer> flags = new HashSet<Integer>();
		try (final SAMFileReader samReader = new SAMFileReader(outputBam)) {
			SAMRecordIterator samRecordIter = samReader.iterator();

			while (samRecordIter.hasNext()) {
				SAMRecord record = samRecordIter.next();
				flags.add(record.getFlags());
				count++;
			}
			samRecordIter.close();
		}
		// - strand pair
		Assert.assertTrue(flags.contains(83));
		Assert.assertTrue(flags.contains(163));
		// + strand pair
		Assert.assertTrue(flags.contains(99));
		Assert.assertTrue(flags.contains(147));
		Assert.assertEquals(count, NUMBER_OF_PROBES * UIDS_PER_PROBE * NUMBER_OF_SAM_ENTRIES_PER_UID_READ_PAIR);
	}

	// @Test(groups = { "stress" })
	public void largeRunTest() throws InterruptedException {
		// make sure that this does not run in the same second as the map run test because the program records with timestamp will overlap
		Thread.sleep(1000);

		String outputBamFileName = "output.bam";

		String[] args = new String[] { "--fastQOne", fastqOneFile.getAbsolutePath(), "--fastQTwo", fastqTwoFile.getAbsolutePath(), "--probe", probesFile.getAbsolutePath(), "--bam",
				new File(outputDirectoryPath, mappingOutputBamFileName).getAbsolutePath(), "--outputDir", outputDirectoryPath, "--outputBamFileName", outputBamFileName, "--uidLength",
				"" + UID_LENGTH, "--outputReports" };
		PrefuppCli.runCommandLineApp(args);

		File outputBam = new File(outputDirectoryPath, outputBamFileName);
		int count = 0;
		Set<Integer> flags = new HashSet<Integer>();
		try (final SAMFileReader samReader = new SAMFileReader(outputBam)) {

			SAMRecordIterator samRecordIter = samReader.iterator();
			while (samRecordIter.hasNext()) {
				SAMRecord record = samRecordIter.next();
				flags.add(record.getFlags());
				count++;
			}
			samRecordIter.close();
		}
		// - strand pair
		Assert.assertTrue(flags.contains(83));
		Assert.assertTrue(flags.contains(163));
		// + strand pair
		Assert.assertTrue(flags.contains(99));
		Assert.assertTrue(flags.contains(147));
		Assert.assertEquals(count, NUMBER_OF_PROBES * UIDS_PER_PROBE * NUMBER_OF_SAM_ENTRIES_PER_UID_READ_PAIR);
	}

	public static void main(String args[]) {
		String sequence = "NNTCNNGGGCNTTNCCNGCCCCTTCNNTCNGGNGTNGTTTNTNNGNGTCGTAGTGCGCNNGATNGAGGGCCCCCCCATTNNANCCAGCTANGNCTCANCTTANNCTTAGCCNANCGANACGTNCGTTCTTANCAAGGNNTCNNTNGGGGGACTNNANGTT";
		ISequence seq = new IupacNucleotideCodeSequence(sequence);
		String reverseCompliment = seq.getReverseCompliment().toString();
		System.out.println(reverseCompliment);
	}

}
