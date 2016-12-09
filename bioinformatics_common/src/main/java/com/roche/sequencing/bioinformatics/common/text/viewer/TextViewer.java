package com.roche.sequencing.bioinformatics.common.text.viewer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.FontUIResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.sequencing.bioinformatics.common.text.GZipIndex;
import com.roche.sequencing.bioinformatics.common.text.GZipIndexer;
import com.roche.sequencing.bioinformatics.common.text.GZipIndexer.GZipIndexPair;
import com.roche.sequencing.bioinformatics.common.text.ITextProgressListener;
import com.roche.sequencing.bioinformatics.common.text.ProgressUpdate;
import com.roche.sequencing.bioinformatics.common.text.TextFileIndex;
import com.roche.sequencing.bioinformatics.common.text.TextFileIndexer;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.InputStreamFactory;
import com.roche.sequencing.bioinformatics.common.utils.JavaUiUtil;
import com.roche.sequencing.bioinformatics.common.utils.ListUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.gzip.BamByteDecoder;
import com.roche.sequencing.bioinformatics.common.utils.gzip.GZipUtil;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IByteDecoder;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IBytes;
import com.roche.sequencing.bioinformatics.common.utils.gzip.RandomAccessFileBytes;

public class TextViewer extends JFrame {

	private final static Logger logger = LoggerFactory.getLogger(TextViewer.class);
	private static final long serialVersionUID = 1L;

	private final static DecimalFormat DF = new DecimalFormat("###,###");

	public final static String APPLICATION_NAME = "Text Viewer";
	public final static String APPLICATION_VERSION = "v1";
	public final static Image APPLICATION_ICON = new ImageIcon(TextViewer.class.getResource("largeTextViewerIcon.png")).getImage();
	private final static Dimension MINIMUM_PANEL_DIMENSION = new Dimension(900, 400);
	public final static FontUIResource DEFAULT_FONT = new FontUIResource("Serif", Font.PLAIN, 12);

	private final static ImageIcon CLOSE_ICON = new ImageIcon(TextViewer.class.getResource("close.png"));
	private final static ImageIcon CLOSE_HOVER_ICON = new ImageIcon(TextViewer.class.getResource("close_hover.png"));
	private final static ImageIcon EXIT_ICON = new ImageIcon(TextViewer.class.getResource("exit.png"));
	private final static ImageIcon IMPORT_DATA_ICON = new ImageIcon(TextViewer.class.getResource("import_data.png"));

	private static Preferences preferences = Preferences.userRoot().node(TextViewer.class.getName());
	private final static String LAST_VIEW_DIRECTORY_PROPERTIES_KEY = "lastview.lastdirectory";
	private final static String LAST_VIEW_LAST_OPENED_FILES_PROPERTIES_KEY = "lastview.lastopenedfiles";
	private final static String LAST_VIEW_LAST_OPENED_FILES_POSITIONS_PROPERTIES_KEY = "lastview.lastopenedfilespositions";
	private final static String LAST_VIEW_FRAME_X_PROPERTIES_KEY = "lastview.x";
	private final static String LAST_VIEW_FRAME_Y_PROPERTIES_KEY = "lastview.y";
	private final static String LAST_VIEW_FRAME_WIDTH_PROPERTIES_KEY = "lastview.framewidth";
	private final static String LAST_VIEW_FRAME_HEIGHT_PROPERTIES_KEY = "lastview.frameheight";
	private final static String LAST_VIEW_WAS_MAXIMIZED_PROPERTIES_KEY = "lastview.wasmaximized";

	private final static Color BACKGROUND_COLOR = new Color(0.80f, 0.80f, 0.80f, 1.0f);
	private final static Image BACKGROUND_LOGO = new ImageIcon(TextViewer.class.getResource("backgroundLogo.png")).getImage();
	private final static int BACKGROUND_IMAGE_HORIZONTAL_SPACING = 100;
	private final static int BACKGROUND_IMAGE_VERTICAL_SPACING = 100;

	private final static String PROPERTIES_DELIMITER = "--NEXT_FILE--";

	private final static int MAX_TAB_TITLE_LENGTH = 30;

	public final static Cursor TEXT_CURSOR = new Cursor(Cursor.TEXT_CURSOR);

	private final static int LINES_FOR_EACH_INDEX = 100;

