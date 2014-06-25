package com.roche.sequencing.bioinformatics.genome;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class GenomeIdentifier {

	private final static Logger logger = LoggerFactory.getLogger(GenomeIdentifier.class);

	private final static int GENOME_COLUMN_INDEX = 0;
	private final static int CONTAINER_COLUMN_INDEX = 1;
	private final static int SIZE_COLUMN_INDEX = 2;
	private final static String NO_MATCH_STRING = "NO_MATCH";
	private static boolean genomeInitializeAttempted = false;
	private static boolean genomeInitialized = false;

	private final static String GENOME_SIZES_TSV_FILE_NAME = "genome_chromosome_sizes_with_windows_formatting.tsv";

	private static Map<String, Map<String, Integer>> chromosomeSizesByChromosomeByGenomeName;

	/**
	 * Checks if there is a stored genome that contains chromosomes that have matching names and sizes
	 * 
	 * @param chromosomeSizesByName
	 * @return lower case genome name of a stored genome that contains chromosomes with matching names and sizes for each of the provided names and sizes or null if no such genome exists
	 */
	public static String getMatchingGenomeName(Map<String, Integer> chromosomeSizesByName) {
		String matchingGenomeName = null;
		initializeGenomes();
		if (genomeInitialized) {
			genomeLoop: for (String genomeName : chromosomeSizesByChromosomeByGenomeName.keySet()) {
				Map<String, Integer> storedChromosomeSizesByChromosomeName = chromosomeSizesByChromosomeByGenomeName.get(genomeName);
				Set<Entry<String, Integer>> entrySet = storedChromosomeSizesByChromosomeName.entrySet();
				boolean allSizesMatch = entrySet.containsAll(chromosomeSizesByName.entrySet());
				if (allSizesMatch) {
					matchingGenomeName = genomeName;
					break genomeLoop;
				}
			}
		} else {
			throw new AssertionError();
		}

		return matchingGenomeName;
	}

	public static String createMismatchGenomeReportText(String expectedGenome, Map<String, Integer> chromosomeSizesByNameFromBam) throws IOException {
		initializeGenomes();
		StringBuilder reportText = new StringBuilder();
		if (genomeInitialized) {

			Map<String, Integer> expectedChromosomeSizes = chromosomeSizesByChromosomeByGenomeName.get(expectedGenome);
			String preheader = "#expected genome=" + expectedGenome + "  *-indicates mismatch" + StringUtil.NEWLINE;
			String header = expectedGenome + "_sequence_name" + StringUtil.TAB + expectedGenome + "_sequence_length" + StringUtil.TAB + "BAM_sequence_name" + StringUtil.TAB + "BAM_sequence_length"
					+ StringUtil.NEWLINE;
			reportText.append(preheader);
			reportText.append(header);

			Set<String> matchingNames = new HashSet<String>();
			for (Entry<String, Integer> entryFromBam : chromosomeSizesByNameFromBam.entrySet()) {
				String chromosomeNameFromBam = entryFromBam.getKey();
				Integer sizeFromBam = entryFromBam.getValue();
				Integer expectedChromosomeSize = expectedChromosomeSizes.get(chromosomeNameFromBam);
				if (expectedChromosomeSize != null) {
					matchingNames.add(chromosomeNameFromBam);
					if (expectedChromosomeSize.equals(sizeFromBam)) {
						reportText.append(chromosomeNameFromBam + StringUtil.TAB + expectedChromosomeSize + StringUtil.TAB + chromosomeNameFromBam + StringUtil.TAB + sizeFromBam + StringUtil.NEWLINE);
					} else {
						reportText.append(chromosomeNameFromBam + StringUtil.TAB + "*" + expectedChromosomeSize + StringUtil.TAB + chromosomeNameFromBam + StringUtil.TAB + "*" + sizeFromBam
								+ StringUtil.NEWLINE);
					}
				} else {
					reportText.append(NO_MATCH_STRING + StringUtil.TAB + NO_MATCH_STRING + StringUtil.TAB + chromosomeNameFromBam + StringUtil.TAB + sizeFromBam + StringUtil.NEWLINE);
				}
			}

			for (Entry<String, Integer> expectedEntry : expectedChromosomeSizes.entrySet()) {
				String expectedChromosomeName = expectedEntry.getKey();
				if (!matchingNames.contains(expectedChromosomeName)) {
					Integer expectedSize = expectedEntry.getValue();
					reportText.append(expectedChromosomeName + StringUtil.TAB + expectedSize + StringUtil.TAB + NO_MATCH_STRING + StringUtil.TAB + NO_MATCH_STRING + StringUtil.NEWLINE);
				}
			}

		} else {
			throw new AssertionError();
		}
		return reportText.toString();
	}

	private static void initializeGenomes() {
		if (!genomeInitializeAttempted) {
			genomeInitializeAttempted = true;
			chromosomeSizesByChromosomeByGenomeName = new HashMap<String, Map<String, Integer>>();
			InputStream stream = GenomeIdentifier.class.getResourceAsStream(GENOME_SIZES_TSV_FILE_NAME);
			String fileAsString;
			try {
				fileAsString = FileUtil.readStreamAsString(stream);
				// the file was originall created using windows so it has windows line ends
				String[] splitByLines = fileAsString.split(StringUtil.WINDOWS_NEWLINE);
				for (String line : splitByLines) {
					String[] splitByColumns = line.split(StringUtil.TAB);

					String genome = splitByColumns[GENOME_COLUMN_INDEX];
					genome = genome.toLowerCase();
					String container = splitByColumns[CONTAINER_COLUMN_INDEX];
					String sizeAsString = splitByColumns[SIZE_COLUMN_INDEX];
					int size = Integer.parseInt(sizeAsString);

					Map<String, Integer> chromosomeSizesByChromosome = null;
					if (chromosomeSizesByChromosomeByGenomeName.containsKey(genome)) {
						chromosomeSizesByChromosome = chromosomeSizesByChromosomeByGenomeName.get(genome);
					} else {
						chromosomeSizesByChromosome = new LinkedHashMap<String, Integer>();
					}
					chromosomeSizesByChromosome.put(container, size);
					chromosomeSizesByChromosomeByGenomeName.put(genome, chromosomeSizesByChromosome);
				}
				genomeInitialized = true;
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}
}
