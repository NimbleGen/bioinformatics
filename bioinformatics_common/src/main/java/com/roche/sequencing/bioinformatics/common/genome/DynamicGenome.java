package com.roche.sequencing.bioinformatics.common.genome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;

public class DynamicGenome implements IGenome {

	private final Map<String, Long> containerToSizeMap;
	private String largestContainerName;
	private Long largestContainerSize;

	public DynamicGenome() {
		containerToSizeMap = new HashMap<String, Long>();
	}

	public void addContainer(String containerName, long size) {
		Long existingSize = containerToSizeMap.get(containerName);
		if (existingSize == null || size > existingSize) {
			containerToSizeMap.put(containerName, size);
		}
		if (largestContainerSize == null || size > largestContainerSize) {
			largestContainerSize = size;
			largestContainerName = containerName;
		}
	}

	public void removeContainer(String containerName) {
		containerToSizeMap.remove(containerName);
	}

	@Override
	public GenomicRangedCoordinate getLargestContainer() {
		GenomicRangedCoordinate largestContainer = null;
		if (largestContainerName != null) {
			largestContainer = new GenomicRangedCoordinate(largestContainerName, 1, largestContainerSize);
		}
		return largestContainer;
	}

	@Override
	public List<GenomicRangedCoordinate> getContainerSizes() {
		List<GenomicRangedCoordinate> containerSizes = new ArrayList<GenomicRangedCoordinate>();

		for (Entry<String, Long> entry : containerToSizeMap.entrySet()) {
			containerSizes.add(new GenomicRangedCoordinate(entry.getKey(), 1, entry.getValue()));
		}

		return containerSizes;
	}

	@Override
	public GenomicRangedCoordinate getContainer(String containerName) {
		GenomicRangedCoordinate container = null;
		Long containerSize = containerToSizeMap.get(containerName);
		if (containerSize != null) {
			container = new GenomicRangedCoordinate(containerName, 1, containerSize);
		}
		return container;
	}

	@Override
	public ISequence getSequence(String containerName, long sequenceStart, long sequenceEnd) {
		return null;
	}

	@Override
	public Set<String> getContainerNames() {
		return containerToSizeMap.keySet();
	}

	@Override
	public long getContainerSize(String containerName) {
		Long size = containerToSizeMap.get(containerName);
		if (size == null) {
			size = 0L;
		}
		return size;
	}

	@Override
	public void close() {
	}

	@Override
	public boolean containsSequences() {
		return false;
	}

	@Override
	public ISequence getSequence(GenomicRangedCoordinate rangedCoordinate) {
		return getSequence(rangedCoordinate.getContainerName(), rangedCoordinate.getStartLocation(), rangedCoordinate.getStopLocation());
	}

	@Override
	public ISequence getSequence(StrandedGenomicRangedCoordinate rangedCoordinate) {
		ISequence sequence = getSequence(rangedCoordinate.getContainerName(), rangedCoordinate.getStartLocation(), rangedCoordinate.getStopLocation());
		if (rangedCoordinate.getStrand() == Strand.REVERSE) {
			sequence = sequence.getReverseCompliment();
		}
		return sequence;
	}

}
