package com.roche.sequencing.bioinformatics.common.statistics;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RunningStatsTest {

	@Test(groups = { "unit" })
	public void meanAndStandardDeviationTest() {
		double[] values = new double[] { 1, 2, 3, 4, 5 };
		RunningStats runningMeanAndVariance = new RunningStats();
		runningMeanAndVariance.addAllValues(values);

		Assert.assertEquals(runningMeanAndVariance.getCurrentMean(), 3.0);
		Assert.assertEquals(runningMeanAndVariance.getCurrentVariance(), 2.5);
	}

}
