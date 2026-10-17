package com.gitee.apigrab.core.http;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

@Slf4j
public class RetryInterceptor implements Interceptor {
    private final int maxRetries;

    public RetryInterceptor(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;
        IOException lastException = null;

        for (int i = 0; i <= maxRetries; i++) {
            if (i > 0) {
                long backoff = (long) Math.pow(2, i - 1) * 1000;
                log.info("Retry {} for request {}, backoff {}ms", i, request.url(), backoff);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            try {
                if (response != null) {
                    response.close();
                }
                response = chain.proceed(request);
                if (response.isSuccessful()) {
                    return response;
                }
            } catch (IOException e) {
                lastException = e;
                log.warn("Request failed (attempt {}/{}): {}", i + 1, maxRetries + 1, e.getMessage());
            }
        }
        if (response != null && !response.isSuccessful()) {
            return response;
        }
        throw java.util.Optional.ofNullable(lastException)
            .orElseGet(() -> new IOException("Request failed after " + (maxRetries + 1) + " attempts"));
    }
}
