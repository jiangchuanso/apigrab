package com.gitee.apigrab.core.http;

import com.gitee.apigrab.core.auth.AuthContribution;
import com.gitee.apigrab.core.auth.AuthHandlerChain;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.common.util.JsonUtils;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.BodyType;
import com.gitee.apigrab.persistence.entity.enums.HttpMethod;
import okhttp3.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HttpRequestBuilder {

    @Resource
    private AuthHandlerChain authHandlerChain;

    public Request build(ApiConfigEntity config) {
        AuthContribution auth = authHandlerChain.handle(config);

        Headers.Builder headersBuilder = auth.getHeaders().newBuilder();
        Map<String, String> customHeaders = config.getHeaders();
        if (customHeaders != null) {
            customHeaders.forEach(headersBuilder::add);
        }

        String url = config.getUrl();
        Map<String, String> queryParams = config.getQueryParams();
        Map<String, String> authQuery = auth.getQueryParams();
        boolean hasQuery = (queryParams != null && !queryParams.isEmpty())
            || (authQuery != null && !authQuery.isEmpty());
        if (hasQuery) {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
            if (queryParams != null) {
                queryParams.forEach(urlBuilder::addQueryParameter);
            }
            if (authQuery != null) {
                authQuery.forEach(urlBuilder::addQueryParameter);
            }
            url = urlBuilder.build().toString();
        }

        Request.Builder builder = new Request.Builder()
            .url(url)
            .headers(headersBuilder.build());

        Map<String, String> authBody = auth.getBodyFields();
        boolean injectBody = authBody != null && !authBody.isEmpty();

        HttpMethod method = config.getMethod();
        if (method == HttpMethod.GET) {
            if (injectBody) {
                throw new CommonException(ResponseErrorCode.BAD_REQUEST,
                    "认证 Token 需要注入请求体，但 GET 请求没有请求体，请改用 Header 或 Query 放置");
            }
            builder.get();
        } else if (method == HttpMethod.POST) {
            builder.post(createRequestBody(config, authBody));
        } else if (method == HttpMethod.PUT) {
            builder.put(createRequestBody(config, authBody));
        } else if (method == HttpMethod.DELETE) {
            builder.delete(createRequestBody(config, authBody));
        }

        return builder.build();
    }

    private RequestBody createRequestBody(ApiConfigEntity config, Map<String, String> authBody) {
        BodyType bodyType = config.getBodyType();
        String bodyContent = config.getBodyContent();
        boolean inject = authBody != null && !authBody.isEmpty();

        if (bodyType == BodyType.JSON) {
            if (!inject) {
                return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), bodyContent);
            }
            return RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                mergeJsonBody(bodyContent, authBody));
        } else if (bodyType == BodyType.XML) {
            if (inject) {
                throw new CommonException(ResponseErrorCode.BAD_REQUEST,
                    "认证 Token 无法注入 XML 请求体，请改用 Header 或 Query 放置");
            }
            // 用 application/xml（现行标准，而非 text/xml）。此前落到兜底分支，
            // XML 正文会被发成空正文 + application/json。
            return RequestBody.create(MediaType.parse("application/xml; charset=utf-8"),
                bodyContent != null ? bodyContent : "");
        } else if (bodyType == BodyType.FORM) {
            return createFormBody(bodyContent, inject ? authBody : null);
        } else {
            if (inject) {
                throw new CommonException(ResponseErrorCode.BAD_REQUEST,
                    "当前 Body 类型为 NONE，无法注入认证 Token，请改用 Header 或 Query 放置");
            }
            return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "");
        }
    }

    /** Merge the auth token fields into an existing JSON object body (or a new object). */
    private String mergeJsonBody(String bodyContent, Map<String, String> authBody) {
        Map<String, Object> obj;
        if (bodyContent == null || bodyContent.trim().isEmpty()) {
            obj = new LinkedHashMap<>();
        } else {
            try {
                obj = JsonUtils.parseMap(bodyContent);
                if (obj == null) {
                    obj = new LinkedHashMap<>();
                }
            } catch (Exception e) {
                throw new CommonException(ResponseErrorCode.BAD_REQUEST,
                    "认证 Token 需要注入 JSON 请求体，但当前正文不是合法的 JSON 对象");
            }
        }
        obj.putAll(authBody);
        return JsonUtils.toJson(obj);
    }

    /**
     * FORM 正文契约：{@code application/x-www-form-urlencoded} 编码串（{@code k1=v1&k2=v2}）。
     *
     * <p>按 {@code &} 分组、每段按**首个** {@code =} 分键值（无 {@code =} 者值为空串），
     * 对键与值做 UTF-8 表单解码后交由 OkHttp 重新编码发送。编码态下键/值不含裸
     * {@code &}/{@code =}（分别以 {@code %26}/{@code %3D} 出现），故按裸分隔符切分是安全的。
     *
     * <p>空正文发送空表单；单段解码失败保留原文，不因个别坏片段让整个请求失败。
     * 认证注入的字段（{@code authBody}）在既有字段之后追加。
     */
    private RequestBody createFormBody(String bodyContent, Map<String, String> authBody) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (bodyContent != null && !bodyContent.isEmpty()) {
            for (String pair : bodyContent.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int idx = pair.indexOf('=');
                String rawKey = idx >= 0 ? pair.substring(0, idx) : pair;
                String rawValue = idx >= 0 ? pair.substring(idx + 1) : "";
                formBuilder.add(formDecode(rawKey), formDecode(rawValue));
            }
        }
        if (authBody != null) {
            authBody.forEach((key, value) -> {
                if (key != null && !key.trim().isEmpty()) {
                    formBuilder.add(key, value == null ? "" : value);
                }
            });
        }
        return formBuilder.build();
    }

    private String formDecode(String part) {
        try {
            return URLDecoder.decode(part, "UTF-8");
        } catch (Exception e) {
            return part;
        }
    }
}
