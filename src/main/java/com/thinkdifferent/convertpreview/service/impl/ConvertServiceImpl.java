package com.thinkdifferent.convertpreview.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.thinkdifferent.convertpreview.config.ConvertConfig;
import com.thinkdifferent.convertpreview.config.SystemConstants;
import com.thinkdifferent.convertpreview.entity.CallBackResult;
import com.thinkdifferent.convertpreview.entity.ConvertEntity;
import com.thinkdifferent.convertpreview.entity.InputType;
import com.thinkdifferent.convertpreview.entity.WriteBackResult;
import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.service.ConvertService;
import com.thinkdifferent.convertpreview.service.RabbitMQService;
import com.thinkdifferent.convertpreview.utils.*;
import com.thinkdifferent.convertpreview.utils.convert4jpg.ConvertJpgEnum;
import com.thinkdifferent.convertpreview.utils.convert4jpg.JpgUtil;
import com.thinkdifferent.convertpreview.utils.convert4ofd.ConvertOfdUtil;
import com.thinkdifferent.convertpreview.utils.convert4pdf.ConvertPdfEnum;
import com.thinkdifferent.convertpreview.utils.convert4pdf.ConvertPdfUtil;
import com.thinkdifferent.convertpreview.utils.watermark.JpgWaterMarkUtil;
import com.thinkdifferent.convertpreview.utils.watermark.OfdWaterMarkUtil;
import com.thinkdifferent.convertpreview.utils.watermark.PdfWaterMarkUtil;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.bouncycastle.crypto.CryptoException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.tool.merge.OFDMerger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ConvertServiceImpl implements ConvertService {

    @Autowired(required = false)
    private JpgUtil jpgUtil;
    @Autowired(required = false)
    private ConvertPdfUtil convertPdfUtil;
    @Autowired(required = false)
    private ConvertOfdUtil convertOfdUtil;
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
        CallBackResult callBackResult = convert(parameters, "convert", null);
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
     * @param type       调用类型：convert，转换；preview，预览；base64，需要返回base64；stream，将文件信息返回Http响应头。
     * @param response   Http响应对象。
     */
    @SneakyThrows
    @Override
    public CallBackResult convert(Map<String, Object> parameters, String type, HttpServletResponse response) {
        // 开始时间
        long stime = System.currentTimeMillis();

        File fileOutputDir = new File(ConvertConfig.outPutPath);
        if (!fileOutputDir.exists()) {
            fileOutputDir.mkdirs();
        }

        // 转换结果
        WriteBackResult writeBackResult = new WriteBackResult(false);
        // 参数转换
        ConvertEntity convertEntity = ConvertEntity.of(parameters);
        // 合并后的文件路径及文件名，无后缀
        String strDestPathFileName = convertEntity.getWriteBack().getOutputPath() + convertEntity.getOutPutFileName();

        // 中间结果的jpg图片
        List<String> listJpg = new ArrayList<>();

        // 1. 获取输入文件、格式转换、多文件合并
        File fileOut = convertAndMerge(convertEntity, strDestPathFileName, type);

        if (fileOut != null && fileOut.exists()) {
            // 2. 加水印、归档章、页标等
            markFile(convertEntity, strDestPathFileName, fileOut);
            // 2.1 对输出文件加密
            outFileEncryptor(convertEntity, fileOut.getAbsolutePath());

            // 3. 回写
            // 3.1 验证转换后文件, 成功后回写
            boolean blnResult = checkOutFile(fileOut, convertEntity);
            if (blnResult) {
                writeBackResult = WriteBackUtil.writeBack(convertEntity.getWriteBack(), convertEntity.getOutPutFileType(),
                        fileOut, listJpg);
            }
        }

        // 判断是转换还是base64。如果是base64，则返回base64值，不回调；如果是转换，则执行回调。
        CallBackResult callBackResult = new CallBackResult();
        // 获取回写类型
        String strWriteBackType = convertEntity.getWriteBackType().name();
        if (fileOut != null && fileOut.exists() && fileOut.length() > 0) {
            if ("base64".equalsIgnoreCase(type)) {
                byte[] b = Files.readAllBytes(Paths.get(fileOut.getAbsolutePath()));
                // 文件转换为字节后，转换后的文件即可删除（文件没用了）。
                callBackResult.setFlag(true);
                callBackResult.setBase64(Base64.getEncoder().encodeToString(b));

                strWriteBackType = "base64";

            } else if ("stream".equalsIgnoreCase(type)) {
                callBackResult.setFlag(true);

                response.setCharacterEncoding("UTF-8");

                @Cleanup InputStream is = new BufferedInputStream(new FileInputStream(fileOut));
                byte[] bytes = new byte[1024];
                //设置响应头参数
                response.addHeader("Content-Disposition", "inline;filename=" + fileOut.getName());
                response.setContentType("application/octet-stream");
                //响应输出流输出
                OutputStream os = new BufferedOutputStream(response.getOutputStream());
                while (is.read(bytes) != -1) {
                    os.write(bytes);
                }
                IOUtils.closeQuietly(os);
                IOUtils.closeQuietly(is);

                callBackResult.setResponse(response);

                strWriteBackType = "stream";

            } else if ("convert".equalsIgnoreCase(type)) {
                // 4. 回调
                callBackResult = writeBack(writeBackResult, convertEntity, listJpg, fileOut);
            }
        }

        // 5. 清理临时文件
        cleanTempFile(strWriteBackType,
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
            throws IOException, CryptoException, GeneralSecurityException, ParseException {
        if (convertEntity.getOutFileEncryptorEntity() != null
                && convertEntity.getOutFileEncryptorEntity().getEncry()) {

            if ("pdf".equalsIgnoreCase(convertEntity.getOutPutFileType())) {
                convertPdfUtil.pdfEncry(strOutFile, convertEntity.getOutFileEncryptorEntity());
            } else {
                convertOfdUtil.ofdEncry(strOutFile, convertEntity.getOutFileEncryptorEntity());
            }
        }

    }

    /**
     * 格式转换处理（包括文件合并）
     *
     * @param convertEntity       传入参数
     * @param strDestPathFileName 设置合并后的PDF文件的路径和文件名
     * @param type                转换类型，"PREVIEW"
     * @return 输出的PDF或OFD文件，已合并
     * @throws IOException err
     */
    private File convertAndMerge(ConvertEntity convertEntity, String strDestPathFileName, String type) throws Exception {
        // 输入文件对象数组
        Input[] inputs = convertEntity.getInputFiles();
        // 转换后的目标文件File对象List
        List<File> targetFiles = new ArrayList<>();
        // 临时文件位置字符串List（包括：url、ftp方式接收的文件（input文件夹中）；各类文件转换ofd过程中生成的pdf文件；pdf、ofd合并时，待合并的文件。)
        List<String> tempFiles = new ArrayList<>();

        try {
            // 获取配置文件中设置的，本服务支持的图片文件扩展名
            String strPicType = ConvertConfig.picType;
            // 图片文件类型转换为数组
            String[] strsPicType = strPicType.split(",");

            // 单个图片文件转换JPG处理（单独处理）
            // 如果输入的文件只有一个，则进行直接转换操作。
            if (inputs.length == 1) {
                // 获取输入文件的File对象
                File fileInput = inputs[0].checkAndGetInputFile();
                if (fileInput.length() == 0) {
                    return null;
                }
                // 获取输入文件的类型（扩展名）
                String strInputFileType = FileTypeUtil.getFileType(fileInput);
                // 如果输入文件的格式在配置的可转换格式列表中，并且，输出格式为“jpg”，则执行图片转JPG功能。
                if (StringUtils.equalsAnyIgnoreCase(strInputFileType, strsPicType)
                        && "jpg".equalsIgnoreCase(convertEntity.getOutPutFileType())) {
                    // 将传入的图片文件转换为jpg文件，存放到输出路径中。返回转换后的JPG文件的路径字符串List。
                    List<String> listJpg = ConvertJpgEnum.convert(fileInput.getCanonicalPath(),
                            strDestPathFileName + ".jpg");

                    // 如果需要生成【首页缩略图】，则只执行“缩略图”操作，不执行后续水印等操作。
                    if (convertEntity.getThumbnail() != null) {
                        // 执行缩略图操作，并返回缩略图文件File对象。（不执行后续操作）
                        return jpgUtil.getThumbnail(convertEntity, listJpg);
                    } else {
                        // 图片添加水印（方法中自动根据传入参数，判断是否添加水印）
                        JpgWaterMarkUtil.mark4JpgList(listJpg, convertEntity);
                        // 返回转换后的JPG文件（临时文件）
                        return new File(strDestPathFileName + ".jpg");
                    }
                }
            }

            // 如果输入的文件有多个，或者输入的文件格式不是JPG，则执行文件格式转换操作。
            convertFileFormat(convertEntity, strDestPathFileName, targetFiles, tempFiles,
                    strsPicType);
            // 如果转换后的目标文件只有一个，则直接返回目标文件对象。
            if (targetFiles.size() == 1) {
                File outFile = targetFiles.get(0);
                // 双层PDF 目前只支持处理单个文件
                if ("pdf".equalsIgnoreCase(convertEntity.getOutPutFileType())) {
                    if (FileUtil.exist(outFile)) {
                        // 双层PDF
                        if (!CollectionUtils.isEmpty(convertEntity.getContexts())) {
                            // 双层PDF修改
                            DoubleLayerPdfUtil.addText(outFile, convertEntity.getContexts());
                        }
                    }
                }
                return outFile;
            } else if (targetFiles.size() > 1) {
                // 如果转换后的目标文件有多个，则需要执行“文件合并”

                if ("pdf".equalsIgnoreCase(convertEntity.getOutPutFileType())) {
                    // 如果输出文件格式为PDF，则执行PDF合并。
                    // 声明PDF合并工具对象
                    PDFMergerUtility pdfMerger = new PDFMergerUtility();
                    // 设置合并后的PDF文件的路径和文件名
                    pdfMerger.setDestinationFileName(strDestPathFileName + ".pdf");
                    // 循环，处理“目标文件列表”中的所有文件，进行合并
                    for (File file : targetFiles) {
                        if (file.length() > 0) {
                            // 将需合并的文件加入“PDF合并工具”对象
                            pdfMerger.addSource(file);
                        }
                        // 将当前处理的待合并文件路径信息，加入到“临时文件列表”中（后续自动删除）
                        tempFiles.add(file.getAbsolutePath());
                    }
                    // 所有待合并文件处理完毕后，执行“合并”动作。
                    pdfMerger.mergeDocuments(null);
                    // 返回合并后的PDF文件对象
                    return new File(pdfMerger.getDestinationFileName());

                } else if ("ofd".equalsIgnoreCase(convertEntity.getOutPutFileType())) {
                    // 如果输出文件格式为OFD，则执行OFD合并。
                    File fileOfd = new File(strDestPathFileName + ".ofd");
                    // 声明OFD合并对象
                    try (OFDMerger ofdMerger = new OFDMerger(fileOfd.toPath())) {
                        // 循环，处理“目标文件列表”中的所有文件，进行合并
                        for (File file : targetFiles) {
                            if (file.length() > 0) {
                                // 将需合并的文件加入“OFD合并对象”
                                ofdMerger.add(file.toPath());
                            }
                            // 将当前处理的待合并文件路径信息，加入到“临时文件列表”中（后续自动删除）
                            tempFiles.add(file.getAbsolutePath());
                        }
                    }
                    // 返回合并后的OFD文件对象
                    return fileOfd;
                } else {
                    // 如果输出的目标格式不是PDF或OFD，则抛出参数异常信息
                    throw new InvalidParameterException("需合并的目标格式暂不支持");
                }
            }
        } catch (Exception e) {
            log.error("获取输入文件异常", e);
            throw e;
        } finally {
            // 预览状态不清理临时文件
            if (!"PREVIEW".equalsIgnoreCase(type)) {
                // 清理临时文件
                // （包括：url、ftp方式接收的文件（input文件夹中）；
                // 各类文件转换ofd过程中生成的pdf文件；pdf、ofd合并时，待合并的文件。)
                for (String tempFile : tempFiles) {
                    FileUtil.del(tempFile);
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
     * @param targetFiles         目标文件File对象List
     * @param tempFiles           临时文件字符串List（包括：url、ftp方式接收的文件（input文件夹中）；各类文件转换ofd过程中生成的pdf文件；pdf、ofd合并时，待合并的文件。
     * @throws Exception err
     */
    private void convertFileFormat(
            ConvertEntity convertEntity,
            String strDestPathFileName,
            List<File> targetFiles,
            List<String> tempFiles,
            String[] strsPicType)
            throws Exception {

        // 多文件处理或转单文件转pdf\ofd
        for (int i = 0; i < convertEntity.getInputFiles().length; i++) {
            // 从输入对象中获取文件（数组中获取每个文件）
            File fileInput = convertEntity.getInputFiles()[i].checkAndGetInputFile();
            if (fileInput.length() == 0) {
                // 如果文件大小为0，则为空文件，不进行转换；进行下一循环。
                continue;
            }
            // 判断文件的类型（扩展名）
            String strInputFileType = FileTypeUtil.getFileType(fileInput);

            // 组装要生成的目标文件的路径和文件名（不含扩展名，后续判断目标格式后加入）（此处文件名带有循环变量。如果处理的只是一个文件，则不加入循环变量）
            String strDestFile = convertEntity.getInputFiles().length == 1
                    ? strDestPathFileName : strDestPathFileName + "_" + i;

            // 输入的格式不等于输出格式，则进行转换
            if (!strInputFileType.equalsIgnoreCase(convertEntity.getOutPutFileType())) {
                // 转换后生成的pdf文件File对象
                File filePdf = null;
                // 其他格式图片文件，转换后生成的JPG文件路径字符串List
                List<String> listTempJpg = new ArrayList<>();

                // 如果输入的文件格式是图片格式（配置文件中设置格式列表），则执行图片转jpg、pdf操作
                if (StringUtils.equalsAnyIgnoreCase(strInputFileType, strsPicType)) {
                    // 将图片文件转换为JPG格式。返回文件路径字符串List
                    if (!"jpg".equalsIgnoreCase(strInputFileType)) {
                        listTempJpg = ConvertJpgEnum.convert(fileInput.getAbsolutePath(), strDestFile + ".jpg");
                    } else {
                        listTempJpg.add(fileInput.getAbsolutePath());
                    }

                    // 如果目标格式就是jpg，则跳过后续处理（pdf、ofd转换），执行下一循环。
                    if ("jpg".equalsIgnoreCase(convertEntity.getOutPutFileType())) {
                        // 将原图片文件加入到“目标文件列表”
                        targetFiles.add(fileInput);
                        // 跳过后续操作，执行下一循环。
                        continue;
                    } else {
                        if (listTempJpg != null) {
                            // 将JPG文件路径字符串List加入到“临时文件列表”
                            tempFiles.addAll(listTempJpg);
                            // jpg文件转pdf（自动完成JPG文件列表中所有文件的转换、合并）
                            filePdf = ConvertPdfEnum.convert(
                                    "jpg",
                                    listTempJpg,
                                    strDestFile + ".pdf",
                                    convertEntity);
                            // 如果输出格式为pdf，则跳过ofd转换。执行下一循环。
                            if ("pdf".equalsIgnoreCase(convertEntity.getOutPutFileType())
                                    && filePdf.exists()) {
                                // 将转换好的PDF文件加入到“目标文件列表”
                                targetFiles.add(filePdf);
                                // 跳过后续操作，执行下一循环。
                                continue;
                            }
                        }
                    }
                } else if (!"ofd".equalsIgnoreCase(strInputFileType)) {
                    // 如果输入的文件格式不是“ofd”（前面已经判断了、处理了图片格式，此时剩余的格式均为Office相关格式），则可以执行转PDF操作。
                    // 将传入的文档文件转换为PDF格式，保存到本地的“输出文件夹”中，并按照传入参数为文件命名。
                    if (!"pdf".equalsIgnoreCase(strInputFileType)) {
                        filePdf = ConvertPdfEnum.convert(
                                getConvertEngine(),
                                fileInput.getAbsolutePath(),
                                strDestFile + ".pdf",
                                convertEntity
                        );
                    } else {
                        // 组装目标文件名、扩展名，并创建File对象。
                        filePdf = new File(strDestFile + ".pdf");
                        // 将输入文件复制到目标文件夹
                        FileUtil.copy(fileInput, filePdf, true);
                    }

                    // 如果PDF转换成功（目标文件夹的PDF存在），则将文件信息加入“目标文件列表”，并跳过后续操作。
                    if (filePdf != null && filePdf.exists()
                            && "pdf".equalsIgnoreCase(convertEntity.getOutPutFileType())) {
                        // 将转换好的PDF文件加入到“目标文件列表”
                        targetFiles.add(filePdf);
                        // 跳过后续操作，执行下一循环。
                        continue;
                    }

                } else if ("ofd".equalsIgnoreCase(strInputFileType)
                        && "pdf".equalsIgnoreCase(convertEntity.getOutPutFileType())) {
                    // 如果文件输入格式为OFD，并且输出格式为PDF，则执行：OFD转PDF操作。
                    // todo 图片水印会跑版。暂时无法解决。
                    File fileOfd = convertOfdUtil.convertOfd2Pdf(fileInput.getAbsolutePath(), strDestFile + ".pdf");
                    // 将转换好的OFD文件加入到“目标文件列表”
                    targetFiles.add(fileOfd);
                    // 跳过后续操作，执行下一循环。
                    continue;
                }

                // 如果输入的文件类型不是OFD，并且，输出文件类型是OFD，则执行PDF文件转OFD（前面的过程，已经把文件处理成了PDF）
                if (!"ofd".equalsIgnoreCase(strInputFileType)
                        && "ofd".equalsIgnoreCase(convertEntity.getOutPutFileType())) {
                    // 如果PDF文件存在，则将文件转换为OFD
                    if (filePdf != null && filePdf.exists()) {
                        // 声明OFD文件的File对象
                        File ofdFile = new File(strDestFile + ".ofd");
                        // 获取PDF文件的路径
                        String strPdfFilePath = (filePdf == null ? fileInput : filePdf).getPath();
                        // 将PDF文件转换为OFD
                        convertOfdUtil.convertPdf2Ofd(strPdfFilePath, ofdFile.getPath());
                        // 将转换后的OFD文件加入“目标文件列表”
                        targetFiles.add(ofdFile);
                        // 将输入的PDF文件（过程文件）加入“临时文件列表”，后续自动删除
                        tempFiles.add(strPdfFilePath);

                        continue;
                    }
                }

            } else {
                // 如果输入的格式等于输出的格式，则不需要转换，直接进行文件复制。
                // 将输入文件复制到目标文件夹（path回写路径，或本地temp文件夹）

                // 如果输入文件存在，则进行文件复制操作。
                if (fileInput != null && fileInput.exists()) {
                    // 组装目标文件名、扩展名，并创建File对象。
                    File destFile = new File(strDestFile + "." + convertEntity.getOutPutFileType().toLowerCase());
                    // 将输入文件复制到目标文件夹
                    FileUtil.copy(fileInput, destFile, true);
                    // 将转换后的OFD文件加入“目标文件列表”
                    targetFiles.add(destFile);
                    // 如果输入方式不是“path”（本地路径），则将数据文件加入到“临时文件列表”中，等待后续删除。
                    // （本地路径输入的文件不能删除，是原始文件；url或ftp下载的文件属于临时文件，可以在复制后删除）
                    if (!"path".equalsIgnoreCase(convertEntity.getInputType().name())) {
                        tempFiles.add(fileInput.getAbsolutePath());
                    }
                }

            }
        }
    }

    /**
     * 获取当前配置的PDF转换引擎的名称，用于后续判断
     *
     * @return 转换引擎名称
     */
    private String getConvertEngine() {
        if (ConvertConfig.wpsEnabled) {
            return "WPS";
        }
        if (ConvertConfig.officeEnabled) {
            return "OFFICE";
        }
        if (ConvertConfig.libreOfficeEnabled) {
            return "LIBRE";
        }

        return null;
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
        String strOut = fileOutNoMark.getAbsolutePath();
        if (convertEntity.getPngMark() != null
                || convertEntity.getTextMark() != null
                || convertEntity.getFirstPageMark() != null
                || convertEntity.getBarCode() != null
                || convertEntity.isPageNum()) {
            String strOutPath = null;
            File fileOutMark = null;
            if (Objects.nonNull(fileOutNoMark)) {
                if ("pdf".equalsIgnoreCase(convertEntity.getOutPutFileType())) {
                    strOutPath = strDestPathFileName + "_wm.pdf";
                    PdfWaterMarkUtil.mark4Pdf(fileOutNoMark.getAbsolutePath(), strOutPath, convertEntity, 0);
                } else if ("ofd".equalsIgnoreCase(convertEntity.getOutPutFileType())) {
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

            if (strOutPath != null
                    && !fileOutMark.equals(fileOutNoMark)) {
                Path pathOut = FileUtil.rename(Paths.get(strOutPath), fileOutNoMark.getName(), true);
                strOut = pathOut.toString();
            } else {
                strOut = strOutPath;
            }
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
    private CallBackResult writeBack(WriteBackResult writeBackResult, ConvertEntity convertEntity, List<String> listJpg, File fileOut) {
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
        if (fileOut != null && fileOut.exists() && fileOut.length() > 0) {
            try {
                String lowerFileName = fileOut.getName().toLowerCase();
                if (lowerFileName.endsWith(".pdf")) {
                    if (convertEntity.getOutFileEncryptorEntity() != null
                            && convertEntity.getOutFileEncryptorEntity().getEncry()) {
                        try (PDDocument doc = PDDocument.load(fileOut, convertEntity.getOutFileEncryptorEntity().getUserPassWord())) {
                            log.info("转换PDF完成，共{}页", doc.getPages().getCount());
                        }
                    } else {
                        try (PDDocument doc = PDDocument.load(fileOut)) {
                            log.info("转换PDF完成，共{}页", doc.getPages().getCount());
                        }
                    }

                } else if (lowerFileName.endsWith("ofd")) {
                    if (convertEntity.getOutFileEncryptorEntity() == null ||
                            StringUtils.isEmpty(convertEntity.getOutFileEncryptorEntity().getUserPassWord())) {
                        try (OFDReader ofdReader = new OFDReader(Paths.get(fileOut.getCanonicalPath()))) {
                            log.info("转换OFD完成，共{}页", ofdReader.getPageList().size());
                        }
                    } else {
                        // todo 有密码的文件读取，尚未解决
                    }
                }

                return true;
            } catch (Exception e) {
                log.error("回写检查异常", e);
                return false;
            }
        }
        return false;
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
        if (!StringUtils.equalsAnyIgnoreCase(writeBackTypeName, "path", "local")) {
            for (String strJpg : listJpg) {
                FileUtil.del(strJpg);
            }
            FileUtil.del(fileOut);
        }

        if (!StringUtils.equalsAnyIgnoreCase(inputTypeName, "path", "local")) {
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
                                           WriteBackResult writeBackResult, String outPutFileName) {
        log.info("回调文件{}, url:{}, header:【{}】, params:【{}】 ", outPutFileName, strCallBackURL,
                StringUtils.join(mapWriteBackHeaders), StringUtils.join(writeBackResult));
        if (StringUtils.isBlank(strCallBackURL)) {
            log.info("文件{}回调地址为空，跳过", outPutFileName);
            return new CallBackResult(true, "回调地址为空，跳过");
        }

        // 回调路径预处理，截取?后的部分
        Map<String, Object> mapParams = writeBackResult.bean2Map();
        if (strCallBackURL.indexOf("?") > -1) {
            String strInputParams = strCallBackURL.substring(strCallBackURL.indexOf("?") + 1);
            String[] strsParams = strInputParams.split("&");
            for (int i = 0; i < strsParams.length; i++) {
                String[] p = strsParams[i].split("=");
                if (p.length == 2) {
                    mapParams.put(p[0], p[1]);
                }
            }

            strCallBackURL = strCallBackURL.substring(0, strCallBackURL.indexOf("?"));
        }

        //发送get请求并接收响应数据
        try (HttpResponse httpResponse = HttpUtil.createGet(strCallBackURL)
                .addHeaders(mapWriteBackHeaders).form(mapParams)
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
     * @param input   输入文件
     * @param params  其他参数
     * @param outType
     * @return 转换后的pdf文件
     */
    @Override
    public File filePreview(Input input, Map<String, String> params, String outType) throws Exception {
        if (input.exists() && (StringUtils.equalsAnyIgnoreCase(FileUtil.extName(input.getInputFile()), "pdf",
                "ofd"))) {
            return input.getInputFile();
        }
        if (Objects.isNull(params)) {
            params = new HashMap<>();
        }
        // 合并后的文件路径及文件名，无后缀
        String strDestPathFileName = ConvertConfig.outPutPath + input.getInputFile().getName();

        String extName = FileUtil.extName(input.getInputFile());
        if (StringUtils.equalsAnyIgnoreCase(extName, "xls", "xlsx")) {
            // excel 特殊处理
            return previewExcel(input, strDestPathFileName);
        } else if (StringUtils.equalsAnyIgnoreCase(extName, "zip", "rar", "tar", "7z", "jar")) {
            // 7z 解压, 返回解压后的文件夹
            return TDZipUtil.unzip(input.getInputFile(),
                    ConvertConfig.outPutPath,
                    params.getOrDefault("zip.password", ""));
        }

        // 默认预览
        return previewDefault(input, strDestPathFileName, outType);
    }

    /**
     * 预览转换, 默认读取 convert.preview.type， 转换 pdf 或 jpg
     *
     * @param input               传入的文件对象
     * @param strDestPathFileName 目标文件名，无后缀
     * @param outType
     * @return pdf转换结果
     * @throws IOException err
     */
    @Nullable
    private File previewDefault(Input input, String strDestPathFileName, String outType) throws Exception {
        String fileExt = ConvertConfig.previewType.equalsIgnoreCase("pdf") ? "pdf" : "jpg";
        if (StringUtils.isNotBlank(outType)) {
            fileExt = outType.equalsIgnoreCase("pdf") ? "pdf" : "jpg";
        }
        ConvertEntity convertEntity = new ConvertEntity();
        convertEntity.setOutPutFileType(fileExt);
        Input[] inputs = {input};
        convertEntity.setInputFiles(inputs);
        // 定时任务清理预览文件
        convertEntity.setInputType(InputType.PATH);

        if (FileUtil.exist(strDestPathFileName + "." + fileExt)) {
            // 已经转换过的，直接返回
            return new File(strDestPathFileName + "." + fileExt);
        }
        if (FileUtil.exist(strDestPathFileName + ".pdf")) {
            // 已经转换过的PDF，直接返回
            return new File(strDestPathFileName + ".pdf");
        }
        return convertAndMerge(convertEntity, strDestPathFileName, "PREVIEW");
    }

    /**
     * 预览excel， csv未处理
     *
     * @param input               传入的文件对象
     * @param strDestPathFileName 目标文件名，无后缀
     * @return excel转换结果
     * @throws IOException err
     */
    @NotNull
    private File previewExcel(Input input, String strDestPathFileName) throws IOException {
        String targetHtmlPath = strDestPathFileName + ".html";
        if (FileUtil.exist(targetHtmlPath)) {
            // 已经转换过的，直接返回
            return new File(targetHtmlPath);
        }

        Excel2HtmlUtil.excel2html(input.getInputFile(), targetHtmlPath);
        return new File(targetHtmlPath);
    }
}
