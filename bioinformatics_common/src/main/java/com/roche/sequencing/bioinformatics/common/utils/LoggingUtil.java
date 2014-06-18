package com.roche.sequencing.bioinformatics.common.utils;

import java.io.File;
import java.io.IOException;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

public class LoggingUtil {

	private LoggingUtil() {
		throw new AssertionError();
	}

	public static void setLogFile(File logFile) throws IOException {
		if (!logFile.exists()) {
			FileUtil.createNewFile(logFile);
		}

		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
		fileAppender.setContext(loggerContext);
		fileAppender.setName(logFile.getName());
		fileAppender.setFile(logFile.getAbsolutePath());

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
		encoder.setPattern("%r %thread %level - %msg%n");
		encoder.start();

		fileAppender.setEncoder(encoder);
		fileAppender.start();

		// attach the rolling file appender to the logger of your choice
		Logger logbackLogger = loggerContext.getLogger("root");
		logbackLogger.addAppender(fileAppender);
	}

}
