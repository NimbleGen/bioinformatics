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

package com.roche.heatseq.objects;

import com.roche.heatseq.utils.BamFileUtil;
import com.roche.heatseq.utils.SAMRecordUtil;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;

/**
 * 
 * Standard ReadPair
 * 
 */
public class ReadPair implements IReadPair {
	private final SAMRecord record;
	private final SAMRecord mate;
	private final String extensionUid;
	private final String ligationUid;
	private final ISequence captureTargetSequence;
	private final String probeId;
	private final boolean readOneExtended;
	private final boolean readTwoExtended;
	private boolean isBestPairInUidGroup;

	public ReadPair(SAMRecord record, SAMRecord mate, String extensionUid, String ligationUid, ISequence captureTargetSequence, String probeId, boolean readOneExtended, boolean readTwoExtended) {
		super();
		this.record = record;
		this.mate = mate;
		this.extensionUid = extensionUid;
		this.ligationUid = ligationUid;
		this.captureTargetSequence = captureTargetSequence;
		this.probeId = probeId;
		this.readOneExtended = readOneExtended;
		this.readTwoExtended = readTwoExtended;
		this.isBestPairInUidGroup = false;
	}

	@Override
	public boolean isReadOneExtended() {
		return readOneExtended;
	}

	@Override
	public boolean isReadTwoExtended() {
		return readTwoExtended;
	}

	@Override
	public String getReadName() {
		return record.getReadName();
	}

	@Override
	public String getExtensionUid() {
		return extensionUid;
	}

	@Override
	public String getLigationUid() {
		return ligationUid;
	}

	@Override
	public SAMRecord getRecord() {
		return record;
	}

	@Override
	public SAMRecord getMateRecord() {
		return mate;
	}

	@Override
	public short getSequenceOneQualityScore() {
		return BamFileUtil.getQualityScore(getSequenceOneQualityString());
	}

	@Override
	public short getSequenceTwoQualityScore() {
		return BamFileUtil.getQualityScore(getSequenceTwoQualityString());
	}

	@Override
	public short getTotalSequenceQualityScore() {
		return (short) (getSequenceOneQualityScore() + getSequenceTwoQualityScore());
	}

	@Override
	public String getSequenceOne() {
		return record.getReadString();
	}

	@Override
	public String getSequenceTwo() {
		return mate.getReadString();
	}

	@Override
	public String getSequenceOneQualityString() {
		return record.getBaseQualityString();
	}

	@Override
	public String getSequenceTwoQualityString() {
		return mate.getBaseQualityString();
	}

	@Override
	public String getReadGroup() {
		String readGroup = (String) record.getAttribute(SAMRecordUtil.READ_GROUP_ATTRIBUTE_TAG);
		return readGroup;
	}

	@Override
	public SAMFileHeader getSamHeader() {
		return record.getHeader();
	}

	@Override
	public String getSequenceName() {
		return record.getReferenceName();
	}

	@Override
	public int getOneMappingQuality() {
		return record.getMappingQuality();
	}

	@Override
	public int getTwoMappingQuality() {
		return mate.getMappingQuality();
	}

	@Override
	public void markAsDuplicate() {
		record.setDuplicateReadFlag(true);
		mate.setDuplicateReadFlag(true);
	}

	@Override
	public boolean isMarkedDuplicate() {
		return record.getDuplicateReadFlag();
	}

	@Override
	public ISequence getCaptureTargetSequence() {
		return captureTargetSequence;
	}

	@Override
	public String getProbeId() {
		return probeId;
	}

	@Override
	public void setAsBestPairInUidGroup() {
		isBestPairInUidGroup = true;
	}

	@Override
	public boolean isBestPairDuplicateGroup() {
		return isBestPairInUidGroup;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((captureTargetSequence == null) ? 0 : captureTargetSequence.hashCode());
		result = prime * result + ((extensionUid == null) ? 0 : extensionUid.hashCode());
		result = prime * result + (isBestPairInUidGroup ? 1231 : 1237);
		result = prime * result + ((ligationUid == null) ? 0 : ligationUid.hashCode());
		result = prime * result + ((mate == null) ? 0 : mate.hashCode());
		result = prime * result + ((probeId == null) ? 0 : probeId.hashCode());
		result = prime * result + (readOneExtended ? 1231 : 1237);
		result = prime * result + (readTwoExtended ? 1231 : 1237);
		result = prime * result + ((record == null) ? 0 : record.hashCode());
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
		ReadPair other = (ReadPair) obj;
		if (captureTargetSequence == null) {
			if (other.captureTargetSequence != null)
				return false;
		} else if (!captureTargetSequence.equals(other.captureTargetSequence))
			return false;
		if (extensionUid == null) {
			if (other.extensionUid != null)
				return false;
		} else if (!extensionUid.equals(other.extensionUid))
			return false;
		if (isBestPairInUidGroup != other.isBestPairInUidGroup)
			return false;
		if (ligationUid == null) {
			if (other.ligationUid != null)
				return false;
		} else if (!ligationUid.equals(other.ligationUid))
			return false;
		if (mate == null) {
			if (other.mate != null)
				return false;
		} else if (!mate.equals(other.mate))
			return false;
		if (probeId == null) {
			if (other.probeId != null)
				return false;
		} else if (!probeId.equals(other.probeId))
			return false;
		if (readOneExtended != other.readOneExtended)
			return false;
		if (readTwoExtended != other.readTwoExtended)
			return false;
		if (record == null) {
			if (other.record != null)
				return false;
		} else if (!record.equals(other.record))
			return false;
		return true;
	}

}
