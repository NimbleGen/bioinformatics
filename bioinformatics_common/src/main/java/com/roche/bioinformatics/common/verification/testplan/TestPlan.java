package com.roche.bioinformatics.common.verification.testplan;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.yaml.snakeyaml.Yaml;

import com.itextpdf.text.Chapter;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.roche.bioinformatics.common.verification.runs.RunResults;
import com.roche.bioinformatics.common.verification.runs.TestPlanRun;
import com.roche.bioinformatics.common.verification.runs.TestPlanRunCheck;
import com.roche.bioinformatics.common.verification.runs.TestPlanRunCheckResult;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.PdfReportUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class TestPlan {

	public static Font SMALL_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 6, Font.NORMAL);
	public static Font SMALL_BOLD_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 6, Font.BOLD);
	public static Font SMALL_HEADER_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 8, Font.BOLD);

	private final String testPlanTitle;
	private final VersionHistorySection versionHistorySection;
	private final ApproversSection approversSection;
	private final ReviewersSection reviewersSection;
	private final DefinitionsSection definitionsSection;
	private final ReferencesSection referencesSection;
	private final RequirementsSection requirementsSection;
	private final NotesSection notesSection;
	private final String scopeSection;
	private final List<TestPlanRun> runs;

	public TestPlan(String testPlanTitle, String scope) {
		super();
		this.testPlanTitle = testPlanTitle;
		this.approversSection = new ApproversSection();
		this.versionHistorySection = new VersionHistorySection();
		this.reviewersSection = new ReviewersSection();
		this.definitionsSection = new DefinitionsSection();
		this.referencesSection = new ReferencesSection();
		this.requirementsSection = new RequirementsSection();
		this.notesSection = new NotesSection();
		this.scopeSection = scope;
		this.runs = new ArrayList<TestPlanRun>();
	}

	public String getScopeSection() {
		return scopeSection;
	}

	public String getTestPlanTitle() {
		return testPlanTitle;
	}

	public ApproversSection getApproversSection() {
		return approversSection;
	}

	public ReviewersSection getReviewersSection() {
		return reviewersSection;
	}

	public VersionHistorySection getVersionHistorySection() {
		return versionHistorySection;
	}

	public DefinitionsSection getDefinitionsSection() {
		return definitionsSection;
	}

	public ReferencesSection getReferencesSection() {
		return referencesSection;
	}

	public RequirementsSection getRequirementsSection() {
		return requirementsSection;
	}

	public NotesSection getNotesSection() {
		return notesSection;
	}

	public void addRun(TestPlanRun run) {
		if (runs.contains(run)) {
			throw new IllegalArgumentException("The TestPlanRun[" + run.getDescription() + "] has already been added to the test plan.");
		}
		runs.add(run);
	}

	public boolean createTestPlan(File outputTestPlan) {
		return createTestPlanReport(null, null, outputTestPlan, null, false);
	}

	/**
	 * 
	 * @param testerName
	 * @param outputReport
	 * @return true if test plan ran succesfully with no failures, false otherwise
	 */
	public boolean createTestPlanReport(String testerName, File applicationToTest, File testPlanExecutionDirectory, File outputReport) {
		return createTestPlanReport(testerName, applicationToTest, outputReport, testPlanExecutionDirectory, true);
	}

	/**
	 * 
	 * @param testerName
	 * @param outputReport
	 * @return true if test plan ran successfully with no failures, false otherwise
	 */
	private boolean createTestPlanReport(String testerName, File applicationToTest, File outputReport, File testPlanExecutionDirectory, boolean createReport) {
		if (createReport && !applicationToTest.exists()) {
			throw new IllegalArgumentException("The provided applicationToTest[" + applicationToTest.getAbsolutePath() + "] does not exist.");
		}

		testPlanExecutionDirectory = new File(testPlanExecutionDirectory, DateUtil.getCurrentDateINYYYYMMDDHHMMSS());

		Document pdfDocument = PdfReportUtil.createDocument(outputReport, testPlanTitle, "", "", "");

		boolean wasSuccess = true;

		// writeBeginningOfReport(pdfDocument);

		Chapter chapter = PdfReportUtil.createChapter("Test Steps", 2);
		chapter.add(Chunk.NEWLINE);

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

		int runNumber = 1;
		for (TestPlanRun run : runs) {
			RunResults runResults = null;
			System.out.println();
			System.out.println("Running step " + runNumber + " : " + run.getDescription());
			if (createReport) {
				runResults = executeRun(run, applicationToTest, testPlanExecutionDirectory, runNumber);
			}

			table.addCell(new PdfPCell());
			table.addCell(new PdfPCell(new Phrase("" + runNumber, SMALL_BOLD_FONT)));
			table.addCell(new PdfPCell(new Phrase(run.getDescription() + StringUtil.NEWLINE + StringUtil.NEWLINE + "java -jar " + Arrays.toString(run.getArguments().toArray()), SMALL_FONT)));
			table.addCell(new PdfPCell());
			table.addCell(new PdfPCell());
			table.addCell(new PdfPCell());

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
				table.addCell(new PdfPCell(new Phrase(referenceTypeAndNumbers, SMALL_FONT)));
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
		pdfDocument.close();

		if (createReport) {

			System.out.println(runNumber + " total runs.");
			System.out.println(passedChecks + " out of " + totalChecks + " checks passed.");

			if (wasSuccess) {
				System.out.println("The test plan PASSED.");
			} else {
				System.out.println("The test plan FAILED.");
			}
		}

		System.out.println(testPlanTitle + " was written to " + outputReport.getAbsolutePath() + ".");

		return wasSuccess;
	}

	private void writeBeginningOfReport(Document document) {
		try {
			Chapter chapter = PdfReportUtil.createChapter(testPlanTitle, 1);

			Section sectionOne = chapter.addSection(PdfReportUtil.getHeaderTextInParagraph("Approvers"), 1);

			PdfPTable table = new PdfPTable(3);
			table.addCell(new PdfPCell(new Phrase("Role", PdfReportUtil.HEADER_FONT)));
			table.addCell(new PdfPCell(new Phrase("Name", PdfReportUtil.HEADER_FONT)));
			table.addCell(new PdfPCell(new Phrase("Signature and Date", PdfReportUtil.HEADER_FONT)));

			for (Approver approver : approversSection.getApprovers()) {
				table.addCell(new PdfPCell(new Phrase(approver.getRole())));
				table.addCell(new PdfPCell(new Phrase(approver.getName())));
				table.addCell(new PdfPCell(new Phrase("                ")));
			}
			sectionOne.add(Chunk.NEWLINE);
			sectionOne.add(table);
			sectionOne.add(Chunk.NEWLINE);

			Section sectionTwo = chapter.addSection(PdfReportUtil.getHeaderTextInParagraph("Reviewers"), 2);
			table = new PdfPTable(2);
			table.addCell(new PdfPCell(new Phrase("Role", PdfReportUtil.HEADER_FONT)));
			table.addCell(new PdfPCell(new Phrase("Name", PdfReportUtil.HEADER_FONT)));

			for (Reviewer reviewer : reviewersSection.getReviewers()) {
				table.addCell(new PdfPCell(new Phrase(reviewer.getRole())));
				table.addCell(new PdfPCell(new Phrase(reviewer.getName())));
			}
			sectionTwo.add(Chunk.NEWLINE);
			sectionTwo.add(table);
			sectionTwo.add(Chunk.NEWLINE);

			Section sectionThree = chapter.addSection(PdfReportUtil.getHeaderTextInParagraph("Definitions"), 3);
			int i = 1;
			for (Definition definition : definitionsSection.getDefinitions()) {
				sectionThree.add(Chunk.NEWLINE);
				sectionThree.add(PdfReportUtil.getTextInParagraph("Term " + i + ": " + definition.getTerm() + " - " + definition.getDefinition()));
				i++;
			}
			sectionThree.add(Chunk.NEWLINE);

			Section sectionFour = chapter.addSection(PdfReportUtil.getHeaderTextInParagraph("Scope"), 4);
			sectionFour.add(Chunk.NEWLINE);
			sectionFour.add(PdfReportUtil.getTextInParagraph(scopeSection));
			sectionFour.add(Chunk.NEWLINE);

			Section sectionFive = chapter.addSection(PdfReportUtil.getHeaderTextInParagraph("References"), 5);
			table = new PdfPTable(3);
			table.addCell(new PdfPCell(new Phrase("Document", PdfReportUtil.HEADER_FONT)));
			table.addCell(new PdfPCell(new Phrase("Reference", PdfReportUtil.HEADER_FONT)));
			table.addCell(new PdfPCell(new Phrase("Version", PdfReportUtil.HEADER_FONT)));

			for (Reference reference : referencesSection.getReferences()) {
				table.addCell(new PdfPCell(new Phrase(reference.getDocumentName())));
				table.addCell(new PdfPCell(new Phrase(reference.getDocumentReference())));
				table.addCell(new PdfPCell(new Phrase(reference.getDocumentVersion())));
			}
			sectionFive.add(Chunk.NEWLINE);
			sectionFive.add(table);
			sectionFive.add(Chunk.NEWLINE);

			Section sectionSix = chapter.addSection(PdfReportUtil.getHeaderTextInParagraph("Plan"), 6);
			table = new PdfPTable(3);
			table.addCell(new PdfPCell(new Phrase("Requirement Type", PdfReportUtil.HEADER_FONT)));
			table.addCell(new PdfPCell(new Phrase("Requirement Number", PdfReportUtil.HEADER_FONT)));
			table.addCell(new PdfPCell(new Phrase("Requirement Text", PdfReportUtil.HEADER_FONT)));

			for (Requirement requirement : requirementsSection.getRequirements()) {
				table.addCell(new PdfPCell(new Phrase(requirement.getRequirementType())));
				table.addCell(new PdfPCell(new Phrase(requirement.getRequirementNumber())));
				table.addCell(new PdfPCell(new Phrase(requirement.getRequirementText())));
			}
			sectionSix.add(Chunk.NEWLINE);
			sectionSix.add(table);
			sectionSix.add(Chunk.NEWLINE);

			Section sectionSeven = chapter.addSection(PdfReportUtil.getHeaderTextInParagraph("Notes"), 7);
			i = 1;
			for (String note : notesSection.getNotes()) {
				sectionSeven.add(Chunk.NEWLINE);
				sectionSeven.add(PdfReportUtil.getTextInParagraph(i + ") " + note));
				i++;
			}
			sectionSeven.add(Chunk.NEWLINE);

			document.add(chapter);

		} catch (DocumentException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private RunResults executeRun(TestPlanRun run, File applicationToTest, File testPlanExecutionDirectory, int stepNumber) {

		String javaHome = System.getProperty("java.home");
		String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

		List<String> processBuilderArguments = new ArrayList<String>();
		processBuilderArguments.add(javaBin);
		processBuilderArguments.add("-jar");
		processBuilderArguments.add(applicationToTest.getAbsolutePath());
		processBuilderArguments.addAll(run.getArguments());

		ProcessBuilder processBuilder = new ProcessBuilder(processBuilderArguments);

		// set the java working directory
		File javaWorkingDirectory = new File(testPlanExecutionDirectory, "run_" + stepNumber);
		try {
			FileUtil.createDirectory(javaWorkingDirectory);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create the execution directory for run " + stepNumber + " at [" + javaWorkingDirectory.getAbsolutePath() + "].", e);
		}
		processBuilder.directory(javaWorkingDirectory);

		String outputString = "";
		String errorString = "";

		try {
			Process process = processBuilder.start();
			StreamListener outputStreamListener = new StreamListener(process.getInputStream());
			StreamListener errorStreamListener = new StreamListener(process.getErrorStream());
			process.waitFor();
			outputString = outputStreamListener.getString();
			errorString = errorStreamListener.getString();
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException("Unable to execute the provide jar file[" + applicationToTest.getAbsolutePath() + "].  " + e.getMessage());
		}

		return new RunResults(outputString, errorString, javaWorkingDirectory, run.getRunDirectory());
	}

	private static class StreamListener {
		private final StringBuilder string;

		public StreamListener(final InputStream inputStream) {
			string = new StringBuilder();
			new Thread(new Runnable() {
				public void run() {
					try (Scanner sc = new Scanner(inputStream)) {
						while (sc.hasNextLine()) {
							string.append(sc.nextLine());
						}
					}
				}
			}).start();
		}

		public String getString() {
			return string.toString();
		}
	}

	private final static String TEST_PLAN_TITLE_KEY = "testPlanTitle";
	private final static String SCOPE_SECTION_KEY = "scopeSection";

	private final static String VERSION_HISTORY_SECTION_KEY = "versionHistorySection";
	private final static String APPROVERS_SECTION_KEY = "approversSection";
	private final static String REVIEWERS_SECTION_KEY = "reviewersSection";
	private final static String DEFINITIONS_SECTION_KEY = "definitionsSection";
	private final static String REFERENCES_SECTION_KEY = "referencesSection";
	private final static String REQUIREMENTS_SECTION_KEY = "requirementsSection";
	private final static String NOTES_SECTION_KEY = "notesSection";

	private final static String VERSIONS_KEY = "versions";
	private final static String VERSION_DESCRIPTION_KEY = "description";
	private final static String VERSION_VERSION_KEY = "version";

	private final static String APPROVERS_KEY = "approvers";
	private final static String APPROVERS_NAME_KEY = "name";
	private final static String APPROVERS_ROLE_KEY = "role";

	private final static String REVIEWERS_KEY = "reviewers";
	private final static String REVIEWERS_NAME_KEY = "name";
	private final static String REVIEWERS_ROLE_KEY = "role";

	private final static String DEFINITIONS_KEY = "definitions";
	private final static String DEFINITIONS_DEFINITION_KEY = "definition";
	private final static String DEFINITIONS_TERM_KEY = "term";

	private final static String REFERENCES_KEY = "references";
	private final static String REFERENCES_NAME_KEY = "documentName";
	private final static String REFERENCES_REFERENCE_KEY = "documentReference";
	private final static String REFERENCES_VERSION_KEY = "documentVersion";

	private final static String REQUIREMENTS_KEY = "requirements";
	private final static String REQUIREMENTS_NUMBER_KEY = "requirementNumber";
	private final static String REQUIREMENTS_TYPE_KEY = "requirementType";
	private final static String REQUIREMENTS_TEXT_KEY = "requirementText";

	private final static String NOTES_KEY = "notes";

	@SuppressWarnings("unchecked")
	public static TestPlan readFromDirectory(File testPlanDirectory) {
		Yaml yaml = new Yaml();

		String[] testPlanYamlFiles = testPlanDirectory.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith("testplan");
			}
		});

		if (testPlanYamlFiles.length == 0) {
			throw new IllegalStateException("There were no .testplan files found in the provided test plan directory[" + testPlanDirectory + "].");
		} else if (testPlanYamlFiles.length > 1) {
			throw new IllegalStateException("There were multiple[" + testPlanYamlFiles.length + "] .testplan files found in the provided test plan directory[" + testPlanDirectory
					+ "] whereas only 1 was expected.");
		}

		TestPlan testPlan = null;

		File inputYaml = new File(testPlanDirectory, testPlanYamlFiles[0]);
		try {
			Map<String, Object> root = (Map<String, Object>) yaml.load(FileUtil.readFileAsString(inputYaml));
			String title = (String) root.get(TEST_PLAN_TITLE_KEY);

			String scope = (String) root.get(SCOPE_SECTION_KEY);

			testPlan = new TestPlan(title, scope);

			Map<String, Object> versionHistorySection = (Map<String, Object>) root.get(VERSION_HISTORY_SECTION_KEY);
			List<Map<String, Object>> versions = (List<Map<String, Object>>) versionHistorySection.get(VERSIONS_KEY);
			for (Map<String, Object> versionMap : versions) {
				Version version = new Version((String) versionMap.get(VERSION_VERSION_KEY), (String) versionMap.get(VERSION_DESCRIPTION_KEY));
				testPlan.getVersionHistorySection().addVersion(version);
			}

			Map<String, Object> approversSection = (Map<String, Object>) root.get(APPROVERS_SECTION_KEY);
			List<Map<String, Object>> approvers = (List<Map<String, Object>>) approversSection.get(APPROVERS_KEY);
			for (Map<String, Object> approverMap : approvers) {
				Approver approver = new Approver((String) approverMap.get(APPROVERS_NAME_KEY), (String) approverMap.get(APPROVERS_ROLE_KEY));
				testPlan.getApproversSection().addApprover(approver);
			}

			Map<String, Object> reviewersSection = (Map<String, Object>) root.get(REVIEWERS_SECTION_KEY);
			List<Map<String, Object>> reviewers = (List<Map<String, Object>>) reviewersSection.get(REVIEWERS_KEY);
			for (Map<String, Object> reviewerMap : reviewers) {
				Reviewer reviewer = new Reviewer((String) reviewerMap.get(REVIEWERS_NAME_KEY), (String) reviewerMap.get(REVIEWERS_ROLE_KEY));
				testPlan.getReviewersSection().addReviewer(reviewer);
			}

			Map<String, Object> definitionsSection = (Map<String, Object>) root.get(DEFINITIONS_SECTION_KEY);
			List<Map<String, Object>> definitions = (List<Map<String, Object>>) definitionsSection.get(DEFINITIONS_KEY);
			for (Map<String, Object> definitionsMap : definitions) {
				Definition definition = new Definition((String) definitionsMap.get(DEFINITIONS_TERM_KEY), (String) definitionsMap.get(DEFINITIONS_DEFINITION_KEY));
				testPlan.getDefinitionsSection().addDefinition(definition);
			}

			Map<String, Object> referencesSection = (Map<String, Object>) root.get(REFERENCES_SECTION_KEY);
			List<Map<String, Object>> references = (List<Map<String, Object>>) referencesSection.get(REFERENCES_KEY);
			for (Map<String, Object> referencesMap : references) {
				Reference reference = new Reference((String) referencesMap.get(REFERENCES_NAME_KEY), (String) referencesMap.get(REFERENCES_REFERENCE_KEY),
						(String) referencesMap.get(REFERENCES_VERSION_KEY));
				testPlan.getReferencesSection().addReference(reference);
			}

			Map<String, Object> requirementsSection = (Map<String, Object>) root.get(REQUIREMENTS_SECTION_KEY);
			List<Map<String, Object>> requirements = (List<Map<String, Object>>) requirementsSection.get(REQUIREMENTS_KEY);
			for (Map<String, Object> requirementsMap : requirements) {
				Requirement requirement = new Requirement((String) requirementsMap.get(REQUIREMENTS_TYPE_KEY), (String) requirementsMap.get(REQUIREMENTS_NUMBER_KEY),
						(String) requirementsMap.get(REQUIREMENTS_TEXT_KEY));
				testPlan.getRequirementsSection().addRequirement(requirement);
			}

			Map<String, Object> notesSection = (Map<String, Object>) root.get(NOTES_SECTION_KEY);
			List<String> notes = (List<String>) notesSection.get(NOTES_KEY);
			for (String note : notes) {
				testPlan.getNotesSection().addNote(note);
			}

		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
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
