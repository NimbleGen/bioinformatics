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

package com.roche.mapping;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.objects.SAMRecordPair;
<<<<<<< HEAD
import com.roche.sequencing.bioinformatics.common.alignment.CigarString;
import com.roche.sequencing.bioinformatics.common.alignment.CigarStringUtil;
=======
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
>>>>>>> master
import com.roche.sequencing.bioinformatics.common.alignment.NeedlemanWunschGlobalAlignment;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * Utility class for creating sam records using picard
 * 
 * 
 */
public class SAMRecordUtil {

	public static final String UID_SAMRECORD_ATTRIBUTE_TAG = "UI";
	public static final String MISMATCH_DETAILS_ATTRIBUTE_TAG = "MD";
	public static final String READ_GROUP_ATTRIBUTE_TAG = "RG";
	public static final String EDIT_DISTANCE_ATTRIBUTE_TAG = "NM";

	private SAMRecordUtil() {
		throw new AssertionError();
	}

	/**
	 * @param sequence
	 * @param uidLength
	 * @return the UID contained within the provided sequence
	 */
	public static String parseUidFromRead(String sequence, int uidLength) {
		String uidSequence = sequence.substring(0, uidLength);
		return uidSequence;
	}

	/**
	 * @param sequence
	 * @param uidLength
	 * @return a read sequence without the UID
	 */
	public static String removeUidFromRead(String sequence, int uidLength) {
		sequence = sequence.substring(uidLength, sequence.length());
		return sequence;
	}

	/**
	 * Set the UID attribute for this SAMRecord
	 * 
	 * @param record
	 * @param uid
	 */
	public static void setSamRecordUidAttribute(SAMRecord record, String uid) {
		record.setAttribute(UID_SAMRECORD_ATTRIBUTE_TAG, uid);
	}

	/**
	 * @param record
	 * @return the UID attribute set for this SAMRecord, null if no such attribute exists.
	 */
	public static String getUidAttribute(SAMRecord record) {
		String uid = (String) record.getAttribute(UID_SAMRECORD_ATTRIBUTE_TAG);
		return uid;
	}

	/**
	 * @param record
	 * @param probe
	 * @return the UID set for this SAMRecord, null if no such attribute exists.
	 */
<<<<<<< HEAD
	public static String getVariableLengthUid(SAMRecord record, Probe probe, PrintWriter primerAlignmentWriter) {
=======
	public static String getUidAttribute(SAMRecord record, Probe probe, IAlignmentScorer alignmentScorer) {
>>>>>>> master
		String uid = (String) record.getAttribute(UID_SAMRECORD_ATTRIBUTE_TAG);
		String completeReadWithUid = uid + record.getReadString();
		ISequence extensionPrimerSequence = probe.getExtensionPrimerSequence();
		return getVariableLengthUid(completeReadWithUid, extensionPrimerSequence, primerAlignmentWriter, probe);
	}

	/**
	 * @param completeReadWithUid
	 * @param extensionPrimerSequence
	 * @return the variable length UID based on the primer alignment, null if the uid cannot be extracted
	 */
	public static String getVariableLengthUid(String completeReadWithUid, ISequence extensionPrimerSequence, PrintWriter primerAlignmentWriter, Probe probe) {
		ISequence completeReadSequence = new IupacNucleotideCodeSequence(completeReadWithUid);
<<<<<<< HEAD
		NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(completeReadSequence, extensionPrimerSequence);
=======
		ISequence primerSequence = probe.getExtensionPrimerSequence();
		NeedlemanWunschGlobalAlignment alignment = new NeedlemanWunschGlobalAlignment(completeReadSequence, primerSequence, alignmentScorer);
		// TODO kurt heilman add report to kick out poor alignments and shorter than expected uids
>>>>>>> master
		int uidEndIndex = alignment.getIndexOfFirstMatchInReference();
		String variableLengthUid = null;
		if (uidEndIndex >= 0) {
			variableLengthUid = completeReadWithUid.substring(0, uidEndIndex);
		}

		int uidLength = variableLengthUid.length();
		int numberOfInsertions = 0;
		int numberOfDeletions = 0;
		int numberOfSubstitutions = 0;

		CigarString cigarString = alignment.getCigarString();
		String sequenceCigarString = cigarString.getCigarString(false, true);
		sequenceCigarString.substring(uidEndIndex, sequenceCigarString.length());
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

		int editDistanceCutoff = (extensionPrimerSequence.size() / 4);

		if (editDistance > 0 && editDistance < editDistanceCutoff) {
			if (primerAlignmentWriter != null) {
				int cutoffIndex = editDistanceCutoff + extensionPrimerSequence.size() + uidLength;
				ISequence referenceSequence = alignment.getAlignmentPair().getReferenceAlignment().subSequence(0, cutoffIndex);
				ISequence querySequence = alignment.getAlignmentPair().getQueryAlignment().subSequence(0, cutoffIndex);
				String probeName = probe.getContainerName();
				String probeCaptureStart = "" + probe.getCaptureTargetStart();
				String probeCaptureStop = "" + probe.getCaptureTargetStop();
				String probeStrand = "" + probe.getProbeStrand();
				primerAlignmentWriter.println(uidLength + StringUtil.TAB + numberOfSubstitutions + StringUtil.TAB + numberOfInsertions + StringUtil.TAB + numberOfDeletions + StringUtil.TAB
						+ editDistance + StringUtil.TAB + referenceSequence + StringUtil.TAB + querySequence + StringUtil.TAB + probeName + StringUtil.TAB + probeCaptureStart + StringUtil.TAB
						+ probeCaptureStop + StringUtil.TAB + probeStrand);
			}
		} else if (editDistance >= editDistanceCutoff) {
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

	static SAMFileHeader createSAMFileHeader() {
		SAMFileHeader header = new SAMFileHeader();
		return header;
	}

	static void createBamFile(SAMFileHeader header, File outputFile, List<SAMRecordPair> records) {
		// Make an output BAM file sorted by coordinates and as compressed as possible
		header.setSortOrder(SortOrder.coordinate);
		SAMFileWriter samWriter = new SAMFileWriterFactory().makeBAMWriter(header, false, outputFile, 9);
		for (SAMRecordPair pair : records) {
			samWriter.addAlignment(pair.getFirstOfPairRecord());
			samWriter.addAlignment(pair.getSecondOfPairRecord());
		}
		samWriter.close();
	}
}
