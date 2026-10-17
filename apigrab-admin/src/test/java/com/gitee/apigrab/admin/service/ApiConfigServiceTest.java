package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.core.collector.DetailEnricher;
import com.gitee.apigrab.core.collector.PageCollector;
import com.gitee.apigrab.core.http.FetchedResponse;
import com.gitee.apigrab.core.http.HttpFetcher;
import com.gitee.apigrab.core.http.HttpResponseParser;
import com.gitee.apigrab.core.http.RespTypeDetector;
import com.gitee.apigrab.core.mapping.FieldMapper;
import com.gitee.apigrab.core.transform.DataTransformChain;
import com.gitee.apigrab.core.transform.GroovyTransformer;
import com.gitee.apigrab.persistence.dao.ApiConfigDao;
import com.gitee.apigrab.persistence.dao.AuthConfigDao;
import com.gitee.apigrab.persistence.dao.CollectTaskDao;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.AuthConfigEntity;
import com.gitee.apigrab.persistence.entity.PageConfig;
import com.gitee.apigrab.persistence.entity.enums.PaginationMode;
import com.gitee.apigrab.persistence.entity.auth.BearerAuthConfig;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import com.gitee.apigrab.persistence.entity.DetailConfig;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import com.gitee.apigrab.persistence.entity.enums.HttpMethod;
import com.gitee.apigrab.persistence.entity.enums.RespType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiConfigServiceTest {

    @Mock private ApiConfigDao apiConfigDao;
    @Mock private AuthConfigDao authConfigDao;
    @Mock private CollectTaskDao collectTaskDao;
    @Mock private HttpFetcher httpFetcher;
    @Mock private PageCollector pageCollector;
    @Mock private DetailEnricher detailEnricher;
    @Mock private HttpResponseParser responseParser;
    // 判定器是纯函数，用 Spy 走真实逻辑，避免把判定规则 mock 掉
    @Spy private RespTypeDetector respTypeDetector = new RespTypeDetector();
    @Mock private GroovyTransformer groovyTransformer;
    @Mock private FieldMapper fieldMapper;
    @Mock private DataTransformChain dataTransformChain;

    @InjectMocks private ApiConfigService service;

    private ApiConfigEntity entity;

    @BeforeEach
    void setUp() {
        entity = new ApiConfigEntity();
        entity.setId(1L);
        entity.setName("测试接口");
        entity.setUrl("https://api.example.com/test");
        entity.setMethod(HttpMethod.GET);
    }

    @Test
    void testGetById() {
        when(apiConfigDao.getById(1L)).thenReturn(entity);
        ApiConfigEntity result = service.getById(1L);
        assertEquals("测试接口", result.getName());
    }

    @Test
    void testGetByIdNotFound() {
        when(apiConfigDao.getById(999L)).thenReturn(null);
        assertThrows(CommonException.class, () -> service.getById(999L));
    }

    @Test
    void testList() {
        when(apiConfigDao.list(any())).thenReturn(Collections.singletonList(entity));
        List<ApiConfigEntity> list = service.list(new ApiConfigEntity());
        assertEquals(1, list.size());
    }

    @Test
    void testCreate() {
        entity.setStatus(null);
        service.create(entity);
        assertEquals(EnableStatus.ENABLED, entity.getStatus());
        verify(apiConfigDao, times(1)).insert(entity);
    }

    @Test
    void testCreateWithStatus() {
        entity.setStatus(EnableStatus.DISABLED);
        service.create(entity);
        assertEquals(EnableStatus.DISABLED, entity.getStatus());
        verify(apiConfigDao, times(1)).insert(entity);
    }

    @Test
    void testUpdate() {
        service.update(entity);
        verify(apiConfigDao, times(1)).updateById(entity);
    }

    // ---------- 认证引用存在性校验（design D9）----------

    @Test
    void testCreateAcceptsExistingAuthConfigReference() {
        entity.setAuthConfigId(5L);
        when(authConfigDao.getById(5L)).thenReturn(new AuthConfigEntity());

        service.create(entity);

        verify(apiConfigDao, times(1)).insert(entity);
    }

    @Test
    void testCreateRejectsMissingAuthConfigReference() {
        entity.setAuthConfigId(5L);
        when(authConfigDao.getById(5L)).thenReturn(null);

        CommonException e = assertThrows(CommonException.class, () -> service.create(entity));
        assertTrue(e.getMessage().contains("认证配置"), "文案应可读: " + e.getMessage());
        verify(apiConfigDao, never()).insert(any());
    }

    @Test
    void testUpdateRejectsMissingAuthConfigReference() {
        entity.setId(1L);
        entity.setAuthConfigId(5L);
        when(authConfigDao.getById(5L)).thenReturn(null);

        assertThrows(CommonException.class, () -> service.update(entity));
        verify(apiConfigDao, never()).updateById(any());
    }

    // ---------- 试跑按引用解析认证（临时配置无派生字段）----------

    @Test
    void testTransientConfigResolvesAuthReference() throws Exception {
        ApiConfigEntity transientConfig = new ApiConfigEntity();
        transientConfig.setUrl("https://api.example.com/auth");
        transientConfig.setAuthConfigId(5L);

        AuthConfigEntity authEntity = new AuthConfigEntity();
        authEntity.setAuthType(AuthType.BEARER);
        BearerAuthConfig bearer = new BearerAuthConfig();
        bearer.setToken("tk");
        authEntity.setAuthConfig(bearer);
        when(authConfigDao.getById(5L)).thenReturn(authEntity);
        when(httpFetcher.fetchResponse(transientConfig)).thenReturn(new FetchedResponse("{}", "application/json"));
        when(responseParser.parse(anyString(), any())).thenReturn(new ArrayList<>());

        service.testConfig(transientConfig);

        assertEquals(AuthType.BEARER, transientConfig.getAuthType(),
            "临时配置试跑前必须按 authConfigId 解析出认证类型");
        assertTrue(transientConfig.getAuthConfig() instanceof BearerAuthConfig);
    }

    @Test
    void testTransientConfigReportsMissingAuthReference() {
        ApiConfigEntity transientConfig = new ApiConfigEntity();
        transientConfig.setUrl("https://api.example.com/auth");
        transientConfig.setAuthConfigId(9L);
        when(authConfigDao.getById(9L)).thenReturn(null);

        Map<String, Object> result = service.testConfig(transientConfig);

        assertFalse((Boolean) result.get("success"));
        assertTrue(String.valueOf(result.get("error")).contains("认证配置"));
    }

    // ---------- 名称唯一性（design C1/C3/C4/C5/C6）----------

    @Test
    void testCreateRejectsDuplicateName() {
        ApiConfigEntity existing = new ApiConfigEntity();
        existing.setId(99L);
        existing.setName("测试接口");
        when(apiConfigDao.getByName("测试接口")).thenReturn(existing);

        CommonException ex = assertThrows(CommonException.class, () -> service.create(entity));
        assertTrue(ex.getMessage().contains("已存在"), "文案应可读: " + ex.getMessage());
        verify(apiConfigDao, never()).insert(any());
    }

    @Test
    void testCreateRejectsBlankName() {
        entity.setName("   ");
        CommonException ex = assertThrows(CommonException.class, () -> service.create(entity));
        assertTrue(ex.getMessage().contains("不能为空"), "文案应可读: " + ex.getMessage());
        verify(apiConfigDao, never()).insert(any());
    }

    @Test
    void testCreateTrimsNameBeforeSaving() {
        entity.setName("  订单接口  ");
        when(apiConfigDao.getByName("订单接口")).thenReturn(null);

        service.create(entity);

        assertEquals("订单接口", entity.getName(), "保存前应去除首尾空白");
        verify(apiConfigDao).getByName("订单接口");
    }

    // ---------- 名称长度上限（design C10）----------

    @Test
    void testCreateRejectsOverlongName() {
        entity.setName(repeat('a', 101));

        CommonException ex = assertThrows(CommonException.class, () -> service.create(entity));

        assertTrue(ex.getMessage().contains("长度"), "文案应可读: " + ex.getMessage());
        verify(apiConfigDao, never()).insert(any());
    }

    @Test
    void testCreateAcceptsNameAtExactLimit() {
        entity.setName(repeat('a', 100));
        when(apiConfigDao.getByName(repeat('a', 100))).thenReturn(null);

        service.create(entity);

        verify(apiConfigDao, times(1)).insert(entity);
    }

    @Test
    void testUpdateRejectsOverlongName() {
        entity.setId(1L);
        entity.setName(repeat('a', 101));

        assertThrows(CommonException.class, () -> service.update(entity));
        verify(apiConfigDao, never()).updateById(any());
    }

    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 判重必须依赖 SQL 等值查询的结果（C4），而不是在 Java 层比较字符串。
     * 这里让 getByName 返回一个“仅大小写不同”的记录——模拟 DB 的大小写不敏感匹配——
     * 服务层应当照单判重，而不是自己判断大小写后放行。
     */
    @Test
    void testCreateRejectsNameMatchedByDatabaseEquality() {
        ApiConfigEntity existing = new ApiConfigEntity();
        existing.setId(99L);
        existing.setName("OrderApi");
        when(apiConfigDao.getByName("ORDERAPI")).thenReturn(existing);

        entity.setName("ORDERAPI");
        assertThrows(CommonException.class, () -> service.create(entity));
        verify(apiConfigDao, never()).insert(any());
    }

    @Test
    void testUpdateAllowsSameNameOnItself() {
        entity.setId(1L);
        entity.setName("测试接口");
        ApiConfigEntity self = new ApiConfigEntity();
        self.setId(1L);
        self.setName("测试接口");
        when(apiConfigDao.getByName("测试接口")).thenReturn(self);

        service.update(entity);

        verify(apiConfigDao, times(1)).updateById(entity);
    }

    @Test
    void testUpdateRejectsNameUsedByAnother() {
        entity.setId(1L);
        entity.setName("测试接口");
        ApiConfigEntity other = new ApiConfigEntity();
        other.setId(2L);
        other.setName("测试接口");
        when(apiConfigDao.getByName("测试接口")).thenReturn(other);

        CommonException ex = assertThrows(CommonException.class, () -> service.update(entity));
        assertTrue(ex.getMessage().contains("已存在"), "文案应可读: " + ex.getMessage());
        verify(apiConfigDao, never()).updateById(any());
    }

    @Test
    void testUpdateTrimsNameBeforeSaving() {
        entity.setId(1L);
        entity.setName("  测试接口  ");
        when(apiConfigDao.getByName("测试接口")).thenReturn(null);

        service.update(entity);

        assertEquals("测试接口", entity.getName());
    }

    @Test
    void testDelete() {
        when(collectTaskDao.list(any())).thenReturn(Collections.emptyList());
        service.delete(1L);
        verify(apiConfigDao, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteBlockedWhenReferencedByTask() {
        CollectTaskEntity referencing = new CollectTaskEntity();
        referencing.setId(5L);
        when(collectTaskDao.list(any())).thenReturn(Collections.singletonList(referencing));

        assertThrows(CommonException.class, () -> service.delete(1L));
        verify(apiConfigDao, never()).deleteById(any());
    }

    @Test
    void testTestApiSuccess() throws Exception {
        entity.setDataPath("$.data");
        when(apiConfigDao.getById(1L)).thenReturn(entity);
        when(httpFetcher.fetchResponse(entity))
            .thenReturn(new FetchedResponse("{\"name\":\"Alice\"}", "application/json"));
        when(responseParser.parse("{\"name\":\"Alice\"}", entity))
            .thenReturn(Collections.singletonList(Collections.singletonMap("name", "Alice")));

        Map<String, Object> result = service.testApi(1L);
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("rawResponse"));
        assertNotNull(result.get("parsedData"));
        assertEquals("$.data", result.get("dataPath"));
        assertEquals("application/json", result.get("contentType"));
        assertEquals(RespType.JSON, result.get("detectedRespType"));
    }

    @Test
    void testTestConfigDetectsNdjsonAndXmlFromHeader() throws Exception {
        ApiConfigEntity ndjson = new ApiConfigEntity();
        ndjson.setUrl("https://api.example.com/ndjson");
        when(httpFetcher.fetchResponse(ndjson))
            .thenReturn(new FetchedResponse("{\"a\":1}", "application/x-ndjson"));
        when(responseParser.parse(anyString(), any())).thenReturn(new ArrayList<>());

        Map<String, Object> ndjsonResult = service.testConfig(ndjson);
        assertEquals(RespType.JSON, ndjsonResult.get("detectedRespType"));
        assertEquals(1, ndjsonResult.get("detectedMultilines"));

        ApiConfigEntity xml = new ApiConfigEntity();
        xml.setUrl("https://api.example.com/xml");
        when(httpFetcher.fetchResponse(xml))
            .thenReturn(new FetchedResponse("<root><a>1</a></root>", "text/xml"));
        when(responseParser.parse(anyString(), any())).thenReturn(new ArrayList<>());

        Map<String, Object> xmlResult = service.testConfig(xml);
        assertEquals(RespType.XML, xmlResult.get("detectedRespType"));
        assertEquals(0, xmlResult.get("detectedMultilines"));
    }

    @Test
    void testTestConfigLeavesDetectionUndeterminedWhenHeaderAbsent() throws Exception {
        ApiConfigEntity transientConfig = new ApiConfigEntity();
        transientConfig.setUrl("https://api.example.com/no-header");
        when(httpFetcher.fetchResponse(transientConfig))
            .thenReturn(new FetchedResponse("{\"name\":\"Carol\"}", null));
        when(responseParser.parse("{\"name\":\"Carol\"}", transientConfig))
            .thenReturn(Collections.singletonList(Collections.singletonMap("name", "Carol")));

        Map<String, Object> result = service.testConfig(transientConfig);

        assertTrue((Boolean) result.get("success"));
        assertNull(result.get("contentType"));
        assertNull(result.get("detectedRespType"), "缺少响应头时必须保持未判定，不得推断为 JSON");
        assertNull(result.get("detectedMultilines"), "缺少响应头时多行结论必须保持未判定");
    }

    @Test
    void testTestConfigReportsDetectionEvenWhenParsingFails() throws Exception {
        // 用户选了 JSON 但接口实际返回 XML：解析失败正是最需要判定信息的时刻
        entity.setRespType(RespType.JSON);
        when(httpFetcher.fetchResponse(entity))
            .thenReturn(new FetchedResponse("<root><a>1</a></root>", "application/xml"));
        when(responseParser.parse(anyString(), any()))
            .thenThrow(new RuntimeException("路径解析失败 (JSONPath=$.data, 响应格式=JSON)"));

        Map<String, Object> result = service.testConfig(entity);

        assertFalse((Boolean) result.get("success"));
        assertTrue(result.containsKey("contentType"), "失败路径也必须回报原始响应头");
        assertTrue(result.containsKey("detectedRespType"), "失败路径也必须回报判定结论");
        assertTrue(result.containsKey("detectedMultilines"), "失败路径也必须回报判定结论");
        assertEquals("application/xml", result.get("contentType"));
        assertEquals(RespType.XML, result.get("detectedRespType"));
        assertEquals(0, result.get("detectedMultilines"));
    }

    @Test
    void testTestConfigParsesWithOriginalConfigNotDetection() throws Exception {
        // C5：判定结论不得回写到参与解析的配置，试跑始终按用户当前设置执行
        entity.setRespType(RespType.JSON);
        when(httpFetcher.fetchResponse(entity))
            .thenReturn(new FetchedResponse("<root><a>1</a></root>", "application/xml"));
        when(responseParser.parse(anyString(), any())).thenReturn(new ArrayList<>());

        service.testConfig(entity);

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(responseParser).parse(anyString(), captor.capture());
        assertEquals(RespType.JSON, captor.getValue().getRespType(),
                "解析所用的响应类型必须是用户当前选择，不能被判定结论替换");
    }

    @Test
    void testTestApiFailureLeavesDetectionKeysPresent() throws Exception {
        when(apiConfigDao.getById(1L)).thenReturn(entity);
        when(httpFetcher.fetchResponse(entity)).thenThrow(new RuntimeException("Connection timeout"));

        Map<String, Object> result = service.testApi(1L);

        assertFalse((Boolean) result.get("success"));
        assertEquals("Connection timeout", result.get("error"));
        assertNull(result.get("detectedRespType"));
        assertNull(result.get("detectedMultilines"));
        assertTrue(result.containsKey("detectedRespType"), "未取到响应时判定键仍须存在");
    }

    @Test
    void testTestConfigReportsBlankPathAsNull() throws Exception {
        ApiConfigEntity transientConfig = new ApiConfigEntity();
        transientConfig.setUrl("https://api.example.com/live");
        transientConfig.setDataPath("   ");
        when(httpFetcher.fetchResponse(transientConfig))
            .thenReturn(new FetchedResponse("{\"name\":\"Carol\"}", "application/json"));
        when(responseParser.parse("{\"name\":\"Carol\"}", transientConfig))
            .thenReturn(Collections.singletonList(Collections.singletonMap("name", "Carol")));

        Map<String, Object> result = service.testConfig(transientConfig);

        assertTrue((Boolean) result.get("success"));
        assertNull(result.get("dataPath"));
    }

    @Test
    void testTestApiFailure() throws Exception {
        when(apiConfigDao.getById(1L)).thenReturn(entity);
        when(httpFetcher.fetchResponse(entity)).thenThrow(new RuntimeException("Connection timeout"));

        Map<String, Object> result = service.testApi(1L);
        assertFalse((Boolean) result.get("success"));
        assertEquals("Connection timeout", result.get("error"));
    }

    @Test
    void testDebugScript() {
        List<Map<String, Object>> sampleData = Collections.singletonList(Collections.singletonMap("a", 1));
        when(groovyTransformer.transform(sampleData, "return records")).thenReturn(sampleData);

        List<Map<String, Object>> result = service.debugScript(1L, "return records", sampleData);
        assertEquals(1, result.size());
    }

    @Test
    void testTestConfigTransientDoesNotCacheSample() throws Exception {
        ApiConfigEntity transientConfig = new ApiConfigEntity();
        transientConfig.setUrl("https://api.example.com/live");
        when(httpFetcher.fetchResponse(transientConfig))
            .thenReturn(new FetchedResponse("{\"name\":\"Bob\"}", "application/json"));
        when(responseParser.parse("{\"name\":\"Bob\"}", transientConfig))
            .thenReturn(Collections.singletonList(Collections.singletonMap("name", "Bob")));

        Map<String, Object> result = service.testConfig(transientConfig);

        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("parsedData"));
        verify(apiConfigDao, never()).updateById(any());
    }

    @Test
    void testTestConfigSuccessCachesFirstRecord() throws Exception {
        when(httpFetcher.fetchResponse(entity))
            .thenReturn(new FetchedResponse("[{\"id\":1,\"name\":\"Alice\"},{\"id\":2}]", "application/json"));
        when(responseParser.parse("[{\"id\":1,\"name\":\"Alice\"},{\"id\":2}]", entity))
            .thenReturn(Arrays.asList(
                new LinkedHashMap<String, Object>() {{ put("id", 1); put("name", "Alice"); }},
                Collections.singletonMap("id", 2)));

        Map<String, Object> result = service.testConfig(entity);

        assertTrue((Boolean) result.get("success"));
        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(apiConfigDao, times(1)).updateById(captor.capture());
        ApiConfigEntity saved = captor.getValue();
        assertEquals(1L, saved.getId());
        assertNotNull(saved.getSampleData());
        assertEquals("Alice", saved.getSampleData().get("name"));
    }

    @Test
    void testTestConfigCachesMergedSampleWhenDetailEnabled() throws Exception {
        DetailConfig dc = new DetailConfig();
        dc.setEnabled(1);
        dc.setUrl("https://api.example.com/detail/${id}");
        entity.setDetailConfig(dc);

        when(httpFetcher.fetchResponse(entity))
            .thenReturn(new FetchedResponse("[{\"id\":1,\"name\":\"Alice\"}]", "application/json"));
        when(responseParser.parse("[{\"id\":1,\"name\":\"Alice\"}]", entity))
            .thenReturn(new ArrayList<>(Collections.singletonList(
                new LinkedHashMap<String, Object>() {{ put("id", 1); put("name", "Alice"); }})));
        // 模拟详情补全合并（真实合并逻辑由 DetailEnricherTest 覆盖）
        doAnswer(inv -> {
            List<Map<String, Object>> recs = inv.getArgument(0);
            recs.get(0).put("age", 20);
            return recs;
        }).when(detailEnricher).enrich(anyList(), any(), any());

        Map<String, Object> result = service.testConfig(entity);

        assertTrue((Boolean) result.get("success"));
        @SuppressWarnings("unchecked")
        Map<String, Object> sample = (Map<String, Object>) result.get("sampleData");
        assertEquals("Alice", sample.get("name"));
        assertEquals(20, sample.get("age"), "试跑采样应包含合并后的详情字段");

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(apiConfigDao, times(1)).updateById(captor.capture());
        assertEquals(20, captor.getValue().getSampleData().get("age"), "缓存样例应为合并后记录");
    }

    @Test
    void testTestConfigFailureDoesNotCacheSample() throws Exception {
        when(httpFetcher.fetchResponse(entity)).thenThrow(new RuntimeException("Connection timeout"));

        Map<String, Object> result = service.testConfig(entity);

        assertFalse((Boolean) result.get("success"));
        assertEquals("Connection timeout", result.get("error"));
        verify(apiConfigDao, never()).updateById(any());
    }

    // ---------- 连接测试按分页首页参数发起真实请求（design D1/D2/D4/D6）----------

    @Test
    void testTestConfigWithPaginationUsesFirstPageRequest() throws Exception {
        entity.setPageEnabled(1);
        PageConfig pc = new PageConfig();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        entity.setPageConfig(pc);

        ApiConfigEntity paged = new ApiConfigEntity();
        paged.setUrl("https://api.example.com/test?page=1&pageSize=20");
        Map<String, String> applied = new LinkedHashMap<>();
        applied.put("page", "1");
        applied.put("pageSize", "20");
        when(pageCollector.buildFirstPageRequest(entity))
            .thenReturn(new PageCollector.PageRequestPlan(paged, applied));
        when(httpFetcher.fetchResponse(paged))
            .thenReturn(new FetchedResponse("{\"data\":[{\"id\":1}]}", "application/json"));
        when(responseParser.parse(anyString(), eq(entity))).thenReturn(new ArrayList<>());

        Map<String, Object> result = service.testConfig(entity);

        assertTrue((Boolean) result.get("success"));
        assertEquals(true, result.get("pageApplied"));
        assertEquals(applied, result.get("appliedPageParams"));
        verify(pageCollector, times(1)).buildFirstPageRequest(entity);
        verify(httpFetcher, times(1)).fetchResponse(paged);
        verify(httpFetcher, never()).fetchResponse(entity);
    }

    @Test
    void testTestConfigPaginationDisabledDoesNotApplyPageParams() throws Exception {
        when(httpFetcher.fetchResponse(entity))
            .thenReturn(new FetchedResponse("{}", "application/json"));
        when(responseParser.parse(anyString(), any())).thenReturn(new ArrayList<>());

        Map<String, Object> result = service.testConfig(entity);

        assertEquals(false, result.get("pageApplied"));
        verify(pageCollector, never()).buildFirstPageRequest(any());
        verify(httpFetcher, times(1)).fetchResponse(entity);
    }

    @Test
    void testTestConfigPaginationBuilderFailureIsReadable() throws Exception {
        entity.setPageEnabled(1);
        PageConfig pc = new PageConfig();
        pc.setMode(PaginationMode.PAGE_NUMBER);
        entity.setPageConfig(pc);
        when(pageCollector.buildFirstPageRequest(entity))
            .thenThrow(new RuntimeException("请求体分页需要 bodyType 为 JSON、FORM 或 XML，当前为: NONE"));

        Map<String, Object> result = service.testConfig(entity);

        assertFalse((Boolean) result.get("success"));
        assertTrue(String.valueOf(result.get("error")).contains("JSON、FORM 或 XML"),
            "实际: " + result.get("error"));
        verify(httpFetcher, never()).fetchResponse(any());
    }

    @Test
    void testPreviewMappingReusesFieldMapperAndChain() {
        when(apiConfigDao.getById(1L)).thenReturn(entity);
        List<Map<String, Object>> sample = Collections.singletonList(Collections.singletonMap("name", "Alice"));
        List<FieldMappingEntity> mappings = Collections.singletonList(new FieldMappingEntity());
        when(fieldMapper.mapFields(sample, mappings)).thenReturn(sample);
        when(dataTransformChain.execute(sample, entity, mappings)).thenReturn(sample);

        List<Map<String, Object>> result = service.previewMapping(1L, mappings, sample);

        assertEquals(1, result.size());
        verify(fieldMapper, times(1)).mapFields(sample, mappings);
        verify(dataTransformChain, times(1)).execute(sample, entity, mappings);
    }

    @Test
    void testPreviewMappingEmptyInputReturnsEmpty() {
        when(apiConfigDao.getById(1L)).thenReturn(entity);

        List<Map<String, Object>> result = service.previewMapping(1L, Collections.emptyList(), Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(fieldMapper, never()).mapFields(any(), any());
    }

    @Test
    void testDetailReturnsDetailAndMergedPreview() throws Exception {
        DetailConfig dc = new DetailConfig();
        dc.setEnabled(1);
        dc.setUrl("https://api.example.com/detail/${id}");

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("name", "detailName");
        detail.put("age", 20);
        when(detailEnricher.fetchDetailObject(entity, dc, "42")).thenReturn(detail);

        Map<String, Object> result = service.testDetail(entity, dc, "42");

        assertTrue((Boolean) result.get("success"));
        assertEquals(detail, result.get("detail"));
        @SuppressWarnings("unchecked")
        Map<String, Object> merged = (Map<String, Object>) result.get("merged");
        assertEquals("42", merged.get("_key"));
        assertEquals("detailName", merged.get("name"));
        assertEquals(20, merged.get("age"));
    }

    @Test
    void testDetailEmptyResponseReportsError() throws Exception {
        DetailConfig dc = new DetailConfig();
        dc.setEnabled(1);
        when(detailEnricher.fetchDetailObject(entity, dc, "42")).thenReturn(null);

        Map<String, Object> result = service.testDetail(entity, dc, "42");

        assertFalse((Boolean) result.get("success"));
        assertNotNull(result.get("error"));
    }

    @Test
    void testDetailBlankSampleKeyRejected() {
        DetailConfig dc = new DetailConfig();
        dc.setEnabled(1);

        Map<String, Object> result = service.testDetail(entity, dc, "  ");

        assertFalse((Boolean) result.get("success"));
        verifyNoInteractions(detailEnricher);
    }
}
