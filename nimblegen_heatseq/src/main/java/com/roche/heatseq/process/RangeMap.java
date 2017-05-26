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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.utils.IlluminaFastQReadNameUtil;
import com.roche.sequencing.bioinformatics.common.utils.fastq.PicardException;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ParsedProbeFile;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.Probe;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.ProbeFileUtil;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

class RangeMap<O> implements IRangeMap<O> {

	private final List<LocationToValue<O>> locationToValue;
	private int minStart = Integer.MAX_VALUE;
	private int maxStop = -Integer.MAX_VALUE;
	private boolean locationsSorted;

	private ForwardComparator<O> forwardComparator = new ForwardComparator<O>();

	public RangeMap() {
		locationToValue = new ArrayList<RangeMap.LocationToValue<O>>();
		locationsSorted = true;
	}

	public int size() {
		return locationToValue.size();
	}

	public List<LocationToValue<O>> getLocationToValueList() {
		return Collections.unmodifiableList(locationToValue);
	}

	public synchronized void put(int startInclusive, int stopInclusive, O object) {
		minStart = Math.min(minStart, startInclusive);
		maxStop = Math.max(maxStop, stopInclusive);
		locationToValue.add(new LocationToValue<O>(object, Math.min(startInclusive, stopInclusive), Math.max(startInclusive, stopInclusive)));
		locationsSorted = false;
	}

	/**
	 * Note that this is not optimized and done using brute force
	 * 
	 * @param startInclusive
	 * @param stopInclusive
	 * @return
	 */
	public synchronized List<O> getObjectsThatOverlapRangeInclusive(int startInclusive, int stopInclusive) {
		List<O> objectsOverlappingRange = new ArrayList<O>();

		for (LocationToValue<O> rangeAndValue : locationToValue) {
			boolean rangesOverlapInclusive = Math.max(startInclusive, rangeAndValue.startLocation) <= Math.min(stopInclusive, rangeAndValue.endLocation);
			if (rangesOverlapInclusive) {
				objectsOverlappingRange.add(rangeAndValue.value);
			}
		}

		return objectsOverlappingRange;
	}

	/**
	 * An optimized function which uses a binary search to identify all ranges that are contained inclusively.
	 */
	public synchronized List<O> getObjectsThatContainRangeInclusive(int startInclusive, int stopInclusive) {
		int startLocationToFind = Math.min(startInclusive, stopInclusive);
		int stopLocationToFind = Math.max(startInclusive, stopInclusive);

		List<O> objectsWithinRange = new ArrayList<O>();

		if (!locationsSorted) {
			Collections.sort(locationToValue, forwardComparator);
			locationsSorted = true;
		}

		int loIndex = 1;
		int hiIndex = locationToValue.size();

		boolean searchComplete = false;

		// binary search
		while (!searchComplete) {
			int afterSearchIndex = loIndex + ((hiIndex - loIndex) / 2);
			if (afterSearchIndex <= locationToValue.size()) {
				int afterSearchLocation = Integer.MAX_VALUE;
				if (afterSearchIndex < locationToValue.size()) {
					afterSearchLocation = locationToValue.get(afterSearchIndex).getStartLocation();
				}
				if (afterSearchLocation >= startLocationToFind) {
					int beforeSearchLocation = locationToValue.get(afterSearchIndex - 1).getStartLocation();
					boolean searchLocationFound = beforeSearchLocation <= startLocationToFind;
					if (searchLocationFound) {
						LocationToValue<O> currentIndex = locationToValue.get(afterSearchIndex - 1);
						// this currently assumes that no probe completely overlaps another probe
						while (currentIndex != null && currentIndex.getEndLocation() >= stopLocationToFind) {
							if (currentIndex.getStartLocation() <= startLocationToFind && currentIndex.getEndLocation() >= stopLocationToFind) {
								objectsWithinRange.add(currentIndex.getValue());
							}
							afterSearchIndex--;
							if (afterSearchIndex > 0) {
								currentIndex = locationToValue.get(afterSearchIndex - 1);
							} else {
								currentIndex = null;
							}
						}
						searchComplete = true;
					} else {
						hiIndex = afterSearchIndex - 1;
					}
				} else if (afterSearchLocation < startLocationToFind) {
					loIndex = afterSearchIndex + 1;
				}

				if (hiIndex < loIndex) {
					searchComplete = true;
				}
			} else {
				searchComplete = true;
			}
		}

		return objectsWithinRange;
	}

	private static class ForwardComparator<O> implements Comparator<LocationToValue<O>> {
		@Override
		public int compare(LocationToValue<O> o1, LocationToValue<O> o2) {
			return Integer.compare(o1.startLocation, o2.startLocation);
		}

	}

	public static class LocationToValue<O> {
		private final O value;
		private final int startLocation;
		private final int endLocation;

		public LocationToValue(O value, int startLocation, int endLocation) {
			super();
			this.value = value;
			this.startLocation = startLocation;
			this.endLocation = endLocation;
		}

		public O getValue() {
			return value;
		}

		public int getStartLocation() {
			return startLocation;
		}

		public int getEndLocation() {
			return endLocation;
		}
	}

	public static void main(String[] args) throws IOException {
		go4();
	}

