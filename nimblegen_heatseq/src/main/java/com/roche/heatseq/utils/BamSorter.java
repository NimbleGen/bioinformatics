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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;

public class BamSorter {

	private final static Logger logger = LoggerFactory.getLogger(FastqSorter.class);
	private final static int RECORDS_PER_CHUNK = 3000000;

	public static void sortBam(File inputBamFile, File tempDirectory, File outputBamFile, final Comparator<SAMRecord> comparator) {
		long start = System.currentTimeMillis();
		try (CloseableAndIterableIterator<SAMRecord> iter = getSortedBamIterator(inputBamFile, tempDirectory, comparator)) {
			SAMFileHeader header = null;
			try (SAMFileReader reader = new SAMFileReader(inputBamFile)) {
				header = reader.getFileHeader();
			}

			SAMFileWriter sortedBamWriter = new SAMFileWriterFactory().makeBAMWriter(header, true, outputBamFile, 0);
			while (iter.hasNext()) {
				SAMRecord record = iter.next();
				sortedBamWriter.addAlignment(record);
			}
		}
		long stop = System.currentTimeMillis();
		logger.info("total time for sorting bam:" + DateUtil.convertMillisecondsToHHMMSS(stop - start));
	}

	public static CloseableAndIterableIterator<SAMRecord> getSortedBamIterator(File inputBamFile, File tempDirectory, final Comparator<SAMRecord> comparator) {
		CloseableAndIterableIterator<SAMRecord> returnIter = null;
		try (SAMFileReader currentReader = new SAMFileReader(inputBamFile)) {
			SAMFileHeader header = currentReader.getFileHeader();
			Iterator<SAMRecord> iter = currentReader.iterator();
			returnIter = getSortedBamIterator(iter, header, tempDirectory, comparator);
		}
		return returnIter;
	}

	public static CloseableAndIterableIterator<SAMRecord> getSortedBamIterator(Iterator<SAMRecord> samIterator, SAMFileHeader header, File tempDirectory, final Comparator<SAMRecord> comparator) {
		File chunkDirectory = new File(tempDirectory, "bam_chunks_" + System.currentTimeMillis() + "/");
		List<File> chunkFiles = createInitialSortedFileChunks(samIterator, header, chunkDirectory, comparator);
		return new SortedBamIterator(chunkDirectory, chunkFiles, comparator);
	}

	public interface CloseableAndIterableIterator<T> extends Iterator<T>, AutoCloseable {
		@Override
		void close();
	}

	public static class SortedBamIterator implements CloseableAndIterableIterator<SAMRecord> {

		private final File chunkDirectory;
		private final TreeSet<SamRecordAndChunkIndex> currentSortedRecords;
		private final SAMRecordIterator[] iters;
		private final SAMFileReader[] readers;
		private SAMRecord nextRecord;

		@SuppressWarnings("resource")
		public SortedBamIterator(File chunkDirectory, List<File> chunkFiles, final Comparator<SAMRecord> comparator) {
			this.chunkDirectory = chunkDirectory;

			currentSortedRecords = new TreeSet<SamRecordAndChunkIndex>(new Comparator<SamRecordAndChunkIndex>() {
				@Override
				public int compare(SamRecordAndChunkIndex o1, SamRecordAndChunkIndex o2) {
					return comparator.compare(o1.getRecord(), o2.getRecord());
				}
			});

			readers = new SAMFileReader[chunkFiles.size()];
			iters = new SAMRecordIterator[chunkFiles.size()];
			for (int i = 0; i < chunkFiles.size(); i++) {
				SAMFileReader currentReader = new SAMFileReader(chunkFiles.get(i));
				readers[i] = currentReader;
				SAMRecordIterator currentIter = currentReader.iterator();
				iters[i] = currentIter;
				if (currentIter.hasNext()) {
					currentSortedRecords.add(new SamRecordAndChunkIndex(currentIter.next(), i));
				}
			}

			nextRecord = getNextRecordToReturn();
		}

		public boolean hasNext() {
			return nextRecord != null;
		}

		private SAMRecord getNextRecordToReturn() {
			SAMRecord nextRecord = null;
			if (currentSortedRecords.size() > 0) {
				SamRecordAndChunkIndex currentRecordAndReaderIndex = currentSortedRecords.pollFirst();
				nextRecord = currentRecordAndReaderIndex.getRecord();
				int chunkIndex = currentRecordAndReaderIndex.getChunkIndex();
				SAMRecordIterator currentIter = iters[chunkIndex];
				if (currentIter.hasNext()) {
					SAMRecord nextChunkRecord = currentIter.next();
					if (!currentSortedRecords.add(new SamRecordAndChunkIndex(nextChunkRecord, chunkIndex))) {
						throw new IllegalStateException("Provided comparator does not uniquely identify the records.");
					}
				}
			}
			return nextRecord;
		}

