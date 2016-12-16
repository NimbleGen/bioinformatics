/*
 *    Copyright 2016 Roche NimbleGen Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.roche.sequencing.bioinformatics.common.utils;

import java.awt.Color;
import java.util.Random;

public class ColorsUtil {

	private final static Random random = new Random();
	private final static float MAX_COLOR_VALUE = 255f;

	private ColorsUtil() {
		throw new AssertionError();
	}

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

	public static Color getRandomPastelColor() {
		float hue = random.nextFloat();
		// Saturation between 0.1 and 0.3
		float saturation = (random.nextInt(2000) + 1000) / 10000f;
		float luminance = 0.9f;
		Color color = Color.getHSBColor(hue, saturation, luminance);
		return color;
	}

	public static Color getRandomRainbowColor() {
		int R = (int) (Math.random() * 256);
		int G = (int) (Math.random() * 256);
		int B = (int) (Math.random() * 256);
		Color color = new Color(R, G, B);
		return color;
	}

	public static Color[] getDisplayableColors() {
		// these color were generated using http://phrogz.net/css/distinct-colors.html
		Color[] displayableColors = new Color[] { new Color(229, 115, 115), new Color(115, 107, 0), new Color(0, 204, 255), new Color(140, 35, 133), new Color(51, 26, 26), new Color(188, 191, 143),
				Color.black, new Color(0, 34, 51), new Color(255, 128, 246), new Color(102, 36, 26), new Color(43, 51, 26), new Color(83, 127, 166), new Color(51, 0, 34), new Color(242, 153, 121),
				new Color(157, 242, 61), new Color(191, 208, 255), new Color(191, 48, 105), new Color(204, 109, 0), new Color(17, 128, 0), new Color(128, 145, 255), new Color(140, 105, 119),
				new Color(102, 54, 0), new Color(121, 242, 153), new Color(57, 57, 230), new Color(89, 22, 40), new Color(51, 33, 13), new Color(115, 153, 130), new Color(72, 64, 128),
				new Color(242, 182, 198), new Color(242, 214, 182), new Color(0, 77, 41), new Color(70, 67, 89), new Color(217, 0, 29), new Color(89, 79, 67), new Color(64, 255, 242),
				new Color(49, 22, 89), new Color(255, 170, 0), new Color(19, 77, 73), new Color(175, 143, 191), new Color(204, 190, 0), new Color(38, 145, 153), new Color(173, 0, 217) };
		return displayableColors;
	}

	public static Color addAlpha(Color color, float alpha) {
		return new Color(color.getRed() / MAX_COLOR_VALUE, color.getGreen() / MAX_COLOR_VALUE, color.getBlue() / MAX_COLOR_VALUE, alpha);
	}

	public static Color addAlpha(Color color, int alpha) {
		return addAlpha(color, (float) alpha / 255f);
	}
}
