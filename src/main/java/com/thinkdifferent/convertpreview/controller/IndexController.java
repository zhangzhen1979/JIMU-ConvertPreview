package com.thinkdifferent.convertpreview.controller;

import com.thinkdifferent.convertpreview.config.SystemConstants;
import com.thinkdifferent.convertpreview.utils.convert4pdf.LocalConvertUtil;
import com.thinkdifferent.convertpreview.vo.MessageBean;
import io.swagger.annotations.ApiOperation;
import net.sf.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collection;

/**
 * @author ltian
 * @version 1.0
 * @date 2022/5/13 14:15
 */
@RestController
public class IndexController {

    @GetMapping
    public String index() {
        return "启动成功";
    }

    @ApiOperation("显示最近错误的数据，最大200条")
    @GetMapping("listError")
    public Collection<JSONObject> listError() {
        return SystemConstants.ERROR_CONVERT_DATA.values();
    }

    @GetMapping("testJacob")
    public MessageBean<Boolean> testJacob(@RequestParam("input") String input, @RequestParam("output") String output)
            throws IOException, InterruptedException {
        return MessageBean.success(LocalConvertUtil.process(input, output));
    }
}
