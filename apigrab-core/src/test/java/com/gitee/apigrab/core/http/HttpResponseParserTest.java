package com.gitee.apigrab.core.http;

import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.RespType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpResponseParserTest {

    private final HttpResponseParser parser = new HttpResponseParser();

    @Test
    void testParseJsonArray() {
        String json = "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setDataPath("$");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).get("name"));
        assertEquals(2, result.get(1).get("id"));
    }

    @Test
    void testParseJsonObject() {
        String json = "{\"id\":1,\"name\":\"Alice\"}";
        ApiConfigEntity config = new ApiConfigEntity();

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).get("name"));
    }

    @Test
    void testParseWithDataPath() {
        String json = "{\"code\":0,\"data\":{\"list\":[{\"x\":1},{\"x\":2}]}}";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setDataPath("$.data.list");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).get("x"));
    }

    @Test
    void testParseWithNestedObjectDataPath() {
        String json = "{\"result\":{\"items\":{\"total\":100,\"rows\":[{\"a\":1}]}}}";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setDataPath("$.result.items.rows");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("a"));
    }

    @Test
    void testParseWithObjectAtDataPath() {
        String json = "{\"data\":{\"user\":{\"id\":5,\"email\":\"test@test.com\"}}}";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setDataPath("$.data.user");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(1, result.size());
        assertEquals("test@test.com", result.get(0).get("email"));
    }

    @Test
    void testParseEmptyDataPath() {
        String json = "{\"key\":\"value\"}";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setDataPath("");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(1, result.size());
        assertEquals("value", result.get(0).get("key"));
    }

    @Test
    void testParseNullDataPath() {
        String json = "{\"key\":\"value\"}";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setDataPath(null);

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(1, result.size());
    }

    @Test
    void testParseXmlWithXPathAttributes() {
        String xml = "<root><items>"
            + "<item id='1'><name>Alice</name></item>"
            + "<item id='2'><name>Bob</name></item>"
            + "</items></root>";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.XML);
        config.setDataPath("//items/item");

        List<Map<String, Object>> result = parser.parse(xml, config);
        assertEquals(2, result.size());
        assertEquals("1", result.get(0).get("@id"));
        assertEquals("Alice", result.get(0).get("name"));
        assertEquals("Bob", result.get(1).get("name"));
    }

    @Test
    void testParseXmlRepeatedChildToLIist() {
        String xml = "<user><tag>a</tag><tag>b</tag><name>Tom</name></user>";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.XML);
        config.setDataPath("//user");

        List<Map<String, Object>> result = parser.parse(xml, config);
        assertEquals(1, result.size());
        assertEquals("Tom", result.get(0).get("name"));
        assertTrue(result.get(0).get("tag") instanceof List);
        assertEquals(2, ((List<?>) result.get(0).get("tag")).size());
    }

    @Test
    void testParseXmlNestedStructure() {
        String xml = "<order><customer><name>Li</name><city>SH</city></customer><amount>10</amount></order>";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.XML);
        config.setDataPath("//order");

        List<Map<String, Object>> result = parser.parse(xml, config);
        assertEquals(1, result.size());
        Object customer = result.get(0).get("customer");
        assertTrue(customer instanceof Map);
        assertEquals("Li", ((Map<?, ?>) customer).get("name"));
        assertEquals("10", result.get(0).get("amount"));
    }

    @Test
    void testParseXmlEmptyDataPathUsesRoot() {
        String xml = "<root><a>1</a></root>";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.XML);
        config.setDataPath("");

        List<Map<String, Object>> result = parser.parse(xml, config);
        assertEquals(1, result.size());
        assertEquals("1", result.get(0).get("a"));
    }

    @Test
    void testParseXmlRootText() {
        String xml = "<root>hello</root>";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.XML);

        List<Map<String, Object>> result = parser.parse(xml, config);
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).get("#text"));
    }

    @Test
    void testExitTypeJsonUnaffected() {
        String json = "[{\"id\":1}]";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.JSON);
        config.setDataPath("$");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("id"));
    }

    @Test
    void testNdjsonMultiline() {
        String ndjson = "{\"id\":1}\n{\"id\":2}\n";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.JSON);
        config.setEnableMultilines(1);

        List<Map<String, Object>> result = parser.parse(ndjson, config);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).get("id"));
        assertEquals(2, result.get(1).get("id"));
    }

