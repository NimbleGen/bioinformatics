package com.roche.heatseq.verification_tests;

import java.io.File;

import com.roche.bioinformatics.common.verification.IStepChecker;
import com.roche.bioinformatics.common.verification.RunResults;
import com.roche.bioinformatics.common.verification.StepCheckerResults;

public class TrimResultsPresentStepChecker implements IStepChecker {

	@Override
	public StepCheckerResults checkStep(RunResults runResults) {
		File outputDirectory = runResults.getOutputDirectory();

		HsqUtilsTrimResultFileSet results = HsqTestPlanUtil.getTrimResultFileSetFromDirectory(outputDirectory);

		String resultsDescription = "";
		boolean isSuccess = true;

		File trimmedR1 = results.getTrimmedR1();
		File trimmedR2 = results.getTrimmedR2();
		File log = results.getLogFile();

		if (trimmedR1 == null || !trimmedR1.exists()) {
			resultsDescription += "Trimmed R1 Not Found.";
			isSuccess = false;
		}

		if (trimmedR2 == null || !trimmedR2.exists()) {
			resultsDescription += "Trimmed R2 Not Found.";
			isSuccess = false;
		}

		if (log == null || !log.exists()) {
			resultsDescription += "Log File Not Found.";
			isSuccess = false;
		}

		if (isSuccess) {
			resultsDescription += "Trimmed R1[" + trimmedR1.getName() + "], Trimmed R2[" + trimmedR2.getName() + "] and the Log File[" + log.getName() + "] were all found in the results directory["
					+ log.getParentFile().getAbsolutePath() + "].";
		}

		return new StepCheckerResults(resultsDescription, isSuccess);
	}

}
