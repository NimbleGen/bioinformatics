package com.roche.sequencing.bioinformatics.common.ui;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * The ImageMap class allows data to be associated with a location on a drawable surface. When the drawable package is used for drawing, a related image map can be created to tie additional
 * information to a specific location on the drawing surface. An ImageMap is a generic class that contains a group of ImageMapAreas. The data that is contained within the ImageMap is defined using
 * generics. An ImageMapArea contains an x coordinate, y coordinate, width, height and data object that is related to this location. The ImageMap has one method that can be used to retrieve specific
 * information relative to a location on the drawing surface, getFirstDataAtPosition(int x, int y). This method returns the first piece of information that has an associated area containing this
 * point. Image maps (like their html counterpart) are very useful in attaching data to an image.
 * 
 * @author Kurt Heilman
 * 
 * @param <D>
 */
public class ImageMap<D> {

	private List<ImageMapArea<D>> areas;

	/**
	 * default constructor
	 */
	public ImageMap() {
		super();
		areas = new ArrayList<ImageMapArea<D>>();
	}

	/**
	 * copy constructor
	 */
	public ImageMap(ImageMap<D> orig) {
		super();
		areas = new ArrayList<ImageMapArea<D>>(orig.areas);
	}

	/**
	 * adds all areas to this image map
	 * 
	 * @param imageMapToAdd
	 */
	public void add(ImageMap<D> imageMapToAdd) {
		areas.addAll(imageMapToAdd.areas);
	}

	/**
	 * adds all areas to this image map
	 * 
	 * @param imageMapToAdd
	 */
	public void add(ImageMap<D> imageMapToAdd, int xOffset, int yOffset) {
		for (ImageMapArea<D> area : imageMapToAdd.areas) {
			areas.add(new ImageMapArea<D>(area.x + xOffset, area.y + yOffset, area.width, area.height, area.getData()));
		}
	}

	/**
	 * Add a piece of data to the area represented by the given (x,y) coordinates, width and height
	 * 
	 * @param data
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public void addArea(D data, int x, int y, int width, int height) {
		areas.add(new ImageMapArea<D>(x, y, width, height, data));
	}

	/**
	 * Add a piece of data to the area represented by the given (x,y) coordinates, width and height
	 * 
	 * @param data
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public void addArea(D data, Rectangle rectangle) {
		areas.add(new ImageMapArea<D>((int) rectangle.getX(), (int) rectangle.getY(), (int) rectangle.getWidth(), (int) rectangle.getHeight(), data));
	}

	/**
	 * Return the first piece of data found at the given position
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public D getFirstDataAtPosition(int x, int y) {
		D bestOverlappingData = null;
		boolean dataFound = false;
		int index = 0;
		while (index < areas.size() && !dataFound) {
			ImageMapArea<D> area = areas.get(index);
			boolean withinXValues = x >= area.getX() && x <= (area.getX() + area.getWidth());
			boolean withinYValues = y >= area.getY() && y <= (area.getY() + area.getHeight());
			if (withinXValues && withinYValues) {
				bestOverlappingData = area.getData();
				dataFound = true;
			}
			index++;
		}
		return bestOverlappingData;
	}

	/**
	 * Return the first piece of data found at the given position
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public List<D> getAllDataAtPosition(int x, int y) {
		List<D> overlappingData = new ArrayList<D>();
		int index = 0;
		while (index < areas.size()) {
			ImageMapArea<D> area = areas.get(index);
			boolean withinXValues = x >= area.getX() && x <= (area.getX() + area.getWidth());
			boolean withinYValues = y >= area.getY() && y <= (area.getY() + area.getHeight());
			if (withinXValues && withinYValues) {
				overlappingData.add(area.getData());
			}
			index++;
		}
		return overlappingData;
	}

	/**
	 * get the ImageMapArea at the given index
	 * 
	 * @param index
	 * @return ImageMapArea
	 */
	public ImageMapArea<D> getImageMapArea(int index) {
		return areas.get(index);
	}

	public Rectangle getFirstAreaMatchingData(D data) {
		Rectangle firstFoundArea = null;
		areaLoop: for (ImageMapArea<D> area : areas) {
			if (area.getData().equals(data)) {
				firstFoundArea = area;
				break areaLoop;
			}
		}
		return firstFoundArea;
	}

	/**
	 * return the number of ImageMapAreas contained within this ImageMap
	 * 
	 * @return int
	 */
	public int size() {
		return areas.size();
	}

	public void clear() {
		areas.clear();
	}
}
