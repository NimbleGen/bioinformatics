package com.roche.sequencing.bioinformatics.common.text.viewer;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

public class ThreeInARowLayoutManager implements LayoutManager {

	@Override
	public void addLayoutComponent(String name, Component comp) {
	}

	@Override
	public void removeLayoutComponent(Component comp) {
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		return null;
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		return null;
	}

	@Override
	public void layoutContainer(Container parent) {
		if (parent.getComponentCount() != 3) {
			throw new IllegalStateException("The " + ThreeInARowLayoutManager.class + " class must have three children components.");
		}

		Component left = parent.getComponent(0);
		Component center = parent.getComponent(1);
		Component right = parent.getComponent(2);

		Dimension leftItemSize = left.getPreferredSize();
		Dimension centerItemSize = center.getPreferredSize();
		Dimension rightItemSize = right.getPreferredSize();

		int x = 0;
		int y = 0;
		left.setBounds(x, y, (int) leftItemSize.getWidth(), (int) leftItemSize.getHeight());

		x = (int) (parent.getWidth() - centerItemSize.getWidth()) / 2;
		center.setBounds(x, y, (int) centerItemSize.getWidth(), (int) centerItemSize.getHeight());

		x = (int) (parent.getWidth() - rightItemSize.getWidth());
		right.setBounds(x, y, (int) rightItemSize.getWidth(), (int) rightItemSize.getHeight());
	}

}
