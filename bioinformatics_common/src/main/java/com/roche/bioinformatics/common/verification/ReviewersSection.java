package com.roche.bioinformatics.common.verification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReviewersSection {

	private final List<Reviewer> reviewers;

	public ReviewersSection() {
		this.reviewers = new ArrayList<Reviewer>();
	}

	public void addReviewer(Reviewer reviewer) {
		this.reviewers.add(reviewer);
	}

	public List<Reviewer> getReviewers() {
		return Collections.unmodifiableList(reviewers);
	}
}
