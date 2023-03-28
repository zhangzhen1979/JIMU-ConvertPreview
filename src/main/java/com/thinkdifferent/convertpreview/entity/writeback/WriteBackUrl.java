package com.thinkdifferent.convertpreview.entity.writeback;

import cn.hutool.core.map.MapUtil;
import com.thinkdifferent.convertpreview.entity.WriteBackResult;
import com.thinkdifferent.convertpreview.utils.WriteBackUtil;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/4/22 10:15
 */
@Log4j2
public class WriteBackUrl extends WriteBack {
    /**
     * 文件上传接口的API地址
     */
    private String url;
    /**
     * 请求头
     */
    private Map<String, String> writeBackHeaders;

    /**
     * 是否匹配
     *
     * @param input 输入内容
     * @return 是否匹配
     */
    @Override
    public boolean match(String input) {
        if (StringUtils.startsWithAny(input, "http://", "https://")) {
            this.setUrl(input);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public WriteBack of(Map<String, Object> writeBack) {
        WriteBackUrl writeBackUrl = new WriteBackUrl();
        writeBackUrl.setUrl(MapUtil.getStr(writeBack, "url"));
        return writeBackUrl;
    }

    /**
     * 转换结果回写
     *
     * @param outPutFileType 目标文件类型
     * @param fileOut        转换后的文件
     * @param listJpg        转换后的jpg文件
     */
    @SneakyThrows
    @Override
    public WriteBackResult writeBack(String outPutFileType, File fileOut, List<String> listJpg) {
        if ("jpg".equalsIgnoreCase(outPutFileType)) {
            if(listJpg != null && listJpg.size() > 0){
                return WriteBackUtil.writeBack2Api(listJpg.get(0), url, writeBackHeaders);
            }else{
                return new WriteBackResult(false, "输出格式为jpg但无数据");
            }
        } else {
            // pdf 和 ofd 的都走这里
            return WriteBackUtil.writeBack2Api(fileOut.getCanonicalPath(), url, writeBackHeaders);
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getWriteBackHeaders() {
        return writeBackHeaders;
    }

    public void setWriteBackHeaders(Map<String, String> writeBackHeaders) {
        this.writeBackHeaders = writeBackHeaders;
    }
}
