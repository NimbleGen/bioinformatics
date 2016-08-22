package com.roche.sequencing.bioinformatics.common.utils.bed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.roche.sequencing.bioinformatics.common.genome.GenomicContext;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class BedFileParser {

	private final static String UNIQUE_TEXT_START = "!$%#";
	private final static String UNIQUE_TEXT_END = "#%$!";

	private BedFileParser() {
		throw new AssertionError();
	}

	public static List<GenomicContext> getGenomicContexts(File bedFile) throws IOException {
		List<GenomicContext> genomicContexts = new ArrayList<GenomicContext>();
		ParsedBedFile parsedBedFile = BedFileParser.parseBedFile(bedFile);

		for (BedTrack bedTrack : parsedBedFile) {
			for (IBedEntry bedEntry : bedTrack.getBedEntries()) {
				String container = bedEntry.getContainerName();
				long start = Math.min(bedEntry.getChromosomeStart(), bedEntry.getChromosomeEnd());
				long stop = Math.max(bedEntry.getChromosomeStart(), bedEntry.getChromosomeEnd());
				Strand strand = Strand.FORWARD;
				if (bedEntry.getStrand() != null) {
					strand = Strand.fromString("" + bedEntry.getStrand());
				}
				String regionName = bedEntry.getName();
				GenomicContext genomicContext = new GenomicContext(container, start, stop, strand, regionName);
				genomicContexts.add(genomicContext);
			}
		}
		return genomicContexts;
	}

	public static ParsedBedFile parseBedFile(File bedFile) throws IOException {
		ParsedBedFile parsedBedFile = new ParsedBedFile();

		try (BufferedReader reader = new BufferedReader(new FileReader(bedFile))) {
			String line;
			BedTrack currentTrack = new BedTrack(bedFile);
			parsedBedFile.addTrack(currentTrack);
			int entriesAddedToCurrentTrack = 0;
			int currentLineNumber = 1;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					// skip
				} else if (line.startsWith("browser")) {
					if (entriesAddedToCurrentTrack > 0) {
						parsedBedFile.addTrack(currentTrack);
						entriesAddedToCurrentTrack = 0;
						currentTrack = new BedTrack(bedFile);
						parsedBedFile.addTrack(currentTrack);
					}
					currentTrack.addBrowserCommand(line);
				} else if (line.startsWith("track")) {
					if (entriesAddedToCurrentTrack > 0) {
						entriesAddedToCurrentTrack = 0;
						currentTrack = new BedTrack(bedFile);
						parsedBedFile.addTrack(currentTrack);
					}
					String lineWithTrackTextRemoved = line.substring(6);

					Map<String, String> quotedTextMap = new HashMap<String, String>();
					while (lineWithTrackTextRemoved.indexOf("\"") != -1) {
						int firstIndex = lineWithTrackTextRemoved.indexOf("\"");
						int secondIndex = lineWithTrackTextRemoved.indexOf("\"", firstIndex + 1);

						if (secondIndex == -1) {
							throw new IllegalStateException("There are an odd number of \"s found in the text[" + line + "] on line[" + currentLineNumber + "] of bed file["
									+ bedFile.getAbsolutePath() + "].");
						}

						String text = lineWithTrackTextRemoved.substring(firstIndex, secondIndex + 1);
						String key = UNIQUE_TEXT_START + quotedTextMap.size() + UNIQUE_TEXT_END;
						quotedTextMap.put(key, text);
						lineWithTrackTextRemoved = lineWithTrackTextRemoved.replace(text, key);
					}

					String[] nameValuePairs = lineWithTrackTextRemoved.split(" ");
					for (String pair : nameValuePairs) {
						String[] splitPair = pair.split("=");
						if (splitPair.length != 2) {
							throw new IllegalStateException("Unable to split name value pair[" + pair + "] on line[" + currentLineNumber + "] of bed file[" + bedFile.getAbsolutePath() + "].");
						} else {
							String name = splitPair[0];
							String value = splitPair[1];
							for (Entry<String, String> entry : quotedTextMap.entrySet()) {
								value = value.replace(entry.getKey(), entry.getValue());
							}
							currentTrack.addTrackNameValuePair(name, value);
						}
					}
				} else {
					String[] splitLine = line.split(StringUtil.TAB + "|\\s+");

					if (splitLine.length < 3) {
						throw new IllegalStateException("Line number[" + currentLineNumber + "], which is as follows: [" + line
								+ "], must contain at least three columns to be a valid bed entry in file[" + bedFile.getAbsolutePath() + "].");
					}

					String chromosomeName = splitLine[0];
					int start = Integer.parseInt(splitLine[1]);
					int stop = Integer.parseInt(splitLine[2]);

					if (splitLine.length > 3) {
						String name = null;
						Integer score = null;
						Character strand = null;
						Integer thickStart = null;
						Integer thickEnd = null;
						RGB itemRgb = null;
						Integer blockCount = null;
						List<Integer> blockSizes = null;
						List<Integer> blockStarts = null;

						for (int index = 3; index < splitLine.length; index++) {
							switch (index) {
							case 3:
								name = splitLine[index];
								break;
							case 4:
								String scoreAsString = splitLine[index];
								if (scoreAsString.isEmpty()) {
									score = null;
								} else {
									try {
										score = Integer.parseInt(scoreAsString);
									} catch (NumberFormatException e) {
										throw new IllegalStateException("The provided value for the score column[" + scoreAsString + "], which is column 5, is not a valid score at line["
												+ currentLineNumber + "] in the bed file[" + bedFile.getAbsolutePath() + "].");
									}
								}
								break;
							case 5:
								strand = splitLine[index].charAt(0);
								break;
							case 6:
								String thickStartAsString = splitLine[index];
								try {
									thickStart = Integer.parseInt(thickStartAsString);
								} catch (NumberFormatException e) {
									throw new IllegalStateException("The provided value for the thick start column[" + thickStartAsString + "], which is column 7, is not a valid thick start at line["
											+ currentLineNumber + "] in the bed file[" + bedFile.getAbsolutePath() + "].");
								}
								break;
							case 7:
								String thickEndAsString = splitLine[index];
								try {
									thickEnd = Integer.parseInt(thickEndAsString);
								} catch (NumberFormatException e) {
									throw new IllegalStateException("The provided value for the thick end column[" + thickEndAsString + "], which is column 8, is not a valid thick end at line["
											+ currentLineNumber + "] in the bed file[" + bedFile.getAbsolutePath() + "].");
								}
								break;
							case 8:
								String rgbValue = splitLine[index];
								if (rgbValue.equals("0")) {
									itemRgb = new RGB(0, 0, 0);
								} else {
									try {
										itemRgb = new RGB(rgbValue);
									} catch (IllegalStateException e) {
										throw new IllegalStateException("The provided value for the itemRgb[" + splitLine[index] + "], which is column 9, is not a valid itemRgb at line["
												+ currentLineNumber + "] in the bed file[" + bedFile.getAbsolutePath() + "].");
									}
								}
								break;
							case 9:
								String blockCountAsString = splitLine[index];
								try {
									blockCount = Integer.parseInt(blockCountAsString);
								} catch (NumberFormatException e) {
									throw new IllegalStateException("The provided value for the blockCount[" + blockCountAsString + "], which is column 10, is not a valid blockCount at line["
											+ currentLineNumber + "] in the bed file[" + bedFile.getAbsolutePath() + "].");
								}
								break;
							case 10:
								String[] splitSizes = splitLine[index].split(",");
								blockSizes = new ArrayList<Integer>();
								for (String splitSize : splitSizes) {
									try {
										blockSizes.add(Integer.parseInt(splitSize));
									} catch (NumberFormatException e) {
										throw new IllegalStateException("The provided value for the blockSizes[" + splitLine[index] + "], which is column 11, is not a valid blockSizes at line["
												+ currentLineNumber + "] in the bed file[" + bedFile.getAbsolutePath() + "].");
									}
								}
								break;
							case 11:
								String[] splitStarts = splitLine[index].split(",");
								blockStarts = new ArrayList<Integer>();
								for (String splitStart : splitStarts) {
									try {
										blockStarts.add(Integer.parseInt(splitStart));
									} catch (NumberFormatException e) {
										throw new IllegalStateException("The provided value for the blockStarts[" + splitLine[index] + "], which is column 12, is not a valid blockStarts at line["
												+ currentLineNumber + "] in the bed file[" + bedFile.getAbsolutePath() + "].");
									}
								}
								break;
							default:
								throw new AssertionError();
							}
						}

						currentTrack.addBedEntry(new BedEntry(chromosomeName, start, stop, name, score, strand, thickStart, thickEnd, itemRgb, blockCount, blockSizes, blockStarts));

					} else {
						currentTrack.addBedEntry(new BedEntry(chromosomeName, start, stop));
					}

					entriesAddedToCurrentTrack++;
				}

				currentLineNumber++;
			}

		}
		return parsedBedFile;
	}

	public static void main(String[] args) throws IOException {
		File bedFile = new File("D:\\kurts_space\\projects\\pete_python\\shortie_data\\coordinates.bed");
		ParsedBedFile parsedBedFile = parseBedFile(bedFile);
		System.out.println(parsedBedFile);
	}

}
