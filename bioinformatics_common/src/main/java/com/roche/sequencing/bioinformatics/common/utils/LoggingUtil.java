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
package com.roche.sequencing.bioinformatics.common.utils;

import java.io.File;
import java.io.IOException;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;

public class LoggingUtil {

	private static File[] currentLogFiles;

	private LoggingUtil() {
		throw new AssertionError();
	}

	public static void setLogFile(String loggerName, File logFile) throws IOException {
		setLogFiles(loggerName, new File[] { logFile });
	}

	public static void setLogFiles(String loggerName, File[] logFiles) throws IOException {

		for (File logFile : logFiles) {
			if (!logFile.exists()) {
				FileUtil.createNewFile(logFile);
			}
		}

		currentLogFiles = logFiles;

		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		// attach the rolling file appender to the logger of your choice
		Logger logbackLogger = loggerContext.getLogger(loggerName);
		logbackLogger.setLevel(Level.ALL);

		for (File logFile : logFiles) {
			FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
			fileAppender.setContext(loggerContext);
			fileAppender.setName(logFile.getName());
			fileAppender.setFile(logFile.getAbsolutePath());

			PatternLayoutEncoder encoder = new PatternLayoutEncoder();
			encoder.setContext(loggerContext);
			encoder.setPattern("%r %d{HH:mm:ss.SSS} - %msg%n");
			encoder.setImmediateFlush(true);
			encoder.start();

			fileAppender.setEncoder(encoder);
			fileAppender.start();

			logbackLogger.addAppender(fileAppender);
		}
	}

	public static File getLogFile() {
		File logFile = null;
		if (currentLogFiles != null && currentLogFiles.length > 0) {
			logFile = currentLogFiles[0];
		}
		return logFile;
	}

	public static void outputLogsToConsole(String loggerName) {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
		encoder.setPattern("%r %d{HH:mm:ss.SSS} - %msg%n");
		encoder.setImmediateFlush(true);
		encoder.start();

		ConsoleAppender<ILoggingEvent> logConsoleAppender = new ConsoleAppender<ILoggingEvent>();
		logConsoleAppender.setContext(loggerContext);
		logConsoleAppender.setName("console");
		logConsoleAppender.setEncoder(encoder);
		logConsoleAppender.start();

		Logger log = loggerContext.getLogger(loggerName);
		log.addAppender(logConsoleAppender);
	}

}
