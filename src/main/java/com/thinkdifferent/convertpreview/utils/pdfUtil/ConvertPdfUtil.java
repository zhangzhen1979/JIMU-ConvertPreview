package com.thinkdifferent.convertpreview.utils.pdfUtil;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigPreview;
import com.thinkdifferent.convertpreview.entity.OutFileEncryptorEntity;
import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import com.thinkdifferent.convertpreview.utils.imgUtil.JpgUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.encryption.*;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * PDF转换工具。
 * 各种格式转换为PDF文件。
 * PDF文件的处理（获取首页图等）
 */
@Component
@Log4j2
public class ConvertPdfUtil {

    @Resource(name = "taskExecutor")
    private Executor executor;

    /**
     * PDF加密，设置权限
     *
     * @param strPdfFilePath         输出的pdf文件路径和文件名
     * @param outFileEncryptorEntity 输出文件加密对象
     * @return
     * @throws IOException
     */
    public File pdfEncry(String strPdfFilePath, OutFileEncryptorEntity outFileEncryptorEntity) throws IOException {
        File filePdf = new File(strPdfFilePath);
        String strPdfEnc = strPdfFilePath + "_enc.pdf";

        if (outFileEncryptorEntity != null && outFileEncryptorEntity.getEncry()) {
            File fileSource = new File(strPdfFilePath);

            PDDocument pdDocument = Loader.loadPDF(fileSource);
            // 读取加密的PDF，需要传入密码
//            @Cleanup PDDocument pdDocument = Loader.loadPDF(fileSource, "123");

            AccessPermission permissions = new AccessPermission();
            // 是否可以插入/删除/旋转页面
            permissions.setCanAssembleDocument(outFileEncryptorEntity.getAssembleDocument());
            // 是否可以复制和提取内容
            permissions.setCanExtractContent(outFileEncryptorEntity.getCopy());

            permissions.setCanExtractForAccessibility(false);
            // 设置用户是否可以填写交互式表单字段（包括签名字段）
            permissions.setCanFillInForm(outFileEncryptorEntity.getFillInForm());
            // 设置用户是否可以修改文档
            permissions.setCanModify(outFileEncryptorEntity.getModify());
            // 设置用户是否可以添加或修改文本注释并填写交互式表单字段，如果canModify()返回true，则创建或修改交互式表单字段（包括签名字段）。
            permissions.setCanModifyAnnotations(outFileEncryptorEntity.getModifyAnnotations());
            // 设置用户是否可以打印。
            permissions.setCanPrint(outFileEncryptorEntity.getPrint());
            // 设置用户是否可以降级格式打印文档
            permissions.setCanPrintFaithful(outFileEncryptorEntity.getPrintDegraded());

            String strOwnerPwd = "";
            String strUserPwd = "";
            if (!StringUtils.isEmpty(outFileEncryptorEntity.getOwnerPassword())) {
                strOwnerPwd = outFileEncryptorEntity.getOwnerPassword();
            }
            if (!StringUtils.isEmpty(outFileEncryptorEntity.getUserPassWord())) {
                strUserPwd = outFileEncryptorEntity.getUserPassWord();
            }
            StandardProtectionPolicy standardProtectionPolicy = new StandardProtectionPolicy(
                    strOwnerPwd,
                    strUserPwd,
                    permissions
            );
            SecurityHandler securityHandler = new StandardSecurityHandler(standardProtectionPolicy);
            securityHandler.prepareDocumentForEncryption(pdDocument);
            PDEncryption pdEncryption = new PDEncryption();
            pdEncryption.setSecurityHandler(securityHandler);
            pdDocument.setEncryptionDictionary(pdEncryption);

            pdDocument.save(strPdfEnc);
            pdDocument.close();

            File filePdfEnc = new File(strPdfEnc);
            if (filePdfEnc.exists()) {
                log.debug("加密后的PDF文件改名，文件{}到{}", strPdfEnc, strPdfFilePath);
                FileUtil.rename(filePdfEnc, FileUtil.getName(filePdf), true);
            }

        }

        return filePdf;
    }

    /**
     * 获取PDF文件首页JPG图片
     *
     * @param strInputPDF  输入的PDF文件路径和文件名
     * @param strOutputJpg 输出的JPG文件路径和文件名
     * @return 转换成功的JPG文件File对象
     */
    public File getFirstJpgFromPdf(String strInputPDF, String strOutputJpg) throws IOException {
        //将读取URL生成File
        File filePdf = new File(strInputPDF);

        // 打开来源 使用pdfbox中的方法
        PDDocument pdfDocument = Loader.loadPDF(filePdf);
        PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);


