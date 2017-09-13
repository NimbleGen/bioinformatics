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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.fastq.PicardException;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordCoordinateComparator;
import htsjdk.samtools.SAMRecordDuplicateComparator;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMRecordQueryNameComparator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

public class BamSorter {

	private final static Logger logger = LoggerFactory.getLogger(FastqSorter.class);
	private final static int RECORDS_PER_CHUNK = 500000;
	private final static int BAM_COMPRESSION_LEVEL = 0; // most compressed

	public static void sortBamFile(File inputBamFile, File outputBamFile, File tempDirectory, final Comparator<SAMRecord> comparator) {
		try (SamReader currentReader = SamReaderFactory.makeDefault().open(inputBamFile)) {
			SAMFileHeader header = currentReader.getFileHeader();
			Iterator<SAMRecord> iter = currentReader.iterator();
			try (CloseableAndIterableIterator<SAMRecord> sortedIter = getSortedBamIterator(iter, header, tempDirectory, comparator)) {

				if (comparator.getClass().equals(SAMRecordCoordinateComparator.class)) {
					header.setSortOrder(SortOrder.coordinate);
				} else if (comparator.getClass().equals(SAMRecordQueryNameComparator.class)) {
					header.setSortOrder(SortOrder.queryname);
				} else if (comparator.getClass().equals(SAMRecordDuplicateComparator.class)) {
					header.setSortOrder(SortOrder.duplicate);
				}
				try (SAMFileWriter writer = new SAMFileWriterFactory().makeBAMWriter(header, true, outputBamFile, BAM_COMPRESSION_LEVEL)) {
					while (sortedIter.hasNext()) {
						SAMRecord nextRecord = sortedIter.next();
						writer.addAlignment(nextRecord);
					}
				}
			}
		} catch (IOException e) {
			throw new PicardException(e.getMessage(), e);
		}

	}

	public static CloseableAndIterableIterator<SAMRecord> getSortedBamIterator(File inputBamFile, File tempDirectory, final Comparator<SAMRecord> comparator) {
		CloseableAndIterableIterator<SAMRecord> returnIter = null;
		try (SamReader currentReader = SamReaderFactory.makeDefault().open(inputBamFile)) {
			SAMFileHeader header = currentReader.getFileHeader();
			Iterator<SAMRecord> iter = currentReader.iterator();
			returnIter = getSortedBamIterator(iter, header, tempDirectory, comparator);
		} catch (IOException e) {
			throw new PicardException(e.getMessage(), e);
		}
		return returnIter;
	}

	private static CloseableAndIterableIterator<SAMRecord> getSortedBamIterator(Iterator<SAMRecord> samIterator, SAMFileHeader header, File tempDirectory, final Comparator<SAMRecord> comparator) {
		File chunkDirectory = new File(tempDirectory, "bam_chunks_" + System.currentTimeMillis() + "/");
		try {
			FileUtil.createDirectory(chunkDirectory);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		List<File> chunkFiles = createInitialSortedFileChunks(samIterator, header, chunkDirectory, comparator);
		return new SortedBamIterator(chunkDirectory, chunkFiles, comparator);
	}

	public interface CloseableAndIterableIterator<T> extends Iterator<T>, AutoCloseable {
		@Override
		void close();
	}

	private static class SortedBamIterator implements CloseableAndIterableIterator<SAMRecord> {

		private final File chunkDirectory;
		private final TreeSet<SamRecordAndChunkIndex> currentSortedRecords;
		private final SAMRecordIterator[] iters;
		private final SamReader[] readers;
		private SAMRecord nextRecord;
		private final Comparator<SAMRecord> comparator;

		private SortedBamIterator(File chunkDirectory, List<File> chunkFiles, final Comparator<SAMRecord> comparator) {
			this.chunkDirectory = chunkDirectory;
			this.comparator = comparator;
			currentSortedRecords = new TreeSet<SamRecordAndChunkIndex>(new Comparator<SamRecordAndChunkIndex>() {
				@Override
				public int compare(SamRecordAndChunkIndex o1, SamRecordAndChunkIndex o2) {
					return comparator.compare(o1.getRecord(), o2.getRecord());
				}
			});

			readers = new SamReader[chunkFiles.size()];
			iters = new SAMRecordIterator[chunkFiles.size()];
			for (int i = 0; i < chunkFiles.size(); i++) {
				SamReader currentReader = SamReaderFactory.makeDefault().open(chunkFiles.get(i));
				readers[i] = currentReader;
				SAMRecordIterator currentIter = currentReader.iterator();
				iters[i] = currentIter;
				if (currentIter.hasNext()) {
					currentSortedRecords.add(new SamRecordAndChunkIndex(currentIter.next(), i));
				}
			}

			nextRecord = getNextRecordToReturn();
		}

		@Override
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
						// add this point we know there is a problem just collecting more info before reporting the problem.
						SamRecordAndChunkIndex alreadyContainedRecord = null;
						for (SamRecordAndChunkIndex samRecordAndChunk : currentSortedRecords) {
							if (comparator.compare(samRecordAndChunk.getRecord(), nextChunkRecord) == 0) {
								alreadyContainedRecord = samRecordAndChunk;
							}
						}
						throw new IllegalStateException("Provided comparator does not uniquely identify the records.  Comparator failed on sam record[" + nextChunkRecord.getReadName() + " isReadOne:"
								+ nextChunkRecord.getFirstOfPairFlag() + "] in chunk[" + chunkIndex + "] which matches [" + alreadyContainedRecord.getRecord().getReadName() + " isReadOne:"
								+ alreadyContainedRecord.getRecord().getFirstOfPairFlag() + "] in chunk[" + alreadyContainedRecord.getChunkIndex() + "].");
					}
				}
			}
			return nextRecord;
		}

		@Override
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

			for (SamReader reader : readers) {
				try {
					reader.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
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
			try {
				this.chunkFile = File.createTempFile(chunkFileIndex + "_temp_sorting_", ".bam", tempDirectory);
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
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
				SAMFileWriter writer = new SAMFileWriterFactory().makeBAMWriter(header, true, chunkFile, BAM_COMPRESSION_LEVEL);

				for (SAMRecord recordToWrite : recordsToWrite) {
					writer.addAlignment(recordToWrite);
				}
				writer.close();
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

	public static class SamRecordNameAsIndexComparator implements Comparator<SAMRecord> {
		@Override
		public int compare(SAMRecord o1, SAMRecord o2) {
			int result = 0;
			try {
				Integer one = Integer.parseInt(o1.getReadName());
				Integer two = Integer.parseInt(o2.getReadName());
				result = Integer.compare(one, two);
			} catch (NumberFormatException e) {
				result = o1.getReadName().compareTo(o2.getReadName());
			}
			if (result == 0) {
				int o1PairNumber = 1;
				if (!o1.getFirstOfPairFlag()) {
					o1PairNumber = 2;
				}
				int o2PairNumber = 1;
				if (!o2.getFirstOfPairFlag()) {
					o2PairNumber = 2;
				}
				result = Integer.compare(o1PairNumber, o2PairNumber);
			}
			return result;
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

}
