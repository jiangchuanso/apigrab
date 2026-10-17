package com.gitee.apigrab.core.http;

import com.gitee.apigrab.persistence.entity.enums.RespType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 判定器契约测试。
 *
 * 核心语义：判定只依据 Content-Type 响应头，绝不接触响应体；「未判定」必须表达为 null，
 * 而不是推断成任何具体格式或开关值——这是 design C3「不做无据推断」的类型层面落点。
 */
class RespTypeDetectorTest {

    private final RespTypeDetector detector = new RespTypeDetector();

    /** 可判定的响应头。 */
    static Stream<Arguments> supportedHeaders() {
        return Stream.of(
                // 普通 JSON：无法从响应头区分 NDJSON，多行结论必须为 null（假设 A1）
                Arguments.of("application/json", RespType.JSON, null),
                Arguments.of("application/json; charset=utf-8", RespType.JSON, null),
                Arguments.of("APPLICATION/JSON", RespType.JSON, null),
                Arguments.of("  application/json  ", RespType.JSON, null),
                Arguments.of("application/vnd.api+json", RespType.JSON, null),
                // 显式 NDJSON 媒体类型：多行结论为开启
                Arguments.of("application/x-ndjson", RespType.JSON, 1),
                Arguments.of("application/ndjson", RespType.JSON, 1),
                Arguments.of("application/jsonl", RespType.JSON, 1),
                Arguments.of("application/jsonl; charset=utf-8", RespType.JSON, 1),
                // XML：与 NDJSON 互斥，多行结论为关闭（假设 A2）
                Arguments.of("application/xml", RespType.XML, 0),
                Arguments.of("text/xml", RespType.XML, 0),
                Arguments.of("application/soap+xml", RespType.XML, 0));
    }

    /** 无法判定的响应头：缺失、为空、仅有分隔符、或落在不受支持的媒体类型上。 */
    static Stream<Arguments> unsupportedHeaders() {
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of(""),
                Arguments.of("   "),
                Arguments.of(";"),
                Arguments.of(";;;"),
                Arguments.of("text/plain"),
                Arguments.of("application/octet-stream"),
                Arguments.of("text/html"),
                Arguments.of("application/x-www-form-urlencoded"),
                Arguments.of("application/jsonish"));
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1} / multilines={2}")
    @MethodSource("supportedHeaders")
    void detectsSupportedContentTypes(String contentType, RespType expectedRespType, Integer expectedMultilines) {
        RespTypeDetector.Detection detection = detector.detect(contentType);

        assertEquals(expectedRespType, detection.getRespType());
        assertEquals(expectedMultilines, detection.getMultilines());
    }

    @ParameterizedTest(name = "[{index}] {0} -> 未判定")
    @MethodSource("unsupportedHeaders")
    void leavesConclusionsUndetectedForUnsupportedContentTypes(String contentType) {
        RespTypeDetector.Detection detection = detector.detect(contentType);

        assertNull(detection.getRespType(), "响应格式必须保持未判定，不得推断为 JSON");
        assertNull(detection.getMultilines(), "多行结论必须保持未判定，不得推断为 0 或 1");
    }

    @Test
    void echoesRawHeaderVerbatim() {
        assertEquals("application/json; charset=utf-8",
                detector.detect("application/json; charset=utf-8").getContentType());
        assertEquals("text/plain", detector.detect("text/plain").getContentType());
        assertNull(detector.detect(null).getContentType());
    }
}