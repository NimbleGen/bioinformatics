package com.roche.sequencing.bioinformatics.nimblegenfiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.roche.sequencing.bioinformatics.common.stringsequence.ILetter;
import com.roche.sequencing.bioinformatics.common.stringsequence.WordMergerUtil;
import com.roche.sequencing.bioinformatics.common.stringsequence.WordMergerUtil.ThresholdedMergeResults;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.DelimitedFileParserUtil;
import com.roche.sequencing.bioinformatics.common.utils.IDelimitedLineParser;
import com.roche.sequencing.bioinformatics.common.utils.InputStreamFactory;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class NdfUtil {

	private final static String[] NDF_PEPTIDE_CONTROLS_HEADER = new String[] { "PROBE_CLASS", "PEPTIDE_SEQUENCE" };

	private final static String[] PROBE_CLASS_TO_EXCLUDE = new String[] { "control:cycle_control", "control:drift" };// , "control:stc" };

	public static String[] getPeptideControls(File file) throws IOException {
		LineParser lineParser = new LineParser();
		DelimitedFileParserUtil.parseFileMultiThreaded(new InputStreamFactory(file), NDF_PEPTIDE_CONTROLS_HEADER, lineParser, StringUtil.TAB, false);
		List<String> peptideControls = lineParser.getPeptideControls();
		return peptideControls.toArray(new String[0]);
	}

	private static class LineParser implements IDelimitedLineParser {
		private final Set<String> peptideControls;

		public LineParser() {
			this.peptideControls = Collections.synchronizedSet(new HashSet<String>());
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

	private static class Peptide implements ILetter {
		private final String abbreviation;
		private final int score;

		public Peptide(String abbreviation, int score) {
			super();
			this.abbreviation = abbreviation;
			this.score = score;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((abbreviation == null) ? 0 : abbreviation.hashCode());
			result = prime * result + score;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Peptide other = (Peptide) obj;
			if (abbreviation == null) {
				if (other.abbreviation != null)
					return false;
			} else if (!abbreviation.equals(other.abbreviation))
				return false;
			if (score != other.score)
				return false;
			return true;
		}

		public String toString() {
			return abbreviation;
		}

		@Override
		public int getScore() {
			return score;
		}

	}

	public static String[] getNdfPeptides(File file) throws IOException {
		NdfPeptideParser lineParser = new NdfPeptideParser();
		DelimitedFileParserUtil.parseFileMultiThreaded(new InputStreamFactory(file), new String[] { "PROBE_SEQUENCE" }, lineParser, StringUtil.TAB, false);
		List<String> peptideControls = lineParser.getPeptideControls();
		return peptideControls.toArray(new String[0]);
	}

	private static class NdfPeptideParser implements IDelimitedLineParser {
		private final Set<String> peptideControls;

		public NdfPeptideParser() {
			this.peptideControls = Collections.synchronizedSet(new LinkedHashSet<String>());
		}

		@Override
		public void parseDelimitedLine(Map<String, String> headerNameToValue) {
			String peptideSequence = headerNameToValue.get("PROBE_SEQUENCE");
			if (peptideSequence != null) {
				peptideControls.add(peptideSequence.replaceAll("X", ""));
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

	public static void main(String[] args) throws IOException {
		long start = System.currentTimeMillis();
		runTwo();
		long end = System.currentTimeMillis();
		System.out.println("total time:" + (end - start));
	}

	public static void runOne() throws IOException {
		File file = new File("C:\\Users\\heilmank\\Desktop\\example_control_sequences_multi_char_AA_original.txt");
		String[] controls = getPeptideControls(file);

		List<ILetter[]> probes = new ArrayList<ILetter[]>();

		System.out.println("size:" + controls.length);
		for (String string : controls) {
			List<Peptide> probe = new ArrayList<NdfUtil.Peptide>();
			String[] split = string.split("\\|");
			for (int i = 0; i < split.length; i++) {
				probe.add(new Peptide(split[i], 1));
			}
			probes.add(probe.toArray(new Peptide[0]));
			System.out.println(string);
		}

		ILetter[] letters = WordMergerUtil.merge(probes);
		System.out.println(ArraysUtil.toString(letters, ", "));
		System.out.println("size:" + letters.length);

	}

	public static void runTwo() throws IOException {
		// File file = new File("D:\\kurts_space\\projects\\5p\\151209_Peptoid_v1_UCSC_PEP.ndf");
		File file = new File("D:\\kurts_space\\projects\\5p\\130925_All_5mer_v2_Innov_PEP_PX1.ndf");
		String[] controls = getNdfPeptides(file);

		List<ILetter[]> probes = new ArrayList<ILetter[]>();

		System.out.println("size:" + controls.length);
		for (String string : controls) {
			List<Peptide> probe = new ArrayList<NdfUtil.Peptide>();
			for (int i = 0; i < string.length(); i++) {
				probe.add(new Peptide("" + string.charAt(i), 1));
			}
			probes.add(probe.toArray(new Peptide[0]));
		}

		ILetter[] letters = WordMergerUtil.merge(probes);
		System.out.println(ArraysUtil.toString(letters, ", "));
		System.out.println("size:" + letters.length);

	}

	public static void runThree() throws IOException {
		long start = System.currentTimeMillis();
		// File file = new File("D:\\kurts_space\\projects\\5p\\151209_Peptoid_v1_UCSC_PEP.ndf");
		File file = new File("D:\\kurts_space\\projects\\5p\\130925_All_5mer_v2_Innov_PEP_PX1.ndf");
		String[] peptides = getNdfPeptides(file);

		File file2 = new File("C:\\Users\\heilmank\\Desktop\\example_control_sequences_multi_char_AA_original.txt");
		String[] controls = getPeptideControls(file2);

		long end = System.currentTimeMillis();
		System.out.println("Peptides:" + peptides.length + " controls:" + controls.length + " time:" + (end - start));

		List<ILetter[]> extraProbes = new ArrayList<ILetter[]>();
		for (String string : peptides) {
			List<Peptide> probe = new ArrayList<NdfUtil.Peptide>();
			for (int i = 0; i < string.length(); i++) {
				probe.add(new Peptide("" + string.charAt(i), 1));
			}
			extraProbes.add(probe.toArray(new Peptide[0]));
		}

		List<ILetter[]> requiredProbes = new ArrayList<ILetter[]>();
		for (String string : controls) {
			List<Peptide> probe = new ArrayList<NdfUtil.Peptide>();
			String[] split = string.split("\\|");
			for (int i = 0; i < split.length; i++) {
				probe.add(new Peptide(split[i], 1));
			}
			requiredProbes.add(probe.toArray(new Peptide[0]));
		}

		// ThresholdedMergeResults results = WordMergerUtil.merge(new ArrayList<ILetter[]>(), requiredProbes, 100);
		ThresholdedMergeResults results = WordMergerUtil.merge(requiredProbes, extraProbes, 300);
		ILetter[] letters = results.getResult();
		System.out.println(ArraysUtil.toString(letters, ", "));

		System.out.println("Excluded:" + results.getExcludedWords().size() + " words");
		System.out.println("Included:" + (extraProbes.size() + requiredProbes.size() - results.getExcludedWords().size()) + " words");
		System.out.println("Size:" + letters.length);
	}
}
