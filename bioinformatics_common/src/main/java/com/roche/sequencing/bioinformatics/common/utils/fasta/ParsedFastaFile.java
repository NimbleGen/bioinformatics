package com.roche.sequencing.bioinformatics.common.utils.fasta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ParsedFastaFile implements Iterable<FastaEntry> {

	private List<FastaEntry> fastaEntries;

	public ParsedFastaFile() {
		fastaEntries = new ArrayList<FastaEntry>();
	}

	void addFastaEntry(FastaEntry fastaEntry) {
		fastaEntries.add(fastaEntry);
	}

	public List<FastaEntry> getFastaEntries() {
		return Collections.unmodifiableList(fastaEntries);
	}

	@Override
	public Iterator<FastaEntry> iterator() {
		return fastaEntries.iterator();
	}

}
