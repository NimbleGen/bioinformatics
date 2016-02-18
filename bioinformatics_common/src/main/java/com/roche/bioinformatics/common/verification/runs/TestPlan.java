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
import com.roche.bioinformatics.common.verification.CliStatusConsole;
import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.JVMUtil;
import com.roche.sequencing.bioinformatics.common.utils.Md5CheckSumUtil;
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
			CliStatusConsole.logStatus("");
			CliStatusConsole.logStatus("Running step " + runNumber + " : " + run.getDescription());
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
						CliStatusConsole.logStatus("  Check " + stepNumber + " passed : " + check.getDescription());
					} else {
						table.addCell(new PdfPCell(new Phrase("Fail", SMALL_FONT)));
						CliStatusConsole.logStatus("  Check " + stepNumber + " failed : " + check.getDescription());
					}

					if (wasCheckSuccess) {
						passedChecks++;
					}

					wasSuccess = wasSuccess && wasCheckSuccess;
				} else {
					table.addCell(grayCell);
					table.addCell(grayCell);
				}
				checkNumber++;
				totalChecks++;

			}
			runNumber++;
		}
		runPlanTableParagraph.add(table);

		try {
			pdfDocument.add(runPlanTableParagraph);
		} catch (DocumentException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		if (createReport) {
			String stopTime = DateUtil.getCurrentDateINYYYYMMDDHHMMSSwithColons();
			// provide details about the system
			Paragraph executionDetailsParagraph = new Paragraph();
			executionDetailsParagraph.add(new Phrase("Test Plan Report Execution Details", REGULAR_BOLD_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(new Phrase("Report generated by " + AutoTestPlanCli.APPLICATION_NAME + " " + AutoTestPlanCli.getApplicationVersion(), REGULAR_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
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
			} catch (IOException e1) {
			}

			executionDetailsParagraph.add(new Phrase("Test Plan CheckSum: " + checkSum(), REGULAR_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(new Phrase("Test Plan Directory: " + testPlanDirectory.getAbsolutePath(), REGULAR_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(new Phrase("Test Plan Results Directory: " + testPlanExecutionDirectory.getAbsolutePath(), REGULAR_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(new Phrase("Start Time: " + startTime, REGULAR_FONT));
			executionDetailsParagraph.add(Chunk.NEWLINE);
			executionDetailsParagraph.add(new Phrase("Stop Time: " + stopTime, REGULAR_FONT));
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
			creationDetailsParagraph.add(new Phrase("Test Plan Creation Details", REGULAR_BOLD_FONT));
			creationDetailsParagraph.add(Chunk.NEWLINE);
			creationDetailsParagraph.add(new Phrase("Report generated by " + AutoTestPlanCli.APPLICATION_NAME + " " + AutoTestPlanCli.getApplicationVersion(), REGULAR_FONT));
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

		pdfDocument.close();

		if (createReport) {

			CliStatusConsole.logStatus(runNumber + " total runs.");
			CliStatusConsole.logStatus(passedChecks + " out of " + totalChecks + " checks passed.");

			if (wasSuccess) {
				CliStatusConsole.logStatus("The test plan PASSED.");
			} else {
				CliStatusConsole.logStatus("The test plan FAILED.");
			}

			CliStatusConsole.logStatus("Test Plan Report was written to " + outputReport.getAbsolutePath() + ".");
		} else {
			CliStatusConsole.logStatus("Test Plan was written to " + outputReport.getAbsolutePath() + ".");
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

}
