package com.roche.sequencing.bioinformatics.common.text.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.text.Document;
import com.roche.sequencing.bioinformatics.common.text.GZipIndex;
import com.roche.sequencing.bioinformatics.common.text.IDocument;
import com.roche.sequencing.bioinformatics.common.text.ITextProgressListener;
import com.roche.sequencing.bioinformatics.common.text.ProgressUpdate;
import com.roche.sequencing.bioinformatics.common.text.TextFileIndex;
import com.roche.sequencing.bioinformatics.common.text.TextPosition;
import com.roche.sequencing.bioinformatics.common.text.viewer.TextViewerUtil.Indexes;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.GraphicsUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.gzip.BamByteDecoder;
import com.roche.sequencing.bioinformatics.common.utils.gzip.GZipBlock;
import com.roche.sequencing.bioinformatics.common.utils.gzip.GZipUtil;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IByteDecoder;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IBytes;

public class TextViewerPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final static DecimalFormat DF = new DecimalFormat("###,###");

	private final static Color DISABLED_COLOR = new Color(0.4f, 0.4f, 0.4f, 0.8f);

	private final static int INDEXING_PANEL_WIDTH = 300;
	private final static int INDEXING_PANEL_HEIGHT = 100;
	public final static FontUIResource INDEXING_FONT = new FontUIResource("Serif", Font.PLAIN, 16);

	private final static int VERTICAL_SCROLLL_BAR_WIDTH = 20;
	private final static int HORIZONTAL_SCROLLL_BAR_HEIGHT = 20;
	private final static int STATUS_PANEL_HEIGHT = 20;

	public final static Cursor TEXT_CURSOR = new Cursor(Cursor.TEXT_CURSOR);

	private final static String ACTION_MAP_KEY_FOR_UP = "caret-up";
	private final static String ACTION_MAP_KEY_FOR_DOWN = "caret-down";

	private final static String ACTION_MAP_KEY_FOR_PAGE_UP = "page-up";
	private final static String ACTION_MAP_KEY_FOR_PAGE_DOWN = "page-down";

	private final static String ACTION_MAP_KEY_FOR_UP_WITH_SHIFT = "selection-up";
	private final static String ACTION_MAP_KEY_FOR_DOWN_WITH_SHIFT = "selection-down";

	private final static String ACTION_MAP_KEY_FOR_PAGE_UP_WITH_SHIFT = "selection-page-up";
	private final static String ACTION_MAP_KEY_FOR_PAGE_DOWN_WITH_SHIFT = "selection-page-down";

	// HOT PINK
	private final static Color DEFAULT_CARET_COLOR = new Color(255, 20, 147);
	private final static int DEFAULT_CARET_WIDTH = 3;

	private final static boolean DEFAULT_SHOW_LINE_NUMBERS = true;

	private final static Color LINE_NUMBER_AREA_BACKGROUND = new Color(225, 225, 225);

	// this needs to be a minimum of 2 because of the way data copying to clipboard is handled
	private final static int DATA_COLUMN_SPACING_IN_CHARACTERS = 2;

	private final static int MAX_LINES_TO_COPY_TO_CLIPBOARD = 1000;

	private final JTextPane lineNumberArea;
	private final JTextArea textArea;
	private final JViewport viewPort;
	private final JPanel statusPanel;

	private final JLabel currentPositionLabel;
	private final JLabel generalStatusLabel;
	private final JLabel fileInfoLabel;

	private JScrollBar verticalScrollBar;
	private JScrollBar horizontalScrollBar;

	private File file;
	private IDocument document;

	private final AtomicBoolean viewPortIsBeingSet;
	private final AtomicBoolean horizontalScrollBarIsBeingSet;

	private final List<Integer> lineStartPositionCharacterIndexesInView;

	private TextViewer parentTextViewer;

	private Integer currentZeroBasedStartingLineNumber;

	private boolean showLineNumbers;

	private boolean isShowDataView;

	private Map<Integer, Integer> maxLengthPerColum;

	private int headerLineNumber;
	private boolean showHeader;
	private String headerText;
	private boolean initIsDone;
	private BitSet dataPositionMapsToTextPosition;

	private TextPosition markInDocument;
	private AtomicBoolean selectedTextIsOffScreen;

	private final JLabel indexingLabel;
	private final JPanel indexingPanel;
	private final JProgressBar indexingProgressBar;
	private final JButton indexingCancelButton;
	SwingWorker<String, String> indexingSwingWorker;

	private final JLabel findingLabel;
	private final JPanel findingPanel;
	private final JProgressBar findingProgressBar;
	private final JButton findingCancelButton;
	private SwingWorker<String, String> findingSwingWorker;

	private String lastEnteredFindString;
	private int lastEnteredGoToLineNumber = 1;

	private boolean isIndexing;

	public TextViewerPanel(TextViewer parentTextViewer, File file) {
		super();
		this.initIsDone = false;
		this.isIndexing = false;

		this.file = file;
		setOpaque(false);

		this.parentTextViewer = parentTextViewer;

		this.isShowDataView = false;
		this.dataPositionMapsToTextPosition = new BitSet();

		lineStartPositionCharacterIndexesInView = new ArrayList<Integer>();
		viewPortIsBeingSet = new AtomicBoolean(false);
		horizontalScrollBarIsBeingSet = new AtomicBoolean(false);

		selectedTextIsOffScreen = new AtomicBoolean(false);

		showLineNumbers = DEFAULT_SHOW_LINE_NUMBERS;

		lineNumberArea = new JTextPane();
		lineNumberArea.setEditable(false);
		// center the text
		StyledDocument doc = lineNumberArea.getStyledDocument();
		SimpleAttributeSet center = new SimpleAttributeSet();
		StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
		doc.setParagraphAttributes(0, doc.getLength(), center, false);
		lineNumberArea.setForeground(Color.DARK_GRAY);

		lineNumberArea.setBackground(LINE_NUMBER_AREA_BACKGROUND);
		add(lineNumberArea);

		indexingLabel = new JLabel();
		indexingLabel.setFont(INDEXING_FONT);
		indexingLabel.setHorizontalAlignment(SwingConstants.CENTER);
		indexingProgressBar = new JProgressBar(0, 100);
		indexingProgressBar.setStringPainted(true);
		indexingCancelButton = new JButton("Cancel");
		indexingCancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (indexingSwingWorker != null) {
					indexingSwingWorker.cancel(true);
					parentTextViewer.closePanel(TextViewerPanel.this);
				}
			}
		});
		indexingPanel = new JPanel(new BorderLayout());
		indexingPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		indexingPanel.setBackground(Color.white);
		indexingPanel.add(indexingLabel, BorderLayout.PAGE_START);
		indexingPanel.add(indexingProgressBar, BorderLayout.CENTER);
		indexingPanel.add(indexingCancelButton, BorderLayout.PAGE_END);

		findingLabel = new JLabel();
		findingLabel.setFont(INDEXING_FONT);
		findingLabel.setHorizontalAlignment(SwingConstants.CENTER);
		findingProgressBar = new JProgressBar(0, 100);
		findingProgressBar.setStringPainted(true);
		findingCancelButton = new JButton("Cancel");
		findingCancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (findingSwingWorker != null) {
					findingSwingWorker.cancel(true);
				}
			}
		});
		findingPanel = new JPanel(new BorderLayout());
		findingPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		findingPanel.setBackground(Color.white);
		findingPanel.add(findingLabel, BorderLayout.PAGE_START);
		findingPanel.add(findingProgressBar, BorderLayout.CENTER);
		findingPanel.add(findingCancelButton, BorderLayout.PAGE_END);

		textArea = new JTextArea() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {

				Dimension letterSize = GraphicsUtil.getSizeOfText("W", parentTextViewer.getTextFont());
				int rowHeight = (int) letterSize.getHeight();
				int numberOfLines = (int) Math.floor(getHeight() / rowHeight);

				if (isShowDataView && maxLengthPerColum != null) {
					numberOfLines--;
					int startingLineNumber = verticalScrollBar.getValue();
					boolean headerLineIsShown = showHeader && (headerLineNumber > 0);// && (startingLineNumber >= headerLineNumber);

					int letterWidth = (int) letterSize.getWidth();

					if (headerLineIsShown) {
						int y = (headerLineNumber - startingLineNumber - 1) * rowHeight;
						if (startingLineNumber >= headerLineNumber) {
							y = 0;
						}

						g.setColor(parentTextViewer.getDataHeaderBackgroundColor());
						g.fillRect(0, y, getWidth(), rowHeight);

						g.setColor(parentTextViewer.getAboveDataHeaderBackgroundColor());
						g.fillRect(0, 0, getWidth(), y);
						g.setColor(parentTextViewer.getBackgroundTextPanelColor());
						g.fillRect(0, y + rowHeight, getWidth(), getHeight() - (y + rowHeight));
					} else {
						g.setColor(parentTextViewer.getBackgroundTextPanelColor());
						g.fillRect(0, 0, getWidth(), getHeight());
					}

					int[] columnXStart = new int[maxLengthPerColum.size()];
					int sum = 0;
					for (int columnIndex = 0; columnIndex < maxLengthPerColum.size(); columnIndex++) {
						columnXStart[columnIndex] = sum;
						sum += ((maxLengthPerColum.get(columnIndex) + DATA_COLUMN_SPACING_IN_CHARACTERS) * letterWidth);
					}

					int y = 0;
					for (int lineIndex = 0; lineIndex < numberOfLines; lineIndex++) {
						for (int columnIndex = 0; columnIndex < maxLengthPerColum.size(); columnIndex++) {
							int x = columnXStart[columnIndex];
							int width = ((maxLengthPerColum.get(columnIndex) + DATA_COLUMN_SPACING_IN_CHARACTERS) * letterWidth);

							g.setColor(parentTextViewer.getDataLineColor());
							g.drawRect(x, y, width, rowHeight);
						}
						y += rowHeight;
					}

					g.setColor(LINE_NUMBER_AREA_BACKGROUND);
					int leftOverWidth = (getWidth() - sum) - 1;
					g.fillRect(getWidth() - leftOverWidth, 0, leftOverWidth, getHeight());
				} else {
					g.setColor(parentTextViewer.getBackgroundTextPanelColor());
					g.fillRect(0, 0, getWidth(), getHeight());
				}

				g.setColor(LINE_NUMBER_AREA_BACKGROUND);
				int leftOverHeight = getHeight() - (numberOfLines * rowHeight);
				g.fillRect(0, getHeight() - leftOverHeight, getWidth(), leftOverHeight);

				if (isIndexing) {
					g.setColor(DISABLED_COLOR);
					// g.fillRect(0, 0, getWidth(), getHeight());
				}

				super.paintComponent(g);

			}

		};
		textArea.setOpaque(false);
		textArea.setEditable(false);
		textArea.setLineWrap(false);
		textArea.getCaret().setBlinkRate(250);
		textArea.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				textArea.getCaret().setVisible(true);
			}

			@Override
			public void focusLost(FocusEvent e) {
			}
		});
		setCaretColor(DEFAULT_CARET_COLOR);
		setCaretWidth(DEFAULT_CARET_WIDTH);

		textArea.setCursor(TEXT_CURSOR);
		textArea.addCaretListener(new CaretListener() {
			private int lastCaretPosition;

			@Override
			public void caretUpdate(CaretEvent e) {
				TextPosition textPosition = getCaretTextPosition();
				if (textPosition != null) {
					currentPositionLabel.setText("  Line:" + DF.format(textPosition.getLineNumber() + 1) + " Column:" + DF.format(textPosition.getColumnIndex() + 1));
				}
				if (isShowDataView && dataPositionMapsToTextPosition != null) {
					Caret caret = textArea.getCaret();
					int dotPosition = caret.getDot();
					int markPosition = caret.getMark();
					if (dotPosition == markPosition) {
						boolean isRightArrow = (dotPosition == (lastCaretPosition + 1));

						boolean doesMapToTextPosition = dataPositionMapsToTextPosition.get(dotPosition);
						while (!doesMapToTextPosition && dotPosition >= 0 && dotPosition < textArea.getText().length()) {
							if (isRightArrow) {
								dotPosition++;
							} else {
								dotPosition--;
							}
							doesMapToTextPosition = dataPositionMapsToTextPosition.get(dotPosition);
						}
						// this if statement is important as it is to avoid a stack overflow error due to infinite recursion.
						if (dotPosition != textArea.getCaretPosition()) {
							caret.setDot(dotPosition);
						}
					} else {
						boolean isDotLeftOfMark = (dotPosition < markPosition);

						boolean doesDotMapToTextPosition = dataPositionMapsToTextPosition.get(dotPosition);
						while (!doesDotMapToTextPosition && dotPosition >= 0 && dotPosition < textArea.getText().length()) {
							if (isDotLeftOfMark) {
								dotPosition++;
							} else {
								dotPosition--;
							}
							doesDotMapToTextPosition = dataPositionMapsToTextPosition.get(dotPosition);
						}

						boolean doesMarkMapToTextPosition = dataPositionMapsToTextPosition.get(markPosition);
						while (!doesMarkMapToTextPosition && markPosition >= 0 && markPosition < textArea.getText().length()) {
							if (isDotLeftOfMark) {
								markPosition--;
							} else {
								markPosition++;
							}
							doesMarkMapToTextPosition = dataPositionMapsToTextPosition.get(markPosition);
						}

						// this if statement is important as it is to avoid a stack overflow error due to infinite recursion.
						if (dotPosition != caret.getDot() || markPosition != caret.getMark()) {
							// this will set the mark
							caret.setDot(markPosition);
							caret.moveDot(dotPosition);

						}

					}
				}
				lastCaretPosition = textArea.getCaretPosition();
			}
		});

		// this is to fix a weird bug where the scrollpane tries to update the caret position
		// and causes the scroll pane's horizontal scroll bar to jump around (thus causing the text area
		// to jump around). Also note that this is why the scroll pane's horizontal scroll bar is not
		// being used in the ui.
		DefaultCaret caret = (DefaultCaret) textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

		viewPort = new JViewport();
		viewPort.setOpaque(false);
		viewPort.setView(textArea);
		add(viewPort);

		setLayout(null);

		verticalScrollBar = new JScrollBar(JScrollBar.VERTICAL);
		verticalScrollBar.setVisible(true);
		verticalScrollBar.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				updateTextInViewer();
			}
		});
		add(verticalScrollBar);

		horizontalScrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
		horizontalScrollBar.setVisible(true);
		horizontalScrollBar.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				updateViewPort();
			}
		});
		add(horizontalScrollBar);

		viewPort.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				moveVerticalScrollBar(e.getWheelRotation());
			}
		});

		viewPort.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				updateHorizontalScrollBar();
			}
		});

		statusPanel = new JPanel();
		statusPanel.setLayout(new ThreeInARowLayoutManager());
		statusPanel.setBackground(Color.LIGHT_GRAY);
		currentPositionLabel = new JLabel();
		generalStatusLabel = new JLabel();
		fileInfoLabel = new JLabel();
		statusPanel.add(currentPositionLabel);
		statusPanel.add(generalStatusLabel);
		statusPanel.add(fileInfoLabel);
		statusPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.black));
		add(statusPanel);

		add(indexingPanel);
		setComponentZOrder(indexingPanel, 0);
		indexingPanel.setVisible(false);

		add(findingPanel);
		setComponentZOrder(findingPanel, 1);
		findingPanel.setVisible(false);

		addComponentListener(new PanelResizeListener());

		bindKeys();
		this.initIsDone = true;
	}

	public void loadFile() throws FileNotFoundException {
		RandomAccessFile randomAccessToFile = new RandomAccessFile(file, "r");

		if (randomAccessToFile != null) {
			ITextProgressListener progressListener = new ITextProgressListener() {
				@Override
				public void progressOccurred(ProgressUpdate progressUpdate) {
					indexingLabel.setText("<html><div style='text-align: center;'>INDEXING<br>Time Left:" + progressUpdate.getEstimatedTimeToCompletionInHHMMSS() + "(HH:MM:SS)</div><html>");
					indexingProgressBar.setValue((int) progressUpdate.getPercentComplete());
				}
			};

			try {
				isIndexing = true;
				indexingLabel.setText("<html><div style='text-align: center;'>INDEXING<br>Time Left:  Calculating...</div><html>");
				indexingProgressBar.setValue(0);
				indexingPanel.setVisible(true);
				Indexes indexes = TextViewerUtil.indexFile(file, progressListener);
				loadDocument(file, randomAccessToFile, indexes.getTextFileIndex(), indexes.getgZipIndex(), indexes.getgZipDictionaryBytes(), indexes.getBamBlockIndexFile());
				indexingPanel.setVisible(false);
				isIndexing = false;
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "Unable to open " + file.getAbsolutePath() + ".  " + e.getMessage(), "Error Opening File", JOptionPane.ERROR_MESSAGE);
			}

		}
	}

	private void loadDocument(File file, RandomAccessFile randomAccessToFile, TextFileIndex textFileIndex, GZipIndex gZipIndex, IBytes gZipDictionaryBytes, File bamBlockIndexFile)
			throws FileNotFoundException {
		String fileExtension = FileUtil.getFileExtension(file).toLowerCase();
		boolean isBamFile = fileExtension.endsWith(TextViewerUtil.BAM_FILE_EXTENSION);
		boolean isProbeInfoFile = file.getName().toLowerCase().endsWith(TextViewerUtil.PROBE_INFO_FILE_ENDING_TEXT);

		IByteDecoder byteConverter = null;
		if (isBamFile) {
			try {
				GZipBlock firstBlock = GZipUtil.getFirstBlock(gZipIndex, file);
				if (firstBlock == null) {
					throw new IllegalStateException("Unable to extract the reference information in the provided bam file[" + file.getAbsolutePath() + "].");
				}
				BamByteDecoder bamByteDecoder = new BamByteDecoder(firstBlock.getUncompressedData(), bamBlockIndexFile);

				byteConverter = bamByteDecoder;
				this.isShowDataView = true;
				this.showHeader = true;
				this.headerLineNumber = bamByteDecoder.getHeaderLineNumber();
			} catch (IOException e1) {
				throw new IllegalStateException(e1.getMessage(), e1);
			}
		} else if (isProbeInfoFile) {
			this.isShowDataView = true;
			this.showHeader = true;
		}

		if (gZipIndex != null) {
			this.document = new Document(textFileIndex, gZipIndex, gZipDictionaryBytes, file, byteConverter);
		} else {
			this.document = new Document(textFileIndex, file, byteConverter);
		}

		int numberOfLinesThatCanFitInView = getNumberOfLinesThatCanFitInViewer();
		textArea.setRows(numberOfLinesThatCanFitInView);
		int charactersPerTab = textArea.getTabSize();
		int longestLine = textFileIndex.getNumberOfCharactersInLongestLine(charactersPerTab);
		textArea.setColumns(longestLine);

		fileInfoLabel.setText("File Size:" + FileUtil.getFileSizeLabel(this.file.length()) + "   File Length:" + DF.format(textFileIndex.getNumberOfLines()) + " Lines  ");
		updateScrollBar();
		updateComponentsLayout();
		parentTextViewer.updateMenu();
	}

	public void showLineNumbers(boolean shouldShowLineNumbers) {
		if (showLineNumbers != shouldShowLineNumbers) {
			showLineNumbers = shouldShowLineNumbers;
			updateComponentsLayout();
		}
	}

	public int getHeaderLineNumber() {
		return headerLineNumber;
	}

	private boolean doesHeaderExist() {
		return (headerLineNumber > 0) && showHeader && isShowDataView;
	}

	public void setHeaderLineNumber(int headerLineNumber) {
		this.headerLineNumber = headerLineNumber;
		if (headerLineNumber > 0) {
			this.headerText = document.getText(headerLineNumber - 1, headerLineNumber - 1)[0];
		}
		setShowHeader(headerLineNumber > 0);
	}

	public boolean isShowHeader() {
		return showHeader;
	}

	public void setShowHeader(boolean showHeader) {
		this.showHeader = showHeader;
	}

	/**
	 * @return the first non comment line
	 */
	public int getDefaultHeaderLineNumber() {
		int defaultLineNumber = 1;
		String[] allText = document.getText(0, 100);
		lineLoop: for (int i = 0; i < allText.length; i++) {
			String lineText = allText[i];
			if (!lineText.startsWith("#")) {
				defaultLineNumber = i + 1;
				break lineLoop;
			}
		}
		return defaultLineNumber;
	}

	public int getTabSize() {
		return textArea.getTabSize();
	}

	public void setTabSize(int tabSize) {
		textArea.setTabSize(tabSize);
		refreshScreen();
		updateTextInViewer();
	}

	public void setCaretColor(Color color) {
		textArea.setCaretColor(color);
	}

	public void setCaretWidth(int width) {
		textArea.putClientProperty("caretWidth", width);
	}

	private TextPosition getCaretTextPosition() {
		return getZeroBasedTextPosition(textArea.getCaretPosition());
	}

	private TextPosition getZeroBasedTextPosition(int position) {
		TextPosition zeroBasedTextPosition = null;
		int lineNumber = -1;
		int column = -1;

		int lastLineCharIndex = 0;
		// TODO possibly take data view into account here and utilize dataPositionMapsToTextPosition
		// to select the first selectable location
		lineLoop: for (int i = 0; i < lineStartPositionCharacterIndexesInView.size(); i++) {
			int curLineCharIndex = lineStartPositionCharacterIndexesInView.get(i);
			if (position >= lastLineCharIndex && position < curLineCharIndex) {
				lineNumber = currentZeroBasedStartingLineNumber + i - 1;
				column = position - lastLineCharIndex;
				zeroBasedTextPosition = new TextPosition(lineNumber, column);
				break lineLoop;
			}
			lastLineCharIndex = curLineCharIndex;
		}
		return zeroBasedTextPosition;
	}

	private Integer getTextAreaPosition(TextPosition textPosition) {
		Integer currentPosition = null;
		if (isTextPositionOnScreen(textPosition)) {
			int zeroBasedLineNumber = textPosition.getLineNumber();
			int lineIndexInTextArea = zeroBasedLineNumber - currentZeroBasedStartingLineNumber;
			int currentPositionFromLine = lineStartPositionCharacterIndexesInView.get(lineIndexInTextArea);
			currentPosition = currentPositionFromLine + textPosition.getColumnIndex();
		}
		return currentPosition;
	}

	private boolean isTextPositionOnScreen(TextPosition textPosition) {
		int lineNumber = textPosition.getLineNumber();
		boolean isOnScreen = (lineNumber >= currentZeroBasedStartingLineNumber) && (lineNumber < (currentZeroBasedStartingLineNumber + lineStartPositionCharacterIndexesInView.size() - 1));
		return isOnScreen;
	}

	public File getFile() {
		return file;
	}

	private void bindKeys() {
		InputMap inputMap = textArea.getInputMap(JPanel.WHEN_FOCUSED);
		ActionMap actionMap = textArea.getActionMap();
		bindKeys(inputMap, actionMap);
	}

	private void bindKeys(InputMap inputMap, ActionMap actionMap) {
		inputMap.put(KeyStroke.getKeyStroke("UP"), "up");
		inputMap.put(KeyStroke.getKeyStroke("DOWN"), "down");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_DOWN_MASK), "up");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK), "down");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0), "up");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0), "down");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "page_up");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "page_down");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, KeyEvent.SHIFT_DOWN_MASK), "up_w_shift");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, KeyEvent.SHIFT_DOWN_MASK), "down_w_shift");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_DOWN_MASK), "up_w_shift");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK), "down_w_shift");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.SHIFT_DOWN_MASK), "page_up_w_shift");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.SHIFT_DOWN_MASK), "page_down_w_shift");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK), "goto");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "find");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), "copy");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK), "sequence");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "loading");

		Action originalUpAction = actionMap.get(ACTION_MAP_KEY_FOR_UP);
		Action originalDownAction = actionMap.get(ACTION_MAP_KEY_FOR_DOWN);

		Action originalUpWithShiftAction = actionMap.get(ACTION_MAP_KEY_FOR_UP_WITH_SHIFT);
		Action originalDownWithShiftAction = actionMap.get(ACTION_MAP_KEY_FOR_DOWN_WITH_SHIFT);

		Action originalPageUpAction = actionMap.get(ACTION_MAP_KEY_FOR_PAGE_UP);
		Action originalPageDownAction = actionMap.get(ACTION_MAP_KEY_FOR_PAGE_DOWN);

		Action originalPageUpWithShiftAction = actionMap.get(ACTION_MAP_KEY_FOR_PAGE_UP_WITH_SHIFT);
		Action originalPageDownWithShiftAction = actionMap.get(ACTION_MAP_KEY_FOR_PAGE_DOWN_WITH_SHIFT);

		actionMap.put("page_up", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean isAtTopOfPage = (getCaretTextPosition().getLineNumber() == currentZeroBasedStartingLineNumber);
				if (isAtTopOfPage) {
					moveVerticalScrollBar(-verticalScrollBar.getVisibleAmount());
				}
				if (originalUpAction != null) {
					originalPageUpAction.actionPerformed(e);
				}
			}
		});

		actionMap.put("page_up_w_shift", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean isAtTopOfPage = (getCaretTextPosition().getLineNumber() == currentZeroBasedStartingLineNumber);
				if (isAtTopOfPage) {
					moveVerticalScrollBar(-verticalScrollBar.getVisibleAmount());
				}
				if (originalPageUpWithShiftAction != null) {
					originalPageUpWithShiftAction.actionPerformed(e);
				}
			}
		});

		actionMap.put("page_down", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				int lastLineOnPage = currentZeroBasedStartingLineNumber + lineStartPositionCharacterIndexesInView.size() - 1;
				boolean isAtBottomOfPage = lastLineOnPage == getCaretTextPosition().getLineNumber() + 1;
				if (isAtBottomOfPage) {
					moveVerticalScrollBar(verticalScrollBar.getVisibleAmount());
				}
				if (originalPageDownAction != null) {
					originalPageDownAction.actionPerformed(e);
				}
			}
		});

		actionMap.put("page_down_w_shift", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				int lastLineOnPage = currentZeroBasedStartingLineNumber + lineStartPositionCharacterIndexesInView.size() - 1;
				boolean isAtBottomOfPage = (lastLineOnPage == (getCaretTextPosition().getLineNumber() + 1));
				if (isAtBottomOfPage) {
					moveVerticalScrollBar(verticalScrollBar.getVisibleAmount());
				}
				if (originalPageDownWithShiftAction != null) {
					originalPageDownWithShiftAction.actionPerformed(e);
				}
			}
		});

		actionMap.put("up", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean isAtTopOfPage = (getCaretTextPosition().getLineNumber() == currentZeroBasedStartingLineNumber);
				if (isAtTopOfPage) {
					moveVerticalScrollBar(-1);
				}
				if (originalUpAction != null) {
					originalUpAction.actionPerformed(e);
				}
			}
		});

		actionMap.put("up_w_shift", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean isAtTopOfPage = (getCaretTextPosition().getLineNumber() == currentZeroBasedStartingLineNumber);
				if (isAtTopOfPage) {
					moveVerticalScrollBar(-1);
				}
				if (originalUpWithShiftAction != null) {
					originalUpWithShiftAction.actionPerformed(e);
				}
			}
		});
		actionMap.put("down", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				int lastLineOnPage = currentZeroBasedStartingLineNumber + lineStartPositionCharacterIndexesInView.size() - 1;
				boolean isAtBottomOfPage = (lastLineOnPage == (getCaretTextPosition().getLineNumber() + 1));
				if (isAtBottomOfPage) {
					moveVerticalScrollBar(1);
				}
				if (originalDownAction != null) {
					originalDownAction.actionPerformed(e);
				}
			}
		});
		actionMap.put("down_w_shift", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				int lastLineOnPage = currentZeroBasedStartingLineNumber + lineStartPositionCharacterIndexesInView.size() - 1;
				boolean isAtBottomOfPage = (lastLineOnPage == (getCaretTextPosition().getLineNumber() + 1));
				if (isAtBottomOfPage) {
					moveVerticalScrollBar(1);
				}
				if (originalDownWithShiftAction != null) {
					originalDownWithShiftAction.actionPerformed(e);
				}
			}
		});

		actionMap.put("goto", new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				haveUserGoToLineNumber();
			}
		});

		actionMap.put("find", new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				haveUserFindText();
			}
		});

		actionMap.put("copy", new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				copySelectedTextToClipboard(false);
			}
		});

		actionMap.put("sequence", new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				System.out.println("sequence");
				copySequenceDetailsToClipboard();
			}
		});
	}

	public void haveUserGoToLineNumber() {
		if (document != null) {
			String lineNumberAsString = (String) JOptionPane.showInputDialog(parentTextViewer, "Enter Line Number (Min:1 Max:" + DF.format(document.getNumberOfLines()) + "):", "Go To Line Number",
					JOptionPane.PLAIN_MESSAGE, null, null, lastEnteredGoToLineNumber);
			if (lineNumberAsString != null && lineNumberAsString.length() > 0) {
				lineNumberAsString = lineNumberAsString.replaceAll(",", "");
				int lineNumber = -1;
				try {
					lineNumber = Integer.parseInt(lineNumberAsString);
				} catch (NumberFormatException e) {
				}

				if (lineNumber >= 1 && lineNumber <= document.getNumberOfLines()) {
					lastEnteredGoToLineNumber = lineNumber;
					setLineNumber(lineNumber);
				} else {
					JOptionPane.showMessageDialog(parentTextViewer,
							"The provided line number[" + DF.format(lineNumber) + "] in invalid.  Acceptable line numbers are within the range of 1 and " + DF.format(document.getNumberOfLines())
									+ "(inclusive).", "Invalid Line Number", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	public void haveUserFindText() {
		if (document != null) {
			String searchString = (String) JOptionPane.showInputDialog(parentTextViewer, "Enter Text to Find:", "Search", JOptionPane.PLAIN_MESSAGE, null, null, lastEnteredFindString);
			findingSwingWorker = new SwingWorker<String, String>() {
				@Override
				protected String doInBackground() throws Exception {
					findingLabel.setText("<html><div style='text-align: center;'>Seaching<br>Time Left: Calculating...</div><html>");
					findingProgressBar.setValue(0);
					findingPanel.setVisible(true);
					try {
						boolean isSearchCaseSensitive = false;
						TextPosition textPosition = getCaretTextPosition();
						int currentLineNumber = textPosition.getLineNumber();
						int currentPositionInLine = textPosition.getColumnIndex();
						if (searchString != null && searchString.length() > 0) {
							ITextProgressListener progressListener = new ITextProgressListener() {
								@Override
								public void progressOccurred(ProgressUpdate progressUpdate) {
									findingLabel.setText("<html><div style='text-align: center;'>Seaching<br>Time Left:" + progressUpdate.getEstimatedTimeToCompletionInHHMMSS()
											+ "(HH:MM:SS)</div><html>");
									findingProgressBar.setValue((int) progressUpdate.getPercentComplete());
									generalStatusLabel.setText("Searching for '" + searchString + "'.  " + progressUpdate.getPercentComplete() + "%  Searched.  Time Left:"
											+ progressUpdate.getEstimatedTimeToCompletionInHHMMSSMMM());
								}
							};
							TextPosition foundTextPosition = document.search(currentLineNumber, currentPositionInLine + 1, isSearchCaseSensitive, searchString, progressListener);

							String message;
							if (foundTextPosition != null) {
								setLineNumber(foundTextPosition.getLineNumber() + 1);
								Caret caret = textArea.getCaret();
								// this will be the mark
								caret.setDot(foundTextPosition.getColumnIndex());
								// this will be the dot thus highlighting the searched for text
								caret.moveDot(foundTextPosition.getColumnIndex() + searchString.length());
								message = "Search for:" + searchString + " resulted in the following line:" + (foundTextPosition.getLineNumber() + 1) + " column:" + foundTextPosition.getColumnIndex();
							} else {
								message = "Search for:" + searchString + " resulted in NO RESULTS.";
							}
							JOptionPane.showMessageDialog(parentTextViewer, message, "Search Results", JOptionPane.INFORMATION_MESSAGE);
							generalStatusLabel.setText("");

							lastEnteredFindString = searchString;
						}
					} finally {
						findingPanel.setVisible(false);
						generalStatusLabel.setText("");
					}
					return null;
				}

			};
			findingSwingWorker.execute();

		}
	}

	public void copySelectedTextToClipboard(boolean alwaysSaveToFile) {
		if (document != null) {

			TextPosition markToUse = null;

			Caret caret = textArea.getCaret();
			int mark = caret.getMark();
			int dot = caret.getDot();

			if (markInDocument != null) {
				markToUse = markInDocument;
			} else {
				if (mark != dot) {
					markToUse = getZeroBasedTextPosition(mark);
				}
			}

			if (markToUse != null) {
				TextPosition dotToUse = getZeroBasedTextPosition(dot);
				int numberOfLines = Math.abs(markToUse.getLineNumber() - dotToUse.getLineNumber()) + 1;
				TextPosition start;
				TextPosition end;
				if (markToUse.isBefore(dotToUse)) {
					start = markToUse;
					end = dotToUse;
				} else {
					start = dotToUse;
					end = markToUse;
				}

				// TODO do not do this on the event dispatch thread!!!!!
				if (alwaysSaveToFile || (numberOfLines > MAX_LINES_TO_COPY_TO_CLIPBOARD)) {
					boolean saveToFile = true;
					if (!alwaysSaveToFile) {
						int result = JOptionPane.showConfirmDialog(parentTextViewer, "The selected text is too large for the clipboard.\nWould you like to save it to a file?",
								"Copied Text Too Large for Clipboard", JOptionPane.YES_NO_OPTION);
						saveToFile = (result == JOptionPane.OK_OPTION);
					}
					if (saveToFile) {
						File file = DialogHelper.getFileForSavingClipBoardContents(TextViewerPanel.this);
						if (file != null) {
							try {
								// TODO show progress and allow to be cancelled
								document.copyTextToFile(file, start.getLineNumber(), start.getColumnIndex(), end.getLineNumber(), end.getColumnIndex());
							} catch (IOException e) {
								JOptionPane
										.showMessageDialog(parentTextViewer, "Unable to save text to file.  " + e.getMessage(), "Unable to Write Selected Text to File", JOptionPane.WARNING_MESSAGE);
							}
						}
					}
				} else {
					String[] text = document.getText(start.getLineNumber(), start.getColumnIndex(), end.getLineNumber(), end.getColumnIndex());
					String selectedText = ArraysUtil.toString(text, StringUtil.NEWLINE);
					StringSelection stringSelection = new StringSelection(selectedText);
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

					clipboard.setContents(stringSelection, null);
				}

			} else {
				JOptionPane.showMessageDialog(parentTextViewer, "Unable to copy text to clipboard when no text is selected.", "No Text Selected", JOptionPane.WARNING_MESSAGE);
			}
		}
	}

	public void copySequenceDetailsToClipboard() {
		if (document != null) {
			TextPosition markToUse = null;

			Caret caret = textArea.getCaret();
			int mark = caret.getMark();
			int dot = caret.getDot();

			if (markInDocument != null) {
				markToUse = markInDocument;
			} else {
				if (mark != dot) {
					markToUse = getZeroBasedTextPosition(mark);
				}
			}

			if (markToUse != null) {
				TextPosition dotToUse = getZeroBasedTextPosition(dot);
				int numberOfLines = Math.abs(markToUse.getLineNumber() - dotToUse.getLineNumber()) + 1;
				if (numberOfLines == 1) {
					TextPosition start;
					TextPosition end;
					if (markToUse.isBefore(dotToUse)) {
						start = markToUse;
						end = dotToUse;
					} else {
						start = dotToUse;
						end = markToUse;
					}
					String[] text = document.getText(start.getLineNumber(), start.getColumnIndex(), end.getLineNumber(), end.getColumnIndex());
					ISequence sequence = new IupacNucleotideCodeSequence(text[0]);
					StringBuilder contents = new StringBuilder();
					contents.append("seq:" + sequence + StringUtil.NEWLINE);
					contents.append("rev:" + sequence.getReverse() + StringUtil.NEWLINE);
					contents.append("cmp:" + sequence.getCompliment() + StringUtil.NEWLINE);
					contents.append(" rc:" + sequence.getReverseCompliment() + StringUtil.NEWLINE);
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection stringSelection = new StringSelection(contents.toString());
					clipboard.setContents(stringSelection, null);
				}

			}
		}
	}

	@Override
	public void setTransferHandler(TransferHandler newHandler) {
		super.setTransferHandler(newHandler);
		textArea.setTransferHandler(newHandler);
	}

	synchronized void setLineNumber(int lineNumber) {
		if (lineNumber >= 1 && lineNumber <= document.getNumberOfLines()) {
			int maxValue = verticalScrollBar.getMaximum() - verticalScrollBar.getVisibleAmount();
			if (lineNumber > maxValue) {
				verticalScrollBar.setValue(maxValue);
			} else {
				verticalScrollBar.setValue(lineNumber - 1);
			}

			textArea.setCaretPosition(0);
		}
	}

	private void updateViewPort() {
		if (!horizontalScrollBarIsBeingSet.get()) {
			int characterPosition = horizontalScrollBar.getValue();
			int textWidth = textArea.getFontMetrics(parentTextViewer.getTextFont()).stringWidth("W");
			int pixelXPosition = characterPosition * textWidth;
			viewPortIsBeingSet.set(true);
			viewPort.setViewPosition(new Point(pixelXPosition, 0));
			viewPortIsBeingSet.set(false);
		}
	}

	private void updateHorizontalScrollBar() {
		if (!viewPortIsBeingSet.get()) {
			double pixelXPosition = viewPort.getViewPosition().getX();
			int textWidth = textArea.getFontMetrics(parentTextViewer.getTextFont()).stringWidth("W");
			int characterPosition = (int) (pixelXPosition / textWidth);
			horizontalScrollBarIsBeingSet.set(true);
			horizontalScrollBar.setValue(characterPosition);
			horizontalScrollBarIsBeingSet.set(false);
		}
	}

	private synchronized void moveVerticalScrollBar(int linesToMove) {
		int currentLineNumber = verticalScrollBar.getValue();
		int maxLineNumber = verticalScrollBar.getMaximum();
		int newLineNumber = currentLineNumber + linesToMove;
		newLineNumber = Math.max(0, newLineNumber);
		newLineNumber = Math.min(newLineNumber, maxLineNumber);
		verticalScrollBar.setValue(newLineNumber);
	}

	private class PanelResizeListener implements ComponentListener {
		@Override
		public void componentShown(ComponentEvent e) {
		}

		@Override
		public void componentResized(ComponentEvent e) {
			refreshScreen();
		}

		@Override
		public void componentMoved(ComponentEvent e) {
		}

		@Override
		public void componentHidden(ComponentEvent e) {
		}
	}

	private void updateComponentsLayout() {
		int lineNumberAreaWidth = 0;
		if (showLineNumbers) {
			lineNumberArea.setVisible(true);
			int lineNumbers = 0;
			if (document != null) {
				lineNumbers = document.getNumberOfLines();
			}
			lineNumberAreaWidth = lineNumberArea.getFontMetrics(parentTextViewer.getTextFont()).stringWidth(lineNumbers + "W");
			lineNumberArea.setBounds(0, 0, lineNumberAreaWidth, getHeight() - HORIZONTAL_SCROLLL_BAR_HEIGHT - STATUS_PANEL_HEIGHT);
		} else {
			lineNumberArea.setVisible(false);
		}

		viewPort.setBounds(lineNumberAreaWidth, 2, getWidth() - VERTICAL_SCROLLL_BAR_WIDTH - lineNumberAreaWidth, getHeight() - HORIZONTAL_SCROLLL_BAR_HEIGHT - STATUS_PANEL_HEIGHT);

		int xStart = (int) (getWidth() - VERTICAL_SCROLLL_BAR_WIDTH);
		verticalScrollBar.setBounds(xStart, 0, VERTICAL_SCROLLL_BAR_WIDTH, getHeight() - HORIZONTAL_SCROLLL_BAR_HEIGHT - STATUS_PANEL_HEIGHT);

		int yStart = (int) (getHeight() - HORIZONTAL_SCROLLL_BAR_HEIGHT - STATUS_PANEL_HEIGHT);
		horizontalScrollBar.setBounds(0, yStart, getWidth() - VERTICAL_SCROLLL_BAR_WIDTH, HORIZONTAL_SCROLLL_BAR_HEIGHT);

		yStart = (int) (getHeight() - STATUS_PANEL_HEIGHT);
		statusPanel.setBounds(0, yStart, getWidth(), STATUS_PANEL_HEIGHT);

		// TODO set indexingJLabel bounds
		xStart = (int) (getWidth() - INDEXING_PANEL_WIDTH) / 2;
		yStart = (int) (getHeight() - INDEXING_PANEL_HEIGHT) / 2;
		indexingPanel.setBounds(xStart, yStart, INDEXING_PANEL_WIDTH, INDEXING_PANEL_HEIGHT);
		findingPanel.setBounds(xStart, yStart, INDEXING_PANEL_WIDTH, INDEXING_PANEL_HEIGHT);
	}

	void refreshScreen() {
		if (this.initIsDone) {
			updateComponentsLayout();
			updateScrollBar();
			updateTextInViewer();
		}

	}

	private int getNumberOfLinesThatCanFitInViewer() {
		int height = viewPort.getHeight();
		int textHeight = textArea.getFontMetrics(parentTextViewer.getTextFont()).getHeight();
		int numberOfLinesThatCanFitInViewer = height / textHeight;
		return Math.max(0, numberOfLinesThatCanFitInViewer - 1);

	}

	private int getNumberOfCharactersThatCanFitInViewer() {
		double width = viewPort.getWidth();
		// We are trying to limit the display to monospaced fonts so which letter
		// is used here for measuring width shouldn't matter.
		double textWidth = textArea.getFontMetrics(parentTextViewer.getTextFont()).stringWidth("W");
		int numberOfCharactersThatCanFitInViewer = (int) Math.ceil(width / textWidth);
		return numberOfCharactersThatCanFitInViewer;
	}

	public synchronized void updateTextInViewer() {
		if (initIsDone) {
			int longestLine = 0;
			textArea.setFont(parentTextViewer.getTextFont());
			lineNumberArea.setFont(parentTextViewer.getTextFont());
			// note this is now done in the textViewers override paintComponent found in the constructor of this
			// class
			// textArea.setBackground(parentTextViewer.getBackgroundTextPanelColor());
			textArea.setForeground(parentTextViewer.getTextColor());

			TextPosition caretTextPositionBeforeNewText = getCaretTextPosition();
			Integer newCaretPosition = null;

			Caret caret = textArea.getCaret();
			int caretDot = caret.getDot();
			int caretMark = caret.getMark();
			TextPosition newMarkInDocument = getZeroBasedTextPosition(caretMark);
			if (caretDot == caretMark) {
				markInDocument = null;
				selectedTextIsOffScreen.set(false);
			} else if (newMarkInDocument != null && !newMarkInDocument.equals(markInDocument) && !selectedTextIsOffScreen.get()) {
				markInDocument = newMarkInDocument;
			}

			int startingLineNumber = verticalScrollBar.getValue();
			currentZeroBasedStartingLineNumber = startingLineNumber;

			lineStartPositionCharacterIndexesInView.clear();
			lineStartPositionCharacterIndexesInView.add(0);

			int numberOfLinesThatCanFitInView = getNumberOfLinesThatCanFitInViewer();

			StringBuilder lineNumberText = new StringBuilder();

			int endingLineNumber = startingLineNumber + numberOfLinesThatCanFitInView;
			boolean headerExists = doesHeaderExist() && (startingLineNumber >= headerLineNumber);
			StringBuilder text = new StringBuilder();
			if (document != null && numberOfLinesThatCanFitInView > 0) {

				int lineNumbersInFile = document.getNumberOfLines();
				endingLineNumber = Math.min(lineNumbersInFile, endingLineNumber);
				String[] allText = document.getText(startingLineNumber, endingLineNumber);
				if (headerExists) {
					allText = ArraysUtil.concatenate(new String[] { headerText }, allText);
				}
				for (int currentLine = startingLineNumber; currentLine <= endingLineNumber; currentLine++) {
					int index = currentLine - startingLineNumber;
					int currentLineNumber = currentLine + 1;
					if (headerExists) {
						if (currentLine == startingLineNumber) {
							currentLineNumber = headerLineNumber;
						} else {
							currentLineNumber--;
						}
					}
					String textForLine = allText[index];
					if (textForLine != null) {
						if (currentLine < lineNumbersInFile) {
							if (currentLine >= startingLineNumber) {
								longestLine = Math.max(longestLine, textForLine.length());

								boolean lineInWhichCaretWasLocated = (caretTextPositionBeforeNewText != null) && (currentLine == caretTextPositionBeforeNewText.getLineNumber());
								if (lineInWhichCaretWasLocated) {
									newCaretPosition = text.length() + Math.min(caretTextPositionBeforeNewText.getColumnIndex(), textForLine.length());
								}

								text.append(textForLine + StringUtil.NEWLINE);
								lineStartPositionCharacterIndexesInView.add(text.length());

								lineNumberText.append((currentLineNumber) + StringUtil.NEWLINE);
							}
						} else {
							text.append(StringUtil.NEWLINE);
							lineStartPositionCharacterIndexesInView.add(text.length());
						}
					}
				}

				String textToAdd = "";
				// remove the last newline
				if (text.length() > 0) {
					textToAdd = text.substring(0, text.length() - 1).toString();
				}

				if (isShowDataView) {
					maxLengthPerColum = new HashMap<Integer, Integer>();
					for (String line : textToAdd.split("" + StringUtil.NEWLINE_SYMBOL)) {
						int columnIndex = 0;
						for (String columnText : line.split(StringUtil.TAB)) {
							Integer previousMax = maxLengthPerColum.get(columnIndex);
							if (previousMax == null) {
								previousMax = 0;
							}
							int max = Math.max(previousMax, columnText.length());
							maxLengthPerColum.put(columnIndex, max);
							columnIndex++;
						}
					}

					int dataLongestLine = DATA_COLUMN_SPACING_IN_CHARACTERS * maxLengthPerColum.size();
					for (Integer columnWidth : maxLengthPerColum.values()) {
						dataLongestLine += columnWidth;
					}
					if (horizontalScrollBar.getMaximum() < dataLongestLine) {
						int oldValue = horizontalScrollBar.getValue();
						horizontalScrollBar.setMaximum(dataLongestLine);
						horizontalScrollBar.setValue(oldValue);
						horizontalScrollBar.repaint();
					}

					int dataPosition = 0;

					// reset these to take into account the new spaces
					lineStartPositionCharacterIndexesInView.clear();
					lineStartPositionCharacterIndexesInView.add(0);
					StringBuilder newTextBuilder = new StringBuilder();
					int lineNumber = startingLineNumber;
					for (String line : textToAdd.split("" + StringUtil.NEWLINE_SYMBOL)) {
						line = line.replaceAll("" + StringUtil.CARRIAGE_RETURN, "");
						int columnIndex = 0;
						StringBuilder newLineBuilder = new StringBuilder();
						for (String columnText : line.split(StringUtil.TAB)) {
							int max = maxLengthPerColum.get(columnIndex);
							int spacesToAdd = ((max + DATA_COLUMN_SPACING_IN_CHARACTERS) - columnText.length());
							newLineBuilder.append(columnText + StringUtil.repeatString(" ", spacesToAdd));
							columnIndex++;

							for (int i = 0; i <= columnText.length(); i++) {
								dataPositionMapsToTextPosition.set(dataPosition, true);
								dataPosition++;
							}
							for (int i = 0; i < spacesToAdd - 1; i++) {
								dataPositionMapsToTextPosition.set(dataPosition, false);
								dataPosition++;
							}
						}

						boolean lineInWhichCaretWasLocated = (caretTextPositionBeforeNewText != null) && (lineNumber == caretTextPositionBeforeNewText.getLineNumber());
						if (lineInWhichCaretWasLocated) {
							newCaretPosition = newTextBuilder.length() + Math.min(caretTextPositionBeforeNewText.getColumnIndex(), newLineBuilder.length());
						}

						newLineBuilder.append(StringUtil.NEWLINE_SYMBOL);
						newTextBuilder.append(newLineBuilder.toString());

						dataPositionMapsToTextPosition.set(dataPosition, true);
						dataPosition++;

						lineStartPositionCharacterIndexesInView.add(newTextBuilder.length());
						lineNumber++;

					}
					textToAdd = newTextBuilder.toString();
				}

				textArea.setText(textToAdd);

				Integer markPosition = null;
				if (markInDocument != null) {
					markPosition = getTextAreaPosition(markInDocument);
					if (markPosition == null) {
						selectedTextIsOffScreen.set(true);
						if (markInDocument.getLineNumber() < currentZeroBasedStartingLineNumber) {
							markPosition = 0;
						} else {
							markPosition = textArea.getText().length() - 1;
						}
					} else {
						selectedTextIsOffScreen.set(false);
					}
				} else {
					selectedTextIsOffScreen.set(false);
				}

				if (newCaretPosition != null) {
					if (markPosition != null) {
						caret.setDot(markPosition);
						caret.moveDot(newCaretPosition);
					} else {
						textArea.setCaretPosition(newCaretPosition);
					}
				} else if (caretTextPositionBeforeNewText != null && (caretTextPositionBeforeNewText.getLineNumber() >= currentZeroBasedStartingLineNumber)) {
					int lastPosition = textToAdd.length() - 1;
					int startOfLastLine;
					if (isShowDataView) {
						startOfLastLine = lineStartPositionCharacterIndexesInView.get(lineStartPositionCharacterIndexesInView.size() - 2);
					} else {
						startOfLastLine = lineStartPositionCharacterIndexesInView.get(lineStartPositionCharacterIndexesInView.size() - 2);
					}

					// move the caret position but attempt to keep the same column
					int newPosition = Math.min(lastPosition, startOfLastLine + caretTextPositionBeforeNewText.getColumnIndex());
					if (markPosition != null) {
						caret.setDot(markPosition);
						caret.moveDot(newPosition);
					} else {
						textArea.setCaretPosition(newPosition);
					}
				} else {
					if (caretTextPositionBeforeNewText != null) {
						int positionForColumn = caretTextPositionBeforeNewText.getColumnIndex();
						int endOfFirstLine;

						if (lineStartPositionCharacterIndexesInView.size() > 1) {
							endOfFirstLine = lineStartPositionCharacterIndexesInView.get(1) - 1;
						} else {
							endOfFirstLine = textArea.getText().length() - 1;
						}

						int newPosition = Math.min(endOfFirstLine, positionForColumn);
						textArea.setCaretPosition(newPosition);
						if (markPosition != null) {
							caret.setDot(markPosition);
							caret.moveDot(newPosition);
						} else {
							textArea.setCaretPosition(newPosition);
						}
					}
				}

			} else {
				textArea.setText("");
			}

			lineNumberArea.setText(lineNumberText.toString());

			updateViewPort();

			revalidate();
			repaint();
		}

	}

	public boolean isShowDataView() {
		return isShowDataView;
	}

	public void setShowDataView(boolean isShowDataView) {
		if (this.isShowDataView != isShowDataView) {
			this.isShowDataView = isShowDataView;
			if (headerLineNumber > 0) {
				setShowHeader(true);
			}
			updateTextInViewer();
		}
	}

	private void updateScrollBar() {
		if (document != null) {
			int numberOfLinesThatCanFitInView = getNumberOfLinesThatCanFitInViewer();
			verticalScrollBar.setMinimum(0);
			int lineNumbersInFile = document.getNumberOfLines();
			verticalScrollBar.setMaximum(lineNumbersInFile - 1);
			verticalScrollBar.setUnitIncrement(1);
			verticalScrollBar.setVisibleAmount(numberOfLinesThatCanFitInView);
			verticalScrollBar.repaint();

			int charactersPerTab = textArea.getTabSize();
			int longestLine = document.getNumberOfCharactersInLongestLine(charactersPerTab);
			int numberOfCharactersThatCanFitInView = getNumberOfCharactersThatCanFitInViewer();
			horizontalScrollBar.setMinimum(0);
			horizontalScrollBar.setMaximum(longestLine);
			horizontalScrollBar.setUnitIncrement(1);
			horizontalScrollBar.setVisibleAmount(numberOfCharactersThatCanFitInView);
			horizontalScrollBar.repaint();
		}

	}

	public void setCurrentPositionLabel(String text) {
		currentPositionLabel.setText(text);
	}

	public void close() throws IOException {
		if (document != null) {
			document.close();
		}
		parentTextViewer.updateCloseAllMenuItem();
	}

	public int getCurrentLineNumber() {
		return currentZeroBasedStartingLineNumber;
	}

	public void setGeneralStatusText(String text) {
		generalStatusLabel.setText(text);
	}

	public TextViewer getTextViewer() {
		return parentTextViewer;
	}

}
