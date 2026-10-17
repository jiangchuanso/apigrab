package com.gitee.apigrab.core.http;

import lombok.extern.slf4j.Slf4j;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class HttpFetcher {

    /** 接口配置未设置连接超时（或非正值）时的默认值（秒）。 */
    static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
    /** 接口配置未设置读取超时（或非正值）时的默认值（秒）。 */
    static final int DEFAULT_READ_TIMEOUT_SECONDS = 60;

    @Resource
    private OkHttpClient okHttpClient;

    @Resource
    private HttpRequestBuilder requestBuilder;

    /**
     * 取回响应体与 {@code Content-Type} 响应头（design C1）。
     *
     * @param config 接口配置定义
     */
    public FetchedResponse fetchResponse(ApiConfigEntity config) throws IOException {
        Request request = requestBuilder.build(config);
        OkHttpClient client = clientFor(config);
        log.info("Fetching {} {}", request.method(), request.url());
        try (Response response = execute(client, request)) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.message());
            }
            String body = response.body() != null ? response.body().string() : "";
            log.debug("Response from {}: {} bytes", request.url(), body.length());
            return new FetchedResponse(body, response.header("Content-Type"));
        }
    }

    /**
     * 只取响应体。委托给 {@link #fetchResponse(ApiConfigEntity)}，
     * 避免两份取值逻辑各自演化（既有调用点与测试 mock 依赖本签名）。
     */
    public String fetch(ApiConfigEntity config) throws IOException {
        return fetchResponse(config).getBody();
    }

    /**
     * 依据接口配置的超时派生请求客户端（design D4）。
     *
     * <p>连接/读取超时来自配置，空或非正值回退默认（10s / 60s，design D3）。派生客户端
     * 由基线 {@link OkHttpClient} 的 {@code newBuilder()} 生成，**共享**其连接池、拦截器
     * 与 TLS 等既有设置，仅覆盖这两个超时。
     */
    OkHttpClient clientFor(ApiConfigEntity config) {
        Integer connect = config == null ? null : config.getConnectTimeout();
        Integer read = config == null ? null : config.getReadTimeout();
        return okHttpClient.newBuilder()
            .connectTimeout(resolveTimeoutSeconds(connect, DEFAULT_CONNECT_TIMEOUT_SECONDS), TimeUnit.SECONDS)
            .readTimeout(resolveTimeoutSeconds(read, DEFAULT_READ_TIMEOUT_SECONDS), TimeUnit.SECONDS)
            .build();
    }

    /** 空或非正值回退 {@code fallback}，否则取原值（design D3，唯一事实来源）。 */
    static int resolveTimeoutSeconds(Integer value, int fallback) {
        return (value == null || value <= 0) ? fallback : value;
    }

    /**
     * 执行请求，并把传输层异常换成可操作的中文提示。
     *
     * <p>原始异常作为 cause 保留（不吞栈）。识别不出具体场景时消息原样透传，行为与改动前一致。
     */
    private Response execute(OkHttpClient client, Request request) throws IOException {
        try {
            return client.newCall(request).execute();
        } catch (IOException e) {
            throw new IOException(HttpFailureHints.describe(e, request.url().toString()), e);
        }
    }
}
