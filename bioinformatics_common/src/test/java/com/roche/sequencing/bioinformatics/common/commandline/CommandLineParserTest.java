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

import junit.framework.Assert;

import org.testng.annotations.Test;

public class CommandLineParserTest {

	@Test(groups = { "integration" }, expectedExceptions = IllegalStateException.class)
	public void duplicateArgumentTest() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup("Command Line Usage:");

		group.addOption(new CommandLineOption("fastQFileOne", "fastQFileOne", null, "The first fastq file", true, false));
		group.addOption(new CommandLineOption("fastQFileTwo", "fastQFileTwo", null, "The second fastq file", true, false));
		group.addOption(new CommandLineOption("bamFile", "bamFile", null, "bamfile", true, false));
		group.addOption(new CommandLineOption("flag", "flag", null, "a flag field", false, true));

		CommandLineParser.parseCommandLineWithExceptions(new String[] { "--fastQFileOne", "a", "--fastQFileTwo", "b", "--bamFile", "bam", "--flag", "--flag" }, group);
	}

	@Test(groups = { "integration" })
	public void baseTest() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup("Command Line Usage:");

		group.addOption(new CommandLineOption("fastQFileOne", "fastQFileOne", null, "The first fastq file", true, false));
		group.addOption(new CommandLineOption("fastQFileTwo", "fastQFileTwo", null, "The second fastq file", true, false));
		group.addOption(new CommandLineOption("b", "b", 'b', "bamfile", true, false));
		group.addOption(new CommandLineOption("flag", "flag", null, "a flag field", false, true));

		Assert.assertFalse(group.getUsage().isEmpty());

		ParsedCommandLine parsedCommandLine = CommandLineParser.parseCommandLineWithExceptions(new String[] { "--fastQFileOne", "a", "--fastQFileTwo", "b", "-b", "bam", "--flag" }, group);
		Assert.assertEquals(parsedCommandLine.getDuplicateArguments().size(), 0);
		Assert.assertEquals(parsedCommandLine.getMissingRequiredOptions().length, 0);
		Assert.assertEquals(parsedCommandLine.getNonOptionArguments().length, 0);
		Assert.assertEquals(parsedCommandLine.getUnrecognizedLongFormOption().size(), 0);
		Assert.assertEquals(parsedCommandLine.getUnrecognizedShortFormOptions().size(), 0);

	}

	@Test(groups = { "integration" }, expectedExceptions = { IllegalStateException.class })
	public void unrecognizedLongTest() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup("Command Line Usage:");

		group.addOption(new CommandLineOption("b", "b", 'b', "bamfile", true, false));
		group.addOption(new CommandLineOption("flag", "flag", null, "a flag field", false, true));

		CommandLineParser.parseCommandLineWithExceptions(new String[] { "--fastQFileOne", "a", "--fastQFileTwo", "b", "-b", "bam", "--flag", "--unrecognized" }, group);
	}

	@Test(groups = { "integration" }, expectedExceptions = { IllegalStateException.class })
	public void unrecognizedShortTest() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup("Command Line Usage:");

		group.addOption(new CommandLineOption("b", "b", 'b', "bamfile", true, false));
		group.addOption(new CommandLineOption("flag", "flag", null, "a flag field", false, true));

		CommandLineParser.parseCommandLineWithExceptions(new String[] { "--fastQFileOne", "a", "--fastQFileTwo", "b", "-b", "bam", "--flag", "-u" }, group);
	}

	@Test(groups = { "integration" }, expectedExceptions = { IllegalStateException.class })
	public void passedArgumentToNonOptionTest() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup("Command Line Usage:");

		group.addOption(new CommandLineOption("flag", "flag", null, "a flag field", false, true));
		group.addOption(new CommandLineOption("flag2", "flag2", null, "a flag field", false, true));
		group.addOption(new CommandLineOption("flag3", "flag3", null, "a flag field", false, true));

		CommandLineParser.parseCommandLineWithExceptions(new String[] { "--flag", "a", "--flag2", "--flag3", }, group);
	}

	@Test(groups = { "integration" })
	public void nonOptionBaseTest() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup("Command Line Usage:");

		group.addOption(new CommandLineOption("flag", "flag", null, "a flag field", false, true));
		group.addOption(new CommandLineOption("flag2", "flag2", null, "a flag field", false, true));
		group.addOption(new CommandLineOption("flag3", "flag3", null, "a flag field", false, true));

		CommandLineParser.parseCommandLineWithExceptions(new String[] { "--flag", "--flag2", "--flag3" }, group);
	}

	@Test(groups = { "integration" }, expectedExceptions = { IllegalStateException.class })
	public void missingArgumentOnOptionTest() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup("Command Line Usage:");

		group.addOption(new CommandLineOption("option", "option", null, "an option field", true, false));

		CommandLineParser.parseCommandLineWithExceptions(new String[] { "--option" }, group);
	}

	@Test(groups = { "integration" }, expectedExceptions = { IllegalStateException.class })
	public void missingRequiredOptionTest() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup("Command Line Usage:");

		group.addOption(new CommandLineOption("fastQFileOne", "fastQFileOne", null, "The first fastq file", true, false));
		group.addOption(new CommandLineOption("fastQFileTwo", "fastQFileTwo", null, "The second fastq file", true, false));
		group.addOption(new CommandLineOption("b", "b", 'b', "bamfile", true, false));
		group.addOption(new CommandLineOption("flag", "flag", null, "a flag field", false, true));

		CommandLineParser.parseCommandLineWithExceptions(new String[] { "--fastQFileTwo", "b", "-b", "bam", "--flag" }, group);
	}

	@Test(groups = { "integration" })
	public void multipleShortOptionsTest() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup("Command Line Usage:");

		group.addOption(new CommandLineOption("a", "a", 'a', "a", true, true));
		group.addOption(new CommandLineOption("b", "b", 'b', "b", true, true));
		group.addOption(new CommandLineOption("c", "c", 'c', "c", true, true));

		CommandLineParser.parseCommandLineWithExceptions(new String[] { "-abc" }, group);
	}

	@Test(groups = { "integration" })
	public void missingOneOfMultipleShortOptionsTest() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup("Command Line Usage:");

		group.addOption(new CommandLineOption("a", "a", 'a', "a", true, true));
		group.addOption(new CommandLineOption("b", "b", 'b', "b", true, true));
		group.addOption(new CommandLineOption("c", "c", 'c', "c", true, true));

		ParsedCommandLine parsedCommandLine = CommandLineParser.parseCommandLine(new String[] { "-ac" }, group);
		Assert.assertEquals(parsedCommandLine.getMissingRequiredOptions().length, 1);
	}

	@Test(groups = { "integration" }, expectedExceptions = { IllegalStateException.class })
	public void duplicateShortOptionsTest() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup("Command Line Usage:");

		group.addOption(new CommandLineOption("a", "a1", 'a', "a", true, true));
		group.addOption(new CommandLineOption("a", "a2", 'a', "a", false, true));
	}

	@Test(groups = { "integration" }, expectedExceptions = { IllegalStateException.class })
	public void duplicateLongOptionsTest() {
		CommandLineOptionsGroup group = new CommandLineOptionsGroup("Command Line Usage:");

		group.addOption(new CommandLineOption("a", "ab", 'a', "a", true, true));
		group.addOption(new CommandLineOption("b", "ab", 'b', "a", false, true));
	}

}
