package com.thinkdifferent.convertpreview.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.consts.ConvertFileTypeEnum;
import com.thinkdifferent.convertpreview.entity.ConvertDocEntity;
import com.thinkdifferent.convertpreview.entity.CoverPage;
import com.thinkdifferent.convertpreview.entity.TargetFile;
import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.service.ConvertDocService;
import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import com.thinkdifferent.convertpreview.utils.ConvertOfdUtil;
import com.thinkdifferent.convertpreview.utils.DocConvertUtil;
import com.thinkdifferent.convertpreview.utils.OnlineCacheUtil;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import com.thinkdifferent.convertpreview.utils.pdfUtil.ConvertPdfUtil;
import com.thinkdifferent.convertpreview.utils.watermark.OfdWaterMarkUtil;
import com.thinkdifferent.convertpreview.utils.watermark.PdfWaterMarkUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.element.Paragraph;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.tool.merge.OFDMerger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.*;

/**
 * 文档处理 service
 *
 * @author ltian
 * @version 1.0
 * @date 2023/11/29 19:00
 */
@Slf4j
@Service
public class ConvertDocServiceImpl implements ConvertDocService {

    @Resource
    private ConvertPdfUtil convertPdfUtil;
    @Resource
    private ConvertOfdUtil convertOfdUtil;

    private Map<String, ConvertTypeService> convertServiceMap;

