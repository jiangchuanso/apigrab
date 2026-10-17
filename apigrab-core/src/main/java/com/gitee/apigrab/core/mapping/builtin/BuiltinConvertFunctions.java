package com.gitee.apigrab.core.mapping.builtin;

import com.gitee.apigrab.core.mapping.ConvertFunction;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class BuiltinConvertFunctions {

    private static final Map<String, ConvertFunction> functions = new HashMap<>();

    static {
        functions.put("to_string", (value, expr) -> Optional.ofNullable(value).map(Object::toString).orElse(null));
        functions.put("to_int", (value, expr) -> {
            if (value == null) return null;
            if (value instanceof Number) return ((Number) value).intValue();
            BigDecimal decimal = parseDecimal(value);
            return decimal == null ? null : decimal.intValue();
        });
        functions.put("to_decimal", (value, expr) -> {
            if (value == null) return null;
            return parseDecimal(value);
        });
        functions.put("to_date", (value, expr) -> {
            // expr 是日期格式，如 "yyyy-MM-dd HH:mm:ss"
            if (value == null) return null;
            String format = Optional.ofNullable(expr).filter(e -> !e.isEmpty()).orElse("yyyy-MM-dd HH:mm:ss");
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                return sdf.format(value instanceof Date ? (Date) value : sdf.parse(value.toString()));
            } catch (Exception e) {
                return value.toString();
            }
        });
        functions.put("default_value", (value, expr) -> Optional.ofNullable(value).orElse(expr));
        functions.put("trim", (value, expr) -> Optional.ofNullable(value).map(Object::toString).map(String::trim).orElse(null));
        functions.put("upper", (value, expr) -> Optional.ofNullable(value).map(Object::toString).map(String::toUpperCase).orElse(null));
        functions.put("lower", (value, expr) -> Optional.ofNullable(value).map(Object::toString).map(String::toLowerCase).orElse(null));
    }

    /**
     * 将任意值解析为 {@link BigDecimal}。
     *
     * <p>空值或非法数字（空串、{@code "abc"}、JSON 里的 {@code "null"} 等）返回 {@code null}，
     * 而**不抛异常**：转换失败只应让该字段取空值，绝不能因单条脏数据中断整批采集
     * （此前 {@code new BigDecimal(value.toString())} 抛出的 {@code NumberFormatException}
     * 会一路冒泡到 {@code CollectorEngine}，导致整次执行失败）。
     */
    private static BigDecimal parseDecimal(Object value) {
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            log.warn("数值转换失败，字段取空值: {}", text);
            return null;
        }
    }

    public static ConvertFunction getFunction(String name) {
        return functions.get(name);
    }

    public static boolean hasFunction(String name) {
        return functions.containsKey(name);
    }
}
