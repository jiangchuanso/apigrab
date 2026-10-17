package com.gitee.apigrab.core.collector;

import com.gitee.apigrab.core.http.HttpFetcher;
import com.gitee.apigrab.core.http.HttpResponseParser;
import com.gitee.apigrab.core.http.RequestTemplateInjector;
import com.jayway.jsonpath.JsonPath;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.DetailConfig;
import com.gitee.apigrab.persistence.entity.enums.HttpMethod;
import com.gitee.apigrab.persistence.entity.enums.RespType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 详情补全：对列表页内的每条记录，用其键值调用详情接口，并把详情对象**顶层合并**到记录上
 * （同名以详情为准）。
 *
 * <p>设计约束（见 design.md）：
 * <ul>
 *   <li>C1：未启用（{@code detailConfig}/{@code enabled} 为空或非 1）时原样返回、零请求。</li>
 *   <li>C6：单条详情失败（超时/报错/无数据/键值为空）→ 记录保留、计入失败样本、不抛出。</li>
 *   <li>C8：有界并发（默认 4），同页结果按列表原序回填。</li>
 *   <li>C9：提交/获取前检查线程中断。</li>
 *   <li>C10：详情请求继承列表接口的认证与超时；响应支持 JSON/XML（复用响应解析器）。</li>
 * </ul>
 */
@Component
@Slf4j
public class DetailEnricher {

    /** 默认并发（design C8）。 */
    static final int DEFAULT_CONCURRENCY = 4;
    /** "取记录自身值"的约定键值来源（标量/字符串数组归一化后的 {@code _value}）。 */
    static final String KEY_FIELD_SELF = "#this";
    /** 键值占位符默认名（design C3）。 */
    static final String DEFAULT_KEY_PLACEHOLDER = "id";

    @Resource
    private HttpFetcher httpFetcher;

    @Resource
    private HttpResponseParser responseParser;

    /**
     * 就地补全一页记录：成功者合并详情（顶层、详情覆盖列表），失败者保留并计入失败样本。
     *
     * @param detailStage 详情补全阶段的计数与失败样本载体；调用方负责其生命周期
     * @return 与入参同一列表（已就地变更）
     */
    public List<Map<String, Object>> enrich(List<Map<String, Object>> records,
                                            ApiConfigEntity config,
                                            StageMetrics detailStage) {
        DetailConfig dc = config == null ? null : config.getDetailConfig();
        if (dc == null || dc.getEnabled() == null || dc.getEnabled() != 1
                || records == null || records.isEmpty()) {
            return records;
        }

        int n = records.size();
        int concurrency = resolveConcurrency(dc, n);
        detailStage.setInputCount(n);

        ExecutorService pool = Executors.newFixedThreadPool(concurrency, daemonFactory());
        List<Future<Boolean>> futures = new ArrayList<>(n);
        try {
            for (int i = 0; i < n; i++) {
                final Map<String, Object> record = records.get(i);
                futures.add(pool.submit(() -> enrichOne(record, config, dc, detailStage)));
            }
            for (int i = 0; i < n; i++) {
                boolean success;
                try {
                    success = futures.get(i).get();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Task execution interrupted", ie);
                } catch (ExecutionException ee) {
                    success = false;
                }
                if (success) {
                    detailStage.setSuccessCount(detailStage.getSuccessCount() + 1);
                    detailStage.setOutputCount(detailStage.getOutputCount() + 1);
                } else {
                    detailStage.setFailCount(detailStage.getFailCount() + 1);
                }
            }
        } finally {
            pool.shutdownNow();
        }
        return records;
    }

