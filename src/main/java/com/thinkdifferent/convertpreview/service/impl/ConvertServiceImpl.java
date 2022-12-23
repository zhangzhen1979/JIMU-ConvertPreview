package com.thinkdifferent.convertpreview.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.lowagie.text.DocumentException;
import com.thinkdifferent.convertpreview.config.ConvertConfig;
import com.thinkdifferent.convertpreview.config.SystemConstants;
import com.thinkdifferent.convertpreview.entity.CallBackResult;
import com.thinkdifferent.convertpreview.entity.ConvertEntity;
import com.thinkdifferent.convertpreview.entity.InputType;
import com.thinkdifferent.convertpreview.entity.WriteBackResult;
import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.entity.input.InputPath;
import com.thinkdifferent.convertpreview.service.ConvertService;
import com.thinkdifferent.convertpreview.service.RabbitMQService;
import com.thinkdifferent.convertpreview.utils.*;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.bouncycastle.crypto.CryptoException;
import org.jetbrains.annotations.NotNull;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.tool.merge.OFDMerger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ConvertServiceImpl implements ConvertService {

    @Autowired(required = false)
    private ConvertUtil convertUtil;
    @Resource
    private RabbitMQService rabbitMQService;

    /**
     * 检测传入的对象是否正确, 不正确抛出异常，可通过异常信息获取失败原因
     *
     * @param jsonInput 传入的参数
     */
    @SneakyThrows
    @Override
    public void checkParams(JSONObject jsonInput) {
        // 参数转换
        ConvertEntity convertEntity = ConvertEntity.of(jsonInput);

        if (convertEntity == null) {
            throw new InterruptedException("参数错误，请检查参数后重新传输。可参看日志中的信息。");
        } else if (convertEntity.getInputFiles() == null || convertEntity.getInputFiles().length == 0) {
            // 判断文件是否存在
            throw new InterruptedException("文件不存在");
        } else {
            if (!"path".equalsIgnoreCase(convertEntity.getInputType().toString())) {
                Input[] inputs = convertEntity.getInputFiles();
                for (Input input : inputs) {
                    FileUtil.del(input.getInputFile());
                }
            }
        }
    }

    /**
     * 异步处理转换, 已经过参数校验
     *
     * @param parameters 输入的参数，JSON格式数据对象
     */
    @Async
    @Override
    public void asyncConvert(Map<String, Object> parameters) {
        CallBackResult callBackResult = convert(parameters);
        if (callBackResult.isFlag()) {
            // 成功，清理失败记录
            SystemConstants.removeErrorData((JSONObject) parameters);
        } else {
            // MQ 重试，不启用 MQ 无法重试
            rabbitMQService.setRetryData2MQ((JSONObject) parameters);
        }
    }

    /**
     * V3 版本
     * 将传入的JSON对象中记录的文件，转换为JPG，输出到指定的目录中；回调应用系统接口，将数据写回。
     *
     * @param parameters 输入的参数，JSON格式数据对象
     */
    @SneakyThrows
    @Override
    public CallBackResult convert(Map<String, Object> parameters) {
        // 开始时间
        long stime = System.currentTimeMillis();

        // 转换结果
        WriteBackResult writeBackResult = new WriteBackResult(false);
        // 参数转换
        ConvertEntity convertEntity = ConvertEntity.of(parameters);
        // 合并后的文件路径及文件名，无后缀
        String strDestPathFileName = convertEntity.getWriteBack().getOutputPath() + convertEntity.getOutPutFileName();

        // 中间结果的jpg图片
        List<String> listJpg = new ArrayList<>();

        // 1. 获取输入文件、格式转换、多文件合并
        File fileOut = mergerInputFile(convertEntity, strDestPathFileName);

        if(fileOut !=null && fileOut.exists()){
            // 2. 加水印、归档章、页标等
            String strOutFile = markFile(convertEntity, strDestPathFileName, fileOut);
            // 2.1 对输出文件加密
            outFileEncryptor(convertEntity, strOutFile);

            fileOut = new File(strOutFile);
            // 3. 回写
            // 3.1 验证转换后文件, 成功后回写
            boolean blnResult = checkOutFile(fileOut, convertEntity);
            if (blnResult) {
                writeBackResult = WriteBackUtil.writeBack(convertEntity.getWriteBack(), convertEntity.getOutPutFileType(),
                        fileOut, listJpg);
            }
        }

        // 4. 回调
        CallBackResult callBackResult = callBack(writeBackResult, convertEntity, listJpg, fileOut);
        // 5. 清理临时文件
        cleanTempFile(convertEntity.getWriteBackType().name(),
                convertEntity.getInputType().name(), convertEntity.getInputFiles(),
                listJpg, fileOut);

        // 结束时间
        long etime = System.currentTimeMillis();
        // 计算执行时间
        log.info("Convert Finish, Use time Total: " + (int) ((etime - stime) / 1000) + " s...");

        return callBackResult;
    }

    /**
     * 对输出的文件（PDF、OFD）加密，控制使用权限。
     *
     * @param convertEntity 输入的转换对象
     * @param strOutFile    输出文件的路径和文件名
     * @throws IOException
     * @throws CryptoException
     * @throws GeneralSecurityException
     */
    private void outFileEncryptor(ConvertEntity convertEntity, String strOutFile)
            throws IOException {
        if (convertEntity.getOutFileEncryptorEntity() != null
                && convertEntity.getOutFileEncryptorEntity().getEncry()) {
            ConvertUtil convertUtil = new ConvertUtil();

            if ("pdf".equalsIgnoreCase(convertEntity.getOutPutFileType())) {
                convertUtil.pdfEncry(strOutFile, convertEntity.getOutFileEncryptorEntity());
            } else {
                // todo OFD加密、权限设置，暂未调试成功
//                convertUtil.ofdEncry(strOutFile, convertEntity.getOutFileEncryptorEntity());
            }
        }

    }

    /**
     * 格式转换处理（包括文件合并）
     *
     * @param convertEntity       传入参数
     * @param strDestPathFileName 设置合并后的PDF文件的路径和文件名
     * @return 出入文件，已合并
     * @throws IOException err
     */
    private File mergerInputFile(ConvertEntity convertEntity, String strDestPathFileName) throws Exception {
        Input[] inputs = convertEntity.getInputFiles();
        List<File> inputFiles = new ArrayList<>();
        List<String> tempFiles = new ArrayList<>();

        try {
            // 单文件转图片特殊处理
            if (inputs.length == 1) {
                File fileInput = inputs[0].checkAndGetInputFile();
                String strInputFileType = FileTypeUtil.getFileType(fileInput);
                // 单文件转图片
                if (!StringUtils.equalsAnyIgnoreCase(strInputFileType, "pdf", "ofd")
                        && "jpg".equalsIgnoreCase(convertEntity.getOutPutFileType())) {
                    // 将传入的图片文件转换为jpg文件，存放到输出路径中
                    List<String> listJpg = convertUtil.convertPic2Jpg(fileInput.getCanonicalPath(),
                            strDestPathFileName + ".jpg");

                    // 如果生成缩略图【首页缩略图】，则不执行后续水印等操作。
                    if (convertEntity.getThumbnail() != null) {
                        return convertUtil.getThumbnail(convertEntity, listJpg);
                    } else {
                        // 图片添加水印
                        JpgWaterMarkUtil.mark4JpgList(listJpg, convertEntity);
                        return null;
                    }
                }
            }

            // 文件格式转换
            String strPicType = ConvertConfig.picType;
            log.info("picType:{}", strPicType);
            String[] picTypes = strPicType.split(",");

            parseInputFile(convertEntity, strDestPathFileName, inputFiles, tempFiles,
                    picTypes);
            if (inputFiles.size() == 1) {
                // 传入单文件，返回
                return new File(strDestPathFileName + "." + convertEntity.getOutPutFileType());
            } else if (inputFiles.size() > 1) {
                // 文件合并
                if (StringUtils.equalsIgnoreCase(convertEntity.getOutPutFileType(), "pdf")) {
                    PDFMergerUtility pdfMerger = new PDFMergerUtility();
                    // 设置合并后的PDF文件的路径和文件名
                    pdfMerger.setDestinationFileName(strDestPathFileName + ".pdf");
                    for (File file : inputFiles) {
                        pdfMerger.addSource(file);
                        tempFiles.add(file.getAbsolutePath());
                    }
                    pdfMerger.mergeDocuments(null);
                    return new File(pdfMerger.getDestinationFileName());
                } else if (StringUtils.equalsIgnoreCase(convertEntity.getOutPutFileType(), "ofd")) {
                    File ofdNoMarkFile = new File(strDestPathFileName + ".ofd");
                    try (OFDMerger ofdMerger = new OFDMerger(ofdNoMarkFile.toPath())) {
                        for (File file : inputFiles) {
                            ofdMerger.add(file.toPath());
                            tempFiles.add(file.getAbsolutePath());
                        }
                    }
                    return ofdNoMarkFile;
                } else {
                    throw new InvalidParameterException("未处理的异常情况");
                }
            }
        } catch (Exception e) {
            log.error("获取输入文件异常", e);
            throw e;
        } finally {
            // 清理临时文件
            for (String tempFile : tempFiles) {
                FileUtil.del(tempFile);
            }
            // 清理下载文件, 格式转换后的文件, =1时需要返回，在回调完成后清理
            if (!"path".equalsIgnoreCase(convertEntity.getInputType().name())
                    && inputFiles.size() > 1) {
                for (File file : inputFiles) {
                    FileUtil.del(file);
                }
            }
        }

        return null;
    }


    /**
     * 将传入文件转换成输出格式
     *
     * @param convertEntity       传入参数
     * @param strDestPathFileName 目标文件路径及文件名，不含后缀
     * @param inputFiles          转换格式后的文件
     * @param tempFiles           临时的图片文件
     * @throws IOException         err
     * @throws DocumentException   err
     */
    private void parseInputFile(
            ConvertEntity convertEntity,
            String strDestPathFileName,
            List<File> inputFiles,
            List<String> tempFiles,
            String[] strsPicType)
            throws IOException, DocumentException {
        // 多文件处理或转单文件转pdf\ofd
        for (int i = 0; i < convertEntity.getInputFiles().length; i++) {
            File fileInput = convertEntity.getInputFiles()[i].checkAndGetInputFile();
            String strInputFileType = FileTypeUtil.getFileType(fileInput);
            File tPdfFile;
            List<String> tempJpgs;

            String strDestFile = convertEntity.getInputFiles().length == 1
                    ? strDestPathFileName : strDestPathFileName + "_" + i;

            // 1、各种图片转jpg、pdf
            if (StringUtils.equalsAnyIgnoreCase(strInputFileType, strsPicType)) {
                tempJpgs = convertUtil.convertPic2Jpg(fileInput.getCanonicalPath(), strDestFile + ".jpg");
                tempFiles.addAll(tempJpgs);
                // 1.1、如果目标格式就是jpg，则pdf、ofd转换跳过。下一循环。
                if (StringUtils.equalsIgnoreCase(convertEntity.getOutPutFileType(), "jpg")) {
                    continue;
                } else {
                    // 1.2、jpg文件转pdf（如果输入文件是pdf，不处理）
                    tPdfFile = convertUtil.convertPic2Pdf(fileInput.getCanonicalPath(),
                            "jpg", tempJpgs, convertEntity);
                    // 1.3、如果输出格式为pdf，则ofd转换跳过。下一循环。
                    if (StringUtils.equalsIgnoreCase(convertEntity.getOutPutFileType(), "pdf")) {
                        inputFiles.add(i, tPdfFile);
                        continue;
                    }
                }
            } else {
                // 2、各种非图片文件转pdf
                tPdfFile = convertUtil.convertOffice2Pdf(
                        fileInput.getCanonicalPath(),
                        strDestFile + ".pdf",
                        convertEntity
                );
                if (tPdfFile != null && tPdfFile.exists()
                        && StringUtils.equalsIgnoreCase(convertEntity.getOutPutFileType(), "pdf")) {
                    inputFiles.add(i, tPdfFile);
                    continue;
                }

            }

            // 3、PDF文件转ofd（如果输入文件是ofd，不处理）
            if (!StringUtils.equalsIgnoreCase(strInputFileType, "ofd")
                    && StringUtils.equalsIgnoreCase(convertEntity.getOutPutFileType(), "ofd")) {

                if (tPdfFile != null && tPdfFile.exists()){
                    File ofdFile = new File(strDestFile + ".ofd");
                    convertUtil.convertPdf2Ofd((tPdfFile == null ? fileInput : tPdfFile).getPath(), ofdFile.getPath());
                    inputFiles.add(i, ofdFile);

                    // 3.1、PDF文件转的，移除临时文件
                    FileUtil.del(tPdfFile);
                    continue;
                }
            }

            // 4、传入pdf，传出pdf | 传入ofd, 传出ofd,
            if(fileInput != null && fileInput.exists()){
                if (StringUtils.equalsAnyIgnoreCase(strInputFileType, "pdf", "ofd")){
                    File copyFile = new File(strDestFile + "." + convertEntity.getOutPutFileType().toLowerCase());
                    if ((convertEntity.getInputFiles()[i] instanceof InputPath)
                            && !copyFile.exists()) {
                        // 4.1、如果是本地文件，复制到输入目录
                        FileUtil.copy(fileInput, copyFile, true);
                        inputFiles.add(i, copyFile);
                    } else {
                        inputFiles.add(i, fileInput);
                    }
                }
            }

        }
    }


    /**
     * 给文件添加水印
     *
     * @param convertEntity
     * @param strDestPathFileName
     * @param fileOutNoMark
     * @return
     * @throws Exception
     */
    private String markFile(ConvertEntity convertEntity, String strDestPathFileName, File fileOutNoMark) throws Exception {
        String strOutPath = null;
        File fileOutMark = null;
        if (Objects.nonNull(fileOutNoMark)) {
            if (StringUtils.equalsIgnoreCase(convertEntity.getOutPutFileType(), "pdf")) {
                strOutPath = strDestPathFileName + "_wm.pdf";
                PdfWaterMarkUtil.mark4Pdf(fileOutNoMark.getAbsolutePath(), strOutPath, convertEntity, 0);
            } else if (StringUtils.equalsIgnoreCase(convertEntity.getOutPutFileType(), "ofd")) {
                strOutPath = strDestPathFileName + "_wm.ofd";
                OfdWaterMarkUtil.mark4Ofd(fileOutNoMark.getAbsolutePath(), strOutPath, convertEntity);
            } else {
                // 缩略图路径
                strOutPath = fileOutNoMark.getAbsolutePath();
            }
            fileOutMark = new File(strOutPath);
        }

        if (fileOutNoMark != null
                && !fileOutMark.equals(fileOutNoMark)) {
            FileUtil.del(fileOutNoMark);
        }

        String strOut;
        if (strOutPath != null
                && !fileOutMark.equals(fileOutNoMark)) {
            Path pathOut = FileUtil.rename(Paths.get(strOutPath), fileOutNoMark.getName(), true);
            strOut = pathOut.toString();
        } else {
            strOut = strOutPath;
        }

        return strOut;
    }

    /**
     * 回调
     *
     * @param writeBackResult 回写结果
     * @param convertEntity   转换后对象
     * @param listJpg         中间临时图片
     * @param fileOut         转换后文件
     * @return 回调结果
     */
    @NotNull
    private CallBackResult callBack(WriteBackResult writeBackResult, ConvertEntity convertEntity, List<String> listJpg, File fileOut) {
        if (writeBackResult.isFlag()) {
            writeBackResult.setFile(Objects.isNull(fileOut) ?
                    Objects.requireNonNull(listJpg).stream().map(f -> new File(f).getName()).collect(Collectors.joining(",")) :
                    fileOut.getName());
            log.info("文件[" + convertEntity.getInputFileName() + "]回写成功");
            writeBackResult.setFlag(true).setMessage("回写结果:" + writeBackResult);
        } else {
            log.info("文件[" + convertEntity.getInputFileName() + "]回写失败");
            writeBackResult.setFlag(false).setMessage("回写结果:" + writeBackResult);
        }

        // 回调
        log.info("开始回调：url={}", convertEntity.getCallBackURL());
        return callBack(convertEntity.getCallBackURL(), convertEntity.getCallBackHeaders()
                , writeBackResult, convertEntity.getOutPutFileName());
    }

    /**
     * 检查输出的文件格式是否正确
     *
     * @param fileOut
     * @param convertEntity
     * @return
     */
    private boolean checkOutFile(File fileOut, ConvertEntity convertEntity) {
        if (fileOut != null && fileOut.exists()) {
            try {
                String lowerFileName = fileOut.getName().toLowerCase();
                if (lowerFileName.endsWith(".pdf")) {
                    if (convertEntity.getOutFileEncryptorEntity() != null
                            && convertEntity.getOutFileEncryptorEntity().getEncry()) {
                        try (PDDocument doc = Loader.loadPDF(fileOut, convertEntity.getOutFileEncryptorEntity().getUserPassWord())) {
                            log.info("转换PDF完成，共{}页", doc.getPages().getCount());
                        }
                    } else {
                        try (PDDocument doc = Loader.loadPDF(fileOut)) {
                            log.info("转换PDF完成，共{}页", doc.getPages().getCount());
                        }
                    }

                } else if (lowerFileName.endsWith("ofd")) {
                    try (OFDReader ofdReader = new OFDReader(Paths.get(fileOut.getCanonicalPath()))) {
                        log.info("转换OFD完成，共{}页", ofdReader.getPageList().size());
                    }
                }
            } catch (Exception e) {
                log.error("回写检查异常", e);
                return false;
            }
        }
        return true;
    }


    /**
     * 清理临时文件
     *
     * @param writeBackTypeName 回写类型
     * @param inputTypeName     输入类型
     * @param inputFiles        输入文件（URL、FTP输入时，是临时文件）
     * @param listJpg           jpg文件list
     * @param fileOut           转换后文件
     */
    private void cleanTempFile(String writeBackTypeName,
                               String inputTypeName, Input[] inputFiles,
                               List<String> listJpg, File fileOut) {
        if (!"path".equalsIgnoreCase(writeBackTypeName)) {
            for (String strJpg : listJpg) {
                FileUtil.del(strJpg);
            }
            FileUtil.del(fileOut);
        }

        if (!"path".equalsIgnoreCase(inputTypeName)) {
            for (Input inputFile : inputFiles) {
                inputFile.clean();
            }
        }
    }

    /**
     * 回调业务系统提供的接口
     *
     * @param strCallBackURL      回调接口URL
     * @param mapWriteBackHeaders 请求头参数
     * @param writeBackResult     参数
     * @param outPutFileName      文件名
     * @return JSON格式的返回结果
     */
    private static CallBackResult callBack(String strCallBackURL, Map<String, String> mapWriteBackHeaders,
                                           WriteBackResult writeBackResult, String
                                                   outPutFileName) {
        log.info("回调文件{}, url:{}, header:【{}】, params:【{}】 ", outPutFileName, strCallBackURL,
                StringUtils.join(mapWriteBackHeaders), StringUtils.join(writeBackResult));
        if (StringUtils.isBlank(strCallBackURL)) {
            log.info("回调{}地址为空，跳过", outPutFileName);
            return new CallBackResult(true, "回调地址为空，跳过");
        }

        //发送get请求并接收响应数据
        try (HttpResponse httpResponse = HttpUtil.createGet(strCallBackURL)
                .addHeaders(mapWriteBackHeaders).form(writeBackResult.bean2Map())
                .execute()) {
            String body = httpResponse.body();
            log.info("回调请求地址:{}, 请求体:{},状态码：{}，结果：{}", strCallBackURL, writeBackResult, httpResponse.isOk(), body);

            if (httpResponse.isOk() && writeBackResult.isFlag()) {
                // 回调成功且转换成功，任务才会结束
                return new CallBackResult(true, "Convert Callback Success.\n" +
                        "Message is :\n" +
                        body);
            } else {
                return new CallBackResult(false, "CallBack error, resp: " + body + ", writeBackResult=" + writeBackResult);
            }
        }
    }

    /**
     * 文件预览
     *
     * @param input 输入文件
     * @return 转换后的pdf文件
     */
    @Override
    public File filePreview(Input input) throws Exception {
        if (input.exists() && input.getInputFile().getName().endsWith(".pdf")) {
            return input.getInputFile();
        }

        ConvertEntity convertEntity = new ConvertEntity();
        convertEntity.setOutPutFileType("pdf");
        Input[] inputs = {input};
        convertEntity.setInputFiles(inputs);
        // 定时任务清理预览文件
        convertEntity.setInputType(InputType.PATH);

        // 合并后的文件路径及文件名，无后缀
        String strDestPathFileName = ConvertConfig.outPutPath + DateUtil.today() + "/" + input.getInputFile().getName();
        if (FileUtil.exist(strDestPathFileName + ".pdf")){
            // 今天已经转换过的，直接返回
            return new File(strDestPathFileName + ".pdf");
        }
        return mergerInputFile(convertEntity, strDestPathFileName);
    }
}