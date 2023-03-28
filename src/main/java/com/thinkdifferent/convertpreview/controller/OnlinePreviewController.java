package com.thinkdifferent.convertpreview.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.thinkdifferent.convertpreview.config.ConvertConfig;
import com.thinkdifferent.convertpreview.consts.ConfigConstants;
import com.thinkdifferent.convertpreview.entity.InputType;
import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.service.ConvertService;
import com.thinkdifferent.convertpreview.utils.SpringUtil;
import com.thinkdifferent.convertpreview.utils.convert4pdf.ConvertPdfUtil;
import lombok.extern.log4j.Log4j2;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /**
     * 预览
     *
     * @param filePath  文件路径, base64格式，支持本地路径、http、ftp
     * @param fileType  文件类型， http格式必传
     * @param params    其他参数, 如：pdf\ofd 用户名密码
     * @param model     model，非必传， pdf officePicture compress ofd
     * @param keyword   pdf高亮关键词，支持持单个词语
     * @param waterMark 水印配置，json, base64，
     * @param md5       文件MD5，判断是否是同一个文件
     * @return ftl文件名
     */
    @RequestMapping("/onlinePreview")
    public String onlinePreview(@RequestParam("filePath") String filePath,
                                @RequestParam(value = "fileType", required = false, defaultValue = "") String fileType,
                                @RequestParam(value = "params", required = false) Map<String, String> params,
                                @RequestParam(value = "outType", required = false, defaultValue = "") String outType,
                                @RequestParam(value = "watermark", required = false, defaultValue = "") String waterMark,
                                @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                @RequestParam(value = "md5", required = false) String md5,
                                Model model) {
        model.addAllAttributes(getDefaultModelParams());
        File inputFile = null;
        try {
            if (filePath.contains(" ")) {
                filePath = filePath.replaceAll(" ", "+").replaceAll("\n", "");
            }
            if (Pattern.matches(base64Pattern, filePath)) {
                filePath = Base64.decodeStr(filePath);
            }
            if (StringUtils.startsWithAny(filePath, "http://", "https://") && StringUtils.isNotBlank(md5)) {
                filePath += "&md5=" + md5;
            }
            Input input = InputType.convert(filePath, fileType);
            if (!input.exists()) {
                model.addAttribute("fileType", fileType);
                model.addAttribute("msg", "文件下载失败");
                return NOT_SUPPORT;
            }
            inputFile = input.getInputFile();
            fileType = FileUtil.extName(inputFile);
            // 根据输出类型选择不同的模板
            if (StringUtils.isBlank(outType)) {
                outType = ConvertConfig.previewType;
            }
            if (FILE_PREVIEW_MAPPING.keySet().contains(fileType.toLowerCase())) {
                outType = FILE_PREVIEW_MAPPING.getOrDefault(fileType, "pdf");
            }
            File convertFile = convertService.filePreview(input, params, FILE_CONVERT_MAPPING.getOrDefault(fileType, outType));
            Assert.isTrue(convertFile.exists(), "转换失败");

            model.addAttribute("keyword", keyword);
            // 水印
            addWaterMark(model, waterMark);
            // 不同模板参数不同
//            if (outType.equalsIgnoreCase(PICTURE_PREVIEW) && )
            MODEL_PARAMS_MAPPING.getOrDefault(outType, MODEL_PARAMS_MAPPING.get(PDF_PREVIEW))
                    .config(model, convertFile);
            // 模型预览
            if (Objects.nonNull(model.getAttribute("imgUrls"))
                    && ((List) model.getAttribute("imgUrls")).size() == 1) {
                return PICTURE_ONE_PREVIEW;
            }
            return outType;
        } catch (Exception e) {
            log.error("预览文件异常", e);
            model.addAttribute("fileType",
                    Objects.isNull(inputFile) || inputFile.length() == 0 ? "文件内容为空，" : ("类型(" + fileType + ")"));
            model.addAttribute("msg", "该文件不允许预览");
            return NOT_SUPPORT;
        }
    }

    /**
     * 添加水印信息
     *
     * @param model
     * @param waterMark
     */
    private void addWaterMark(Model model, String waterMark) {
        if (StringUtils.isBlank(waterMark)) {
            return;
        }
        waterMark = Base64.decodeStr(waterMark);
        JSONObject joWaterMark = JSONObject.fromObject(waterMark);
        if (StringUtils.isNotBlank(joWaterMark.optString("content"))) {
            model.addAttribute("watermarkImage", "");
        }
        model.addAttribute("watermarkTxt", joWaterMark.optString("content", watermarkText));
        // 旋转角度
        model.addAttribute("watermarkAngle", joWaterMark.optDouble("rotate", 30));
        // 字体
        String font = joWaterMark.optString("font");
        String[] splits;
        if (StringUtils.isNotBlank(font) && (splits = font.split(" ")).length == 3) {
            model.addAttribute("watermarkFontsize", splits[1]);
        }
        // 旋转角度
        model.addAttribute("watermarkAngle", joWaterMark.optString("rotate"));
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

    @GetMapping("/download")
    public void download(@RequestParam("urlPath") String url, HttpServletResponse resp) {
        try {
            String filePath = aes.decryptStr(url);
            IOUtils.write(FileUtil.readBytes(filePath), resp.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getWatermarkImage() {
        if (Objects.isNull(strWatermarkImage)) {
            if (StringUtils.isBlank(watermarkImage)) {
                strWatermarkImage = "";
            } else {
                // 转base64
                File watermarkFile = FileUtil.file(".", watermarkImage);
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
            if (StringUtils.isBlank(watermarkText)) {
                watermarkText = cn.hutool.extra.spring.SpringUtil.getProperty("convert.preview.watermarkTxt");
            }
            if (StringUtils.isBlank(ConvertConfig.blnChangeType)) {
                ConvertConfig.blnChangeType = cn.hutool.extra.spring.SpringUtil.getProperty("convert.preview.blnChange");
            }
            DEFAULT_MODEL_PARAMS = new HashMap<String, Object>() {{
                put("baseUrl", ConfigConstants.baseUrl);
                put("watermarkImage", getWatermarkImage());
                put("watermarkTxt", watermarkText);
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
                put("blnExcel", "false");
                put("pdfUrl", "");
                put("htmlUrl", "");
                put("blnChangeType", ConvertConfig.blnChangeType);
            }};
        }
        return DEFAULT_MODEL_PARAMS;
    }

    private Map<String, Object> DEFAULT_MODEL_PARAMS;


    /**
     * 默认文件转换类型
     */
    private static final Map<String, String> FILE_CONVERT_MAPPING = new HashMap<String, String>() {{
        put("xls", PDF_PREVIEW);
        put("xlsx", PDF_PREVIEW);
        put("ppt", PDF_PREVIEW);
        put("pptx", PDF_PREVIEW);
        put("doc", PDF_PREVIEW);
        put("docx", PDF_PREVIEW);
        put("rtf", PDF_PREVIEW);
        put("vsd", PDF_PREVIEW);
        put("vsdx", PDF_PREVIEW);
    }};

    /**
     * 文件类型及默认预览页面
     */
    private static final Map<String, String> FILE_PREVIEW_MAPPING = new HashMap<String, String>() {{
        // put("pdf", PICTURE_PREVIEW);
        put("xls", PDF_PREVIEW);
        put("xlsx", PDF_PREVIEW);
//        put("ppt", PDF_PREVIEW);
//        put("pptx", PDF_PREVIEW);
//        put("doc", PDF_PREVIEW);
//        put("docx", PDF_PREVIEW);
        // put("png", PICTURE_PREVIEW);
        put("ofd", OFD_PREVIEW);
        // 压缩文件预览
        put("zip", COMPRESS_PREVIEW);
        put("7z", COMPRESS_PREVIEW);
        put("rar", COMPRESS_PREVIEW);
        put("jar", COMPRESS_PREVIEW);
        put("tar", COMPRESS_PREVIEW);
        // 图片
    }};

    /**
     * 模板及参数处理页面
     */
    private static final Map<String, ModelParams> MODEL_PARAMS_MAPPING = new HashMap<String, ModelParams>() {{
        put(PDF_PREVIEW, (model, convertFile) -> {
            if (StringUtils.equalsAnyIgnoreCase(FileUtil.extName(convertFile), "xls", "xlsx", "html")) {
                // excel转html, 直接返回
                model.addAttribute("blnExcel", "true");
                model.addAttribute("htmlUrl",
                        "/api/download?urlPath=" + aes.encryptHex(convertFile.getCanonicalPath()));
            } else {
                model.addAttribute("pdfUrl", OnlinePreviewController.aes.encryptHex(convertFile.getCanonicalPath()));
            }
        });
        put(PICTURE_PREVIEW, (model, convertFile) -> {
            // 图片预览
            List<String> jpgUrl = SpringUtil.getClass(ConvertPdfUtil.class).pdf2jpg(convertFile)
                    .stream()
                    .map(str -> "/api/download?urlPath=" + OnlinePreviewController.aes.encryptHex(str))
                    .collect(Collectors.toList());
            model.addAttribute("imgUrls", jpgUrl);
            model.addAttribute("currentUrl", jpgUrl.size() > 0 ? jpgUrl.get(0) : "");
        });
        put(COMPRESS_PREVIEW, (model, convertFile) -> {
            String strFileTree = Objects.isNull(convertFile) ? "{}" : com.thinkdifferent.convertpreview.utils.FileUtil.fileTree(convertFile);
            model.addAttribute("fileTree", strFileTree);
        });
        put(OFD_PREVIEW, (model, convertFile) -> model.addAttribute("currentUrl",
                "/api/download?urlPath=" + OnlinePreviewController.aes.encryptHex(convertFile.getPath())));
    }};

    private static final String PICTURE_PREVIEW = "officePicture";
    private static final String PICTURE_ONE_PREVIEW = "picture";
    private static final String PDF_PREVIEW = "pdf";
    private static final String NOT_SUPPORT = "fileNotSupported";
    private static final String COMPRESS_PREVIEW = "compress";
    private static final String OFD_PREVIEW = "ofd";

    private static final AES aes = SecureUtil.aes();
    private static String strWatermarkImage;

    @Value("${convert.preview.watermarkTxt:}")
    private String watermarkText;
    @Value("${convert.preview.watermarkImage:}")
    private String watermarkImage;

    @Resource
    private ConvertService convertService;

    interface ModelParams {
        void config(Model model, File convertFile) throws IOException;
    }

    private static final String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9" +
            "+/]{2}==)$";
}
