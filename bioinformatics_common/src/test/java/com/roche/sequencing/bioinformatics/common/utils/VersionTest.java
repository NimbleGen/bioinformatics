package com.roche.sequencing.bioinformatics.common.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

public class VersionTest {

	String[] versionStrings = new String[] { "1.1.0", "1.3.1-beta", "1.3.1_05-ea", "1.8.0_31", "1.7" };
	Version[] versions = new Version[] { new Version(1, 1, 0, 0, ""), new Version(1, 3, 1, 0, "beta"), new Version(1, 3, 1, 5, "ea"), new Version(1, 8, 0, 31, ""), new Version(1, 7, 0, 0, "") };

	@Test(groups = { "unit" })
	public void testVersions() {
		for (int i = 0; i < versionStrings.length; i++) {
			String versionString = versionStrings[i];
			Version version = versions[i];
			Assert.assertEquals(Version.fromString(versionString), version);
		}
	}
}
