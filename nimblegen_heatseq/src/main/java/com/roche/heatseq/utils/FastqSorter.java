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

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.CloseableIterator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;

public class FastqSorter {
	private final static Logger logger = LoggerFactory.getLogger(FastqSorter.class);
	private final static int RECORDS_PER_CHUNK = 3000000;

	private static void sortFastq(File inputFastQFile, File tempDirectory, File outputFastQFile, final Comparator<FastqRecord> comparator) {
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
		return getSortedFastqIterator(new FastqReader(inputFastqFile), tempDirectory, comparator);
	}

	private static CloseableIterator<FastqRecord> getSortedFastqIterator(Iterator<FastqRecord> fastqIterator, File tempDirectory, final Comparator<FastqRecord> comparator) {
		File chunkDirectory = new File(tempDirectory, "fastq_chunks_" + System.currentTimeMillis() + "/");
		List<File> chunkFiles = createInitialSortedFileChunks(fastqIterator, chunkDirectory, comparator);
		return new SortedFastqIterator(chunkDirectory, chunkFiles, comparator);
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

		public FastqRecord next() {
			FastqRecord recordToReturn = nextRecord;
			nextRecord = getNextRecordToReturn();
			return recordToReturn;
		}

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

	private static List<File> createInitialSortedFileChunks(Iterator<FastqRecord> fastqIterator, File tempDirectory, Comparator<FastqRecord> comparator) {
		List<File> chunkFiles = new ArrayList<File>();
		List<FastqRecord> currentInputBuffer = new ArrayList<FastqRecord>();

		while (fastqIterator.hasNext()) {
			FastqRecord record = fastqIterator.next();
			currentInputBuffer.add(record);

			if (currentInputBuffer.size() > RECORDS_PER_CHUNK) {
				FastqDataWriterHelper dataWriterHelper = new FastqDataWriterHelper(chunkFiles.size(), tempDirectory, currentInputBuffer, comparator);
				dataWriterHelper.run();
				currentInputBuffer = new ArrayList<FastqRecord>();
				chunkFiles.add(dataWriterHelper.getChunkFile());
			}
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

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		File inputFile = new File("D:/kurts_space/heatseq/big/r1.fastq");
		File tempDirectory = new File("D:/kurts_space/heatseq/big/temp/");
		File outputFile = new File("D:/kurts_space/heatseq/big/sorted_r1.fastq");

		sortFastq(inputFile, tempDirectory, outputFile, new Comparator<FastqRecord>() {
			@Override
			public int compare(FastqRecord o1, FastqRecord o2) {
				return o1.getReadHeader().compareTo(o2.getReadHeader());
			}
		});
		long stop = System.currentTimeMillis();
		logger.info("total time:" + DateUtil.convertMillisecondsToHHMMSS(stop - start));
	}

}
