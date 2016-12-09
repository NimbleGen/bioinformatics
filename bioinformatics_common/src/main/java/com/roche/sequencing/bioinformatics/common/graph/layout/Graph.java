package com.roche.sequencing.bioinformatics.common.graph.layout;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Graph {

	private Map<Long, Node<?>> nodeCreationIndexToNodeMap;
	private final Map<Long, Set<Long>> fromNodeToToNodeMap;
	private final Map<Long, Set<Long>> toNodeToFromNodeMap;

	public Graph() {
		fromNodeToToNodeMap = new HashMap<Long, Set<Long>>();
		toNodeToFromNodeMap = new HashMap<Long, Set<Long>>();
		nodeCreationIndexToNodeMap = new HashMap<Long, Node<?>>();
	}

	private Node<?> getExistingNode(Object object) {
		Node<?> foundNode = null;
		nodeLoop: for (Node<?> node : nodeCreationIndexToNodeMap.values()) {
			// comparing object references
			if (object == node.getContents()) {
				foundNode = node;
				break nodeLoop;
			}
		}
		return foundNode;
	}

	Node<?> getNode(Object object) {
		Node<?> node = getExistingNode(object);
		if (node == null) {
			node = new Node<Object>(object);
		}
		return node;
	}

	public void connectNodes(Object fromObject, Object toObject) {
		Node<?> fromNode = getNode(fromObject);
		Node<?> toNode = getNode(toObject);

		connectNodes(fromNode, toNode);
	}

	private synchronized void connectNodes(Node<?> fromNode, Node<?> toNode) {
		nodeCreationIndexToNodeMap.put(fromNode.getCreationIndex(), fromNode);
		nodeCreationIndexToNodeMap.put(toNode.getCreationIndex(), toNode);

		Set<Long> toNodes = fromNodeToToNodeMap.get(fromNode.getCreationIndex());
		if (toNodes == null) {
			toNodes = new HashSet<Long>();
			fromNodeToToNodeMap.put(fromNode.getCreationIndex(), toNodes);
		}
		toNodes.add(toNode.getCreationIndex());

		Set<Long> fromNodes = toNodeToFromNodeMap.get(toNode.getCreationIndex());
		if (fromNodes == null) {
			fromNodes = new HashSet<Long>();
			toNodeToFromNodeMap.put(toNode.getCreationIndex(), fromNodes);
		}
		fromNodes.add(fromNode.getCreationIndex());
	}

	synchronized Set<Node<?>> getConnectedToNodes(Node<?> fromNode) {
		Set<Node<?>> toNodes;
		Set<Long> toNodeIndexes = fromNodeToToNodeMap.get(fromNode.getCreationIndex());
		if (toNodeIndexes == null) {
			toNodes = Collections.emptySet();
		} else {
			toNodes = new HashSet<Node<?>>();
			for (Long toNodeCreationIndex : toNodeIndexes) {
				toNodes.add(nodeCreationIndexToNodeMap.get(toNodeCreationIndex));
			}
		}
		return toNodes;
	}

	synchronized Set<Node<?>> getConnectedFromNodes(Node<?> toNode) {
		Set<Node<?>> fromNodes;
		Set<Long> fromNodeIndexes = toNodeToFromNodeMap.get(toNode.getCreationIndex());
		if (fromNodeIndexes == null) {
			fromNodes = Collections.emptySet();
		} else {
			fromNodes = new HashSet<Node<?>>();
			for (Long creationIndex : fromNodeIndexes) {
				fromNodes.add(nodeCreationIndexToNodeMap.get(creationIndex));
			}
		}
		return fromNodes;
	}

	synchronized Set<Node<?>> getNodes() {
		return new HashSet<Node<?>>(nodeCreationIndexToNodeMap.values());
	}

	public void replaceObject(Object objectToReplace, Object replacementObject) {
		Node<?> nodeToReplace = getNode(objectToReplace);
		Node<?> replacementNode = getNode(replacementObject);
		replaceNode(nodeToReplace, replacementNode);
	}

	public synchronized void replaceNode(Node<?> nodeToReplace, Node<?> replacementNode) {
		nodeCreationIndexToNodeMap.put(replacementNode.getCreationIndex(), replacementNode);
		Set<Long> toNodes = fromNodeToToNodeMap.get(nodeToReplace.getCreationIndex());
		if (toNodes != null) {
			for (Long toNodeCreationIndex : toNodes) {
				connectNodes(replacementNode, nodeCreationIndexToNodeMap.get(toNodeCreationIndex));
			}
		}

		Set<Long> fromNodes = toNodeToFromNodeMap.get(nodeToReplace.getCreationIndex());
		if (fromNodes != null) {
			for (Long fromNodeCreationIndex : fromNodes) {
				connectNodes(getNodeByCreatioIndex(fromNodeCreationIndex), replacementNode);
			}
		}

		removeNode(nodeToReplace);
	}

	private void removeNode(Node<?> nodeToRemove) {
		Set<Long> toNodes = fromNodeToToNodeMap.get(nodeToRemove.getCreationIndex());
		if (toNodes != null) {
			for (Long toNodeCreationIndex : toNodes) {
				Set<Long> tosFromNodes = toNodeToFromNodeMap.get(toNodeCreationIndex);
				tosFromNodes.remove(nodeToRemove.getCreationIndex());
			}
		}

		Set<Long> fromNodes = toNodeToFromNodeMap.get(nodeToRemove.getCreationIndex());
		if (fromNodes != null) {
			for (Long fromNodeCreationIndex : fromNodes) {
				Set<Long> fromsToNodes = fromNodeToToNodeMap.get(fromNodeCreationIndex);
				fromsToNodes.remove(nodeToRemove.getCreationIndex());
			}
		}

		nodeCreationIndexToNodeMap.remove(nodeToRemove.getCreationIndex());
	}

	Node<?> getNodeByCreatioIndex(Long nodeCreationIndex) {
		return nodeCreationIndexToNodeMap.get(nodeCreationIndex);
	}

	public static void main(String[] args) {
		Graph graph = new Graph();
		String a = "a";
		String b = "b";
		String c = "c";
		String d = "d";
		String e = "e";
		String f = "f";

		graph.connectNodes(a, b);
		graph.connectNodes(b, c);
		graph.connectNodes(c, d);
		graph.connectNodes(d, e);
		graph.connectNodes(d, f);

		Node<?> nodeA = graph.getNode(a);
		System.out.println(nodeA);
		System.out.println(graph.getConnectedToNodes(nodeA));
	}

	public int getNumberOfNodes() {
		return nodeCreationIndexToNodeMap.size();
	}

}
