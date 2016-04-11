package com.roche.sequencing.bioinformatics.common.utils.bed;

import java.util.List;

public interface IBedEntry {

	String getChromosomeName();

	int getChromosomeStart();

	int getChromosomeEnd();

	String getName();

	Integer getScore();

	Character getStrand();

	Integer getThickStart();

	Integer getThickEnd();

	RGB getItemRgb();

	Integer getBlockCount();

	List<Integer> getBlockSizes();

	List<Integer> getBlockStarts();

}
