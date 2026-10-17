package com.gitee.apigrab.persistence.handler;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

public class FailureSamplesTypeHandler extends JacksonTypeHandler<List<Map<String, Object>>> {

    @Override
    protected TypeReference<List<Map<String, Object>>> type() {
        return new TypeReference<List<Map<String, Object>>>() {};
    }
}
