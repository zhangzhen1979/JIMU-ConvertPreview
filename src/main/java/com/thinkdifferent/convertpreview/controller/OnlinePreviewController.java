package com.thinkdifferent.convertpreview.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.thinkdifferent.convertpreview.config.ConvertConfig;
import com.thinkdifferent.convertpreview.entity.InputType;
import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.service.ConvertService;
import com.thinkdifferent.convertpreview.utils.ConvertPdfUtil;
import com.thinkdifferent.convertpreview.utils.SpringUtil;
import lombok.extern.log4j.Log4j2;
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
     * @param filePath 文件路径, base64格式，支持本地路径、http、ftp
     * @param fileType 文件类型， http格式必传
     * @param params   其他参数, 如：pdf\ofd 用户名密码
     * @param model    model，非必传， pdf officePicture compress ofd
     * @return ftl文件名
     */
    @RequestMapping("/onlinePreview")
    public String onlinePreview(@RequestParam("filePath") String filePath,
                                @RequestParam(value = "fileType", required = false, defaultValue = "") String fileType,
                                @RequestParam(value = "params", required = false) Map<String, String> params,
                                @RequestParam(value = "outType", required = false, defaultValue = "") String outType,
                                Model model) {
        model.addAllAttributes(getDefaultModelParams());
        try {
            filePath = Base64.decodeStr(filePath);
            Input input = InputType.convert(filePath, fileType);
            if (!input.exists()) {
                model.addAttribute("fileType", fileType);
                model.addAttribute("msg", "文件下载失败");
                return NOT_SUPPORT;
            }
            fileType = FileUtil.extName(input.getInputFile());
            File convertFile = convertService.filePreview(input, params);
            Assert.isTrue(convertFile.exists(), "转换失败");
            // 根据输出类型选择不同的模板
            if (StringUtils.isBlank(outType)) {
                outType = ConvertConfig.previewType;
            }
            if (StringUtils.equalsAnyIgnoreCase(fileType, "ofd", "xls", "xlsx")) {
                outType = FILE_PREVIEW_MAPPING.getOrDefault(fileType, "pdf");
            }
            // 不同模板参数不同
            MODEL_PARAMS_MAPPING.getOrDefault(outType, MODEL_PARAMS_MAPPING.get(PDF_PREVIEW))
                    .config(model, convertFile);
            return outType;
        } catch (Exception e) {
            log.error("预览文件异常", e);
            model.addAttribute("fileType", fileType);
            model.addAttribute("msg", "该文件不允许预览");
            return NOT_SUPPORT;
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
    private Map<String,Object> getDefaultModelParams(){
        if (CollectionUtil.isEmpty(DEFAULT_MODEL_PARAMS)){
            if (StringUtils.isBlank(watermarkText)){
                watermarkText = cn.hutool.extra.spring.SpringUtil.getProperty("convert.preview.watermarkTxt");
            }
            if (StringUtils.isBlank(ConvertConfig.blnChangeType)){
                ConvertConfig.blnChangeType = cn.hutool.extra.spring.SpringUtil.getProperty("convert.preview.blnChange");
            }
            DEFAULT_MODEL_PARAMS = new HashMap() {{
                put("baseUrl", com.thinkdifferent.convertpreview.consts.ConfigConstants.baseUrl);
                put("watermarkImage", getWatermarkImage());
                put("watermarkTxt", watermarkText);
                put("watermarkXSpace", 10);
                put("watermarkYSpace", 10);
                put("watermarkFont", "微软雅黑");
                put("watermarkFontsize", "18px");
                put("watermarkColor", "black");
                put("watermarkAlpha", 0.2);
                put("watermarkWidth", "240");
                put("watermarkHeight", "80");
                put("watermarkAngle", "10");
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
    private Map<String, Object> DEFAULT_MODEL_PARAMS ;

    /**
     * 文件类型及默认预览页面
     */
    private static final Map<String, String> FILE_PREVIEW_MAPPING = new HashMap() {{
        put("pdf", PICTURE_PREVIEW);
        put("xls", PICTURE_PREVIEW);
        put("xlsx", PICTURE_PREVIEW);
        // put("png", PICTURE_PREVIEW);
        put("ofd", OFD_PREVIEW);
        // 压缩文件预览
        put("zip", COMPRESS_PREVIEW);
        put("7z", COMPRESS_PREVIEW);
        put("rar", COMPRESS_PREVIEW);
        put("jar", COMPRESS_PREVIEW);
        put("tar", COMPRESS_PREVIEW);
    }};

    /**
     * 模板及参数处理页面
     */
    private static final Map<String, ModelParams> MODEL_PARAMS_MAPPING = new HashMap() {{
        put(PDF_PREVIEW, (ModelParams) (model, convertFile) -> {
            if (StringUtils.equalsAnyIgnoreCase(FileUtil.extName(convertFile), "xls", "xlsx")) {
                // excel转html, 直接返回
                model.addAttribute("blnExcel", "true");
                model.addAttribute("htmlUrl",
                        "/api/download?urlPath=" + aes.encryptHex(convertFile.getCanonicalPath()));
            } else {
                model.addAttribute("pdfUrl", OnlinePreviewController.aes.encryptHex(convertFile.getCanonicalPath()));
            }
        });
        put(PICTURE_PREVIEW, (ModelParams) (model, convertFile) -> {
            // 图片预览
            List<String> jpgUrl = SpringUtil.getClass(ConvertPdfUtil.class).pdf2jpg(convertFile)
                    .stream()
                    .map(str -> "/api/download?urlPath=" + OnlinePreviewController.aes.encryptHex(str))
                    .collect(Collectors.toList());
            model.addAttribute("imgUrls", jpgUrl);
            model.addAttribute("currentUrl", jpgUrl.get(0));
        });
        put(COMPRESS_PREVIEW, (ModelParams) (model, convertFile) -> {
            String strFileTree = Objects.isNull(convertFile) ? "{}" : com.thinkdifferent.convertpreview.utils.FileUtil.fileTree(convertFile);
            model.addAttribute("fileTree", strFileTree);
        });
        put(OFD_PREVIEW, (ModelParams) (model, convertFile) -> {
            model.addAttribute("currentUrl",
                    "/api/download?urlPath=" + OnlinePreviewController.aes.encryptHex(convertFile.getPath()));
        });
    }};

    private static final String PICTURE_PREVIEW = "officePicture";
    private static final String PDF_PREVIEW = "pdf";
    private static final String NOT_SUPPORT = "fileNotSupported";
    private static final String COMPRESS_PREVIEW = "compress";
    private static final String OFD_PREVIEW = "ofd";

    private static final AES aes = SecureUtil.aes();
    private static String strWatermarkImage;

    @Value("${convert.preview.watermarkTxt:默认水印}")
    private String watermarkText;
    @Value("${convert.preview.watermarkImage:}")
    private String watermarkImage;

    @Resource
    private ConvertService convertService;

    interface ModelParams {
        void config(Model model, File convertFile) throws IOException;
    }

}
