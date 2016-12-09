package com.roche.heatseq.merged_read_process;

public class ReadMergerDetails {

	private final int processedReadsCount;
	private final int numberOfReadsToProcess;

	public ReadMergerDetails(int processedReadsCount, int numberOfReadsToProcess) {
		super();
		this.processedReadsCount = processedReadsCount;
		this.numberOfReadsToProcess = numberOfReadsToProcess;
	}

	public int getProcessedReadsCount() {
		return processedReadsCount;
	}

	public int getNumberOfReadsToProcess() {
		return numberOfReadsToProcess;
	}

}
