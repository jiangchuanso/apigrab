package com.gitee.apigrab.core.collector;

import cn.hutool.core.util.XmlUtil;
import com.gitee.apigrab.core.http.HttpFetcher;
import com.gitee.apigrab.core.http.HttpResponseParser;
import com.gitee.apigrab.core.http.RequestTemplateInjector;
import com.gitee.apigrab.common.util.JsonUtils;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.PageConfig;
import com.gitee.apigrab.persistence.entity.enums.BodyType;
import com.gitee.apigrab.persistence.entity.enums.PaginationMode;
import com.gitee.apigrab.persistence.entity.enums.ParamLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Resource;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

@Component
public class PageCollector {

    private static final Logger log = LoggerFactory.getLogger(PageCollector.class);

    @Resource
    private HttpFetcher httpFetcher;

    @Resource
    private HttpResponseParser responseParser;

    /**
     * 分页采集结果封装，包含数据记录和分页信息
     */
    public static class CollectAllResult {
        private final List<Map<String, Object>> records;
        private final int pageCount;

        public CollectAllResult(List<Map<String, Object>> records, int pageCount) {
            this.records = records;
            this.pageCount = pageCount;
        }

        public List<Map<String, Object>> getRecords() { return records; }
        public int getPageCount() { return pageCount; }
    }

    /** 单个分页参数的注入描述。name 为配置字段名，同时用作占位符名与请求体键名。 */
    private static class PageParam {
        final String value;
        final String name;

        PageParam(String value, String name) {
            this.value = value;
            this.name = name;
        }
    }

    /**
     * 逐页消费回调。每成功取到一页（非空）即回调一次；返回 {@code false} 表示停止后续页。
     *
     * <p>回调在执行页级请求/解析的 try/catch **之外**触发，因此回调内抛出的异常
     * （如转换失败、写库连接失败）会向上传播，而不会被当作"页请求失败"静默吞掉。
     */
    @FunctionalInterface
    public interface PageHandler {
        boolean onPage(List<Map<String, Object>> records, int pageIndex);
    }

    public List<Map<String, Object>> collectAll(ApiConfigEntity config) {
        return collectAllWithPageInfo(config).getRecords();
    }

    /**
     * 逐页流式采集入口：与 {@link #collectAllWithPageInfo} 共用分页推进与参数注入逻辑，
     * 但不同时把所有页保留在内存，而是每页回调一次由调用方即时处理（转换/写库）。
     *
     * @return 实际采集到的非空页数（不分页时为 1）
     */
    public int collectPages(ApiConfigEntity config, PageHandler handler) {
        if (config.getPageEnabled() == null || config.getPageEnabled() != 1) {
            // 不分页：单页处理
            List<Map<String, Object>> records;
            try {
                String response = httpFetcher.fetch(config);
                records = responseParser.parse(response, config);
            } catch (Exception e) {
                log.error("Failed to fetch API: {}", config.getUrl(), e);
                throw new RuntimeException("API fetch failed: " + e.getMessage(), e);
            }
            handler.onPage(records, 1);
            return 1;
        }

        // 分页采集
        PageConfig pc = Optional.ofNullable(config.getPageConfig()).orElseGet(PageConfig::new);

        if (isBodyLocation(pc)) {
            BodyType bodyType = config.getBodyType();
            if (bodyType != BodyType.JSON && bodyType != BodyType.FORM && bodyType != BodyType.XML) {
                throw new RuntimeException("请求体分页需要 bodyType 为 JSON、FORM 或 XML，当前为: " + bodyType);
            }
        }

        PaginationMode pageMode = Optional.ofNullable(pc.getMode()).orElse(PaginationMode.PAGE_NUMBER);
        int maxPages = intVal(pc.getMaxPages(), 100);
        int totalPages = intVal(pc.getTotalPages(), 0);
        boolean stopByBatchSize = Optional.ofNullable(pc.getStopByBatchSize()).orElse(true);

        switch (pageMode) {
            case OFFSET:
                return collectByOffset(config, pc, maxPages, totalPages, stopByBatchSize, handler);
            case CURSOR:
                return collectByCursor(config, pc, maxPages, handler);
            case PAGE_NUMBER:
            default:
                return collectByPageNumber(config, pc, maxPages, totalPages, stopByBatchSize, handler);
        }
    }

