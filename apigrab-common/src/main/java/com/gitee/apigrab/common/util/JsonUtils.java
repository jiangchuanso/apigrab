package com.gitee.apigrab.common.util;

import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@Slf4j
public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON serialization failed", e);
            throw new RuntimeException("JSON 序列化失败: " + e.getMessage(), e);
        }
    }

    public static <T> T parse(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON deserialization failed", e);
            throw new RuntimeException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }

    public static <T> T parse(String json, TypeReference<T> typeRef) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.error("JSON deserialization failed", e);
            throw new RuntimeException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }

    public static Map<String, Object> parseMap(String json) {
        return parse(json, new TypeReference<Map<String, Object>>() {});
    }

    public static Map<String, String> parseStringMap(String json) {
        return parse(json, new TypeReference<Map<String, String>>() {});
    }

    public static List<Map<String, Object>> parseList(String json) {
        return parse(json, new TypeReference<List<Map<String, Object>>>() {});
    }
}
