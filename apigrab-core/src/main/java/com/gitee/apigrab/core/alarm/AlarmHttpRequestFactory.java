package com.gitee.apigrab.core.alarm;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javax.net.ssl.SSLContext;

@Slf4j
public class AlarmHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {

    private static final TrustStrategy TRUST_ALL = (x509Certificates, authType) -> true;

    public AlarmHttpRequestFactory() {
        try {
            SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, TRUST_ALL).build();
            SSLConnectionSocketFactory csf =
                new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            cm.setMaxTotal(50);
            cm.setDefaultMaxPerRoute(10);

            CloseableHttpClient client = HttpClients.custom()
                .setSSLSocketFactory(csf)
                .setConnectionManager(cm)
                .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectTimeout(2000)
                    .setSocketTimeout(60000)
                    .build())
                .build();
            setHttpClient(client);
        } catch (Exception e) {
            log.warn("Failed to init SSL RestTemplate", e);
        }
    }
}
