package com.roche.sequencing.bioinformatics.common.utils;

import java.awt.Color;

public class ColorsUtil {

	/**
	 * taken and modified from java.awt.Color
	 * 
	 * @param color
	 * @param factor
	 *            (0 through 1 inclusive) a factor of zero returns the same color and a factor of 1 will return the brightest possible color of the provided color
	 * 
	 * @return
	 */
	public static Color getBrighterColor(Color color, double factor) {
		Color newColor = null;
		if (factor < 0 || factor > 1) {
			throw new IllegalStateException("The provided factor[" + factor + "] must be between 0 and 1 inclusive.");
		}
		factor = 1 - factor;

		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		int alpha = color.getAlpha();

		/*
		 * From 2D group: 1. black.brighter() should return grey 2. applying brighter to blue will always return blue, brighter 3. non pure color (non zero rgb) will eventually return white
		 */
		int i = (int) (1.0 / (1.0 - factor));
		if (r == 0 && g == 0 && b == 0) {
			newColor = new Color(i, i, i, alpha);
		} else {
			if (r > 0 && r < i) {
				r = i;
			}
			if (g > 0 && g < i) {
				g = i;
			}
			if (b > 0 && b < i) {
				b = i;
			}
			newColor = new Color(Math.min((int) (r / factor), 255), Math.min((int) (g / factor), 255), Math.min((int) (b / factor), 255), alpha);
		}

		return newColor;
	}

}
