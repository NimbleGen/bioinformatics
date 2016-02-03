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

package com.roche.sequencing.bioinformatics.common.commandline;

import java.util.ArrayList;
import java.util.List;

class OptionMatchingResults {
	private final List<String> unrecogzniedShortFormOptions;
	private final List<String> unrecogzniedLongFormOptions;
	private final List<CommandLineOption> matchingOptions;

	OptionMatchingResults() {
		unrecogzniedShortFormOptions = new ArrayList<String>();
		unrecogzniedLongFormOptions = new ArrayList<String>();
		matchingOptions = new ArrayList<CommandLineOption>();
	}

	void addUnrecognizedShortFormOption(String unrecognizedOption) {
		unrecogzniedShortFormOptions.add(unrecognizedOption);
	}

	void addUnrecognizedLongFormOption(String unrecognizedOption) {
		unrecogzniedLongFormOptions.add(unrecognizedOption);
	}

	void addMatchingOption(CommandLineOption matchingOption) {
		matchingOptions.add(matchingOption);
	}

	public Iterable<String> getUnrecognizedShortFormOptions() {
		return unrecogzniedShortFormOptions;
	}

	public Iterable<String> getUnrecognizedLongFormOptions() {
		return unrecogzniedLongFormOptions;
	}

	public Iterable<CommandLineOption> getMatchingOptions() {
		return matchingOptions;
	}
}
