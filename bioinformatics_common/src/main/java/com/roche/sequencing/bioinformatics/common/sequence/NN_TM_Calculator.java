package com.roche.sequencing.bioinformatics.common.sequence;

public class NN_TM_Calculator {

	private final static double UNIVERSAL_GAS_CONSTANT = 1.9872;// universal gas constant in Cal/degrees C*Mol;
	private final static double DEFAULT_OLIGO_CONCENTRATION = 0.00000005;
	private final static double DEFAULT_SALT_CONCENTRATION = 0.05;
	private final static NN_TM_ParametersSourceEnum DEFAULT_PARAMTERS_SOURCE = NN_TM_ParametersSourceEnum.SantaLucia1997;

	public static double getTm(ISequence sequence) {
		return getTm(sequence, DEFAULT_PARAMTERS_SOURCE, DEFAULT_OLIGO_CONCENTRATION, DEFAULT_SALT_CONCENTRATION);
	}

	public static double getTm(ISequence sequence, NN_TM_ParametersSourceEnum parametersSource, double oligoConcentration, double saltConcentration) {
		NN_TM_Pair parameterPair = parametersSource.getParameters();
		NN_TM_Parameters deltaHParameters = parameterPair.getDeltaHParameters();
		NN_TM_Parameters deltaSParameters = parameterPair.getDeltaSParameters();

		double deltaH = deltaHParameters.getInitial();
		double deltaS = deltaSParameters.getInitial();

		boolean sequenceContainsGorC = sequence.contains(NucleotideCode.GUANINE) || sequence.contains(NucleotideCode.CYTOSINE);
		if (sequenceContainsGorC) {
			deltaH += deltaHParameters.getAtLeastOneGC();
			deltaS += deltaSParameters.getAtLeastOneGC();
		} else {
			deltaH += deltaHParameters.getAllAT();
			deltaS += deltaSParameters.getAllAT();
		}

		ICode firstCode = sequence.getCodeAt(0);
		ICode secondCode = sequence.getCodeAt(1);
		ICode lastCode = sequence.getCodeAt(sequence.size() - 1);

		boolean fivePrimeEndIsT = firstCode.matches(NucleotideCode.THYMINE);
		if (fivePrimeEndIsT) {
			deltaH += deltaHParameters.getFivePrimeEndIsT();
			deltaS += deltaSParameters.getFivePrimeEndIsT();
		}

		// this means that the compliment strand has a 5' T
		boolean threePrimerEndIsA = lastCode.matches(NucleotideCode.ADENINE);
		if (threePrimerEndIsA) {
			deltaH += deltaHParameters.getFivePrimeEndIsT();
			deltaS += deltaSParameters.getFivePrimeEndIsT();
		}

		int initialAorTCount = 0;
		int initialGorCCount = 0;
		if (firstCode.matches(NucleotideCode.ADENINE) || firstCode.matches(NucleotideCode.THYMINE)) {
			initialAorTCount++;
		} else {
			initialGorCCount++;
		}

		if (secondCode.matches(NucleotideCode.ADENINE) || secondCode.matches(NucleotideCode.THYMINE)) {
			initialAorTCount++;
		} else {
			initialGorCCount++;
		}

		deltaH += (initialAorTCount * deltaHParameters.getInitAorT());
		deltaS += (initialAorTCount * deltaSParameters.getInitAorT());

		deltaH += (initialGorCCount * deltaHParameters.getInitGorC());
		deltaS += (initialGorCCount * deltaSParameters.getInitGorC());

		for (int i = 0; i < sequence.size() - 1; i++) {
			ISequence twoMerSequence = sequence.subSequence(i, i + 1);
			deltaH += deltaHParameters.getValueForTwoMer(twoMerSequence);
			deltaS += deltaSParameters.getValueForTwoMer(twoMerSequence);
		}

		// System.out.println("python dh:" + deltaH);
		// System.out.println("python ds:" + deltaS);

		double numerator = -(deltaH + 3.4) * 1000;
		double denominator = (UNIVERSAL_GAS_CONSTANT * Math.log(1.0 / oligoConcentration)) - deltaS;
		double meltingTemperature = (numerator / denominator) + 16.6 * (Math.log10(saltConcentration)) - 273.15;
		// double dnac1 = 25;
		// double dnac2 = 25;
		// double k = (dnac1 - (dnac2 / 2.0)) * Math.pow(Math.E, -9);
		// double meltingTemperature = (1000 * deltaH) / (deltaS + (UNIVERSAL_GAS_CONSTANT * (Math.log(k)))) - 273.15;

		return meltingTemperature;
	}

}
