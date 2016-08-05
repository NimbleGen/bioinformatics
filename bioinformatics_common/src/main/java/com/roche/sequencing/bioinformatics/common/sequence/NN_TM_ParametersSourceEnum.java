package com.roche.sequencing.bioinformatics.common.sequence;

public enum NN_TM_ParametersSourceEnum {
	Breslauer1986, Sugimoto1996, SantaLucia1997, SantaLucia2004;

	public NN_TM_Pair getParameters() {
		NN_TM_Pair pair = null;
		switch (this) {
		case Breslauer1986:
			pair = NN_TM_Pair.getBreslauer1986Parameters();
			break;
		case Sugimoto1996:
			pair = NN_TM_Pair.getSugimoto1996Parameters();
			break;
		case SantaLucia1997:
			pair = NN_TM_Pair.getSantaLucia1997Parameters();
			break;
		case SantaLucia2004:
			pair = NN_TM_Pair.getSantaLucia2004Parameters();
			break;
		default:
			throw new AssertionError();
		}
		return pair;
	}

}
