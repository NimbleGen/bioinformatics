package com.roche.sequencing.bioinformatics.common.text.viewer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.sequencing.bioinformatics.common.text.GZipIndex;
import com.roche.sequencing.bioinformatics.common.text.GZipIndexer;
import com.roche.sequencing.bioinformatics.common.text.GZipIndexer.GZipIndexPair;
import com.roche.sequencing.bioinformatics.common.text.ITextFileIndexerLineListeners;
import com.roche.sequencing.bioinformatics.common.text.ITextProgressListener;
import com.roche.sequencing.bioinformatics.common.text.TextFileIndex;
import com.roche.sequencing.bioinformatics.common.text.TextFileIndexer;
import com.roche.sequencing.bioinformatics.common.text.TextSearchIndex;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.InputStreamFactory;
import com.roche.sequencing.bioinformatics.common.utils.gzip.BamByteDecoder;
import com.roche.sequencing.bioinformatics.common.utils.gzip.GZipUtil;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IByteDecoder;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IBytes;
import com.roche.sequencing.bioinformatics.common.utils.gzip.RandomAccessFileBytes;

public class TextViewerUtil {

	private final static Logger logger = LoggerFactory.getLogger(TextViewerUtil.class);

	private final static String INDEX_EXTENSION = "idx";
	private final static String TEXT_SEARCH_EXTENSION = "srch";
	private final static String GZIP_DICTIONARY_EXTENSION = "gzdict";
	private final static String GZIP_INDEX_EXTENSION = "gzx";
	private final static String GZIP_FILE_EXTENSION = "gz";
	final static String BAM_FILE_EXTENSION = "bam";
	final static String PROBE_INFO_FILE_ENDING_TEXT = "probe_info.txt";
	private final static String BAM_BLOCK_INDEX = "bamblockindex";
	private final static String INDEX_DIR = "text_viewer_indexes";

	private final static int LINES_FOR_EACH_INDEX = 100;

	private TextViewerUtil() {
		throw new AssertionError();
	}

	public static Indexes indexFile(File file, ITextProgressListener progressListener) {
		TextFileIndex textFileIndex = null;
		TextSearchIndex textSearchIndex = null;
		GZipIndex gZipIndex = null;
		IBytes gZipDictionaryBytes = null;
		File bamBlockIndexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + BAM_BLOCK_INDEX);

