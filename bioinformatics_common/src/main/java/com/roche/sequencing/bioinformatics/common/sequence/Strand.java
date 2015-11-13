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

/**
 * 
 * Represents a genomic strand
 * 
 */
public enum Strand {
	FORWARD("+"), REVERSE("-");

	private String symbol;

	private Strand(String symbol) {
		this.symbol = symbol;
	}

	@Override
	public String toString() {
		return symbol;
	}

	/**
	 * @param text
	 * @return the Strand enum that matches this string, "+" will return FORWARD, "-" will return REVERSE and anything else will return null.
	 */
	public static Strand fromString(String text) {
		Strand returnStrand = null;
		if (text != null) {
			strandLoop: for (Strand strand : Strand.values()) {
				if (text.equalsIgnoreCase(strand.symbol)) {
					returnStrand = strand;
					break strandLoop;
				}
			}
		}
		return returnStrand;
	}

	/**
	 * @return the opposite strand
	 */
	public Strand getOpposite() {
		Strand opposite = Strand.FORWARD;
		if (this == Strand.FORWARD) {
			opposite = Strand.REVERSE;
		}
		return opposite;
	}

	/**
	 * @return the symbol associated with this strand ("+" for FORWARD and "-" for REVERSE
	 */
	public String getSymbol() {
		return symbol;
	}
}
