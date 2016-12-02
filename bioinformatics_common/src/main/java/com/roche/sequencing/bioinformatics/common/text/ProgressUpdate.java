package com.roche.sequencing.bioinformatics.common.text;

import com.roche.sequencing.bioinformatics.common.utils.CommonMathUtil;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;

public class ProgressUpdate {

	private final int linesRead;
	private final double percentComplete;
	private final long currentProcessTimeInMilliseconds;

	public ProgressUpdate(int linesRead, double percentComplete, long currentProcessTimeInMilliseconds) {
		super();
		this.linesRead = linesRead;
		this.percentComplete = percentComplete;
		this.currentProcessTimeInMilliseconds = currentProcessTimeInMilliseconds;
	}

	public int getLinesRead() {
		return linesRead;
	}

	public double getPercentComplete() {
		return CommonMathUtil.round(percentComplete, 2);
	}

	public long getCurrentProcessTimeInMilliseconds() {
		return currentProcessTimeInMilliseconds;
	}

	public long getEstimatedTimeToCompletionInMilliseconds() {
		double currentRate = (double) currentProcessTimeInMilliseconds / percentComplete;
		long timeToCompleteInMs = (long) Math.round(currentRate * (100 - percentComplete));
		return timeToCompleteInMs;
	}

	public String getEstimatedTimeToCompletionInHHMMSSMMM() {
		return DateUtil.convertMillisecondsToHHMMSSMMM(getEstimatedTimeToCompletionInMilliseconds());
	}

	public String getEstimatedCompletionTimeInYYYYMMDDHHMMSS() {
		long estimatedCompletionTime = System.currentTimeMillis() + getEstimatedTimeToCompletionInMilliseconds();
		return DateUtil.convertTimeInMillisecondsToDate(estimatedCompletionTime);
	}

	@Override
	public String toString() {
		return "ProgressUpdate [linesRead=" + linesRead + ", percentComplete=" + percentComplete + ", getEstimatedTimeToCompletionInHHMMSSMMM()=" + getEstimatedTimeToCompletionInHHMMSSMMM()
				+ ", getEstimatedCompletionTimeInYYYYMMDDHHMMSS()=" + getEstimatedCompletionTimeInYYYYMMDDHHMMSS() + "]";
	}

}
