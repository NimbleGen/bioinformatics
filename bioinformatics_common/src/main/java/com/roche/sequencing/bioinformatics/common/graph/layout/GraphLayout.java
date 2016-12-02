package com.roche.sequencing.bioinformatics.common.graph.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class GraphLayout {

	private final Map<Node<?>, Cell> nodeToMatrixPositionMap;
	private final Graph graph;

	private GraphLayout(Graph graph) {
		this.graph = graph;
		nodeToMatrixPositionMap = new HashMap<Node<?>, Cell>();
	}

	public Cell getNodesPositionInLayoutMatrix(Node<?> node) {
		return nodeToMatrixPositionMap.get(node);
	}

	public Graph getGraph() {
		return graph;
	}

	public Cell getMaxCell() {
		int maxRow = 0;
		int maxColumn = 0;
		for (Cell cell : nodeToMatrixPositionMap.values()) {
			maxRow = Math.max(cell.getRow(), maxRow);
			maxColumn = Math.max(cell.getColumn(), maxColumn);
		}
		return new Cell(maxRow, maxColumn);
	}

	public static GraphLayout layoutGraph(Graph graph) {
		GraphLayout graphLayout = new GraphLayout(graph);

		// find all the nodes that have no from nodes
		Set<Node<?>> startingNodes = new HashSet<Node<?>>();
		for (Node<?> node : graph.getNodes()) {
			System.out.println(node.getContents());
			if (graph.getConnectedFromNodes(node).isEmpty()) {
				startingNodes.add(node);
			}
		}

		Map<Node<?>, Integer> depthByNode = new HashMap<Node<?>, Integer>();
		Set<Node<?>> currentNodes = new HashSet<Node<?>>(startingNodes);

		int currentDepth = 1;
		while (currentNodes.size() > 0) {
			Set<Node<?>> nextNodes = new HashSet<Node<?>>();

			for (Node<?> fromNode : currentNodes) {
				for (Node<?> toNode : graph.getConnectedToNodes(fromNode)) {
					depthByNode.put(toNode, currentDepth);
					nextNodes.add(toNode);
				}
			}

			currentNodes = nextNodes;
			currentDepth++;
		}

		// assign depths to starting nodes
		for (Node<?> fromNode : startingNodes) {
			int maxDepth = 0;
			for (Node<?> toNode : graph.getConnectedToNodes(fromNode)) {
				maxDepth = Math.max(maxDepth, depthByNode.get(toNode) - 1);
			}
			depthByNode.put(fromNode, maxDepth);
		}

		// at this point each node should be assigned a depth
		int maxColumnIndex = 0;
		int mostRowsInAColumn = 0;
		Map<Integer, List<Node<?>>> nodesByDepth = new HashMap<Integer, List<Node<?>>>();
		for (Entry<Node<?>, Integer> entry : depthByNode.entrySet()) {
			Node<?> node = entry.getKey();
			int depth = entry.getValue();

			List<Node<?>> nodesAtDepth = nodesByDepth.get(depth);
			if (nodesAtDepth == null) {
				nodesAtDepth = new ArrayList<Node<?>>();
				nodesByDepth.put(depth, nodesAtDepth);
			}
			nodesAtDepth.add(node);
			maxColumnIndex = Math.max(maxColumnIndex, depth);
			mostRowsInAColumn = Math.max(mostRowsInAColumn, nodesAtDepth.size());
		}

		int numberOfRowsInLayout = mostRowsInAColumn;
		// the number of rows in layout must be odd
		if (mostRowsInAColumn % 2 == 0) {
			numberOfRowsInLayout++;
		}

		for (int cellColumn = 0; cellColumn < maxColumnIndex + 1; cellColumn++) {
			List<Node<?>> nodesAtColumn = nodesByDepth.get(cellColumn);
			Collections.sort(nodesAtColumn, new Comparator<Node<?>>() {
				@Override
				public int compare(Node<?> o1, Node<?> o2) {
					return Integer.compare(graph.getAddedIndexOfNode(o1), graph.getAddedIndexOfNode(o2));
				}
			});

			boolean oddManExists = nodesAtColumn.size() % 2 == 1;
			int startingIndex = (numberOfRowsInLayout - nodesAtColumn.size()) / 2;
			// int endingIndex = numberOfRowsInLayout-startingIndex;
			for (int index = 0; index < nodesAtColumn.size(); index++) {
				Node<?> node = nodesAtColumn.get(index);
				/**
				 * <pre>
				 * Odd Man In The Center
				 *       o o
				 *   o o o o
				 * o   o   o
				 *   o o o o
				 *       o o
				 * </pre>
				 */
				boolean isOddCenterMan = (oddManExists && (index == nodesAtColumn.size() / 2));
				int cellRow;
				if (isOddCenterMan) {
					cellRow = numberOfRowsInLayout / 2;
				} else {
					if (index < (nodesAtColumn.size() / 2)) {
						cellRow = startingIndex + index;
					} else {
						cellRow = numberOfRowsInLayout - startingIndex - (nodesAtColumn.size() - index);
					}

				}

				graphLayout.nodeToMatrixPositionMap.put(node, new Cell(cellRow, cellColumn));
			}
		}

		return graphLayout;
	}

	public void replaceNode(Node<?> nodeToReplace, Node<?> replacementNode) {
		graph.replaceNode(nodeToReplace, replacementNode);
		Cell cell = nodeToMatrixPositionMap.get(nodeToReplace);
		nodeToMatrixPositionMap.remove(nodeToReplace);
		nodeToMatrixPositionMap.put(replacementNode, cell);
	}
}
