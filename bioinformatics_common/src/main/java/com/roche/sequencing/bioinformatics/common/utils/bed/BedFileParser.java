package com.roche.sequencing.bioinformatics.common.utils.bed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class BedFileParser {

	public static ParsedBedFile parseBedFile(File bedFile) throws IOException {
		ParsedBedFile parsedBedFile = new ParsedBedFile();

		try (BufferedReader reader = new BufferedReader(new FileReader(bedFile))) {
			String line;

			BedTrack currentTrack = new BedTrack();
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
						currentTrack = new BedTrack();
						parsedBedFile.addTrack(currentTrack);
					}
					currentTrack.addBrowserCommand(line);
				} else if (line.startsWith("track")) {
					if (entriesAddedToCurrentTrack > 0) {
						entriesAddedToCurrentTrack = 0;
						currentTrack = new BedTrack();
						parsedBedFile.addTrack(currentTrack);
					}
					String lineWithoutTrackText = line.substring(7);
					String[] nameValuePairs = lineWithoutTrackText.split(" ");
					for (String pair : nameValuePairs) {
						String[] splitPair = pair.split("=");
						if (splitPair.length != 2) {
							throw new IllegalStateException("Unable to split name value pair[" + pair + "] on line[" + currentLineNumber + "] of bed file[" + bedFile.getAbsolutePath() + "].");
						} else {
							String name = splitPair[0];
							String value = splitPair[1];
							currentTrack.addTrackNameValuePair(name, value);
						}
					}
				} else {
					String[] splitLine = line.split(StringUtil.TAB);

					if (splitLine.length < 3) {
						throw new IllegalStateException("Line number[" + currentLineNumber + "] must contain at least three columsn to be a valid bed entry in file[" + bedFile.getAbsolutePath()
								+ "].");
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
								score = Integer.parseInt(splitLine[index]);
								break;
							case 5:
								strand = splitLine[index].charAt(0);
								break;
							case 6:
								thickStart = Integer.parseInt(splitLine[index]);
								break;
							case 7:
								thickEnd = Integer.parseInt(splitLine[index]);
								break;
							case 8:
								String[] splitRgb = splitLine[index].split(",");
								itemRgb = new RGB(Integer.parseInt(splitRgb[0]), Integer.parseInt(splitRgb[1]), Integer.parseInt(splitRgb[2]));
								break;
							case 9:
								blockCount = Integer.parseInt(splitLine[index]);
								break;
							case 10:
								String[] splitSizes = splitLine[index].split(",");
								blockSizes = new ArrayList<Integer>();
								for (String splitSize : splitSizes) {
									blockSizes.add(Integer.parseInt(splitSize));
								}
								break;
							case 11:
								String[] splitStarts = splitLine[index].split(",");
								blockStarts = new ArrayList<Integer>();
								for (String splitStart : splitStarts) {
									blockStarts.add(Integer.parseInt(splitStart));
								}
								break;
							default:
								throw new AssertionError();
							}
						}

						currentTrack.addBedEntry(new BedEntry(chromosomeName, start, stop, name, score, strand, thickStart, thickEnd, itemRgb, blockCount, blockSizes, blockStarts));

					} else {
						currentTrack.addBedEntry(new SimpleBedEntry(chromosomeName, start, stop));
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
