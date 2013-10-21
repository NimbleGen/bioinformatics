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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;

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
import com.roche.heatseq.qualityreport.NucleotideCompositionUtil;
import com.roche.heatseq.qualityreport.ProbeProcessingStats;
import com.roche.heatseq.qualityreport.ReportManager;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.StatisticsUtil;

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

	private final File outputDirectory;
	private final String outputFilePrefix;
	private final boolean shouldOutputQualityReports;

	private ReportManager reportManager;

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
	public MapperFiltererAndExtender(File fastQOneFile, File fastQTwoFile, File probeFile, File outputFile, File outputDirectory, String outputFilePrefix, boolean shouldOutputQualityReports,
			int numProcessors, int uidLength, boolean allowVariableLengthUids, String programName, String programVersion, String commandLineSignature, IAlignmentScorer alignmentScorer) {

		super();
		samRecordPairs = new ArrayList<SAMRecordPair>();
		uidAndProbeReferenceToFastQLineMapping = new ConcurrentHashMap<ProbeReference, Map<String, Set<QualityScoreAndFastQLineIndex>>>();
		this.fastQOneFile = fastQOneFile;
		this.fastQTwoFile = fastQTwoFile;
		this.probeFile = probeFile;
		this.outputFile = outputFile;

		this.alignmentScorer = alignmentScorer;

		started = false;
		this.numProcessors = numProcessors;
		mapFilterAndExtendSemaphore = new Semaphore(numProcessors);
		mapReadSemaphore = new Semaphore(numProcessors);
		this.uidLength = uidLength;
		this.allowVariableLengthUids = allowVariableLengthUids;
		this.programName = programName;
		this.programVersion = programVersion;
		this.commandLineSignature = commandLineSignature;

		this.outputDirectory = outputDirectory;
		this.outputFilePrefix = outputFilePrefix;
		this.shouldOutputQualityReports = shouldOutputQualityReports;
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

			TallyMap<String> readNamesToDistinctProbeAssignmentCount = new TallyMap<String>();
			Set<ISequence> distinctUids = Collections.newSetFromMap(new ConcurrentHashMap<ISequence, Boolean>());
			List<ISequence> uids = new ArrayList<ISequence>();
			int totalProbes = 0;
			int totalMappedReads = 0;
			int totalReads = 0;

			try {
				ProbesBySequenceName probesBySequenceName = ProbeFileUtil.parseProbeInfoFile(probeFile);

				SAMFileHeader samHeader = BamFileUtil.getHeader(probesBySequenceName, commandLineSignature, programName, programVersion);

				reportManager = new ReportManager(outputDirectory, outputFilePrefix, uidLength, samHeader, shouldOutputQualityReports);

				SubReadProbeMapper probeMapper = new SubReadProbeMapper();
				probeMapper.addProbes(probesBySequenceName);

				Integer fastQ1PrimerLength = null;
				Integer fastQ2PrimerLength = null;

				for (String sequenceName : probesBySequenceName.getSequenceNames()) {
					for (Probe probe : probesBySequenceName.getProbesBySequenceName(sequenceName)) {
						totalProbes++;
						if (fastQ1PrimerLength == null && fastQ2PrimerLength == null) {
							fastQ1PrimerLength = probe.getExtensionPrimerSequence().size();
							fastQ2PrimerLength = probe.getLigationPrimerSequence().size();
						}
					}
				}

				String readGroupName = fastQOneFile.getName() + "_and_" + fastQTwoFile.getName();
				SAMReadGroupRecord readGroup = new SAMReadGroupRecord(readGroupName);
				readGroup.setPlatform("illumina");
				readGroup.setSample(readGroupName);
				samHeader.addReadGroup(readGroup);

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

							totalReads += 2;

							MapUidAndProbeTask mapUidAndProbeTask = new MapUidAndProbeTask(recordOne, recordTwo, probeMapper, fastqLineIndex, fastQ1PrimerLength, fastQ2PrimerLength, uidLength,
									allowVariableLengthUids, reportManager, alignmentScorer);
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

					Set<ISequence> distinctUidsByProbe = new HashSet<ISequence>();
					List<ISequence> weightedUidsByProbe = new ArrayList<ISequence>();

					// loop by uid
					for (Entry<String, Set<QualityScoreAndFastQLineIndex>> uidToqualityScoreAndFastQLineIndexesEntry : uidToQualityScoreAndFastQLineIndexes.entrySet()) {
						Set<QualityScoreAndFastQLineIndex> qualityScoreAndFastQLineIndexes = uidToqualityScoreAndFastQLineIndexesEntry.getValue();
						String uid = uidToqualityScoreAndFastQLineIndexesEntry.getKey();
						distinctUidsByProbe.add(new IupacNucleotideCodeSequence(uid));
						ISequence uidSequence = new IupacNucleotideCodeSequence(uid);
						distinctUids.add(uidSequence);
						synchronized (uids) {
							uids.add(uidSequence);
						}
						int numberOfReadPairs = qualityScoreAndFastQLineIndexes.size();

						for (int j = 0; j < numberOfReadPairs; j++) {
							weightedUidsByProbe.add(uidSequence);
						}

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

					totalMappedReads += 2 * totalReadPairs;
					int totalDuplicateReadPairsRemoved = totalReadPairs - totalReadPairsRemainingAfterReduction;

					double[] numberOfReadsPairsPerUidArray = new double[numberOfReadsPairsPerUid.size()];
					for (int i = 0; i < numberOfReadsPairsPerUid.size(); i++) {
						numberOfReadsPairsPerUidArray[i] = (double) numberOfReadsPairsPerUid.get(i);
					}

					double averageNumberOfReadPairsPerUid = StatisticsUtil.arithmeticMean(numberOfReadsPairsPerUidArray);
					double standardDeviationOfReadPairsPerUid = StatisticsUtil.standardDeviation(numberOfReadsPairsPerUidArray);

					long probeProcessingStopTime = System.currentTimeMillis();
					int totalTimeToProcessInMs = (int) (probeProcessingStopTime - probeProcessingStartTime);

					if (reportManager.isReporting()) {
						int totalUids = uidToQualityScoreAndFastQLineIndexes.size();

						String uidNucleotideComposition = NucleotideCompositionUtil.getNucleotideComposition(distinctUidsByProbe);
						String uidNucleotideCompositionByPosition = NucleotideCompositionUtil.getNucleotideCompositionByPosition(distinctUidsByProbe);

						String weightedUidNucleotideComposition = NucleotideCompositionUtil.getNucleotideComposition(weightedUidsByProbe);
						String weightedUidNucleotideCompositionByPosition = NucleotideCompositionUtil.getNucleotideCompositionByPosition(weightedUidsByProbe);

						ProbeProcessingStats probeProcessingStats = new ProbeProcessingStats(probeReference.getProbe(), totalUids, averageNumberOfReadPairsPerUid, standardDeviationOfReadPairsPerUid,
								totalDuplicateReadPairsRemoved, totalReadPairsRemainingAfterReduction, minNumberOfReadPairsPerUid, maxNumberOfReadPairsPerUid, uidOfEntryWithMaxNumberOfReadPairs,
								totalTimeToProcessInMs, uidNucleotideComposition, uidNucleotideCompositionByPosition, weightedUidNucleotideComposition, weightedUidNucleotideCompositionByPosition);
						reportManager.getDetailsReport().writeEntry(probeProcessingStats);

						Probe probe = probeReference.getProbe();
						String[] line = new String[uidToQualityScoreAndFastQLineIndexes.size() + 1];
						line[0] = probe.getProbeId();
						int columnIndex = 1;

						List<Integer> uidCounts = new ArrayList<Integer>();

						for (Set<QualityScoreAndFastQLineIndex> readsByUid : uidToQualityScoreAndFastQLineIndexes.values()) {
							uidCounts.add(readsByUid.size());
						}

						Collections.sort(uidCounts, new Comparator<Integer>() {
							@Override
							public int compare(Integer o1, Integer o2) {
								return o2.compareTo(o1);
							}
						});

						for (int uidCount : uidCounts) {
							line[columnIndex] = "" + uidCount;
							columnIndex++;
						}

						reportManager.getUniqueProbeTalliesWriter().writeLine((Object[]) line);

						reportManager.getProbeCoverageWriter().writeLine(
								(Object[]) new String[] { probe.getSequenceName(), "" + probe.getStart(), "" + probe.getStop(), "" + probe.getProbeId(), "" + totalUids,
										probe.getProbeStrand().getSymbol(), "" + probe.getCaptureTargetStart(), "" + probe.getCaptureTargetStop(), "", "", "", "" });
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
								readNamesToDistinctProbeAssignmentCount.add(recordOne.getReadHeader());
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

			if (reportManager.isReporting()) {
				long processingTimeInMs = end - start;
				reportManager.completeSummaryReport(readNamesToDistinctProbeAssignmentCount, distinctUids, uids, processingTimeInMs, totalProbes, totalReads, totalMappedReads);
			}

			reportManager.close();

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
		private final ReportManager reportManger;
		private final IAlignmentScorer alignmentScorer;

		public MapUidAndProbeTask(FastqRecord recordOne, FastqRecord recordTwo, SubReadProbeMapper probeMapper, int fastqLineIndex, int fastQOnePrimerLength, int fastQTwoPrimerLength, int uidLength,
				boolean allowVariableLengthUids, ReportManager reportManager, IAlignmentScorer alignmentScorer) {
			super();
			this.recordOne = recordOne;
			this.recordTwo = recordTwo;
			this.probeMapper = probeMapper;
			this.fastqLineIndex = fastqLineIndex;
			this.fastQOnePrimerLength = fastQOnePrimerLength;
			this.fastQTwoPrimerLength = fastQTwoPrimerLength;
			this.uidLength = uidLength;
			this.allowVariableLengthUids = allowVariableLengthUids;
			this.alignmentScorer = alignmentScorer;
			this.reportManger = reportManager;
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
							uid = SAMRecordUtil.getVariableLengthUid(completeReadWithUid, extensionPrimerSequence, reportManager, matchingProbe, alignmentScorer);

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

							if (reportManager.isReporting()) {

								String probeCaptureStart = "" + matchingProbe.getCaptureTargetStart();
								String probeCaptureStop = "" + matchingProbe.getCaptureTargetStop();
								String probeStrand = matchingProbe.getProbeStrand().toString();
								String readSequence = queryOneSequence.toString();

								String readName = recordOne.getReadHeader();
								reportManger.getProbeUidQualityWriter().writeLine(matchingProbe.getProbeId(), matchingProbe.getSequenceName(), probeCaptureStart, probeCaptureStop, probeStrand,
										uid.toUpperCase(), sequenceOneQualityScore, sequenceTwoQualityScore, qualityScore, readName, readSequence);
							}

						} else {
							if (reportManager.isReporting()) {
								reportManager.getUnableToAlignPrimerWriter().writeLine(matchingProbe.getSequenceName(), matchingProbe.getStart(), matchingProbe.getStop(),
										matchingProbe.getExtensionPrimerSequence(), recordOne.getReadHeader(), recordOne.getReadString());
							}
						}
					} else if ((matchingProbes.size() > 1)) {
						if (reportManager.isReporting()) {
							for (ProbeReference matchingProbe : matchingProbes) {
								Probe probe = matchingProbe.getProbe();
								reportManager.getAmbiguousMappingWriter().writeLine(recordOne.getReadHeader(), recordOne.getReadString(), probe.getSequenceName(), probe.getExtensionPrimerStart(),
										probe.getExtensionPrimerStop(), probe.getCaptureTargetStart(), probe.getCaptureTargetStop(), probe.getLigationPrimerStart(), probe.getLigationPrimerStop(),
										probe.getProbeStrand());
							}
						}
					} else if (matchingProbes.size() == 0) {
						if (reportManager.isReporting()) {
							FastqWriter fastqOneUnableToMapWriter = reportManager.getFastqOneUnableToMapWriter();
							synchronized (fastqOneUnableToMapWriter) {
								fastqOneUnableToMapWriter.write(recordOne);
							}
							FastqWriter fastqTwoUnableToMapWriter = reportManager.getFastqTwoUnableToMapWriter();
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
