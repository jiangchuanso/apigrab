package com.gitee.apigrab.core.transform;

import com.gitee.apigrab.persistence.entity.enums.TransformType;

import java.util.List;
import java.util.Map;

public class BuiltinTransformer implements DataTransformer {

    @Override
    public boolean supports(TransformType transformType) {
        return transformType == null || transformType == TransformType.FIELD_MAPPING;
    }

    @Override
    public List<Map<String, Object>> transform(List<Map<String, Object>> records, String script) {
        return records;
    }
}
