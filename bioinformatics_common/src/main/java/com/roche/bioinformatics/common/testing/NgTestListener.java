/*
 *    Copyright 2016 Roche NimbleGen Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
