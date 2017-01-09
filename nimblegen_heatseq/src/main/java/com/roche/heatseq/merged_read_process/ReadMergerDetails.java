package com.roche.heatseq.merged_read_process;

import java.util.List;

public class ReadMergerDetails {

	private final int processedReadsCount;
	private final int numberOfReadsToProcess;
	private final boolean completedSuccesfully;
	private final List<Throwable> exceptions;

	public ReadMergerDetails(int processedReadsCount, int numberOfReadsToProcess, boolean completedSuccesfully, List<Throwable> exceptions) {
		super();
		this.processedReadsCount = processedReadsCount;
		this.numberOfReadsToProcess = numberOfReadsToProcess;
		this.completedSuccesfully = completedSuccesfully;
		this.exceptions = exceptions;
	}

	public int getProcessedReadsCount() {
		return processedReadsCount;
	}

	public int getNumberOfReadsToProcess() {
		return numberOfReadsToProcess;
	}

	public boolean isCompletedSuccesfully() {
		return completedSuccesfully;
	}

	public List<Throwable> getExceptions() {
		return exceptions;
	}

}
