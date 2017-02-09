package com.roche.sequencing.bioinformatics.common.java;

public enum JvmBitDepthEnum {
	BIT_DEPTH_32(32), BIT_DEPTH_64(64);

	private final int bitDepth;

	private JvmBitDepthEnum(int bitDepth) {
		this.bitDepth = bitDepth;
	}

	public boolean isSufficientToHandleProvidedBitDepth(Integer bitDepth) {
		boolean isSufficient = true;
		if (bitDepth != null) {
			isSufficient = bitDepth >= this.bitDepth;
		}
		return isSufficient;
	}
}
