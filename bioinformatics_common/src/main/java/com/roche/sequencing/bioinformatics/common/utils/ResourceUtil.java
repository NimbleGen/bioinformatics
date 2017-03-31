package com.roche.sequencing.bioinformatics.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourceUtil {

	private final static String DEFAULT_TEMP_FOLDER_NAME = "jar_resource_";
	private final static String ZIP_FILE_EXTENSION = "zip";

	private final static boolean DEFAULT_DELETE_TEMP_FILES_ON_EXIT = false;
	private final static boolean DEFAULT_OVERRIDE_EXISTING_FILES = true;

	public static File getResourceAsPermanentReadOnlyFile(Class<?> resourceReferenceClass, String resourceName) throws IOException {
		String md5Sum = Md5CheckSumUtil.md5Sum(resourceReferenceClass, resourceName);
		File permanentReadOnlyFile = new File(FileUtil.getTempDirectory(), FileUtil.getFileNameWithoutExtension(resourceName) + "_" + md5Sum + FileUtil.getFileExtension(resourceName));
		if (!permanentReadOnlyFile.exists()) {
			String contents = readResourceAsString(resourceReferenceClass, resourceName);
			FileUtil.writeStringToFile(permanentReadOnlyFile, contents);
		}
		return permanentReadOnlyFile;
	}

	public static String readResourceAsString(Class<?> resourceReferenceClass, String resourceName) throws IOException {
		StringBuilder resourceData = new StringBuilder();

		try (Reader reader = new InputStreamReader(getResourceAsInputStream(resourceReferenceClass, resourceName))) {
			char[] buf = new char[1000];
			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1) {
				resourceData.append(buf, 0, numRead);
			}
		}
		return resourceData.toString();
	}

	private static File getZippedResourceAsDirectory(Class<?> resourceReferenceClass, String resourceName, File tempParentDirectory, boolean deleteTempFilesOnExit) throws IOException {
		File folder = null;
		boolean isInJar = isInJar(resourceReferenceClass);

		InputStream inputStream = null;
		if (isInJar) {
			inputStream = resourceReferenceClass.getResourceAsStream(resourceName);
		} else {
			URL resource = resourceReferenceClass.getResource(resourceName);
			if (resource == null) {
				throw new IllegalStateException("Resource[" + resourceName + "] not found relative to class[" + resourceReferenceClass + "].");
			}
			inputStream = new FileInputStream(new File(resource.getFile()));
		}

		if (inputStream != null) {
			File tempDir = tempParentDirectory;
			folder = new File(tempDir, resourceName);

			byte[] buffer = new byte[1024];

			try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
				ZipEntry zipEntry = null;

				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					if (!zipEntry.isDirectory()) {
						String fileName = zipEntry.getName();
						File newFile = new File(folder + File.separator + fileName);
						FileUtil.createNewFile(newFile);
						FileOutputStream fileOutputStream = new FileOutputStream(newFile);

						int length;
						while ((length = zipInputStream.read(buffer)) > 0) {
							fileOutputStream.write(buffer, 0, length);
						}

						fileOutputStream.close();
						if (deleteTempFilesOnExit) {
							newFile.deleteOnExit();
						}
					}
				}
			}

			folder.setExecutable(true);
			if (deleteTempFilesOnExit) {
				folder.deleteOnExit();
			}
		} else {
			throw new IllegalStateException("Unable to find the resource[" + resourceName + "] relative to the reference class[" + resourceReferenceClass + "].");
		}

		return folder;
	}

	public static File[] getResourcesAsFiles(Class<?> resourceReferenceClass, String[] resourceNames) throws IOException {
		return getResourcesAsFiles(resourceReferenceClass, resourceNames, DEFAULT_TEMP_FOLDER_NAME, DEFAULT_DELETE_TEMP_FILES_ON_EXIT);
	}

	public static File getResourceAsFile(Class<?> resourceReferenceClass, String resourceName) throws IOException {
		return getResourceAsFile(resourceReferenceClass, resourceName, DEFAULT_TEMP_FOLDER_NAME, DEFAULT_DELETE_TEMP_FILES_ON_EXIT);
	}

	public static File getResourceAsFile(Class<?> resourceReferenceClass, String resourceName, String tempFolderName, boolean deleteTempFilesOnExit) throws IOException {
		return getResourcesAsFiles(resourceReferenceClass, new String[] { resourceName }, tempFolderName, deleteTempFilesOnExit)[0];
	}

	public static File[] getResourcesAsFiles(Class<?> resourceReferenceClass, String[] resourceNames, String tempFolderName, boolean deleteTempFilesOnExit) throws IOException {
		File tempFolder = Files.createTempDirectory(tempFolderName, new FileAttribute<?>[0]).toFile();
		return getResourcesAsFiles(resourceReferenceClass, resourceNames, tempFolder, deleteTempFilesOnExit);
	}

	public static File[] getResourcesAsFiles(Class<?> resourceReferenceClass, String[] resourceNames, File outputFolder, boolean deleteFilesOnExit) throws IOException {
		File[] results = null;
		results = getResourcesAsFiles(resourceReferenceClass, resourceNames, outputFolder, deleteFilesOnExit, false, true);
		return results;
	}

	public static File getResourcesAsPermanentTempFile(Class<?> resourceReferenceClass, String resourceName, File tempDirectory) throws IOException {
		return getResourcesAsPermanentTempFiles(resourceReferenceClass, new String[] { resourceName }, tempDirectory)[0];
	}

	public static File getResourcesAsPermanentTempFile(Class<?> resourceReferenceClass, String resourceName, File tempDirectory, boolean overrideExistingFiles) throws IOException {
		return getResourcesAsPermanentTempFiles(resourceReferenceClass, new String[] { resourceName }, tempDirectory, overrideExistingFiles)[0];
	}

	public static File[] getResourcesAsPermanentTempFiles(Class<?> resourceReferenceClass, String[] resourceNames, File tempDirectory) throws IOException {
		return getResourcesAsPermanentTempFiles(resourceReferenceClass, resourceNames, tempDirectory, DEFAULT_OVERRIDE_EXISTING_FILES);
	}

	public static File[] getResourcesAsPermanentTempFiles(Class<?> resourceReferenceClass, String[] resourceNames, File tempDirectory, boolean overrideExistingFiles) throws IOException {
		return getResourcesAsFiles(resourceReferenceClass, resourceNames, tempDirectory, false, true, overrideExistingFiles);
	}

	public synchronized static File[] getResourcesAsFiles(Class<?> resourceReferenceClass, String[] resourceNames, File outputDirectory, boolean deleteTempFilesOnExit, boolean moveAllFilesToTempFolder,
			boolean overrideExistingFiles) throws IOException {
		if (outputDirectory == null) {
			outputDirectory = new File(FileUtil.getTempDirectory(), resourceReferenceClass.getName());
		}

		File[] files = new File[resourceNames.length];
		boolean isInJar = isInJar(resourceReferenceClass);

		int i = 0;
		for (String resourceName : resourceNames) {
			boolean isZipFile = resourceName.toLowerCase().endsWith(ZIP_FILE_EXTENSION);
			if (isZipFile) {
				File file = getZippedResourceAsDirectory(resourceReferenceClass, resourceName, outputDirectory, deleteTempFilesOnExit);
				files[i] = file;
			} else {
				if (isInJar || moveAllFilesToTempFolder) {
					try (InputStream inputStream = resourceReferenceClass.getResourceAsStream(resourceName)) {
						if (inputStream != null) {

							File file = new File(outputDirectory, resourceName);
							boolean fileShouldBeCopied = true;
							if (file.exists()) {
								String existingMd5Sum = Md5CheckSumUtil.md5Sum(file);
								String newMd5Sum = Md5CheckSumUtil.md5Sum(resourceReferenceClass, resourceName);
								if (existingMd5Sum.equals(newMd5Sum)) {
									fileShouldBeCopied = false;
								}
							} else {
								FileUtil.createNewFile(file);
							}

							if (fileShouldBeCopied) {
								Files.copy(inputStream, file.getAbsoluteFile().toPath(), new CopyOption[] { StandardCopyOption.REPLACE_EXISTING });
							}

							file.setExecutable(true);
							if (deleteTempFilesOnExit) {
								file.deleteOnExit();
							}

							files[i] = file;
						} else {
							throw new IllegalStateException("Unable to find the resource[" + resourceName + "] relative to the reference class[" + resourceReferenceClass + "].");
						}
					}
				} else {
					URL resource = resourceReferenceClass.getResource(resourceName);
					if (resource == null) {
						throw new IllegalStateException("Resource[" + resourceName + "] not found relative to class[" + resourceReferenceClass + "].");
					}
					File file = new File(resource.getFile());
					files[i] = file;
				}
			}
			i++;
		}
		return files;
	}

	public static boolean isInJar(Class<?> resourceReferenceClass) {
		boolean isInJar = false;
		try {
			File resourceAsFile = new File(resourceReferenceClass.getProtectionDomain().getCodeSource().getLocation().toURI());
			// if in jar will not be a directory
			isInJar = !resourceAsFile.isDirectory();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return isInJar;
	}

	public static InputStream getResourceAsInputStream(Class<?> resourceClass, String resourceName) {
		InputStream inputStream = resourceClass.getResourceAsStream(resourceName);
		if (inputStream == null) {
			throw new IllegalStateException("Unable to find resource [" + resourceName + "] relative to class [" + resourceClass.getName() + "].");
		}
		return inputStream;
	}
}