    public CollectAllResult collectAllWithPageInfo(ApiConfigEntity config) {
        List<Map<String, Object>> allRecords = new ArrayList<>();
        int actualPages = collectPages(config, (records, pageIndex) -> {
            allRecords.addAll(records);
            return true;
        });
        return new CollectAllResult(allRecords, actualPages);
    }

    /**
     * 「首页请求」构建结果：注入了首页分页参数的请求实体，以及首页参数取值（name -&gt; value）。
     * 供连接测试复用采集的注入逻辑，使测试与采集同源。
     */
    public static class PageRequestPlan {
        private final ApiConfigEntity request;
        private final Map<String, String> appliedParams;

        public PageRequestPlan(ApiConfigEntity request, Map<String, String> appliedParams) {
            this.request = request;
            this.appliedParams = appliedParams;
        }

        public ApiConfigEntity getRequest() {
            return request;
        }

        /** 首页参数取值（参数名 -&gt; 取值），用于向用户展示本次按分页配置请求了哪些参数。 */
        public Map<String, String> getAppliedParams() {
            return appliedParams;
        }
    }

    /**
     * 按当前分页配置构建「首页请求」（页码=1 / 偏移=0 / 初始游标），并返回首页参数取值。
     *
     * <p>注入与采集**同源**：复用 {@link #createPagedConfig} + {@link #applyPageParams}
     * （占位符替换优先、否则固定键注入），参数名兜底默认与采集循环逐字一致。
     * 本方法不修改传入的 config，也不参与多页遍历。
     *
     * @throws RuntimeException 参数位置为 BODY 而正文类型不支持注入时（信息与采集一致）
     */
    public PageRequestPlan buildFirstPageRequest(ApiConfigEntity config) {
        PageConfig pc = Optional.ofNullable(config.getPageConfig()).orElseGet(PageConfig::new);

        ParamLocation location = resolveLocation(pc);
        if (location == ParamLocation.BODY) {
            BodyType bodyType = config.getBodyType();
            if (bodyType != BodyType.JSON && bodyType != BodyType.FORM && bodyType != BodyType.XML) {
                throw new RuntimeException("请求体分页需要 bodyType 为 JSON、FORM 或 XML，当前为: " + bodyType);
            }
        }

        PaginationMode pageMode = Optional.ofNullable(pc.getMode()).orElse(PaginationMode.PAGE_NUMBER);
        int pageSize = intVal(pc.getPageSize(), 50);

        List<PageParam> params;
        switch (pageMode) {
            case OFFSET:
                params = Arrays.asList(
                    new PageParam("0", str(pc.getOffsetParam(), "offset")),
                    new PageParam(String.valueOf(pageSize), str(pc.getLimitParam(), "limit")));
                break;
            case CURSOR:
                params = Collections.singletonList(
                    new PageParam(Optional.ofNullable(pc.getInitialCursor()).orElse(""),
                        str(pc.getCursorParam(), "cursor")));
                break;
            case PAGE_NUMBER:
            default:
                params = Arrays.asList(
                    new PageParam("1", str(pc.getPageParam(), "page")),
                    new PageParam(String.valueOf(pageSize), str(pc.getSizeParam(), "pageSize")));
        }

        ApiConfigEntity paged = createPagedConfig(config, config.getUrl());
        applyPageParams(paged, location, params);

        Map<String, String> applied = new LinkedHashMap<>();
        for (PageParam param : params) {
            applied.put(param.name, param.value);
        }
        return new PageRequestPlan(paged, applied);
    }

