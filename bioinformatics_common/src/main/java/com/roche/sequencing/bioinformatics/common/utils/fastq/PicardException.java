package com.roche.sequencing.bioinformatics.common.utils.fastq;

public class PicardException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	PicardException(final String message) {
		super(message);
	}

	public PicardException(final String message, final Throwable throwable) {
		super(message, throwable);
	}

}
