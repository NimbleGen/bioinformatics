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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
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
import com.roche.sequencing.bioinformatics.common.utils.ListUtil;
import com.roche.sequencing.bioinformatics.common.utils.fastq.PicardException;

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

	/**
	 * Working from an unsorted bam file and unsorted paired fastQ files, output a merged, read index sorted bam file.
	 * 
	 * This is essentially a classic hash join: http://en.wikipedia.org/wiki/Hash_join#Classic_hash_join
	 * 
	 * If performance becomes an issue here we can first partition to hash buckets on disk (a Grace hash join: http://en.wikipedia.org/wiki/Hash_join#Grace_hash_join)
	 * 
	 * @param unsortedBamFile
	 * @param unsortedFastq1File
	 * @param unsortedFastq2File
	 * @param bamFileWithRawFastqSequencesAndQualities
	 * @return
	 */
	public static File createBamFileWithDataFromRawFastqFiles(File unsortedDedupedBamFile, File originalUnsortedFastq1File, File originalUnsortedFastq2File, File dedupedBamFileWithOriginalNames,
			boolean trimmingSkipped, ProbeTrimmingInformation probeTrimmingInformation, File tempDirectory, MergedSamNamingConvention namingConvention, boolean useFastqSequenceAndQualities,
			boolean shouldCheckIBamfTrimmed, Comparator<FastqRecord> fastqComparator, Comparator<SAMRecord> bamComparator, boolean useFastqIndexesAsFastqReadNamesWhenMerging,
			boolean addIndexFromInputFastqToNameWithUnderscoreDelimiter) {
		// Each new iteration starts at the first record.
		try (CloseableIterator<FastqRecord> fastq1Iter = FastqSorter.getSortedFastqIterator(originalUnsortedFastq1File, tempDirectory, fastqComparator,
				addIndexFromInputFastqToNameWithUnderscoreDelimiter)) {
			try (CloseableIterator<FastqRecord> fastq2Iter = FastqSorter.getSortedFastqIterator(originalUnsortedFastq2File, tempDirectory, fastqComparator,
					addIndexFromInputFastqToNameWithUnderscoreDelimiter)) {

				long bamSortStart = System.currentTimeMillis();
				try (CloseableAndIterableIterator<SAMRecord> samIter = BamSorter.getSortedBamIterator(unsortedDedupedBamFile, tempDirectory, bamComparator)) {

					long bamSortStop = System.currentTimeMillis();
					logger.info("Time to sort bam by integer read names (read indexes from raw fastq files):" + DateUtil.convertMillisecondsToHHMMSSMMM(bamSortStop - bamSortStart) + "(HH:MM:SS:MMM)");

					long mergeStart = System.currentTimeMillis();
					SAMFileHeader header = null;
					try (SamReader samReader = SamReaderFactory.makeDefault().open(unsortedDedupedBamFile)) {
						header = samReader.getFileHeader();
						header.setSortOrder(SortOrder.unsorted);
					} catch (IOException e) {
						throw new PicardException(e.getMessage(), e);
					}

					// Make a sorted, bam file writer with the fastest level of compression (0)
					SAMFileWriter samWriter = new SAMFileWriterFactory().setMaxRecordsInRam(PrimerReadExtensionAndPcrDuplicateIdentification.DEFAULT_MAX_RECORDS_IN_RAM).setTempDirectory(tempDirectory)
							.makeBAMWriter(header, true, dedupedBamFileWithOriginalNames, 0);

					MergedSamIterator mergedSamIterator = new MergedSamIterator(samIter, fastq1Iter, fastq2Iter, trimmingSkipped, probeTrimmingInformation, namingConvention,
							useFastqSequenceAndQualities, shouldCheckIBamfTrimmed, useFastqIndexesAsFastqReadNamesWhenMerging);
					while (mergedSamIterator.hasNext()) {
						samWriter.addAlignment(mergedSamIterator.next());
					}

					long mergeStop = System.currentTimeMillis();

					logger.info("Done merging raw fastq names with deduped bam file[" + dedupedBamFileWithOriginalNames.getAbsolutePath() + "]:"
							+ DateUtil.convertMillisecondsToHHMMSS(mergeStop - mergeStart));

					if (mergedSamIterator.getTotalMatches() == 0) {
						throw new IllegalStateException(
								"The read names in the input Fastq files do not match the reads names in the provided bam/sam file.  Some sample Bam File Record Read Names are as follows: ["
										+ ListUtil.toString(mergedSamIterator.getSampleSamReadNames()) + "].  Some sample Fastq Read Names are as follows: ["
										+ ListUtil.toString(mergedSamIterator.getSampleFastqReadNames()) + "].");
					}

					samWriter.close();
				}

			}
		}

		return dedupedBamFileWithOriginalNames;
	}

	public static enum MergedSamNamingConvention {
		FASTQ_READ_NAME, SAM_BAM_READ_NAME, INDEX_AFTER_UNDERSCORE_DELIMITER_IN_FASTQ_READ_NAME
	}

	private static class MergedSamIterator implements Iterator<SAMRecord> {

		private final Iterator<SAMRecord> samIter;
		private final Iterator<FastqRecord> fastq1Iter;
		private final Iterator<FastqRecord> fastq2Iter;
		private final boolean trimmingSkipped;
		private final ProbeTrimmingInformation probeTrimmingInformation;
		private final MergedSamNamingConvention namingConvention;
		private final boolean useFastqSequenceAndQualities;
		private final boolean shouldCheckIfBamTrimmed;
		private final boolean useFastqIndexesAsFastqReadNamesWhenMerging;
		// these are used for logging purposes, basically to give the user some example names when the readnames in the fastq and sam files do not match
		private final static int NUMBER_OF_SAMPLE_READ_NAMES_TO_STORE = 5;
		private final Set<String> sampleSamReadNames;
		private final Set<String> sampleFastqReadNames;

		private SAMRecord nextRecord;
		private SAMRecord samRecord;
		private FastqRecord fastqOneRecord;
		private FastqRecord fastqTwoRecord;
		private int totalMatchingPairs;
		private int fastqIndex;
		private String lastSamRecordName;
		private boolean lastSamRecordIsFirstOfPair;
		private String lastSamRecordDescription;

		public MergedSamIterator(Iterator<SAMRecord> samIter, Iterator<FastqRecord> fastq1Iter, Iterator<FastqRecord> fastq2Iter, boolean trimmingSkipped,
				ProbeTrimmingInformation probeTrimmingInformation, MergedSamNamingConvention namingConvention, boolean useFastqSequenceAndQualities, boolean shouldCheckIfBamTrimmed,
				boolean useFastqIndexesAsFastqReadNamesWhenMerging) {
			super();
			this.samIter = samIter;
			this.fastq1Iter = fastq1Iter;
			this.fastq2Iter = fastq2Iter;
			this.trimmingSkipped = trimmingSkipped;
			this.probeTrimmingInformation = probeTrimmingInformation;
			this.totalMatchingPairs = 0;
			this.namingConvention = namingConvention;
			this.useFastqSequenceAndQualities = useFastqSequenceAndQualities;
			this.shouldCheckIfBamTrimmed = shouldCheckIfBamTrimmed;
			this.useFastqIndexesAsFastqReadNamesWhenMerging = useFastqIndexesAsFastqReadNamesWhenMerging;
			this.fastqIndex = -1;
			this.sampleSamReadNames = new HashSet<>();
			this.sampleFastqReadNames = new HashSet<>();
			nextRecord = getNextRecordToReturn();
		}

		private SAMRecord getNextRecordToReturn() {
			SAMRecord nextRecordToReturn = null;

			while ((((samRecord == null && samIter.hasNext()) || samRecord != null)
					&& ((fastqOneRecord == null && fastq1Iter.hasNext() && fastqTwoRecord == null && fastq2Iter.hasNext()) || (fastqOneRecord != null && fastqTwoRecord != null)))
					&& nextRecordToReturn == null) {
				if (samRecord == null) {
					samRecord = samIter.next();

					if (samRecord != null && sampleSamReadNames.size() < NUMBER_OF_SAMPLE_READ_NAMES_TO_STORE) {
						sampleSamReadNames.add(samRecord.getReadName());
					}
				}

				if (fastqOneRecord == null && fastqTwoRecord == null) {
					fastqIndex++;
					fastqOneRecord = fastq1Iter.next();
					fastqTwoRecord = fastq2Iter.next();

					if (fastqOneRecord != null && sampleFastqReadNames.size() < NUMBER_OF_SAMPLE_READ_NAMES_TO_STORE) {
						sampleFastqReadNames.add(fastqOneRecord.getReadHeader());
					}
				}

				String samName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(samRecord.getReadName());

				String fastqOneName = fastqOneRecord.getReadHeader();
				String fastqTwoName = fastqTwoRecord.getReadHeader();

				int lastIndexOfUnderscore = fastqOneName.lastIndexOf(FastqSorter.FASTQ_NAME_AND_INDEX_DELIMITER);
				if ((lastIndexOfUnderscore >= 0) && (lastIndexOfUnderscore < fastqOneName.length())) {
					fastqOneName = fastqOneName.substring(0, lastIndexOfUnderscore);
				}

				lastIndexOfUnderscore = fastqTwoName.lastIndexOf(FastqSorter.FASTQ_NAME_AND_INDEX_DELIMITER);
				if ((lastIndexOfUnderscore >= 0) && (lastIndexOfUnderscore < fastqTwoName.length())) {
					fastqTwoName = fastqTwoName.substring(0, lastIndexOfUnderscore);
				}

				fastqOneName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(fastqOneName);
				fastqTwoName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(fastqTwoName);

				// make sure the fastq files have the same read header
				int fastqComp = fastqOneName.compareTo(fastqTwoName);
				if (fastqComp != 0) {
					throw new IllegalStateException("The read names within the Fastq files do not match, Fastq1 has read name [" + fastqOneName + "] at line [" + (totalMatchingPairs * 4)
							+ "] whereas Fastq2 has read name [" + fastqTwoName + "] at line [" + (totalMatchingPairs * 4) + "].");
				}
				int samToFastqComparison = 0;
				if (useFastqIndexesAsFastqReadNamesWhenMerging) {
					Integer samNameAsInt = Integer.parseInt(samName);
					samToFastqComparison = Integer.compare(samNameAsInt, fastqIndex);
				} else {
					samToFastqComparison = samName.compareTo(fastqOneName);
				}
				if (samToFastqComparison < 0) {
					boolean recordsUniqueIdentifiersMatch = samRecord != null && lastSamRecordName != null && samRecord.getReadName().equals(lastSamRecordName)
							&& samRecord.getFirstOfPairFlag() == lastSamRecordIsFirstOfPair;
					if (recordsUniqueIdentifiersMatch) {
						String readNumberString = "Read 1/2";
						if (samRecord.getSecondOfPairFlag()) {
							readNumberString = "Read 2/2";
						}
						logger.warn("Duplicate SAMRecord entry for " + readNumberString + " with Read Name[" + samRecord.getReadName() + "].  The records are as follows:[" + samRecord + "], ["
								+ lastSamRecordDescription + "].  Only the first entry will be used, skipping all subsequent, duplicate entries.");
					} else {
						logger.warn("Skipping SAMRecorq entry[" + samRecord + "] because it doesn't match values in the fastq files.");
					}
					lastSamRecordName = samRecord.getReadName();
					lastSamRecordDescription = samRecord.toString();
					lastSamRecordIsFirstOfPair = samRecord.getFirstOfPairFlag();
					samRecord = null;
				} else if (samToFastqComparison > 0) {
					fastqOneRecord = null;
					fastqTwoRecord = null;
				} else if (samToFastqComparison == 0) {
					FastqRecord fastqRecord = null;
					if (samRecord.getFirstOfPairFlag()) {
						fastqRecord = fastqOneRecord;
					} else {
						fastqRecord = fastqTwoRecord;

						fastqOneRecord = null;
						fastqTwoRecord = null;
						totalMatchingPairs++;
					}
					String readString = samRecord.getReadString();
					String qualityString = samRecord.getBaseQualityString();
					String readName = null;
					if (useFastqSequenceAndQualities) {
						readString = fastqRecord.getReadString();
						qualityString = fastqRecord.getBaseQualityString();
					}
					if (namingConvention == MergedSamNamingConvention.FASTQ_READ_NAME) {
						readName = fastqOneName;
					} else if (namingConvention == MergedSamNamingConvention.INDEX_AFTER_UNDERSCORE_DELIMITER_IN_FASTQ_READ_NAME) {
						String fastqReadName = fastqRecord.getReadHeader();
						lastIndexOfUnderscore = fastqReadName.lastIndexOf(FastqSorter.FASTQ_NAME_AND_INDEX_DELIMITER);
						if ((lastIndexOfUnderscore >= 0) && (lastIndexOfUnderscore < fastqReadName.length())) {
							// grab the index that was placed by the FastqSorter
							readName = fastqReadName.substring(lastIndexOfUnderscore + FastqSorter.FASTQ_NAME_AND_INDEX_DELIMITER.length(), fastqReadName.length());
						} else {
							throw new IllegalStateException("The provided Fastsq Records do not terminate in an underscore id pattern as required by the "
									+ MergedSamNamingConvention.INDEX_AFTER_UNDERSCORE_DELIMITER_IN_FASTQ_READ_NAME.name() + " naming convention.");
						}
					} else if (namingConvention == MergedSamNamingConvention.SAM_BAM_READ_NAME) {
						readName = samRecord.getReadName();
					} else {
						throw new AssertionError();
					}
					lastSamRecordName = samRecord.getReadName();
					lastSamRecordDescription = samRecord.toString();
					lastSamRecordIsFirstOfPair = samRecord.getFirstOfPairFlag();
					nextRecordToReturn = checkAndStoreFastqInfoInRecord(samRecord, readString, qualityString, trimmingSkipped, probeTrimmingInformation, readName, shouldCheckIfBamTrimmed);
					samRecord = null;
				}

			}
			return nextRecordToReturn;
		}

		public Set<String> getSampleSamReadNames() {
			return Collections.unmodifiableSet(sampleSamReadNames);
		}

		public Set<String> getSampleFastqReadNames() {
			return Collections.unmodifiableSet(sampleFastqReadNames);
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
			return totalMatchingPairs;
		}

		@Override
		public void remove() {
			throw new IllegalStateException("Method not implemented.");

		}

	}

	private static SAMRecord checkAndStoreFastqInfoInRecord(SAMRecord samRecord, String readSequenceFromFastq, String readBaseQualityFromFastq, boolean trimmingSkipped,
			ProbeTrimmingInformation probeTrimmingInformation, String readName, boolean shouldCheckIfBamTrimmed) {
		if (!trimmingSkipped && (shouldCheckIfBamTrimmed && !isTrimAmountCorrect(samRecord, readSequenceFromFastq, readBaseQualityFromFastq, trimmingSkipped, probeTrimmingInformation))) {
			throw new UnableToMergeFastqAndBamFilesException();
		}
		return storeFastqInfoInRecord(samRecord, readSequenceFromFastq, readBaseQualityFromFastq, readName);
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
	private static SAMRecord storeFastqInfoInRecord(SAMRecord record, String readSequenceFromFastq, String readBaseQualityFromFastq, String readName) {
		int mappedReadLength = record.getReadLength();
		SAMRecordUtil.setMappedReadLength(record, mappedReadLength);
		record.setReadString(readSequenceFromFastq);
		record.setBaseQualityString(readBaseQualityFromFastq);
		record.setReadName(readName);
		return record;
	}
}
