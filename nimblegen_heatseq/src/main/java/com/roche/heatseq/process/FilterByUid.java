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

package com.roche.heatseq.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sf.samtools.SAMRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.IReadPair;
import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.objects.ReadPair;
import com.roche.heatseq.objects.SAMRecordPair;
import com.roche.heatseq.objects.UidReductionResultsForAProbe;
import com.roche.heatseq.qualityreport.NucleotideCompositionUtil;
import com.roche.heatseq.qualityreport.ProbeProcessingStats;
import com.roche.mapping.SAMRecordUtil;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.StatisticsUtil;

/**
 * Filter a set of reads to find the best read per UID
 */
public class FilterByUid {
	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(FilterByUid.class);

	/**
	 * We never create instances of this class, we just use static methods
	 */
	private FilterByUid() {
		throw new AssertionError();
	}

	/**
	 * Filter a set of reads to find the best read per UID
	 * 
	 * @param probe
	 * @param readNameToRecordsMap
	 *            The reads that aligned within the probe target area
	 * @param probeUidQualityWriter
	 *            Used to report on UID quality
	 * @return A UidReductionResultsForAProbe containing the processing statistics and the reduced probe set
	 */
	static UidReductionResultsForAProbe reduceProbesByUid(Probe probe, Map<String, SAMRecordPair> readNameToRecordsMap, TabDelimitedFileWriter probeUidQualityWriter,
			TabDelimitedFileWriter unableToAlignPrimerWriter, TabDelimitedFileWriter primerAlignmentWriter, TabDelimitedFileWriter uniqueProbeTalliesWriter,
			TabDelimitedFileWriter probeCoverageWriter, boolean allowVariableLengthUids, IAlignmentScorer alignmentScorer, Set<ISequence> distinctUids, List<ISequence> uids) {
		List<IReadPair> readPairs = new ArrayList<IReadPair>();

		long probeProcessingStartInMs = System.currentTimeMillis();

		// Process the data into a list
		List<IReadPair> datas = new ArrayList<IReadPair>();
		for (Entry<String, SAMRecordPair> compressedReadNameToSamRecordPairEntry : readNameToRecordsMap.entrySet()) {
			SAMRecordPair recordPair = compressedReadNameToSamRecordPairEntry.getValue();
			SAMRecord record = recordPair.getFirstOfPairRecord();
			SAMRecord mate = recordPair.getSecondOfPairRecord();

			if ((record != null) && (mate != null)) {
				String uid = null;
				if (allowVariableLengthUids) {
					uid = SAMRecordUtil.getVariableLengthUid(record, probe, primerAlignmentWriter, alignmentScorer);
				} else {
					uid = SAMRecordUtil.getUidAttribute(record);
				}
				if (uid != null) {
					ISequence uidSequence = new IupacNucleotideCodeSequence(uid);
					distinctUids.add(uidSequence);
					synchronized (uidSequence) {
						uids.add(uidSequence);
					}
					datas.add(new ReadPair(record, mate, uid));
				} else {
					unableToAlignPrimerWriter.writeLine(probe.getProbeId(), probe.getSequenceName(), probe.getStart(), probe.getStop(), probe.getExtensionPrimerSequence(), record.getReadName(),
							record.getReadString());
				}
			}
		}

		Map<String, List<IReadPair>> uidToDataMap = new HashMap<String, List<IReadPair>>();
		for (IReadPair read : datas) {
			String uid = read.getUid();
			// TODO 7/2/2013 Kurt Heilman this basically is using a string comparison instead of a sequence comparison to match the UIDs. Is this appropriate?
			List<IReadPair> uidData = uidToDataMap.get(uid);

			if (uidData == null) {
				uidData = new ArrayList<IReadPair>();
			}

			uidData.add(read);
			uidToDataMap.put(uid, uidData);
		}

		int totalReadPairsRemainingAfterReduction = 0;
		int totalReadPairs = 0;
		int totalDuplicateReadPairsRemoved = 0;
		int minNumberOfReadPairsPerUid = Integer.MAX_VALUE;
		int maxNumberOfReadPairsPerUid = 0;
		String uidOfEntryWithMaxNumberOfReadPairs = "";
		int[] sizeByUid = new int[uidToDataMap.size()];
		int i = 0;

		Set<ISequence> uniqueUidsByProbe = new HashSet<ISequence>();
		List<ISequence> weightedUidsByProbe = new ArrayList<ISequence>();
		for (String uid : uidToDataMap.keySet()) {
			ISequence uidSequence = new IupacNucleotideCodeSequence(uid);
			uniqueUidsByProbe.add(uidSequence);

			List<IReadPair> pairsDataByUid = uidToDataMap.get(uid);

			int readPairsByUid = pairsDataByUid.size();

			for (int j = 0; j < readPairsByUid; j++) {
				weightedUidsByProbe.add(uidSequence);
			}

			totalReadPairs += readPairsByUid;
			sizeByUid[i] = readPairsByUid;
			minNumberOfReadPairsPerUid = Math.min(minNumberOfReadPairsPerUid, readPairsByUid);

			if (readPairsByUid > maxNumberOfReadPairsPerUid) {
				maxNumberOfReadPairsPerUid = readPairsByUid;
				uidOfEntryWithMaxNumberOfReadPairs = uid;
			}

			totalDuplicateReadPairsRemoved += (pairsDataByUid.size() - 1);

			printProbeUidQualities(probe, pairsDataByUid, probeUidQualityWriter);

			IReadPair bestPair = findBestData(pairsDataByUid);

			readPairs.add(bestPair);

			totalReadPairsRemainingAfterReduction++;
			i++;
		}

		if (minNumberOfReadPairsPerUid == Integer.MAX_VALUE) {
			minNumberOfReadPairsPerUid = 0;
		}

		int totalUids = uidToDataMap.size();
		double averageNumberOfReadPairsPerUid = 0;

		if ((totalReadPairs != 0) && (totalUids != 0)) {
			averageNumberOfReadPairsPerUid = totalReadPairs / ((double) totalUids);
		}

		double standardDeviationOfReadPairsPerUid = Double.NaN;
		if (sizeByUid.length > 1) {
			standardDeviationOfReadPairsPerUid = StatisticsUtil.standardDeviation(sizeByUid);
		}

		if (uniqueProbeTalliesWriter != null) {
			String[] line = new String[uidToDataMap.size() + 1];
			line[0] = probe.getProbeId();
			int columnIndex = 1;

			List<UidNameToCountPair> uidCounts = new ArrayList<UidNameToCountPair>();

			for (Entry<String, List<IReadPair>> readsByUid : uidToDataMap.entrySet()) {
				uidCounts.add(new UidNameToCountPair(readsByUid.getKey(), readsByUid.getValue().size()));
			}

			Collections.sort(uidCounts, new Comparator<UidNameToCountPair>() {

				@Override
				public int compare(UidNameToCountPair o1, UidNameToCountPair o2) {
					return Integer.compare(o2.getCount(), o1.getCount());
				}
			});

			for (UidNameToCountPair uidNameAndCount : uidCounts) {
				line[columnIndex] = uidNameAndCount.getUidName() + ":" + uidNameAndCount.getCount();
				columnIndex++;
			}
			uniqueProbeTalliesWriter.writeLine((Object[]) line);
		}

		if (probeCoverageWriter != null) {
			probeCoverageWriter.writeLine((Object[]) new String[] { probe.getSequenceName(), "" + probe.getStart(), "" + probe.getStop(), "" + probe.getProbeId(), "" + totalUids,
					probe.getProbeStrand().getSymbol(), "" + probe.getCaptureTargetStart(), "" + probe.getCaptureTargetStop(), "", "", "", "" });
		}

		long probeProcessingStopInMs = System.currentTimeMillis();
		int totalTimeToProcessInMs = (int) (probeProcessingStopInMs - probeProcessingStartInMs);

		String uidComposition = NucleotideCompositionUtil.getNucleotideComposition(uniqueUidsByProbe);
		String uidCompositionByPosition = NucleotideCompositionUtil.getNucleotideCompositionByPosition(uniqueUidsByProbe);

		String weightedUidComposition = NucleotideCompositionUtil.getNucleotideComposition(weightedUidsByProbe);
		String weightedUidCompositionByPosition = NucleotideCompositionUtil.getNucleotideCompositionByPosition(weightedUidsByProbe);

		ProbeProcessingStats probeProcessingStats = new ProbeProcessingStats(probe, totalUids, averageNumberOfReadPairsPerUid, standardDeviationOfReadPairsPerUid, totalDuplicateReadPairsRemoved,
				totalReadPairsRemainingAfterReduction, minNumberOfReadPairsPerUid, maxNumberOfReadPairsPerUid, uidOfEntryWithMaxNumberOfReadPairs, totalTimeToProcessInMs, uidComposition,
				uidCompositionByPosition, weightedUidComposition, weightedUidCompositionByPosition);

		return new UidReductionResultsForAProbe(probeProcessingStats, readPairs);
	}

