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

package com.roche.sequencing.bioinformatics.common.alignment;

import com.roche.sequencing.bioinformatics.common.sequence.ICode;

public interface IAlignmentScorer {
	/**
	 * 
	 * @param codeOne
	 * @param codeTwo
	 * @return the score associated with the relationship between codeOne and codeTwo
	 */
	double getMatchScore(ICode codeOne, ICode codeTwo);

	/**
	 * @return the score associated with the continuation of a gap
	 */
	double getGapScore();

	/**
	 * 
	 * @return the score associated with the beginning of a gap
	 */
	double getGapStartScore();

	/**
	 * 
	 * @return true if gaps at the beginning or end of an alignment should be penalized
	 */
	boolean shouldPenalizeTerminalGaps();
}
