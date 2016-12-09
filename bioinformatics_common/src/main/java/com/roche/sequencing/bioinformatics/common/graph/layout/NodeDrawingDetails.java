package com.roche.sequencing.bioinformatics.common.graph.layout;

import com.roche.sequencing.bioinformatics.common.ui.ImageMap;

public class NodeDrawingDetails {
	private final EdgeConnectionPoints edgeConnectionPoints;
	private final ImageMap<String> clickKeys;
	private final ImageMap<HyperlinkAction> hyperlinkActions;

	public NodeDrawingDetails(EdgeConnectionPoints edgeConnectionPoints, ImageMap<String> clickKeys, ImageMap<HyperlinkAction> hyperlinkActions) {
		super();
		this.edgeConnectionPoints = edgeConnectionPoints;
		this.clickKeys = clickKeys;
		this.hyperlinkActions = hyperlinkActions;
	}

	public EdgeConnectionPoints getEdgeConnectionPoints() {
		return edgeConnectionPoints;
	}

	public ImageMap<String> getClickKeys() {
		return clickKeys;
	}

	public ImageMap<HyperlinkAction> getHyperlinkActions() {
		return hyperlinkActions;
	}

}
