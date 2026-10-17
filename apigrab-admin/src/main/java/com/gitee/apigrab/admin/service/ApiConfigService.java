package com.gitee.apigrab.admin.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.core.collector.DetailEnricher;
import com.gitee.apigrab.core.collector.PageCollector;
import com.gitee.apigrab.core.collector.StageMetrics;
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
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import com.gitee.apigrab.persistence.entity.DetailConfig;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import com.gitee.apigrab.persistence.entity.enums.Stage;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ApiConfigService {

    /** 与列定义 `rc_api_config.name VARCHAR(100)` 对应（design C10）。就近定义：各表宽度可独立变化。 */
    private static final int NAME_MAX_LENGTH = 100;

    /** 补全试跑预览中展示"本次试跑所用键值"的合成字段名（非真实列）。 */
    private static final String PREVIEW_KEY_FIELD = "_key";

    @Resource
    private ApiConfigDao apiConfigDao;

    @Resource
    private AuthConfigDao authConfigDao;

    @Resource
    private CollectTaskDao collectTaskDao;

    @Resource
    private HttpFetcher httpFetcher;

    @Resource
    private PageCollector pageCollector;

    @Resource
    private DetailEnricher detailEnricher;

    @Resource
    private HttpResponseParser responseParser;

    @Resource
    private RespTypeDetector respTypeDetector;

    @Resource
    private GroovyTransformer groovyTransformer;

    @Resource
    private FieldMapper fieldMapper;

    @Resource
    private DataTransformChain dataTransformChain;

    public ApiConfigEntity getById(Long id) {
        ApiConfigEntity entity = apiConfigDao.getById(id);
        if (entity == null) {
            throw new CommonException(ResponseErrorCode.NOT_FOUND, "接口配置不存在");
        }
        return entity;
    }

    public List<ApiConfigEntity> list(ApiConfigEntity param) {
        return apiConfigDao.list(param);
    }

    public void create(ApiConfigEntity entity) {
        entity.setName(requireUniqueName(entity.getName(), null));
        validateAuthConfigReference(entity.getAuthConfigId());
        if (entity.getStatus() == null) {
            entity.setStatus(EnableStatus.ENABLED);
        }
        apiConfigDao.insert(entity);
    }

    public void update(ApiConfigEntity entity) {
        entity.setName(requireUniqueName(entity.getName(), entity.getId()));
        validateAuthConfigReference(entity.getAuthConfigId());
        apiConfigDao.updateById(entity);
    }

    /**
     * 接口保存时校验引用的认证配置存在（design D9）。{@code authConfigId} 为空即「无认证」，不校验。
     */
    private void validateAuthConfigReference(Long authConfigId) {
        if (authConfigId == null) {
            return;
        }
        if (authConfigDao.getById(authConfigId) == null) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "所引用的认证配置不存在");
        }
    }

    /**
     * 试跑/测试按引用解析认证（design D5/D9）：临时（未保存）配置只有 authConfigId，
     * 需在此加载被引用认证并填充 authType/authConfig 供认证链使用。
     * 已由关联查询（持久化配置）填充时跳过，避免多余查询。
     */
    private void resolveAuthReference(ApiConfigEntity config) {
        if (config == null || config.getAuthConfigId() == null) {
            return;
        }
        if (config.getAuthType() != null && config.getAuthConfig() != null) {
            return;
        }
        AuthConfigEntity auth = authConfigDao.getById(config.getAuthConfigId());
        if (auth == null) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "所引用的认证配置不存在");
        }
        config.setAuthType(auth.getAuthType());
        config.setAuthConfig(auth.getAuthConfig());
    }

    /**
     * 名称归一与唯一性校验，返回 trim 后的名称（design C3/C4/C5/C6）。
     *
     * <p><b>判重必须走 {@code getByName} 的 SQL 等值查询，不得在 Java 层比较字符串。</b>
     * 表使用 MySQL 默认（大小写不敏感）排序规则，SQL 等值天然忽略大小写，与数据库唯一索引的
     * 判定一致；若改用 Java 比较会与之分叉，产生「服务层放行 → DB 拒绝」的割裂错误。
     *
     * @param selfId 更新时传入自身 id 以排除自己；新增时传 {@code null}
     */
    private String requireUniqueName(String rawName, Long selfId) {
        String name = StrUtil.trimToNull(rawName);
        if (name == null) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "接口配置名称不能为空");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST,
                "接口配置名称长度不能超过 " + NAME_MAX_LENGTH + " 个字符");
        }
        ApiConfigEntity existing = apiConfigDao.getByName(name);
        if (existing != null && (selfId == null || !selfId.equals(existing.getId()))) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "接口配置名称已存在");
        }
        return name;
    }

    public void delete(Long id) {
        // 被任一任务引用的接口配置禁止删除（字段映射归属任务，不随接口配置删除）
        CollectTaskEntity probe = new CollectTaskEntity();
        probe.setApiConfigId(id);
        List<CollectTaskEntity> referencingTasks = collectTaskDao.list(probe);
        if (referencingTasks != null && !referencingTasks.isEmpty()) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "该接口配置已被任务引用，无法删除");
        }
        apiConfigDao.deleteById(id);
    }

    public Map<String, Object> testApi(Long id) {
        return testConfig(getById(id));
    }

    /**
     * 补全试跑：以样例键值发起一次详情请求，返回详情对象与"列表记录合并详情后"的预览（design C11）。
     * 不写业务数据；失败时 {@code success=false} 并附错误信息。父接口的认证/超时在此解析后继承。
     */
    public Map<String, Object> testDetail(ApiConfigEntity config, DetailConfig detailConfig, String sampleKey) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (detailConfig == null) {
                throw new CommonException(ResponseErrorCode.BAD_REQUEST, "详情补全配置为空");
            }
            if (StrUtil.isBlank(sampleKey)) {
                throw new CommonException(ResponseErrorCode.BAD_REQUEST, "请提供用于试跑的样例键值");
            }
            resolveAuthReference(config);

            Map<String, Object> detail = detailEnricher.fetchDetailObject(config, detailConfig, sampleKey);

            Map<String, Object> merged = new LinkedHashMap<>();
            merged.put(PREVIEW_KEY_FIELD, sampleKey);
            if (detail != null) {
                merged.putAll(detail);
            }
            result.put("detail", detail);
            result.put("merged", merged);
            result.put("success", detail != null);
            if (detail == null) {
                result.put("error", "详情响应为空（请检查详情数据路径 itemPath）");
            }
        } catch (Exception e) {
            log.warn("Failed when testDetail: {}", e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * Execute a trial request with the given config (may be an unsaved transient config).
     * Never writes business data. On success, caches the first parsed record into the
     * config's sample_data when the config is already persisted.
     *
     * <p>启用分页时，按当前分页配置以**首页**参数构造真实请求（与采集同源，design D1/D2），
     * 使测试能反映分页参数是否被目标接口接受；结果携带 {@code pageApplied} 与首页参数取值。
     * 未启用分页时保持既有单次请求语义（design D6）。仅请求首页一次，不遍历后续页。
     */
    public Map<String, Object> testConfig(ApiConfigEntity config) {
        Map<String, Object> result = new HashMap<>();
        result.put("dataPath", StrUtil.trimToNull(config.getDataPath()));
        // 判定三键恒存在，取不到时为 null，前端据此判定「未识别」并保留用户现有设置（design C6）
        result.put("contentType", null);
        result.put("detectedRespType", null);
        result.put("detectedMultilines", null);
        result.put("pageApplied", false);
        try {
            // 临时配置需先按 authConfigId 解析认证，否则试跑不带认证（design D5/D9）
            resolveAuthReference(config);

            // 启用分页：以首页分页参数构造真实请求；否则沿用原始单次请求（design D1/D2/D6）
            ApiConfigEntity requestConfig = config;
            if (config.getPageEnabled() != null && config.getPageEnabled() == 1) {
                PageCollector.PageRequestPlan plan = pageCollector.buildFirstPageRequest(config);
                requestConfig = plan.getRequest();
                result.put("pageApplied", true);
                result.put("appliedPageParams", plan.getAppliedParams());
            }

            FetchedResponse fetched = httpFetcher.fetchResponse(requestConfig);
            result.put("rawResponse", fetched.getBody());
            applyDetection(result, fetched.getContentType());

            // 解析仍使用请求传入的原始配置：判定结论不得替换用户当前选择（design C5）
            List<Map<String, Object>> parsedData = responseParser.parse(fetched.getBody(), config);
            result.put("parsedData", parsedData);

            // 采样记录供目标字段映射使用：详情补全启用时必须为"合并详情后"的记录，
            // 否则映射面板看不到详情字段，无法配置其目标列。
            Map<String, Object> sample = buildSampleRecord(config, parsedData);
            result.put("sampleData", sample);

            result.put("success", true);

            cacheSampleData(config, sample);
        } catch (Exception e) {
            log.warn("Failed when testConfig with error: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /** 依据响应头填入判定结论，失败路径同样调用，前端才能在解析出错时看到接口实际返回的类型。 */
    private void applyDetection(Map<String, Object> result, String contentType) {
        RespTypeDetector.Detection detection = respTypeDetector.detect(contentType);
        result.put("contentType", detection.getContentType());
        result.put("detectedRespType", detection.getRespType());
        result.put("detectedMultilines", detection.getMultilines());
    }

    /**
     * 构造供目标字段映射使用的采样记录。详情补全启用时返回"列表首条 + 其详情合并"的结果，
     * 使映射面板能列出详情字段（否则无法配置详情字段的目标列）；未启用时即列表首条。
     * 补全失败时降级为列表首条——采样失败不阻断接口试跑本身。
     */
    private Map<String, Object> buildSampleRecord(ApiConfigEntity config, List<Map<String, Object>> parsedData) {
        if (parsedData == null || parsedData.isEmpty()) {
            return null;
        }
        Map<String, Object> first = parsedData.get(0);
        DetailConfig dc = config.getDetailConfig();
        if (dc == null || dc.getEnabled() == null || dc.getEnabled() != 1) {
            return first;
        }
        try {
            // 复制后补全，避免把详情并进 parsedData（响应视图应保持原始列表形态）
            List<Map<String, Object>> one = new ArrayList<>();
            one.add(new LinkedHashMap<>(first));
            detailEnricher.enrich(one, config, new StageMetrics(Stage.DETAIL, 0));
            return one.get(0);
        } catch (Exception e) {
            log.warn("Failed to enrich detail sample for api config {}: {}", config.getId(), e.getMessage());
            return first;
        }
    }

    private void cacheSampleData(ApiConfigEntity config, Map<String, Object> sample) {
        if (config.getId() == null || sample == null || sample.isEmpty()) {
            return;
        }
        try {
            ApiConfigEntity update = new ApiConfigEntity();
            update.setId(config.getId());
            // updateById 无条件写 auth_config_id，部分更新必须带上当前引用以免误清。
            update.setAuthConfigId(config.getAuthConfigId());
            update.setSampleData(sample);
            apiConfigDao.updateById(update);
        } catch (Exception e) {
            log.warn("Failed to cache sample data for api config {}: {}", config.getId(), e.getMessage());
        }
    }

    public List<Map<String, Object>> debugScript(Long id, String script, List<Map<String, Object>> sampleData) {
        return groovyTransformer.transform(sampleData, script);
    }

    /**
     * Preview the effect of the given mapping draft (and optional transform script) on
     * sample data, reusing the same FieldMapper + DataTransformChain as real collection.
     * Does not persist anything (design M7).
     *
     * @param mappings   current draft mappings; when null, falls back to persisted mappings
     * @param sampleData sample records; when empty, falls back to the cached sample_data (single record)
     */
    public List<Map<String, Object>> previewMapping(Long id, List<FieldMappingEntity> mappings,
                                                    List<Map<String, Object>> sampleData) {
        ApiConfigEntity config = getById(id);
        List<Map<String, Object>> input = resolveSample(config, sampleData);
        // 字段映射归属任务，预览仅使用调用方提供的草稿映射
        List<FieldMappingEntity> effectiveMappings =
            Optional.ofNullable(mappings).orElseGet(ArrayList::new);
        if (input.isEmpty() || effectiveMappings == null || effectiveMappings.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> mapped = fieldMapper.mapFields(input, effectiveMappings);
        return dataTransformChain.execute(mapped, config, effectiveMappings);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveSample(ApiConfigEntity config, List<Map<String, Object>> sampleData) {
        if (sampleData != null && !sampleData.isEmpty()) {
            return sampleData;
        }
        if (config.getSampleData() != null && !config.getSampleData().isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            list.add(config.getSampleData());
            return list;
        }
        return new ArrayList<>();
    }
}
