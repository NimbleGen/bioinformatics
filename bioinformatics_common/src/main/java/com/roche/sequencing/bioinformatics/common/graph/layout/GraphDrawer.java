package com.roche.sequencing.bioinformatics.common.graph.layout;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.plaf.FontUIResource;

import com.roche.sequencing.bioinformatics.common.ui.ImageMap;
import com.roche.sequencing.bioinformatics.common.utils.GraphicsUtil;
import com.roche.sequencing.bioinformatics.common.utils.LineStyle;

public class GraphDrawer {

	private final static int EDGE_OUTER_THICKNESS = 7;
	private final static int EDGE_INNER_THICKNESS = 1;
	private final static Color EDGE_OUTER_COLOR = Color.black;
	private final static Color EDGE_INNER_COLOR = Color.blue;

	private final Map<Graph, Integer> cachedMinFontSize;

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

	private Map<Integer, Dimension> lastCalculatedSizeByNodeAndMinFontSizeHashCode;

	public GraphDrawer(NodeDrawer<String> defaultNodeDrawer, int columnSpacingInPixels, int rowSpacingInPixels) {
		this.defaultNodeDrawer = defaultNodeDrawer;
		nodeDrawerByClass = new HashMap<Class<?>, NodeDrawer<?>>();
		this.columnSpacingInPixels = columnSpacingInPixels;
		this.rowSpacingInPixels = rowSpacingInPixels;
		lastCalculatedSizeByNodeAndMinFontSizeHashCode = new HashMap<Integer, Dimension>();
		cachedMinFontSize = new HashMap<Graph, Integer>();
	}

	public void addNodeDrawer(NodeDrawer<?> nodeDrawer) {
		nodeDrawerByClass.put(nodeDrawer.getNodeClass(), nodeDrawer);
		cachedMinFontSize.clear();
	}

	private int calculateMinFontSize(Graph graph) {
		int minFontSize = Integer.MAX_VALUE;
		for (Node<?> node : graph.getNodes()) {
			NodeDrawer<?> nodeDrawer = getNodeDrawer(node);
			minFontSize = Math.min(minFontSize, nodeDrawer.getMinFontSize());
		}
		return minFontSize;
	}

	private int getMinFontSize(Graph graph) {
		Integer minFontSize = cachedMinFontSize.get(graph);
		if (minFontSize == null) {
			minFontSize = calculateMinFontSize(graph);
			cachedMinFontSize.put(graph, minFontSize);
		}
		return minFontSize;
	}

	private ResourceSizer getResourceSizer(Graph graph, int desiredMinFontSize) {
		int minFontSize = getMinFontSize(graph);
		ResourceSizer resourceSize = new ResourceSizer(minFontSize, desiredMinFontSize);
		return resourceSize;
	}

	public Dimension getDrawnGraphDimensions(GraphLayout graphLayout, int desiredMinFontSize) {
		Cell maxCell = graphLayout.getMaxCell();

		Graph graph = graphLayout.getGraph();

		ResourceSizer resourceSizer = getResourceSizer(graph, desiredMinFontSize);

		Dimension cellDimensions = getCellDimensions(graph.getNodes(), resourceSizer);

		int width = (maxCell.getColumn() + 1) * (int) cellDimensions.getWidth();
		int widthSpacing = (maxCell.getColumn() + 1) * columnSpacingInPixels;
		int height = (maxCell.getRow() + 1) * (int) cellDimensions.getHeight();
		int heightSpacing = (maxCell.getRow() + 1) * rowSpacingInPixels;
		return new Dimension(width + widthSpacing, height + heightSpacing);
	}

	private Dimension getCellDimensions(Set<Node<?>> nodes, ResourceSizer resourceSizer) {

		int maxNodeWidth = resourceSizer.adjustLength(MIN_CELL_WIDTH);
		int maxNodeHeight = resourceSizer.adjustLength(MIN_CELL_HEIGHT);

		for (Node<?> node : nodes) {
			Dimension nodeSize = getNodeDimensions(node, resourceSizer);
			maxNodeWidth = Math.max((int) nodeSize.getWidth(), maxNodeWidth);
			maxNodeHeight = Math.max((int) nodeSize.getHeight(), maxNodeHeight);

		}
		return new Dimension(maxNodeWidth, maxNodeHeight);
	}

