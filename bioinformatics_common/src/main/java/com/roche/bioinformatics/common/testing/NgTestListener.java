package com.roche.bioinformatics.common.testing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.testng.IResultMap;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

public class NgTestListener extends TestListenerAdapter {

	private final static Map<Class<?>, ITestContext> classToContextMap = new ConcurrentHashMap<Class<?>, ITestContext>();

	private static ITestContext getTestContext(Class<?> testClass) {
		return classToContextMap.get(testClass);
	}

	public static boolean hasFailedTests(Class<?> testClass) {
		ITestContext testContext = getTestContext(testClass);
		boolean hasFailedTests = false;
		if (testContext != null) {
			IResultMap failedTests = testContext.getFailedTests();
			failedTestLoop: for (ITestResult failedTestResult : failedTests.getAllResults()) {
				if (failedTestResult.getMethod().getRealClass() == testClass) {
					hasFailedTests = true;
					break failedTestLoop;
				}
			}
		}
		return hasFailedTests;
	}

	@Override
	public void onStart(ITestContext testContext) {
		for (ITestNGMethod method : testContext.getAllTestMethods()) {
			classToContextMap.put(method.getRealClass(), testContext);
		}
	}
}
