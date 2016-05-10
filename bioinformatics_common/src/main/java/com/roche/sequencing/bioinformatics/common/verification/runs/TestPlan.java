package com.roche.sequencing.bioinformatics.common.verification.runs;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.roche.sequencing.bioinformatics.common.utils.AlphaNumericStringComparator;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.JVMUtil;
import com.roche.sequencing.bioinformatics.common.utils.LoggingUtil;
import com.roche.sequencing.bioinformatics.common.utils.Md5CheckSumUtil;
import com.roche.sequencing.bioinformatics.common.utils.OSUtil;
import com.roche.sequencing.bioinformatics.common.utils.PdfReportUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.Version;
import com.roche.sequencing.bioinformatics.common.utils.ZipUtil;
import com.roche.sequencing.bioinformatics.common.verification.AutoTestPlanCli;
import com.roche.sequencing.bioinformatics.common.verification.CliStatusConsole;

public class TestPlan {

	public final static Font SMALL_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 6, Font.NORMAL);
	public final static Font REGULAR_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 8, Font.NORMAL);
	public final static Font REGULAR_BOLD_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
	public final static Font SMALL_BOLD_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 6, Font.BOLD);
	public final static Font SMALL_HEADER_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 8, Font.BOLD);

	public final static String CONSOLE_OUTPUT_FILE_NAME = "console.output";
	public final static String CONSOLE_ERRORS_FILE_NAME = "console.errors";

	private final static String ZIP_EXTENSION = ".zip";

	private final File testPlanDirectory;
	private final List<TestPlanRun> runs;
	private final List<Precondition> preconditions;

	public TestPlan(File testPlanDirectory) {
		super();
		this.runs = new ArrayList<TestPlanRun>();
		this.preconditions = new ArrayList<Precondition>();
		this.testPlanDirectory = testPlanDirectory;
	}

	private void addRun(TestPlanRun run) {
		if (runs.contains(run)) {
			String description = run.getDescription();
			if (description == null) {
				description = "Run the following " + run.getCommand() + " command:";
			}
			throw new IllegalArgumentException("The TestPlanRun[" + description + "] has already been added to the test plan.");
		}
		runs.add(run);
	}

	private void addRuns(List<TestPlanRun> runs) {
		for (TestPlanRun run : runs) {
			addRun(run);
		}
	}

	private void addPrecondition(Precondition precondition) {
		if (preconditions.contains(precondition)) {
			throw new IllegalArgumentException("The Precondition[" + precondition + "] has already been added to the test plan.");
		}
		preconditions.add(precondition);
	}

	public boolean createTestPlan(File outputTestPlan, String applicationName) {
		return createTestPlanReport(applicationName, null, outputTestPlan, null, null, false, false, null, null);
	}

	/**
	 * 
	 * @param testerName
	 * @param outputReport
	 * @return true if test plan ran succesfully with no failures, false otherwise
	 */
	public boolean createTestPlanReport(File applicationToTest, File testPlanExecutionDirectory, File outputReport, File optionalJvmBinPath, boolean createZip, String startingFolder,
			String stoppingFolder) {
		return createTestPlanReport(null, applicationToTest, outputReport, testPlanExecutionDirectory, optionalJvmBinPath, true, createZip, startingFolder, stoppingFolder);
	}

	/**
	 * 
	 * @param testerName
	 * @param outputReport
	 * @return true if test plan ran successfully with no failures, false otherwise
	 */
	private boolean createTestPlanReport(String applicationName, File applicationToTest, File outputReport, File testPlanExecutionDirectory, File optionalJvmBinPath, boolean createReport,
			boolean createZip, String startingFolder, String stoppingFolder) {
		String startTime = DateUtil.getCurrentDateINYYYYMMDDHHMMSSwithColons();
		if (createReport && !applicationToTest.exists()) {
			throw new IllegalArgumentException("The provided applicationToTest[" + applicationToTest.getAbsolutePath() + "] does not exist.");
		}

		Document pdfDocument = PdfReportUtil.createDocument(outputReport, "", "", "", "");

		boolean wasSuccess = true;

		Element titleText = null;
		if (createReport) {
			titleText = new Phrase("Test Plan Report", REGULAR_BOLD_FONT);
		} else {
			titleText = new Phrase("Test Plan", REGULAR_BOLD_FONT);
		}

		Paragraph title = new Paragraph();
		title.setAlignment(Element.ALIGN_MIDDLE);
		title.add(titleText);
		try {
			pdfDocument.add(titleText);
		} catch (DocumentException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		Paragraph runPlanTableParagraph = new Paragraph();
		runPlanTableParagraph.add(Chunk.NEWLINE);

		PdfPTable table = new PdfPTable(6);
		try {
			table.setWidths(new float[] { 20f, 15f, 50f, 35f, 40f, 20f });
		} catch (DocumentException e1) {
			e1.printStackTrace();
		}
		table.addCell(new PdfPCell(new Phrase("Reference Type and No.", SMALL_HEADER_FONT)));
		table.addCell(new PdfPCell(new Phrase("Step Number", SMALL_HEADER_FONT)));
		table.addCell(new PdfPCell(new Phrase("Test Step", SMALL_HEADER_FONT)));
		table.addCell(new PdfPCell(new Phrase("Acceptance Criteria", SMALL_HEADER_FONT)));
		table.addCell(new PdfPCell(new Phrase("Actual Results", SMALL_HEADER_FONT)));
		table.addCell(new PdfPCell(new Phrase("Results (Pass/Fail)", SMALL_HEADER_FONT)));

		int totalChecks = 0;
		int passedChecks = 0;

		PdfPCell grayCell = new PdfPCell();
		grayCell.setBackgroundColor(new BaseColor(Color.LIGHT_GRAY));

		PdfPCell emptyWhiteCell = new PdfPCell();
		emptyWhiteCell.setBackgroundColor(new BaseColor(Color.WHITE));

		PdfPCell naCell = new PdfPCell(new Phrase("NA", SMALL_FONT));
		naCell.setHorizontalAlignment(Element.ALIGN_MIDDLE);

		for (Precondition precondition : preconditions) {
			table.addCell(naCell);
			table.addCell(new PdfPCell(new Phrase("Precondition", SMALL_FONT)));
			table.addCell(new PdfPCell(new Phrase(precondition.getDescription(), SMALL_FONT)));
			table.addCell(grayCell);
			table.addCell(grayCell);
			table.addCell(grayCell);
		}

		int runNumber = 1;
		int executedRuns = 0;

		List<TestPlanRun> sortedRuns = new ArrayList<TestPlanRun>(runs);
		Collections.sort(sortedRuns, new Comparator<TestPlanRun>() {
			private AlphaNumericStringComparator windowsComparator = new AlphaNumericStringComparator();

			@Override
			public int compare(TestPlanRun o1, TestPlanRun o2) {
				return windowsComparator.compare(o1.getRunDirectory().getName(), o2.getRunDirectory().getName());
			}
		});

		boolean startingFolderFound = true;
		boolean stoppingFolderFound = false;

		if (startingFolder != null) {
			boolean startingFolderExists = false;
			runLoop: for (TestPlanRun run : sortedRuns) {
				String runFolderName = run.getRunDirectory().getName();
				startingFolderExists = runFolderName.equals(startingFolder);
				if (startingFolderExists) {
					break runLoop;
				}
			}

			if (startingFolderExists) {
				startingFolderFound = false;
			} else {
				throw new IllegalStateException("Could not find startFolder[" + startingFolder + "] in test plan directory[" + testPlanDirectory.getAbsolutePath() + "].");
			}
		}

		List<String[]> notesByStepNumber = new ArrayList<String[]>();
		Set<String> allRequirements = new HashSet<String>();

		for (TestPlanRun run : sortedRuns) {
			startingFolderFound = startingFolderFound || run.getRunDirectory().getName().equals(startingFolder);

			if (startingFolderFound && !stoppingFolderFound) {

				stoppingFolderFound = run.getRunDirectory().getName().equals(stoppingFolder);

				RunResults runResults = null;
				CliStatusConsole.logStatus("");

				String description = run.getDescription();
				if (description == null) {
					description = "Run the following " + run.getCommand() + " command:";
				}

				CliStatusConsole.logStatus("Running step " + runNumber + "(" + run.getRunDirectory().getAbsolutePath() + ") : " + description);
				if (createReport) {
					runResults = executeRun(run, applicationToTest, testPlanExecutionDirectory, runNumber, optionalJvmBinPath);
				}

				if (run.getNotes() != null) {
					for (String note : run.getNotes()) {
						notesByStepNumber.add(new String[] { "" + runNumber, note });
					}
				}

				table.addCell(naCell);
				table.addCell(new PdfPCell(new Phrase("" + runNumber, SMALL_BOLD_FONT)));

				String applicationToTestName = applicationName;
				if (createReport) {
					applicationToTestName = applicationToTest.getAbsolutePath();
				}

				table.addCell(new PdfPCell(new Phrase(description + StringUtil.NEWLINE + StringUtil.NEWLINE + "java -jar " + applicationToTestName + " "
						+ ArraysUtil.toString(run.getArguments().toArray(new String[0]), " "), SMALL_FONT)));

				table.addCell(grayCell);
				table.addCell(grayCell);
				table.addCell(grayCell);

				int checkNumber = 1;

				List<TestPlanRunCheck> sortedChecks = new ArrayList<TestPlanRunCheck>(run.getChecks());
				Collections.sort(sortedChecks, new Comparator<TestPlanRunCheck>() {
					private AlphaNumericStringComparator windowsComparator = new AlphaNumericStringComparator();

					@Override
					public int compare(TestPlanRunCheck o1, TestPlanRunCheck o2) {
						return windowsComparator.compare(o1.getFileName(), o2.getFileName());
					}
				});

				for (TestPlanRunCheck check : sortedChecks) {

					String referenceTypeAndNumbers = "";
					for (String requirement : check.getRequirements()) {
						allRequirements.add(requirement);
						referenceTypeAndNumbers += requirement + StringUtil.NEWLINE;
					}
					String stepNumber = runNumber + "." + checkNumber;
					if (referenceTypeAndNumbers.isEmpty()) {
						table.addCell(naCell);
					} else {
						table.addCell(new PdfPCell(new Phrase(referenceTypeAndNumbers, SMALL_FONT)));
					}

					if (check.getNotes() != null) {
						for (String note : check.getNotes()) {
							notesByStepNumber.add(new String[] { stepNumber, note });
						}
					}

					table.addCell(new PdfPCell(new Phrase(stepNumber, SMALL_FONT)));
					table.addCell(new PdfPCell(new Phrase(check.getDescription(), SMALL_FONT)));

					if (check.isInformationOnly()) {
						table.addCell(grayCell);
					} else {
						table.addCell(new PdfPCell(new Phrase(check.getAcceptanceCriteria(), SMALL_FONT)));
					}

					if (runResults != null) {
						TestPlanRunCheckResult checkResult = check.check(runResults);

						table.addCell(new PdfPCell(new Phrase(checkResult.getResultDescription(), SMALL_FONT)));

						boolean wasCheckSuccess = checkResult.isPassed();

						if (check.isInformationOnly()) {
							table.addCell(grayCell);
						} else if (wasCheckSuccess) {
							table.addCell(new PdfPCell(new Phrase("Pass", SMALL_FONT)));
							CliStatusConsole.logStatus("  Check " + stepNumber + " passed : " + check.getDescription());
						} else {
							table.addCell(new PdfPCell(new Phrase("Fail", SMALL_FONT)));
							CliStatusConsole.logStatus("  Check " + stepNumber + " failed : " + check.getDescription());
							CliStatusConsole.logStatus("    Reason for Failure: " + checkResult.getResultDescription());
						}

						if (wasCheckSuccess) {
							passedChecks++;
						}

						wasSuccess = wasSuccess && wasCheckSuccess;
					} else {
						if (check.isInformationOnly()) {
							table.addCell(new PdfPCell(new Phrase("Console Output:               ", SMALL_FONT)));
							table.addCell(grayCell);
						} else {
							table.addCell(emptyWhiteCell);
							table.addCell(emptyWhiteCell);
						}
					}
					checkNumber++;
					totalChecks++;

				}
				executedRuns++;
			}
			runNumber++;
		}
		runPlanTableParagraph.add(table);

		try {
			pdfDocument.add(runPlanTableParagraph);
		} catch (DocumentException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		if (notesByStepNumber.size() > 0) {
			Paragraph notesTableParagraph = new Paragraph();
			notesTableParagraph.add(Chunk.NEWLINE);

			notesTableParagraph.add(new Phrase("Notes", REGULAR_BOLD_FONT));

			PdfPTable notesTable = new PdfPTable(2);
			try {
				notesTable.setWidths(new float[] { 15f, 165f });
			} catch (DocumentException e1) {
				e1.printStackTrace();
			}
			notesTable.addCell(new PdfPCell(new Phrase("Step Number", SMALL_HEADER_FONT)));
			notesTable.addCell(new PdfPCell(new Phrase("Notes", SMALL_HEADER_FONT)));

			for (String[] entry : notesByStepNumber) {
				notesTable.addCell(new PdfPCell(new Phrase(entry[0], SMALL_FONT)));
				notesTable.addCell(new PdfPCell(new Phrase(entry[1], SMALL_FONT)));
			}

			notesTableParagraph.add(notesTable);
			notesTableParagraph.add(Chunk.NEWLINE);
			try {
				pdfDocument.add(notesTableParagraph);
			} catch (DocumentException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		if (allRequirements.size() > 0) {
			Paragraph requirementsParagraph = new Paragraph();
			requirementsParagraph.setLeading(7, 0);
			requirementsParagraph.add(Chunk.NEWLINE);
			requirementsParagraph.add(new Phrase("Tested Requirements", REGULAR_BOLD_FONT));
			requirementsParagraph.add(Chunk.NEWLINE);
			requirementsParagraph.add(Chunk.NEWLINE);
			List<String> sortedRequirements = new ArrayList<String>(allRequirements);
			Collections.sort(sortedRequirements, new AlphaNumericStringComparator());

			for (String requirement : sortedRequirements) {
				requirementsParagraph.add(new Phrase(requirement, SMALL_FONT));
				requirementsParagraph.add(Chunk.NEWLINE);
			}

			try {
				pdfDocument.add(requirementsParagraph);
			} catch (DocumentException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		if (createReport) {
			String stopTime = DateUtil.getCurrentDateINYYYYMMDDHHMMSSwithColons();
			// provide details about the system
			Paragraph executionDetailsParagraph = new Paragraph();
			executionDetailsParagraph.setLeading(9, 0);
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(new Phrase("Test Plan Report Execution Details", REGULAR_BOLD_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(new Phrase("Report generated by " + AutoTestPlanCli.APPLICATION_NAME + " " + AutoTestPlanCli.getApplicationVersion(), REGULAR_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
			boolean isRunningWithinJar = AutoTestPlanCli.class.getResource("AutoTestPlanCli.class").toString().startsWith("jar");
			if (isRunningWithinJar) {
				try {
					File jarFile = new java.io.File(AutoTestPlanCli.class.getProtectionDomain().getCodeSource().getLocation().getPath());
					executionDetailsParagraph.add(new Phrase("Generating Software Jar File: " + jarFile.getAbsolutePath(), REGULAR_FONT));
					executionDetailsParagraph.add(Chunk.NEWLINE);
				} catch (Exception e) {
					// Not a big deal if the generating software is not included
				}
			}
			String userName = System.getProperty("user.name");
			if (userName != null && !userName.isEmpty()) {
				executionDetailsParagraph.add(new Phrase("Report generation initiated by: " + userName, REGULAR_FONT));
				executionDetailsParagraph.add(Chunk.NEWLINE);
			}

			try {
				InetAddress address = InetAddress.getLocalHost();
				if (address != null) {
					executionDetailsParagraph.add(new Phrase("Computer IP Address: " + address.getHostAddress(), REGULAR_FONT));
					executionDetailsParagraph.add(Chunk.NEWLINE);
					String hostName = address.getHostName();
					if (hostName != null && !hostName.isEmpty()) {
						executionDetailsParagraph.add(new Phrase("Computer Host Name: " + hostName, REGULAR_FONT));
						executionDetailsParagraph.add(Chunk.NEWLINE);
					}
				}
			} catch (UnknownHostException ex) {
			}
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(new Phrase("Application to Test: " + applicationToTest.getAbsolutePath(), REGULAR_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);

			try {
				String md5Sum = Md5CheckSumUtil.md5sum(applicationToTest);
				executionDetailsParagraph.add(new Phrase("Jar File Md5Sum: " + md5Sum, REGULAR_FONT));
				executionDetailsParagraph.add(Chunk.NEWLINE);
				executionDetailsParagraph.add(new Phrase("Jar File Location: " + applicationToTest.getAbsolutePath(), REGULAR_FONT));
				executionDetailsParagraph.add(Chunk.NEWLINE);
			} catch (IOException e1) {
			}

			executionDetailsParagraph.add(new Phrase("Test Plan CheckSum: " + checkSum(), REGULAR_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(new Phrase("Test Plan Directory: " + testPlanDirectory.getAbsolutePath(), REGULAR_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(new Phrase("Test Plan Results Directory: " + testPlanExecutionDirectory.getAbsolutePath(), REGULAR_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(new Phrase("Start Time: " + startTime + " (yyyy:MM:dd HH:mm:ss)", REGULAR_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(new Phrase("Stop Time: " + stopTime + " (yyyy:MM:dd HH:mm:ss)", REGULAR_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(new Phrase("Operating System: " + OSUtil.getOsName() + " " + OSUtil.getOsBits() + "-bit", REGULAR_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
			String javaBin = getJavaBinPath(optionalJvmBinPath);
			executionDetailsParagraph.add(new Phrase("Java Bin Path: " + javaBin, REGULAR_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
			Version javaVersion = JVMUtil.getJavaVersion(new File(javaBin));
			executionDetailsParagraph.add(new Phrase("Java Version: " + javaVersion, REGULAR_FONT));
			try {
				pdfDocument.add(executionDetailsParagraph);
			} catch (DocumentException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		} else {
			// provide details about the system
			Paragraph creationDetailsParagraph = new Paragraph();
			creationDetailsParagraph.setLeading(9, 0);
			creationDetailsParagraph.add(Chunk.NEWLINE);
			creationDetailsParagraph.add(Chunk.NEWLINE);
			creationDetailsParagraph.add(Chunk.NEWLINE);
			creationDetailsParagraph.add(new Phrase("Test Plan Creation Details", REGULAR_BOLD_FONT));
			creationDetailsParagraph.add(Chunk.NEWLINE);
			creationDetailsParagraph.add(Chunk.NEWLINE);
			creationDetailsParagraph.add(new Phrase("Report generated by " + AutoTestPlanCli.APPLICATION_NAME + " " + AutoTestPlanCli.getApplicationVersion(), REGULAR_FONT));

			boolean isRunningWithinJar = AutoTestPlanCli.class.getResource("AutoTestPlanCli.class").toString().startsWith("jar");
			if (isRunningWithinJar) {
				try {
					File jarFile = new java.io.File(AutoTestPlanCli.class.getProtectionDomain().getCodeSource().getLocation().getPath());
					creationDetailsParagraph.add(Chunk.NEWLINE);
					creationDetailsParagraph.add(new Phrase("Generating Software Jar File: " + jarFile.getAbsolutePath(), REGULAR_FONT));
				} catch (Exception e) {
					// Not a big deal if the generating software is not included
				}
			}

			creationDetailsParagraph.add(Chunk.NEWLINE);
			String userName = System.getProperty("user.name");
			if (userName != null && !userName.isEmpty()) {
				creationDetailsParagraph.add(new Phrase("Report generation initiated by: " + userName, REGULAR_FONT));
				creationDetailsParagraph.add(Chunk.NEWLINE);
			}

			try {
				InetAddress address = InetAddress.getLocalHost();
				if (address != null) {
					creationDetailsParagraph.add(new Phrase("Computer IP Address: " + address.getHostAddress(), REGULAR_FONT));
					creationDetailsParagraph.add(Chunk.NEWLINE);
					String hostName = address.getHostName();
					if (hostName != null && !hostName.isEmpty()) {
						creationDetailsParagraph.add(new Phrase("Computer Host Name: " + hostName, REGULAR_FONT));
						creationDetailsParagraph.add(Chunk.NEWLINE);
					}
				}
			} catch (UnknownHostException ex) {
			}

			creationDetailsParagraph.add(new Phrase("Test Plan CheckSum: " + checkSum(), REGULAR_FONT));
			creationDetailsParagraph.add(Chunk.NEWLINE);
			creationDetailsParagraph.add(new Phrase("Test Plan Directory: " + testPlanDirectory.getAbsolutePath(), REGULAR_FONT));
			creationDetailsParagraph.add(Chunk.NEWLINE);

			try {
				pdfDocument.add(creationDetailsParagraph);
			} catch (DocumentException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		Paragraph summaryParagraph = new Paragraph();
		if (createReport) {
			summaryParagraph.add(Chunk.NEWLINE);
			summaryParagraph.add(new Phrase("Test Plan Creation Details", REGULAR_BOLD_FONT));
			summaryParagraph.add(Chunk.NEWLINE);
			int totalRuns = runNumber - 1;
			if (totalRuns > 1) {
				summaryParagraph.add(new Phrase(totalRuns + " total runs.", REGULAR_FONT));
			} else {
				summaryParagraph.add(new Phrase("1 total run.", REGULAR_FONT));
			}
			summaryParagraph.add(Chunk.NEWLINE);
			if ((executedRuns > 1) || (executedRuns == 0)) {
				summaryParagraph.add(new Phrase(executedRuns + " runs executed.", REGULAR_FONT));
			} else {
				summaryParagraph.add(new Phrase("1 run executed.", REGULAR_FONT));
			}
			summaryParagraph.add(Chunk.NEWLINE);
			summaryParagraph.add(new Phrase(passedChecks + " out of " + totalChecks + " checks passed.", REGULAR_FONT));
			summaryParagraph.add(Chunk.NEWLINE);
			summaryParagraph.add(Chunk.NEWLINE);
			if (wasSuccess) {
				summaryParagraph.add(new Phrase("The test plan PASSED.", REGULAR_FONT));
			} else {
				summaryParagraph.add(new Phrase("The test plan FAILED.", REGULAR_FONT));
			}
		}

		try {
			pdfDocument.add(summaryParagraph);
		} catch (DocumentException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		pdfDocument.close();
		CliStatusConsole.logStatus("");
		if (createReport) {
			int totalRuns = runNumber - 1;
			if (totalRuns > 1) {
				CliStatusConsole.logStatus(totalRuns + " total runs.");
			} else {
				CliStatusConsole.logStatus("1 total run.");
			}
			if ((executedRuns > 1) || (executedRuns == 0)) {
				CliStatusConsole.logStatus(executedRuns + " runs executed.");
			} else {
				CliStatusConsole.logStatus("1 run executed.");
			}
			CliStatusConsole.logStatus(passedChecks + " out of " + totalChecks + " checks passed.");

			if (wasSuccess) {
				CliStatusConsole.logStatus("The test plan PASSED.");
			} else {
				CliStatusConsole.logStatus("The test plan FAILED.");
			}
			CliStatusConsole.logStatus("");
			CliStatusConsole.logStatus("Test Plan Report was written to " + outputReport.getAbsolutePath() + ".");
		} else {
			CliStatusConsole.logStatus("Test Plan was written to " + outputReport.getAbsolutePath() + ".");
		}

		CliStatusConsole.logStatus("");

		if (createZip) {
			String zipFileName = testPlanExecutionDirectory.getName() + "_" + DateUtil.getCurrentDateINYYYYMMDDHHMMSS() + ZIP_EXTENSION;
			File outputZipFile = new File(testPlanExecutionDirectory.getParentFile(), zipFileName);
			CliStatusConsole.logStatus("Creating results Zip file at [" + outputZipFile + "].");
			List<File> directoriesAndFilesToZip = new ArrayList<File>();
			directoriesAndFilesToZip.add(testPlanExecutionDirectory);
			directoriesAndFilesToZip.add(testPlanDirectory);
			directoriesAndFilesToZip.add(outputReport);
			directoriesAndFilesToZip.add(applicationToTest);
			directoriesAndFilesToZip.add(LoggingUtil.getLogFile());

			boolean isRunningWithinJar = TestPlan.class.getResource("TestPlan.class").toString().startsWith("jar");
			if (isRunningWithinJar) {
				try {
					File jarFile = new java.io.File(TestPlan.class.getProtectionDomain().getCodeSource().getLocation().getPath());
					directoriesAndFilesToZip.add(jarFile);
				} catch (Exception e) {
					CliStatusConsole.logStatus("Unable to zip the AutoTestPlan application within the zipped results.");
				}
			}

			ZipUtil.zipDirectoriesAndFiles(outputZipFile, directoriesAndFilesToZip);
			CliStatusConsole.logStatus("Done creating results Zip file at [" + outputZipFile + "].");
			CliStatusConsole.logStatus("");
			CliStatusConsole.logStatus("Deleting results directory at [" + testPlanExecutionDirectory + "].");
			try {
				FileUtil.deleteDirectory(testPlanExecutionDirectory);
				CliStatusConsole.logStatus("Done deleting results directory at [" + testPlanExecutionDirectory + "].");
			} catch (IOException e) {
				CliStatusConsole.logStatus("Unable to deleting the results directory at [" + testPlanExecutionDirectory + "].");
				CliStatusConsole.logError(e);
			}
		}

		return wasSuccess;
	}

	private String getJavaBinPath(File optionalJvmBinPath) {
		String javaBin = "";
		if (optionalJvmBinPath != null) {
			javaBin = optionalJvmBinPath.getAbsolutePath();
		} else {
			String javaHome = System.getProperty("java.home");
			javaBin = javaHome + File.separator + "bin" + File.separator;
		}
		return javaBin;
	}

	private RunResults executeRun(TestPlanRun run, File applicationToTest, File testPlanExecutionDirectory, int stepNumber, File optionalJvmBinPath) {

		String javaBin = getJavaBinPath(optionalJvmBinPath);

		List<String> processBuilderArguments = new ArrayList<String>();
		processBuilderArguments.add(new File(javaBin, "java").getAbsolutePath());
		processBuilderArguments.add("-jar");
		processBuilderArguments.add(applicationToTest.getAbsolutePath());
		processBuilderArguments.addAll(run.getArguments());

		ProcessBuilder processBuilder = new ProcessBuilder(processBuilderArguments);

		// set the java working directory
		File outputDirectory = new File(testPlanExecutionDirectory, "run_" + stepNumber);

		if (outputDirectory.exists()) {
			throw new IllegalStateException("The provided Test Plan Execution Directory[" + testPlanExecutionDirectory.getAbsolutePath()
					+ "] already contains results.  Please provide an empty Test Plan Execution Directory.");
		}

		try {
			FileUtil.createDirectory(outputDirectory);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create the execution directory for run " + stepNumber + " at [" + outputDirectory.getAbsolutePath() + "].", e);
		}
		processBuilder.directory(outputDirectory);

		for (Precondition precondition : preconditions) {
			precondition.checkPrecondition(outputDirectory, new File(javaBin));
		}

		String consoleOutputString = "";
		String consoleErrorsString = "";

		try {
			Process process = processBuilder.start();
			StreamListener outputStreamListener = new StreamListener(process.getInputStream());
			StreamListener errorStreamListener = new StreamListener(process.getErrorStream());
			process.waitFor();
			consoleOutputString = outputStreamListener.getString();
			consoleErrorsString = errorStreamListener.getString();
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException("Unable to execute the provide jar file[" + applicationToTest.getAbsolutePath() + "].  " + e.getMessage());
		}

		try {
			FileUtil.writeStringToFile(new File(outputDirectory, CONSOLE_OUTPUT_FILE_NAME), consoleOutputString);
			FileUtil.writeStringToFile(new File(outputDirectory, CONSOLE_ERRORS_FILE_NAME), consoleErrorsString);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		File resultsDirectory = outputDirectory;
		if (run.getResultsSubDirectory() != null) {
			resultsDirectory = new File(resultsDirectory, run.getResultsSubDirectory());
		}

		return new RunResults(consoleOutputString, consoleErrorsString, resultsDirectory, run.getRunDirectory());
	}

	private static class StreamListener {
		private final StringBuilder string;

		public StreamListener(final InputStream inputStream) {
			string = new StringBuilder();
			new Thread(new Runnable() {
				public void run() {
					try (Scanner sc = new Scanner(inputStream)) {
						while (sc.hasNextLine()) {
							string.append(sc.nextLine() + StringUtil.NEWLINE);
						}
					}
				}
			}).start();
		}

		public String getString() {
			return string.toString();
		}
	}

	public static TestPlan readFromDirectory(File testPlanDirectory) {
		TestPlan testPlan = new TestPlan(testPlanDirectory);

		List<Precondition> preconditions = Precondition.readFromDirectory(testPlanDirectory);
		for (Precondition precondition : preconditions) {
			testPlan.addPrecondition(precondition);
		}

		testPlan.addRuns(recursivelyReadRunsFromDirectory(testPlanDirectory, testPlanDirectory, null));

		return testPlan;
	}

	private static List<TestPlanRun> recursivelyReadRunsFromDirectory(File directory, File testPlanDirectory, TestPlanRun runSettingsToInherit) {
		List<TestPlanRun> runs = new ArrayList<TestPlanRun>();

		TestPlanRun run = TestPlanRun.readFromDirectory(testPlanDirectory, directory, runSettingsToInherit);

		boolean isRunsFoundInSubDirectories = false;
		File[] subdirectories = FileUtil.getSubDirectories(directory);
		for (File subdirectory : subdirectories) {
			List<TestPlanRun> runsFoundInSubDirectories = null;

			if (run == null) {
				runsFoundInSubDirectories = recursivelyReadRunsFromDirectory(subdirectory, testPlanDirectory, runSettingsToInherit);
			} else {
				runsFoundInSubDirectories = recursivelyReadRunsFromDirectory(subdirectory, testPlanDirectory, run);
			}

			if (runsFoundInSubDirectories.size() > 0) {
				isRunsFoundInSubDirectories = true;
				runs.addAll(runsFoundInSubDirectories);
			}
		}

		if (run != null && !isRunsFoundInSubDirectories) {
			runs.add(run);
		}

		return runs;
	}

	public long checkSum() {
		final int prime = 31;
		long result = 1;
		if (preconditions != null) {
			for (Precondition precondition : preconditions) {
				result = prime * result + precondition.checkSum();
			}
		}

		if (runs != null) {
			for (TestPlanRun run : runs) {
				result = prime * result + run.checkSum();
			}
		}

		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((preconditions == null) ? 0 : preconditions.hashCode());
		result = prime * result + ((runs == null) ? 0 : runs.hashCode());
		result = prime * result + ((testPlanDirectory == null) ? 0 : testPlanDirectory.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TestPlan other = (TestPlan) obj;
		if (preconditions == null) {
			if (other.preconditions != null)
				return false;
		} else if (!preconditions.equals(other.preconditions))
			return false;
		if (runs == null) {
			if (other.runs != null)
				return false;
		} else if (!runs.equals(other.runs))
			return false;
		if (testPlanDirectory == null) {
			if (other.testPlanDirectory != null)
				return false;
		} else if (!testPlanDirectory.equals(other.testPlanDirectory))
			return false;
		return true;
	}

	public static void main(String[] args) {
		AlphaNumericStringComparator comp = new AlphaNumericStringComparator();
		String[] strings = new String[] { "run01", "run1", "run2", "run02", "run10", "run11", "run12", "run7" };
		Arrays.sort(strings, comp);
		System.out.println(ArraysUtil.toString(strings, ","));

	}

}