    /**
     * 文档转换
     *
     * @param convertDocEntity 转换对象
     * @return 转换后文件或父级文件目录
     */
    @Override
    public TargetFile convert(ConvertDocEntity convertDocEntity) throws IOException {
        // 文件记录缓存信息
        File cacheFile = createCacheFile(convertDocEntity);

        // 实际总页数
        int intPageCount = 0;
        // 返回的【页数】
        Long longReturnPageCount = 0L;
        // 页码计数器
        Integer intPageNum = 0;
        if(convertDocEntity.getPageNum() != null &&
                convertDocEntity.getPageNum().getStartNum() != null){
            intPageNum = convertDocEntity.getPageNum().getStartNum();
        }
        // 转换后的目标文件File对象List
        List<File> targetFiles = new ArrayList<>();

        // 获取原始的：目标文件名、目标格式
        String strTargetFileName = convertDocEntity.getOutPutFileName();
        String strTargetFormat = convertDocEntity.getOutPutFileType();

        // 合并后的文件路径及文件名，无后缀
        String strDestPathFileName = convertDocEntity.getWriteBack().getOutputPath() + convertDocEntity.getOutPutFileName();

        try {
            // 循环，每个文件单独处理（先不合并）
            List<Input> listInput = convertDocEntity.getInputFiles();
            for(int i=0;i<listInput.size();i++) {
                Input input = listInput.get(i);

                File fileInput = input.getInputFile();
                String strInputExt = FileUtil.extName(fileInput);

                // 如果转换目标格式是OFD，则文件先转成PDF，处理完毕后，再整体转OFD
                if(!"ofd".equalsIgnoreCase(strInputExt) && "ofd".equalsIgnoreCase(strTargetFormat)){
                    convertDocEntity.setOutPutFileType("pdf");
                    convertDocEntity.setOutPutFileName(strTargetFileName + "_ofd");
                }

                // 1. 单个文件格式转换
                File fileOutSingle = convertSingleFile(fileInput, convertDocEntity);
                // 为文件改名，为后续【文件合并】服务
                if (fileOutSingle.getClass().getName().equals("java.io.File")) {
                    fileOutSingle = FileUtil.rename(
                            fileOutSingle,
                            fileOutSingle.getAbsolutePath() + "_" + i + "." + convertDocEntity.getOutPutFileType(),
                            true);
                }

                if (fileOutSingle != null && fileOutSingle.exists()) {
                    // 2. 封面
                    fileOutSingle = makeCover(convertDocEntity, fileOutSingle);
                    // 3. 水印、归档章、页签
                    // 如果【空白页】无需添加页码，则直接加水印
                    if(!input.getBlankPageHaveNum()){
                        // 添加【页码】全局变量，用于为合并文件添加页码。
                        convertDocEntity.getPageNum().setStartNum(intPageNum);
                        fileOutSingle = markFile(convertDocEntity, fileOutSingle, intPageCount);
                    }

                    // 获取单个文件的页数，并累加
                    if("pdf".equalsIgnoreCase(convertDocEntity.getOutPutFileType()) &&
                            fileOutSingle.getName().endsWith(".pdf")){
                        try (PDDocument doc = Loader.loadPDF(fileOutSingle)) {
                            intPageCount = intPageCount + doc.getPages().getCount();
                            intPageNum = intPageNum  + doc.getPages().getCount();
                        }

                    }else if("ofd".equalsIgnoreCase(convertDocEntity.getOutPutFileType())){
                        try (OFDReader ofdReader = new OFDReader(Paths.get(fileOutSingle.getCanonicalPath()))) {
                            intPageCount = intPageCount + ofdReader.getPageList().size();
                            intPageNum = intPageNum  + ofdReader.getPageList().size();
                        }
                    }else if("jpg".equalsIgnoreCase(convertDocEntity.getOutPutFileType())){
                        intPageCount = 1;
                        intPageNum = 1;
                    }
                    longReturnPageCount = Long.valueOf(intPageCount);

                    // 4 加空白页
                    // 判断是否开启【双面打印】属性，如果为true，则通过添加空白页的方式，确保最后一页为偶数页
                    if(input.getDuplexPrint()){
                        // 如果当前文件【页数】是奇数，则添加空白页
                        if(intPageCount%2 == 1) {
                            if("pdf".equalsIgnoreCase(convertDocEntity.getOutPutFileType())){
                                try (PDDocument doc = Loader.loadPDF(fileOutSingle)) {
                                    PDPage page = new PDPage(doc.getPage(0).getMediaBox());
                                    doc.addPage(page);
                                    doc.save(fileOutSingle.getAbsoluteFile() + "_AddBlank.pdf");
                                    doc.close();

                                    FileUtil.rename(
                                            new File(fileOutSingle.getAbsoluteFile() + "_AddBlank.pdf"),
                                            fileOutSingle.getAbsolutePath(),
                                            true);

                                    intPageCount ++;
                                }

                                if(input.getBlankPageHaveNum()) {
                                    intPageNum++;
                                    fileOutSingle = markFile(convertDocEntity, fileOutSingle, 0);
                                }

                            }else if("ofd".equalsIgnoreCase(convertDocEntity.getOutPutFileType())){
                                // 创建一个新页面，空的
                                File fileBlank = new File(fileOutSingle.getAbsolutePath()+ "_blank.ofd");
                                try(OFDDoc ofdDocBlank = new OFDDoc(Paths.get(fileBlank.getAbsolutePath()))) {
                                    Paragraph p = new Paragraph("");
                                    ofdDocBlank.add(p);
                                }

                                // 声明OFD合并对象
                                File fileTemp = new File(fileOutSingle.toPath() + "_merger.ofd");
                                try (OFDMerger ofdMerger = new OFDMerger(fileTemp.toPath())) {
                                    ofdMerger.add(fileOutSingle.toPath());
                                    ofdMerger.add(fileBlank.toPath());
                                }

                                FileUtil.rename(fileTemp, fileOutSingle.getAbsolutePath(), true);

                                // 删掉这个空页面
                                FileUtil.del(fileBlank);

                                intPageCount ++;
                            }
                        }

                    }

                    targetFiles.add(fileOutSingle);
                }

            }

            // 5 多PDF/OFD文件合并：如果转换后的目标文件有多个，则需要执行“文件合并”
            File fileTarget = mergeFiles(
                    targetFiles,
                    convertDocEntity.getOutPutFileType(),
                    strDestPathFileName + "." + convertDocEntity.getOutPutFileType()
            );


            List<String> docFileExt = Arrays.asList("pdf", "ofd");
            if (docFileExt.contains(convertDocEntity.getOutPutFileType().toLowerCase())) {
                // 6. 截取指定页，形成新的PDF/OFD
                TargetFile targetFileCut = DocConvertUtil.cutFile(convertDocEntity, fileTarget);
                longReturnPageCount = targetFileCut.getLongPageCount();

                // 如果转换目标格式是OFD，则文件先转成PDF，处理完毕后，再整体转OFD
                if("ofd".equalsIgnoreCase(strTargetFormat)){
                    convertDocEntity.setOutPutFileType("ofd");
                    convertDocEntity.setOutPutFileName(strTargetFileName);

                    fileTarget = convertSingleFile(fileTarget, convertDocEntity);
                }

                // 7. 附件
                fileTarget = addAttachments(convertDocEntity, fileTarget);
                // 8. 加密
                fileTarget = fileEncrypt(convertDocEntity, fileTarget);
            }

            TargetFile targetFile = new TargetFile();
            targetFile.setTarget(fileTarget);
            targetFile.setLongPageCount(longReturnPageCount);

            return targetFile;
        } catch (Exception e) {
            log.error("转换异常", e);
            throw e;
        } finally {
            // 99. 删除接口报文缓存文件
            FileUtil.del(cacheFile);
        }
    }

