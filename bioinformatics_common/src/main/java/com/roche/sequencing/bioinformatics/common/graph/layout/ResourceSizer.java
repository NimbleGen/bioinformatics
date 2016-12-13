package com.roche.sequencing.bioinformatics.common.graph.layout;

import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import com.roche.sequencing.bioinformatics.common.utils.ImageUtil;

public class ResourceSizer {
	private final int desiredMinFontSize;
	private final int difference;
	private final double scaleFactor;

	public ResourceSizer(int minFontSize, int desiredMinFontSize) {
		this.desiredMinFontSize = desiredMinFontSize;
		this.difference = desiredMinFontSize - minFontSize;
		this.scaleFactor = (double) desiredMinFontSize / (double) minFontSize;
	}

	public Font adjustFont(Font font) {
		return font.deriveFont((float) (font.getSize() + difference));
	}

	public ImageIcon adjustImageIcon(ImageIcon imageIcon) {
		BufferedImage scaledImage = ImageUtil.scale(imageIcon.getImage(), scaleFactor);
		return new ImageIcon(scaledImage);
	}

	public Image adjustImage(Image image) {
		BufferedImage scaledImage = ImageUtil.scale(image, scaleFactor);
		return scaledImage;
	}

	public int adjustLength(int length) {
		int adjustedLength = (int) (length * scaleFactor);
		return adjustedLength;
	}

	public int getDesiredMinFontSize() {
		return desiredMinFontSize;
	}

}
