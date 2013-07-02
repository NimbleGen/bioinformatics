package com.roche.sequencing.bioinformatics.common.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ArraysUtilTest {

	@Test(groups = { "unit" })
	public void testMin() {
		Assert.assertEquals(ArraysUtil.min(-1, 5, 10, 10000), -1);
	}

	@Test(groups = { "unit" })
	public void testMax() {
		Assert.assertEquals(ArraysUtil.max(-1, 5, 10, 10000), 10000);
	}

	@Test(groups = { "unit" })
	public void testConvertToDoubleArray() {
		double[] doubleValues = new double[] { -1, 5, 10, 10000 };
		int[] intValues = new int[] { -1, 5, 10, 10000 };
		Assert.assertEquals(ArraysUtil.convertToDoubleArray(intValues), doubleValues);
	}

}