    /**
     * 对输出的文件（PDF、OFD）加密，控制使用权限。
     *
     * @param convertDocEntity 输入的转换对象
     * @param inputFile        需要加密的文件
     * @return 加密后文件
     */
    @SneakyThrows
    private File fileEncrypt(ConvertDocEntity convertDocEntity, File inputFile) {
        if (convertDocEntity.getOutFileEncryptorEntity() == null
                || !convertDocEntity.getOutFileEncryptorEntity().getEncry()) {
            return inputFile;
        }

        String strInputFilePath = FileUtil.getCanonicalPath(inputFile);
        if ("pdf".equalsIgnoreCase(convertDocEntity.getOutPutFileType())) {
            return convertPdfUtil.pdfEncry(strInputFilePath, convertDocEntity.getOutFileEncryptorEntity());
        } else {
            return convertOfdUtil.ofdEncry(strInputFilePath, convertDocEntity.getOutFileEncryptorEntity());
        }
    }

    /**
     * 给文件添加：附件
     *
     * @param targetFile       输入文件（无附件）
     * @param convertDocEntity 转换参数
     * @return 处理后的文件File对象
     */
    private File addAttachments(ConvertDocEntity convertDocEntity, File targetFile) {
        if (Objects.isNull(targetFile) || !StringUtils.equalsAnyIgnoreCase(convertDocEntity.getOutPutFileType(), "pdf", "ofd")) {
            return targetFile;
        }
        // 【附件】List
        List<Input> listAttach = new ArrayList<>();
        // 版式文件附件处理， 是否将输入的文件做为【附件】
        if (convertDocEntity.isInputAsAttach()) {
            listAttach.addAll(convertDocEntity.getInputFiles());
        }
        // 传入的【附件】参数
        if (convertDocEntity.getAttachments() != null && convertDocEntity.getAttachments().size() > 0) {
            listAttach.addAll(convertDocEntity.getAttachments());
        }
        if (CollectionUtil.isNotEmpty(listAttach)) {
            if ("pdf".equalsIgnoreCase(convertDocEntity.getOutPutFileType())) {
                return convertPdfUtil.addAttachments(targetFile, listAttach, convertDocEntity.getOutPutFileType());
            } else if ("ofd".equalsIgnoreCase(convertDocEntity.getOutPutFileType())) {
                return convertOfdUtil.addAttachments(targetFile, listAttach, convertDocEntity.getOutPutFileType());
            }
        }
        return targetFile;
    }

    /**
     * 给文件添加：封面
     *
     * @param convertDocEntity 转换参数
     * @param fileInput        输入文件（无水印）
     * @return 处理后的文件File对象
     */
    @SneakyThrows
    private File makeCover(ConvertDocEntity convertDocEntity, File fileInput) {
        // 如果传入的参数中，包含[封面]，则执行添加封面操作
        if (convertDocEntity.getCoverPage() != null) {
            CoverPage coverPage = convertDocEntity.getCoverPage();
            // 如果输入的文件存在，则进行加封面操作
            if (Objects.nonNull(fileInput)) {
                if ("pdf".equalsIgnoreCase(convertDocEntity.getOutPutFileType())) {
                    return coverPage.add2Pdf(fileInput);
                } else if ("ofd".equalsIgnoreCase(convertDocEntity.getOutPutFileType())) {
                    return coverPage.add2Ofd(fileInput);
                }
            }
        }
        return fileInput;
    }

