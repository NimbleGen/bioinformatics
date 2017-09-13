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
package com.roche.heatseq.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.process.ReadNameTracking;
import com.roche.heatseq.utils.BamSorter.CloseableAndIterableIterator;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.IlluminaFastQReadNameUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.fastq.FastqReader;
import com.roche.sequencing.bioinformatics.common.utils.fastq.FastqWriter;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.CloseableIterator;

public class FastqSorter {
	private final static Logger logger = LoggerFactory.getLogger(FastqSorter.class);
	private final static int RECORDS_PER_CHUNK = 3000000;
	public final static String FASTQ_NAME_AND_INDEX_DELIMITER = " _ ";

	public static void sortFastq(File inputFastQFile, File tempDirectory, File outputFastQFile, final Comparator<FastqRecord> comparator) {
		long start = System.currentTimeMillis();
		CloseableIterator<FastqRecord> iter = getSortedFastqIterator(inputFastQFile, tempDirectory, comparator);

		try (FastqWriter sortedFastqWriter = new FastqWriter(outputFastQFile)) {
			while (iter.hasNext()) {
				FastqRecord record = iter.next();
				sortedFastqWriter.write(record);
			}
		}
		iter.close();
		long stop = System.currentTimeMillis();
		logger.info("total time for sorting fastq:" + DateUtil.convertMillisecondsToHHMMSS(stop - start));
	}

	public static CloseableIterator<FastqRecord> getSortedFastqIterator(File inputFastqFile, File tempDirectory, final Comparator<FastqRecord> comparator) {
		return getSortedFastqIterator(inputFastqFile, tempDirectory, comparator, false);
	}

	public static CloseableIterator<FastqRecord> getSortedFastqIterator(File inputFastqFile, File tempDirectory, final Comparator<FastqRecord> comparator,
			boolean addOriginalIndexesToNameWithUnderscoreDelimiter) {
		CloseableIterator<FastqRecord> iter = null;
		if (comparator == null) {
			iter = new FastqIteratorWithoutSorting(inputFastqFile, addOriginalIndexesToNameWithUnderscoreDelimiter);
		} else {
			iter = getSortedFastqIterator(new FastqReader(inputFastqFile), tempDirectory, comparator, addOriginalIndexesToNameWithUnderscoreDelimiter);
		}

		return iter;
	}

	private static CloseableIterator<FastqRecord> getSortedFastqIterator(Iterator<FastqRecord> fastqIterator, File tempDirectory, final Comparator<FastqRecord> comparator,
			boolean addOriginalIndexesToNameWithUnderscoreDelimiter) {
		File chunkDirectory = new File(tempDirectory, "fastq_chunks_" + System.currentTimeMillis() + "/");
		List<File> chunkFiles = createInitialSortedFileChunks(fastqIterator, chunkDirectory, comparator, addOriginalIndexesToNameWithUnderscoreDelimiter);
		return new SortedFastqIterator(chunkDirectory, chunkFiles, comparator);
	}

	private static class FastqIteratorWithoutSorting implements CloseableIterator<FastqRecord> {

		private final FastqReader reader;
		private final Iterator<FastqRecord> iterator;
		private final boolean addOriginalIndexesToNameWithUnderscoreDelimiter;
		private int index;

		public FastqIteratorWithoutSorting(File fastqFile, boolean addOriginalIndexesToNameWithUnderscoreDelimiter) {
			reader = new FastqReader(fastqFile);
			iterator = reader.iterator();
			this.addOriginalIndexesToNameWithUnderscoreDelimiter = addOriginalIndexesToNameWithUnderscoreDelimiter;
			index = 0;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public FastqRecord next() {
			FastqRecord record = iterator.next();
			if (addOriginalIndexesToNameWithUnderscoreDelimiter) {
				record = new FastqRecord(record.getReadHeader() + FASTQ_NAME_AND_INDEX_DELIMITER + index, record.getReadString(), record.getBaseQualityHeader(), record.getBaseQualityString());
			}
			index++;
			return record;
		}

		@Override
		public void close() {
			reader.close();
		}

	}

