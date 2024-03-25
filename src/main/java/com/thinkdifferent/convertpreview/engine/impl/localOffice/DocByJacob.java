package com.thinkdifferent.convertpreview.engine.impl.localOffice;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import com.thinkdifferent.convertpreview.config.ConvertDocConfigEngineLocal;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;
import java.util.Objects;

@Component
@Log4j2
public class DocByJacob {
    /***
     *
     * Word转PDF（Jacob）
     *
     * @param appName     应用名称：wps、office
     * @param inputFile   输入文件
     * @param pdfFile     输出pdf文件
     * @return
     */

    public synchronized static long doc2PDF(String appName, String inputFile, String pdfFile) {
        ActiveXComponent app = null;
        Dispatch docs = null;
        Dispatch word = null;
        try {
            //  这句是调用初始化并放入内存中等待调用
            ComThread.InitSTA();
            // 打开WPS或Word应用程序
            if ("wps".equalsIgnoreCase(appName)) {
                app = new ActiveXComponent("KWPS.Application");
            } else if ("office".equalsIgnoreCase(appName)) {
                app = new ActiveXComponent("Word.Application");
            }
            log.info("开始转化Word为PDF...");
            long date = new Date().getTime();
            // 设置Word不可见
            app.setProperty("Visible", new Variant(0));
            app.setProperty("DisplayAlerts", new Variant(false));
            // 禁用宏
            app.setProperty("AutomationSecurity", new Variant(3));
            // 获得Word中所有打开的文档，返回documents对象
            docs = app.getProperty("Documents").toDispatch();
            // 调用Documents对象中Open方法打开文档，并返回打开的文档对象Document
            word = Dispatch.call(docs, "Open", inputFile, false, true).toDispatch();

            if ("wps".equalsIgnoreCase(appName)) {
                // 关闭所有修订
                if (ConvertDocConfigEngineLocal.localAcceptRevisions) {
                    Dispatch.put(word, "TrackRevisions", new Variant(false));
                    Dispatch.put(word, "PrintRevisions", new Variant(false));
                    Dispatch.put(word, "ShowRevisions", new Variant(false));
                }
                // 关闭批注
                if (ConvertDocConfigEngineLocal.localDeleteComments) {
                    Dispatch.call(word, "DeleteAllComments");
                }
            } else if ("office".equalsIgnoreCase(appName)) {

            }


            /***
             *
             * 调用Document对象的SaveAs方法，将文档保存为pdf格式
             *
             * Dispatch.call(doc, "SaveAs", pdfFile, wdFormatPDF
             * word保存为pdf格式宏，值为17 )
             *
             */
            Dispatch.call(word, "ExportAsFixedFormat", pdfFile, 17);// word保存为pdf格式宏，值为17
            log.info(word);
            // 关闭文档
            long date2 = new Date().getTime();
            return date2 - date;
        } catch (Exception | Error e) {
            log.error("使用本地应用将Word转PDF异常", e);
            return -1;
        } finally {
            if (Objects.nonNull(word)) {
                Dispatch.call(word, "Close", false);
            }
            if (Objects.nonNull(app)) {
                app.invoke("Quit", 0);
            }
            // 释放占用的内存空间
            try {
                if (Objects.nonNull(word)) {
                    word.safeRelease();
                }
                if (Objects.nonNull(docs)) {
                    docs.safeRelease();
                }
                ComThread.Release();
            } catch (Exception | Error e) {
                e.printStackTrace();
            }
        }

    }