    /**
     * 给文件添加水印
     *
     * @param convertDocEntity 转换参数
     * @param fileInput        输入文件（无水印）
     * @param intPageCount        【总页数】计数器
     * @return 处理后的文件File对象
     */
    @SneakyThrows
    private File markFile(ConvertDocEntity convertDocEntity, File fileInput,
                          int intPageCount) {
        if (!Arrays.asList("jpg", "pdf", "ofd").contains(FileUtil.extName(fileInput))) {
            return fileInput;
        }
        // 文件的输出路径和文件名
        String strOutPath;
        // 加水印后的临时文件
        File fileTemp = fileInput;
        // 如果传入的参数中，包含图片水印、文字水印、首页水印、条码、页码等，则执行添加水印操作
        if (ArrayUtil.matchIndex(Objects::nonNull,
                convertDocEntity.getPageNum(), convertDocEntity.getCopyRight()) > -1
                &&
                (convertDocEntity.getPageNum().isEnable() ||
                        convertDocEntity.getCopyRight().isEnable()) ||
                ArrayUtil.matchIndex(Objects::nonNull,
                        convertDocEntity.getPngMark(), convertDocEntity.getTextMark(),
                        convertDocEntity.getFirstPageMark(), convertDocEntity.getBarCode()) > -1) {
            // 【页码水印】计数器
            int intPageNum = 0;
            // 如果设置启用【页码水印】，并且【起始页码】不为空，则将计数器-1（后续循环使用）
            if (convertDocEntity.getPageNum().isEnable() &&
                    convertDocEntity.getPageNum().getStartNum() != null) {
                intPageNum = convertDocEntity.getPageNum().getStartNum() - 1;
            }
            // 如果输入的文件存在，则进行加水印操作
            if (Objects.nonNull(fileInput)) {
                if ("pdf".equalsIgnoreCase(convertDocEntity.getOutPutFileType())) {
                    // 如果输出pdf文件，则生成加水印后的"*_wm.pdf"文件
                    strOutPath = fileInput.getAbsolutePath() + "_wm.pdf";
                    PdfWaterMarkUtil.mark4Pdf(fileInput.getAbsolutePath(), strOutPath, convertDocEntity, intPageNum, intPageCount);
                } else if ("ofd".equalsIgnoreCase(convertDocEntity.getOutPutFileType())) {
                    // 如果输出ofd文件，则生成加水印后的"*_wm.ofd"文件
                    strOutPath = fileInput.getAbsolutePath() + "_wm.ofd";
                    OfdWaterMarkUtil.mark4Ofd(fileInput.getAbsolutePath(), strOutPath, convertDocEntity, intPageNum, intPageCount);
                } else {
                    // 否则，获取输入文件的路径作为输出文件路径。
                    strOutPath = fileInput.getAbsolutePath();
                }
                // 声明输出文件File对象
                fileTemp = new File(strOutPath);
            }
        }
        // 如果输出文件不等于输入文件，则将加水印后的文件复制到输出文件夹
        if (fileTemp.exists() && !fileTemp.equals(fileInput)) {
            FileUtil.rename(fileTemp, fileInput.getAbsolutePath(), true);
        }

        // 清除二级临时文件夹
        String strTempPath = SystemUtil.beautifulFilePath(fileTemp.getAbsolutePath());
        strTempPath = strTempPath.substring(0, strTempPath.lastIndexOf("/"));
        File fileTempPath  = new File(strTempPath);
        if(fileTempPath.isDirectory() && fileTempPath.listFiles().length == 0){
            FileUtil.del(fileTempPath);
        }
        return fileInput;
    }

