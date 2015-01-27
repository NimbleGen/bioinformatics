package com.roche.sequencing.bioinformatics.common.utils;

import java.awt.Color;

public class ColorsUtil {

	public static Color getBrighterColor(Color color, double brightnessRatio) {
		if (brightnessRatio > 1.0) {
			throw new IllegalStateException("brightnessRatio must be less than or equal to 1.0");
		}
		if (brightnessRatio < 0.0) {
			throw new IllegalStateException("brightnessRatio must be greater than or equal to 0.0");
		}

		float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), new float[3]);
		double brightness = hsb[2];
		double newBrightness = brightness;

		Color newColor = color;
		if (brightnessRatio != 0.5) {
			if (brightnessRatio < 0.5) {
				double darkerRatio = brightnessRatio * 2;
				newBrightness = (darkerRatio * brightness);
			} else if (brightnessRatio > 0.5) {
				double brighterRatio = (brightnessRatio - 0.5) * 2;
				newBrightness = brightness + (brighterRatio * (1 - brightness));
			}
			int newRGB = Color.HSBtoRGB(hsb[0], hsb[1], (float) newBrightness);
			int red = (newRGB >> 16) & 0xFF;
			int green = (newRGB >> 8) & 0xFF;
			int blue = newRGB & 0xFF;
			newColor = new Color(red, green, blue, color.getAlpha());
		}
		return newColor;
	}
}
