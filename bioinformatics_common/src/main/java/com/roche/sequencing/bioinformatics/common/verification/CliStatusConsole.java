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
package com.roche.sequencing.bioinformatics.common.verification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliStatusConsole {

	private static Logger logger = LoggerFactory.getLogger("console");

	private CliStatusConsole() {
		throw new AssertionError();
	}

	public static void logStatus(String statusMessage) {
		System.out.println(statusMessage);
		logger.info(statusMessage);
	}

	public static void logError(String errorMessage) {
		System.err.println(errorMessage);
		logger.error(errorMessage);
	}

	public static void logError(Throwable throwable) {
		if (throwable.getMessage() != null) {
			System.err.println(throwable.getMessage());
		} else {
			System.err.println("The following type of error was thrown:" + throwable.getClass().getName());
		}
		logger.error(throwable.getMessage(), throwable);
	}

}
