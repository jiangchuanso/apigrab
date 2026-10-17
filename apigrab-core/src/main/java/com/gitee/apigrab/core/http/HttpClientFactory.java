package com.gitee.apigrab.core.http;

import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class HttpClientFactory {

    /**
     * {@code apigrab.http.insecure-tls=true} 时使用的信任管理器：接受任意证书。
     *
     * <p>OkHttp 的 {@code sslSocketFactory(factory, trustManager)} 需要传入具体
     * {@link X509TrustManager}（用于其内部证书校验/Pinning），因此这里显式提供实例，
     * 而不是 {@code null}。
     */
    private static final X509TrustManager TRUST_ALL = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // 故意不校验：见 apigrab.http.insecure-tls
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // 故意不校验：见 apigrab.http.insecure-tls
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    @Bean
    public OkHttpClient okHttpClient(
            @Value("${apigrab.http.insecure-tls:true}") boolean insecureTls) {
        ConnectionPool pool = new ConnectionPool(50, 5, TimeUnit.MINUTES);
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectionPool(pool)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(new RetryInterceptor(3))
            .addInterceptor(new LoggingInterceptor());

        if (insecureTls) {
            applyTrustAll(builder);
            log.warn("apigrab.http.insecure-tls=true 已生效：采集客户端不再校验 TLS 证书与主机名。"
                + "该设置对整个采集链路生效，请勿在需要防中间人攻击的环境开启。");
        }
        return builder.build();
    }

    private void applyTrustAll(OkHttpClient.Builder builder) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {TRUST_ALL}, new SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), TRUST_ALL);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            // 配置已显式要求跳过校验：此处失败说明运行环境不支持该能力，
            // 必须让启动直接失败，而不是静默退回「仍在校验」的安全行为后让人误以为已生效。
            throw new IllegalStateException(
                "无法为 apigrab.http.insecure-tls 初始化 SSLContext", e);
        }
    }
}
