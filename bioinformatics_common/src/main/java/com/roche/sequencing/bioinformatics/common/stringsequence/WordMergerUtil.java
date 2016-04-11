package com.roche.sequencing.bioinformatics.common.stringsequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.roche.sequencing.bioinformatics.common.stringsequence.alignment.NeedlemanWunschGlobalStringAlignment;
import com.roche.sequencing.bioinformatics.common.stringsequence.alignment.StringAlignmentPair;

public class WordMergerUtil {

	private WordMergerUtil() {
		throw new AssertionError();
	}

	public static ILetter[] merge(List<ILetter[]> words) {
		return merge(words, WordMergerScorer.DEFAULT_LARGE_NEGATIVE_NUMBER_FOR_MISMATCH_PENALTY);
	}

	public static ILetter[] merge(List<ILetter[]> words, double largeNegativeNumberForMismatchPenalty) {
		WordMergerScorer scorer = new WordMergerScorer(largeNegativeNumberForMismatchPenalty);

		final List<Integer> scores = new ArrayList<Integer>();
		List<Integer> indexes = new ArrayList<Integer>();

		for (int i = 0; i < words.size(); i++) {
			indexes.add(i);
			ILetter[] word = words.get(i);
			scores.add(getScore(word));
		}

		Collections.sort(indexes, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return Integer.compare(scores.get(o1), scores.get(o2));
			}
		});

		ILetter[] combinedWord = null;
		// add from the largest scored words to the smallest scored words
		for (int sortedIndex : indexes) {
			ILetter[] currentWord = words.get(sortedIndex);
			combinedWord = merge(currentWord, combinedWord, scorer);
		}

		return combinedWord;
	}

	private static int getScore(ILetter[] word) {
		int wordScore = 0;
		for (ILetter letter : word) {
			wordScore += letter.getScore();
		}
		return wordScore;
	}

	private static ILetter[] merge(ILetter[] wordOne, ILetter[] wordTwo, WordMergerScorer scorer) {
		ILetter[] mergedWord = null;
		if (wordOne != null && wordTwo != null) {

			NeedlemanWunschGlobalStringAlignment alignment = new NeedlemanWunschGlobalStringAlignment(Arrays.asList(wordOne), Arrays.asList(wordTwo), scorer);
			StringAlignmentPair alignmentPair = alignment.getAlignmentPair();
			mergedWord = alignmentPair.getMergedAlignment().toArray(new ILetter[0]);

		} else if (wordTwo == null) {
			mergedWord = wordOne;
		} else if (wordOne == null) {
			mergedWord = wordTwo;
		}
		return mergedWord;
	}

}
