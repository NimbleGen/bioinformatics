package com.roche.sequencing.bioinformatics.common.utils;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
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

	public void applyToGraphics(Graphics2D graphics2D, int lineWidth) {
		BasicStroke stroke = null;
		switch (this) {
		case LINE_SOLID:
			stroke = new BasicStroke(lineWidth);
			break;
		case LINE_DOT:
			stroke = new BasicStroke((float) lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] { 2f }, 0f);
			break;
		case LINE_DASH:
			stroke = new BasicStroke((float) lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] { 8.0f, 3.0f }, 0f);
			break;
		case LINE_DASHDOT:
			stroke = new BasicStroke((float) lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] { 8.0f, 3.0f, 2.0f, 3.0f }, 0f);
			break;
		case LINE_DASHDOTDOT:
			stroke = new BasicStroke((float) lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] { 8.0f, 3.0f, 2.0f, 3.0f, 2.0f, 3.0f }, 0f);
			break;
		default:
			stroke = new BasicStroke((float) lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] { 2f }, 0f);
			break;
		}
		graphics2D.setStroke(stroke);
	}
}
