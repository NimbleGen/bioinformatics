package com.roche.sequencing.bioinformatics.common.graph.layout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public class DefaultNodeDrawer extends NodeDrawer<String> {

	public DefaultNodeDrawer() {
		super(String.class);
	}

	@Override
	public NodeDrawingDetails drawNode(Graphics2D graphics, Point2D startPosition, Node<?> node, Point2D mouseLocation) {
		Dimension size = getNodeSize(node);
		graphics.setColor(Color.green);
		graphics.fillRect((int) startPosition.getX() + 5, (int) startPosition.getY() + 5, (int) size.getWidth() - 10, (int) size.getHeight() - 10);
		graphics.setColor(Color.red);
		graphics.drawRect((int) startPosition.getX() + 5, (int) startPosition.getY() + 5, (int) size.getWidth() - 10, (int) size.getHeight() - 10);
		String string = node.getContents().toString();
		graphics.drawString(string, (int) startPosition.getX() + 12, (int) startPosition.getY() + 22);
		EdgeConnectionPoints edgeConnectionPoints = new EdgeConnectionPoints(new Point2D.Double(startPosition.getX() + 5, startPosition.getY() + 15), new Point2D.Double(startPosition.getX() + 25,
				startPosition.getY() + 15));
		return new NodeDrawingDetails(edgeConnectionPoints, null);
	}

	@Override
	public Dimension getNodeSize(Node<?> node) {
		return new Dimension(30, 30);
	}

}
