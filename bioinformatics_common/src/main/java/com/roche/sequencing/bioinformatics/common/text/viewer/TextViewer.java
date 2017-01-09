package com.roche.sequencing.bioinformatics.common.text.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.FontUIResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.sequencing.bioinformatics.common.text.fonts.FontHelper;
import com.roche.sequencing.bioinformatics.common.text.viewer.ColorChooserHelper.TextViewerColor;
import com.roche.sequencing.bioinformatics.common.utils.ColorsUtil;
import com.roche.sequencing.bioinformatics.common.utils.JavaUiUtil;
import com.roche.sequencing.bioinformatics.common.utils.ListUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class TextViewer extends JFrame {

	private final static Logger logger = LoggerFactory.getLogger(TextViewer.class);
	private static final long serialVersionUID = 1L;

	private final static Font MONOSPACED_FONT = new Font("Monospaced", Font.PLAIN, 12);

	public final static String APPLICATION_NAME = "Text Viewer";
	public final static String APPLICATION_VERSION = "v1";
	public final static Image APPLICATION_ICON = new ImageIcon(TextViewer.class.getResource("largeTextViewerIcon.png")).getImage();
	private final static Dimension MINIMUM_PANEL_DIMENSION = new Dimension(900, 400);
	public final static FontUIResource DEFAULT_DISPLAY_FONT = new FontUIResource("Serif", Font.PLAIN, 12);

	final static ImageIcon ABOUT_ICON = new ImageIcon(TextViewer.class.getResource("about.png"));
	final static ImageIcon CLOSE_ALL_ICON = new ImageIcon(TextViewer.class.getResource("close_all.png"));
	public final static FontUIResource MENU_FONT = new FontUIResource("Serif", Font.PLAIN, 16);
	public final static FontUIResource SMALL_MENU_FONT = new FontUIResource("Serif", Font.PLAIN, 14);

	private final static ImageIcon GOTO_ICON = new ImageIcon(TextViewer.class.getResource("goto.png"));
	private final static ImageIcon CLIPBOARD_ICON = new ImageIcon(TextViewer.class.getResource("clipboard.png"));
	private final static ImageIcon SAVE_ICON = new ImageIcon(TextViewer.class.getResource("save.png"));
	private final static ImageIcon SEARCH_ICON = new ImageIcon(TextViewer.class.getResource("find.png"));
	private final static ImageIcon CLOSE_ICON = new ImageIcon(TextViewer.class.getResource("close.png"));
	private final static ImageIcon CLOSE_HOVER_ICON = new ImageIcon(TextViewer.class.getResource("close_hover.png"));
	private final static ImageIcon EXIT_ICON = new ImageIcon(TextViewer.class.getResource("exit.png"));
	private final static ImageIcon IMPORT_DATA_ICON = new ImageIcon(TextViewer.class.getResource("import_data.png"));

	private static Preferences preferences = Preferences.userRoot().node(TextViewer.class.getName());
	private final static String LAST_VIEW_DIRECTORY_PROPERTIES_KEY = "lastview.lastdirectory";
	private final static String LAST_VIEW_LAST_OPENED_FILES_PROPERTIES_KEY = "lastview.lastopenedfiles";
	private final static String LAST_VIEW_LAST_OPENED_FILES_POSITIONS_PROPERTIES_KEY = "lastview.lastopenedfilespositions";
	private final static String LAST_VIEW_LAST_OPENED_FILES_IS_DATA_VIEW_PROPERTIES_KEY = "lastview.lastopenedfiles.isdataview";
	private final static String LAST_VIEW_LAST_OPENED_FILES_HEADER_LINE_NUMBERS_PROPERTIES_KEY = "last_data_header_line_numbers";
	private final static String LAST_VIEW_LAST_OPENED_FILES_TAB_SIZE_PROPERTIES_KEY = "last_data_tab_sizes";
	private final static String LAST_VIEW_FRAME_X_PROPERTIES_KEY = "lastview.x";
	private final static String LAST_VIEW_FRAME_Y_PROPERTIES_KEY = "lastview.y";
	private final static String LAST_VIEW_FRAME_WIDTH_PROPERTIES_KEY = "lastview.framewidth";
	private final static String LAST_VIEW_FRAME_HEIGHT_PROPERTIES_KEY = "lastview.frameheight";
	private final static String LAST_VIEW_WAS_MAXIMIZED_PROPERTIES_KEY = "lastview.wasmaximized";

	private final static String LAST_SELECTED_TAB_PROPERTIES_KEY = "last_selected_tab";
	private final static String LAST_FONT_NAME_PROPERTIES_KEY = "last_font_name";
	private final static String LAST_FONT_SIZE_PROPERTIES_KEY = "last_font_size";
	private final static String LAST_FONT_COLOR_PROPERTIES_KEY = "last_font_color";
	private final static String LAST_FONT_COLOR_OPACITY_PROPERTIES_KEY = "last_font_color_opacity";
	private final static String LAST_FONT_BACKGROUND_COLOR_PROPERTIES_KEY = "last_background_color";
	private final static String LAST_FONT_BACKGROUND_COLOR_OPACITY_PROPERTIES_KEY = "last_background_color_opacity";
	private final static String LAST_SHOW_LINE_NUMBERS_PROPERTIES_KEY = "last_show_line_numbers";
	private final static String LAST_DATA_HEADER_BACKGROUND_COLOR_PROPERTIES_KEY = "last_data_header_background_color";
	private final static String LAST_DATA_HEADER_BACKGROUND_COLOR_OPACITY_PROPERTIES_KEY = "last_data_header_background_color_opacity";
	private final static String LAST_ABOVE_DATA_HEADER_BACKGROUND_COLOR_PROPERTIES_KEY = "last_above_data_header_background_color";
	private final static String LAST_ABOVE_DATA_HEADER_BACKGROUND_COLOR_OPACITY_PROPERTIES_KEY = "last_above_data_header_background_color_opacity";
	private final static String LAST_DATA_LINE_COLOR_PROPERTIES_KEY = "last_data_line_color";
	private final static String LAST_DATA_LINE_COLOR_OPACITY_PROPERTIES_KEY = "last_data_line_color_opacity";

	private final static Color BACKGROUND_COLOR = new Color(0.80f, 0.80f, 0.80f, 1.0f);
	private final static Image BACKGROUND_LOGO = new ImageIcon(TextViewer.class.getResource("backgroundLogo.png")).getImage();
	private final static int BACKGROUND_IMAGE_HORIZONTAL_SPACING = 100;
	private final static int BACKGROUND_IMAGE_VERTICAL_SPACING = 100;

	private final static String PROPERTIES_DELIMITER = "--NEXT_FILE--";

	private final static int MAX_TAB_TITLE_LENGTH = 30;

	public final static Cursor TEXT_CURSOR = new Cursor(Cursor.TEXT_CURSOR);

	private final JTabbedPane tabbedPane;
	private final List<TextViewerPanel> textViewerPanels;

	private JMenuItem menuItemCloseAllFiles;
	private JRadioButtonMenuItem viewAsDataRadioButtonMenuItem;
	private JRadioButtonMenuItem viewAsTextRadioButtonMenuItem;
	private JSpinner fontSizeSpinner;
	private JSpinner headerLineSpinner;
	private JSpinner tabSizeSpinner;

	private final boolean isStandAlone;

	private boolean showLineNumbers;
	private Font textFont;
	private Color textColor;
	private Color backgroundTextPanelColor;
	private Color dataHeaderBackgroundColor;
	private Color aboveDataHeaderBackgroundColor;
	private Color dataLineColor;

	private JPanel dataOptionsPanel;
	private JPanel textOptionsPanel;

	private JCheckBox headerExistsCheckBox;
	private JLabel headerLineLabel;

	private List<Font> monoSpacedFonts;

	public TextViewer() {
		this(true);
	}

	public TextViewer(boolean isStandAlone) {
		this.isStandAlone = isStandAlone;

		monoSpacedFonts = new ArrayList<Font>();
		monoSpacedFonts.add(MONOSPACED_FONT);
		monoSpacedFonts.addAll(FontHelper.getAvailableMonoSpacedFonts());

		setIconImage(APPLICATION_ICON);
		setMinimumSize(MINIMUM_PANEL_DIMENSION);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			logger.error(e.getMessage(), e);
		}

		JavaUiUtil.setUIFont(DEFAULT_DISPLAY_FONT);

		this.showLineNumbers = true;
		this.textFont = monoSpacedFonts.get(0);
		this.textColor = Color.BLACK;
		this.backgroundTextPanelColor = Color.WHITE;
		this.dataHeaderBackgroundColor = new Color(0f, 0f, 1f, 0.25f);
		this.aboveDataHeaderBackgroundColor = Color.LIGHT_GRAY;
		this.dataLineColor = Color.BLACK;

		if (isStandAlone) {
			setDefaultCloseOperation(EXIT_ON_CLOSE);
		} else {
			setDefaultCloseOperation(HIDE_ON_CLOSE);
		}
		pack();
		setTitle(APPLICATION_NAME + " " + APPLICATION_VERSION);

		textViewerPanels = new ArrayList<TextViewerPanel>();
		tabbedPane = new JTabbedPane() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				if (tabbedPane.getTabCount() == 0) {
					drawBackground(g, getWidth(), getHeight());
				}
			}
		};
		tabbedPane.setOpaque(false);
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updateMenu();
			}
		});
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		setContentPane(tabbedPane);

		try {
			String frameXAsString = preferences.get(LAST_VIEW_FRAME_X_PROPERTIES_KEY, null);
			int frameX = Integer.parseInt(frameXAsString);

			String frameYAsString = preferences.get(LAST_VIEW_FRAME_Y_PROPERTIES_KEY, null);
			int frameY = Integer.parseInt(frameYAsString);

			String frameWidthAsString = preferences.get(LAST_VIEW_FRAME_WIDTH_PROPERTIES_KEY, null);
			int frameWidth = Integer.parseInt(frameWidthAsString);

			String frameHeightAsString = preferences.get(LAST_VIEW_FRAME_HEIGHT_PROPERTIES_KEY, null);
			int frameHeight = Integer.parseInt(frameHeightAsString);

			String wasMaximizedAsString = preferences.get(LAST_VIEW_WAS_MAXIMIZED_PROPERTIES_KEY, null);
			boolean wasMaximized = Boolean.parseBoolean(wasMaximizedAsString);

			if (wasMaximized) {
				setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
			}

			setLocation(frameX, frameY);
			setSize(frameWidth, frameHeight);
		} catch (NumberFormatException e) {
			logger.warn(e.getMessage(), e);
		}

		String fontName = preferences.get(LAST_FONT_NAME_PROPERTIES_KEY, null);
		if (fontName != null) {
			fontLoop: for (Font font : monoSpacedFonts) {
				if (font.getName().equals(fontName)) {
					textFont = font;
					break fontLoop;
				}
			}
		}

		String fontSizeAsString = preferences.get(LAST_FONT_SIZE_PROPERTIES_KEY, null);
		if (fontSizeAsString != null) {
			try {
				int fontSize = Integer.parseInt(fontSizeAsString);
				textFont = textFont.deriveFont((float) fontSize);
			} catch (NumberFormatException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		String fontColorAsString = preferences.get(LAST_FONT_COLOR_PROPERTIES_KEY, null);
		if (fontColorAsString != null) {
			try {
				int fontColorRgb = Integer.parseInt(fontColorAsString);
				textColor = new Color(fontColorRgb);
			} catch (NumberFormatException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		String fontColorOpacityAsString = preferences.get(LAST_FONT_COLOR_OPACITY_PROPERTIES_KEY, null);
		if (fontColorOpacityAsString != null) {
			try {
				int fontColorAlpha = Integer.parseInt(fontColorOpacityAsString);
				textColor = ColorsUtil.addAlpha(textColor, fontColorAlpha);
			} catch (NumberFormatException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		String fontBackgroundColorAsString = preferences.get(LAST_FONT_BACKGROUND_COLOR_PROPERTIES_KEY, null);
		if (fontBackgroundColorAsString != null) {
			try {
				int backgroundColorRgb = Integer.parseInt(fontBackgroundColorAsString);
				backgroundTextPanelColor = new Color(backgroundColorRgb);
			} catch (NumberFormatException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		String fontBackgroundOpacityColorAsString = preferences.get(LAST_FONT_BACKGROUND_COLOR_OPACITY_PROPERTIES_KEY, null);
		if (fontBackgroundOpacityColorAsString != null) {
			try {
				int backgroundColorAlpha = Integer.parseInt(fontBackgroundOpacityColorAsString);
				backgroundTextPanelColor = ColorsUtil.addAlpha(backgroundTextPanelColor, backgroundColorAlpha);
			} catch (NumberFormatException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		String showLineNumbersAsString = preferences.get(LAST_SHOW_LINE_NUMBERS_PROPERTIES_KEY, null);
		if (showLineNumbersAsString != null) {
			try {
				showLineNumbers = Boolean.parseBoolean(showLineNumbersAsString);
			} catch (NumberFormatException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		String dataBackgroundColorAsString = preferences.get(LAST_DATA_HEADER_BACKGROUND_COLOR_PROPERTIES_KEY, null);
		if (dataBackgroundColorAsString != null) {
			try {
				int backgroundColorRgb = Integer.parseInt(dataBackgroundColorAsString);
				dataHeaderBackgroundColor = new Color(backgroundColorRgb);
			} catch (NumberFormatException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		String dataBackgroundOpacityColorAsString = preferences.get(LAST_DATA_HEADER_BACKGROUND_COLOR_OPACITY_PROPERTIES_KEY, null);
		if (dataBackgroundOpacityColorAsString != null) {
			try {
				int backgroundColorAlpha = Integer.parseInt(dataBackgroundOpacityColorAsString);
				dataHeaderBackgroundColor = ColorsUtil.addAlpha(dataHeaderBackgroundColor, backgroundColorAlpha);
			} catch (NumberFormatException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		String aboveDataBackgroundColorAsString = preferences.get(LAST_ABOVE_DATA_HEADER_BACKGROUND_COLOR_PROPERTIES_KEY, null);
		if (aboveDataBackgroundColorAsString != null) {
			try {
				int aboveDataBackgroundColorRgb = Integer.parseInt(aboveDataBackgroundColorAsString);
				aboveDataHeaderBackgroundColor = new Color(aboveDataBackgroundColorRgb);
			} catch (NumberFormatException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		String aboveDataBackgroundOpacityColorAsString = preferences.get(LAST_ABOVE_DATA_HEADER_BACKGROUND_COLOR_OPACITY_PROPERTIES_KEY, null);
		if (aboveDataBackgroundOpacityColorAsString != null) {
			try {
				int aboveDataBackgroundColorAlpha = Integer.parseInt(aboveDataBackgroundOpacityColorAsString);
				aboveDataHeaderBackgroundColor = ColorsUtil.addAlpha(aboveDataHeaderBackgroundColor, aboveDataBackgroundColorAlpha);
			} catch (NumberFormatException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		String dataLineColorAsString = preferences.get(LAST_DATA_LINE_COLOR_PROPERTIES_KEY, null);
		if (dataLineColorAsString != null) {
			try {
				int backgroundColorRgb = Integer.parseInt(dataLineColorAsString);
				dataLineColor = new Color(backgroundColorRgb);
			} catch (NumberFormatException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		String dataLineOpacityColorAsString = preferences.get(LAST_DATA_LINE_COLOR_OPACITY_PROPERTIES_KEY, null);
		if (dataLineOpacityColorAsString != null) {
			try {
				int lineColorAlpha = Integer.parseInt(dataLineOpacityColorAsString);
				dataLineColor = ColorsUtil.addAlpha(dataLineColor, lineColorAlpha);
			} catch (NumberFormatException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		tabbedPane.setTransferHandler(new DragAndDropFileTransferHandler(this));
		setTransferHandler(new DragAndDropFileTransferHandler(this));

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				boolean isMaximized = TextViewer.this.getExtendedState() == JFrame.MAXIMIZED_BOTH;
				preferences.put(LAST_VIEW_WAS_MAXIMIZED_PROPERTIES_KEY, "" + isMaximized);
				saveFrameLocationPreferences();

				if (isStandAlone) {
					List<String> fileNames = new ArrayList<String>();
					List<String> lineNumbers = new ArrayList<String>();
					List<String> isDataView = new ArrayList<String>();
					List<String> headerLineNumbers = new ArrayList<String>();
					List<String> tabSizes = new ArrayList<String>();
					for (TextViewerPanel textViewerPanel : textViewerPanels) {
						fileNames.add(textViewerPanel.getFile().getAbsolutePath());
						lineNumbers.add("" + (textViewerPanel.getCurrentLineNumber() + 1));
						isDataView.add("" + textViewerPanel.isShowDataView());
						if (textViewerPanel.isShowHeader()) {
							headerLineNumbers.add("" + textViewerPanel.getHeaderLineNumber());
						} else {
							headerLineNumbers.add("" + 0);
						}
						tabSizes.add("" + textViewerPanel.getTabSize());
					}
					preferences.put(LAST_VIEW_LAST_OPENED_FILES_PROPERTIES_KEY, ListUtil.toString(fileNames, PROPERTIES_DELIMITER));
					preferences.put(LAST_VIEW_LAST_OPENED_FILES_POSITIONS_PROPERTIES_KEY, ListUtil.toString(lineNumbers, PROPERTIES_DELIMITER));
					preferences.put(LAST_VIEW_LAST_OPENED_FILES_IS_DATA_VIEW_PROPERTIES_KEY, ListUtil.toString(isDataView, PROPERTIES_DELIMITER));
					preferences.put(LAST_VIEW_LAST_OPENED_FILES_HEADER_LINE_NUMBERS_PROPERTIES_KEY, ListUtil.toString(headerLineNumbers, PROPERTIES_DELIMITER));
					preferences.put(LAST_VIEW_LAST_OPENED_FILES_TAB_SIZE_PROPERTIES_KEY, ListUtil.toString(tabSizes, PROPERTIES_DELIMITER));
					preferences.put(LAST_SELECTED_TAB_PROPERTIES_KEY, "" + tabbedPane.getSelectedIndex());
				}

				preferences.put(LAST_FONT_NAME_PROPERTIES_KEY, textFont.getFontName());
				preferences.put(LAST_FONT_SIZE_PROPERTIES_KEY, "" + textFont.getSize());
				preferences.put(LAST_FONT_COLOR_PROPERTIES_KEY, "" + textColor.getRGB());
				preferences.put(LAST_FONT_COLOR_OPACITY_PROPERTIES_KEY, "" + textColor.getAlpha());
				preferences.put(LAST_FONT_BACKGROUND_COLOR_PROPERTIES_KEY, "" + backgroundTextPanelColor.getRGB());
				preferences.put(LAST_FONT_BACKGROUND_COLOR_OPACITY_PROPERTIES_KEY, "" + backgroundTextPanelColor.getAlpha());
				preferences.put(LAST_SHOW_LINE_NUMBERS_PROPERTIES_KEY, "" + showLineNumbers);
				preferences.put(LAST_DATA_HEADER_BACKGROUND_COLOR_PROPERTIES_KEY, "" + dataHeaderBackgroundColor.getRGB());
				preferences.put(LAST_DATA_HEADER_BACKGROUND_COLOR_OPACITY_PROPERTIES_KEY, "" + dataHeaderBackgroundColor.getAlpha());
				preferences.put(LAST_ABOVE_DATA_HEADER_BACKGROUND_COLOR_PROPERTIES_KEY, "" + aboveDataHeaderBackgroundColor.getRGB());
				preferences.put(LAST_ABOVE_DATA_HEADER_BACKGROUND_COLOR_OPACITY_PROPERTIES_KEY, "" + aboveDataHeaderBackgroundColor.getAlpha());
				preferences.put(LAST_DATA_LINE_COLOR_PROPERTIES_KEY, "" + dataLineColor.getRGB());
				preferences.put(LAST_DATA_LINE_COLOR_OPACITY_PROPERTIES_KEY, "" + dataLineColor.getAlpha());

				for (TextViewerPanel textViewerPanel : textViewerPanels) {
					try {
						textViewerPanel.close();
					} catch (IOException e) {
						logger.warn(e.getMessage(), e);
					}
				}
			}
		});

		initMenu();

		if (isStandAlone) {
			try {
				List<Integer> lastOpenedFilePositions = new ArrayList<Integer>();
				String lastOpenedFilePositionsAsString = preferences.get(LAST_VIEW_LAST_OPENED_FILES_POSITIONS_PROPERTIES_KEY, null);
				if (lastOpenedFilePositionsAsString != null) {
					String[] split = lastOpenedFilePositionsAsString.split(PROPERTIES_DELIMITER);

					for (String lastOpenedFilePositionAsString : split) {
						if (!lastOpenedFilePositionAsString.isEmpty()) {
							try {
								int lastOpenedFilePosition = Integer.parseInt(lastOpenedFilePositionAsString);
								lastOpenedFilePositions.add(lastOpenedFilePosition);
							} catch (NumberFormatException e) {
								logger.warn(e.getMessage(), e);
							}
						}
					}

				}

				List<Boolean> lastOpenededShowAsDataList = new ArrayList<Boolean>();
				String lastOpenedShowAsDataForAll = preferences.get(LAST_VIEW_LAST_OPENED_FILES_IS_DATA_VIEW_PROPERTIES_KEY, null);
				if (lastOpenedShowAsDataForAll != null) {
					String[] split = lastOpenedShowAsDataForAll.split(PROPERTIES_DELIMITER);

					for (int i = 0; i < split.length; i++) {
						String lastOpenedShowAsDataAsString = split[i];
						if (!lastOpenedShowAsDataAsString.isEmpty()) {
							boolean showAsData = Boolean.parseBoolean(lastOpenedShowAsDataAsString);
							lastOpenededShowAsDataList.add(showAsData);
						}
					}
				}

				List<Integer> lastOpenededHeaderLineNumbersList = new ArrayList<Integer>();
				String lastOpenedHeaderLineNumbersForAll = preferences.get(LAST_VIEW_LAST_OPENED_FILES_HEADER_LINE_NUMBERS_PROPERTIES_KEY, null);
				if (lastOpenedHeaderLineNumbersForAll != null) {
					String[] split = lastOpenedHeaderLineNumbersForAll.split(PROPERTIES_DELIMITER);

					for (int i = 0; i < split.length; i++) {
						String lastOpenedHeaderLineNumberAsString = split[i];
						if (!lastOpenedHeaderLineNumberAsString.isEmpty()) {
							int headerLineNumber = Integer.parseInt(lastOpenedHeaderLineNumberAsString);
							lastOpenededHeaderLineNumbersList.add(headerLineNumber);
						}
					}
				}

				List<Integer> lastOpenededTabSizesList = new ArrayList<Integer>();
				String lastOpenededTabSizesListForAll = preferences.get(LAST_VIEW_LAST_OPENED_FILES_TAB_SIZE_PROPERTIES_KEY, null);
				if (lastOpenededTabSizesListForAll != null) {
					String[] split = lastOpenededTabSizesListForAll.split(PROPERTIES_DELIMITER);

					for (int i = 0; i < split.length; i++) {
						String lastOpenedTabSizeAsString = split[i];
						if (!lastOpenedTabSizeAsString.isEmpty()) {
							int tabSize = Integer.parseInt(lastOpenedTabSizeAsString);
							lastOpenededTabSizesList.add(tabSize);
						}
					}
				}

				int selectedTab = 0;
				String selectedTabAsString = preferences.get(LAST_SELECTED_TAB_PROPERTIES_KEY, null);
				if (selectedTabAsString != null) {
					selectedTab = Integer.parseInt(selectedTabAsString);
				}

				String lastOpenedFileNames = preferences.get(LAST_VIEW_LAST_OPENED_FILES_PROPERTIES_KEY, null);
				if (lastOpenedFileNames != null) {
					String[] split = lastOpenedFileNames.split(PROPERTIES_DELIMITER);

					for (int i = 0; i < split.length; i++) {
						String lastOpenedFileName = split[i];
						if (!lastOpenedFileName.isEmpty()) {
							File lastOpenedFile = new File(lastOpenedFileName);

							final int index = i;

							IStuffToDoAfterFileIsLoaded stuffToDoAfterFileIsLoaded = new IStuffToDoAfterFileIsLoaded() {

								@Override
								public void doAfterFileIsLoaded(TextViewerPanel textViewerPanel) {
									if (textViewerPanel != null) {
										if (index < lastOpenedFilePositions.size()) {
											int lineNumber = lastOpenedFilePositions.get(index);
											textViewerPanel.setLineNumber(lineNumber);
										}
										if (index < lastOpenededShowAsDataList.size()) {
											boolean showAsData = lastOpenededShowAsDataList.get(index);
											textViewerPanel.setShowDataView(showAsData);
										}
										if (index < lastOpenededHeaderLineNumbersList.size()) {
											int headerLineNumber = lastOpenededHeaderLineNumbersList.get(index);
											textViewerPanel.setHeaderLineNumber(headerLineNumber);
										}
										if (index < lastOpenededTabSizesList.size()) {
											int tabSize = lastOpenededTabSizesList.get(index);
											textViewerPanel.setTabSize(tabSize);
										}
									}
								}
							};
							readInFile(lastOpenedFile, stuffToDoAfterFileIsLoaded);

						}
					}
					tabbedPane.setSelectedIndex(selectedTab);
					updateMenu();
				}

			} catch (Exception e) {
				logger.warn("Unable to reinitialize all files.", e);
			}
		}

		updateCloseAllMenuItem();
		setVisible(true);
	}

	private void initMenu() {
		JMenuBar mainMenuBar = new JMenuBar();
		mainMenuBar.setBorder(BorderFactory.createLineBorder(Color.black));
		setJMenuBar(mainMenuBar);

		JMenu menuFile = new JMenu("File");
		menuFile.setFont(MENU_FONT);
		JMenuItem openMenuItem = new JMenuItem("Open", IMPORT_DATA_ICON);
		openMenuItem.setFont(MENU_FONT);
		openMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File fileToLoad = getViewFileForLoadingFromUser(TextViewer.this);
				if (fileToLoad != null) {
					readInFile(fileToLoad);
				}
			}
		});
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke('O', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuFile.add(openMenuItem);

		menuItemCloseAllFiles = new JMenuItem("Close All", CLOSE_ALL_ICON);
		menuItemCloseAllFiles.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				closeAllFiles();
			}
		});
		menuItemCloseAllFiles.setFont(MENU_FONT);
		menuFile.add(menuItemCloseAllFiles);

		JMenuItem quitMenuItem = new JMenuItem("Exit", EXIT_ICON);
		quitMenuItem.setFont(MENU_FONT);
		quitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isStandAlone) {
					System.exit(0);
				} else {
					setVisible(false);
				}
			}
		});
		quitMenuItem.setAccelerator(KeyStroke.getKeyStroke('X', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuFile.add(quitMenuItem);
		mainMenuBar.add(menuFile);

		JMenu menuEdit = new JMenu("Edit");
		menuEdit.setFont(MENU_FONT);
		JMenuItem copyToClipBoardMenuItem = new JMenuItem("Copy Selection to ClipBoard", CLIPBOARD_ICON);
		copyToClipBoardMenuItem.setFont(MENU_FONT);
		copyToClipBoardMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TextViewerPanel selectedPanel = (TextViewerPanel) tabbedPane.getSelectedComponent();
				if (selectedPanel != null) {
					selectedPanel.copySelectedTextToClipboard(false);
				}
			}
		});
		copyToClipBoardMenuItem.setAccelerator(KeyStroke.getKeyStroke('C', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuEdit.add(copyToClipBoardMenuItem);

		JMenuItem saveSelectionToFileMenuItem = new JMenuItem("Save Selection to File", SAVE_ICON);
		saveSelectionToFileMenuItem.setFont(MENU_FONT);
		saveSelectionToFileMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TextViewerPanel selectedPanel = (TextViewerPanel) tabbedPane.getSelectedComponent();
				if (selectedPanel != null) {
					selectedPanel.copySelectedTextToClipboard(true);
				}
			}
		});
		saveSelectionToFileMenuItem.setAccelerator(KeyStroke.getKeyStroke('S', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuEdit.add(saveSelectionToFileMenuItem);

		JMenuItem gotoLineNumberMenuItem = new JMenuItem("Go To Line Number", GOTO_ICON);
		gotoLineNumberMenuItem.setFont(MENU_FONT);
		gotoLineNumberMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TextViewerPanel selectedPanel = (TextViewerPanel) tabbedPane.getSelectedComponent();
				if (selectedPanel != null) {
					selectedPanel.haveUserGoToLineNumber();
				}
			}
		});
		gotoLineNumberMenuItem.setAccelerator(KeyStroke.getKeyStroke('G', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuEdit.add(gotoLineNumberMenuItem);

		JMenuItem findTextMenuItem = new JMenuItem("Find Text", SEARCH_ICON);
		findTextMenuItem.setFont(MENU_FONT);
		findTextMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TextViewerPanel selectedPanel = (TextViewerPanel) tabbedPane.getSelectedComponent();
				if (selectedPanel != null) {
					selectedPanel.haveUserFindText();
				}
			}
		});
		findTextMenuItem.setAccelerator(KeyStroke.getKeyStroke('F', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuEdit.add(findTextMenuItem);

		mainMenuBar.add(menuEdit);

		JMenu menuView = new JMenu("View");
		menuView.setFont(MENU_FONT);
		ButtonGroup textOrDataGroup = new ButtonGroup();
		menuView.addSeparator();

		dataOptionsPanel = new JPanel(new GridLayout(5, 1));
		textOptionsPanel = new JPanel();

		viewAsTextRadioButtonMenuItem = new StayOpenRadioButtonMenuItem("View as Text");
		viewAsTextRadioButtonMenuItem.setFont(MENU_FONT);
		viewAsTextRadioButtonMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TextViewerPanel selectedPanel = (TextViewerPanel) tabbedPane.getSelectedComponent();
				if (selectedPanel != null) {
					selectedPanel.setShowDataView(false);
					dataOptionsPanel.setVisible(false);
					textOptionsPanel.setVisible(true);
				}
			}
		});
		textOrDataGroup.add(viewAsTextRadioButtonMenuItem);

		headerExistsCheckBox = new JCheckBox("Include Header");
		viewAsDataRadioButtonMenuItem = new StayOpenRadioButtonMenuItem("View as Data");
		viewAsDataRadioButtonMenuItem.setFont(MENU_FONT);
		viewAsDataRadioButtonMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TextViewerPanel selectedPanel = (TextViewerPanel) tabbedPane.getSelectedComponent();
				selectedPanel.setShowDataView(true);
				dataOptionsPanel.setVisible(true);
				textOptionsPanel.setVisible(false);
				headerExistsCheckBox.setSelected(true);
			}
		});
		textOrDataGroup.add(viewAsDataRadioButtonMenuItem);

		// general view items

		JMenu fontSubMenu = new JMenu("Font:");
		fontSubMenu.setFont(MENU_FONT);
		ButtonGroup fontGroup = new ButtonGroup();
		List<JRadioButtonMenuItem> fontRadioItems = new ArrayList<JRadioButtonMenuItem>();
		for (Font font : monoSpacedFonts) {
			JRadioButtonMenuItem fontItem = new StayOpenRadioButtonMenuItem(font.getFontName());
			if (font.getFontName().equals(textFont.getFontName())) {
				fontItem.setSelected(true);
			}
			fontRadioItems.add(fontItem);
			fontItem.setFont(font);
			fontGroup.add(fontItem);
			fontSubMenu.add(fontItem);
			fontItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Font selectedFont = font;
					textFont = selectedFont.deriveFont((float) ((Integer) fontSizeSpinner.getValue()));
					updateCurrentTextViewerPanel();
				}
			});
		}
		menuView.add(fontSubMenu);

		JMenuItem fontSizeMenuItem = new JMenuItem("Font Size:");
		fontSizeMenuItem.setFont(MENU_FONT);
		fontSizeMenuItem.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		SpinnerModel numberModel = new SpinnerNumberModel(textFont.getSize(), 1, 30, 1);
		fontSizeSpinner = new JSpinner(numberModel);
		fontSizeSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				textFont = textFont.deriveFont((float) ((Integer) fontSizeSpinner.getValue()));
				updateCurrentTextViewerPanel();
			}
		});
		fontSizeMenuItem.add(fontSizeSpinner);
		menuView.add(fontSizeMenuItem);

		JMenuItem fontColorMenuItem = new JMenuItem("Font Color", ColorChooserHelper.createIcon(textColor));
		fontColorMenuItem.setFont(MENU_FONT);
		fontColorMenuItem.addActionListener(ColorChooserHelper.createActionListener(this, "Select Font Color", TextViewerColor.FONT, this));
		menuView.add(fontColorMenuItem);

		JMenuItem backgroundColorMenuItem = new JMenuItem("Background Color", ColorChooserHelper.createIcon(backgroundTextPanelColor));
		backgroundColorMenuItem.setFont(MENU_FONT);
		backgroundColorMenuItem.addActionListener(ColorChooserHelper.createActionListener(this, "Select Background Color", TextViewerColor.BACKGROUND, this));
		menuView.add(backgroundColorMenuItem);

		JCheckBoxMenuItem showLineNumbersCheckBox = new JCheckBoxMenuItem("Show Line Numbers");
		showLineNumbersCheckBox.setFont(MENU_FONT);
		showLineNumbersCheckBox.setSelected(showLineNumbers);
		showLineNumbersCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("checked.");
				TextViewerPanel selectedPanel = (TextViewerPanel) tabbedPane.getSelectedComponent();
				showLineNumbers = showLineNumbersCheckBox.isSelected();
				selectedPanel.showLineNumbers(showLineNumbers);
			}
		});
		menuView.add(showLineNumbersCheckBox);

		menuView.addSeparator();
		// end general view items

		menuView.add(viewAsTextRadioButtonMenuItem);
		menuView.add(viewAsDataRadioButtonMenuItem);
		menuView.addSeparator();

		textOptionsPanel
				.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Text Settings", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, SMALL_MENU_FONT));

		JPanel tabSizePanel = new JPanel();
		JLabel tabSizeLabel = new JLabel("Tab Size:");
		tabSizeLabel.setFont(SMALL_MENU_FONT);
		tabSizePanel.add(tabSizeLabel);
		numberModel = new SpinnerNumberModel(0, 0, 200, 1);
		tabSizeSpinner = new JSpinner(numberModel);
		tabSizeSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				TextViewerPanel selectedPanel = (TextViewerPanel) tabbedPane.getSelectedComponent();
				if (selectedPanel != null) {
					selectedPanel.setTabSize((Integer) tabSizeSpinner.getValue());
				}
			}
		});
		tabSizeSpinner.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				try {
					tabSizeSpinner.commitEdit();
				} catch (ParseException e1) {
					logger.warn(e1.getMessage(), e);
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
			}
		});
		tabSizePanel.add(tabSizeSpinner);
		textOptionsPanel.add(tabSizePanel);
		menuView.add(textOptionsPanel);

		dataOptionsPanel
				.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Data Settings", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, SMALL_MENU_FONT));

		JButton aboveHeaderColorMenuItem = new JButton("Above Header Color", ColorChooserHelper.createIcon(aboveDataHeaderBackgroundColor));
		aboveHeaderColorMenuItem.setFont(MENU_FONT);
		aboveHeaderColorMenuItem.addActionListener(ColorChooserHelper.createActionListener(this, "Select Above Header Color", TextViewerColor.ABOVE_HEADER_BACKGROUND, this));
		dataOptionsPanel.add(aboveHeaderColorMenuItem);

		JButton headerColorMenuItem = new JButton("Header Color", ColorChooserHelper.createIcon(dataHeaderBackgroundColor));
		headerColorMenuItem.setFont(MENU_FONT);
		headerColorMenuItem.addActionListener(ColorChooserHelper.createActionListener(this, "Select Header Color", TextViewerColor.HEADER_BACKGROUND, this));
		dataOptionsPanel.add(headerColorMenuItem);

		JButton lineColorMenuItem = new JButton("Data Lines Color", ColorChooserHelper.createIcon(dataLineColor));
		lineColorMenuItem.setFont(MENU_FONT);
		lineColorMenuItem.addActionListener(ColorChooserHelper.createActionListener(this, "Select Data Lines Color", TextViewerColor.LINES, this));
		dataOptionsPanel.add(lineColorMenuItem);

		headerExistsCheckBox.setFont(SMALL_MENU_FONT);
		dataOptionsPanel.add(headerExistsCheckBox);

		JPanel headerLinePanel = new JPanel();
		headerLineLabel = new JLabel("Line Number:");
		headerLinePanel.setFont(SMALL_MENU_FONT);
		headerLinePanel.add(headerLineLabel);
		numberModel = new SpinnerNumberModel(0, 0, 1000000, 1);
		headerLineSpinner = new JSpinner(numberModel);
		headerLineSpinner.setPreferredSize(new Dimension(50, (int) headerLineSpinner.getPreferredSize().getHeight()));
		headerLineSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				TextViewerPanel selectedPanel = (TextViewerPanel) tabbedPane.getSelectedComponent();
				if (selectedPanel != null) {
					selectedPanel.setHeaderLineNumber((Integer) headerLineSpinner.getValue());
					updateCurrentTextViewerPanel();
				}
			}
		});
		headerLinePanel.add(headerLineSpinner);

		headerExistsCheckBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean headerExists = headerExistsCheckBox.isSelected();
				headerLineLabel.setEnabled(headerExists);
				headerLineSpinner.setEnabled(headerExists);
				TextViewerPanel selectedPanel = (TextViewerPanel) tabbedPane.getSelectedComponent();
				if (selectedPanel != null) {
					selectedPanel.setShowHeader(headerExists);
					if (headerExists) {
						if (selectedPanel.getHeaderLineNumber() == 0) {
							int defaultHeaderLineNumber = selectedPanel.getDefaultHeaderLineNumber();
							selectedPanel.setHeaderLineNumber(defaultHeaderLineNumber);
							headerLineSpinner.setValue(defaultHeaderLineNumber);
						} else {
							headerLineSpinner.setValue(selectedPanel.getHeaderLineNumber());
						}
					}
					updateCurrentTextViewerPanel();
				}
			}
		});

		dataOptionsPanel.add(headerLinePanel);
		menuView.add(dataOptionsPanel);
		mainMenuBar.add(menuView);

		JMenu menuHelp = new JMenu("Help");
		menuHelp.setFont(MENU_FONT);
		JMenuItem menuItemAbout = new JMenuItem("About", ABOUT_ICON);
		menuItemAbout.setFont(MENU_FONT);
		menuHelp.add(menuItemAbout);

		menuItemAbout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				JPanel panel = new JPanel(new BorderLayout());
				panel.add(new JLabel(new ImageIcon(APPLICATION_ICON)), BorderLayout.LINE_START);
				JPanel innerPanel = new JPanel();
				innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.PAGE_AXIS));
				innerPanel.add(new JLabel("Text Viewer"));
				innerPanel.add(new JLabel("Version: " + APPLICATION_VERSION));
				innerPanel.add(new JLabel("Copyright 2017 Roche NimbleGen, Inc."));
				panel.add(innerPanel, BorderLayout.CENTER);
				panel.add(new JLabel(new ImageIcon(BACKGROUND_LOGO)), BorderLayout.LINE_END);
				JOptionPane.showMessageDialog(TextViewer.this, panel, "About Text Viewer", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		mainMenuBar.add(menuHelp);
	}

	private void updateMenu() {
		TextViewerPanel selectedPanel = (TextViewerPanel) tabbedPane.getSelectedComponent();
		if (selectedPanel != null) {
			setTitle(APPLICATION_NAME + " " + APPLICATION_VERSION + " | " + selectedPanel.getFile().getName());

			if (selectedPanel.isShowDataView()) {
				viewAsDataRadioButtonMenuItem.setSelected(true);
				dataOptionsPanel.setVisible(true);
				textOptionsPanel.setVisible(false);
			} else {
				viewAsTextRadioButtonMenuItem.setSelected(true);
				dataOptionsPanel.setVisible(false);
				textOptionsPanel.setVisible(true);
			}
			tabSizeSpinner.setValue(selectedPanel.getTabSize());
			boolean headerExists = selectedPanel.isShowHeader();
			headerExistsCheckBox.setSelected(headerExists);
			headerLineLabel.setEnabled(headerExists);
			headerLineSpinner.setEnabled(headerExists);
			selectedPanel.setHeaderLineNumber(selectedPanel.getHeaderLineNumber());

			if (selectedPanel.isShowDataView()) {
				viewAsDataRadioButtonMenuItem.setSelected(true);
			} else {
				viewAsTextRadioButtonMenuItem.setSelected(true);
			}

			updateCurrentTextViewerPanel();

		} else {
			setTitle(APPLICATION_NAME + " " + APPLICATION_VERSION);
		}

	}

	private void closeAllFiles() {
		for (TextViewerPanel panel : textViewerPanels) {
			tabbedPane.remove(panel);
			try {
				panel.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		textViewerPanels.clear();

	}

	public void updateCurrentTextViewerPanel() {
		TextViewerPanel selectedPanel = (TextViewerPanel) tabbedPane.getSelectedComponent();
		if (selectedPanel != null) {
			selectedPanel.updateTextInViewer();
		}
	}

	public Color getDataHeaderBackgroundColor() {
		return dataHeaderBackgroundColor;
	}

	public Color getAboveDataHeaderBackgroundColor() {
		return aboveDataHeaderBackgroundColor;
	}

	public void setDataHeaderBackgroundColor(Color dataHeaderBackgroundColor) {
		this.dataHeaderBackgroundColor = dataHeaderBackgroundColor;
	}

	public void setAboveDataHeaderBackgroundColor(Color aboveDataHeaderBackgroundColor) {
		this.aboveDataHeaderBackgroundColor = aboveDataHeaderBackgroundColor;
	}

	public Color getDataLineColor() {
		return dataLineColor;
	}

	public void setDataLineColor(Color dataLineColor) {
		this.dataLineColor = dataLineColor;
	}

	public Font getTextFont() {
		return textFont;
	}

	public Color getTextColor() {
		return textColor;
	}

	public void setTextColor(Color color) {
		textColor = color;
	}

	public Color getBackgroundTextPanelColor() {
		return backgroundTextPanelColor;
	}

	public void setBackgroundTextPanelColor(Color color) {
		backgroundTextPanelColor = color;
	}

	private void saveFrameLocationPreferences() {
		preferences.put(LAST_VIEW_FRAME_X_PROPERTIES_KEY, "" + (int) (getLocation().getX()));
		preferences.put(LAST_VIEW_FRAME_Y_PROPERTIES_KEY, "" + (int) (getLocation().getY()));
		preferences.put(LAST_VIEW_FRAME_WIDTH_PROPERTIES_KEY, "" + (int) (getWidth()));
		preferences.put(LAST_VIEW_FRAME_HEIGHT_PROPERTIES_KEY, "" + (int) (getHeight()));
	}

	public TextViewerPanel readInFile(File file) {
		return readInFile(file, null);
	}

	private static interface IStuffToDoAfterFileIsLoaded {
		void doAfterFileIsLoaded(TextViewerPanel textViewerPanel);
	}

	public TextViewerPanel readInFile(File file, IStuffToDoAfterFileIsLoaded runAfterFileIsLoaded) {
		TextViewerPanel textViewerPanel = null;

		boolean fileIsAlreadyOpen = false;
		tabLoop: for (int tabIndex = 0; tabIndex < tabbedPane.getTabCount(); tabIndex++) {
			Component component = tabbedPane.getComponentAt(tabIndex);
			if (component instanceof TextViewerPanel) {
				textViewerPanel = (TextViewerPanel) component;
				File panelFile = textViewerPanel.getFile();
				if (panelFile.equals(file)) {
					tabbedPane.setSelectedComponent(component);
					fileIsAlreadyOpen = true;
					break tabLoop;
				}
			}
		}

		if (!fileIsAlreadyOpen) {

			textViewerPanel = new TextViewerPanel(this, file);
			TextViewerPanel selectedPanel = (TextViewerPanel) tabbedPane.getSelectedComponent();
			if (selectedPanel != null) {
				selectedPanel.setGeneralStatusText("");
			}

			textViewerPanel.setTransferHandler(new DragAndDropFileTransferHandler(this));

			textViewerPanels.add(textViewerPanel);
			tabbedPane.addTab(file.getName(), textViewerPanel);
			tabbedPane.setTabComponentAt(tabbedPane.indexOfComponent(textViewerPanel), createTabLabelPanel(textViewerPanel));

			tabbedPane.setSelectedComponent(textViewerPanel);

			final TextViewerPanel finalTextViewerPanel = textViewerPanel;
			textViewerPanel.indexingSwingWorker = new SwingWorker<String, String>() {
				@Override
				protected String doInBackground() throws Exception {
					finalTextViewerPanel.loadFile();
					if (runAfterFileIsLoaded != null) {
						runAfterFileIsLoaded.doAfterFileIsLoaded(finalTextViewerPanel);
					}
					return null;
				}
			};
			textViewerPanel.indexingSwingWorker.execute();
			// open the file

		}
		updateCloseAllMenuItem();
		return textViewerPanel;
	}

	private JPanel createTabLabelPanel(TextViewerPanel textViewerPanel) {
		JPanel panelTabLabel = new JPanel(new GridBagLayout());
		panelTabLabel.setOpaque(false);
		String title = StringUtil.reduceString(textViewerPanel.getFile().getName(), MAX_TAB_TITLE_LENGTH);
		JLabel labelTitle = new JLabel(title + " ");
		labelTitle.setToolTipText(textViewerPanel.getFile().getAbsolutePath());
		JButton buttonClose = new JButton(CLOSE_ICON);
		buttonClose.setBorder(null);
		buttonClose.setRolloverIcon(CLOSE_HOVER_ICON);
		buttonClose.setPreferredSize(new Dimension(CLOSE_ICON.getIconWidth(), CLOSE_ICON.getIconHeight()));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;

		panelTabLabel.add(labelTitle, gbc);

		gbc.gridx++;
		gbc.weightx = 0;
		panelTabLabel.add(buttonClose, gbc);

		buttonClose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				closePanel(textViewerPanel);
			}
		});

		labelTitle.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				tabbedPane.setSelectedComponent(textViewerPanel);
			}
		});

		return panelTabLabel;
	}

	public void closePanel(TextViewerPanel textViewerPanel) {
		try {
			textViewerPanel.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		textViewerPanels.remove(textViewerPanel);
		tabbedPane.remove(textViewerPanel);
	}

	public static File getViewFileForLoadingFromUser(JFrame parentFrame) {
		File dataFile = null;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setApproveButtonText("Open File");
		fileChooser.setDialogTitle("Choose File to Open");

		String lastDirectory = preferences.get(LAST_VIEW_DIRECTORY_PROPERTIES_KEY, null);
		if (lastDirectory != null) {
			fileChooser.setCurrentDirectory(new File(lastDirectory));
		}
		int returnValue = fileChooser.showOpenDialog(parentFrame);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			dataFile = fileChooser.getSelectedFile();
			preferences.put(LAST_VIEW_DIRECTORY_PROPERTIES_KEY, dataFile.getParent());
		}
		return dataFile;
	}

	private void drawBackground(Graphics graphics, int screenWidth, int screenHeight) {
		graphics.setColor(BACKGROUND_COLOR);
		graphics.fillRect(0, 0, screenWidth, screenHeight);
		int imageWidth = BACKGROUND_LOGO.getWidth(null);
		int imageHeight = BACKGROUND_LOGO.getHeight(null);

		for (int x = 0; x <= screenWidth; x += imageWidth + BACKGROUND_IMAGE_HORIZONTAL_SPACING) {
			for (int y = 0; y <= screenHeight; y += imageHeight + BACKGROUND_IMAGE_VERTICAL_SPACING) {
				graphics.drawImage(BACKGROUND_LOGO, x, y, null);
			}
		}
	}

	public static void main(String[] args) {
		TextViewer textViewer = new TextViewer();

		for (String arg : args) {
			File file = new File(arg);
			if (file.exists()) {
				textViewer.readInFile(file);
			}
		}
	}

	public void updateCloseAllMenuItem() {
		menuItemCloseAllFiles.setEnabled(tabbedPane.getTabCount() > 0);
	}

}
