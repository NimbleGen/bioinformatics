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

package com.roche.heatseq.process;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.ExtendReadResults;
import com.roche.heatseq.objects.IReadPair;
import com.roche.heatseq.objects.ReadPair;
import com.roche.heatseq.utils.SAMRecordUtil;
import com.roche.sequencing.bioinformatics.common.alignment.AlignmentPair;
import com.roche.sequencing.bioinformatics.common.alignment.CigarString;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
import com.roche.sequencing.bioinformatics.common.alignment.SimpleAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.sequence.ICode;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCode;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.Probe;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;

/**
 * Extends reads for a probe to the primer locations
 * 
 */
final class ExtendReadsToPrimer {
	private static final Logger logger = LoggerFactory.getLogger(ExtendReadsToPrimer.class);

	private static final int PRIMER_ALIGNMENT_BUFFER = 25;
	// The location of the end of the primer must be at least this many bases from the end of the read
	private static final int LENGTH_THRESHOLD_FOR_EXTENDED_READ = 10;
	private static final double LENGTH_NORMALIZED_ALIGNMENT_SCORE_THRESHOLD = 0;
	private static final double WEIGHTED_ALIGNMENT_SCORE_THRESHOLD = 4.0;

	/**
	 * We just use static methods from this class
	 */
	private ExtendReadsToPrimer() {
		throw new AssertionError();
	}

	/**
	 * Extends reads for a probe to the primer locations
	 * 
	 * @param readPair
	 * @return UniqueProbeRepresentativeData with extended reads or null if it could not be extended
	 */
	private static IReadPair extendReadPair(Probe probe, IReadPair readPair, IAlignmentScorer alignmentScorer) {
		return extendReadPair(readPair.isMarkedDuplicate(), readPair.getExtensionUid(), readPair.getLigationUid(), probe, readPair.getSamHeader(), readPair.getSequenceName(), readPair.getReadName(),
				readPair.getReadGroup(), new IupacNucleotideCodeSequence(readPair.getSequenceOne()), readPair.getSequenceOneQualityString(), new IupacNucleotideCodeSequence(readPair.getSequenceTwo()),
				readPair.getSequenceTwoQualityString(), readPair.getOneMappingQuality(), readPair.getTwoMappingQuality(), readPair.isBestPairDuplicateGroup(), alignmentScorer);
	}

