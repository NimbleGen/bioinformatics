package com.roche.bioinformatics.common.verification.runs;

import java.awt.Color;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
import com.roche.bioinformatics.common.verification.AutoTestPlanCli;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.JVMUtil;
import com.roche.sequencing.bioinformatics.common.utils.OSUtil;
import com.roche.sequencing.bioinformatics.common.utils.PdfReportUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.Version;

public class TestPlan {

	public final static Font SMALL_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 6, Font.NORMAL);
	public final static Font REGULAR_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 8, Font.NORMAL);
	public final static Font REGULAR_BOLD_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
	public final static Font SMALL_BOLD_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 6, Font.BOLD);
	public final static Font SMALL_HEADER_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 8, Font.BOLD);

	private final static String CONSOLE_OUTPUT_FILE_NAME = "console_output.txt";
	private final static String CONSOLE_ERRORS_FILE_NAME = "console_errors.txt";

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
			throw new IllegalArgumentException("The TestPlanRun[" + run.getDescription() + "] has already been added to the test plan.");
		}
		runs.add(run);
	}

	private void addPrecondition(Precondition precondition) {
		if (preconditions.contains(precondition)) {
			throw new IllegalArgumentException("The Precondition[" + precondition + "] has already been added to the test plan.");
		}
		preconditions.add(precondition);
	}

	public boolean createTestPlan(File outputTestPlan) {
		return createTestPlanReport(null, outputTestPlan, null, null, false);
	}

	/**
	 * 
	 * @param testerName
	 * @param outputReport
	 * @return true if test plan ran succesfully with no failures, false otherwise
	 */
	public boolean createTestPlanReport(File applicationToTest, File testPlanExecutionDirectory, File outputReport, File optionalJvmBinPath) {
		return createTestPlanReport(applicationToTest, outputReport, testPlanExecutionDirectory, optionalJvmBinPath, true);
	}

	/**
	 * 
	 * @param testerName
	 * @param outputReport
	 * @return true if test plan ran successfully with no failures, false otherwise
	 */
	private boolean createTestPlanReport(File applicationToTest, File outputReport, File testPlanExecutionDirectory, File optionalJvmBinPath, boolean createReport) {
		String startTime = DateUtil.getCurrentDateINYYYYMMDDHHMMSSwithColons();

		if (createReport && !applicationToTest.exists()) {
			throw new IllegalArgumentException("The provided applicationToTest[" + applicationToTest.getAbsolutePath() + "] does not exist.");
		}

		Document pdfDocument = PdfReportUtil.createDocument(outputReport, "", "", "", "");

		boolean wasSuccess = true;

		// writeBeginningOfReport(pdfDocument);

		Paragraph chapter = new Paragraph();
		chapter.add(Chunk.NEWLINE);

		if (createReport) {
			chapter.add(new Phrase("Test Plan", REGULAR_BOLD_FONT));
		} else {
			chapter.add(new Phrase("Test Plan Report", REGULAR_BOLD_FONT));
		}

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
		table.addCell(new PdfPCell(new Phrase("Acceptance Results", SMALL_HEADER_FONT)));
		table.addCell(new PdfPCell(new Phrase("Results (Pass/Fail)", SMALL_HEADER_FONT)));

		int totalChecks = 0;
		int passedChecks = 0;

		PdfPCell grayCell = new PdfPCell();
		grayCell.setBackgroundColor(new BaseColor(Color.LIGHT_GRAY));

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
		for (TestPlanRun run : runs) {
			RunResults runResults = null;
			System.out.println();
			System.out.println("Running step " + runNumber + " : " + run.getDescription());
			if (createReport) {
				runResults = executeRun(run, applicationToTest, testPlanExecutionDirectory, runNumber, optionalJvmBinPath);
			}

			table.addCell(naCell);
			table.addCell(new PdfPCell(new Phrase("" + runNumber, SMALL_BOLD_FONT)));
			table.addCell(new PdfPCell(new Phrase(run.getDescription() + StringUtil.NEWLINE + StringUtil.NEWLINE + "java -jar " + ArraysUtil.toString(run.getArguments().toArray(new String[0]), " "),
					SMALL_FONT)));

			table.addCell(grayCell);
			table.addCell(grayCell);
			table.addCell(grayCell);

			if (run.getChecks().size() == 0) {
				throw new IllegalStateException("The provided step[" + run.getDescription() + "] has not checks which make it an invalid test.");
			}

			int checkNumber = 1;
			for (TestPlanRunCheck check : run.getChecks()) {

				String referenceTypeAndNumbers = "";
				for (String requirement : check.getRequirements()) {
					referenceTypeAndNumbers += requirement + StringUtil.NEWLINE;
				}
				String stepNumber = runNumber + "." + checkNumber;
				if (referenceTypeAndNumbers.isEmpty()) {
					table.addCell(naCell);
				} else {
					table.addCell(new PdfPCell(new Phrase(referenceTypeAndNumbers, SMALL_FONT)));
				}

				table.addCell(new PdfPCell(new Phrase(stepNumber, SMALL_FONT)));
				table.addCell(new PdfPCell(new Phrase(check.getDescription(), SMALL_FONT)));
				table.addCell(new PdfPCell(new Phrase(check.getAcceptanceCriteria(), SMALL_FONT)));

				if (runResults != null) {
					TestPlanRunCheckResult checkResult = check.check(runResults);
					table.addCell(new PdfPCell(new Phrase(checkResult.getResultDescription(), SMALL_FONT)));

					boolean wasCheckSuccess = checkResult.isPassed();

					if (wasCheckSuccess) {
						table.addCell(new PdfPCell(new Phrase("Pass", SMALL_FONT)));
						System.out.println("  Check " + stepNumber + " passed : " + check.getDescription());
					} else {
						table.addCell(new PdfPCell(new Phrase("Fail", SMALL_FONT)));
						System.out.println("  Check " + stepNumber + " failed : " + check.getDescription());
					}

					if (wasCheckSuccess) {
						passedChecks++;
					}

					wasSuccess = wasSuccess && wasCheckSuccess;
				} else {
					table.addCell(new PdfPCell(new Phrase("", SMALL_FONT)));
					table.addCell(new PdfPCell(new Phrase("", SMALL_FONT)));
				}
				checkNumber++;
				totalChecks++;

			}
			runNumber++;
		}
		chapter.add(table);

		try {
			pdfDocument.add(chapter);
		} catch (DocumentException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		if (createReport) {
			String stopTime = DateUtil.getCurrentDateINYYYYMMDDHHMMSSwithColons();
			// provide details about the system
			Paragraph chapter2 = new Paragraph();
			chapter2.add(Chunk.NEWPAGE);
			chapter2.add(new Phrase("Test Plan Report Execution Details", REGULAR_BOLD_FONT));
			chapter2.add(Chunk.NEWLINE);
			chapter2.add(new Phrase("Report generated by the " + AutoTestPlanCli.APPLICATION_NAME + " application version: " + AutoTestPlanCli.getApplicationVersion(), REGULAR_FONT));
			chapter2.add(Chunk.NEWLINE);
			String userName = System.getProperty("user.name");
			if (userName != null && !userName.isEmpty()) {
				chapter2.add(new Phrase("Report generation initiated by: " + userName, REGULAR_FONT));
				chapter2.add(Chunk.NEWLINE);
			}

			try {
				InetAddress address = InetAddress.getLocalHost();
				if (address != null) {
					chapter2.add(new Phrase("Computer IP Address: " + address.getHostAddress(), REGULAR_FONT));
					chapter2.add(Chunk.NEWLINE);
					String hostName = address.getHostName();
					if (hostName != null && !hostName.isEmpty()) {
						chapter2.add(new Phrase("Computer Host Name: " + hostName, REGULAR_FONT));
						chapter2.add(Chunk.NEWLINE);
					}
				}
			} catch (UnknownHostException ex) {
			}
			chapter2.add(Chunk.NEWLINE);
			chapter2.add(new Phrase("Application to Test: " + applicationToTest.getAbsolutePath(), REGULAR_FONT));
			chapter2.add(Chunk.NEWLINE);
			chapter2.add(new Phrase("Test Plan Directory: " + testPlanDirectory.getAbsolutePath(), REGULAR_FONT));
			chapter2.add(Chunk.NEWLINE);
			chapter2.add(new Phrase("Test Plan Results Directory: " + testPlanExecutionDirectory.getAbsolutePath(), REGULAR_FONT));
			chapter2.add(Chunk.NEWLINE);
			chapter2.add(new Phrase("Start Time: " + startTime, REGULAR_FONT));
			chapter2.add(Chunk.NEWLINE);
			chapter2.add(new Phrase("Stop Time: " + stopTime, REGULAR_FONT));
			chapter2.add(Chunk.NEWLINE);
			chapter2.add(Chunk.NEWLINE);
			chapter2.add(new Phrase("Operating System: " + OSUtil.getOsName() + " " + OSUtil.getOsBits() + "-bit", REGULAR_FONT));
			chapter2.add(Chunk.NEWLINE);
			String javaBin = getJavaBinPath(optionalJvmBinPath);
			chapter2.add(new Phrase("Java Bin Path: " + javaBin, REGULAR_FONT));
			chapter2.add(Chunk.NEWLINE);
			Version javaVersion = JVMUtil.getJavaVersion(new File(javaBin));
			chapter2.add(new Phrase("Java Version: " + javaVersion, REGULAR_FONT));
			try {
				pdfDocument.add(chapter2);
			} catch (DocumentException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		pdfDocument.close();

		if (createReport) {

			System.out.println(runNumber + " total runs.");
			System.out.println(passedChecks + " out of " + totalChecks + " checks passed.");

			if (wasSuccess) {
				System.out.println("The test plan PASSED.");
			} else {
				System.out.println("The test plan FAILED.");
			}

			System.out.println("Test Plan Report was written to " + outputReport.getAbsolutePath() + ".");
		} else {
			System.out.println("Test Plan was written to " + outputReport.getAbsolutePath() + ".");
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

		return new RunResults(consoleOutputString, consoleErrorsString, outputDirectory, run.getRunDirectory());
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

		String[] subdirectories = testPlanDirectory.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});

		for (String subdirectoryName : subdirectories) {
			File subdirectory = new File(testPlanDirectory, subdirectoryName);
			TestPlanRun run = TestPlanRun.readFromDirectory(subdirectory);
			if (run != null) {
				testPlan.addRun(run);
			}
		}

		return testPlan;
	}
}
