package com.roche.sequencing.bioinformatics.common.utils.gzip;

import java.util.Arrays;

public class GzipHeaderExtraSubfield {

	private final int subfieldIdentifierOne;
	private final int subfieldIdentifierTwo;
	private final byte[] subfieldBytes;

	public GzipHeaderExtraSubfield(int subfieldIdentifierOne, int subfieldIdentifierTwo, byte[] subfieldBytes) {
		super();
		this.subfieldIdentifierOne = subfieldIdentifierOne;
		this.subfieldIdentifierTwo = subfieldIdentifierTwo;
		this.subfieldBytes = subfieldBytes;
	}

	public int getSubfieldIdentifierOne() {
		return subfieldIdentifierOne;
	}

	public int getSubfieldIdentifierTwo() {
		return subfieldIdentifierTwo;
	}

	public byte[] getSubfieldBytes() {
		return subfieldBytes;
	}

	@Override
	public String toString() {
		return "GzipHeaderExtraSubfield [subfieldIdentifierOne=" + subfieldIdentifierOne + ", subfieldIdentifierTwo=" + subfieldIdentifierTwo + ", subfieldBytes=" + Arrays.toString(subfieldBytes)
				+ "]";
	}

}
