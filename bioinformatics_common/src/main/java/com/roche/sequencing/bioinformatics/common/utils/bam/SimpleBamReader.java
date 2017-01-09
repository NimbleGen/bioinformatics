package com.roche.sequencing.bioinformatics.common.utils.bam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import com.roche.sequencing.bioinformatics.common.utils.ByteUtil;

public class SimpleBamReader {

	private final static String BAM_MAGIC_NUMBER_AS_STRING = "BAM\1";
	public final static byte[] BAM_MAGIC_NUMBER_BYTES = BAM_MAGIC_NUMBER_AS_STRING.getBytes();

	public static void loadBam(File bamFile) {
		// TODO: NOTE: The whole file is encrypted so need to decrypt before can read the magic number

		System.out.println("Expected:" + ByteUtil.getBinaryStringOfBits(BAM_MAGIC_NUMBER_BYTES));

		try (RandomAccessFile fileAccess = new RandomAccessFile(bamFile, "r")) {
			byte[] magicNumberBytes = new byte[4];
			fileAccess.read(magicNumberBytes);
			System.out.println("Actual  :" + ByteUtil.getBinaryStringOfBits(magicNumberBytes));
			if (!Arrays.equals(magicNumberBytes, BAM_MAGIC_NUMBER_BYTES)) {
				throw new IllegalStateException("The provided BAM file[" + bamFile.getAbsolutePath() + "] contains the wrong magic number[" + new String(magicNumberBytes) + "] where ["
						+ BAM_MAGIC_NUMBER_AS_STRING + "] was excpected.");
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// File bamFile = new File("D:\\kurts_space\\hsq_with_pete\\1\\new_results\\start0_numreads100\\result.bam");
		// File bamFile = new File("D:\\kurts_space\\hsq_with_pete\\1\\Batch1_Capture1_NA12878_L001_2000000reads_dedup.bam");
		File bamFile = new File("C:\\Users\\heilmank\\Desktop\\wgEncodeRikenCageGm12878CellPapAlnRep1.bam");
		loadBam(bamFile);
	}
}
