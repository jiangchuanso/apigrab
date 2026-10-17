package com.gitee.apigrab.core.http;

import cn.hutool.core.util.StrUtil;
import com.gitee.apigrab.persistence.entity.enums.RespType;
import org.springframework.stereotype.Component;

/**
 * 依据 HTTP 响应的 {@code Content-Type} 头判定接口响应格式（design C2）。
 *
 * <p>本判定器是纯函数：只接受响应头字符串，<b>不接收也不得读取响应体</b>。
 * 判定不出时 {@link Detection} 的两个结论字段均为 {@code null}——这是「不做无据推断」
 * 在类型层面的落点，调用方据此决定是否写入表单，任何默认值都会构成静默改值。
 */
@Component
public class RespTypeDetector {

    /** 显式声明为多行 JSON 的媒体类型；这三种可以判定出 NDJSON 形态。 */
    private static final String[] NDJSON_TYPES = {
            "application/x-ndjson", "application/ndjson", "application/jsonl"};

    /** 归一化后判定为 {@link RespType#JSON}。 */
    public Detection detect(String contentType) {
        String normalized = normalize(contentType);
        if (normalized == null) {
            return new Detection(null, null, contentType);
        }
        for (String ndjson : NDJSON_TYPES) {
            if (ndjson.equals(normalized)) {
                // NDJSON 是 JSON 的一种形态，多行开关据此开启
                return new Detection(RespType.JSON, 1, contentType);
            }
        }
        if (hasSubtype(normalized, "json")) {
            // 普通 JSON 媒体类型无法区分是否为 NDJSON，多行结论保持未判定（假设 A1）
            return new Detection(RespType.JSON, null, contentType);
        }
        if (hasSubtype(normalized, "xml")) {
            // XML 分支先于 NDJSON 分支生效，二者互斥，多行开关据此关闭（假设 A2）
            return new Detection(RespType.XML, 0, contentType);
        }
        return new Detection(null, null, contentType);
    }

    /** 去空白、丢弃分号之后的参数、转为小写；结果为空时返回 {@code null}。 */
    private String normalize(String contentType) {
        String trimmed = StrUtil.trimToNull(contentType);
        if (trimmed == null) {
            return null;
        }
        int semicolon = trimmed.indexOf(';');
        if (semicolon >= 0) {
            trimmed = trimmed.substring(0, semicolon);
        }
        return StrUtil.trimToNull(trimmed.toLowerCase());
    }

    /** 子类型是否精确等于或以 {@code +后缀} 形式结尾。 */
    private boolean hasSubtype(String normalized, String subtype) {
        int slash = normalized.indexOf('/');
        if (slash < 0 || slash == normalized.length() - 1) {
            return false;
        }
        String actual = normalized.substring(slash + 1);
        return actual.equals(subtype) || actual.endsWith("+" + subtype);
    }

    /**
     * 判定结果。{@code respType} 与 {@code multilines} 为 {@code null} 表示「未能判定」，
     * 调用方必须原样保留用户现有设置。
     */
    public static class Detection {

        private final RespType respType;
        private final Integer multilines;
        private final String contentType;

        public Detection(RespType respType, Integer multilines, String contentType) {
            this.respType = respType;
            this.multilines = multilines;
            this.contentType = contentType;
        }

        /** 判定的响应格式；{@code null} 表示未能判定。 */
        public RespType getRespType() {
            return respType;
        }

        /** 判定的多行响应形态（1 开启 / 0 关闭）；{@code null} 表示未能判定。 */
        public Integer getMultilines() {
            return multilines;
        }

        /** 原始 Content-Type 头，未携带时为 {@code null}。 */
        public String getContentType() {
            return contentType;
        }
    }
}