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

package com.roche.sequencing.bioinformatics.common.utils.probeinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * A map for organizing probes by sequence name
 * 
 */
public class ParsedProbeFile implements Iterable<Probe> {
	private final Map<String, List<Probe>> probesBySequence;
	private final Map<String, Probe> probesByProbeId;
	private int maxProbeIdLength;

	public ParsedProbeFile() {
		super();
		probesBySequence = new LinkedHashMap<String, List<Probe>>();
		probesByProbeId = new HashMap<String, Probe>();
		this.maxProbeIdLength = 0;
	}

	public int getMaxProbeIdLength() {
		return maxProbeIdLength;
	}

	/**
	 * Associate a probe with this sequenceName
	 * 
	 * @param sequenceName
	 * @param probe
	 */
	public void addProbe(String sequenceName, Probe probe) {
		List<Probe> probes = probesBySequence.get(sequenceName);

		if (probes == null) {
			probes = new ArrayList<Probe>();
		}

		maxProbeIdLength = Math.max(maxProbeIdLength, probe.getProbeId().length());

		probes.add(probe);
		probesBySequence.put(sequenceName, probes);
		probesByProbeId.put(probe.getProbeId(), probe);
	}

	public Probe getProbe(String probeId) {
		return probesByProbeId.get(probeId);
	}

	/**
	 * @param sequenceName
	 * @return all the probes associated with this sequence name
	 */
	public List<Probe> getProbesBySequenceName(String sequenceName) {
		List<Probe> probes = probesBySequence.get(sequenceName);

		if (probes != null) {
			probes = Collections.unmodifiableList(probes);
		}

		return probes;
	}

	/**
	 * @return all of the probes, regardless of sequence name
	 */
	public List<Probe> getProbes() {
		List<Probe> allProbes = new ArrayList<Probe>();
		for (List<Probe> probesForASequence : probesBySequence.values()) {
			allProbes.addAll(probesForASequence);
		}
		return allProbes;
	}

	/**
	 * @return a set of all sequence names
	 */
	public Set<String> getSequenceNames() {
		return Collections.unmodifiableSet(probesBySequence.keySet());
	}

	/**
	 * @param sequenceName
	 * @return true if the given sequenceName is found in this object
	 */
	public boolean containsSequenceName(String sequenceName) {
		return probesBySequence.containsKey(sequenceName);
	}

	@Override
	public Iterator<Probe> iterator() {
		return getProbes().iterator();
	}

}
