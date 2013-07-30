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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sf.picard.fastq.FastqReader;
import net.sf.picard.fastq.FastqRecord;
import net.sf.picard.fastq.FastqWriter;
import net.sf.picard.fastq.FastqWriterFactory;
import net.sf.picard.io.IoUtil;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

import com.roche.mapping.SAMRecordUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;

public class BamFileInternalUtil {

	public static void splitBamFilesByMatches(File bamFileOne, File bamFileTwo, File outputDirectory) {
		IoUtil.assertFileIsReadable(bamFileOne);
		IoUtil.assertFileIsReadable(bamFileOne);

		File matchFile = new File(outputDirectory, "match.bam");
		File misMatchOneFile = new File(outputDirectory, "mismatch_one.bam");
		File misMatchTwoFile = new File(outputDirectory, "mismatch_two.bam");
		File onlyInOneFile = new File(outputDirectory, "only_in_one.bam");
		File onlyInTwoFile = new File(outputDirectory, "only_in_two.bam");

		try {
			FileUtil.createNewFile(matchFile);
			FileUtil.createNewFile(misMatchOneFile);
			FileUtil.createNewFile(misMatchTwoFile);
			FileUtil.createNewFile(onlyInOneFile);
			FileUtil.createNewFile(onlyInTwoFile);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		List<SAMRecord> bamFileOneRecords = new ArrayList<SAMRecord>();

		final SAMFileReader reader = new SAMFileReader(IoUtil.openFileForReading(bamFileOne));
		for (final SAMRecord record : reader) {
			bamFileOneRecords.add(record);
		}

		SAMRecordComparator comparator = new SAMRecordComparator();

		SAMFileHeader header1 = reader.getFileHeader();
		reader.close();
		Collections.sort(bamFileOneRecords, comparator);

		List<SAMRecord> bamFileTwoRecords = new ArrayList<SAMRecord>();
		final SAMFileReader reader2 = new SAMFileReader(IoUtil.openFileForReading(bamFileTwo));
		for (final SAMRecord record : reader2) {
			bamFileTwoRecords.add(record);
		}
		SAMFileHeader header2 = reader2.getFileHeader();
		reader2.close();
		Collections.sort(bamFileTwoRecords, comparator);

		header1.setSortOrder(SortOrder.queryname);
		header2.setSortOrder(SortOrder.queryname);

		final SAMFileWriter matchWriter = new SAMFileWriterFactory().makeSAMOrBAMWriter(header1, true, matchFile);
		final SAMFileWriter mismatchOneWriter = new SAMFileWriterFactory().makeSAMOrBAMWriter(header1, true, misMatchOneFile);
		final SAMFileWriter mismatchTwoWriter = new SAMFileWriterFactory().makeSAMOrBAMWriter(header2, true, misMatchTwoFile);
		final SAMFileWriter onlyInOneWriter = new SAMFileWriterFactory().makeSAMOrBAMWriter(header1, true, onlyInOneFile);
		final SAMFileWriter onlyInTwoWriter = new SAMFileWriterFactory().makeSAMOrBAMWriter(header2, true, onlyInTwoFile);

		int oneIndex = 0;
		int twoIndex = 0;

		while (oneIndex < bamFileOneRecords.size() && twoIndex < bamFileTwoRecords.size()) {
			SAMRecord one = bamFileOneRecords.get(oneIndex);
			SAMRecord two = bamFileTwoRecords.get(twoIndex);

			int compareResult = comparator.compare(one, two);

			if (compareResult == 0) {
				String errorDetails = getDoNotMatchDetails(one, two);
				boolean doDetailsMatch = errorDetails == null || errorDetails.isEmpty();
				if (doDetailsMatch) {
					matchWriter.addAlignment(one);
				} else {

					one.setAttribute("ER", errorDetails);
					mismatchOneWriter.addAlignment(one);
					two.setAttribute("ER", errorDetails);
					mismatchTwoWriter.addAlignment(two);
				}
				oneIndex++;
				twoIndex++;
			} else if (compareResult < 0) {
				onlyInOneWriter.addAlignment(one);
				oneIndex++;
			} else if (compareResult > 0) {
				onlyInTwoWriter.addAlignment(two);
				twoIndex++;
			}
		}

		matchWriter.close();
		mismatchOneWriter.close();
		mismatchTwoWriter.close();
		onlyInOneWriter.close();
		onlyInTwoWriter.close();
	}

	private static String getDoNotMatchDetails(SAMRecord one, SAMRecord two) {
		StringBuilder details = new StringBuilder();
		String mm1 = (String) one.getAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG);
		String mm2 = (String) two.getAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG);
		if (mm1 != null && mm1.charAt(0) == 0) {
			mm1 = mm1.substring(1);
		}
		if (mm1 != null && mm1.charAt(mm1.length() - 1) == 0) {
			mm1 = mm1.substring(0, mm1.length() - 1);
		}
		if (mm2 != null && mm2.charAt(0) == 0) {
			mm2 = mm2.substring(1);
		}
		if (mm2 != null && mm2.charAt(mm2.length() - 1) == 0) {
			mm2 = mm2.substring(0, mm2.length() - 1);
		}

		if (!one.getReadString().equals(two.getReadString())) {
			details.append(":Read Sequence:");
		}

		if (!one.getBaseQualityString().equals(two.getBaseQualityString())) {
			details.append(":Read Quality:");
		}

