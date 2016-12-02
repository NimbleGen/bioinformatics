package com.roche.sequencing.bioinformatics.common.text.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.roche.sequencing.bioinformatics.common.text.Document;
import com.roche.sequencing.bioinformatics.common.text.GZipIndex;
import com.roche.sequencing.bioinformatics.common.text.IDocument;
import com.roche.sequencing.bioinformatics.common.text.ITextProgressListener;
import com.roche.sequencing.bioinformatics.common.text.ProgressUpdate;
import com.roche.sequencing.bioinformatics.common.text.TextFileIndex;
import com.roche.sequencing.bioinformatics.common.text.TextPosition;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.gzip.BamByteDecoder;
import com.roche.sequencing.bioinformatics.common.utils.gzip.GZipBlock;
import com.roche.sequencing.bioinformatics.common.utils.gzip.GZipUtil;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IByteDecoder;
import com.roche.sequencing.bioinformatics.common.utils.gzip.IBytes;

public class TextViewerPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final static DecimalFormat DF = new DecimalFormat("###,###");

	private final static int VERTICAL_SCROLLL_BAR_WIDTH = 20;
	private final static int HORIZONTAL_SCROLLL_BAR_HEIGHT = 20;
	private final static int STATUS_PANEL_HEIGHT = 20;

	public final static Cursor TEXT_CURSOR = new Cursor(Cursor.TEXT_CURSOR);

	private final static String ACTION_MAP_KEY_FOR_UP = "caret-up";
	private final static String ACTION_MAP_KEY_FOR_DOWN = "caret-down";

	private final static String ACTION_MAP_KEY_FOR_PAGE_UP = "page-up";
	private final static String ACTION_MAP_KEY_FOR_PAGE_DOWN = "page-down";

	// HOT PINK
	private final static Color DEFAULT_CARET_COLOR = new Color(255, 20, 147);
	private final static int DEFAULT_CARET_WIDTH = 3;

	private final static boolean DEFAULT_SHOW_LINE_NUMBERS = true;

	private final JTextPane lineNumberArea;
	private final JTextArea textArea;
	private JTable tableArea;
	private final JViewport viewPort;
	private final JPanel statusPanel;

	private final JLabel currentPositionLabel;
	private final JLabel fileInfoLabel;

	private JScrollBar verticalScrollBar;
	private JScrollBar horizontalScrollBar;

	private final File file;
	private final IDocument document;

	private final AtomicBoolean viewPortIsBeingSet;
	private final AtomicBoolean horizontalScrollBarIsBeingSet;

	private final List<Integer> lineStartPositionCharacterIndexesInView;

	private TextViewer parentTextViewer;

	private Integer currentStartingLineNumber;

	private boolean showLineNumbers;

	// TODO incorporate this into the panel
	private boolean showTableView = false;

	public TextViewerPanel(TextViewer parentTextViewer, File file, RandomAccessFile randomAccessToFile, TextFileIndex textFileIndex, GZipIndex gZipIndex, IBytes gZipDictionaryBytes,
			File bamBlockIndexFile) throws FileNotFoundException {
		super();

		this.parentTextViewer = parentTextViewer;
		this.file = file;

		boolean isBamFile = FileUtil.getFileExtension(file).toLowerCase().endsWith(TextViewer.BAM_FILE_EXTENSION);

		IByteDecoder byteConverter = null;
		if (isBamFile) {
			try {
				GZipBlock firstBlock = GZipUtil.getFirstBlock(gZipIndex, file);
				if (firstBlock == null) {
					throw new IllegalStateException("Unable to extract the reference information in the provided bam file[" + file.getAbsolutePath() + "].");
				}
				byteConverter = new BamByteDecoder(firstBlock.getUncompressedData(), bamBlockIndexFile);
			} catch (IOException e1) {
				throw new IllegalStateException(e1.getMessage(), e1);
			}
		}

		if (gZipIndex != null) {
			this.document = new Document(textFileIndex, gZipIndex, gZipDictionaryBytes, file, byteConverter);
		} else {
			this.document = new Document(textFileIndex, file, byteConverter);
		}

		lineStartPositionCharacterIndexesInView = new ArrayList<Integer>();
		viewPortIsBeingSet = new AtomicBoolean(false);
		horizontalScrollBarIsBeingSet = new AtomicBoolean(false);

		showLineNumbers = DEFAULT_SHOW_LINE_NUMBERS;

		lineNumberArea = new JTextPane();
		lineNumberArea.setEditable(false);
		// center the text
		StyledDocument doc = lineNumberArea.getStyledDocument();
		SimpleAttributeSet center = new SimpleAttributeSet();
		StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
		doc.setParagraphAttributes(0, doc.getLength(), center, false);
		lineNumberArea.setForeground(Color.DARK_GRAY);

		lineNumberArea.setBackground(new Color(225, 225, 225));
		add(lineNumberArea);

		textArea = new JTextArea();
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

		textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		textArea.setCursor(TEXT_CURSOR);
		textArea.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				TextPosition textPosition = getCaretTextPosition();
				if (textPosition != null) {
					currentPositionLabel.setText("  Line:" + DF.format(textPosition.getLineNumber()) + " Column:" + DF.format(textPosition.getColumnIndex() + 1));
				}
			}
		});

		// this is to fix a weird bug where the scrollpane tries to update the caret position
		// and causes the scroll pane's horizontal scroll bar to jump around (thus causing the text area
		// to jump around). Also note that this is why the scroll pane's horizontal scroll bar is not
		// being used in the ui.
		DefaultCaret caret = (DefaultCaret) textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

		tableArea = new JTable() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component component = super.prepareRenderer(renderer, row, column);
				int rendererWidth = component.getPreferredSize().width;
				TableColumn tableColumn = getColumnModel().getColumn(column);
				tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
				return component;
			}
		};
		tableArea.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableArea.setTableHeader(null);
		tableArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		// TODO make this dynamic so it does not rely on a specific font and size
		tableArea.setRowHeight(17);
		tableArea.setRowSelectionAllowed(false);
		tableArea.setCellSelectionEnabled(true);

		viewPort = new JViewport();
		if (showTableView) {
			viewPort.setView(tableArea);
		} else {
			viewPort.setView(textArea);
		}
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
		statusPanel.setLayout(new BorderLayout());
		statusPanel.setBackground(Color.LIGHT_GRAY);
		currentPositionLabel = new JLabel();
		fileInfoLabel = new JLabel();
		statusPanel.add(currentPositionLabel, BorderLayout.LINE_START);
		statusPanel.add(fileInfoLabel, BorderLayout.LINE_END);
		statusPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.black));
		add(statusPanel);

		addComponentListener(new PanelResizeListener());

		int numberOfLinesThatCanFitInView = getNumberOfLinesThatCanFitInViewer();
		textArea.setRows(numberOfLinesThatCanFitInView);
		int charactersPerTab = textArea.getTabSize();
		int longestLine = textFileIndex.getNumberOfCharactersInLongestLine(charactersPerTab);
		textArea.setColumns(longestLine);

		fileInfoLabel.setText("File Size:" + FileUtil.getFileSizeLabel(this.file.length()) + "   File Length:" + DF.format(textFileIndex.getNumberOfLines()) + " Lines  ");
		updateScrollBar();

		bindKeys();

	}

	public void setCaretColor(Color color) {
		textArea.setCaretColor(color);
	}

	public void setCaretWidth(int width) {
		textArea.putClientProperty("caretWidth", width);
	}

	private TextPosition getCaretTextPosition() {
		TextPosition textPosition = null;
		int lineNumber = -1;
		int column = -1;

		int caretPosition = textArea.getCaretPosition();

		int lastLineCharIndex = 0;

		lineLoop: for (int i = 0; i < lineStartPositionCharacterIndexesInView.size(); i++) {
			int curLineCharIndex = lineStartPositionCharacterIndexesInView.get(i);
			if (caretPosition >= lastLineCharIndex && caretPosition < curLineCharIndex) {
				lineNumber = currentStartingLineNumber + i;
				column = caretPosition - lastLineCharIndex;
				textPosition = new TextPosition(lineNumber, column);
				break lineLoop;
			}
			lastLineCharIndex = curLineCharIndex;
		}
		return textPosition;
	}

	public File getFile() {
		return file;
	}

	private void bindKeys() {
		InputMap inputMap = textArea.getInputMap(JPanel.WHEN_FOCUSED);
		ActionMap actionMap = textArea.getActionMap();
		bindKeys(inputMap, actionMap);

		inputMap = tableArea.getInputMap(JPanel.WHEN_FOCUSED);
		actionMap = tableArea.getActionMap();
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
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK), "goto");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "find");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), "copy");

		Action originalUpAction = actionMap.get(ACTION_MAP_KEY_FOR_UP);
		Action originalDownAction = actionMap.get(ACTION_MAP_KEY_FOR_DOWN);

		Action originalPageUpAction = actionMap.get(ACTION_MAP_KEY_FOR_PAGE_UP);
		Action originalPageDownAction = actionMap.get(ACTION_MAP_KEY_FOR_PAGE_DOWN);

		actionMap.put("page_up", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean isAtTopOfPage = getCaretTextPosition().getLineNumber() == (currentStartingLineNumber + 1);
				if (isAtTopOfPage) {
					moveVerticalScrollBar(-verticalScrollBar.getVisibleAmount());
				}
				if (originalUpAction != null) {
					originalPageUpAction.actionPerformed(e);
				}
			}
		});

		actionMap.put("page_down", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean isAtBottomOfPage = (currentStartingLineNumber + lineStartPositionCharacterIndexesInView.size() - 1) == getCaretTextPosition().getLineNumber();
				if (isAtBottomOfPage) {
					moveVerticalScrollBar(verticalScrollBar.getVisibleAmount());
				}
				if (originalPageDownAction != null) {
					originalPageDownAction.actionPerformed(e);
				}
			}
		});

		actionMap.put("up", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean isAtTopOfPage = getCaretTextPosition().getLineNumber() == (currentStartingLineNumber + 1);
				if (isAtTopOfPage) {
					moveVerticalScrollBar(-1);
					if (originalUpAction != null) {
						originalUpAction.actionPerformed(e);
					}
				}
				if (originalUpAction != null) {
					originalUpAction.actionPerformed(e);
				}
			}
		});
		actionMap.put("down", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean isAtBottomOfPage = (currentStartingLineNumber + lineStartPositionCharacterIndexesInView.size() - 1) == getCaretTextPosition().getLineNumber();
				if (isAtBottomOfPage) {
					moveVerticalScrollBar(1);
				}
				if (originalDownAction != null) {
					originalDownAction.actionPerformed(e);
				}
			}
		});

		actionMap.put("goto", new AbstractAction() {

			private static final long serialVersionUID = 1L;
			private int lastEnteredLineNumber = 1;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (document != null) {
					String lineNumberAsString = (String) JOptionPane.showInputDialog(parentTextViewer, "Enter Line Number (Min:1 Max:" + DF.format(document.getNumberOfLines()) + "):",
							"Go To Line Number", JOptionPane.PLAIN_MESSAGE, null, null, lastEnteredLineNumber);
					if (lineNumberAsString != null && lineNumberAsString.length() > 0) {
						lineNumberAsString.replaceAll(",", "");
						int lineNumber = -1;
						try {
							lineNumber = Integer.parseInt(lineNumberAsString);
						} catch (NumberFormatException e) {
						}

						if (lineNumber >= 1 && lineNumber <= document.getNumberOfLines()) {
							lastEnteredLineNumber = lineNumber;
							setLineNumber(lineNumber);
						} else {
							JOptionPane.showMessageDialog(parentTextViewer, "The provided line number[" + DF.format(lineNumber)
									+ "] in invalid.  Acceptable line numbers are within the range of 1 and " + DF.format(document.getNumberOfLines()) + "(inclusive).", "Invalid Line Number",
									JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			}
		});

		actionMap.put("find", new AbstractAction() {

			private static final long serialVersionUID = 1L;
			private String lastEnteredString;
			private Boolean lastSearchWasCaseSensitive;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (document != null) {
					String searchString = (String) JOptionPane.showInputDialog(parentTextViewer, "Enter Text to Find:", "Search", JOptionPane.PLAIN_MESSAGE, null, null, lastEnteredString);
					boolean isSearchCaseSensitive = false;
					TextPosition textPosition = getCaretTextPosition();
					int currentLineNumber = textPosition.getLineNumber();
					int currentPositionInLine = textPosition.getColumnIndex();

					if (searchString != null && searchString.length() > 0) {
						ITextProgressListener progressListener = new ITextProgressListener() {
							@Override
							public void progressOccurred(ProgressUpdate progressUpdate) {
								System.out.println(progressUpdate.getPercentComplete() + "%  Time Left:" + progressUpdate.getEstimatedTimeToCompletionInHHMMSSMMM());
							}
						};
						TextPosition foundTextPosition = document.search(currentLineNumber, currentPositionInLine + 1, isSearchCaseSensitive, searchString, progressListener);

						String message;
						if (foundTextPosition != null) {
							setLineNumber(foundTextPosition.getLineNumber() + 1);
							textArea.setCaretPosition(foundTextPosition.getColumnIndex());
							message = "Search for:" + searchString + " resulted in the following line:" + (foundTextPosition.getLineNumber() + 1) + " column:" + foundTextPosition.getColumnIndex();
						} else {
							message = "Search for:" + searchString + " resulted in NO RESULTS.";
						}

						JOptionPane.showMessageDialog(parentTextViewer, message, "Search Results", JOptionPane.INFORMATION_MESSAGE);

						lastEnteredString = searchString;
						lastSearchWasCaseSensitive = isSearchCaseSensitive;
					}
				}
			}
		});

		actionMap.put("copy", new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (document != null) {
					if (showTableView) {
						copyTableSelectionToClipboard();
					} else {
						String selectedText = textArea.getSelectedText();
						if (selectedText != null && !selectedText.isEmpty()) {
							StringSelection stringSelection = new StringSelection(selectedText);
							Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
							clipboard.setContents(stringSelection, null);
						}
					}
				}
			}
		});
	}

	private void copyTableSelectionToClipboard() {
		int numCols = tableArea.getSelectedColumnCount();
		int numRows = tableArea.getSelectedRowCount();
		int[] rowsSelected = tableArea.getSelectedRows();
		int[] colsSelected = tableArea.getSelectedColumns();
		if (numRows != rowsSelected[rowsSelected.length - 1] - rowsSelected[0] + 1 || numRows != rowsSelected.length || numCols != colsSelected[colsSelected.length - 1] - colsSelected[0] + 1
				|| numCols != colsSelected.length) {

			JOptionPane.showMessageDialog(null, "Invalid Copy Selection", "Invalid Copy Selection", JOptionPane.ERROR_MESSAGE);
			return;
		}

		StringBuffer excelStr = new StringBuffer();
		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++) {
				excelStr.append(tableArea.getValueAt(rowsSelected[i], colsSelected[j]));
				if (j < numCols - 1) {
					excelStr.append(StringUtil.TAB);
				}
			}
			excelStr.append(StringUtil.NEWLINE);
		}

		StringSelection stringSelection = new StringSelection(excelStr.toString());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}

	@Override
	public void setTransferHandler(TransferHandler newHandler) {
		super.setTransferHandler(newHandler);
		textArea.setTransferHandler(newHandler);
		tableArea.setTransferHandler(newHandler);
	}

	void setLineNumber(int lineNumber) {
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
			int textWidth = textArea.getFontMetrics(textArea.getFont()).stringWidth("W");
			int pixelXPosition = characterPosition * textWidth;
			viewPortIsBeingSet.set(true);
			viewPort.setViewPosition(new Point(pixelXPosition, 0));
			viewPortIsBeingSet.set(false);
		}
	}

	private void updateHorizontalScrollBar() {
		if (!viewPortIsBeingSet.get()) {
			double pixelXPosition = viewPort.getViewPosition().getX();
			int textWidth = textArea.getFontMetrics(textArea.getFont()).stringWidth("W");
			int characterPosition = (int) (pixelXPosition / textWidth);
			horizontalScrollBarIsBeingSet.set(true);
			horizontalScrollBar.setValue(characterPosition);
			horizontalScrollBarIsBeingSet.set(false);
		}
	}

	private void moveVerticalScrollBar(int linesToMove) {
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
			int lineNumbers = document.getNumberOfLines();
			lineNumberAreaWidth = lineNumberArea.getFontMetrics(lineNumberArea.getFont()).stringWidth(lineNumbers + "W");
			lineNumberArea.setBounds(0, 0, lineNumberAreaWidth, getHeight() - HORIZONTAL_SCROLLL_BAR_HEIGHT - STATUS_PANEL_HEIGHT);
		} else {
			lineNumberArea.setVisible(false);
		}

		if (showTableView) {
			viewPort.setBounds(lineNumberAreaWidth, 5, getWidth() - VERTICAL_SCROLLL_BAR_WIDTH - lineNumberAreaWidth, getHeight() - HORIZONTAL_SCROLLL_BAR_HEIGHT - STATUS_PANEL_HEIGHT);
		} else {
			viewPort.setBounds(lineNumberAreaWidth, 2, getWidth() - VERTICAL_SCROLLL_BAR_WIDTH - lineNumberAreaWidth, getHeight() - HORIZONTAL_SCROLLL_BAR_HEIGHT - STATUS_PANEL_HEIGHT);
		}

		int xStart = (int) (getWidth() - VERTICAL_SCROLLL_BAR_WIDTH);
		verticalScrollBar.setBounds(xStart, 0, VERTICAL_SCROLLL_BAR_WIDTH, getHeight() - HORIZONTAL_SCROLLL_BAR_HEIGHT - STATUS_PANEL_HEIGHT);

		int yStart = (int) (getHeight() - HORIZONTAL_SCROLLL_BAR_HEIGHT - STATUS_PANEL_HEIGHT);
		horizontalScrollBar.setBounds(0, yStart, getWidth() - VERTICAL_SCROLLL_BAR_WIDTH, HORIZONTAL_SCROLLL_BAR_HEIGHT);

		yStart = (int) (getHeight() - STATUS_PANEL_HEIGHT);
		statusPanel.setBounds(0, yStart, getWidth(), STATUS_PANEL_HEIGHT);
	}

	private void refreshScreen() {
		updateComponentsLayout();

		updateScrollBar();
		updateTextInViewer();
	}

	private int getNumberOfLinesThatCanFitInViewer() {
		int height = viewPort.getHeight();
		int textHeight = textArea.getFontMetrics(textArea.getFont()).getHeight();
		int numberOfLinesThatCanFitInViewer = height / textHeight;
		return Math.max(0, numberOfLinesThatCanFitInViewer - 1);

	}

	private int getNumberOfCharactersThatCanFitInViewer() {
		double width = viewPort.getWidth();
		// We are trying to limit the display to monospaced fonts so which letter
		// is used here for measuring width shouldn't matter.
		double textWidth = textArea.getFontMetrics(textArea.getFont()).stringWidth("W");
		int numberOfCharactersThatCanFitInViewer = (int) Math.ceil(width / textWidth);
		return numberOfCharactersThatCanFitInViewer;
	}

	private synchronized void updateTextInViewer() {
		int longestLine = 0;

		TextPosition caretTextPositionBeforeNewText = getCaretTextPosition();
		Integer newCaretPosition = null;

		lineStartPositionCharacterIndexesInView.clear();
		lineStartPositionCharacterIndexesInView.add(0);

		int startingLineNumber = verticalScrollBar.getValue();
		currentStartingLineNumber = startingLineNumber;
		int numberOfLinesThatCanFitInView = getNumberOfLinesThatCanFitInViewer();

		StringBuilder lineNumberText = new StringBuilder();

		int endingLineNumber = startingLineNumber + numberOfLinesThatCanFitInView;
		StringBuilder text = new StringBuilder();
		if (document != null && numberOfLinesThatCanFitInView > 0) {

			int lineNumbersInFile = document.getNumberOfLines();

			String[] allText = document.getText(startingLineNumber, endingLineNumber);
			for (int currentLine = startingLineNumber; currentLine <= endingLineNumber; currentLine++) {
				int index = currentLine - startingLineNumber;
				String textForLine = allText[index];
				if (currentLine < lineNumbersInFile) {
					if (currentLine >= startingLineNumber) {
						longestLine = Math.max(longestLine, textForLine.length());

						if ((caretTextPositionBeforeNewText != null) && (currentLine == caretTextPositionBeforeNewText.getLineNumber() - 1)) {
							newCaretPosition = text.length() + Math.min(caretTextPositionBeforeNewText.getColumnIndex(), textForLine.length());
						}

						text.append(textForLine + StringUtil.NEWLINE);
						lineStartPositionCharacterIndexesInView.add(text.length());

						lineNumberText.append((currentLine + 1) + StringUtil.NEWLINE);
					}
				} else {
					text.append(StringUtil.NEWLINE);
					lineStartPositionCharacterIndexesInView.add(text.length());
				}
			}

			String textToAdd = "";
			// remove the last newline
			if (text.length() > 0) {
				textToAdd = text.substring(0, text.length() - 1).toString();
			}

			if (showTableView) {
				String[] lines = textToAdd.split(StringUtil.NEWLINE);
				int columns = document.getMostTabsFoundInALine() + 2;
				String[][] data = new String[lines.length][columns];
				String[] columnNames = new String[columns];
				for (int i = 0; i < lines.length; i++) {
					String line = lines[i];
					int j = 0;
					for (String value : line.split(StringUtil.TAB)) {
						data[i][j] = value + " ";
						j++;
					}
					data[i][j] = StringUtil.repeatString(" ", document.getNumberOfCharactersInLongestLine(0) - line.length());
				}
				tableArea.setModel(new DefaultTableModel(data, columnNames) {
					private static final long serialVersionUID = 1L;

					@Override
					public boolean isCellEditable(int row, int column) {
						return false;
					}
				});
			} else {
				textArea.setText(textToAdd);
				if (newCaretPosition != null) {
					textArea.setCaretPosition(newCaretPosition);
				} else if (caretTextPositionBeforeNewText != null && (caretTextPositionBeforeNewText.getLineNumber() > currentStartingLineNumber)) {
					if (caretTextPositionBeforeNewText != null) {
						int lastPosition = text.length() - 1;
						int startOfLastLine = lineStartPositionCharacterIndexesInView.get(lineStartPositionCharacterIndexesInView.size() - 1);

						// move the caret position but attempt to keep the same column
						int newPosition = Math.min(lastPosition, startOfLastLine + caretTextPositionBeforeNewText.getColumnIndex());
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
					}
				}
			}

		} else {
			if (showTableView) {
				// tableArea.setModel(new DefaultTableModel(new String[0][0], null));
			} else {
				textArea.setText("");
			}
		}

		lineNumberArea.setText(lineNumberText.toString());

		updateViewPort();

		revalidate();
		repaint();

	}

	private void updateScrollBar() {
		if (document != null) {
			int numberOfLinesThatCanFitInView = getNumberOfLinesThatCanFitInViewer();
			verticalScrollBar.setMinimum(0);
			int lineNumbersInFile = document.getNumberOfLines();
			verticalScrollBar.setMaximum(lineNumbersInFile - 1);
			verticalScrollBar.setValue(0);
			verticalScrollBar.setUnitIncrement(1);
			verticalScrollBar.setVisibleAmount(numberOfLinesThatCanFitInView);
			verticalScrollBar.repaint();

			int charactersPerTab = textArea.getTabSize();
			int longestLine = document.getNumberOfCharactersInLongestLine(charactersPerTab);
			int numberOfCharactersThatCanFitInView = getNumberOfCharactersThatCanFitInViewer();
			horizontalScrollBar.setMinimum(0);
			horizontalScrollBar.setMaximum(longestLine);
			horizontalScrollBar.setValue(0);
			horizontalScrollBar.setUnitIncrement(1);
			horizontalScrollBar.setVisibleAmount(numberOfCharactersThatCanFitInView);
			horizontalScrollBar.repaint();
		}

	}

	public void setCurrentPositionLabel(String text) {
		currentPositionLabel.setText(text);
	}

	public void close() throws IOException {
		document.close();
	}

	public int getCurrentLineNumber() {
		return currentStartingLineNumber;
	}

}
