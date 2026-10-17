package com.gitee.apigrab.core.http;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Protocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryInterceptorTest {

    private RetryInterceptor interceptor;

    @Mock
    private Interceptor.Chain chain;

    private Request request;

    @BeforeEach
    void setUp() {
        interceptor = new RetryInterceptor(2); // maxRetries=2, so up to 3 total attempts
        request = new Request.Builder().url("https://example.com/api").get().build();
    }

    @Test
    void testSuccessOnFirstAttempt() throws IOException {
        when(chain.request()).thenReturn(request);
        Response successResponse = new Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(okhttp3.ResponseBody.create(okhttp3.MediaType.parse("application/json"), "{}"))
            .build();
        when(chain.proceed(any())).thenReturn(successResponse);

        Response result = interceptor.intercept(chain);
        assertNotNull(result);
        assertEquals(200, result.code());
        verify(chain, times(1)).proceed(any());
    }

    @Test
    void testSuccessAfterRetry() throws IOException {
        when(chain.request()).thenReturn(request);
        Response successResponse = new Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(okhttp3.ResponseBody.create(okhttp3.MediaType.parse("application/json"), "{}"))
            .build();
        // First attempt fails with IOException
        when(chain.proceed(any()))
            .thenThrow(new IOException("Connection refused"))
            .thenReturn(successResponse);

        Response result = interceptor.intercept(chain);
        assertNotNull(result);
        assertEquals(200, result.code());
        verify(chain, times(2)).proceed(any());
    }

    @Test
    void testAllRetriesFailWithIoException() throws IOException {
        when(chain.request()).thenReturn(request);
        when(chain.proceed(any())).thenThrow(new IOException("Network error"));

        IOException thrown = assertThrows(IOException.class, () -> interceptor.intercept(chain));
        assertTrue(thrown.getMessage().contains("Network error"));
        verify(chain, times(3)).proceed(any()); // 1 initial + 2 retries
    }

    @Test
    void testNonSuccessfulResponseReturned() throws IOException {
        when(chain.request()).thenReturn(request);
        Response errorResponse = new Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(500)
            .message("Server Error")
            .body(okhttp3.ResponseBody.create(okhttp3.MediaType.parse("application/json"), "{}"))
            .build();
        when(chain.proceed(any())).thenReturn(errorResponse);

        Response result = interceptor.intercept(chain);
        assertNotNull(result);
        assertEquals(500, result.code()); // Returns the non-2xx response
        verify(chain, times(3)).proceed(any()); // Retried up to maxRetries
    }

    @Test
    void testConstructor() {
        RetryInterceptor r = new RetryInterceptor(0);
        assertNotNull(r);
    }
}
