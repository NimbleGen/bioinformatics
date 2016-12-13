package com.roche.sequencing.bioinformatics.common.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

/**
 * A class for image utilities.
 * 
 */
public class ImageUtil {
	public enum RotationEnum {
		ROTATE_90_CLOCKWISE, ROTATE_180, ROTATE_270_CLOCKWISE, ROTATE_90_COUNTER_CLOCKWISE, ROTATE_270_COUNTER_CLOCKWISE
	};

	private ImageUtil() {
		throw new AssertionError();
	}

	/**
	 * 
	 * @param image
	 * @param angleInDegrees
	 * @return buffered image rotated with the values slightly changed due to interpolation
	 */

	public static BufferedImage rotate(BufferedImage image, double angleInDegrees) {
		double angleInRadians = Math.toRadians(angleInDegrees);
		double sin = Math.abs(Math.sin(angleInRadians)), cos = Math.abs(Math.cos(angleInRadians));
		double width = image.getWidth(), height = image.getHeight();
		double newWidth = (int) Math.floor(width * cos + height * sin), newHeight = (int) Math.floor(height * cos + width * sin);

		BufferedImage rotatedImage = new BufferedImage((int) newWidth, (int) newHeight, image.getType());

		Graphics2D graphics2D = rotatedImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.translate((double) (newWidth - width) / 2.0, (double) (newHeight - height) / 2.0);
		graphics2D.rotate(angleInRadians, width / 2.0, height / 2.0);
		graphics2D.drawRenderedImage(image, null);
		graphics2D.dispose();
		return rotatedImage;
	}

	/**
	 * 
	 * @param image
	 * @param rotation
	 * @return a rotated buffered image with the exact values
	 */
	public static BufferedImage rotate(BufferedImage image, RotationEnum rotation) {
		BufferedImage rotatedImage = null;

		int origWidth = image.getWidth();
		int origHeight = image.getHeight();

		if (rotation == RotationEnum.ROTATE_90_CLOCKWISE || rotation == RotationEnum.ROTATE_270_COUNTER_CLOCKWISE) {
			rotatedImage = new BufferedImage(image.getHeight(), image.getWidth(), image.getType());
			for (int origX = 0; origX < image.getWidth(); origX++) {
				for (int origY = 0; origY < image.getHeight(); origY++) {
					int newX = origHeight - origY - 1;
					int newY = origX;

					rotatedImage.getRaster().setSample(newX, newY, 0, image.getData().getSample(origX, origY, 0));
				}
			}
		} else if (rotation == RotationEnum.ROTATE_270_CLOCKWISE || rotation == RotationEnum.ROTATE_90_COUNTER_CLOCKWISE) {
			rotatedImage = new BufferedImage(image.getHeight(), image.getWidth(), image.getType());
			for (int origX = 0; origX < image.getWidth(); origX++) {
				for (int origY = 0; origY < image.getHeight(); origY++) {
					int newX = origY;
					int newY = origWidth - origX - 1;

					rotatedImage.getRaster().setSample(newX, newY, 0, image.getData().getSample(origX, origY, 0));
				}
			}
		} else if (rotation == RotationEnum.ROTATE_180) {
			rotatedImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
			for (int origX = 0; origX < image.getWidth(); origX++) {
				for (int origY = 0; origY < image.getHeight(); origY++) {
					int newX = origHeight - origY - 1;
					int newY = origWidth - origX - 1;

					rotatedImage.getRaster().setSample(newX, newY, 0, image.getData().getSample(origX, origY, 0));
				}
			}
		} else {
			throw new AssertionError();
		}

		return rotatedImage;
	}

	public static BufferedImage deepCopy(BufferedImage bufferedImage) {
		ColorModel cm = bufferedImage.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bufferedImage.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	public static BufferedImage scale(Image imageToScale, double scaleFactor) {
		BufferedImage scaledImage = null;
		if (imageToScale != null) {
			int newWidth = (int) (imageToScale.getWidth(null) * scaleFactor);
			int newHeight = (int) (imageToScale.getHeight(null) * scaleFactor);

			scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics2D = scaledImage.createGraphics();
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.drawImage(imageToScale, 0, 0, newWidth, newHeight, null);
			graphics2D.dispose();
		}
		return scaledImage;
	}
}
