package com.roche.sequencing.bioinformatics.common.utils.bed;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.sequencing.bioinformatics.common.genome.GenomicRangedCoordinate;

public class BedTrack {

	private Logger logger = LoggerFactory.getLogger(BedTrack.class);

	private final static String TRACK_NAME = "name";
	private final static String USE_SCORE_NAME = "useScore";
	private final static String ITEM_RGB_NAME = "itemRgb";
	private final static String COLOR_BY_STRAND_NAME = "colorByStrand";
	private final static String VISIBILITY_NAME = "visibility";

	private final File file;
	private final List<String> browserCommands;
	private final Map<String, String> nameValuePairs;
	private final Map<String, List<IBedEntry>> bedEntriesByContainer;
	private final Map<String, Long> largestCoordinateByContainer;

	public BedTrack(File file) {
		super();
		this.file = file;
		this.browserCommands = new ArrayList<String>();
		this.nameValuePairs = new HashMap<String, String>();
		this.bedEntriesByContainer = new HashMap<String, List<IBedEntry>>();
		this.largestCoordinateByContainer = new HashMap<String, Long>();
	}

	void addBrowserCommand(String browserCommand) {
		this.browserCommands.add(browserCommand);
	}

	void addTrackNameValuePair(String name, String value) {
		this.nameValuePairs.put(name, value);
	}

	public String getName() {
		String name = nameValuePairs.get(TRACK_NAME);
		if (name == null) {
			name = "";
		}
		return name;
	}

	public File getFile() {
		return file;
	}

	public boolean isUseBedScore() {
		String useScoreValue = nameValuePairs.get(USE_SCORE_NAME);
		boolean isUseBedScore = useScoreValue != null && useScoreValue.equals("1");
		return isUseBedScore;
	}

	public boolean isItemRgbOn() {
		String itemRgb = nameValuePairs.get(ITEM_RGB_NAME);
		boolean isItemRgbOn = itemRgb != null && itemRgb.toLowerCase().contains("on");
		return isItemRgbOn;
	}

	public boolean isColoredByStrand() {
		boolean isColoredByStrand = getBedColorsByStrand() != null;
		return isColoredByStrand;
	}