    private int collectByPageNumber(ApiConfigEntity config, PageConfig pc, int maxPages,
                                    int totalPages, boolean stopByBatchSize, PageHandler handler) {
        ParamLocation location = resolveLocation(pc);
        int page = 1;
        int pagesCollected = 0;
        int pageSize = intVal(pc.getPageSize(), 50);
        String pageParam = str(pc.getPageParam(), "page");
        String sizeParam = str(pc.getSizeParam(), "pageSize");

        while (page <= maxPages) {
            if (totalPages > 0 && page > totalPages) {
                break;
            }
            checkInterrupted();
            List<Map<String, Object>> records;
            try {
                ApiConfigEntity paged = createPagedConfig(config, config.getUrl());
                applyPageParams(paged, location, Arrays.asList(
                    new PageParam(String.valueOf(page), pageParam),
                    new PageParam(String.valueOf(pageSize), sizeParam)));
                String response = httpFetcher.fetch(paged);
                records = responseParser.parse(response, config);
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new RuntimeException("Task execution interrupted", e);
                }
                log.error("Page collection failed at page {}", page, e);
                break;
            }

            if (records.isEmpty()) {
                break;
            }
            pagesCollected++;
            boolean proceed = handler.onPage(records, pagesCollected);
            page++;
            if (!proceed) {
                break;
            }
            if (stopByBatchSize && records.size() < pageSize) {
                break;
            }
            sleepInterval(pc);
        }
        return pagesCollected;
    }

    private int collectByOffset(ApiConfigEntity config, PageConfig pc, int maxPages,
                                int totalPages, boolean stopByBatchSize, PageHandler handler) {
        ParamLocation location = resolveLocation(pc);
        int offset = 0;
        int pagesCollected = 0;
        int pageSize = intVal(pc.getPageSize(), 50);
        String offsetParam = str(pc.getOffsetParam(), "offset");
        String limitParam = str(pc.getLimitParam(), "limit");

        for (int i = 0; i < maxPages; i++) {
            if (totalPages > 0 && (i + 1) > totalPages) {
                break;
            }
            checkInterrupted();
            List<Map<String, Object>> records;
            try {
                ApiConfigEntity paged = createPagedConfig(config, config.getUrl());
                applyPageParams(paged, location, Arrays.asList(
                    new PageParam(String.valueOf(offset), offsetParam),
                    new PageParam(String.valueOf(pageSize), limitParam)));
                String response = httpFetcher.fetch(paged);
                records = responseParser.parse(response, config);
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new RuntimeException("Task execution interrupted", e);
                }
                log.error("Offset collection failed at offset {}", offset, e);
                break;
            }

            if (records.isEmpty()) {
                break;
            }
            pagesCollected++;
            boolean proceed = handler.onPage(records, pagesCollected);
            offset += pageSize;
            if (!proceed) {
                break;
            }
            if (stopByBatchSize && records.size() < pageSize) {
                break;
            }
            sleepInterval(pc);
        }
        return pagesCollected;
    }

    private int collectByCursor(ApiConfigEntity config, PageConfig pc, int maxPages, PageHandler handler) {
        ParamLocation location = resolveLocation(pc);
        String cursorParam = str(pc.getCursorParam(), "cursor");
        String cursor = Optional.ofNullable(pc.getInitialCursor()).orElse("");
        String previousCursor = null;
        int pagesCollected = 0;

        for (int i = 0; i < maxPages; i++) {
            checkInterrupted();
            List<Map<String, Object>> records;
            String nextCursor;
            try {
                ApiConfigEntity paged = createPagedConfig(config, config.getUrl());
                applyPageParams(paged, location, Collections.singletonList(
                    new PageParam(cursor, cursorParam)));
                String response = httpFetcher.fetch(paged);

                // 尝试从 JSONPath 响应中提取下一页游标
                nextCursor = extractNextCursor(response, pc);
                records = responseParser.parse(response, config);
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new RuntimeException("Task execution interrupted", e);
                }
                log.error("Cursor collection failed", e);
                break;
            }

            if (records.isEmpty()) {
                break;
            }
            pagesCollected++;
            boolean proceed = handler.onPage(records, pagesCollected);
            if (!proceed) {
                break;
            }

            if (nextCursor == null || nextCursor.isEmpty() || nextCursor.equals(previousCursor)) {
                break;
            }
            previousCursor = cursor;
            cursor = nextCursor;
            sleepInterval(pc);
        }
        return pagesCollected;
    }

    /** 分页循环中的中断检查：超时取消后立即停止后续拉页/写库，避免继续消耗。 */
    private void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeException("Task execution interrupted");
        }
    }

    /**
     * 应用单页参数：先做占位符替换；已以占位符出现的参数跳过固定键注入（去重）；
     * 其余参数按参数位置注入：
     * <ul>
     *   <li>PATH：URL / 请求头 / 查询参数 / 请求体中的 {@code ${参数名}} 优先替换；均未出现时
     *       退化为查询参数追加，避免路径占位符漏配导致参数静默丢失；</li>
     *   <li>BODY：注入请求体（同名键替换、缺失则新增）；</li>
     *   <li>QUERY（默认）：替换已存在的同名查询参数（URL 查询串或 queryParams），
     *       两处都没有时才追加，避免请求中出现重复的分页参数。</li>
     * </ul>
     *
     * <p>占位符命中时不再注入固定键，但 QUERY 位置仍需清理 URL 中残留的同名参数：
     * 占位符可能只出现在请求头或 queryParams 中，此时 URL 里的旧取值会与替换结果一起发出，
     * 造成两份同名参数。
     */
    private void applyPageParams(ApiConfigEntity config, ParamLocation location, List<PageParam> params) {
        for (PageParam param : params) {
            if (location == ParamLocation.PATH) {
                // PATH 需要替换 URL 中的占位符，故使用包含 URL 的替换入口
                if (RequestTemplateInjector.replacePlaceholder(config, param.name, param.value)) {
                    continue;
                }
                config.setUrl(appendQueryParam(config.getUrl(), param.name, param.value));
                continue;
            }
            boolean placeholderReplaced = RequestTemplateInjector.replaceRequestFields(config, param.name, param.value);
            if (location == ParamLocation.BODY) {
                if (!placeholderReplaced) {
                    injectBodyParam(config, param.name, param.value);
                }
                continue;
            }
            if (placeholderReplaced) {
                // 占位符已在请求头/查询参数中替换；移除 URL 中残留的同名参数，避免最终请求出现两份
                String rewritten = rewriteUrlQueryParam(config.getUrl(), param.name, null);
                if (rewritten != null) {
                    config.setUrl(rewritten);
                }
                continue;
            }
            if (!replaceQueryParam(config, param.name, param.value)) {
                config.setUrl(appendQueryParam(config.getUrl(), param.name, param.value));
            }
        }
    }

    /**
     * 向请求体注入一个参数：JSON 递归替换同名键，找不到则顶层新增；FORM 追加/替换表单字段；
     * XML 按元素名就地替换，找不到则在根元素下新增（语义与 JSON 对齐，见 design D1）。
     */
    private void injectBodyParam(ApiConfigEntity config, String key, String value) {
        BodyType bodyType = config.getBodyType();
        if (bodyType == BodyType.JSON) {
            Map<String, Object> obj = JsonUtils.parseMap(config.getBodyContent());
            if (obj == null) {
                obj = new LinkedHashMap<>();
            }
            if (!setRecursively(obj, key, value)) {
                obj.put(key, value);
            }
            config.setBodyContent(JsonUtils.toJson(obj));
        } else if (bodyType == BodyType.FORM) {
            config.setBodyContent(setFormParam(config.getBodyContent(), key, value));
        } else if (bodyType == BodyType.XML) {
            config.setBodyContent(setXmlParam(config.getBodyContent(), key, value));
        } else {
            throw new RuntimeException("请求体分页需要 bodyType 为 JSON、FORM 或 XML，当前为: " + bodyType);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean setRecursively(Map<String, Object> map, String key, Object value) {
        boolean found = false;
        if (map.containsKey(key)) {
            map.put(key, value);
            found = true;
        }
        for (Object v : map.values()) {
            if (v instanceof Map) {
                if (setRecursively((Map<String, Object>) v, key, value)) {
                    found = true;
                }
            }
        }
        return found;
    }

    /**
     * 向 XML 请求体注入一个参数：按元素名在所有层级就地替换内容，找不到则在根元素下新增。
     *
     * <p><b>根元素不参与匹配</b>：{@code getElementsByTagName} 会把根元素本身也算作匹配
     * （实测 {@code parseXml("<page/>")} 的 {@code getElementsByTagName("page").getLength() == 1}），
     * 若分页参数名与根标签同名，整份文档会被塌缩成一个元素。故遍历从根元素的子节点开始，
     * 这正对应 JSON 的「文档对象 = 根元素、顶层键 = 根的子元素」。
     */
    private String setXmlParam(String content, String key, String value) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("XML 请求体为空，无法注入分页参数 " + key + "（至少需要一个根元素）");
        }
        Document doc;
        try {
            doc = XmlUtil.parseXml(content);
        } catch (Exception e) {
            throw new RuntimeException("XML 请求体解析失败，无法注入分页参数 " + key + "：" + e.getMessage(), e);
        }
        Element root = doc.getDocumentElement();
        if (root == null) {
            throw new RuntimeException("XML 请求体缺少根元素，无法注入分页参数 " + key);
        }
        if (!replaceXmlElements(root, key, value)) {
            Element added = doc.createElement(key);
            added.setTextContent(value);
            root.appendChild(added);
        }
        // toStr 恒输出 XML 声明并压缩空白（design D3）：注入后请求体不会与用户输入逐字相同
        return XmlUtil.toStr(doc);
    }

    /**
     * 在 parent 的子孙元素中按名称就地替换内容；返回是否命中。
     *
     * <p>命中后仍继续搜索其余分支，与 JSON 的 {@link #setRecursively} 替换「全部同名键」一致。
     * 名称同时接受限定名与本地名：有命名空间时（{@code ns:page}）只写 {@code page} 也能命中。
     */
    private boolean replaceXmlElements(Node parent, String key, String value) {
        boolean found = false;
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) node;
            if (key.equals(element.getTagName()) || key.equals(element.getLocalName())) {
                // 与 JSON 一致：就地替换内容；若该元素含子元素，子元素会被一并替换掉（behavior B2）
                element.setTextContent(value);
                found = true;
                // setTextContent 已移除全部子节点，无需再向下递归
                continue;
            }
            if (replaceXmlElements(element, key, value)) {
                found = true;
            }
        }
        return found;
    }

    private String extractNextCursor(String response, PageConfig pc) {
        String cursorPath = str(pc.getCursorPath(), "$.next_cursor");
        try {
            Object cursor = com.jayway.jsonpath.JsonPath.read(response, cursorPath);
            return Optional.ofNullable(cursor).map(Object::toString).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private ParamLocation resolveLocation(PageConfig pc) {
        return Optional.ofNullable(pc.getParamLocation()).orElse(ParamLocation.QUERY);
    }

    private boolean isBodyLocation(PageConfig pc) {
        return pc.getParamLocation() == ParamLocation.BODY;
    }

    private String str(String value, String def) {
        if (value == null || value.trim().isEmpty()) {
            return def;
        }
        return value;
    }

    private int intVal(Integer value, int def) {
        return Optional.ofNullable(value).orElse(def);
    }

    private void sleepInterval(PageConfig pc) {
        int interval = intVal(pc.getRequestIntervalMs(), 0);
        if (interval > 0) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 向 FORM 正文注入一个参数。
     *
     * <p>FORM 正文契约是 {@code application/x-www-form-urlencoded} 编码串，故注入的键/值必须先按标准
     * 表单规则编码再写入，保持整串处于一致编码态（值里的 {@code &}/{@code =} 不会破坏分隔）。
     * 已存在的段按原样保留（其本身已是编码态）。
     */
    private String setFormParam(String content, String key, String value) {
        Map<String, String> form = new LinkedHashMap<>();
        if (content != null && !content.isEmpty()) {
            for (String pair : content.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int idx = pair.indexOf('=');
                if (idx >= 0) {
                    form.put(pair.substring(0, idx), pair.substring(idx + 1));
                } else {
                    form.put(pair, "");
                }
            }
        }
        form.put(urlEncode(key), urlEncode(value));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }

    private String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * QUERY 位置写入一个分页参数：命中同名查询参数时就地替换，否则返回 {@code false} 交由调用方追加。
     *
     * <p>查询参数可能同时存在于两处：用户直接写进 url 的查询串，以及 queryParams 表单配置。
     * {@code HttpRequestBuilder} 会把 queryParams 追加到 url 之后，因此两处同名时请求会带上两个
     * 同名参数（多数服务端只取第一个），分页取值可能不生效。处理策略：
     * <ul>
     *   <li>queryParams 有同名键：更新该键，并从 url 查询串中移除同名键（保留 map 作为唯一来源）；</li>
     *   <li>只有 url 有同名键：就地替换首个同名项的取值，丢弃其余同名重复项（避免请求带多份同名参数）；</li>
     *   <li>两处都没有：返回 false。</li>
     * </ul>
     * 比较参数名时兼容 url 中的百分号编码（如 {@code %E9%A1%B5%E7%A0%81} 对应 {@code 页码}）。
     */
    private boolean replaceQueryParam(ApiConfigEntity config, String name, String value) {
        boolean inQueryParams = false;
        Map<String, String> queryParams = config.getQueryParams();
        if (queryParams != null && queryParams.containsKey(name)) {
            queryParams.put(name, value);
            inQueryParams = true;
        }
        // map 命中时从 url 中移除同名键，避免 HttpRequestBuilder 追加后出现两份
        String rewritten = rewriteUrlQueryParam(config.getUrl(), name, inQueryParams ? null : value);
        if (rewritten != null) {
            config.setUrl(rewritten);
            return true;
        }
        return inQueryParams;
    }

    /**
     * 就地改写 url 查询串中的同名参数：{@code value} 为 null 时删除该参数，否则把首个同名项的取值
     * 替换为 {@code value} 并丢弃其余同名重复项。保留其它参数、参数顺序与 {@code #fragment}；
     * 未命中时返回 null（调用方据此决定是否追加）。
     */
    private String rewriteUrlQueryParam(String url, String name, String value) {
        if (url == null || name == null || name.isEmpty()) {
            return null;
        }
        int queryStart = url.indexOf('?');
        if (queryStart < 0) {
            return null;
        }
        int fragmentStart = url.indexOf('#', queryStart);
        String fragment = fragmentStart >= 0 ? url.substring(fragmentStart) : "";
        int queryEnd = fragmentStart >= 0 ? fragmentStart : url.length();
        String base = url.substring(0, queryStart);
        String query = url.substring(queryStart + 1, queryEnd);

        List<String> kept = new ArrayList<>();
        boolean matched = false;
        boolean valueWritten = false;
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String rawName = eq >= 0 ? pair.substring(0, eq) : pair;
            if (!sameParamName(rawName, name)) {
                kept.add(pair);
                continue;
            }
            matched = true;
            // 同名重复项收敛为一份：只保留首个出现的参数名
            if (value != null && !valueWritten) {
                kept.add(rawName + "=" + value);
                valueWritten = true;
            }
        }
        if (!matched) {
            return null;
        }
        String rebuilt = String.join("&", kept);
        return rebuilt.isEmpty() ? base + fragment : base + "?" + rebuilt + fragment;
    }

    private boolean sameParamName(String rawName, String name) {
        if (rawName.equals(name)) {
            return true;
        }
        try {
            return URLDecoder.decode(rawName, "UTF-8").equals(name);
        } catch (Exception e) {
            // 非法百分号编码：按字面量比较，不因用户 url 写错而中断分页
            return false;
        }
    }

    private String appendQueryParam(String url, String paramName, String paramValue) {
        if (url.contains("?")) {
            return url + "&" + paramName + "=" + paramValue;
        } else {
            return url + "?" + paramName + "=" + paramValue;
        }
    }

    private ApiConfigEntity createPagedConfig(ApiConfigEntity original, String pagedUrl) {
        ApiConfigEntity copy = new ApiConfigEntity();
        copy.setId(original.getId());
        copy.setName(original.getName());
        copy.setUrl(pagedUrl);
        copy.setMethod(original.getMethod());
        copy.setHeaders(copyMap(original.getHeaders()));
        copy.setQueryParams(copyMap(original.getQueryParams()));
        copy.setBodyType(original.getBodyType());
        copy.setBodyContent(original.getBodyContent());
        copy.setAuthConfigId(original.getAuthConfigId());
        copy.setAuthType(original.getAuthType());
        copy.setAuthConfig(original.getAuthConfig());
        copy.setRespType(original.getRespType());
        copy.setDataPath(original.getDataPath());
        copy.setTransformType(original.getTransformType());
        copy.setConvertScript(original.getConvertScript());
        copy.setEnableMultilines(original.getEnableMultilines());
        return copy;
    }

    private Map<String, String> copyMap(Map<String, String> source) {
        return Optional.ofNullable(source).map(LinkedHashMap::new).orElse(null);
    }
}
