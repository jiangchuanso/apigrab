package com.gitee.apigrab.core.transform;

import com.gitee.apigrab.persistence.entity.enums.TransformType;
import lombok.extern.slf4j.Slf4j;
import com.gitee.apigrab.common.util.JsonUtils;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GroovyTransformer implements DataTransformer {

    @Override
    public boolean supports(TransformType transformType) {
        return transformType == TransformType.GROOVY;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> transform(List<Map<String, Object>> records, String script) {
        if (StringUtils.isBlank(script)) {
            return records;
        }

        try {
            Binding binding = new Binding();
            binding.setVariable("records", records);
            binding.setVariable("log", log);
            binding.setVariable("json", new GroovyJsonHelper());

            GroovyShell shell = new GroovyShell(binding);
            Object result = shell.evaluate(script);

            if (result instanceof List) {
                return (List<Map<String, Object>>) result;
            } else {
                log.warn("Groovy script did not return a List, returning original records");
                return records;
            }
        } catch (Exception e) {
            log.error("Failed to execute Groovy transform script", e);
            throw new RuntimeException("Groovy script execution failed: " + e.getMessage(), e);
        }
    }

    public static class GroovyJsonHelper {

        public String toJson(Object obj) {
            return JsonUtils.toJson(obj);
        }

        public <T> T fromJson(String json, Class<T> clazz) {
            return JsonUtils.parse(json, clazz);
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> parseMap(String json) {
            return fromJson(json, Map.class);
        }

        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> parseList(String json) {
            return fromJson(json, List.class);
        }
    }
}
