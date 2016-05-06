package com.roche.sequencing.bioinformatics.common.utils.bed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BedTrack {

	private final List<String> browserCommands;
	private final Map<String, String> nameValuePairs;
	private final Map<String, List<IBedEntry>> bedEntriesByChromosome;

	public BedTrack() {
		super();
		this.browserCommands = new ArrayList<String>();
		this.nameValuePairs = new HashMap<String, String>();
		this.bedEntriesByChromosome = new HashMap<String, List<IBedEntry>>();
	}

	void addBrowserCommand(String browserCommand) {
		this.browserCommands.add(browserCommand);
	}

	void addTrackNameValuePair(String name, String value) {
		this.nameValuePairs.put(name, value);
	}

	void addBedEntry(IBedEntry bedEntry) {
		String chromosomeName = bedEntry.getChromosomeName();
		List<IBedEntry> bedEntries = null;
		if (bedEntriesByChromosome.containsKey(chromosomeName)) {
			bedEntries = bedEntriesByChromosome.get(chromosomeName);
		} else {
			bedEntries = new ArrayList<IBedEntry>();
			bedEntriesByChromosome.put(chromosomeName, bedEntries);
		}
		bedEntries.add(bedEntry);
	}

	public List<String> getBrowserCommands() {
		return Collections.unmodifiableList(browserCommands);
	}

	public Map<String, String> getNameValuePairs() {
		return Collections.unmodifiableMap(nameValuePairs);
	}

	public List<IBedEntry> getBedEntries() {
		List<IBedEntry> bedEntries = new ArrayList<IBedEntry>();
		for (List<IBedEntry> value : bedEntriesByChromosome.values()) {
			bedEntries.addAll(value);
		}
		return bedEntries;
	}

}
