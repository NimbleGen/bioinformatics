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
import com.roche.heatseq.objects.ProbesByContainerName;
import com.roche.heatseq.objects.SAMRecordPair;
import com.roche.heatseq.process.BamFileUtil;
import com.roche.heatseq.process.ExtendReadsToPrimer;
import com.roche.heatseq.process.ProbeFileUtil;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
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
	private boolean started;
	private final int numProcessors;
	private final int uidLength;
	private final List<SAMRecordPair> samRecordPairs;
	private final Map<UidAndProbeReference, Set<QualityScoreAndFastQLineIndex>> uidAndProbeReferenceToFastQLineMapping;

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
	public MapperFiltererAndExtender(File fastQOneFile, File fastQTwoFile, File probeFile, File outputFile, File ambiguousMappingFile, int numProcessors, int uidLength, String programName,
			String programVersion, String commandLineSignature, IAlignmentScorer alignmentScorer) {
		super();
		samRecordPairs = new ArrayList<SAMRecordPair>();
		uidAndProbeReferenceToFastQLineMapping = new ConcurrentHashMap<UidAndProbeReference, Set<QualityScoreAndFastQLineIndex>>();
		this.fastQOneFile = fastQOneFile;
		this.fastQTwoFile = fastQTwoFile;
		this.probeFile = probeFile;
		this.outputFile = outputFile;
		this.alignmentScorer = alignmentScorer;
		if (ambiguousMappingFile != null) {
			try {
				ambiguousMappingWriter = new PrintWriter(ambiguousMappingFile);
				ambiguousMappingWriter.println("readName" + StringUtil.TAB + "extension_primer_start" + StringUtil.TAB + "extension_primer_stop" + StringUtil.TAB + "capture_target_start"
						+ StringUtil.TAB + "capture_target_stop" + StringUtil.TAB + "ligation_primer_start" + StringUtil.TAB + "ligation_primer_stop" + StringUtil.TAB + "probe_strand");
			} catch (FileNotFoundException e) {
				throw new IllegalStateException(e);
			}
		}
		started = false;
		this.numProcessors = numProcessors;
		mapFilterAndExtendSemaphore = new Semaphore(numProcessors);
		mapReadSemaphore = new Semaphore(numProcessors);
		this.uidLength = uidLength;
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
				ProbesByContainerName probesByContainerName = ProbeFileUtil.parseProbeInfoFile(probeFile);
				SubReadProbeMapper probeMapper = new SubReadProbeMapper();
				probeMapper.addProbes(probesByContainerName);
				SAMFileHeader samHeader = SAMRecordUtil.createSAMFileHeader();

				Integer fastQ1PrimerLength = null;
				Integer fastQ2PrimerLength = null;

				for (String containerName : probesByContainerName.getContainerNames()) {
					int sequenceLength = 0;
					for (Probe probe : probesByContainerName.getProbesByContainerName(containerName)) {
						// TODO Kurt Heilman 6/21/2013 pull chromosome/container length from probe info file if we change the format to include container/chromosome length
						sequenceLength = Math.max(sequenceLength, probe.getStop() + 1);
						if (fastQ1PrimerLength == null && fastQ2PrimerLength == null) {
							fastQ1PrimerLength = probe.getExtensionPrimerSequence().size();
							fastQ2PrimerLength = probe.getLigationPrimerSequence().size();
						}
					}
					SAMSequenceRecord sequenceRecord = new SAMSequenceRecord(containerName, sequenceLength);
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
									ambiguousMappingWriter);
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
				for (Entry<UidAndProbeReference, Set<QualityScoreAndFastQLineIndex>> entry : uidAndProbeReferenceToFastQLineMapping.entrySet()) {
					int maxScore = 0;
					int maxScoreFastQLineIndex = -1;
					ProbeReference probeReference = entry.getKey().getProbeReference();
					Set<QualityScoreAndFastQLineIndex> qualityScoreAndFastQLineIndexes = entry.getValue();
					for (QualityScoreAndFastQLineIndex qualityScoreAndFastQIndex : qualityScoreAndFastQLineIndexes) {
						if (qualityScoreAndFastQIndex.getQualityScore() >= maxScore) {
							maxScore = qualityScoreAndFastQIndex.getQualityScore();
							maxScoreFastQLineIndex = qualityScoreAndFastQIndex.getFastQLineIndex();
						}
					}
					nonFilteredFastQLineIndexes.put(maxScoreFastQLineIndex, probeReference);
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
			} catch (IOException e) {
				e.printStackTrace();
			}
			long end = System.currentTimeMillis();

			if (ambiguousMappingWriter != null) {
				ambiguousMappingWriter.close();
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
		private final PrintWriter ambiguousMappingWriter;

		public MapUidAndProbeTask(FastqRecord recordOne, FastqRecord recordTwo, SubReadProbeMapper probeMapper, int fastqLineIndex, int fastQOnePrimerLength, int fastQTwoPrimerLength, int uidLength,
				PrintWriter ambiguousMappingWriter) {
			super();
			this.recordOne = recordOne;
			this.recordTwo = recordTwo;
			this.probeMapper = probeMapper;
			this.fastqLineIndex = fastqLineIndex;
			this.fastQOnePrimerLength = fastQOnePrimerLength;
			this.fastQTwoPrimerLength = fastQTwoPrimerLength;
			this.uidLength = uidLength;
			this.ambiguousMappingWriter = ambiguousMappingWriter;
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
						int qualityScore = BamFileUtil.getQualityScore(recordOneQualityString) + BamFileUtil.getQualityScore(recordTwoQualityString);
						UidAndProbeReference uidAndProbeKey = new UidAndProbeReference(uid, matchingProbes.get(0));
						Set<QualityScoreAndFastQLineIndex> set = uidAndProbeReferenceToFastQLineMapping.get(uidAndProbeKey);
						if (set == null) {
							set = Collections.newSetFromMap(new ConcurrentHashMap<QualityScoreAndFastQLineIndex, Boolean>());
						}
						set.add(new QualityScoreAndFastQLineIndex(qualityScore, fastqLineIndex));
						uidAndProbeReferenceToFastQLineMapping.put(uidAndProbeKey, set);
					} else if ((matchingProbes.size() > 1) && (ambiguousMappingWriter != null)) {
						for (ProbeReference matchingProbe : matchingProbes) {
							Probe probe = matchingProbe.getProbe();
							ambiguousMappingWriter.println(recordOne.getReadString() + StringUtil.TAB + probe.getExtensionPrimerStart() + StringUtil.TAB + probe.getExtensionPrimerStop()
									+ StringUtil.TAB + probe.getCaptureTargetStart() + StringUtil.TAB + probe.getCaptureTargetStop() + StringUtil.TAB + probe.getLigationPrimerStart() + StringUtil.TAB
									+ probe.getLigationPrimerStop() + StringUtil.TAB + probe.getProbeStrand());
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
				String containerName = probeReference.getProbe().getContainerName();

				IReadPair readPair = ExtendReadsToPrimer.extendReadPair(uid, probeReference.getProbe(), samHeader, containerName, readName, readGroupName, queryOneSequence, recordOneQualityString,
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
