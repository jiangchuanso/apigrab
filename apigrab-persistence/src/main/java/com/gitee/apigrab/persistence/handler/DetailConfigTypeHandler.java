package com.gitee.apigrab.persistence.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.gitee.apigrab.persistence.entity.DetailConfig;

public class DetailConfigTypeHandler extends JacksonTypeHandler<DetailConfig> {

    @Override
    protected TypeReference<DetailConfig> type() {
        return new TypeReference<DetailConfig>() {};
    }
}
