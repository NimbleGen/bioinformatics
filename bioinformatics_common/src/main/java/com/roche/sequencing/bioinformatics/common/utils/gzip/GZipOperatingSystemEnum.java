package com.roche.sequencing.bioinformatics.common.utils.gzip;

public enum GZipOperatingSystemEnum {

	FAT(0), AMIGA(1), VMS(2), UNIX(3), VM_CMS(4), ATARI_TOS(5), HPFS(6), MAC(7), Z_SYSTEM(8), CP_M(9), TOPS_20(10), NTFS(11), QDOS(12), ACORN_RISCOS(13), UNKNOWN(255);

	private final int intValue;

	private GZipOperatingSystemEnum(int intValue) {
		this.intValue = intValue;
	}

	public static GZipOperatingSystemEnum getOperatingSystemEnum(int intValueFromGzip) {
		GZipOperatingSystemEnum osEnum = UNKNOWN;
		enumLoop: for (GZipOperatingSystemEnum currentEnum : GZipOperatingSystemEnum.values()) {
			if (currentEnum.intValue == intValueFromGzip) {
				osEnum = currentEnum;
				break enumLoop;
			}
		}
		return osEnum;
	}

}
