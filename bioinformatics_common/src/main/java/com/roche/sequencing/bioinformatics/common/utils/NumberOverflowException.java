package com.roche.sequencing.bioinformatics.common.utils;

public class NumberOverflowException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NumberOverflowException() {
		super();
	}

	NumberOverflowException(String message) {
		super(message);
	}

}
