package com.roche.heatseq.verification_tests;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.roche.bioinformatics.common.verification.Approver;
import com.roche.bioinformatics.common.verification.Definition;
import com.roche.bioinformatics.common.verification.Person;
import com.roche.bioinformatics.common.verification.Reference;
import com.roche.bioinformatics.common.verification.Requirement;
import com.roche.bioinformatics.common.verification.Reviewer;
import com.roche.bioinformatics.common.verification.TestPlan;
import com.roche.bioinformatics.common.verification.TestPlanStep;
import com.roche.bioinformatics.common.verification.TestPlanStepCheck;
import com.roche.heatseq.cli.HsqUtilsCli;
import com.roche.heatseq.verification_tests.HsqTestPlanUtil.HsqUtilsCommandEnum;
import com.roche.sequencing.bioinformatics.common.utils.DateUtil;

public class TestPlan10Test {

	private TestPlan testPlan;

	@BeforeClass
	public void setup() {
		String scope = "Verify that HSQUtils software correctly trims FASTQ formatted sequencing reads given a read set (read one and read two) and a probe information file.";

		testPlan = new TestPlan("02462 SW Test Report", new Person("Mark", "D`Ascenzo"), scope, HsqUtilsCli.class);
		testPlan.getApproversSection().addApprover(new Approver(new Person("Todd", "Richmond"), "Technical Lead"));
		testPlan.getApproversSection().addApprover(new Approver(new Person("Lori", "Radavich"), "Quality Representative"));
		testPlan.getApproversSection().addApprover(new Approver(new Person("Kurt", "Heilman"), "Tester"));

		testPlan.getReviewersSection().addReviewer(new Reviewer(new Person("Mark", "D`Ascenzo"), "Research Informatics"));

		testPlan.getDefinitionsSection().addDefinition(new Definition("PS", "Product Specification"));
		testPlan.getDefinitionsSection()
				.addDefinition(
						new Definition(
								"Probe Information File",
								"The probe information or probe info file is a tab-delimited plain text file which is intended to be delivered to HEAT-seq and HEAT-seq Ultra customers. It describes all HEAT-seq probes in a design including primer sequences and genomic target locations. The probe info file is needed during data analysis to determine appropriate read trimming lengths and to match reads to probes using coordinates and sequence.  See also: http://wiki.dmd.roche.com/index.php/Probe_Information_File"));

		testPlan.getReferencesSection().addReference(new Reference("Requirements Document", "02642 Product Requirements Doc (PRD) 1200000069949", "02"));

		testPlan.getRequirementsSection().addRequirement(
				new Requirement("PS", "60.001.046.001",
						"The system shall provide a means to perform trimming based on the uid extension, ligation primer and ligation primer lengths defined in the probe information file."));
		testPlan.getRequirementsSection().addRequirement(
				new Requirement("PS", "60.001.046.016",
						"The system shall utilize the following key-value pairs for trimming when presented in the header of the probe information file: extension_uid, ligation_uid, genome.  "));
		testPlan.getRequirementsSection().addRequirement(new Requirement("PS", "60.001.046.026", "The system shall allow users to specify an output directory to place all outputted files."));
		testPlan.getRequirementsSection().addRequirement(
				new Requirement("PS", "60.001.046.028", "The system shall use the current working directory as the output directory if the output directory is not provided by the user."));
		testPlan.getRequirementsSection().addRequirement(
				new Requirement("PS", "60.001.049.068",
						"The system shall require the following inputs be provided for trimming: raw read one fastq file, raw read two fastq file, probe information file."));

		testPlan.getNotesSection()
				.addNote(
						"Dataset 5: This is comprised of three simulated datasets with testable combinations of extension_uid and ligation_uid values. The combinations are: a. exenstion_uid=10 and ligation_uid=0 b. extension_uid= 0 and ligation_uid=10 c. extension_uid=10 and ligation_uid=10.  Included with the dataset are fastq files before and after trimming and probe_info files that specify corresponding header values for trimming the fastq sequence files using HSQUtils.");

		File testPlan10Directory = new File("D:\\kurts_space\\heatseq\\Test_Plans\\testplan10_data");
		File testPlan10Workspace = new File("D:\\kurts_space\\heatseq\\Test_Plans\\testplan10_workspace");

		String timeStamp = DateUtil.getCurrentDateINYYYYMMDDHHMMSS();

		File outputDir = new File(testPlan10Workspace, "step1_results_" + timeStamp + "\\");
		String[] args = HsqTestPlanUtil.createArgs(HsqUtilsCommandEnum.TRIM, new File(testPlan10Directory, "step1\\"), null);
		String stepDescription = "Verify that the system shall require the raw read one fastq file, raw read two fastq file, probe information file as inputs for trimming by executing the below command and wait until the prompt is back";
		TestPlanStep step = new TestPlanStep(stepDescription, args, outputDir);

		String verificationDescription = "Verify that the system shall use the current working directory as the output directory if the output directory is not provided by the user.  Note that the '--outputDir' option was not provided in the arguments.";
		step.addVerification(new TestPlanStepCheck(verificationDescription, "Three files are in the current working directory.", new TrimResultsPresentStepChecker()));
		testPlan.addStep(step);

	}

	@Test(groups = { "validation" })
	public void step1() {
		Assert.assertTrue(testPlan.runTestPlan("Kurt Heilman", new File("testPlan10Results.pdf")));
	}
}
