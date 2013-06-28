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

class QualityScoreAndFastQLineIndex {

	private final int qualityScore;
	private final int fastQLineIndex;

	QualityScoreAndFastQLineIndex(int qualityScore, int fastQLineIndex) {
		super();
		this.qualityScore = qualityScore;
		this.fastQLineIndex = fastQLineIndex;
	}

	public int getQualityScore() {
		return qualityScore;
	}

	public int getFastQLineIndex() {
		return fastQLineIndex;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fastQLineIndex;
		result = prime * result + qualityScore;
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
		QualityScoreAndFastQLineIndex other = (QualityScoreAndFastQLineIndex) obj;
		if (fastQLineIndex != other.fastQLineIndex)
			return false;
		if (qualityScore != other.qualityScore)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "QualityScoreAndFastQLineIndex [qualityScore=" + qualityScore + ", fastQLineIndex=" + fastQLineIndex + "]";
	}

}