		// check if there is an index file
		// prepend a '.' to the file name so it is possibly hidden or at least separates from actual file name when files
		// are listed
		File indexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + INDEX_EXTENSION);
		File textSearchFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + TEXT_SEARCH_EXTENSION);
		boolean isGzipFile = FileUtil.getFileExtension(file).toLowerCase().endsWith(GZIP_FILE_EXTENSION);
		boolean isBamFile = FileUtil.getFileExtension(file).toLowerCase().endsWith(BAM_FILE_EXTENSION);
		if (isGzipFile || isBamFile || GZipUtil.isCompressed(file)) {
			indexFile = new File(
					file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + FileUtil.getFileNameWithoutExtension(file.getName()) + "." + INDEX_EXTENSION);
			File gZipIndexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + GZIP_INDEX_EXTENSION);
			File gZipDictionaryFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + GZIP_DICTIONARY_EXTENSION);
			if (gZipIndexFile.exists() && gZipDictionaryFile.exists()) {
				try {
					gZipIndex = GZipIndexer.loadIndexFile(gZipIndexFile, gZipDictionaryFile);
					gZipDictionaryBytes = new RandomAccessFileBytes(new RandomAccessFile(gZipDictionaryFile, "r"));
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}
			}

			if (gZipIndex == null || gZipDictionaryBytes == null || (isBamFile && !bamBlockIndexFile.exists())) {
				try {

					IByteDecoder byteConverter = null;
					if (isBamFile) {
						byteConverter = new BamByteDecoder();
					}

					GZipIndexPair gZipIndexPair = GZipIndexer.indexGZipBlocks(new InputStreamFactory(file), gZipDictionaryFile, LINES_FOR_EACH_INDEX, progressListener, byteConverter);
					if (byteConverter != null) {
						byteConverter.persistToFile(bamBlockIndexFile);
					}

					gZipIndex = gZipIndexPair.getGzipIndex();
					textFileIndex = gZipIndexPair.getTextFileIndex();
					gZipDictionaryBytes = new RandomAccessFileBytes(new RandomAccessFile(gZipDictionaryFile, "r"));

					GZipIndexer.saveGZipIndexToFile(gZipIndex, gZipIndexFile);
					TextFileIndexer.saveIndexedTextToFile(textFileIndex, indexFile);
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}

		if (indexFile.exists()) {// && searcherIndexFile.exists()) {
			try {
				textFileIndex = TextFileIndexer.loadIndexFile(indexFile);
				if (textFileIndex.getFileSizeInBytes() != file.length() || textFileIndex.getVersion() != TextFileIndexer.VERSION) {
					System.out.println("wiping out index.");
					textFileIndex = null;
				} else {
					textSearchIndex = new TextSearchIndex(textSearchFile, true);
				}
				// textSearcher = new TextSearcher(searcherIndexFile);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		if (textFileIndex == null) {
			// TODO if the file is larger than a set size load a sample and display it first before creating the index
			// assuming that the user would initially want to see the format of the file and maybe doesn't even want
			// to see the whole file

			try {
				textSearchIndex = new TextSearchIndex(textSearchFile, false);
				TextSearchIndexer textSearchIndexer = new TextSearchIndexer(textSearchIndex);

				textFileIndex = TextFileIndexer.indexText(new InputStreamFactory(file), LINES_FOR_EACH_INDEX, textSearchIndexer, progressListener);
				textSearchIndex.closeIndexWriter();
				TextFileIndexer.saveIndexedTextToFile(textFileIndex, indexFile);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		return new Indexes(textFileIndex, gZipIndex, gZipDictionaryBytes, bamBlockIndexFile, textSearchIndex);
	}

	private static class TextSearchIndexer implements ITextFileIndexerLineListeners {

		private final TextSearchIndex textSearchIndex;

		public TextSearchIndexer(TextSearchIndex textSearchIndex) {
			super();
			this.textSearchIndex = textSearchIndex;
		}

		@Override
		public void lineRead(int lineNumber, String lineText) {
			textSearchIndex.addLine(lineNumber, lineText);
		}

	}

	public static boolean isFileIndexed(File file) {
		boolean isFileIndexed = false;

		if (file.exists()) {
			// check if there is an index file
			// prepend a '.' to the file name so it is possibly hidden or at least separates from actual file name when files
			// are listed
			File indexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + INDEX_EXTENSION);
			boolean isGzipFile = FileUtil.getFileExtension(file).toLowerCase().endsWith(GZIP_FILE_EXTENSION);
			boolean isBamFile = FileUtil.getFileExtension(file).toLowerCase().endsWith(BAM_FILE_EXTENSION);
			if (isGzipFile || isBamFile || GZipUtil.isCompressed(file)) {
				indexFile = new File(
						file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + FileUtil.getFileNameWithoutExtension(file.getName()) + "." + INDEX_EXTENSION);
				File gZipIndexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + GZIP_INDEX_EXTENSION);
				File gZipDictionaryFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + GZIP_DICTIONARY_EXTENSION);
				if (isBamFile) {
					File bamBlockIndexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + BAM_BLOCK_INDEX);
					isFileIndexed = (gZipIndexFile.exists() && gZipDictionaryFile.exists() && bamBlockIndexFile.exists());
				} else {
					isFileIndexed = (gZipIndexFile.exists() && gZipDictionaryFile.exists());
				}
			} else {
				isFileIndexed = indexFile.exists();
			}
		}
		return isFileIndexed;
	}

	public static int getNumberOfLinesFromIndex(File file) {
		int numberOfLines = 0;

		if (file.exists()) {
			// check if there is an index file
			// prepend a '.' to the file name so it is possibly hidden or at least separates from actual file name when files
			// are listed

			File indexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + INDEX_EXTENSION);
			boolean isGzipFile = FileUtil.getFileExtension(file).toLowerCase().endsWith(GZIP_FILE_EXTENSION);
			boolean isBamFile = FileUtil.getFileExtension(file).toLowerCase().endsWith(BAM_FILE_EXTENSION);
			if (isGzipFile || isBamFile || GZipUtil.isCompressed(file)) {
				indexFile = new File(
						file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + FileUtil.getFileNameWithoutExtension(file.getName()) + "." + INDEX_EXTENSION);
			}

			if (indexFile.exists()) {// && searcherIndexFile.exists()) {
				try {
					TextFileIndex textFileIndex = TextFileIndexer.loadIndexFile(indexFile);
					numberOfLines = textFileIndex.getNumberOfLines();
					// textSearcher = new TextSearcher(searcherIndexFile);
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
		return numberOfLines;
	}

	public static class Indexes {
		private final TextFileIndex textFileIndex;
		private final GZipIndex gZipIndex;
		private final IBytes gZipDictionaryBytes;
		private final File bamBlockIndexFile;
		private final TextSearchIndex textSearchIndex;

		public Indexes(TextFileIndex textFileIndex, GZipIndex gZipIndex, IBytes gZipDictionaryBytes, File bamBlockIndexFile, TextSearchIndex textSearchIndex) {
			super();
			this.textFileIndex = textFileIndex;
			this.gZipIndex = gZipIndex;
			this.gZipDictionaryBytes = gZipDictionaryBytes;
			this.bamBlockIndexFile = bamBlockIndexFile;
			this.textSearchIndex = textSearchIndex;
		}

		public TextFileIndex getTextFileIndex() {
			return textFileIndex;
		}

		public GZipIndex getgZipIndex() {
			return gZipIndex;
		}

		public IBytes getgZipDictionaryBytes() {
			return gZipDictionaryBytes;
		}

		public File getBamBlockIndexFile() {
			return bamBlockIndexFile;
		}

		public TextSearchIndex getTextSearchIndex() {
			return textSearchIndex;
		}

	}

}
