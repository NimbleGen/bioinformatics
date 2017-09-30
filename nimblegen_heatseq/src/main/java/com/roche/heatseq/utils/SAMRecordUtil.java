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

package com.roche.heatseq.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.SAMRecordPair;
import com.roche.heatseq.qualityreport.ReportManager;
import com.roche.sequencing.bioinformatics.common.alignment.CigarString;
import com.roche.sequencing.bioinformatics.common.alignment.CigarStringUtil;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.Probe;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;

/**
 * Utility class for creating sam records using picard
 * 
 * 
 */
public class SAMRecordUtil {

	private static Logger logger = LoggerFactory.getLogger(SAMRecordUtil.class);

	private static final String EXTENSION_UID_SAMRECORD_ATTRIBUTE_TAG = "EI";
	private static final String LIGATION_UID_SAMRECORD_ATTRIBUTE_TAG = "LI";
	private static final String UID_GROUP_SAMRECORD_ATTRIBUTE_TAG = "UG";
	private static final String PROBE_ID_SAMRECORD_ATTRIBUTE_TAG = "PI";
	public static final String MISMATCH_DETAILS_ATTRIBUTE_TAG = "MD";
	public static final String READ_GROUP_ATTRIBUTE_TAG = "RG";
	public static final String EDIT_DISTANCE_ATTRIBUTE_TAG = "NM";
	private static final String EXTENSION_ERROR_ATTRIBUTE_TAG = "EE";
	private static final String MAPPED_READ_LENGTH_ATTRIBUTE_TAG = "ML";
	private static final String ALTERNATIVE_HITS_ATTRIBUTE_TAG = "XA";

	private SAMRecordUtil() {
		throw new AssertionError();
	}

	/**
	 * @param sequence
	 * @param uidLength
	 * @return the UID contained within the provided sequence
	 */
	public static String parseUidFromReadOne(String sequence, int extensionUidLength) {
		String uidSequence = sequence.substring(0, extensionUidLength);
		return uidSequence;
	}

	/**
	 * @param sequence
	 * @param uidLength
	 * @return the UID contained within the provided sequence
	 */
	public static String parseUidFromReadTwo(String sequence, int ligationUidLength) {
		String uidSequence = sequence.substring(0, ligationUidLength);
		return uidSequence;
	}

	/**
	 * @param sequence
	 * @param uidLength
	 * @return a read sequence without the UID
	 */

	public static String removeUidFromReadOne(String sequence, int extensionUidLength) {
		sequence = sequence.substring(extensionUidLength, sequence.length());
		return sequence;
	}

	/**
	 * @param sequence
	 * @param uidLength
	 * @return a read sequence without the UID
	 */
	public static String removeUidFromReadTwo(String sequence, int ligationUidLength) {
		sequence = sequence.substring(ligationUidLength, sequence.length());
		return sequence;
	}

	/**
	 * Set the extension UID attribute for this SAMRecord
	 * 
	 * @param record
	 * @param uid
	 */
	public static void setSamRecordExtensionUidAttribute(SAMRecord record, String uid) {
		record.setAttribute(EXTENSION_UID_SAMRECORD_ATTRIBUTE_TAG, uid);
	}

	/**
	 * Set the extension UID attribute for this SAMRecord
	 * 
	 * @param record
	 * @param uid
	 */
	public static void setSamRecordUidGroupAttribute(SAMRecord record, String uidGroup) {
		record.setAttribute(UID_GROUP_SAMRECORD_ATTRIBUTE_TAG, uidGroup);
	}

	/**
	 * Set the ligation UID attribute for this SAMRecord
	 * 
	 * @param record
	 * @param uid
	 */
	public static void setSamRecordLigationUidAttribute(SAMRecord record, String uid) {
		record.setAttribute(LIGATION_UID_SAMRECORD_ATTRIBUTE_TAG, uid);
	}

	/**
	 * Set the read length of the truncated read used for mapping this is necessary because we override this read with the raw read
	 * 
	 * @param record
	 * @param length
	 */
	public static void setMappedReadLength(SAMRecord record, int length) {
		record.setAttribute(MAPPED_READ_LENGTH_ATTRIBUTE_TAG, length);
	}

	public static int getMappedReadLengthFromAttribute(SAMRecord record) {
		Object lengthAsString = record.getAttribute(MAPPED_READ_LENGTH_ATTRIBUTE_TAG);
		Integer length = null;
		if (lengthAsString instanceof Integer) {
			length = (Integer) lengthAsString;
		}
		return length;
	}

