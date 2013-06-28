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

package com.roche.sequencing.bioinformatics.common.commandline;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CommandLineParserTest {

	@BeforeClass
	public void setUp() {
	}

	@Test(groups = { "integration" }, expectedExceptions = IllegalStateException.class)
	public void parseCommandLineTest() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup("Command Line Usage:");

		group.addOption(new CommandLineOption("fastQFileOne", "fastQFileOne", null, "The first fastq file", true, false));
		group.addOption(new CommandLineOption("fastQFileTwo", "fastQFileTwo", null, "The second fastq file", true, false));
		group.addOption(new CommandLineOption("bamFile", "bamFile", null, "bamfile", true, false));
		group.addOption(new CommandLineOption("flag", "flag", null, "a flag field", false, true));

		CommandLineParser.parseCommandLineWithExceptions(new String[] { "--fastQFileOne", "a", "--fastQFileTwo", "b", "--bamFile", "bam", "--flag", "--flag" }, group);

	}

}
