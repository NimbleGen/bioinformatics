package com.roche.heatseq.process;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.roche.sequencing.bioinformatics.common.utils.probeinfo.Probe;

public class ReadToProbeAssignmentResults {

	private final Set<Probe>[] readIndexToProbeMemoryStore;
	private final File alternativeHitsBamFile;

	@SuppressWarnings("unchecked")
	public ReadToProbeAssignmentResults(int numberOfReads, File alternativeHitsBamFile) {
		super();
		this.readIndexToProbeMemoryStore = (Set<Probe>[]) new Set<?>[numberOfReads];
		this.alternativeHitsBamFile = alternativeHitsBamFile;
	}

	public synchronized boolean containsKey(int readIndex) {
		boolean containsKey = readIndexToProbeMemoryStore[readIndex] != null;
		return containsKey;
	}

	public synchronized void put(int readIndex, Set<Probe> containedProbesInBothReads) {
		for (Probe probe : containedProbesInBothReads) {
			Set<Probe> probes = readIndexToProbeMemoryStore[readIndex];
			if (probes == null) {
				probes = new HashSet<Probe>();
				readIndexToProbeMemoryStore[readIndex] = probes;
			}
			probes.add(probe);
		}
	}

	public synchronized void remove(int readIndex) {
		readIndexToProbeMemoryStore[readIndex] = null;
	}

	public synchronized Set<Probe> getAssignedProbes(int readIndex) {
		Set<Probe> probes = readIndexToProbeMemoryStore[readIndex];
		return probes;
	}

	public synchronized Iterator<Integer> getReadNames() {
		return new ReadNameIter();
	}

	public class ReadNameIter implements Iterator<Integer> {

		private int nextIndex;

		public ReadNameIter() {
			nextIndex = -1;
			next();
		}

		@Override
		public boolean hasNext() {
			return nextIndex < readIndexToProbeMemoryStore.length && readIndexToProbeMemoryStore[nextIndex] != null;
		}

		@Override
		public Integer next() {
			Integer next = nextIndex;
			do {
				nextIndex++;
			} while (nextIndex < readIndexToProbeMemoryStore.length && readIndexToProbeMemoryStore[nextIndex] == null);

			return next;
		}

	}

	public File getAlternativeHitsBamFile() {
		return alternativeHitsBamFile;
	}

}
