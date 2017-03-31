package com.roche.sequencing.bioinformatics.common.text;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextSearchIndex {
	private Logger logger = LoggerFactory.getLogger(TextSearchIndex.class);

	private final Analyzer analyzer;
	private Directory index;
	private final IndexWriterConfig config;
	private final IndexWriter luceneIndexWriter;

	private final static String LINE_NUMBER = "line_number";
	private final static String LINE_TEXT = "line_text";

	private final static int SEARCH_RESULTS_LIMIT = 1000;

	private final boolean loadExistingIndex;

	public TextSearchIndex(File indexFile, boolean loadExistingIndex) throws IOException {
		this.loadExistingIndex = loadExistingIndex;
		this.analyzer = new WhitespaceAnalyzer();

		this.index = FSDirectory.open(indexFile);

		config = new IndexWriterConfig(Version.LUCENE_4_10_1, analyzer);
		luceneIndexWriter = new IndexWriter(index, config);
		config.setOpenMode(OpenMode.APPEND);
	}

	public void addLine(int lineNumber, String text) {
		if (loadExistingIndex) {
			throw new IllegalStateException("Cannot add lines to an already existing index.");
		}
		Document luceneDocument = new Document();
		luceneDocument.add(new IntField(LINE_NUMBER, (int) (lineNumber - 1), Field.Store.YES));
		luceneDocument.add(new TextField(LINE_TEXT, text, Field.Store.NO));

		try {
			synchronized (this) {
				luceneIndexWriter.addDocument(luceneDocument);
			}
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	public void closeIndex() {
		if (!loadExistingIndex) {
			try {
				index.close();
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

	public void closeIndexWriter() {
		try {
			luceneIndexWriter.close();
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	public int[] search(String searchString) {
		int[] lineNumbers = null;
		try {
			QueryParser queryParser = new QueryParser(LINE_TEXT, analyzer);

			if (!searchString.endsWith("*")) {
				searchString += "*";
			}

			if (!searchString.startsWith("*")) {
				searchString = "*" + searchString;
			}

			queryParser.setAllowLeadingWildcard(true);
			Query mainQuery = queryParser.parse(searchString);

			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			TopScoreDocCollector collector = TopScoreDocCollector.create(SEARCH_RESULTS_LIMIT, true);

			searcher.search(mainQuery, collector);

			ScoreDoc[] hits = collector.topDocs().scoreDocs;
			lineNumbers = new int[hits.length];
			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				Document luceneDocument = searcher.doc(docId);
				int lineNumber = Integer.parseInt(luceneDocument.get(LINE_NUMBER));
				lineNumbers[i] = lineNumber;
			}
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
		return lineNumbers;
	}

}
