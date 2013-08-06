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

package com.roche.heatseq.objects;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMRecord;

/**
 * 
 * Represents an Illumina Read Pair coupled with a UID
 * 
 */
public interface IReadPair {
	String getReadName();

	String getUid();

	SAMRecord getRecord();

	SAMRecord getMateRecord();

	short getSequenceOneQualityScore();

	short getSequenceTwoQualityScore();

	short getTotalSequenceQualityScore();

	String getSequenceOne();

	String getSequenceTwo();

	String getSequenceOneQualityString();

	String getSequenceTwoQualityString();

	String getReadGroup();

	SAMFileHeader getSamHeader();

	int getRecordAlignmentStart();

	int getRecordAlignmentEnd();

	int getMateAlignmentStart();

	int getMateAlignmentEnd();

	String getSequenceName();

	int getOneMappingQuality();

	int getTwoMappingQuality();

}
