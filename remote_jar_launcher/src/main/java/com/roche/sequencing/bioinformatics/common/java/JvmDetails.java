package com.roche.sequencing.bioinformatics.common.java;

import java.io.File;

public class JvmDetails {
	private final JavaVersion version;
	private final JvmBitDepthEnum jvmBitDepth;
	private final JvmTypeEnum jvmType;
	private final String vendor;
	private final File jvmPath;
	private final File javaExecutablePath;

	public JvmDetails(JavaVersion version, JvmBitDepthEnum jvmBitDepth, JvmTypeEnum jvmType, String vendor, File jvmPath, File javaExecutablePath) {
		super();
		this.version = version;
		this.jvmBitDepth = jvmBitDepth;
		this.jvmType = jvmType;
		this.vendor = vendor;
		this.jvmPath = jvmPath;
		this.javaExecutablePath = javaExecutablePath;
	}

	public JavaVersion getVersion() {
		return version;
	}

	public JvmBitDepthEnum getJvmBitDepth() {
		return jvmBitDepth;
	}

	public JvmTypeEnum getJvmType() {
		return jvmType;
	}

	public String getVendor() {
		return vendor;
	}

	public File getJvmPath() {
		return jvmPath;
	}

	public File getJavaExecutablePath() {
		return javaExecutablePath;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((javaExecutablePath == null) ? 0 : javaExecutablePath.hashCode());
		result = prime * result + ((jvmBitDepth == null) ? 0 : jvmBitDepth.hashCode());
		result = prime * result + ((jvmPath == null) ? 0 : jvmPath.hashCode());
		result = prime * result + ((jvmType == null) ? 0 : jvmType.hashCode());
		result = prime * result + ((vendor == null) ? 0 : vendor.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		JvmDetails other = (JvmDetails) obj;
		if (javaExecutablePath == null) {
			if (other.javaExecutablePath != null)
				return false;
		} else if (!javaExecutablePath.equals(other.javaExecutablePath))
			return false;
		if (jvmBitDepth != other.jvmBitDepth)
			return false;
		if (jvmPath == null) {
			if (other.jvmPath != null)
				return false;
		} else if (!jvmPath.equals(other.jvmPath))
			return false;
		if (jvmType != other.jvmType)
			return false;
		if (vendor == null) {
			if (other.vendor != null)
				return false;
		} else if (!vendor.equals(other.vendor))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "JvmDetails [version=" + version + ", jvmBitDepth=" + jvmBitDepth + ", jvmType=" + jvmType + ", vendor=" + vendor + ", jvmPath=" + jvmPath + ", javaExecutablePath=" + javaExecutablePath
				+ "]";
	}

}
