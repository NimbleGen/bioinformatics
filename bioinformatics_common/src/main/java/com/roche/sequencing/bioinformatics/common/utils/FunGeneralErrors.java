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
package com.roche.sequencing.bioinformatics.common.utils;

import java.io.InputStream;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunGeneralErrors {

	private static Logger logger = LoggerFactory.getLogger(FunGeneralErrors.class);

	private static String[] funErrorsFromFile;
	private static String DEFAULT_ERROR = "There has been a problem.";
	private static boolean FUN_ERRORS_RETRIEVAL_ATTEMPTED = false;

	private FunGeneralErrors() {
		throw new AssertionError();
	}

	public static String getFunError() {
		String funError = DEFAULT_ERROR;
		if (!FUN_ERRORS_RETRIEVAL_ATTEMPTED) {
			retrieveFunErrorsFromFile();
		}

		if (funErrorsFromFile != null && funErrorsFromFile.length > 0) {
			Random random = new Random(System.currentTimeMillis());
			int randomIndex = random.nextInt(funErrorsFromFile.length);
			funError = funErrorsFromFile[randomIndex];
			if (funError.isEmpty()) {
				funError = DEFAULT_ERROR;
			}
		}

		return funError;
	}

	private static void retrieveFunErrorsFromFile() {
		FUN_ERRORS_RETRIEVAL_ATTEMPTED = true;
		try {
			InputStream stream = FunGeneralErrors.class.getResourceAsStream("fun_errors.txt");
			String fileAsString = FileUtil.readStreamAsString(stream);
			funErrorsFromFile = fileAsString.split(StringUtil.WINDOWS_NEWLINE);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
}
