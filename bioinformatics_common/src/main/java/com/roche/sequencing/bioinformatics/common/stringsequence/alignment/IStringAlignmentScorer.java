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

package com.roche.sequencing.bioinformatics.common.stringsequence.alignment;

import com.roche.sequencing.bioinformatics.common.stringsequence.ILetter;

public interface IStringAlignmentScorer {
	/**
	 * 
	 * @param codeOne
	 * @param codeTwo
	 * @return the score associated with the relationship between codeOne and codeTwo
	 */
	double getMatchScore(ILetter codeOne, ILetter codeTwo);

	/**
	 * @return the score associated with the continuation of a gap
	 */
	double getGapScore(ILetter code);

	/**
	 * 
	 * @return the score associated with the beginning of a gap
	 */
	double getGapStartScore(ILetter code);

	/**
	 * 
	 * @return true if gaps at the beginning of an alignment should be penalized
	 */
	boolean shouldPenalizeStartingTerminalGaps();

	/**
	 * 
	 * @return true if gaps at the end of an alignment should be penalized
	 */
	boolean shouldPenalizeEndingTerminalGaps();
}
