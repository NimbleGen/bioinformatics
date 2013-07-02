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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.samtools.SAMRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.IReadPair;
import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.objects.ReadPair;
import com.roche.heatseq.objects.SAMRecordPair;
import com.roche.heatseq.objects.UidReductionResultsForAProbe;
import com.roche.heatseq.qualityreport.ProbeProcessingStats;
import com.roche.mapping.SAMRecordUtil;
import com.roche.sequencing.bioinformatics.common.utils.StatisticsUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

/**
 * Filter a set of reads to find the best read per UID
 */
class FilterByUid {
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
	 * @param chromosomeName
	 * @param readNameToRecordsMap
	 *            The reads that aligned within the probe target area
	 * @param probeUidQualityWriter
	 *            Used to report on UID quality
	 * @return A UidReductionResultsForAProbe containing the processing statistics and the reduced probe set
	 */
	static UidReductionResultsForAProbe reduceProbesByUid(Probe probe, String chromosomeName, Map<String, SAMRecordPair> readNameToRecordsMap, PrintWriter probeUidQualityWriter) {
		List<IReadPair> readPairs = new ArrayList<IReadPair>();

		long probeProcessingStartInMs = System.currentTimeMillis();

		// Process the data into a list
		List<IReadPair> datas = new ArrayList<IReadPair>();
		for (Entry<String, SAMRecordPair> compressedReadNameToSamRecordPairEntry : readNameToRecordsMap.entrySet()) {
			SAMRecordPair recordPair = compressedReadNameToSamRecordPairEntry.getValue();
			SAMRecord record = recordPair.getFirstOfPairRecord();
			SAMRecord mate = recordPair.getSecondOfPairRecord();

			if ((record != null) && (mate != null)) {
				String uid = SAMRecordUtil.getUidAttribute(record);
				datas.add(new ReadPair(record, mate, uid));
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

		for (String uid : uidToDataMap.keySet()) {
			List<IReadPair> pairsDataByUid = uidToDataMap.get(uid);

			int readPairsByUid = pairsDataByUid.size();

			totalReadPairs += readPairsByUid;
			sizeByUid[i] = readPairsByUid;
			minNumberOfReadPairsPerUid = Math.min(minNumberOfReadPairsPerUid, readPairsByUid);

			if (readPairsByUid > maxNumberOfReadPairsPerUid) {
				maxNumberOfReadPairsPerUid = readPairsByUid;
				uidOfEntryWithMaxNumberOfReadPairs = uid;
			}

			totalDuplicateReadPairsRemoved += (pairsDataByUid.size() - 1);

			printProbeUidQualities(probe, chromosomeName, pairsDataByUid, probeUidQualityWriter);

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

		long probeProcessingStopInMs = System.currentTimeMillis();
		int totalTimeToProcessInMs = (int) (probeProcessingStopInMs - probeProcessingStartInMs);

		ProbeProcessingStats probeProcessingStats = new ProbeProcessingStats(probe, chromosomeName, totalUids, averageNumberOfReadPairsPerUid, standardDeviationOfReadPairsPerUid,
				totalDuplicateReadPairsRemoved, totalReadPairsRemainingAfterReduction, minNumberOfReadPairsPerUid, maxNumberOfReadPairsPerUid, uidOfEntryWithMaxNumberOfReadPairs,
				totalTimeToProcessInMs);

		return new UidReductionResultsForAProbe(probeProcessingStats, readPairs);
	}

	/**
	 * Report on the probe UID qualities
	 * 
	 * @param probe
	 * @param containerName
	 * @param data
	 * @param probeUidQualityWriter
	 */
	private static void printProbeUidQualities(Probe probe, String containerName, List<IReadPair> data, PrintWriter probeUidQualityWriter) {
		if (probeUidQualityWriter != null) {
			synchronized (probeUidQualityWriter) {
				for (IReadPair currentPair : data) {
					int probeIndex = -1;
					String probeCaptureStart = "";
					String probeCaptureStop = "";
					String probeStrand = "";
					String uid = "";
					String sequenceQualityScore = "";
					String sequenceTwoQualityScore = "";
					String totalQualityScore = "";

					if (currentPair != null) {
						probeIndex = probe.getIndex();
						probeCaptureStart = "" + probe.getCaptureTargetStart();
						probeCaptureStop = "" + probe.getCaptureTargetStop();
						probeStrand = probe.getProbeStrand().toString();

						uid = "" + currentPair.getUid();

						sequenceQualityScore = "" + currentPair.getSequenceOneQualityScore();
						sequenceTwoQualityScore = "" + currentPair.getSequenceTwoQualityScore();
						totalQualityScore = "" + currentPair.getTotalSequenceQualityScore();

					}
					probeUidQualityWriter.println(probeIndex + StringUtil.TAB + containerName + StringUtil.TAB + probeCaptureStart + StringUtil.TAB + probeCaptureStop + StringUtil.TAB + probeStrand
							+ StringUtil.TAB + uid.toUpperCase() + StringUtil.TAB + sequenceQualityScore + StringUtil.TAB + sequenceTwoQualityScore + StringUtil.TAB + totalQualityScore
							+ StringUtil.TAB + currentPair.getReadName());
				}
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
