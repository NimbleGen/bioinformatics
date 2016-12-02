package com.roche.sequencing.bioinformatics.common.graph.layout;

import com.roche.sequencing.bioinformatics.common.ui.ImageMap;

public class GraphDrawingDetails {

	private final ImageMap<Node<?>> nodeImageMap;
	private final ImageMap<String> clickKeysImageMap;

	public GraphDrawingDetails(ImageMap<Node<?>> nodeImageMap, ImageMap<String> clickKeysImageMap) {
		super();
		this.nodeImageMap = nodeImageMap;
		this.clickKeysImageMap = clickKeysImageMap;
	}

	public ImageMap<Node<?>> getNodeImageMap() {
		return nodeImageMap;
	}

	public ImageMap<String> getClickKeysImageMap() {
		return clickKeysImageMap;
	}

}
