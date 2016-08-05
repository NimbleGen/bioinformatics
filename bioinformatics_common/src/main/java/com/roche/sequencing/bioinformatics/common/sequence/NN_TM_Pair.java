package com.roche.sequencing.bioinformatics.common.sequence;

class NN_TM_Pair {

	private final NN_TM_Parameters deltaH;
	private final NN_TM_Parameters deltaS;

	public NN_TM_Pair(NN_TM_Parameters deltaH, NN_TM_Parameters deltaS) {
		super();
		this.deltaH = deltaH;
		this.deltaS = deltaS;
	}

	public NN_TM_Parameters getDeltaHParameters() {
		return deltaH;
	}

	public NN_TM_Parameters getDeltaSParameters() {
		return deltaS;
	}

	// # Breslauer et al. (1986), Proc Natl Acad Sci USA 83: 3746-3750
	// DNA_NN1 = {
	// 'init': (0, 0), 'init_A/T': (0, 0), 'init_G/C': (0, 0),
	// 'init_oneG/C': (0, -16.8), 'init_allA/T': (0, -20.1), 'init_5T/A': (0, 0),
	// 'sym': (0, -1.3),
	// 'AA/TT': (-9.1, -24.0), 'AT/TA': (-8.6, -23.9), 'TA/AT': (-6.0, -16.9),
	// 'CA/GT': (-5.8, -12.9), 'GT/CA': (-6.5, -17.3), 'CT/GA': (-7.8, -20.8),
	// 'GA/CT': (-5.6, -13.5), 'CG/GC': (-11.9, -27.8), 'GC/CG': (-11.1, -26.7),
	// 'GG/CC': (-11.0, -26.6)}
	//
	public static NN_TM_Pair getBreslauer1986Parameters() {
		NN_TM_Parameters deltaH = new NN_TM_Parameters(0, 0, 0, 0, 0, 0, -9.1, -8.6, -6.0, -5.8, -6.5, -7.8, -5.6, -11.9, -11.1, -11.0);
		NN_TM_Parameters deltaS = new NN_TM_Parameters(0, -16.8, -20.1, 0, 0, 0, -24.0, -23.9, -16.9, -12.9, -17.3, -20.8, -13.5, -27.8, -26.7, -26.6);
		return new NN_TM_Pair(deltaH, deltaS);

	}

	// # Sugimoto et al. (1996), Nuc Acids Res 24 : 4501-4505
	// DNA_NN2 = {
	// 'init': (0.6, -9.0), 'init_A/T': (0, 0), 'init_G/C': (0, 0),
	// 'init_oneG/C': (0, 0), 'init_allA/T': (0, 0), 'init_5T/A': (0, 0),
	// 'sym': (0, -1.4),
	// 'AA/TT': (-8.0, -21.9), 'AT/TA': (-5.6, -15.2), 'TA/AT': (-6.6, -18.4),
	// 'CA/GT': (-8.2, -21.0), 'GT/CA': (-9.4, -25.5), 'CT/GA': (-6.6, -16.4),
	// 'GA/CT': (-8.8, -23.5), 'CG/GC': (-11.8, -29.0), 'GC/CG': (-10.5, -26.4),
	// 'GG/CC': (-10.9, -28.4)}
	//
	public static NN_TM_Pair getSugimoto1996Parameters() {
		NN_TM_Parameters deltaH = new NN_TM_Parameters(0.6, 0, 0, 0, 0, 0, -8.0, -5.6, -6.6, -8.2, -9.4, -6.6, -8.8, -11.8, -10.5, -10.9);
		NN_TM_Parameters deltaS = new NN_TM_Parameters(-9.0, 0, 0, 0, 0, 0, -21.9, -15.2, -18.4, -21.0, -25.5, -16.4, -23.5, -29.0, -26.4, -28.4);
		return new NN_TM_Pair(deltaH, deltaS);
	}

