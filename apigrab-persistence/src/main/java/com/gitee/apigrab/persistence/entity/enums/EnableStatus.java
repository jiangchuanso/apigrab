package com.gitee.apigrab.persistence.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Generic enabled/disabled status (api / datasource / alarm): 0 disabled, 1 enabled.
 */
public enum EnableStatus implements CodedEnum {
    DISABLED(0),
    ENABLED(1);

    private final int code;

    EnableStatus(int code) {
        this.code = code;
    }

    @Override
    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static EnableStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (EnableStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
