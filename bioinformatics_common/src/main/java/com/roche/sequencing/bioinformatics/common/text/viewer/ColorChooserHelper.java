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
import javax.swing.JButton;
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

	public static ActionListener createActionListener(JFrame parentFrame, String title, TextViewerColor textViewerColor, TextViewer textViewer) {
		ActionListener actionListener = new ActionListener() {
			private Color startingColor;

			@SuppressWarnings("deprecation")
			@Override
			public void actionPerformed(ActionEvent event) {
				Object menuObject = event.getSource();

				startingColor = getColor(textViewer, textViewerColor);

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
						setColor(textViewer, textViewerColor, previewPanel.curColor);
						textViewer.updateCurrentTextViewerPanel();
					}
				});

				AbstractColorChooserPanel commonColorsPanel = colorChooser.getChooserPanels()[0];
				AbstractColorChooserPanel rgbPanel = colorChooser.getChooserPanels()[3];
				colorChooser.setChooserPanels(new AbstractColorChooserPanel[] { commonColorsPanel, rgbPanel });

				ColorTracker okColorTracker = new ColorTracker(colorChooser, menuObject);
				ActionListener cancelActionListener = new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						setColor(textViewer, textViewerColor, startingColor);
						textViewer.updateCurrentTextViewerPanel();
					}
				};
				JDialog dialog = JColorChooser.createDialog(parentFrame, title, true, colorChooser, okColorTracker, cancelActionListener);

				dialog.show(); // blocks until user brings dialog down...
			}
		};
		return actionListener;
	}

	private static void setColor(TextViewer textViewer, TextViewerColor colorType, Color color) {
		switch (colorType) {
		case BACKGROUND:
			textViewer.setBackgroundTextPanelColor(color);
			break;
		case HEADER_BACKGROUND:
			textViewer.setDataHeaderBackgroundColor(color);
			break;
		case ABOVE_HEADER_BACKGROUND:
			textViewer.setAboveDataHeaderBackgroundColor(color);
			break;
		case LINES:
			textViewer.setDataLineColor(color);
			break;
		case FONT:
			textViewer.setTextColor(color);
			break;
		default:
			throw new AssertionError();
		}
	}

	private static Color getColor(TextViewer textViewer, TextViewerColor colorType) {
		Color color = null;
		switch (colorType) {
		case BACKGROUND:
			color = textViewer.getBackgroundTextPanelColor();
			break;
		case ABOVE_HEADER_BACKGROUND:
			color = textViewer.getAboveDataHeaderBackgroundColor();
			break;
		case HEADER_BACKGROUND:
			color = textViewer.getDataHeaderBackgroundColor();
			break;
		case LINES:
			color = textViewer.getDataLineColor();
			break;
		case FONT:
			color = textViewer.getTextColor();
			break;
		default:
			throw new AssertionError();
		}
		return color;
	}

	public static enum TextViewerColor {
		BACKGROUND, ABOVE_HEADER_BACKGROUND, HEADER_BACKGROUND, LINES, FONT
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
		private final JMenuItem menuItem;
		private final JButton button;

		public ColorTracker(JColorChooser colorChooser, Object menuObject) {
			this.colorChooser = colorChooser;
			if (menuObject instanceof JMenuItem) {
				this.menuItem = (JMenuItem) menuObject;
				this.button = null;
			} else if (menuObject instanceof JButton) {
				this.button = (JButton) menuObject;
				this.menuItem = null;
			} else {
				throw new AssertionError();
			}
			color = colorChooser.getColor();
		}

		public void actionPerformed(ActionEvent e) {
			System.out.println("ok");
			color = colorChooser.getColor();
			if (button != null) {
				button.setIcon(createIcon(color));
			} else if (menuItem != null) {
				menuItem.setIcon(createIcon(color));
			}

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
