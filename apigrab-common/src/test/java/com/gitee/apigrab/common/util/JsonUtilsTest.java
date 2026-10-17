package com.gitee.apigrab.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

    @Test
    void testToJson() {
        String json = JsonUtils.toJson(new TestBean("hello", 42));
        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"hello\""));
        assertTrue(json.contains("\"value\":42"));
    }

    @Test
    void testToJsonNull() {
        // Jackson ObjectMapper.writeValueAsString(null) returns the string "null"
        // The try-catch in toJson catches JsonProcessingException only, so it returns "null"
        String json = JsonUtils.toJson(null);
        // ObjectMapper writes null as the literal string "null"
        assertEquals("null", json);
    }

    @Test
    void testParse() {
        String json = "{\"name\":\"test\",\"value\":100}";
        TestBean bean = JsonUtils.parse(json, TestBean.class);
        assertNotNull(bean);
        assertEquals("test", bean.name);
        assertEquals(100, bean.value);
    }

    @Test
    void testParseInvalid() {
        assertThrows(RuntimeException.class, () -> JsonUtils.parse("{invalid}", TestBean.class));
    }

    @Test
    void testParseMap() {
        String json = "{\"key1\":\"val1\",\"key2\":123}";
        Map<String, Object> map = JsonUtils.parseMap(json);
        assertNotNull(map);
        assertEquals("val1", map.get("key1"));
        assertEquals(123, map.get("key2"));
    }

    @Test
    void testParseList() {
        String json = "[{\"id\":1},{\"id\":2}]";
        List<Map<String, Object>> list = JsonUtils.parseList(json);
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).get("id"));
    }

    @Test
    void testParseListInvalid() {
        assertThrows(RuntimeException.class, () -> JsonUtils.parseList("not_a_list"));
    }

    @Test
    void testParseMapInvalid() {
        assertThrows(RuntimeException.class, () -> JsonUtils.parseMap("not json"));
    }

    @Test
    void testParseBlankReturnsNull() {
        assertNull(JsonUtils.parseMap(null));
        assertNull(JsonUtils.parseMap(""));
    }

    @Test
    void testParseGenericTypeRef() {
        String json = "{\"code\":0,\"message\":\"成功\",\"data\":{\"id\":1}}";
        java.util.Map<String, Object> result = JsonUtils.parse(json, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
        assertNotNull(result);
        assertEquals(0, result.get("code"));
    }

    static class TestBean {
        public String name;
        public int value;
        public TestBean() {}
        public TestBean(String name, int value) { this.name = name; this.value = value; }
    }
}
