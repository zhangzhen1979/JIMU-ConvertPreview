package com.thinkdifferent.convertpreview.controller;

import com.thinkdifferent.convertpreview.config.SystemConstants;
import com.thinkdifferent.convertpreview.engine.impl.localOffice.LocalConvertDocUtil;
import com.thinkdifferent.convertpreview.vo.MessageBean;
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

    @GetMapping("listError")
    public Collection<JSONObject> listError() {
        return SystemConstants.ERROR_CONVERT_DATA.values();
    }

    @GetMapping("testJacob")
    public MessageBean<Boolean> testJacob(@RequestParam("input") String input, @RequestParam("output") String output)
            throws IOException {
        return MessageBean.success(LocalConvertDocUtil.process(input, output));
    }
}
