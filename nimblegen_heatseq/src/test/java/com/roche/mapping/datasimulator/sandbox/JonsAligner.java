package com.roche.mapping.datasimulator.sandbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
import com.roche.sequencing.bioinformatics.common.alignment.SimpleAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.sequence.ICode;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCode;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class JonsAligner {

	private static Map<String, String> readFastaFile(File inputFastaFile) throws FileNotFoundException, IOException {
		Map<String, String> readToSequenceMap = new LinkedHashMap<String, String>();
		try (BufferedReader reader = new BufferedReader(new FileReader(inputFastaFile), FileUtil.BYTES_PER_KB)) {
			String currentLine;
			String lastSequence = "";
			String lastSequenceIdLine = "";
			while ((currentLine = reader.readLine()) != null) {
				boolean isDescription = (currentLine.startsWith(">"));
				if (isDescription) {
					// write out the last entry we saw
					if (!lastSequenceIdLine.isEmpty()) {
						readToSequenceMap.put(lastSequenceIdLine, lastSequence);
						lastSequenceIdLine = "";
						lastSequence = "";
					}
					// TODO Kurt Heilman 7/16/2013 look for sequenceids that are too long or duplicates
					lastSequenceIdLine = currentLine;
				} else {
					boolean isComment = (currentLine.startsWith(";"));
					boolean isSequence = !isComment;
					if (isSequence) {
						lastSequence += currentLine;
					}
				}
			}

			// write out the last entry
			if (lastSequenceIdLine != null) {
				readToSequenceMap.put(lastSequenceIdLine, lastSequence);
			}
		}
		return readToSequenceMap;
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {

		File outputFile = new File("C:/Users/heilmank/Desktop/output.txt");
		FileWriter fileWriter = new FileWriter(outputFile);
		try (BufferedWriter fastaWriter = new BufferedWriter(fileWriter, FileUtil.BYTES_PER_KB)) {

			ISequence referenceSequence = new IupacNucleotideCodeSequence("NNNWNNNWNNACACTACCGTCGGATGTGCTCTTCCGATCT");
			IAlignmentScorer scorer = new JonsScorer(5, -1, -10, -20, false);
			int maxNs = 25;

			File inputFastaFile = new File("C:/Users/heilmank/Desktop/ATM140Mid18Ref.fa");
			Map<String, String> queries = readFastaFile(inputFastaFile);

			for (String readName : queries.keySet()) {
				fastaWriter.write(readName + "________________________________________________" + StringUtil.NEWLINE);
				ISequence querySequence = new IupacNucleotideCodeSequence(queries.get(readName));

				ISequence extension = querySequence.subSequence(0, 8);
				ISequence ligation = querySequence.subSequence(9, 18);

				ISequence query = extension.subSequence(0, 7);
				query.append(ligation);
				NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(referenceSequence, query, scorer);
				printAlignment(readName, alignment, fastaWriter);

				for (int i = 0; i < maxNs; i++) {
					query = new IupacNucleotideCodeSequence(extension.toString() + net.sf.samtools.util.StringUtil.repeatCharNTimes('N', i) + ligation.toString());
					alignment = new NeedlemanWunschGlobalAlignment(referenceSequence, query, scorer);
					printAlignment(readName, alignment, fastaWriter);
				}
			}
		}

	}

	private static void printAlignment(String readName, NeedlemanWunschGlobalAlignment alignment, Writer writer) throws IOException {
		writer.write(readName + ", edit distance:" + alignment.getEditDistance() + StringUtil.NEWLINE);
		writer.write(alignment.getAlignmentPair().getReferenceAlignment().toString() + StringUtil.NEWLINE);
		writer.write(net.sf.samtools.util.StringUtil.repeatCharNTimes(' ', alignment.getIndexOfFirstMatchInReference()) + alignment.getCigarString().getCigarString(false, true) + StringUtil.NEWLINE);
		writer.write(alignment.getAlignmentPair().getQueryAlignment().toString() + StringUtil.NEWLINE);
		writer.write(StringUtil.NEWLINE);
	}

	private static class JonsScorer extends SimpleAlignmentScorer {

		public JonsScorer(int match, int mismatch, int gapExtension, int gapStart, boolean shouldPenalizeTerminalGaps) {
			super(match, mismatch, gapExtension, gapStart, shouldPenalizeTerminalGaps);
		}

		@Override
		public int getMatchScore(ICode codeOne, ICode codeTwo) {
			int score = 0;
			if (!(codeOne.equals(IupacNucleotideCode.N) || (codeTwo.equals(IupacNucleotideCode.N)))) {
				score = super.getMatchScore(codeOne, codeTwo);
			}
			return score;
		}

	}

}
