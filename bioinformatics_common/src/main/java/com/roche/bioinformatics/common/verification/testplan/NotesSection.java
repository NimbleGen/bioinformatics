package com.roche.bioinformatics.common.verification.testplan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotesSection {

	private final List<String> notes;

	public NotesSection() {
		this.notes = new ArrayList<String>();
	}

	public void addNote(String note) {
		this.notes.add(note);
	}

	public List<String> getNotes() {
		return Collections.unmodifiableList(notes);
	}

}
