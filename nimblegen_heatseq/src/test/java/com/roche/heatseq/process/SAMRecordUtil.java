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

import java.io.File;
import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceRecord;

import com.roche.heatseq.objects.SAMRecordPair;

public class SAMRecordUtil {

	private SAMRecordUtil() {
		throw new AssertionError();
	}

	public static SAMRecord createSAMRecord(SAMFileHeader header, String readName, String referenceName, int alignmentStart, String cigarString, String readSequence, String baseQualityString,
			int mappingQuality) {

		SAMRecord record = new SAMRecord(header);
		record.setAlignmentStart(alignmentStart);
		record.setCigarString(cigarString);
		record.setReadString(readSequence);
		record.setReadName(readName);
		record.setBaseQualityString(baseQualityString);
		record.setReferenceName(referenceName);
		record.setMappingQuality(mappingQuality);
		record.setReadUnmappedFlag(false);
		record.validateCigar(-1);
		return record;
	}

	public static SAMRecordPair createSAMRecordPair(SAMRecord samRecordFirstOfPair, SAMRecord samRecordSecondOfPair) {
		samRecordFirstOfPair.setFirstOfPairFlag(true);
		samRecordFirstOfPair.setProperPairFlag(true);
		samRecordFirstOfPair.setReadNegativeStrandFlag(false);
		samRecordFirstOfPair.setReadPairedFlag(true);
		samRecordSecondOfPair.setSecondOfPairFlag(true);
		samRecordSecondOfPair.setProperPairFlag(true);
		samRecordSecondOfPair.setReadNegativeStrandFlag(true);
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
		int inferredInsertSize = mate.getAlignmentStart() - samRecord.getAlignmentStart();
		samRecord.setInferredInsertSize(inferredInsertSize);
	}

	public static SAMFileHeader createSAMFileHeader() {
		SAMFileHeader header = new SAMFileHeader();
		header.addSequence(new SAMSequenceRecord("ref1", 1000));
		return header;
	}

	public static void createBamFile(SAMFileHeader header, File outputFile, List<SAMRecordPair> records) {
		SAMFileWriter samWriter = new SAMFileWriterFactory().makeBAMWriter(header, false, outputFile);
		for (SAMRecordPair pair : records) {
			samWriter.addAlignment(pair.getFirstOfPairRecord());
			samWriter.addAlignment(pair.getSecondOfPairRecord());
		}
		samWriter.close();
	}

}
