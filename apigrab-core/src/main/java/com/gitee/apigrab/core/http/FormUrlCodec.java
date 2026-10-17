package com.gitee.apigrab.core.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * FORM 请求体（{@code application/x-www-form-urlencoded} 编码串）上的占位符替换。
 *
 * <p>FORM 正文的存储契约是编码串（前端按标准表单序列化落库，后端
 * {@code HttpRequestBuilder#createFormBody} 逐段解码后重新编码发送），因此值里的
 * {@code ${id}} 会以 {@code %24%7Bid%7D} 的形式保存。字面量匹配无法识别这种编码态，
 * 本工具按 <b>解码 → 替换 → 编码</b> 处理：
 * <ul>
 *   <li>未编码（{@code id=${id}}）与已编码（{@code id=%24%7Bid%7D}）两种存量写法都能命中；</li>
 *   <li>只有命中的键值段会被重新编码，其余段按原样保留，不改变既有字节；</li>
 *   <li>注入值按表单规则编码，值内的 {@code &}/{@code =}/{@code +}/{@code %} 不会破坏报文分隔。</li>
 * </ul>
 *
 * <p>各段解码失败（如非法的 {@code %} 序列）时以原文参与匹配；命中段仍会按字面 {@code %} 重新编码
 * （{@code %zz} → {@code %25zz}），发送侧解码后语义不变。
 */
public final class FormUrlCodec {

    private static final String UTF_8 = "UTF-8";

    private FormUrlCodec() {
    }

    /**
     * 把 {@code token} 替换为 {@code value}，输出仍是可直接落库/发送的编码串。
     *
     * @return 替换后的编码串；未命中任何占位符时返回 {@code null}（调用方据此判断"未替换"）
     */
    public static String replacePlaceholder(String encodedBody, String token, String value) {
        if (encodedBody == null || encodedBody.isEmpty() || token == null || token.isEmpty()) {
            return null;
        }
        String replacement = value == null ? "" : value;
        boolean matched = false;
        List<String> pairs = new ArrayList<>();
        for (String pair : encodedBody.split("&", -1)) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            String rawKey = idx >= 0 ? pair.substring(0, idx) : pair;
            String rawValue = idx >= 0 ? pair.substring(idx + 1) : "";
            String key = decode(rawKey);
            String val = decode(rawValue);
            if (!key.contains(token) && !val.contains(token)) {
                pairs.add(pair);
                continue;
            }
            matched = true;
            pairs.add(encodeFormComponent(key.replace(token, replacement)) + "="
                    + encodeFormComponent(val.replace(token, replacement)));
        }
        return matched ? String.join("&", pairs) : null;
    }

    private static String decode(String part) {
        try {
            return URLDecoder.decode(part, UTF_8);
        } catch (Exception e) {
            return part;
        }
    }

    /** 表单编码：空格转 {@code +}，保留字符转百分号编码（与前端 URLSearchParams 对称）。 */
    private static String encodeFormComponent(String value) {
        try {
            return URLEncoder.encode(value, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported", e);
        }
    }
}
