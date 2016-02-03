package com.roche.bioinformatics.common.verification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApproversSection {

	private final List<Approver> approvers;

	public ApproversSection() {
		approvers = new ArrayList<Approver>();
	}

	public void addApprover(Approver approver) {
		this.approvers.add(approver);
	}

	public List<Approver> getApprovers() {
		return Collections.unmodifiableList(approvers);
	}
}
