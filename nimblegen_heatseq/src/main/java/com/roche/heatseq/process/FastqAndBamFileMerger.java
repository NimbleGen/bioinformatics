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

package com.roche.heatseq.process;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.process.FastqReadTrimmer.ProbeTrimmingInformation;
import com.roche.heatseq.process.FastqReadTrimmer.TrimmedRead;
import com.roche.heatseq.utils.BamSorter;
import com.roche.heatseq.utils.BamSorter.CloseableAndIterableIterator;
import com.roche.heatseq.utils.FastqSorter;
import com.roche.heatseq.utils.SAMRecordUtil;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.IlluminaFastQReadNameUtil;
import com.roche.sequencing.bioinformatics.common.utils.fastq.PicardException;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.Probe;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.CloseableIterator;

/**
 * Merges alignment information from a BAM file with read string, quality string, and UID from two input fastQ files, joining on the read name. Stores the result in a 'merged' BAM file.
 */
class FastqAndBamFileMerger {

	private static Logger logger = LoggerFactory.getLogger(FastqAndBamFileMerger.class);

	/**
	 * This class just has one public static method - createMergedFastqAndBamFileFromUnsortedFiles
	 */
	private FastqAndBamFileMerger() {
		throw new AssertionError();
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

	public static File createMergedFastqAndBamFileFromUnsortedFiles(File unsortedBamFile, File unsortedBamFileIndex, File unsortedFastq1File, File unsortedFastq2File, File outputBamFile,
			boolean trimmingSkipped, ProbeTrimmingInformation probeTrimmingInformation, File tempDirectory, Map<String, Set<Probe>> readsToProbeAssignments, String commonReadNameBeginning) {
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
	private static File createMergedFastqAndBamFileFromUnsortedFilesNew(File unsortedBamFile, File unsortedBamFileIndex, File unsortedFastq1File, File unsortedFastq2File, File outputBamFile,
			boolean trimmingSkipped, ProbeTrimmingInformation probeTrimmingInformation, File tempDirectory, Map<String, Set<Probe>> readsToProbeAssignments, String commonReadNameBeginning) {
		// Each new iteration starts at the first record.

		long bamSortStart = System.currentTimeMillis();
		try (CloseableAndIterableIterator<SAMRecord> samIter = BamSorter.getSortedBamIterator(unsortedBamFile, tempDirectory, new BamSorter.SamRecordNameComparator())) {

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
			try (SamReader samReader = SamReaderFactory.makeDefault().open(unsortedBamFile)) {
				header = samReader.getFileHeader();
			} catch (IOException e) {
				throw new PicardException(e.getMessage(), e);
			}
			header.setSortOrder(SortOrder.coordinate);

			// Make a sorted, bam file writer with the fastest level of compression
			SAMFileWriter samWriter = new SAMFileWriterFactory().setMaxRecordsInRam(PrimerReadExtensionAndPcrDuplicateIdentification.DEFAULT_MAX_RECORDS_IN_RAM).setTempDirectory(tempDirectory)
					.makeBAMWriter(header, false, outputBamFile, 0);

			MergedSamIterator mergedSamIterator = new MergedSamIterator(samIter, fastq1Iter, fastq2Iter, trimmingSkipped, probeTrimmingInformation, commonReadNameBeginning);
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
			samWriter.close();
		}

		return outputBamFile;
	}

	private static class MergedSamIterator implements Iterator<SAMRecord> {

		private final Iterator<SAMRecord> samIter;
		private final Iterator<FastqRecord> fastq1Iter;
		private final Iterator<FastqRecord> fastq2Iter;
		private final boolean trimmingSkipped;
		private final ProbeTrimmingInformation probeTrimmingInformation;
		private SAMRecord nextRecord;
		private SAMRecord samRecord;
		private FastqRecord fastqOneRecord;
		private FastqRecord fastqTwoRecord;
		private final String commonReadNameBeginning;
		private int totalMatches;

		public MergedSamIterator(Iterator<SAMRecord> samIter, Iterator<FastqRecord> fastq1Iter, Iterator<FastqRecord> fastq2Iter, boolean trimmingSkipped,
				ProbeTrimmingInformation probeTrimmingInformation, String commonReadNameBeginning) {
			super();
			this.samIter = samIter;
			this.fastq1Iter = fastq1Iter;
			this.fastq2Iter = fastq2Iter;
			this.trimmingSkipped = trimmingSkipped;
			this.probeTrimmingInformation = probeTrimmingInformation;
			this.commonReadNameBeginning = commonReadNameBeginning;
			this.totalMatches = 0;
			nextRecord = getNextRecordToReturn();
		}

		private SAMRecord getNextRecordToReturn() {
			SAMRecord nextRecordToReturn = null;
			while (((samRecord == null && samIter.hasNext()) || (fastqOneRecord == null && fastq1Iter.hasNext() && fastqTwoRecord == null && fastq2Iter.hasNext())) && nextRecordToReturn == null) {
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
						if (samRecord.getFirstOfPairFlag()) {
							nextRecordToReturn = checkAndStoreFastqInfoInRecord(samRecord, fastqOneRecord.getReadString(), fastqOneRecord.getBaseQualityString(), trimmingSkipped,
									probeTrimmingInformation);
						} else {
							nextRecordToReturn = checkAndStoreFastqInfoInRecord(samRecord, fastqTwoRecord.getReadString(), fastqTwoRecord.getBaseQualityString(), trimmingSkipped,
									probeTrimmingInformation);
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
		if (!trimmingSkipped && !isTrimAmountCorrect(samRecord, readSequenceFromFastq, readBaseQualityFromFastq, trimmingSkipped, probeTrimmingInformation)) {
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
