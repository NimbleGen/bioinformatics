package com.roche.sequencing.bioinformatics.common.graph.layout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public abstract class NodeDrawer<T> {

	private final Class<?> nodeClass;

	public NodeDrawer(Class<?> nodeClass) {
		this.nodeClass = nodeClass;
	}

	public Class<?> getNodeClass() {
		return nodeClass;
	}

	public abstract Dimension getNodeSize(Node<?> node);

	public abstract NodeDrawingDetails drawNode(Graphics2D graphics, Point2D startPosition, Node<?> node, Point2D mousePointerLocation);

	protected void drawBackgroundAndBorder(Graphics2D graphics, Point2D startPosition, Node<?> node, Point2D mouseLocation, Dimension nodeSize) {
		// fill in background color

		graphics.setColor(Color.WHITE);
		graphics.fillRect((int) startPosition.getX(), (int) startPosition.getY(), (int) nodeSize.getWidth(), (int) nodeSize.getHeight());

		// fill in border color
		if (mouseLocation != null) {
			boolean isMousePointerHorizontallyContained = (mouseLocation.getX() > startPosition.getX()) && (mouseLocation.getX() < (startPosition.getX() + nodeSize.getWidth()));
			boolean isMousePointerVerticallyContained = (mouseLocation.getY() > startPosition.getY()) && (mouseLocation.getY() < (startPosition.getY() + nodeSize.getHeight()));
			boolean isMousePointerContained = isMousePointerHorizontallyContained && isMousePointerVerticallyContained;

			if (isMousePointerContained) {
				graphics.setColor(Color.GREEN);
			} else {
				graphics.setColor(Color.BLACK);
			}
		} else {
			graphics.setColor(Color.BLACK);
		}
		graphics.drawRect((int) startPosition.getX(), (int) startPosition.getY(), (int) nodeSize.getWidth(), (int) nodeSize.getHeight());
	}

	protected EdgeConnectionPoints getEdgeConnectionPoints(Point2D startPosition, Dimension nodeSize) {
		EdgeConnectionPoints edgeConnectionPoints = new EdgeConnectionPoints(new Point2D.Double(startPosition.getX(), startPosition.getY() + (nodeSize.getHeight() / 2)), new Point2D.Double(
				(startPosition.getX() + nodeSize.getWidth()), startPosition.getY() + (nodeSize.getHeight() / 2)));
		return edgeConnectionPoints;
	}

}
