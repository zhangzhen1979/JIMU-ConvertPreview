package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.system.OsInfo;
import com.thinkdifferent.convertpreview.config.SystemConstants;
import com.thinkdifferent.convertpreview.entity.PdfEntity;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.thinkdifferent.convertpreview.config.SystemConstants.SYSTEM_PDF_ENTITY_CACHE;

@Log4j2
public class SystemUtil {

    /**
     * 获取路径，自动根据操作系统、设置的内容，判断是绝对路径还是相对路径。统一处理为绝对路径
     *
     * @param path 输入的path
     * @return 转换后的绝对路径。
     */
    public static String getPath(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        path = SystemUtil.beautifulFilePath(path);
        if (new OsInfo().isWindows()) {
            if (":".equals(path.substring(1, 2))) {
                // 如果输入的路径中，第二个字符是【:】，则是绝对路径
            } else {
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                // 否则，是相对路径；即在当前服务所在文件夹的下面
                path = SystemUtil.beautifulPath(System.getProperty("user.dir")) + path;
            }
        } else {
            if (path.startsWith("/")) {
                // 如果输入的路径中，第一个字符是【/】，则是绝对路径
            } else {
                // 否则，是相对路径；即在当前服务所在文件夹的下面
                path = SystemUtil.beautifulPath(System.getProperty("user.dir")) + path;
            }
        }

        return path;
    }

    /**
     * 路径美化
     *
     * @param path 原始路径
     * @return 美化后的路径
     */
    public static String beautifulPath(String path) {
        path = _beautifulPath(path);
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        return beautifulFilePath(path);
    }

    /**
     * 文件路径标准化
     *
     * @param filePath 文件路径
     * @return 标准化结果
     */
    public static String beautifulFilePath(String filePath) {
        return _beautifulPath(filePath);
    }

    private static String _beautifulPath(String path) {
        if (StringUtils.isBlank(path)) {
            return path;
        }
        if (path.contains("\\") && !path.contains("\\\\")) {
            path = path.replace("\\", "/");
        } else {
            path = path.replace("\\\\", "/");
        }
        return path;
    }

    /**
     * 调整图片大小
     *
     * @param image          原始图片流
     * @param standardWidth  修改后的宽度
     * @param standardHeight 修改后的高度
     * @return 调整后的图片
     */
    public static BufferedImage resize(BufferedImage image, int standardWidth, int standardHeight) {
        if (image.getWidth() == standardWidth && image.getHeight() == standardHeight) {
            return image;
        }

        BufferedImage resizeImage = new BufferedImage(standardWidth, standardHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = resizeImage.createGraphics();
        graphics.drawImage(image, 0, 0, standardWidth, standardHeight, null);
        graphics.dispose();

        if (image != null) {
            image.getGraphics().dispose();
        }

        return resizeImage;
    }

    /**
     * 将修改后的PDF页面图片合并成PDF文件
     *
     * @param mapBase64s 修改后的PDF页面图片
     * @param inputPdf   原始PDF图片，用于获取未修改的页面
     * @param uuid       唯一主键，用于临时文件名
     * @return 修改后的PDF文件
     */
    public static File png2pdf(Map<String, String> mapBase64s, File inputPdf, String uuid) throws Exception {
        // 创建临时文件
        String secretPdfPath = SystemUtil.beautifulPath(SystemConstants.INPUT_FILE_PATH) + "temp/"
                + uuid + ".pdf";
        FileUtil.touch(secretPdfPath);
        @Cleanup PDDocument document = Loader.loadPDF(inputPdf);
        @Cleanup PDDocument newDocument = new PDDocument();
        // 获取原PDF页数
        int totalNum = document.getNumberOfPages();

        //　PDF文档渲染对象，在rendering包
        BufferedImage bufferedImage = null;
        PDFRenderer renderer = new PDFRenderer(document);
        for (int i = 0; i < totalNum; i++) {
            bufferedImage = mapBase64s.containsKey(i + 1 + "")
                    // 传入key 从1开始， 但pdf读取从0开始
                    ? ImgUtil.toImage(mapBase64s.get(i + 1 + ""))
                    // 原页面转图片
                    : renderer.renderImage(i);

            PDPage page = new PDPage();
            newDocument.addPage(page);

            @Cleanup PDPageContentStream pageContentStream = new PDPageContentStream(newDocument, page);
            @Cleanup ByteArrayOutputStream os = new ByteArrayOutputStream();

            // bufferedImage to byteArray
            ImageIO.write(bufferedImage, "png", os);
            PDImageXObject imageXObject = PDImageXObject.createFromByteArray(newDocument, os.toByteArray(), UUID.randomUUID() + ".png");
            // PDImageXObject imageXObject = PDImageXObject.createFromFile("D:\\SimpleSolution\\simple_solution.png", newDocument);
            pageContentStream.drawImage(imageXObject, 0, 0);

            newDocument.save(secretPdfPath);
        }

        if (bufferedImage != null) {
            bufferedImage.getGraphics().dispose();
        }

        return new File(secretPdfPath);
    }


    /**
     * 从缓存中获取传入对象
     *
     * @param uuid 唯一主键
     * @return 系统接收对象
     */
    public static PdfEntity getPdfEntityFromCache(String uuid) throws ExecutionException {
        Optional<PdfEntity> entity = SYSTEM_PDF_ENTITY_CACHE.get(uuid);
        return entity.orElse(null);
    }

    /**
     * 记录缓存数据
     *
     * @param entity 缓存数据
     */
    public static void saveCache(PdfEntity entity) {
        SYSTEM_PDF_ENTITY_CACHE.put(entity.getUuid(), Optional.of(entity));
    }

    /**
     * 根据输入的页码范围，转换为页码List
     *
     * @param strPageLimits 页码范围。支持：1,4-7,9-12
     * @return 处理后的页码List
     */
    public static List<Integer> getPages(String strPageLimits) {
        if (StringUtils.isBlank(strPageLimits)) {
            return Collections.emptyList();
        }
        // 处理输出指定页控制
        List<Integer> listPages = new ArrayList<>();
        String[] strPages = strPageLimits.split(",");
        for (String strPage : strPages) {
            if (strPage.contains("-")) {
                int intStart = Integer.parseInt(strPage.substring(0, strPage.indexOf("-")));
                int intEnd = Integer.parseInt(strPage.substring(strPage.indexOf("-") + 1));
                for (int j = intStart; j <= intEnd; j++) {
                    listPages.add(j);
                }
            } else {
                listPages.add(Integer.valueOf(strPage));
            }
        }

        return listPages;
    }

    public static void main(String[] args) {
        List<Integer> listDemo = getPages("1,4-7,9-12");

        for (Integer intPage : listDemo) {
            System.out.println(intPage);
        }

    }


}
