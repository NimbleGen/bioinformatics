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

package com.roche.sequencing.bioinformatics.common.sequence;

public interface ISequence extends Iterable<ICode> {
	/**
	 * Add the provided sequenceAsString to the end of this sequence
	 * 
	 * @param sequenceAsString
	 */
	ISequence append(ISequence sequenceAsString);

	/**
	 * @return the number of codes present in this sequence
	 */
	int size();

	/**
	 * @param i
	 * @return the code found at the ith position.
	 */
	ICode getCodeAt(int i);

	/**
	 * 
	 * @param start
	 * @param end
	 * @return the sequence starting at start and ending at end (inclusive)
	 */
	ISequence subSequence(int start, int end);

	/**
	 * 
	 * @param start
	 * @return the sequence starting at start and ending at the end of the string
	 */
	ISequence subSequence(int start);

	/**
	 * @return the reverse compliment representation of this sequence
	 */
	ISequence getReverseCompliment();

	/**
	 * @return the reverse representation of this sequence
	 */
	ISequence getReverse();

	/**
	 * @return the compliment representation of this sequence
	 */
	ISequence getCompliment();

	/**
	 * @return the percent of bases in this sequence that are g's or c's
	 */
	double getGCPercent();

	boolean contains(ICode nucleotide);
}
