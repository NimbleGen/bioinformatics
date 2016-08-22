package com.roche.sequencing.bioinformatics.common.utils.bed;

import java.util.List;

public interface IBedEntry {

	String getContainerName();

	int getChromosomeStart();

	int getChromosomeEnd();

	String getName();

	Integer getScore();

	Character getStrand();

	Integer getThickStart();

	Integer getThickEnd();

	RGB getItemRgb();

	int getBlockCount();

	List<Integer> getBlockSizes();

	List<Integer> getBlockStarts();

}
