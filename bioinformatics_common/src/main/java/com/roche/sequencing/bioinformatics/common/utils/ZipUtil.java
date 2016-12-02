package com.roche.sequencing.bioinformatics.common.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

	private ZipUtil() {
		throw new AssertionError();
	}

	public static void zipDirectoriesAndFiles(File outputFile, List<File> directoriesAndFiles) {

		try {
			FileOutputStream fos = new FileOutputStream(outputFile);
			ZipOutputStream zos = new ZipOutputStream(fos);

			for (File directoryOrFile : directoriesAndFiles) {
				if (directoryOrFile.isDirectory()) {
					for (File file : FileUtil.getAllSubFiles(directoryOrFile)) {
						addToZipFile(directoryOrFile.getParentFile(), file, zos);
					}
				} else {
					addToZipFile(directoryOrFile.getParentFile(), directoryOrFile, zos);
				}

			}

			zos.close();
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void addToZipFile(File rootDirectory, File file, ZipOutputStream zos) throws FileNotFoundException, IOException {

		String zipKey = FileUtil.convertToRelativePath(rootDirectory, file);

		FileInputStream fis = new FileInputStream(file);
		ZipEntry zipEntry = new ZipEntry(zipKey);
		zos.putNextEntry(zipEntry);

		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zos.write(bytes, 0, length);
		}

		zos.closeEntry();
		fis.close();
	}

	/**
	 * Checks if an input stream is gzipped.
	 * 
	 * @param in
	 * @return
	 */
	public static boolean isGZipped(InputStream in) {
		int magic = 0;
		try {
			if (!in.markSupported()) {
				in = new BufferedInputStream(in);
			}
			in.mark(2);
			try {
				magic = in.read() & 0xff | ((in.read() << 8) & 0xff00);
				in.reset();
			} catch (IOException e) {
				e.printStackTrace(System.err);
				return false;
			}
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace(System.err);
				return false;
			}
		}
		return magic == GZIPInputStream.GZIP_MAGIC;
	}

}
