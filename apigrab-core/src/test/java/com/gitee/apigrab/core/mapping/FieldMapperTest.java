package com.gitee.apigrab.core.mapping;

import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.TargetType;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FieldMapperTest {

    private final FieldMapper fieldMapper = new FieldMapper();

    private FieldMappingEntity mapping(String sourceField, String targetField, String targetType,
                                        String defaultValue, String convertExpr) {
        FieldMappingEntity m = new FieldMappingEntity();
        m.setSourceField(sourceField);
        m.setTargetField(targetField);
        m.setTargetType(targetType == null ? null : TargetType.valueOf(targetType));
        m.setDefaultValue(defaultValue);
        m.setConvertExpr(convertExpr);
        m.setSortOrder(0);
        return m;
    }

    @Test
    void testMapFields() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(mapping("name", "user_name", "VARCHAR", null, null));
        mappings.add(mapping("age", "user_age", "BIGINT", null, "to_int"));

        List<Map<String, Object>> records = new ArrayList<>();
        Map<String, Object> record = new HashMap<>();
        record.put("name", "Alice");
        record.put("age", "25");
        records.add(record);

        List<Map<String, Object>> result = fieldMapper.mapFields(records, mappings);
        assertEquals(1, result.size());
        Map<String, Object> mapped = result.get(0);
        assertEquals("Alice", mapped.get("user_name"));
        assertEquals(25, mapped.get("user_age")); // converted to int by to_int
        assertNull(mapped.get("name")); // original field not included
    }

    @Test
    void testMapFieldsWithDefaultValue() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(mapping("missing", "field", "VARCHAR", "default_val", null));

        List<Map<String, Object>> records = Collections.singletonList(new HashMap<>());
        List<Map<String, Object>> result = fieldMapper.mapFields(records, mappings);
        assertEquals("default_val", result.get(0).get("field"));
    }

    @Test
    void testMapFieldsWithJsonPath() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(mapping("$.user.name", "name", "VARCHAR", null, null));
        mappings.add(mapping("$.user.address.city", "city", "VARCHAR", null, null));

        Map<String, Object> nested = new HashMap<>();
        Map<String, Object> user = new HashMap<>();
        user.put("name", "Bob");
        Map<String, Object> address = new HashMap<>();
        address.put("city", "Beijing");
        user.put("address", address);
        nested.put("user", user);

        List<Map<String, Object>> records = Collections.singletonList(nested);
        List<Map<String, Object>> result = fieldMapper.mapFields(records, mappings);
        assertEquals("Bob", result.get(0).get("name"));
        assertEquals("Beijing", result.get(0).get("city"));
    }

    @Test
    void testMapFieldsWithNullMappings() {
        List<Map<String, Object>> records = Collections.singletonList(Collections.singletonMap("a", 1));
        List<Map<String, Object>> result = fieldMapper.mapFields(records, null);
        assertSame(records, result);
    }

    @Test
    void testMapFieldsWithEmptyMappings() {
        List<Map<String, Object>> records = Collections.singletonList(Collections.singletonMap("a", 1));
        List<Map<String, Object>> result = fieldMapper.mapFields(records, Collections.emptyList());
        assertSame(records, result);
    }

    @Test
    void testMapFieldsWithTrimConvert() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(mapping("name", "name", "VARCHAR", null, "trim"));

        List<Map<String, Object>> records = Collections.singletonList(Collections.singletonMap("name", "  Hello  "));
        List<Map<String, Object>> result = fieldMapper.mapFields(records, mappings);
        assertEquals("Hello", result.get(0).get("name"));
    }

    @Test
    void testMapFieldsWithUpperCaseConvert() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(mapping("code", "code", "VARCHAR", null, "upper"));

        List<Map<String, Object>> records = Collections.singletonList(Collections.singletonMap("code", "abc"));
        List<Map<String, Object>> result = fieldMapper.mapFields(records, mappings);
        assertEquals("ABC", result.get(0).get("code"));
    }

    @Test
    void testMapFieldsWithToIntOnNonNumericValueDoesNotFail() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(mapping("age", "user_age", "BIGINT", null, "to_int"));

        List<Map<String, Object>> records = Collections.singletonList(
            Collections.singletonMap("age", "not-a-number"));

        List<Map<String, Object>> result = fieldMapper.mapFields(records, mappings);
        assertEquals(1, result.size());
        assertNull(result.get(0).get("user_age"), "无法解析为数字的字段应取空值而非中断采集");
    }

    @Test
    void testMapFieldsFromRawJson() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(mapping("name", "user_name", "VARCHAR", null, null));
        mappings.add(mapping("age", "user_age", "BIGINT", null, null));

        String rawJson = "{\"name\":\"Alice\",\"age\":30}";
        List<Map<String, Object>> result = fieldMapper.mapFieldsFromRawJson(rawJson, mappings);
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).get("user_name"));
        // JSON integer literal is parsed as Long
        assertEquals(30L, result.get(0).get("user_age"));
    }

    @Test
    void testMapFieldsFromRawJsonEmptyMappings() {
        String rawJson = "{\"a\":1}";
        List<Map<String, Object>> result = fieldMapper.mapFieldsFromRawJson(rawJson, Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    void testMapFieldsFromRawJsonInvalidJson() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(mapping("name", "name", "VARCHAR", null, null));
        List<Map<String, Object>> result = fieldMapper.mapFieldsFromRawJson("{invalid}", mappings);
        assertTrue(result.isEmpty());
    }

    @Test
    void testMapFieldsWithJsonPathNotFoundReturnsNull() {
        List<FieldMappingEntity> mappings = new ArrayList<>();
        mappings.add(mapping("$.nonexistent.path", "field", "VARCHAR", "fallback", null));

        List<Map<String, Object>> records = Collections.singletonList(Collections.singletonMap("a", 1));
        List<Map<String, Object>> result = fieldMapper.mapFields(records, mappings);
        assertEquals("fallback", result.get(0).get("field"));
    }
}
