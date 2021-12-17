package org.ofd.render;


import org.apache.commons.io.IOUtils;
import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.block.ImageObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.pageDescription.clips.CT_Clip;
import org.ofdrw.core.pageDescription.clips.Clips;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.text.CT_CGTransform;
import org.ofdrw.core.text.TextCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.geom.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class OFDPageDrawer extends PDFGraphicsStreamEngine {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private GeneralPath linePath = new GeneralPath();
    private int clipWindingRule = -1;

    private OFDCreator ofdCreator;
    private CT_Layer ctLayer;
    private float scale;
    private PDPage page;
    private float PX2MM = 2.834175f;
    private int pageRotation;

    private Area lastClip;

    public CT_Layer getCtLayer() {
        // 获取时，从队列添加
        for (Element ele : queue) {
            ctLayer.add(ele);
        }
        return ctLayer;
    }

    protected OFDPageDrawer(int idx, PDPage page, OFDCreator ofdCreator, float scale) throws IOException {
        super(page);
        this.page = page;
        this.ofdCreator = ofdCreator;
        ctLayer = this.ofdCreator.createLayer();
        this.scale = scale;
    }

    public void drawPage() throws IOException {
        processPage(this.getPage());
    }


    @Override
    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
        linePath.moveTo((float) p0.getX(), (float) p0.getY());
        linePath.lineTo((float) p1.getX(), (float) p1.getY());
        linePath.lineTo((float) p2.getX(), (float) p2.getY());
        linePath.lineTo((float) p3.getX(), (float) p3.getY());
        linePath.closePath();
    }

    @Override
    public void drawImage(PDImage pdImage) throws IOException {
        ByteArrayOutputStream bosImage = new ByteArrayOutputStream();
        String suffix = "png";
        ImageIO.write(pdImage.getImage(), suffix, bosImage);
        String name = String.format("%s.%s", bcMD5(bosImage.toByteArray()), suffix);
        ofdCreator.putImage(name, bosImage.toByteArray(), suffix);

        Matrix ctmNew = this.getGraphicsState().getCurrentTransformationMatrix();
        float imageXScale = ctmNew.getScalingFactorX();
        float imageYScale = ctmNew.getScalingFactorY();
        double x = ctmNew.getTranslateX() * scale;
        double y = (page.getCropBox().getHeight() - ctmNew.getTranslateY() - imageYScale) * scale;
        double w = imageXScale * scale;
        double h = imageYScale * scale;
        ImageObject imageObject = new ImageObject(ofdCreator.getNextRid());
        imageObject.setBoundary(x, y, w, h);
        imageObject.setResourceID(new ST_RefID(ST_ID.getInstance(ofdCreator.getImageMap().get(name))));
        imageObject.setCTM(ST_Array.getInstance(String.format("%.0f 0 0 %.0f 0 0", w, h)));
        setImageClip(imageObject, x, y, w, h);
        ctLayer.add(imageObject);
    }

    private String getPointsFromClipArea(Area area, double x, double y, double w, double h) {
        String abbreviatedData = "";
        double[] coords = new double[6];
        for (PathIterator pi = area.getPathIterator(null);
             !pi.isDone();
             pi.next()) {
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    abbreviatedData += String.format("M %.1f %.1f ", (coords[0] * scale - x) / w, ((page.getCropBox().getHeight() - coords[1]) * scale - y) / h);
                    break;
                case PathIterator.SEG_LINETO:
                    abbreviatedData += String.format("L %.1f %.1f ", (coords[0] * scale - x) / w, ((page.getCropBox().getHeight() - coords[1]) * scale - y) / h);
                    break;
                case PathIterator.SEG_CUBICTO:
                    abbreviatedData += String.format("B %.1f %.1f %.1f %.1f %.1f %.1f ", (coords[0] * scale - x) / w, ((page.getCropBox().getHeight() - coords[1]) * scale - y) / h, (coords[2] * scale - x) / w, ((page.getCropBox().getHeight() - coords[3]) * scale - y) / h, (coords[4] * scale - x) / w, ((page.getCropBox().getHeight() - coords[5]) * scale - y) / h);
                    break;
                case PathIterator.SEG_CLOSE:
                    abbreviatedData += " C";
            }
        }
        return abbreviatedData;
    }

    private void setImageClip(ImageObject imageObject, double ix, double iy, double iw, double ih) {
        Area clippingPath = this.getGraphicsState().getCurrentClippingPath();
        if (clippingPath != lastClip) {
            if (clippingPath.getPathIterator((AffineTransform) null).isDone()) {
                return;
            } else {
                double w = (clippingPath.getBounds().width * scale);
                double h = (clippingPath.getBounds().height * scale);
                double x = (clippingPath.getBounds().x * scale);
                double y = (page.getCropBox().getHeight() - clippingPath.getBounds().y - clippingPath.getBounds().height) * scale;
                x = Double.parseDouble(String.format("%.2f", x));
                y = Double.parseDouble(String.format("%.2f", y));
                w = Double.parseDouble(String.format("%.0f", w));
                h = Double.parseDouble(String.format("%.0f", h));
                double clipX = Math.abs(x - ix) / iw;
                double clipY = Math.abs(y - iy) / ih;
                double clipW = w / iw;
                double clipH = h / ih;
                lastClip = clippingPath;
                String abbreviatedData = getPointsFromClipArea(clippingPath, x, y, w, h);
                if (abbreviatedData.contains("B") || (clipX >= 0 && clipY >= 0 && (clipW >= 0 && clipW <= 1) && (clipH >= 0 && clipH <= 1))) {
                    OFDElement abbreviatedDataEle = OFDElement.getInstance("AbbreviatedData");
                    abbreviatedDataEle.setText(abbreviatedData);
                    CT_Path ctPath = new CT_Path();
                    ctPath.setFill(true);
                    ctPath.setStroke(false);
                    ctPath.setBoundary(clipX, clipY, clipW, clipH);
                    ctPath.add(abbreviatedDataEle);
                    org.ofdrw.core.pageDescription.clips.Area area = new org.ofdrw.core.pageDescription.clips.Area();
                    area.add(ctPath);
                    CT_Clip ct_clip = new CT_Clip();
                    ct_clip.addArea(area);
                    Clips clips = new Clips();
                    clips.addClip(ct_clip);
                    imageObject.setClips(clips);
                }
            }
        }
    }

    private String bcMD5(byte[] imageBytes) {
        Digest digest = new MD5Digest();
        digest.update(imageBytes, 0, imageBytes.length);
        byte[] md5Bytes = new byte[digest.getDigestSize()];
        digest.doFinal(md5Bytes, 0);
        return org.bouncycastle.util.encoders.Hex.toHexString(md5Bytes);
    }

    @Override
    public void clip(int windingRule) throws IOException {
        clipWindingRule = windingRule;
    }

    @Override
    public void moveTo(float x, float y) throws IOException {
        linePath.moveTo(x, y);
    }

    @Override
    public void lineTo(float x, float y) throws IOException {
        linePath.lineTo(x, y);
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException {
        linePath.curveTo(x1, y1, x2, y2, x3, y3);
    }

    @Override
    public Point2D getCurrentPoint() throws IOException {
        return linePath.getCurrentPoint();
    }

    @Override
    public void closePath() throws IOException {
        linePath.closePath();
    }

    @Override
    public void endPath() throws IOException {
        if (clipWindingRule != -1) {
            linePath.setWindingRule(clipWindingRule);
            getGraphicsState().intersectClippingPath(linePath);
            clipWindingRule = -1;
        }
        linePath.reset();
    }

    @Override
    public void strokePath() throws IOException {
        ctLayer.add(getPathObject(false));
        linePath.reset();
    }

    @Override
    public void fillPath(int i) throws IOException {
        ctLayer.add(getPathObject(true));
        linePath.reset();
    }

    /**
     * 获取路径对象
     */
    private PathObject getPathObject(boolean fill) throws IOException {
        double x = 0, y = 0;
        float w = page.getCropBox().getWidth() * scale;
        float h = page.getCropBox().getHeight() * scale;
        double lineWidth = getGraphicsState().getLineWidth() * scale;

        PathObject path = new PathObject(ST_ID.getInstance(String.valueOf(ofdCreator.getNextRid())));
        path.setLineWidth(lineWidth);
        path.setBoundary(x, y, w, h);
        AbbreviatedData data = new AbbreviatedData();
        drawLine(linePath.getPathIterator(null), data, h);
        if (fill) {
            CT_Color nonStrokeColor = getNonStrokeColor();
            if (nonStrokeColor != null) {
                path.setFillColor(nonStrokeColor);
                path.setFill(true);
                path.setStroke(false);
            }
        } else {
            CT_Color strokeColor = getStrokeColor();
            if (strokeColor != null) {
                path.setStroke(true);
                path.setFill(false);
                path.setStrokeColor(strokeColor);
            } else {
                path.setStroke(false);
            }
        }
        path.setAbbreviatedData(data);
        return path;
    }

    /**
     * 获取非stroke颜色
     */
    private CT_Color getNonStrokeColor() throws IOException {
        PDColor color = getGraphicsState().getNonStrokingColor();
        if (color == null) {
            return null;
        }
        return getColor(color);
    }

    /**
     * 获取stroke颜色
     */
    private CT_Color getStrokeColor() throws IOException {
        PDColor color = getGraphicsState().getStrokingColor();
        if (color == null) {
            return null;
        }
        return getColor(color);
    }

    /**
     * 画线
     */
    private void drawLine(PathIterator iterator, AbbreviatedData data, float height) throws IOException {
        double[] coords = new double[6];
        while (!iterator.isDone()) {
            switch (iterator.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    data.moveTo(coords[0] * scale, height - coords[1] * scale);
                    break;
                case PathIterator.SEG_LINETO:
                    data.lineTo(coords[0] * scale, height - coords[1] * scale);
                    break;
                case PathIterator.SEG_CUBICTO:
                    data.B(coords[0] * scale, height - coords[1] * scale,
                            coords[2] * scale, height - coords[3] * scale,
                            coords[4] * scale, height - coords[5] * scale);
                    break;
                case PathIterator.SEG_CLOSE:
                    data.close();
                    closePath();
                    break;
                default:
                    break;
            }
            iterator.next();
        }
    }

    @Override
    public void fillAndStrokePath(int windingRule) throws IOException {
        GeneralPath path = (GeneralPath) this.linePath.clone();
        this.fillPath(windingRule);
        this.linePath = path;
        this.strokePath();
    }

    @Override
    public void shadingFill(COSName cosName) throws IOException {

    }

    private TextObject $textObj = null;
    private TextCode $textCode = null;
    private Double $deltaX = 0d;
    private List<String> $deltaXList = null;
    private Double $deltaY = 0d;
    private List<String> $deltaYList = null;
    private StringBuffer $text = null;
    // 字符索引
    private int $stringIndex = 0;
    private List<String> $glyphs = null;
    private CmapLookup $cmap = null;
    private byte[] textByte;
    // 字体转换索引
    private List<String> $cgTransforms = null;
    /**
     * 元素队列
     */
    private List<Element> queue = new ArrayList<Element>();

    @Override
    protected void showText(byte[] string) throws IOException {
        // 初始化
        $stringIndex = 0;
        $textObj = null;
        $textCode = null;
        $deltaX = 0d;
        $deltaY = 0d;
        $text = new StringBuffer();
        $glyphs = new ArrayList<String>();
        $deltaXList = new ArrayList<String>();
        $deltaYList = new ArrayList<String>();
        $cgTransforms = new ArrayList<String>();
        $cmap = null;
        textByte = string;
        // 处理文字
        super.showText(string);

        // 完成添加文字
        String text = $text.toString();
        if (null == text || "".equals(text)) {
            return;
        }
        if ($glyphs != null && $glyphs.size() > 0) {
            // 添加字符变换
            addCGTransform(text);
        }

        if ($deltaXList != null && $deltaXList.size() > 0) {
            // 宽度根据所有字体累加
            Double deltaWidth = $textObj.getBoundary().getWidth();
            for (String w : $deltaXList) {
                deltaWidth += Double.valueOf(w);
            }
            $textObj.setBoundary($textObj.getBoundary().setWidth(deltaWidth));

            $textCode.setDeltaX(new ST_Array($deltaXList.toArray(new String[$deltaXList.size()])));
        }

        if ($deltaYList != null && $deltaYList.size() > 0) {
            // 高度根据所有字体累加
            Double deltaHeight = $textObj.getBoundary().getHeight();
            for (String h : $deltaYList) {
                deltaHeight += Double.valueOf(h);
            }
            $textObj.setBoundary($textObj.getBoundary().setHeight(deltaHeight));
            $textCode.setDeltaY(new ST_Array($deltaYList.toArray(new String[$deltaYList.size()])));
        }

        // 扩展
        drawTextParam();
        $textCode.setContent(text);
        $textObj.addTextCode($textCode);
        queue.add($textObj);
    }

    /**
     * Glyph bounding boxes.
     */
    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code,
                             Vector displacement) throws IOException {
        String unicode = font.toUnicode(code);
        boolean isUnicode = false;
        if (null == unicode || "".equals(unicode)) {
            unicode = "¤";// 占位符
            isUnicode = true;
        }

        $cgTransforms.add($stringIndex + "-" + unicode.length());
        PDGraphicsState state = getGraphicsState();
        RenderingMode renderingMode = state.getTextState().getRenderingMode();
        Matrix ctm = state.getCurrentTransformationMatrix();
        float fontSize = state.getTextState().getFontSize();
        float horizontalScaling = state.getTextState().getHorizontalScaling()
                / 100f;
        Matrix textMatrix = getTextMatrix();

        double angle = getRotate(textRenderingMatrix);
        BoundingBox bbox = font.getBoundingBox();
        if (bbox.getLowerLeftY() < Short.MIN_VALUE) {
            // PDFBOX-2158 and PDFBOX-3130
            // files by Salmat eSolutions / ClibPDF Library
            bbox.setLowerLeftY(-(bbox.getLowerLeftY() + 65536));
        }
        // 1/2 the bbox is used as the height todo: why?
        float glyphHeight = bbox.getHeight() / 2;

        // sometimes the bbox has very high values, but CapHeight is OK
        PDFontDescriptor fontDescriptor = font.getFontDescriptor();
        if (fontDescriptor != null) {
            float capHeight = fontDescriptor.getCapHeight();
            if (Float.compare(capHeight, 0) != 0 && (capHeight < glyphHeight
                    || Float.compare(glyphHeight, 0) == 0)) {
                glyphHeight = capHeight;
            }
            // PDFBOX-3464, PDFBOX-4480, PDFBOX-4553:
            // sometimes even CapHeight has very high value, but Ascent and
            // Descent are ok
            float ascent = fontDescriptor.getAscent();
            float descent = fontDescriptor.getDescent();
            if (capHeight > ascent && ascent > 0 && descent < 0
                    && ((ascent - descent) / 2 < glyphHeight
                    || Float.compare(glyphHeight, 0) == 0)) {
                glyphHeight = (ascent - descent) / 2;
            }
        }

        // transformPoint from glyph space -> text space
        float height;
        if (font instanceof PDType3Font) {
            height = font.getFontMatrix().transformPoint(0, glyphHeight).y;
        } else {
            height = glyphHeight / 1000;
        }

        float displacementX = displacement.getX();
        // the sorting algorithm is based on the width of the character. As the
        // displacement
        // for vertical characters doesn't provide any suitable value for it, we
        // have to
        // calculate our own
        TrueTypeFont ttf = null;
        if (font instanceof PDTrueTypeFont) {
            ttf = ((PDTrueTypeFont) font).getTrueTypeFont();
        } else if (font instanceof PDType0Font) {
            PDCIDFont cidFont = ((PDType0Font) font).getDescendantFont();
            if (cidFont instanceof PDCIDFontType2) {
                ttf = ((PDCIDFontType2) cidFont).getTrueTypeFont();
            }
        }

        if (font.isVertical()) {
            displacementX = font.getWidth(code) / 1000;
            // there may be an additional scaling factor for true type fonts
            if (ttf != null && ttf.getUnitsPerEm() != 1000) {
                displacementX *= 1000f / ttf.getUnitsPerEm();
            }
        }

        //
        // legacy calculations which were previously in PDFStreamEngine
        //
        // DO NOT USE THIS CODE UNLESS YOU ARE WORKING WITH PDFTextStripper.
        // THIS CODE IS DELIBERATELY INCORRECT
        //

        // (modified) combined displacement, this is calculated *without* taking
        // the character
        // spacing and word spacing into account, due to legacy code in
        // TextStripper
        float tx = displacementX * fontSize * horizontalScaling;
        float ty = displacement.getY() * fontSize;

        // (modified) combined displacement matrix
        Matrix td = Matrix.getTranslateInstance(tx, ty);

        // (modified) text rendering matrix
        Matrix nextTextRenderingMatrix = td.multiply(textMatrix).multiply(ctm); // text
        // space
        // ->
        // device
        // space
        float nextX = nextTextRenderingMatrix.getTranslateX();
        float nextY = nextTextRenderingMatrix.getTranslateY();

        float dxDisplay = nextX - textRenderingMatrix.getTranslateX();
        float dyDisplay = height * textRenderingMatrix.getScalingFactorY();

        float glyphSpaceToTextSpaceFactor = 1 / 1000f;
        if (font instanceof PDType3Font) {
            glyphSpaceToTextSpaceFactor = font.getFontMatrix().getScaleX();
        }

        float spaceWidthText = 0;
        try {
            spaceWidthText = font.getSpaceWidth() * glyphSpaceToTextSpaceFactor;
        } catch (Throwable exception) {
            logger.error(exception.getMessage(), exception);
        }

        if (spaceWidthText == 0) {
            spaceWidthText = font.getAverageFontWidth()
                    * glyphSpaceToTextSpaceFactor;
            spaceWidthText *= .80f;
        }
        if (spaceWidthText == 0) {
            spaceWidthText = 1.0f; // if could not find font, use a generic
            // value
        }

        // the space width has to be transformed into display units
        float spaceWidthDisplay = spaceWidthText
                * textRenderingMatrix.getScalingFactorX();

        if (angle == 0d) {
            fontSize = textRenderingMatrix.getScaleY() / PX2MM;
        }
        // 修复字体水平旋转，大小为空
        if (fontSize == 0) {
            fontSize = dyDisplay / PX2MM;
        }

        TextPosition textPosition = new TextPosition(pageRotation,
                page.getCropBox().getWidth(), page.getCropBox().getHeight(), textRenderingMatrix,
                nextX, nextY, Math.abs(dyDisplay), dxDisplay,
                Math.abs(spaceWidthDisplay), unicode, new int[]{code}, font,
                fontSize, (int) (fontSize * textMatrix.getScalingFactorX()));
        String fontName = "";
        if (font.getFontDescriptor() != null) {
            fontName = font.getFontDescriptor().getFontFamily();
            if (null == fontName || "".equals(fontName)) {
                fontName = font.getName();
            }
        }
        boolean willBeSubset = font.isEmbedded() || font.willBeSubset();
        String text = unicode;
        $text.append(text);
        int fontInt = 1;
        if ($stringIndex == 0) {
            long textID = ofdCreator.getNextRid();
            $textObj = new TextObject(textID);

            String fontHash = "" + font.hashCode();
            String fontId = ofdCreator.getFontMap().get(fontHash);
            if (fontId == null) {
                byte[] fontBytes = writeFont(font);
                fontId = ofdCreator.putFont(fontHash, fontName, fontName, fontBytes, ".otf");
            }
            fontInt = Integer.valueOf(fontId);

            $textCode = new TextCode();
            AffineTransform transform = new AffineTransform();
            transform.rotate(Math.toRadians(angle), textPosition.getWidth(),
                    textPosition.getHeight());

            if (angle == 0)
			{
				$textCode.setX(0d);
				$textCode.setY((double)fontSize);
				$textObj.setBoundary(
								textPosition.getX() / PX2MM,
						Math.abs(
								textPosition.getY() / PX2MM
										- fontSize),
						textRenderingMatrix.getScaleX()
								/ PX2MM,
						dyDisplay).setFont(fontInt)
						.setSize((double)fontSize);
			}
			else
			{
				$textCode.setX(0d);
				$textCode.setY(0d);
				$textObj.setBoundary(0, 0, textPosition.getX(),
						textPosition.getY()).setFont(fontInt).setSize(fontSize / (double)PX2MM);
			}

        }

        Integer glyphId = code;
        if (font instanceof PDTrueTypeFont) {
            PDTrueTypeFont ttfFont = (PDTrueTypeFont) font;
            glyphId = ttfFont.codeToGID(code);
        } else if (font instanceof PDType1CFont) {
            PDType1CFont type1CFont = (PDType1CFont) font;
            String name = type1CFont.codeToName(code);
            glyphId = type1CFont.getCFFType1Font().nameToGID(name);
        }
        if ($stringIndex == 0 && ttf != null && willBeSubset) {
            try {
                $cmap = ttf.getUnicodeCmapLookup();
            } catch (Exception e) {
                //logger.warn("加载字体异常",e);
            }
            /**
             * 存在cmap,从cmap中获取glyphId,否则code即为glyphId<br>
             * 详见PDFDebugger查看pdf内部结构方法源码 <br>
             *
             * @link org.apache.pdfbox.debugger.PDFDebugger
             */
            if ($cmap != null && ttf != null && willBeSubset) {
                glyphId = $cmap.getGlyphId(text.codePointAt(0));
            }
        }
        // unicode编码字体
        if (isUnicode || glyphId == 0) {
            glyphId = code;
        }
        if (willBeSubset) {
            $glyphs.add("" + glyphId);
        }

        Double topLeftX = textPosition.getX() / (double) PX2MM;
        if ($stringIndex > 0 && $deltaX.compareTo(topLeftX) != 0) {
            Double w = topLeftX - $deltaX;
            $deltaXList.add("" + w);
        }
        $deltaX = topLeftX;

        Double topLeftY = textPosition.getY() / (double) PX2MM;

        if ($stringIndex > 0 && $deltaY.compareTo(topLeftY) != 0) {
            Double h = topLeftY - $deltaY;
            $deltaYList.add("" + h);
        }
        $deltaY = topLeftY;

        Float scaleX = 1f;
        Float scaleY = 1f;
        Float lineWidth = state.getLineWidth();
        // 粗体
        if (renderingMode.isStroke() && lineWidth != null) {
            // minimum line width as used by Adobe Reader
            if (lineWidth < 0.25) {
                lineWidth = 0.25f;
            }
            $textObj.setLineWidth(
                    lineWidth.doubleValue() / PX2MM);
        }

        if (angle == 0d) {
            $textObj.setCTM(
                    new ST_Array(scaleX, ctm.getShearX() / PX2MM,
                            ctm.getShearY() / PX2MM, scaleY, 0, 0));
        } else {
            $textObj.setCTM(new ST_Array(
                    nextTextRenderingMatrix.getScaleX(),
                    nextTextRenderingMatrix.getShearX(),
                    nextTextRenderingMatrix.getShearY(),
                    nextTextRenderingMatrix.getScaleY(),
                    textPosition.getX() / PX2MM,
                    textPosition.getY() / PX2MM));
        }
        if (nextTextRenderingMatrix.getScaleX() != nextTextRenderingMatrix.getScaleY()) {
            $textObj.setHScale((double) nextTextRenderingMatrix.getScaleX());
        }
        $stringIndex++;
    }

    private byte[] writeFont(PDFont font) {
        byte[] fontBytes = null;
        InputStream is = null;
        try {
            if (font instanceof PDTrueTypeFont) {
                PDTrueTypeFont f = (PDTrueTypeFont) font;
                is = f.getTrueTypeFont().getOriginalData();
            } else if (font instanceof PDType0Font) {
                PDType0Font type0Font = (PDType0Font) font;
                if (type0Font.getDescendantFont() instanceof PDCIDFontType2) {
                    PDCIDFontType2 ff = (PDCIDFontType2) type0Font
                            .getDescendantFont();
                    is = ff.getTrueTypeFont().getOriginalData();
                } else if (type0Font
                        .getDescendantFont() instanceof PDCIDFontType0) {
                    // a Type0 CIDFont contains CFF font
                    PDCIDFontType0 cidType0Font = (PDCIDFontType0) type0Font
                            .getDescendantFont();
                    PDFontDescriptor s = cidType0Font.getFontDescriptor();
                    PDStream s1 = s.getFontFile();
                    if (s1 != null) {
                        is = s1.createInputStream();
                    }
                    PDStream s2 = s.getFontFile2();
                    if (s2 != null) {
                        is = s2.createInputStream();
                    }
                    PDStream s3 = s.getFontFile3();
                    if (s3 != null) {
                        is = s3.createInputStream();
                    }
                } else {
                    PDType0Font f = (PDType0Font) font;
                    PDFontDescriptor s = f.getFontDescriptor();
                    PDStream s1 = s.getFontFile();
                    if (s1 != null) {
                        is = s1.createInputStream();
                    }
                    PDStream s2 = s.getFontFile2();
                    if (s2 != null) {
                        is = s2.createInputStream();
                    }
                    PDStream s3 = s.getFontFile3();
                    if (s3 != null) {
                        is = s3.createInputStream();
                    }
                }
            } else if (font instanceof PDType1Font) {
                PDType1Font f = (PDType1Font) font;
                PDFontDescriptor s = f.getFontDescriptor();
                PDStream s1 = s.getFontFile();
                if (s1 != null) {
                    is = s1.createInputStream();
                }
                PDStream s2 = s.getFontFile2();
                if (s2 != null) {
                    is = s2.createInputStream();
                }
                PDStream s3 = s.getFontFile3();
                if (s3 != null) {
                    is = s3.createInputStream();
                }
            } else if (font instanceof PDType1CFont) {
                PDType1CFont f = (PDType1CFont) font;
                PDFontDescriptor s = f.getFontDescriptor();
                PDStream s1 = s.getFontFile();
                if (s1 != null) {
                    is = s1.createInputStream();
                }
                PDStream s2 = s.getFontFile2();
                if (s2 != null) {
                    is = s2.createInputStream();
                }
                PDStream s3 = s.getFontFile3();
                if (s3 != null) {
                    is = s3.createInputStream();
                }
            } else if (font instanceof PDType3Font) {
                PDType3Font f = (PDType3Font) font;
                PDFontDescriptor s = f.getFontDescriptor();
                PDStream s1 = s.getFontFile();
                if (s1 != null) {
                    is = s1.createInputStream();
                }
                PDStream s2 = s.getFontFile2();
                if (s2 != null) {
                    is = s2.createInputStream();
                }
                PDStream s3 = s.getFontFile3();
                if (s3 != null) {
                    is = s3.createInputStream();
                }
            }
            if (is != null) {
                fontBytes = IOUtils.toByteArray(is);
            } else {
                logger.error("生成字体文件异常:" + font.getClass());
            }
        } catch (Exception e) {
            logger.error("生成字体文件异常:" + font.getName(), e);
        } finally {
			IOUtils.closeQuietly(is);
		}
        return fontBytes;
    }

    /**
     * 绘画文字扩展属性<br/>
     * <p>
     * 绘画文字扩展属性
     * </p>
     *
     * @throws IOException 异常
     */
    private void drawTextParam() throws IOException {
        PDGraphicsState state = getGraphicsState();
        RenderingMode renderingMode = state.getTextState().getRenderingMode();

        setColor($textObj, state.getNonStrokingColor(), true);
        if (renderingMode.isFill()) {
            $textObj.setFill(true);
        }
        setColor($textObj, state.getStrokingColor(), false);
        if (renderingMode.isStroke()) {
            $textObj.setStroke(true);
        }
    }

    /**
     * 计算旋转角度<br/>
     * <p>
     * 计算旋转角度
     * </p>
     *
     * @param textMatrix
     * @return
     */
    private double getRotate(Matrix textMatrix) {
        double angle = 180 * Math.atan2(Float.valueOf(textMatrix.getShearX()),
                Float.valueOf(textMatrix.getScaleY())) / Math.PI;
        if (angle < 0) {
            angle = 360 + angle;
        }
        return angle;
    }

    private void setColor(Object object, PDColor color,
                          boolean noStroke) throws IOException {
        if (color == null) {
            return;
        }
        CT_Color ctColor = getColor(color);
        if (noStroke) {
            ctColor.setOFDName("FillColor");
            if (object instanceof TextObject) {
                TextObject o = (TextObject) object;
                o.setFillColor(ctColor);
            } else if (object instanceof PathObject) {
                PathObject o = (PathObject) object;
                o.setFillColor(ctColor);
            } else if (object instanceof CT_Path) {
                CT_Path o = (CT_Path) object;
                o.setFillColor(ctColor);
            }
        } else {
            ctColor.setOFDName("StrokeColor");
            if (object instanceof TextObject) {
                TextObject o = (TextObject) object;
                o.setStrokeColor(ctColor);
            } else if (object instanceof PathObject) {
                PathObject o = (PathObject) object;
                o.setStrokeColor(ctColor);
            } else if (object instanceof CT_Path) {
                CT_Path o = (CT_Path) object;
                o.setStrokeColor(ctColor);
            }
        }
    }

    private CT_Color getColor(PDColor color) throws IOException {
        CT_Color ctColor = new CT_Color();
        if (color != null) {
            PDColorSpace colorSpace = color.getColorSpace();
            float[] rgb = colorSpace.toRGB(color.getComponents());
            ctColor = CT_Color.rgb(toRgbNumber(rgb[0]), toRgbNumber(rgb[1]), toRgbNumber(rgb[2]));
        }
        return ctColor;
    }

    /**
	 * 添加字符变换
	 * 
	 * @param text
	 */
	private void addCGTransform(String text) {
		int total = text.length();
        CT_CGTransform cgTransform = new CT_CGTransform();
        cgTransform.setCodePosition(0);
        cgTransform.setGlyphCount(total);
        cgTransform.setCodeCount(total);
        cgTransform.setGlyphs(new ST_Array($glyphs.toArray(new String[$glyphs.size()])));
        $textObj.addCGTransform(cgTransform);
	}

    /**
     * 转换为rgb
     */
    private int toRgbNumber(float color) {
        return color < 0 ? 0 : (int) ((color > 1 ? 1 : color) * 255);
    }
}
