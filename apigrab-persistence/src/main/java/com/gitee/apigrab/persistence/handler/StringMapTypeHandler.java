package com.gitee.apigrab.persistence.handler;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

public class StringMapTypeHandler extends JacksonTypeHandler<Map<String, String>> {

    @Override
    protected TypeReference<Map<String, String>> type() {
        return new TypeReference<Map<String, String>>() {};
    }
}
