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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.heatseq.objects.IReadPair;
import com.roche.heatseq.objects.ReadPair;
import com.roche.heatseq.objects.SAMRecordPair;
import com.roche.heatseq.objects.UidReductionResultsForAProbe;
import com.roche.heatseq.qualityreport.ProbeProcessingStats;
import com.roche.heatseq.qualityreport.ReportManager;
import com.roche.heatseq.utils.NucleotideCompositionUtil;
import com.roche.heatseq.utils.SAMRecordUtil;
import com.roche.sequencing.bioinformatics.common.alignment.IAlignmentScorer;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.TabDelimitedFileWriter;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.Probe;

import htsjdk.samtools.SAMRecord;

/**
 * Filter a set of reads to find the best read per UID
 */
class FilterByUid {
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
	static UidReductionResultsForAProbe reduceReadsByProbeAndUid(Probe probe, Map<Integer, SAMRecordPair> readIndexToRecordsMap, ReportManager reportManager, boolean allowVariableLengthUids,
			int expectedExtensionUidLength, int expectedLigationUidLength, IAlignmentScorer alignmentScorer, Set<ISequence> distinctUids, List<ISequence> uids, boolean markDuplicates,
			boolean useStrictReadToProbeMatching) {

		List<IReadPair> uniqueReadPairs = new ArrayList<IReadPair>();
		List<IReadPair> duplicateReadPairs = new ArrayList<IReadPair>();

		// Process the data into a list
		List<IReadPair> datas = new ArrayList<IReadPair>();
		for (SAMRecordPair recordPair : readIndexToRecordsMap.values()) {
			SAMRecord record = recordPair.getFirstOfPairRecord();
			SAMRecord mate = recordPair.getSecondOfPairRecord();
			boolean readPairAlignsWithProbeCoordinates = true;
			if ((record != null) && (mate != null)) {
				String extensionUid = null;
				String ligationUid = null;
				if (allowVariableLengthUids) {

					extensionUid = SAMRecordUtil.getExtensionVariableLengthUid(record, probe, reportManager, alignmentScorer);
					ligationUid = SAMRecordUtil.getLigationVariableLengthUid(mate, probe, reportManager, alignmentScorer);

					if (useStrictReadToProbeMatching && (extensionUid != null) && (ligationUid != null)) {
						boolean readOneAlignsWithProbeCoordinates = PrimerReadExtensionAndPcrDuplicateIdentification.readPairAlignsWithProbeCoordinates(probe, record, true, extensionUid.length(),
								ligationUid.length());
						boolean readTwoAlignsWithProbeCoordinates = PrimerReadExtensionAndPcrDuplicateIdentification.readPairAlignsWithProbeCoordinates(probe, mate, false, ligationUid.length(),
								ligationUid.length());
						readPairAlignsWithProbeCoordinates = readOneAlignsWithProbeCoordinates && readTwoAlignsWithProbeCoordinates;
					}
				} else {
					extensionUid = SAMRecordUtil.parseUidFromReadOne(record.getReadString(), expectedExtensionUidLength);
					ligationUid = SAMRecordUtil.parseUidFromReadTwo(mate.getReadString(), expectedLigationUidLength);
				}
				if (readPairAlignsWithProbeCoordinates && extensionUid != null && ligationUid != null) {

					ISequence extensionUidSequence = new IupacNucleotideCodeSequence(extensionUid);
					ISequence ligationUidSequence = new IupacNucleotideCodeSequence(ligationUid);
					ISequence fullUidSequence = new IupacNucleotideCodeSequence();
					fullUidSequence.append(extensionUidSequence);
					fullUidSequence.append(ligationUidSequence);
					distinctUids.add(fullUidSequence);
					synchronized (uids) {
						uids.add(fullUidSequence);
					}

					// leave the uid on, this way if its short we won't cut off primer sequence and only uid diversity will be affected
					String readOneString = record.getReadString();// SAMRecordUtil.removeUidFromReadOne(record.getReadString(), extensionUid.length());
					String readOneBaseQualityString = record.getBaseQualityString();// SAMRecordUtil.removeUidFromReadOne(record.getBaseQualityString(), extensionUid.length());

					// leave the uid on, this way if its short we won't cut off primer sequence and only uid diversity will be affected
					String readTwoString = mate.getReadString();// SAMRecordUtil.removeUidFromReadOne(mate.getReadString(), ligationUid.length());
					String readTwoBaseQualityString = mate.getBaseQualityString();// SAMRecordUtil.removeUidFromReadOne(mate.getBaseQualityString(), ligationUid.length());

					record.setReadString(readOneString);
					record.setBaseQualityString(readOneBaseQualityString);

					mate.setReadString(readTwoString);
					mate.setBaseQualityString(readTwoBaseQualityString);

					datas.add(new ReadPair(record, mate, extensionUid, ligationUid, probe.getCaptureTargetSequence(), probe.getProbeId(), false, false));
				}
			}
		}

		Map<String, List<IReadPair>> uidToDataMap = new HashMap<String, List<IReadPair>>();
		for (IReadPair readPair : datas) {
			String extensionUid = readPair.getExtensionUid();
			String ligationUid = readPair.getLigationUid();
			String fullUid = extensionUid + ligationUid;
			List<IReadPair> uidData = uidToDataMap.get(fullUid);

			if (uidData == null) {
				uidData = new ArrayList<IReadPair>();
			}

			uidData.add(readPair);
			uidToDataMap.put(fullUid, uidData);
		}

		int minNumberOfReadPairsPerUid = Integer.MAX_VALUE;
		int maxNumberOfReadPairsPerUid = 0;
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

			sizeByUid[i] = readPairsByUid;
			minNumberOfReadPairsPerUid = Math.min(minNumberOfReadPairsPerUid, readPairsByUid);
			maxNumberOfReadPairsPerUid = Math.max(maxNumberOfReadPairsPerUid, readPairsByUid);

			IReadPair bestPair = findBestData(pairsDataByUid);
			bestPair.setAsBestPairInUidGroup();
			uniqueReadPairs.add(bestPair);

			for (IReadPair readPair : pairsDataByUid) {
				if (!readPair.equals(bestPair)) {
					if (markDuplicates) {
						readPair.markAsDuplicate();
						if (ReadNameTracking.shouldTrackReadName(readPair.getReadName())) {
							String message = "Read Name[" + readPair.getReadName() + "] with uid[" + uid + "] has been marked as a duplicate read.";
							System.out.println(message);
							logger.info(message);
						}
					} else {
						if (ReadNameTracking.shouldTrackReadName(readPair.getReadName())) {
							String message = "Read Name[" + readPair.getReadName() + "] with uid[" + uid + "] has been identified as a duplicate read.";
							System.out.println(message);
							logger.info(message);
						}
					}
					duplicateReadPairs.add(readPair);
				} else {
					if (ReadNameTracking.shouldTrackReadName(readPair.getReadName())) {
						String message = "Read Name[" + readPair.getReadName() + "] with uid[" + uid + "] has been identified as a unique read.";
						System.out.println(message);
						logger.info(message);
					}
				}
			}

			i++;
		}

