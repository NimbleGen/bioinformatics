package com.roche.sequencing.bioinformatics.common.graph.layout;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import com.roche.sequencing.bioinformatics.common.ui.ImageMap;
import com.roche.sequencing.bioinformatics.common.utils.GraphicsUtil;
import com.roche.sequencing.bioinformatics.common.utils.LineStyle;

public class GraphDrawer {

	private final static int EDGE_OUTER_THICKNESS = 7;
	private final static int EDGE_INNER_THICKNESS = 1;
	private final static Color EDGE_OUTER_COLOR = Color.black;
	private final static Color EDGE_INNER_COLOR = Color.blue;

	private final Map<Class<?>, NodeDrawer<?>> nodeDrawerByClass;
	private final NodeDrawer<String> defaultNodeDrawer;
	private final int columnSpacingInPixels;
	private final int rowSpacingInPixels;
	private Dimension lastDrawnCellDimensions;

	public GraphDrawer(NodeDrawer<String> defaultNodeDrawer, int columnSpacingInPixels, int rowSpacingInPixels) {
		this.defaultNodeDrawer = defaultNodeDrawer;
		nodeDrawerByClass = new HashMap<Class<?>, NodeDrawer<?>>();
		this.columnSpacingInPixels = columnSpacingInPixels;
		this.rowSpacingInPixels = rowSpacingInPixels;
	}

	public void addNodeDrawer(NodeDrawer<?> nodeDrawer) {
		nodeDrawerByClass.put(nodeDrawer.getNodeClass(), nodeDrawer);
	}

	public Dimension getDrawnGraphDimensions(GraphLayout graphLayout) {
		Cell maxCell = graphLayout.getMaxCell();

		Graph graph = graphLayout.getGraph();
		Dimension cellDimensions = getCellDimensions(graph);

		int width = (maxCell.getColumn() + 1) * (int) cellDimensions.getWidth();
		int widthSpacing = (maxCell.getColumn() + 1) * columnSpacingInPixels;
		int height = (maxCell.getRow() + 1) * (int) cellDimensions.getHeight();
		int heightSpacing = (maxCell.getRow() + 1) * rowSpacingInPixels;
		return new Dimension(width + widthSpacing, height + heightSpacing);
	}

	public Dimension getCellDimensions(Graph graph) {
		int maxNodeWidth = 0;
		int maxNodeHeight = 0;

		for (Node<?> node : graph.getNodes()) {
			NodeDrawer<?> nodeDrawer = nodeDrawerByClass.get(node.getContents().getClass());
			if (nodeDrawer == null) {
				nodeDrawer = defaultNodeDrawer;
			}
			Dimension nodeSize = nodeDrawer.getNodeSize(node);
			maxNodeWidth = Math.max((int) nodeSize.getWidth(), maxNodeWidth);
			maxNodeHeight = Math.max((int) nodeSize.getHeight(), maxNodeHeight);

		}
		return new Dimension(maxNodeWidth, maxNodeHeight);
	}

