package com.roche.sequencing.bioinformatics.common.graph.layout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

public class DefaultNodeDrawer extends NodeDrawer<String> {

	public DefaultNodeDrawer() {
		super(String.class);
	}

	@Override
	public NodeDrawingDetails drawNode(Graphics2D graphics, Rectangle nodeBounds, Node<?> node, Point2D mouseLocation) {
		graphics.setColor(Color.green);
		graphics.fillRect((int) nodeBounds.getX() + 5, (int) nodeBounds.getY() + 5, (int) nodeBounds.getWidth() - 10, (int) nodeBounds.getHeight() - 10);
		graphics.setColor(Color.red);
		graphics.drawRect((int) nodeBounds.getX() + 5, (int) nodeBounds.getY() + 5, (int) nodeBounds.getWidth() - 10, (int) nodeBounds.getHeight() - 10);
		String string = node.getContents().toString();
		graphics.drawString(string, (int) nodeBounds.getX() + 12, (int) nodeBounds.getY() + 22);
		EdgeConnectionPoints edgeConnectionPoints = new EdgeConnectionPoints(new Point2D.Double(nodeBounds.getX() + 5, nodeBounds.getY() + 15), new Point2D.Double(nodeBounds.getX() + 25,
				nodeBounds.getY() + 15));
		return new NodeDrawingDetails(edgeConnectionPoints, null, null);
	}

	@Override
	public Dimension getNodeSize(Node<?> node) {
		return new Dimension(30, 30);
	}

}
