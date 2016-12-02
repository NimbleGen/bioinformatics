package com.roche.sequencing.bioinformatics.common.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.swing.SwingUtilities2;

@SuppressWarnings("restriction")
public class GraphicsUtil {

	private final static Logger logger = LoggerFactory.getLogger(GraphicsUtil.class);
	private final static String NEWLINE_SEPARATOR = System.getProperty("line.separator");

	private GraphicsUtil() {
		throw new AssertionError();
	}

	/**
	 * returns the size of the text
	 * 
	 * @param text
	 * @param font
	 * @return new Dimension object with the size of the text
	 */

	public static Dimension getSizeOfTextInComponent(String text, Font font, JComponent component) {
		FontMetrics fontMetrics = SwingUtilities2.getFontMetrics(component, font);
		return new Dimension(fontMetrics.stringWidth(text), fontMetrics.getHeight());
	}

	private static JLabel emptyJLabel = null;

	/**
	 * If we haven't already, create a JLabel to use for measuring Strings
	 * 
	 * @return
	 */
	private static JLabel getEmptyJLabel() {
		if (emptyJLabel == null) {
			if (emptyJLabel == null) {
				try {
					if (SwingUtilities.isEventDispatchThread()) {
						emptyJLabel = new JLabel();
					} else {
						SwingUtilities.invokeAndWait(new Runnable() {
							@Override
							public void run() {
								emptyJLabel = new JLabel();
							}
						});
					}
				} catch (InterruptedException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Exception trying to create empty jLabel.", e);
					}
				} catch (InvocationTargetException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Exception trying to create empty jLabel.", e);
					}
				}
			}
		}
		return emptyJLabel;
	}

	/**
	 * Returns the size of the text (handles new lines)
	 * 
	 * @param text
	 * @param font
	 * @param component
	 * @return new Dimension object with the size of the text
	 */
	public static Dimension getSizeOfText(String text, Font font) {

		FontMetrics fontMetrics = SwingUtilities2.getFontMetrics(getEmptyJLabel(), font);
		String[] lines = text.split(NEWLINE_SEPARATOR);

		// count the number of newlines in text
		int numberOfLines = lines.length;
		int heightOfOneLine = fontMetrics.getHeight();
		int totalHeight = numberOfLines * heightOfOneLine;

		int maxWidth = 0;
		for (String line : lines) {
			maxWidth = Math.max(fontMetrics.stringWidth(line), maxWidth);
		}

		return new Dimension(maxWidth, totalHeight);

	}

	/**
	 * Find first instance of a JFrame. In some cases there are instances of Window which will cause the dialog to fail.
	 * 
	 * @param parentFrames
	 * @return
	 */
	public static JFrame getParentFrame(Frame[] parentFrames) {
		for (Frame frame : parentFrames) {
			if (frame instanceof JFrame) {
				return (JFrame) frame;
			}
		}
		return null;
	}

	public static void setLineStyle(Graphics2D graphics, LineStyle lineStyle, float lineWidth) {
		BasicStroke stroke = null;
		switch (lineStyle) {
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
		graphics.setStroke(stroke);
	}

	public void fillGradientRectangle(Graphics2D graphics, int x, int y, int width, int height, boolean verticalGradient) {
		Color color1 = graphics.getColor();
		Color color2 = graphics.getBackground();
		Float x1 = new Float(0);
		Float y1 = new Float(0);
		Float x2 = null;
		Float y2 = null;
		if (verticalGradient) {
			x1 = new Float(x + (width / 2));
			x2 = new Float(x + (width / 2));
			y2 = new Float(y + height);
		} else {
			x2 = new Float(x + width);
			y1 = new Float(y + (height / 2));
			y2 = new Float(y + (height / 2));
		}
		GradientPaint gradPaint = new GradientPaint(x1, y1, color1, x2, y2, color2, false);
		Paint oldPaint = graphics.getPaint();
		graphics.setPaint(gradPaint);
		graphics.fill(new Rectangle(x, y, width, height));
		graphics.setPaint(oldPaint);
	}

	public static Dimension getStringExtent(Font font, String text) {
		FontRenderContext frContext = new FontRenderContext(null, true, true);
		GlyphVector stringVector = font.createGlyphVector(frContext, text);
		Rectangle2D imgTemplate = stringVector.getPixelBounds(frContext, 0, 0);
		Dimension dimension = new Dimension((int) imgTemplate.getWidth(), (int) imgTemplate.getHeight());
		return dimension;
	}

	public static Dimension getStringExtent(Graphics2D graphics, String stringValue) {
		FontRenderContext frContext = new FontRenderContext(null, true, true);
		Font curFont = graphics.getFont();
		GlyphVector stringVector = curFont.createGlyphVector(frContext, stringValue);
		Rectangle2D imgTemplate = stringVector.getPixelBounds(frContext, 0, 0);
		Dimension extent = new Dimension((int) imgTemplate.getWidth(), (int) imgTemplate.getHeight());
		return extent;
	}

	public static Dimension getStringExtentForTransform(Graphics2D graphics, String stringValue) {
		FontRenderContext frContext = new FontRenderContext(null, true, true);
		Font curFont = graphics.getFont();
		GlyphVector stringVector = curFont.createGlyphVector(frContext, stringValue);
		Rectangle2D imgTemplate = stringVector.getPixelBounds(frContext, 0, 0);
		Dimension dimension = new Dimension((int) imgTemplate.getWidth(), (int) imgTemplate.getHeight());
		return dimension;
	}

	public static Dimension getStringDimensions(Graphics2D graphics, Font font, String string) {
		FontMetrics fontMetrics = graphics.getFontMetrics(font);
		int width = fontMetrics.stringWidth(string);
		int height = fontMetrics.getHeight();
		return new Dimension(width, height);
	}

	/**
	 * translate this transform by given offsets
	 * 
	 * @param offsetX
	 * @param offsetY
	 */
	public void translate(AffineTransform transform, float offsetX, float offsetY) {
		// if (swtTransform != null) {
		// swtTransform.translate(offsetX, offsetY);
		// } else
		if (transform != null) {
			transform.translate(offsetX, offsetY);
		}
	}

	/**
	 * rotate the given transform
	 * 
	 * @param angleInDegrees
	 */
	public static void rotate(AffineTransform transform, float angleInDegrees) {
		if (transform != null) {
			// the awt rotate requires radians so we need to convert
			// from degrees to radians
			double angleInRadians = angleInDegrees * (Math.PI / 180);
			transform.rotate(angleInRadians);
		}
	}

	private static double getAngleFromOriginInDegrees(Point point) {
		int quadrant = getQuadrantFromSigns(point);

		int quadrantDegrees = 0;
		double xyRatio = 0.0;

		double x = (double) Math.abs(point.getX());
		double y = (double) Math.abs(point.getY());

		if (quadrant == 1) {
			quadrantDegrees = 0;
			xyRatio = x / y;
		} else if (quadrant == 2) {
			quadrantDegrees = 90;
			xyRatio = y / x;
		} else if (quadrant == 3) {
			quadrantDegrees = 180;
			xyRatio = x / y;
		} else if (quadrant == 4) {
			quadrantDegrees = 270;
			xyRatio = y / x;
		} else {
			throw new IllegalStateException("unknown quadrant");
		}
		// tan theta = opposite / adjacent
		// tan angleFromOrigin = y / x or x/y
		// tan^-1(y/x or x/y) = angleFromOrigin
		double angleFromOriginInRadians = Math.atan(xyRatio);
		double angleFromOriginInDegrees = quadrantDegrees + Math.toDegrees(angleFromOriginInRadians);

		return angleFromOriginInDegrees;
	}

	private static int getQuadrantFromSigns(Point point) {
		int quadrant = 1;
		if (point.getX() >= 0 && point.getY() >= 0) {
			quadrant = 1;
		} else if (point.getX() >= 0 && point.getY() < 0) {
			quadrant = 2;
		} else if (point.getX() < 0 && point.getY() < 0) {
			quadrant = 3;
		} else if (point.getX() < 0 && point.getY() >= 0) {
			quadrant = 4;
		} else {
			IllegalStateException e = new IllegalStateException("cannot find quadrant from point.");
			throw e;
		}

		return quadrant;
	}

	public static Point getPointPositionOnRotatedTransform(Point originalPoint, double rotationInDegrees) {
		Point newPoint = null;

		// A^2 + B^2 = C^2
		double distanceFromOrigin = Math.sqrt((originalPoint.getX() * originalPoint.getX()) + (originalPoint.getY() * originalPoint.getY()));

		double angleFromOriginInDegrees = getAngleFromOriginInDegrees(originalPoint);

		double angle = angleFromOriginInDegrees + rotationInDegrees;
		int quadrant = (((int) (angleFromOriginInDegrees + rotationInDegrees) / 90) % 4) + 1;

		double newAngleFromOriginInDegrees = (angle % 90);
		double newAngleFromOriginInRadians = Math.toRadians(newAngleFromOriginInDegrees);
		double newX = distanceFromOrigin * Math.sin(newAngleFromOriginInRadians);
		double newY = distanceFromOrigin * Math.cos(newAngleFromOriginInRadians);

		int newIntX = (int) Math.round(newX);
		int newIntY = (int) Math.round(newY);

		if (quadrant == 1) {
			newPoint = new Point(newIntX, newIntY);
		} else if (quadrant == 2) {
			newPoint = new Point(newIntY, -newIntX);
		} else if (quadrant == 3) {
			newPoint = new Point(-newIntX, -newIntY);
		} else if (quadrant == 4) {
			newPoint = new Point(-newIntY, newIntX);
		} else {
			IllegalStateException e = new IllegalStateException("invalid quadrant.");
			throw e;
		}
		return newPoint;
	}

	public static Point getOriginalPositionFromRotatedTransform(Point transformPoint, double rotationInDegrees) {
		return getPointPositionOnRotatedTransform(transformPoint, 360 - rotationInDegrees);
	}

	public static void setGraphicsHints(Graphics2D graphics2D) {
		graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

}