    /**
     * 单个文件转换
     *
     * @param fileInput    单个输入文件对象
     * @param convertDocEntity 转换对象
     * @return 合并后文件
     */
    @SneakyThrows
    private File convertSingleFile(File fileInput, ConvertDocEntity convertDocEntity) {
        // ConvertFileTypeEnum 类型
        String outPutFileType = convertDocEntity.getOutPutFileType();
        // 合并后的文件路径及文件名，无后缀
        String strTargetFilePath = ConvertDocConfigBase.outPutPath + convertDocEntity.getOutPutFileName();
        // 1. 输入文件转换
        // 获取首字母大写的转换类型
        String inputType = StringUtils.capitalize(ConvertFileTypeEnum.getType(fileInput));

        ConvertTypeService convertTypeService;
        // 是否需要生成【只读版式文件】
        if (convertDocEntity.isOutPutReadOnly()) {
            convertTypeService = getConvertTypeService(
                    "convert" + inputType + "2JpgServiceImpl");
            convertTypeService.convert(fileInput, strTargetFilePath + "_jpg");
            OnlineCacheUtil.putTemp(fileInput,"jpg", FileUtil.file( strTargetFilePath+ "_jpg"));
            convertTypeService = getConvertTypeService(
                    "convertImg2" + StringUtils.capitalize(outPutFileType) + "ServiceImpl");

            fileInput = new File(strTargetFilePath + "_jpg");
        } else {
            convertTypeService = getConvertTypeService(
                    "convert" + inputType + "2" + StringUtils.capitalize(outPutFileType) + "ServiceImpl");
        }

        // 删除临时文件及目录
//            FileUtil.del(strTempFilePath + "_jpg");
//            FileUtil.del(strTempFilePath + "_jpg.jpg");
//            FileUtil.del(strTempFilePath + ".pdf.ofd");
        return convertTypeService.convert(fileInput, strTargetFilePath);
    }

    /**
     * 转换、合并文件
     *
     * @param convertDocEntity 转换对象
     * @return 合并后文件
     */
    @SneakyThrows
    private File convertAndMerge(ConvertDocEntity convertDocEntity) {
        // ConvertFileTypeEnum 类型
        String outPutFileType = convertDocEntity.getOutPutFileType();
        List<File> targetFiles = new ArrayList<>();
        // 合并后的文件路径及文件名，无后缀
        String targetFilePath = ConvertDocConfigBase.outPutPath + convertDocEntity.getOutPutFileName();
        // 1. 输入文件转换
        File inputFile;
        int intTemp = 0;
        for (Input input : convertDocEntity.getInputFiles()) {
            inputFile = input.getInputFile();
            String strTempFilePath = targetFilePath + "_" + intTemp;
            // 获取首字母大写的转换类型
            String inputType = StringUtils.capitalize(ConvertFileTypeEnum.getType(inputFile));
            // 是否需要生成【只读版式文件】
            if (convertDocEntity.isOutPutReadOnly()) {
                ConvertTypeService convertTypeService = getConvertTypeService(
                        "convert" + inputType + "2JpgServiceImpl");
                convertTypeService.convert(inputFile, strTempFilePath + "_jpg");
                OnlineCacheUtil.putTemp(inputFile,"jpg", FileUtil.file( strTempFilePath+ "_jpg"));
                convertTypeService = getConvertTypeService(
                        "convertImg2" + StringUtils.capitalize(outPutFileType) + "ServiceImpl");
                targetFiles.add(convertTypeService.convert(new File(strTempFilePath + "_jpg"), strTempFilePath));
            } else {
                ConvertTypeService convertTypeService = getConvertTypeService(
                        "convert" + inputType + "2" + StringUtils.capitalize(outPutFileType) + "ServiceImpl");
                targetFiles.add(convertTypeService.convert(inputFile, strTempFilePath));
            }

            // 删除临时文件及目录
//            FileUtil.del(strTempFilePath + "_jpg");
//            FileUtil.del(strTempFilePath + "_jpg.jpg");
//            FileUtil.del(strTempFilePath + ".pdf.ofd");

            intTemp++;
        }

        // 2. 多文件合并
        return mergeFiles(targetFiles, outPutFileType, targetFilePath + "." + outPutFileType);
    }

