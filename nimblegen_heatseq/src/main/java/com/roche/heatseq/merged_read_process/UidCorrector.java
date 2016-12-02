package com.roche.heatseq.merged_read_process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.roche.heatseq.objects.Probe;
import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class UidCorrector {

	private final static int DEFAULT_EDIT_DISTANCE_FOR_UID_GROUPING = 1;

	/**
	 * This method will add uidGroup to each of the probe assignments (a uid group is made by comining similar uids for a give probe id) Note: This is a mutator method, and it returns the same thing
	 * that is passed in just for the sake of clarity regarding input and output.
	 * 
	 * @param readNameToProbeAssignmentMap
	 * @return the passed in map with a uid group set in the probe assignment where possible
	 */
	public static Map<String, ProbeAssignment> correctUids(Map<String, ProbeAssignment> readNameToProbeAssignmentMap) {
		UidGroupLookup uidGroupLookup = generateUidGroupLookup(readNameToProbeAssignmentMap.values(), DEFAULT_EDIT_DISTANCE_FOR_UID_GROUPING);

		for (ProbeAssignment probeAssignment : readNameToProbeAssignmentMap.values()) {
			Probe probe = probeAssignment.getAssignedProbe();
			if (probe != null) {
				String uid = probeAssignment.getUid();
				String probeId = probe.getProbeId();
				String uidGroup = uidGroupLookup.getUidGroup(probeId, uid);
				probeAssignment.setUidGroup(uidGroup);
			}
		}

		return readNameToProbeAssignmentMap;
	}

	private static UidGroupLookup generateUidGroupLookup(Collection<ProbeAssignment> allProbeAssignments, int uidEditDistance) {
		Map<String, TallyMap<String>> uidsByProbeId = new HashMap<String, TallyMap<String>>();
		System.out.println("total read assignments:" + allProbeAssignments.size());
		for (ProbeAssignment probeAssignment : allProbeAssignments) {
			Probe assignedProbe = probeAssignment.getAssignedProbe();
			if (assignedProbe != null) {
				String probeId = assignedProbe.getProbeId();
				TallyMap<String> tallyMap = uidsByProbeId.get(probeId);
				if (tallyMap == null) {
					tallyMap = new TallyMap<String>();
					uidsByProbeId.put(probeId, tallyMap);
				}
				String uid = probeAssignment.getUid();
				tallyMap.add(uid);
			}
		}

		Map<String, Map<String, String>> uidToMainUidByProbeIdMap = new HashMap<String, Map<String, String>>();
		for (Entry<String, TallyMap<String>> entry : uidsByProbeId.entrySet()) {
			String probeId = entry.getKey();
			TallyMap<String> uidTallyMap = entry.getValue();

			List<Entry<String, Integer>> uidAndCounts = uidTallyMap.getObjectsSortedFromMostTalliesToLeast();
			Set<Integer> indexesThatHaveBeenMerged = new HashSet<Integer>();

			Map<String, String> uidToMainUidMap = new HashMap<String, String>();
			uidToMainUidByProbeIdMap.put(probeId, uidToMainUidMap);

			for (int i = 0; i < uidAndCounts.size(); i++) {
				if (!indexesThatHaveBeenMerged.contains(i)) {
					String uid = uidAndCounts.get(i).getKey();
					int count = uidAndCounts.get(i).getValue();
					uidToMainUidMap.put(uid, uid);
					for (int j = i + 1; j < uidAndCounts.size(); j++) {
						if (!indexesThatHaveBeenMerged.contains(j)) {
							String uidForMerge = uidAndCounts.get(j).getKey();
							int countForMerge = uidAndCounts.get(j).getValue();
							if (count > countForMerge) {
								if (StringUtil.contains(uid, uidForMerge, uidEditDistance)) {
									indexesThatHaveBeenMerged.add(j);
									uidToMainUidMap.put(uidForMerge, uid);
								}
							}
						}
					}
				}
			}
		}

		return new UidGroupLookup(uidToMainUidByProbeIdMap);
	}

	private static class UidGroupLookup {

		private Map<String, Map<String, String>> uidToMainUidByProbeIdMap;

		public UidGroupLookup(Map<String, Map<String, String>> uidToMainUidByProbeIdMap) {
			super();
			this.uidToMainUidByProbeIdMap = uidToMainUidByProbeIdMap;
		}

		public String getUidGroup(String probeId, String uid) {
			String groupUid = null;
			Map<String, String> mainUidByUidMap = this.uidToMainUidByProbeIdMap.get(probeId);
			if (mainUidByUidMap != null) {
				groupUid = mainUidByUidMap.get(uid);
			}
			if (groupUid == null) {
				groupUid = uid;
			}
			return groupUid;
		}

		public List<String> getProbeIds() {
			return new ArrayList<String>(uidToMainUidByProbeIdMap.keySet());
		}

		public Map<String, String> getUidToMainUidMap(String probeId) {
			return uidToMainUidByProbeIdMap.get(probeId);
		}

	}

}
