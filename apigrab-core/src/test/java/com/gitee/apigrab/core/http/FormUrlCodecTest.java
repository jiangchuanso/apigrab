package com.gitee.apigrab.core.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FormUrlCodecTest {

    @Test
    void replacesRawToken() {
        assertEquals("id=42&scope=full",
                FormUrlCodec.replacePlaceholder("id=${id}&scope=full", "${id}", "42"));
    }

    @Test
    void replacesPercentEncodedToken() {
        // 前端按表单规则序列化后，值里的 ${id} 变成 %24%7Bid%7D
        assertEquals("id=42", FormUrlCodec.replacePlaceholder("id=%24%7Bid%7D", "${id}", "42"));
    }

    @Test
    void encodesInjectedValueAndLeavesOtherPairsUntouched() {
        assertEquals("q=a%26b&id=7&x=1",
                FormUrlCodec.replacePlaceholder("q=a%26b&id=%24%7Bid%7D&x=1", "${id}", "7"),
                "未命中的段应原样保留（其本身已是编码态）");
    }

    @Test
    void encodesReservedCharactersInValue() {
        assertEquals("id=a%26b%3Dc%2Bd+5%25",
                FormUrlCodec.replacePlaceholder("id=${id}", "${id}", "a&b=c+d 5%"),
                "注入值里的 & = + % 必须编码，否则会破坏表单分隔");
    }

    @Test
    void replacesTokenInKey() {
        assertEquals("42=x", FormUrlCodec.replacePlaceholder("%24%7Bid%7D=x", "${id}", "42"));
    }

    @Test
    void replacesEveryOccurrenceWithinOneValue() {
        assertEquals("id=1-1", FormUrlCodec.replacePlaceholder("id=${id}-%24%7Bid%7D", "${id}", "1"));
    }

    @Test
    void replacesLowercasePercentEncoding() {
        // 不同工具产出的十六进制大小写可能不同
        assertEquals("id=42", FormUrlCodec.replacePlaceholder("id=%24%7bid%7d", "${id}", "42"));
    }

    @Test
    void encodesNonAsciiValueSymmetrically() {
        assertEquals("name=%E5%BC%A0%E4%B8%89+%26+%E6%9D%8E%E5%9B%9B",
                FormUrlCodec.replacePlaceholder("name=${id}", "${id}", "张三 & 李四"));
    }

    @Test
    void nullValueReplacesTokenWithEmptyString() {
        assertEquals("id=&a=1", FormUrlCodec.replacePlaceholder("id=${id}&a=1", "${id}", null));
    }

    @Test
    void returnsNullWhenTokenAbsent() {
        assertNull(FormUrlCodec.replacePlaceholder("a=1&b=2", "${id}", "42"));
        assertNull(FormUrlCodec.replacePlaceholder(null, "${id}", "42"));
        assertNull(FormUrlCodec.replacePlaceholder("", "${id}", "42"));
        assertNull(FormUrlCodec.replacePlaceholder("a=1", null, "42"));
        assertNull(FormUrlCodec.replacePlaceholder("a=1", "", "42"));
    }

    @Test
    void malformedPercentIsReencodedAsLiteralPercent() {
        // 解码失败时以原文匹配；命中段按字面 % 重新编码，发送侧解码后仍是 42%zz
        assertEquals("id=42%25zz", FormUrlCodec.replacePlaceholder("id=${id}%zz", "${id}", "42"));
    }
}
