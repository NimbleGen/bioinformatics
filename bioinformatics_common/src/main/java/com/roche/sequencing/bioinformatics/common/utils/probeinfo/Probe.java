/*
 *    Copyright 2016 Roche NimbleGen Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.roche.sequencing.bioinformatics.common.utils.probeinfo;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.NucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * 
 * Represents information about a probe
 * 
 */
public class Probe {
	private final String probeId;

	private final int extensionPrimerStart;
	private final int extensionPrimerStop;
	private final ISequence extensionPrimerSequence;

	private final int ligationPrimerStart;
	private final int ligationPrimerStop;
	private final ISequence ligationPrimerSequence;

	private final int captureTargetStart;
	private final int captureTargetStop;
	private final ISequence captureTargetSequence;

	private final Strand probeStrand;

	private final String annotation;

	private final String sequenceName;

	private Integer hashCode;

	public Probe(String probeId, String sequenceName, int extensionPrimerStart, int extensionPrimerStop, ISequence extensionPrimerSequence, int ligationPrimerStart, int ligationPrimerStop,
			ISequence ligationPrimerSequence, int captureTargetStart, int captureTargetStop, ISequence captureTargetSequence, Strand probeStrand, String annotation) {
		super();
		this.probeId = probeId;
		this.sequenceName = sequenceName;
		this.extensionPrimerStart = extensionPrimerStart;
		this.extensionPrimerStop = extensionPrimerStop;
		this.extensionPrimerSequence = extensionPrimerSequence;
		this.ligationPrimerStart = ligationPrimerStart;
		this.ligationPrimerStop = ligationPrimerStop;
		this.ligationPrimerSequence = ligationPrimerSequence;
		this.captureTargetStart = captureTargetStart;
		this.captureTargetStop = captureTargetStop;
		this.captureTargetSequence = captureTargetSequence;
		this.probeStrand = probeStrand;
		this.annotation = annotation;
	}

	public String getProbeId() {
		return this.probeId;
	}

	public int getStart() {
		return ArraysUtil.min(ligationPrimerStart, ligationPrimerStop, extensionPrimerStart, extensionPrimerStop);
	}

	public int getStop() {
		return ArraysUtil.max(ligationPrimerStart, ligationPrimerStop, extensionPrimerStart, extensionPrimerStop);
	}

	public ISequence getProbeSequence() {
		ISequence probeSequence = new NucleotideCodeSequence(extensionPrimerSequence);
		probeSequence.append(new NucleotideCodeSequence(captureTargetSequence));
		probeSequence.append(new NucleotideCodeSequence(ligationPrimerSequence));
		return probeSequence;
	}

	public int getExtensionPrimerStart() {
		return extensionPrimerStart;
	}

	public int getExtensionPrimerStop() {
		return extensionPrimerStop;
	}

	public ISequence getExtensionPrimerSequence() {
		return extensionPrimerSequence;
	}

	public int getLigationPrimerStart() {
		return ligationPrimerStart;
	}

	public int getLigationPrimerStop() {
		return ligationPrimerStop;
	}

	public ISequence getLigationPrimerSequence() {
		return ligationPrimerSequence;
	}

	public int getCaptureTargetStart() {
		return captureTargetStart;
	}

	public int getCaptureTargetStop() {
		return captureTargetStop;
	}

	public ISequence getCaptureTargetSequence() {
		return captureTargetSequence;
	}

	public Strand getProbeStrand() {
		return probeStrand;
	}

	public String getProbeAlignmentAsString() {
		StringBuilder alignmentAsString = new StringBuilder();

		int start = getIndexOfProbeStartInReference();
		int stop = getIndexOfProbeStopInReference();

		boolean isReverse = (start == getLigationPrimerStart());

		int totalLength = stop - start;

		String startLabel = null;
		String endLabel = null;

		String sequence = "" + getExtensionPrimerSequence() + getCaptureTargetSequence() + getLigationPrimerSequence();
		String primerMarkers = "e" + StringUtil.getSpacesAsString(getExtensionPrimerSequence().size() - 2) + "em" + StringUtil.getSpacesAsString(getCaptureTargetSequence().size() - 2) + "ml"
				+ StringUtil.getSpacesAsString(getLigationPrimerSequence().size() - 2) + "l";

		if (isReverse) {
			startLabel = "ligation primer start";
			endLabel = "extension primer stop";

			StringBuilder sequenceBuilder = new StringBuilder(sequence);

			sequence = sequenceBuilder.reverse().toString();

			StringBuilder markerBuilder = new StringBuilder(primerMarkers);

			primerMarkers = markerBuilder.reverse().toString();
		} else {
			startLabel = "extension primer start";
			endLabel = "ligation primer stop";
		}

		int numberOfSpaces = totalLength - (startLabel.length() + endLabel.length());
		String spaces = StringUtil.getSpacesAsString(numberOfSpaces);

		alignmentAsString.append(startLabel + spaces.toString() + endLabel + StringUtil.NEWLINE);

		String startCoordAsString = "" + start;
		String stopCoordAsString = "" + stop;

		numberOfSpaces = totalLength - (startCoordAsString.length() + stopCoordAsString.length());
		spaces = StringUtil.getSpacesAsString(numberOfSpaces);

		alignmentAsString.append(startCoordAsString + spaces + stopCoordAsString + StringUtil.NEWLINE);
		alignmentAsString.append(primerMarkers + StringUtil.NEWLINE);

		// print out last digit of locations in probe
		StringBuilder numbers = new StringBuilder();

		for (int i = getExtensionPrimerStart(); i <= getExtensionPrimerStop(); i++) {
			numbers.append(i % 10);
		}

		for (int i = getCaptureTargetStart(); i <= getCaptureTargetStop(); i++) {
			numbers.append(i % 10);
		}

		for (int i = getLigationPrimerStart(); i <= getLigationPrimerStop(); i++) {
			numbers.append(i % 10);
		}

		alignmentAsString.append(numbers.toString() + StringUtil.NEWLINE);

		// print out probe sequences
		alignmentAsString.append(sequence + StringUtil.NEWLINE);

		return alignmentAsString.toString();
	}

