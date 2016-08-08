package com.roche.sequencing.bioinformatics.common.utils.gff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.roche.sequencing.bioinformatics.common.sequence.Strand;

public class ParsedGffFile {

	private final Map<String, List<GffEntry>> gffEntriesByContainer;
	private final List<String> sources;
	private final List<String> types;

	ParsedGffFile() {
		gffEntriesByContainer = new HashMap<String, List<GffEntry>>();
		sources = new ArrayList<String>();
		types = new ArrayList<String>();
	}

	String getSource(int indexOfSource) {
		return sources.get(indexOfSource);
	}

	String getType(int indexOfType) {
		return types.get(indexOfType);
	}

	void addEntry(String containerName, String source, String type, long start, long stop, double score, Strand strand, String attributes) {
		List<GffEntry> gffEntriesForContainer = gffEntriesByContainer.get(containerName);
		if (gffEntriesForContainer == null) {
			gffEntriesForContainer = new ArrayList<GffEntry>();
			gffEntriesByContainer.put(containerName, gffEntriesForContainer);
		}

		int indexOfSource = sources.indexOf(source);
		if (indexOfSource == -1) {
			indexOfSource = sources.size();
			sources.add(source);
		}

		int indexOfType = types.indexOf(type);
		if (indexOfType == -1) {
			indexOfType = types.size();
			types.add(type);
		}
		GffEntry entry = new GffEntry(this, indexOfSource, indexOfType, start, stop, score, strand, attributes);
		gffEntriesForContainer.add(entry);
	}

	void sortContainers() {
		for (List<GffEntry> entriesForContainer : gffEntriesByContainer.values()) {
			Collections.sort(entriesForContainer, new Comparator<GffEntry>() {
				@Override
				public int compare(GffEntry o1, GffEntry o2) {
					int result = Long.compare(o1.getStart(), o2.getStart());
					if (result == 0) {
						result = Long.compare(o1.getStop(), o2.getStop());
					}
					return result;
				}
			});
		}

	}

	public List<String> getContainerNames() {
		return new ArrayList<String>(gffEntriesByContainer.keySet());
	}

	public List<GffEntry> getSortedEntriesForContainer(String containerName) {
		return gffEntriesByContainer.get(containerName);
	}

}