	/**
	 * 
	 * @param uid
	 * @param probe
	 * @param samHeader
	 * @param sequenceName
	 * @param readName
	 * @param readGroup
	 * @param sequenceOne
	 * @param sequenceOneQualityString
	 * @param sequenceTwo
	 * @param sequenceTwoQualityString
	 * @param oneMappingQuality
	 * @param twoMappingQuality
	 * @return readPair that has been extended to the primers (the primers are not included in the new alignment)
	 */
	private static IReadPair extendReadPair(boolean isMarkedDuplicate, String extensionUid, String ligationUid, Probe probe, SAMFileHeader samHeader, String sequenceName, String readName,
			String readGroup, ISequence sequenceOne, String sequenceOneQualityString, ISequence sequenceTwo, String sequenceTwoQualityString, int oneMappingQuality, int twoMappingQuality,
			boolean isBestDuplicate, IAlignmentScorer alignmentScorer) {
		IReadPair extendedReadPair = null;
		boolean readOneExtended = false;
		boolean readTwoExtended = false;

		try {
			sequenceTwo = sequenceTwo.getCompliment();

			boolean readOneIsOnReverseStrand = (probe.getProbeStrand() == Strand.REVERSE);
			boolean readTwoIsOnReverseStrand = !readOneIsOnReverseStrand;

			int primerReferencePositionAdjacentToSequence = probe.getExtensionPrimerStop();
			if (readOneIsOnReverseStrand) {
				primerReferencePositionAdjacentToSequence = probe.getExtensionPrimerStart();
			}

			ISequence captureTargetSequence;
			ISequence extensionPrimer;
			ISequence sequenceOneCompliment;

			if (readOneIsOnReverseStrand) {
				captureTargetSequence = probe.getCaptureTargetSequence().getCompliment();
				extensionPrimer = probe.getExtensionPrimerSequence().getCompliment();
				sequenceOneCompliment = sequenceOne.getCompliment();
			} else {
				captureTargetSequence = probe.getCaptureTargetSequence();
				extensionPrimer = probe.getExtensionPrimerSequence();
				sequenceOneCompliment = sequenceOne;
			}

			ReadExtensionDetails readOneExtensionDetails = calculateDetailsForReadExtensionToPrimer(extensionPrimer, primerReferencePositionAdjacentToSequence, captureTargetSequence,
					sequenceOneCompliment, readOneIsOnReverseStrand, alignmentScorer);

			SAMRecord readOneRecord = null;

			if (readOneExtensionDetails != null) {
				ISequence readOneExtendedSequence = sequenceOne.subSequence(readOneExtensionDetails.getReadStart(), readOneExtensionDetails.getReadStop());
				String readOneExtendedBaseQualities = sequenceOneQualityString.substring(readOneExtensionDetails.getReadStart(), readOneExtensionDetails.getReadStop() + 1).toString();
				int readOneReferenceLength = probe.getCaptureTargetSequence().size();
				primerReferencePositionAdjacentToSequence = probe.getLigationPrimerStart();
				CigarString cigarString = readOneExtensionDetails.getAlignmentCigarString();
				if (readOneIsOnReverseStrand) {
					readOneExtendedSequence = readOneExtendedSequence.getReverseCompliment();
					readOneExtendedBaseQualities = StringUtil.reverse(readOneExtendedBaseQualities);
					readOneReferenceLength = -readOneReferenceLength;
					primerReferencePositionAdjacentToSequence = probe.getLigationPrimerStop();
				}

				readOneExtended = true;
				readOneRecord = createRecord(samHeader, readName, readGroup, readOneIsOnReverseStrand, cigarString, readOneExtensionDetails.getMismatchDetailsString(),
						readOneExtensionDetails.getEditDistance(), readOneExtensionDetails.getAlignmentStartInReference(), readOneExtendedSequence.toString(), readOneExtendedBaseQualities,
						sequenceName, oneMappingQuality, readOneReferenceLength, isMarkedDuplicate, extensionUid, ligationUid, probe.getProbeId(), isBestDuplicate);
			}

			ISequence ligationPrimer = probe.getLigationPrimerSequence().getReverseCompliment();
			ISequence sequenceTwoCompliment = sequenceTwo.getCompliment();
			if (readOneIsOnReverseStrand) {
				ligationPrimer = probe.getLigationPrimerSequence().getReverseCompliment();
				sequenceTwoCompliment = sequenceTwo.getCompliment();
				captureTargetSequence = probe.getCaptureTargetSequence().getReverseCompliment();
			} else {
				ligationPrimer = probe.getLigationPrimerSequence().getReverse();
				sequenceTwoCompliment = sequenceTwo;
				captureTargetSequence = probe.getCaptureTargetSequence().getReverse();
			}

			ReadExtensionDetails readTwoExtensionDetails = calculateDetailsForReadExtensionToPrimer(ligationPrimer, primerReferencePositionAdjacentToSequence, captureTargetSequence,
					sequenceTwoCompliment, readTwoIsOnReverseStrand, alignmentScorer);

			SAMRecord readTwoRecord = null;

			if (readTwoExtensionDetails != null) {
				ISequence readTwoExtendedSequence = sequenceTwo.subSequence(readTwoExtensionDetails.getReadStart(), readTwoExtensionDetails.getReadStop()).getCompliment();
				String readTwoExtendedBaseQualities = sequenceTwoQualityString.substring(readTwoExtensionDetails.getReadStart(), readTwoExtensionDetails.getReadStop() + 1);
				int readTwoReferenceLength = probe.getCaptureTargetSequence().size();
				CigarString cigarString = readTwoExtensionDetails.getAlignmentCigarString();
				if (readTwoIsOnReverseStrand) {
					readTwoExtendedSequence = readTwoExtendedSequence.getReverseCompliment();
					readTwoExtendedBaseQualities = StringUtil.reverse(readTwoExtendedBaseQualities);
					readTwoReferenceLength = -readTwoReferenceLength;
				}
				readTwoExtended = true;
				readTwoRecord = createRecord(samHeader, readName, readGroup, readTwoIsOnReverseStrand, cigarString, readTwoExtensionDetails.getMismatchDetailsString(),
						readTwoExtensionDetails.getEditDistance(), readTwoExtensionDetails.getAlignmentStartInReference(), readTwoExtendedSequence.toString(), readTwoExtendedBaseQualities,
						sequenceName, twoMappingQuality, readTwoReferenceLength, isMarkedDuplicate, extensionUid, ligationUid, probe.getProbeId(), isBestDuplicate);
			}

			if (readOneRecord != null && readTwoRecord != null) {
				SAMRecordUtil.setSAMRecordsAsPair(readOneRecord, readTwoRecord);
			}

			extendedReadPair = new ReadPair(readOneRecord, readTwoRecord, extensionUid, ligationUid, probe.getCaptureTargetSequence(), probe.getProbeId(), readOneExtended, readTwoExtended);

		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw new IllegalStateException(e.getMessage(), e);
		}

		return extendedReadPair;
	}