		public SAMRecord next() {
			SAMRecord recordToReturn = nextRecord;
			nextRecord = getNextRecordToReturn();
			return recordToReturn;
		}

		@Override
		public void close() {
			for (SAMRecordIterator iter : iters) {
				iter.close();
			}

			for (SAMFileReader reader : readers) {
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
			throw new IllegalStateException("This method is not implemented.");
		}
	}

	private static class SamRecordAndChunkIndex {
		private final SAMRecord record;
		private final int chunkIndex;

		public SamRecordAndChunkIndex(SAMRecord record, int chunkIndex) {
			super();
			this.record = record;
			this.chunkIndex = chunkIndex;
		}

		public SAMRecord getRecord() {
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
			SamRecordAndChunkIndex other = (SamRecordAndChunkIndex) obj;
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

	private static List<File> createInitialSortedFileChunks(Iterator<SAMRecord> iter, SAMFileHeader header, File tempDirectory, Comparator<SAMRecord> comparator) {
		List<File> chunkFiles = new ArrayList<File>();
		List<SAMRecord> currentInputBuffer = new ArrayList<SAMRecord>();

		header.setSortOrder(SortOrder.unsorted);

		while (iter.hasNext()) {
			SAMRecord record = iter.next();
			currentInputBuffer.add(record);

			if (currentInputBuffer.size() > RECORDS_PER_CHUNK) {
				SamDataWriterHelper dataWriterHelper = new SamDataWriterHelper(header, chunkFiles.size(), tempDirectory, currentInputBuffer, comparator);
				dataWriterHelper.run();
				currentInputBuffer = new ArrayList<SAMRecord>();
				chunkFiles.add(dataWriterHelper.getChunkFile());
			}
		}

		if (currentInputBuffer.size() > 0) {
			SamDataWriterHelper dataWriterHelper = new SamDataWriterHelper(header, chunkFiles.size(), tempDirectory, currentInputBuffer, comparator);
			dataWriterHelper.run();
			chunkFiles.add(dataWriterHelper.getChunkFile());
		}

		return chunkFiles;
	}

	private static class SamDataWriterHelper implements Runnable {
		private final Logger logger = LoggerFactory.getLogger(SamDataWriterHelper.class);

		private final List<SAMRecord> recordsToWrite;
		private final Comparator<SAMRecord> comparator;
		private final File chunkFile;
		private final SAMFileHeader header;

		public SamDataWriterHelper(SAMFileHeader header, int chunkFileIndex, File tempDirectory, List<SAMRecord> recordsToWrite, Comparator<SAMRecord> comparator) {
			super();
			this.header = header;
			this.recordsToWrite = recordsToWrite;
			this.comparator = comparator;
			this.chunkFile = new File(tempDirectory, chunkFileIndex + "_temp_sorting.sam");
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
				Collections.sort(recordsToWrite, comparator);

				SAMFileWriter writer = new SAMFileWriterFactory().makeBAMWriter(header, true, chunkFile, 0);

				for (SAMRecord recordToWrite : recordsToWrite) {
					writer.addAlignment(recordToWrite);
				}
				writer.close();
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

	public static class SamRecordNameComparator implements Comparator<SAMRecord> {
		@Override
		public int compare(SAMRecord o1, SAMRecord o2) {
			int result = o1.getReadName().compareTo(o2.getReadName());
			if (result == 0) {
				if (o1.getFirstOfPairFlag() && !o2.getFirstOfPairFlag()) {
					result = -1;
				} else {
					result = 1;
				}
			}
			return result;
		}
	}

	public static class SamRecordCoordinatesComparator implements Comparator<SAMRecord> {
		@Override
		public int compare(SAMRecord o1, SAMRecord o2) {
			int result = o1.getReadName().compareTo(o2.getReadName());
			if (result == 0) {
				if (o1.getFirstOfPairFlag() && !o2.getFirstOfPairFlag()) {
					result = -1;
				} else {
					result = 1;
				}
			}
			return result;
		}
	}

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		File inputFile = new File("D:/kurts_space/heatseq/big/input.bam");
		File tempDirectory = new File("D:/kurts_space/heatseq/big/temp/");
		File outputFile = new File("D:/kurts_space/heatseq/big/sorted_input.bam");

		sortBam(inputFile, tempDirectory, outputFile, new SamRecordNameComparator());
		long stop = System.currentTimeMillis();
		System.out.println("total time:" + DateUtil.convertMillisecondsToHHMMSS(stop - start));
	}

}
