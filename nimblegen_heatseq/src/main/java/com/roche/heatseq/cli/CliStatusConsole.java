package com.roche.heatseq.cli;

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
	}

	public static void logError(Throwable throwable) {
		// System.out.println(throwable.getMessage());
		System.err.println(throwable.getMessage());
		logger.error(throwable.getMessage(), throwable);
	}

}