	private static class SortedFastqIterator implements CloseableIterator<FastqRecord> {

		private final File chunkDirectory;
		private final TreeSet<FastqRecordAndChunkIndex> currentSortedRecords;
		private final FastqReader[] readers;
		private FastqRecord nextRecord;

		@SuppressWarnings("resource")
		private SortedFastqIterator(File chunkDirectory, List<File> chunkFiles, final Comparator<FastqRecord> comparator) {
			this.chunkDirectory = chunkDirectory;

			currentSortedRecords = new TreeSet<FastqRecordAndChunkIndex>(new Comparator<FastqRecordAndChunkIndex>() {
				@Override
				public int compare(FastqRecordAndChunkIndex o1, FastqRecordAndChunkIndex o2) {
					return comparator.compare(o1.getRecord(), o2.getRecord());
				}
			});

			readers = new FastqReader[chunkFiles.size()];
			for (int i = 0; i < chunkFiles.size(); i++) {
				FastqReader currentReader = new FastqReader(chunkFiles.get(i), false);
				readers[i] = currentReader;
				if (currentReader.hasNext()) {
					currentSortedRecords.add(new FastqRecordAndChunkIndex(currentReader.next(), i));
				}
			}

			FastqRecordAndChunkIndex currentRecordAndReaderIndex = currentSortedRecords.pollFirst();

			nextRecord = currentRecordAndReaderIndex.getRecord();
			int chunkIndex = currentRecordAndReaderIndex.getChunkIndex();
			FastqReader currentReader = readers[chunkIndex];
			if (currentReader.hasNext()) {
				currentSortedRecords.add(new FastqRecordAndChunkIndex(currentReader.next(), chunkIndex));
			}

		}

		@Override
		public boolean hasNext() {
			return nextRecord != null;
		}

		private FastqRecord getNextRecordToReturn() {
			FastqRecord nextRecord = null;
			if (currentSortedRecords.size() > 0) {
				FastqRecordAndChunkIndex currentRecordAndReaderIndex = currentSortedRecords.pollFirst();
				nextRecord = currentRecordAndReaderIndex.getRecord();
				int chunkIndex = currentRecordAndReaderIndex.getChunkIndex();
				FastqReader currentReader = readers[chunkIndex];
				if (currentReader.hasNext()) {
					currentSortedRecords.add(new FastqRecordAndChunkIndex(currentReader.next(), chunkIndex));
				}
			}
			return nextRecord;
		}

		@Override
		public FastqRecord next() {
			FastqRecord recordToReturn = nextRecord;
			nextRecord = getNextRecordToReturn();
			return recordToReturn;
		}

