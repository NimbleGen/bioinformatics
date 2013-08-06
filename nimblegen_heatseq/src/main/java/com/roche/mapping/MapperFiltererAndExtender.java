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

package com.roche.mapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import net.sf.picard.fastq.FastqReader;
import net.sf.picard.fastq.FastqRecord;
import net.sf.picard.fastq.FastqWriter;
import net.sf.picard.fastq.FastqWriterFactory;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.IReadPair;
import com.roche.heatseq.objects.IlluminaFastQHeader;
import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.objects.ProbesBySequenceName;
import com.roche.heatseq.objects.SAMRecordPair;
import com.roche.heatseq.process.BamFileUtil;
import com.roche.heatseq.process.ExtendReadsToPrimer;
import com.roche.heatseq.process.ProbeFileUtil;
import com.roche.heatseq.qualityreport.DetailsReport;
import com.roche.heatseq.qualityreport.ProbeProcessingStats;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.StatisticsUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * 
 * Class that maps, filters and extends fastq files with a probe info file
 * 
 */
public class MapperFiltererAndExtender {

	private final Logger logger = LoggerFactory.getLogger(MapperFiltererAndExtender.class);

	private final static int DEFAULT_MAPPING_QUALITY = 60;

	private final File fastQOneFile;
	private final File fastQTwoFile;
	private final File probeFile;
	private final File outputFile;
	private PrintWriter ambiguousMappingWriter;
	private PrintWriter probeUidQualityWriter;
	private PrintWriter unableToAlignPrimerWriter;
	private PrintWriter primerAlignmentWriter;
	private DetailsReport detailsReport;

	private FastqWriter fastqOneUnableToMapWriter;
	private FastqWriter fastqTwoUnableToMapWriter;

	private boolean started;
	private final int numProcessors;
	private final int uidLength;
	private final boolean allowVariableLengthUids;
	private final List<SAMRecordPair> samRecordPairs;
	private final Map<ProbeReference, Map<String, Set<QualityScoreAndFastQLineIndex>>> uidAndProbeReferenceToFastQLineMapping;

	private Semaphore mapFilterAndExtendSemaphore;
	private Semaphore mapReadSemaphore;

	private final String programName;
	private final String programVersion;
	private final String commandLineSignature;
	private final IAlignmentScorer alignmentScorer;

