package com.roche.sequencing.bioinformatics.common.graph.layout;

import java.awt.geom.Point2D;

public class EdgeConnectionPoints {
	private final Point2D toEdgeConnection;
	private final Point2D fromEdgeConnection;

	public EdgeConnectionPoints(Point2D toEdgeConnection, Point2D fromEdgeConnection) {
		super();
		this.toEdgeConnection = toEdgeConnection;
		this.fromEdgeConnection = fromEdgeConnection;
	}

	public Point2D getToEdgeConnection() {
		return toEdgeConnection;
	}

	public Point2D getFromEdgeConnection() {
		return fromEdgeConnection;
	}

	@Override
	public String toString() {
		return "EdgeConnectionPoints [toEdgeConnection=" + toEdgeConnection + ", fromEdgeConnection=" + fromEdgeConnection + "]";
	}

	public EdgeConnectionPoints scale(double scale) {
		Point2D scaledToEdgeConnection = new Point2D.Double(toEdgeConnection.getX() * scale, toEdgeConnection.getY() * scale);
		Point2D scaledFromEdgeConnection = new Point2D.Double(fromEdgeConnection.getX() * scale, fromEdgeConnection.getY() * scale);
		return new EdgeConnectionPoints(scaledToEdgeConnection, scaledFromEdgeConnection);
	}

	public EdgeConnectionPoints scaleWithOffset(double scale, int xOffset, int yOffset) {
		Point2D scaledToEdgeConnection = new Point2D.Double((toEdgeConnection.getX() * scale) + xOffset, (toEdgeConnection.getY() * scale) + yOffset);
		Point2D scaledFromEdgeConnection = new Point2D.Double((fromEdgeConnection.getX() * scale) + xOffset, (fromEdgeConnection.getY() * scale) + yOffset);
		return new EdgeConnectionPoints(scaledToEdgeConnection, scaledFromEdgeConnection);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fromEdgeConnection == null) ? 0 : fromEdgeConnection.hashCode());
		result = prime * result + ((toEdgeConnection == null) ? 0 : toEdgeConnection.hashCode());
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
		EdgeConnectionPoints other = (EdgeConnectionPoints) obj;
		if (fromEdgeConnection == null) {
			if (other.fromEdgeConnection != null)
				return false;
		} else if (!fromEdgeConnection.equals(other.fromEdgeConnection))
			return false;
		if (toEdgeConnection == null) {
			if (other.toEdgeConnection != null)
				return false;
		} else if (!toEdgeConnection.equals(other.toEdgeConnection))
			return false;
		return true;
	}
}
