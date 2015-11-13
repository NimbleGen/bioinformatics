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
package com.roche.heatseq.objects;

import java.util.List;

public class ExtendReadResults {

	private final List<IReadPair> extendedReads;
	private final List<IReadPair> unableToExtendReads;

	public ExtendReadResults(List<IReadPair> extendedReads, List<IReadPair> unableToExtendReads) {
		super();
		this.extendedReads = extendedReads;
		this.unableToExtendReads = unableToExtendReads;
	}

	public List<IReadPair> getExtendedReads() {
		return extendedReads;
	}

	public List<IReadPair> getUnableToExtendReads() {
		return unableToExtendReads;
	}

}
