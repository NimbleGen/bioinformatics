package com.roche.sequencing.bioinformatics.common.stringsequence;

import com.roche.sequencing.bioinformatics.common.stringsequence.alignment.IStringAlignmentScorer;

class WordMergerScorer implements IStringAlignmentScorer {

	final static double DEFAULT_LARGE_NEGATIVE_NUMBER_FOR_MISMATCH_PENALTY = -100000;

	private final double largeNegativeNumberForMismatchPenalty;

	public WordMergerScorer(double largeNegativeNumberForMismatchPenalty) {
		this.largeNegativeNumberForMismatchPenalty = largeNegativeNumberForMismatchPenalty;
	}

	public WordMergerScorer() {
		this.largeNegativeNumberForMismatchPenalty = DEFAULT_LARGE_NEGATIVE_NUMBER_FOR_MISMATCH_PENALTY;
	}

	@Override
	public double getMatchScore(ILetter codeOne, ILetter codeTwo) {
		double score = 0.0;
		if (codeOne.equals(codeTwo)) {
			score = codeOne.getScore();
		} else {
			score = largeNegativeNumberForMismatchPenalty;
		}

		return score;
	}

	@Override
	public double getGapScore(ILetter code) {
		return -code.getScore();
	}

	@Override
	public double getGapStartScore(ILetter code) {
		return getGapScore(code);
	}

	@Override
	public boolean shouldPenalizeStartingTerminalGaps() {
		return true;
	}

	@Override
	public boolean shouldPenalizeEndingTerminalGaps() {
		return true;
	}

}
