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

package com.roche.heatseq.objects;

import java.io.File;

import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;

/**
 * 
 * Simple Object for holding onto Various Settings used by the heat seq application
 * 
 */
public class ApplicationSettings {

	private final File probeFile;
	private final File bamFile;
	private final File bamFileIndex;
	private final File fastQ1WithUidsFile;
	private final File fastQ2File;
	private final File outputDirectory;
	private final String outputBamFileName;
	private final String outputFilePrefix;
	private final String originalBamFileName;
	private final boolean shouldOutputQualityReports;
	private final boolean shouldOutputFastq;
	private final String commandLineSignature;
	private final String programName;
	private final String programVersion;
	private final int numProcessors;
	private final boolean allowVariableLengthUids;
	private final IAlignmentScorer alignmentScorer;
	private final boolean notTrimmedToWithinTheCaptureTargetSequence;
	private final int uidLength;

	/**
	 * Default Constructor
	 * 
	 * @param probeFile
	 * @param bamFile
	 * @param bamFileIndex
	 * @param fastQ1WithUidsFile
	 * @param fastQ2File
	 * @param outputDirectory
	 * @param outputFilePrefix
	 * @param originalBamFileName
	 * @param shouldOutputQualityReports
	 * @param shouldOutputFastq
	 * @param shouldExtendReads
	 * @param commandLineSignature
	 * @param programName
	 * @param programVersion
	 * @param numProcessors
	 */
	public ApplicationSettings(File probeFile, File bamFile, File bamFileIndex, File fastQ1WithUidsFile, File fastQ2File, File outputDirectory, String outputBamFileName, String outputFilePrefix,
			String originalBamFileName, boolean shouldOutputQualityReports, boolean shouldOutputFastq, String commandLineSignature, String programName, String programVersion, int numProcessors,
			boolean allowVariableLengthUids, IAlignmentScorer alignmentScorer, boolean notTrimmedToWithinTheCaptureTargetSequence, int uidLength) {
		super();
		this.probeFile = probeFile;
		this.bamFile = bamFile;
		this.bamFileIndex = bamFileIndex;
		this.fastQ1WithUidsFile = fastQ1WithUidsFile;
		this.fastQ2File = fastQ2File;
		this.outputDirectory = outputDirectory;
		this.outputBamFileName = outputBamFileName;
		this.outputFilePrefix = outputFilePrefix;
		this.originalBamFileName = originalBamFileName;
		this.shouldOutputQualityReports = shouldOutputQualityReports;
		this.shouldOutputFastq = shouldOutputFastq;
		this.commandLineSignature = commandLineSignature;
		this.programName = programName;
		this.programVersion = programVersion;
		this.numProcessors = numProcessors;
		this.allowVariableLengthUids = allowVariableLengthUids;
		this.alignmentScorer = alignmentScorer;
		this.notTrimmedToWithinTheCaptureTargetSequence = notTrimmedToWithinTheCaptureTargetSequence;
		this.uidLength = uidLength;
	}

	/**
	 * @return probeFile
	 */
	public File getProbeFile() {
		return probeFile;
	}

	/**
	 * @return bamFile
	 */
	public File getBamFile() {
		return bamFile;
	}

	/**
	 * @return bamFileIndex
	 */
	public File getBamFileIndex() {
		return bamFileIndex;
	}

	/**
	 * @return fastQ1File (contains uids)
	 */
	public File getFastQ1WithUidsFile() {
		return fastQ1WithUidsFile;
	}

	/**
	 * @return fastQ2File
	 */
	public File getFastQ2File() {
		return fastQ2File;
	}

	/**
	 * @return outputDirectory
	 */
	public File getOutputDirectory() {
		return outputDirectory;
	}

	/**
	 * @return outputBamFileName
	 */
	public String getOutputBamFileName() {
		return outputBamFileName;
	}

	/**
	 * @return the prefix to prepend to the output result file
	 */
	public String getOutputFilePrefix() {
		return outputFilePrefix;
	}

	/**
	 * @return the original BAM file name
	 */
	public String getOriginalBamFileName() {
		return originalBamFileName;
	}

	/**
	 * @return true if the outputQualityReports should be created
	 */
	public boolean isShouldOutputQualityReports() {
		return shouldOutputQualityReports;
	}

	/**
	 * @return true if the outputFastQ files should be created
	 */
	public boolean isShouldOutputFastq() {
		return shouldOutputFastq;
	}

	/**
	 * @return the signature of the command line call used to run this application
	 */
	public String getCommandLineSignature() {
		return commandLineSignature;
	}

	/**
	 * @return the program name associated with this application
	 */
	public String getProgramName() {
		return programName;
	}

	/**
	 * @return the version of the program
	 */
	public String getProgramVersion() {
		return programVersion;
	}

	/**
	 * @return the number of processors requested to run this application
	 */
	public int getNumProcessors() {
		return numProcessors;
	}

	/**
	 * @return true if variable length uids are allowed (basically the uid length will be autodetected for each read by aligning to the extension primer and using what is left over as the uid).
	 */
	public boolean isAllowVariableLengthUids() {
		return allowVariableLengthUids;
	}

	/**
	 * @return an alignment scorer with penalties the user has passed in or the default values
	 */
	public IAlignmentScorer getAlignmentScorer() {
		return alignmentScorer;
	}

	/**
	 * @return true if the user has indicated that the reads have not been trimmed to be within the capture target sequence
	 */
	public boolean isNotTrimmedWithinTheCaptureTargetSequence() {
		return notTrimmedToWithinTheCaptureTargetSequence;
	}

	/**
	 * @return uid length as set by the user
	 */
	public int getUidLength() {
		return uidLength;
	}

}
