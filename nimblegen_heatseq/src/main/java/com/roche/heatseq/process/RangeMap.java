package com.roche.heatseq.process;

import java.util.List;

public interface RangeMap<O> {

	void put(int startInclusive, int stopInclusive, O object);

	List<O> getObjectsThatContainRangeInclusive(int startInclusive, int stopInclusive);

	List<O> getObjectsThatContainRangeInclusiveOld(int startInclusive, int stopInclusive);

}
