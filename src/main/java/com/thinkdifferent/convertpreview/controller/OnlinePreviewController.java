package com.thinkdifferent.convertpreview.controller;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.thinkdifferent.convertpreview.consts.ConfigConstants;
import com.thinkdifferent.convertpreview.entity.InputType;
import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.service.ConvertService;
import com.thinkdifferent.convertpreview.utils.ConvertPdfUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
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
     * @param path     文件路径
     * @param fileType 文件类型
     * @param params   其他参数
     * @param model    model
     * @return ftl文件名
     */
    @RequestMapping("/onlinePreview")
    public String onlinePreview(@RequestParam("filePath") String path,
                                @RequestParam(value = "fileType", required = false, defaultValue = "未知") String fileType,
                                @RequestParam(value = "params", required = false) Map<String, String> params,
                                @RequestParam(value = "outType", required = false, defaultValue = "pdf") String outType,
                                Model model) {
        model.addAttribute("baseUrl", ConfigConstants.baseUrl);
        model.addAttribute("watermarkImage", getWatermarkImage());
        model.addAttribute("watermarkTxt", watermarkText);
        model.addAttribute("watermarkXSpace", 10);
        model.addAttribute("watermarkYSpace", 10);
        model.addAttribute("watermarkFont", "微软雅黑");
        model.addAttribute("watermarkFontsize", "18px");
        model.addAttribute("watermarkColor", "black");
        model.addAttribute("watermarkAlpha", 0.2);
        model.addAttribute("watermarkWidth", "240");
        model.addAttribute("watermarkHeight", "80");
        model.addAttribute("watermarkAngle", "10");

        model.addAttribute("pdfPresentationModeDisable", "true");
        model.addAttribute("pdfOpenFileDisable", "true");
        model.addAttribute("pdfPrintDisable", "true");
        model.addAttribute("pdfDownloadDisable", "true");
        model.addAttribute("pdfBookmarkDisable", "true");
        try {
            Input input = InputType.convert(path, fileType);
            if (!input.exists()) {
                model.addAttribute("fileType", fileType);
                model.addAttribute("msg", "文件下载失败");
                return NOT_SUPPORT;
            }
            File pdfFile = convertService.filePreview(input);
            if ("pdf".equalsIgnoreCase(outType)) {
                model.addAttribute("pdfUrl", aes.encryptHex(pdfFile.getCanonicalPath()));
                return PDF_PREVIEW;
            } else {
                // 图片预览
                List<String> jpgUrl = convertPdfUtil.pdf2jpg(pdfFile)
                        .stream()
                        .map(str -> "/api/download?urlPath=" + aes.encryptHex(str))
                        .collect(Collectors.toList());
                model.addAttribute("imgUrls", jpgUrl);
                model.addAttribute("currentUrl", jpgUrl.get(0));
                return PICTURE_PREVIEW;
            }
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

    private static final String PICTURE_PREVIEW = "officePicture";
    private static final String PDF_PREVIEW = "pdf";
    private static final String NOT_SUPPORT = "fileNotSupported";
    private static final AES aes = SecureUtil.aes();
    private static String strWatermarkImage;

    @Value("${convert.preview.watermarkTxt:默认水印}")
    private String watermarkText;
    @Value("${convert.preview.watermarkImage:}")
    private String watermarkImage;

    @Resource
    private ConvertService convertService;
    @Resource
    private ConvertPdfUtil convertPdfUtil;
}
