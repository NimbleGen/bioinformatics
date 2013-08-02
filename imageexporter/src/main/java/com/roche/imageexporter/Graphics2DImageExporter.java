package com.roche.imageexporter;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.imageio.ImageIO;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.AbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.image.TIFFTranscoder;
import org.apache.fop.render.ps.EPSTranscoder;
import org.apache.fop.render.ps.PSTranscoder;
import org.apache.fop.svg.PDFTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This object allows for the exporting of Graphics2D objects to various image
 * formats. Because of the protocols used with rendering on graphics2D objects
 * the export type must be designated at construction(different output types
 * have different protocols for rendering which are hidden by this object). Once
 * created the graphics2D object can be retrieved and drawn on. After the
 * drawing is complete, the image can be exported with a given file name.
 * 
 * example usage: String outputFileName = "sampleOutputImage.pdf";
 * Graphics2DImageExporter pdfExporter = new
 * Graphics2DImageExporter(Graphics2DImageExporter
 * .ImageType.PDF,r.width,r.height); Graphics2D g2d =
 * pdfExporter.getGraphics2D(); //draw on the graphics2D object here
 * pdfExporter.exportImage(outputFileName);
 * 
 * 
 * @author Kurt Heilman
 * 
 */
public class Graphics2DImageExporter {

    private int width, height;
    private ImageType imageType;
    // used for ImageFileType1 & 3
    private Document document;
    private SVGGraphics2D svgGraphics2D;
    // used for ImageFileType2
    private BufferedImage bufferedImage;
    private Graphics2D graphics2D;
    private static final Logger logger = LoggerFactory.getLogger(Graphics2DImageExporter.class);

    // keep track of which file types can be exported
    // and which protocol is needed to export it (designated by ImageFileType1,2
    // & 3)
    public static enum ImageType {

        JPEG(ImageFileType1.JPEG), JPG(ImageFileType1.JPG), PNG(ImageFileType1.PNG), EPS(ImageFileType1.EPS), PS(ImageFileType1.PS), PDF(ImageFileType1.PDF), TIFF(ImageFileType1.TIFF), BMP(
        ImageFileType2.BMP), GIF(ImageFileType2.GIF), SVG(ImageFileType3.SVG);
        private Object imageFileType;

        private ImageType(Object fileType) {
            this.imageFileType = fileType;
        }

        public boolean isGroup1() {
            return (imageFileType instanceof ImageFileType1);
        }

        public boolean isGroup2() {
            return (imageFileType instanceof ImageFileType2);
        }

        public boolean isGroup3() {
            return (imageFileType instanceof ImageFileType3);
        }

        public String getExtension() {
            return this.toString().toLowerCase();
        }
    }

    private static enum ImageFileType1 {

        JPEG(new JPEGTranscoder()), JPG(new JPEGTranscoder()), PNG(new PNGTranscoder()), EPS(new EPSTranscoder()), PS(new PSTranscoder()), PDF(new PDFTranscoder()), TIFF(new TIFFTranscoder());
        private AbstractTranscoder transcoder;

        private ImageFileType1(AbstractTranscoder transcoder) {
            this.transcoder = transcoder;
        }