		if (!one.getCigarString().equals(two.getCigarString())) {
			details.append(":Cigar:");
		}

		if (!(one.getAlignmentStart() == two.getAlignmentStart())) {
			details.append(":Position:");
		}

		if (mm1 != null && mm2 != null && !mm1.equals(mm2)) {
			details.append(":Tag--MD:");
		}

		Integer oneEditDistance = (Integer) one.getAttribute(SAMRecordUtil.EDIT_DISTANCE_ATTRIBUTE_TAG);
		Integer twoEditDistance = (Integer) two.getAttribute(SAMRecordUtil.EDIT_DISTANCE_ATTRIBUTE_TAG);
		if (oneEditDistance != null && twoEditDistance != null && !(oneEditDistance.equals(twoEditDistance))) {
			details.append(":Tag--NM:");
		}

		if (!(one.getInferredInsertSize() == two.getInferredInsertSize())) {
			details.append(":Template Length:");
		}

		return details.toString();
	}

	private static class SAMRecordComparator implements Comparator<SAMRecord> {
		@Override
		public int compare(SAMRecord o1, SAMRecord o2) {
			int result = o1.getReadName().compareTo(o2.getReadName());
			if (result == 0) {
				result = Boolean.compare(o1.getMateNegativeStrandFlag(), o2.getMateNegativeStrandFlag());
			}
			return result;
		}
	}

	public static void createSubsetBamFile(final File inputSamOrBamFile, final File outputSamOrBamFile) {
		final SAMFileReader inputSam = new SAMFileReader(inputSamOrBamFile);
		final SAMFileWriter outputSam = new SAMFileWriterFactory().makeSAMOrBAMWriter(inputSam.getFileHeader(), true, outputSamOrBamFile);

		SAMRecordIterator iter = inputSam.queryOverlapping("chr1", 866376, 866568);

		while (iter.hasNext()) {
			final SAMRecord samRecord = iter.next();
			outputSam.addAlignment(samRecord);
		}
		outputSam.close();
		inputSam.close();
	}

	public static String[] getReadNames(File bamFile) {
		List<String> readNames = new ArrayList<String>();
		final SAMFileReader inputSam = new SAMFileReader(bamFile);
		// BamFileUtil.createIndex(inputSam, new File(inputSamOrBamFile.getAbsolutePath() + ".bai"));

		SAMRecordIterator iter = inputSam.iterator();

		while (iter.hasNext()) {
			final SAMRecord samRecord = iter.next();
			readNames.add(samRecord.getReadName());
		}
		inputSam.close();
		return readNames.toArray(new String[0]);
	}

	public static void filterFastqByReadNames(String[] readNames, File inputFastqOneFile, File inputFastqTwoFile, File outputFastqOneFile, File outputFastqTwoFile) {
		final FastqWriterFactory factory = new FastqWriterFactory();
		FastqWriter writerOne = factory.newWriter(outputFastqOneFile);
		FastqWriter writerTwo = factory.newWriter(outputFastqTwoFile);
		try (FastqReader fastQOneReader = new FastqReader(inputFastqOneFile)) {
			try (FastqReader fastQTwoReader = new FastqReader(inputFastqTwoFile)) {

				while (fastQOneReader.hasNext() && fastQTwoReader.hasNext()) {
					FastqRecord fastQOneRecord = fastQOneReader.next();
					FastqRecord fastQTwoRecord = fastQTwoReader.next();

					boolean match = false;
					readNameLoop: for (String readName : readNames) {
						String readHeaderOne = fastQOneRecord.getReadHeader();
						if (readHeaderOne.contains(readName)) {
							match = true;
							break readNameLoop;
						}
					}

					if (match) {
						writerOne.write(fastQOneRecord);
						writerTwo.write(fastQTwoRecord);
					}
				}
			}
		}
		writerOne.close();
		writerTwo.close();

	}

	public static void main(String[] args) {

		// File inputOne = new File("C:\\Users\\heilmank\\Desktop\\012813\\results.bam");
		// File inputTwo = new File("C:\\Users\\heilmank\\Desktop\\012813\\MIPALA.srt_REDUCED.bam");
		// sortOnReadName(inputTwo, outputTwo);
		// File inputOne = new File("C:\\Users\\heilmank\\Desktop\\junk\\dsHybrid.srt_REDUCED.bam");
		// File inputTwo = new File("C:\\Users\\heilmank\\Desktop\\junk\\dsHybrid.srt_REDUCED_strand.bam");
		// File outputDirectory = new File("C:\\Users\\heilmank\\Desktop\\junk");
		// splitBamFilesByMatches(inputOne, inputTwo, outputDirectory);

		// createSubsetBamFile(new File("D:/Todds_problem_set/UID-57.sorted.bam"), new File("D:/Todds_problem_set/reverse_test.bam"));
		String[] readNames = getReadNames(new File("D:/Todds_problem_set/reverse_test.bam"));
		filterFastqByReadNames(readNames, new File("D:/Todds_problem_set/UID-57_R1.fastq"), new File("D:/Todds_problem_set/UID-57_R2.fastq"), new File("D:/Todds_problem_set/reverse_test_one.fastq"),
				new File("D:/Todds_problem_set/reverse_test_two.fastq"));
		// BamFileUtil.sortOnReadName(new File("D:/Todds_problem_set/reverse_test.bam"), new File("D:/Todds_problem_set/reverse_test_sorted_by_readname.bam"));
	}

}
