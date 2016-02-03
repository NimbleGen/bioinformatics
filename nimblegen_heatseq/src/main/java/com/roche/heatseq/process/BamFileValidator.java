/*
 *    Copyright 2016 Roche NimbleGen Inc.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class BamFileValidator {

	private BamFileValidator() {
		throw new AssertionError();
	}

	public static void validate(File bamOrSamFile) {
		if (!bamOrSamFile.exists()) {
			throw new IllegalStateException("Unable to find provided BAM or SAM file[" + bamOrSamFile.getAbsolutePath() + "].");
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(bamOrSamFile))) {
			reader.read();
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("The provided BAM or SAM file[" + bamOrSamFile.getAbsolutePath() + "] does not have read permissions.", e);
		} catch (IOException e) {
			throw new IllegalStateException("The provided BAM or SAM file[" + bamOrSamFile.getAbsolutePath() + "] does not have read permissions.", e);
		}

		if (bamOrSamFile.length() == 0) {
			throw new IllegalStateException("The provided BAM or SAM file[" + bamOrSamFile.getAbsolutePath() + "] is empty.");
		}
	}
}
