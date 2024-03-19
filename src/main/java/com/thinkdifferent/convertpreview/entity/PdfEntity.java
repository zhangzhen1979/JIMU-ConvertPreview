package com.thinkdifferent.convertpreview.entity;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.thinkdifferent.convertpreview.entity.input.Input;
import com.thinkdifferent.convertpreview.entity.writeback.WriteBack;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import com.thinkdifferent.convertpreview.vo.PdfInputVO;
import lombok.Cleanup;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
@Accessors(chain = true)
@Log4j2
public class PdfEntity {
    private String uuid;
    /**
     * 输入文件
     */
    private Input input;
    /**
     * 文件回写
     */
    private WriteBack writeBack;
    /**
     * 回调地址
     */
    private String callBackUrl;

    /**
     * 输入对象转换
     *
     * @param vo 输入对象
     * @return 标准entity
     */
    @SneakyThrows
    public static PdfEntity of(PdfInputVO vo) {
        log.debug("接收参数:{}", vo);
        PdfEntity result = SystemUtil.getPdfEntityFromCache(vo.getUuid());
        if (Objects.isNull(result)) {
            result = new PdfEntity().setUuid(vo.getUuid())
                    .setInput(InputType.convert(vo.getFilePath(), vo.getFileName(), vo.getFileType()))
                    // 只支持url的
                    .setWriteBack(WriteBackType.of(vo.getWriteBack()))
                    .setCallBackUrl(vo.getCallBack());
        }

        return result;
    }

    /**
     * 清理文件
     */
    public void clean() {
        if (Objects.nonNull(input)) {
            input.clean();
        }
    }

    /**
     * 回调接口
     *
     * @param blnWriteBack 回写结果
     */
    public String callBack(boolean blnWriteBack) {
        if (StringUtils.isBlank(callBackUrl)) {
            return "未提供回调地址";
        }
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("flag", blnWriteBack);
        reqBody.put("uuid", uuid);
        //发送get请求并接收响应数据
        @Cleanup HttpResponse httpResponse = HttpUtil.createGet(callBackUrl)
                //.addHeaders(mapWriteBackHeaders)
                .form(reqBody)
                .execute();
        String body = httpResponse.body();

        log.info("回调结果：{}， body：{}", httpResponse.isOk(), body);
        return httpResponse.isOk() ? body : "回调接口异常，状态码：" + httpResponse.getStatus() + " ,返回内容：" + body;
    }
}