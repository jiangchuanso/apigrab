package com.gitee.apigrab.persistence.handler;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

public class ObjectMapTypeHandler extends JacksonTypeHandler<Map<String, Object>> {

    @Override
    protected TypeReference<Map<String, Object>> type() {
        return new TypeReference<Map<String, Object>>() {};
    }
}
