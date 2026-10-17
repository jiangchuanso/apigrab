package com.gitee.apigrab.core.mapping;

public interface ConvertFunction {
    Object convert(Object sourceValue, String expression);
}
