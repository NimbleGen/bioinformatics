package com.roche.sequencing.bioinformatics.common.genome;

import java.util.List;
import java.util.Set;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;

public interface IGenome {

	GenomicRangedCoordinate getLargestContainer();

	List<GenomicRangedCoordinate> getContainerSizes();

	GenomicRangedCoordinate getContainer(String containerName);

	ISequence getSequence(String containerName, long sequenceStart, long sequenceEnd);

	Set<String> getContainerNames();

	long getContainerSize(String containerName);

	void close();

	boolean containsSequences();

	ISequence getSequence(GenomicRangedCoordinate rangedCoordinate);

	ISequence getSequence(StrandedGenomicRangedCoordinate rangedCoordinate);

}
