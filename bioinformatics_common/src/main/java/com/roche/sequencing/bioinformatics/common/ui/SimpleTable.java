package com.roche.sequencing.bioinformatics.common.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class SimpleTable extends JPanel {

	private static final long serialVersionUID = 1L;
	private final JComponent[][] cellComponents;
	private int[] lastCalcuatedWidthsByColumn;
	private int[] lastCalcuatedHeightsByRow;

	private final static int COLUMN_SPACING = 8;
	private final static int ROW_SPACING = 5;

	private final int MAX_COLUMN_WIDTH = 400;

	public SimpleTable(int numberOfRows, int numberOfColumns) {
		this.cellComponents = new JComponent[numberOfRows][numberOfColumns];
		this.lastCalcuatedHeightsByRow = new int[numberOfRows];
		this.lastCalcuatedWidthsByColumn = new int[numberOfColumns];
		setLayout(null);
	}

	public void setCellValue(int rowIndex, int columnIndex, Object value) {
		setValueAsComponent(rowIndex, columnIndex, value);
	}

	private void setValueAsComponent(int rowIndex, int columnIndex, Object value) {
		JComponent component = null;
		if (value != null) {
			if (value instanceof JComponent) {
				component = (JComponent) value;
			} else {
				JLabel label = new JLabel();
				String[] lines = StringUtil.splitIntoLines(value.toString(), label.getFont(), MAX_COLUMN_WIDTH);
				component = new JLabel("<html>" + ArraysUtil.toString(lines, "<br>") + "</html>");
			}
		}

		JComponent oldComponent = cellComponents[rowIndex][columnIndex];
		if (oldComponent != null) {
			remove(oldComponent);
		}
		add(component);
		cellComponents[rowIndex][columnIndex] = component;
	}

	private void updateRowHeightsAndColumnWidths() {
		for (int i = 0; i < lastCalcuatedHeightsByRow.length; i++) {
			lastCalcuatedHeightsByRow[i] = 0;
		}
		for (int i = 0; i < lastCalcuatedWidthsByColumn.length; i++) {
			lastCalcuatedWidthsByColumn[i] = 0;
		}
		for (int rowIndex = 0; rowIndex < cellComponents.length; rowIndex++) {
			JComponent[] row = cellComponents[rowIndex];
			for (int columnIndex = 0; columnIndex < row.length; columnIndex++) {
				JComponent component = row[columnIndex];
				Dimension componentDimensions;
				if (component != null) {
					componentDimensions = component.getPreferredSize();
				} else {
					componentDimensions = new Dimension(0, 0);
				}

				lastCalcuatedHeightsByRow[rowIndex] = Math.max(lastCalcuatedHeightsByRow[rowIndex], (int) componentDimensions.getHeight());
				lastCalcuatedWidthsByColumn[columnIndex] = Math.max(lastCalcuatedWidthsByColumn[columnIndex], (int) componentDimensions.getWidth());
			}
		}

		int rowSum = 0;
		for (int rowIndex = 0; rowIndex < lastCalcuatedHeightsByRow.length; rowIndex++) {
			rowSum += lastCalcuatedHeightsByRow[rowIndex] + ROW_SPACING;
		}

		int columnSum = COLUMN_SPACING;
		for (int columnIndex = 0; columnIndex < lastCalcuatedWidthsByColumn.length; columnIndex++) {
			columnSum += lastCalcuatedWidthsByColumn[columnIndex] + (2 * COLUMN_SPACING);
		}
		columnSum -= COLUMN_SPACING;

		setPreferredSize(new Dimension(columnSum + 1, rowSum + 1));
	}

	public void updateChildComponentBounds() {
		updateRowHeightsAndColumnWidths();

		int[] startingYByRow = new int[lastCalcuatedHeightsByRow.length];
		int rowSum = 0;
		for (int rowIndex = 0; rowIndex < lastCalcuatedHeightsByRow.length; rowIndex++) {
			startingYByRow[rowIndex] = rowSum;
			rowSum += lastCalcuatedHeightsByRow[rowIndex] + ROW_SPACING;
		}

		int[] startingXByColumn = new int[lastCalcuatedWidthsByColumn.length];
		int columnSum = COLUMN_SPACING;
		for (int columnIndex = 0; columnIndex < lastCalcuatedWidthsByColumn.length; columnIndex++) {
			startingXByColumn[columnIndex] = columnSum;
			columnSum += lastCalcuatedWidthsByColumn[columnIndex] + (2 * COLUMN_SPACING);
		}

		for (int rowIndex = 0; rowIndex < cellComponents.length; rowIndex++) {
			JComponent[] row = cellComponents[rowIndex];
			for (int columnIndex = 0; columnIndex < row.length; columnIndex++) {
				JComponent component = row[columnIndex];
				if (component != null) {
					int cellWidth = lastCalcuatedWidthsByColumn[columnIndex];
					int cellHeight = lastCalcuatedHeightsByRow[rowIndex];

					int cellX = startingXByColumn[columnIndex];
					int cellY = startingYByRow[rowIndex];

					Dimension componentSize = component.getPreferredSize();

					int x = (int) (cellX + ((cellWidth - componentSize.getWidth()) / 2));
					int y = (int) (cellY + ((cellHeight - componentSize.getHeight()) / 2));

					component.setBounds(x, y, (int) componentSize.getWidth(), (int) componentSize.getHeight());
				}
			}
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		// paint the cell borders
		int[] startingYByRow = new int[lastCalcuatedHeightsByRow.length];
		int rowSum = 0;
		for (int rowIndex = 0; rowIndex < lastCalcuatedHeightsByRow.length; rowIndex++) {
			startingYByRow[rowIndex] = rowSum;
			rowSum += lastCalcuatedHeightsByRow[rowIndex] + ROW_SPACING;
		}

		int[] startingXByColumn = new int[lastCalcuatedWidthsByColumn.length];
		int columnSum = 0;
		for (int columnIndex = 0; columnIndex < lastCalcuatedWidthsByColumn.length; columnIndex++) {
			startingXByColumn[columnIndex] = columnSum;
			columnSum += (COLUMN_SPACING + lastCalcuatedWidthsByColumn[columnIndex] + COLUMN_SPACING);
		}

		for (int rowIndex = 0; rowIndex < cellComponents.length; rowIndex++) {
			JComponent[] row = cellComponents[rowIndex];
			for (int columnIndex = 0; columnIndex < row.length; columnIndex++) {
				int cellWidth = lastCalcuatedWidthsByColumn[columnIndex] + (2 * COLUMN_SPACING);
				int cellHeight = lastCalcuatedHeightsByRow[rowIndex] + ROW_SPACING;

				int cellX = startingXByColumn[columnIndex];
				int cellY = startingYByRow[rowIndex];
				g.setColor(Color.black);
				g.drawRect(cellX, cellY, cellWidth, cellHeight);

			}
		}

	}

}
