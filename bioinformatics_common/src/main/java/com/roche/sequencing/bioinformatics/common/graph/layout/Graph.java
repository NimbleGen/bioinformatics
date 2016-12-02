package com.roche.sequencing.bioinformatics.common.graph.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph {

	private final List<Node<?>> nodes;
	private Map<Node<?>, Integer> cachedNodeToIndexMap;
	private final Map<Integer, Set<Integer>> fromNodeToToNodeMap;
	private final Map<Integer, Set<Integer>> toNodeToFromNodeMap;

	public Graph() {
		fromNodeToToNodeMap = new HashMap<Integer, Set<Integer>>();
		toNodeToFromNodeMap = new HashMap<Integer, Set<Integer>>();
		nodes = new ArrayList<Node<?>>();
	}

	public void connectNodes(Node<?> fromNode, Node<?> toNode) {
		int fromNodeIndex = nodes.indexOf(fromNode);
		if (fromNodeIndex < 0) {
			fromNodeIndex = nodes.size();
			nodes.add(fromNode);
			cachedNodeToIndexMap = null;
		}

		int toNodeIndex = nodes.indexOf(toNode);
		if (toNodeIndex < 0) {
			toNodeIndex = nodes.size();
			nodes.add(toNode);
			cachedNodeToIndexMap = null;
		}

		Set<Integer> toNodes = fromNodeToToNodeMap.get(fromNodeIndex);
		if (toNodes == null) {
			toNodes = new HashSet<Integer>();
			fromNodeToToNodeMap.put(fromNodeIndex, toNodes);
		}
		toNodes.add(toNodeIndex);

		Set<Integer> fromNodes = toNodeToFromNodeMap.get(toNodeIndex);
		if (fromNodes == null) {
			fromNodes = new HashSet<Integer>();
			toNodeToFromNodeMap.put(toNodeIndex, fromNodes);
		}
		fromNodes.add(fromNodeIndex);
	}

	public Set<Node<?>> getConnectedToNodes(Node<?> fromNode) {
		Set<Node<?>> toNodes;
		Integer fromNodeIndex = getAddedIndexOfNode(fromNode);
		Set<Integer> toNodeIndexes = fromNodeToToNodeMap.get(fromNodeIndex);
		if (toNodeIndexes == null) {
			toNodes = Collections.emptySet();
		} else {
			toNodes = new HashSet<Node<?>>();
			for (Integer index : toNodeIndexes) {
				toNodes.add(nodes.get(index));
			}
		}
		return toNodes;
	}

	public Set<Node<?>> getConnectedFromNodes(Node<?> toNode) {
		Set<Node<?>> fromNodes;
		Integer toNodeIndex = getAddedIndexOfNode(toNode);
		Set<Integer> fromNodeIndexes = toNodeToFromNodeMap.get(toNodeIndex);
		if (fromNodeIndexes == null) {
			fromNodes = Collections.emptySet();
		} else {
			fromNodes = new HashSet<Node<?>>();
			for (Integer index : fromNodeIndexes) {
				fromNodes.add(nodes.get(index));
			}
		}
		return fromNodes;
	}

	public Set<Node<?>> getNodes() {
		return new HashSet<Node<?>>(nodes);
	}

	private void createCachedNodeToIndexMap() {
		cachedNodeToIndexMap = new HashMap<Node<?>, Integer>();

		for (int i = 0; i < nodes.size(); i++) {
			cachedNodeToIndexMap.put(nodes.get(i), i);
		}
	}

	public int getAddedIndexOfNode(Node<?> node) {
		if (cachedNodeToIndexMap == null) {
			createCachedNodeToIndexMap();
		}
		return cachedNodeToIndexMap.get(node);
	}

	public void replaceNode(Node<?> nodeToReplace, Node<?> replacementNode) {
		int nodeIndex = getAddedIndexOfNode(nodeToReplace);
		if (nodeIndex >= 0) {
			nodes.set(nodeIndex, replacementNode);
			cachedNodeToIndexMap.remove(nodeToReplace);
			cachedNodeToIndexMap.put(replacementNode, nodeIndex);
		}
	}

}
