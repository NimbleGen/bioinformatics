package com.roche.bioinformatics.common.verification;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.itextpdf.text.Chapter;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.PdfReportUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class TestPlan {

	public static Font SMALL_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 6, Font.NORMAL);

	private final Class<?> mainClass;
	private final String testPlanTitle;
	private final Person author;
	private final ApproversSection approversSection;
	private final ReviewersSection reviewersSection;
	private final DefinitionsSection definitionsSection;
	private final ReferencesSection referencesSection;
	private final RequirementsSection requirementsSection;
	private final NotesSection notesSection;
	private final String scope;
	private final List<TestPlanStep> steps;

	public TestPlan(String testPlanTitle, Person author, String scope, Class<?> mainClass) {
		super();
		this.testPlanTitle = testPlanTitle;
		this.author = author;
		this.approversSection = new ApproversSection();
		this.reviewersSection = new ReviewersSection();
		this.definitionsSection = new DefinitionsSection();
		this.referencesSection = new ReferencesSection();
		this.requirementsSection = new RequirementsSection();
		this.notesSection = new NotesSection();
		this.scope = scope;
		this.mainClass = mainClass;
		this.steps = new ArrayList<TestPlanStep>();
	}

	public String getScope() {
		return scope;
	}

	public Class<?> getMainClass() {
		return mainClass;
	}

	public String getTestPlanTitle() {
		return testPlanTitle;
	}

	public Person getAuthor() {
		return author;
	}

	public ApproversSection getApproversSection() {
		return approversSection;
	}

	public ReviewersSection getReviewersSection() {
		return reviewersSection;
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

	public void addStep(TestPlanStep step) {
		steps.add(step);
	}

	/**
	 * 
	 * @param testerName
	 * @param outputReport
	 * @return true if test plan ran succesfully with no failures, false otherwise
	 */
	public boolean runTestPlan(String testerName, File outputReport) {
		Document pdfDocument = PdfReportUtil.createDocument(outputReport, testPlanTitle, "", author.getName(), author.getName());

		boolean wasSuccess = false;

		writeBeginningOfReport(pdfDocument);

		Chapter chapter = PdfReportUtil.createChapter("Test Steps", 2);
		chapter.add(Chunk.NEWLINE);

		PdfPTable table = new PdfPTable(5);
		table.addCell(new PdfPCell(new Phrase("Step Number", PdfReportUtil.HEADER_FONT)));
		table.addCell(new PdfPCell(new Phrase("Test Step", PdfReportUtil.HEADER_FONT)));
		table.addCell(new PdfPCell(new Phrase("Acceptance Criteria", PdfReportUtil.HEADER_FONT)));
		table.addCell(new PdfPCell(new Phrase("Acceptance Results", PdfReportUtil.HEADER_FONT)));
		table.addCell(new PdfPCell(new Phrase("Results (Pass/Fail)", PdfReportUtil.HEADER_FONT)));

		int stepNumber = 1;
		for (TestPlanStep step : steps) {
			// TODO print out step information
			RunResults runResults = runStep(step);

			wasSuccess = runResults.getConsoleErrors().isEmpty();

			System.out.println(runResults.getConsoleErrors());
			System.out.println(runResults.getConsoleOutput());

			table.addCell(new PdfPCell(new Phrase("" + stepNumber, SMALL_FONT)));
			table.addCell(new PdfPCell(new Phrase(step.getDescription() + StringUtil.NEWLINE + "java -jar " + Arrays.toString(step.getArgs()), SMALL_FONT)));
			table.addCell(new PdfPCell());
			table.addCell(new PdfPCell());
			table.addCell(new PdfPCell());
			for (TestPlanStepCheck check : step.getStepChecks()) {
				table.addCell(new PdfPCell());
				table.addCell(new PdfPCell(new Phrase(check.getDescription(), SMALL_FONT)));
				table.addCell(new PdfPCell(new Phrase(check.getExpectedResultDescription(), SMALL_FONT)));
				StepCheckerResults checkResults = check.getStepChecker().checkStep(runResults);
				table.addCell(new PdfPCell(new Phrase(checkResults.getResultsDescription(), SMALL_FONT)));
				boolean wasCheckSuccess = checkResults.isSuccess();

				if (wasCheckSuccess) {
					table.addCell(new PdfPCell(new Phrase("Pass", SMALL_FONT)));
				} else {
					table.addCell(new PdfPCell(new Phrase("Fail", SMALL_FONT)));
				}
				wasSuccess = wasSuccess && wasCheckSuccess;
			}
			stepNumber++;
		}
		chapter.add(table);

		try {
			pdfDocument.add(chapter);
		} catch (DocumentException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		pdfDocument.close();

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
				table.addCell(new PdfPCell(new Phrase(approver.getPerson().getName())));
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
				table.addCell(new PdfPCell(new Phrase(reviewer.getPerson().getName())));
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
			sectionFour.add(PdfReportUtil.getTextInParagraph(scope));
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

	private RunResults runStep(TestPlanStep step) {
		PrintStream originalOutput = System.out;
		PrintStream originalError = System.err;
		Interceptor systemOutInterceptor = new Interceptor(originalOutput);
		Interceptor systemErrorInterceptor = new Interceptor(originalError);
		System.setOut(systemOutInterceptor);
		System.setErr(systemErrorInterceptor);

		// set the java working directory
		File javaWorkingDirectory = step.getResultsDirectory();
		if (javaWorkingDirectory != null) {
			if (!javaWorkingDirectory.exists()) {
				try {
					FileUtil.createDirectory(javaWorkingDirectory);
				} catch (IOException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			}
			System.setProperty("user.dir", javaWorkingDirectory.getAbsolutePath());
		}

		try {
			final Method mainMethod = mainClass.getMethod("main", String[].class);
			final Object[] args = new Object[1];
			args[0] = step.getArgs();
			mainMethod.invoke(null, args);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		System.setOut(originalOutput);
		System.setErr(originalError);

		return new RunResults(systemOutInterceptor.getString(), systemErrorInterceptor.getString(), step.getResultsDirectory());
	}

	private static class Interceptor extends PrintStream {
		private final StringBuilder string;

		public Interceptor(OutputStream out) {
			super(out, true);
			string = new StringBuilder();
		}

		@Override
		public void print(String s) {
			string.append(s);
		}

		@Override
		public void println(String s) {
			string.append(s + StringUtil.NEWLINE);
		}

		public String getString() {
			return string.toString();
		}
	}

}
