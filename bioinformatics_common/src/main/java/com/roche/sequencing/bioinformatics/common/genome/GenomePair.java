package com.roche.sequencing.bioinformatics.common.genome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;

public class GenomePair implements IGenome {

	private final IGenome genomeOne;
	private final IGenome genomeTwo;

	public GenomePair(IGenome genomeOne, IGenome genomeTwo) {
		super();
		this.genomeOne = genomeOne;
		this.genomeTwo = genomeTwo;
	}

	@Override
	public GenomicRangedCoordinate getLargestContainer() {
		GenomicRangedCoordinate largestContainer = genomeOne.getLargestContainer();
		GenomicRangedCoordinate genomeTwoLargestContainer = genomeTwo.getLargestContainer();

		if (largestContainer == null || (genomeTwoLargestContainer != null && genomeTwoLargestContainer.getStopLocation() > largestContainer.getStopLocation())) {
			largestContainer = genomeTwoLargestContainer;
		}

		return largestContainer;
	}

	@Override
	public List<GenomicRangedCoordinate> getContainerSizes() {
		List<GenomicRangedCoordinate> containers = new ArrayList<GenomicRangedCoordinate>();

		for (String containerName : getContainerNames()) {
			containers.add(getContainer(containerName));
		}

		return containers;
	}

	@Override
	public GenomicRangedCoordinate getContainer(String containerName) {
		Long genomeOneContainerSize = genomeOne.getContainerSize(containerName);
		Long genomeTwoContainerSize = genomeTwo.getContainerSize(containerName);
		GenomicRangedCoordinate container = new GenomicRangedCoordinate(containerName, 1, Math.max(genomeOneContainerSize, genomeTwoContainerSize));
		return container;
	}

	@Override
	public ISequence getSequence(String containerName, long sequenceStart, long sequenceEnd) {
		ISequence sequence = genomeOne.getSequence(containerName, sequenceStart, sequenceEnd);
		if (sequence == null) {
			sequence = genomeTwo.getSequence(containerName, sequenceStart, sequenceEnd);
		}

		return sequence;
	}

	@Override
	public Set<String> getContainerNames() {
		Set<String> containerNames = new HashSet<String>();
		containerNames.addAll(genomeOne.getContainerNames());
		containerNames.addAll(genomeTwo.getContainerNames());
		return containerNames;
	}

	@Override
	public long getContainerSize(String containerName) {
		GenomicRangedCoordinate container = getContainer(containerName);
		return container.getStopLocation();
	}

	@Override
	public void close() {
		genomeOne.close();
		genomeTwo.close();
	}

	@Override
	public boolean containsSequences() {
		return genomeOne.containsSequences() || genomeTwo.containsSequences();
	}

}