	private static ReadExtensionDetails calculateDetailsForReadExtensionToPrimer(ISequence primerSequence, int primerReferencePositionAdjacentToSequence, ISequence captureSequence,
			ISequence readSequence, boolean isReversed, IAlignmentScorer alignmentScorer) {
		ReadExtensionDetails readExtensionDetails = null;

		Integer primerEndIndexInRead = getPrimerEndIndexInRead(primerSequence, readSequence);

		boolean primerAlignedsuccessfully = (primerEndIndexInRead != null) && (primerEndIndexInRead >= 0) && (primerEndIndexInRead < readSequence.size());

		if (primerAlignedsuccessfully) {
			int captureTargetStartIndexInRead = primerEndIndexInRead + 1;
			ISequence readWithoutPrimer = readSequence.subSequence(captureTargetStartIndexInRead, readSequence.size());
			NeedlemanWunschGlobalAlignment readAlignmentWithReference = new NeedlemanWunschGlobalAlignment(captureSequence, readWithoutPrimer, alignmentScorer);
			boolean readAlignedsuccessfully = (readAlignmentWithReference.getLengthNormalizedAlignmentScore() > LENGTH_NORMALIZED_ALIGNMENT_SCORE_THRESHOLD);

			if (readAlignedsuccessfully) {
				AlignmentPair alignment = readAlignmentWithReference.getAlignmentPair();

				// get rid of the end insertion bases added when trying to match the entire capture target
				alignment = alignment.getAlignmentWithoutEndingQueryInserts();

				CigarString cigarString = alignment.getCigarString();
				String mismatchDetailsString = alignment.getMismatchDetailsString();
				int editDistance = alignment.getEditDistance();
				int alignmentStartInReference = 0;

				int readLength = readWithoutPrimer.size() - 1;

				if (readLength > LENGTH_THRESHOLD_FOR_EXTENDED_READ) {
					if (isReversed) {
						cigarString = alignment.getReverseCigarString();
						mismatchDetailsString = alignment.getReverseMismatchDetailsString();

						int referenceLength = cigarString.getLengthOfReference();

						alignmentStartInReference = primerReferencePositionAdjacentToSequence - referenceLength;
					} else {
						alignmentStartInReference = primerReferencePositionAdjacentToSequence + 1;
					}

					readExtensionDetails = new ReadExtensionDetails(alignmentStartInReference, captureTargetStartIndexInRead, cigarString, mismatchDetailsString, editDistance, readLength);
				}
			}
		}

		return readExtensionDetails;
	}

	private static SAMRecord createRecord(SAMFileHeader samHeader, String readName, String readGroup, boolean isNegativeStrand, CigarString cigarString, String mismatchDetailsString, int editDistance,
			int alignmentStartInReference, String readString, String baseQualityString, String sequenceName, int mappingQuality, int referenceLength, boolean isMarkedDuplicate, String extensionUid,
			String ligationUid, String probeId, boolean isBestDuplicate) {
		if (readString.length() != baseQualityString.length()) {
			throw new IllegalStateException(
					"SAMRecord read[" + readString + "] length[" + readString.length() + "] and base quality[" + baseQualityString + "] length[" + baseQualityString.length() + "] must be the same.");
		}

		SAMRecord record = new SAMRecord(samHeader);
		record.setMateNegativeStrandFlag(!isNegativeStrand);
		record.setReadNegativeStrandFlag(isNegativeStrand);
		record.setMappingQuality(mappingQuality);
		record.setReadName(readName);
		record.setReferenceName(sequenceName);
		record.setInferredInsertSize(referenceLength);
		record.setCigarString(cigarString.getStandardCigarString());
		record.setAlignmentStart(alignmentStartInReference);
		record.setReadString(readString);
		record.setBaseQualityString(baseQualityString);
		record.setDuplicateReadFlag(isMarkedDuplicate);
		if (!extensionUid.isEmpty()) {
			SAMRecordUtil.setSamRecordExtensionUidAttribute(record, extensionUid);
		}
		if (!ligationUid.isEmpty()) {
			SAMRecordUtil.setSamRecordLigationUidAttribute(record, ligationUid);
		}
		if (!probeId.isEmpty()) {
			SAMRecordUtil.setSamRecordProbeIdAttribute(record, probeId);
		}
		if (mismatchDetailsString != null && !mismatchDetailsString.isEmpty()) {
			record.setAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG, mismatchDetailsString);
		}

