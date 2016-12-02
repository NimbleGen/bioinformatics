package com.roche.sequencing.bioinformatics.common.ui;

import java.awt.Rectangle;

/**
 * A rectangular position and associated piece of data used by the ImageMap
 * 
 * @author Kurt Heilman
 * 
 * @param <D>
 */
public class ImageMapArea<D> extends Rectangle {

	private static final long serialVersionUID = 1L;
	private final D data;

	/**
	 * default constructor
	 * 
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param data
	 */
	public ImageMapArea(int x, int y, int width, int height, D data) {
		super(x, y, width, height);

		this.data = data;
	}

	/**
	 * return the data associated with this area
	 * 
	 * @return data
	 */
	public D getData() {
		return data;
	}
}