	// # Allawi and SantaLucia (1997), Biochemistry 36: 10581-10594
	// DNA_NN3 = {
	// 'init': (0, 0), 'init_A/T': (2.3, 4.1), 'init_G/C': (0.1, -2.8),
	// 'init_oneG/C': (0, 0), 'init_allA/T': (0, 0), 'init_5T/A': (0, 0),
	// 'sym': (0, -1.4),
	// 'AA/TT': (-7.9, -22.2), 'AT/TA': (-7.2, -20.4), 'TA/AT': (-7.2, -21.3),
	// 'CA/GT': (-8.5, -22.7), 'GT/CA': (-8.4, -22.4), 'CT/GA': (-7.8, -21.0),
	// 'GA/CT': (-8.2, -22.2), 'CG/GC': (-10.6, -27.2), 'GC/CG': (-9.8, -24.4),
	// 'GG/CC': (-8.0, -19.9)}
	//
	public static NN_TM_Pair getSantaLucia1997Parameters() {
		NN_TM_Parameters deltaH = new NN_TM_Parameters(0, 0, 0, 0, 2.3, 0.1, -7.9, -7.2, -7.2, -8.5, -8.4, -7.8, -8.2, -10.6, -9.8, -8.0);
		NN_TM_Parameters deltaS = new NN_TM_Parameters(0, 0, 0, 0, 4.1, -2.8, -22.2, -20.4, -21.3, -22.7, -22.4, -21.0, -22.2, -27.2, -24.4, -19.9);
		return new NN_TM_Pair(deltaH, deltaS);
	}

	// # SantaLucia & Hicks (2004), Annu. Rev. Biophys. Biomol. Struct 33: 415-440
	// DNA_NN4 = {
	// 'init': (0.2, -5.7), 'init_A/T': (2.2, 6.9), 'init_G/C': (0, 0),
	// 'init_oneG/C': (0, 0), 'init_allA/T': (0, 0), 'init_5T/A': (0, 0),
	// 'sym': (0, -1.4),
	// 'AA/TT': (-7.6, -21.3), 'AT/TA': (-7.2, -20.4), 'TA/AT': (-7.2, -20.4),
	// 'CA/GT': (-8.5, -22.7), 'GT/CA': (-8.4, -22.4), 'CT/GA': (-7.8, -21.0),
	// 'GA/CT': (-8.2, -22.2), 'CG/GC': (-10.6, -27.2), 'GC/CG': (-9.8, -24.4),
	// 'GG/CC': (-8.0, -19.0)}
	public static NN_TM_Pair getSantaLucia2004Parameters() {
		NN_TM_Parameters deltaH = new NN_TM_Parameters(0.2, 0, 0, 0, 2.2, 0, -7.6, -7.2, -7.2, -8.5, -8.4, -7.8, -8.2, -10.6, -9.8, -8.0);
		NN_TM_Parameters deltaS = new NN_TM_Parameters(-5.7, 0, 0, 0, 6.9, 0, -21.3, -20.4, -20.4, -22.7, -22.4, -21.0, -22.2, -27.2, -24.4, -19.0);
		return new NN_TM_Pair(deltaH, deltaS);
	}

