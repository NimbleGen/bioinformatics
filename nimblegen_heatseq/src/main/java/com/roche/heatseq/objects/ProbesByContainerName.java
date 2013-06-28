/**
 *   Copyright 2013 Roche NimbleGen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.roche.heatseq.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * A map for organizing probes by container/chromosome name
 * 
 */
public class ProbesByContainerName {
	private final Map<String, List<Probe>> probesByContainer;

	public ProbesByContainerName() {
		super();
		probesByContainer = new LinkedHashMap<String, List<Probe>>();
	}

	/**
	 * Associate a probe with this containerName
	 * 
	 * @param containerName
	 * @param probe
	 */
	public void addProbe(String containerName, Probe probe) {
		List<Probe> probes = probesByContainer.get(containerName);

		if (probes == null) {
			probes = new ArrayList<Probe>();
		}

		probes.add(probe);
		probesByContainer.put(containerName, probes);
	}

	/**
	 * @param containerName
	 * @return all the probes associated with this container name
	 */
	public List<Probe> getProbesByContainerName(String containerName) {
		List<Probe> probes = probesByContainer.get(containerName);

		if (probes != null) {
			probes = Collections.unmodifiableList(probes);
		}

		return probes;
	}

	/**
	 * @return all of the probes, regardless of container name
	 */
	public List<Probe> getProbes() {
		List<Probe> allProbes = new ArrayList<Probe>();
		for (List<Probe> probesForAContainer : probesByContainer.values()) {
			allProbes.addAll(probesForAContainer);
		}
		return allProbes;
	}

	/**
	 * @return a set of all container names
	 */
	public Set<String> getContainerNames() {
		return Collections.unmodifiableSet(probesByContainer.keySet());
	}

	/**
	 * @param containerName
	 * @return true if the given containerName is found in this object
	 */
	public boolean containsContainerName(String containerName) {
		return probesByContainer.containsKey(containerName);
	}

}