		@Override
		public void close() {
			for (FastqReader reader : readers) {
				reader.close();
			}
			try {
				FileUtil.deleteDirectory(chunkDirectory);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		@Override
		public void remove() {
			throw new IllegalStateException("Method not implemented.");

		}

	}

	private static class FastqRecordAndChunkIndex {
		private final FastqRecord record;
		private final int chunkIndex;

		public FastqRecordAndChunkIndex(FastqRecord record, int chunkIndex) {
			super();
			this.record = record;
			this.chunkIndex = chunkIndex;
		}

		public FastqRecord getRecord() {
			return record;
		}

		public int getChunkIndex() {
			return chunkIndex;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + chunkIndex;
			result = prime * result + ((record == null) ? 0 : record.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FastqRecordAndChunkIndex other = (FastqRecordAndChunkIndex) obj;
			if (chunkIndex != other.chunkIndex)
				return false;
			if (record == null) {
				if (other.record != null)
					return false;
			} else if (!record.equals(other.record))
				return false;
			return true;
		}

	}

	private static List<File> createInitialSortedFileChunks(Iterator<FastqRecord> fastqIterator, File tempDirectory, Comparator<FastqRecord> comparator,
			boolean addOriginalIndexesToNameWithUnderscoreDelimiter) {
		List<File> chunkFiles = new ArrayList<File>();
		List<FastqRecord> currentInputBuffer = new ArrayList<FastqRecord>();

		int index = 0;
		while (fastqIterator.hasNext()) {
			FastqRecord record = fastqIterator.next();
			if (addOriginalIndexesToNameWithUnderscoreDelimiter) {
				FastqRecord newRecordWithAppendedIndexes = new FastqRecord(record.getReadHeader() + FASTQ_NAME_AND_INDEX_DELIMITER + index, record.getReadString(), record.getBaseQualityHeader(),
						record.getBaseQualityString());
				record = newRecordWithAppendedIndexes;
			}

			// update ReadNameTracker
			ReadNameTracking.trackIndexIfTrackingReadName(record.getReadHeader(), index);

			currentInputBuffer.add(record);

			if (currentInputBuffer.size() > RECORDS_PER_CHUNK) {
				FastqDataWriterHelper dataWriterHelper = new FastqDataWriterHelper(chunkFiles.size(), tempDirectory, currentInputBuffer, comparator);
				dataWriterHelper.run();
				currentInputBuffer = new ArrayList<FastqRecord>();
				chunkFiles.add(dataWriterHelper.getChunkFile());
			}
			index++;
		}

		if (currentInputBuffer.size() > 0) {
			FastqDataWriterHelper dataWriterHelper = new FastqDataWriterHelper(chunkFiles.size(), tempDirectory, currentInputBuffer, comparator);
			dataWriterHelper.run();
			chunkFiles.add(dataWriterHelper.getChunkFile());
		}

		return chunkFiles;
	}

	private static class FastqDataWriterHelper implements Runnable {
		private final Logger logger = LoggerFactory.getLogger(FastqDataWriterHelper.class);

		private final List<FastqRecord> recordsToWriter;
		private final Comparator<FastqRecord> comparator;
		private final File chunkFile;

		public FastqDataWriterHelper(int chunkFileIndex, File tempDirectory, List<FastqRecord> recordsToWriter, Comparator<FastqRecord> comparator) {
			super();
			this.recordsToWriter = recordsToWriter;
			this.comparator = comparator;
			this.chunkFile = new File(tempDirectory, chunkFileIndex + "_temp_sorting.fastq");
		}

		@Override
		public void run() {
			writeOutData();
		}

		public File getChunkFile() {
			return chunkFile;
		}

		private void writeOutData() {
			try {
				FileUtil.createNewFile(chunkFile);
				FastqWriter writer = new FastqWriter(chunkFile);
				Collections.sort(recordsToWriter, comparator);

				for (FastqRecord recordToWrite : recordsToWriter) {
					writer.write(recordToWrite);
				}
				writer.close();
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

	public static class FastqRecordNameComparator implements Comparator<FastqRecord> {
		@Override
		public int compare(FastqRecord o1, FastqRecord o2) {
			return o1.getReadHeader().compareTo(o2.getReadHeader());
		}
	}

	public static void main(String[] args) throws IOException {
		// long start = System.currentTimeMillis();
		// File inputFile = new File("D:/kurts_space/heatseq/big/r1.fastq");
		// File tempDirectory = new File("D:/kurts_space/heatseq/big/temp/");
		// File outputFile = new File("D:/kurts_space/heatseq/big/sorted_r1.fastq");
		//
		// sortFastq(inputFile, tempDirectory, outputFile, new Comparator<FastqRecord>() {
		// @Override
		// public int compare(FastqRecord o1, FastqRecord o2) {
		// return o1.getReadHeader().compareTo(o2.getReadHeader());
		// }
		// });
		// long stop = System.currentTimeMillis();
		// logger.info("total time:" + DateUtil.convertMillisecondsToHHMMSS(stop - start));
		File fastqFile = new File("D:\\kurts_space\\shared\\hsq_stand\\S01_Typical_Batch_1_rep1_S1_R1_quality_filtered.fastq");
		File fastq2File = new File("D:\\kurts_space\\shared\\hsq_stand\\S01_Typical_Batch_1_rep1_S1_R2_quality_filtered.fastq");
		int numberOfLines = FileUtil.countNumberOfLinesInFile(fastqFile);
		System.out.println("number of fastq entries:" + (numberOfLines / 4));
		File tempDir = new File("C:\\Users\\heilmank\\AppData\\Local\\Temp\\proc8_Results_HSQUtils_3962064479944936215\\");
		int count = 0;
		File namesOut = new File("C:\\Users\\heilmank\\Desktop\\fastqnames.txt");
		File namesOut2 = new File("C:\\Users\\heilmank\\Desktop\\fastqnames2.txt");
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(namesOut)))) {
			try (BufferedWriter writer2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(namesOut2)))) {
				try (CloseableIterator<FastqRecord> iter = getSortedFastqIterator(fastqFile, tempDir, new FastqRecordNameComparator())) {
					try (CloseableIterator<FastqRecord> iter2 = getSortedFastqIterator(fastq2File, tempDir, new FastqRecordNameComparator())) {
						while (iter.hasNext() && iter2.hasNext()) {
							FastqRecord record = iter.next();
							FastqRecord record2 = iter2.next();
							String readName = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(record.getReadHeader());
							String readName2 = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(record2.getReadHeader());
							if (readName.equals("D00680:241:HKL7CBCXY:1:1104:3291:58467")) {
								System.out.println("match found.");
							}
							if (!readName.equals(readName2)) {
								throw new AssertionError();
							}
							writer.write(readName + StringUtil.NEWLINE);
							writer2.write(readName2 + StringUtil.NEWLINE);
							count++;
						}
					}
				}
			}
		}
		System.out.println("count of fastq entries from iter:" + count);

		File samFile = new File("D:\\kurts_space\\shared\\hsq_stand\\S01_Typical_Batch_1_rep1_S1_sorted.bam");
		File samNamesOut = new File("C:\\Users\\heilmank\\Desktop\\samNames.txt");
		File samNamesOut2 = new File("C:\\Users\\heilmank\\Desktop\\samNames2.txt");
		int sam1Count = 0;
		int sam2Count = 0;
		SAMRecord lastRecord = null;
		TallyMap<String> tally = new TallyMap<>();
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(samNamesOut)))) {
			try (BufferedWriter writer2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(samNamesOut2)))) {
				try (CloseableAndIterableIterator<SAMRecord> iter = BamSorter.getSortedBamIterator(samFile, tempDir, new BamSorter.SamRecordNameComparator())) {
					while (iter.hasNext()) {
						SAMRecord record = iter.next();
						tally.add(record.getReadName());
						if (record.getFirstOfPairFlag()) {
							writer.write(record.getReadName() + StringUtil.NEWLINE);
							if (lastRecord != null && lastRecord.getFirstOfPairFlag()) {
								System.out.println(
										"Two first of pair flags found in a row so assuming that read two of " + lastRecord.getReadName() + " is missing.  This read[" + record + "] will be skipped.");
							}
							sam1Count++;
						} else if (record.getSecondOfPairFlag()) {
							writer2.write(record.getReadName() + StringUtil.NEWLINE);
							if (lastRecord != null && (!lastRecord.getReadName().equals(record.getReadName()) || (!lastRecord.getFirstOfPairFlag()))) {
								if (record.getFirstOfPairFlag() == lastRecord.getFirstOfPairFlag()) {
									System.out.println("duplicate record.");
								}
								System.out.println("Record " + record.getReadName() + " does not have a read one. Last records name was " + lastRecord.getReadName());
							}
							sam2Count++;
						}
						lastRecord = record;
					}
				}
			}
		}
		for (String lar : tally.getObjectsWithLargestCount()) {
			System.out.println(lar + ":" + tally.getCount(lar));
		}
		System.out.println("count of read 1 in sam records from iter:" + sam1Count);
		System.out.println("count of read 2 in sam records from iter:" + sam2Count);

	}

}
