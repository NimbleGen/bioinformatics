package com.roche.sequencing.bioinformatics.common.text.viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.Serializable;

import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.MenuSelectionManager;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.colorchooser.ColorSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ColorChooserHelper {

	private final static int DEFAULT_WIDTH = 16;
	private final static int DEFAULT_HEIGHT = 16;

	public static ActionListener createActionListener(JFrame parentFrame, String title, boolean isTextColor, TextViewer textViewer) {
		ActionListener actionListener = new ActionListener() {
			private Color startingColor;

			@SuppressWarnings("deprecation")
			@Override
			public void actionPerformed(ActionEvent event) {
				JMenuItem menuItem = (JMenuItem) event.getSource();

				if (isTextColor) {
					startingColor = textViewer.getTextColor();
				} else {
					startingColor = textViewer.getBackgroundTextPanelColor();
				}

				MenuSelectionManager.defaultManager().clearSelectedPath();
				JColorChooser colorChooser = new JColorChooser(startingColor);
				final MyPreviewPanel previewPanel = new MyPreviewPanel(colorChooser);
				colorChooser.setPreviewPanel(previewPanel);

				ColorSelectionModel model = colorChooser.getSelectionModel();
				model.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent evt) {
						ColorSelectionModel model = (ColorSelectionModel) evt.getSource();
						previewPanel.curColor = model.getSelectedColor();
						if (isTextColor) {
							textViewer.setTextColor(previewPanel.curColor);
						} else {
							textViewer.setBackgroundTextPanelColor(previewPanel.curColor);
						}
						textViewer.updateCurrentTextViewerPanel();
					}
				});

				AbstractColorChooserPanel commonColorsPanel = colorChooser.getChooserPanels()[0];
				AbstractColorChooserPanel rgbPanel = colorChooser.getChooserPanels()[3];
				colorChooser.setChooserPanels(new AbstractColorChooserPanel[] { commonColorsPanel, rgbPanel });

				ColorTracker okColorTracker = new ColorTracker(colorChooser, menuItem);
				ActionListener cancelActionListener = new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						System.out.println("cancel");
						if (isTextColor) {
							textViewer.setTextColor(startingColor);
						} else {
							textViewer.setBackgroundTextPanelColor(startingColor);
						}
						textViewer.updateCurrentTextViewerPanel();
					}
				};
				JDialog dialog = JColorChooser.createDialog(parentFrame, title, true, colorChooser, okColorTracker, cancelActionListener);

				dialog.show(); // blocks until user brings dialog down...
			}
		};
		return actionListener;
	}

	static class MyPreviewPanel extends JComponent {
		private static final long serialVersionUID = 1L;
		Color curColor;

		public MyPreviewPanel(JColorChooser chooser) {
			curColor = chooser.getColor();

			setPreferredSize(new Dimension(50, 50));
		}

		public void paint(Graphics g) {
			g.setColor(curColor);
			g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
		}
	}

	private static class ColorTracker implements ActionListener, Serializable {
		private static final long serialVersionUID = 1L;
		private JColorChooser colorChooser;
		private Color color;
		private JMenuItem menuItem;

		public ColorTracker(JColorChooser colorChooser, JMenuItem menuItem) {
			this.colorChooser = colorChooser;
			this.menuItem = menuItem;
			color = colorChooser.getColor();
		}

		public void actionPerformed(ActionEvent e) {
			System.out.println("ok");
			color = colorChooser.getColor();
			menuItem.setIcon(createIcon(color));
		}
	}

	public static interface ColorChangedListener {
		public void colorChanged(Color newColor);
	}

	public static ImageIcon createIcon(Color main) {
		return createIcon(main, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	private static ImageIcon createIcon(Color main, int width, int height) {
		BufferedImage image = new BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(main);
		graphics.fillRect(0, 0, width, height);
		graphics.setXORMode(Color.DARK_GRAY);
		graphics.drawRect(0, 0, width - 1, height - 1);
		image.flush();
		ImageIcon icon = new ImageIcon(image);
		return icon;
	}
}
