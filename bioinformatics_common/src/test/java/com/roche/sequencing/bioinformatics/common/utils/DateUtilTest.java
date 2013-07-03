package com.roche.sequencing.bioinformatics.common.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DateUtilTest {

	@Test(groups = { "unit" })
	public void testGetCurrentDate() {
		// just make sure that it doesn't blow up
		String currentDate = DateUtil.getCurrentDateINYYYY_MM_DD_HH_MM_SS();
		Assert.assertFalse(currentDate.isEmpty());
	}

}