	/**
	 * Default constructor
	 * 
	 * @param fastQOneFile
	 * @param fastQTwoFile
	 * @param probeFile
	 * @param outputFile
	 * @param ambiguousMappingFile
	 * @param numProcessors
	 * @param uidLength
	 */
	public MapperFiltererAndExtender(File fastQOneFile, File fastQTwoFile, File probeFile, File outputFile, File ambiguousMappingFile, File probeUidQualityFile, File unableToAlignPrimerFile,
			File fastqOneUnableToMapFile, File fastqTwoUnableToMapFile, File primerAlignmentFile, File detailsReportFile, int numProcessors, int uidLength, boolean allowVariableLengthUids,
			String programName, String programVersion, String commandLineSignature, IAlignmentScorer alignmentScorer) {

		super();
		samRecordPairs = new ArrayList<SAMRecordPair>();
		uidAndProbeReferenceToFastQLineMapping = new ConcurrentHashMap<ProbeReference, Map<String, Set<QualityScoreAndFastQLineIndex>>>();
		this.fastQOneFile = fastQOneFile;
		this.fastQTwoFile = fastQTwoFile;
		this.probeFile = probeFile;
		this.outputFile = outputFile;

		this.alignmentScorer = alignmentScorer;

		if (ambiguousMappingFile != null) {
			try {
				ambiguousMappingWriter = new PrintWriter(ambiguousMappingFile);
				ambiguousMappingWriter.println("readName" + StringUtil.TAB + "readString" + StringUtil.TAB + "sequence_name" + StringUtil.TAB + "extension_primer_start" + StringUtil.TAB
						+ "extension_primer_stop" + StringUtil.TAB + "capture_target_start" + StringUtil.TAB + "capture_target_stop" + StringUtil.TAB + "ligation_primer_start" + StringUtil.TAB
						+ "ligation_primer_stop" + StringUtil.TAB + "probe_strand");
			} catch (FileNotFoundException e) {
				throw new IllegalStateException(e);
			}
		}
		if (probeUidQualityFile != null) {
			try {
				probeUidQualityWriter = new PrintWriter(probeUidQualityFile);
				probeUidQualityWriter.println("probe_id" + StringUtil.TAB + "probe_sequence_name" + StringUtil.TAB + "probe_capture_start" + StringUtil.TAB + "probe_capture_stop" + StringUtil.TAB
						+ "strand" + StringUtil.TAB + "uid" + StringUtil.TAB + "read_one_quality" + StringUtil.TAB + "read_two_quality" + StringUtil.TAB + "total_quality" + StringUtil.TAB
						+ "read_name" + StringUtil.TAB + "read_sequence");
			} catch (FileNotFoundException e) {
				throw new IllegalStateException(e);
			}
		}
		if (unableToAlignPrimerFile != null) {
			try {
				unableToAlignPrimerWriter = new PrintWriter(unableToAlignPrimerFile);
				unableToAlignPrimerWriter.println("sequence_name" + StringUtil.TAB + "probe_start" + StringUtil.TAB + "protbe_stop" + StringUtil.TAB + "extension_primer_sequence" + StringUtil.TAB
						+ "read_name" + StringUtil.TAB + "read_string");
			} catch (FileNotFoundException e) {
				throw new IllegalStateException(e);
			}
		}

		if (primerAlignmentFile != null) {
			try {
				primerAlignmentWriter = new PrintWriter(primerAlignmentFile);
				primerAlignmentWriter.println("uid_length" + StringUtil.TAB + "substituions" + StringUtil.TAB + "insertions" + StringUtil.TAB + "deletions" + StringUtil.TAB + "edit_distance"
						+ StringUtil.TAB + "read" + StringUtil.TAB + "extension_primer" + StringUtil.TAB + "probe_sequence_name" + StringUtil.TAB + "capture_target_start" + StringUtil.TAB
						+ "capture_target_stop" + StringUtil.TAB + "probe_strand");
			} catch (FileNotFoundException e) {
				throw new IllegalStateException(e);
			}
		}

		if (detailsReportFile != null) {
			try {
				detailsReport = new DetailsReport(detailsReportFile);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		final FastqWriterFactory factory = new FastqWriterFactory();
		if (fastqOneUnableToMapFile != null && fastqTwoUnableToMapFile != null) {
			fastqOneUnableToMapWriter = factory.newWriter(fastqOneUnableToMapFile);
			fastqTwoUnableToMapWriter = factory.newWriter(fastqTwoUnableToMapFile);
		}

		started = false;
		this.numProcessors = numProcessors;
		mapFilterAndExtendSemaphore = new Semaphore(numProcessors);
		mapReadSemaphore = new Semaphore(numProcessors);
		this.uidLength = uidLength;
		this.allowVariableLengthUids = allowVariableLengthUids;
		this.programName = programName;
		this.programVersion = programVersion;
		this.commandLineSignature = commandLineSignature;
	}

	private static class MyThreadGroup extends ThreadGroup {
		public MyThreadGroup() {
			super("Mapping-Thread-Group");
		}

		public void uncaughtException(Thread t, Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * map, filter and extend the fastq files passed in to the constructor using the probe info file passed into the constructor.
	 */
	public void mapFilterAndExtend() {
		if (!started) {
			started = true;
			long start = System.currentTimeMillis();
			try {
				ProbesBySequenceName probesBySequenceName = ProbeFileUtil.parseProbeInfoFile(probeFile);
				SubReadProbeMapper probeMapper = new SubReadProbeMapper();
				probeMapper.addProbes(probesBySequenceName);
				SAMFileHeader samHeader = SAMRecordUtil.createSAMFileHeader();

				Integer fastQ1PrimerLength = null;
				Integer fastQ2PrimerLength = null;

				for (String sequenceName : probesBySequenceName.getSequenceNames()) {
					int sequenceLength = 0;
					for (Probe probe : probesBySequenceName.getProbesBySequenceName(sequenceName)) {
						// TODO Kurt Heilman 6/21/2013 pull sequence length from probe info file if we change the format to include sequence length
						sequenceLength = Math.max(sequenceLength, probe.getStop() + 1);
						if (fastQ1PrimerLength == null && fastQ2PrimerLength == null) {
							fastQ1PrimerLength = probe.getExtensionPrimerSequence().size();
							fastQ2PrimerLength = probe.getLigationPrimerSequence().size();
						}
					}
					SAMSequenceRecord sequenceRecord = new SAMSequenceRecord(sequenceName, sequenceLength);
					samHeader.addSequence(sequenceRecord);
				}
				String readGroupName = fastQOneFile.getName() + "_and_" + fastQTwoFile.getName();
				SAMReadGroupRecord readGroup = new SAMReadGroupRecord(readGroupName);
				readGroup.setPlatform("illumina");
				readGroup.setSample(readGroupName);
				samHeader.addReadGroup(readGroup);

				List<SAMProgramRecord> programRecords = new ArrayList<SAMProgramRecord>();
				String uniqueProgramGroupId = programName + "_" + DateUtil.getCurrentDateINYYYY_MM_DD_HH_MM_SS();
				SAMProgramRecord programRecord = new SAMProgramRecord(uniqueProgramGroupId);
				programRecord.setProgramName(programName);
				programRecord.setProgramVersion(programVersion);
				programRecord.setCommandLine(commandLineSignature);
				programRecords.add(programRecord);
				samHeader.setProgramRecords(programRecords);

				final ThreadGroup threadGroup = new MyThreadGroup();
				ExecutorService executor = Executors.newFixedThreadPool(numProcessors, new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						return new Thread(threadGroup, r, "map-to-probe-thread");
					}
				});

				int fastqLineIndex = 0;
				try (FastqReader fastQOneReader = new FastqReader(fastQOneFile)) {
					try (FastqReader fastQTwoReader = new FastqReader(fastQTwoFile)) {

						while (fastQOneReader.hasNext() && fastQTwoReader.hasNext()) {
							FastqRecord recordOne = fastQOneReader.next();
							FastqRecord recordTwo = fastQTwoReader.next();

							MapUidAndProbeTask mapUidAndProbeTask = new MapUidAndProbeTask(recordOne, recordTwo, probeMapper, fastqLineIndex, fastQ1PrimerLength, fastQ2PrimerLength, uidLength,
									allowVariableLengthUids, ambiguousMappingWriter, probeUidQualityWriter, unableToAlignPrimerWriter, fastqOneUnableToMapWriter, fastqTwoUnableToMapWriter,
									primerAlignmentWriter, alignmentScorer);
							try {
								mapFilterAndExtendSemaphore.acquire();
							} catch (InterruptedException e) {
								logger.warn(e.getMessage(), e);
							}
							executor.submit(mapUidAndProbeTask);

							fastqLineIndex++;
						}
					}
				}

				executor.shutdown();
				try {
					executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
				logger.debug("Done mapping fastq reads to probes.");
				// TODO calculate and print out stats

				Map<Integer, ProbeReference> nonFilteredFastQLineIndexes = new HashMap<Integer, ProbeReference>();

				List<ProbeReference> sortedProbeReferences = new ArrayList<ProbeReference>(uidAndProbeReferenceToFastQLineMapping.keySet());
				Collections.sort(sortedProbeReferences, new Comparator<ProbeReference>() {
					@Override
					public int compare(ProbeReference o1, ProbeReference o2) {
						return o1.getProbe().getSequenceName().compareTo(o2.getProbe().getSequenceName());
					}

				});

				// loop by probe
				for (ProbeReference probeReference : sortedProbeReferences) {
					Map<String, Set<QualityScoreAndFastQLineIndex>> uidToQualityScoreAndFastQLineIndexes = uidAndProbeReferenceToFastQLineMapping.get(probeReference);
					long probeProcessingStartTime = System.currentTimeMillis();
					int totalReadPairs = 0;
					int totalReadPairsRemainingAfterReduction = 0;
					int maxNumberOfReadPairsPerUid = 0;
					int minNumberOfReadPairsPerUid = Integer.MAX_VALUE;
					String uidOfEntryWithMaxNumberOfReadPairs = null;

					List<Integer> numberOfReadsPairsPerUid = new ArrayList<Integer>();

					// loop by uid
					for (Entry<String, Set<QualityScoreAndFastQLineIndex>> uidToqualityScoreAndFastQLineIndexesEntry : uidToQualityScoreAndFastQLineIndexes.entrySet()) {
						Set<QualityScoreAndFastQLineIndex> qualityScoreAndFastQLineIndexes = uidToqualityScoreAndFastQLineIndexesEntry.getValue();
						String uid = uidToqualityScoreAndFastQLineIndexesEntry.getKey();

						int numberOfReadPairs = qualityScoreAndFastQLineIndexes.size();
						numberOfReadsPairsPerUid.add(numberOfReadPairs);
						if (numberOfReadPairs > maxNumberOfReadPairsPerUid) {
							maxNumberOfReadPairsPerUid = numberOfReadPairs;
							uidOfEntryWithMaxNumberOfReadPairs = uid;
						}
						minNumberOfReadPairsPerUid = Math.min(minNumberOfReadPairsPerUid, numberOfReadPairs);

						int maxScore = 0;
						int maxScoreFastQLineIndex = -1;
						for (QualityScoreAndFastQLineIndex qualityScoreAndFastQIndex : qualityScoreAndFastQLineIndexes) {
							if (qualityScoreAndFastQIndex.getQualityScore() >= maxScore) {
								maxScore = qualityScoreAndFastQIndex.getQualityScore();
								maxScoreFastQLineIndex = qualityScoreAndFastQIndex.getFastQLineIndex();
							}
							totalReadPairs++;
						}
						totalReadPairsRemainingAfterReduction++;
						nonFilteredFastQLineIndexes.put(maxScoreFastQLineIndex, probeReference);
					}

					int totalDuplicateReadPairsRemoved = totalReadPairs - totalReadPairsRemainingAfterReduction;

					double[] numberOfReadsPairsPerUidArray = new double[numberOfReadsPairsPerUid.size()];
					for (int i = 0; i < numberOfReadsPairsPerUid.size(); i++) {
						numberOfReadsPairsPerUidArray[i] = (double) numberOfReadsPairsPerUid.get(i);
					}

					double averageNumberOfReadPairsPerUid = StatisticsUtil.arithmeticMean(numberOfReadsPairsPerUidArray);
					double standardDeviationOfReadPairsPerUid = StatisticsUtil.standardDeviation(numberOfReadsPairsPerUidArray);

					long probeProcessingStopTime = System.currentTimeMillis();
					int totalTimeToProcessInMs = (int) (probeProcessingStopTime - probeProcessingStartTime);

					if (detailsReport != null) {
						int totalUids = uidToQualityScoreAndFastQLineIndexes.size();
						ProbeProcessingStats probeProcessingStats = new ProbeProcessingStats(probeReference.getProbe(), totalUids, averageNumberOfReadPairsPerUid, standardDeviationOfReadPairsPerUid,
								totalDuplicateReadPairsRemoved, totalReadPairsRemainingAfterReduction, minNumberOfReadPairsPerUid, maxNumberOfReadPairsPerUid, uidOfEntryWithMaxNumberOfReadPairs,
								totalTimeToProcessInMs);
						detailsReport.writeEntry(probeProcessingStats);
					}
				}

				executor = Executors.newFixedThreadPool(numProcessors, new ThreadFactory() {

					@Override
					public Thread newThread(Runnable r) {
						return new Thread(threadGroup, r, "align_mapping-thread");
					}
				});

				fastqLineIndex = 0;
				try (FastqReader fastQOneReader = new FastqReader(fastQOneFile)) {
					try (FastqReader fastQTwoReader = new FastqReader(fastQTwoFile)) {
						while (fastQOneReader.hasNext() && fastQTwoReader.hasNext()) {
							FastqRecord recordOne = fastQOneReader.next();
							FastqRecord recordTwo = fastQTwoReader.next();
							if (nonFilteredFastQLineIndexes.containsKey(fastqLineIndex)) {

								ProbeReference probeReference = nonFilteredFastQLineIndexes.get(fastqLineIndex);

								MapReadTask mapReadTask = new MapReadTask(recordOne, recordTwo, probeReference, samHeader, readGroupName, alignmentScorer);
								try {
									mapReadSemaphore.acquire();
								} catch (InterruptedException e) {
									logger.warn(e.getMessage(), e);
								}
								executor.submit(mapReadTask);
							}
							fastqLineIndex++;
						}
					}
				}
				executor.shutdown();
				try {
					executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
				SAMRecordUtil.createBamFile(samHeader, outputFile, samRecordPairs);

				// Create the index for the SAM file
				BamFileUtil.createIndex(outputFile);

			} catch (IOException e) {
				e.printStackTrace();
			}
			long end = System.currentTimeMillis();

			if (ambiguousMappingWriter != null) {
				ambiguousMappingWriter.close();
			}

			if (probeUidQualityWriter != null) {
				probeUidQualityWriter.close();
			}
			if (unableToAlignPrimerWriter != null) {
				unableToAlignPrimerWriter.close();
			}
			if (primerAlignmentWriter != null) {
				primerAlignmentWriter.close();
			}
			if (fastqOneUnableToMapWriter != null) {
				fastqOneUnableToMapWriter.close();
			}
			if (fastqTwoUnableToMapWriter != null) {
				fastqTwoUnableToMapWriter.close();
			}
			logger.debug("Total time: " + DateUtil.convertMillisecondsToHHMMSS(end - start));
		}
	}

	private synchronized void addToSamRecordPairs(SAMRecordPair pair) {
		samRecordPairs.add(pair);
	}

	private class MapUidAndProbeTask implements Runnable {
		private final Logger logger = LoggerFactory.getLogger(MapUidAndProbeTask.class);

		private final FastqRecord recordOne;
		private final FastqRecord recordTwo;
		private final SubReadProbeMapper probeMapper;
		private final int fastqLineIndex;
		private final int fastQOnePrimerLength;
		private final int fastQTwoPrimerLength;
		private final int uidLength;
		private final boolean allowVariableLengthUids;
		private final PrintWriter ambiguousMappingWriter;
		private final PrintWriter probeUidQualityWriter;
		private final PrintWriter unableToAlignPrimerWriter;
		private final FastqWriter fastqOneUnableToMapWriter;
		private final FastqWriter fastqTwoUnableToMapWriter;
		private final PrintWriter primerAlignmentWriter;
		private final IAlignmentScorer alignmentScorer;

		public MapUidAndProbeTask(FastqRecord recordOne, FastqRecord recordTwo, SubReadProbeMapper probeMapper, int fastqLineIndex, int fastQOnePrimerLength, int fastQTwoPrimerLength, int uidLength,
				boolean allowVariableLengthUids, PrintWriter ambiguousMappingWriter, PrintWriter probeUidQualityWriter, PrintWriter unableToAlignPrimerWriter, FastqWriter fastqOneUnableToMapWriter,
				FastqWriter fastqTwoUnableToMapWriter, PrintWriter primerAlignmentWriter, IAlignmentScorer alignmentScorer) {
			super();
			this.recordOne = recordOne;
			this.recordTwo = recordTwo;
			this.probeMapper = probeMapper;
			this.fastqLineIndex = fastqLineIndex;
			this.fastQOnePrimerLength = fastQOnePrimerLength;
			this.fastQTwoPrimerLength = fastQTwoPrimerLength;
			this.uidLength = uidLength;
			this.allowVariableLengthUids = allowVariableLengthUids;
			this.ambiguousMappingWriter = ambiguousMappingWriter;
			this.probeUidQualityWriter = probeUidQualityWriter;
			this.unableToAlignPrimerWriter = unableToAlignPrimerWriter;
			this.primerAlignmentWriter = primerAlignmentWriter;
			this.fastqOneUnableToMapWriter = fastqOneUnableToMapWriter;
			this.fastqTwoUnableToMapWriter = fastqTwoUnableToMapWriter;
			this.alignmentScorer = alignmentScorer;
		}

		@Override
		public void run() {
			try {
				String uid = SAMRecordUtil.parseUidFromRead(recordOne.getReadString(), uidLength);
				ISequence queryOneSequence = new IupacNucleotideCodeSequence(SAMRecordUtil.removeUidFromRead(recordOne.getReadString(), uidLength));
				queryOneSequence = queryOneSequence.subSequence(fastQOnePrimerLength, queryOneSequence.size() - 1);
				String recordOneQualityString = SAMRecordUtil.removeUidFromRead(recordOne.getBaseQualityString(), uidLength);
				recordOneQualityString = recordOneQualityString.substring(fastQOnePrimerLength, recordOneQualityString.length());

				ISequence queryTwoSequence = new IupacNucleotideCodeSequence(recordTwo.getReadString());
				queryTwoSequence = queryTwoSequence.subSequence(0, (queryTwoSequence.size() - 1) - fastQTwoPrimerLength);
				String recordTwoQualityString = recordTwo.getBaseQualityString();
				recordTwoQualityString = recordTwoQualityString.substring(0, recordTwoQualityString.length() - fastQTwoPrimerLength);

				if (queryOneSequence.size() != recordOneQualityString.length()) {
					throw new IllegalStateException("query one sequence length[" + queryOneSequence.size() + "] does not equal record one quality length[" + recordOneQualityString.length() + "].");
				}

				if (queryTwoSequence.size() != recordTwoQualityString.length()) {
					throw new IllegalStateException("query two sequence length[" + queryTwoSequence.size() + "] does not equal record two quality length[" + recordTwoQualityString.length() + "].");
				}

				if ((queryOneSequence.size() > 0) && (queryTwoSequence.size() > 0)) {

					Set<ProbeReference> oneBestCandidates = probeMapper.getBestCandidates(queryOneSequence);
					Set<ProbeReference> twoBestCandidates = probeMapper.getBestCandidates(queryTwoSequence);

					List<ProbeReference> matchingProbes = new ArrayList<ProbeReference>();

					for (ProbeReference queryOneProbeReference : oneBestCandidates) {
						for (ProbeReference queryTwoProbeReference : twoBestCandidates) {
							boolean candidatesMatch = queryOneProbeReference.getProbe().equals(queryTwoProbeReference.getProbe());
							boolean candidateOneStrandMatchesProbeStrand = queryOneProbeReference.getProbeStrand() == queryOneProbeReference.getProbe().getProbeStrand();
							boolean candidatesOnOppositeStrand = queryOneProbeReference.getProbeStrand() != queryTwoProbeReference.getProbeStrand();
							if (candidatesMatch && candidateOneStrandMatchesProbeStrand && candidatesOnOppositeStrand) {
								matchingProbes.add(queryOneProbeReference);
							}
						}
					}

					if (matchingProbes.size() == 1) {
						int sequenceOneQualityScore = BamFileUtil.getQualityScore(recordOneQualityString);
						int sequenceTwoQualityScore = BamFileUtil.getQualityScore(recordTwoQualityString);
						int qualityScore = sequenceOneQualityScore + sequenceTwoQualityScore;

						ProbeReference matchingProbeReference = matchingProbes.get(0);
						Probe matchingProbe = matchingProbeReference.getProbe();

						if (allowVariableLengthUids) {
							// now that we have a probe we can verify that the uid length is correct
							ISequence extensionPrimerSequence = matchingProbe.getExtensionPrimerSequence();
							String completeReadWithUid = recordOne.getReadString();
							uid = SAMRecordUtil.getVariableLengthUid(completeReadWithUid, extensionPrimerSequence, primerAlignmentWriter, matchingProbe, alignmentScorer);

							// the discovered uid length is not equivalent to the provided length so reset the sequence and quality string
							if (uid.length() != uidLength) {
								queryOneSequence = new IupacNucleotideCodeSequence(SAMRecordUtil.removeUidFromRead(recordOne.getReadString(), uid.length()));
								queryOneSequence = queryOneSequence.subSequence(fastQOnePrimerLength, queryOneSequence.size() - 1);
								recordOneQualityString = SAMRecordUtil.removeUidFromRead(recordOne.getBaseQualityString(), uid.length());
								recordOneQualityString = recordOneQualityString.substring(fastQOnePrimerLength, recordOneQualityString.length());
							}
						}

						if (uid != null) {
							Map<String, Set<QualityScoreAndFastQLineIndex>> uidToFastQLineMapping = uidAndProbeReferenceToFastQLineMapping.get(matchingProbeReference);
							if (uidToFastQLineMapping == null) {
								uidToFastQLineMapping = new ConcurrentHashMap<String, Set<QualityScoreAndFastQLineIndex>>();
							}
							Set<QualityScoreAndFastQLineIndex> set = uidToFastQLineMapping.get(uid);
							if (set == null) {
								set = Collections.newSetFromMap(new ConcurrentHashMap<QualityScoreAndFastQLineIndex, Boolean>());
							}
							set.add(new QualityScoreAndFastQLineIndex(qualityScore, fastqLineIndex));
							uidToFastQLineMapping.put(uid, set);
							uidAndProbeReferenceToFastQLineMapping.put(matchingProbeReference, uidToFastQLineMapping);

							if (probeUidQualityWriter != null) {

								int probeIndex = matchingProbe.getIndex();
								String probeCaptureStart = "" + matchingProbe.getCaptureTargetStart();
								String probeCaptureStop = "" + matchingProbe.getCaptureTargetStop();
								String probeStrand = matchingProbe.getProbeStrand().toString();
								String readSequence = queryOneSequence.toString();

								String readName = recordOne.getReadHeader();

								probeUidQualityWriter.println(probeIndex + StringUtil.TAB + matchingProbe.getSequenceName() + StringUtil.TAB + probeCaptureStart + StringUtil.TAB + probeCaptureStop
										+ StringUtil.TAB + probeStrand + StringUtil.TAB + uid.toUpperCase() + StringUtil.TAB + sequenceOneQualityScore + StringUtil.TAB + sequenceTwoQualityScore
										+ StringUtil.TAB + qualityScore + StringUtil.TAB + readName + StringUtil.TAB + readSequence);
							}

						} else {
							if (unableToAlignPrimerWriter != null) {
								unableToAlignPrimerWriter.println(matchingProbe.getSequenceName() + StringUtil.TAB + matchingProbe.getStart() + StringUtil.TAB + matchingProbe.getStop()
										+ matchingProbe.getExtensionPrimerSequence() + StringUtil.TAB + recordOne.getReadHeader() + StringUtil.TAB + recordOne.getReadString());
							}
						}
					} else if ((matchingProbes.size() > 1) && (ambiguousMappingWriter != null)) {
						for (ProbeReference matchingProbe : matchingProbes) {
							Probe probe = matchingProbe.getProbe();
							ambiguousMappingWriter.println(recordOne.getReadHeader() + StringUtil.TAB + recordOne.getReadString() + StringUtil.TAB + probe.getSequenceName() + StringUtil.TAB
									+ probe.getExtensionPrimerStart() + StringUtil.TAB + probe.getExtensionPrimerStop() + StringUtil.TAB + probe.getCaptureTargetStart() + StringUtil.TAB
									+ probe.getCaptureTargetStop() + StringUtil.TAB + probe.getLigationPrimerStart() + StringUtil.TAB + probe.getLigationPrimerStop() + StringUtil.TAB
									+ probe.getProbeStrand());
						}
					} else if (matchingProbes.size() == 0) {
						if (fastqOneUnableToMapWriter != null) {
							synchronized (fastqOneUnableToMapWriter) {
								fastqOneUnableToMapWriter.write(recordOne);
							}
						}
						if (fastqTwoUnableToMapWriter != null) {
							synchronized (fastqTwoUnableToMapWriter) {
								fastqTwoUnableToMapWriter.write(recordTwo);
							}
						}
					}

				}
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			} finally {
				mapFilterAndExtendSemaphore.release();
			}

		}
	}

	private class MapReadTask implements Runnable {
		private final Logger logger = LoggerFactory.getLogger(MapReadTask.class);

		private final FastqRecord recordOne;
		private final FastqRecord recordTwo;
		private final ProbeReference probeReference;
		private final SAMFileHeader samHeader;
		private final String readGroupName;
		private final IAlignmentScorer alignmentScorer;

		public MapReadTask(FastqRecord recordOne, FastqRecord recordTwo, ProbeReference probeReference, SAMFileHeader samHeader, String readGroupName, IAlignmentScorer alignmentScorer) {
			super();
			this.recordOne = recordOne;
			this.recordTwo = recordTwo;
			this.probeReference = probeReference;
			this.samHeader = samHeader;
			this.readGroupName = readGroupName;
			this.alignmentScorer = alignmentScorer;
		}

		@Override
		public void run() {
			try {
				String uid = SAMRecordUtil.parseUidFromRead(recordOne.getReadString(), uidLength);
				ISequence queryOneSequence = new IupacNucleotideCodeSequence(SAMRecordUtil.removeUidFromRead(recordOne.getReadString(), uidLength));
				String recordOneQualityString = SAMRecordUtil.removeUidFromRead(recordOne.getBaseQualityString(), uidLength);

				ISequence queryTwoSequence = new IupacNucleotideCodeSequence(recordTwo.getReadString());
				String recordTwoQualityString = recordTwo.getBaseQualityString();

				IlluminaFastQHeader illuminaReadHeader = IlluminaFastQHeader.parseIlluminaFastQHeader(recordOne.getReadHeader());
				String readName = illuminaReadHeader.getBaseHeader();
				String sequenceName = probeReference.getProbe().getSequenceName();

				IReadPair readPair = ExtendReadsToPrimer.extendReadPair(uid, probeReference.getProbe(), samHeader, sequenceName, readName, readGroupName, queryOneSequence, recordOneQualityString,
						queryTwoSequence, recordTwoQualityString, DEFAULT_MAPPING_QUALITY, DEFAULT_MAPPING_QUALITY, alignmentScorer);

				if (readPair != null) {
					SAMRecord samRecordFirstOfPair = readPair.getRecord();
					SAMRecord samRecordSecondOfPair = readPair.getMateRecord();

					if (samRecordFirstOfPair != null && samRecordSecondOfPair != null) {
						SAMRecordPair pair = SAMRecordUtil.setSAMRecordsAsPair(samRecordFirstOfPair, samRecordSecondOfPair);
						MapperFiltererAndExtender.this.addToSamRecordPairs(pair);
					}
				}
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			} finally {
				mapReadSemaphore.release();
			}

		}

	}

}
