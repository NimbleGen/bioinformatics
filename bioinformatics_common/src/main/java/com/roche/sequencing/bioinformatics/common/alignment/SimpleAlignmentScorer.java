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

public class SimpleAlignmentScorer implements IAlignmentScorer {

	public final static double DEFAULT_MATCH_SCORE = 1;
	public final static double DEFAULT_MISMATCH_PENALTY = -4;
	public final static double DEFAULT_GAP_OPEN_PENALTY = -6;
	public final static double DEFAULT_GAP_EXTEND_PENALTY = -1;

	private final double match;
	private final double mismatch;
	private final double gapExtension;
	private final double gapStart;
	private final boolean shouldPenalizeStartingTerminalGaps;
	private final boolean shouldPenalizeEndingTerminalGaps;

	public SimpleAlignmentScorer() {
		super();
		this.match = DEFAULT_MATCH_SCORE;
		this.mismatch = DEFAULT_MISMATCH_PENALTY;
		this.gapExtension = DEFAULT_GAP_EXTEND_PENALTY;
		this.gapStart = DEFAULT_GAP_OPEN_PENALTY;
		shouldPenalizeStartingTerminalGaps = false;
		shouldPenalizeEndingTerminalGaps = false;
	}

	public SimpleAlignmentScorer(double match, double mismatch, double gapExtension, double gapStart, boolean shouldPenalizeTerminalGaps) {
		this(match, mismatch, gapExtension, gapStart, shouldPenalizeTerminalGaps, shouldPenalizeTerminalGaps);
	}

	public SimpleAlignmentScorer(double match, double mismatch, double gapExtension, double gapStart, boolean shouldPenalizeStartingTerminalGaps, boolean shouldPenalizeEndingTerminalGaps) {
		super();
		this.match = match;
		this.mismatch = mismatch;
		this.gapExtension = gapExtension;
		this.gapStart = gapStart;
		this.shouldPenalizeStartingTerminalGaps = shouldPenalizeStartingTerminalGaps;
		this.shouldPenalizeEndingTerminalGaps = shouldPenalizeEndingTerminalGaps;
	}

	@Override
	public double getMatchScore(ICode codeOne, ICode codeTwo) {
		double score = 0;

		if (codeOne.matches(codeTwo)) {
			score += match;
		} else {
			score += mismatch;
		}

		return score;
	}

	@Override
	public double getGapScore() {
		return gapExtension;
	}

	@Override
	public double getGapStartScore() {
		return gapStart;
	}

	@Override
	public boolean shouldPenalizeStartingTerminalGaps() {
		return shouldPenalizeStartingTerminalGaps;
	}

	@Override
	public boolean shouldPenalizeEndingTerminalGaps() {
		return shouldPenalizeEndingTerminalGaps;
	}

}