	/**
	 * #Internal mismatch and inosine table (DNA) # Allawi & SantaLucia (1997), Biochemistry 36: 10581-10594 # Allawi & SantaLucia (1998), Biochemistry 37: 9435-9444 # Allawi & SantaLucia (1998),
	 * Biochemistry 37: 2170-2179 # Allawi & SantaLucia (1998), Nucl Acids Res 26: 2694-2701 # Peyret et al. (1999), Biochemistry 38: 3468-3477 # Watkins & SantaLucia (2005), Nucl Acids Res 33:
	 * 6258-6267 DNA_IMM1 = { 'AG/TT': (1.0, 0.9), 'AT/TG': (-2.5, -8.3), 'CG/GT': (-4.1, -11.7), 'CT/GG': (-2.8, -8.0), 'GG/CT': (3.3, 10.4), 'GG/TT': (5.8, 16.3), 'GT/CG': (-4.4, -12.3), 'GT/TG':
	 * (4.1, 9.5), 'TG/AT': (-0.1, -1.7), 'TG/GT': (-1.4, -6.2), 'TT/AG': (-1.3, -5.3), 'AA/TG': (-0.6, -2.3), 'AG/TA': (-0.7, -2.3), 'CA/GG': (-0.7, -2.3), 'CG/GA': (-4.0, -13.2), 'GA/CG': (-0.6,
	 * -1.0), 'GG/CA': (0.5, 3.2), 'TA/AG': (0.7, 0.7), 'TG/AA': (3.0, 7.4), 'AC/TT': (0.7, 0.2), 'AT/TC': (-1.2, -6.2), 'CC/GT': (-0.8, -4.5), 'CT/GC': (-1.5, -6.1), 'GC/CT': (2.3, 5.4), 'GT/CC':
	 * (5.2, 13.5), 'TC/AT': (1.2, 0.7), 'TT/AC': (1.0, 0.7), 'AA/TC': (2.3, 4.6), 'AC/TA': (5.3, 14.6), 'CA/GC': (1.9, 3.7), 'CC/GA': (0.6, -0.6), 'GA/CC': (5.2, 14.2), 'GC/CA': (-0.7, -3.8),
	 * 'TA/AC': (3.4, 8.0), 'TC/AA': (7.6, 20.2), 'AA/TA': (1.2, 1.7), 'CA/GA': (-0.9, -4.2), 'GA/CA': (-2.9, -9.8), 'TA/AA': (4.7, 12.9), 'AC/TC': (0.0, -4.4), 'CC/GC': (-1.5, -7.2), 'GC/CC': (3.6,
	 * 8.9), 'TC/AC': (6.1, 16.4), 'AG/TG': (-3.1, -9.5), 'CG/GG': (-4.9, -15.3), 'GG/CG': (-6.0, -15.8), 'TG/AG': (1.6, 3.6), 'AT/TT': (-2.7, -10.8), 'CT/GT': (-5.0, -15.8), 'GT/CT': (-2.2, -8.4),
	 * 'TT/AT': (0.2, -1.5), 'AI/TC': (-8.9, -25.5), 'TI/AC': (-5.9, -17.4), 'AC/TI': (-8.8, -25.4), 'TC/AI': (-4.9, -13.9), 'CI/GC': (-5.4, -13.7), 'GI/CC': (-6.8, -19.1), 'CC/GI': (-8.3, -23.8),
	 * 'GC/CI': (-5.0, -12.6), 'AI/TA': (-8.3, -25.0), 'TI/AA': (-3.4, -11.2), 'AA/TI': (-0.7, -2.6), 'TA/AI': (-1.3, -4.6), 'CI/GA': (2.6, 8.9), 'GI/CA': (-7.8, -21.1), 'CA/GI': (-7.0, -20.0),
	 * 'GA/CI': (-7.6, -20.2), 'AI/TT': (0.49, -0.7), 'TI/AT': (-6.5, -22.0), 'AT/TI': (-5.6, -18.7), 'TT/AI': (-0.8, -4.3), 'CI/GT': (-1.0, -2.4), 'GI/CT': (-3.5, -10.6), 'CT/GI': (0.1, -1.0),
	 * 'GT/CI': (-4.3, -12.1), 'AI/TG': (-4.9, -15.8), 'TI/AG': (-1.9, -8.5), 'AG/TI': (0.1, -1.8), 'TG/AI': (1.0, 1.0), 'CI/GG': (7.1, 21.3), 'GI/CG': (-1.1, -3.2), 'CG/GI': (5.8, 16.9), 'GG/CI':
	 * (-7.6, -22.0), 'AI/TI': (-3.3, -11.9), 'TI/AI': (0.1, -2.3), 'CI/GI': (1.3, 3.0), 'GI/CI': (-0.5, -1.3)}
	 * 
	 * # Terminal mismatch table (DNA) # SantaLucia & Peyret (2001) Patent Application WO 01/94611 DNA_TMM1 = { 'AA/TA': (-3.1, -7.8), 'TA/AA': (-2.5, -6.3), 'CA/GA': (-4.3, -10.7), 'GA/CA': (-8.0,
	 * -22.5), 'AC/TC': (-0.1, 0.5), 'TC/AC': (-0.7, -1.3), ' CC/GC': (-2.1, -5.1), 'GC/CC': (-3.9, -10.6), 'AG/TG': (-1.1, -2.1), 'TG/AG': (-1.1, -2.7), 'CG/GG': (-3.8, -9.5), 'GG/CG': (-0.7, -19.2),
	 * 'AT/TT': (-2.4, -6.5), 'TT/AT': (-3.2, -8.9), 'CT/GT': (-6.1, -16.9), 'GT/CT': (-7.4, -21.2), 'AA/TC': (-1.6, -4.0), 'AC/TA': (-1.8, -3.8), 'CA/GC': (-2.6, -5.9), 'CC/GA': (-2.7, -6.0),
	 * 'GA/CC': (-5.0, -13.8), 'GC/CA': (-3.2, -7.1), 'TA/AC': (-2.3, -5.9), 'TC/AA': (-2.7, -7.0), 'AC/TT': (-0.9, -1.7), 'AT/TC': (-2.3, -6.3), 'CC/GT': (-3.2, -8.0), 'CT/GC': (-3.9, -10.6),
	 * 'GC/CT': (-4.9, -13.5), 'GT/CC': (-3.0, -7.8), 'TC/AT': (-2.5, -6.3), 'TT/AC': (-0.7, -1.2), 'AA/TG': (-1.9, -4.4), 'AG/TA': (-2.5, -5.9), 'CA/GG': (-3.9, -9.6), 'CG/GA': (-6.0, -15.5),
	 * 'GA/CG': (-4.3, -11.1), ' GG/CA': (-4.6, -11.4), 'TA/AG': (-2.0, -4.7), 'TG/AA': (-2.4, -5.8), 'AG/TT': (-3.2, -8.7), 'AT/TG': (-3.5, -9.4), 'CG/GT': (-3.8, -9.0), 'CT/GG': (-6.6, -18.7),
	 * 'GG/CT': (-5.7, -15.9), 'GT/CG': (-5.9, -16.1), 'TG/AT': (-3.9, -10.5), 'TT/AG': (-3.6, -9.8)}
	 * 
	 * # Dangling ends table (DNA) # Bommarito et al. (2000), Nucl Acids Res 28: 1929-1934 DNA_DE1 = { 'AA/.T': (0.2, 2.3), 'AC/.G': (-6.3, -17.1), 'AG/.C': (-3.7, -10.0), 'AT/.A': (-2.9, -7.6),
	 * 'CA/.T': (0.6, 3.3), 'CC/.G': (-4.4, -12.6), 'CG/.C': (-4.0, -11.9), 'CT/.A': (-4.1, -13.0), 'GA/.T': (-1.1, -1.6), 'GC/.G': (-5.1, -14.0), 'GG/.C': (-3.9, -10.9), 'GT/.A': (-4.2, -15.0),
	 * 'TA/.T': (-6.9, -20.0), 'TC/.G': (-4.0, -10.9), 'TG/.C': (-4.9, -13.8), 'TT/.A': (-0.2, -0.5), '.A/AT': (-0.7, -0.8), '.C/AG': (-2.1, -3.9), '.G/AC': (-5.9, -16.5), '.T/AA': (-0.5, -1.1),
	 * '.A/CT': (4.4, 14.9), '.C/CG': (-0.2, -0.1), '.G/CC': (-2.6, -7.4), '.T/CA': (4.7, 14.2), '.A/GT': (-1.6, -3.6), '.C/GG': (-3.9, -11.2), '.G/GC': (-3.2, -10.4), '.T/GA': (-4.1, -13.1), '.A/TT':
	 * (2.9, 10.4), '.C/TG': (-4.4, -13.1), '.G/TC': (-5.2, -15.0), '.T/TA': (-3.8, -12.6)}
	 **/

}
