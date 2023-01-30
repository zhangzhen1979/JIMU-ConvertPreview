package com.thinkdifferent.convertpreview.utils.convert4pdf.jacob;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;
import java.util.Objects;

@Component
@Log4j2
public class ConvertByJacob {
    /***
     *
     * Word转PDF（Jacob）
     *
     * @param appName     应用名称：wps、office
     * @param inputFile   输入文件
     * @param pdfFile     输出pdf文件
     * @return
     */

    public synchronized static int doc2PDF(String appName, String inputFile, String pdfFile) {
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
            return (int) ((date2 - date) / 1000);
        } catch (Exception e) {
            log.error("本地应用转pdf异常", e);
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
            } catch (Exception e) {
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
    public synchronized static int xls2PDF(String appName, String inputFile, String pdfFile) {
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
            // 转换格式
            Dispatch.invoke(excel, "ExportAsFixedFormat", Dispatch.Method, new Object[]{new Variant(0), // PDF格式=0
                    pdfFile, new Variant(0) // 0=标准 (生成的PDF图片不会变模糊) 1=最小文件
                    // (生成的PDF图片糊的一塌糊涂)
            }, new int[1]);

            // 这里放弃使用SaveAs
            /*
             * Dispatch.invoke(excel,"SaveAs",Dispatch.Method,new Object[]{
             * outFile, new Variant(57), new Variant(false), new Variant(57),
             * new Variant(57), new Variant(false), new Variant(true), new
             * Variant(57), new Variant(true), new Variant(true), new
             * Variant(true) },new int[1]);
             */
            long date2 = new Date().getTime();
            return (int) ((date2 - date) / 1000);
        } catch (Exception e) {
            log.error("wps excel转pdf异常", e);
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
            } catch (Exception e) {
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
    public synchronized static int ppt2PDF(String appName, String inputFile, String pdfFile) {
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

            log.info("开始转化PPT为PDF...");
            long date = new Date().getTime();

            ppts = app.getProperty("Presentations").toDispatch();
            powerPoint = Dispatch.call(ppts, "Open", inputFile, true, // ReadOnly
                    //    false, // Untitled指定文件是否有标题
                    false// WithWindow指定文件是否可见
            ).toDispatch();

            pdfFile = pdfFile.replaceAll("/", "\\\\");
            Dispatch.invoke(powerPoint,
                    "SaveAs",
                    Dispatch.Method,
                    new Object[]{pdfFile, new Variant(32)},
                    new int[1]);

            long date2 = new Date().getTime();
            return (int) ((date2 - date) / 1000);
        } catch (Exception e) {
            log.error("ppt 转 pdf 异常", e);
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /***
     * vsd（Visio））转化成PDF（只支持Office，Jacob方式调用）
     *
     * @param inputFile   输入文件
     * @param pdfFile     输出pdf文件
     * @return
     */
    public synchronized static int vsd2PDF(String inputFile, String pdfFile) {
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

            log.info("开始转化VSD为PDF...");
            long date = new Date().getTime();

            vsd = documents.invoke("Open", new Variant(fileInput.getPath())).toDispatch();

            Dispatch.invoke(vsd,
                    "ExportAsFixedFormat",
                    Dispatch.Method,
                    new Object[]{new Variant(1), pdfFile, new Variant(1), new Variant(0)},
                    new int[1]);

            long date2 = new Date().getTime();
            return (int) ((date2 - date) / 1000);
        } catch (Exception e) {
            log.error("vsd 转 pdf 异常", e);
            return -1;
        } finally {
            if (Objects.nonNull(vsd)) {
                Dispatch.call(vsd, "Close");
            }
            if (Objects.nonNull(documents)) {
                Dispatch.call(documents, "Close");
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