		if (minNumberOfReadPairsPerUid == Integer.MAX_VALUE) {
			minNumberOfReadPairsPerUid = 0;
		}

		TabDelimitedFileWriter uniqueProbeTalliesWriter = reportManager.getUniqueProbeTalliesWriter();
		TabDelimitedFileWriter probeCoverageWriter = reportManager.getProbeCoverageWriter();

		if (uniqueProbeTalliesWriter != null || probeCoverageWriter != null) {
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
			if (uniqueProbeTalliesWriter != null) {
				uniqueProbeTalliesWriter.writeLine((Object[]) line);
			}

			if (probeCoverageWriter != null) {
				probeCoverageWriter.writeLine((Object[]) new String[] { probe.getSequenceName(), "" + probe.getStart(), "" + probe.getStop(), "" + probe.getProbeId(), "" + uniqueReadPairs.size(),
						probe.getProbeStrand().getSymbol(), "" + probe.getCaptureTargetStart(), "" + probe.getCaptureTargetStop(), "", "", "", "" });
			}
		}

		String uidComposition = NucleotideCompositionUtil.getNucleotideComposition(uniqueUidsByProbe);
		String uidCompositionByPosition = NucleotideCompositionUtil.getNucleotideCompositionByPosition(uniqueUidsByProbe);

		String weightedUidComposition = NucleotideCompositionUtil.getNucleotideComposition(weightedUidsByProbe);
		String weightedUidCompositionByPosition = NucleotideCompositionUtil.getNucleotideCompositionByPosition(weightedUidsByProbe);

		ProbeProcessingStats probeProcessingStats = new ProbeProcessingStats(probe, duplicateReadPairs.size(), uniqueReadPairs.size(), maxNumberOfReadPairsPerUid, uidComposition,
				uidCompositionByPosition, weightedUidComposition, weightedUidCompositionByPosition);

		return new UidReductionResultsForAProbe(probeProcessingStats, uniqueReadPairs, duplicateReadPairs);
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
	 * @param data
	 * @return the read pair with the best quality in the list
	 */
	private static IReadPair findBestData(List<IReadPair> data) {
		IReadPair bestPair = null;
		int bestScore = Integer.MIN_VALUE;
		Integer bestReadPairIdAsInteger = null;
		for (IReadPair currentPair : data) {
			int currentScore = currentPair.getTotalSequenceQualityScore();
			String currentPairId = currentPair.getReadName();
			Integer currentPairIdAsInteger = Integer.parseInt(currentPairId);
			if ((currentScore > bestScore) || ((currentScore == bestScore) && (Integer.compare(currentPairIdAsInteger, bestReadPairIdAsInteger) < 0))) {
				bestScore = currentScore;
				bestPair = currentPair;
				bestReadPairIdAsInteger = currentPairIdAsInteger;
			}
		}

		return bestPair;
	}

}
