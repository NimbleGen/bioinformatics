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

import com.roche.heatseq.cli.CliStatusConsole;
import com.roche.heatseq.objects.ParsedProbeFile;
import com.roche.heatseq.utils.ProbeFileUtil;
import com.roche.heatseq.utils.ProbeFileUtil.ProbeHeaderInformation;

public class ProbeInfoFileValidator {

	private ProbeInfoFileValidator() {
		throw new AssertionError();
	}

	public static ParsedProbeFile validateAndParseProbeInfoFile(File probeInfoFile) {
		if (!probeInfoFile.exists()) {
			throw new IllegalStateException("Unable to find provided PROBE file[" + probeInfoFile.getAbsolutePath() + "].");
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(probeInfoFile))) {
			reader.read();
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("The provided PROBE file[" + probeInfoFile.getAbsolutePath() + "] does not have read permissions.", e);
		} catch (IOException e) {
			throw new IllegalStateException("The provided PROBE file[" + probeInfoFile.getAbsolutePath() + "] does not have read permissions.", e);
		}

		if (probeInfoFile.length() == 0) {
			throw new IllegalStateException("The provided PROBE file[" + probeInfoFile.getAbsolutePath() + "] is empty.");
		}

		try {
			ProbeHeaderInformation probeHeaderInformation = ProbeFileUtil.extractProbeHeaderInformation(probeInfoFile);
			String md5SumFromHeader = probeHeaderInformation.getHeaderlessMd5Sum();
			if (md5SumFromHeader != null) {
				String calculatedMd5Sum = ProbeFileUtil.getHeaderlessMd5SumOfFile(probeInfoFile);
				if (!md5SumFromHeader.equals(calculatedMd5Sum)) {
					CliStatusConsole.logError("WARNING: The probe information found within the probe information file[" + probeInfoFile.getAbsolutePath()
							+ "] has been modified from the originally produced content supplied by Roche NimbleGen.");
				}
			}

		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Unable to find provided PROBE file[" + probeInfoFile.getAbsolutePath() + "].", e);
		}

		ParsedProbeFile probeInfo = null;
		try {
			probeInfo = ProbeFileUtil.parseProbeInfoFile(probeInfoFile);
			if (probeInfo.getProbes().size() == 0) {
				throw new IllegalStateException("The provided PROBE file[" + probeInfoFile.getAbsolutePath() + "] contains no probe entries.");
			}
		} catch (IOException e) {
			throw new IllegalStateException("Unable to parse the provided PROBE file[" + probeInfoFile.getAbsolutePath() + "].", e);
		}
		return probeInfo;
	}

}
