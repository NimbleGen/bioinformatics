package com.roche.sequencing.bioinformatics.genome;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

public class GenomeIdentifierTest {

	@Test(groups = { "unit" })
	public void testHg19() {
		Map<String, Integer> hg19Sizes = new HashMap<String, Integer>();
		hg19Sizes.put("chr1", 249250621);
		hg19Sizes.put("chr3", 198022430);
		String matchingName = GenomeIdentifier.getMatchingGenomeName(hg19Sizes);
		Assert.assertEquals(matchingName, "hg19");
	}

	@Test(groups = { "unit" })
	public void testHg19_2() {
		Map<String, Integer> hg19Sizes = new HashMap<String, Integer>();
		hg19Sizes.put("chr1", 249250621);
		hg19Sizes.put("nonsense", -11119);
		String matchingName = GenomeIdentifier.getMatchingGenomeName(hg19Sizes);
		Assert.assertNull(matchingName);
	}

}
