package com.thinkdifferent.convertpreview.engine.impl;

import com.thinkdifferent.convertpreview.engine.EngineService;
import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class AbstractEngineServiceImpl implements EngineService {
    {
       log.info("启用引擎：" + this.getClass().getCanonicalName());
    }
}
