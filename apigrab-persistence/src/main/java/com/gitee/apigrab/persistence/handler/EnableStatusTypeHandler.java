package com.gitee.apigrab.persistence.handler;

import com.gitee.apigrab.persistence.entity.enums.EnableStatus;

/**
 * Maps the {@link EnableStatus} enum to/from its integer code (no-arg constructor so MyBatis
 * can instantiate it without inferring a javaType).
 */
public class EnableStatusTypeHandler extends CodedEnumTypeHandler<EnableStatus> {

    public EnableStatusTypeHandler() {
        super(EnableStatus.class);
    }
}
