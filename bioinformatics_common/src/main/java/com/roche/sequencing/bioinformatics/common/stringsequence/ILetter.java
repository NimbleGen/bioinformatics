package com.roche.sequencing.bioinformatics.common.stringsequence;

public interface ILetter {
	int getScore();

	boolean matches(ILetter rhs);
}
