package com.roche.sequencing.bioinformatics.common.utils.bed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParsedBedFile {

	private final List<BedTrack> bedTracks;

	public ParsedBedFile() {
		super();
		this.bedTracks = new ArrayList<BedTrack>();
	}

	void addTrack(BedTrack bedTrack) {
		this.bedTracks.add(bedTrack);
	}

	public List<BedTrack> getBedTracks() {
		return Collections.unmodifiableList(bedTracks);
	}

}
