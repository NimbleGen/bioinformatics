package com.roche.sequencing.bioinformatics.common.utils;

import java.io.Serializable;

public enum LineStyle implements Serializable {

	LINE_SOLID(1), LINE_DASH(2), LINE_DOT(3), LINE_DASHDOT(4), LINE_DASHDOTDOT(5);

	private int value;

	private LineStyle(int value) {
		this.value = value;
	}

	public static LineStyle getByValue(int value) {
		LineStyle lineStyle = null;

		LineStyle[] styles = values();
		int i = 0;
		while (i < styles.length && lineStyle == null) {
			LineStyle currentStyle = styles[i];
			if (currentStyle.getValue() == value) {
				lineStyle = currentStyle;
			}
			i++;
		}

		if (lineStyle == null) {
			lineStyle = LineStyle.LINE_SOLID;
		}

		return lineStyle;
	}

	public int getValue() {
		return value;
	}
}
