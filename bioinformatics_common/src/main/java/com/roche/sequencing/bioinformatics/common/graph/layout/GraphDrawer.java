package com.roche.sequencing.bioinformatics.common.graph.layout;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

	// if these are set to be larger than the max node then
	// the jittering problem associated with constantly changing max
	// node sizes goes away
	private final int MIN_CELL_WIDTH = 450;
	private final int MIN_CELL_HEIGHT = 400;

	// this number must be equal to or less than 1/2
	private double ABSENT_MAN_IN_THE_MIDDLE_GRAVITY = 49.0 / 100.0;

	private Map<Integer, Dimension> lastCalculatedSizeByNodeHashCode;

	public GraphDrawer(NodeDrawer<String> defaultNodeDrawer, int columnSpacingInPixels, int rowSpacingInPixels) {
		this.defaultNodeDrawer = defaultNodeDrawer;
		nodeDrawerByClass = new HashMap<Class<?>, NodeDrawer<?>>();
		this.columnSpacingInPixels = columnSpacingInPixels;
		this.rowSpacingInPixels = rowSpacingInPixels;
		lastCalculatedSizeByNodeHashCode = new HashMap<Integer, Dimension>();
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
		int maxNodeWidth = MIN_CELL_WIDTH;
		int maxNodeHeight = MIN_CELL_HEIGHT;

		for (Node<?> node : graph.getNodes()) {
			Dimension nodeSize = getNodeDimensions(node);
			maxNodeWidth = Math.max((int) nodeSize.getWidth(), maxNodeWidth);
			maxNodeHeight = Math.max((int) nodeSize.getHeight(), maxNodeHeight);

		}
		return new Dimension(maxNodeWidth, maxNodeHeight);
	}

	public GraphDrawingDetails drawGraph(Graphics2D graphics, GraphLayout graphLayout, Point2D mouseLocation) {

		synchronized (graphLayout) {
			GraphicsUtil.setGraphicsHints(graphics);

			Graph graph = graphLayout.getGraph();
			Set<Node<?>> graphNodes = graph.getNodes();
			Dimension cellDimensions = getCellDimensions(graph);
			ImageMap<Object> nodeImageMap = new ImageMap<Object>();
			ImageMap<String> clickKeysImageMap = new ImageMap<String>();
			ImageMap<HyperlinkAction> hyperlinkActionsMap = new ImageMap<HyperlinkAction>();

			Map<Long, EdgeConnectionPoints> edgeConnectionPointsByNodeCreationIndex = new HashMap<Long, EdgeConnectionPoints>();
			for (Node<?> node : graphNodes) {
				NodeDrawingResults results = redrawNode(graphics, graphLayout, node, mouseLocation, cellDimensions);
				if (results != null) {
					NodeDrawingDetails details = results.getNodeDrawingDetails();
					ImageMap<String> clickKeys = details.getClickKeys();
					if (clickKeys != null) {
						clickKeysImageMap.add(clickKeys);
					}

					ImageMap<HyperlinkAction> hyperlinkActions = details.getHyperlinkActions();
					if (hyperlinkActions != null) {
						hyperlinkActionsMap.add(hyperlinkActions);
					}

					EdgeConnectionPoints edgeConnectionPoints = details.getEdgeConnectionPoints();
					edgeConnectionPointsByNodeCreationIndex.put(node.getCreationIndex(), edgeConnectionPoints);
					Rectangle nodeBounds = results.getNodeBounds();
					nodeImageMap.addArea(node.getContents(), (int) nodeBounds.getX(), (int) nodeBounds.getY(), (int) nodeBounds.getWidth(), (int) nodeBounds.getHeight());
				}
			}

			for (Node<?> fromNode : graphNodes) {
				for (Node<?> toNode : graph.getConnectedToNodes(fromNode)) {
					Point2D fromPoint = edgeConnectionPointsByNodeCreationIndex.get(fromNode.getCreationIndex()).getFromEdgeConnection();
					Point2D toPoint = edgeConnectionPointsByNodeCreationIndex.get(toNode.getCreationIndex()).getToEdgeConnection();
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

			GraphDrawingDetails graphDrawingDetails = new GraphDrawingDetails(nodeImageMap, clickKeysImageMap, hyperlinkActionsMap);
			return graphDrawingDetails;
		}
	}

	public NodeDrawingResults redrawNode(Graphics2D graphics, GraphLayout graphLayout, Object nodeObject, Point2D mouseLocation) {
		synchronized (graphLayout) {
			Graph graph = graphLayout.getGraph();
			Node<?> node = graph.getNode(nodeObject);
			return redrawNode(graphics, graphLayout, node, mouseLocation, null);
		}
	}

	public Rectangle getNodesBounds(Object nodeObject, GraphLayout graphLayout) {
		synchronized (graphLayout) {
			Graph graph = graphLayout.getGraph();
			Node<?> node = graph.getNode(nodeObject);
			return getNodesBounds(node, graphLayout, null);
		}
	}

	private NodeDrawer<?> getNodeDrawer(Node<?> node) {
		NodeDrawer<?> nodeDrawer = nodeDrawerByClass.get(node.getContents().getClass());
		if (nodeDrawer == null) {
			nodeDrawer = defaultNodeDrawer;
		}
		return nodeDrawer;
	}

	private Dimension getNodeDimensions(Node<?> node) {
		int nodeHashcode = node.hashCode();
		Dimension nodeSize = lastCalculatedSizeByNodeHashCode.get(nodeHashcode);
		if (nodeSize == null) {
			NodeDrawer<?> nodeDrawer = getNodeDrawer(node);
			if (nodeDrawer == null) {
				nodeDrawer = defaultNodeDrawer;
			}
			nodeSize = nodeDrawer.getNodeSize(node);
			lastCalculatedSizeByNodeHashCode.put(nodeHashcode, nodeSize);
		}

		return nodeSize;
	}

	public Rectangle getNodesBounds(Node<?> node, GraphLayout graphLayout, Dimension cellDimensions) {
		synchronized (graphLayout) {
			Rectangle nodeBounds = null;

			if (cellDimensions == null) {
				cellDimensions = getCellDimensions(graphLayout.getGraph());
			}
			Cell cell = graphLayout.getNodesPositionInLayoutMatrix(node);
			if (cell != null) {
				Dimension nodeSize = getNodeDimensions(node);
				double xDiff = (cellDimensions.getWidth() - nodeSize.getWidth()) / 2.0;
				double yDiff = (cellDimensions.getHeight() - nodeSize.getHeight()) / 2.0;

				double x = cell.getColumn() * (cellDimensions.getWidth() + columnSpacingInPixels) + xDiff;
				double y = cell.getRow() * (cellDimensions.getHeight() + rowSpacingInPixels) + yDiff;

				if (!graphLayout.doesContainingColumnContainManInTheMiddle(node)) {
					boolean isAboveCenter = graphLayout.isAboveCenter(node);
					if (isAboveCenter) {
						y += cellDimensions.getHeight() * ABSENT_MAN_IN_THE_MIDDLE_GRAVITY;
					} else {
						y -= cellDimensions.getHeight() * ABSENT_MAN_IN_THE_MIDDLE_GRAVITY;
					}
				}

				nodeBounds = new Rectangle((int) x, (int) y, (int) nodeSize.getWidth(), (int) nodeSize.getHeight());
			}

			return nodeBounds;
		}
	}

	private NodeDrawingResults redrawNode(Graphics2D graphics, GraphLayout graphLayout, Node<?> node, Point2D mouseLocation, Dimension cellDimensions) {
		NodeDrawingResults results = null;

		Rectangle nodeBounds = getNodesBounds(node, graphLayout, cellDimensions);
		NodeDrawer<?> nodeDrawer = getNodeDrawer(node);
		if (nodeBounds != null && nodeDrawer != null) {
			Shape oldClip = graphics.getClip();
			graphics.setClip(nodeBounds);

			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
			graphics.fillRect((int) nodeBounds.getX(), (int) nodeBounds.getY(), (int) nodeBounds.getWidth(), (int) nodeBounds.getHeight());
			// reset composite
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

			NodeDrawingDetails details = nodeDrawer.drawNode(graphics, nodeBounds, node, mouseLocation);
			graphics.setClip(oldClip);
			results = new NodeDrawingResults(details, new Rectangle((int) nodeBounds.getX(), (int) nodeBounds.getY(), (int) nodeBounds.getWidth(), (int) nodeBounds.getHeight()));
		}

		return results;
	}

	public void drawGraph(Graphics2D graphics, Graph graph) {
		GraphLayout graphLayout = GraphLayout.layoutGraph(graph);
		drawGraph(graphics, graphLayout, null);
	}

	private static class NodeDrawingResults {
		private final NodeDrawingDetails nodeDrawingDetails;
		private final Rectangle nodeBounds;

		public NodeDrawingResults(NodeDrawingDetails nodeDrawingDetails, Rectangle nodeBounds) {
			super();
			this.nodeDrawingDetails = nodeDrawingDetails;
			this.nodeBounds = nodeBounds;
		}

		public NodeDrawingDetails getNodeDrawingDetails() {
			return nodeDrawingDetails;
		}

		public Rectangle getNodeBounds() {
			return nodeBounds;
		}

	}

}
