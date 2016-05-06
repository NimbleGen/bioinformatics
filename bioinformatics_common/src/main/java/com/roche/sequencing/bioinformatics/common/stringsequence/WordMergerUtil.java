package com.roche.sequencing.bioinformatics.common.stringsequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.roche.multithreading.IExceptionListener;
import com.roche.multithreading.PausableFixedThreadPoolExecutor;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.stringsequence.alignment.NeedlemanWunschGlobalStringAlignment;
import com.roche.sequencing.bioinformatics.common.stringsequence.alignment.StringAlignmentPair;

public class WordMergerUtil {

	private static Random RANDOM_NUMBER_GENERATOR = new Random(System.currentTimeMillis());
	private static final int DEFAULT_NUMBER_OF_SHAKES_PER_ITERATION = 200;
	private static final int DEFAULT_NUMBER_OF_ITERATIONS_TO_WAIT_FOR_IMPROVEMENT = 100;

	private WordMergerUtil() {
		throw new AssertionError();
	}

	public static ILetter[] merge(List<ILetter[]> words) {
		return merge(words, DEFAULT_NUMBER_OF_ITERATIONS_TO_WAIT_FOR_IMPROVEMENT, DEFAULT_NUMBER_OF_SHAKES_PER_ITERATION);
	}

	public static ThresholdedMergeResults merge(List<ILetter[]> requiredWords, List<ILetter[]> extraWords, int maxScoreThreshold) {
		return merge(requiredWords, extraWords, maxScoreThreshold, DEFAULT_NUMBER_OF_ITERATIONS_TO_WAIT_FOR_IMPROVEMENT, DEFAULT_NUMBER_OF_SHAKES_PER_ITERATION);
	}

	private static ILetter[] merge(List<ILetter[]> words, int numberOfIterationsToWaitForImprovement, int numberOfShakesPerIteration) {
		return merge(words, numberOfIterationsToWaitForImprovement, numberOfShakesPerIteration, WordMergerScorer.DEFAULT_LARGE_NEGATIVE_NUMBER_FOR_MISMATCH_PENALTY);
	}

	public static class ThresholdedMergeResults {
		private final ILetter[] result;
		private final List<ILetter[]> excludedWords;

		private ThresholdedMergeResults(ILetter[] result, List<ILetter[]> excludedWords) {
			super();
			this.result = result;
			this.excludedWords = excludedWords;
		}

		public ILetter[] getResult() {
			return result;
		}

		public List<ILetter[]> getExcludedWords() {
			return excludedWords;
		}
	}

	private static ThresholdedMergeResults merge(List<ILetter[]> requiredWords, List<ILetter[]> extraWords, int maxScoreThreshold, int numberOfIterationsToWaitForImprovement,
			int numberOfShakesPerIterations) {
		return merge(requiredWords, extraWords, maxScoreThreshold, numberOfIterationsToWaitForImprovement, numberOfShakesPerIterations,
				WordMergerScorer.DEFAULT_LARGE_NEGATIVE_NUMBER_FOR_MISMATCH_PENALTY);
	}

