package com.roche.sequencing.bioinformatics.common.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.roche.sequencing.bioinformatics.common.statistics.RunningStats;

public class Timer {

	private final Map<String, RunningStats> runningStatsByKey;
	private final Map<String, Long> currentTimers;

	public Timer() {
		super();
		this.runningStatsByKey = new HashMap<String, RunningStats>();
		this.currentTimers = new HashMap<String, Long>();
	}

	public void start(String key) {
		currentTimers.put(key, System.currentTimeMillis());
	}

	public void stop(String key) {
		long stopTime = System.currentTimeMillis();
		if (currentTimers.containsKey(key)) {
			long startTime = currentTimers.get(key);
			long totalTime = stopTime - startTime;
			RunningStats runningStats = runningStatsByKey.get(key);
			if (runningStats == null) {
				runningStats = new RunningStats();
				runningStatsByKey.put(key, runningStats);
			}
			runningStats.addValue(totalTime);
		}
	}

	public String getSummary() {
		StringBuilder stringBuilder = new StringBuilder();
		for (Entry<String, RunningStats> entry : runningStatsByKey.entrySet()) {
			RunningStats stats = entry.getValue();
			if (stats.getSumOfValues() > 0) {
				stringBuilder.append(entry.getKey() + ":" + StringUtil.TAB);
				stringBuilder.append("Total:" + DateUtil.convertMillisecondsToHHMMSSMMM((int) Math.ceil(stats.getSumOfValues())) + StringUtil.TAB);
				stringBuilder.append("Mean:" + DateUtil.convertMillisecondsToHHMMSSMMM((int) stats.getCurrentMean().intValue()) + StringUtil.TAB);
				stringBuilder.append("Min:" + DateUtil.convertMillisecondsToHHMMSSMMM((int) Math.ceil(stats.getMinValue())) + StringUtil.TAB);
				stringBuilder.append("Max:" + DateUtil.convertMillisecondsToHHMMSSMMM((int) Math.ceil(stats.getMaxValue())) + StringUtil.TAB);
				stringBuilder.append("Entries:" + stats.getNumberOfValues() + StringUtil.NEWLINE);
			}
		}
		return stringBuilder.toString();
	}

}
