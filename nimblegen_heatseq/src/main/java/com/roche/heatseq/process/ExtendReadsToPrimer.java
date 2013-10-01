/**
 *   Copyright 2013 Roche NimbleGen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.roche.heatseq.process;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.IReadPair;
import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.objects.ReadPair;
import com.roche.mapping.SAMRecordUtil;
import com.roche.sequencing.bioinformatics.common.alignment.CigarString;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
import com.roche.sequencing.bioinformatics.common.sequence.ICode;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCode;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * Extends reads for a probe to the primer locations
 * 
 */
public final class ExtendReadsToPrimer {
	private static final Logger logger = LoggerFactory.getLogger(ExtendReadsToPrimer.class);

	private static final int PRIMER_ALIGNMENT_BUFFER = 10;
	private static final int PRIMER_ACCEPTANCE_BUFFER = 2;

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
		return extendReadPair(readPair.getUid(), probe, readPair.getSamHeader(), readPair.getSequenceName(), readPair.getReadName(), readPair.getReadGroup(),
				new IupacNucleotideCodeSequence(readPair.getSequenceOne()), readPair.getSequenceOneQualityString(), new IupacNucleotideCodeSequence(readPair.getSequenceTwo()),
				readPair.getSequenceTwoQualityString(), readPair.getOneMappingQuality(), readPair.getTwoMappingQuality(), alignmentScorer);
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
	public static IReadPair extendReadPair(String uid, Probe probe, SAMFileHeader samHeader, String sequenceName, String readName, String readGroup, ISequence sequenceOne,
			String sequenceOneQualityString, ISequence sequenceTwo, String sequenceTwoQualityString, int oneMappingQuality, int twoMappingQuality, IAlignmentScorer alignmentScorer) {
		IReadPair extendedReadPair = null;

		try {
			sequenceTwo = sequenceTwo.getCompliment();

			ISequence captureTargetSequence = probe.getCaptureTargetSequence();
			ISequence extensionPrimer = probe.getExtensionPrimerSequence();
			ISequence ligationPrimer = probe.getLigationPrimerSequence();
			boolean readOneIsOnReverseStrand = probe.getProbeStrand() == Strand.REVERSE;
			boolean readTwoIsOnReverseStrand = !readOneIsOnReverseStrand;

			int primerReferencePositionAdjacentToSequence = probe.getExtensionPrimerStop();
			if (readOneIsOnReverseStrand) {
				primerReferencePositionAdjacentToSequence = probe.getExtensionPrimerStart();
			}
			ReadExtensionDetails readOneExtensionDetails = calculateDetailsForReadExtensionToPrimer(extensionPrimer, primerReferencePositionAdjacentToSequence, captureTargetSequence, sequenceOne,
					false, readOneIsOnReverseStrand, alignmentScorer);

			if (readOneExtensionDetails != null) {
				ISequence readOneExtendedSequence = sequenceOne.subSequence(readOneExtensionDetails.getReadStart(), sequenceOne.size());
				String readOneExtendedBaseQualities = sequenceOneQualityString.substring(readOneExtensionDetails.getReadStart(), sequenceOneQualityString.length()).toString();
				int readOneReferenceLength = probe.getCaptureTargetSequence().size();
				primerReferencePositionAdjacentToSequence = probe.getLigationPrimerStart();
				if (readOneIsOnReverseStrand) {
					readOneExtendedSequence = readOneExtendedSequence.getReverseCompliment();
					readOneExtendedBaseQualities = StringUtil.reverse(readOneExtendedBaseQualities);
					readOneReferenceLength = -readOneReferenceLength;
					primerReferencePositionAdjacentToSequence = probe.getLigationPrimerStop();
				}

				SAMRecord readOneExtendedRecord = extendRecord(samHeader, readName, readGroup, readOneIsOnReverseStrand, readOneExtensionDetails.getAlignmentCigarString(),
						readOneExtensionDetails.getMismatchDetailsString(), readOneExtensionDetails.getAlignmentStartInReference(), readOneExtendedSequence.toString(), readOneExtendedBaseQualities,
						sequenceName, oneMappingQuality, probe.getCaptureTargetSequence().size(), uid, probe.getProbeId());

				ReadExtensionDetails readTwoExtensionDetails = calculateDetailsForReadExtensionToPrimer(ligationPrimer, primerReferencePositionAdjacentToSequence, captureTargetSequence, sequenceTwo,
						true, readTwoIsOnReverseStrand, alignmentScorer);

				if (readTwoExtensionDetails != null) {
					String readTwoExtendedBaseQualities = sequenceTwoQualityString.substring(readTwoExtensionDetails.getReadStart(), sequenceTwoQualityString.length());
					ISequence readTwoExtendedSequence = sequenceTwo.subSequence(readTwoExtensionDetails.getReadStart(), sequenceTwo.size()).getCompliment();
					int readTwoReferenceLength = probe.getCaptureTargetSequence().size();
					if (readTwoIsOnReverseStrand) {
						readTwoExtendedSequence = readTwoExtendedSequence.getReverseCompliment();
						readTwoExtendedBaseQualities = StringUtil.reverse(readTwoExtendedBaseQualities);
						readTwoReferenceLength = -readTwoReferenceLength;
					}
					SAMRecord readTwoExtendedRecord = extendRecord(samHeader, readName, readGroup, readTwoIsOnReverseStrand, readTwoExtensionDetails.getAlignmentCigarString(),
							readTwoExtensionDetails.getMismatchDetailsString(), readTwoExtensionDetails.getAlignmentStartInReference(), readTwoExtendedSequence.toString(),
							readTwoExtendedBaseQualities, sequenceName, twoMappingQuality, readTwoReferenceLength, uid, probe.getProbeId());

					SAMRecordUtil.setSAMRecordsAsPair(readOneExtendedRecord, readTwoExtendedRecord);

					extendedReadPair = new ReadPair(readOneExtendedRecord, readTwoExtendedRecord, uid);

				}
			}
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}

		return extendedReadPair;
	}

