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
}