	private final static String INDEX_EXTENSION = "idx";
	private final static String GZIP_DICTIONARY_EXTENSION = "gzdict";
	private final static String GZIP_INDEX_EXTENSION = "gzx";
	private final static String GZIP_FILE_EXTENSION = "gz";
	final static String BAM_FILE_EXTENSION = "bam";
	private final static String BAM_BLOCK_INDEX = "bamblockindex";
	private final static String INDEX_DIR = "text_viewer_indexes";

	private final JTabbedPane tabbedPane;
	private final List<TextViewerPanel> textViewerPanels;

	public TextViewer() {

		setIconImage(APPLICATION_ICON);
		setMinimumSize(MINIMUM_PANEL_DIMENSION);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			logger.error(e.getMessage(), e);
		}

		JavaUiUtil.setUIFont(DEFAULT_FONT);

		JMenuBar mainMenuBar = new JMenuBar();
		mainMenuBar.setBorder(BorderFactory.createLineBorder(Color.black));
		setJMenuBar(mainMenuBar);
		JMenu file = new JMenu("File");
		JMenuItem openMenuItem = new JMenuItem("Open", IMPORT_DATA_ICON);
		openMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File fileToLoad = getViewFileForLoadingFromUser(TextViewer.this);
				if (fileToLoad != null) {
					readInFile(fileToLoad);
				}
			}
		});
		file.add(openMenuItem);

		JMenuItem quitMenuItem = new JMenuItem("Quit", EXIT_ICON);
		quitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		file.add(quitMenuItem);

		mainMenuBar.add(file);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setTitle(APPLICATION_NAME + " " + APPLICATION_VERSION);
		setVisible(true);

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
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				TextViewerPanel selectedPanel = (TextViewerPanel) tabbedPane.getSelectedComponent();
				if (selectedPanel != null) {
					setTitle(APPLICATION_NAME + " " + APPLICATION_VERSION + " | " + selectedPanel.getFile().getName());
				} else {
					setTitle(APPLICATION_NAME + " " + APPLICATION_VERSION);
				}
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

		tabbedPane.setTransferHandler(new DragAndDropFileTransferHandler(this));
		setTransferHandler(new DragAndDropFileTransferHandler(this));

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				boolean isMaximized = TextViewer.this.getExtendedState() == JFrame.MAXIMIZED_BOTH;
				preferences.put(LAST_VIEW_WAS_MAXIMIZED_PROPERTIES_KEY, "" + isMaximized);
				saveFrameLocationPreferences();
				List<String> fileNames = new ArrayList<String>();
				List<String> lineNumbers = new ArrayList<String>();
				for (TextViewerPanel textViewerPanel : textViewerPanels) {
					fileNames.add(textViewerPanel.getFile().getAbsolutePath());
					lineNumbers.add("" + textViewerPanel.getCurrentLineNumber());
				}
				preferences.put(LAST_VIEW_LAST_OPENED_FILES_PROPERTIES_KEY, ListUtil.toString(fileNames, PROPERTIES_DELIMITER));
				preferences.put(LAST_VIEW_LAST_OPENED_FILES_POSITIONS_PROPERTIES_KEY, ListUtil.toString(lineNumbers, PROPERTIES_DELIMITER));

				for (TextViewerPanel textViewerPanel : textViewerPanels) {
					try {
						textViewerPanel.close();
					} catch (IOException e) {
						logger.warn(e.getMessage(), e);
					}
				}
			}
		});

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

			String lastOpenedFileNames = preferences.get(LAST_VIEW_LAST_OPENED_FILES_PROPERTIES_KEY, null);
			if (lastOpenedFileNames != null) {
				String[] split = lastOpenedFileNames.split(PROPERTIES_DELIMITER);

				for (int i = 0; i < split.length; i++) {
					String lastOpenedFileName = split[i];
					if (!lastOpenedFileName.isEmpty()) {
						File lastOpenedFile = new File(lastOpenedFileName);
						TextViewerPanel textViewerPanel = readInFile(lastOpenedFile);

						if (textViewerPanel != null) {
							if (i < lastOpenedFilePositions.size()) {
								int lineNumber = lastOpenedFilePositions.get(i);
								textViewerPanel.setLineNumber(lineNumber);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.warn("Unable to reinitialize all files.", e);
		}

	}

	private void saveFrameLocationPreferences() {
		preferences.put(LAST_VIEW_FRAME_X_PROPERTIES_KEY, "" + (int) (getLocation().getX()));
		preferences.put(LAST_VIEW_FRAME_Y_PROPERTIES_KEY, "" + (int) (getLocation().getY()));
		preferences.put(LAST_VIEW_FRAME_WIDTH_PROPERTIES_KEY, "" + (int) (getWidth()));
		preferences.put(LAST_VIEW_FRAME_HEIGHT_PROPERTIES_KEY, "" + (int) (getHeight()));
	}

	public static boolean isFileIndexed(File file) {
		boolean isFileIndexed = false;

		if (file.exists()) {
			// check if there is an index file
			// prepend a '.' to the file name so it is possibly hidden or at least separates from actual file name when files
			// are listed
			File indexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + INDEX_EXTENSION);
			boolean isGzipFile = FileUtil.getFileExtension(file).toLowerCase().endsWith(GZIP_FILE_EXTENSION);
			boolean isBamFile = FileUtil.getFileExtension(file).toLowerCase().endsWith(BAM_FILE_EXTENSION);
			if (isGzipFile || isBamFile || GZipUtil.isCompressed(file)) {
				indexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + FileUtil.getFileNameWithoutExtension(file.getName()) + "."
						+ INDEX_EXTENSION);
				File gZipIndexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + GZIP_INDEX_EXTENSION);
				File gZipDictionaryFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + GZIP_DICTIONARY_EXTENSION);
				if (isBamFile) {
					File bamBlockIndexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + BAM_BLOCK_INDEX);
					isFileIndexed = (gZipIndexFile.exists() && gZipDictionaryFile.exists() && bamBlockIndexFile.exists());
				} else {
					isFileIndexed = (gZipIndexFile.exists() && gZipDictionaryFile.exists());
				}
			} else {
				isFileIndexed = indexFile.exists();
			}
		}
		return isFileIndexed;
	}

	public static Indexes indexFile(File file, ITextProgressListener progressListener) {
		TextFileIndex textFileIndex = null;
		GZipIndex gZipIndex = null;
		IBytes gZipDictionaryBytes = null;
		File bamBlockIndexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + BAM_BLOCK_INDEX);

		// check if there is an index file
		// prepend a '.' to the file name so it is possibly hidden or at least separates from actual file name when files
		// are listed
		File indexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + INDEX_EXTENSION);
		boolean isGzipFile = FileUtil.getFileExtension(file).toLowerCase().endsWith(GZIP_FILE_EXTENSION);
		boolean isBamFile = FileUtil.getFileExtension(file).toLowerCase().endsWith(BAM_FILE_EXTENSION);
		if (isGzipFile || isBamFile || GZipUtil.isCompressed(file)) {
			indexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + FileUtil.getFileNameWithoutExtension(file.getName()) + "."
					+ INDEX_EXTENSION);
			File gZipIndexFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + GZIP_INDEX_EXTENSION);
			File gZipDictionaryFile = new File(file.getParentFile().getAbsolutePath() + File.separator + INDEX_DIR + File.separator + "." + file.getName() + "." + GZIP_DICTIONARY_EXTENSION);
			if (gZipIndexFile.exists() && gZipDictionaryFile.exists()) {
				try {
					gZipIndex = GZipIndexer.loadIndexFile(gZipIndexFile, gZipDictionaryFile);
					gZipDictionaryBytes = new RandomAccessFileBytes(new RandomAccessFile(gZipDictionaryFile, "r"));
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}
			}

			if (gZipIndex == null || gZipDictionaryBytes == null || (isBamFile && !bamBlockIndexFile.exists())) {
				try {

					IByteDecoder byteConverter = null;
					if (isBamFile) {
						byteConverter = new BamByteDecoder();
					}

					GZipIndexPair gZipIndexPair = GZipIndexer.indexGZipBlocks(new InputStreamFactory(file), gZipDictionaryFile, LINES_FOR_EACH_INDEX, progressListener, byteConverter);
					if (byteConverter != null) {
						byteConverter.persistToFile(bamBlockIndexFile);
					}

					gZipIndex = gZipIndexPair.getGzipIndex();
					textFileIndex = gZipIndexPair.getTextFileIndex();
					gZipDictionaryBytes = new RandomAccessFileBytes(new RandomAccessFile(gZipDictionaryFile, "r"));

					GZipIndexer.saveGZipIndexToFile(gZipIndex, gZipIndexFile);
					TextFileIndexer.saveIndexedTextToFile(textFileIndex, indexFile);
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}

		if (indexFile.exists()) {// && searcherIndexFile.exists()) {
			try {
				textFileIndex = TextFileIndexer.loadIndexFile(indexFile);
				// textSearcher = new TextSearcher(searcherIndexFile);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		if (textFileIndex == null) {
			// TODO if the file is larger than a set size load a sample and display it first before creating the index
			// assuming that the user would initially want to see the format of the file and maybe doesn't even want
			// to see the whole file

			try {
				textFileIndex = TextFileIndexer.indexText(file, LINES_FOR_EACH_INDEX, progressListener);
				TextFileIndexer.saveIndexedTextToFile(textFileIndex, indexFile);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}

		return new Indexes(textFileIndex, gZipIndex, gZipDictionaryBytes, bamBlockIndexFile);
	}

	public TextViewerPanel readInFile(File file) {
		TextViewerPanel textViewerPanel = null;
		RandomAccessFile randomAccessToFile = null;

		// open the file
		try {
			randomAccessToFile = new RandomAccessFile(file, "r");

			if (randomAccessToFile != null) {
				ITextProgressListener progressListener = new ITextProgressListener() {

					@Override
					public void progressOccurred(ProgressUpdate progressUpdate) {
						System.out.println(progressUpdate);
						// TODO create a ui element to display this information
						// System.out.println(progressUpdate.getPercentComplete() + "% complete  Lines Reads:" + progressUpdate.getLinesRead() + " Estimated Completion:"
						// + progressUpdate.getEstimatedTimeToCompletionInHHMMSSMMM() + " Estimated Completion Time:" + progressUpdate.getEstimatedCompletionTimeInYYYYMMDDHHMMSS());
					}
				};

				Indexes indexes = indexFile(file, progressListener);

				textViewerPanel = new TextViewerPanel(this, file, randomAccessToFile, indexes.getTextFileIndex(), indexes.getgZipIndex(), indexes.getgZipDictionaryBytes(),
						indexes.getBamBlockIndexFile());

				textViewerPanel.setTransferHandler(new DragAndDropFileTransferHandler(this));

				textViewerPanels.add(textViewerPanel);
				tabbedPane.addTab(file.getName(), textViewerPanel);
				tabbedPane.setTabComponentAt(tabbedPane.indexOfComponent(textViewerPanel), createTabLabelPanel(textViewerPanel));

				tabbedPane.setSelectedComponent(textViewerPanel);
			}
		} catch (FileNotFoundException e1) {
			JOptionPane.showMessageDialog(this, "Unable to open file[" + file.getAbsolutePath() + "].", "Error Opening File", JOptionPane.ERROR_MESSAGE);
		}
		return textViewerPanel;
	}

	public static class Indexes {
		private final TextFileIndex textFileIndex;
		private final GZipIndex gZipIndex;
		private final IBytes gZipDictionaryBytes;
		private final File bamBlockIndexFile;

		public Indexes(TextFileIndex textFileIndex, GZipIndex gZipIndex, IBytes gZipDictionaryBytes, File bamBlockIndexFile) {
			super();
			this.textFileIndex = textFileIndex;
			this.gZipIndex = gZipIndex;
			this.gZipDictionaryBytes = gZipDictionaryBytes;
			this.bamBlockIndexFile = bamBlockIndexFile;
		}

		public TextFileIndex getTextFileIndex() {
			return textFileIndex;
		}

		public GZipIndex getgZipIndex() {
			return gZipIndex;
		}

		public IBytes getgZipDictionaryBytes() {
			return gZipDictionaryBytes;
		}

		public File getBamBlockIndexFile() {
			return bamBlockIndexFile;
		}

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
				try {
					textViewerPanel.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				textViewerPanels.remove(textViewerPanel);
				tabbedPane.remove(textViewerPanel);
			}
		});

		labelTitle.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				tabbedPane.setSelectedComponent(textViewerPanel);
			}
		});

		return panelTabLabel;
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
}