    /***
     *
     * Excel转化成PDF(Jacob)
     *
     * @param appName     应用名称：wps、office
     * @param inputFile   输入文件
     * @param pdfFile     输出pdf文件
     * @return
     */
    public synchronized static long xls2PDF(String appName, String inputFile, String pdfFile) {
        ActiveXComponent app = null;
        Dispatch excel = null;
        Dispatch xlss = null;
        try {
            //  这句是调用初始化并放入内存中等待调用
            ComThread.InitSTA();
            // 打开WPS或Excel应用程序
            if ("wps".equalsIgnoreCase(appName)) {
                app = new ActiveXComponent("KET.Application");
            } else if ("office".equalsIgnoreCase(appName)) {
                app = new ActiveXComponent("Excel.Application");
            }
            log.info("开始转化Excel为PDF...");
            long date = new Date().getTime();
            app.setProperty("Visible", new Variant(0));
            app.setProperty("DisplayAlerts", new Variant(false));
            app.setProperty("AutomationSecurity", new Variant(3)); // 禁用宏
            xlss = app.getProperty("Workbooks").toDispatch();

            excel = Dispatch
                    .invoke(xlss, "Open", Dispatch.Method,
                            new Object[]{inputFile, new Variant(false), new Variant(false)}, new int[9])
                    .toDispatch();

            // 获取所有Sheet的数量
            Dispatch sheets = Dispatch.get(excel, "Sheets").toDispatch();
            int count = Dispatch.get(sheets, "Count").getInt();

            // 遍历每个Sheet
            for (int i = 1; i <= count; i++) {
                Dispatch sheet = Dispatch.invoke(sheets, "Item", Dispatch.Get, new Object[]{i}, new int[1]).toDispatch();

                // 获取每页sheet的PageSetup对象
                Dispatch pageSetup = Dispatch.get(sheet, "PageSetup").toDispatch();
                // 设置每页sheet自适应页面宽度和高度。
                Dispatch.put(pageSetup, "FitToPagesWide", new Variant(1));
                Dispatch.put(pageSetup, "FitToPagesTall", new Variant(1));

                // 关闭批注
                if (ConvertDocConfigEngineLocal.localDeleteComments) {
                    // 获取批注集合
                    Dispatch comments = Dispatch.get(sheet, "Comments").toDispatch();
                    int commentsCount = Dispatch.get(comments, "Count").getInt();

                    // 遍历批注集合并删除
                    for (int j = commentsCount; j >= 1; j--) {
                        Dispatch comment = Dispatch.invoke(comments, "Item", Dispatch.Get, new Object[]{j}, new int[1]).toDispatch();
                        Dispatch.call(comment, "Delete");
                    }
                }
            }

            // 转换格式
            Dispatch.invoke(excel, "ExportAsFixedFormat", Dispatch.Method, new Object[]{
                    new Variant(0), // Type。PDF格式=0
                    pdfFile,// FileName。一个表示要保存文件的文件名的字符串。 可以包括完整路径，否则 Excel 会将文件保存在当前文件夹中。
                    new Variant(0), // Quality。0=标准 (生成的PDF图片不会变模糊) 1=最小文件(生成的PDF图片糊的一塌糊涂)
                    new Variant(false),// IncludeDocProperties。是否包含文档属性。设置为 True 以指示应包含文档属性，或设置为 False 以指示省略它们。
                    new Variant(true)// IgnorePrintAreas。如果设置为 True，则忽略在发布时设置的任何打印区域。 如果设置为 False，则使用发布时设置的打印区域。
            }, new int[1]);

            long date2 = new Date().getTime();
            return (date2 - date);
        } catch (Exception | Error e) {
            log.error("使用本地应用将Excel转PDF异常", e);
            return -1;
        } finally {
            if (Objects.nonNull(excel)) {
                Dispatch.call(excel, "Close", new Variant(false));
            }

            if (app != null) {
                app.invoke("Quit", new Variant[]{});
            }

            // 释放占用的内存空间
            try {
                if (Objects.nonNull(excel)) {
                    excel.safeRelease();
                }
                if (Objects.nonNull(xlss)) {
                    xlss.safeRelease();
                }
                ComThread.Release();
            } catch (Exception | Error e) {
                e.printStackTrace();
            }
        }
    }

