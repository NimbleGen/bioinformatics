package com.roche.sequencing.bioinformatics.common.graph.layout;

import com.roche.sequencing.bioinformatics.common.ui.ImageMap;

public class GraphDrawingDetails {

	private final ImageMap<Object> nodeImageMap;
	private final ImageMap<String> clickKeysImageMap;
	private final ImageMap<HyperlinkAction> hyperlinkActions;

	public GraphDrawingDetails(ImageMap<Object> nodeImageMap, ImageMap<String> clickKeysImageMap, ImageMap<HyperlinkAction> hyperlinkActions) {
		super();
		this.nodeImageMap = nodeImageMap;
		this.clickKeysImageMap = clickKeysImageMap;
		this.hyperlinkActions = hyperlinkActions;
	}

	public ImageMap<Object> getNodeImageMap() {
		return nodeImageMap;
	}

	public ImageMap<String> getClickKeysImageMap() {
		return clickKeysImageMap;
	}

	public ImageMap<HyperlinkAction> getHyperlinkActions() {
		return hyperlinkActions;
	}

}