        // 以400 dpi 读取存入 BufferedImage 对象
        BufferedImage buffImage = pdfRenderer.renderImageWithDPI(0, 400, ImageType.RGB);
        //文件储存对象
        File fileJpg = JpgUtil.bufferedImage2Jpg(buffImage, strOutputJpg);
        if (buffImage != null) {
            buffImage.getGraphics().dispose();
        }

        // 关闭文档
        pdfDocument.close();

        //注意关闭文档与删除文档的顺序
        //删除临时的file
        FileUtil.del(filePdf);

        return fileJpg;

    }

    /**
     * pdf文件转换成jpg图片集
     * 2023年5月4日 新增pdf异步转换为图片功能，同步转换数量：convert.preview.asyncImgNum, 其他部分通过异步转换处理
     *
     * @param filePdf   pdf文件
     * @param listPages 截取页码
     * @param blnAsync  是否异步
     * @return 图片访问集合
     */
    public List<String> pdf2jpg(File filePdf, List<Integer> listPages, boolean blnAsync) {
        return pdf2jpg(filePdf
                , SystemUtil.beautifulPath(ConvertDocConfigBase.outPutPath) + FileUtil.mainName(filePdf)
                , listPages, blnAsync);
    }

    /**
     * pdf文件转换成jpg图片集
     * 2023年5月4日 新增pdf异步转换为图片功能，同步转换数量：convert.preview.asyncImgNum, 其他部分通过异步转换处理
     *
     * @param filePdf      pdf文件
     * @param strTargetDir 目标文件目录
     * @param listPages    截取页码
     * @param blnAsync     是否异步
     * @return 图片访问集合
     */
    public List<String> pdf2jpg(File filePdf, String strTargetDir, List<Integer> listPages, boolean blnAsync) {
        if (FileUtil.extName(filePdf).equals("jpg")) {
            return Collections.singletonList(FileUtil.getCanonicalPath(filePdf));
        }
        List<String> imageUrls = new ArrayList<>();
        String imageFileSuffix = ".jpg";
        // pdf 总页数
        int intBasePageNum = 0;
        // 已完成的图片数量
        int finishedImgNum = 0;
        BufferedImage buffImage = null;
        try (PDDocument doc = Loader.loadPDF(filePdf)) {
            intBasePageNum = doc.getNumberOfPages();
            int pageCount = doc.getNumberOfPages();
            // 异步转换的图片数量
            if (blnAsync && ConvertDocConfigPreview.asyncImgNum > 0) {
                pageCount = Math.min(ConvertDocConfigPreview.asyncImgNum, pageCount);
            }
            PDFRenderer pdfRenderer = new PDFRenderer(doc);

            File fileFolder = new File(strTargetDir);
            // 如果是文件，则删除（后面要创建同名文件夹）
            if (fileFolder.isFile()) {
                fileFolder.delete();
            }

            if (!fileFolder.exists()) {
                // 不存在创建目录， 转换
                fileFolder.mkdirs();
            } else {
                // 已存在，不删除，如果已转化的图片数量 < 首次转换的数量，进行转换，其他，直接返回现有图片数量
                finishedImgNum = FileUtil.listFileNames(strTargetDir).size();
                if (ConvertDocConfigPreview.asyncImgNum > 0) {
                    pageCount = Math.max(pageCount, finishedImgNum);
                }
            }
            String strOutputJpg;

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                if (CollectionUtil.isNotEmpty(listPages) && !listPages.contains(pageIndex + 1)) {
                    // 如果设置了指定页转换，并且当前页不在控制列表中，则跳过，不转换。
                    continue;
                }

                strOutputJpg = strTargetDir + "/" + pageIndex + imageFileSuffix;
                if (!FileUtil.exist(strOutputJpg)) {
                    buffImage = pdfRenderer.renderImageWithDPI(pageIndex, ConvertDocConfigBase.picDpi);
                    //文件储存对象
                    JpgUtil.bufferedImage2Jpg(buffImage, strOutputJpg);
                }
                imageUrls.add(strOutputJpg);
            }
        } catch (IOException e) {
            log.error("Convert PDF to JPG Exception, pdfFilePath：{}", filePdf.getPath(), e);
        } finally {
            if (buffImage != null) {
                buffImage.getGraphics().dispose();
            }
        }

        // 启用异步转换，另起线程继续进行文件转换
        if (ConvertDocConfigPreview.asyncImgNum > 0 &&
                intBasePageNum > ConvertDocConfigPreview.asyncImgNum &&
                intBasePageNum > finishedImgNum) {
            getExecutor().execute(() -> {
                BufferedImage buffImageAsync = null;
                try (PDDocument asyncDoc = Loader.loadPDF(filePdf)) {
                    int asyncPageCount = asyncDoc.getNumberOfPages();
                    PDFRenderer asyncPdfRenderer = new PDFRenderer(asyncDoc);

                    for (int pageIndex = imageUrls.size(); pageIndex < asyncPageCount; pageIndex++) {
                        if (CollectionUtil.isNotEmpty(listPages) && !listPages.contains(pageIndex + 1)) {
                            // 如果设置了指定页转换，并且当前页不在控制列表中，则跳过，不转换。
                            continue;
                        }

                        String strAsyncOutputJpg = strTargetDir + "/" + pageIndex + imageFileSuffix;
                        if (!FileUtil.exist(strAsyncOutputJpg)) {
                            buffImageAsync = asyncPdfRenderer.renderImageWithDPI(pageIndex, ConvertDocConfigBase.picDpi);
                            //文件储存对象
                            JpgUtil.bufferedImage2Jpg(buffImageAsync, strAsyncOutputJpg);
                        }
                    }
                } catch (IOException e) {
                    log.error("Async Convert pdf to jpg exception, pdfFilePath：{}", filePdf.getPath(), e);
                } finally {
                    if (buffImageAsync != null) {
                        buffImageAsync.getGraphics().dispose();
                    }
                }
            });
        }
        return imageUrls;
    }

    /**
     * 将传入的附件加入到PDF中
     *
     * @param fileInput  输入文件（无附件）
     * @param listInputs 附件数组
     * @return
     */
    public File addAttachments(File fileInput, List<Input> listInputs, String strInputType) {
        try (final PDDocument doc = Loader.loadPDF(fileInput)) {
            PDDocumentNameDictionary names = new PDDocumentNameDictionary(doc.getDocumentCatalog());
            PDEmbeddedFilesNameTreeNode efTree = new PDEmbeddedFilesNameTreeNode();
            List<PDEmbeddedFilesNameTreeNode> kids = new ArrayList<>();

            for (int i = 0; i < listInputs.size(); i++) {
                String strFileName = listInputs.get(i).getInputFile().getAbsolutePath();
                strFileName = SystemUtil.beautifulFilePath(strFileName);
                Path pathAtt = Paths.get(strFileName);
                String attachName = strFileName.substring(strFileName.lastIndexOf("/") + 1);

                // first create the file specification, which holds the embedded file
                PDComplexFileSpecification fs = new PDComplexFileSpecification();
                fs.setFileUnicode(attachName);

                // create a dummy file stream, this would probably normally be a FileInputStream
                byte[] data = FileUtil.readBytes(strFileName);
                ByteArrayInputStream byteFile = new ByteArrayInputStream(data);

                // now lets some of the optional parameters
                PDEmbeddedFile ef = new PDEmbeddedFile(doc, byteFile);
                ef.setSubtype(FileUtil.getMimeType(strFileName));
                ef.setSize(data.length);
                ef.setCreationDate(Calendar.getInstance());
                fs.setEmbeddedFile(ef);

                // create a new tree node and add the embedded file
                PDEmbeddedFilesNameTreeNode treeNode = new PDEmbeddedFilesNameTreeNode();
                treeNode.setNames(Collections.singletonMap("Attchement File" + i, fs));

                // add the new node as kid to the root node

                kids.add(treeNode);

                if (!StringUtils.equalsAnyIgnoreCase(strInputType, "path", "local")) {
                    FileUtil.del(strFileName);
                    if (FileUtil.isDirEmpty(pathAtt.getParent())) {
                        FileUtil.del(pathAtt.getParent());
                    }
                }
            }

            efTree.setKids(kids);

            // add the tree to the document catalog
            names.setEmbeddedFiles(efTree);
            doc.getDocumentCatalog().setNames(names);

            String oldFilePath = FileUtil.getCanonicalPath(fileInput);
            File newFile = new File(oldFilePath + "_t.pdf");
            doc.save(newFile);
            FileUtil.move(newFile, fileInput, true);
        } catch (IOException e) {
            System.err.println("Exception while trying to create pdf document - " + e);
        }

        return fileInput;
    }

    private Executor getExecutor() {
        if (Objects.isNull(executor)) {
            executor = SpringUtil.getBean("taskExecutor");
        }
        return executor;
    }
}
