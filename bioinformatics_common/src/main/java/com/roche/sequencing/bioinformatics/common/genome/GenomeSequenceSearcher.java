package com.roche.sequencing.bioinformatics.common.genome;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.sequencing.bioinformatics.common.mapping.TallyMap;
import com.roche.sequencing.bioinformatics.common.multithreading.IExceptionListener;
import com.roche.sequencing.bioinformatics.common.multithreading.PausableFixedThreadPoolExecutor;
import com.roche.sequencing.bioinformatics.common.sequence.ISequence;
import com.roche.sequencing.bioinformatics.common.sequence.NucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.SequenceUtil;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;
import com.roche.sequencing.bioinformatics.common.statistics.RunningStats;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.BitSetUtil;
import com.roche.sequencing.bioinformatics.common.utils.ByteUtil;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileBasedBitSet;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.IBitSet;
import com.roche.sequencing.bioinformatics.common.utils.LargeBitSet;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class GenomeSequenceSearcher {

	private final Logger logger = LoggerFactory.getLogger(GenomeSequenceSearcher.class);

	private final static int THREADS_FOR_PROCESSING_SEQUENCES = Math.min(20, Math.max(4, Runtime.getRuntime().availableProcessors()));;

	public final static int LOOKUP_SEQUENCE_LENGTH = 11;

	private final static byte EMPTY_BYTE = (byte) 0;

	private final static ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
	private final static boolean IS_SIGNED = false;

	private final static int BYTES_TO_USE_FOR_GLOBAL_VARIABLES = 4;

	private final static int LOOKUP_CACHE_SIZE = 50;

	private final IBitSet bitset;

	private final int numberOfBitsToStoreBitStart;
	private final int numberOfBitsToStoreEntrySizeForASequence;
	private final int bitsToStoreContainerNames;
	private final int bitsToStoreLocation;

	private final IGenome genome;

	private final int startBitForLinksAndNumberOfEntriesBlocks;
	private final int startBitForLocationsChunks;
	private final String[] containerNames;

	private final static String[] BASES = new String[] { "A", "C", "G", "T" };

	private final LinkedHashMap<Integer, LookupResult> cachedLookupResults;

	private GenomeSequenceSearcher(IBitSet bitset, IGenome genome) {
		this.bitset = bitset;
		this.genome = genome;
		cachedLookupResults = new LinkedHashMap<Integer, LookupResult>();

		int startBit = 0;
		List<Byte> containerNameBytesList = new ArrayList<Byte>();
		byte currentByte = BitSetUtil.getByteArray(bitset, startBit, startBit + 7)[0];
		while (currentByte != EMPTY_BYTE) {
			containerNameBytesList.add(currentByte);
			startBit += 8;
			byte[] byteArray = BitSetUtil.getByteArray(bitset, startBit, startBit + 7);
			if (byteArray.length > 0) {
				currentByte = byteArray[0];
			} else {
				currentByte = 0;
			}
		}

		startBit += 8;

		byte[] containerNamesBytes = new byte[containerNameBytesList.size()];
		for (int i = 0; i < containerNameBytesList.size(); i++) {
			containerNamesBytes[i] = containerNameBytesList.get(i);
		}

		String tabDelimitedContainerNamesAsString = new String(containerNamesBytes, StandardCharsets.UTF_8);
		this.containerNames = tabDelimitedContainerNamesAsString.split(StringUtil.TAB);

		int endBit = startBit + (BYTES_TO_USE_FOR_GLOBAL_VARIABLES * ByteUtil.BITS_PER_BYTE - 1);
		byte[] bytes = BitSetUtil.getByteArray(bitset, startBit, endBit);
		numberOfBitsToStoreBitStart = ByteUtil.convertBytesToInt(bytes, BYTE_ORDER, IS_SIGNED);

		startBit = endBit + 1;
		endBit = startBit + BYTES_TO_USE_FOR_GLOBAL_VARIABLES * ByteUtil.BITS_PER_BYTE - 1;
		bytes = BitSetUtil.getByteArray(bitset, startBit, endBit);
		numberOfBitsToStoreEntrySizeForASequence = ByteUtil.convertBytesToInt(bytes, BYTE_ORDER, IS_SIGNED);

		startBit = endBit + 1;
		endBit = startBit + BYTES_TO_USE_FOR_GLOBAL_VARIABLES * ByteUtil.BITS_PER_BYTE - 1;
		bytes = BitSetUtil.getByteArray(bitset, startBit, endBit);
		bitsToStoreContainerNames = ByteUtil.convertBytesToInt(bytes, BYTE_ORDER, IS_SIGNED);

		startBit = endBit + 1;
		endBit = startBit + BYTES_TO_USE_FOR_GLOBAL_VARIABLES * ByteUtil.BITS_PER_BYTE - 1;
		bytes = BitSetUtil.getByteArray(bitset, startBit, endBit);
		bitsToStoreLocation = ByteUtil.convertBytesToInt(bytes, BYTE_ORDER, IS_SIGNED);

		startBitForLinksAndNumberOfEntriesBlocks = endBit + 1;

		int totalSeqenceNumbers = (int) Math.pow(4, LOOKUP_SEQUENCE_LENGTH);
		int linkAndNumberEntriesSectionSizeInBits = totalSeqenceNumbers * (numberOfBitsToStoreBitStart + numberOfBitsToStoreEntrySizeForASequence);
		startBitForLocationsChunks = startBitForLinksAndNumberOfEntriesBlocks + linkAndNumberEntriesSectionSizeInBits;
	}

	public int getLookupSequenceLength() {
		return LOOKUP_SEQUENCE_LENGTH;
	}

	private static String[] getContainerNames(Genome genome) {
		Set<String> containerNamesSet = genome.getContainerNames();
		String[] containerNames = new String[containerNamesSet.size()];
		int i = 0;
		Iterator<String> iter = containerNamesSet.iterator();
		while (iter.hasNext()) {
			String containerName = iter.next();
			containerNames[i] = containerName;
			i++;
		}
		return containerNames;
	}

	public static GenomeSequenceSearcher createGenomeSearcherFromGenome(Genome genome, int maxHitsPerSequence) {
		int totalSeqenceNumbers = (int) Math.pow(4, LOOKUP_SEQUENCE_LENGTH);
		LocationsChunk[] chunkArray = new LocationsChunk[totalSeqenceNumbers];

		BitSet sequenceToExcludeBitSet = new BitSet();

		String[] containerNames = getContainerNames(genome);

		int bitsToStoreContainerNames = BitSetUtil.getBitsRequiredToStoreUnsignedInt(containerNames.length);
		int bitsToStoreLocation = BitSetUtil.getBitsRequiredToStoreUnsignedLong(genome.getLargestContainer().getStopLocation());

		AtomicInteger containersFinishedCount = new AtomicInteger();

		PausableFixedThreadPoolExecutor executor = new PausableFixedThreadPoolExecutor(THREADS_FOR_PROCESSING_SEQUENCES, "SEQUENCE_PROCESSING_");
		executor.addExceptionListener(new IExceptionListener() {
			@Override
			public void exceptionOccurred(Throwable throwable) {
				throw new RuntimeException(throwable.getMessage(), throwable);
			}
		});

		for (String containerName : containerNames) {
			SequenceChunker chunker = new SequenceChunker(bitsToStoreContainerNames, bitsToStoreLocation, containerNames, containerName, containersFinishedCount, sequenceToExcludeBitSet, chunkArray,
					genome, maxHitsPerSequence);
			executor.submit(chunker);
		}

		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		int numberOfPermutations = (int) Math.pow(4, LOOKUP_SEQUENCE_LENGTH);
		long[] bitStarts = new long[numberOfPermutations];
		int[] numberOfEntriesArray = new int[numberOfPermutations];
		int maxNumberOfEntries = 0;
		long currentBitIndexInChunkSection = 0;

		int bitsPerEntry = bitsToStoreContainerNames + bitsToStoreLocation;

		for (int numberForSequence = 0; numberForSequence < numberOfPermutations; numberForSequence++) {
			LocationsChunk locationChunks = chunkArray[numberForSequence];
			bitStarts[numberForSequence] = currentBitIndexInChunkSection;
			if (locationChunks != null) {
				int numberOfEntries = locationChunks.size;
				currentBitIndexInChunkSection += (bitsPerEntry * numberOfEntries);
				numberOfEntriesArray[numberForSequence] = numberOfEntries;
				maxNumberOfEntries = Math.max(numberOfEntries, maxNumberOfEntries);
			} else {
				numberOfEntriesArray[numberForSequence] = 0;
			}

		}

		int numberOfBitsToStoreBitStart = BitSetUtil.getBitsRequiredToStoreUnsignedLong(currentBitIndexInChunkSection);
		int numberOfBitsToStoreEntrySizeForASequence = BitSetUtil.getBitsRequiredToStoreUnsignedInt(maxNumberOfEntries);

		LargeBitSet bitset = new LargeBitSet();

		long currentBitIndex = 0;

		String tabDelimitedContainerNames = ArraysUtil.toString(containerNames, StringUtil.TAB);
		byte[] containerNamesBytes = tabDelimitedContainerNames.getBytes();
		currentBitIndex += BitSetUtil.copy(containerNamesBytes, bitset, currentBitIndex);

		// This is so we know when the names are done
		byte[] emptyByteArray = new byte[] { EMPTY_BYTE };
		currentBitIndex += BitSetUtil.copy(emptyByteArray, bitset, currentBitIndex);

		// 1 write out number of bits to store bit start
		currentBitIndex += BitSetUtil.copy(ByteUtil.convertIntToBytes(numberOfBitsToStoreBitStart, BYTES_TO_USE_FOR_GLOBAL_VARIABLES, BYTE_ORDER, IS_SIGNED), bitset, currentBitIndex);
		// 2 write out number of bits to store entry size
		currentBitIndex += BitSetUtil.copy(ByteUtil.convertIntToBytes(numberOfBitsToStoreEntrySizeForASequence, BYTES_TO_USE_FOR_GLOBAL_VARIABLES, BYTE_ORDER, IS_SIGNED), bitset, currentBitIndex);
		// 3 write out number of bits to store container name
		currentBitIndex += BitSetUtil.copy(ByteUtil.convertIntToBytes(bitsToStoreContainerNames, BYTES_TO_USE_FOR_GLOBAL_VARIABLES, BYTE_ORDER, IS_SIGNED), bitset, currentBitIndex);
		// 4 write out number of bits to store location
		currentBitIndex += BitSetUtil.copy(ByteUtil.convertIntToBytes(bitsToStoreLocation, BYTES_TO_USE_FOR_GLOBAL_VARIABLES, BYTE_ORDER, IS_SIGNED), bitset, currentBitIndex);

		// 5 write out bit starts and number of entries for every sequence number (block size is based on 1 and 2 above
		for (int numberForSequence = 0; numberForSequence < bitStarts.length; numberForSequence++) {
			int numberOfEntries = numberOfEntriesArray[numberForSequence];
			long bitStart = bitStarts[numberForSequence];

			if (numberOfEntries == 0) {
				if (sequenceToExcludeBitSet.get(numberForSequence)) {
					bitStart = 1;
				} else {
					bitStart = 0;
				}
			}

			int bytesToUse = (int) Math.ceil(numberOfBitsToStoreBitStart / (double) ByteUtil.BITS_PER_BYTE);
			currentBitIndex += BitSetUtil.copy(ByteUtil.convertLongToBytes(bitStart, bytesToUse, BYTE_ORDER, IS_SIGNED), numberOfBitsToStoreBitStart, bitset, currentBitIndex);
			bytesToUse = (int) Math.ceil(numberOfBitsToStoreEntrySizeForASequence / (double) ByteUtil.BITS_PER_BYTE);
			currentBitIndex += BitSetUtil.copy(ByteUtil.convertIntToBytes(numberOfEntries, bytesToUse, BYTE_ORDER, IS_SIGNED), numberOfBitsToStoreEntrySizeForASequence, bitset, currentBitIndex);
		}

		// 6 write out LocationsChunks for each
		for (int numberForSequence = 0; numberForSequence < numberOfPermutations; numberForSequence++) {
			LocationsChunk locationsChunk = chunkArray[numberForSequence];
			bitStarts[numberForSequence] = currentBitIndexInChunkSection;
			if (locationsChunk != null) {
				int numberOfBits = locationsChunk.size * bitsPerEntry;
				currentBitIndex += BitSetUtil.copy(locationsChunk.bitset, numberOfBits, bitset, currentBitIndex);
			}
		}

		System.out.println("last location:" + currentBitIndex);
		System.out.println("number of bitsets used:" + bitset.getNumberOfBitSetsUsed());

		return new GenomeSequenceSearcher(bitset, genome);
	}

	private static class SequenceChunker implements Runnable {

		private final int bitsToStoreContainerNames;
		private final int bitsToStoreLocation;
		private final String[] containerNames;
		private final String containerName;
		private final AtomicInteger containersFinishedCount;
		private final BitSet sequenceToExcludeBitSet;
		private final LocationsChunk[] chunkArray;
		private final Genome genome;
		private final int maxHitsPerSequence;

		public SequenceChunker(int bitsToStoreContainerNames, int bitsToStoreLocation, String[] containerNames, String containerName, AtomicInteger containersFinishedCount,
				BitSet sequenceToExcludeBitSet, LocationsChunk[] chunkArray, Genome genome, int maxHitPerSequence) {
			super();
			this.bitsToStoreContainerNames = bitsToStoreContainerNames;
			this.bitsToStoreLocation = bitsToStoreLocation;
			this.containerNames = containerNames;
			this.containerName = containerName;
			this.containersFinishedCount = containersFinishedCount;
			this.sequenceToExcludeBitSet = sequenceToExcludeBitSet;
			this.chunkArray = chunkArray;
			this.genome = genome;
			this.maxHitsPerSequence = maxHitPerSequence;
		}

		@Override
		public void run() {
			System.out.println("starting container:" + containerName + ".");
			long timeStart = System.currentTimeMillis();
			long size = genome.getContainerSize(containerName);
			ISequence containerSequence = genome.getSequence(containerName, 1, size - 1);

			for (int startInSequence = 0; startInSequence <= (size - LOOKUP_SEQUENCE_LENGTH - 2); startInSequence++) {
				int stopInSequence = startInSequence + LOOKUP_SEQUENCE_LENGTH - 1;

				Integer numberForSequence = null;
				try {
					numberForSequence = getNumberForSequence(containerSequence, startInSequence, stopInSequence);
				} catch (AssertionError e) {
					System.out.println("error in container:" + containerName);
				}

				boolean unrecognizedBaseFound = (numberForSequence == null);
				if (!unrecognizedBaseFound) {
					boolean sequenceIsExcluded = sequenceToExcludeBitSet.get(numberForSequence);
					if (!sequenceIsExcluded) {
						LocationsChunk locationsChunk = chunkArray[numberForSequence];
						if (locationsChunk == null) {
							locationsChunk = new LocationsChunk();
							chunkArray[numberForSequence] = locationsChunk;
						} else {
							if (locationsChunk.size() > maxHitsPerSequence) {
								sequenceToExcludeBitSet.set(numberForSequence);
								chunkArray[numberForSequence] = null;
							} else {
								int startInGenome = startInSequence + 1;
								GenomicCoordinate coordinate = new GenomicCoordinate(containerName, startInGenome);
								synchronized (locationsChunk) {
									locationsChunk.add(coordinate, bitsToStoreContainerNames, bitsToStoreLocation, containerNames);
								}
							}
						}
					}
				}
			}

			long timeStop = System.currentTimeMillis();
			System.out.println("done with container:" + containerName + "(" + containersFinishedCount.incrementAndGet() + " of " + containerNames.length + ") in "
					+ DateUtil.convertMillisecondsToHHMMSSMMM(timeStop - timeStart) + ".");

		}

	}

	public static GenomeSequenceSearcher createInMemoryGenomeSearcherFromInputStream(InputStream genomeSequenceSearcherInputStream, Genome genome) throws IOException {
		LargeBitSet bitset = LargeBitSet.readLargeBitSetFromInputStream(genomeSequenceSearcherInputStream);
		GenomeSequenceSearcher gss = new GenomeSequenceSearcher(bitset, genome);
		return gss;

	}

	public static GenomeSequenceSearcher createInMemoryGenomeSequenceSearcherFromFile(File genomeSequenceSearcherFile, Genome genome) throws IOException {
		return createInMemoryGenomeSearcherFromInputStream(new FileInputStream(genomeSequenceSearcherFile), genome);
	}

	public static GenomeSequenceSearcher createFileBasedGenomeSequenceSearcherFromFile(File genomeSequenceSearcherFile, IGenome genome) throws IOException {
		FileBasedBitSet fileBasedBitSet = new FileBasedBitSet(genomeSequenceSearcherFile);
		GenomeSequenceSearcher gss = new GenomeSequenceSearcher(fileBasedBitSet, genome);
		return gss;
	}

	public void saveGenomeSearcherToFile(File outputFile) throws IOException {
		System.out.println("The file being written has a size of " + bitset.size() + " in bits");
		bitset.writeToFile(outputFile);
	}

	public StrandedLookupResult search(ISequence sequence) {
		List<StrandedGenomicRangedCoordinate> strandedSearchResults = new ArrayList<StrandedGenomicRangedCoordinate>();

		LookupResult forwardLookupResult = forwardStrandSearch(sequence);
		if (forwardLookupResult.getLookupResultType() == LookupResultTypeEnum.RESULTS) {
			List<GenomicCoordinate> forwardSearchResults = forwardLookupResult.getFoundCoordinates();
			boolean forwardSearchContainedExcludedSequence = (forwardSearchResults == null);
			if (!forwardSearchContainedExcludedSequence) {
				for (GenomicCoordinate coordinates : forwardSearchResults) {
					long start = coordinates.getLocation();
					long stop = start + sequence.size() - 1;
					strandedSearchResults.add(new StrandedGenomicRangedCoordinate(coordinates.getContainerName(), Strand.FORWARD, start, stop));
				}
			}
		}

		LookupResult reverseLookupResult = forwardStrandSearch(sequence.getReverseCompliment());
		if (reverseLookupResult.getLookupResultType() == LookupResultTypeEnum.RESULTS) {
			List<GenomicCoordinate> reverseSearchResults = reverseLookupResult.getFoundCoordinates();
			boolean reverseSearchContainedExcludedSequence = (reverseSearchResults == null);
			if (!reverseSearchContainedExcludedSequence) {
				for (GenomicCoordinate coordinates : reverseSearchResults) {
					long stop = coordinates.getLocation();
					long start = stop + sequence.size() - 1;
					strandedSearchResults.add(new StrandedGenomicRangedCoordinate(coordinates.getContainerName(), Strand.REVERSE, start, stop));
				}
			}
		}

		StrandedLookupResult strandedLookupResult;
		if (strandedSearchResults.size() == 0) {
			if (forwardLookupResult.getLookupResultType() == LookupResultTypeEnum.RESULTS) {
				strandedLookupResult = new StrandedLookupResult(LookupResultTypeEnum.NO_RESULTS);
			} else {
				strandedLookupResult = new StrandedLookupResult(forwardLookupResult.getLookupResultType());
			}
		} else {
			strandedLookupResult = new StrandedLookupResult(strandedSearchResults);
		}

		return strandedLookupResult;
	}

	private LookupResult forwardStrandSearch(ISequence sequence) {
		LookupResult result = null;
		if (sequence.size() < LOOKUP_SEQUENCE_LENGTH) {
			throw new IllegalStateException("The provided sequence[" + sequence + "] of length[" + sequence.size() + "] must be of length[" + LOOKUP_SEQUENCE_LENGTH + "] or greater.");
		}

		boolean isFirstPass = true;
		boolean containsTooManyResultsType = false;

		Map<GenomicCoordinate, BitSet> possibleMatchesToCoverageMap = new HashMap<GenomicCoordinate, BitSet>();
		sequenceLoop: for (int startIndex = 0; startIndex <= (sequence.size() - LOOKUP_SEQUENCE_LENGTH); startIndex++) {
			int stopIndex = startIndex + LOOKUP_SEQUENCE_LENGTH - 1;
			ISequence sequenceToLookup = sequence.subSequence(startIndex, stopIndex);

			LookupResult lookupResult = lookupSequence(sequenceToLookup);
			if (lookupResult.getLookupResultType() == LookupResultTypeEnum.RESULTS) {
				Set<GenomicCoordinate> newCoordinates = new HashSet<GenomicCoordinate>();
				for (GenomicCoordinate coordinate : lookupResult.getFoundCoordinates()) {
					long coordinateStart = coordinate.getLocation() - startIndex;
					GenomicCoordinate newCoordinate = new GenomicCoordinate(coordinate.getContainerName(), coordinateStart);
					newCoordinates.add(newCoordinate);

					if (isFirstPass) {
						possibleMatchesToCoverageMap.put(newCoordinate, new BitSet());
					}

					BitSet coverageMap = possibleMatchesToCoverageMap.get(newCoordinate);
					if (coverageMap != null) {
						for (int i = startIndex; i <= stopIndex; i++) {
							coverageMap.set(i);
						}
					}
				}

				if (isFirstPass) {
					isFirstPass = false;
				} else {
					Set<GenomicCoordinate> coordinatesToRemove = new HashSet<GenomicCoordinate>();
					for (GenomicCoordinate existingCoordinate : possibleMatchesToCoverageMap.keySet()) {
						if (!newCoordinates.contains(existingCoordinate)) {
							coordinatesToRemove.add(existingCoordinate);
						}
					}
					for (GenomicCoordinate coordinateToRemove : coordinatesToRemove) {
						possibleMatchesToCoverageMap.remove(coordinateToRemove);
					}
					if (possibleMatchesToCoverageMap.size() == 0) {
						result = new LookupResult(LookupResultTypeEnum.NO_RESULTS);
						break sequenceLoop;
					}
				}
			} else if (lookupResult.getLookupResultType() == LookupResultTypeEnum.POTENTIAL_MATCH_LIMIT_REACHED) {
				containsTooManyResultsType = true;
			} else {
				result = new LookupResult(lookupResult.getLookupResultType());
				break sequenceLoop;
			}
		}

		if (result == null) {
			if (isFirstPass && containsTooManyResultsType) {
				result = new LookupResult(LookupResultTypeEnum.POTENTIAL_MATCH_LIMIT_REACHED);
			} else {
				List<GenomicCoordinate> matches = new ArrayList<GenomicCoordinate>();
				for (Entry<GenomicCoordinate, BitSet> entry : possibleMatchesToCoverageMap.entrySet()) {
					GenomicCoordinate coordinate = entry.getKey();
					if (containsTooManyResultsType) {
						// check coverage map
						BitSet coverageMap = entry.getValue();
						boolean hasFullCoverage = true;
						coverageLoop: for (int i = 0; i < sequence.size(); i++) {
							hasFullCoverage = (coverageMap.get(i));
							if (!hasFullCoverage) {
								break coverageLoop;
							}
						}

						if (hasFullCoverage) {
							matches.add(coordinate);
						} else {
							// need to check since the too many results lookupSequences might not have been matches
							ISequence genomicSequence = genome.getSequence(coordinate.getContainerName(), coordinate.getLocation(), coordinate.getLocation() + sequence.size() - 1);
							boolean sequencesMatch = SequenceUtil.matches(genomicSequence, sequence);
							if (sequencesMatch) {
								matches.add(coordinate);
							}
						}

					} else {
						// no need to check since the same coordinate was found in all lookupSequences
						matches.add(coordinate);
					}

				}

				result = new LookupResult(matches);
			}
		}

		return result;
	}

	public LookupResult lookupSequence(ISequence sequenceToLookup) {
		LookupResult lookupResult = null;

		if (sequenceToLookup.size() != LOOKUP_SEQUENCE_LENGTH) {
			lookupResult = new LookupResult(LookupResultTypeEnum.SEQUENCE_IS_WRONG_SIZE);
			logger.info("The provided lookup sequence[" + sequenceToLookup + "] with length[" + sequenceToLookup.size() + "] is not of the required lookup sequence length[" + LOOKUP_SEQUENCE_LENGTH
					+ "].");
		} else {

			Integer numberForSequence = getNumberForSequence(sequenceToLookup, 0, sequenceToLookup.size() - 1);

			if (numberForSequence == null) {
				lookupResult = new LookupResult(LookupResultTypeEnum.SEQUENCE_CONTAINS_NON_ACGT_SEQUENCE);
			} else {
				lookupResult = lookupSequence(numberForSequence);
			}
		}

		return lookupResult;
	}

	public static class LookupResult {
		private final LookupResultTypeEnum lookupResultType;
		private final List<GenomicCoordinate> foundCoordinates;

		public LookupResult(LookupResultTypeEnum lookupResultType) {
			super();
			this.lookupResultType = lookupResultType;
			this.foundCoordinates = null;
		}

		public LookupResult(List<GenomicCoordinate> foundCoordinates) {
			super();
			this.lookupResultType = LookupResultTypeEnum.RESULTS;
			this.foundCoordinates = foundCoordinates;
		}

		public LookupResultTypeEnum getLookupResultType() {
			return lookupResultType;
		}

		public List<GenomicCoordinate> getFoundCoordinates() {
			return foundCoordinates;
		}

	}

	public static class StrandedLookupResult {
		private final LookupResultTypeEnum lookupResultType;
		private final List<StrandedGenomicRangedCoordinate> foundCoordinates;

		public StrandedLookupResult(LookupResultTypeEnum lookupResultType) {
			super();
			this.lookupResultType = lookupResultType;
			this.foundCoordinates = null;
		}

		public StrandedLookupResult(List<StrandedGenomicRangedCoordinate> foundCoordinates) {
			super();
			this.lookupResultType = LookupResultTypeEnum.RESULTS;
			this.foundCoordinates = foundCoordinates;
		}

		public LookupResultTypeEnum getLookupResultType() {
			return lookupResultType;
		}

		public List<StrandedGenomicRangedCoordinate> getFoundCoordinates() {
			return foundCoordinates;
		}

		public String getResultDescription() {
			StringBuilder resultDescription = new StringBuilder();
			if (lookupResultType == LookupResultTypeEnum.RESULTS) {
				resultDescription.append("Found:" + StringUtil.NEWLINE);
				for (StrandedGenomicRangedCoordinate location : foundCoordinates) {
					resultDescription.append(location + StringUtil.NEWLINE);
				}
			} else {
				System.out.println(lookupResultType);
			}
			return resultDescription.toString();
		}

	}

	public static enum LookupResultTypeEnum {
		POTENTIAL_MATCH_LIMIT_REACHED, NO_RESULTS, SEQUENCE_CONTAINS_NON_ACGT_SEQUENCE, SEQUENCE_IS_WRONG_SIZE, RESULTS
	}

	public LookupResult lookupSequence(int numberForSequence) {
		LookupResult lookupResult = cachedLookupResults.get(numberForSequence);

		if (lookupResult == null) {
			int linkAndEntryBlockSize = numberOfBitsToStoreBitStart + numberOfBitsToStoreEntrySizeForASequence;
			long start = startBitForLinksAndNumberOfEntriesBlocks + (numberForSequence * linkAndEntryBlockSize);

			byte[] bytes = BitSetUtil.getByteArray(bitset, start, start + numberOfBitsToStoreBitStart - 1);
			long chunkBitStart = ByteUtil.convertBytesToLong(bytes, BYTE_ORDER, IS_SIGNED);

			start = start + numberOfBitsToStoreBitStart;
			bytes = BitSetUtil.getByteArray(bitset, start, start + numberOfBitsToStoreEntrySizeForASequence - 1);
			int numberOfEntries = ByteUtil.convertBytesToInt(bytes, BYTE_ORDER, IS_SIGNED);

			if (numberOfEntries > 0) {
				start = startBitForLocationsChunks + chunkBitStart;
				int bitSize = bitsToStoreContainerNames + bitsToStoreLocation;

				BitSet chunkBitSet = bitset.getBitSet(start, start + (numberOfEntries * bitSize));
				LocationsChunk locationsChunk = new LocationsChunk(chunkBitSet, numberOfEntries);

				List<GenomicCoordinate> coordinates = locationsChunk.getGenomicCoordinates(bitsToStoreContainerNames, bitsToStoreLocation, containerNames);
				lookupResult = new LookupResult(coordinates);
			} else {
				if (chunkBitStart == 1) {
					// the sequence was excluded
					lookupResult = new LookupResult(LookupResultTypeEnum.POTENTIAL_MATCH_LIMIT_REACHED);

				} else {
					lookupResult = new LookupResult(LookupResultTypeEnum.NO_RESULTS);
				}
			}

			cachedLookupResults.put(numberForSequence, lookupResult);
			if (cachedLookupResults.size() > LOOKUP_CACHE_SIZE) {
				int oldestNumberForSequence = cachedLookupResults.entrySet().iterator().next().getKey();
				cachedLookupResults.remove(oldestNumberForSequence);
			}
		}

		return lookupResult;
	}

	public static Integer getNumberForSequence(ISequence sequence) {
		return getNumberForSequence(sequence, 0, sequence.size() - 1);
	}

	public static Integer getNumberForSequence(ISequence sequence, int start, int stop) {

		if ((stop - start + 1) != LOOKUP_SEQUENCE_LENGTH) {
			throw new IllegalStateException("The provided sequence:" + sequence.subSequence(start, stop) + " of sequence length[" + (stop - start + 1) + "] is not of the correct length["
					+ LOOKUP_SEQUENCE_LENGTH + "].");
		}

		Integer value = 0;
		sequenceLoop: for (int i = stop; i >= start; i--) {
			int base = (int) Math.pow(4, i - start);
			try {
				String ntBase = sequence.getCodeAt(i).toString();

				for (int baseIndex = 0; baseIndex < BASES.length; baseIndex++) {
					if (ntBase.equals(BASES[baseIndex])) {
						value = value + (baseIndex * base);
						continue sequenceLoop;
					}
				}
			} catch (NullPointerException e) {
				System.out.println("Sequence:" + sequence + " start:" + start + "  stop:" + stop + " length:" + sequence.size());
			}
			// a non recognized base was found so return null
			value = null;
			break sequenceLoop;
		}

		return value;
	}

	private static class LocationsChunk {
		private final BitSet bitset;
		private int size;

		public LocationsChunk() {
			bitset = new BitSet();
			size = 0;
		}

		public LocationsChunk(BitSet bitset, int size) {
			this.bitset = bitset;
			this.size = size;
		}

		public void add(GenomicCoordinate coordinate, int bitsToStoreContainerNames, int bitsToStoreLocation, String[] containerNames) {
			int bitsPerEntry = bitsToStoreContainerNames + bitsToStoreLocation;

			int currentBitIndex = bitsPerEntry * size;

			int containerIndex = ArraysUtil.indexOf(containerNames, coordinate.getContainerName());
			if (containerIndex < 0) {
				throw new IllegalStateException("Could not find the provided container name[" + coordinate.getContainerName() + "].");
			}
			byte[] containerBytes = ByteUtil.convertIntToBytes(containerIndex, (int) Math.ceil(bitsToStoreContainerNames / (double) ByteUtil.BITS_PER_BYTE), BYTE_ORDER, IS_SIGNED);
			currentBitIndex += BitSetUtil.copy(containerBytes, bitsToStoreContainerNames, bitset, currentBitIndex);

			long location = coordinate.getLocation();
			byte[] locationBytes = ByteUtil.convertLongToBytes(location, (int) Math.ceil(bitsToStoreLocation / (double) ByteUtil.BITS_PER_BYTE), BYTE_ORDER, IS_SIGNED);
			currentBitIndex += BitSetUtil.copy(locationBytes, bitsToStoreLocation, bitset, currentBitIndex);

			size++;
		}

		public GenomicCoordinate get(int index, int bitsToStoreContainerNames, int bitsToStoreLocation, String[] containerNames) {
			if (index >= size) {
				throw new IndexOutOfBoundsException("The provided index[" + index + "] is larger or equal to the size[" + size + "].");
			}

			if (index < 0) {
				throw new IndexOutOfBoundsException("The provided index[" + index + "] is less than zero.");
			}

			int bitsPerEntry = bitsToStoreContainerNames + bitsToStoreLocation;

			int currentBitIndex = bitsPerEntry * index;
			byte[] containerBytes = BitSetUtil.getByteArray(bitset, currentBitIndex, currentBitIndex + bitsToStoreContainerNames - 1);
			int containerIndex = ByteUtil.convertBytesToInt(containerBytes, BYTE_ORDER, IS_SIGNED);

			currentBitIndex += bitsToStoreContainerNames;
			byte[] locationBytes = BitSetUtil.getByteArray(bitset, currentBitIndex, currentBitIndex + bitsToStoreLocation - 1);

			long location = ByteUtil.convertBytesToLong(locationBytes, BYTE_ORDER, IS_SIGNED);

			String containerName = containerNames[containerIndex];

			return new GenomicCoordinate(containerName, location);
		}

		public List<GenomicCoordinate> getGenomicCoordinates(int bitsToStoreContainerNames, int bitsToStoreLocation, String[] containerNames) {
			List<GenomicCoordinate> coordinates = new ArrayList<GenomicCoordinate>();

			for (int i = 0; i < size; i++) {
				coordinates.add(get(i, bitsToStoreContainerNames, bitsToStoreLocation, containerNames));
			}

			return coordinates;
		}

		public int size() {
			return size;
		}
	}

	public static void main(String[] args) throws IOException {
		int maxHitsPerSequence = 1000;
		File genomeFile = new File("D:\\kurts_space\\sequence\\hg19_genome.gnm");
		File genomeSequenceSearcherFileOne = new File("D:\\kurts_space\\sequence\\hg19_genome_11_1000.gss");
		File genomeSequenceSearcherFileTwo = new File("D:\\kurts_space\\sequence\\hg19_genome_11_2000.gss");

		File outputOne = new File("D:\\kurts_space\\sequence\\1000_output.txt");
		File outputTwo = new File("D:\\kurts_space\\sequence\\2000_output.txt");

		Genome genome;
		try {
			genome = new Genome(genomeFile);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		// ISequence sequence = new NucleotideCodeSequence("ACTAGACTTTGACAGATGGGCTGGATTTGGGCAAAAGAAACTGGGAAGGGAGCTCCAGACAAGAGAAATAGCAGCAAGTGCCTGGAGGCTGGAAGGCAGCTGGC");
		ISequence sequence = new NucleotideCodeSequence("ACTAGACTTTGACATGCTCTAGAAAACAACACAGAGTAACTGGTGTAAAATGTAAACTATTAAACTAGAACATATCTACAAAAATAAGATGTG");

		// GenomeSequenceSearcher gss = createGenomeSearcherFromGenome(genome);
		// gss.saveGenomeSearcherToFile(genomeSequenceSearcherFile);
		// System.out.println("Saved gss file to [" + genomeSequenceSearcherFile.getAbsolutePath() + "].");

		// profileGenome(gss);

		GenomeSequenceSearcher gss1 = createFileBasedGenomeSequenceSearcherFromFile(genomeSequenceSearcherFileOne, genome);
		GenomeSequenceSearcher gss2 = createFileBasedGenomeSequenceSearcherFromFile(genomeSequenceSearcherFileTwo, genome);

		// profileGenome(gss2);
		profileEfficacyOfSearcher(genome, gss1, gss2, outputOne, outputTwo);

	}

	public static void profileGenome(GenomeSequenceSearcher gss, int maxHitsPerSequence) {
		int notPresentCount = 0;
		int repetitiveCount = 0;
		int matchCount = 0;

		RunningStats stats = new RunningStats();
		TallyMap<Integer> tallies = new TallyMap<Integer>();
		int totalSeqenceNumbers = (int) Math.pow(4, LOOKUP_SEQUENCE_LENGTH);
		for (int i = 0; i < totalSeqenceNumbers; i++) {
			LookupResult lookupResult = gss.lookupSequence(i);
			if (lookupResult.getLookupResultType() == LookupResultTypeEnum.RESULTS) {
				int size = lookupResult.getFoundCoordinates().size();
				stats.add(size);
				tallies.add(size);
				matchCount++;
			} else if (lookupResult.getLookupResultType() == LookupResultTypeEnum.NO_RESULTS) {
				notPresentCount++;
			} else if (lookupResult.getLookupResultType() == LookupResultTypeEnum.POTENTIAL_MATCH_LIMIT_REACHED) {
				repetitiveCount++;
			}

			if (((i * 100) % totalSeqenceNumbers) == 0) {
				System.out.println("percent:" + (i * 100) / totalSeqenceNumbers + "  " + i + " of " + totalSeqenceNumbers);
			}
		}

		System.out.println(stats);
		for (int i = 0; i < maxHitsPerSequence; i++) {
			System.out.println(tallies.getCount(i));
		}

		System.out.println("match:" + matchCount + " repetitive:" + repetitiveCount + " notPresent:" + notPresentCount);
	}

	public static void profileEfficacyOfSearcher(Genome genome, GenomeSequenceSearcher gss1, GenomeSequenceSearcher gss2, File fileOne, File fileTwo) throws IOException {
		TallyMap<LookupResultTypeEnum> oneAllTally = new TallyMap<GenomeSequenceSearcher.LookupResultTypeEnum>();
		TallyMap<LookupResultTypeEnum> twoAllTally = new TallyMap<GenomeSequenceSearcher.LookupResultTypeEnum>();
		long count = 0;

		FileUtil.createNewFile(fileOne);
		FileUtil.createNewFile(fileTwo);

		try (BufferedWriter writerOne = new BufferedWriter(new FileWriter(fileOne))) {
			try (BufferedWriter writerTwo = new BufferedWriter(new FileWriter(fileTwo))) {

				for (String containerName : genome.getContainerNames()) {
					writerOne.write("starting container:" + containerName + " at " + DateUtil.getCurrentDateINYYYYMMDDHHMMSSwithColons() + StringUtil.NEWLINE);
					writerTwo.write("starting container:" + containerName + " at " + DateUtil.getCurrentDateINYYYYMMDDHHMMSSwithColons() + StringUtil.NEWLINE);
					long start = System.currentTimeMillis();
					TallyMap<LookupResultTypeEnum> onetally = new TallyMap<GenomeSequenceSearcher.LookupResultTypeEnum>();
					TallyMap<LookupResultTypeEnum> twotally = new TallyMap<GenomeSequenceSearcher.LookupResultTypeEnum>();
					ISequence sequence = genome.getSequence(containerName);
					if (sequence != null) {
						for (int i = 1; i < sequence.size() - LOOKUP_SEQUENCE_LENGTH - 1; i++) {
							Integer numberForSequence = getNumberForSequence(sequence, i, i + LOOKUP_SEQUENCE_LENGTH - 1);
							if (numberForSequence != null) {
								LookupResultTypeEnum one = gss1.lookupSequence(numberForSequence).getLookupResultType();
								onetally.add(one);
								oneAllTally.add(one);
								LookupResultTypeEnum two = gss2.lookupSequence(numberForSequence).getLookupResultType();
								twotally.add(two);
								twoAllTally.add(two);
							}
							count++;
						}
					}
					long stop = System.currentTimeMillis();
					writerOne.write("Done with container:" + containerName + " at " + DateUtil.getCurrentDateINYYYYMMDDHHMMSSwithColons() + " in "
							+ DateUtil.convertMillisecondsToHHMMSSMMM(stop - start) + StringUtil.NEWLINE);
					writerOne.write(onetally.getHistogramAsString() + StringUtil.NEWLINE);
					writerTwo.write("Done with container:" + containerName + " at " + DateUtil.getCurrentDateINYYYYMMDDHHMMSSwithColons() + " in "
							+ DateUtil.convertMillisecondsToHHMMSSMMM(stop - start) + StringUtil.NEWLINE);
					writerTwo.write(onetally.getHistogramAsString() + StringUtil.NEWLINE);
				}

				writerOne.write("All--count" + count + oneAllTally.getHistogramAsString() + StringUtil.NEWLINE);
				writerTwo.write("All--count" + count + oneAllTally.getHistogramAsString() + StringUtil.NEWLINE);
			}
		}
	}
}
