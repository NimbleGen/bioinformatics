/*
 *    Copyright 2013 Roche NimbleGen Inc.
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

import net.sf.samtools.SAMRecord;

/**
 * 
 * Container for holding onto a pair of SAMRecords
 * 
 */
public class SAMRecordPair {
	private SAMRecord firstOfPairRecord;
	private SAMRecord secondOfPairRecord;

	/**
	 * Basic Constructor for use with the setters
	 */
	public SAMRecordPair() {
		super();
	}

	/**
	 * Constructor
	 * 
	 * @param firstOfPairRecord
	 * @param secondOfPairRecord
	 */
	public SAMRecordPair(SAMRecord firstOfPairRecord, SAMRecord secondOfPairRecord) {
		super();
		this.firstOfPairRecord = firstOfPairRecord;
		this.secondOfPairRecord = secondOfPairRecord;
	}

	public SAMRecord getFirstOfPairRecord() {
		return firstOfPairRecord;
	}

	public void setFirstOfPairRecord(SAMRecord firstOfPairRecord) {
		this.firstOfPairRecord = firstOfPairRecord;
	}

	public SAMRecord getSecondOfPairRecord() {
		return secondOfPairRecord;
	}

	public void setSecondOfPairRecord(SAMRecord secondOfPairRecord) {
		this.secondOfPairRecord = secondOfPairRecord;
	}
}
