package com.roche.sequencing.bioinformatics.common.utils;

public class LineParserException extends Exception {

	private static final long serialVersionUID = 1L;

	public LineParserException() {
		super();
	}

	public LineParserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public LineParserException(String message, Throwable cause) {
		super(message, cause);
	}

	public LineParserException(String message) {
		super(message);
	}

	public LineParserException(Throwable cause) {
		super(cause);
	}

}