	public GraphDrawingDetails drawGraph(Graphics2D graphics, GraphLayout graphLayout, Point2D mouseLocation, int desiredMinFontSize) {

		synchronized (graphLayout) {
			GraphicsUtil.setGraphicsHints(graphics);

			Graph graph = graphLayout.getGraph();

			Set<Node<?>> graphNodes = graph.getNodes();

			ResourceSizer resourceSizer = getResourceSizer(graph, desiredMinFontSize);

			Dimension cellDimensions = getCellDimensions(graphNodes, resourceSizer);
			ImageMap<Object> nodeImageMap = new ImageMap<Object>();
			ImageMap<String> clickKeysImageMap = new ImageMap<String>();
			ImageMap<HyperlinkAction> hyperlinkActionsMap = new ImageMap<HyperlinkAction>();

			Map<Long, EdgeConnectionPoints> edgeConnectionPointsByNodeCreationIndex = new HashMap<Long, EdgeConnectionPoints>();
			for (Node<?> node : graphNodes) {
				NodeDrawingResults results = redrawNode(graphics, graphLayout, node, mouseLocation, cellDimensions, desiredMinFontSize);
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

			int edgeOuterThickness = resourceSizer.adjustLength(EDGE_OUTER_THICKNESS);
			int edgeInnerThickness = resourceSizer.adjustLength(EDGE_INNER_THICKNESS);

			for (Node<?> fromNode : graphNodes) {
				for (Node<?> toNode : graph.getConnectedToNodes(fromNode)) {
					EdgeConnectionPoints fromNodeEdgeConnectionPoints = edgeConnectionPointsByNodeCreationIndex.get(fromNode.getCreationIndex());
					if (fromNodeEdgeConnectionPoints == null) {
						throw new IllegalStateException("Could not find edge connection points for from node:" + fromNode);
					}
					Point2D fromPoint = edgeConnectionPointsByNodeCreationIndex.get(fromNode.getCreationIndex()).getFromEdgeConnection();

					EdgeConnectionPoints toNodeEdgeConnectionPoints = edgeConnectionPointsByNodeCreationIndex.get(toNode.getCreationIndex());
					if (toNodeEdgeConnectionPoints == null) {
						throw new IllegalStateException("Could not find edge connection points for to node:" + toNode);
					}
					Point2D toPoint = toNodeEdgeConnectionPoints.getToEdgeConnection();

					Shape originalClip = graphics.getClip();

					// note the thickness of the line causes it to overrun the destination point
					// so setting a clip prevents this bleeding of pixels into the node
					Line2D line = new Line2D.Double(fromPoint.getX(), 0, toPoint.getX(), Integer.MAX_VALUE);
					graphics.setClip(line.getBounds());

					graphics.setColor(EDGE_OUTER_COLOR);
					LineStyle.LINE_SOLID.applyToGraphics(graphics, edgeOuterThickness);
					graphics.drawLine((int) fromPoint.getX(), (int) fromPoint.getY(), (int) toPoint.getX(), (int) toPoint.getY());

					graphics.setColor(EDGE_INNER_COLOR);
					LineStyle.LINE_SOLID.applyToGraphics(graphics, edgeInnerThickness);
					graphics.drawLine((int) fromPoint.getX(), (int) fromPoint.getY(), (int) toPoint.getX(), (int) toPoint.getY());

					graphics.setClip(originalClip);
				}
			}

			GraphDrawingDetails graphDrawingDetails = new GraphDrawingDetails(nodeImageMap, clickKeysImageMap, hyperlinkActionsMap);
			return graphDrawingDetails;
		}
	}

	public NodeDrawingResults redrawNode(Graphics2D graphics, GraphLayout graphLayout, Object nodeObject, Point2D mouseLocation, int desiredMinFontSize) {
		synchronized (graphLayout) {
			Graph graph = graphLayout.getGraph();
			Node<?> node = graph.getNode(nodeObject);
			return redrawNode(graphics, graphLayout, node, mouseLocation, null, desiredMinFontSize);
		}
	}

	public Rectangle getNodesBounds(Object nodeObject, GraphLayout graphLayout, int desiredMinFontSize) {
		synchronized (graphLayout) {
			Graph graph = graphLayout.getGraph();
			Node<?> node = graph.getNode(nodeObject);

			ResourceSizer resourceSizer = getResourceSizer(graph, desiredMinFontSize);

			return getNodesBounds(node, graphLayout, null, resourceSizer);
		}
	}

	private NodeDrawer<?> getNodeDrawer(Node<?> node) {
		NodeDrawer<?> nodeDrawer = nodeDrawerByClass.get(node.getContents().getClass());
		if (nodeDrawer == null) {
			nodeDrawer = defaultNodeDrawer;
		}
		return nodeDrawer;
	}

	private Dimension getNodeDimensions(Node<?> node, ResourceSizer resourceSizer) {
		int nodeAndMinFontSizeHashcode = getNodeAndMinFontSizeHash(node, resourceSizer.getDesiredMinFontSize());
		Dimension nodeSize = lastCalculatedSizeByNodeAndMinFontSizeHashCode.get(nodeAndMinFontSizeHashcode);
		if (nodeSize == null) {
			NodeDrawer<?> nodeDrawer = getNodeDrawer(node);
			if (nodeDrawer == null) {
				nodeDrawer = defaultNodeDrawer;
			}
			nodeSize = nodeDrawer.getNodeSize(node, resourceSizer);
			lastCalculatedSizeByNodeAndMinFontSizeHashCode.put(nodeAndMinFontSizeHashcode, nodeSize);
		}

		return nodeSize;
	}

	private Rectangle getNodesBounds(Node<?> node, GraphLayout graphLayout, Dimension cellDimensions, ResourceSizer resourceSizer) {
		synchronized (graphLayout) {
			Rectangle nodeBounds = null;

			if (cellDimensions == null) {
				cellDimensions = getCellDimensions(graphLayout.getGraph().getNodes(), resourceSizer);
			}
			Cell cell = graphLayout.getNodesPositionInLayoutMatrix(node);
			if (cell != null) {
				Dimension nodeSize = getNodeDimensions(node, resourceSizer);
				double xDiff = (cellDimensions.getWidth() - nodeSize.getWidth()) / 2.0;
				double yDiff = (cellDimensions.getHeight() - nodeSize.getHeight()) / 2.0;

				double x = cell.getColumn() * (cellDimensions.getWidth() + resourceSizer.adjustLength(columnSpacingInPixels)) + xDiff;
				double y = cell.getRow() * (cellDimensions.getHeight() + resourceSizer.adjustLength(rowSpacingInPixels)) + yDiff;

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

	private NodeDrawingResults redrawNode(Graphics2D graphics, GraphLayout graphLayout, Node<?> node, Point2D mouseLocation, Dimension cellDimensions, int desiredMinFontSize) {
		NodeDrawingResults results = null;

		Graph graph = graphLayout.getGraph();
		ResourceSizer resourceSizer = getResourceSizer(graph, desiredMinFontSize);

		Rectangle nodeBounds = getNodesBounds(node, graphLayout, cellDimensions, resourceSizer);
		NodeDrawer<?> nodeDrawer = getNodeDrawer(node);
		if (nodeBounds != null && nodeDrawer != null) {
			Shape oldClip = graphics.getClip();
			graphics.setClip(nodeBounds);

			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
			graphics.fillRect((int) nodeBounds.getX(), (int) nodeBounds.getY(), (int) nodeBounds.getWidth(), (int) nodeBounds.getHeight());
			// reset composite
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

			NodeDrawingDetails details = nodeDrawer.drawNode(graphics, nodeBounds, node, mouseLocation, resourceSizer);
			graphics.setClip(oldClip);
			results = new NodeDrawingResults(details, new Rectangle((int) nodeBounds.getX(), (int) nodeBounds.getY(), (int) nodeBounds.getWidth(), (int) nodeBounds.getHeight()));
		}

		return results;
	}

	public void drawGraph(Graphics2D graphics, Graph graph, int desiredMinFontSize) {
		GraphLayout graphLayout = GraphLayout.layoutGraph(graph);
		drawGraph(graphics, graphLayout, null, desiredMinFontSize);
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

	public static void main(String[] args) {
		Font TITLE_FONT = new FontUIResource("Serif", Font.BOLD, 16);
		for (int i = 1; i < 26; i++) {
			Dimension textSize = GraphicsUtil.getSizeOfText("H", TITLE_FONT.deriveFont(Font.BOLD, i));
			System.out.println("font:" + i + "  " + textSize);
		}
	}

	private static int getNodeAndMinFontSizeHash(Node<?> node, int minFontSize) {
		final int prime = 31;
		int result = 1;
		result = prime * result + node.hashCode();
		result = prime * result + minFontSize;
		return result;
	}

}