	/**
	 * Set the probeId attribute for this SAMRecord
	 * 
	 * @param record
	 * @param uid
	 */
	public static void setSamRecordProbeIdAttribute(SAMRecord record, String probeId) {
		record.setAttribute(PROBE_ID_SAMRECORD_ATTRIBUTE_TAG, probeId);
	}

	/**
	 * Set the extension error attribute for this SAMRecord
	 * 
	 * @param record
	 * @param uid
	 */

	public static void setExtensionErrorAttribute(SAMRecord record, boolean unableToExtendReadOne, boolean unableToExtendReadTwo) {
		if (unableToExtendReadOne && unableToExtendReadTwo) {
			record.setAttribute(EXTENSION_ERROR_ATTRIBUTE_TAG, "FAILED_TO_EXTEND_READ_ONE_AND_READ_TWO");
		} else if (unableToExtendReadOne) {
			record.setAttribute(EXTENSION_ERROR_ATTRIBUTE_TAG, "FAILED_TO_EXTEND_READ_ONE");
		} else if (unableToExtendReadTwo) {
			record.setAttribute(EXTENSION_ERROR_ATTRIBUTE_TAG, "FAILED_TO_EXTEND_READ_TWO");
		}
	}

	/**
	 * @param record
	 * @param probe
	 * @return the UID set for this SAMRecord, null if no such attribute exists.
	 */
	public static String getExtensionVariableLengthUid(SAMRecord record, Probe probe, ReportManager reportManager, IAlignmentScorer alignmentScorer) {
		String completeReadWithUid = record.getReadString();
		ISequence extensionPrimerSequence = probe.getExtensionPrimerSequence();
		return getVariableLengthUid(completeReadWithUid, extensionPrimerSequence, reportManager, probe, alignmentScorer);
	}

	public static int getEditDistance(SAMRecord record) {
		int editDistance = 0;
		Object editDistanceAsObject = record.getAttribute(EDIT_DISTANCE_ATTRIBUTE_TAG);
		if (editDistanceAsObject != null) {
			if (editDistanceAsObject instanceof Integer) {
				editDistance = (Integer) editDistanceAsObject;
			}
		}
		return editDistance;
	}

	/**
	 * @param record
	 * @param probe
	 * @return the UID set for this SAMRecord, null if no such attribute exists.
	 */
	public static String getLigationVariableLengthUid(SAMRecord record, Probe probe, ReportManager reportManager, IAlignmentScorer alignmentScorer) {
		String completeReadWithUid = record.getReadString();
		ISequence ligationPrimerSequence = probe.getLigationPrimerSequence().getReverseCompliment();
		return getVariableLengthUid(completeReadWithUid, ligationPrimerSequence, reportManager, probe, alignmentScorer);
	}

	/**
	 * @param completeReadWithUid
	 * @param primerSequence
	 * @return the variable length UID based on the primer alignment, null if the uid cannot be extracted
	 */
	private static String getVariableLengthUid(String completeReadWithUid, ISequence primerSequence, ReportManager reportManager, Probe probe, IAlignmentScorer alignmentScorer) {
		ISequence completeReadSequence = new IupacNucleotideCodeSequence(completeReadWithUid);
		NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(completeReadSequence, primerSequence, alignmentScorer);
		int uidEndIndex = alignment.getIndexOfFirstMatchInReference();
		String variableLengthUid = null;
		if (uidEndIndex >= 0) {
			variableLengthUid = completeReadWithUid.substring(0, uidEndIndex);
		}

		int numberOfInsertions = 0;
		int numberOfDeletions = 0;
		int numberOfSubstitutions = 0;

		CigarString cigarString = alignment.getCigarString();
		String sequenceCigarString = cigarString.getCigarString(false, true, false, false);
		int insertsSinceLastInsertionOrMismatch = 0;
		for (Character character : sequenceCigarString.toCharArray()) {
			if (character == CigarStringUtil.CIGAR_SEQUENCE_MISMATCH) {
				numberOfSubstitutions++;
				insertsSinceLastInsertionOrMismatch = 0;
			} else if (character == CigarStringUtil.CIGAR_INSERTION_TO_REFERENCE) {
				numberOfInsertions++;
				insertsSinceLastInsertionOrMismatch = 0;
			} else if (character == CigarStringUtil.CIGAR_DELETION_FROM_REFERENCE) {
				numberOfDeletions++;
			}
		}
		numberOfDeletions -= insertsSinceLastInsertionOrMismatch;

		int editDistance = numberOfInsertions + numberOfDeletions + numberOfSubstitutions;

		int editDistanceCutoff = (primerSequence.size() / 4);

		if (editDistance >= editDistanceCutoff) {
			variableLengthUid = null;
		}

		return variableLengthUid;
	}

