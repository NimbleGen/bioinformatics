package com.roche.heatseq.process;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.roche.sequencing.bioinformatics.common.utils.PrefixCompressionDictionary;
import com.roche.sequencing.bioinformatics.common.utils.PrefixCompressionDictionary.PrefixCompressedString;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ParsedProbeFile;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.Probe;

import htsjdk.samtools.SAMRecord;

public class ReadToProbeAssignmentResultsWithFileStore {

	// NOTE: This was part of an attempt to push a memory stored map into file storage. In the end the
	// query times were a big issue so the effort was stopped. This is being placed here just in case this solution
	// is needed in the short term. If you are reading this and have no idea what it is feel free to delete it.
	// Also see H2ReadToProbeFileStore
	// Kurt Heilman

	private Map<String, Set<Probe>> readToProbeMemoryStore;
	private final H2ReadToProbeFileStore readToProbeFileStore;

	private final Map<String, IRangeMap<SAMRecord>> alternateMappings;
	private final ParsedProbeFile parsedProbeFile;
	private final PrefixCompressionDictionary dictionary;

	public ReadToProbeAssignmentResultsWithFileStore(File tempDirectory, ParsedProbeFile probeFile, PrefixCompressionDictionary dictionary) {
		super();
		this.dictionary = dictionary;
		this.readToProbeMemoryStore = new ConcurrentHashMap<String, Set<Probe>>();
		int maxReadLength = dictionary.getMaxSerializedStringLength();
		int maxProbeIdLength = probeFile.getMaxProbeIdLength();
		this.readToProbeFileStore = new H2ReadToProbeFileStore(new File(tempDirectory, "read_to_probe"), maxReadLength, maxProbeIdLength);
		this.alternateMappings = new ConcurrentHashMap<>();
		this.parsedProbeFile = probeFile;
	}

	public PrefixCompressionDictionary getDictionary() {
		return dictionary;
	}

	public void switchToFileBasedStore() {
		if (readToProbeMemoryStore != null) {
			readToProbeFileStore.putAll(readToProbeMemoryStore);
			readToProbeMemoryStore.clear();
			readToProbeMemoryStore = null;
			readToProbeFileStore.indexReadNames();
		}
	}

	public boolean containsKey(String readName) {
		String serializedReadName = dictionary.compress(readName).getSerializedString();

		boolean containsKey = false;
		if (readToProbeMemoryStore != null) {
			containsKey = readToProbeMemoryStore.containsKey(serializedReadName);
		} else {
			containsKey = readToProbeFileStore.getProbeIds(serializedReadName).size() > 0;
		}

		return containsKey;
	}

	public void put(String readName, Set<Probe> containedProbesInBothReads) {
		if (readToProbeMemoryStore == null) {
			throw new IllegalStateException("The ReadToProbeAssignmentResult object is readOnly once switchToFileBasedStore() has been called.");
		}
		String serializedReadName = dictionary.compress(readName).getSerializedString();
		for (Probe probe : containedProbesInBothReads) {
			Set<Probe> probes = readToProbeMemoryStore.get(serializedReadName);
			if (probes == null) {
				probes = new HashSet<Probe>();
				readToProbeMemoryStore.put(serializedReadName, probes);
			}
			probes.add(probe);
		}
	}

	public void remove(String readName) {
		if (readToProbeMemoryStore == null) {
			throw new IllegalStateException("The ReadToProbeAssignmentResult object is readOnly once switchToFileBasedStore() has been called.");
		}
		String serializedReadName = dictionary.compress(readName).getSerializedString();
		readToProbeMemoryStore.remove(serializedReadName);
	}

	public Set<Probe> getAssignedProbes(String readName) {
		String serializedReadName = dictionary.compress(readName).getSerializedString();

		Set<Probe> probes = null;
		if (readToProbeMemoryStore != null) {
			probes = readToProbeMemoryStore.get(serializedReadName);
		} else {
			probes = new HashSet<Probe>();
			for (String probeId : readToProbeFileStore.getProbeIds(serializedReadName)) {
				probes.add(parsedProbeFile.getProbe(probeId));
			}
		}

		return probes;
	}

	public Iterator<String> getReadNames() {
		Iterator<String> iter = null;
		if (readToProbeMemoryStore != null) {
			iter = new ReadNameIter(readToProbeMemoryStore.keySet().iterator());
		} else {
			iter = new ReadNameIter(readToProbeFileStore.getReadNames());
		}
		return iter;
	}

	private class ReadNameIter implements Iterator<String> {

		private final Iterator<String> readNameIter;

		public ReadNameIter(Iterator<String> readNameIter) {
			super();
			this.readNameIter = readNameIter;
		}

		@Override
		public boolean hasNext() {
			return readNameIter.hasNext();
		}

		@Override
		public String next() {
			String serializedString = readNameIter.next();
			PrefixCompressedString compressedString = new PrefixCompressedString(serializedString);
			return dictionary.decompress(compressedString);
		}

	}

	public IRangeMap<SAMRecord> getAlternateLocationsBySequence(String container) {
		return alternateMappings.get(container);
	}

	public void putAlternateLocationsBySequence(String container, IRangeMap<SAMRecord> ranges) {
		alternateMappings.put(container, ranges);

	}

	public Collection<IRangeMap<SAMRecord>> getAlternateMappingRangeMaps() {
		return alternateMappings.values();
	}

}
