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

import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * 
 * Class for Holding onto the details included in an Illumina Fastq header
 * 
 */
public class IlluminaFastQHeader {
	private final String uniqueInstrumentName;
	private final int runId;
	private final String flowcellId;
	private final int flowcellLane;
	private final int tileNumberInFlowcellLane;
	private final int xCoordinate;
	private final int yCoordinate;
	private Short pairNumber;
	private Boolean isBad;
	private Integer activeControlBits;
	private String indexSequence;

	IlluminaFastQHeader(String uniqueInstrumentName, int runId, String flowcellId, int flowcellLane, int tileNumberInFlowcellLane, int xCoordinate, int yCoordinate, short pairNumber, boolean isBad,
			int activeControlBits, String indexSequence) {
		super();
		this.uniqueInstrumentName = uniqueInstrumentName;
		this.runId = runId;
		this.flowcellId = flowcellId;
		this.flowcellLane = flowcellLane;
		this.tileNumberInFlowcellLane = tileNumberInFlowcellLane;
		this.xCoordinate = xCoordinate;
		this.yCoordinate = yCoordinate;
		this.pairNumber = pairNumber;
		this.isBad = isBad;
		this.activeControlBits = activeControlBits;
		this.indexSequence = indexSequence;
	}

	IlluminaFastQHeader(String uniqueInstrumentName, int runId, String flowcellId, int flowcellLane, int tileNumberInFlowcellLane, int xCoordinate, int yCoordinate) {
		super();
		this.uniqueInstrumentName = uniqueInstrumentName;
		this.runId = runId;
		this.flowcellId = flowcellId;
		this.flowcellLane = flowcellLane;
		this.tileNumberInFlowcellLane = tileNumberInFlowcellLane;
		this.xCoordinate = xCoordinate;
		this.yCoordinate = yCoordinate;
	}

	IlluminaFastQHeader(String uniqueInstrumentName, int flowcellLane, int tileNumberInFlowcellLane, int xCoordinate, int yCoordinate, short pairNumber) {
		super();
		this.uniqueInstrumentName = uniqueInstrumentName;
		this.flowcellLane = flowcellLane;
		this.tileNumberInFlowcellLane = tileNumberInFlowcellLane;
		this.xCoordinate = xCoordinate;
		this.yCoordinate = yCoordinate;
		this.pairNumber = pairNumber;
		this.runId = 0;
		this.flowcellId = "UNIDENTIFIED_FLOWCELL";
	}

	public String getBaseHeader() {
		return uniqueInstrumentName + ":" + runId + ":" + flowcellId + ":" + flowcellLane + ":" + tileNumberInFlowcellLane + ":" + xCoordinate + ":" + yCoordinate;
	}

	public String getAdditionalHeader() {
		String isBadString = "0";
		if (isBad) {
			isBadString = "1";
		}
		return pairNumber + ":" + isBadString + ":" + activeControlBits + ":" + indexSequence;
	}

	public String getUniqueInstrumentName() {
		return uniqueInstrumentName;
	}

	public int getRunId() {
		return runId;
	}

	public String getFlowcellId() {
		return flowcellId;
	}

	public int getFlowcellLane() {
		return flowcellLane;
	}

	public int getTileNumberInFlowcellLane() {
		return tileNumberInFlowcellLane;
	}

	public int getXCoordinate() {
		return xCoordinate;
	}

	public int getYCoordinate() {
		return yCoordinate;
	}

	public Short getPairNumber() {
		return pairNumber;
	}

	public Integer getActiveControlBits() {
		return activeControlBits;
	}

	public String getIndexSequence() {
		return indexSequence;
	}

	@Override
	public String toString() {
		return "IlluminaFastQHeader [uniqueInstrumentName=" + uniqueInstrumentName + ", runId=" + runId + ", flowcellId=" + flowcellId + ", flowcellLane=" + flowcellLane
				+ ", tileNumberInFlowcellLane=" + tileNumberInFlowcellLane + ", xCoordinate=" + xCoordinate + ", yCoordinate=" + yCoordinate + ", pairNumber=" + pairNumber + ", isBad=" + isBad
				+ ", activeControlBits=" + activeControlBits + ", indexSequence=" + indexSequence + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;

		result = prime * result + ((activeControlBits == null) ? 0 : activeControlBits.hashCode());
		result = prime * result + ((flowcellId == null) ? 0 : flowcellId.hashCode());
		result = prime * result + flowcellLane;
		result = prime * result + ((indexSequence == null) ? 0 : indexSequence.hashCode());
		result = prime * result + ((isBad == null) ? 0 : isBad.hashCode());
		result = prime * result + ((pairNumber == null) ? 0 : pairNumber.hashCode());
		result = prime * result + runId;
		result = prime * result + tileNumberInFlowcellLane;
		result = prime * result + ((uniqueInstrumentName == null) ? 0 : uniqueInstrumentName.hashCode());
		result = prime * result + xCoordinate;
		result = prime * result + yCoordinate;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		IlluminaFastQHeader other = (IlluminaFastQHeader) obj;

		if (activeControlBits == null) {
			if (other.activeControlBits != null) {
				return false;
			}
		} else if (!activeControlBits.equals(other.activeControlBits)) {
			return false;
		}

		if (flowcellId == null) {
			if (other.flowcellId != null) {
				return false;
			}
		} else if (!flowcellId.equals(other.flowcellId)) {
			return false;
		}

		if (flowcellLane != other.flowcellLane) {
			return false;
		}

		if (indexSequence == null) {
			if (other.indexSequence != null) {
				return false;
			}
		} else if (!indexSequence.equals(other.indexSequence)) {
			return false;
		}

		if (isBad == null) {
			if (other.isBad != null) {
				return false;
			}
		} else if (!isBad.equals(other.isBad)) {
			return false;
		}

		if (pairNumber == null) {
			if (other.pairNumber != null) {
				return false;
			}
		} else if (!pairNumber.equals(other.pairNumber)) {
			return false;
		}

		if (runId != other.runId) {
			return false;
		}

		if (tileNumberInFlowcellLane != other.tileNumberInFlowcellLane) {
			return false;
		}

		if (uniqueInstrumentName == null) {
			if (other.uniqueInstrumentName != null) {
				return false;
			}
		} else if (!uniqueInstrumentName.equals(other.uniqueInstrumentName)) {
			return false;
		}

		if (xCoordinate != other.xCoordinate) {
			return false;
		}

		if (yCoordinate != other.yCoordinate) {
			return false;
		}

		return true;
	}

