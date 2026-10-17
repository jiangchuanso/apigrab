package com.gitee.apigrab.persistence.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.gitee.apigrab.persistence.entity.PageConfig;

public class PageConfigTypeHandler extends JacksonTypeHandler<PageConfig> {

    @Override
    protected TypeReference<PageConfig> type() {
        return new TypeReference<PageConfig>() {};
    }
}
