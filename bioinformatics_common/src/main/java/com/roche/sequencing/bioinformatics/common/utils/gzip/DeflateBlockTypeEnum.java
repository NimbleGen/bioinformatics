package com.roche.sequencing.bioinformatics.common.utils.gzip;

public enum DeflateBlockTypeEnum {
	NO_COMPRESSION(0), COMPRESSED_WITH_FIXED_HUFFMAN_CODES(1), COMPRESSED_WITH_DYNAMIC_HUFFMAN_CODES(2), RESERVED(3);

	private final int value;

	private DeflateBlockTypeEnum(int value) {
		this.value = value;
	}

	public static DeflateBlockTypeEnum getBlockType(int value) {
		DeflateBlockTypeEnum blockType = null;

		enumLoop: for (DeflateBlockTypeEnum currentBlockType : DeflateBlockTypeEnum.values()) {
			if (currentBlockType.value == value) {
				blockType = currentBlockType;
				break enumLoop;
			}
		}

		return blockType;
	}

}
