package com.roche.sequencing.bioinformatics.common.utils.gff;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class GffFileParser {

	private GffFileParser() {
		throw new AssertionError();
	}

	public static ParsedGffFile parseGffFile(File gffFile) throws FileNotFoundException, IOException {
		return parseGffFile(new FileInputStream(gffFile));
	}

	public static ParsedGffFile parseGffFile(InputStream gffInputStream) throws IOException {
		ParsedGffFile parsedGffFile = new ParsedGffFile();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(gffInputStream))) {
			String line = "";
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					// skip
				} else {
					String[] splitLine = line.split(StringUtil.TAB);
					if (splitLine.length == 9) {
						String sequenceId = splitLine[0];
						String source = splitLine[1];
						String type = splitLine[2];
						String startAsString = splitLine[3];
						String stopAsString = splitLine[4];
						String scoreAsString = splitLine[5];
						String strandAsString = splitLine[6];
						// skipping phase
						String attributes = splitLine[8];

						long start = Long.parseLong(startAsString);
						long stop = Long.parseLong(stopAsString);
						double score = Double.parseDouble(scoreAsString);
						Strand strand = Strand.fromString(strandAsString);

						parsedGffFile.addEntry(sequenceId, source, type, start, stop, score, strand, attributes);
					}
				}
			}
		}

		parsedGffFile.sortContainers();

		return parsedGffFile;
	}

}
