package com.roche.sequencing.bioinformatics.common.stringsequence.alignment;

import com.roche.sequencing.bioinformatics.common.stringsequence.ILetter;

public class Gap implements ILetter {

	private final static String STRING_FOR_GAP = "_";
	public final static Gap GAP = new Gap();

	private Gap() {
	}

	@Override
	public int getScore() {
		return 0;
	}

	public String toString() {
		return STRING_FOR_GAP;
	}
}
