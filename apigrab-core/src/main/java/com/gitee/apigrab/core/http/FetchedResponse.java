package com.gitee.apigrab.core.http;

/**
 * 一次 HTTP 取值的响应体与响应头片段（design C1）。
 *
 * <p>不可变值对象。字段范围刻意收窄为响应体与 {@code Content-Type} 一个响应头：
 * 判定响应格式所需的证据已经足够，后续若需展示完整响应头，在本对象上扩展即可，
 * 不构成对既有调用方的破坏性变更。
 */
public class FetchedResponse {

    private final String body;
    private final String contentType;

    public FetchedResponse(String body, String contentType) {
        this.body = body;
        this.contentType = contentType;
    }

    /** 响应体原文。 */
    public String getBody() {
        return body;
    }

    /** {@code Content-Type} 响应头；响应未携带该头时为 {@code null}。 */
    public String getContentType() {
        return contentType;
    }
}