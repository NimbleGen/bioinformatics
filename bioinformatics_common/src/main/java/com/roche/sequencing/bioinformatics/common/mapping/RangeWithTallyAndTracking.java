package com.roche.sequencing.bioinformatics.common.mapping;

import java.util.Collections;
import java.util.List;

public class RangeWithTallyAndTracking<V> {

	private final int start;
	private final int stop;
	private final List<V> trackedItems;

	public RangeWithTallyAndTracking(int start, int stop, List<V> trackedItems) {
		super();
		this.start = start;
		this.stop = stop;
		this.trackedItems = trackedItems;
	}

	public int getStart() {
		return start;
	}

	public int getStop() {
		return stop;
	}

	public int getCount() {
		return trackedItems.size();
	}

	public List<V> getTrackedItems() {
		return Collections.unmodifiableList(trackedItems);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + start;
		result = prime * result + stop;
		result = prime * result + ((trackedItems == null) ? 0 : trackedItems.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RangeWithTallyAndTracking<?> other = (RangeWithTallyAndTracking<?>) obj;
		if (start != other.start)
			return false;
		if (stop != other.stop)
			return false;
		if (trackedItems == null) {
			if (other.trackedItems != null)
				return false;
		} else if (!trackedItems.equals(other.trackedItems))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RangeWithTallyAndTracking [start=" + start + ", stop=" + stop + ", trackedItems=" + trackedItems + "]";
	}

}
