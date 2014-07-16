package com.roche.heatseq.qualityreport;

import java.io.InputStream;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.roche.sequencing.bioinformatics.common.utils.FileUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class FunGeneralErrors {

	private static Logger logger = LoggerFactory.getLogger(FunGeneralErrors.class);

	private static String[] funErrorsFromFile;
	private static String DEFAULT_ERROR = "There has been a problem.";
	private static boolean FUN_ERRORS_RETRIEVED = false;

	private FunGeneralErrors() {
		throw new AssertionError();
	}

	public static String getFunError() {
		String funError = DEFAULT_ERROR;
		if (!FUN_ERRORS_RETRIEVED) {
			retrieveFunErrorsFromFile();
		}

		if (funErrorsFromFile != null && funErrorsFromFile.length > 0) {
			Random random = new Random(System.currentTimeMillis());
			int randomIndex = random.nextInt(funErrorsFromFile.length);
			funError = funErrorsFromFile[randomIndex];
			if (funError.isEmpty()) {
				funError = DEFAULT_ERROR;
			}
		}

		return funError;
	}

	private static void retrieveFunErrorsFromFile() {
		FUN_ERRORS_RETRIEVED = true;
		try {
			InputStream stream = FunGeneralErrors.class.getResourceAsStream("fun_errors.txt");
			String fileAsString = FileUtil.readStreamAsString(stream);
			funErrorsFromFile = fileAsString.split(StringUtil.WINDOWS_NEWLINE);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
}
