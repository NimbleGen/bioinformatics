package com.roche.sequencing.bioinformatics.common.utils.bed;

import java.awt.Color;

public class RGB {
	private final int red;
	private final int green;
	private final int blue;

	public RGB(int red, int green, int blue) {
		super();
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	public RGB(String commaSeparatedRgbValues) {
		String[] splitRgb = commaSeparatedRgbValues.split(",");
		if (splitRgb.length != 3) {
			throw new IllegalStateException("The provided value for the itemRgb[" + commaSeparatedRgbValues + "] is not a valid comma separated RGB value.");
		}
		try {
			this.red = Integer.parseInt(splitRgb[0]);
			this.green = Integer.parseInt(splitRgb[1]);
			this.blue = Integer.parseInt(splitRgb[2]);
		} catch (NumberFormatException e) {
			throw new IllegalStateException("The provided value for the itemRgb[" + commaSeparatedRgbValues + "] is not a valid comma separated RGB value.");
		}
	}

	public int getRed() {
		return red;
	}

	public int getGreen() {
		return green;
	}

	public int getBlue() {
		return blue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + blue;
		result = prime * result + green;
		result = prime * result + red;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RGB other = (RGB) obj;
		if (blue != other.blue)
			return false;
		if (green != other.green)
			return false;
		if (red != other.red)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RGB [red=" + red + ", green=" + green + ", blue=" + blue + "]";
	}

	public Color getColor() {
		return new Color(red, green, blue);
	}

}
