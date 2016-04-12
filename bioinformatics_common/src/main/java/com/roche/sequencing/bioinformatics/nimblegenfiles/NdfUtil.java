package com.roche.sequencing.bioinformatics.nimblegenfiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.DelimitedFileParserUtil;
import com.roche.sequencing.bioinformatics.common.utils.IDelimitedLineParser;
import com.roche.sequencing.bioinformatics.common.utils.InputStreamFactory;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class NdfUtil {

	private final static String[] NDF_PEPTIDE_CONTROLS_HEADER = new String[] { "PROBE_CLASS", "PEPTIDE_SEQUENCE" };

	private final static String[] PROBE_CLASS_TO_EXCLUDE = new String[] { "control:cycle_control", "control:drift" };// ,"control:stc"};

	public static String[] getPeptideControls(File file) throws IOException {
		LineParser lineParser = new LineParser();
		DelimitedFileParserUtil.parseFile(new InputStreamFactory(file), NDF_PEPTIDE_CONTROLS_HEADER, lineParser, StringUtil.TAB, false);
		List<String> peptideControls = lineParser.getPeptideControls();
		return peptideControls.toArray(new String[0]);
	}

	private static class LineParser implements IDelimitedLineParser {
		private final Set<String> peptideControls;

		public LineParser() {
			this.peptideControls = new HashSet<String>();
		}

		@Override
		public void parseDelimitedLine(Map<String, String> headerNameToValue) {
			String probeClass = headerNameToValue.get(NDF_PEPTIDE_CONTROLS_HEADER[0]);
			String peptideSequence = headerNameToValue.get(NDF_PEPTIDE_CONTROLS_HEADER[1]);

			if (!ArraysUtil.contains(PROBE_CLASS_TO_EXCLUDE, probeClass.toLowerCase())) {
				String[] splitPeptideSequence = peptideSequence.split("\\|");
				List<String> peptides = new ArrayList<String>();
				for (String peptide : splitPeptideSequence) {
					peptide = peptide.replaceAll("X", "");
					if (!peptide.isEmpty()) {
						peptides.add(peptide);
					}
				}
				if (peptides.size() > 0) {
					peptideControls.add(ArraysUtil.toString(peptides.toArray(new String[0]), "|"));
				}
			}
		}

		@Override
		public void doneParsing(int linesOfData, String[] headerNames) {
		}

		@Override
		public void threadInterrupted() {
		}

		public List<String> getPeptideControls() {
			return new ArrayList<String>(peptideControls);
		}
	}
}