	private static ReadExtensionDetails calculateDetailsForReadExtensionToPrimer(ISequence primerSequence, int primerReferencePositionAdjacentToSequence, ISequence captureSequence,
			ISequence readSequence, boolean isLigationPrimer, boolean isReversed, IAlignmentScorer alignmentScorer) {
		ReadExtensionDetails readExtensionDetails = null;

		if (isLigationPrimer) {
			primerSequence = primerSequence.getReverse();
			captureSequence = captureSequence.getReverse();
		}

		Integer primerEndIndexInRead = getPrimerEndIndexInRead(primerSequence, readSequence, alignmentScorer);
		boolean primerAlignedSuccesfully = (primerEndIndexInRead != null) && (primerEndIndexInRead >= 0) && (primerEndIndexInRead < readSequence.size());

		if (primerAlignedSuccesfully) {
			int captureTargetStartIndexInRead = primerEndIndexInRead + 1;
			ISequence readWithoutPrimer = readSequence.subSequence(captureTargetStartIndexInRead, readSequence.size());
			NeedlemanWunschGlobalAlignment readAlignmentWithReference = new NeedlemanWunschGlobalAlignment(captureSequence, readWithoutPrimer, alignmentScorer);
			boolean readAlignedSuccesfully = readAlignmentWithReference.getLengthNormalizedAlignmentScore() > 0;

			if (readAlignedSuccesfully) {
				CigarString cigarString = readAlignmentWithReference.getCigarString();
				String mismatchDetailsString = readAlignmentWithReference.getMismatchDetailsString();
				int alignmentStartInReference = 0;

				ISequence referenceAlignment = readAlignmentWithReference.getAlignmentPair().getReferenceAlignmentWithoutEndingInserts();
				if (isReversed) {
					cigarString = readAlignmentWithReference.getReverseCigarString();
					mismatchDetailsString = readAlignmentWithReference.getReverseMismatchDetailsString();
					alignmentStartInReference = primerReferencePositionAdjacentToSequence - referenceAlignment.size();
				} else {
					int offset = readAlignmentWithReference.getAlignmentPair().getFirstNonInsertMatchInReference();
					alignmentStartInReference = primerReferencePositionAdjacentToSequence + offset + 1;
				}
				readExtensionDetails = new ReadExtensionDetails(alignmentStartInReference, captureTargetStartIndexInRead, cigarString, mismatchDetailsString);
			}
		}

		return readExtensionDetails;
	}

