package com.roche.sequencing.bioinformatics.common.graph.layout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.File;

import com.roche.sequencing.bioinformatics.common.utils.LineStyle;

public abstract class NodeDrawer<T> {

	private final Class<?> nodeClass;

	public NodeDrawer(Class<?> nodeClass) {
		this.nodeClass = nodeClass;
	}

	public Class<?> getNodeClass() {
		return nodeClass;
	}

	public abstract int getMinFontSize();

	public abstract Dimension getNodeSize(Node<?> node, ResourceSizer resourceSizer);

	public abstract NodeDrawingDetails drawNode(Graphics2D graphics, Rectangle nodeBounds, Node<?> node, Point2D mousePointerLocation, ResourceSizer resourceSizer);

	public static void drawBackgroundAndBorder(Graphics2D graphics, Rectangle nodeBounds, Node<?> node, Point2D mouseLocation, Color backgroundColor, Color highlightBackgroundColor,
			Color borderColor, Color highlightBorderColor) {
		boolean isHighlighted = false;
		if (mouseLocation != null) {
			boolean isMousePointerHorizontallyContained = (mouseLocation.getX() > nodeBounds.getX()) && (mouseLocation.getX() < (nodeBounds.getX() + nodeBounds.getWidth()));
			boolean isMousePointerVerticallyContained = (mouseLocation.getY() > nodeBounds.getY()) && (mouseLocation.getY() < (nodeBounds.getY() + nodeBounds.getHeight()));
			boolean isMousePointerContained = isMousePointerHorizontallyContained && isMousePointerVerticallyContained;
			isHighlighted = isMousePointerContained;
		}

		LineStyle.LINE_SOLID.applyToGraphics(graphics, 1);

		graphics.setColor(Color.white);
		graphics.fillRect((int) nodeBounds.getX() + 1, (int) nodeBounds.getY() + 1, (int) nodeBounds.getWidth() - 2, (int) nodeBounds.getHeight() - 2);

		// fill in background color
		if (isHighlighted) {
			graphics.setColor(highlightBackgroundColor);
			graphics.fillRect((int) nodeBounds.getX() + 1, (int) nodeBounds.getY() + 1, (int) nodeBounds.getWidth() - 2, (int) nodeBounds.getHeight() - 2);
		} else {
			if (!backgroundColor.equals(Color.WHITE)) {
				graphics.setColor(backgroundColor);
				graphics.fillRect((int) nodeBounds.getX() + 1, (int) nodeBounds.getY() + 1, (int) nodeBounds.getWidth() - 2, (int) nodeBounds.getHeight() - 2);
			}
		}

		if (isHighlighted) {
			graphics.setColor(highlightBorderColor);
		} else {
			graphics.setColor(borderColor);
		}

		graphics.drawRect((int) nodeBounds.getX(), (int) nodeBounds.getY(), (int) nodeBounds.getWidth() - 1, (int) nodeBounds.getHeight() - 1);
	}

	public static EdgeConnectionPoints getEdgeConnectionPoints(Rectangle nodeBounds) {
		EdgeConnectionPoints edgeConnectionPoints = new EdgeConnectionPoints(new Point2D.Double(nodeBounds.getX(), nodeBounds.getY() + (nodeBounds.getHeight() / 2)), new Point2D.Double(
				(nodeBounds.getX() + nodeBounds.getWidth()), nodeBounds.getY() + (nodeBounds.getHeight() / 2)));
		return edgeConnectionPoints;
	}

	public static String getApplicableExtension(String[] extensions, File file) {
		String applicableExtension = null;
		if (file != null) {
			extensionLoop: for (String extension : extensions) {
				boolean endsWithAllowedExtension = file.getName().endsWith(extension);
				if (endsWithAllowedExtension) {
					applicableExtension = extension;
					break extensionLoop;
				}
			}
		}
		return applicableExtension;
	}

}