	public GraphDrawingDetails drawGraph(Graphics2D graphics, GraphLayout graphLayout) {
		GraphicsUtil.setGraphicsHints(graphics);

		Graph graph = graphLayout.getGraph();
		lastDrawnCellDimensions = getCellDimensions(graph);
		ImageMap<Node<?>> nodeImageMap = new ImageMap<Node<?>>();
		ImageMap<String> clickKeysImageMap = new ImageMap<String>();

		Map<Node<?>, EdgeConnectionPoints> edgeConnectionPointsByNode = new HashMap<Node<?>, EdgeConnectionPoints>();
		for (Node<?> node : graph.getNodes()) {
			NodeDrawer<?> nodeDrawer = nodeDrawerByClass.get(node.getContents().getClass());
			if (nodeDrawer == null) {
				nodeDrawer = defaultNodeDrawer;
			}
			Cell cell = graphLayout.getNodesPositionInLayoutMatrix(node);
			if (cell != null) {
				Dimension nodeSize = nodeDrawer.getNodeSize(node);
				double yDiff = (lastDrawnCellDimensions.getHeight() - nodeSize.getHeight()) / 2.0;
				double xDiff = (lastDrawnCellDimensions.getWidth() - nodeSize.getWidth()) / 2.0;

				Point2D startPosition = new Point2D.Double(cell.getColumn() * (lastDrawnCellDimensions.getWidth() + columnSpacingInPixels) + xDiff, cell.getRow()
						* (lastDrawnCellDimensions.getHeight() + rowSpacingInPixels) + yDiff);
				NodeDrawingDetails details = nodeDrawer.drawNode(graphics, startPosition, node, null);
				ImageMap<String> clickKeys = details.getClickKeys();
				if (clickKeys != null) {
					clickKeysImageMap.add(details.getClickKeys());
				}
				EdgeConnectionPoints edgeConnectionPoints = details.getEdgeConnectionPoints();
				edgeConnectionPointsByNode.put(node, edgeConnectionPoints);
				nodeImageMap.addArea(node, (int) startPosition.getX(), (int) startPosition.getY(), (int) nodeSize.getWidth(), (int) nodeSize.getHeight());
			} else {
				System.out.println(node + "  " + node.getContents());
				throw new AssertionError();
			}
		}

		for (Node<?> fromNode : graph.getNodes()) {
			for (Node<?> toNode : graph.getConnectedToNodes(fromNode)) {
				Point2D fromPoint = edgeConnectionPointsByNode.get(fromNode).getFromEdgeConnection();
				Point2D toPoint = edgeConnectionPointsByNode.get(toNode).getToEdgeConnection();
				Shape originalClip = graphics.getClip();

				// note the thickness of the line causes it to overrun the destination point
				// so setting a clip prevents this bleeding of pixels into the node
				Line2D line = new Line2D.Double(fromPoint.getX(), 0, toPoint.getX(), Integer.MAX_VALUE);
				graphics.setClip(line.getBounds());

				graphics.setColor(EDGE_OUTER_COLOR);
				LineStyle.LINE_SOLID.applyToGraphics(graphics, EDGE_OUTER_THICKNESS);
				graphics.drawLine((int) fromPoint.getX(), (int) fromPoint.getY(), (int) toPoint.getX(), (int) toPoint.getY());

				graphics.setColor(EDGE_INNER_COLOR);
				LineStyle.LINE_SOLID.applyToGraphics(graphics, EDGE_INNER_THICKNESS);
				graphics.drawLine((int) fromPoint.getX(), (int) fromPoint.getY(), (int) toPoint.getX(), (int) toPoint.getY());

				graphics.setClip(originalClip);
			}
		}

		GraphDrawingDetails graphDrawingDetails = new GraphDrawingDetails(nodeImageMap, clickKeysImageMap);
		return graphDrawingDetails;
	}

	public void redrawNode(Graphics2D graphics, GraphLayout graphLayout, Node<?> node, Point2D mouseLocation) {
		NodeDrawer<?> nodeDrawer = nodeDrawerByClass.get(node.getContents().getClass());
		if (nodeDrawer == null) {
			nodeDrawer = defaultNodeDrawer;
		}
		Cell cell = graphLayout.getNodesPositionInLayoutMatrix(node);
		if (cell != null) {
			Dimension nodeSize = nodeDrawer.getNodeSize(node);
			double yDiff = (lastDrawnCellDimensions.getHeight() - nodeSize.getHeight()) / 2.0;
			double xDiff = (lastDrawnCellDimensions.getWidth() - nodeSize.getWidth()) / 2.0;

			Point2D startPosition = new Point2D.Double(cell.getColumn() * (lastDrawnCellDimensions.getWidth() + columnSpacingInPixels) + xDiff, cell.getRow()
					* (lastDrawnCellDimensions.getHeight() + rowSpacingInPixels) + yDiff);

			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
			graphics.fillRect((int) startPosition.getX(), (int) startPosition.getY(), (int) nodeSize.getWidth(), (int) nodeSize.getHeight());
			// reset composite
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

			nodeDrawer.drawNode(graphics, startPosition, node, mouseLocation);
		}
	}

	public void drawGraph(Graphics2D graphics, Graph graph) {
		GraphLayout graphLayout = GraphLayout.layoutGraph(graph);
		drawGraph(graphics, graphLayout);
	}

}
