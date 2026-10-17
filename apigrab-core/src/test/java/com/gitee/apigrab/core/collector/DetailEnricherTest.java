package com.gitee.apigrab.core.collector;

import com.gitee.apigrab.core.http.HttpFetcher;
import com.gitee.apigrab.core.http.HttpResponseParser;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.DetailConfig;
import com.gitee.apigrab.persistence.entity.enums.BodyType;
import com.gitee.apigrab.persistence.entity.enums.HttpMethod;
import com.gitee.apigrab.persistence.entity.enums.Stage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DetailEnricherTest {

    private HttpFetcher httpFetcher;
    private HttpResponseParser responseParser;

    private DetailEnricher newEnricher() throws Exception {
        httpFetcher = mock(HttpFetcher.class);
        responseParser = mock(HttpResponseParser.class);
        DetailEnricher enricher = new DetailEnricher();
        setField(enricher, "httpFetcher", httpFetcher);
        setField(enricher, "responseParser", responseParser);
        return enricher;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field f = DetailEnricher.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private ApiConfigEntity config(DetailConfig dc) {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setDetailConfig(dc);
        return config;
    }

    private DetailConfig enabledDetail(String keyField) {
        DetailConfig dc = new DetailConfig();
        dc.setEnabled(1);
        dc.setKeyField(keyField);
        dc.setUrl("https://api.example.com/detail/${id}");
        dc.setMethod(HttpMethod.GET);
        dc.setItemPath("$.data");
        dc.setConcurrency(4);
        return dc;
    }

    @Test
    void mergesDetailTopLevelWithDetailWinningOnCollision() throws Exception {
        DetailEnricher enricher = newEnricher();
        DetailConfig dc = enabledDetail("id");
        StageMetrics stage = new StageMetrics(Stage.DETAIL, 10);

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", 1);
        record.put("name", "listName");
        List<Map<String, Object>> records = new ArrayList<>(Collections.singletonList(record));

        // 详情抽到 itemPath 后返回一条：同名 name 覆盖，新增 age
        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("detail-body");
        when(responseParser.parse(eq("detail-body"), any(ApiConfigEntity.class)))
                .thenReturn(Collections.singletonList(detailRecord("name", "detailName", "age", 20)));

        enricher.enrich(records, config(dc), stage);

        assertEquals("detailName", record.get("name"), "同名以详情为准");
        assertEquals(20, record.get("age"), "缺失字段由详情补上");
        assertEquals(1, record.get("id"), "列表独有字段保留");
        assertEquals(1, stage.getSuccessCount());
        assertEquals(0, stage.getFailCount());
    }

    @Test
    void singleFailureKeepsRecordAndRecordsFailureSample() throws Exception {
        DetailEnricher enricher = newEnricher();
        DetailConfig dc = enabledDetail("id");
        StageMetrics stage = new StageMetrics(Stage.DETAIL, 10);

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", 5);
        List<Map<String, Object>> records = new ArrayList<>(Collections.singletonList(record));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenThrow(new RuntimeException("boom"));

        enricher.enrich(records, config(dc), stage);

        assertEquals(1, record.size(), "失败时列表行原样保留");
        assertEquals(5, record.get("id"));
        assertEquals(0, stage.getSuccessCount());
        assertEquals(1, stage.getFailCount());
        assertFalse(stage.getFailureCollector().isEmpty());
    }

    @Test
    void emptyKeyFailsWithoutIssuingRequest() throws Exception {
        DetailEnricher enricher = newEnricher();
        DetailConfig dc = enabledDetail("#this");
        StageMetrics stage = new StageMetrics(Stage.DETAIL, 10);

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("x", 1); // 无 _value
        List<Map<String, Object>> records = new ArrayList<>(Collections.singletonList(record));

        enricher.enrich(records, config(dc), stage);

        verify(httpFetcher, never()).fetch(any(ApiConfigEntity.class));
        assertEquals(1, stage.getFailCount());
    }

    @Test
    void formBodyKeyInjectedEvenWhenPlaceholderIsFormEncoded() throws Exception {
        DetailEnricher enricher = newEnricher();
        DetailConfig dc = enabledDetail("id");
        dc.setMethod(HttpMethod.POST);
        dc.setBodyType(BodyType.FORM);
        dc.setBodyContent("id=%24%7Bid%7D&scope=full");
        StageMetrics stage = new StageMetrics(Stage.DETAIL, 10);

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", 99);
        List<Map<String, Object>> records = new ArrayList<>(Collections.singletonList(record));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("detail-body");
        when(responseParser.parse(anyString(), any(ApiConfigEntity.class)))
                .thenReturn(Collections.singletonList(detailRecord("age", 7)));

        enricher.enrich(records, config(dc), stage);

        org.mockito.ArgumentCaptor<ApiConfigEntity> captor = org.mockito.ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher).fetch(captor.capture());
        assertEquals("id=99&scope=full", captor.getValue().getBodyContent(),
                "编码态的 ${id} 应被识别并替换，且注入值按表单规则编码");
        assertEquals(BodyType.FORM, captor.getValue().getBodyType());
        assertEquals(7, record.get("age"));
    }

    @Test
    void formBodyKeyValueWithReservedCharactersIsEncodedOnce() throws Exception {
        DetailEnricher enricher = newEnricher();
        DetailConfig dc = enabledDetail("id");
        dc.setMethod(HttpMethod.POST);
        dc.setBodyType(BodyType.FORM);
        dc.setBodyContent("id=%24%7Bid%7D&scope=full");
        StageMetrics stage = new StageMetrics(Stage.DETAIL, 10);

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", "a&b=c");
        List<Map<String, Object>> records = new ArrayList<>(Collections.singletonList(record));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("detail-body");
        when(responseParser.parse(anyString(), any(ApiConfigEntity.class)))
                .thenReturn(Collections.singletonList(detailRecord("age", 7)));

        enricher.enrich(records, config(dc), stage);

        org.mockito.ArgumentCaptor<ApiConfigEntity> captor = org.mockito.ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher).fetch(captor.capture());
        assertEquals("id=a%26b%3Dc&scope=full", captor.getValue().getBodyContent(),
                "键值里的 &/= 必须编码，否则会被拆成额外字段");
    }

    @Test
    void scalarValueKeyInjectedIntoUrl() throws Exception {
        DetailEnricher enricher = newEnricher();
        DetailConfig dc = enabledDetail("#this");
        StageMetrics stage = new StageMetrics(Stage.DETAIL, 10);

        Map<String, Object> record = new LinkedHashMap<>();
        record.put(HttpResponseParser.SCALAR_VALUE_KEY, 99);
        List<Map<String, Object>> records = new ArrayList<>(Collections.singletonList(record));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("detail-body");
        when(responseParser.parse(anyString(), any(ApiConfigEntity.class)))
                .thenReturn(Collections.singletonList(detailRecord("age", 7)));

        enricher.enrich(records, config(dc), stage);

        org.mockito.ArgumentCaptor<ApiConfigEntity> captor = org.mockito.ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher).fetch(captor.capture());
        assertEquals("https://api.example.com/detail/99", captor.getValue().getUrl());
        assertEquals(7, record.get("age"));
    }

    @Test
    void concurrentEnrichmentRoutesEachDetailToItsOwnRecord() throws Exception {
        DetailEnricher enricher = newEnricher();
        DetailConfig dc = enabledDetail("id");
        StageMetrics stage = new StageMetrics(Stage.DETAIL, 10);

        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", i);
            records.add(r);
        }

        // 让先提交的记录更慢完成，制造乱序；返回请求 URL 作为 body
        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenAnswer(invocation -> {
            ApiConfigEntity req = invocation.getArgument(0);
            String url = req.getUrl();
            int id = Integer.parseInt(url.substring(url.lastIndexOf('/') + 1));
            Thread.sleep((7 - id) * 40L);
            return url;
        });
        when(responseParser.parse(anyString(), any(ApiConfigEntity.class))).thenAnswer(invocation ->
                Collections.singletonList(detailRecord("detailUrl", invocation.getArgument(0))));

        enricher.enrich(records, config(dc), stage);

        for (int i = 1; i <= 6; i++) {
            Map<String, Object> r = records.get(i - 1);
            assertEquals(i, r.get("id"));
            assertEquals("https://api.example.com/detail/" + i, r.get("detailUrl"), "每条记录拿到自己的详情");
        }
        assertEquals(6, stage.getSuccessCount());
    }

    @Test
    void disabledConfigReturnsUnchangedWithoutRequests() throws Exception {
        DetailEnricher enricher = newEnricher();
        DetailConfig dc = enabledDetail("id");
        dc.setEnabled(0);
        StageMetrics stage = new StageMetrics(Stage.DETAIL, 10);

        List<Map<String, Object>> records = new ArrayList<>();
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", 1);
        records.add(record);

        List<Map<String, Object>> out = enricher.enrich(records, config(dc), stage);

        assertSame(records, out);
        verifyNoInteractions(httpFetcher);
        assertEquals(0, stage.getInputCount());
    }

    @Test
    void nullDetailConfigReturnsUnchanged() throws Exception {
        DetailEnricher enricher = newEnricher();
        StageMetrics stage = new StageMetrics(Stage.DETAIL, 10);
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(new LinkedHashMap<>());

        List<Map<String, Object>> out = enricher.enrich(records, new ApiConfigEntity(), stage);

        assertSame(records, out);
        verifyNoInteractions(httpFetcher);
    }

    private static Map<String, Object> detailRecord(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