    /***
     * ppt转化成PDF
     *
     * @param appName     应用名称：wps、office
     * @param inputFile   输入文件
     * @param pdfFile     输出pdf文件
     * @return
     */
    public synchronized static long ppt2PDF(String appName, String inputFile, String pdfFile) {
        ActiveXComponent app = null;
        Dispatch powerPoint = null;
        Dispatch ppts = null;
        try {
            //  这句是调用初始化并放入内存中等待调用
            ComThread.InitSTA();
            // 打开WPS或PPT应用程序
            if ("wps".equalsIgnoreCase(appName)) {
                app = new ActiveXComponent("KWPP.Application");
            } else if ("office".equalsIgnoreCase(appName)) {
                app = new ActiveXComponent("PowerPoint.Application");
            }
            app.setProperty("DisplayAlerts", new Variant(false));

            log.info("开始转化PowerPoint为PDF...");
            long date = new Date().getTime();

            ppts = app.getProperty("Presentations").toDispatch();
            powerPoint = Dispatch.call(ppts, "Open", inputFile, true, // ReadOnly
                    //    false, // Untitled指定文件是否有标题
                    false// WithWindow指定文件是否可见
            ).toDispatch();

//            // 关闭批注
//            if(ConvertDocConfig.localDeleteComments){
//                // 遍历每个Slide
//                Dispatch slides = Dispatch.get(powerPoint, "Slides").toDispatch();
//                int count = Dispatch.get(slides, "Count").getInt();
//                for (int i = 1; i <= count; i++) {
//                    Dispatch slide = Dispatch.invoke(slides, "Item", Dispatch.Get, new Object[]{ i }, new int[1]).toDispatch();
//
//                    // 获取批注集合
//                    Dispatch comments = Dispatch.get(slide, "Comments").toDispatch();
//                    int commentsCount = Dispatch.get(comments, "Count").getInt();
//
//                    // 逐个删除批注
//                    for (int j = commentsCount; j >= 1; j--) {
//                        Dispatch comment = Dispatch.invoke(comments, "Item", Dispatch.Get, new Object[]{ j }, new int[1]).toDispatch();
//                        Dispatch.call(comment, "Delete");
//                    }
//                }
//            }

            pdfFile = pdfFile.replaceAll("/", "\\\\");
            Dispatch.invoke(powerPoint,
                    "SaveAs",
                    Dispatch.Method,
                    new Object[]{pdfFile, new Variant(32)},
                    new int[1]);

            long date2 = new Date().getTime();
            return (date2 - date);
        } catch (Exception | Error e) {
            log.error("使用本地应用将PowerPoint转PDF异常", e);
            return -1;
        } finally {
            if (Objects.nonNull(powerPoint)) {
                Dispatch.call(powerPoint, "Close");
            }
            if (Objects.nonNull(app)) {
                app.invoke("Quit");
            }

            // 释放占用的内存空间
            try {
                if (Objects.nonNull(powerPoint)) {
                    powerPoint.safeRelease();
                }
                if (Objects.nonNull(ppts)) {
                    ppts.safeRelease();
                }
                ComThread.Release();
            } catch (Exception | Error e) {
                e.printStackTrace();
            }
        }
    }


    /***
     * vsd（Visio）转化成PDF（只支持Office，Jacob方式调用）
     *
     * @param inputFile   输入文件
     * @param pdfFile     输出pdf文件
     * @return
     */
    public synchronized static long vsd2PDF(String inputFile, String pdfFile) {
        ActiveXComponent app = null;
        ActiveXComponent documents = null;
        Dispatch vsd = null;
        try {
            //  这句是调用初始化并放入内存中等待调用
            ComThread.InitSTA();

            File fileInput = new File(inputFile);

            // 打开Office应用程序
            app = new ActiveXComponent("Visio.Application");
            app.setProperty("Visible", new Variant(0));

            documents = new ActiveXComponent(app.getProperty("Documents").toDispatch());

            log.info("开始转化Visio为PDF...");
            long date = new Date().getTime();

            vsd = documents.invoke("Open", new Variant(fileInput.getPath())).toDispatch();

            Dispatch.invoke(vsd,
                    "ExportAsFixedFormat",
                    Dispatch.Method,
                    new Object[]{new Variant(1), pdfFile, new Variant(1), new Variant(0)},
                    new int[1]);

            long date2 = new Date().getTime();
            return (date2 - date);
        } catch (Exception | Error e) {
            log.error("使用本地应用将Visio转PDF异常", e);
            return -1;
        } finally {
            if (Objects.nonNull(vsd)) {
                Dispatch.call(vsd, "Close");
            }
            if (Objects.nonNull(app)) {
                app.invoke("Quit");
            }

            // 释放占用的内存空间
            try {
                if (Objects.nonNull(vsd)) {
                    vsd.safeRelease();
                }
                ComThread.Release();
            } catch (Exception | Error e) {
                e.printStackTrace();
            }
        }
    }


}