	private static SAMRecord extendRecord(SAMFileHeader samHeader, String readName, String readGroup, boolean isNegativeStrand, CigarString cigarString, String mdString,
			int alignmentStartInReference, String readString, String baseQualityString, String sequenceName, int mappingQuality, int referenceLength, String uid, String probeId) {
		if (readString.length() != baseQualityString.length()) {
			throw new IllegalStateException("SAMRecord read[" + readString + "] length[" + readString.length() + "] and base quality[" + baseQualityString + "] length[" + baseQualityString.length()
					+ "] must be the same.");
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
		SAMRecordUtil.setSamRecordUidAttribute(record, uid);
		SAMRecordUtil.setSamRecordProbeIdAttribute(record, probeId);
		if (mdString != null && !mdString.isEmpty()) {
			record.setAttribute(SAMRecordUtil.MISMATCH_DETAILS_ATTRIBUTE_TAG, mdString);
		}
		record.setAttribute(SAMRecordUtil.READ_GROUP_ATTRIBUTE_TAG, readGroup);
		record.setAttribute(SAMRecordUtil.EDIT_DISTANCE_ATTRIBUTE_TAG, cigarString.getEditDistance());
		return record;
	}

	static Integer getPrimerEndIndexInRead(ISequence primerSequence, ISequence readSequence, IAlignmentScorer alignmentScorer) {
		// cutoff excess sequence beyond primer
		readSequence = readSequence.subSequence(0, Math.min(readSequence.size() - 1, primerSequence.size() + PRIMER_ALIGNMENT_BUFFER));
		NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(primerSequence, readSequence, alignmentScorer);

		ISequence readAlignment = alignment.getAlignmentPair().getReferenceAlignment();

		int firstNonGapIndex = -1;

		double lengthNormalizedAlignmentScore = alignment.getLengthNormalizedAlignmentScore();

		if (lengthNormalizedAlignmentScore > 0) {
			int currentIndexFromEnd = 0;

			// walk backwards until we stop seeing gaps
			alignmentLoop: for (ICode code : readAlignment.getReverse()) {
				if (!code.matches(IupacNucleotideCode.GAP)) {
					firstNonGapIndex = readAlignment.size() - currentIndexFromEnd;

					break alignmentLoop;
				}

				currentIndexFromEnd++;
			}
		}

		Integer probeEndIndexInRead = null;
		if (firstNonGapIndex >= primerSequence.size() - PRIMER_ACCEPTANCE_BUFFER && firstNonGapIndex <= primerSequence.size() + PRIMER_ACCEPTANCE_BUFFER) {
			probeEndIndexInRead = firstNonGapIndex - 1;
		}

		return probeEndIndexInRead;
	}

	static List<IReadPair> extendReadsToPrimers(Probe probe, List<IReadPair> readPairs, PrintWriter extensionErrorsWriter, IAlignmentScorer alignmentScorer) {
		List<IReadPair> extendedReadPairs = new ArrayList<IReadPair>();

		for (IReadPair readPair : readPairs) {
			IReadPair extendedReadPair = ExtendReadsToPrimer.extendReadPair(probe, readPair, alignmentScorer);

			if (extendedReadPair != null) {
				extendedReadPairs.add(extendedReadPair);
			} else {
				if (extensionErrorsWriter != null) {
					synchronized (extensionErrorsWriter) {
						extensionErrorsWriter.println();
						extensionErrorsWriter.println("--------------------");
						extensionErrorsWriter.println("PROBE " + probe.getProbeId() + ": " + probe.getSequenceName() + ":" + probe.getCaptureTargetStart() + " to " + probe.getCaptureTargetStop());
						extensionErrorsWriter.println();
						extensionErrorsWriter.println("fastqOne Sequence: " + readPair.getSequenceOne());
						extensionErrorsWriter.println(StringUtil.TAB + "mapping in reference start: " + readPair.getRecordAlignmentStart());
						extensionErrorsWriter.println(StringUtil.TAB + "mapping in reference stop: " + readPair.getRecordAlignmentEnd());
						extensionErrorsWriter.println("fastqTwo Sequence: " + readPair.getSequenceTwo());
						extensionErrorsWriter.println(StringUtil.TAB + "mapping in reference start: " + readPair.getMateAlignmentStart());
						extensionErrorsWriter.println(StringUtil.TAB + "mapping in reference stop: " + readPair.getMateAlignmentEnd());
						extensionErrorsWriter.println();
						extensionErrorsWriter.println("--------------------");
						extensionErrorsWriter.println();
					}
				}
			}
		}

		return extendedReadPairs;
	}

	/**
	 * 
	 * The details required to extend a read
	 * 
	 */
	private static class ReadExtensionDetails {
		private final int alignmentStartInReference;
		private final int readStart;
		private final CigarString alignmentCigarString;
		private final String mismatchDetailsString;

		public ReadExtensionDetails(int alignmentStartInReference, int readStart, CigarString alignmentCigarString, String mdString) {
			super();
			this.alignmentStartInReference = alignmentStartInReference;
			this.readStart = readStart;
			this.alignmentCigarString = alignmentCigarString;
			this.mismatchDetailsString = mdString;
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
			return mismatchDetailsString;
		}

	}
}
