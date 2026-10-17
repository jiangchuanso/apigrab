package com.gitee.apigrab.core.collector;

import com.gitee.apigrab.core.http.HttpFetcher;
import com.gitee.apigrab.core.http.HttpResponseParser;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.PageConfig;
import com.gitee.apigrab.persistence.entity.enums.BodyType;
import com.gitee.apigrab.persistence.entity.enums.HttpMethod;
import com.gitee.apigrab.persistence.entity.enums.PaginationMode;
import com.gitee.apigrab.persistence.entity.enums.ParamLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PageCollectorTest {

    private PageCollector collector;
    private HttpFetcher httpFetcher;
    private HttpResponseParser responseParser;

    @BeforeEach
    void setUp() throws Exception {
        collector = new PageCollector();
        httpFetcher = Mockito.mock(HttpFetcher.class);
        responseParser = Mockito.mock(HttpResponseParser.class);

        java.lang.reflect.Field f1 = PageCollector.class.getDeclaredField("httpFetcher");
        f1.setAccessible(true);
        f1.set(collector, httpFetcher);

        java.lang.reflect.Field f2 = PageCollector.class.getDeclaredField("responseParser");
        f2.setAccessible(true);
        f2.set(collector, responseParser);
    }

    private PageConfig pc() {
        return new PageConfig();
    }

    @Test
    void testCollectAllNoPagination() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(0);

        when(httpFetcher.fetch(config)).thenReturn("{\"data\":[{\"id\":1}]}");
        Map<String, Object> record = new HashMap<>();
        record.put("id", 1);
        when(responseParser.parse(anyString(), eq(config))).thenReturn(Collections.singletonList(record));

        List<Map<String, Object>> result = collector.collectAll(config);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("id"));
    }

    @Test
    void testCollectAllWithPageInfo() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(0);

        when(httpFetcher.fetch(config)).thenReturn("{\"data\":[{\"id\":1}]}");
        when(responseParser.parse(anyString(), eq(config))).thenReturn(Collections.singletonList(new HashMap<>()));

        PageCollector.CollectAllResult result = collector.collectAllWithPageInfo(config);
        assertEquals(1, result.getRecords().size());
        assertEquals(1, result.getPageCount());
    }

    @Test
    void testCollectAllFetchFailed() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/fail");
        config.setPageEnabled(0);

        when(httpFetcher.fetch(config)).thenThrow(new RuntimeException("Connection refused"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> collector.collectAll(config));
        assertTrue(ex.getMessage().contains("API fetch failed"));
    }

    @Test
    void testCollectAllWithPageNumberPagination() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setPageSize(10);
        pc.setMaxPages(3);
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class)))
            .thenReturn("{\"data\":[{\"id\":1}]}")
            .thenReturn("{\"data\":[]}");
        when(responseParser.parse(eq("{\"data\":[{\"id\":1}]}"), any()))
            .thenReturn(Collections.singletonList(new HashMap<>()));
        when(responseParser.parse(eq("{\"data\":[]}"), any()))
            .thenReturn(Collections.emptyList());

        PageCollector.CollectAllResult result = collector.collectAllWithPageInfo(config);
        assertEquals(1, result.getRecords().size());
        assertEquals(1, result.getPageCount());
    }

    @Test
    void testCollectAllWithOffsetPagination() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.OFFSET);
        pc.setPageSize(10);
        pc.setMaxPages(2);
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class)))
            .thenReturn("{\"data\":[{\"id\":1}]}")
            .thenReturn("{\"data\":[]}");
        when(responseParser.parse(eq("{\"data\":[{\"id\":1}]}"), any()))
            .thenReturn(Collections.singletonList(new HashMap<>()));
        when(responseParser.parse(eq("{\"data\":[]}"), any()))
            .thenReturn(Collections.emptyList());

        PageCollector.CollectAllResult result = collector.collectAllWithPageInfo(config);
        assertEquals(1, result.getPageCount());
    }

    @Test
    void testCollectAllWithBodyJsonPageNumber() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setMethod(HttpMethod.POST);
        config.setBodyType(BodyType.JSON);
        config.setBodyContent("{\"foo\":\"bar\"}");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setParamLocation(ParamLocation.BODY);
        pc.setPageSize(10);
        pc.setMaxPages(5);
        pc.setStopByBatchSize(false);
        pc.setPageParam("pageNo");
        pc.setSizeParam("size");
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class)))
            .thenReturn("{\"data\":[{\"id\":1}]}")
            .thenReturn("{\"data\":[]}");
        when(responseParser.parse(eq("{\"data\":[{\"id\":1}]}"), any()))
            .thenReturn(Collections.singletonList(new HashMap<>()));
        when(responseParser.parse(eq("{\"data\":[]}"), any()))
            .thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, times(2)).fetch(captor.capture());
        String firstBody = captor.getAllValues().get(0).getBodyContent();
        String secondBody = captor.getAllValues().get(1).getBodyContent();
        assertTrue(firstBody.contains("\"pageNo\":\"1\""));
        assertTrue(firstBody.contains("\"size\":\"10\""));
        assertTrue(firstBody.contains("\"foo\":\"bar\""));
        assertTrue(secondBody.contains("\"pageNo\":\"2\""));
        assertEquals("https://api.example.com/data", captor.getAllValues().get(0).getUrl());
    }

    @Test
    void testCollectAllWithBodyFormPageNumber() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setMethod(HttpMethod.POST);
        config.setBodyType(BodyType.FORM);
        config.setBodyContent("a=1");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setParamLocation(ParamLocation.BODY);
        pc.setPageSize(10);
        pc.setMaxPages(5);
        pc.setPageParam("pageNo");
        pc.setSizeParam("size");
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        String body = captor.getAllValues().get(0).getBodyContent();
        assertTrue(body.contains("a=1"));
        assertTrue(body.contains("pageNo=1"));
        assertTrue(body.contains("size=10"));
    }

    @Test
    void testCollectAllWithBodyFormCursorEncodesInjectedValue() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setMethod(HttpMethod.POST);
        config.setBodyType(BodyType.FORM);
        config.setBodyContent("a=x%26y");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.CURSOR);
        pc.setParamLocation(ParamLocation.BODY);
        pc.setMaxPages(1);
        pc.setCursorParam("cursor");
        pc.setInitialCursor("p&q");
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        String body = captor.getAllValues().get(0).getBodyContent();
        assertTrue(body.contains("a=x%26y"), "已存在段应原样保留（其本身已是编码态）: " + body);
        assertTrue(body.contains("cursor=p%26q"), "注入值里的 & 应被编码为 %26: " + body);
    }

    @Test
    void testBodyFormPlaceholderReplacedEvenWhenFormEncoded() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setMethod(HttpMethod.POST);
        config.setBodyType(BodyType.FORM);
        // 前端按表单序列化后，值里的 ${pageNo}/${size} 以 %24%7B..%7D 落库；键名与参数名无关，
        // 因此修复前无法命中，只能退化为追加同名字段。
        config.setBodyContent("q=%24%7BpageNo%7D&size=%24%7Bsize%7D&a=1");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setParamLocation(ParamLocation.BODY);
        pc.setPageSize(10);
        pc.setMaxPages(1);
        pc.setPageParam("pageNo");
        pc.setSizeParam("size");
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        String body = captor.getAllValues().get(0).getBodyContent();
        assertEquals("q=1&size=10&a=1", body, "编码态占位符应按表单规则替换，且不残留/不重复");
    }

    @Test
    void testCollectAllWithBodyXmlPageNumberReplacesExistingElements() throws Exception {
        ApiConfigEntity config = xmlConfig(
            "<request><pageNo>0</pageNo><size>0</size><q>kw</q></request>");
        PageConfig pc = bodyPageNumberConfig("pageNo", "size", 10);
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class)))
            .thenReturn("<data><row/></data>")
            .thenReturn("<data/>");
        when(responseParser.parse(anyString(), any()))
            .thenReturn(Collections.singletonList(new HashMap<>()))
            .thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, times(2)).fetch(captor.capture());
        String firstBody = captor.getAllValues().get(0).getBodyContent();
        String secondBody = captor.getAllValues().get(1).getBodyContent();

        assertTrue(firstBody.contains("<pageNo>1</pageNo>"), "实际: " + firstBody);
        assertTrue(firstBody.contains("<size>10</size>"), "实际: " + firstBody);
        assertTrue(firstBody.contains("<q>kw</q>"), "未注入的节点必须保留，实际: " + firstBody);
        assertTrue(secondBody.contains("<pageNo>2</pageNo>"), "实际: " + secondBody);
    }

    @Test
    void testCollectAllWithBodyXmlAppendsWhenElementAbsent() throws Exception {
        ApiConfigEntity config = xmlConfig("<request><q>kw</q></request>");
        config.setPageConfig(bodyPageNumberConfig("pageNo", "size", 10));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("<data/>");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        String body = captor.getAllValues().get(0).getBodyContent();

        assertTrue(body.contains("<pageNo>1</pageNo>"), "无同名元素时应在根下新增，实际: " + body);
        assertTrue(body.contains("<size>10</size>"), "实际: " + body);
        assertTrue(body.contains("<q>kw</q>"), "实际: " + body);
    }

    /**
     * 关键回归：分页参数名与根标签同名时，根元素不得参与匹配，
     * 否则整份文档会被塌缩成一个元素（getElementsByTagName 会把根自身也算作匹配）。
     */
    @Test
    void testCollectAllWithBodyXmlRootElementIsNotCollapsed() throws Exception {
        ApiConfigEntity config = xmlConfig("<pageNo><q>kw</q></pageNo>");
        config.setPageConfig(bodyPageNumberConfig("pageNo", "size", 10));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("<data/>");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        String body = captor.getAllValues().get(0).getBodyContent();

        assertTrue(body.contains("<q>kw</q>"),
            "根元素被当作匹配项并塌缩，原内容丢失，实际: " + body);
    }

    @Test
    void testCollectAllWithBodyXmlNestedElementReplaced() throws Exception {
        ApiConfigEntity config = xmlConfig("<req><filter><pageNo>0</pageNo></filter></req>");
        config.setPageConfig(bodyPageNumberConfig("pageNo", "size", 10));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("<data/>");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        String body = captor.getAllValues().get(0).getBodyContent();

        assertTrue(body.contains("<filter><pageNo>1</pageNo></filter>"),
            "嵌套同名元素应就地替换，实际: " + body);
    }

    @Test
    void testCollectAllWithBodyXmlInjectsReplacementForElementWithChildren() throws Exception {
        // behavior B2：命中的元素若含子元素，其内容会被整体替换（与 JSON 语义对齐）
        ApiConfigEntity config = xmlConfig("<req><pageNo><cur>1</cur></pageNo></req>");
        config.setPageConfig(bodyPageNumberConfig("pageNo", "size", 10));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("<data/>");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        String body = captor.getAllValues().get(0).getBodyContent();

        assertTrue(body.contains("<pageNo>1</pageNo>"), "实际: " + body);
        assertFalse(body.contains("<cur>"), "子元素应随内容替换而消失，实际: " + body);
    }

    /** 非法 XML：不抛出到调用方，按既有分页循环语义记录并停止（与 JSON 分支一致）。 */
    @Test
    void testCollectAllWithBodyXmlMalformedStopsWithoutThrowing() throws Exception {
        ApiConfigEntity config = xmlConfig("<a><b></a>");
        config.setPageConfig(bodyPageNumberConfig("pageNo", "size", 10));

        PageCollector.CollectAllResult result = collector.collectAllWithPageInfo(config);

        assertEquals(0, result.getPageCount(), "解析失败应停止分页，不产生页数");
        verify(httpFetcher, never()).fetch(any(ApiConfigEntity.class));
    }

    private ApiConfigEntity xmlConfig(String body) {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setMethod(HttpMethod.POST);
        config.setBodyType(BodyType.XML);
        config.setBodyContent(body);
        config.setPageEnabled(1);
        return config;
    }

    private PageConfig bodyPageNumberConfig(String pageParam, String sizeParam, int pageSize) {
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setParamLocation(ParamLocation.BODY);
        pc.setPageSize(pageSize);
        pc.setMaxPages(5);
        pc.setStopByBatchSize(false);
        pc.setPageParam(pageParam);
        pc.setSizeParam(sizeParam);
        return pc;
    }

    @Test
    void testCollectAllWithBodyCursor() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setMethod(HttpMethod.POST);
        config.setBodyType(BodyType.JSON);
        config.setBodyContent("{}");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.CURSOR);
        pc.setParamLocation(ParamLocation.BODY);
        pc.setMaxPages(5);
        pc.setCursorPath("$.next");
        pc.setCursorParam("cursor");
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class)))
            .thenReturn("{\"next\":\"c2\"}")
            .thenReturn("{\"next\":null}");
        when(responseParser.parse(eq("{\"next\":\"c2\"}"), any()))
            .thenReturn(Collections.singletonList(new HashMap<>()));
        when(responseParser.parse(eq("{\"next\":null}"), any()))
            .thenReturn(Collections.emptyList());

        PageCollector.CollectAllResult result = collector.collectAllWithPageInfo(config);
        assertEquals(1, result.getPageCount());
    }

    @Test
    void testCollectAllBodyInvalidBodyTypeThrows() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setBodyType(BodyType.NONE);
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setParamLocation(ParamLocation.BODY);
        pc.setPageSize(10);
        pc.setMaxPages(2);
        config.setPageConfig(pc);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> collector.collectAllWithPageInfo(config));
        assertTrue(ex.getMessage().contains("JSON、FORM 或 XML"), "实际: " + ex.getMessage());
    }

    @Test
    void testCollectAllQueryLocationUnchanged() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setPageSize(10);
        pc.setMaxPages(5);
        pc.setParamLocation(ParamLocation.QUERY);
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        assertTrue(captor.getAllValues().get(0).getUrl().contains("page=1"));
    }

    /** URL 查询串已有同名分页参数（含重复项）：就地替换，不再追加。 */
    @Test
    void testQueryPaginationReplacesUrlQueryParamInPlace() throws Exception {
        ApiConfigEntity config = queryPaginationConfig("https://api.example.com/data?page=99&foo=bar&page=98");
        config.setPageConfig(queryPageNumberConfig("page", "size", 10));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        ApiConfigEntity paged = captor.getAllValues().get(0);
        // 同名重复项收敛为一份；其它参数与顺序不变，缺失的 size 才追加
        assertEquals("https://api.example.com/data?page=1&foo=bar&size=10", paged.getUrl());
    }

    /** 分页键只在 queryParams 中：更新该键，不同时写入 URL（否则 HttpRequestBuilder 会追加出重复参数）。 */
    @Test
    void testQueryPaginationReplacesQueryParamsEntry() throws Exception {
        ApiConfigEntity config = queryPaginationConfig("https://api.example.com/data?foo=bar");
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("page", "99");
        config.setQueryParams(queryParams);
        config.setPageConfig(queryPageNumberConfig("page", "size", 10));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        ApiConfigEntity paged = captor.getAllValues().get(0);
        assertEquals("1", paged.getQueryParams().get("page"));
        assertEquals("https://api.example.com/data?foo=bar&size=10", paged.getUrl(),
                "不得再向 URL 追加同名键；URL 与 queryParams 都没有 size 时才追加");
        // 原配置不被修改（每页都基于副本构建）
        assertEquals("99", config.getQueryParams().get("page"));
        assertEquals("https://api.example.com/data?foo=bar", config.getUrl());
    }

    /**
     * URL 与 queryParams 同时存在同名分页键：URL 中移除、queryParams 中更新。
     * HttpRequestBuilder 会把 queryParams 追加到 URL 后，最终请求只应有一个 page 参数。
     */
    @Test
    void testQueryPaginationDeduplicatesUrlAndQueryParams() throws Exception {
        ApiConfigEntity config = queryPaginationConfig("https://api.example.com/data?page=99&foo=bar");
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("page", "99");
        queryParams.put("size", "5");
        config.setQueryParams(queryParams);
        config.setPageConfig(queryPageNumberConfig("page", "size", 10));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        ApiConfigEntity paged = captor.getAllValues().get(0);

        assertEquals("https://api.example.com/data?foo=bar", paged.getUrl(),
            "URL 中的同名键应移除: " + paged.getUrl());
        assertEquals("1", paged.getQueryParams().get("page"));
        assertEquals("10", paged.getQueryParams().get("size"));

        // 模拟 HttpRequestBuilder：URL 查询串 + queryParams 追加后的最终效果
        String effective = paged.getUrl();
        for (Map.Entry<String, String> entry : paged.getQueryParams().entrySet()) {
            effective = effective + (effective.contains("?") ? "&" : "?") + entry.getKey() + "=" + entry.getValue();
        }
        assertEquals(1, countOccurrences(effective, "page="), "最终查询串中 page 只能出现一次: " + effective);
    }

    /**
     * 分页参数只以占位符出现在 queryParams（URL 里还有旧值）：
     * 占位符替换后仍须移除 URL 中的同名参数，否则最终请求会带两份 page。
     */
    @Test
    void testQueryPaginationPlaceholderInQueryParamsRemovesStaleUrlParam() throws Exception {
        ApiConfigEntity config = queryPaginationConfig("https://api.example.com/data?page=99&foo=bar");
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("page", "${page}");
        config.setQueryParams(queryParams);
        config.setPageConfig(queryPageNumberConfig("page", "size", 10));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        ApiConfigEntity paged = captor.getAllValues().get(0);
        assertEquals("1", paged.getQueryParams().get("page"));
        assertFalse(paged.getUrl().contains("page="), "URL 中残留的同名键应移除: " + paged.getUrl());
        assertEquals("https://api.example.com/data?foo=bar&size=10", paged.getUrl());
    }

    /** URL 中的参数名可能是百分号编码：仍应识别为同名并替换，而不是追加出第二份。 */
    @Test
    void testQueryPaginationMatchesPercentEncodedParamName() throws Exception {
        ApiConfigEntity config = queryPaginationConfig(
            "https://api.example.com/data?%E9%A1%B5%E7%A0%81=99");
        config.setPageConfig(queryPageNumberConfig("页码", "size", 10));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        String url = captor.getAllValues().get(0).getUrl();
        assertFalse(url.contains("%E9%A1%B5%E7%A0%81=99"), "旧值应被替换: " + url);
        assertTrue(url.contains("%E9%A1%B5%E7%A0%81=1"), "编码后的参数名应保留并替换取值: " + url);
    }

    private ApiConfigEntity queryPaginationConfig(String url) {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl(url);
        config.setPageEnabled(1);
        return config;
    }

    private PageConfig queryPageNumberConfig(String pageParam, String sizeParam, int pageSize) {
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setParamLocation(ParamLocation.QUERY);
        pc.setPageSize(pageSize);
        pc.setMaxPages(1);
        pc.setStopByBatchSize(false);
        pc.setPageParam(pageParam);
        pc.setSizeParam(sizeParam);
        return pc;
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    @Test
    void testCollectAllWithPathPageNumber() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data/${page}/${size}");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setParamLocation(ParamLocation.PATH);
        pc.setPageSize(10);
        pc.setMaxPages(2);
        pc.setStopByBatchSize(false);
        pc.setPageParam("page");
        pc.setSizeParam("size");
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class)))
            .thenReturn("{\"data\":[{\"id\":1}]}")
            .thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any()))
            .thenReturn(Collections.singletonList(new HashMap<>()))
            .thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, times(2)).fetch(captor.capture());
        assertEquals("https://api.example.com/data/1/10", captor.getAllValues().get(0).getUrl());
        assertEquals("https://api.example.com/data/2/10", captor.getAllValues().get(1).getUrl());
        assertEquals("https://api.example.com/data/${page}/${size}", config.getUrl(), "不得修改传入的配置");
    }

    @Test
    void testCollectAllWithPathOffset() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data/${offset}/${limit}");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.OFFSET);
        pc.setParamLocation(ParamLocation.PATH);
        pc.setPageSize(10);
        pc.setMaxPages(2);
        pc.setStopByBatchSize(false);
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class)))
            .thenReturn("{\"data\":[{\"id\":1}]}")
            .thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any()))
            .thenReturn(Collections.singletonList(new HashMap<>()))
            .thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, times(2)).fetch(captor.capture());
        assertEquals("https://api.example.com/data/0/10", captor.getAllValues().get(0).getUrl());
        assertEquals("https://api.example.com/data/10/10", captor.getAllValues().get(1).getUrl());
    }

    @Test
    void testCollectAllWithPathCursor() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data/${cursor}");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.CURSOR);
        pc.setParamLocation(ParamLocation.PATH);
        pc.setCursorParam("cursor");
        pc.setCursorPath("$.next");
        pc.setInitialCursor("c1");
        pc.setMaxPages(5);
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class)))
            .thenReturn("{\"next\":\"c2\"}")
            .thenReturn("{\"next\":\"\"}");
        when(responseParser.parse(anyString(), any()))
            .thenReturn(Collections.singletonList(new HashMap<>()));

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, times(2)).fetch(captor.capture());
        assertEquals("https://api.example.com/data/c1", captor.getAllValues().get(0).getUrl());
        assertEquals("https://api.example.com/data/c2", captor.getAllValues().get(1).getUrl());
    }

    @Test
    void testCollectAllWithPathMissingPlaceholderFallsBackToQuery() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setParamLocation(ParamLocation.PATH);
        pc.setPageSize(10);
        pc.setMaxPages(2);
        pc.setStopByBatchSize(false);
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class)))
            .thenReturn("{\"data\":[{\"id\":1}]}")
            .thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any()))
            .thenReturn(Collections.singletonList(new HashMap<>()))
            .thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, times(2)).fetch(captor.capture());
        String url = captor.getAllValues().get(0).getUrl();
        assertTrue(url.contains("page=1"), "未占位的参数应退化为查询参数，实际: " + url);
        assertTrue(url.contains("pageSize=10"), "实际: " + url);
    }

    @Test
    void testPlaceholderReplacementInHeadersAndBody() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setMethod(HttpMethod.POST);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Page", "${page}");
        config.setHeaders(headers);
        config.setBodyType(BodyType.JSON);
        config.setBodyContent("{\"q\":{\"page\":\"${page}\"}}");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setPageSize(10);
        pc.setMaxPages(5);
        pc.setParamLocation(ParamLocation.QUERY);
        pc.setStopByBatchSize(false);
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class)))
            .thenReturn("{\"data\":[{\"id\":1}]}")
            .thenReturn("{\"data\":[{\"id\":2}]}")
            .thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.singletonList(new HashMap<>()));

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        ApiConfigEntity first = captor.getAllValues().get(0);
        assertEquals("1", first.getHeaders().get("X-Page"));
        assertTrue(first.getBodyContent().contains("\"page\":\"1\""));
    }

    @Test
    void testPlaceholderDedupeSkipsQueryInjection() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setMethod(HttpMethod.POST);
        config.setBodyType(BodyType.JSON);
        config.setBodyContent("{\"page\":\"${page}\"}");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setPageSize(10);
        pc.setMaxPages(5);
        pc.setParamLocation(ParamLocation.QUERY);
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.emptyList());

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        String url = captor.getAllValues().get(0).getUrl();
        assertFalse(url.contains("page="));
        assertTrue(url.contains("pageSize="));
    }

    @Test
    void testTotalPagesStops() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setPageSize(10);
        pc.setMaxPages(100);
        pc.setTotalPages(2);
        pc.setStopByBatchSize(false);
        config.setPageConfig(pc);
        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[{\"id\":1}]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.singletonList(new HashMap<>()));

        PageCollector.CollectAllResult result = collector.collectAllWithPageInfo(config);
        assertEquals(2, result.getPageCount());
        verify(httpFetcher, times(2)).fetch(any(ApiConfigEntity.class));
    }

    @Test
    void testStopByBatchSizeStopsEarly() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setPageSize(10);
        pc.setMaxPages(100);
        config.setPageConfig(pc);
        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[{\"id\":1}]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.singletonList(new HashMap<>()));

        PageCollector.CollectAllResult result = collector.collectAllWithPageInfo(config);
        assertEquals(1, result.getPageCount());
        verify(httpFetcher, times(1)).fetch(any(ApiConfigEntity.class));
    }

    @Test
    void testNestedBodyKeyRecursiveReplacement() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setMethod(HttpMethod.POST);
        config.setBodyType(BodyType.JSON);
        config.setBodyContent("{\"q\":{\"page\":0}}");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setParamLocation(ParamLocation.BODY);
        pc.setPageSize(10);
        pc.setMaxPages(5);
        pc.setStopByBatchSize(false);
        config.setPageConfig(pc);

        when(httpFetcher.fetch(any(ApiConfigEntity.class)))
            .thenReturn("{\"data\":[{\"id\":1}]}")
            .thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.singletonList(new HashMap<>()));

        collector.collectAllWithPageInfo(config);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(httpFetcher, atLeastOnce()).fetch(captor.capture());
        assertTrue(captor.getAllValues().get(0).getBodyContent().contains("\"page\":\"1\""));
    }

    @Test
    void testRequestIntervalMsThrottle() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setPageSize(10);
        pc.setMaxPages(5);
        pc.setStopByBatchSize(false);
        pc.setRequestIntervalMs(60);
        config.setPageConfig(pc);
        when(httpFetcher.fetch(any(ApiConfigEntity.class)))
            .thenReturn("{\"data\":[{\"id\":1}]}")
            .thenReturn("{\"data\":[]}");
        when(responseParser.parse(anyString(), any())).thenReturn(Collections.singletonList(new HashMap<>()));

        long start = System.currentTimeMillis();
        collector.collectAllWithPageInfo(config);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 50, "expected throttle sleep, elapsed=" + elapsed);
    }

    // ---------- 逐页流式入口 collectPages ----------

    private PageConfig pageNumberConfig(int pageSize, int maxPages, boolean stopByBatchSize) {
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setPageSize(pageSize);
        pc.setMaxPages(maxPages);
        pc.setStopByBatchSize(stopByBatchSize);
        return pc;
    }

    @Test
    void testCollectPagesInvokesHandlerPerPage() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        config.setPageConfig(pageNumberConfig(10, 3, false));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[{\"id\":1}]}");
        when(responseParser.parse(anyString(), any()))
            .thenReturn(Collections.singletonList(new HashMap<>()));

        List<Integer> indices = new ArrayList<>();
        int pages = collector.collectPages(config, (records, pageIndex) -> {
            indices.add(pageIndex);
            return true;
        });

        assertEquals(3, pages);
        assertEquals(Arrays.asList(1, 2, 3), indices);
    }

    @Test
    void testCollectPagesHandlerReturningFalseStops() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        config.setPageConfig(pageNumberConfig(10, 5, false));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[{\"id\":1}]}");
        when(responseParser.parse(anyString(), any()))
            .thenReturn(Collections.singletonList(new HashMap<>()));

        int pages = collector.collectPages(config, (records, pageIndex) -> false);

        assertEquals(1, pages);
        verify(httpFetcher, times(1)).fetch(any(ApiConfigEntity.class));
    }

    @Test
    void testCollectPagesNonPagedSingleCallback() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(0);

        when(httpFetcher.fetch(config)).thenReturn("{\"data\":[{\"id\":1}]}");
        when(responseParser.parse(anyString(), eq(config)))
            .thenReturn(Collections.singletonList(new HashMap<>()));

        int[] calls = {0};
        int pages = collector.collectPages(config, (records, pageIndex) -> {
            calls[0]++;
            return true;
        });

        assertEquals(1, pages);
        assertEquals(1, calls[0]);
    }

    @Test
    void testCollectPagesStopsOnFetchFailureWithoutThrowing() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        config.setPageConfig(pageNumberConfig(10, 5, false));

        when(httpFetcher.fetch(any(ApiConfigEntity.class)))
            .thenReturn("{\"data\":[{\"id\":1}]}")
            .thenThrow(new RuntimeException("boom"));
        when(responseParser.parse(anyString(), any()))
            .thenReturn(Collections.singletonList(new HashMap<>()));

        int[] calls = {0};
        int pages = collector.collectPages(config, (records, pageIndex) -> {
            calls[0]++;
            return true;
        });

        assertEquals(1, pages);
        assertEquals(1, calls[0]);
    }

    @Test
    void testCollectPagesHandlerExceptionPropagates() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        config.setPageConfig(pageNumberConfig(10, 5, false));

        when(httpFetcher.fetch(any(ApiConfigEntity.class))).thenReturn("{\"data\":[{\"id\":1}]}");
        when(responseParser.parse(anyString(), any()))
            .thenReturn(Collections.singletonList(new HashMap<>()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> collector.collectPages(config,
            (records, pageIndex) -> {
                throw new RuntimeException("transform boom");
            }));
        assertTrue(ex.getMessage().contains("transform boom"), "回调异常应向上传播而非被吞掉");
    }

    @Test
    void testCollectPagesStopsWhenInterrupted() throws Exception {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        config.setPageConfig(pageNumberConfig(10, 5, false));

        Thread.currentThread().interrupt();
        try {
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> collector.collectPages(config, (records, pageIndex) -> true));
            assertTrue(ex.getMessage().contains("interrupted"));
            verify(httpFetcher, never()).fetch(any(ApiConfigEntity.class));
        } finally {
            Thread.interrupted();
        }
    }

    // ---------- 首页请求构建（design D1/D2）----------

    @Test
    void buildFirstPageRequestPageNumberQuery() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setParamLocation(ParamLocation.QUERY);
        pc.setPageSize(10);
        config.setPageConfig(pc);

        PageCollector.PageRequestPlan plan = collector.buildFirstPageRequest(config);
        String url = plan.getRequest().getUrl();
        assertTrue(url.contains("page=1"), "实际: " + url);
        assertTrue(url.contains("pageSize=10"), "实际: " + url);
        assertEquals("1", plan.getAppliedParams().get("page"));
        assertEquals("10", plan.getAppliedParams().get("pageSize"));
        assertEquals("https://api.example.com/data", config.getUrl(), "不得修改传入的配置");
    }

    @Test
    void buildFirstPageRequestOffset() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.OFFSET);
        pc.setParamLocation(ParamLocation.QUERY);
        pc.setPageSize(10);
        config.setPageConfig(pc);

        PageCollector.PageRequestPlan plan = collector.buildFirstPageRequest(config);
        String url = plan.getRequest().getUrl();
        assertTrue(url.contains("offset=0"), "实际: " + url);
        assertTrue(url.contains("limit=10"), "实际: " + url);
        assertEquals("0", plan.getAppliedParams().get("offset"));
        assertEquals("10", plan.getAppliedParams().get("limit"));
    }

    @Test
    void buildFirstPageRequestCursor() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.CURSOR);
        pc.setParamLocation(ParamLocation.QUERY);
        pc.setCursorParam("cursor");
        pc.setInitialCursor("abc");
        config.setPageConfig(pc);

        PageCollector.PageRequestPlan plan = collector.buildFirstPageRequest(config);
        assertTrue(plan.getRequest().getUrl().contains("cursor=abc"),
            "实际: " + plan.getRequest().getUrl());
        assertEquals("abc", plan.getAppliedParams().get("cursor"));
    }

    @Test
    void buildFirstPageRequestBodyJsonInjectsParams() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setMethod(HttpMethod.POST);
        config.setBodyType(BodyType.JSON);
        config.setBodyContent("{\"foo\":\"bar\"}");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setParamLocation(ParamLocation.BODY);
        pc.setPageSize(10);
        pc.setPageParam("pageNo");
        pc.setSizeParam("size");
        config.setPageConfig(pc);

        PageCollector.PageRequestPlan plan = collector.buildFirstPageRequest(config);
        String body = plan.getRequest().getBodyContent();
        assertTrue(body.contains("\"pageNo\":\"1\""), "实际: " + body);
        assertTrue(body.contains("\"size\":\"10\""), "实际: " + body);
        assertTrue(body.contains("\"foo\":\"bar\""), "实际: " + body);
    }

    @Test
    void buildFirstPageRequestPath() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data/${page}/${pageSize}");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setParamLocation(ParamLocation.PATH);
        pc.setPageSize(10);
        config.setPageConfig(pc);

        PageCollector.PageRequestPlan plan = collector.buildFirstPageRequest(config);
        assertEquals("https://api.example.com/data/1/10", plan.getRequest().getUrl());
        assertEquals("1", plan.getAppliedParams().get("page"));
        assertEquals("10", plan.getAppliedParams().get("pageSize"));
        assertEquals("https://api.example.com/data/${page}/${pageSize}", config.getUrl(), "不得修改传入的配置");
    }

    @Test
    void buildFirstPageRequestPlaceholderDedupeSkipsFixedKey() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setMethod(HttpMethod.POST);
        config.setBodyType(BodyType.JSON);
        config.setBodyContent("{\"page\":\"${page}\"}");
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setParamLocation(ParamLocation.QUERY);
        pc.setPageSize(10);
        config.setPageConfig(pc);

        PageCollector.PageRequestPlan plan = collector.buildFirstPageRequest(config);
        String url = plan.getRequest().getUrl();
        assertFalse(url.contains("page="), "占位符已替换，不应再追加固定键，实际: " + url);
        assertTrue(url.contains("pageSize=10"), "实际: " + url);
        assertTrue(plan.getRequest().getBodyContent().contains("\"page\":\"1\""),
            "实际: " + plan.getRequest().getBodyContent());
    }

    @Test
    void buildFirstPageRequestBodyUnsupportedBodyTypeThrows() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/data");
        config.setBodyType(BodyType.NONE);
        config.setPageEnabled(1);
        PageConfig pc = pc();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        pc.setParamLocation(ParamLocation.BODY);
        pc.setPageSize(10);
        config.setPageConfig(pc);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> collector.buildFirstPageRequest(config));
        assertTrue(ex.getMessage().contains("JSON、FORM 或 XML"), "实际: " + ex.getMessage());
    }
}
