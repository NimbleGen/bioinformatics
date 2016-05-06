/*
 *    Copyright 2016 Roche NimbleGen Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.roche.sequencing.bioinformatics.common.utils;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;

public class ScreenUtil {
	public static Rectangle getFramesCurrentScreenSize(JFrame currentFrame) {
		Rectangle nonFullScreenBounds = currentFrame.getBounds();

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();

		boolean currentScreenFound = false;
		int index = 0;

		java.awt.Rectangle currentScreenSize = null;
		// figure out which screen the frame is currently in and get its max
		// size
		// and upper left corner
		while (!currentScreenFound && index < gs.length) {
			java.awt.Rectangle screenBounds = gs[index].getDefaultConfiguration().getBounds();

			int frameCenterX = (int) (nonFullScreenBounds.getX() + (nonFullScreenBounds.getWidth() / 2));
			int frameCenterY = (int) (nonFullScreenBounds.getY() + (nonFullScreenBounds.getHeight() / 2));
			int screenStartX = (int) (screenBounds.getX());
			int screenEndX = (int) (screenStartX + screenBounds.getWidth());
			int screenStartY = (int) (screenBounds.getY());
			int screenEndY = (int) (screenStartY + screenBounds.getHeight());
			Rectangle2D screen = new Rectangle2D.Double(screenStartX, screenStartY, (screenEndX - screenStartX + 1), (screenEndY - screenStartY + 1));
			currentScreenFound = screen.contains(frameCenterX, frameCenterY);
			if (currentScreenFound) {
				currentScreenSize = screenBounds;
			}
			index++;
		}

		if (!currentScreenFound) {
			currentScreenSize = ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
		}
		return currentScreenSize;
	}

	
	public static Point getPointWithinGraphicsEnvironment(Point pointToTest, Dimension windowDimension) {
		Point pointToReturn = new Point(pointToTest);
		// Point can be a few pixels off of screen
		pointToTest.x += 30;
		pointToTest.y += 10;
		GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();

		GraphicsDevice[] graphicsDevices = graphicsEnvironment.getScreenDevices();

		Rectangle rectangleContainingPoint = null;
		// Get size of each screen
		for (GraphicsDevice graphicsDevice : graphicsDevices) {
			// DisplayMode displayMode = graphicsDevice.getDisplayMode();
			// int screenWidth = dm.getWidth();
			// int screenHeight = dm.getHeight();
			GraphicsConfiguration[] graphicsConfigurations = graphicsDevice.getConfigurations();
			for (GraphicsConfiguration graphicsConfiguration : graphicsConfigurations) {
				Rectangle rect = graphicsConfiguration.getBounds();
				if (rectangleContainingPoint == null && rect.contains(pointToTest)) {
					// This should be the only rectangle that contains this point
					rectangleContainingPoint = rect;
				}
			}
		}
		Rectangle rectangleToForceCenteringOn = null;
		if (rectangleContainingPoint == null) {
			// The point is not on screen, and therefore we need to generate one that is on screen.
			Rectangle primaryScreenRectangle = graphicsEnvironment.getMaximumWindowBounds();
			// Note getMaximumWindowBounds does not get the full multidisplay rectangle, for some reason.
			rectangleToForceCenteringOn = primaryScreenRectangle;
		} else {
			// Point is on screen, but let's make sure the window is obvious
			final float PERCENT_OF_WINDOW_ON_SCREEN_FACTOR = 0.4f;
			int reasonableWindowWidth = (int) (windowDimension.width * PERCENT_OF_WINDOW_ON_SCREEN_FACTOR);
			int reasonableWindowHeight = (int) (windowDimension.height * PERCENT_OF_WINDOW_ON_SCREEN_FACTOR);
			if ((!rectangleContainingPoint.contains(new Point(pointToTest.x + reasonableWindowWidth, pointToTest.y)))
					|| (!rectangleContainingPoint.contains(new Point(pointToTest.x, pointToTest.y + reasonableWindowHeight)))) {
				// Center point on this screen
				rectangleToForceCenteringOn = rectangleContainingPoint;
			}
		}
		if (rectangleToForceCenteringOn != null) {
			pointToReturn = new Point((int) rectangleToForceCenteringOn.getCenterX(), (int) rectangleToForceCenteringOn.getCenterY());
			int smallestWidth = Math.min(rectangleToForceCenteringOn.width, windowDimension.width);
			int smallestHeight = Math.min(rectangleToForceCenteringOn.height, windowDimension.height);
			// Center point on primary screen
			pointToReturn.x -= smallestWidth / 2;
			pointToReturn.y -= smallestHeight / 2;
		}
		return pointToReturn;
	}
}
