package com.roche.sequencing.bioinformatics.common.utils.fasta;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;

public class FastaFileParser {

	public static String DESCRIPTION_LINE_PRECURSOR = ">";

	public static ParsedFastaFile parseFastaFile(File fastaFile) throws IOException {
		ParsedFastaFile parsedFastaFile = new ParsedFastaFile();

		try (BufferedReader reader = new BufferedReader(new FileReader(fastaFile))) {
			String line;

			String currentDescription = null;
			StringBuilder currentSequence = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					// skip
				} else if (line.startsWith(DESCRIPTION_LINE_PRECURSOR)) {
					if (currentDescription != null) {
						parsedFastaFile.addFastaEntry(new FastaEntry(currentDescription, new IupacNucleotideCodeSequence(currentSequence.toString())));
					}
					currentDescription = line;
					currentSequence = new StringBuilder();
				} else {
					currentSequence.append(line);
				}
			}
			if (currentDescription != null) {
				parsedFastaFile.addFastaEntry(new FastaEntry(currentDescription, new IupacNucleotideCodeSequence(currentSequence.toString())));
			}
		}
		return parsedFastaFile;
	}
}
