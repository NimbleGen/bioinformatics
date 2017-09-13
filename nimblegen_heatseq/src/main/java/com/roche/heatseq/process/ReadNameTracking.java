package com.roche.heatseq.process;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.sequencing.bioinformatics.common.utils.IlluminaFastQReadNameUtil;

public class ReadNameTracking {

	private static final Set<String> readNamesToTrack = new HashSet<>();
	private static final Logger logger = LoggerFactory.getLogger(ReadNameTracking.class);

	// static {
	// programmatically add names for tracking
	// addReadNameToTrack("M01947:260:000000000-AD4UR:1:1110:10708:19080");
	// programmatically add read index for tracking
	// addReadNameToTrack("0");
	// }

	private ReadNameTracking() {
		throw new AssertionError();
	}

	public static void addReadNameToTrack(String readName) {
		readNamesToTrack.add(readName);
		System.out.println("Now tracking read name[" + readName + "].");
	}

	public static Set<String> getReadNamesToTrack() {
		return Collections.unmodifiableSet(readNamesToTrack);
	}

	public static boolean shouldTrackReadName(String readName) {
		return readNamesToTrack.contains(readName);
	}

	public static boolean shouldTrackReadName(int readIndex) {
		return readNamesToTrack.contains("" + readIndex);
	}

	public static void trackIndexIfTrackingReadName(String readHeader, int index) {
		if (readNamesToTrack.size() > 0) {

			String uniqueReadHeader = IlluminaFastQReadNameUtil.getUniqueIdForReadHeader(readHeader);
			if (readNamesToTrack.contains(uniqueReadHeader) && !readNamesToTrack.contains("" + index)) {
				readNamesToTrack.add("" + index);
				String message = "Now tracking index[" + index + "] for read name[" + uniqueReadHeader + "].";
				System.out.println(message);
				logger.info(message);
			}

			if (readNamesToTrack.contains("" + index) && !readNamesToTrack.contains(uniqueReadHeader)) {
				readNamesToTrack.add(uniqueReadHeader);
				String message = "Now tracking read name[" + uniqueReadHeader + "] for index[" + index + "].";
				System.out.println(message);
				logger.info(message);
			}
		}

	}

}