	@SuppressWarnings("unused")
	private static void go2() throws IOException {
		ParsedProbeFile probeInfo = ProbeFileUtil.parseProbeInfoFile(new File("D://kurts_space/heatseq/big/probe_info.txt"));
		Set<String> sequenceNames = probeInfo.getSequenceNames();

		Map<String, IRangeMap<Probe>> positveStrandProbesRangesBySequenceName = new HashMap<String, IRangeMap<Probe>>();
		Map<String, IRangeMap<Probe>> negativeStrandProbesRangesBySequenceName = new HashMap<String, IRangeMap<Probe>>();
		for (String sequenceName : sequenceNames) {
			if (sequenceName.equals("chr1")) {
				List<Probe> probes = probeInfo.getProbesBySequenceName(sequenceName);
				IRangeMap<Probe> positiveStrandRangeMap = new RangeMap<Probe>();
				IRangeMap<Probe> negativeStrandRangeMap = new RangeMap<Probe>();
				for (Probe probe : probes) {
					int queryStart = probe.getStart();

					int queryStop = probe.getStop();

					if (probe.getProbeStrand() == Strand.FORWARD) {
						positiveStrandRangeMap.put(queryStart, queryStop, probe);
					} else {
						negativeStrandRangeMap.put(queryStart, queryStop, probe);
					}
				}
				positveStrandProbesRangesBySequenceName.put(sequenceName, positiveStrandRangeMap);
				negativeStrandProbesRangesBySequenceName.put(sequenceName, negativeStrandRangeMap);
			}
		}
		System.out.println("done with range map");
		IRangeMap<Probe> rangeMap = positveStrandProbesRangesBySequenceName.get("chr1");
		// System.out.println(rangeMap.getObjectsThatContainRangeInclusive(925929, 926025).size());
		for (Probe probe : rangeMap.getObjectsThatContainRangeInclusive(8871926, 8871973)) {
			System.out.println(probe.getProbeId() + "  " + probe.getStart() + "  " + probe.getStop());
		}
		// System.out.println(rangeMap.getObjectsThatContainRangeInclusive(84937745, 84937839).size());
	}

	@SuppressWarnings("unused")
	private static void go3() {
		RangeMap<String> rangeMap = new RangeMap<String>();
		rangeMap.put(2145, 2284, "a");
		rangeMap.put(6462, 6601, "b");
		rangeMap.put(8617, 8756, "c");
		rangeMap.put(8712, 8851, "d");
		System.out.println(rangeMap.getObjectsThatContainRangeInclusive(8741, 8787));

	}

	private static void go4() {
		RangeMap<String> rangeMap = new RangeMap<String>();
		rangeMap.put(1, 10, "a");
		rangeMap.put(2, 11, "b");
		rangeMap.put(3, 12, "c");
		rangeMap.put(4, 13, "d");
		System.out.println(rangeMap.getObjectsThatContainRangeInclusive(5, 7));

	}

	@SuppressWarnings("unused")
	private static void go() {
		// NewRangeMap<String> rangeMap = new NewRangeMap<String>();
		// rangeMap.put(1, 3, "a");
		// rangeMap.put(4, 7, "b");
		// rangeMap.put(6, 10, "c");
		// rangeMap.put(9, 13, "d");
		// rangeMap.put(15, 20, "e");
		// System.out.println(rangeMap.getObjectsContainedWithinRangeInclusive(15, 20));

		long start = System.currentTimeMillis();

		File bamFile = new File("D://kurts_space/heatseq/big/input.bam");
		File bamFileIndex = new File("D://kurts_space/heatseq/big/input.bai");

		try (SamReader samReader = SamReaderFactory.makeDefault().open(SamInputResource.of(bamFile).index(bamFileIndex))) {
			SAMRecordIterator samIter = samReader.iterator();

			Map<String, SAMRecord> firstFoundRecordByReadName = new HashMap<String, SAMRecord>();

			int lineNumber = 1;
			// Scan through the entire bam, finding matches by name to the fastq data in our hash
			while (samIter.hasNext()) {
				SAMRecord samRecord = samIter.next();
				String readNameFromBam = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(samRecord.getReadName());

				if (readNameFromBam.equals("HISEQ-MFG:361:HFCJVADXX:2:2110:4461:33497")) {
					System.out.println(samRecord.getReferenceName() + " " + samRecord.getAlignmentStart() + " " + samRecord.getAlignmentEnd() + " line:" + lineNumber);
				}

				// if (firstFoundRecordByReadName.containsKey(readNameFromBam)) {
				// SAMRecord firstFoundRecord = firstFoundRecordByReadName.get(readNameFromBam);
				// boolean firstFoundIsCorrect = firstFoundRecord.getMateUnmappedFlag() == samRecord.getReadUnmappedFlag();
				// boolean secondFoundIsCorrect = samRecord.getMateUnmappedFlag() == firstFoundRecord.getReadUnmappedFlag();
				// if (!firstFoundIsCorrect || !secondFoundIsCorrect) {
				// System.out.println("read name pair[" + readNameFromBam + "] is not labeled correctly with regards to mapped flag at line[" + lineNumber + "].");
				// System.out.println("r1_mate_unmapped[" + firstFoundRecord.getMateUnmappedFlag() + "] r1_unmapped[" + firstFoundRecord.getReadUnmappedFlag() + "] r2_mate_unmapped["
				// + samRecord.getMateUnmappedFlag() + "] r2_unmapped[" + samRecord.getReadUnmappedFlag() + "].");
				// }
				// firstFoundRecordByReadName.remove(readNameFromBam);
				// } else {
				// firstFoundRecordByReadName.put(readNameFromBam, samRecord);
				// }

				lineNumber++;
			}

			System.out.println("orphaned reads:");
			for (String name : firstFoundRecordByReadName.keySet()) {
				System.out.println(name);
			}

			long end = System.currentTimeMillis();
			System.out.println("total read time:" + (end - start));

			samIter.close();
		} catch (IOException e) {
			throw new PicardException(e.getMessage(), e);
		}

	}

	@Override
	public int getMinStart() {
		return minStart;
	}

	@Override
	public int getMaxStop() {
		return maxStop;
	}
}
