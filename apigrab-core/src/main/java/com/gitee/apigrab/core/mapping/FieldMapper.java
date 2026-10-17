package com.gitee.apigrab.core.mapping;

import lombok.extern.slf4j.Slf4j;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.gitee.apigrab.core.mapping.builtin.BuiltinConvertFunctions;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class FieldMapper {
    private static final net.minidev.json.parser.JSONParser parser = new net.minidev.json.parser.JSONParser(
        net.minidev.json.parser.JSONParser.MODE_JSON_SIMPLE);

    public List<Map<String, Object>> mapFields(List<Map<String, Object>> records, List<FieldMappingEntity> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return records;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> record : records) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            for (FieldMappingEntity mapping : mappings) {
                Object value = extractValue(record, mapping);
                mapped.put(mapping.getTargetField(), value);
            }
            result.add(mapped);
        }
        return result;
    }

    public List<Map<String, Object>> mapFieldsFromRawJson(String rawJson, List<FieldMappingEntity> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Object parsed = parser.parse(rawJson);
            Map<String, Object> record;
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) parsed;
                record = m;
            } else {
                record = new HashMap<>();
                record.put("_root", parsed);
            }

            Map<String, Object> mapped = new LinkedHashMap<>();
            for (FieldMappingEntity mapping : mappings) {
                Object value = extractValue(record, mapping);
                mapped.put(mapping.getTargetField(), value);
            }

            List<Map<String, Object>> result = new ArrayList<>();
            result.add(mapped);
            return result;
        } catch (Exception e) {
            log.error("Failed to parse raw JSON for field mapping", e);
            return new ArrayList<>();
        }
    }

    private Object extractValue(Map<String, Object> record, FieldMappingEntity mapping) {
        String sourceField = mapping.getSourceField();
        Object value = null;

        if (sourceField != null && !sourceField.isEmpty()) {
            if (sourceField.startsWith("$")) {
                try {
                    value = JsonPath.read(record, sourceField);
                } catch (PathNotFoundException e) {
                    value = null;
                }
            } else {
                value = record.get(sourceField);
            }
        }

        // 默认值处理
        if (value == null && mapping.getDefaultValue() != null) {
            value = mapping.getDefaultValue();
        }

        // 转换表达式处理：单个字段转换失败（如数值无法解析）不得中断整批采集，
        // 降级为该字段取空值并告警——否则一条脏数据会把整次执行拖垮。
        String convertExpr = mapping.getConvertExpr();
        if (convertExpr != null && !convertExpr.isEmpty() && value != null) {
            ConvertFunction function = BuiltinConvertFunctions.getFunction(convertExpr);
            if (function != null) {
                try {
                    value = function.convert(value, null);
                } catch (Exception e) {
                    log.warn("转换表达式 '{}' 执行失败（源字段 {}, 值: {}），字段取空值: {}",
                            convertExpr, sourceField, value, e.getMessage());
                    value = null;
                }
            }
        }

        return value;
    }
}
