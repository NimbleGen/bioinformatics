package com.roche.sequencing.bioinformatics.common.sequence;

class NN_TM_Parameters {

	private final double initial;
	private final double atLeastOneGC;
	private final double allAT;
	private final double fivePrimeEndInT;
	private final double initAorT;
	private final double initGorC;
	private final double AAorTT;
	private final double AT;
	private final double TA;
	private final double CAorTG;
	private final double GTorAC;
	private final double CTorAG;
	private final double GAorTC;
	private final double CG;
	private final double GC;
	private final double GGorCC;

	public NN_TM_Parameters(double initial, double atLeastOneGC, double allAT, double fivePrimeEndInT, double initAorT, double initGorC, double AAorTT, double AT, double TA, double CAorTG,
			double GTorAC, double CTorAG, double GAorTC, double CG, double GC, double GGorCC) {
		super();
		this.initial = initial;
		this.atLeastOneGC = atLeastOneGC;
		this.allAT = allAT;
		this.fivePrimeEndInT = fivePrimeEndInT;
		this.initAorT = initAorT;
		this.initGorC = initGorC;
		this.AAorTT = AAorTT;
		this.AT = AT;
		this.TA = TA;
		this.CAorTG = CAorTG;
		this.GTorAC = GTorAC;
		this.CTorAG = CTorAG;
		this.GAorTC = GAorTC;
		this.CG = CG;
		this.GC = GC;
		this.GGorCC = GGorCC;
	}

	public double getInitial() {
		return initial;
	}

	public double getAtLeastOneGC() {
		return atLeastOneGC;
	}

	public double getAllAT() {
		return allAT;
	}

	public double getFivePrimeEndIsT() {
		return fivePrimeEndInT;
	}

	public double getInitAorT() {
		return initAorT;
	}

	public double getInitGorC() {
		return initGorC;
	}

	public double getAAorTT() {
		return AAorTT;
	}

	public double getAT() {
		return AT;
	}

	public double getTA() {
		return TA;
	}

	public double getCAorTG() {
		return CAorTG;
	}

	public double getGTorAC() {
		return GTorAC;
	}

	public double getCTorAG() {
		return CTorAG;
	}

	public double getGAorTC() {
		return GAorTC;
	}

	public double getCG() {
		return CG;
	}

	public double getGC() {
		return GC;
	}

	public double getGGorCC() {
		return GGorCC;
	}

	public double getValueForTwoMer(ISequence sequence) {
		double value = 0;
		if (sequence.size() != 2) {
			throw new IllegalStateException("Expecting a sequence of length 2.  The provided sequence[" + sequence + "] was of length [" + sequence.size() + "].");
		}

		String sequenceAsString = sequence.toString();
		switch (sequenceAsString) {
		case "AA":
		case "TT":
			value = AAorTT;
			break;
		case "AT":
			value = AT;
			break;
		case "TA":
			value = TA;
			break;
		case "CA":
		case "TG":
			value = CAorTG;
			break;
		case "GT":
		case "AC":
			value = GTorAC;
			break;
		case "CT":
		case "AG":
			value = CTorAG;
			break;
		case "GA":
		case "TC":
			value = GAorTC;
			break;
		case "CG":
			value = CG;
			break;
		case "GC":
			value = GC;
			break;
		case "GG":
		case "CC":
			value = GGorCC;
			break;
		default:
			value = 0.0;
		}
		return value;
	}

}
