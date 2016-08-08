package com.roche.sequencing.bioinformatics.common.genome;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.roche.sequencing.bioinformatics.common.sequence.SimpleNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;

public class FastaDirectoryParser {

	private final static int BYTE_BUFFER_SIZE = 4096;
	private final static String FASTA_HEADER_LINE_START = ">";

	public static void parseFastaFile(File fastaDirectoryOrFile, IParsedFastaProcessor fastaProcessor) throws FileNotFoundException, IOException {
		List<File> genomeFiles = new ArrayList<File>();
		if (fastaDirectoryOrFile.isDirectory()) {
			for (String fileName : fastaDirectoryOrFile.list(new FastaFileNameFilter())) {
				genomeFiles.add(new File(fastaDirectoryOrFile, fileName));
			}
		} else {
			genomeFiles.add(fastaDirectoryOrFile);
		}
		for (File fastaFile : genomeFiles) {
			String currentContainerName = null;
			SimpleNucleotideCodeSequence currentSequence = new SimpleNucleotideCodeSequence();
			long currentStartTime = System.currentTimeMillis();
			try (BufferedReader fastaBufferedReader = new BufferedReader(new FileReader(fastaFile), BYTE_BUFFER_SIZE)) {
				String line;
				while ((line = fastaBufferedReader.readLine()) != null) {
					if (line.startsWith(FASTA_HEADER_LINE_START)) {
						if (currentContainerName != null) {
							fastaProcessor.sequenceProcessed(currentContainerName, currentSequence);
							currentSequence = new SimpleNucleotideCodeSequence();
							System.out.println("done with " + currentContainerName + " in " + DateUtil.convertMillisecondsToHHMMSS(System.currentTimeMillis() - currentStartTime));
							currentStartTime = System.currentTimeMillis();
						}
						// start a new container
						int endIndex = line.length();
						int indexOfSpace = line.indexOf(" ");
						if (indexOfSpace >= 1) {
							endIndex = indexOfSpace;
						}
						if (endIndex > 1) {
							currentContainerName = line.substring(1, endIndex);
						}
					} else {
						// write the information to the genome file using the current container name

						currentSequence.append(new SimpleNucleotideCodeSequence(line.toUpperCase()));

					}
				}
				if (currentContainerName != null) {
					fastaProcessor.sequenceProcessed(currentContainerName, currentSequence);
					System.out.println("done with " + currentContainerName + " in " + DateUtil.convertMillisecondsToHHMMSS(System.currentTimeMillis() - currentStartTime));
					currentStartTime = System.currentTimeMillis();
				}
			}
		}
		fastaProcessor.doneProcessing();

	}
}
