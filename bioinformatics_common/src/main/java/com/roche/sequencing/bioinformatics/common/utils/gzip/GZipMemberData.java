package com.roche.sequencing.bioinformatics.common.utils.gzip;

import java.time.LocalDateTime;
import java.util.List;

public class GZipMemberData {

	private final boolean fileIsAsciiText;
	private final LocalDateTime originalFileLastModified;
	private final DeflateCompressionEnum deflateCompressionType;
	private final GZipOperatingSystemEnum operatingSystem;
	private final List<GzipHeaderExtraSubfield> subfields;
	private final String originalFileName;
	private final String comment;
	private final Integer cyclicRedundancyCheck16ForHeader;
	private final int compressedMemberHeaderSizeInBytes;

	public GZipMemberData(boolean fileIsAsciiText, LocalDateTime originalFileLastModified, DeflateCompressionEnum deflateCompressionType, GZipOperatingSystemEnum operatingSystem,
			List<GzipHeaderExtraSubfield> subfields, String originalFileName, String comment, Integer cyclicRedundancyCheck16ForHeader, int compressedSizeInBytes) {
		super();
		this.fileIsAsciiText = fileIsAsciiText;
		this.originalFileLastModified = originalFileLastModified;
		this.deflateCompressionType = deflateCompressionType;
		this.operatingSystem = operatingSystem;
		this.subfields = subfields;
		this.originalFileName = originalFileName;
		this.comment = comment;
		this.cyclicRedundancyCheck16ForHeader = cyclicRedundancyCheck16ForHeader;
		this.compressedMemberHeaderSizeInBytes = compressedSizeInBytes;
	}

	public boolean isFileIsAsciiText() {
		return fileIsAsciiText;
	}

	public LocalDateTime getOriginalFileLastModified() {
		return originalFileLastModified;
	}

	public DeflateCompressionEnum getDeflateCompressionType() {
		return deflateCompressionType;
	}

	public GZipOperatingSystemEnum getOperatingSystem() {
		return operatingSystem;
	}

	public List<GzipHeaderExtraSubfield> getSubfields() {
		return subfields;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public String getComment() {
		return comment;
	}

	public Integer getCyclicRedundancyCheck16ForHeader() {
		return cyclicRedundancyCheck16ForHeader;
	}

	public int getCompressedSizeInBytes() {
		return compressedMemberHeaderSizeInBytes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((comment == null) ? 0 : comment.hashCode());
		result = prime * result + compressedMemberHeaderSizeInBytes;
		result = prime * result + ((cyclicRedundancyCheck16ForHeader == null) ? 0 : cyclicRedundancyCheck16ForHeader.hashCode());
		result = prime * result + ((deflateCompressionType == null) ? 0 : deflateCompressionType.hashCode());
		result = prime * result + (fileIsAsciiText ? 1231 : 1237);
		result = prime * result + ((operatingSystem == null) ? 0 : operatingSystem.hashCode());
		result = prime * result + ((originalFileLastModified == null) ? 0 : originalFileLastModified.hashCode());
		result = prime * result + ((originalFileName == null) ? 0 : originalFileName.hashCode());
		result = prime * result + ((subfields == null) ? 0 : subfields.hashCode());
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
		GZipMemberData other = (GZipMemberData) obj;
		if (comment == null) {
			if (other.comment != null)
				return false;
		} else if (!comment.equals(other.comment))
			return false;
		if (compressedMemberHeaderSizeInBytes != other.compressedMemberHeaderSizeInBytes)
			return false;
		if (cyclicRedundancyCheck16ForHeader == null) {
			if (other.cyclicRedundancyCheck16ForHeader != null)
				return false;
		} else if (!cyclicRedundancyCheck16ForHeader.equals(other.cyclicRedundancyCheck16ForHeader))
			return false;
		if (deflateCompressionType != other.deflateCompressionType)
			return false;
		if (fileIsAsciiText != other.fileIsAsciiText)
			return false;
		if (operatingSystem != other.operatingSystem)
			return false;
		if (originalFileLastModified == null) {
			if (other.originalFileLastModified != null)
				return false;
		} else if (!originalFileLastModified.equals(other.originalFileLastModified))
			return false;
		if (originalFileName == null) {
			if (other.originalFileName != null)
				return false;
		} else if (!originalFileName.equals(other.originalFileName))
			return false;
		if (subfields == null) {
			if (other.subfields != null)
				return false;
		} else if (!subfields.equals(other.subfields))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GZipMemberData [fileIsAsciiText=" + fileIsAsciiText + ", originalFileLastModified=" + originalFileLastModified + ", deflateCompressionType=" + deflateCompressionType
				+ ", operatingSystem=" + operatingSystem + ", subfields=" + subfields + ", originalFileName=" + originalFileName + ", comment=" + comment + ", cyclicRedundancyCheck16ForHeader="
				+ cyclicRedundancyCheck16ForHeader + ", compressedMemberHeaderSizeInBytes=" + compressedMemberHeaderSizeInBytes + "]";
	}

}
