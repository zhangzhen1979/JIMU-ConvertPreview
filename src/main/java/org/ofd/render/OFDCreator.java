package org.ofd.render;

import org.ofd.render.config.OfdResIdDefine;
import org.ofd.render.dir.DocDir;
import org.ofd.render.dir.OFDDir;
import org.ofd.render.dir.PageDir;
import org.ofd.render.dir.PagesDir;
import org.ofdrw.core.basicStructure.doc.CT_CommonData;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicStructure.pageObj.Content;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageTree.Page;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.MediaType;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.core.basicStructure.res.resources.ColorSpaces;
import org.ofdrw.core.basicStructure.res.resources.Fonts;
import org.ofdrw.core.basicStructure.res.resources.MultiMedias;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.color.colorSpace.BitsPerComponent;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.color.colorSpace.OFDColorSpaceType;
import org.ofdrw.core.text.font.CT_Font;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OFDCreator {
    private Map<String, String> fontMap;
    private Map<String, String> imageMap;
    private OFDDir ofdDir;

    private DocDir docDir;

    private long localRID = OfdResIdDefine.RID_VarStart;

    public long getNextRid() {
        return ++localRID;
    }

    public long getCurrRid() {
        return localRID;
    }

    public Map<String, String> getFontMap() {
        return fontMap;
    }

    public Map<String, String> getImageMap() {
        return imageMap;
    }

    private MultiMedias mms;

    public OFDCreator() {
        fontMap = new HashMap<>();
        imageMap = new HashMap<>();
        ofdDir = new OFDDir();

        //生成ofd.xml文件
        OFD ofd = new OFD();
        ofd.attribute("Version").setValue("1.1");

        DocBody docBody = new DocBody();
        docBody.setDocRoot(new ST_Loc("Doc_0/Document.xml"));

        CT_DocInfo docInfo = new CT_DocInfo();
        docInfo.setDocID(UUID.randomUUID());
        docInfo.setCreatorVersion("1.0");
        docInfo.setAuthor("OFD");
        docInfo.setCreationDate(LocalDate.now());
        docInfo.setCreator("OFD");
        docBody.setDocInfo(docInfo);
        ofd.addDocBody(docBody);
        ofdDir.add(this.genDocDir());
        ofdDir.setOfd(ofd);
    }

    private DocDir genDocDir() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        docDir = new DocDir();
        docDir.setPublicRes(genPublicRes());
        docDir.setDocumentRes(genDocumentRes());
        Document doc = genDoc();
        docDir.setDocument(doc);

        PagesDir pagesDir = new PagesDir();
        docDir.setPages(pagesDir);
        return docDir;
    }

    private Res genPublicRes() {
        Res ret = new Res();
        ret.setBaseLoc(new ST_Loc("Res"));
        CT_ColorSpace clrSpace = new CT_ColorSpace(OFDColorSpaceType.RGB, OfdResIdDefine.RID_ColorSpace);
        clrSpace.setBitsPerComponent(BitsPerComponent.BIT_8);
        ColorSpaces clrSpaces = new ColorSpaces();
        clrSpaces.addColorSpace(clrSpace);
        ret.addResource(clrSpaces);

        Fonts fonts = new Fonts();
        ret.addResource(fonts);

        return ret;
    }

    private Res genDocumentRes() {
        Res ret = new Res();
        ret.setBaseLoc(new ST_Loc("Res"));

        mms = new MultiMedias();
        ret.add(mms);

        return ret;
    }

    public Document genDoc() {
        Document doc = new Document();

        //1. 创建commonData
        CT_CommonData commonData = new CT_CommonData();
        commonData.setDefaultCS(new ST_RefID(OfdResIdDefine.RID_ColorSpace));

        //1.1页面区域
        CT_PageArea pageArea = new CT_PageArea();
        pageArea.setPhysicalBox(0, 0, 210, 297);
        pageArea.setApplicationBox(0, 0, 210, 297);
        pageArea.setContentBox(0, 0, 210, 297);
        commonData.setPageArea(pageArea);
        commonData.setPublicRes(new ST_Loc("PublicRes.xml"));
        commonData.setDocumentRes(new ST_Loc("DocumentRes.xml"));
        doc.setCommonData(commonData);

        Pages pages = new Pages();
        doc.setPages(pages);
        return doc;
    }

    public String putFont(String familyName, String fontName, byte[] fontBytes, String suffix) {
        String fontId = this.fontMap.get(fontName);
        if (this.fontMap.get(fontName) == null) {
            long currentId = this.getNextRid();
            CT_Font fntKt = new CT_Font();
            fntKt.setFamilyName(familyName);
            fntKt.setFontName(fontName);
            fntKt.setID(currentId);
            if (fontBytes != null) {
                fntKt.setFontFile(new ST_Loc(fontName + suffix));
                docDir.addResource(fontName + suffix, fontBytes);
            }
            Fonts fonts = this.ofdDir.getDocDefault().getPublicRes().getFonts().get(0);
            if (fonts != null) {
                this.ofdDir.getDocDefault().getPublicRes().getFonts().get(0).addFont(fntKt);
            }
            fontId = String.valueOf(currentId);
            this.fontMap.put(fontName, fontId);
        }
        return fontId;
    }

    // 以hashcode作为文件名，防止字体重名
    public String putFont(String fontHash, String familyName, String fontName, byte[] fontBytes, String suffix) {
        String fontId = this.fontMap.get(fontHash);
        if (fontId == null) {
            long currentId = this.getNextRid();
            CT_Font fntKt = new CT_Font();
            fntKt.setFamilyName(familyName);
            fntKt.setFontName(fontName);
            fntKt.setID(currentId);
            if (fontBytes != null) {
                fntKt.setFontFile(new ST_Loc("font_" + currentId + suffix));
                docDir.addResource("font_" + currentId + suffix, fontBytes);
            }
            Fonts fonts = this.ofdDir.getDocDefault().getPublicRes().getFonts().get(0);
            if (fonts != null) {
                this.ofdDir.getDocDefault().getPublicRes().getFonts().get(0).addFont(fntKt);
            }
            fontId = String.valueOf(currentId);
            this.fontMap.put(fontHash, fontId);
        }
        return fontId;
    }

    public void putImage(String name, byte[] imageBytes, String suffix) {
        if (this.imageMap.get(name) == null) {
            long currentId = this.getNextRid();
            CT_MultiMedia mmEwm = new CT_MultiMedia();
            mmEwm.setID(new ST_ID(currentId));
            mmEwm.setType(MediaType.Image);
            mmEwm.setFormat(suffix.toUpperCase());
            mmEwm.setMediaFile(new ST_Loc(name));
            mms.addMultiMedia(mmEwm);
            docDir.addResource(name, imageBytes);
            this.imageMap.put(name, String.valueOf(currentId));
        }
    }

    public void addPage(int idx) {
        Page page = new Page(getNextRid(), String.format("Pages/Page_%d/Content.xml", idx));
        docDir.getDocument().getPages().addPage(page);
    }


    public CT_Layer createLayer() {
        CT_Layer layerInv = new CT_Layer();
        layerInv.setObjID(new ST_ID(getNextRid()));
        return layerInv;
    }

    public void addPageContent(int idx, CT_Layer ctLayer, float width, float height) {
        PageDir pageDirInv = new PageDir();
        pageDirInv.setIndex(idx);
        org.ofdrw.core.basicStructure.pageObj.Page pageInv = new org.ofdrw.core.basicStructure.pageObj.Page();
        CT_PageArea areaInv = new CT_PageArea();
        areaInv.setPhysicalBox(0, 0, width, height);
        pageInv.setArea(areaInv);
        Content contentInv = new Content();
        contentInv.addLayer(ctLayer);
        pageInv.setContent(contentInv);
        pageDirInv.setContent(pageInv);
        docDir.getPages().add(pageDirInv);
    }

    public byte[] jar() throws IOException {
        docDir.getDocument().getCommonData().setMaxUnitID(getCurrRid());
        Map<String, byte[]> virtualFileMap = new ConcurrentHashMap<>(64);
        return ofdDir.jar(virtualFileMap);
    }

    public void jar(OutputStream output) throws IOException {
        docDir.getDocument().getCommonData().setMaxUnitID(getCurrRid());
        Map<String, byte[]> virtualFileMap = new ConcurrentHashMap<>(64);
        ofdDir.jar(virtualFileMap, output);
    }
}
