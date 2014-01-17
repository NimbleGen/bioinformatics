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
import java.util.HashMap;
import java.util.Map;

import net.sf.picard.fastq.FastqReader;
import net.sf.picard.fastq.FastqRecord;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.IlluminaFastQHeader;

/**
 * Merges alignment information from a BAM file with read string, quality string, and UID from two input fastQ files, joining on the read name. Stores the result in a 'merged' BAM file.
 */
public class FastqAndBamFileMerger {

	private static Logger logger = LoggerFactory.getLogger(FastqAndBamFileMerger.class);

	/**
	 * This class just has one public static method - createMergedFastqAndBamFileFromUnsortedFiles
	 */
	private FastqAndBamFileMerger() {
		throw new AssertionError();
	}

	/**
	 * Stores just the information we need from the fastQ file in an in-memory hash.
	 */
	private static class SimpleFastqRecord {
		private final String seqLine;
		private final String qualLine;

		public SimpleFastqRecord(final String seqLine, final String qualLine) {
			this.seqLine = seqLine;
			this.qualLine = qualLine;
		}

		public String getReadString() {
			return seqLine;
		}

		public String getBaseQualityString() {
			return qualLine;
		}
	}

	/**
	 * Join all records in an in-memory read name->SimpleFastqRecord hash with all records in the bam file, outputs the joined records
	 * 
	 * @param nameToFastQRecord
	 * @param samReader
	 * @param processFirstOfPairReads
	 * @param samWriter
	 */
	private static void mergeAndOutputRecords(Map<String, SimpleFastqRecord> nameToFastQRecord, SAMFileReader samReader, boolean processFirstOfPairReads, SAMFileWriter samWriter, int uidLength) {

		SAMRecordIterator samIter = samReader.iterator();

		// Scan through the entire bam, finding matches by name to the fastq data in our hash
		while (samIter.hasNext()) {
			SAMRecord samRecord = samIter.next();

			// Only process the bam records that match the fastQ file we're working with
			if ((processFirstOfPairReads && samRecord.getFirstOfPairFlag()) || (!processFirstOfPairReads && samRecord.getSecondOfPairFlag())) {
				SimpleFastqRecord simpleFastqRecord = nameToFastQRecord.get(samRecord.getReadName());
				if (simpleFastqRecord != null) {
					String readString = simpleFastqRecord.getReadString();
					String baseQualityString = simpleFastqRecord.getBaseQualityString();
					String uid = null;
					if (processFirstOfPairReads) {
						uid = SAMRecordUtil.parseUidFromRead(readString, uidLength);
						readString = SAMRecordUtil.removeUidFromRead(readString, uidLength);
						baseQualityString = SAMRecordUtil.removeUidFromRead(baseQualityString, uidLength);
					}
					SAMRecord modifiedRecord = storeFastqInfoInRecord(samRecord, readString, baseQualityString, uid);
					samWriter.addAlignment(modifiedRecord);
				}
			}
		}

		samIter.close();
	}

	/**
	 * An alternate approach that hashes the fastq file data instead of the bam file data
	 * 
	 * @param samReader
	 * @param unsortedFastQFile
	 * @param processFirstOfPairReads
	 * @param samWriter
	 */
	private static void createMergedFastqAndBamFileFromUnsortedFiles(SAMFileReader samReader, File unsortedFastQFile, boolean processFirstOfPairReads, SAMFileWriter samWriter, int maximumHashSize,
			int uidLength) {

		int bamFilePassesCount = 0;

		Map<String, SimpleFastqRecord> nameToFastQRecord = new HashMap<String, SimpleFastqRecord>(maximumHashSize);
		try (FastqReader fastQReader = new FastqReader(unsortedFastQFile)) {
			while (fastQReader.hasNext()) {
				FastqRecord fastQRecord = fastQReader.next();

				nameToFastQRecord.put(IlluminaFastQHeader.getBaseHeader(fastQRecord.getReadHeader()), new SimpleFastqRecord(fastQRecord.getReadString(), fastQRecord.getBaseQualityString()));

				if (nameToFastQRecord.size() > maximumHashSize) {

					mergeAndOutputRecords(nameToFastQRecord, samReader, processFirstOfPairReads, samWriter, uidLength);
					bamFilePassesCount++;

					// Done processing that chunk of the fastq file.
					nameToFastQRecord.clear();
				}
			}

			// Process the last chunk of the fastq file
			if (nameToFastQRecord.size() > 0) {
				mergeAndOutputRecords(nameToFastQRecord, samReader, processFirstOfPairReads, samWriter, uidLength);
				bamFilePassesCount++;
			}

			// Keep track of how many passes through the bam file that took
			if (!processFirstOfPairReads) {
				logger.debug("Joined fastQ and bam file in " + bamFilePassesCount + " passes");
			}
		}
	}

