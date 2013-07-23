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
import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

import com.roche.heatseq.objects.SAMRecordPair;

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
		SAMFileWriter samWriter = new SAMFileWriterFactory().makeBAMWriter(header, false, outputFile);
		for (SAMRecordPair pair : records) {
			samWriter.addAlignment(pair.getFirstOfPairRecord());
			samWriter.addAlignment(pair.getSecondOfPairRecord());
		}
		samWriter.close();
	}
}
