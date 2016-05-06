package com.roche.sequencing.bioinformatics.common.utils;

public class Version implements Comparable<Version> {

	private final int majorVersion;
	private final int minorVersion;
	private final int microVersion;
	private final int updateNumber;
	private final String identifier;

	Version(int majorVersion, int minorVersion, int microVersion, int updateNumber, String identifier) {
		super();
		this.majorVersion = majorVersion;
		this.minorVersion = minorVersion;
		this.microVersion = microVersion;
		this.updateNumber = updateNumber;
		this.identifier = identifier;
	}

	public int getMajorVersion() {
		return majorVersion;
	}

	public int getMinorVersion() {
		return minorVersion;
	}

	public int getMicroVersion() {
		return microVersion;
	}

	public int getUpdateNumber() {
		return updateNumber;
	}

	public String getIdentifier() {
		return identifier;
	}

	@Override
	public String toString() {
		String versionAsString = majorVersion + "." + minorVersion + "." + microVersion;
		if (updateNumber != 0) {
			versionAsString += "_" + updateNumber;
		}
		if (identifier != null && !identifier.isEmpty()) {
			versionAsString += "-" + identifier;
		}
		return versionAsString;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
		result = prime * result + majorVersion;
		result = prime * result + microVersion;
		result = prime * result + minorVersion;
		result = prime * result + updateNumber;
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
		Version other = (Version) obj;
		if (identifier == null) {
			if (other.identifier != null)
				return false;
		} else if (!identifier.equals(other.identifier))
			return false;
		if (majorVersion != other.majorVersion)
			return false;
		if (microVersion != other.microVersion)
			return false;
		if (minorVersion != other.minorVersion)
			return false;
		if (updateNumber != other.updateNumber)
			return false;
		return true;
	}

	public static Version fromString(String versionAsString) {
		int majorVersion = 0;
		int minorVersion = 0;
		int microVersion = 0;
		int updateNumber = 0;
		String identifier = "";
		try {
			String[] split = versionAsString.split("\\.");

			if (split.length != 3 && split.length != 2) {
				throw new IllegalArgumentException("Unrecognized format for provided version string[" + versionAsString
						+ "].  Expect format is majorVersion.minorVersion.microVersion_updateNumber-identifier.");
			}

			majorVersion = Integer.parseInt(split[0]);
			minorVersion = Integer.parseInt(split[1]);

			if (split.length == 3) {
				String[] split2 = split[2].split("_");
				if (split2.length == 1) {
					String[] split3 = split2[0].split("-");
					microVersion = Integer.parseInt(split3[0]);
					if (split3.length == 2) {
						identifier = split3[1];
					}
				} else if (split2.length == 2) {
					microVersion = Integer.parseInt(split2[0]);

					String[] split3 = split2[1].split("-");
					updateNumber = Integer.parseInt(split3[0]);
					if (split3.length == 2) {
						identifier = split3[1];
					}
				} else {
					throw new IllegalArgumentException("Unrecognized format for provided version string[" + versionAsString
							+ "].  Expect format is majorVersion.minorVersion.microVersion_updateNumber-identifier.");
				}
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Unrecognized format for provided version string[" + versionAsString
					+ "].  Expect format is majorVersion.minorVersion.microVersion_updateNumber-identifier.");
		}

		return new Version(majorVersion, minorVersion, microVersion, updateNumber, identifier);
	}

	@Override
	public int compareTo(Version o) {
		int result = 1;
		if (o != null) {
			result = Integer.compare(majorVersion, o.majorVersion);
			if (result == 0) {
				result = Integer.compare(minorVersion, o.minorVersion);
			}
			if (result == 0) {
				result = Integer.compare(microVersion, o.microVersion);
			}
			if (result == 0) {
				result = Integer.compare(updateNumber, o.updateNumber);
			}
			if (result == 0) {
				result = identifier.compareTo(o.identifier);
			}
		}
		return result;
	}
}
