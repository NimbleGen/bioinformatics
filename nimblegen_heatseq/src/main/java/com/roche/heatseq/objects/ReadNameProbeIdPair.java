/*
 *    Copyright 2013 Roche NimbleGen Inc.
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
package com.roche.heatseq.objects;

public class ReadNameProbeIdPair {

	private final String readName;
	private final String probeId;

	public ReadNameProbeIdPair(String readName, String probeId) {
		super();
		this.readName = readName;
		this.probeId = probeId;
	}

	public String getReadName() {
		return readName;
	}

	public String getProbeId() {
		return probeId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((probeId == null) ? 0 : probeId.hashCode());
		result = prime * result + ((readName == null) ? 0 : readName.hashCode());
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
		ReadNameProbeIdPair other = (ReadNameProbeIdPair) obj;
		if (probeId == null) {
			if (other.probeId != null)
				return false;
		} else if (!probeId.equals(other.probeId))
			return false;
		if (readName == null) {
			if (other.readName != null)
				return false;
		} else if (!readName.equals(other.readName))
			return false;
		return true;
	}

}
