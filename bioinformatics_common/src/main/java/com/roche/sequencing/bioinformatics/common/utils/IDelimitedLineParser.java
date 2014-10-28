package com.roche.sequencing.bioinformatics.common.utils;

import java.util.Map;

public interface IDelimitedLineParser {

	void parseDelimitedLine(Map<String, String> headerNameToValue);

	void doneParsing(int linesOfData, String[] headerNames);

}
