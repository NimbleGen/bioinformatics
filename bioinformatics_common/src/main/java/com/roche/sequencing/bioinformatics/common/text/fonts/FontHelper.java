package com.roche.sequencing.bioinformatics.common.text.fonts;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FontHelper {

	private static final Logger logger = LoggerFactory.getLogger(FontHelper.class);

	// see internalGetFontNames and main method to see where these names came from
	private final static String[] FONT_FILE_NAMES = new String[] { "Bitstream Vera Sans Mono Bold Oblique.ttf", "Bitstream Vera Sans Mono Bold.ttf", "Bitstream Vera Sans Mono Oblique.ttf",
			"Bitstream Vera Sans Mono Roman.ttf", "BPmonoBold.ttf", "BPmonoItalics.ttf", "Cella.ttf", "CutiveMono-Regular.ttf", "kongtext.ttf", "Lekton-Bold.ttf", "Lekton-Italic.ttf",
			"Lekton-Regular.ttf", "LiberationMono-Bold.ttf", "LiberationMono-BoldItalic.ttf", "LiberationMono-Italic.ttf", "LiberationMono-Regular.ttf", "origap__.ttf", "origa___.ttf", "PTM55FT.ttf",
			"UbuntuMono-B.ttf", "UbuntuMono-BI.ttf", "UbuntuMono-R.ttf", "UbuntuMono-RI.ttf" };

	private static final float DEFAULT_FONT_SIZE = 12f;

	private FontHelper() {
		throw new AssertionError();
	}

	public static List<Font> getAvailableMonoSpacedFonts() {
		List<Font> monoSpacedFonts = new ArrayList<Font>();

		for (String fontFileName : FONT_FILE_NAMES) {
			Font font = getFontUIResourceFromFile(fontFileName, DEFAULT_FONT_SIZE);
			if (font != null) {
				monoSpacedFonts.add(font);
			}
		}

		return monoSpacedFonts;
	}

	private static Font getFontUIResourceFromFile(String fileName, float size) {
		Font font = null;
		try {
			InputStream is = FontHelper.class.getResourceAsStream(fileName);
			font = Font.createFont(Font.TRUETYPE_FONT, is);
			font = font.deriveFont(size);
		} catch (FontFormatException | IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage(), e);
		}
		return font;
	}

	private static String internalGetFontNames() {
		StringBuilder sb = new StringBuilder();
		File file = new File("C:\\kurts_space\\github\\bioinformatics\\bioinformatics_common\\src\\main\\resources\\com\\roche\\sequencing\\bioinformatics\\common\\text\\fonts");
		for (String fileName : file.list()) {
			if (fileName.endsWith(".ttf")) {
				sb.append("\"" + fileName + "\",");
			}
		}
		return sb.substring(0, sb.length() - 1);
	}

	public static void main(String[] args) {
		// It would be nice to just have the ttf files dynamically pulled but this is problematic
		// when embedded in a jar file so I will just use this
		// little script to build up the file names whenever a new file is added
		// and update the FONT_FILE_NAMES value so the fonts can be explicitly pulled
		System.out.println(internalGetFontNames());

		// List<Font> fonts = getAvailableMonoSpacedFonts();
		// System.out.println(fonts.size());
	}

}
