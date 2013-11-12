package com.roche.sandbox;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.ColumnArrangement;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;

import com.roche.imageexporter.Graphics2DImageExporter;
import com.roche.imageexporter.Graphics2DImageExporter.ImageType;
import com.roche.sequencing.bioinformatics.common.utils.DelimitedFileParserUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class PieChart {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		File prefuppSummaryFile = new File("D:/labchip/results/_25pM_4hr_200bpprefupp_summary.txt");
		File probeBreakdownFile = new File("D:/labchip/results/_25pM_4hr_200bp_probe_breakdown.txt");
		File outputFile = new File("D:/labchip/results/_25pM_4hr_200bp_read_pairs_pie_charts.pdf");
		generateVisualReport("Read Pair Summary", prefuppSummaryFile, probeBreakdownFile, outputFile);
	}

	public static void generateVisualReport(String title, File prefuppSummaryFile, File probeBreakdownFile, File outputFile) throws IOException {

		Map<String, Integer> unmappedData = new HashMap<String, Integer>();

		Map<String, Integer> assignedData = new HashMap<String, Integer>();

		String[] probeBreakdownHeader = new String[] { "probe_id", "unique_mapped_on_target_read_pairs", "duplicate_mapped_on_target_read_pairs", "mapped_on_target_read_pairs", "unmapped_read_pairs",
				"total_read_pairs" };
		Iterator<Map<String, String>> probeBreakdowns = DelimitedFileParserUtil.getHeaderNameToValueMapRowIteratorFromDelimitedFile(probeBreakdownFile, probeBreakdownHeader, StringUtil.TAB);
		int unmappedAssignedPairs = 0;
		while (probeBreakdowns.hasNext()) {
			Map<String, String> probeBreakdown = probeBreakdowns.next();
			String probeId = probeBreakdown.get(probeBreakdownHeader[0]);
			int uniqueMappedOnTargetReadPairs = Integer.valueOf(probeBreakdown.get(probeBreakdownHeader[1]));
			int duplicateMappedOnTargetReadPairs = Integer.valueOf(probeBreakdown.get(probeBreakdownHeader[2]));
			int mappedOnTargetReadPairs = Integer.valueOf(probeBreakdown.get(probeBreakdownHeader[3]));
			int unmappedReadPairs = Integer.valueOf(probeBreakdown.get(probeBreakdownHeader[4]));
			int totalReadPairs = Integer.valueOf(probeBreakdown.get(probeBreakdownHeader[5]));
			unmappedData.put(probeId, unmappedReadPairs);
			assignedData.put(probeId, unmappedReadPairs + mappedOnTargetReadPairs);
			unmappedAssignedPairs += unmappedReadPairs;
		}

		Map<String, String> prefuppSummary = DelimitedFileParserUtil.parseNameDelimiterValueNewLineFile(prefuppSummaryFile, StringUtil.TAB);
		int totalReads = Integer.valueOf(prefuppSummary.get("total_input_reads"));
		int totalReadPairs = totalReads / 2;
		int onTargetReads = Integer.valueOf(prefuppSummary.get("on_target_reads"));
		int mappedOnTargetPairs = onTargetReads / 2;
		int totalUnmappedReadPairs = totalReadPairs - mappedOnTargetPairs;
		int unmappedUnassignedToProbe = totalUnmappedReadPairs - unmappedAssignedPairs;
		unmappedData.put("Unassigned", unmappedUnassignedToProbe);

		Map<String, Integer> allReadPairsData = new HashMap<String, Integer>();
		allReadPairsData.put("Mapped Pairs On-Target", mappedOnTargetPairs);
		allReadPairsData.put("Unmapped Pairs Assigned To Probe", unmappedAssignedPairs);
		allReadPairsData.put("Unmapped Pairs Unassigned To Probe", unmappedUnassignedToProbe);
		// allReadPairsData.put("Mapped On-Target Pairs", 375002);
		// allReadPairsData.put("Mapped Off-Target Pairs", 22537);
		// allReadPairsData.put("Unmapped Assigned To Probe", 21034);
		// allReadPairsData.put("Unmapped UnAssigned To Probe", 299724);

		Graphics2DImageExporter imageExporter = new Graphics2DImageExporter(ImageType.PDF, 1300, 2000);
		Graphics2D graphics = imageExporter.getGraphics2D();
		graphics.setColor(Color.black);
		graphics.setFont(graphics.getFont().deriveFont((float) 35));
		graphics.drawString(title, 40, 50);

		generatePieChart("All Read Pairs", graphics, new Rectangle(50, 75, 600, 600), allReadPairsData, true, false);
		generatePieChart("Unmapped Read Pairs by Probe", graphics, new Rectangle(700, 75, 600, 600), unmappedData, true, true);
		// generatePieChart("Unmapped Read Pairs by Probe", graphics, new Rectangle(50, 725, 600, 1100), unmappedData, true);
		generatePieChart("All Probe Related Read Pairs (Mapped and Unmapped)", graphics, new Rectangle(100, 725, 1100, 1100), assignedData, false, true);

		try {
			imageExporter.exportImage(outputFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void generatePieChart(String title, Graphics2D graphics, Rectangle drawLocation, Map<String, Integer> dataset, boolean includeLabels, boolean groupLessThanOnePercentInOther) {

		DefaultPieDataset pieDataset = new DefaultPieDataset();
		List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>();
		list.addAll(dataset.entrySet());
		Collections.sort(list, new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		int other = 0;
		double sum = 0;
		for (Entry<String, Integer> data : list) {
			sum += data.getValue();
		}
		for (Entry<String, Integer> data : list) {
			double value = data.getValue();
			double percent = value / sum;
			if (groupLessThanOnePercentInOther && percent < 0.01) {
				other += value;
			} else {
				pieDataset.setValue(data.getKey(), data.getValue());
			}
		}
		if (other > 0) {
			pieDataset.setValue("Other", other);
		}

		JFreeChart chart = ChartFactory.createPieChart(title, pieDataset, // data
				true, true, // tooltips
				false // no URL generation
				);

		LegendTitle legend = (LegendTitle) chart.getSubtitle(0);
		legend.setPosition(RectangleEdge.BOTTOM);
		legend.setHorizontalAlignment(HorizontalAlignment.LEFT);

		// set a custom background for the chart
		// chart.setBackgroundPaint(new GradientPaint(new Point(0, 0), new Color(20, 20, 20), new Point(400, 200), Color.DARK_GRAY));

		// customise the title position and font
		TextTitle t = chart.getTitle();
		t.setHorizontalAlignment(HorizontalAlignment.LEFT);
		t.setPaint(Color.black);

		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setOutlineVisible(false);
		plot.setBackgroundPaint(null);
		plot.setInteriorGap(0.0);
		plot.setBaseSectionOutlinePaint(Color.black);

		LegendTitle legendTitle = ((JFreeChart) chart).getLegend();
		LegendTitle legendTitleNew = new LegendTitle(plot, new ColumnArrangement(), new ColumnArrangement());
		legendTitleNew.setPosition(legendTitle.getPosition());
		legendTitleNew.setBackgroundPaint(legendTitle.getBackgroundPaint());

		// Remove old Legend
		((JFreeChart) chart).removeLegend();
		// Add new Legend
		((JFreeChart) chart).addLegend(legendTitleNew);

		PieSectionLabelGenerator chartGen = new StandardPieSectionLabelGenerator("{0}: {2}", new DecimalFormat("###,###,###,###"), new DecimalFormat("0%"));
		PieSectionLabelGenerator legendGen = new StandardPieSectionLabelGenerator("{0}: {1} ({2})", new DecimalFormat("###,###,###,###"), new DecimalFormat("0%"));
		if (includeLabels) {
			plot.setLabelGenerator(chartGen);
		} else {
			plot.setLabelGenerator(null);
		}

		plot.setLegendLabelGenerator(legendGen);

		chart.draw(graphics, drawLocation);

	}
}