        // this code is specific to which protocol is used
        public void exportImageToFile(Document document, SVGGraphics2D svg2d, String outputFileName, int width, int height) throws FileNotFoundException, TranscoderException {
            Element svgRoot = document.getDocumentElement();
            svgRoot.setPrefix("svg");

            //transcoder.addTranscodingHint(ImageTranscoder.KEY_BACKGROUND_COLOR
            // , svg2d.getBackground());
            // there is a bug in the export code, so these
            // attributes need to be set for the image to be outputed
            // properly
            svg2d.getRoot(svgRoot);
            svgRoot.setAttributeNS(null, "width", "" + width);
            svgRoot.setAttributeNS(null, "height", "" + height);
            if (this == JPEG || this == JPG) {
                transcoder.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(1.0));
            }
            TranscoderInput input = new TranscoderInput(document);
            OutputStream ostream = new FileOutputStream(outputFileName);
            TranscoderOutput output = new TranscoderOutput(ostream);
            transcoder.transcode(input, output);
            try {
                ostream.flush();
                ostream.close();
                ostream = null;
                System.gc();
            } catch (IOException ex) {
                logger.warn(ex.getMessage());
            }

        }
    }

    private static enum ImageFileType2 {
        // JPEG(), ImageFileType1 Exporter produced better quality
        // JPG(),

        BMP(), GIF();
        // PNG(); ImageFileType1 Exporter produced better quality

        private ImageFileType2() {
        }

        public String extension() {
            return this.toString().toLowerCase();
        }

        // this code is specific to which protocol is used
        public void exportImageToFile(BufferedImage bufferedImage, String outputFileNameWithExtension) throws IOException {
            FileOutputStream out = new FileOutputStream(outputFileNameWithExtension);
            ImageIO.write(bufferedImage, extension(), out);
            try {
                out.flush();
                out.close();
                out = null;
                System.gc();
            } catch (IOException ex) {
                logger.warn(ex.getMessage());
            }
        }
    }

    private static enum ImageFileType3 {

        SVG();

        private ImageFileType3() { 
        }

		//
		// public String extension() {
		// return this.toString().toLowerCase();
		// }

        // this code is specific to which protocol is used
        public void exportImageToFile(SVGGraphics2D svgGraphics2D, String outputFileNameWithExtension) throws IOException {
            boolean useCSS = true;
            Writer out = new OutputStreamWriter(new FileOutputStream(outputFileNameWithExtension));
            svgGraphics2D.stream(out, useCSS);
             try {
                out.flush();
                out.close();
                out = null;
                System.gc();
            } catch (IOException ex) {
                logger.warn(ex.getMessage());
            }
        }
    }

    public Graphics2DImageExporter(ImageType imageType, int width, int height) {
        super();

        this.width = width;
        this.height = height;
        this.imageType = imageType;

        if (imageType.isGroup1() || imageType.isGroup3()) {
            DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

            String svgNS = "http://www.w3.org/2000/svg";
            document = domImpl.createDocument(svgNS, "svg", null);
            // TranscoderInput input = new TranscoderInput(document);

            svgGraphics2D = new SVGGraphics2D(document);

            svgGraphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            svgGraphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            svgGraphics2D.setPaint(java.awt.Color.white);
        } else if (imageType.isGroup2()) {
            bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            graphics2D = bufferedImage.createGraphics();
        }
    }

    public ImageType getImageType() {
        return imageType;
    }

    public Graphics2D getGraphics2D() {
        Graphics2D returnGraphics2D = null;
        if (imageType.isGroup1() || imageType.isGroup3()) {
            returnGraphics2D = svgGraphics2D;
        } else if (imageType.isGroup2()) {
            returnGraphics2D = graphics2D;
        }

        return returnGraphics2D;
    }

    public void exportImage(String outputFileNameWithExtension) throws Exception {
        if (imageType.isGroup1()) {
            ((ImageFileType1) imageType.imageFileType).exportImageToFile(document, svgGraphics2D, outputFileNameWithExtension, width, height);
        } else if (imageType.isGroup2()) {
            ((ImageFileType2) imageType.imageFileType).exportImageToFile(bufferedImage, outputFileNameWithExtension);
        } else if (imageType.isGroup3()) {
            ((ImageFileType3) imageType.imageFileType).exportImageToFile(svgGraphics2D, outputFileNameWithExtension);
        }
    }

    public void setWidth(int width) {
        this.width = width;
        if (imageType.isGroup2()) {
            bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            graphics2D = bufferedImage.createGraphics();
        }
    }

    public void setHeight(int height) {
        this.height = height;
        if (imageType.isGroup2()) {
            bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            graphics2D = bufferedImage.createGraphics();
        }
    }
}
