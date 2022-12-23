package com.thinkdifferent.convertpreview.vo;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 接收PDF 对象
 */
@Data
@Builder
public class PdfInputVO {
    @NotBlank(message = "uuid不能为空")
    private String uuid;
    /**
     * 支持格式
     * 本地路径： D:/temp/a.pdf /data/a.pdf
     * 下载路径： http://www.baidu.com/pdf1.pdf  https://www.baidu.com/fwewgwerw (特殊格式的url)
     * ftp：     ftp://username:password/dir1/file1.pdf ftp://dir1/file1.pdf
     */
    @NotBlank(message = "文件路径不能为空")
    private String filePath;
    private String fileType;
    /**
     * 回调：http://.... get , 带uuid
     */
    @NotBlank(message = "回调地址不能为空")
    private String callBack;
    /**
     * 回写，
     * ftp: ftp://username:password/dir1/
     * http://www/api/writeBack POST
     */
    private String writeBack;

}
