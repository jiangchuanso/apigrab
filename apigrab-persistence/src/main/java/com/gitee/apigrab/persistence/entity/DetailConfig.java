package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.HttpMethod;
import com.gitee.apigrab.persistence.entity.enums.BodyType;
import com.gitee.apigrab.persistence.entity.enums.RespType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

/**
 * Detail enrichment configuration (persisted as JSON in APIGRAB_API_CONFIG.detail_config).
 *
 * <p>Describes a second request that is invoked once per list record: the list record's key
 * value (extracted via {@link #keyField}) is injected into the detail request template at the
 * {@link #keyPlaceholder} token, the detail object is extracted from the response via
 * {@link #itemPath} and merged onto the record top-level (detail wins on name collision).
 */
@Getter
@Setter
public class DetailConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Whether detail enrichment is enabled (1=on, 0/null=off). */
    private Integer enabled;
    /** Key source within a list record. Empty/"#this" = the record value itself (scalar arrays). */
    private String keyField;
    /** Placeholder name injected into the detail request template. Defaults to "id". */
    private String keyPlaceholder;

    private String url;
    private HttpMethod method;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private BodyType bodyType;
    private String bodyContent;

    /** Detail response type (JSON/XML). Defaults to JSON when null. */
    private RespType respType;
    /** Path that extracts the detail object from the detail response (JSONPath/XPath). */
    private String itemPath;

    /** Bounded concurrency for detail requests. Defaults to 4 when null/non-positive. */
    private Integer concurrency;
}
