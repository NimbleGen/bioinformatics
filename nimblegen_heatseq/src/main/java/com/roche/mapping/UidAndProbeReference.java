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

package com.roche.mapping;

/**
 * 
 * Class to pair UID and probe reference
 * 
 */
class UidAndProbeReference {

	private final String uid;
	private final ProbeReference probeReference;
	private Integer hashCode;

	UidAndProbeReference(String uid, ProbeReference probeReference) {
		super();
		this.uid = uid;
		this.probeReference = probeReference;
	}

	/**
	 * 
	 * @return UID
	 */
	public String getUid() {
		return uid;
	}

	/**
	 * @return probeReference
	 */
	public ProbeReference getProbeReference() {
		return probeReference;
	}

	@Override
	public int hashCode() {
		if (hashCode == null) {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((probeReference == null) ? 0 : probeReference.hashCode());
			result = prime * result + ((uid == null) ? 0 : uid.hashCode());
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UidAndProbeReference other = (UidAndProbeReference) obj;
		if (probeReference == null) {
			if (other.probeReference != null)
				return false;
		} else if (!probeReference.equals(other.probeReference))
			return false;
		if (uid == null) {
			if (other.uid != null)
				return false;
		} else if (!uid.equals(other.uid))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "UidAndProbeReference [uid=" + uid + ", probeReference=" + probeReference + "]";
	}

}
