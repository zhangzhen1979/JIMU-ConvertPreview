package com.thinkdifferent.convertpreview.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.ConvertException;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.extra.spring.SpringUtil;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigBase;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigPreview;
import com.thinkdifferent.convertpreview.config.ConvertVideoConfig;
import com.thinkdifferent.convertpreview.consts.ConvertFileTypeEnum;
import com.thinkdifferent.convertpreview.entity.InputType;
import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.entity.input.InputPath;
import com.thinkdifferent.convertpreview.service.ConvertService;
import com.thinkdifferent.convertpreview.service.PoiConvertTypeService;
import com.thinkdifferent.convertpreview.utils.AesUtil;
import com.thinkdifferent.convertpreview.utils.ArcZipUtil;
import com.thinkdifferent.convertpreview.utils.ConvertOfdUtil;
import com.thinkdifferent.convertpreview.utils.OnlineCacheUtil;
import com.thinkdifferent.convertpreview.utils.pdfUtil.ConvertPdfUtil;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.ofdrw.reader.OFDReader;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 预览controller
 *
 * @author ltian
 * @version 1.0
 * @date 2022/12/8 15:57
 */
@Log4j2
@Controller
@RequestMapping("/api")
public class OnlinePreviewController {

    private static final String JPG_PREVIEW = "jpg";
    private static final String PDF_PREVIEW = "pdf";
    private static final String COMPRESS_PREVIEW = "compress";
    private static final String MEDIA_PREVIEW = "media";
    private static final String HTML_PREVIEW = "html";
    private static final String XML_PREVIEW = "xml";

    private static final String NOT_SUPPORT = "fileNotSupported";
    private static final String ERROR = "error";

    private static String strWatermarkImage;

    @Resource
    private ConvertService convertService;

    interface ModelParams {
        void config(Model model, File convertFile) throws IOException;
    }

    private static final String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9" +
            "+/]{2}==)$";

    @SneakyThrows
    @GetMapping(value = "/getZipList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getZipList(@RequestParam("filePath") String filePath,
                                @RequestParam(value = "fileType", required = false, defaultValue = "") String fileType,
                                @RequestParam(value = "pId",required = false,defaultValue = "-1") String pId,
                                @RequestParam("pathInFile") String pathInFile){
        filePath = getFilePathByMd5(filePath, "", "", null);
        Input input = InputType.convert(filePath, null, fileType);

        return ArcZipUtil.fileTree(input.getInputFile(), pathInFile, pId);
    }