	public BedColorsByStrand getBedColorsByStrand() {
		BedColorsByStrand bedColorsByStrand = null;

		String colorByStrandValue = nameValuePairs.get(COLOR_BY_STRAND_NAME);
		if (colorByStrandValue != null) {
			colorByStrandValue = colorByStrandValue.replaceAll("\"", "");
			String[] splitBySpace = colorByStrandValue.split("\\s");
			if (splitBySpace.length == 2) {
				String firstColorString = splitBySpace[0];
				String secondColorString = splitBySpace[1];

				try {
					RGB firstRGB = new RGB(firstColorString);
					RGB secondRGB = new RGB(secondColorString);
					bedColorsByStrand = new BedColorsByStrand(firstRGB.getColor(), secondRGB.getColor());
				} catch (IllegalStateException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}

		return bedColorsByStrand;
	}

	void addBedEntry(IBedEntry bedEntry) {
		String containerName = bedEntry.getContainerName();

		Long existingLargestCoordinate = largestCoordinateByContainer.get(containerName);
		if (existingLargestCoordinate == null || existingLargestCoordinate < bedEntry.getChromosomeEnd()) {
			largestCoordinateByContainer.put(containerName, (long) bedEntry.getChromosomeEnd());
		}

		List<IBedEntry> bedEntries = null;
		if (bedEntriesByContainer.containsKey(containerName)) {
			bedEntries = bedEntriesByContainer.get(containerName);
		} else {
			bedEntries = new ArrayList<IBedEntry>();
			bedEntriesByContainer.put(containerName, bedEntries);
		}
		bedEntries.add(bedEntry);
	}

	public List<String> getBrowserCommands() {
		return Collections.unmodifiableList(browserCommands);
	}

	public Map<String, String> getNameValuePairs() {
		return Collections.unmodifiableMap(nameValuePairs);
	}

	public List<IBedEntry> getBedEntries() {
		List<IBedEntry> bedEntries = new ArrayList<IBedEntry>();
		for (List<IBedEntry> value : bedEntriesByContainer.values()) {
			bedEntries.addAll(value);
		}
		return bedEntries;
	}

	public List<IBedEntry> getSortedBedEntries(String chromosomeName) {
		List<IBedEntry> bedEntriesForChromosome = bedEntriesByContainer.get(chromosomeName);
		if (bedEntriesForChromosome == null) {
			bedEntriesForChromosome = Collections.emptyList();
		}
		Collections.sort(bedEntriesForChromosome, new Comparator<IBedEntry>() {
			@Override
			public int compare(IBedEntry o1, IBedEntry o2) {
				int result = Long.compare(o1.getChromosomeStart(), o2.getChromosomeStart());
				if (result == 0) {
					result = Long.compare(o1.getChromosomeEnd(), o2.getChromosomeEnd());
				}
				return result;
			}
		});
		return Collections.unmodifiableList(bedEntriesForChromosome);
	}

	public List<String> getSortedChromsomeNames() {
		List<String> sortedContainerNames = new ArrayList<String>(bedEntriesByContainer.keySet());
		Collections.sort(sortedContainerNames, GenomicRangedCoordinate.CONTAINER_COMPARATOR);
		return sortedContainerNames;
	}

	public GenomicRangedCoordinate getGenomicCoordinatesOfNextFeature(GenomicRangedCoordinate currentGenomicCoordinates) {
		GenomicRangedCoordinate nextFeature = null;

		List<String> sortedContainerNames = getSortedChromsomeNames();
		if (sortedContainerNames.size() > 0) {
			int containerIndex = sortedContainerNames.indexOf(currentGenomicCoordinates.getContainerName());

			if (containerIndex == -1) {
				// find the first container passed this container
				int currentIndex = 0;
				while (containerIndex == -1 && currentIndex < sortedContainerNames.size()) {
					String currentContainerName = sortedContainerNames.get(currentIndex);
					int comparisonResult = GenomicRangedCoordinate.CONTAINER_COMPARATOR.compare(currentGenomicCoordinates.getContainerName(), currentContainerName);
					if (comparisonResult < 0) {
						containerIndex = currentIndex;
					}
					currentIndex++;
				}

				if (containerIndex == -1) {
					containerIndex = 0;
				}
			}

			int containersChecked = 0;
			// note the starting container may get checked twice
			// if there is only one container and we are at the last entry we
			// have to start over in the same container
			while (nextFeature == null && containersChecked <= sortedContainerNames.size()) {
				String currentContainer = sortedContainerNames.get(containerIndex);
				List<IBedEntry> bedEntriesForContainer = getSortedBedEntries(currentContainer);
				bedLoop: for (IBedEntry bedEntry : bedEntriesForContainer) {
					boolean hasLoopedThroughAllContaines = (containersChecked == sortedContainerNames.size());
					boolean nextFeatureFound = bedEntry.getChromosomeStart() > currentGenomicCoordinates.getStartLocation()
							&& (bedEntry.getChromosomeEnd() > currentGenomicCoordinates.getStopLocation());
					if (hasLoopedThroughAllContaines || nextFeatureFound) {
						nextFeature = new GenomicRangedCoordinate(bedEntry.getContainerName(), bedEntry.getChromosomeStart(), bedEntry.getChromosomeEnd());
						break bedLoop;
					}
				}
				containerIndex = (containerIndex + 1) % sortedContainerNames.size();
				containersChecked++;
			}
		}

		return nextFeature;
	}

	public GenomicRangedCoordinate getGenomicCoordinatesOfPreviousFeature(GenomicRangedCoordinate currentGenomicCoordinates) {
		GenomicRangedCoordinate previousFeature = null;

		List<String> sortedContainerNames = getSortedChromsomeNames();

		if (sortedContainerNames.size() > 0) {
			int containerIndex = sortedContainerNames.indexOf(currentGenomicCoordinates.getContainerName());

			if (containerIndex == -1) {
				// find the first container passed this container
				int currentIndex = sortedContainerNames.size() - 1;
				while (containerIndex == -1 && currentIndex > 0) {
					String currentContainerName = sortedContainerNames.get(currentIndex);
					int comparisonResult = GenomicRangedCoordinate.CONTAINER_COMPARATOR.compare(currentGenomicCoordinates.getContainerName(), currentContainerName);
					if (comparisonResult > 0) {
						containerIndex = currentIndex;
					}
					currentIndex--;
				}

				if (containerIndex == -1) {
					containerIndex = sortedContainerNames.size() - 1;
				}
			}

			int containersChecked = 0;
			// note the starting container may get checked twice
			// if there is only one container and we are at the last entry we
			// have to start over in the same container
			while (previousFeature == null && containersChecked <= sortedContainerNames.size()) {
				String currentContainer = sortedContainerNames.get(containerIndex);
				List<IBedEntry> bedEntriesForContainer = getSortedBedEntries(currentContainer);

				bedLoop: for (int i = bedEntriesForContainer.size() - 1; i >= 0; i--) {
					IBedEntry bedEntry = bedEntriesForContainer.get(i);

					boolean hasLoopedThroughAllContaines = (containersChecked == sortedContainerNames.size());
					boolean nextFeatureFound = bedEntry.getChromosomeEnd() < currentGenomicCoordinates.getStopLocation()
							&& (bedEntry.getChromosomeStart() < currentGenomicCoordinates.getStartLocation());
					if (hasLoopedThroughAllContaines || nextFeatureFound) {
						previousFeature = new GenomicRangedCoordinate(bedEntry.getContainerName(), bedEntry.getChromosomeStart(), bedEntry.getChromosomeEnd());
						break bedLoop;
					}
				}
				containerIndex = (containerIndex - 1) % sortedContainerNames.size();
				containersChecked++;
			}
		}

		return previousFeature;
	}

	public int getVisibility() {
		int visibility = 1;
		if (nameValuePairs.containsKey(VISIBILITY_NAME)) {
			try {
				visibility = Integer.parseInt(nameValuePairs.get(VISIBILITY_NAME));
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		return visibility;
	}

	public List<GenomicRangedCoordinate> getContainers() {
		List<GenomicRangedCoordinate> containers = new ArrayList<GenomicRangedCoordinate>();

		for (Entry<String, Long> entry : largestCoordinateByContainer.entrySet()) {
			containers.add(new GenomicRangedCoordinate(entry.getKey(), 1, entry.getValue()));
		}

		return containers;
	}
}
