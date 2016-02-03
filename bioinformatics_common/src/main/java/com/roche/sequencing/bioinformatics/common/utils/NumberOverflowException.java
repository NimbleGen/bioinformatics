package com.roche.sequencing.bioinformatics.common.utils;

public class NumberOverflowException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NumberOverflowException() {
		super();
	}

	public NumberOverflowException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NumberOverflowException(String message, Throwable cause) {
		super(message, cause);
	}

	public NumberOverflowException(String message) {
		super(message);
	}

	public NumberOverflowException(Throwable cause) {
		super(cause);
	}

}
