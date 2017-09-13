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

import java.io.File;

import com.roche.heatseq.process.FastqReadTrimmer.ProbeTrimmingInformation;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ParsedProbeFile;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ProbeFileUtil.ProbeHeaderInformation;

/**
 * 
 * Simple Object for holding onto Various Settings used by the heat seq application
 * 
 */
public class ApplicationSettings {

	private final File probeFile;
	private final ParsedProbeFile parsedProbeFile;
	private final File bamFile;
	private final File fastQ1File;
	private final File fastQ2File;
	private final File outputDirectory;
	private final File tempDirectory;
	private final String outputBamFileName;
	private final String outputFilePrefix;
	private final String originalBamFileName;
	private final boolean shouldOutputReports;
	private final boolean shouldExcludeProgramInBamHeader;
	private final String commandLineSignature;
	private final String programName;
	private final String programVersion;
	private final int numProcessors;
	private final boolean allowVariableLengthUids;
	private final IAlignmentScorer alignmentScorer;
	private final int extensionUidLength;
	private final int ligationUidLength;
	private final boolean markDuplicates;
	private final boolean keepDuplicates;
	private final boolean mergePairs;
	private final boolean useStrictReadToProbeMatching;
	private final ProbeHeaderInformation probeHeaderInformation;
	private final boolean readsNotTrimmed;
	private final String sampleName;
	private final int numberOfRecordsInFastq;
	private final ProbeTrimmingInformation probeTrimmingInformation;

	/**
	 * Default Constructor
	 * 
	 * @param probeFile
	 * @param bamFile
	 * @param bamFileIndex
	 * @param fastQ1File
	 * @param fastQ2File
	 * @param outputDirectory
	 * @param outputFilePrefix
	 * @param originalBamFileName
	 * @param shouldOutputReports
	 * @param shouldOutputFastq
	 * @param shouldExtendReads
	 * @param commandLineSignature
	 * @param programName
	 * @param programVersion
	 * @param numProcessors
	 */
	public ApplicationSettings(File probeFile, ParsedProbeFile parsedProbeFile, File bamFile, File fastQ1File, File fastQ2File, File outputDirectory, File tempDirectory, String outputBamFileName,
			String outputFilePrefix, String originalBamFileName, boolean shouldOutputReports, boolean shouldExcludeProgramInBamHeader, String commandLineSignature, String programName,
			String programVersion, int numProcessors, boolean allowVariableLengthUids, IAlignmentScorer alignmentScorer, int extensionUidLength, int ligationUidLength, boolean markDuplicates,
			boolean keepDuplicates, boolean mergePairs, boolean useStrictReadToProbeMatching, ProbeHeaderInformation probeHeaderInformation, boolean readsNotTrimmed, String sampleName,
			int numberOfRecordsInFastq, ProbeTrimmingInformation probeTrimmingInformation) {

		super();
		this.probeFile = probeFile;
		this.parsedProbeFile = parsedProbeFile;
		this.bamFile = bamFile;
		this.fastQ1File = fastQ1File;
		this.fastQ2File = fastQ2File;
		this.outputDirectory = outputDirectory;
		this.tempDirectory = tempDirectory;
		this.outputBamFileName = outputBamFileName;
		this.outputFilePrefix = outputFilePrefix;
		this.originalBamFileName = originalBamFileName;
		this.shouldOutputReports = shouldOutputReports;
		this.shouldExcludeProgramInBamHeader = shouldExcludeProgramInBamHeader;
		this.commandLineSignature = commandLineSignature;
		this.programName = programName;
		this.programVersion = programVersion;
		this.numProcessors = numProcessors;
		this.allowVariableLengthUids = allowVariableLengthUids;
		this.alignmentScorer = alignmentScorer;
		this.extensionUidLength = extensionUidLength;
		this.ligationUidLength = ligationUidLength;
		this.markDuplicates = markDuplicates;
		this.keepDuplicates = keepDuplicates;
		this.mergePairs = mergePairs;
		this.useStrictReadToProbeMatching = useStrictReadToProbeMatching;
		this.probeHeaderInformation = probeHeaderInformation;
		this.readsNotTrimmed = readsNotTrimmed;
		this.sampleName = sampleName;
		this.numberOfRecordsInFastq = numberOfRecordsInFastq;
		this.probeTrimmingInformation = probeTrimmingInformation;
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
	 * @return fastQ1File
	 */
	public File getFastQ1File() {
		return fastQ1File;
	}

	/**
	 * @return fastQ2File
	 */
	public File getFastQ2File() {
		return fastQ2File;
	}

	/**
	 * @return useStrictReadToProbeMatching
	 */
	public boolean isUseStrictReadToProbeMatching() {
		return useStrictReadToProbeMatching;
	}

	/**
	 * @return outputDirectory
	 */
	public File getOutputDirectory() {
		return outputDirectory;
	}

	/**
	 * @return tempDirectory
	 */
	public File getTempDirectory() {
		return tempDirectory;
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
	public boolean isShouldOutputReports() {
		return shouldOutputReports;
	}

	/**
	 * @return true if the program entry should not be included in the output bam file.
	 */
	public boolean isShouldExcludeProgramInBamHeader() {
		return shouldExcludeProgramInBamHeader;
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
	 * @return extension uid length as set by the user
	 */
	public int getExtensionUidLength() {
		return extensionUidLength;
	}

	/**
	 * @return ligation uid length as set by the user
	 */
	public int getLigationUidLength() {
		return ligationUidLength;
	}

	/**
	 * @return true if duplicates should be marked in the bam file opposed to removed
	 */
	public boolean isMarkDuplicates() {
		return markDuplicates;
	}

	/**
	 * @return true if duplicates should be kept in the bam file opposed to removed or marked
	 */
	public boolean isKeepDuplicates() {
		return keepDuplicates;
	}

	/**
	 * @return true if the pairs should be merged
	 */
	public boolean isMergePairs() {
		return mergePairs;
	}

	public ProbeHeaderInformation getProbeHeaderInformation() {
		return probeHeaderInformation;
	}

	public boolean isReadsNotTrimmed() {
		return readsNotTrimmed;
	}

	public String getSampleName() {
		return sampleName;
	}

	public ParsedProbeFile getParsedProbeFile() {
		return parsedProbeFile;
	}

	public int getNumberOfRecordsInFastq() {
		return numberOfRecordsInFastq;
	}

	public ProbeTrimmingInformation getProbeTrimmingInformation() {
		return probeTrimmingInformation;
	}

}