@Test
    void testSinglePathDeepNesting() {
        String json = "{\"code\":0,\"data\":{\"result\":{\"payload\":{\"rows\":[{\"a\":1},{\"a\":2}]}}}}";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.JSON);
        config.setDataPath("$.data.result.payload.rows");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).get("a"));
        assertEquals(2, result.get(1).get("a"));
    }

    @Test
    void testArrayExpansionSinglePath() {
        String json = "{\"code\":0,\"data\":[{\"items\":[{\"sku\":\"X\"}]},{\"items\":[{\"sku\":\"Y\"}]}]}";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.JSON);
        config.setDataPath("$.data[*].items[*]");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(2, result.size());
        assertEquals("X", result.get(0).get("sku"));
        assertEquals("Y", result.get(1).get("sku"));
    }

    @Test
    void testArrayExpansionWithoutTrailingWildcardYieldsNestedArrays() {
        String json = "{\"code\":0,\"data\":[{\"items\":[{\"sku\":\"X\"}]},{\"items\":[{\"sku\":\"Y\"}]}]}";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.JSON);
        config.setDataPath("$.data[*].items");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(2, result.size());
        assertTrue(result.get(0) instanceof java.util.List,
                "缺少尾段 [*] 时 Jayway 不展平，每条记录仍是数组而非 Map");
    }

    @Test
    void testPathPointsToObjectYieldsSingleRecord() {
        String json = "{\"code\":0,\"data\":{\"user\":{\"id\":7}}}";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.JSON);
        config.setDataPath("$.data.user");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(1, result.size());
        assertEquals(7, result.get(0).get("id"));
    }

    @Test
    void testPathPointsToScalarYieldsEmptyList() {
        String json = "{\"code\":0}";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.JSON);
        config.setDataPath("$.code");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertTrue(result.isEmpty());
    }

    @Test
    void testInvalidJsonPathReportsLanguage() {
        String json = "{\"code\":0}";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.JSON);
        config.setDataPath("$.missing[");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> parser.parse(json, config));
        assertTrue(ex.getMessage().contains("路径解析失败"));
        assertTrue(ex.getMessage().contains("JSONPath"));
        assertTrue(ex.getMessage().contains("响应格式=JSON"));
        assertTrue(ex.getMessage().contains("$.missing["));
    }

    @Test
    void testInvalidXPathReportsLanguage() {
        String xml = "<r><items><i/></items></r>";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.XML);
        config.setDataPath("//items[");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> parser.parse(xml, config));
        assertTrue(ex.getMessage().contains("路径解析失败"));
        assertTrue(ex.getMessage().contains("XPath"));
        assertTrue(ex.getMessage().contains("响应格式=XML"));
        assertTrue(ex.getMessage().contains("//items["));
    }

    @Test
    void testBlankPathTreatedAsEmptyForJson() {
        String json = "{\"code\":0,\"data\":1}";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.JSON);
        config.setDataPath("   ");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).get("code"));
        assertEquals(1, result.get(0).get("data"));
    }

    @Test
    void testBlankPathTreatedAsEmptyForXml() {
        String xml = "<r><code>0</code></r>";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.XML);
        config.setDataPath("  ");

        List<Map<String, Object>> result = parser.parse(xml, config);
        assertEquals(1, result.size());
        assertEquals("0", result.get(0).get("code"));
    }

    @Test
    void testNdjsonWithNonEmptyPath() {
        String ndjson = "{\"data\":{\"id\":1}}\n{\"data\":{\"id\":2}}\n";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.JSON);
        config.setEnableMultilines(1);
        config.setDataPath("$.data");

        List<Map<String, Object>> result = parser.parse(ndjson, config);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).get("id"));
        assertEquals(2, result.get(1).get("id"));
    }

    @Test
    void testScalarNumberArrayNormalizedToValueRecords() {
        String json = "[1,2,3]";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.JSON);
        config.setDataPath("$");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(3, result.size());
        assertEquals(1, result.get(0).get(HttpResponseParser.SCALAR_VALUE_KEY));
        assertEquals(3, result.get(2).get(HttpResponseParser.SCALAR_VALUE_KEY));
    }

    @Test
    void testStringArrayNormalizedToValueRecords() {
        String json = "[\"a\",\"b\"]";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.JSON);
        config.setDataPath("$");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(2, result.size());
        assertEquals("a", result.get(0).get(HttpResponseParser.SCALAR_VALUE_KEY));
        assertEquals("b", result.get(1).get(HttpResponseParser.SCALAR_VALUE_KEY));
    }

    @Test
    void testScalarArrayAtNestedPathNormalized() {
        String json = "{\"code\":0,\"data\":{\"ids\":[10,20]}}";
        ApiConfigEntity config = new ApiConfigEntity();
        config.setRespType(RespType.JSON);
        config.setDataPath("$.data.ids");

        List<Map<String, Object>> result = parser.parse(json, config);
        assertEquals(2, result.size());
        assertEquals(10, result.get(0).get(HttpResponseParser.SCALAR_VALUE_KEY));
        assertEquals(20, result.get(1).get(HttpResponseParser.SCALAR_VALUE_KEY));
    }
}
