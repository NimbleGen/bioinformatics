package com.roche.sequencing.bioinformatics.common.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.itextpdf.text.Anchor;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class PdfReportUtil {

	public static Font CAT_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
	public static Font NORMAL_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL);
	public static Font HEADER_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 14, Font.BOLD);

	private static Font SUB_FONT = new Font(Font.FontFamily.TIMES_ROMAN, 16, Font.BOLD);

	private PdfReportUtil() {
		throw new AssertionError();
	}

	public static Document createDocument(File outputFile, String title, String keywords, String author, String creator) {
		Document document = new Document();

		try {
			PdfWriter.getInstance(document, new FileOutputStream(outputFile));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e.getMessage(), e);

		} catch (DocumentException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		document.open();
		document.addTitle(title);
		document.addKeywords(keywords);
		document.addAuthor(author);
		document.addCreator(creator);

		return document;
	}

	public static Chapter createChapter(String chapterName, int chapterNumber) {
		Anchor anchor = new Anchor(chapterName, HEADER_FONT);
		anchor.setName(chapterName);
		Chapter chapter = new Chapter(new Paragraph(chapterName, CAT_FONT), chapterNumber);
		return chapter;
	}

	public static Paragraph getPhotoInParagraph(BufferedImage bufferedImage, float scalePercent, float spacingAbove) throws BadElementException, IOException {
		Paragraph paragraph = new Paragraph(" ", SUB_FONT);
		PdfPTable table = new PdfPTable(1);
		table.addCell(getPhotoCell(bufferedImage, scalePercent, true));
		paragraph.add(table);
		paragraph.setLeading(0);
		paragraph.setSpacingAfter(0);
		paragraph.setSpacingBefore(spacingAbove);
		return paragraph;
	}

	public static Paragraph getTextInParagraph(String text) {
		return getTextInParagraph(text, NORMAL_FONT);
	}

	public static Paragraph getHeaderTextInParagraph(String text) {
		return getTextInParagraph(text, HEADER_FONT);
	}

	public static Paragraph getTextInParagraph(String text, Font font) {
		Paragraph paragraph = new Paragraph(" ", font);
		paragraph.add(new Chunk(text));
		return paragraph;
	}

	public static Paragraph getPhotosInParagraph(List<BufferedImage> bufferedImages, float scalePercent, boolean isHorizontallyCentered) throws BadElementException, IOException {
		Paragraph paragraph = new Paragraph(" ", SUB_FONT);
		PdfPTable table = new PdfPTable(1);
		for (BufferedImage bufferedImage : bufferedImages) {
			table.addCell(getPhotoCell(bufferedImage, scalePercent, isHorizontallyCentered));
		}
		table.setHorizontalAlignment(Element.ALIGN_CENTER);

		paragraph.add(table);
		paragraph.setLeading(0);
		paragraph.setSpacingAfter(0);
		paragraph.setSpacingBefore(0);
		return paragraph;
	}

	public static PdfPCell getPhotoCell(BufferedImage bufferedImage, float scalePercent, boolean isHorizontallyCentered) throws BadElementException, IOException {
		Image jpeg = Image.getInstance(bufferedImage, null);
		jpeg.scalePercent(scalePercent);
		jpeg.setAlignment(Image.MIDDLE);
		PdfPCell photoCell = new PdfPCell(jpeg);
		photoCell.setBorder(0);
		if (isHorizontallyCentered) {
			photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		} else {
			photoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		}

		photoCell.setVerticalAlignment(Element.ALIGN_TOP);
		int height = (int) Math.ceil(bufferedImage.getHeight() * scalePercent / 100);
		photoCell.setFixedHeight(height);
		return photoCell;
	}

}
