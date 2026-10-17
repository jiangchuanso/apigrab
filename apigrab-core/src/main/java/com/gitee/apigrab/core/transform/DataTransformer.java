package com.gitee.apigrab.core.transform;

import com.gitee.apigrab.persistence.entity.enums.TransformType;

import java.util.List;
import java.util.Map;

public interface DataTransformer {

    boolean supports(TransformType transformType);

    List<Map<String, Object>> transform(List<Map<String, Object>> records, String script);
}