	/**
	 * Create a SAMRecordPair with the supplied SAMRecord objects, making sure all the correct flags are set.
	 * 
	 * @param samRecordFirstOfPair
	 * @param samRecordSecondOfPair
	 * @return SAMRecordPair
	 */
	public static SAMRecordPair setSAMRecordsAsPair(SAMRecord samRecordFirstOfPair, SAMRecord samRecordSecondOfPair) {
		samRecordFirstOfPair.setFirstOfPairFlag(true);
		samRecordFirstOfPair.setProperPairFlag(true);
		samRecordFirstOfPair.setReadPairedFlag(true);
		samRecordSecondOfPair.setSecondOfPairFlag(true);
		samRecordSecondOfPair.setProperPairFlag(true);
		samRecordSecondOfPair.setReadPairedFlag(true);

		assignMateAttributes(samRecordFirstOfPair, samRecordSecondOfPair);
		assignMateAttributes(samRecordSecondOfPair, samRecordFirstOfPair);
		return new SAMRecordPair(samRecordFirstOfPair, samRecordSecondOfPair);
	}

	private static void assignMateAttributes(SAMRecord samRecord, SAMRecord mate) {
		samRecord.setMateAlignmentStart(mate.getAlignmentStart());
		samRecord.setMateNegativeStrandFlag(mate.getReadNegativeStrandFlag());
		samRecord.setMateReferenceIndex(mate.getReferenceIndex());
		samRecord.setMateReferenceName(mate.getReferenceName());
		samRecord.setMateUnmappedFlag(mate.getReadUnmappedFlag());
	}

	public static void createBamFile(SAMFileHeader header, File outputFile, List<SAMRecordPair> records) {
		// Make an output BAM file sorted by coordinates and as compressed as possible
		header.setSortOrder(SortOrder.coordinate);
		SAMFileWriter samWriter = new SAMFileWriterFactory().makeBAMWriter(header, false, outputFile, 9);
		for (SAMRecordPair pair : records) {
			samWriter.addAlignment(pair.getFirstOfPairRecord());
			samWriter.addAlignment(pair.getSecondOfPairRecord());
		}
		samWriter.close();
	}

	public static class SamReadCount {
		private final int totalUnmappedReads;
		private final int totalMappedReads;

		private SamReadCount(int totalUnmappedReads, int totalMappedReads) {
			super();
			this.totalUnmappedReads = totalUnmappedReads;
			this.totalMappedReads = totalMappedReads;
		}

		public int getTotalUnmappedReads() {
			return totalUnmappedReads;
		}

		public int getTotalMappedReads() {
			return totalMappedReads;
		}

		public int getTotalReads() {
			return totalMappedReads + totalUnmappedReads;
		}

	}

	public static String getProbeId(SAMRecord record) {
		String probeId = null;
		Object probeIdAsObject = record.getAttribute(PROBE_ID_SAMRECORD_ATTRIBUTE_TAG);
		if (probeIdAsObject != null) {
			probeId = probeIdAsObject.toString();
		}
		return probeId;
	}

	public static String getExtensionUid(SAMRecord record) {
		String extensionUid = null;
		Object extensionUidAsObject = record.getAttribute(EXTENSION_UID_SAMRECORD_ATTRIBUTE_TAG);
		if (extensionUidAsObject != null) {
			extensionUid = extensionUidAsObject.toString();
		}
		return extensionUid;
	}

	public static String getLigationUid(SAMRecord record) {
		String ligationUid = null;
		Object ligationUidAsObject = record.getAttribute(LIGATION_UID_SAMRECORD_ATTRIBUTE_TAG);
		if (ligationUidAsObject != null) {
			ligationUid = ligationUidAsObject.toString();
		}
		return ligationUid;
	}

	public static String getUidGroup(SAMRecord record) {
		String uidGroup = null;
		Object uidGroupAsObject = record.getAttribute(UID_GROUP_SAMRECORD_ATTRIBUTE_TAG);
		if (uidGroupAsObject != null) {
			uidGroup = uidGroupAsObject.toString();
		}
		return uidGroup;
	}

