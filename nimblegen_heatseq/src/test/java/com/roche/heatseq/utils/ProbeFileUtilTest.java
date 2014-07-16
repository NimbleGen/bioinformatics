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

package com.roche.heatseq.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ProbeFileUtilTest {

	@Test(groups = { "unit" })
	public void extractExtensionUidTest() throws FileNotFoundException, URISyntaxException {
		File probeInfoFile = new File(ProbeFileUtilTest.class.getResource("probe_info_with_header.txt").toURI());
		Integer extensionUidLength = ProbeFileUtil.extractExtensionUidLength(probeInfoFile);
		Assert.assertEquals((int) extensionUidLength, 10);
	}

	@Test(groups = { "unit" })
	public void extractLigationUidTest() throws FileNotFoundException, URISyntaxException {
		File probeInfoFile = new File(ProbeFileUtilTest.class.getResource("probe_info_with_header.txt").toURI());
		Integer ligationUidLength = ProbeFileUtil.extractLigationUidLength(probeInfoFile);
		Assert.assertEquals((int) ligationUidLength, 0);
	}

	@Test(groups = { "unit" })
	public void extractGenomeTest() throws FileNotFoundException, URISyntaxException {
		File probeInfoFile = new File(ProbeFileUtilTest.class.getResource("probe_info_with_header.txt").toURI());
		String genome = ProbeFileUtil.extractGenomeNameInLowerCase(probeInfoFile);
		Assert.assertEquals(genome, "hg19");
	}

}
