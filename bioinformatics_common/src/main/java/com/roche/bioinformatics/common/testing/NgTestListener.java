package com.roche.bioinformatics.common.testing;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.TestListenerAdapter;

public class NgTestListener extends TestListenerAdapter {

	private final static Map<Class<?>, ITestContext> classToContextMap = new ConcurrentHashMap<Class<?>, ITestContext>();

	public static ITestContext getTestConext(Class<?> testClass) {
		return classToContextMap.get(testClass);
	}

	public static boolean hasFailedTests(Class<?> testClass) {
		ITestContext testContext = getTestConext(testClass);
		boolean hasFailedTests = false;
		if (testContext != null) {
			hasFailedTests = testContext.getFailedTests().size() > 0;
		}
		return hasFailedTests;
	}

	@Override
	public void onStart(ITestContext testContext) {
		Set<Class<?>> testClasses = new HashSet<Class<?>>();
		for (ITestNGMethod method : testContext.getAllTestMethods()) {
			testClasses.add(method.getRealClass());
		}
		if (testClasses.size() > 1) {
			throw new AssertionError();
		}
		if (testClasses.size() == 1) {
			Class<?> testClass = testClasses.iterator().next();
			classToContextMap.put(testClass, testContext);
		}
	}
}
