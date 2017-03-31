package com.roche.sequencing.bioinformatics.common.fastqtool;

import com.roche.sequencing.bioinformatics.common.sequence.ISequence;

public enum OrientationEnum {
	FORWARD("FWD"), REVERSE("RVS"), REVERSE_COMPLIMENT("RVS_CMP"), COMPLIMENT("CMP");

	private final String abbreviation;

	private OrientationEnum(String abbreviation) {
		this.abbreviation = abbreviation;
	}

	public String getAbbreviation() {
		return abbreviation;
	}

	public ISequence orientSequence(ISequence sequence) {
		ISequence orientedSequence = null;

		switch (this) {
		case COMPLIMENT:
			orientedSequence = sequence.getCompliment();
			break;
		case REVERSE_COMPLIMENT:
			orientedSequence = sequence.getReverseCompliment();
			break;
		case REVERSE:
			orientedSequence = sequence.getReverse();
			break;
		case FORWARD:
			orientedSequence = sequence;
			break;
		default:
			throw new AssertionError();
		}

		return orientedSequence;
	}

	public static OrientationEnum getOrientationByAbbreviation(String abbreviation) {
		OrientationEnum orientation = null;

		orientationLoop: for (OrientationEnum possibleOrientation : values()) {
			if (possibleOrientation.getAbbreviation().equals(abbreviation)) {
				orientation = possibleOrientation;
				break orientationLoop;
			}
		}

		return orientation;
	}

}
