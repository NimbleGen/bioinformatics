package com.roche.sequencing.bioinformatics.common.utils.bed;

import java.awt.Color;

public class BedColorUtil {

	/**
	 * see https://genome.ucsc.edu/FAQ/FAQformat.html#format1
	 * 
	 * @param bedScore
	 * @return associated color of given bedScore
	 */
	public static Color getColor(int bedScore) {
		Color color = null;
		if (bedScore < 167) {
			color = new Color(226, 226, 226);
		} else if (bedScore < 278) {
			color = new Color(198, 198, 198);
		} else if (bedScore < 389) {
			color = new Color(170, 170, 170);
		} else if (bedScore < 500) {
			color = new Color(141, 141, 141);
		} else if (bedScore < 612) {
			color = new Color(113, 113, 113);
		} else if (bedScore < 723) {
			color = new Color(85, 85, 85);
		} else if (bedScore < 834) {
			color = new Color(56, 56, 56);
		} else if (bedScore < 945) {
			color = new Color(28, 28, 28);
		} else {
			color = new Color(0, 0, 0);
		}
		return color;
	}

}