	public int getIndexOfProbeStartInReference() {
		int start = ArraysUtil.min(getExtensionPrimerStart(), getExtensionPrimerStop(), getLigationPrimerStart(), getLigationPrimerStop());

		return start;
	}

	public int getIndexOfProbeStopInReference() {
		int stop = ArraysUtil.max(getExtensionPrimerStart(), getExtensionPrimerStop(), getLigationPrimerStart(), getLigationPrimerStop());

		return stop;
	}

	public String getSequenceName() {
		return sequenceName;
	}

	public String getAnnotation() {
		return annotation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
		result = prime * result + ((captureTargetSequence == null) ? 0 : captureTargetSequence.hashCode());
		result = prime * result + captureTargetStart;
		result = prime * result + captureTargetStop;
		result = prime * result + ((extensionPrimerSequence == null) ? 0 : extensionPrimerSequence.hashCode());
		result = prime * result + extensionPrimerStart;
		result = prime * result + extensionPrimerStop;
		result = prime * result + ((hashCode == null) ? 0 : hashCode.hashCode());
		result = prime * result + ((ligationPrimerSequence == null) ? 0 : ligationPrimerSequence.hashCode());
		result = prime * result + ligationPrimerStart;
		result = prime * result + ligationPrimerStop;
		result = prime * result + ((probeId == null) ? 0 : probeId.hashCode());
		result = prime * result + ((probeStrand == null) ? 0 : probeStrand.hashCode());
		result = prime * result + ((sequenceName == null) ? 0 : sequenceName.hashCode());
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
		Probe other = (Probe) obj;
		if (annotation == null) {
			if (other.annotation != null)
				return false;
		} else if (!annotation.equals(other.annotation))
			return false;
		if (captureTargetSequence == null) {
			if (other.captureTargetSequence != null)
				return false;
		} else if (!captureTargetSequence.equals(other.captureTargetSequence))
			return false;
		if (captureTargetStart != other.captureTargetStart)
			return false;
		if (captureTargetStop != other.captureTargetStop)
			return false;
		if (extensionPrimerSequence == null) {
			if (other.extensionPrimerSequence != null)
				return false;
		} else if (!extensionPrimerSequence.equals(other.extensionPrimerSequence))
			return false;
		if (extensionPrimerStart != other.extensionPrimerStart)
			return false;
		if (extensionPrimerStop != other.extensionPrimerStop)
			return false;
		if (hashCode == null) {
			if (other.hashCode != null)
				return false;
		} else if (!hashCode.equals(other.hashCode))
			return false;
		if (ligationPrimerSequence == null) {
			if (other.ligationPrimerSequence != null)
				return false;
		} else if (!ligationPrimerSequence.equals(other.ligationPrimerSequence))
			return false;
		if (ligationPrimerStart != other.ligationPrimerStart)
			return false;
		if (ligationPrimerStop != other.ligationPrimerStop)
			return false;
		if (probeId == null) {
			if (other.probeId != null)
				return false;
		} else if (!probeId.equals(other.probeId))
			return false;
		if (probeStrand != other.probeStrand)
			return false;
		if (sequenceName == null) {
			if (other.sequenceName != null)
				return false;
		} else if (!sequenceName.equals(other.sequenceName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Probe [probeId=" + probeId + ", extensionPrimerStart=" + extensionPrimerStart + ", extensionPrimerStop=" + extensionPrimerStop + ", extensionPrimerSequence=" + extensionPrimerSequence
				+ ", ligationPrimerStart=" + ligationPrimerStart + ", ligationPrimerStop=" + ligationPrimerStop + ", ligationPrimerSequence=" + ligationPrimerSequence + ", captureTargetStart="
				+ captureTargetStart + ", captureTargetStop=" + captureTargetStop + ", captureTargetSequence=" + captureTargetSequence + ", probeStrand=" + probeStrand + ", annotation=" + annotation
				+ ", sequenceName=" + sequenceName + ", hashCode=" + hashCode + "]";
	}

}
