/*
 *    Copyright 2013 Roche NimbleGen Inc.
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
package com.roche.heatseq.process;

import java.io.File;

import com.roche.heatseq.cli.DeduplicationCli;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class InputFilesExistValidator {

	private InputFilesExistValidator() {
		throw new AssertionError();
	}

	public static void validate(File fastqOne, File fastqTwo, File probeInfo) {
		validate(fastqOne, fastqTwo, probeInfo, null);
	}

	public static void validate(File fastqOne, File fastqTwo, File probeInfo, File bam) {
		boolean probeInfoNeededAndDoesNotExist = isNeededAndDoesNotExist(probeInfo);
		boolean fastqOneNeededAndDoesNotExist = isNeededAndDoesNotExist(fastqOne);
		boolean fastqTwoNeededAndDoesNotExist = isNeededAndDoesNotExist(fastqTwo);
		boolean bamNeededAndDoesNotExist = isNeededAndDoesNotExist(bam);

		if (probeInfoNeededAndDoesNotExist || fastqOneNeededAndDoesNotExist || fastqTwoNeededAndDoesNotExist || bamNeededAndDoesNotExist) {
			StringBuilder errorMessage = new StringBuilder();
			errorMessage.append("Unable to find the following provided file(s):" + StringUtil.NEWLINE);
			if (probeInfoNeededAndDoesNotExist) {
				errorMessage.append("  --" + DeduplicationCli.PROBE_OPTION.getLongFormOption() + " " + probeInfo.getAbsolutePath() + StringUtil.NEWLINE);
			}
			if (fastqOneNeededAndDoesNotExist) {
				errorMessage.append("  --" + DeduplicationCli.FASTQ_ONE_OPTION.getLongFormOption() + " " + fastqOne.getAbsolutePath() + StringUtil.NEWLINE);
			}
			if (fastqTwoNeededAndDoesNotExist) {
				errorMessage.append("  --" + DeduplicationCli.FASTQ_TWO_OPTION.getLongFormOption() + " " + fastqTwo.getAbsolutePath() + StringUtil.NEWLINE);
			}
			if (bamNeededAndDoesNotExist) {
				errorMessage.append("  --" + DeduplicationCli.INPUT_BAM_OPTION.getLongFormOption() + " " + bam.getAbsolutePath() + StringUtil.NEWLINE);
			}
			throw new IllegalStateException(errorMessage.toString());
		}
	}

	private static boolean isNeededAndDoesNotExist(File file) {
		boolean isNeededAndDoesNotExist = false;
		boolean isNeeded = file != null;
		if (isNeeded) {
			isNeededAndDoesNotExist = !file.exists();
		}
		return isNeededAndDoesNotExist;
	}

}
