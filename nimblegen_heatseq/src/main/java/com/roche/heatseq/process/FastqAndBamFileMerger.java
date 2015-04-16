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
import java.util.Iterator;
import java.util.Map;

import net.sf.picard.fastq.FastqRecord;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.util.CloseableIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.process.FastqReadTrimmer.ProbeTrimmingInformation;
import com.roche.heatseq.process.FastqReadTrimmer.TrimmedRead;
import com.roche.heatseq.utils.BamSorter;
import com.roche.heatseq.utils.FastqReader;
import com.roche.heatseq.utils.FastqSorter;
import com.roche.heatseq.utils.IlluminaFastQReadNameUtil;
import com.roche.heatseq.utils.SAMRecordUtil;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;

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
	private static int mergeAndOutputRecords(Map<String, SimpleFastqRecord> nameToFastQRecord, SAMFileReader samReader, boolean processFirstOfPairReads, SAMFileWriter samWriter,
			boolean trimmingSkipped, ProbeTrimmingInformation probeTrimmingInformation, String commonReadNameBeginning) {
		int numberOfRecordsWritten = 0;
		SAMRecordIterator samIter = samReader.iterator();

		// Scan through the entire bam, finding matches by name to the fastq data in our hash
		while (samIter.hasNext()) {
			SAMRecord samRecord = samIter.next();

			// Only process the bam records that match the fastQ file we're working with
			if ((processFirstOfPairReads && samRecord.getFirstOfPairFlag()) || (!processFirstOfPairReads && samRecord.getSecondOfPairFlag())) {
				String readNameFromBam = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(commonReadNameBeginning, samRecord.getReadName());
				SimpleFastqRecord simpleFastqRecord = nameToFastQRecord.get(readNameFromBam);
				if (simpleFastqRecord != null) {
					String readString = simpleFastqRecord.getReadString();
					String baseQualityString = simpleFastqRecord.getBaseQualityString();

					if (!isTrimAmountCorrect(samRecord, readString, baseQualityString, trimmingSkipped, probeTrimmingInformation)) {
						throw new UnableToMergeFastqAndBamFilesException();
					}

					SAMRecord modifiedRecord = storeFastqInfoInRecord(samRecord, readString, baseQualityString);
					samWriter.addAlignment(modifiedRecord);
					numberOfRecordsWritten++;
				}
			}
		}

		samIter.close();
		return numberOfRecordsWritten;
	}

	private static boolean isTrimAmountCorrect(SAMRecord record, String readStringFromFastq, String baseQualityStringFromFastq, boolean trimmingSkipped,
			ProbeTrimmingInformation probeTrimmingInformation) {
		boolean isTrimAmountCorrect = true;

		boolean isAReadOne = record.getFirstOfPairFlag();
		if (trimmingSkipped) {
			isTrimAmountCorrect = record.getReadString().equals(readStringFromFastq) && record.getBaseQualityString().equals(baseQualityStringFromFastq);
		} else {
			TrimmedRead trimmedRead = null;
			if (isAReadOne) {
				trimmedRead = FastqReadTrimmer.trim(readStringFromFastq, baseQualityStringFromFastq, probeTrimmingInformation.getReadOneTrimFromStart(), probeTrimmingInformation.getReadOneTrimStop(),
						probeTrimmingInformation.isPerformThreePrimeTrimming());
			} else {
				trimmedRead = FastqReadTrimmer.trim(readStringFromFastq, baseQualityStringFromFastq, probeTrimmingInformation.getReadTwoTrimFromStart(), probeTrimmingInformation.getReadTwoTrimStop(),
						probeTrimmingInformation.isPerformThreePrimeTrimming());
			}
			int expectedTrimmedSize = trimmedRead.getTrimmedReadString().length();
			int expectedTrimmedQualitySize = trimmedRead.getTrimmedReadQuality().length();
			int actualTrimmedSize = record.getReadString().length();
			int actualTrimmedQualitySize = record.getBaseQualityString().length();
			isTrimAmountCorrect = (actualTrimmedSize <= expectedTrimmedSize) && (actualTrimmedQualitySize <= expectedTrimmedQualitySize);
			if (!isTrimAmountCorrect) {
				logger.info("Readname:" + record.getReadName() + " is not trimmed appropriately.  Expected_Trimmed_Size[" + expectedTrimmedSize + "] Actual_Trimmed_Size[" + actualTrimmedSize
						+ "].  Expected_Quality_Trimmed_Size[" + expectedTrimmedQualitySize + "] Actual_Quality_Trimmed_Size[" + actualTrimmedQualitySize + "].");
			}
		}
		return isTrimAmountCorrect;
	}

	/**
	 * An alternate approach that hashes the fastq file data instead of the bam file data
	 * 
	 * @param samReader
	 * @param unsortedFastQFile
	 * @param processFirstOfPairReads
	 * @param samWriter
	 */
	private static int createMergedFastqAndBamFileFromUnsortedFiles(SAMFileReader samReader, File unsortedFastQFile, boolean processFirstOfPairReads, SAMFileWriter samWriter, int maximumHashSize,
			boolean trimmingSkipped, ProbeTrimmingInformation probeTrimmingInformation, Map<String, Probe> readsToProbeAssignments, String commonReadNameBeginning) {

		int bamFilePassesCount = 0;

		int totalRecordsWritten = 0;

		Map<String, SimpleFastqRecord> nameToFastQRecord = new HashMap<String, SimpleFastqRecord>(maximumHashSize);
		try (FastqReader fastQReader = new FastqReader(unsortedFastQFile)) {
			while (fastQReader.hasNext()) {
				FastqRecord fastQRecord = fastQReader.next();

				String readName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(commonReadNameBeginning, fastQRecord.getReadHeader());

				if (readsToProbeAssignments.containsKey(readName)) {
					nameToFastQRecord.put(readName, new SimpleFastqRecord(fastQRecord.getReadString(), fastQRecord.getBaseQualityString()));
				}

				if (nameToFastQRecord.size() > maximumHashSize) {

					totalRecordsWritten += mergeAndOutputRecords(nameToFastQRecord, samReader, processFirstOfPairReads, samWriter, trimmingSkipped, probeTrimmingInformation, commonReadNameBeginning);
					bamFilePassesCount++;

					// Done processing that chunk of the fastq file.
					nameToFastQRecord.clear();
				}
			}

			// Process the last chunk of the fastq file
			if (nameToFastQRecord.size() > 0) {
				totalRecordsWritten += mergeAndOutputRecords(nameToFastQRecord, samReader, processFirstOfPairReads, samWriter, trimmingSkipped, probeTrimmingInformation, commonReadNameBeginning);
				bamFilePassesCount++;
			}

			// Keep track of how many passes through the bam file that took
			if (!processFirstOfPairReads) {
				logger.debug("Joined fastQ and bam file in " + bamFilePassesCount + " passes");
			}
		}
		return totalRecordsWritten;
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
	public static File createMergedFastqAndBamFileFromUnsortedFilesOld(File unsortedBamFile, File unsortedBamFileIndex, File unsortedFastq1File, File unsortedFastq2File, File outputBamFile,
			boolean trimmingSkipped, ProbeTrimmingInformation probeTrimmingInformation, File tempDirectory, Map<String, Probe> readsToProbeAssignments, String commonReadNameBeginning) {
		// Each new iteration starts at the first record.
		SAMFileReader samReader = null;

		try {
			if (unsortedBamFileIndex == null) {
				// this is more than likely a SAM file which cannot be indexed
				samReader = new SAMFileReader(unsortedBamFile);
			} else {
				samReader = new SAMFileReader(unsortedBamFile, unsortedBamFileIndex);
			}

			SAMFileHeader header = samReader.getFileHeader();
			header.setSortOrder(SAMFileHeader.SortOrder.coordinate);

			// Make a sorted, bam file writer with the fastest level of compression
			SAMFileWriter samWriter = new SAMFileWriterFactory().setTempDirectory(tempDirectory).makeBAMWriter(samReader.getFileHeader(), false, outputBamFile, 0);

			// Build the in-memory hash from the fastQ files.
			int maximumHashSize = 700000;// getMaximumHashSizeForMerge(unsortedBamFile, unsortedFastq1File);

			// Process the first of pair reads and the first fastQ file.
			int totalRecordsWrittenInFastq1 = createMergedFastqAndBamFileFromUnsortedFiles(samReader, unsortedFastq1File, true, samWriter, maximumHashSize, trimmingSkipped, probeTrimmingInformation,
					readsToProbeAssignments, commonReadNameBeginning);

			if (totalRecordsWrittenInFastq1 == 0) {
				throw new IllegalStateException("The read names in the input Fastq one file does not match the reads names in the provided bam/sam file.");
			}

			// since the samReader.iterator for a samReader initialized using a sam file behaves differently than for a samReader initialized using a bam file (the former starts where the last
			// iterator ended)
			// we need to reinitialize the sam reader (this wouldn't be necessary for the bam file but since
			// I'm not positive that sam files can't have indexes--which is what I'm using for an indicator of the sam file--I will reinitialize for both cases.
			samReader.close();
			if (unsortedBamFileIndex == null) {
				// this is more than likely a SAM file which cannot be indexed
				samReader = new SAMFileReader(unsortedBamFile);
			} else {
				samReader = new SAMFileReader(unsortedBamFile, unsortedBamFileIndex);
			}

			// Process the second of pair reads and the second fastQ file.
			int totalRecordsWrittenInFastq2 = createMergedFastqAndBamFileFromUnsortedFiles(samReader, unsortedFastq2File, false, samWriter, maximumHashSize, trimmingSkipped, probeTrimmingInformation,
					readsToProbeAssignments, commonReadNameBeginning);
			if (totalRecordsWrittenInFastq2 == 0) {
				throw new IllegalStateException("The read names in the input Fastq two file does not match the reads names in the provided bam/sam file.");
			}

			samWriter.close();
		} finally {
			if (samReader != null) {
				samReader.close();
			}
		}

		return outputBamFile;
	}

	public static File createMergedFastqAndBamFileFromUnsortedFiles(File unsortedBamFile, File unsortedBamFileIndex, File unsortedFastq1File, File unsortedFastq2File, File outputBamFile,
			boolean trimmingSkipped, ProbeTrimmingInformation probeTrimmingInformation, File tempDirectory, Map<String, Probe> readsToProbeAssignments, String commonReadNameBeginning) {
		return createMergedFastqAndBamFileFromUnsortedFilesNew(unsortedBamFile, unsortedBamFileIndex, unsortedFastq1File, unsortedFastq2File, outputBamFile, trimmingSkipped, probeTrimmingInformation,
				tempDirectory, readsToProbeAssignments, commonReadNameBeginning);
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
	public static File createMergedFastqAndBamFileFromUnsortedFilesNew(File unsortedBamFile, File unsortedBamFileIndex, File unsortedFastq1File, File unsortedFastq2File, File outputBamFile,
			boolean trimmingSkipped, ProbeTrimmingInformation probeTrimmingInformation, File tempDirectory, Map<String, Probe> readsToProbeAssignments, String commonReadNameBeginning) {
		// Each new iteration starts at the first record.

		long bamSortStart = System.currentTimeMillis();
		CloseableIterator<SAMRecord> samIter = BamSorter.getSortedBamIterator(unsortedBamFile, tempDirectory, new BamSorter.SamRecordNameComparator());
		long bamSortStop = System.currentTimeMillis();
		logger.info("time to sort bam:" + DateUtil.convertMillisecondsToHHMMSS(bamSortStop - bamSortStart));

		long fastqOneSortStart = System.currentTimeMillis();
		CloseableIterator<FastqRecord> fastq1Iter = FastqSorter.getSortedFastqIterator(unsortedFastq1File, tempDirectory, new FastqSorter.FastqRecordNameComparator());
		long fastqOneSortStop = System.currentTimeMillis();
		logger.info("time to sort fastq1:" + DateUtil.convertMillisecondsToHHMMSS(fastqOneSortStop - fastqOneSortStart));

		long fastqTwoSortStart = System.currentTimeMillis();
		CloseableIterator<FastqRecord> fastq2Iter = FastqSorter.getSortedFastqIterator(unsortedFastq2File, tempDirectory, new FastqSorter.FastqRecordNameComparator());
		long fastqTwoSortStop = System.currentTimeMillis();
		logger.info("time to sort fastq2:" + DateUtil.convertMillisecondsToHHMMSS(fastqTwoSortStop - fastqTwoSortStart));
		long mergeStart = System.currentTimeMillis();

		SAMFileHeader header = null;
		try (SAMFileReader samReader = new SAMFileReader(unsortedBamFile)) {
			header = samReader.getFileHeader();
		}
		header.setSortOrder(SortOrder.coordinate);

		// Make a sorted, bam file writer with the fastest level of compression
		SAMFileWriter samWriter = new SAMFileWriterFactory().setTempDirectory(tempDirectory).makeBAMWriter(header, false, outputBamFile, 0);

		MergedSamIterator mergedSamIterator = new MergedSamIterator(samIter, fastq1Iter, fastq2Iter, trimmingSkipped, probeTrimmingInformation, commonReadNameBeginning, readsToProbeAssignments);
		// CloseableIterator<SAMRecord> sortedMergedSamIter = BamSorter.getSortedBamIterator(mergedSamIterator, header, tempDirectory, SortOrder.coordinate.getComparatorInstance());
		while (mergedSamIterator.hasNext()) {
			samWriter.addAlignment(mergedSamIterator.next());
		}

		long mergeStop = System.currentTimeMillis();

		logger.info("Done merging[" + outputBamFile.getAbsolutePath() + "]:" + DateUtil.convertMillisecondsToHHMMSS(mergeStop - mergeStart));

		if (mergedSamIterator.getTotalMatches() == 0) {
			throw new IllegalStateException("The read names in the input Fastq files do not match the reads names in the provided bam/sam file.");
		}

		fastq1Iter.close();
		fastq2Iter.close();
		samIter.close();
		samWriter.close();

		return outputBamFile;
	}

	private static class MergedSamIterator implements Iterator<SAMRecord> {

		private final Iterator<SAMRecord> samIter;
		private final Iterator<FastqRecord> fastq1Iter;
		private final Iterator<FastqRecord> fastq2Iter;
		private final boolean trimmingSkipped;
		private final ProbeTrimmingInformation probeTrimmingInformation;
		private final Map<String, Probe> readsToProbeAssignments;
		private SAMRecord nextRecord;
		private SAMRecord samRecord;
		private FastqRecord fastqOneRecord;
		private FastqRecord fastqTwoRecord;
		private final String commonReadNameBeginning;
		private int totalMatches;

		public MergedSamIterator(Iterator<SAMRecord> samIter, Iterator<FastqRecord> fastq1Iter, Iterator<FastqRecord> fastq2Iter, boolean trimmingSkipped,
				ProbeTrimmingInformation probeTrimmingInformation, String commonReadNameBeginning, Map<String, Probe> readsToProbeAssignments) {
			super();
			this.samIter = samIter;
			this.fastq1Iter = fastq1Iter;
			this.fastq2Iter = fastq2Iter;
			this.trimmingSkipped = trimmingSkipped;
			this.probeTrimmingInformation = probeTrimmingInformation;
			this.commonReadNameBeginning = commonReadNameBeginning;
			this.totalMatches = 0;
			this.readsToProbeAssignments = readsToProbeAssignments;
			nextRecord = getNextRecordToReturn();
		}

		private SAMRecord getNextRecordToReturn() {
			SAMRecord nextRecordToReturn = null;
			while (samIter.hasNext() && fastq1Iter.hasNext() && fastq2Iter.hasNext() && nextRecordToReturn == null) {
				if (samRecord == null) {
					samRecord = samIter.next();
				}

				if (fastqOneRecord == null) {
					fastqOneRecord = fastq1Iter.next();
				}

				if (fastqTwoRecord == null) {
					fastqTwoRecord = fastq2Iter.next();
				}

				String samName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(commonReadNameBeginning, samRecord.getReadName());
				String fastqOneName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(commonReadNameBeginning, fastqOneRecord.getReadHeader());
				String fastqTwoName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(commonReadNameBeginning, fastqTwoRecord.getReadHeader());

				// make sure the fastq files have the same read header
				int fastqComp = fastqOneName.compareTo(fastqTwoName);
				if (fastqComp < 0) {
					fastqOneRecord = null;
				} else if (fastqComp > 0) {
					fastqTwoRecord = null;
				} else if (fastqComp == 0) {
					int samToFastqComparison = samName.compareTo(fastqOneName);
					if (samToFastqComparison < 0) {
						samRecord = null;
					} else if (samToFastqComparison > 0) {
						fastqOneRecord = null;
						fastqTwoRecord = null;
					} else if (samToFastqComparison == 0) {
						totalMatches++;
						if (readsToProbeAssignments.containsKey(samName)) {
							if (samRecord.getFirstOfPairFlag()) {
								nextRecordToReturn = checkAndStoreFastqInfoInRecord(samRecord, fastqOneRecord.getReadString(), fastqOneRecord.getBaseQualityString(), trimmingSkipped,
										probeTrimmingInformation);
							} else {
								nextRecordToReturn = checkAndStoreFastqInfoInRecord(samRecord, fastqTwoRecord.getReadString(), fastqTwoRecord.getBaseQualityString(), trimmingSkipped,
										probeTrimmingInformation);
							}
						}
						samRecord = null;
					}
				}
			}
			return nextRecordToReturn;
		}

		@Override
		public boolean hasNext() {
			return nextRecord != null;
		}

		@Override
		public SAMRecord next() {
			SAMRecord nextRecordToReturn = nextRecord;
			nextRecord = getNextRecordToReturn();
			return nextRecordToReturn;
		}

		public int getTotalMatches() {
			return totalMatches;
		}

		@Override
		public void remove() {
			throw new IllegalStateException("Method not implemented.");

		}
	}

	private static SAMRecord checkAndStoreFastqInfoInRecord(SAMRecord samRecord, String readSequenceFromFastq, String readBaseQualityFromFastq, boolean trimmingSkipped,
			ProbeTrimmingInformation probeTrimmingInformation) {
		if (!isTrimAmountCorrect(samRecord, readSequenceFromFastq, readBaseQualityFromFastq, trimmingSkipped, probeTrimmingInformation)) {
			throw new UnableToMergeFastqAndBamFilesException();
		}
		return storeFastqInfoInRecord(samRecord, readSequenceFromFastq, readBaseQualityFromFastq);
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
	private static SAMRecord storeFastqInfoInRecord(SAMRecord record, String readSequenceFromFastq, String readBaseQualityFromFastq) {
		int mappedReadLength = record.getReadLength();
		SAMRecordUtil.setMappedReadLength(record, mappedReadLength);
		record.setReadString(readSequenceFromFastq);
		record.setBaseQualityString(readBaseQualityFromFastq);
		return record;
	}
}
