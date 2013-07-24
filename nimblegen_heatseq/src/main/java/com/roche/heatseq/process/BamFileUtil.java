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

import net.sf.picard.io.IoUtil;
import net.sf.picard.sam.ValidateSamFile;
import net.sf.samtools.BAMIndexer;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

public class BamFileUtil {

	public final static short PHRED_ASCII_OFFSET = 33;

	private BamFileUtil() {
		throw new AssertionError();
	}

	/**
	 * @param baseQualityString
	 * @return the cumulative quality score for the given baseQualityString
	 */
	public static short getQualityScore(String baseQualityString) {
		short sumOfQualityScores = 0;

		for (int i = 0; i < baseQualityString.length(); i++) {
			short nonAdjustedBaseScore = (short) baseQualityString.charAt(i);
			short baseScore = (short) (nonAdjustedBaseScore - PHRED_ASCII_OFFSET);

			sumOfQualityScores += baseScore;
		}

		return sumOfQualityScores;
	}

	static void createIndexOnCoordinateSortedBamFile(SAMFileReader samReader, File outputIndexFile) {
		createIndex(samReader, outputIndexFile);
	}

	/**
	 * Generates a BAM index file from an input BAM file
	 * 
	 * @param reader
	 *            SAMFileReader for input BAM file
	 * @param output
	 *            File for output index file
	 */
	public static void createIndex(SAMFileReader reader, File output) {

		BAMIndexer indexer = new BAMIndexer(output, reader.getFileHeader());

		reader.enableFileSource(true);

		// create and write the content
		for (SAMRecord rec : reader) {
			indexer.processAlignment(rec);
		}
		indexer.finish();

	}

	/**
	 * Sort the provided input file by readname and place the result in output
	 * 
	 * @param input
	 * @param output
	 * @return output
	 */
	public static File sortOnReadName(File input, File output) {
		return picardSort(input, output, SortOrder.queryname);
	}

	/**
	 * Sort the provided input file by coordinates and place the result in output
	 * 
	 * @param input
	 * @param output
	 * @return output
	 */
	static File sortOnCoordinates(File input, File output) {
		return picardSort(input, output, SortOrder.coordinate);
	}

	/**
	 * Use picard to sort the provided input file
	 * 
	 * @param input
	 * @param output
	 * @param sortOrder
	 * @return
	 */
	private static File picardSort(File input, File output, SortOrder sortOrder) {
		IoUtil.assertFileIsReadable(input);
		IoUtil.assertFileIsWritable(output);

		final SAMFileReader reader = new SAMFileReader(IoUtil.openFileForReading(input));

		SAMFileHeader header = reader.getFileHeader();
		header.setSortOrder(sortOrder);

		final SAMFileWriter writer = new SAMFileWriterFactory().makeSAMOrBAMWriter(header, false, output);

		for (final SAMRecord record : reader) {
			writer.addAlignment(record);
		}
		writer.close();
		reader.close();

		return output;
	}

	/**
	 * Use picards validateSamFile to validate the provided inputBamFile
	 * 
	 * @param inputBamFile
	 * @param outputErrorFile
	 */
	public static void validateSamFile(File inputBamFile, File outputErrorFile) {
		ValidateSamFile.main(new String[] { "INPUT=" + inputBamFile.getAbsolutePath(), "OUTPUT=" + outputErrorFile.getAbsolutePath() });
	}

	public static void main(String[] args) {
		// File inputBamFile = new File("S:\\public\\candace\\dsHybrid.bam");
		File inputBamFile = new File("C:\\Users\\heilmank\\Desktop\\junk\\dsHybrid.srt_REDUCED.bam");
		File outputErrorFile = new File("C:\\Users\\heilmank\\Desktop\\junk\\validation_errors.txt");
		validateSamFile(inputBamFile, outputErrorFile);
	}

}