    @PostMapping(value = "/checkPermissions", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JSONObject checkPermissions(@RequestBody JSONObject joInput) {
        JSONObject joReturn = new JSONObject();
        joReturn.put("key", "");
        joReturn.put("message", "");

        if (!joInput.isEmpty()) {
            // todo 需要实现：检查用户名、密码匹配;检查当前是否在有效期。
            // todo 如果系统中设置了用户名密码：如果用户名、密码匹配，且在有效期，则返回key，否则返回对应的提示信息。
            // todo 如果系统中没有设置用户名密码：文件在有效期，则返回key，否则返回对应的提示信息。
            joReturn.put("key", (joInput.optString("username", "admin") + "++"
                    + joInput.optString("password", "admin") + "0000000000000000").substring(0, 16));
        } else {
            joReturn.put("message", "文件未到有效期，或用户名密码错误");
        }

        return joReturn;
    }

    /**
     * 预览
     *
     * @param filePath         文件路径, base64格式，支持本地路径、http、ftp
     * @param fileType         文件类型， http格式必传
     * @param model            model，非必传， pdf officePicture compress ofd
     * @param keyword          pdf高亮关键词，支持持单个词语
     * @param waterMark        水印配置，json, base64，
     * @param md5              文件MD5，判断是否是同一个文件
     * @param callBackUrl      回调地址, base64 str, 默认空，      此项不为空时可进行pdf遮盖
     * @param writeBackType    回写类型, WriteBackType str, 默认空，不会写，    此项不为空时可进行pdf遮盖
     * @param writeBack        回写配置信息, base64 json, 默认空，     此项不为空时可进行pdf遮盖
     * @param writeBackHeaders 回写请求头，base64 json, 默认空
     * @param controlParams    页面按钮控制参数，json。Base64后的页面按钮控制权限信息（AES加密）, 默认空
     * @return ftl文件名
     */
    @RequestMapping("/onlinePreview")
    public String onlinePreview(@RequestParam("filePath") String filePath,
                                @RequestParam(value = "fileType", required = false, defaultValue = "") String fileType,
                                @RequestParam(value = "fileInZip", required = false, defaultValue = "") String fileInZip,
                                @RequestParam(value = "blnDirInZip", required = false, defaultValue = "false") boolean blnDirInZip,
                                @RequestParam(value = "ticket", required = false, defaultValue = "") String ticket,
                                @RequestParam(value = "outType", required = false, defaultValue = "") String outType,
                                @RequestParam(value = "watermark", required = false, defaultValue = "") String waterMark,
                                @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                @RequestParam(value = "md5", required = false) String md5,
                                @RequestParam(value = "callBackUrl", required = false, defaultValue = "") String callBackUrl,
                                @RequestParam(value = "writeBackType", required = false, defaultValue = "") String writeBackType,
                                @RequestParam(value = "writeBack", required = false, defaultValue = "") String writeBack,
                                @RequestParam(value = "writeBackHeaders", required = false, defaultValue = "") String writeBackHeaders,
                                @RequestParam(value = "controlParams", required = false, defaultValue = "") String controlParams,
                                Model model) {

        model.addAllAttributes(getDefaultModelParams());
        File inputFile = null;
        try {
            filePath = getFilePathByMd5(filePath, md5, controlParams, model);

            Input input = InputType.convert(filePath, null, fileType);
            if (!input.exists()) {
                model.addAttribute("fileType", fileType);
                model.addAttribute("msg", "文件获取失败");
                return NOT_SUPPORT;
            }
            // 压缩包文件分级展现处理
            if (StringUtils.isNotBlank(fileInZip)){
                File zipFile = input.getInputFile();
                if (blnDirInZip){
                    // dir
                    String dirTree = ArcZipUtil.fileTree(zipFile, fileInZip);
                    MODEL_PARAMS_MAPPING.get(COMPRESS_PREVIEW)
                            .config(model, new ArcZipUtil.ZipFile("tmp", dirTree));
                    return COMPRESS_PREVIEW;
                }else{
                    // 压缩包内文件
                    File fileInZipFile = ArcZipUtil.unzipOneFile(zipFile, fileInZip, ConvertDocConfigBase.outPutPath, "");
                    input = new InputPath().of(FileUtil.getCanonicalPath(fileInZipFile), null, null);
                }
            }
            inputFile = input.getInputFile();
            fileType = FileUtil.extName(inputFile);

            // 文本类文件以 html格式预览
            if (com.thinkdifferent.convertpreview.utils.FileUtil.isText(inputFile)) {
                // html 指定水印
                decodeWaterMark(outType, waterMark, model);
                MODEL_PARAMS_MAPPING.get(HTML_PREVIEW).config(model, inputFile);
                return HTML_PREVIEW;
            }

            // 根据输入类型选择默认的输出类型
            outType = getFileOutType(fileType, outType);

            // 根据输出类型选择不同的模板
            log.info("接收到的参数，fileType:{}, outType:{}", fileType, outType);

            // 需要转换格式后才能预览的
            JSONObject joInput = new JSONObject();
            joInput.put("inputFile", FileUtil.getCanonicalPath(input.getInputFile()));
            joInput.put("inputFileType", fileType);
            joInput.put("outPutFileType", outType);

            String cacheName = joInput.toString();
            // 缓存文件
            File targetFile = OnlineCacheUtil.get(cacheName);
            String previewFilePath = null;
            if (FileUtil.exist(targetFile)) {
                if (FileUtil.extName(targetFile).equalsIgnoreCase(outType)) {
                    // 1. 缓存文件是目标类型，直接返回
                    previewFilePath = FileUtil.getCanonicalPath(targetFile);
                } else {
                    // 2. 缓存文件是中间类型，使用中间类型转换
                    joInput.put("inputFile", FileUtil.getCanonicalPath(targetFile));
                    joInput.put("inputFileType", FileUtil.extName(targetFile));
                    joInput.put("outPutFileType", outType);
                }
            }
            if (StringUtils.isBlank(previewFilePath)) {
                joInput.put("outPutFileName", cn.hutool.core.lang.UUID.randomUUID().toString());
                joInput.put("callBackURL", callBackUrl);
                if (StringUtils.isNotBlank(writeBack)) {
                    joInput.put("writeBackType", writeBackType);
                    joInput.put("writeBack", writeBack);
                    joInput.put("writeBackHeaders", writeBackHeaders);
                }
                joInput.put("inputType", InputType.PATH.name());
                // pdf 水印
                joInput.put("waterMark", decodeWaterMark(outType, waterMark, model));

                targetFile = OnlineCacheUtil.get(cacheName, () -> convertService.convert(joInput).getTarget());
            }
            // pdf 高亮
            if (StringUtils.isNotBlank(keyword)) {
                model.addAttribute("keyword", keyword);
            }

            // 根据转换后文件类型选择不同的模板
            if (FileUtil.isFile(previewFilePath) && !MEDIA_PREVIEW.equalsIgnoreCase(outType)) {
                if (!outType.equalsIgnoreCase(FileUtil.extName(previewFilePath))) {
                    outType = FileUtil.extName(previewFilePath);
                }
            } else {
                if (!StringUtils.equalsAny(outType, COMPRESS_PREVIEW, MEDIA_PREVIEW)) {
                    // 文件夹除压缩包外，其他以设置的形式预览
                    if("pdf".equalsIgnoreCase(ConvertDocConfigPreview.previewType)){
                        outType = PDF_PREVIEW;
                    }else{
                        outType = JPG_PREVIEW;
                    }
                }
            }
            // 兼容poi等预览结果
            decodeWaterMark(outType, waterMark, model);
            if (!MODEL_PARAMS_MAPPING.containsKey(outType)) {
                outType = PDF_PREVIEW;
            }
            MODEL_PARAMS_MAPPING.get(outType).config(model, targetFile);
            return outType;
        } catch (
                Exception e) {
            log.error("预览文件异常", e);
            model.addAttribute("fileType",
                    Objects.isNull(inputFile) || inputFile.length() == 0 ? "文件内容为空，" : ("类型(" + fileType + ")"));
            model.addAttribute("msg", "该文件不允许预览");
            return NOT_SUPPORT;
        }
    }

    /**
     * 获取文件的转换类型，兼容 yml\默认配置
     *
     * @param fileType 文件类型
     * @param outType  参数传入的转换类型， pdf | officePicture
     * @return 转换类型, 合并转换类型及预览页面
     */
    private String getFileOutType(String fileType, String outType) {
        addDefaultConfig();

        if (StringUtils.isNotBlank(outType)) {
            // 输出格式转为 预览格式，原输出格式为officePicture时，转为jpg
            return outType.equalsIgnoreCase("pdf") ? "pdf" : "jpg";
        }
        // 音视频格式
        if (ConvertVideoConfig.extList.contains(fileType)) {
            return "media";
        }
        return MAP_FILE_2_PREVIEW.get(ConvertFileTypeEnum.valueOfExtName(fileType).name());
    }

    private static boolean blnAddDefaultConfigFlag = false;

    private void addDefaultConfig() {
        if (blnAddDefaultConfigFlag) {
            return;
        }
        blnAddDefaultConfigFlag = true;
        if (StringUtils.isNotBlank(ConvertDocConfigPreview.previewType)) {
            // 设置配置的默认预览格式
            MAP_FILE_2_PREVIEW.put(ConvertFileTypeEnum.pdf.name(), ConvertDocConfigPreview.previewType);
            MAP_FILE_2_PREVIEW.put(ConvertFileTypeEnum.cad.name(), ConvertDocConfigPreview.previewType);
        }
        // 使用engine转换word等 doc\xlsx\ppt 默认转pdf, 但如果启用poi预览，则以html格式进行转换
        try {
            SpringUtil.getBeansOfType(PoiConvertTypeService.class)
                    .keySet()
                    .stream()
                    .map(bean -> StringUtils.substring(bean.getClass().getName(), 7, -11).toLowerCase())
                    .forEach(simpleName -> {
                        if (simpleName.startsWith("word")) {
                            MAP_FILE_2_PREVIEW.put("doc", "html");
                            MAP_FILE_2_PREVIEW.put("docx", "html");
                            MAP_FILE_2_PREVIEW.put(ConvertFileTypeEnum.word.name(), "html");
                        } else if (simpleName.startsWith("excel")) {
                            MAP_FILE_2_PREVIEW.put("xls", "html");
                            MAP_FILE_2_PREVIEW.put("xlsx", "html");
                            MAP_FILE_2_PREVIEW.put(ConvertFileTypeEnum.excel.name(), "html");
                        } else if (simpleName.startsWith("ppt")) {
                            MAP_FILE_2_PREVIEW.put("ppt", "html");
                            MAP_FILE_2_PREVIEW.put("pptx", "html");
                            MAP_FILE_2_PREVIEW.put(ConvertFileTypeEnum.ppt.name(), "html");
                        }
                    });
        } catch (Exception ignored) {
        }
    }

    private String getFilePathByMd5(String filePath, String md5, String controlParams, Model model) throws UnsupportedEncodingException {
        if (controlParams != null && !controlParams.isEmpty()) {
            changeCtrlParams(controlParams, model);
        }

        if (filePath.contains(" ")) {
            filePath = filePath.replaceAll(" ", "+").replaceAll("\n", "");
        }
        filePath = urlDecode(filePath);
        if (Pattern.matches(base64Pattern, filePath)) {
            filePath = Base64.decodeStr(filePath);
        }
        if (StringUtils.startsWithAny(filePath, "http://", "https://") && StringUtils.isNotBlank(md5)) {
            filePath += "&md5=" + md5;
        }
        return filePath;
    }

    /**
     * 解析水印
     *
     * @param outType
     * @param waterMark
     * @param model
     * @return
     * @throws UnsupportedEncodingException
     */
    private String decodeWaterMark(String outType, String waterMark, Model model) throws UnsupportedEncodingException {
        if (ConvertDocConfigPreview.watermarkEnable) {
            if (waterMark != null && !"".equals(waterMark)) {
                waterMark = urlDecode(waterMark);
                waterMark = Base64.decodeStr(waterMark);
                // 如果不是PDF方式预览，则添加页面动态水印；否则，直接为文件添加水印
                if (!"pdf".equalsIgnoreCase(outType)) {
                    addWaterMark2Model(model, waterMark);
                    waterMark = null;
                } else {
                    JSONObject inputWaterMark = JSONObject.fromObject(waterMark);
                    // pdf, 将传入的水印json转为转换支持的结构
                    JSONObject joWaterMark = new JSONObject();
                    joWaterMark.put("alpha", inputWaterMark.optString("opacity"));
                    inputWaterMark.remove("opacity");
                    inputWaterMark.put("degree", inputWaterMark.optInt("rotate"));
                    inputWaterMark.remove("rotate");
                    inputWaterMark.put("fontColor", inputWaterMark.optString("color"));
                    inputWaterMark.remove("color");
                    inputWaterMark.put("fontSize", inputWaterMark.optInt("fontsize"));
                    inputWaterMark.remove("fontsize");
                    inputWaterMark.put("waterMarkText", inputWaterMark.optString("content"));
                    inputWaterMark.remove("content");
                    joWaterMark.put("text", inputWaterMark);
                    waterMark = joWaterMark.toString();
                }
            }
        } else {
            waterMark = null;
        }
        return waterMark;
    }

    /**
     * 添加水印信息
     *
     * @param model
     * @param waterMark
     */
    private void addWaterMark2Model(Model model, String waterMark) {
        if (StringUtils.isBlank(waterMark)) {
            return;
        }
        JSONObject joWaterMark = JSONObject.fromObject(waterMark);
        if (StringUtils.isNotBlank(joWaterMark.optString("content"))) {
            model.addAttribute("watermarkImage", "");
        }

        String strContent = joWaterMark.optString("content", ConvertDocConfigPreview.watermarkText);
        if (strContent.indexOf("\n") > -1) {
            strContent = strContent.replaceAll("\n", "");
        }

        model.addAttribute("watermarkTxt", strContent);
        // 旋转角度
        model.addAttribute("watermarkAngle", joWaterMark.optDouble("rotate", 30));
        // 字体
        String font = joWaterMark.optString("font");
        String[] splits;
        if (StringUtils.isNotBlank(font) && (splits = font.split(" ")).length == 3) {
            model.addAttribute("watermarkFontsize", splits[1]);
        }
        // 透明度
        String fillStyle = joWaterMark.optString("fillstyle");
        if (StringUtils.isNotBlank(fillStyle)) {
            fillStyle = fillStyle.substring(fillStyle.indexOf("("), fillStyle.indexOf(")"));
            String[] split = fillStyle.split(",");
            if (split.length == 4) {
                model.addAttribute("watermarkAlpha", split[3]);
            }
        }
        // 2023年3月21日 颜色 #fff
        if (StringUtils.isNotBlank(joWaterMark.optString("color"))) {
            model.addAttribute("watermarkColor", joWaterMark.optString("color"));
        }
        // 透明度 0.8
        if (StringUtils.isNotBlank(joWaterMark.optString("opacity"))) {
            model.addAttribute("watermarkAlpha", joWaterMark.optString("opacity"));
        }
        // 字体大小
        if (StringUtils.isNotBlank(joWaterMark.optString("fontsize"))) {
            model.addAttribute("watermarkFontsize", joWaterMark.optString("fontsize") + "px");
        }
    }

    /**
     * 检测pdf图片是否转换完成，
     * 传入文件UUID，获取转换后的文件是否存在，
     *
     * @param filePath
     * @return
     */
    @RequestMapping(value="/checkImg",produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<String> checkImages(@RequestParam("filePath") String filePath, @RequestParam(value = "md5", required = false) String md5) {
        if (!filePath.isEmpty()) {
            try {
                filePath = AesUtil.decryptStr(filePath);
                if (FileUtil.isDirectory(filePath)) {
                    //
                    String finalFilePath = filePath;
                    List<String> fileNames = FileUtil.listFileNames(filePath);
                    fileNames.sort(Comparator.comparingInt(s -> Integer.parseInt(s.substring(0, s.indexOf(".")))));
                    return fileNames
                            .stream()
                            .map(imgName -> "/api/download?urlPath=" + AesUtil.encryptStr(
                                    FileUtil.getCanonicalPath(FileUtil.file(finalFilePath, imgName))))
                            .collect(Collectors.toList());
                }
            } catch (Exception | Error e) {
                log.error("查询pdf转换图片完成情况", e);
            }
        }
        return new ArrayList<>();
    }

    /**
     * 判断输入的字符串中是否包含转义符“%”，如果有，则进行解码
     *
     * @param input 输入的转义字符串
     * @return 输出的解码后的字符串。
     * @throws UnsupportedEncodingException err
     */
    private String urlDecode(String input) throws UnsupportedEncodingException {
        if (Pattern.matches("%", input)) {
            input = URLDecoder.decode(input, null);
        }
        return input;
    }

    /**
     * 设置预览页面按钮开关参数
     *
     * @param controlParams 控制参数JSON
     * @param model         参数对象
     */
    private void changeCtrlParams(String controlParams, Model model) {
        if (controlParams != null && !controlParams.isEmpty()) {
            try {
                //解密为原字符串
                String decryptStr = decodeUrlParamByAes(controlParams);

                JSONObject jsonCtrlParams = JSONObject.fromObject(decryptStr);
                // 是否允许开启演示模式（默认禁止演示模式）
                if (jsonCtrlParams.containsKey("PPTMode")) {
                    model.addAttribute("pdfPresentationModeDisable", changeKey(jsonCtrlParams.optBoolean("PPTMode")));
                }
                // 是否允许在线打印（默认禁止打印）
                if (jsonCtrlParams.containsKey("print")) {
                    model.addAttribute("pdfPrintDisable", changeKey(jsonCtrlParams.optBoolean("print")));
                }
                // 是否允许下载当前文件（默认禁止下载）
                if (jsonCtrlParams.containsKey("download")) {
                    model.addAttribute("pdfDownloadDisable", changeKey(jsonCtrlParams.optBoolean("download")));
                }

            } catch (Exception | Error e) {
                log.error("页面控制参数编码或内容错误， key={}，controlParams={}",
                        ConvertDocConfigBase.urlEncodeKey,
                        controlParams);
                log.error(e);
            }

        }

    }


    /**
     * URL参数解码
     *
     * @param strParam 编码后的密文（先Base64，在URLEncode）
     */
    private String decodeUrlParamByAes(String strParam) throws UnsupportedEncodingException {
        if (strParam != null &&
                !"".equals(strParam) &&
                !"null".equalsIgnoreCase(strParam)) {
            // 先URLDecode
            strParam = urlDecode(strParam);

            try {
                //构建AES
                byte[] key = null;
                if (ConvertDocConfigBase.urlEncodeKey != null
                        && !ConvertDocConfigBase.urlEncodeKey.isEmpty()) {
                    key = ConvertDocConfigBase.urlEncodeKey.getBytes();
                }
                AES aes = SecureUtil.aes(key);
                //解密为原字符串
                return aes.decryptStr(strParam, CharsetUtil.CHARSET_UTF_8);

            } catch (Exception | Error e) {
                log.error("URL参数传入的密文错误，解码失败。 params={}",
                        strParam);
                log.error(e);
            }
        }
        return null;
    }

    /**
     * 转换传入的参数值。传入为false，转换为“true”；传入为true，转换为“false”
     *
     * @param value bln
     * @return 转换后的值
     */
    private String changeKey(boolean value) {
        if (value) {
            return "false";
        } else {
            return "true";
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Object> download(@RequestParam(value = "urlPath", required = false) String url,
                                           @RequestParam(value = "file", required = false) String strFile) {
        try {
            HttpHeaders headers = new HttpHeaders();

            String downloadFilePath = "";
            if (StringUtils.isNotBlank(url)) {
                downloadFilePath = AesUtil.decryptStr(url);
                AbstractResource resource = null;
                long resourceLength = -1L;
                // 兼容m3u8格式预览
                if (FileUtil.extName(url).equalsIgnoreCase("m3u8")){
                    for (int i = 0; i < ConvertVideoConfig.m3u8DownloadWait; i++) {
                        if (FileUtil.exist(url)){
                            break;
                        }
                        ThreadUtil.safeSleep(1000);
                    }
                    if(!FileUtil.exist(url)){
                        throw new ConvertException("m3u8文件解析失败，请稍后重试");
                    }

                    // m3u8解析，需要处理ts前缀
                    List<String> contents = FileUtil.readUtf8Lines(url).stream().map(s -> {
                        if (s.startsWith("out") && s.endsWith(".ts")) {
                            return "/api/download?urlPath=" + AesUtil.encryptStr(FileUtil.getParent(url, 1) + "/"+ s);
                        }
                        return s;
                    }).collect(Collectors.toList());
                    byte[] bytes = String.join("\n", contents).getBytes(StandardCharsets.UTF_8);
                    resource = new ByteArrayResource(bytes);
                    resourceLength = bytes.length;
                }else{
                    resource = new FileSystemResource(downloadFilePath);
                    resourceLength = new File(downloadFilePath).length();
                }
                headers.add("Content-Disposition", "attachment;filename=" + URLEncoder.encode(FileUtil.getName(downloadFilePath), "UTF-8"));
                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(resourceLength)
                        .contentType(MediaType.parseMediaType("application/octet-stream"))
                        .body(resource);
            } else if (StringUtils.isNotBlank(strFile)) {
                downloadFilePath = Base64.decodeStr(strFile);
            }
            File file = FileUtil.file(downloadFilePath);
            if (StringUtils.isBlank(downloadFilePath) || !FileUtil.exist(file)) {
                return ResponseEntity.notFound().build();
            }


            boolean blnTextFile = com.thinkdifferent.convertpreview.utils.FileUtil.isText(file);
            String strExt = FileUtil.extName(file);
            if (blnTextFile) {
                headers.add("Content-Type", "text/html;charset=utf-8");
                String fileContent = FileUtil.readUtf8String(file);
                if (!StringUtils.equalsAnyIgnoreCase(strExt, "xml", "htm", "html")) {
                    fileContent = fileContent.replace("\n", "<br/>");
                }
                return ResponseEntity.ok().headers(headers).body(fileContent);
            } else {
                headers.add("Content-Disposition", "attachment;filename=" + URLEncoder.encode(file.getName(), "UTF-8"));
                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(file.length())
                        .contentType(MediaType.parseMediaType("application/octet-stream"))
                        .body(new FileSystemResource(file));
            }
        } catch (Exception | Error e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    private String getWatermarkImage() {
        if (Objects.isNull(strWatermarkImage)) {
            if (StringUtils.isBlank(ConvertDocConfigPreview.watermarkImage)) {
                strWatermarkImage = "";
            } else {
                // 转base64
                File watermarkFile = FileUtil.file(".", ConvertDocConfigPreview.watermarkImage);
                try {
                    String imageType = FileUtil.getType(watermarkFile);
                    strWatermarkImage = "data:image/" + imageType + ";base64,"
                            + ImgUtil.toBase64(ImageIO.read(watermarkFile), imageType);
                } catch (IOException e) {
                    log.error("水印图片转base64异常", e);
                    strWatermarkImage = "";
                }
            }
        }
        return strWatermarkImage;
    }


    /**
     * 默认的model参数
     */
    private Map<String, Object> getDefaultModelParams() {
        if (CollectionUtil.isEmpty(DEFAULT_MODEL_PARAMS)) {
            if (ConvertDocConfigPreview.watermarkEnable) {
                if (StringUtils.isBlank(ConvertDocConfigPreview.watermarkText)) {
                    ConvertDocConfigPreview.watermarkText = SpringUtil.getProperty("convert.preview.watermarkTxt");
                }
            } else {
                ConvertDocConfigPreview.watermarkText = "";
            }

            if (ConvertDocConfigPreview.watermarkEnable) {
                if (!StringUtils.isBlank(getWatermarkImage())) {
                    ConvertDocConfigPreview.watermarkImage = getWatermarkImage();
                }
            } else {
                ConvertDocConfigPreview.watermarkImage = "";
            }

            DEFAULT_MODEL_PARAMS = new HashMap<String, Object>() {{
                put("watermarkImage", ConvertDocConfigPreview.watermarkImage);
                put("watermarkTxt", ConvertDocConfigPreview.watermarkText);
                put("watermarkFontsize", "18px");
                put("watermarkAlpha", 0.8);
                put("watermarkAngle", "30");
                put("watermarkXSpace", 10);
                put("watermarkYSpace", 10);
                put("watermarkFont", "微软雅黑");
                put("watermarkColor", "gray");
                put("watermarkWidth", "240");
                put("watermarkHeight", "80");
                put("pdfPresentationModeDisable", "true");
                put("pdfOpenFileDisable", "true");
                put("pdfPrintDisable", "true");
                put("pdfDownloadDisable", "true");
                put("pdfBookmarkDisable", "true");
                put("keyword", "");
                put("blnExcel", "false");
                put("blnOfd", "false");
                put("pdfUrl", "");
                put("htmlUrl", "");
                put("xmlUrl", "");
                put("uid", "");
                put("numberOfPages", "0");
                // m3u8 播放配置
                put("hlsJs", "");
                put("hlsPlug", "");
            }};
        }
        return DEFAULT_MODEL_PARAMS;
    }

    private Map<String, Object> DEFAULT_MODEL_PARAMS;

    /**
     * 文件 -- 转换格式, 会根据配置信息及输出修改
     * 会根据 yml 变化，只变化一次
     *
     * @see ConvertFileTypeEnum
     */
    private static final Map<String, String> MAP_FILE_2_PREVIEW = new HashMap<String, String>() {{
        put(ConvertFileTypeEnum.img.name(), "jpg");
        put(ConvertFileTypeEnum.ppt.name(), "jpg");
        put(ConvertFileTypeEnum.pdf.name(), "pdf");
        put(ConvertFileTypeEnum.cad.name(), "pdf");
        put(ConvertFileTypeEnum.xml.name(), "pdf");
        put(ConvertFileTypeEnum.ofd.name(), "ofd");
        put(ConvertFileTypeEnum.html.name(), "html");
        put(ConvertFileTypeEnum.zip.name(), "compress");
        // 默认输出为 pdf
        put(ConvertFileTypeEnum.engine.name(), "pdf");
        // 是为了取值方便
        put("jpg", "jpg");
        put("compress", "compress");
        put("mp3", MEDIA_PREVIEW);
        put("mp4", MEDIA_PREVIEW);
    }};

    /**
     * 模板及参数处理页面
     */
    private static final Map<String, ModelParams> MODEL_PARAMS_MAPPING = new HashMap<String, ModelParams>() {{
        put(PDF_PREVIEW, (model, convertFile) -> {
            if (StringUtils.equalsAnyIgnoreCase(FileUtil.extName(convertFile), "xls", "xlsx", "html", "ofd")) {
                if ("ofd".equalsIgnoreCase(FileUtil.extName(convertFile))) {
                    model.addAttribute("blnOfd", "true");
                } else {
                    // excel转html, 直接返回
                    model.addAttribute("blnExcel", "true");
                }
                model.addAttribute("htmlUrl",
                        "/api/download?urlPath=" + AesUtil.encryptStr(convertFile.getCanonicalPath()));
            } else {
                model.addAttribute("pdfUrl", AesUtil.encryptStr(convertFile.getCanonicalPath()));
            }
        });
        put(JPG_PREVIEW, (model, convertFile) -> {
            // 图片预览
            List<String> jpgUrl = new ArrayList<>();
            if (convertFile.isDirectory()) {
                List<String> jpgPaths = FileUtil.loopFiles(convertFile).stream().map(FileUtil::getCanonicalPath).collect(Collectors.toList());
                jpgUrl = jpgPaths.stream()
                        .map(str -> "/api/download?urlPath=" + AesUtil.encryptStr(str))
                        .collect(Collectors.toList());
                // 照片父路径
                String parentPath = FileUtil.getParent(jpgPaths.get(0), 1);
                model.addAttribute("filePath", AesUtil.encryptStr(parentPath));
            } else if ("pdf".equalsIgnoreCase(FileUtil.extName(convertFile))) {
                List<String> jpgPaths = SpringUtil.getBean(ConvertPdfUtil.class).pdf2jpg(convertFile, null, true);
                jpgUrl = jpgPaths.stream()
                        .map(str -> "/api/download?urlPath=" + AesUtil.encryptStr(str))
                        .collect(Collectors.toList());
                // pdf 总页数
                try (PDDocument asyncDoc = Loader.loadPDF(convertFile)) {
                    model.addAttribute("numberOfPages", asyncDoc.getNumberOfPages());
                }
                // 照片父路径
                String parentPath = FileUtil.getParent(jpgPaths.get(0), 1);
                model.addAttribute("filePath", AesUtil.encryptStr(parentPath));
            } else if ("ofd".equalsIgnoreCase(FileUtil.extName(convertFile))) {
                List<String> jpgPaths = SpringUtil.getBean(ConvertOfdUtil.class).convertOfd2Jpg(convertFile, null, true);
                jpgUrl = jpgPaths.stream()
                        .map(str -> "/api/download?urlPath=" + AesUtil.encryptStr(str))
                        .collect(Collectors.toList());
                // ofd 总页数
                try (OFDReader reader = new OFDReader(Paths.get(convertFile.getAbsolutePath()))) {
                    model.addAttribute("numberOfPages", reader.getNumberOfPages());
                }
                // 照片父路径
                String parentPath = FileUtil.getParent(jpgPaths.get(0), 1);
                model.addAttribute("filePath", AesUtil.encryptStr(parentPath));
            } else {
                jpgUrl.add("/api/download?urlPath=" + AesUtil.encryptStr(convertFile.getAbsolutePath()));
                model.addAttribute("numberOfPages", 1);
                model.addAttribute("filePath", "");
            }
            model.addAttribute("imgUrls", jpgUrl);
            model.addAttribute("currentUrl", jpgUrl.size() > 0 ? jpgUrl.get(0) : "");
        });
        put(COMPRESS_PREVIEW, (model, zipFile) -> {
            // @see ArcZipUtil.ZipFile
            String strFileTree = Objects.isNull(zipFile) ? "{}" : ArcZipUtil.readZipFree(zipFile);
            model.addAttribute("fileTree", strFileTree);
        });
        put(MEDIA_PREVIEW, ((model, convertFile) ->{
            if ("m3u8".equalsIgnoreCase(FileUtil.extName(convertFile))){
                model.addAttribute("hlsJs", "<script type=\"text/javascript\" src=\"/ckplayer/hls.js/hls.min.js\"></script>");
                model.addAttribute("hlsPlug", "plug:'hls.js',");
            }
                model.addAttribute("mediaUrl",
                        "download?urlPath=" + AesUtil.encryptStr(convertFile.getPath()));
        }
        ));
        put(HTML_PREVIEW, ((model, convertFile) ->
                model.addAttribute("htmlUrl",
                        "/api/download?urlPath=" + AesUtil.encryptStr(convertFile.getCanonicalPath()))
        ));
        put(XML_PREVIEW, (model, convertFile) -> {
            model.addAttribute("xmlUrl",
                    "/api/download?urlPath=" + AesUtil.encryptStr(convertFile.getCanonicalPath()));
        });

    }};


}