	private static ThresholdedMergeResults merge(List<ILetter[]> requiredWords, List<ILetter[]> extraWords, int maxScoreThreshold, int numberOfIterationsToWaitForImprovement,
			int numberOfShakesPerIteration, double largeNegativeNumberForMismatchPenalty) {
		WordMergerScorer scorer = new WordMergerScorer(largeNegativeNumberForMismatchPenalty);

		ILetter[] requiredWord = merge(requiredWords, numberOfIterationsToWaitForImprovement, numberOfShakesPerIteration, largeNegativeNumberForMismatchPenalty);

		int scoreForRequiredWord = getScore(requiredWord);

		if (scoreForRequiredWord > maxScoreThreshold) {
			throw new IllegalStateException("The provided required words have a larger score than the provided threshold[" + maxScoreThreshold + "].");
		}

		final List<Integer> scoresForExtraWords = new ArrayList<Integer>();
		List<Integer> indexesSortedOnScore = new ArrayList<Integer>();
		for (int i = 0; i < extraWords.size(); i++) {
			ILetter[] word = extraWords.get(i);
			ILetter[] newWord = merge(requiredWord, word, scorer);
			int extraWordScore = getScore(newWord) - scoreForRequiredWord;
			scoresForExtraWords.add(extraWordScore);

			indexesSortedOnScore.add(i);
		}

		Collections.sort(indexesSortedOnScore, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return Integer.compare(scoresForExtraWords.get(o1), scoresForExtraWords.get(o2));
			}
		});

		List<ILetter[]> allUsedWords = new ArrayList<ILetter[]>(requiredWords);

		List<ILetter[]> excludedWords = new ArrayList<ILetter[]>();
		ILetter[] finalWord = requiredWord;
		boolean thresholdMet = false;
		for (int index : indexesSortedOnScore) {
			ILetter[] word = extraWords.get(index);
			if (thresholdMet) {
				excludedWords.add(word);
			} else {
				allUsedWords.add(word);
				ILetter[] newWord = merge(finalWord, word, scorer);
				newWord = randomShake(newWord, allUsedWords, scorer, 20);
				int score = getScore(newWord);
				if (score > maxScoreThreshold) {
					thresholdMet = true;
					excludedWords.add(word);
					allUsedWords.remove(word);
				} else {
					finalWord = newWord;
				}
			}
		}

		return new ThresholdedMergeResults(finalWord, excludedWords);
	}

	private static ILetter[] merge(List<ILetter[]> words, int numberOfIterationsToWaitForImprovement, int numberOfShakesPerIteration, double largeNegativeNumberForMismatchPenalty) {
		WordMergerScorer scorer = new WordMergerScorer(largeNegativeNumberForMismatchPenalty);

		ILetter[] bestWord = null;
		int bestScore = Integer.MAX_VALUE;

		int iterationsSinceLastImprovement = 0;
		int totalIterations = 0;

		while (iterationsSinceLastImprovement < numberOfIterationsToWaitForImprovement) {
			ILetter[] combinedWord = new ILetter[0];
			for (ILetter[] currentWord : words) {
				combinedWord = merge(combinedWord, currentWord, scorer);
			}
			combinedWord = randomShake(combinedWord, words, scorer, numberOfShakesPerIteration);

			int score = getScore(combinedWord);
			if (score < bestScore) {
				bestWord = combinedWord;
				System.out.println("found improvement from (" + bestScore + ") to (" + score + ") at iteration:" + totalIterations);
				bestScore = score;
				iterationsSinceLastImprovement = 0;

			} else {
				iterationsSinceLastImprovement++;
			}
			totalIterations++;
			System.out.println("iteration:" + totalIterations);
		}
		System.out.println("best score:" + bestScore);
		return bestWord;
	}

	private static ILetter[] randomShake(ILetter[] combinedWord, List<ILetter[]> words, WordMergerScorer scorer, int numberOfShakes) {
		BestResultHolder bestResultHolder = new BestResultHolder(combinedWord);

		int numProcessors = Runtime.getRuntime().availableProcessors();
		PausableFixedThreadPoolExecutor executor = new PausableFixedThreadPoolExecutor(Math.max(numProcessors - 2, 1), "WORD_MERGER_");
		executor.addExceptionListener(new IExceptionListener() {
			@Override
			public void exceptionOccurred(Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		});

		for (int i = 0; i < numberOfShakes; i++) {
			executor.submit(new RandomShakeRunner(bestResultHolder, words, scorer));
		}

		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return bestResultHolder.getBestResult();
	}

	private static class BestResultHolder {
		private int bestScore;
		private ILetter[] bestResult;

		public BestResultHolder(ILetter[] initialResult) {
			bestScore = getScore(initialResult);
			bestResult = initialResult;
		}

		public synchronized void submitResult(ILetter[] result) {
			int score = getScore(result);
			if (score < bestScore) {
				bestScore = score;
				bestResult = result;
				System.out.print(" " + score);
			}
		}

		public synchronized ILetter[] getBestResult() {
			return bestResult;
		}
	}

	private static class RandomShakeRunner implements Runnable {
		private final BestResultHolder bestResultHolder;
		private final List<ILetter[]> words;
		private final WordMergerScorer scorer;

		public RandomShakeRunner(BestResultHolder bestResultHolder, List<ILetter[]> words, WordMergerScorer scorer) {
			super();
			this.bestResultHolder = bestResultHolder;
			this.words = words;
			this.scorer = scorer;
		}

		@Override
		public void run() {
			ILetter[] newResult = randomShake(bestResultHolder.getBestResult(), words, scorer);
			bestResultHolder.submitResult(newResult);
		}
	}

	private static ILetter[] randomShake(ILetter[] combinedWord, List<ILetter[]> words, WordMergerScorer scorer) {
		combinedWord = clean(combinedWord, words);

		int maxWordsToShake = Math.max(words.size() - 1, 0);

		int numberOfWordsToShake = RANDOM_NUMBER_GENERATOR.nextInt(maxWordsToShake + 1);
		Set<Integer> indexesToShake = new HashSet<Integer>();

		while (indexesToShake.size() < numberOfWordsToShake) {
			indexesToShake.add(RANDOM_NUMBER_GENERATOR.nextInt(words.size()));
		}

		combinedWord = shake(combinedWord, words, scorer, new ArrayList<Integer>(indexesToShake));

		combinedWord = clean(combinedWord, words);

		return combinedWord;
	}

	private static ILetter[] shake(ILetter[] combinedWord, List<ILetter[]> words, WordMergerScorer scorer, List<Integer> indexesToShake) {
		List<ILetter[]> newWords = new ArrayList<ILetter[]>(words);
		Collections.sort(indexesToShake, Collections.reverseOrder());

		for (int i : indexesToShake) {
			newWords.remove(i);
		}

		combinedWord = clean(combinedWord, newWords);

		for (int i : indexesToShake) {
			combinedWord = merge(combinedWord, words.get(i), scorer);
		}

		combinedWord = clean(combinedWord, words);

		return combinedWord;
	}

	private static ILetter[] clean(ILetter[] mergedWord, List<ILetter[]> words) {
		boolean[] isUsed = new boolean[mergedWord.length];

		for (int i = 0; i < words.size(); i++) {
			ILetter[] word = words.get(i);

			int indexInMergedWord = 0;
			int indexInWord = 0;

			while (indexInWord < word.length && indexInMergedWord < mergedWord.length) {

				ILetter wordLetter = word[indexInWord];
				ILetter mergedWordLetter = mergedWord[indexInMergedWord];

				if (wordLetter.equals(mergedWordLetter)) {
					isUsed[indexInMergedWord] = true;
					indexInWord++;
				} else if (indexInMergedWord == mergedWord.length - 1) {
					throw new IllegalStateException("The provided merged word is not a solution to the provided words.");
				}

				indexInMergedWord++;
			}
		}

		List<ILetter> cleanedMergedWord = new ArrayList<ILetter>();
		for (int i = 0; i < mergedWord.length; i++) {
			if (isUsed[i]) {
				cleanedMergedWord.add(mergedWord[i]);
			}
		}

		return cleanedMergedWord.toArray(new ILetter[0]);
	}

	@SuppressWarnings("unused")
	private static Map<ILetter, Integer> getMaxCounts(List<ILetter[]> words) {
		Map<ILetter, Integer> maxCounts = new HashMap<ILetter, Integer>();

		for (ILetter[] word : words) {
			TallyMap<ILetter> tally = new TallyMap<ILetter>();
			for (ILetter letter : word) {
				tally.add(letter);
			}

			for (Entry<ILetter, Integer> entry : tally.getObjectsSortedFromMostTalliesToLeast()) {
				ILetter letter = entry.getKey();
				int count = entry.getValue();
				if (maxCounts.containsKey(letter)) {
					int currentMaxCount = maxCounts.get(letter);
					if (count > currentMaxCount) {
						maxCounts.put(letter, count);
					}
				} else {
					maxCounts.put(letter, count);
				}
			}
		}

		return maxCounts;
	}

	private static int getScore(ILetter[] word) {
		int wordScore = 0;
		if (word.length > 0) {
			for (ILetter letter : word) {
				wordScore += letter.getScore();
			}
		}
		return wordScore;
	}

	private static ILetter[] merge(ILetter[] wordOne, ILetter[] wordTwo, WordMergerScorer scorer) {
		ILetter[] mergedWord = null;
		if (wordOne != null && wordTwo != null) {
			ILetter[] longerWord = null;
			ILetter[] shorterWord = null;

			if (wordOne.length > wordTwo.length) {
				longerWord = wordOne;
				shorterWord = wordTwo;
			} else {
				longerWord = wordTwo;
				shorterWord = wordOne;
			}

			// avoid the global alignment if possible since it is expensive
			if (isQueryContainedWithinReference(shorterWord, longerWord)) {
				mergedWord = longerWord;
			} else {
				NeedlemanWunschGlobalStringAlignment alignment = new NeedlemanWunschGlobalStringAlignment(Arrays.asList(wordOne), Arrays.asList(wordTwo), scorer);
				StringAlignmentPair alignmentPair = alignment.getAlignmentPair();
				mergedWord = alignmentPair.getMergedAlignment().toArray(new ILetter[0]);
			}
		} else if (wordTwo == null) {
			mergedWord = wordOne;
		} else if (wordOne == null) {
			mergedWord = wordTwo;
		}

		return mergedWord;
	}

	private static boolean isQueryContainedWithinReference(ILetter[] query, ILetter[] reference) {
		int i = 0;
		int j = 0;

		while (i < query.length && j < reference.length) {
			if (query[i] == reference[j]) {
				i++;
				j++;
			} else {
				j++;
			}
		}

		boolean isContained = (i == query.length);
		return isContained;
	}

}
