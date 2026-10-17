package com.gitee.apigrab.core.http;

import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link HttpFetcher} 契约测试。
 *
 * <p>{@code okhttp3.Response} 在 OkHttp 3.14.9 中是 final 类，本项目只有 mockito-core
 * （子类 mock maker），无法 mock；mockwebserver 也不在本地仓库。因此改用 JDK 自带的
 * {@link HttpServer} 在临时端口起真实回环服务，配合真实 {@link OkHttpClient}，
 * 仅 mock 请求构造职责——这样 Content-Type 响应头是真正经过 HTTP 传输的。
 */
class HttpFetcherTest {

    private HttpServer server;
    private HttpFetcher fetcher;

    private volatile String responseBody = "{}";
    private volatile String responseContentType;
    private volatile int responseStatus = 200;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/probe", exchange -> {
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            if (responseContentType != null) {
                exchange.getResponseHeaders().set("Content-Type", responseContentType);
            }
            exchange.sendResponseHeaders(responseStatus, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/probe";
        HttpRequestBuilder requestBuilder = mock(HttpRequestBuilder.class);
        when(requestBuilder.build(any(ApiConfigEntity.class)))
                .thenAnswer(invocation -> new Request.Builder().url(url).build());

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();

        fetcher = new HttpFetcher();
        inject(fetcher, "okHttpClient", client);
        inject(fetcher, "requestBuilder", requestBuilder);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchResponseReturnsBodyAndContentTypeHeader() throws IOException {
        responseContentType = "application/json; charset=utf-8";
        responseBody = "{\"id\":1}";

        FetchedResponse result = fetcher.fetchResponse(new ApiConfigEntity());

        assertEquals("{\"id\":1}", result.getBody());
        assertEquals("application/json; charset=utf-8", result.getContentType());
    }

    @Test
    void fetchResponseReturnsNullContentTypeWhenHeaderAbsent() throws IOException {
        responseContentType = null;
        responseBody = "{\"id\":2}";

        FetchedResponse result = fetcher.fetchResponse(new ApiConfigEntity());

        assertEquals("{\"id\":2}", result.getBody());
        assertNull(result.getContentType(), "响应未携带 Content-Type 时必须为 null，不得推断默认值");
    }

    @Test
    void fetchResponseThrowsIoExceptionOnNonSuccessStatus() {
        responseStatus = 500;
        responseContentType = "text/plain";
        responseBody = "boom";

        IOException error = assertThrows(IOException.class,
                () -> fetcher.fetchResponse(new ApiConfigEntity()));

        assertTrue(error.getMessage().startsWith("HTTP 500: "),
                "异常消息格式须与改动前一致，实际为: " + error.getMessage());
    }

    @Test
    void fetchDelegatesToFetchResponseAndReturnsBodyOnly() throws IOException {
        responseContentType = "application/xml";
        responseBody = "<root><a>1</a></root>";

        String body = fetcher.fetch(new ApiConfigEntity());

        assertEquals("<root><a>1</a></root>", body);
    }

    @Test
    void clientForAppliesConfiguredTimeouts() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setConnectTimeout(5);
        config.setReadTimeout(120);

        OkHttpClient client = fetcher.clientFor(config);

        assertEquals(5000, client.connectTimeoutMillis());
        assertEquals(120000, client.readTimeoutMillis());
    }

    @Test
    void clientForFallsBackToDefaultsWhenUnset() {
        OkHttpClient client = fetcher.clientFor(new ApiConfigEntity());

        assertEquals(10000, client.connectTimeoutMillis());
        assertEquals(60000, client.readTimeoutMillis());
    }

    @Test
    void clientForFallsBackToDefaultsWhenNonPositive() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setConnectTimeout(0);
        config.setReadTimeout(-5);

        OkHttpClient client = fetcher.clientFor(config);

        assertEquals(10000, client.connectTimeoutMillis());
        assertEquals(60000, client.readTimeoutMillis());
    }

    @Test
    void resolveTimeoutSecondsBoundaries() {
        assertEquals(10, HttpFetcher.resolveTimeoutSeconds(null, 10));
        assertEquals(10, HttpFetcher.resolveTimeoutSeconds(0, 10));
        assertEquals(10, HttpFetcher.resolveTimeoutSeconds(-1, 10));
        assertEquals(7, HttpFetcher.resolveTimeoutSeconds(7, 10));
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}