	public static List<AlternativeHit> getAlternativeHitsFromAttribute(SAMRecord record) {
		List<AlternativeHit> alternativeHits;

		Object alternativeHitsAsObject = record.getAttribute(ALTERNATIVE_HITS_ATTRIBUTE_TAG);
		if (alternativeHitsAsObject != null) {
			alternativeHits = AlternativeHit.fromAttributeText(record, alternativeHitsAsObject.toString());
		} else {
			alternativeHits = Collections.emptyList();
		}
		return alternativeHits;
	}

	public static String getAlternativeHitsAsString(List<AlternativeHit> alternativeHits) {
		StringBuilder text = new StringBuilder();
		for (AlternativeHit hit : alternativeHits) {
			text.append(hit.toAttributeText());
		}
		return text.toString();
	}

	public static void setAlternativeHitsAttribute(SAMRecord record, List<AlternativeHit> alternativeHits) {

		String alternativeHitsString = getAlternativeHitsAsString(alternativeHits);

		record.setAttribute(ALTERNATIVE_HITS_ATTRIBUTE_TAG, alternativeHitsString);
	}

	public static boolean recordHasAltHits(SAMRecord record) {
		return record.getAttribute(ALTERNATIVE_HITS_ATTRIBUTE_TAG) != null;
	}

	public static class AlternativeHit {
		private final SAMRecord copyOfSamRecord;

		private AlternativeHit(SAMRecord samRecord, String container, int start, int stop, Strand strand, String cigarString) {
			super();
			this.copyOfSamRecord = samRecord.deepCopy();
			this.copyOfSamRecord.setReferenceName(container);
			this.copyOfSamRecord.setAlignmentStart(start);
			this.copyOfSamRecord.setReadNegativeStrandFlag(strand == Strand.REVERSE);
			this.copyOfSamRecord.setCigarString(cigarString);
		}

		public AlternativeHit(SAMRecord record) {
			super();
			this.copyOfSamRecord = record.deepCopy();
		}

		public SAMRecord getAsSAMRecord() {
			return copyOfSamRecord;
		}

		public String getContainer() {
			return copyOfSamRecord.getReferenceName();
		}

		public int getStart() {
			return copyOfSamRecord.getAlignmentStart();
		}

		public int getStop() {
			return copyOfSamRecord.getAlignmentEnd();
		}

		public Strand getStrand() {
			Strand strand = null;
			if (copyOfSamRecord.getReadNegativeStrandFlag()) {
				strand = Strand.REVERSE;
			} else {
				strand = Strand.FORWARD;
			}
			return strand;
		}

		public String getCigarString() {
			return copyOfSamRecord.getCigarString();
		}

		public static List<AlternativeHit> fromAttributeText(SAMRecord samRecord, String attributeText) {
			List<AlternativeHit> alternativeHits = new ArrayList<>();
			String[] entries = attributeText.split(";");
			for (String entry : entries) {
				String[] fields = entry.split(",");
				if (fields.length == 4) {
					String container = fields[0];

					try {
						int startAndStrand = Integer.parseInt(fields[1]);
						Strand strand = Strand.FORWARD;
						if (startAndStrand < 0) {
							strand = Strand.REVERSE;
						}
						int start = Math.abs(startAndStrand);

						String cigarString = fields[2];

						// int editDistance = Integer.parseInt(fields[3]);

						int length = CigarStringUtil.expandCigarString(cigarString).length();
						int stop = start + length;

						alternativeHits.add(new AlternativeHit(samRecord, container, start, stop, strand, cigarString));

					} catch (NumberFormatException e) {
						logger.warn("Unable to parse the XA attribute text[" + attributeText + "] so the alternative hit will not be utilized.");
					}

				}
			}

			// go through the alternative hits for this record and update their alternative hits
			for (AlternativeHit currentHit : alternativeHits) {
				List<AlternativeHit> alternativeHitsForCurrentHit = new ArrayList<>(alternativeHits);
				// remove this mapping as an alternative hit since it is now the primary mapping
				alternativeHitsForCurrentHit.remove(currentHit);
				// add the original mapping
				alternativeHitsForCurrentHit.add(new AlternativeHit(samRecord));

				setAlternativeHitsAttribute(currentHit.copyOfSamRecord, alternativeHitsForCurrentHit);
			}

			return alternativeHits;
		}

		public String toAttributeText() {
			StringBuilder attributeText = new StringBuilder();

			attributeText.append(getContainer() + ",");
			if (getStrand() == Strand.FORWARD) {
				attributeText.append("+");
			} else {
				attributeText.append("-");
			}
			attributeText.append(getStart() + ",");
			attributeText.append(getCigarString() + ",");
			attributeText.append(getEditDistance(copyOfSamRecord) + ";");
			return attributeText.toString();
		}

	}
}