	/**
	 * 
	 * @param readHeader
	 * @return IlluminaFastQHeader if the readHeader could be parsed, otherwise null.
	 */
	public static IlluminaFastQHeader parseIlluminaFastQHeader(String readHeader) {
		IlluminaFastQHeader header = null;

		String[] readHeaderSplitByColon = readHeader.split(":");
		if (readHeaderSplitByColon.length >= 7) {
			String uniqueInstrumentName = readHeaderSplitByColon[0];
			int runId = Integer.valueOf(readHeaderSplitByColon[1]);
			String flowcellId = readHeaderSplitByColon[2];
			int flowcellLane = Integer.valueOf(readHeaderSplitByColon[3]);
			int tileNumberInFlowcellLane = Integer.valueOf(readHeaderSplitByColon[4]);
			int xCoordinate = Integer.valueOf(readHeaderSplitByColon[5]);

			String[] sixtchColumnSplitBySpace = readHeaderSplitByColon[6].split(" ");

			int yCoordinate = Integer.valueOf(sixtchColumnSplitBySpace[0]);
			if (sixtchColumnSplitBySpace.length == 2 && readHeaderSplitByColon.length == 10) {
				short pairNumber = Short.valueOf(sixtchColumnSplitBySpace[1]);
				boolean isBad = Boolean.valueOf(readHeaderSplitByColon[7]);
				int activeControlBits = Integer.valueOf(readHeaderSplitByColon[8]);
				String indexSequence = readHeaderSplitByColon[9];
				header = new IlluminaFastQHeader(uniqueInstrumentName, runId, flowcellId, flowcellLane, tileNumberInFlowcellLane, xCoordinate, yCoordinate, pairNumber, isBad, activeControlBits,
						indexSequence);
			} else {
				header = new IlluminaFastQHeader(uniqueInstrumentName, runId, flowcellId, flowcellLane, tileNumberInFlowcellLane, xCoordinate, yCoordinate);
			}
		} else if (readHeaderSplitByColon.length == 5) {
			String uniqueInstrumentName = readHeaderSplitByColon[0];
			int flowcellLane = Integer.valueOf(readHeaderSplitByColon[1]);
			int tileNumberInFlowcellLane = Integer.valueOf(readHeaderSplitByColon[2]);
			int xCoordinate = Integer.valueOf(readHeaderSplitByColon[3]);
			String[] fourthColumnSplitByPound = readHeaderSplitByColon[4].split("#");
			String[] fourthColumnSplitByForwardSlash = readHeaderSplitByColon[4].split("/");
			int yCoordinate = Integer.valueOf(fourthColumnSplitByPound[0]);
			short pairNumber = Short.valueOf(fourthColumnSplitByForwardSlash[1]);
			header = new IlluminaFastQHeader(uniqueInstrumentName, flowcellLane, tileNumberInFlowcellLane, xCoordinate, yCoordinate, pairNumber);
		}

		return header;

	}

	/**
	 * Parse and return the base header out of an Illumina fastQ read header
	 * 
	 * @param readHeader
	 *            The Illumina fastQ read header
	 * @return A base header in the format uniqueInstrumentName:runId:flowcellId:flowcellLane:tileNumberInFlowcellLane: xCoordinate:yCoordinate, or the original header if the source header is not in
	 *         the format we expect.
	 */
	public static String getBaseHeader(String readHeader) {
		String baseHeader = "";
		// Find the colon before the Y value
		int indexOfSixthColon = StringUtil.nthOccurrence(readHeader, ':', 5);

		// Find the space after the Y value
		int indexOfSpaceAfterSixthColon = -1;
		if (indexOfSixthColon != -1) {
			indexOfSpaceAfterSixthColon = readHeader.indexOf(' ', indexOfSixthColon);
			if (indexOfSpaceAfterSixthColon == -1) {
				indexOfSpaceAfterSixthColon = readHeader.length();
			}
		}

		// If everything was formatted
		if (indexOfSpaceAfterSixthColon != -1) {
			// this is a
			baseHeader = readHeader.substring(0, indexOfSpaceAfterSixthColon);
		} else {
			throw new IllegalStateException("Could not parse read header[" + readHeader + "].");
		}
		return baseHeader;
	}

	/**
	 * Parse and return the unique identifier from the fastQ read header
	 * 
	 * @param readHeader
	 * @return
	 */
	public static String getUniqueIdForReadHeader(String readHeader) {
		// sample read name:M01077:35:000000000-A3J96:1:1102:13646:7860 1:N:0:1
		// originally this code only used information from the third colon to the sixth colon
		// but if reads are combined across multiple runs the information before the
		// third colon is necessary to uniquely identify the read
		return getBaseHeader(readHeader);
	}

	public static void main(String[] args) {
		System.out.println(getBaseHeader("@MS5_15454:1:1110:12527:26507#26/1"));
	}
}
