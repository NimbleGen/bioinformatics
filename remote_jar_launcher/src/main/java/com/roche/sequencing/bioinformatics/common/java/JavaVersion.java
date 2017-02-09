package com.roche.sequencing.bioinformatics.common.java;

public class JavaVersion {
	private final int majorVersion;
	private final int minorVersion;

	public JavaVersion(int majorVersion, int minorVersion) {
		super();
		this.majorVersion = majorVersion;
		this.minorVersion = minorVersion;
	}

	public int getMajorVersion() {
		return majorVersion;
	}

	public int getMinorVersion() {
		return minorVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + majorVersion;
		result = prime * result + minorVersion;
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
		JavaVersion other = (JavaVersion) obj;
		if (majorVersion != other.majorVersion)
			return false;
		if (minorVersion != other.minorVersion)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return majorVersion + "update" + minorVersion;
	}

}
