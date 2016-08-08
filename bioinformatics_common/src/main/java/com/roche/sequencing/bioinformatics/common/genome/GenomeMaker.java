package com.roche.sequencing.bioinformatics.common.genome;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.roche.sequencing.bioinformatics.common.sequence.SimpleNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class GenomeMaker {

	private GenomeMaker() {
		throw new AssertionError();
	}

	public static void createGenomeFileFromFastaDirectory(File fastaDirectoryOrFile, File outputGenomeFile) throws FileNotFoundException, IOException {
		// erase the existing file since the content at the end of the file
		// would be preserved if it is not written over thus preventing the containerInformationStart location
		// from being stored at the very end of the file
		if (outputGenomeFile.exists()) {
			outputGenomeFile.delete();
		}

		FileUtil.createNewFile(outputGenomeFile);
		try (RandomAccessFile genomeWriter = new RandomAccessFile(outputGenomeFile, "rw")) {

			final Map<String, StartAndStopLocationsInFile> startAndStopLocationsByContainer = new LinkedHashMap<String, StartAndStopLocationsInFile>();

			FastaProcessor fastaProcessor = new FastaProcessor(genomeWriter, startAndStopLocationsByContainer);

			FastaDirectoryParser.parseFastaFile(fastaDirectoryOrFile, fastaProcessor);

			long containerInformationStart = genomeWriter.getFilePointer();
			// write out the container information at the bottom of the file.
			for (Entry<String, StartAndStopLocationsInFile> entry : startAndStopLocationsByContainer.entrySet()) {
				String containerName = entry.getKey();
				StartAndStopLocationsInFile startAndStopLocation = entry.getValue();
				String entryLine = containerName + StringUtil.TAB + startAndStopLocation.getStartLocationInBytes() + StringUtil.TAB + startAndStopLocation.getStopLocationInBytes()
						+ StringUtil.LINUX_NEWLINE;
				genomeWriter.write(entryLine.getBytes());
			}
			// write out the container information Start at the end of the file
			genomeWriter.writeLong(containerInformationStart);
		}

	}

	private static class FastaProcessor implements IParsedFastaProcessor {
		private final RandomAccessFile genomeWriter;
		private final Map<String, StartAndStopLocationsInFile> startAndStopLocationsByContainer;
		private long currentContainerStart;

		public FastaProcessor(RandomAccessFile genomeWriter, Map<String, StartAndStopLocationsInFile> startAndStopLocationsByContainer) {
			super();
			this.genomeWriter = genomeWriter;
			this.startAndStopLocationsByContainer = startAndStopLocationsByContainer;
			try {
				currentContainerStart = genomeWriter.getFilePointer();
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		@Override
		public void sequenceProcessed(String containerName, SimpleNucleotideCodeSequence sequence) {
			try {
				genomeWriter.write(sequence.getSequenceAsBytes());
				startAndStopLocationsByContainer.put(containerName, new StartAndStopLocationsInFile(currentContainerStart, genomeWriter.getFilePointer()));
				currentContainerStart = genomeWriter.getFilePointer();
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		@Override
		public void doneProcessing() {
		}
	}

	public static void main(String[] args) {
		// File sequenceDir = new File("Y://genomes/homo_sapiens/hg38_GRCh38/sequence/hg38_all.fa");
		// File sequenceDir = new File("Y://genomes/homo_sapiens/hg19_GRCh37/sequence/original/");
		// try {
		// GenomeMaker.createGenomeFileFromFastaDirectory(sequenceDir, new File("D://kurts_space/sequence/hg19_genome.gnm"));
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
	}

}
