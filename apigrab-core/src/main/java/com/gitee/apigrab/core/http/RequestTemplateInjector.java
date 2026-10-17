package com.gitee.apigrab.core.http;

import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.BodyType;

import java.util.Map;

/**
 * Replaces {@code ${name}} placeholders inside a request template (url / headers / queryParams /
 * bodyContent) with a runtime value.
 *
 * <p>Shared by pagination parameter injection and detail-enrichment key injection. QUERY/BODY
 * pagination replaces headers/query/body only (url excluded to preserve existing behavior), while
 * PATH pagination also replaces the url so a page value can sit in the path; detail enrichment
 * includes the url too, so a key can sit in the path or query string — design C3.
 *
 * <p>FORM 请求体是 {@code application/x-www-form-urlencoded} 编码串，值中的占位符可能已被
 * 编码成 {@code %24%7Bid%7D}；此类正文交给 {@link FormUrlCodec} 做"解码 → 替换 → 编码"，
 * 因此原样与编码态两种写法都能命中。JSON/XML/NONE 仍按字面量替换。
 */
public final class RequestTemplateInjector {

    private RequestTemplateInjector() {
    }

    /**
     * Replace {@code ${name}} in headers / queryParams / bodyContent (not the url).
     *
     * @return {@code true} if any replacement happened
     */
    public static boolean replaceRequestFields(ApiConfigEntity config, String name, String value) {
        return replacePlaceholder(config, name, value, false);
    }

    /**
     * Replace {@code ${name}} across url / headers / queryParams / bodyContent.
     *
     * @return {@code true} if any replacement happened
     */
    public static boolean replacePlaceholder(ApiConfigEntity config, String name, String value) {
        return replacePlaceholder(config, name, value, true);
    }

    /**
     * Replace the {@code ${name}} token within the given request template.
     *
     * @param includeUrl whether the url is also eligible for replacement
     * @return {@code true} if any replacement happened
     */
    public static boolean replacePlaceholder(ApiConfigEntity config, String name, String value, boolean includeUrl) {
        if (config == null || name == null || name.isEmpty()) {
            return false;
        }
        String token = "${" + name + "}";
        String replacement = value == null ? "" : value;
        boolean replaced = false;

        if (includeUrl && config.getUrl() != null && config.getUrl().contains(token)) {
            config.setUrl(config.getUrl().replace(token, replacement));
            replaced = true;
        }
        replaced |= replaceInMap(config.getHeaders(), token, replacement);
        replaced |= replaceInMap(config.getQueryParams(), token, replacement);
        String body = config.getBodyContent();
        if (body != null) {
            if (config.getBodyType() == BodyType.FORM) {
                String replacedBody = FormUrlCodec.replacePlaceholder(body, token, replacement);
                if (replacedBody != null) {
                    config.setBodyContent(replacedBody);
                    replaced = true;
                }
            } else if (body.contains(token)) {
                config.setBodyContent(body.replace(token, replacement));
                replaced = true;
            }
        }
        return replaced;
    }

    /** Replace {@code token} in every value of the map; returns whether any value changed. */
    public static boolean replaceInMap(Map<String, String> map, String token, String value) {
        if (map == null || map.isEmpty()) {
            return false;
        }
        boolean replaced = false;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String v = entry.getValue();
            if (v != null && v.contains(token)) {
                entry.setValue(v.replace(token, value));
                replaced = true;
            }
        }
        return replaced;
    }
}