		record.setAttribute(SAMRecordUtil.EDIT_DISTANCE_ATTRIBUTE_TAG, editDistance);
		record.setAttribute(SAMRecordUtil.READ_GROUP_ATTRIBUTE_TAG, readGroup);
		return record;
	}

	private static Integer getPrimerEndIndexInRead(ISequence primerSequence, ISequence readSequence) {
		// cutoff excess sequence beyond primer
		readSequence = readSequence.subSequence(0, Math.min(readSequence.size() - 1, primerSequence.size() + PRIMER_ALIGNMENT_BUFFER));

		IAlignmentScorer scorer = new SimpleAlignmentScorer(SimpleAlignmentScorer.DEFAULT_MATCH_SCORE, SimpleAlignmentScorer.DEFAULT_MISMATCH_PENALTY, SimpleAlignmentScorer.DEFAULT_GAP_EXTEND_PENALTY,
				SimpleAlignmentScorer.DEFAULT_GAP_OPEN_PENALTY, true, false);

		NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(readSequence, primerSequence, scorer);
		ISequence readAlignment = alignment.getAlignmentPair().getReferenceAlignment();
		ISequence primerAlignment = alignment.getAlignmentPair().getQueryAlignment();

		ISequence primer = alignment.getAlignmentPair().getQueryAlignment();
		int primerIndex = 0;
		while (primer.getCodeAt(primerIndex).equals(IupacNucleotideCode.GAP)) {
			primerIndex++;
		}
		Integer primerEndIndexInRead = null;

		// walk backwards until we stop seeing gaps in the reference (aka read)
		ISequence reverseReadAlignment = readAlignment.getReverse();
		ISequence reversePrimerAlignment = primerAlignment.getReverse();
		boolean passedTrailingPrimerGaps = false;
		int i = 0;
		primerEndIndexInRead = 0;

		double weightedAlignmentScore = 0;
		int firstNonGapIndex = -1;

		while (i < reverseReadAlignment.size()) {
			ICode currentReadCode = reverseReadAlignment.getCodeAt(i);
			ICode currentPrimerCode = reversePrimerAlignment.getCodeAt(i);
			if (!passedTrailingPrimerGaps && !currentPrimerCode.matches(IupacNucleotideCode.GAP)) {
				passedTrailingPrimerGaps = true;
				firstNonGapIndex = i;
			}

			// start counting all bases in read when the initial primer gaps have been passed
			if (passedTrailingPrimerGaps && !currentReadCode.matches(IupacNucleotideCode.GAP)) {
				primerEndIndexInRead++;
				if (currentReadCode.matches(currentPrimerCode)) {
					double distanceFromFirstNonGap = i - firstNonGapIndex;
					double weightedScore = Math.pow(0.95, distanceFromFirstNonGap);
					weightedAlignmentScore += weightedScore;
				}
			}

			i++;
		}

		if (weightedAlignmentScore < WEIGHTED_ALIGNMENT_SCORE_THRESHOLD) {
			primerEndIndexInRead = null;
		}

		if (primerEndIndexInRead != null) {
			// indexes are zero based and counts are one based so subtract one
			primerEndIndexInRead -= 1;
		}

		return primerEndIndexInRead;
	}

	static ExtendReadResults extendReadsToPrimers(Probe probe, List<IReadPair> readPairs, IAlignmentScorer alignmentScorer) {
		List<IReadPair> extendedReadPairs = new ArrayList<IReadPair>();
		List<IReadPair> unableToExtendReadPairs = new ArrayList<IReadPair>();

		for (IReadPair readPair : readPairs) {
			IReadPair extendedReadPair = ExtendReadsToPrimer.extendReadPair(probe, readPair, alignmentScorer);

			if (extendedReadPair.isReadOneExtended() && extendedReadPair.isReadTwoExtended()) {
				extendedReadPairs.add(extendedReadPair);
			} else {
				unableToExtendReadPairs.add(readPair);
			}
		}

		return new ExtendReadResults(extendedReadPairs, unableToExtendReadPairs);
	}

	/**
	 * 
	 * The details required to extend a read
	 * 
	 */
	private static class ReadExtensionDetails {
		private final int alignmentStartInReference;
		private final int readStart;
		private final int editDistance;
		private final int readLength;
		private final CigarString alignmentCigarString;
		private final String captureTargetMismatchDetailsString;

		public ReadExtensionDetails(int alignmentStartInReference, int readStart, CigarString captureTargetAlignmentCigarString, String captureTargetMismatchDetailsString, int editDistance,
				int readLength) {
			super();
			this.alignmentStartInReference = alignmentStartInReference;
			this.readStart = readStart;
			this.alignmentCigarString = captureTargetAlignmentCigarString;
			this.captureTargetMismatchDetailsString = captureTargetMismatchDetailsString;
			this.editDistance = editDistance;
			this.readLength = readLength;
		}

		public int getReadStop() {
			return readStart + readLength;
		}

		public int getAlignmentStartInReference() {
			return alignmentStartInReference;
		}

		public int getReadStart() {
			return readStart;
		}

		public CigarString getAlignmentCigarString() {
			return alignmentCigarString;
		}

		public String getMismatchDetailsString() {
			return captureTargetMismatchDetailsString;
		}

		public int getEditDistance() {
			return editDistance;
		}
	}
}