	/**
	 * Decide how many records can fit in the hash before we run out of memory.
	 * 
	 * @param unsortedBamFile
	 * @param unsortedFastq1File
	 * @param hashFastQFile
	 * @return
	 */
	private static int getMaximumHashSizeForMerge(File unsortedBamFile, File unsortedFastQFile) {
		int maximumHashSize = 7000000;

		long initialFreeMemory = Runtime.getRuntime().freeMemory();

		// We'll put the records in one of these hash maps
		Map<String, SimpleFastqRecord> nameToFastQRecord = new HashMap<String, SimpleFastqRecord>();

		int recordCount = 0;

		// We're building the hash on the fastQ files, decide how many fastQ records will fit in memory while still leaving
		// enough memory to iterate over the BAM file
		try (FastqReader fastQReader = new FastqReader(unsortedFastQFile)) {

			while (fastQReader.hasNext() && recordCount < 1000) {
				recordCount++;
				FastqRecord fastQRecord = fastQReader.next();
				nameToFastQRecord.put(IlluminaFastQHeader.getBaseHeader(fastQRecord.getReadHeader()), new SimpleFastqRecord(fastQRecord.getReadString(), fastQRecord.getBaseQualityString()));
			}
		}

		if (recordCount > 0) {
			long currentFreeMemory = Runtime.getRuntime().freeMemory();
			double memoryAllocatedPerRecord = (initialFreeMemory - currentFreeMemory) / recordCount;

			// Allow half the initial free memory for this hash table, setting the minimum value to 2000000
			if (memoryAllocatedPerRecord > 0) {
				maximumHashSize = Math.max(maximumHashSize, (int) ((initialFreeMemory / 2) / memoryAllocatedPerRecord));
			}
			logger.debug("Memory allocated per record[" + memoryAllocatedPerRecord + "] maximumHashSize[" + maximumHashSize + "]");
		}

		return maximumHashSize;
	}

	/**
	 * Working from an unsorted bam file and unsorted paired fastQ files, output a merged, coordinate sorted bam file.
	 * 
	 * This is essentially a classic hash join: http://en.wikipedia.org/wiki/Hash_join#Classic_hash_join
	 * 
	 * If performance becomes an issue here we can first partition to hash buckets on disk (a Grace hash join: http://en.wikipedia.org/wiki/Hash_join#Grace_hash_join)
	 * 
	 * @param unsortedBamFile
	 * @param unsortedFastq1File
	 * @param unsortedFastq2File
	 * @param outputBamFile
	 * @return
	 */
	static File createMergedFastqAndBamFileFromUnsortedFiles(File unsortedBamFile, File unsortedFastq1File, File unsortedFastq2File, File outputBamFile, int uidLength) {
		// Each new iteration starts at the first record.
		try (SAMFileReader samReader = new SAMFileReader(unsortedBamFile)) {

			SAMFileHeader header = samReader.getFileHeader();
			header.setSortOrder(SAMFileHeader.SortOrder.coordinate);

			// Make a sorted, bam file writer with the fastest level of compression
			SAMFileWriter samWriter = new SAMFileWriterFactory().makeBAMWriter(samReader.getFileHeader(), false, outputBamFile, 0);

			// Build the in-memory hash from the fastQ files.
			int maximumHashSize = getMaximumHashSizeForMerge(unsortedBamFile, unsortedFastq1File);

			// Process the first of pair reads and the first fastQ file.
			createMergedFastqAndBamFileFromUnsortedFiles(samReader, unsortedFastq1File, true, samWriter, maximumHashSize, uidLength);

			// Process the second of pair reads and the second fastQ file.
			createMergedFastqAndBamFileFromUnsortedFiles(samReader, unsortedFastq2File, false, samWriter, maximumHashSize, uidLength);

			samWriter.close();
		}

		return outputBamFile;
	}

	/**
	 * Stores the read string, quality string, and UID in a merged SAM record
	 * 
	 * @param record
	 * @param readSequenceFromFastq
	 * @param readBaseQualityFromFastq
	 * @param uid
	 * @return
	 */
	private static SAMRecord storeFastqInfoInRecord(SAMRecord record, String readSequenceFromFastq, String readBaseQualityFromFastq, String uid) {
		if (uid != null) {
			SAMRecordUtil.setSamRecordUidAttribute(record, uid);
		}
		record.setReadString(readSequenceFromFastq);
		record.setBaseQualityString(readBaseQualityFromFastq);
		return record;
	}

	/**
	 * @param record
	 * @return The read quality string from a merged SAM record
	 */
	public static String getReadBaseQualityFromMergedRecord(SAMRecord record) {
		return record.getBaseQualityString();
	}

	/**
	 * @param record
	 * @return The read string from a merged SAM record
	 */
	public static String getReadSequenceFromMergedRecord(SAMRecord record) {
		return record.getReadString();
	}
}
