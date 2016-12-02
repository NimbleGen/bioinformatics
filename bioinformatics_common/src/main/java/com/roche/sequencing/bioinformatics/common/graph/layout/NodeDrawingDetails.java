package com.roche.sequencing.bioinformatics.common.graph.layout;

import com.roche.sequencing.bioinformatics.common.ui.ImageMap;

public class NodeDrawingDetails {
	private final EdgeConnectionPoints edgeConnectionPoints;
	private final ImageMap<String> clickKeys;

	public NodeDrawingDetails(EdgeConnectionPoints edgeConnectionPoints, ImageMap<String> clickKeys) {
		super();
		this.edgeConnectionPoints = edgeConnectionPoints;
		this.clickKeys = clickKeys;
	}

	public EdgeConnectionPoints getEdgeConnectionPoints() {
		return edgeConnectionPoints;
	}

	public ImageMap<String> getClickKeys() {
		return clickKeys;
	}

}