    /**
     * 合并转换后的文件
     *
     * @param targetFiles    需要合并的文件列表，除图片外，后缀相同
     * @param outPutFileType 返回的文件类型，图片类可返回父级文件
     * @param targetFilePath 目标文件路径，带后缀
     * @return 合并后的文件或文件目录
     * @see ConvertFileTypeEnum
     */
    private File mergeFiles(List<File> targetFiles, String outPutFileType, String targetFilePath) throws IOException {
        // 单个文件
        ConvertFileTypeEnum convertFileTypeEnum = ConvertFileTypeEnum.valueOfExtName(outPutFileType);
        if (targetFiles.size() == 1) {
            File file = targetFiles.get(0);
            if ("compress".equals(outPutFileType) || ("pdf".equalsIgnoreCase(outPutFileType)
                    && StringUtils.endsWithAny(file.getName().toLowerCase(), ".html", ".xml"))) {
                return file;
            }
            if (file.isDirectory()) {
                if (ConvertFileTypeEnum.img != convertFileTypeEnum) {
                    file = file.listFiles()[0];
                } else if (file.listFiles().length == 1) {
                    return file.listFiles()[0];
                }
            }
            // 重命名成真实的文件名
            File targetFile = new File(targetFilePath);
            FileUtil.move(file, targetFile, true);
            return targetFile;
        }
        switch (convertFileTypeEnum) {
            case img:
                // 多页tif 预览，返回转换后的jpg 目录
                return targetFiles.get(0);
            case ofd:
                // 如果输出文件格式为OFD，则执行OFD合并。
                File fileOfd = new File(targetFilePath);
                // 声明OFD合并对象
                try (OFDMerger ofdMerger = new OFDMerger(fileOfd.toPath())) {
                    // 循环，处理“目标文件列表”中的所有文件，进行合并
                    for (File file : targetFiles) {
                        if (file.length() > 0) {
                            // 将需合并的文件加入“OFD合并对象”
                            ofdMerger.add(file.toPath());
                        }
                    }
                }

                for (File file : targetFiles) {
                    if (file.length() > 0) {
                        // 删除临时文件
                        FileUtil.del(file);
                    }
                }
                // 返回合并后的OFD文件对象
                return fileOfd;
            case pdf:
                // 声明PDF合并工具对象
                PDFMergerUtility pdfMerger = new PDFMergerUtility();
                // 设置合并后的PDF文件的路径和文件名
                pdfMerger.setDestinationFileName(targetFilePath);
                // 循环，处理“目标文件列表”中的所有文件，进行合并
                for (File file : targetFiles) {
                    if (file.length() > 0) {
                        // 将需合并的文件加入“PDF合并工具”对象
                        pdfMerger.addSource(file);
                    }
                }
                // 所有待合并文件处理完毕后，执行“合并”动作。
                pdfMerger.mergeDocuments(null);

                for (File file : targetFiles) {
                    if (file.length() > 0) {
                        // 删除临时文件
                        FileUtil.del(file);
                    }
                }
                // 返回合并后的PDF文件对象
                return new File(pdfMerger.getDestinationFileName());
            default:
                throw new InvalidParameterException("暂不支持需合并的目标格式:" + outPutFileType);
        }
    }

    /**
     * 根据实现类的名称获取对应转换实现类
     *
     * @param serviceName 实现类的名称
     * @return 转换实现类
     */
    private ConvertTypeService getConvertTypeService(String serviceName) {
        if (Objects.isNull(convertServiceMap)) {
            convertServiceMap = SpringUtil.getBeansOfType(ConvertTypeService.class);
        }
        ConvertTypeService convertTypeService = convertServiceMap.get(serviceName);
        Assert.notNull(convertTypeService, "不支持的转换格式：" + serviceName);
        return convertTypeService;
    }

    /**
     * 创建缓存文件，失败重试
     *
     * @param convertDocEntity 转换对象
     * @return 创建后的文件
     */
    private File createCacheFile(ConvertDocEntity convertDocEntity) {
        String content = JSONUtil.toJsonStr(convertDocEntity);
        String contentMd5 = SecureUtil.md5(content);
        File jsonFile = FileUtil.file(ConvertDocConfigBase.inPutTempPath, "inputJson", contentMd5 + ".json");
        FileUtil.writeUtf8String(content, jsonFile);
        return jsonFile;
    }

    /**
     * 根据文件名，删除临时文件夹中的临时文件
     *
     * @param strFileName 临时文件名
     * @return 删除是否成功
     */
    @Override
    public boolean deleteTempFile(String strFileName) {
        String strFilePathName = SystemUtil.beautifulPath(ConvertDocConfigBase.outPutPath) + strFileName;
        return FileUtil.del(strFilePathName);
    }
}