	private static class UidNameToCountPair {
		private final String uidName;
		private final int count;

		public UidNameToCountPair(String uidName, int count) {
			super();
			this.uidName = uidName;
			this.count = count;
		}

		public String getUidName() {
			return uidName;
		}

		public int getCount() {
			return count;
		}

	};

	/**
	 * Report on the probe UID qualities
	 * 
	 * @param probe
	 * @param data
	 * @param probeUidQualityWriter
	 */
	public static void printProbeUidQualities(Probe probe, List<IReadPair> data, TabDelimitedFileWriter probeUidQualityWriter) {
		if (probeUidQualityWriter != null) {
			for (IReadPair currentPair : data) {
				String probeId = "";
				String probeCaptureStart = "";
				String probeCaptureStop = "";
				String probeStrand = "";
				String uid = "";
				String sequenceQualityScore = "";
				String sequenceTwoQualityScore = "";
				String totalQualityScore = "";
				String readName = "";

				if (currentPair != null) {
					probeId = probe.getProbeId();
					probeCaptureStart = "" + probe.getCaptureTargetStart();
					probeCaptureStop = "" + probe.getCaptureTargetStop();
					probeStrand = probe.getProbeStrand().toString();

					uid = "" + currentPair.getUid();
					readName = currentPair.getReadName();

					sequenceQualityScore = "" + currentPair.getSequenceOneQualityScore();
					sequenceTwoQualityScore = "" + currentPair.getSequenceTwoQualityScore();
					totalQualityScore = "" + currentPair.getTotalSequenceQualityScore();

				}
				probeUidQualityWriter.writeLine(probeId, probe.getSequenceName(), probeCaptureStart, probeCaptureStop, probeStrand, uid.toUpperCase(), sequenceQualityScore, sequenceTwoQualityScore,
						totalQualityScore, readName);
			}
		}
	}

	/**
	 * @param data
	 * @return the read pair with the best quality in the list
	 */
	private static IReadPair findBestData(List<IReadPair> data) {
		IReadPair bestPair = null;
		int bestScore = Integer.MIN_VALUE;

		for (IReadPair currentPair : data) {
			int currentScore = currentPair.getTotalSequenceQualityScore();

			if (currentScore > bestScore) {
				bestScore = currentScore;
				bestPair = currentPair;
			}
		}

		return bestPair;
	}

}