    /**
     * 补全单条记录。失败按 KEEP_PARENT 语义处理（记录不动、计入失败样本、返回 false）。
     * 供批量路径与「补全试跑」复用。
     */
    public boolean enrichOne(Map<String, Object> record, ApiConfigEntity config,
                             DetailConfig dc, StageMetrics detailStage) {
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeException("Task execution interrupted");
        }
        try {
            Object key = resolveKey(record, dc.getKeyField());
            if (key == null) {
                recordFailure(detailStage, "详情 key 为空", record);
                return false;
            }
            Map<String, Object> detail = fetchDetailObject(config, dc, String.valueOf(key));
            if (detail == null) {
                recordFailure(detailStage, "详情响应为空", record);
                return false;
            }
            record.putAll(detail);
            return true;
        } catch (Exception e) {
            log.debug("Detail enrichment failed for a record: {}", e.getMessage());
            recordFailure(detailStage, e.getMessage(), record);
            return false;
        }
    }

    /**
     * 以单个键值发起一次详情请求并返回详情对象（抽取失败或无数据时返回 {@code null}）。
     * 供补全试跑复用（design C11）。
     */
    public Map<String, Object> fetchDetailObject(ApiConfigEntity config, DetailConfig dc, String keyValue) throws Exception {
        ApiConfigEntity detailRequest = buildDetailRequest(config, dc);
        RequestTemplateInjector.replacePlaceholder(detailRequest, keyPlaceholder(dc), keyValue);
        String body = httpFetcher.fetch(detailRequest);
        List<Map<String, Object>> detail = responseParser.parse(body, detailRequest);
        return detail.isEmpty() ? null : detail.get(0);
    }

    /** 按 keyField 从记录取键值：空/{@code #this} 取归一化后的记录自身值；{@code $} 前缀走 JSONPath；否则字段名。 */
    public Object resolveKey(Map<String, Object> record, String keyField) {
        String field = keyField == null ? "" : keyField.trim();
        if (field.isEmpty() || KEY_FIELD_SELF.equals(field)) {
            return record.get(HttpResponseParser.SCALAR_VALUE_KEY);
        }
        if (field.startsWith("$")) {
            try {
                return JsonPath.read(record, field);
            } catch (Exception e) {
                return null;
            }
        }
        return record.get(field);
    }

    private void recordFailure(StageMetrics detailStage, String reason, Map<String, Object> record) {
        if (detailStage != null) {
            detailStage.getFailureCollector().add(reason, record);
        }
    }

    private ApiConfigEntity buildDetailRequest(ApiConfigEntity parent, DetailConfig dc) {
        ApiConfigEntity b = new ApiConfigEntity();
        b.setUrl(dc.getUrl());
        b.setMethod(dc.getMethod() != null ? dc.getMethod() : HttpMethod.GET);
        b.setHeaders(copyMap(dc.getHeaders()));
        b.setQueryParams(copyMap(dc.getQueryParams()));
        b.setBodyType(dc.getBodyType());
        b.setBodyContent(dc.getBodyContent());
        // C10：继承列表接口的认证与超时
        b.setConnectTimeout(parent == null ? null : parent.getConnectTimeout());
        b.setReadTimeout(parent == null ? null : parent.getReadTimeout());
        b.setAuthConfigId(parent == null ? null : parent.getAuthConfigId());
        b.setAuthType(parent == null ? null : parent.getAuthType());
        b.setAuthConfig(parent == null ? null : parent.getAuthConfig());
        b.setRespType(dc.getRespType() != null ? dc.getRespType() : RespType.JSON);
        b.setDataPath(dc.getItemPath());
        return b;
    }

    private String keyPlaceholder(DetailConfig dc) {
        return Optional.ofNullable(dc.getKeyPlaceholder())
                .filter(v -> !v.trim().isEmpty())
                .orElse(DEFAULT_KEY_PLACEHOLDER);
    }

    private int resolveConcurrency(DetailConfig dc, int recordCount) {
        Integer configured = dc.getConcurrency();
        int concurrency = (configured == null || configured <= 0) ? DEFAULT_CONCURRENCY : configured;
        return Math.max(1, Math.min(concurrency, recordCount));
    }

    private Map<String, String> copyMap(Map<String, String> source) {
        return Optional.ofNullable(source).map(LinkedHashMap::new).orElse(null);
    }

    private ThreadFactory daemonFactory() {
        AtomicInteger seq = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, "detail-enrich-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }
}
