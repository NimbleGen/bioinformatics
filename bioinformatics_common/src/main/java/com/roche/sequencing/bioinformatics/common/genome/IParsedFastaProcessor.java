package com.roche.sequencing.bioinformatics.common.genome;

import com.roche.sequencing.bioinformatics.common.sequence.SimpleNucleotideCodeSequence;

public interface IParsedFastaProcessor {

	void sequenceProcessed(String containerName, SimpleNucleotideCodeSequence sequence);

	void doneProcessing();

}
