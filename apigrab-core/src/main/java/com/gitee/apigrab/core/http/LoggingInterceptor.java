package com.gitee.apigrab.core.http;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

@Slf4j
public class LoggingInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        long start = System.currentTimeMillis();
        log.debug("--> {} {}", request.method(), request.url());

        Response response = chain.proceed(request);
        long cost = System.currentTimeMillis() - start;
        log.debug("<-- {} {} ({}ms)", response.code(), request.url(), cost);

        return response;
    }
}
