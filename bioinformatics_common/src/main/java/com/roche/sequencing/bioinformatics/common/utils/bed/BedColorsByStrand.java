package com.roche.sequencing.bioinformatics.common.utils.bed;

import java.awt.Color;

import com.roche.sequencing.bioinformatics.common.sequence.Strand;

public class BedColorsByStrand {

	private final Color forwardStrandColor;
	private final Color reverseStrandColor;

	public BedColorsByStrand(Color forwardStrandColor, Color reverseStrandColor) {
		super();
		this.forwardStrandColor = forwardStrandColor;
		this.reverseStrandColor = reverseStrandColor;
	}

	public Color getForwardStrandColor() {
		return forwardStrandColor;
	}

	public Color getReverseStrandColor() {
		return reverseStrandColor;
	}

	public Color getColor(Strand strand) {
		Color color = null;
		if (strand == Strand.FORWARD) {
			color = forwardStrandColor;
		} else if (strand == Strand.REVERSE) {
			color = reverseStrandColor;
		} else {
			throw new AssertionError();
		}
		return color;
	}

}
