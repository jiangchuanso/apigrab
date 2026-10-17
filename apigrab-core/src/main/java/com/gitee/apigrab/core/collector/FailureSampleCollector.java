package com.gitee.apigrab.core.collector;

import com.gitee.apigrab.common.util.JsonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects a bounded number of failure samples for a stage.
 * Each sample holds a reason and a truncated source record snippet.
 * Observation-only: never throws on serialization failure.
 */
public class FailureSampleCollector {

    private static final int MAX_TEXT_LEN = 500;

    private final int limit;
    private final List<Map<String, Object>> samples = new ArrayList<>();

    public FailureSampleCollector(int limit) {
        this.limit = Math.max(0, limit);
    }

    public void add(String reason, Object sourceRecord) {
        if (samples.size() >= limit) {
            return;
        }
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("reason", truncate(reason));
        sample.put("sourceRecord", truncate(serialize(sourceRecord)));
        samples.add(sample);
    }

    public boolean isEmpty() {
        return samples.isEmpty();
    }

    public List<Map<String, Object>> getSamples() {
        return samples;
    }

    public String toJson() {
        if (samples.isEmpty()) {
            return null;
        }
        try {
            return JsonUtils.toJson(samples);
        } catch (Exception e) {
            return null;
        }
    }

    private String serialize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence) {
            return value.toString();
        }
        try {
            return JsonUtils.toJson(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > MAX_TEXT_LEN ? text.substring(0, MAX_TEXT_LEN) : text;
    }
}
