package com.roche.sequencing.bioinformatics.common.graph.layout;


public class HyperlinkAction {

	private final String text;
	private final String hoverInfoText;
	private final Runnable clickAction;

	public HyperlinkAction(String text, String hoverInfoText, Runnable clickAction) {
		super();
		this.text = text;
		this.hoverInfoText = hoverInfoText;
		this.clickAction = clickAction;
	}

	public String getText() {
		return text;
	}

	public String getHoverInfoText() {
		return hoverInfoText;
	}

	public Runnable getClickAction() {
		return clickAction;
	}

}
