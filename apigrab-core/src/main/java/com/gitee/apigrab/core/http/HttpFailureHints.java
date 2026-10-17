package com.gitee.apigrab.core.http;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.regex.Pattern;

/**
 * 把传输层异常的原始消息翻译成可操作的中文提示。
 *
 * <p>起因：把 {@code http://} 指向一个 TLS 端口时，OkHttp 只会抛出
 * {@code unexpected end of stream} / {@code EOFException: \n not found: limit=7
 * content=15030300020250…}。这条消息看不出「地址协议写错了」，使用者会去排查 OkHttp 与网络，
 * 而真正要做的是把地址改成 {@code https://}。
 *
 * <p>本类<strong>只影响异常消息文本，不改变任何请求行为</strong>；识别失败时原样返回原始消息。
 * 由于 OkHttp 不在异常里携带响应字节，判定只能依据异常链的消息文本，属启发式。
 */
final class HttpFailureHints {

    /**
     * TLS 记录前缀：ContentType（15=alert / 16=handshake / 17=application_data）+ 版本 03 0x。
     * 对应 Okio 的 {@code EOFException} 消息形如 {@code content=15030300020250…}。
     *
     * <p>必须成对匹配两个字节：单看 {@code content=15…} 会把普通十六进制内容误判为 TLS。
     */
    private static final Pattern TLS_RECORD_ON_PLAIN_HTTP =
        Pattern.compile("content=\\s*1[567]0[0-9a-f]", Pattern.CASE_INSENSITIVE);

    private HttpFailureHints() {
    }

    /**
     * @param url 本次请求的地址，用于在提示里指明配置对象
     * @return 可操作的中文提示；无法识别时返回原始消息
     */
    static String describe(IOException e, String url) {
        String raw = String.valueOf(e.getMessage());
        if (looksLikeTlsRecordOnPlainHttp(e)) {
            return "目标地址 " + url + " 返回的是 TLS 记录而不是 HTTP 响应，"
                + "说明该端口是 HTTPS 服务，但接口地址配置成了 http://。"
                + "请把接口地址改为 https:// 后重试。"
                + "（若证书为内部自签名/私有 CA 导致仍失败，见配置项 apigrab.http.insecure-tls）"
                + " 原始错误：" + raw;
        }
        if (looksLikeCertificateRejection(e)) {
            return "目标地址 " + url + " 的 TLS 证书不被信任（自签名或私有 CA）。"
                + "请把该 CA 导入 JVM 信任库，或开启 apigrab.http.insecure-tls 后重试。"
                + " 原始错误：" + raw;
        }
        if (looksLikeTlsHandshakeFailure(e)) {
            // 走到这里说明是 TLS 层失败，但看不出是证书问题（可能是协议版本/加密套件不匹配），
            // 因此只提示「握手失败」，不误导使用者去处理证书。
            return "目标地址 " + url + " 的 TLS 握手失败（非证书信任问题，可能是协议版本或加密套件不匹配）。"
                + " 原始错误：" + raw;
        }
        return raw;
    }

    private static boolean looksLikeTlsRecordOnPlainHttp(IOException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && TLS_RECORD_ON_PLAIN_HTTP.matcher(message).find()) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeCertificateRejection(IOException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof CertificateException) {
                return true;
            }
            String message = t.getMessage();
            if (message != null && (message.contains("PKIX")
                || message.contains("CertPath")
                || message.toLowerCase().contains("unable to find valid certification path")
                || message.toLowerCase().contains("certificate"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeTlsHandshakeFailure(IOException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SSLException) {
                return true;
            }
        }
        return false;
    }
}
